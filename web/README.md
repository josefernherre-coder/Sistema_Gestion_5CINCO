# 5CINCO – Demo web

Versión web **de demostración** de 5CINCO, pensada para abrirse desde el navegador o el celular (por ejemplo, escaneando un QR). La aplicación real de la empresa es de escritorio (ver el README de la raíz del repositorio).

> **Todos los datos son ficticios.** La base de datos de la empresa no está en este repositorio ni en el servidor.

## Qué incluye
- Inicio de sesión (claves guardadas como hash PBKDF2) con roles superusuario y administrador.
- Módulos de **clientes, proyectos (propuestas), contratos, códigos de compra, productos, proveedores, empleados, contratistas, asignaciones y salario mínimo**: consultar, buscar, crear, editar y eliminar (eliminar solo para el superusuario).
- Los datos sensibles de empleados (salario, EPS, ARL, pensión y caja de compensación) solo los ve y edita el superusuario.
- Contratos: si se deja vacío el valor y se elige un proyecto de origen, el valor sale del total con IVA de las líneas del proyecto. Cada contrato tiene su pantalla de **códigos de compra** (los de los productos usados se asignan solos).
- **Calcular valores** por proyecto o contrato: subtotal, descuento, IVA, retefuente, reteICA, reteIVA y neto a recibir. Un contrato ligado a un proyecto muestra las líneas del proyecto y se pueden copiar para editarlas.
- **Reportes** de contratos por rango de fechas o por año, con totales y **descarga en Excel** (hojas: Contratos, Resumen por año y Códigos por contrato).
- **Usuarios** (solo superusuario): crear, cambiar rol, activar/desactivar, restablecer clave y eliminar.
- Botón para **restaurar los datos de ejemplo** (superusuario).
- No incluye los PDF de contratos (en la app de escritorio se guardan dentro de la base).

## Usuarios de prueba
| Usuario | Clave | Rol |
|---|---|---|
| `demo` | `demo1234` | Superusuario |
| `admin_demo` | `admin1234` | Administrador |

## Tecnologías
Java 17 (servidor HTTP del JDK, sin frameworks), SQLite con sqlite-jdbc, HTML/CSS responsive, Docker, Render.

## Ejecutar localmente
Requiere JDK 17.
```bash
cd web
mkdir out
javac -encoding UTF-8 -cp "lib/*" -d out $(find src -name '*.java')
java -cp "out:lib/*" web.Main          # en Windows: java -cp "out;lib/*" web.Main
```
Abre http://localhost:8080 (o el puerto de la variable `PORT`).

Al iniciar, la app copia `demo/5CINCO_demo.db` a `data/5CINCO.db` y trabaja sobre esa copia, así que cada reinicio vuelve a los datos de ejemplo.

## Publicar en Render
1. Sube esta carpeta (`web/`) al repositorio de GitHub.
2. En Render: **New → Web Service** y conecta el repositorio.
3. **Root Directory:** `web` · **Language/Runtime:** Docker · **Instance type:** Free.
4. Pulsa **Create Web Service**. Cuando termine, Render muestra el enlace público (`https://….onrender.com`).
5. Prueba el enlace desde un celular y genera el QR con esa dirección.

En el plan gratuito, la página se duerme si nadie entra y tarda cerca de un minuto en despertar: ábrela antes de la exposición. Al despertar, los datos de ejemplo se restauran.

## Estructura
```
web/
├── Dockerfile
├── lib/sqlite-jdbc-3.46.1.3.jar
├── demo/5CINCO_demo.db         Base ficticia
└── src/web/                    Main (rutas), Db, Auth, Html, Mod (CRUD genérico), Calc (cálculos),
                                Extras (códigos, reportes, usuarios), Reporte, Xlsx, Res
```
