# 🎸 Arpeg.io (idea base)

![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-purple?style=flat&logo=kotlin)
![Platform](https://img.shields.io/badge/Platform-Android-green?style=flat&logo=android)
![Status](https://img.shields.io/badge/Status-Development-orange)
![License](https://img.shields.io/badge/License-MIT-blue)

**Tu estudio de música de bolsillo: Sincronización inteligente de Audio y Partituras.**

---

## 📄 Descripción del Proyecto

**Arpeg.io** es una aplicación nativa para Android desarrollada como **Trabajo de Fin de Grado (TFG)** del ciclo de Desarrollo de Aplicaciones Multiplataforma (DAM).

La aplicación soluciona la problemática de la fragmentación de herramientas en el estudio musical. Permite a los músicos visualizar **tablaturas interactivas** (procedentes de servicios web como Songsterr) simultáneamente con la reproducción de su **referencia auditiva** (YouTube Oficial), todo en una interfaz unificada de pantalla dividida ("Split-Screen").

El sistema actúa como un orquestador de APIs, automatizando la búsqueda y sincronización de recursos para ofrecer una experiencia de usuario fluida y libre de distracciones.

---

## 🚀 Funcionalidades Principales

### 1. 🔍 Búsqueda Cruzada Inteligente
Mediante un único input del usuario, el sistema bifurca la petición para localizar:
* **Audio:** Prioriza versiones "Official Audio" o "Topic" en YouTube para garantizar alta fidelidad sonora.
* **Partitura:** Realiza una búsqueda filtrada en bases de datos de tablaturas (Songsterr) para encontrar la versión exacta de la canción.

### 2. 📺 Interfaz de Estudio (Split-View)
* **Panel Superior:** Reproductor nativo de YouTube con controles de *seek*, pausa y volumen.
* **Panel Inferior:** WebView optimizado. Utiliza **Inyección de JavaScript** para manipular el DOM de la web destino, eliminando anuncios, cabeceras y menús de navegación, maximizando el área útil para la partitura.

### 3. ⭐ Gestión de Repertorio (Persistencia)
* Almacenamiento local mediante **Room Database (SQLite)**.
* Permite guardar canciones favoritas para acceder rápidamente al par *VideoID + URL* sin necesidad de realizar nuevas peticiones a la API.

---

## 🛠️ Stack Tecnológico

El proyecto sigue las directrices de **Arquitectura Moderna de Android** y el patrón **MVVM (Model-View-ViewModel)**.

| Categoría | Tecnología | Justificación Técnica |
| :--- | :--- | :--- |
| **Lenguaje** | **Kotlin** | Estándar actual de desarrollo Android. |
| **Arquitectura** | **MVVM** | Separación de lógica de negocio y UI. |
| **Networking** | **Retrofit 2 + Gson** | Consumo de APIs REST y parseo de JSON. |
| **Concurrencia** | **Coroutines** | Gestión de hilos en segundo plano (I/O). |
| **Multimedia** | **Android-YouTube-Player** | Librería eficiente para embed de video (IFrame API wrapper). |
| **Web** | **WebView + JS Injection** | Renderizado y limpieza de interfaces web externas. |
| **Persistencia** | **Room** | Abstracción sobre SQLite para guardar favoritos. |
| **Imágenes** | **Glide** | Carga asíncrona y cacheo de carátulas. |

---

## 🧩 Arquitectura del Sistema

El flujo de datos de la aplicación se estructura de la siguiente manera:



1.  **Capa de Vista (Activity/Fragment):** Recoge el input y observa los cambios en el ViewModel (LiveData/StateFlow).
2.  **Capa de ViewModel:** Gestiona el estado de la UI y lanza las Corrutinas.
3.  **Capa de Modelo (Repository):**
    * Consulta a **YouTube Data API v3** para obtener el `videoId`.
    * Consulta a **Google Custom Search JSON API** para obtener la `targetUrl`.
4.  **Capa de Datos Local:** Si no hay conexión, intenta recuperar datos de **Room**.

---

## ⚙️ Configuración e Instalación

Para ejecutar este proyecto en local, necesitas configurar las claves de API.

1.  **Clonar el repositorio:**
    ```bash
    git clone [https://github.com/tu-usuario/Arpeg.io.git](https://github.com/tu-usuario/Arpeg.io.git)
    ```
2.  **Configurar API Keys:**
    Crea un archivo `apikey.properties` (o en `local.properties`) en la raíz del proyecto y añade tus claves de Google Cloud Console:
    ```properties
    YOUTUBE_API_KEY="TU_CLAVE_AQUI"
    GOOGLE_SEARCH_KEY="TU_CLAVE_AQUI"
    SEARCH_ENGINE_ID="TU_CX_ID_AQUI"
    ```
3.  **Compilar:**
    Abre el proyecto en **Android Studio** y sincroniza Gradle.

---

## 🎓 Competencias del Ciclo (DAM)

Este proyecto evidencia la adquisición de competencias en los siguientes módulos:

* **PMDM (Programación Multimedia y Dispositivos Móviles):** Desarrollo de interfaces nativas complejas, integración de sensores/multimedia y gestión del ciclo de vida.
* **PSP (Programación de Servicios y Procesos):** Programación multihilo (Corrutinas) y comunicaciones de red seguras.
* **AD (Acceso a Datos):** Implementación de persistencia local (Room) y consumo de datos remotos.
* **DI (Desarrollo de Interfaces):** Diseño adaptable y experiencia de usuario (UX).

---

## 🔮 Mejoras Futuras (Roadmap)

* [ ] Implementación de afinador cromático usando el micrófono.
* [ ] Modo "Offline" con descarga de PDFs.
* [ ] Sincronización de favoritos en la nube con Firebase Auth.

---

## 👤 Autor

**[Jorge del Hoyo Ballestín]**
* **Grado:** 2º DAM (Desarrollo de Aplicaciones Multiplataforma)
* **Centro:** [IES Pablo Serrano]
* **Contacto:** [jdhb2004@gmail.com]

---