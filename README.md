# 5CINCO – Sistema de gestión

[![Demo web](https://img.shields.io/badge/Demo%20web-en%20l%C3%ADnea-8fb81e)](https://sistema-gestion-5cinco.onrender.com)
![Java 17](https://img.shields.io/badge/Java-17-orange)
![SQLite](https://img.shields.io/badge/SQLite-3-blue)

Aplicativo para apoyar la gestión de **5CINCO-PROYECTOS Y SERVICIOS S.A.S.**: clientes, proyectos (propuestas), contratos, empleados, proveedores y costos. Tiene una **versión de escritorio** (la aplicación real) y una **demo web** con datos ficticios.

Proyecto integrador – Tecnología en Desarrollo de Sistemas Informáticos, Institución Universitaria Tecnológica de Santander (UTS), Bucaramanga. Feria Systegra UTS 2026.

**Integrantes:** Jose Fernando Herreño Acuña, Andrés Eduardo Hernández Ochoa, Miguel Ángel Herrera Viviescas.
**Docentes:** Elsa Patricia Carvajal Valero (Docente de Planeación de Sistemas Informáticos), Laura Cristina Duarte Quintero (Docente de Motores de Bases de Datos).

## 🌐 Demo en línea

**👉 https://sistema-gestion-5cinco.onrender.com**

Funciona desde el computador o el celular, sin instalar nada. Todos los datos son **ficticios**; lo que cambies se restaura al reiniciar la demo.

| Usuario | Clave | Rol |
|---|---|---|
| `demo` | `demo1234` | Superusuario |
| `admin_demo` | `admin1234` | Administrador |

> La demo está en un plan gratuito: si nadie la ha abierto en un rato, la primera carga puede tardar cerca de un minuto en "despertar".

## Descripción

Reemplaza el manejo del negocio en hojas de cálculo por una base de datos relacional con interfaz de escritorio. Funcionalidades principales:

- Inicio de sesión con contraseñas guardadas como hash (PBKDF2-HMAC-SHA256) y dos roles: **superusuario** (datos sensibles, eliminación y gestión de usuarios) y **administrador**.
- Módulos de registro, consulta, edición y eliminación: clientes, proyectos, contratos, códigos de compra UNSPSC, productos, proveedores, empleados, contratistas y salario mínimo.
- Asignación de empleados y contratistas por contrato.
- **Calcular valores:** subtotal, descuento, IVA, retefuente, reteICA y reteIVA por línea, con totales por proyecto o contrato.
- Valor de participación y su equivalente en SMMLV según el año de terminación del contrato.
- Reportes de contratos por año o rango de fechas, con exportación a Excel.
- Gestión de usuarios (crear, cambiar rol, activar/desactivar, restablecer clave).

## Versiones del aplicativo

| | Escritorio (real) | Demo web |
|---|---|---|
| Carpeta | `src/` | `web/` |
| Interfaz | Java Swing | Navegador / celular |
| Base de datos | `5CINCO.db` (privada de la empresa) | `demo/5CINCO_demo.db` (ficticia) |
| Módulos y cálculos | Completos | Los mismos, salvo los PDF de contratos |
| Publicación | Local (NetBeans / Ant) | Render (Docker) |

## Tecnologías utilizadas

- Java 17
- Swing (escritorio) y servidor HTTP del JDK + HTML/CSS responsive (demo web)
- SQLite 3 mediante JDBC (`sqlite-jdbc-3.46.1.3.jar`, en `lib/`)
- Apache Ant / Apache NetBeans
- Docker y Render (despliegue de la demo)
- PlantUML (diagramas del modelo entidad-relación)

> Durante la planeación se consideró PostgreSQL, pero el código está configurado únicamente para SQLite.

## Instrucciones de ejecución (escritorio)

Requisitos: JDK 17 o superior y Apache NetBeans (o Ant).

1. Clona el repositorio y ábrelo en NetBeans (*Archivo > Abrir proyecto*).
2. **Base de datos de ejemplo:** copia `demo/5CINCO_demo.db` a la raíz del proyecto con el nombre `5CINCO.db`.
3. Ejecuta el proyecto completo con **F6** (clase principal `pkg5cinco.Main`). No ejecutes `Login.java` directamente, porque no tiene método `main`.

También se puede ejecutar con Ant: `ant run`.

Para correr o publicar la demo web, ver [`web/README.md`](web/README.md).

## Base de datos de ejemplo (NO es la real)

La base real de la empresa (`5CINCO.db`) **no se incluye** en este repositorio porque contiene datos de clientes, contratos, proveedores, empleados y documentos PDF; por eso está en `.gitignore`.

En su lugar, `demo/5CINCO_demo.db` (y su versión en texto `demo/5CINCO_demo.sql`) es una base **ficticia** con la misma estructura y el mismo funcionamiento que la real: 14 tablas, vistas y reglas de cálculo. Todos los nombres, documentos, teléfonos, correos (`@ejemplo.test`) y valores son inventados; los usuarios y claves de la tabla de arriba son solo de demostración. Incluye 5 clientes, 5 proyectos, 3 contratos con líneas de cálculo, 8 productos, empleados, contratistas, proveedores, asignaciones y el salario mínimo por año. Se puede modificar libremente sin afectar datos reales.

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
