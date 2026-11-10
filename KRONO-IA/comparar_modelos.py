"""
KRONO-AI - H1: comparación del modelo elegido contra otros algoritmos
=====================================================================
Responde dos preguntas para la sustentación:

  1. ¿Por qué elegimos regresión lineal (Ridge log-log) + corrección híbrida por usuario?
  2. ¿Contra qué modelos lo comparamos?

Compara seis algoritmos de regresión de scikit-learn, todos con las MISMAS variables
(conjunto APP: lo que la app ya conoce al crear una tarea) y el mismo objetivo
log(duración real):

  M1  Regresión lineal Ridge      (modelo elegido, exportable a Kotlin)
  M2  Bosque aleatorio            (ensamble de árboles)
  M3  Gradient Boosting           (árboles secuenciales)
  M4  K vecinos más cercanos      (basado en similitud)
  M5  Máquina de soporte vectorial (SVR, kernel RBF)
  M6  Red neuronal (MLP)          (perceptrón multicapa)

Cada modelo se evalúa:
  - solo (predicción de población), y
  - + agente híbrido (la misma corrección por sesgo personal que usa la app),
para que la comparación sea justa: el componente híbrido se le ofrece a todos.

Validación:
  - Prueba externa: historial_test.csv (usuarios que el modelo nunca vio).
  - Validación cruzada agrupada por usuario (GroupKFold, 5 pliegues) sobre el
    entrenamiento: da media ± desviación del error, no un solo número.

Salidas en ./resultados/comparacion:
  comparacion_modelos.csv, validacion_cruzada.csv, error_por_categoria.csv
  y 6 gráficas .png

Uso (desde la carpeta KRONO-IA):
    python comparar_modelos.py
"""

import os
import time

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from sklearn.base import clone
from sklearn.ensemble import GradientBoostingRegressor, RandomForestRegressor
from sklearn.linear_model import Ridge
from sklearn.metrics import mean_absolute_error
from sklearn.model_selection import GroupKFold
from sklearn.neighbors import KNeighborsRegressor
from sklearn.neural_network import MLPRegressor
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler
from sklearn.svm import SVR

from entrenar_modelo import SEMILLA, cargar, hibrido, matriz, metricas

CARPETA = os.path.dirname(os.path.abspath(__file__))
SALIDA = os.path.join(CARPETA, "resultados", "comparacion")
CONJUNTO = "APP"

MODELOS = {
    "M1 Ridge (elegido)": Ridge(alpha=1.0),
    "M2 Bosque aleatorio": RandomForestRegressor(n_estimators=300, min_samples_leaf=3, random_state=SEMILLA),
    "M3 Gradient Boosting": GradientBoostingRegressor(n_estimators=300, learning_rate=0.05, max_depth=3,
                                                      random_state=SEMILLA),
    "M4 KNN (k=25)": make_pipeline(StandardScaler(), KNeighborsRegressor(n_neighbors=25, weights="distance")),
    "M5 SVR (RBF)": make_pipeline(StandardScaler(), SVR(C=1.0, epsilon=0.05)),
    "M6 Red neuronal MLP": make_pipeline(StandardScaler(), MLPRegressor(hidden_layer_sizes=(32, 16), alpha=1e-3,
                                                                        max_iter=2000, early_stopping=True,
                                                                        random_state=SEMILLA)),
}

# Qué tan fácil es llevar cada modelo al celular (Kotlin, sin servidor).
DESPLIEGUE = {
    "M1 Ridge (elegido)": "Fórmula de 7 coeficientes en JSON; se implementa en ~10 líneas de Kotlin",
    "M2 Bosque aleatorio": "300 árboles; requiere TFLite/ONNX o un servidor",
    "M3 Gradient Boosting": "300 árboles; requiere TFLite/ONNX o un servidor",
    "M4 KNN (k=25)": "Debe llevar los 3.200 registros de entrenamiento al teléfono",
    "M5 SVR (RBF)": "Debe llevar todos los vectores de soporte; requiere ONNX o servidor",
    "M6 Red neuronal MLP": "Requiere TFLite/ONNX; caja negra",
}

COLOR_ELEGIDO = "#BD00FF"
COLOR_OTROS = "#9aa0b4"
COLOR_HIBRIDO = "#49D9E5"


def corto(nombre):
    return nombre.split(" ")[0]


def contar_parametros(modelo):
    m = modelo[-1] if hasattr(modelo, "steps") else modelo
    if isinstance(m, Ridge):
        return m.coef_.size + 1
    if isinstance(m, (RandomForestRegressor, GradientBoostingRegressor)):
        arboles = m.estimators_.ravel() if hasattr(m.estimators_, "ravel") else m.estimators_
        return int(sum(a.tree_.node_count for a in arboles))
    if isinstance(m, KNeighborsRegressor):
        return int(m.n_samples_fit_ * (m.n_features_in_ + 1))
    if isinstance(m, SVR):
        return int(m.support_vectors_.size + m.dual_coef_.size + 1)
    if isinstance(m, MLPRegressor):
        return int(sum(c.size for c in m.coefs_) + sum(b.size for b in m.intercepts_))
    return np.nan


# ---------------------------------------------------------------------------
def evaluar_prueba(train, test):
    Xtr = matriz(train, CONJUNTO)
    Xte = matriz(test, CONJUNTO, Xtr.columns)
    ytr = np.log(train["duracion_real_min"])
    real = test["duracion_real_min"].to_numpy(dtype=float)

    filas, preds = [], {}
    for nombre, base in MODELOS.items():
        modelo = clone(base)
        t0 = time.perf_counter()
        modelo.fit(Xtr, ytr)
        t_entrena = time.perf_counter() - t0
        t0 = time.perf_counter()
        pred = np.exp(modelo.predict(Xte))
        t_pred_ms = (time.perf_counter() - t0) / len(Xte) * 1000
        pred_h = hibrido(test, pred)
        preds[nombre] = (pred, pred_h)

        for etiqueta, p in (("solo", pred), ("+ híbrido", pred_h)):
            fila = metricas(nombre, etiqueta, real, p)
            fila["modo"] = fila.pop("variables")
            fila["entrenamiento_s"] = round(t_entrena, 3)
            fila["prediccion_ms_por_tarea"] = round(t_pred_ms, 4)
            fila["parametros"] = contar_parametros(modelo)
            fila["despliegue_en_app"] = DESPLIEGUE[nombre]
            filas.append(fila)
    return pd.DataFrame(filas), preds, real


def validacion_cruzada(train, pliegues=5):
    grupos = train["usuario_id"]
    filas = []
    for k, (i_tr, i_va) in enumerate(GroupKFold(n_splits=pliegues).split(train, groups=grupos)):
        tr, va = train.iloc[i_tr], train.iloc[i_va].reset_index(drop=True)
        Xtr = matriz(tr, CONJUNTO)
        Xva = matriz(va, CONJUNTO, Xtr.columns)
        real = va["duracion_real_min"].to_numpy(dtype=float)
        for nombre, base in MODELOS.items():
            modelo = clone(base).fit(Xtr, np.log(tr["duracion_real_min"]))
            pred = np.exp(modelo.predict(Xva))
            filas.append({"modelo": nombre, "pliegue": k + 1,
                          "MAE_solo": mean_absolute_error(real, pred),
                          "MAE_hibrido": mean_absolute_error(real, hibrido(va, pred))})
    detalle = pd.DataFrame(filas)
    resumen = detalle.groupby("modelo", sort=False).agg(
        MAE_solo_media=("MAE_solo", "mean"), MAE_solo_std=("MAE_solo", "std"),
        MAE_hibrido_media=("MAE_hibrido", "mean"), MAE_hibrido_std=("MAE_hibrido", "std")).round(2)
    return detalle, resumen


# ---------------------------------------------------------------------------
# Gráficas
# ---------------------------------------------------------------------------
def colores(nombres):
    return [COLOR_ELEGIDO if "elegido" in n else COLOR_OTROS for n in nombres]


def g1_mae_prueba(tabla, ruta):
    piv = tabla.pivot(index="estimador", columns="modo", values="MAE_min").loc[list(MODELOS)]
    x = np.arange(len(piv))
    plt.figure(figsize=(10, 5))
    plt.bar(x - 0.2, piv["solo"], 0.4, label="Modelo solo", color=colores(piv.index), alpha=0.55)
    barras = plt.bar(x + 0.2, piv["+ híbrido"], 0.4, label="Modelo + agente híbrido", color=colores(piv.index))
    plt.bar_label(barras, fmt="%.1f", fontsize=9)
    plt.xticks(x, piv.index, rotation=15)
    plt.ylabel("Error absoluto medio (min) · menor es mejor")
    plt.title("H1 · Error en usuarios nuevos (historial_test.csv)")
    plt.legend()
    plt.tight_layout()
    plt.savefig(ruta, dpi=150)
    plt.close()


def g2_validacion_cruzada(detalle, ruta):
    datos = [detalle.loc[detalle["modelo"] == m, "MAE_hibrido"] for m in MODELOS]
    plt.figure(figsize=(10, 5))
    caja = plt.boxplot(datos, patch_artist=True, widths=0.5)
    for parche, c in zip(caja["boxes"], colores(MODELOS)):
        parche.set_facecolor(c)
        parche.set_alpha(0.7)
    plt.xticks(range(1, len(MODELOS) + 1), list(MODELOS), rotation=15)
    plt.ylabel("MAE por pliegue (min)")
    plt.title("H1 · Validación cruzada por usuario (5 pliegues, modelo + híbrido)")
    plt.tight_layout()
    plt.savefig(ruta, dpi=150)
    plt.close()


def g3_real_vs_predicho(preds, real, ruta):
    fig, ejes = plt.subplots(2, 3, figsize=(14, 9), sharex=True, sharey=True)
    tope = np.percentile(real, 99.5)
    for eje, (nombre, (_, p_h)) in zip(ejes.ravel(), preds.items()):
        eje.scatter(real, p_h, s=10, alpha=0.3, color=COLOR_ELEGIDO if "elegido" in nombre else "#5b6280")
        eje.plot([0, tope], [0, tope], "k--", lw=1)
        eje.set_title(f"{nombre}\nMAE {mean_absolute_error(real, p_h):.1f} min")
        eje.set_xlim(0, tope)
        eje.set_ylim(0, tope)
    for eje in ejes[1]:
        eje.set_xlabel("Duración real (min)")
    for eje in ejes[:, 0]:
        eje.set_ylabel("Predicción (min)")
    fig.suptitle("H1 · Real vs. predicho (modelo + híbrido). Línea punteada = predicción perfecta")
    plt.tight_layout()
    plt.savefig(ruta, dpi=150)
    plt.close()


def g4_distribucion_error(preds, real, ruta):
    datos = [(p_h - real) / real * 100 for _, p_h in preds.values()]
    plt.figure(figsize=(10, 5))
    caja = plt.boxplot(datos, patch_artist=True, showfliers=False, widths=0.5)
    for parche, c in zip(caja["boxes"], colores(preds)):
        parche.set_facecolor(c)
        parche.set_alpha(0.7)
    plt.axhline(0, color="k", lw=1, ls="--")
    plt.axhspan(-20, 20, color=COLOR_HIBRIDO, alpha=0.12, label="±20 % (acierto)")
    plt.xticks(range(1, len(preds) + 1), list(preds), rotation=15)
    plt.ylabel("Error relativo (%)  ·  <0 subestima, >0 sobreestima")
    plt.title("H1 · Distribución del error por modelo (modelo + híbrido)")
    plt.legend(loc="upper right")
    plt.tight_layout()
    plt.savefig(ruta, dpi=150)
    plt.close()


def g5_precision_vs_complejidad(tabla, ruta):
    t = tabla[tabla["modo"] == "+ híbrido"].set_index("estimador").loc[list(MODELOS)]
    plt.figure(figsize=(9, 5.5))
    for nombre, fila in t.iterrows():
        c = COLOR_ELEGIDO if "elegido" in nombre else "#5b6280"
        plt.scatter(fila["parametros"], fila["MAE_min"], s=160, color=c, zorder=3)
        plt.annotate(nombre, (fila["parametros"], fila["MAE_min"]), textcoords="offset points",
                     xytext=(8, 6), fontsize=9)
    plt.xscale("log")
    plt.xlabel("Tamaño del modelo (nº de parámetros / nodos / datos guardados, escala log)")
    plt.ylabel("MAE en usuarios nuevos (min)")
    plt.title("H1 · Precisión vs. complejidad: ¿vale la pena un modelo más pesado?")
    plt.grid(alpha=0.3)
    plt.tight_layout()
    plt.savefig(ruta, dpi=150)
    plt.close()


def g6_por_categoria(test, preds, real, ruta):
    tabla = pd.DataFrame({corto(n): np.abs(p_h - real) for n, (_, p_h) in preds.items()})
    tabla["categoria"] = test["categoria"].to_numpy()
    tabla = tabla.groupby("categoria").mean()
    plt.figure(figsize=(9, 5))
    im = plt.imshow(tabla.to_numpy(), cmap="magma_r", aspect="auto")
    plt.colorbar(im, label="MAE (min)")
    plt.xticks(range(tabla.shape[1]), tabla.columns)
    plt.yticks(range(tabla.shape[0]), tabla.index)
    for i in range(tabla.shape[0]):
        for j in range(tabla.shape[1]):
            v = tabla.iat[i, j]
            plt.text(j, i, f"{v:.1f}", ha="center", va="center",
                     color="white" if v > tabla.to_numpy().mean() else "black", fontsize=9)
    plt.title("H1 · Error por categoría de tarea (modelo + híbrido)")
    plt.tight_layout()
    plt.savefig(ruta, dpi=150)
    plt.close()
    return tabla.round(1)


# ---------------------------------------------------------------------------
def main():
    os.makedirs(SALIDA, exist_ok=True)
    train, test = cargar()

    tabla, preds, real = evaluar_prueba(train, test)
    tabla.to_csv(os.path.join(SALIDA, "comparacion_modelos.csv"), index=False)

    detalle, resumen = validacion_cruzada(train)
    resumen.to_csv(os.path.join(SALIDA, "validacion_cruzada.csv"))

    g1_mae_prueba(tabla, os.path.join(SALIDA, "1_mae_por_modelo.png"))
    g2_validacion_cruzada(detalle, os.path.join(SALIDA, "2_validacion_cruzada.png"))
    g3_real_vs_predicho(preds, real, os.path.join(SALIDA, "3_real_vs_predicho.png"))
    g4_distribucion_error(preds, real, os.path.join(SALIDA, "4_distribucion_error.png"))
    g5_precision_vs_complejidad(tabla, os.path.join(SALIDA, "5_precision_vs_complejidad.png"))
    cat = g6_por_categoria(test, preds, real, os.path.join(SALIDA, "6_error_por_categoria.png"))
    cat.to_csv(os.path.join(SALIDA, "error_por_categoria.csv"))

    pd.set_option("display.width", 200)
    cols = ["estimador", "modo", "MAE_min", "MAPE_pct", "R2", "aciertos_20pct", "sesgo_medio_min",
            "entrenamiento_s", "prediccion_ms_por_tarea", "parametros"]
    print("\n=== Prueba externa: usuarios nuevos (historial_test.csv) ===\n")
    print(tabla[cols].to_string(index=False))
    print("\n=== Validación cruzada agrupada por usuario (5 pliegues) ===\n")
    print(resumen.to_string())
    print(f"\nResultados y gráficas en: {SALIDA}")


if __name__ == "__main__":
    main()
