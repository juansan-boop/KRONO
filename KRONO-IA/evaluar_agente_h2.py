"""
KRONO-AI - H2: Evaluación del Agente de Búsqueda Informada (Elemento de Desempeño)
===================================================================================
Evalúa el Elemento de Desempeño (H2) sobre los 60 escenarios de 3 días
(escenarios_tareas.csv y escenarios_bloques.csv), comparando 4 políticas de planificación:

  1. FIFO + Estimación Usuario (B0):
     Orden de llegada original sin búsqueda ni ML; planifica a ciegas con la estimación subjetiva.
  2. EDF (Fecha Límite) + Estimación Usuario (B0):
     Earliest Deadline First; ordena por fecha límite sin ML ni afinidad horaria.
  3. Búsqueda Informada H2 + Estimación Usuario (B0):
     Algoritmo de búsqueda informada con función de evaluación heurística f(n) = g(n) + h(n)
     (urgencia, prioridad, afinidad circadiana y fatiga), pero planifica con estimación del usuario.
  4. Agente KRONO-AI Completo (Búsqueda H2 + Predicción Híbrida H1):
     Combina la búsqueda informada H2 con las duraciones predichas por el modelo híbrido H1
     (M1 Ridge log-log + corrección online de sesgo de usuario).

Métricas Oficiales (60 escenarios de 3 días):
  1. entregas_a_tiempo_pct: Porcentaje de tareas completadas antes o en su fecha_limite_min.
  2. aprovechamiento_bloques_pct: Eficiencia de uso productivo de bloques (minutos a tiempo y afines menos penalización por desborde).
  3. reordenamientos_emergencia_prom: Promedio de reprogramaciones por sobrecosto o desplazamiento.

Salidas en ./resultados:
  - metricas_agente_h2.csv
  - desempeno_agente_h2.png

Uso:
  python evaluar_agente_h2.py
"""

import json
import math
import os
import sys

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

SEMILLA = 42
CARPETA = os.path.dirname(os.path.abspath(__file__))
SALIDA = os.path.join(CARPETA, "resultados")

# Carga cognitiva relativa por categoría (0.0 a 1.0)
CARGA_COGNITIVA = {
    "programacion": 1.00,
    "estudio_parcial": 0.85,
    "redaccion": 0.75,
    "ejercicios": 0.60,
    "exposicion": 0.55,
    "lectura": 0.40,
}

# Fricción según bloque horario (ritmo circadiano)
FRICCION_BLOQUE = {
    "manana": -0.30,  # Alta frescura mental: favorece tareas pesadas
    "tarde": 0.00,    # Capacidad media
    "noche": 0.60,    # Fatiga acumulada: penaliza tareas de alta concentración
}

# Ponderación por prioridad
PESO_PRIORIDAD = {
    3: 3.0,  # Alta
    2: 1.8,  # Media
    1: 1.0,  # Baja
}


def cargar_datos():
    tareas_path = os.path.join(CARPETA, "escenarios_tareas.csv")
    bloques_path = os.path.join(CARPETA, "escenarios_bloques.csv")
    modelo_path = os.path.join(SALIDA, "modelo_app.json")

    if not os.path.exists(tareas_path) or not os.path.exists(bloques_path):
        raise FileNotFoundError("Faltan archivos de escenarios. Ejecuta primero SCRIPT.PY.")
    if not os.path.exists(modelo_path):
        raise FileNotFoundError("Falta modelo_app.json. Ejecuta primero entrenar_modelo.py.")

    tareas_df = pd.read_csv(tareas_path)
    bloques_df = pd.read_csv(bloques_path)
    with open(modelo_path, "r", encoding="utf-8") as f:
        modelo_app = json.load(f)

    return tareas_df, bloques_df, modelo_app


# ---------------------------------------------------------------------------
# Estimador H1 (M1 + Adaptación Online al Sesgo Personal)
# ---------------------------------------------------------------------------
def predecir_duracion_m1(tarea, bloque_horario, dias_a_limite, modelo_app):
    intercepto = modelo_app["intercepto"]
    coefs = modelo_app["coeficientes"]
    log_est = math.log(tarea["estimacion_usuario_min"])
    prio = float(tarea["prioridad"])
    c_bloque = coefs.get(f"bloque_horario_{bloque_horario}", 0.0)

    log_pred = (
        intercepto
        + coefs["log_estimacion"] * log_est
        + coefs["dias_a_fecha_limite"] * dias_a_limite
        + coefs["prioridad"] * prio
        + c_bloque
    )
    return max(5.0, math.exp(log_pred))


def predecir_duracion_h1(tarea, bloque_horario, tiempo_actual_min, razones_completadas, modelo_app):
    dias_limite = max(0.0, (tarea["fecha_limite_min"] - tiempo_actual_min) / 1440.0)
    pred_m1 = predecir_duracion_m1(tarea, bloque_horario, dias_limite, modelo_app)
    k = modelo_app["hibrido"]["K"]
    ventana = modelo_app["hibrido"]["ventana"]
    recientes = razones_completadas[-ventana:]

    if recientes:
        n = len(recientes)
        sesgo = math.exp(np.mean(np.log(recientes)))
        peso = n / (n + k)
        factor = 1.0 + (sesgo - 1.0) * peso
    else:
        # Arranque en frío: M1 predice directamente la duración real esperada
        factor = 1.0

    return max(5.0, pred_m1 * factor)


def obtener_duracion_planificada(tarea, bloque, tiempo_cursor, politica, razones_h1, restante_real, modelo_app):
    fraccion_restante = restante_real / tarea["duracion_real_min"]
    if politica == "KRONO_AI":
        # Factor de reserva preventiva del 5% para absorber varianza estocástica y proteger entregas
        dur_total = predecir_duracion_h1(tarea, bloque["bloque_horario"], tiempo_cursor, razones_h1, modelo_app) * 1.05
    elif politica == "H2_B0":
        # Búsqueda informada con margen preventivo heurístico del 15%
        dur_total = float(tarea["estimacion_usuario_min"]) * 1.15
    else:
        # B0: Estimación subjetiva
        dur_total = float(tarea["estimacion_usuario_min"])
    return max(5.0, dur_total * fraccion_restante)


# ---------------------------------------------------------------------------
# Heurística de Búsqueda Informada H2: f(n) = g(n) + h(n)
# ---------------------------------------------------------------------------
def evaluar_secuencia_en_bloques(orden_tareas, bloques_restantes, estado_tareas, politica, razones_h1, modelo_app):
    """
    Simula la proyección del cronograma en los bloques restantes bajo el orden dado
    y evalúa el costo total f(n) = g(n) + h(n):
      - Penalización por vencimiento o holgura ajustada
      - Ponderación por prioridad
      - Desajuste circadiano y fatiga
      - Penalización por fragmentación (división de tareas)
    """
    costo_total = 0.0
    idx_bloque = 0
    num_bloques = len(bloques_restantes)
    if num_bloques == 0:
        return 999999.0

    bloque_actual = bloques_restantes[idx_bloque]
    tiempo_bloque_restante = float(bloque_actual["duracion_min"])
    cursor_tiempo = float(bloque_actual["inicio_min"])

    for tarea in orden_tareas:
        tid = tarea["tarea_id"]
        restante_real = estado_tareas[tid]["restante_real"]
        dur_plan = obtener_duracion_planificada(tarea, bloque_actual, cursor_tiempo, politica, razones_h1, restante_real, modelo_app)

        trabajo_pendiente = dur_plan
        splits = 0

        while trabajo_pendiente > 0:
            if idx_bloque >= num_bloques:
                costo_total += 30000.0 * tarea["prioridad"]
                break

            if tiempo_bloque_restante <= 0:
                idx_bloque += 1
                if idx_bloque >= num_bloques:
                    costo_total += 30000.0 * tarea["prioridad"]
                    break
                bloque_actual = bloques_restantes[idx_bloque]
                tiempo_bloque_restante = float(bloque_actual["duracion_min"])
                cursor_tiempo = float(bloque_actual["inicio_min"])

            if trabajo_pendiente <= tiempo_bloque_restante:
                asignado = trabajo_pendiente
                trabajo_pendiente = 0.0
                tiempo_bloque_restante -= asignado
                cursor_tiempo += asignado
            else:
                asignado = tiempo_bloque_restante
                trabajo_pendiente -= asignado
                tiempo_bloque_restante = 0.0
                cursor_tiempo += asignado
                splits += 1

            # Costo de fricción circadiana y fatiga en este fragmento (desempate inteligente)
            carga = CARGA_COGNITIVA.get(tarea["categoria"], 0.6)
            friccion = FRICCION_BLOQUE.get(bloque_actual["bloque_horario"], 0.0)
            fatiga = tarea["fatiga"] / 3.0
            holgura_estimada = float(tarea["fecha_limite_min"]) - cursor_tiempo
            peso_circadiano = 8.0 if (politica == "H2_B0" and holgura_estimada < 480.0) else 25.0
            costo_circadiano = carga * friccion * fatiga * (asignado / dur_plan) * peso_circadiano
            costo_total += costo_circadiano

        tiempo_fin = cursor_tiempo
        limite = float(tarea["fecha_limite_min"])
        holgura = limite - (tiempo_fin + (dur_plan * 0.15 if politica == "H2_B0" else 0.0))
        w_prio = PESO_PRIORIDAD.get(tarea["prioridad"], 1.0)

        # Costo por vencimiento y holgura (prioridad absoluta a fechas límite)
        if holgura < 0:
            costo_total += 25000.0 * w_prio + abs(holgura) * 60.0 * w_prio
        elif holgura < 240.0:  # Menos de 4 horas
            costo_total += 2000.0 * w_prio + (3500.0 * w_prio) / (holgura + 10.0)
        elif holgura < 720.0:  # Menos de 12 horas
            costo_total += 400.0 * w_prio + (1200.0 * w_prio) / (holgura + 30.0)
        else:
            costo_total += (300.0 * w_prio) / (holgura / 60.0 + 1.0)

        # Penalización por división
        costo_total += splits * 30.0

    return costo_total


def buscar_orden_optimo_h2(candidatas, bloques_restantes, estado_tareas, politica, razones_h1, modelo_app, beam_width=20):
    """Búsqueda informada Beam Search que explora el espacio de asignación."""
    if len(candidatas) <= 1:
        return candidatas

    beam = [([], candidatas)]
    for _ in range(len(candidatas)):
        siguientes = []
        for parcial, restantes in beam:
            restantes_ordenados = sorted(restantes, key=lambda t: (t["fecha_limite_min"], -t["prioridad"]))
            for prox in restantes:
                nuevo_parcial = parcial + [prox]
                nuevo_restantes = [t for t in restantes_ordenados if t["tarea_id"] != prox["tarea_id"]]
                costo = evaluar_secuencia_en_bloques(
                    nuevo_parcial + nuevo_restantes, bloques_restantes, estado_tareas, politica, razones_h1, modelo_app
                )
                siguientes.append((costo, nuevo_parcial, [t for t in restantes if t["tarea_id"] != prox["tarea_id"]]))
        siguientes.sort(key=lambda x: x[0])
        beam = [(parcial, restantes) for costo, parcial, restantes in siguientes[:beam_width]]

    return beam[0][0]


# ---------------------------------------------------------------------------
# Simulación de Ejecución en el Entorno
# ---------------------------------------------------------------------------
def simular_politica_escenario(escenario_id, politica, tareas_df, bloques_df, modelo_app):
    tareas = tareas_df[tareas_df["escenario_id"] == escenario_id].to_dict("records")
    bloques = bloques_df[bloques_df["escenario_id"] == escenario_id].sort_values("inicio_min").to_dict("records")

    estado_tareas = {
        t["tarea_id"]: {
            "restante_real": float(t["duracion_real_min"]),
            "completada": False,
            "minuto_completada": None,
            "entregada_a_tiempo": False,
        }
        for t in tareas
    }

    razones_h1 = []
    reordenamientos_emergencia = 0
    minutos_afines_a_tiempo_tot = 0.0
    penalizacion_desborde_tot = 0.0
    minutos_bloques_utilizados_tot = 0.0

    idx_b = 0
    while idx_b < len(bloques):
        bloque = bloques[idx_b]
        tareas_pendientes = [t for t in tareas if not estado_tareas[t["tarea_id"]]["completada"]]
        if not tareas_pendientes:
            break

        bloques_restantes = bloques[idx_b:]

        # 1. Determinar el orden de ejecución según la política
        if politica == "FIFO":
            orden = sorted(tareas_pendientes, key=lambda t: t["tarea_id"])
        elif politica == "EDF":
            orden = sorted(tareas_pendientes, key=lambda t: (t["fecha_limite_min"], -t["prioridad"]))
        elif politica in ("H2_B0", "KRONO_AI"):
            orden = buscar_orden_optimo_h2(tareas_pendientes, bloques_restantes, estado_tareas, politica, razones_h1, modelo_app)
        else:
            raise ValueError(f"Política no reconocida: {politica}")

        # 2. Construir el plan del bloque usando duraciones planificadas
        dur_bloque = float(bloque["duracion_min"])
        inicio_bloque = float(bloque["inicio_min"])
        minutos_disponibles = dur_bloque
        cursor_tiempo = inicio_bloque

        plan_bloque = []
        for t in orden:
            if minutos_disponibles <= 0:
                break
            restante_real = estado_tareas[t["tarea_id"]]["restante_real"]
            dur_plan = obtener_duracion_planificada(t, bloque, cursor_tiempo, politica, razones_h1, restante_real, modelo_app)

            if dur_plan <= minutos_disponibles:
                plan_bloque.append((t, dur_plan, True))
                minutos_disponibles -= dur_plan
                cursor_tiempo += dur_plan
            else:
                # Permite avanzar parcialmente si quedan al menos 10.0 minutos disponibles
                if minutos_disponibles >= 10.0 or len(plan_bloque) == 0:
                    plan_bloque.append((t, minutos_disponibles, False))
                    minutos_disponibles = 0.0
                    cursor_tiempo += dur_plan
                    break
                else:
                    continue

        if not plan_bloque:
            idx_b += 1
            continue

        # 3. Simular la ejecución real en el bloque
        offset_bloque = 0.0
        reordenamiento_en_bloque = False
        penalizacion_desborde_bloque = 0.0
        minutos_afines_a_tiempo_bloque = 0.0

        for idx_item, (t, min_plan, espera_terminar) in enumerate(plan_bloque):
            if offset_bloque >= dur_bloque:
                reordenamiento_en_bloque = True
                penalizacion_desborde_bloque += 15.0
                continue

            tid = t["tarea_id"]
            restante_real = estado_tareas[tid]["restante_real"]
            tiempo_libre = dur_bloque - offset_bloque

            carga = CARGA_COGNITIVA.get(t["categoria"], 0.6)
            friccion = FRICCION_BLOQUE.get(bloque["bloque_horario"], 0.0)
            factor_afinidad = max(0.75, 1.0 - carga * max(0.0, friccion) * 0.3)

            # Efecto real de fatiga y desajuste circadiano sobre el tiempo requerido
            penalizacion_ritmo = 1.0 + max(0.0, carga * friccion * (t["fatiga"] / 3.0) * 0.18)
            tiempo_necesario = restante_real * penalizacion_ritmo

            if tiempo_necesario <= min_plan:
                tiempo_ejecutado = tiempo_necesario
                estado_tareas[tid]["restante_real"] = 0.0
                estado_tareas[tid]["completada"] = True
                min_fin = inicio_bloque + offset_bloque + tiempo_ejecutado
                estado_tareas[tid]["minuto_completada"] = min_fin

                a_tiempo = (min_fin <= t["fecha_limite_min"])
                if a_tiempo:
                    estado_tareas[tid]["entregada_a_tiempo"] = True
                    minutos_afines_a_tiempo_bloque += (tiempo_ejecutado / penalizacion_ritmo) * factor_afinidad
                else:
                    minutos_afines_a_tiempo_bloque += (tiempo_ejecutado / penalizacion_ritmo) * 0.30

                if politica == "KRONO_AI":
                    pred_m1 = predecir_duracion_m1(t, bloque["bloque_horario"], (t["fecha_limite_min"] - min_fin) / 1440.0, modelo_app)
                    razones_h1.append(t["duracion_real_min"] / pred_m1)

                offset_bloque += tiempo_ejecutado

            else:
                tiempo_ejecutado = min(tiempo_necesario, tiempo_libre)
                avanzado_real = tiempo_ejecutado / penalizacion_ritmo
                estado_tareas[tid]["restante_real"] -= avanzado_real
                offset_bloque += tiempo_ejecutado

                termino = (estado_tareas[tid]["restante_real"] <= 0.001)
                if termino:
                    estado_tareas[tid]["completada"] = True
                    min_fin = inicio_bloque + offset_bloque
                    estado_tareas[tid]["minuto_completada"] = min_fin
                    a_tiempo = (min_fin <= t["fecha_limite_min"])
                    if a_tiempo:
                        estado_tareas[tid]["entregada_a_tiempo"] = True
                        minutos_afines_a_tiempo_bloque += avanzado_real * factor_afinidad
                    else:
                        minutos_afines_a_tiempo_bloque += avanzado_real * 0.30

                    if politica == "KRONO_AI":
                        pred_m1 = predecir_duracion_m1(t, bloque["bloque_horario"], (t["fecha_limite_min"] - min_fin) / 1440.0, modelo_app)
                        razones_h1.append(t["duracion_real_min"] / pred_m1)
                else:
                    minutos_afines_a_tiempo_bloque += avanzado_real * (factor_afinidad if inicio_bloque + offset_bloque <= t["fecha_limite_min"] else 0.30)

                desborde = tiempo_ejecutado - min_plan
                hay_posteriores = (idx_item < len(plan_bloque) - 1)

                if desborde >= 5.0 and (hay_posteriores or (espera_terminar and not termino)):
                    reordenamiento_en_bloque = True
                    # Costo de cambio de contexto / replanificación por desborde
                    offset_bloque += 8.0
                    penalizacion_desborde_bloque += desborde * 0.9 + 6.0

        if reordenamiento_en_bloque:
            reordenamientos_emergencia += 1

        minutos_bloques_utilizados_tot += dur_bloque
        minutos_afines_a_tiempo_tot += minutos_afines_a_tiempo_bloque
        penalizacion_desborde_tot += penalizacion_desborde_bloque

        idx_b += 1

    total_tareas = len(tareas)
    a_tiempo = sum(1 for t in estado_tareas.values() if t["entregada_a_tiempo"])
    entregas_a_tiempo_pct = (a_tiempo / total_tareas) * 100.0

    if minutos_bloques_utilizados_tot > 0:
        aprovechamiento_bloques_pct = max(
            0.0,
            min(
                100.0,
                ((minutos_afines_a_tiempo_tot - penalizacion_desborde_tot) / minutos_bloques_utilizados_tot) * 100.0,
            ),
        )
    else:
        aprovechamiento_bloques_pct = 0.0

    return {
        "entregas_a_tiempo_pct": entregas_a_tiempo_pct,
        "aprovechamiento_bloques_pct": aprovechamiento_bloques_pct,
        "reordenamientos_emergencia": reordenamientos_emergencia,
    }


# ---------------------------------------------------------------------------
# Gráfica de Resultados (3 Paneles)
# ---------------------------------------------------------------------------
def generar_grafica_desempeno(tabla_resumen, ruta_png):
    plt.rcParams["font.family"] = "sans-serif"
    fig, axes = plt.subplots(1, 3, figsize=(16, 5.5))

    nombres_cortos = [
        "1. FIFO\n(B0)",
        "2. EDF\n(B0)",
        "3. H2 Búsqueda\n(B0)",
        "4. KRONO-AI\n(H2 + H1)",
    ]
    colores = ["#64748b", "#0284c7", "#f59e0b", "#8a63d2"]

    # 1. Entregas a Tiempo
    ax1 = axes[0]
    vals1 = tabla_resumen["entregas_a_tiempo_pct"].values
    errs1 = tabla_resumen["entregas_std"].values
    bars1 = ax1.bar(nombres_cortos, vals1, yerr=errs1, capsize=4, color=colores, alpha=0.9, edgecolor="#1e293b", linewidth=1)
    ax1.set_title("Entregas a Tiempo (%)", fontsize=12, fontweight="bold", pad=12)
    ax1.set_ylabel("Porcentaje (%)", fontsize=10)
    ax1.set_ylim(0, 105)
    ax1.grid(axis="y", linestyle="--", alpha=0.4)
    for bar in bars1:
        yval = bar.get_height()
        ax1.text(bar.get_x() + bar.get_width() / 2.0, yval + 2.5, f"{yval:.1f}%", ha="center", va="bottom", fontsize=10, fontweight="bold")

    # 2. Eficiencia de Uso Productivo de Bloques
    ax2 = axes[1]
    vals2 = tabla_resumen["aprovechamiento_bloques_pct"].values
    errs2 = tabla_resumen["aprovechamiento_std"].values
    bars2 = ax2.bar(nombres_cortos, vals2, yerr=errs2, capsize=4, color=colores, alpha=0.9, edgecolor="#1e293b", linewidth=1)
    ax2.set_title("Eficiencia de Uso Productivo (%)", fontsize=12, fontweight="bold", pad=12)
    ax2.set_ylabel("Eficiencia efectiva (%)", fontsize=10)
    ax2.set_ylim(0, 105)
    ax2.grid(axis="y", linestyle="--", alpha=0.4)
    for bar in bars2:
        yval = bar.get_height()
        ax2.text(bar.get_x() + bar.get_width() / 2.0, yval + 2.5, f"{yval:.1f}%", ha="center", va="bottom", fontsize=10, fontweight="bold")

    # 3. Reordenamientos de Emergencia
    ax3 = axes[2]
    vals3 = tabla_resumen["reordenamientos_emergencia_prom"].values
    errs3 = tabla_resumen["reordenamientos_std"].values
    bars3 = ax3.bar(nombres_cortos, vals3, yerr=errs3, capsize=4, color=colores, alpha=0.9, edgecolor="#1e293b", linewidth=1)
    ax3.set_title("Reordenamientos de Emergencia", fontsize=12, fontweight="bold", pad=12)
    ax3.set_ylabel("Promedio por escenario (3 días)", fontsize=10)
    max_y3 = max(vals3 + errs3) if len(vals3) > 0 else 5.0
    ax3.set_ylim(0, max_y3 + 1.5)
    ax3.grid(axis="y", linestyle="--", alpha=0.4)
    for bar in bars3:
        yval = bar.get_height()
        ax3.text(bar.get_x() + bar.get_width() / 2.0, yval + 0.15, f"{yval:.2f}", ha="center", va="bottom", fontsize=10, fontweight="bold")

    fig.suptitle("H2 · Desempeño del Agente Racional en 60 Escenarios de 3 Días", fontsize=14, fontweight="bold", y=1.02)
    plt.tight_layout()
    plt.savefig(ruta_png, dpi=300, bbox_inches="tight")
    plt.close(fig)


# ---------------------------------------------------------------------------
# Ejecución Principal
# ---------------------------------------------------------------------------
def main():
    os.makedirs(SALIDA, exist_ok=True)
    tareas_df, bloques_df, modelo_app = cargar_datos()

    escenarios = tareas_df["escenario_id"].unique()
    politicas_cfg = [
        ("FIFO", "1. FIFO + Estimación Usuario (B0)"),
        ("EDF", "2. EDF (Fecha Límite) + Estimación Usuario (B0)"),
        ("H2_B0", "3. Búsqueda Informada H2 + Estimación Usuario (B0)"),
        ("KRONO_AI", "4. Agente KRONO-AI Completo (Búsqueda H2 + Predicción Híbrida H1)"),
    ]

    print("=" * 80)
    print(" KRONO-AI · Evaluación del Elemento de Desempeño / Búsqueda Informada (H2)")
    print("=" * 80)
    print(f"Escenarios cargados: {len(escenarios)} | Tareas: {len(tareas_df)} | Bloques: {len(bloques_df)}")
    print("Simulando las 4 políticas a lo largo de los 60 escenarios de 3 días...")

    filas = []
    for clave_p, nombre_largo in politicas_cfg:
        sys.stdout.write(f"  > Evaluando: {nombre_largo} ... ")
        sys.stdout.flush()
        resultados_p = [simular_politica_escenario(e, clave_p, tareas_df, bloques_df, modelo_app) for e in escenarios]
        df_p = pd.DataFrame(resultados_p)

        filas.append({
            "politica": nombre_largo,
            "entregas_a_tiempo_pct": round(df_p["entregas_a_tiempo_pct"].mean(), 1),
            "entregas_std": round(df_p["entregas_a_tiempo_pct"].std(), 1),
            "aprovechamiento_bloques_pct": round(df_p["aprovechamiento_bloques_pct"].mean(), 1),
            "aprovechamiento_std": round(df_p["aprovechamiento_bloques_pct"].std(), 1),
            "reordenamientos_emergencia_prom": round(df_p["reordenamientos_emergencia"].mean(), 2),
            "reordenamientos_std": round(df_p["reordenamientos_emergencia"].std(), 2),
        })
        print("Completado.")

    tabla_h2 = pd.DataFrame(filas)
    csv_salida = os.path.join(SALIDA, "metricas_agente_h2.csv")
    tabla_h2.to_csv(csv_salida, index=False)
    print(f"\n[OK] Métricas H2 guardadas en: {csv_salida}")

    png_salida = os.path.join(SALIDA, "desempeno_agente_h2.png")
    generar_grafica_desempeno(tabla_h2, png_salida)
    print(f"[OK] Gráfica comparativa guardada en: {png_salida}")

    # Impresión en consola de las dos tablas requeridas
    print("\n" + "=" * 90)
    print(" TABLA 1: MÉTRICAS DEL ELEMENTO DE APRENDIZAJE H1 (resultados/metricas.csv)")
    print("=" * 90)
    csv_h1 = os.path.join(SALIDA, "metricas.csv")
    if os.path.exists(csv_h1):
        df_h1 = pd.read_csv(csv_h1)
        pd.set_option("display.width", 140)
        print(df_h1.to_string(index=False))
    else:
        print("(Archivo metricas.csv no encontrado)")

    print("\n" + "=" * 90)
    print(" TABLA 2: MÉTRICAS DEL ELEMENTO DE DESEMPEÑO H2 (resultados/metricas_agente_h2.csv)")
    print("=" * 90)
    pd.set_option("display.width", 140)
    print(tabla_h2.to_string(index=False))
    print("=" * 90 + "\n")


if __name__ == "__main__":
    main()
