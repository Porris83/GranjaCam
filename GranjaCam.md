# GranjaCam (Versión Beta)

Aplicación Android que utiliza la cámara del dispositivo para detectar rostros y tomar fotografías automáticamente.  
Cuando detecta un rostro nuevo o cuando el mismo rostro permanece en el encuadre durante **40 segundos**, la aplicación captura una imagen.

> ⚠️ **Proyecto en fase beta. Algunas funciones aún están en desarrollo o no funcionan del todo correctamente.**

---

## Funcionalidades actuales

- Activación automática de la cámara trasera al iniciar la aplicación.
- Detección de rostros en tiempo real mediante **ML Kit**.
- Visualización de rostros detectados con superposición visual (overlay).
- Toma automática de fotografías cuando:
  - Se detecta un rostro nuevo.
  - El mismo rostro permanece en la vista durante 40 segundos.
- Efecto de sonido al capturar imágenes.
- Almacenamiento de fotografías en la galería del dispositivo.

---

## Próximas mejoras

- Integración con modelo entrenado personalizado *(modelo en desarrollo,  ver [modelo en este repositorio](https://github.com/Porris83/   Modelo-Clasificador-de-cerdos))*.
- Mejoras en la precisión de detección.
- Optimización del rendimiento en dispositivos de gama baja.
- Posibilidad de configurar parámetros de detección.
- Interfaz de usuario mejorada.

---

## Requisitos técnicos

- Android 7.0 (API 24) o superior
- Permisos de cámara
- Permisos de almacenamiento

---

## Estructura del proyecto

- `MainActivity.kt`: Actividad principal que gestiona la cámara y detección.
- `Overlay.kt`: Vista personalizada para visualizar rostros detectados.
- `FaceDetectionViewModel.kt`: ViewModel para la lógica de detección con ML Kit.
- `camera_sound.mp3`: Sonido que se reproduce al tomar fotografías.

---

## Instalación

1. Clona este repositorio.
2. Abre el proyecto en Android Studio.
3. Sincroniza con Gradle.
4. Ejecuta la aplicación en un dispositivo real (recomendado) o emulador.

---

## Permisos requeridos

La aplicación solicitará los siguientes permisos:

- Cámara (`android.permission.CAMERA`)
- Almacenamiento (`android.permission.WRITE_EXTERNAL_STORAGE`)

---

## Autor

**Ariel Vilche**  
Estudiante de 2° año - Tecnicatura Universitaria en Desarrollo de Aplicaciones Móviles  
Proyecto personal con fines de aprendizaje y portfolio.

---

## Licencia

> Este proyecto no tiene licencia definida aún.

---

> ⚠️ **Nota importante:** Esta aplicación está en desarrollo activo y algunas funcionalidades pueden presentar comportamientos inesperados.