# 02 · Grabar el journey y abrir el Recorder (Mac y Windows)

El Recorder de Gatling convierte una grabación del navegador en una simulation en Java. Aquí se usa el modo **HAR Converter**: se graba el journey en Chrome, se exporta un archivo `.har` y el Recorder lo traduce a código.

## 1. Requisitos

- JDK 17 o superior y Maven 3.9+ en el PATH.
- Una **sesión de escritorio**: el Recorder es una ventana gráfica y no funciona por SSH o en un servidor sin pantalla.

### Mac (Terminal)

```bash
java -version
mvn -v
# Si Maven usa un JDK equivocado:
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

### Windows (PowerShell)

```powershell
java -version
mvn -v
# Si falta JAVA_HOME (ajusta la ruta a tu JDK y abre una terminal nueva):
setx JAVA_HOME "C:\Program Files\Java\jdk-21"
```

Maven se descarga de <https://maven.apache.org/download.cgi>; descomprímelo y agrega su carpeta `bin` al PATH.

> Los comandos y capturas de este repo se probaron en Mac. En Windows el comando es el mismo; solo cambian las rutas y el uso de comillas en PowerShell.

## 2. Grabar el HAR en Chrome

1. Abre una pestaña nueva y las herramientas de desarrollo: `F12` (Windows) o `Cmd + Option + I` (Mac).
2. Pestaña **Network**. Marca **Preserve log** y limpia la lista.
3. Recorre el journey completo: Home → buscar producto → agregar al carro → checkout → los 4 pasos.
4. Clic derecho sobre la lista de requests → **Save all as HAR with content**.

Recomendaciones:
- **Usa datos ficticios** al llenar el formulario. El HAR guarda lo que escribes (nombre, dirección, teléfono) y las cookies de sesión.
- **No subas el HAR al repositorio.**
- Si el sitio te rechaza un dato (por ejemplo un código postal inválido), se grabará también el intento fallido. Se limpia después, ver [03-paso-a-paso.md](03-paso-a-paso.md).

## 3. Abrir el Recorder

Desde la carpeta del proyecto (donde está el `pom.xml`), en Mac o Windows:

```bash
mvn gatling:recorder
```

La primera vez descarga dependencias. Se abre la ventana **Gatling Recorder – Configuration**.

## 4. Configuración recomendada (salida limpia)

| Campo | Valor |
|---|---|
| Recorder mode | **HAR Converter** |
| HAR File | Ruta del `.har`. Mac: `/Users/<tu-usuario>/Desktop/sitio.har`. Windows: `C:\Users\<tu-usuario>\Downloads\sitio.har` |
| Package | `onlineshop` |
| Class Name | `RecordedSimulation` (distinto del nombre de tu solución, para no sobrescribirla) |
| Format | Java 17 |
| Follow Redirects | ✓ (junta cada 302 con su redirección en un solo request) |
| Infer HTML resources | ✗ (si no, agrupa los estáticos en un bloque `.resources(...)`) |
| Automatic Referers | ✓ |
| Remove cache headers | ✓ |
| Use Class Name as request prefix / HTTP method and URI as postfix | ✗ |
| Save & check response bodies | ✗ (genera archivos de respuesta y checks de cuerpo completo) |
| Simulations folder | Mac: `.../gatling-onlineshop/src/test/java`. Windows: `...\gatling-onlineshop\src\test\java` |
| Encoding | UTF-8 |
| Save preferences | ✓ si vas a repetir la importación |

### Filtros

Marca **Enable Filters** y configura:

- **AllowList** (`+`): `.*testingyes\.com/onlineshop/.*`
- **DenyList**: pulsa **No static resources** (agrega css, js, imágenes y fuentes) y suma con `+`:
  - `.*/module/ps_facebook/.*`
  - `.*/module/blockwishlist/.*`
  - `.*/module/productcomments/.*`

Esos tres módulos de PrestaShop hacen llamadas AJAX de fondo y no son parte del journey.

Pulsa **Start !**. El archivo queda en `src/test/java/onlineshop/RecordedSimulation.java`.

## 5. Modo proxy (alternativa)

En **Recorder mode** elige **HTTP Proxy** (puerto 8000 por defecto), configura el proxy del navegador en `localhost:8000`, recorre el journey y pulsa **Stop & Save**. Para sitios HTTPS hay que instalar el certificado del Recorder en el navegador. Con el HAR no hace falta ni lo uno ni lo otro, por eso es el modo de este material.

Referencia: [Recorder](https://docs.gatling.io/reference/script/http/recorder/).

## 6. Ejecutar el código generado

```bash
mvn gatling:test -Dgatling.simulationClass=onlineshop.RecordedSimulation
```

En PowerShell: `mvn gatling:test "-Dgatling.simulationClass=onlineshop.RecordedSimulation"`.
