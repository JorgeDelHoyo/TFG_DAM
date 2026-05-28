# 🎸 ARPEG.IO — Estudio de Música de Bolsillo

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.21-purple?style=for-the-badge&logo=kotlin)](https://kotlinlang.org/)
[![Platform](https://img.shields.io/badge/Platform-Android_8.0+_(API_26+)-3DDC84?style=for-the-badge&logo=android)](https://www.android.com/)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM_/_Clean-blue?style=for-the-badge)](https://developer.android.com/topic/architecture)
[![UI](https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Testing](https://img.shields.io/badge/Coverage-93%25_JaCoCo-success?style=for-the-badge)](https://jacoco.org/)

---

## 📄 Descripción del Proyecto

**ARPEG.IO** es una aplicación nativa para Android desarrollada como **Trabajo de Fin de Grado (TFG)** para el ciclo superior de **Desarrollo de Aplicaciones Multiplataforma (DAM)**. 

La aplicación resuelve de forma elegante la fragmentación de herramientas al estudiar un instrumento musical. Ofrece un entorno integrado de **pantalla dividida ("Split-Screen")** donde el músico puede reproducir su vídeo/audio de referencia y seguir simultáneamente la **tablatura interactiva** (.gp3 / Guitar Pro) de forma coordinada, eliminando cualquier distracción.

---

## 🚀 Funcionalidades Principales

### 1. 🌐 Doble Origen de Datos (Dual-Source Data Flow)
La aplicación implementa el patrón **Repository** centralizado que abstrae e integra dos fuentes de información diferenciadas:
*   **Comunidad (Remoto - Firebase Firestore)**: Canciones recomendadas por la comunidad que enlazan dinámicamente un vídeo oficial de YouTube con su partitura interactiva.
*   **Mis Canciones (Local - Room + File Importer)**: Permite al usuario importar sus propias partituras en formato `.gp3` desde el almacenamiento del dispositivo, copiando el archivo de forma segura en el almacenamiento interno privado de la app (`filesDir`) y catalogándolo en base de datos local.

### 2. 📺 Interfaz de Reproducción Sincronizada
*   **Reproductor Integrado**: Uso eficiente del reproductor iframe de YouTube optimizado para móviles.
*   **Visor de Partituras Web**: WebView personalizado e interactivo con inyección de código dinámico y soporte completo para interactuar con tablaturas.

### 3. 📂 Biblioteca Reactiva e Inteligente
*   Listas dinámicas separadas en acordeón que cargan de forma reactiva con `Flows` en tiempo real.
*   Capacidad de realizar operaciones CRUD en caliente (renombrar títulos, eliminar archivos locales del almacenamiento y catálogo, filtrar canciones).

---

## 🛠️ Stack Tecnológico y Arquitectura

El proyecto está diseñado bajo las directrices modernas de la arquitectura Android recomendada por Google, con un flujo unidireccional de datos en capas de abstracción bien definidas:

| Capa | Tecnología | Función Técnica |
| :--- | :--- | :--- |
| **Interfaz (UI)** | **Jetpack Compose** | UI declarativa moderna y reactiva, transiciones suaves y estados unificados. |
| **Arquitectura** | **MVVM / UDF** | *Model-View-ViewModel* con flujo de datos unidireccional para garantizar una UI libre de efectos secundarios. |
| **Inyección de Dependencias** | **Dagger Hilt** | Gestión automatizada del ciclo de vida y acoplamiento de componentes. |
| **Base de Datos** | **Room Database** | Persistencia local robusta sobre SQLite mediante DAOs asíncronos. |
| **Nube y Red** | **Firebase Firestore** | Gestión en tiempo real de la base de datos de canciones de la comunidad. |
| **Concurrencia** | **Kotlin Coroutines + Flow** | Ejecución asíncrona de operaciones I/O en hilos secundarios e intercambio reactivo de estados en tiempo real. |

---

## 🧪 Pruebas Unitarias y Calidad de Código (JaCoCo)

El proyecto cuenta con una suite completa de pruebas unitarias locales automatizadas que garantizan la estabilidad de la lógica del negocio ante futuras actualizaciones o refactorizaciones:

*   **Mockito (`mockito-core` y `mockito-kotlin`)**: Utilizado para simular las llamadas de red y de persistencia local (Firebase/Room), logrando tests unitarios rápidos y 100% aislados en la JVM local de desarrollo.
*   **MainDispatcherRule**: Regla JUnit personalizada para redirigir el despachador Main de Coroutines en la máquina local de desarrollo, garantizando ejecución síncrona.
*   **JaCoCo (Java Code Coverage)**: Configurado en el entorno Gradle del proyecto con filtros estrictos de exclusión de clases generadas e inyecciones de Hilt, logrando un reporte interactivo limpio con métricas de primer nivel:
    *   **93% de Cobertura general en Instrucciones**.
    *   **80% de Cobertura en Ramas condicionales**.
    *   **100% de Cobertura** en `LibraryViewModel`, modelo de datos `Song` y base de datos `Converters`.

---

## ⚙️ Configuración e Instalación

Para ejecutar el proyecto en tu entorno local de desarrollo:

1.  **Clonar el repositorio**:
    ```bash
    git clone https://github.com/JorgeDelHoyo/TFG_DAM.git
    ```
2.  **Abrir el Proyecto**:
    Abre la carpeta `TFG_versions/TFGv0.1` en **Android Studio** (Koala o superior).
3.  **Configurar dependencias y SDK**:
    Sincroniza el proyecto con Gradle. Requiere **Java JDK 17** o superior (recomendado JDK 21).
4.  **Ejecutar los Tests en local**:
    ```powershell
    .\gradlew.bat :app:testDebugUnitTest
    ```
5.  **Generar Reporte de Cobertura**:
    ```powershell
    .\gradlew.bat :app:testDebugUnitTest :app:jacocoTestReport
    ```
    *(El reporte visual interactivo en HTML se generará en `app/build/reports/jacoco/jacocoTestReport/html/index.html`)*

---

## 🎓 Alineamiento Académico (Módulos DAM)

Este proyecto evidencia la aplicación práctica y profesional de los conocimientos adquiridos a lo largo del ciclo superior de **Desarrollo de Aplicaciones Multiplataforma**:

*   **PMDM (Programación Multimedia y Dispositivos Móviles)**: Integración de vídeo en streaming de YouTube, renderizado de WebView dinámico e interfaces de usuario avanzadas con Jetpack Compose.
*   **PSP (Programación de Servicios y Procesos)**: Programación concurrente asíncrona mediante Coroutines de Kotlin para evitar bloqueos del hilo de renderizado principal (UI).
*   **AD (Acceso a Datos)**: Integración híbrida de datos en tiempo real mediante Firestore Cloud DB y persistencia offline segura mediante Room (SQLite) con convertidores de tipos Gson complejos.
*   **DI (Desarrollo de Interfaces)**: Arquitectura MVVM reactiva, diseño adaptativo, control estricto de estados gráficos y animaciones fluidas.
*   **SGE (Sistemas de Gestión Empresarial)**: Automatización de tareas de despliegue y empaquetado seguro en la tienda oficial.

---

## 👤 Autor e Información Académica

*   **Autor**: Jorge del Hoyo Ballestín
*   **Centro de Estudios**: IES Pablo Serrano (Zaragoza)
*   **Especialidad**: 2º DAM (Desarrollo de Aplicaciones Multiplataforma)
*   **Contacto**: [jdhb2004@gmail.com](mailto:jdhb2004@gmail.com)
*   **GitHub**: [@JorgeDelHoyo](https://github.com/JorgeDelHoyo)