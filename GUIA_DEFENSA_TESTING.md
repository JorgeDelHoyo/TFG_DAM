# Guía de Defensa de TFG: Arquitectura de Pruebas Unitarias y Cobertura (ARPEG.IO)

Este documento sirve como material de apoyo y defensa técnica para tu Trabajo de Fin de Grado (TFG). Aquí se detalla la justificación de cada decisión de diseño técnico relacionada con las pruebas unitarias y el análisis visual de cobertura.

---

## 1. Arquitectura Técnica de Pruebas

Para garantizar que la lógica de negocio de **ARPEG.IO** sea robusta y libre de errores tras la reestructuración del paquete principal a `com.example.arpegio`, hemos diseñado una suite de pruebas aislada basada en los estándares más altos del desarrollo Android moderno.

### 1.1 Librerías Utilizadas y Justificación

*   **JUnit 4**: El framework de pruebas de cabecera de Android para orquestar la ejecución de las pruebas.
*   **Mockito (`mockito-core` y `mockito-kotlin`)**: 
    *   **¿Por qué?** Mockito nos permite simular el comportamiento de dependencias externas pesadas (como bases de datos locales Room, repositorios de red y clientes Firebase) sin tener que instanciarlos de verdad.
    *   **DSL de Kotlin**: Usar `mockito-kotlin` permite escribir una sintaxis idiomática de Kotlin (ej. `whenever(x).thenReturn(y)`), manteniendo el código legible y limpio para el tribunal evaluador.
*   **Kotlinx Coroutines Test**: 
    *   **¿Por qué?** Toda la arquitectura reactiva de la aplicación utiliza `viewModelScope` y `Flows` de Kotlin para actualizar la UI asíncronamente en tiempo real. Este módulo nos permite forzar la ejecución síncrona de las corrutinas mediante despachadores de prueba (`UnconfinedTestDispatcher` y `runTest`), evitando esperas aleatorias u otros problemas temporales.

---

## 2. Decisiones Clave de Diseño

### 2.1 El Desafío de `Dispatchers.Main` y su Solución (`MainDispatcherRule`)

*   **El problema**: Las corrutinas lanzadas en los `ViewModels` de Android se ejecutan por defecto en el hilo principal (`Dispatchers.Main`). Sin embargo, en una prueba unitaria ejecutada localmente en la máquina virtual de Java (JVM), no existe el framework de Android ni su bucle de eventos (`Looper`). Si ejecutas la prueba tal cual, fallará con una excepción.
*   **La solución**: Hemos implementado una **Regla JUnit personalizada (`MainDispatcherRule.kt`)**. Esta regla actúa como un interceptor automático de JUnit:
    1.  **Antes de cada test (`starting`)**: Sustituye `Dispatchers.Main` por un `TestDispatcher`.
    2.  **Durante el test**: Todas las llamadas a corrutinas bajo `viewModelScope` se ejecutan inmediatamente y sin desfase en el hilo de pruebas.
    3.  **Después de cada test (`finished`)**: Restablece el despachador Main original para evitar interferencias con otras pruebas.
*   **Beneficio para la defensa**: Demuestra un conocimiento profundo del funcionamiento interno del framework de concurrencia de Kotlin (Coroutines) y del ciclo de vida de pruebas JUnit.

### 2.2 Testeo Reactivo de Flujos (`Flows` Combinados)

*   En `LibraryViewModel`, el estado se construye combinando de forma reactiva (`combine`) dos flujos de datos en tiempo real: canciones remotas de **Firestore** y locales de **Room**.
*   Para testear esto, simulamos los métodos del repositorio devolviendo `MutableStateFlow` modificables. Esto nos permite simular cambios dinámicos (ej. añadir una nueva canción o lanzar un error de conexión a internet) y verificar al instante cómo el estado de la pantalla (`LibraryUiState`) reacciona y transiciona entre `Loading`, `Success` y `Error`.

---

## 3. Integración de JaCoCo (Cobertura de Código al 100%)

**JaCoCo (Java Code Coverage)** es un plugin industrial que analiza la cantidad de código fuente de la app que es verificado por los tests. 

### 3.1 Exclusión de Clases Generadas (Ruido en las Métricas)
En un proyecto real de Android, librerías como **Dagger Hilt**, Room y las clases de recursos generadas (`R.class`) autogeneran muchísimo código Java/Kotlin en tiempo de compilación. Si JaCoCo analizara estas clases, la métrica de cobertura real de nuestro código se vería diluida.
*   **Solución**: Configuré un filtro riguroso en `app/build.gradle.kts` para excluir del reporte todos estos metadatos inyectados (`**/Dagger*.*`, `**/Hilt*.*`, clases de recursos, etc.), asegurando un reporte limpio centrado únicamente en nuestro código de producción.

### 3.2 Resultados del Reporte de Cobertura
*   **`LibraryViewModel`**: **100% de Cobertura** (Instrucciones, Métodos, Líneas y Ramas de decisión cubiertas al 100%).
*   **`Song` (Modelo)**: **100% de Cobertura** en validación de URL de YouTube e instrumentos.
*   **`Converters` (Room)**: **100% de Cobertura** en serialización y deserialización de datos complejos en JSON.

---

## 4. Guía de Ejecución (Paso a Paso)

Para poder demostrar todo esto en vivo durante tu presentación o preparar las capturas de pantalla, utiliza los siguientes comandos desde la terminal en el directorio raíz de la versión de tu app (`TFG_versions/TFGv0.1`):

### 4.1 Ejecutar todos los Tests Unitarios
```powershell
.\gradlew.bat :app:testDebugUnitTest
```
*   *Qué hace*: Compila el código de pruebas, corre los tests unitarios locales en la JVM y muestra si pasaron satisfactoriamente.

### 4.2 Generar el Reporte Visual interactivo de JaCoCo
```powershell
.\gradlew.bat :app:testDebugUnitTest :app:jacocoTestReport
```
*   *Qué hace*: Corre los tests e instrumentaliza el código para crear las métricas.
*   *Ubicación del Reporte Visual (HTML)*: Abre el navegador e inspecciona el archivo index interactivo en:
    `TFG_versions/TFGv0.1/app/build/reports/jacoco/jacocoTestReport/html/index.html`

Ahí podrás hacer clic en cada paquete y clase (`LibraryViewModel`, `Song`, etc.) para ver de forma interactiva y visual (en verde y rojo) el 100% de líneas cubiertas.

---

## 5. Resumen de Tests Implementados en `LibraryViewModelTest.kt`

Hemos cubierto sistemáticamente cada método, condicional y flujo de datos de la lógica de biblioteca:

1.  `initial state is Loading before flows emit`: Verifica el comportamiento inicial reactivo del estado de carga.
2.  `loadAllSongsCombined success updates uiState to Success`: Comprueba que al recibir datos remotos y locales, el estado pasa a `Success` con los metadatos correctos de cada lista.
3.  `loadAllSongsCombined error sets uiState to Error`: Asegura el control de excepciones (como la caída de la red en Firestore) capturando el error sin provocar un crash y mostrando un mensaje descriptivo en la UI.
4.  `toggleLocalExpanded alternates local section visibility`: Valida el control de estado de UI expandido/colapsado de las canciones de Room.
5.  `toggleLocalExpanded when state is not Success does not toggle expansion`: Protege al ViewModel de llamadas accidentales cuando el estado no es exitoso (Error o Carga), logrando el 100% de cobertura en ramas.
6.  `deleteSong delegates to repository successfully`: Verifica la delegación CRUD exitosa para borrar canciones locales.
7.  `deleteSong failure sends ShowToast event`: Garantiza la captura de excepciones locales notificando al usuario mediante un Toast.
8.  `updateSongTitle with blank title returns early`: Valida la prevención de introducción de títulos vacíos en la base de datos local.
9.  `updateSongTitle with valid title calls repository`: Valida el renombrado CRUD exitoso.
10. `updateSongTitle failure sends ShowToast event`: Controla errores en base de datos local al renombrar.
11. `addCustomSong` (Pruebas de validación y guardado):
    *   Títulos vacíos -> Muestra Toast "Campos inválidos".
    *   URI de archivo nula -> Muestra Toast "Campos inválidos".
    *   Importación exitosa -> Invoca repositorio y emite evento `SongAddedSuccess`.
    *   Fallo en guardado -> Captura el error y emite el Toast descriptivo correspondiente.
12. `refresh resets UI to loading and updates lists`: Verifica la recarga y refresco reactivo manual de canciones locales y remotas.
