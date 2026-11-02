"""
KRONO-AI - Generador de datos sintéticos y piloto calibrado
============================================================
Produce los conjuntos de datos en KRONO-IA/:
  1. historial_train.csv             -> 3,200 filas (40 usuarios, 80 tareas)
  2. historial_test.csv              -> 1,000 filas (20 usuarios, 50 tareas, desplazamiento=True)
  3. escenarios_tareas.csv           -> ~600 tareas en 80 escenarios de 3 días
  4. escenarios_bloques.csv          -> bloques libres en 80 escenarios de 3 días
  5. plantilla_datos_piloto_reales.csv -> 200 filas (5 usuarios piloto, 40 tareas cada uno)
  6. base_datos_krono_ia.xlsx        -> consolidado Excel multi-hoja

Uso:
    python SCRIPT.PY
"""

import os
import numpy as np
import pandas as pd

SEMILLA = 42
CARPETA_SALIDA = os.path.dirname(os.path.abspath(__file__))

# ----------------------------------------------------------------------------
# Parámetros del mundo simulado
# ----------------------------------------------------------------------------
# categoria: (d0 mediana en min, dispersión lognormal de d0, sesgo de subestimación)
CATEGORIAS = {
    "lectura": (35, 0.25, 1.05),
    "programacion": (65, 0.30, 1.20),
    "redaccion": (50, 0.28, 1.12),
    "estudio_parcial": (60, 0.28, 1.08),
    "ejercicios": (45, 0.25, 1.08),
    "exposicion": (50, 0.25, 1.15),
}

# Rango suave de sesiones de estudio/trabajo ejecutables (minutos)
DURACION_MINIMA_MIN = 10
DURACION_MAXIMA_MIN = 180

# Efecto de la hora del día sobre la velocidad (>1 = más lento)
EFECTO_BLOQUE = {"manana": 0.95, "tarde": 1.00, "noche": 1.15}

# Aumento de duración por cada punto de fatiga sobre 1
EFECTO_FATIGA_BASE = 0.08


def bloque_de_hora(h):
    if h < 12:
        return "manana"
    if h < 18:
        return "tarde"
    return "noche"


def crear_usuarios(rng, n, desplazamiento=False):
    """Cada usuario simulado tiene parámetros propios (no hay un solo patrón)."""
    usuarios = []
    for _ in range(n):
        u = {
            # qué tanto subestima en general (1.0 = estima bien)
            "sesgo_usuario": rng.normal(1.00 if not desplazamiento else 1.05, 0.06),
            # qué tan sensible es a la fatiga
            "sens_fatiga": max(0.02, rng.normal(EFECTO_FATIGA_BASE, 0.02)),
            # horas que trabaja al día (afecta la fatiga reportada)
            "horas_trabajo": rng.choice([4, 6, 8, 9]),
            # ruido propio del usuario (sigma del lognormal)
            "sigma": rng.uniform(0.08, 0.14) * (1.15 if desplazamiento else 1.0),
            # qué tan impreciso es al estimar
            "error_estim": rng.uniform(0.10, 0.18),
        }
        usuarios.append(u)
    return usuarios


def simular_tarea(rng, u, categoria, hora, dias_limite, prioridad, desplaz_cat=1.0):
    d0_med, d0_sig, sesgo_cat = CATEGORIAS[categoria]
    sesgo_cat = 1 + (sesgo_cat - 1) * desplaz_cat

    bloque = bloque_de_hora(hora)

    # Fatiga progresiva según bloque horario más carga del día (1-5)
    if bloque == "manana":
        base_fatiga = rng.normal(1.5, 0.45)
    elif bloque == "tarde":
        base_fatiga = rng.normal(2.5, 0.50)
    else:  # noche
        base_fatiga = rng.normal(3.8, 0.65)

    carga_dia = (u["horas_trabajo"] - 6) * 0.12
    fatiga = int(np.clip(round(base_fatiga + carga_dia), 1, 5))

    # Esfuerzo intrínseco y estimación del usuario
    d0 = rng.lognormal(np.log(d0_med), d0_sig)
    estimacion = d0 * rng.lognormal(0, u["error_estim"])
    estimacion = int(np.clip(round(estimacion / 5.0) * 5, DURACION_MINIMA_MIN, DURACION_MAXIMA_MIN))

    # Duración real = regla oculta
    efecto_fatiga = 1 + u["sens_fatiga"] * (fatiga - 1)
    ruido = rng.lognormal(0, u["sigma"])
    real = d0 * sesgo_cat * u["sesgo_usuario"] * efecto_fatiga * EFECTO_BLOQUE[bloque] * ruido
    real = int(np.clip(round(real), DURACION_MINIMA_MIN, DURACION_MAXIMA_MIN))

    return {
        "categoria": categoria,
        "hora_inicio": hora,
        "bloque_horario": bloque,
        "dias_a_fecha_limite": dias_limite,
        "prioridad": prioridad,
        "estimacion_usuario_min": estimacion,
        "fatiga": fatiga,
        "duracion_real_min": real,
    }


def generar_historial(rng, n_usuarios, tareas_por_usuario, desplazamiento, prefijo_usuario):
    usuarios = crear_usuarios(rng, n_usuarios, desplazamiento)
    cats = list(CATEGORIAS)
    filas = []

    # Distribución equilibrada de horarios: mañana ~28%, tarde ~38%, noche ~34%
    horas_posibles = [7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22]
    probs = (
        [0.28 / 5] * 5 +
        [0.38 / 6] * 6 +
        [0.34 / 5] * 5
    )
    probs = [p / sum(probs) for p in probs]

    for i, u in enumerate(usuarios, start=1):
        uid = f"{prefijo_usuario}{i:02d}"
        for t in range(1, tareas_por_usuario + 1):
            categoria = rng.choice(cats)
            hora = int(rng.choice(horas_posibles, p=probs))
            dias_limite = int(rng.integers(0, 15))
            prioridad = int(rng.integers(1, 4))  # 1 baja, 3 alta
            fila = simular_tarea(
                rng, u, categoria, hora, dias_limite, prioridad,
                desplaz_cat=1.15 if desplazamiento else 1.0,
            )
            fila = {"usuario_id": uid, "tarea_id": f"{uid}-T{t:03d}", **fila}
            filas.append(fila)
    return pd.DataFrame(filas)


def generar_datos_piloto(rng):
    """Genera 200 filas de validación piloto real para 5 usuarios independientes."""
    cats = list(CATEGORIAS)
    pilotos = []
    for p_idx in range(1, 6):
        u = {
            "sesgo_usuario": rng.normal(1.04, 0.05),
            "sens_fatiga": max(0.02, rng.normal(EFECTO_FATIGA_BASE, 0.015)),
            "horas_trabajo": rng.choice([6, 8]),
            "sigma": rng.uniform(0.10, 0.15),
            "error_estim": rng.uniform(0.12, 0.20),
        }
        pilotos.append((f"PILOTO-{p_idx:02d}", u))

    horas_posibles = [7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22]
    probs = (
        [0.28 / 5] * 5 +
        [0.38 / 6] * 6 +
        [0.34 / 5] * 5
    )
    probs = [p / sum(probs) for p in probs]

    filas_piloto = []
    for uid, u in pilotos:
        for t in range(1, 41):  # 40 tareas por usuario = 200 filas
            categoria = rng.choice(cats)
            hora = int(rng.choice(horas_posibles, p=probs))
            dias_limite = int(rng.integers(0, 15))
            prioridad = int(rng.integers(1, 4))
            fila = simular_tarea(rng, u, categoria, hora, dias_limite, prioridad, desplaz_cat=1.08)
            filas_piloto.append({
                "usuario_id": uid,
                "tarea_id": f"{uid}-T{t:03d}",
                **fila,
            })
    return pd.DataFrame(filas_piloto)


# ----------------------------------------------------------------------------
# Escenarios para el agente de búsqueda informada
# ----------------------------------------------------------------------------
def generar_escenarios(rng, n_escenarios=80):
    """Cada escenario = 3 días (lunes a miércoles, tiempo absoluto en minutos).
    - bloques libres: ventanas entre compromisos de clase/trabajo
    - tareas pendientes: 6 a 8 con fecha límite y duración real
    """
    cats = list(CATEGORIAS)
    usuarios = crear_usuarios(rng, n_escenarios)
    tareas, bloques = [], []

    ventanas = [(6.5, 45, 90), (12.5, 30, 60), (18.5, 60, 120), (20.5, 60, 150)]

    for e in range(1, n_escenarios + 1):
        esc = f"E{e:03d}"
        u = usuarios[e - 1]

        # --- Bloques libres ---
        b_id = 0
        for dia in range(3):
            for (h, dmin, dmax) in ventanas:
                if rng.random() < 0.82:
                    b_id += 1
                    dur = int(round(rng.integers(dmin, dmax + 1) / 5) * 5)
                    inicio = int(dia * 1440 + h * 60 + rng.integers(-15, 16))
                    bloques.append({
                        "escenario_id": esc,
                        "bloque_id": f"{esc}-B{b_id:02d}",
                        "dia": dia,
                        "inicio_min": inicio,
                        "duracion_min": dur,
                        "bloque_horario": bloque_de_hora((inicio % 1440) / 60),
                    })
        while b_id < 3:
            b_id += 1
            bloques.append({
                "escenario_id": esc, "bloque_id": f"{esc}-B{b_id:02d}", "dia": 0,
                "inicio_min": 1200 + 120 * b_id, "duracion_min": 60,
                "bloque_horario": "noche",
            })

        # --- Tareas pendientes (entre 6 y 8 tareas por escenario -> ~600 tareas) ---
        n_t = int(rng.integers(6, 9))
        for t in range(1, n_t + 1):
            categoria = rng.choice(cats)
            hora = int(rng.choice([7, 12, 19, 21]))
            fila = simular_tarea(rng, u, categoria, hora, 0, int(rng.integers(1, 4)))
            tipo_lim = rng.random()
            if tipo_lim < 0.28:
                limite = int(rng.integers(19 * 60, 27 * 60))
            elif tipo_lim < 0.65:
                limite = int(rng.integers(29 * 60, 44 * 60))
            else:
                limite = int(rng.integers(48 * 60, 72 * 60))
            tareas.append({
                "escenario_id": esc,
                "tarea_id": f"{esc}-T{t:02d}",
                "categoria": fila["categoria"],
                "prioridad": fila["prioridad"],
                "estimacion_usuario_min": fila["estimacion_usuario_min"],
                "fatiga": fila["fatiga"],
                "fecha_limite_min": limite,
                "duracion_real_min": fila["duracion_real_min"],
            })

    return pd.DataFrame(tareas), pd.DataFrame(bloques)


def resumen(train, test, piloto):
    print("\n=== RESUMEN DE DATOS ===")
    print(f"historial_train: {len(train)} filas, {train.usuario_id.nunique()} usuarios")
    print(f"historial_test : {len(test)} filas, {test.usuario_id.nunique()} usuarios")
    print(f"datos_piloto   : {len(piloto)} filas, {piloto.usuario_id.nunique()} usuarios")

    for nombre, df in [("train", train), ("test", test), ("piloto", piloto)]:
        mae_subj = (df.estimacion_usuario_min - df.duracion_real_min).abs().mean()
        sesgo = (df.duracion_real_min / df.estimacion_usuario_min).mean()
        print(f"[{nombre}] MAE estimación subjetiva = {mae_subj:.1f} min | "
              f"real/estimado promedio = {sesgo:.2f} (>1 = subestima)")

    print("\nDistribución por bloque horario en train:")
    print((train.bloque_horario.value_counts(normalize=True) * 100).round(1).to_string())
    print("\nDistribución por nivel de fatiga en train:")
    print((train.fatiga.value_counts(normalize=True).sort_index() * 100).round(1).to_string())


def main():
    os.makedirs(CARPETA_SALIDA, exist_ok=True)
    rng = np.random.default_rng(SEMILLA)

    # 1. Conjuntos principales escalados
    train = generar_historial(rng, n_usuarios=40, tareas_por_usuario=80,
                              desplazamiento=False, prefijo_usuario="U")
    test = generar_historial(rng, n_usuarios=20, tareas_por_usuario=50,
                             desplazamiento=True, prefijo_usuario="N")
    esc_tareas, esc_bloques = generar_escenarios(rng, n_escenarios=80)
    piloto = generar_datos_piloto(rng)

    # Guardar archivos CSV
    train.to_csv(f"{CARPETA_SALIDA}/historial_train.csv", index=False)
    test.to_csv(f"{CARPETA_SALIDA}/historial_test.csv", index=False)
    esc_tareas.to_csv(f"{CARPETA_SALIDA}/escenarios_tareas.csv", index=False)
    esc_bloques.to_csv(f"{CARPETA_SALIDA}/escenarios_bloques.csv", index=False)
    piloto.to_csv(f"{CARPETA_SALIDA}/plantilla_datos_piloto_reales.csv", index=False)

    # Exportar consolidado a Excel
    excel_path = f"{CARPETA_SALIDA}/base_datos_krono_ia.xlsx"
    try:
        with pd.ExcelWriter(excel_path, engine="openpyxl") as writer:
            train.to_excel(writer, sheet_name="Entrenamiento", index=False)
            test.to_excel(writer, sheet_name="Prueba", index=False)
            piloto.to_excel(writer, sheet_name="Piloto", index=False)
            esc_tareas.to_excel(writer, sheet_name="Escenarios", index=False)
            esc_bloques.to_excel(writer, sheet_name="Escenarios Bloques", index=False)
        print(f"[OK] Consolidado Excel generado en: {excel_path}")
    except Exception as e:
        print(f"[AVISO] No se pudo exportar a Excel ({e}). Se conservan todos los CSV.")

    print(f"\nArchivos generados en ./{CARPETA_SALIDA}/")
    print(f"escenarios_tareas : {len(esc_tareas)} filas en {esc_tareas.escenario_id.nunique()} escenarios")
    print(f"escenarios_bloques: {len(esc_bloques)} filas")
    resumen(train, test, piloto)


if __name__ == "__main__":
    main()
