"""
KRONO-AI - H1: modelo que predice la duración real de una tarea
=================================================================
Entrena con historial_train.csv y evalúa con historial_test.csv (usuarios NUEVOS,
con más ruido y sesgos distintos: mide si el modelo generaliza).

Compara cinco estimadores, de menor a mayor "inteligencia":

  B0  Estimación del usuario          lo que el estudiante cree que tardará
  B1  Estimación x factor global      corrige la falacia de planificación promedio
  B2  Regla actual de la app (F-11)   promedio de las últimas 5 tareas del mismo
                                      usuario y categoría (mín. 3); si no, B0
  M1  Regresión lineal log-log        modelo exportable a la app (modelo_app.json)
  M2  Bosque aleatorio                modelo no lineal, de referencia
  H   Agente híbrido (M1 + usuario)   M1 corregido con el sesgo personal que el
                                      agente aprende tarea a tarea (arranque en frío)

Y tres conjuntos de variables, según lo que la app puede conocer:

  APP       estimación, bloque horario, días a la fecha límite, prioridad
            (datos que la app YA tiene al crear una tarea)
  +SUBCAT   APP + subcategoría académica (lectura, programación...)
            (la app tendría que agregar ese campo)
  COMPLETO  +SUBCAT + fatiga (la app tendría que preguntarla)

Salidas en ./resultados:
  metricas.csv, metricas_por_categoria.csv, metricas_piloto.csv, modelo_app.json y gráficas .png

Uso (desde la carpeta KRONO-IA):
    python entrenar_modelo.py
"""

import json
import os

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor
from sklearn.linear_model import Ridge
from sklearn.metrics import mean_absolute_error, r2_score

SEMILLA = 42
CARPETA = os.path.dirname(os.path.abspath(__file__))
SALIDA = os.path.join(CARPETA, "resultados")

# Regla F-11 de la app (EstimateTaskDurationUseCase): mismos parámetros.
F11_MINIMO = 3
F11_VENTANA = 5
F11_REDONDEO = 5

# Agente híbrido: cuánto confía en el sesgo personal tras n tareas: n / (n + K).
HIBRIDO_K = 2
HIBRIDO_VENTANA = 15

# Tolerancia para contar una predicción como "acertada".
TOLERANCIA = 0.20

CONJUNTOS = {
    "APP": {"num": ["dias_a_fecha_limite", "prioridad"], "cat": ["bloque_horario"]},
    "+SUBCAT": {"num": ["dias_a_fecha_limite", "prioridad"], "cat": ["bloque_horario", "categoria"]},
    "COMPLETO": {"num": ["dias_a_fecha_limite", "prioridad", "fatiga"], "cat": ["bloque_horario", "categoria"]},
}


# ---------------------------------------------------------------------------
# Datos
# ---------------------------------------------------------------------------
def cargar():
    train = pd.read_csv(os.path.join(CARPETA, "historial_train.csv"))
    test = pd.read_csv(os.path.join(CARPETA, "historial_test.csv"))
    return train, test


def matriz(df, conjunto, columnas=None):
    """Variables del conjunto. La estimación entra en logaritmo: la duración real
    es multiplicativa respecto a ella (real ≈ estimación × sesgos)."""
    cfg = CONJUNTOS[conjunto]
    X = df[cfg["num"]].astype(float).copy()
    X["log_estimacion"] = np.log(df["estimacion_usuario_min"])
    X = pd.concat([X, pd.get_dummies(df[cfg["cat"]], dtype=float)], axis=1)
    if columnas is not None:
        X = X.reindex(columns=columnas, fill_value=0.0)
    return X


# ---------------------------------------------------------------------------
# Estimadores
# ---------------------------------------------------------------------------
def b0_usuario(train, test):
    return test["estimacion_usuario_min"].to_numpy(dtype=float)


def b1_factor_global(train, test):
    factor = (train["duracion_real_min"] / train["estimacion_usuario_min"]).median()
    return test["estimacion_usuario_min"].to_numpy(dtype=float) * factor, factor


def b2_regla_app(test):
    """Simula la app en orden: para cada tarea solo conoce las anteriores del usuario."""
    pred = np.empty(len(test))
    for usuario, grupo in test.groupby("usuario_id", sort=False):
        vistas = []
        for idx, fila in grupo.iterrows():
            previas = [r for c, r in vistas if c == fila["categoria"]][-F11_VENTANA:]
            if len(previas) >= F11_MINIMO:
                valor = max(F11_REDONDEO, round(np.mean(previas) / F11_REDONDEO) * F11_REDONDEO)
            else:
                valor = fila["estimacion_usuario_min"]
            pred[test.index.get_loc(idx)] = valor
            vistas.append((fila["categoria"], fila["duracion_real_min"]))
    return pred


def m1_lineal(train, test, conjunto):
    Xtr = matriz(train, conjunto)
    Xte = matriz(test, conjunto, Xtr.columns)
    modelo = Ridge(alpha=1.0).fit(Xtr, np.log(train["duracion_real_min"]))
    return np.exp(modelo.predict(Xte)), modelo, list(Xtr.columns)


def m2_bosque(train, test, conjunto):
    Xtr = matriz(train, conjunto)
    Xte = matriz(test, conjunto, Xtr.columns)
    modelo = RandomForestRegressor(n_estimators=300, min_samples_leaf=3, random_state=SEMILLA)
    modelo.fit(Xtr, np.log(train["duracion_real_min"]))
    return np.exp(modelo.predict(Xte)), modelo, list(Xtr.columns)


def hibrido(test, pred_modelo):
    """Agente que aprende de cada usuario: multiplica la predicción del modelo por el
    sesgo personal observado (real/predicho) en sus últimas tareas, con más peso
    cuantas más tareas haya medido. Un usuario nuevo recibe la predicción del modelo."""
    pred = np.empty(len(test))
    for _, grupo in test.groupby("usuario_id", sort=False):
        razones = []
        for idx, fila in grupo.iterrows():
            i = test.index.get_loc(idx)
            recientes = razones[-HIBRIDO_VENTANA:]
            if recientes:
                n = len(recientes)
                sesgo = float(np.exp(np.mean(np.log(recientes))))
                peso = n / (n + HIBRIDO_K)
                factor = 1 + (sesgo - 1) * peso
            else:
                factor = 1.0
            pred[i] = pred_modelo[i] * factor
            razones.append(fila["duracion_real_min"] / pred_modelo[i])
    return pred


# ---------------------------------------------------------------------------
# Métricas
# ---------------------------------------------------------------------------
def metricas(nombre, conjunto, real, pred):
    error = pred - real
    return {
        "estimador": nombre,
        "variables": conjunto,
        "MAE_min": round(mean_absolute_error(real, pred), 1),
        "MAPE_pct": round(float(np.mean(np.abs(error) / real)) * 100, 1),
        "R2": round(r2_score(real, pred), 3),
        "aciertos_20pct": round(float(np.mean(np.abs(error) / real <= TOLERANCIA)) * 100, 1),
        "sesgo_medio_min": round(float(np.mean(error)), 1),
    }


# ---------------------------------------------------------------------------
# Exportación del modelo para la app
# ---------------------------------------------------------------------------
def exportar_modelo_app(modelo, columnas, factor_global, ruta):
    """Coeficientes de M1 (APP) para replicarlo en Kotlin:
    log(real) = intercepto + Σ coef_i · x_i, con x = log(estimación), días, prioridad
    y una variable 0/1 por bloque horario."""
    datos = {
        "descripcion": "Regresión lineal log-log (Ridge) entrenada con historial_train.csv",
        "formula": "duracion_min = exp(intercepto + sum(coeficientes[x] * valor_x))",
        "variables": {
            "log_estimacion": "ln(estimación del usuario en minutos)",
            "dias_a_fecha_limite": "días entre el inicio y la fecha límite",
            "prioridad": "1 baja, 2 media, 3 alta",
            "bloque_horario_*": "1 si la tarea empieza en ese bloque (manana < 12 h, tarde < 18 h, noche)",
        },
        "intercepto": round(float(modelo.intercept_), 6),
        "coeficientes": {c: round(float(v), 6) for c, v in zip(columnas, modelo.coef_)},
        "factor_global_b1": round(float(factor_global), 4),
        "hibrido": {"K": HIBRIDO_K, "ventana": HIBRIDO_VENTANA},
    }
    with open(ruta, "w", encoding="utf-8") as f:
        json.dump(datos, f, ensure_ascii=False, indent=2)


# ---------------------------------------------------------------------------
# Gráficas
# ---------------------------------------------------------------------------
def grafica_mae(tabla, ruta):
    datos = tabla.sort_values("MAE_min", ascending=False)
    etiquetas = datos["estimador"] + " · " + datos["variables"]
    plt.figure(figsize=(9, 5))
    plt.barh(etiquetas, datos["MAE_min"], color="#8a63d2")
    plt.xlabel("Error absoluto medio en usuarios nuevos (minutos)")
    plt.title("H1 · ¿Qué estimador predice mejor la duración real?")
    plt.tight_layout()
    plt.savefig(ruta, dpi=150)
    plt.close()


def grafica_dispersion(real, predicciones, ruta):
    fig, ejes = plt.subplots(1, len(predicciones), figsize=(5 * len(predicciones), 4.5), sharey=True)
    tope = max(real.max(), *(p.max() for p in predicciones.values()))
    for eje, (nombre, pred) in zip(np.atleast_1d(ejes), predicciones.items()):
        eje.scatter(real, pred, alpha=0.3, s=15, color="#8a63d2")
        eje.plot([0, tope], [0, tope], "k--", lw=1)
        eje.set_title(nombre)
        eje.set_xlabel("Duración real (min)")
        eje.set_xlim(0, tope)
        eje.set_ylim(0, tope)
    ejes[0].set_ylabel("Predicción (min)")
    plt.tight_layout()
    plt.savefig(ruta, dpi=150)
    plt.close()


def grafica_importancia(modelo, columnas, ruta):
    imp = pd.Series(modelo.feature_importances_, index=columnas).sort_values()
    plt.figure(figsize=(7, 4.5))
    imp.plot(kind="barh", color="#2bb3c0")
    plt.xlabel("Importancia de la variable (Gini)")
    plt.title("H1 · Variables más informativas para predecir la duración")
    plt.tight_layout()
    plt.savefig(ruta, dpi=150)
    plt.close()


def grafica_aprendizaje(test, pred_modelo, pred_hibrido, ruta):
    """Error medio en minutos según cuántas tareas lleva medidas el usuario."""
    posicion = test.groupby("usuario_id").cumcount()
    real = test["duracion_real_min"].to_numpy()
    tramos = pd.cut(posicion, bins=[-1, 9, 19, 39, 1000], labels=["1-10", "11-20", "21-40", "41+"])
    tabla = pd.DataFrame({
        "tramo": tramos,
        "Modelo M1 (solo población)": np.abs(pred_modelo - real),
        "Agente híbrido (población + usuario)": np.abs(pred_hibrido - real),
    }).groupby("tramo", observed=True).mean()
    tabla.plot(kind="bar", figsize=(8, 4.5), color=["#8a63d2", "#2bb3c0"], rot=0)
    plt.ylabel("Error absoluto medio (min)")
    plt.xlabel("Tareas ya medidas del usuario")
    plt.title("El agente híbrido corrige el sesgo de cada usuario")
    plt.tight_layout()
    plt.savefig(ruta, dpi=150)
    plt.close()


# ---------------------------------------------------------------------------
def main():
    os.makedirs(SALIDA, exist_ok=True)
    train, test = cargar()
    real = test["duracion_real_min"].to_numpy(dtype=float)

    filas = []
    pred_b0 = b0_usuario(train, test)
    pred_b1, factor = b1_factor_global(train, test)
    pred_b2 = b2_regla_app(test)
    filas += [
        metricas("B0 estimación usuario", "-", real, pred_b0),
        metricas("B1 factor global", "-", real, pred_b1),
        metricas("B2 regla app (F-11)", "-", real, pred_b2),
    ]

    resultados = {}
    for conjunto in CONJUNTOS:
        pred_m1, m1, cols_m1 = m1_lineal(train, test, conjunto)
        pred_m2, m2, cols_m2 = m2_bosque(train, test, conjunto)
        pred_h = hibrido(test, pred_m1)
        resultados[conjunto] = {"M1": (pred_m1, m1, cols_m1), "M2": (pred_m2, m2, cols_m2), "H": pred_h}
        filas += [
            metricas("M1 regresión lineal", conjunto, real, pred_m1),
            metricas("M2 bosque aleatorio", conjunto, real, pred_m2),
            metricas("H agente híbrido", conjunto, real, pred_h),
        ]

    tabla = pd.DataFrame(filas)
    tabla.to_csv(os.path.join(SALIDA, "metricas.csv"), index=False)

    por_categoria = pd.DataFrame({
        "categoria": test["categoria"],
        "B0 estimación usuario": np.abs(pred_b0 - real),
        "B2 regla app": np.abs(pred_b2 - real),
        "H agente híbrido (APP)": np.abs(resultados["APP"]["H"] - real),
        "H agente híbrido (+SUBCAT)": np.abs(resultados["+SUBCAT"]["H"] - real),
    }).groupby("categoria").mean().round(1)
    por_categoria.to_csv(os.path.join(SALIDA, "metricas_por_categoria.csv"))

    pred_m1_app, m1_app, cols_app = resultados["APP"]["M1"]
    exportar_modelo_app(m1_app, cols_app, factor, os.path.join(SALIDA, "modelo_app.json"))

    grafica_mae(tabla, os.path.join(SALIDA, "mae_por_estimador.png"))
    grafica_dispersion(real, {
        "B0 estimación usuario": pred_b0,
        "M1 lineal (APP)": pred_m1_app,
        "H agente híbrido (APP)": resultados["APP"]["H"],
    }, os.path.join(SALIDA, "real_vs_predicho.png"))
    _, m2_completo, cols_completo = resultados["COMPLETO"]["M2"]
    grafica_importancia(m2_completo, cols_completo, os.path.join(SALIDA, "importancia_variables.png"))
    grafica_aprendizaje(test, pred_m1_app, resultados["APP"]["H"], os.path.join(SALIDA, "aprendizaje_por_usuario.png"))

    pd.set_option("display.width", 140)
    print("\n=== H1 · Evaluación en usuarios nuevos (historial_test.csv, 1000 filas) ===\n")
    print(tabla.to_string(index=False))
    print(f"\nFactor global de subestimación (B1): x{factor:.2f}")
    print("\nError absoluto medio por categoría (min):\n")
    print(por_categoria.to_string())

    # --- Evaluación adicional en datos piloto (200 filas) ---
    ruta_piloto = os.path.join(CARPETA, "plantilla_datos_piloto_reales.csv")
    if os.path.exists(ruta_piloto):
        piloto = pd.read_csv(ruta_piloto)
        real_p = piloto["duracion_real_min"].to_numpy(dtype=float)
        pred_b0_p = b0_usuario(train, piloto)
        pred_b2_p = b2_regla_app(piloto)

        # H con conjunto APP
        X_p_app = matriz(piloto, "APP", cols_app)
        pred_m1_p_app = np.exp(m1_app.predict(X_p_app))
        pred_h_p_app = hibrido(piloto, pred_m1_p_app)

        # H con conjunto COMPLETO
        pred_m1_comp, m1_comp, cols_comp = resultados["COMPLETO"]["M1"]
        X_p_comp = matriz(piloto, "COMPLETO", cols_comp)
        pred_m1_p_comp = np.exp(m1_comp.predict(X_p_comp))
        pred_h_p_comp = hibrido(piloto, pred_m1_p_comp)

        filas_piloto = [
            metricas("B0 estimación usuario", "-", real_p, pred_b0_p),
            metricas("B2 regla app (F-11)", "-", real_p, pred_b2_p),
            metricas("H agente híbrido", "APP", real_p, pred_h_p_app),
            metricas("H agente híbrido", "COMPLETO", real_p, pred_h_p_comp),
        ]
        tabla_piloto = pd.DataFrame(filas_piloto)
        tabla_piloto.to_csv(os.path.join(SALIDA, "metricas_piloto.csv"), index=False)
        print("\n=== H1 · Validación Piloto (plantilla_datos_piloto_reales.csv, 200 filas) ===\n")
        print(tabla_piloto.to_string(index=False))

    print(f"\nResultados y gráficas en: {SALIDA}")


if __name__ == "__main__":
    main()
