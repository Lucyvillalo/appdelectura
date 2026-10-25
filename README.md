# Mi biblioteca lectora

Aplicación Android en Kotlin con Jetpack Compose y Material 3 para gestionar libros, progreso, metas anuales y estadísticas. Los datos se guardan en Firebase Realtime Database dentro del espacio privado de cada usuario.

## Requisitos
- Android Studio con Android SDK 37 y JDK 17.
- Firebase project con una app Android cuyo identificador sea `com.example.desafio3dsm`.

## Configurar Firebase
1. Crea un proyecto en https://console.firebase.google.com/.
2. Añade una aplicación Android con paquete `com.example.desafio3dsm`.
3. Descarga `google-services.json` y cópialo en `app/google-services.json`.
4. En Authentication > Sign-in method, habilita Email/Password.
5. En Realtime Database, crea la base de datos y publica las reglas de `database.rules.json`.
6. Sincroniza Gradle desde Android Studio.

La aplicación activa la persistencia local de Realtime Database al iniciar. La base de datos almacena metas en `/users/{uid}/goals/{year}` y libros en `/users/{uid}/books/{bookId}`. No incluyas el archivo `google-services.json` en repositorios públicos si contiene configuración propia de tu proyecto.

## Ejecutar
Abre esta carpeta en Android Studio, espera la sincronización de Gradle y ejecuta `app` en un emulador o dispositivo Android 7.0 (API 24) o superior.

## Pruebas manuales
1. Registra una cuenta con correo válido, contraseña de seis caracteres y confirmación coincidente; revisa también las validaciones de formato y contraseña.
2. Cierra y vuelve a abrir la app: la sesión debe continuar. Cierra sesión desde Inicio y comprueba el retorno al acceso.
3. Define una meta inválida (vacía, decimal o cero) y una meta válida mayor que cero.
4. Crea un libro comprobando título, autor, páginas, género y estado; intenta guardar datos inválidos y confirma los mensajes por campo.
5. Marca un libro Terminado: debe exigir calificación; selecciona estrellas y guarda. Cambia luego su estado y confirma que no conserva fecha ni calificación.
6. Edita y elimina libros; revisa progreso, estados y estrellas en Biblioteca.
7. Termina libros de distintos géneros y revisa cantidades, empates, páginas y mejor calificación en Estadísticas.
8. Inicia sesión con otra cuenta para confirmar que cada usuario ve sus propios datos.
# appdelectura
