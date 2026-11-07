# 5CINCO – Sistema de gestión

Aplicativo de escritorio para apoyar la gestión de **5CINCO-PROYECTOS Y SERVICIOS S.A.S.**: clientes, proyectos (propuestas), contratos, empleados, proveedores y costos.

Proyecto integrador – Tecnología en Desarrollo de Sistemas Informáticos, Unidades Tecnológicas de Santander (UTS), Bucaramanga. Feria Systegra UTS 2026.

**Integrantes:** Jose Fernando Herreño Acuña, Andrés Eduardo Hernández Ochoa, Miguel Ángel Herrera Viviescas.
**Docente:** Elsa Patricia Carvajal Valero.

## Descripción

Reemplaza el manejo del negocio en hojas de cálculo por una base de datos relacional con interfaz de escritorio. Funcionalidades principales:

- Inicio de sesión con contraseñas guardadas como hash (PBKDF2-HMAC-SHA256) y dos roles: **superusuario** (datos sensibles, eliminación y gestión de usuarios) y **administrador**.
- Módulos de registro, consulta, edición y eliminación: clientes, proyectos, contratos, códigos de compra UNSPSC, productos, proveedores, empleados, contratistas y salario mínimo.
- Asignación de empleados y contratistas por contrato.
- **Calcular valores:** subtotal, descuento, IVA, retefuente, reteICA y reteIVA por línea, con totales por proyecto o contrato.
- Valor de participación y su equivalente en SMMLV según el año de terminación del contrato.
- Reportes de contratos por año o rango de fechas, con exportación a Excel.
- Gestión de usuarios (crear, cambiar rol, activar/desactivar, restablecer clave).

## Tecnologías utilizadas

- Java 17 y Swing (interfaz gráfica)
- SQLite 3 mediante JDBC (`sqlite-jdbc-3.46.1.3.jar`, en `lib/`)
- Apache Ant / Apache NetBeans
- PlantUML (diagramas del modelo entidad-relación)

> Durante la planeación se consideró PostgreSQL, pero el código está configurado únicamente para SQLite.

## Demo web (enlace)

La aplicación real es de escritorio, pero en la carpeta [`web/`](web/) hay una **demo web** con datos ficticios, pensada para abrirse desde el navegador o el celular (por ejemplo, con un QR). Incluye los mismos módulos y reglas de cálculo, con la base `demo/5CINCO_demo.db`; **no usa la base real**.

- Enlace: `[https://TU-SERVICIO.onrender.com](https://sistema-gestion-5cinco.onrender.com)` 
- Usuarios de prueba: ver la tabla de más abajo.
- Cómo publicarla y ejecutarla: ver [`web/README.md`](web/README.md).

## Instrucciones de ejecución

Requisitos: JDK 17 o superior y Apache NetBeans (o Ant).

1. Clona el repositorio y ábrelo en NetBeans (*Archivo > Abrir proyecto*).
2. **Base de datos de ejemplo:** copia `demo/5CINCO_demo.db` a la raíz del proyecto con el nombre `5CINCO.db`.
3. Ejecuta el proyecto completo con **F6** (clase principal `pkg5cinco.Main`). No ejecutes `Login.java` directamente, porque no tiene método `main`.

También se puede ejecutar con Ant: `ant run`.

### Usuarios de la base de ejemplo

| Usuario | Clave | Rol |
|---|---|---|
| `demo` | `demo1234` | Superusuario |
| `admin_demo` | `admin1234` | Administrador |

## Base de datos de ejemplo (NO es la real)

La base real de la empresa (`5CINCO.db`) **no se incluye** en este repositorio porque contiene datos de clientes, contratos, proveedores, empleados y documentos PDF; por eso está en `.gitignore`.

En su lugar, `demo/5CINCO_demo.db` (y su versión en texto `demo/5CINCO_demo.sql`) es una base **ficticia** con la misma estructura y el mismo funcionamiento que la real: 14 tablas, vistas y reglas de cálculo. Todos los nombres, documentos, teléfonos, correos (`@ejemplo.test`) y valores son inventados; los usuarios y claves de la tabla anterior son solo de demostración. Incluye 5 clientes, 5 proyectos, 3 contratos con líneas de cálculo, 8 productos, empleados, contratistas, proveedores, asignaciones y el salario mínimo por año. Se puede modificar libremente sin afectar datos reales.

## Estructura

```
├── build.xml, manifest.mf, nbproject/   Proyecto Ant / NetBeans
├── lib/                                 sqlite-jdbc
├── demo/                                Base de datos ficticia (.db y .sql)
├── web/                                 Demo web (Java + Docker, para Render)
└── src/
    ├── pkg5cinco/   Punto de entrada (Main)
    ├── datos/       Conexión, autenticación, sesión y acceso a datos
    ├── interfaz/    Pantallas y módulos (Swing)
    ├── reportes/    Reporte de contratos y exportación a Excel
    └── imagenes/    Recursos gráficos
```
