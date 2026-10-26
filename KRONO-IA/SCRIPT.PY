"""
KRONO-AI - Generador de datos sintéticos
========================================
Produce 4 archivos CSV en la carpeta ./datos:

  1. historial_train.csv      -> entrenamiento del modelo de regresión (H1)
  2. historial_test.csv       -> prueba (usuarios NUEVOS, más ruido, sesgos distintos)
  3. escenarios_tareas.csv    -> tareas pendientes por escenario (para el agente, H2)
  4. escenarios_bloques.csv   -> bloques libres por escenario (para el agente, H2)

Uso:
    python generar_datos.py

Todo es reproducible gracias a la semilla fija (SEMILLA).

REGLA OCULTA (la "verdad" que el modelo debe descubrir):
    real = d0 * sesgo_categoria * sesgo_usuario * efecto_fatiga * efecto_hora * ruido
donde d0 es el esfuerzo intrínseco de la tarea y el usuario estima
    estimacion_usuario = d0 * error_de_estimacion  (redondeado a múltiplos de 5 min)
Esto reproduce la falacia de planificación (el usuario subestima, sobre todo
en ciertas categorías) y la fatiga (a más fatiga, más tarda).

LIMITACIÓN (decirla en el informe): el modelo "descubrirá" lo que está escrito
aquí. Con datos sintéticos H1 valida el pipeline, no el mundo real.
"""

import os
import numpy as np
import pandas as pd

SEMILLA = 42
CARPETA_SALIDA = "datos"

# ----------------------------------------------------------------------------
# Parámetros del mundo simulado (AJÚSTENLOS y documenten los cambios)
# ----------------------------------------------------------------------------
# categoria: (d0 mediana en min, dispersión lognormal de d0, sesgo de subestimación)
# sesgo > 1 => la tarea tarda MÁS de lo que "debería" -> el usuario subestima
CATEGORIAS = {
    "lectura":         (45, 0.45, 1.00),
    "programacion":    (120, 0.50, 1.20),
    "redaccion":       (90, 0.45, 1.12),
    "estudio_parcial": (100, 0.40, 1.05),
    "ejercicios":      (60, 0.40, 1.08),
    "exposicion":      (75, 0.40, 1.15),
}

# Efecto de la hora del día sobre la velocidad (>1 = más lento)
EFECTO_BLOQUE = {"manana": 0.95, "tarde": 1.00, "noche": 1.15}

# Aumento de duración por cada punto de fatiga sobre 1 (promedio poblacional)
EFECTO_FATIGA_BASE = 0.08


def bloque_de_hora(h):
    if h < 12:
        return "manana"
    if h < 18:
        return "tarde"
    return "noche"


def crear_usuarios(rng, n, desplazamiento=False):
    """Cada usuario simulado tiene parámetros propios (no hay un solo patrón).
    Con desplazamiento=True los parámetros se mueven (prueba más difícil)."""
    usuarios = []
    for _ in range(n):
        u = {
            # qué tanto subestima en general (1.0 = estima bien)
            "sesgo_usuario": rng.normal(1.00 if not desplazamiento else 1.06, 0.08),
            # qué tan sensible es a la fatiga
            "sens_fatiga": max(0.02, rng.normal(EFECTO_FATIGA_BASE, 0.025)),
            # horas que trabaja al día (afecta la fatiga reportada)
            "horas_trabajo": rng.choice([4, 6, 8, 9]),
            # ruido propio del usuario (sigma del lognormal)
            "sigma": rng.uniform(0.12, 0.22) * (1.5 if desplazamiento else 1.0),
            # qué tan impreciso es al estimar
            "error_estim": rng.uniform(0.15, 0.30),
        }
        usuarios.append(u)
    return usuarios


def simular_tarea(rng, u, categoria, hora, dias_limite, prioridad, desplaz_cat=1.0):
    d0_med, d0_sig, sesgo_cat = CATEGORIAS[categoria]
    sesgo_cat = 1 + (sesgo_cat - 1) * desplaz_cat

    bloque = bloque_de_hora(hora)

    # Fatiga reportada (1-5): crece con horas trabajadas y con la hora del día
    fatiga_latente = (
        1.0
        + u["horas_trabajo"] / 4.0
        + (hora - 8) / 8.0
        + rng.normal(0, 0.6)
    )
    fatiga = int(np.clip(round(fatiga_latente), 1, 5))

    # Esfuerzo intrínseco y estimación del usuario
    d0 = rng.lognormal(np.log(d0_med), d0_sig)
    estimacion = d0 * rng.lognormal(0, u["error_estim"])
    estimacion = max(5, int(round(estimacion / 5.0)) * 5)

    # Duración real = regla oculta
    efecto_fatiga = 1 + u["sens_fatiga"] * (fatiga - 1)
    ruido = rng.lognormal(0, u["sigma"])
    real = d0 * sesgo_cat * u["sesgo_usuario"] * efecto_fatiga * EFECTO_BLOQUE[bloque] * ruido
    real = max(5, int(round(real)))

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
    for i, u in enumerate(usuarios, start=1):
        uid = f"{prefijo_usuario}{i:02d}"
        for t in range(1, tareas_por_usuario + 1):
            categoria = rng.choice(cats)
            # Estudiante trabajador: horas de estudio concentradas en tarde-noche
            hora = int(rng.choice([6, 7, 12, 13, 18, 19, 20, 21, 22], p=[
                0.05, 0.05, 0.08, 0.07, 0.15, 0.20, 0.20, 0.15, 0.05]))
            dias_limite = int(rng.integers(0, 15))
            prioridad = int(rng.integers(1, 4))  # 1 baja, 3 alta
            fila = simular_tarea(
                rng, u, categoria, hora, dias_limite, prioridad,
                desplaz_cat=1.15 if desplazamiento else 1.0,
            )
            fila = {"usuario_id": uid, "tarea_id": f"{uid}-T{t:03d}", **fila}
            filas.append(fila)
    return pd.DataFrame(filas)


# ----------------------------------------------------------------------------
# Escenarios para el agente de búsqueda informada
# ----------------------------------------------------------------------------
def generar_escenarios(rng, n_escenarios=60):
    """Cada escenario = 3 días (lunes a miércoles, tiempo absoluto en minutos).
    - bloques libres: ventanas entre compromisos de clase/trabajo
    - tareas pendientes: 4 a 8, con fecha límite y duración real (oculta al agente)
    Los tiempos son minutos desde el inicio del día 0 a las 00:00.
    """
    cats = list(CATEGORIAS)
    usuarios = crear_usuarios(rng, n_escenarios)
    tareas, bloques = [], []

    # ventanas candidatas por día: (hora inicio, minutos mínimo, minutos máximo)
    ventanas = [(6.0, 30, 90), (12.0, 30, 60), (18.5, 45, 120), (20.5, 60, 150)]

    for e in range(1, n_escenarios + 1):
        esc = f"E{e:03d}"
        u = usuarios[e - 1]

        # --- Bloques libres ---
        b_id = 0
        for dia in range(3):
            for (h, dmin, dmax) in ventanas:
                if rng.random() < 0.75:  # no todos los días hay todas las ventanas
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
        # asegura al menos 3 bloques
        while b_id < 3:
            b_id += 1
            bloques.append({
                "escenario_id": esc, "bloque_id": f"{esc}-B{b_id:02d}", "dia": 0,
                "inicio_min": 1200 + 120 * b_id, "duracion_min": 60,
                "bloque_horario": "noche",
            })

        # --- Tareas pendientes ---
        n_t = int(rng.integers(4, 9))
        for t in range(1, n_t + 1):
            categoria = rng.choice(cats)
            hora = int(rng.choice([7, 12, 19, 21]))
            fila = simular_tarea(rng, u, categoria, hora, 0, int(rng.integers(1, 4)))
            # fecha límite absoluta: entre 0.5 y 3 días desde el inicio del escenario
            limite = int(rng.integers(12 * 60, 3 * 1440 + 1))
            tareas.append({
                "escenario_id": esc,
                "tarea_id": f"{esc}-T{t:02d}",
                "categoria": fila["categoria"],
                "prioridad": fila["prioridad"],
                "estimacion_usuario_min": fila["estimacion_usuario_min"],
                "fatiga": fila["fatiga"],
                "fecha_limite_min": limite,
                "duracion_real_min": fila["duracion_real_min"],  # solo para simular la ejecución
            })

    return pd.DataFrame(tareas), pd.DataFrame(bloques)


# ----------------------------------------------------------------------------
# Verificación rápida (sanidad)
# ----------------------------------------------------------------------------
def resumen(train, test):
    print("\n=== RESUMEN ===")
    print(f"historial_train: {len(train)} filas, {train.usuario_id.nunique()} usuarios")
    print(f"historial_test : {len(test)} filas, {test.usuario_id.nunique()} usuarios")

    for nombre, df in [("train", train), ("test", test)]:
        mae_subj = (df.estimacion_usuario_min - df.duracion_real_min).abs().mean()
        sesgo = (df.duracion_real_min / df.estimacion_usuario_min).mean()
        print(f"[{nombre}] MAE estimación subjetiva = {mae_subj:.1f} min | "
              f"real/estimado promedio = {sesgo:.2f} (>1 = subestima)")

    # Prueba opcional con un bosque aleatorio, si scikit-learn está instalado
    try:
        from sklearn.ensemble import RandomForestRegressor
        feats = ["categoria", "bloque_horario", "dias_a_fecha_limite",
                 "estimacion_usuario_min", "fatiga", "prioridad"]
        Xtr = pd.get_dummies(train[feats])
        Xte = pd.get_dummies(test[feats]).reindex(columns=Xtr.columns, fill_value=0)
        rf = RandomForestRegressor(n_estimators=200, random_state=SEMILLA)
        rf.fit(Xtr, train.duracion_real_min)
        mae_rf = (rf.predict(Xte) - test.duracion_real_min).abs().mean()
        mae_subj = (test.estimacion_usuario_min - test.duracion_real_min).abs().mean()
        mae_cat = None
        prom_cat = train.groupby("categoria").duracion_real_min.mean()
        mae_cat = (test.categoria.map(prom_cat) - test.duracion_real_min).abs().mean()
        print(f"[test] MAE bosque aleatorio = {mae_rf:.1f} min "
              f"(mejora {100*(1-mae_rf/mae_subj):.0f}% vs subjetiva; "
              f"promedio por categoría = {mae_cat:.1f} min)")
    except ImportError:
        print("(scikit-learn no instalado: omito la prueba del modelo. pip install scikit-learn)")


def main():
    os.makedirs(CARPETA_SALIDA, exist_ok=True)
    rng = np.random.default_rng(SEMILLA)

    train = generar_historial(rng, n_usuarios=10, tareas_por_usuario=80,
                              desplazamiento=False, prefijo_usuario="U")
    test = generar_historial(rng, n_usuarios=4, tareas_por_usuario=60,
                             desplazamiento=True, prefijo_usuario="N")  # usuarios nuevos
    esc_tareas, esc_bloques = generar_escenarios(rng, n_escenarios=60)

    train.to_csv(f"{CARPETA_SALIDA}/historial_train.csv", index=False)
    test.to_csv(f"{CARPETA_SALIDA}/historial_test.csv", index=False)
    esc_tareas.to_csv(f"{CARPETA_SALIDA}/escenarios_tareas.csv", index=False)
    esc_bloques.to_csv(f"{CARPETA_SALIDA}/escenarios_bloques.csv", index=False)

    # Plantilla para los datos PILOTO REALES (llenar en Google Sheets / Forms)
    plantilla = pd.DataFrame([{
        "usuario_id": "REAL01", "tarea_id": "REAL01-T001", "categoria": "lectura",
        "hora_inicio": 20, "bloque_horario": "noche", "dias_a_fecha_limite": 3,
        "prioridad": 2, "estimacion_usuario_min": 45, "fatiga": 3,
        "duracion_real_min": 60,
    }])
    plantilla.to_csv(f"{CARPETA_SALIDA}/plantilla_datos_piloto_reales.csv", index=False)

    print(f"Archivos generados en ./{CARPETA_SALIDA}/")
    print(f"escenarios_tareas : {len(esc_tareas)} filas en {esc_tareas.escenario_id.nunique()} escenarios")
    print(f"escenarios_bloques: {len(esc_bloques)} filas")
    resumen(train, test)


if __name__ == "__main__":
    main()
