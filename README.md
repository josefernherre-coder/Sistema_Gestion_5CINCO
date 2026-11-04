# 5CINCO – Sistema de gestión

Aplicación de escritorio desarrollada en Java para apoyar los procesos de gestión de 5CINCO-PROYECTOS Y SERVICIOS S.A.S.

## Tecnologías identificadas en esta versión

- Java 17
- Apache NetBeans (proyecto Java Ant)
- SQLite mediante JDBC (`sqlite-jdbc-3.46.1.3.jar`)

> **Importante:** esta versión del código se conecta a SQLite mediante el archivo `5CINCO.db`. Aunque en la planeación académica se había mencionado PostgreSQL, el código incluido en este ZIP no está configurado para PostgreSQL.

## Abrir el proyecto

1. Descarga o clona este repositorio.
2. En NetBeans, selecciona **File > Open Project**.
3. Selecciona la carpeta `5CINCO`.
4. Verifica que el proyecto use JDK 17.
5. Coloca una copia autorizada de `5CINCO.db` en la carpeta raíz del proyecto para ejecutar la aplicación.

## Base de datos y privacidad

El archivo `5CINCO.db` no se incluye en el repositorio porque la copia entregada contiene registros de clientes, contratos, proveedores, empleados y documentos PDF. No publiques esa base de datos en GitHub, incluso si el repositorio es privado, sin autorización de la empresa.

Solicita una copia de desarrollo autorizada o una base de datos anonimizada para compartir con el equipo. El archivo local `5CINCO.db` está excluido mediante `.gitignore`.

## Dependencia JDBC

El proyecto incluye `lib/sqlite-jdbc-3.46.1.3.jar` y la configuración de NetBeans referencia esa biblioteca.

## Estructura principal

- `src/pkg5cinco/`: punto de entrada de la aplicación.
- `src/datos/`: conexión, autenticación, sesión y acceso a datos.
- `src/interfaz/`: ventanas, paneles y componentes de la interfaz.
- `src/reportes/`: generación de reportes.
- `src/imagenes/`: recursos gráficos.
- `lib/`: bibliotecas externas.
- `nbproject/`: configuración del proyecto NetBeans.


## Equipo

Jose Fernando Herreño Acuña

Andrés Eduardo Hernández Ochoa

Miguel Ángel Herrera Viviescas
