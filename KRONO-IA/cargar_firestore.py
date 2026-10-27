"""
KRONO-AI - Carga de datos sintéticos a Cloud Firestore
======================================================
Sube a la cuenta de un usuario de prueba de KRONO:

  * El historial de UN usuario sintético de historial_train.csv (por defecto U01)
    como tareas COMPLETADAS con su tiempo real medido, más una sesión de tiempo
    por tarea. Esto alimenta la estimación por historial (F-11).
  * Las tareas de UN escenario de escenarios_tareas.csv (por defecto E001) como
    tareas PENDIENTES, que el asistente de prioridades ordena (F-12).

Los documentos usan exactamente el formato que lee la app (TaskDto y TimeSessionDto
en core-data), bajo users/{uid}/tasks y users/{uid}/timeSessions.

Correspondencias (la app solo tiene Estudio, Trabajo y Personal):
  * Todas las categorías académicas del CSV -> ESTUDIO; la original queda en la nota.
  * prioridad 1 -> BAJA, 2 -> MEDIA, 3 -> ALTA (1 baja, 3 alta en SCRIPT.PY).
  * La fatiga y el bloque horario no existen en la app: también van en la nota.

Todos los documentos llevan el prefijo "sint-" en su id, así que se pueden borrar
sin tocar las tareas reales del usuario (opción --borrar).

Requisitos:
    pip install firebase-admin pandas

Uso (desde la carpeta KRONO):
    python KRONO-IA/cargar_firestore.py --clave C:\\ruta\\clave.json --uid <UID>
    python KRONO-IA/cargar_firestore.py --clave ... --uid <UID> --usuario U03 --escenario E010
    python KRONO-IA/cargar_firestore.py --clave ... --uid <UID> --borrar

IMPORTANTE: la clave de servicio da acceso total al proyecto. Guárdala FUERA del
repositorio y nunca la subas a GitHub.
"""

import argparse
import os
import sys
import uuid
from datetime import datetime, timedelta

import pandas as pd

PREFIJO = "sint-"
CARPETA = os.path.dirname(os.path.abspath(__file__))
MS_POR_MINUTO = 60_000
LOTE_MAXIMO = 400  # Firestore admite hasta 500 escrituras por lote

PRIORIDADES = {1: "BAJA", 2: "MEDIA", 3: "ALTA"}
NOMBRES_CATEGORIA = {
    "lectura": "Lectura",
    "programacion": "Programación",
    "redaccion": "Redacción",
    "estudio_parcial": "Estudio para parcial",
    "ejercicios": "Ejercicios",
    "exposicion": "Exposición",
}


def argumentos():
    p = argparse.ArgumentParser(description="Carga datos sintéticos de KRONO-IA a Firestore.")
    p.add_argument("--clave", required=True, help="Ruta al JSON de la cuenta de servicio (fuera del repo).")
    p.add_argument("--uid", required=True, help="UID del usuario de prueba (Authentication > Usuarios).")
    p.add_argument("--usuario", default="U01", help="Usuario sintético de historial_train.csv.")
    p.add_argument("--escenario", default="E001", help="Escenario de escenarios_tareas.csv.")
    p.add_argument("--borrar", action="store_true", help="Solo borra los datos sintéticos cargados antes.")
    return p.parse_args()


def validar_clave(ruta):
    if not os.path.isfile(ruta):
        sys.exit(f"No encuentro la clave: {ruta}")
    repo = os.path.dirname(CARPETA)
    if os.path.abspath(ruta).startswith(repo + os.sep):
        sys.exit("La clave está dentro del repositorio. Muévela fuera de la carpeta KRONO para no subirla a GitHub.")


def conectar(ruta_clave):
    import firebase_admin
    from firebase_admin import credentials, firestore

    firebase_admin.initialize_app(credentials.Certificate(ruta_clave))
    return firestore.client()


def iso_local(fecha):
    """Formato de LocalDateTime de kotlinx-datetime que usa TaskDto.dueDate."""
    return fecha.strftime("%Y-%m-%dT%H:%M")


def nota(fila):
    nombre = NOMBRES_CATEGORIA.get(fila["categoria"], fila["categoria"])
    partes = [f"Dato sintético · {nombre}", f"fatiga {fila['fatiga']}"]
    if "bloque_horario" in fila:
        partes.append(f"bloque {fila['bloque_horario']}")
    return " · ".join(partes)


def tarea(id_tarea, titulo, fila, vence, completada, real_min):
    return {
        "id": id_tarea,
        "title": titulo,
        "category": "ESTUDIO",
        "priority": PRIORIDADES.get(int(fila["prioridad"]), "MEDIA"),
        "dueDate": iso_local(vence),
        "estimatedDurationMillis": int(fila["estimacion_usuario_min"]) * MS_POR_MINUTO,
        "actualDurationMillis": int(real_min) * MS_POR_MINUTO,
        "isCompleted": completada,
        "note": nota(fila),
        "subtasks": [],
    }


def sesion(id_tarea, inicio, real_min):
    fin = inicio + timedelta(minutes=int(real_min))
    return {
        "id": f"{PREFIJO}{uuid.uuid4()}",
        "taskId": id_tarea,
        "startedAtMillis": int(inicio.timestamp() * 1000),
        "endedAtMillis": int(fin.timestamp() * 1000),
        "durationMillis": int(real_min) * MS_POR_MINUTO,
        "type": "CRONOMETRO",
    }


def documentos_historial(usuario, ahora):
    df = pd.read_csv(os.path.join(CARPETA, "historial_train.csv"))
    filas = df[df.usuario_id == usuario].reset_index(drop=True)
    if filas.empty:
        sys.exit(f"El usuario {usuario} no está en historial_train.csv")
    tareas, sesiones = [], []
    for i, fila in filas.iterrows():
        # Una tarea por día hacia atrás, a la hora en que se hizo, para que la más
        # reciente del CSV sea también la más reciente en la app.
        dia = (ahora - timedelta(days=len(filas) - i)).replace(minute=0, second=0, microsecond=0)
        inicio = dia.replace(hour=int(fila["hora_inicio"]))
        id_tarea = f"{PREFIJO}{fila['tarea_id']}"
        titulo = f"{NOMBRES_CATEGORIA.get(fila['categoria'], fila['categoria'])} ({fila['tarea_id']})"
        vence = inicio + timedelta(days=int(fila["dias_a_fecha_limite"]))
        tareas.append(tarea(id_tarea, titulo, fila, vence, True, fila["duracion_real_min"]))
        sesiones.append(sesion(id_tarea, inicio, fila["duracion_real_min"]))
    return tareas, sesiones


def documentos_escenario(escenario, ahora):
    df = pd.read_csv(os.path.join(CARPETA, "escenarios_tareas.csv"))
    filas = df[df.escenario_id == escenario]
    if filas.empty:
        sys.exit(f"El escenario {escenario} no está en escenarios_tareas.csv")
    pendientes = []
    for _, fila in filas.iterrows():
        id_tarea = f"{PREFIJO}{fila['tarea_id']}"
        titulo = f"{NOMBRES_CATEGORIA.get(fila['categoria'], fila['categoria'])} ({fila['tarea_id']})"
        vence = ahora + timedelta(minutes=int(fila["fecha_limite_min"]))
        # duracion_real_min del escenario es solo para simular: aún no se ha trabajado.
        pendientes.append(tarea(id_tarea, titulo, fila, vence, False, 0))
    return pendientes


def escribir(db, coleccion, documentos):
    for i in range(0, len(documentos), LOTE_MAXIMO):
        lote = db.batch()
        for doc in documentos[i:i + LOTE_MAXIMO]:
            lote.set(coleccion.document(doc["id"]), doc)
        lote.commit()


def borrar_sinteticos(db, usuario_ref):
    total = 0
    for nombre in ("tasks", "timeSessions"):
        for doc in usuario_ref.collection(nombre).stream():
            if doc.id.startswith(PREFIJO):
                doc.reference.delete()
                total += 1
    return total


def main():
    args = argumentos()
    validar_clave(args.clave)
    db = conectar(args.clave)
    usuario_ref = db.collection("users").document(args.uid)

    borrados = borrar_sinteticos(db, usuario_ref)
    print(f"Borrados {borrados} documentos sintéticos anteriores.")
    if args.borrar:
        return

    ahora = datetime.now()
    tareas, sesiones = documentos_historial(args.usuario, ahora)
    pendientes = documentos_escenario(args.escenario, ahora)

    escribir(db, usuario_ref.collection("tasks"), tareas + pendientes)
    escribir(db, usuario_ref.collection("timeSessions"), sesiones)
    print(f"Cargadas {len(tareas)} tareas completadas de {args.usuario} con {len(sesiones)} sesiones,")
    print(f"y {len(pendientes)} tareas pendientes del escenario {args.escenario}, en users/{args.uid}.")


if __name__ == "__main__":
    main()
