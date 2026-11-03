"""
KRONO-AI - Carga de datos de demostración e IA a Cloud Firestore
================================================================
Inyecta en la cuenta del usuario de prueba (por defecto pruebaia@example.com):

  1. HISTORIAL CALIBRADO PARA F-11 (H1):
     12 tareas completadas (Estudio, Trabajo y Personal) con duraciones estimadas
     y reales medidas por el cronómetro, con sus respectivas TimeSessions.
     Esto activa inmediatamente el chip de sugerencia dinámica (F-11) y el modelo
     híbrido H1 con su adaptación online al sesgo personal.

  2. ESCENARIO ACTIVO DE TAREAS PENDIENTES PARA F-12 (H2) Y F-08 (CRONÓMETRO):
     6 tareas pendientes realistas con fechas límite escalonadas (en 3 horas, hoy
     en la noche, mañana, y en 2-3 días), prioridades variadas (ALTA, MEDIA, BAJA),
     subtareas embebidas (algunas parcialmente hechas para mostrar el efecto
     Zeigarnik) y combinación de tareas con y sin estimación manual previa.

Todos los documentos se guardan bajo users/{uid}/tasks y users/{uid}/timeSessions
cumpliendo estrictamente el esquema de TaskDto y TimeSessionDto de KRONO.

Uso:
    python KRONO-IA/cargar_firestore.py --email pruebaia@example.com
    python KRONO-IA/cargar_firestore.py --clave C:\\ruta\\serviceAccountKey.json --email pruebaia@example.com
    python KRONO-IA/cargar_firestore.py --email pruebaia@example.com --borrar
"""

import argparse
import os
import sys
import uuid
from datetime import datetime, timedelta

PREFIJO = "sint-"
MS_POR_MINUTO = 60_000
LOTE_MAXIMO = 400

RUTAS_CLAVE_PREDETERMINADAS = [
    os.path.join(os.path.expanduser("~"), "serviceAccountKey.json"),
    os.path.join(os.path.expanduser("~"), "Downloads", "serviceAccountKey.json"),
    os.path.join(os.path.expanduser("~"), "Desktop", "serviceAccountKey.json"),
    "C:\\serviceAccountKey.json",
    os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "serviceAccountKey.json"),
]


def argumentos():
    p = argparse.ArgumentParser(description="Inyecta datos de demostración de IA en Cloud Firestore.")
    p.add_argument("--clave", default=None, help="Ruta al JSON de credenciales de servicio de Firebase Admin.")
    p.add_argument("--email", default="pruebaia@example.com", help="Correo del usuario en Firebase Authentication.")
    p.add_argument("--uid", default=None, help="UID explícito del usuario (opcional; se busca por email si se omite).")
    p.add_argument("--borrar", action="store_true", help="Solo borra las tareas y sesiones sintéticas anteriores.")
    return p.parse_args()


def resolver_clave(ruta_explicit):
    if ruta_explicit:
        if not os.path.isfile(ruta_explicit):
            sys.exit(f"\n[ERROR] No se encuentra el archivo de credenciales en: {ruta_explicit}")
        return os.path.abspath(ruta_explicit)

    env_clave = os.environ.get("GOOGLE_APPLICATION_CREDENTIALS")
    if env_clave and os.path.isfile(env_clave):
        return os.path.abspath(env_clave)

    for ruta in RUTAS_CLAVE_PREDETERMINADAS:
        if os.path.isfile(ruta):
            return os.path.abspath(ruta)

    rutas_texto = "\n  - ".join(RUTAS_CLAVE_PREDETERMINADAS)
    print(f"""
[!] ATENCIÓN: No se encontró la llave privada de Firebase Admin (serviceAccountKey.json).

Para obtenerla:
  1. Ve a Firebase Console (https://console.firebase.google.com/project/krono-5ef6e/settings/serviceaccounts/adminsdk)
  2. Haz clic en "Generar nueva clave privada" y descarga el archivo JSON.
  3. Guárdalo en alguna de estas ubicaciones recomendadas:
  - {rutas_texto}
  4. O ejecútalo indicando la ruta directa con:
     python KRONO-IA/cargar_firestore.py --clave "C:\\ruta\\tu_archivo.json" --email pruebaia@example.com
""")
    sys.exit(1)


def conectar_firebase(ruta_clave):
    import firebase_admin
    from firebase_admin import credentials, firestore

    if not firebase_admin._apps:
        cred = credentials.Certificate(ruta_clave)
        firebase_admin.initialize_app(cred)
    return firestore.client()


def obtener_uid_usuario(email, uid_arg):
    if uid_arg:
        return uid_arg
    from firebase_admin import auth

    try:
        user = auth.get_user_by_email(email)
        return user.uid
    except Exception as e:
        sys.exit(f"\n[ERROR] No se pudo encontrar al usuario con correo '{email}' en Firebase Authentication:\n{e}")


def iso_local(dt):
    """Formato ISO YYYY-MM-DDTHH:MM compatible con LocalDateTime.parse de Kotlin."""
    return dt.strftime("%Y-%m-%dT%H:%M")


def generar_historial_calibrado_h1(ahora):
    """
    12 tareas completadas con duraciones reales y estimadas para activar F-11 (H1).
    Muestra subestimación empírica realista (duracion_real ~ 1.3 - 1.4 * estimacion).
    """
    plantilla = [
        # ESTUDIO (5 tareas)
        ("est-01", "ESTUDIO", "MEDIA", "Lectura de Modelos de IA Híbridos", 30, 45, 7),
        ("est-02", "ESTUDIO", "ALTA", "Taller de Algoritmos Voraces y Grafos", 45, 60, 6),
        ("est-03", "ESTUDIO", "MEDIA", "Resumen de Concurrencia en Kotlin", 35, 50, 5),
        ("est-04", "ESTUDIO", "ALTA", "Laboratorio de Base de Datos Firestore", 60, 85, 3),
        ("est-05", "ESTUDIO", "MEDIA", "Repaso para Parcial de Sistemas", 40, 55, 2),
        # TRABAJO (4 tareas)
        ("tra-01", "TRABAJO", "MEDIA", "Revisión de Pull Requests en GitHub", 30, 40, 6),
        ("tra-02", "TRABAJO", "ALTA", "Implementación de Autenticación Firebase", 60, 90, 4),
        ("tra-03", "TRABAJO", "MEDIA", "Diseño de Tokens de Diseño Lumina Glass", 45, 65, 3),
        ("tra-04", "TRABAJO", "MEDIA", "Refactorización de Repositorios en Dominio", 50, 70, 1),
        # PERSONAL (3 tareas)
        ("per-01", "PERSONAL", "BAJA", "Organización de Espacio de Estudio", 20, 25, 5),
        ("per-02", "PERSONAL", "MEDIA", "Planificación de Horarios Semanales", 30, 35, 3),
        ("per-03", "PERSONAL", "BAJA", "Configuración de Entorno Android Studio", 40, 45, 1),
    ]

    tareas = []
    sesiones = []

    for codigo, cat, prio, titulo, est_min, real_min, dias_atras in plantilla:
        id_tarea = f"{PREFIJO}hist-{codigo}"
        fecha_realizacion = (ahora - timedelta(days=dias_atras)).replace(hour=14, minute=0, second=0, microsecond=0)
        fecha_vence = fecha_realizacion + timedelta(days=1)

        t_doc = {
            "id": id_tarea,
            "title": titulo,
            "category": cat,
            "priority": prio,
            "dueDate": iso_local(fecha_vence),
            "estimatedDurationMillis": est_min * MS_POR_MINUTO,
            "actualDurationMillis": real_min * MS_POR_MINUTO,
            "isCompleted": True,
            "note": f"Dato sintético de calibración H1 · Mediana observada: {real_min} min",
            "subtasks": [],
        }
        tareas.append(t_doc)

        # TimeSession asociada
        inicio_sesion = fecha_realizacion
        fin_sesion = inicio_sesion + timedelta(minutes=real_min)
        s_doc = {
            "id": f"{PREFIJO}ses-{codigo}",
            "taskId": id_tarea,
            "startedAtMillis": int(inicio_sesion.timestamp() * 1000),
            "endedAtMillis": int(fin_sesion.timestamp() * 1000),
            "durationMillis": real_min * MS_POR_MINUTO,
            "type": "CRONOMETRO",
        }
        sesiones.append(s_doc)

    return tareas, sesiones


def generar_escenario_pendientes_h2(ahora):
    """
    6 tareas pendientes con fechas escalonadas, prioridades variadas,
    subtareas (efecto Zeigarnik) y mezcla de estimaciones manuales.
    """
    hoy_noche = ahora.replace(hour=22, minute=0, second=0, microsecond=0)
    if hoy_noche <= ahora:
        hoy_noche = ahora + timedelta(hours=3)

    return [
        {
            "id": f"{PREFIJO}pend-01",
            "title": "Entrega de Taller de Estructuras de Datos",
            "category": "ESTUDIO",
            "priority": "ALTA",
            "dueDate": iso_local(ahora + timedelta(hours=3)),
            "estimatedDurationMillis": 50 * MS_POR_MINUTO,
            "actualDurationMillis": 0,
            "isCompleted": False,
            "note": "Entrega crítica próxima · Demuestra advertencia de margen y prioridad alta",
            "subtasks": [
                {
                    "id": str(uuid.uuid4()),
                    "taskId": f"{PREFIJO}pend-01",
                    "title": "Implementar árboles de búsqueda balanceados",
                    "isDone": True,
                    "position": 0,
                },
                {
                    "id": str(uuid.uuid4()),
                    "taskId": f"{PREFIJO}pend-01",
                    "title": "Diseñar pruebas unitarias de inserción y borrado",
                    "isDone": False,
                    "position": 1,
                },
                {
                    "id": str(uuid.uuid4()),
                    "taskId": f"{PREFIJO}pend-01",
                    "title": "Compilar informe en PDF y subir al aula virtual",
                    "isDone": False,
                    "position": 2,
                },
            ],
        },
        {
            "id": f"{PREFIJO}pend-02",
            "title": "Diapositivas para Exposición Final de Proyecto",
            "category": "ESTUDIO",
            "priority": "ALTA",
            "dueDate": iso_local(hoy_noche),
            "estimatedDurationMillis": 60 * MS_POR_MINUTO,
            "actualDurationMillis": 0,
            "isCompleted": False,
            "note": "Vence hoy en la noche · Demuestra continuidad Zeigarnik",
            "subtasks": [
                {
                    "id": str(uuid.uuid4()),
                    "taskId": f"{PREFIJO}pend-02",
                    "title": "Estructurar resumen ejecutivo y justificación técnica",
                    "isDone": True,
                    "position": 0,
                },
                {
                    "id": str(uuid.uuid4()),
                    "taskId": f"{PREFIJO}pend-02",
                    "title": "Diagramar arquitectura Lumina Glass y motor IA",
                    "isDone": False,
                    "position": 1,
                },
            ],
        },
        {
            "id": f"{PREFIJO}pend-03",
            "title": "Corrección de Bug Crítico en Pasarela de Pagos",
            "category": "TRABAJO",
            "priority": "ALTA",
            "dueDate": iso_local(ahora + timedelta(days=1, hours=2)),
            "estimatedDurationMillis": 45 * MS_POR_MINUTO,
            "actualDurationMillis": 0,
            "isCompleted": False,
            "note": "Alta prioridad laboral con fecha límite mañana",
            "subtasks": [],
        },
        {
            "id": f"{PREFIJO}pend-04",
            "title": "Lectura de Artículos de Microservicios",
            "category": "ESTUDIO",
            "priority": "MEDIA",
            "dueDate": iso_local(ahora + timedelta(days=2)),
            "estimatedDurationMillis": None,  # Sin estimación manual -> prueba sugerencia de IA (F-11)
            "actualDurationMillis": 0,
            "isCompleted": False,
            "note": "Sin estimación manual previa · Permite probar el chip de sugerencia inteligente F-11",
            "subtasks": [],
        },
        {
            "id": f"{PREFIJO}pend-05",
            "title": "Actualización de Documentación de API REST",
            "category": "TRABAJO",
            "priority": "MEDIA",
            "dueDate": iso_local(ahora + timedelta(days=3)),
            "estimatedDurationMillis": 40 * MS_POR_MINUTO,
            "actualDurationMillis": 0,
            "isCompleted": False,
            "note": "Tarea intermedia de desarrollo",
            "subtasks": [
                {
                    "id": str(uuid.uuid4()),
                    "taskId": f"{PREFIJO}pend-05",
                    "title": "Documentar contratos de endpoints",
                    "isDone": False,
                    "position": 0,
                },
                {
                    "id": str(uuid.uuid4()),
                    "taskId": f"{PREFIJO}pend-05",
                    "title": "Agregar ejemplos de peticiones cURL",
                    "isDone": False,
                    "position": 1,
                },
            ],
        },
        {
            "id": f"{PREFIJO}pend-06",
            "title": "Mantenimiento y Respaldo de Archivos Personales",
            "category": "PERSONAL",
            "priority": "BAJA",
            "dueDate": iso_local(ahora + timedelta(days=3, hours=5)),
            "estimatedDurationMillis": 30 * MS_POR_MINUTO,
            "actualDurationMillis": 0,
            "isCompleted": False,
            "note": "Tarea ligera de baja prioridad",
            "subtasks": [],
        },
    ]


def escribir_en_lotes(db, coleccion, documentos):
    for i in range(0, len(documentos), LOTE_MAXIMO):
        lote = db.batch()
        for doc in documentos[i : i + LOTE_MAXIMO]:
            lote.set(coleccion.document(doc["id"]), doc)
        lote.commit()


def borrar_documentos_sinteticos(db, usuario_ref):
    total = 0
    for nombre in ("tasks", "timeSessions"):
        col = usuario_ref.collection(nombre)
        docs = col.stream()
        for doc in docs:
            if doc.id.startswith(PREFIJO):
                doc.reference.delete()
                total += 1
    return total


def main():
    args = argumentos()
    ruta_clave = resolver_clave(args.clave)
    print(f"[*] Conectando a Firebase con: {ruta_clave}")
    db = conectar_firebase(ruta_clave)

    uid = obtener_uid_usuario(args.email, args.uid)
    print(f"[*] Usuario identificado: {args.email} (UID: {uid})")

    usuario_ref = db.collection("users").document(uid)
    usuario_ref.set(
        {
            "email": args.email,
            "role": "Estudiante y trabajador",
            "planDemo": True,
        },
        merge=True,
    )

    borrados = borrar_documentos_sinteticos(db, usuario_ref)
    if borrados > 0:
        print(f"[-] Limpiados {borrados} documentos de demostración anteriores.")

    if args.borrar:
        print("[OK] Datos sintéticos eliminados. Fin.")
        return

    ahora = datetime.now()
    tareas_hist, sesiones_hist = generar_historial_calibrado_h1(ahora)
    tareas_pend = generar_escenario_pendientes_h2(ahora)

    escribir_en_lotes(db, usuario_ref.collection("tasks"), tareas_hist + tareas_pend)
    escribir_en_lotes(db, usuario_ref.collection("timeSessions"), sesiones_hist)

    print("\n" + "=" * 70)
    print(" INYECCIÓN EXITOSA EN CLOUD FIRESTORE")
    print("=" * 70)
    print(f"Usuario: {args.email} | UID: {uid}")
    print(f"Tareas Completadas (Calibración H1): {len(tareas_hist)}")
    print(f"Sesiones de Tiempo (TimeSessions):   {len(sesiones_hist)}")
    print(f"Tareas Pendientes  (Asistente H2):    {len(tareas_pend)}")
    print("-" * 70)
    print("Tareas Pendientes Inyectadas:")
    for t in tareas_pend:
        est = f"{t['estimatedDurationMillis'] // MS_POR_MINUTO} min" if t['estimatedDurationMillis'] else "Sin estimación"
        sub = f"({len(t['subtasks'])} subtareas)" if t['subtasks'] else ""
        print(f"  [{t['priority']}] {t['title']} | Vence: {t['dueDate']} | Est: {est} {sub}")
    print("=" * 70)


if __name__ == "__main__":
    main()
