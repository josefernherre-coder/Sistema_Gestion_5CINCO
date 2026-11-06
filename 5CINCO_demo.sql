BEGIN TRANSACTION;
CREATE TABLE asignaciones (id_asignacion INTEGER PRIMARY KEY AUTOINCREMENT, id_contrato INTEGER NOT NULL REFERENCES contratos(id_contrato) ON DELETE CASCADE, id_empleado INTEGER REFERENCES empleados(id_empleado), id_contratista INTEGER REFERENCES contratistas(id_contratista), actividad VARCHAR, fecha_inicio DATE, fecha_fin DATE, valor_pagado NUMERIC CHECK (valor_pagado IS NULL OR valor_pagado >= 0), CHECK ((id_empleado IS NULL) <> (id_contratista IS NULL)), CHECK (valor_pagado IS NULL OR id_contratista IS NOT NULL));
INSERT INTO "asignaciones" VALUES(1,1,1,NULL,'Coordinación','2025-04-01','2025-05-15',NULL);
INSERT INTO "asignaciones" VALUES(2,1,NULL,1,'Carpintería','2025-04-05','2025-05-10',800000);
INSERT INTO "asignaciones" VALUES(3,3,2,NULL,'Instalación','2026-03-05',NULL,NULL);
CREATE TABLE clientes (
    id_cliente          INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre              VARCHAR NOT NULL,
    tipo_cliente        VARCHAR CHECK (tipo_cliente IN
                        ('publica','privada','persona_natural','consorcio_union_temporal')),
    tipo_documento      VARCHAR NOT NULL DEFAULT 'NIT' CHECK (tipo_documento IN ('NIT','CC')),
    numero_documento    VARCHAR UNIQUE,            -- NULL permitido (CEINCOAS no trae NIT)
    digito_verificacion VARCHAR,
    telefono            VARCHAR,
    correo              VARCHAR,
    direccion           VARCHAR
);
INSERT INTO "clientes" VALUES(1,'ALCALDÍA MUNICIPAL DE EJEMPLO','publica','NIT','900000001','1','6000001','contacto@ejemplo.test','Calle 1 # 1-01');
INSERT INTO "clientes" VALUES(2,'COLEGIO DEMO SAN JOSÉ','privada','NIT','900000002','2','6000002','colegio@ejemplo.test','Carrera 2 # 2-02');
INSERT INTO "clientes" VALUES(3,'EMPRESA DEMO S.A.S.','privada','NIT','900000003','3','6000003','info@ejemplo.test','Avenida 3 # 3-03');
INSERT INTO "clientes" VALUES(4,'PERSONA DE PRUEBA UNO','persona_natural','CC','10000001',NULL,'3000000001','prueba1@ejemplo.test','Calle 4 # 4-04');
INSERT INTO "clientes" VALUES(5,'UNIÓN TEMPORAL DEMO 2026','consorcio_union_temporal','NIT','900000005','5','6000005','ut@ejemplo.test','Carrera 5 # 5-05');
CREATE TABLE codigos_compra (id_codigo INTEGER PRIMARY KEY AUTOINCREMENT, codigo VARCHAR NOT NULL UNIQUE, descripcion VARCHAR NOT NULL, tipo VARCHAR CHECK (tipo IS NULL OR tipo IN ('bien','material','servicio')), grupo VARCHAR);
INSERT INTO "codigos_compra" VALUES(1,'30101500','Ángulos','material','Componentes y suministros de construcción');
INSERT INTO "codigos_compra" VALUES(2,'30171500','Puertas','material','Componentes y suministros de construcción');
INSERT INTO "codigos_compra" VALUES(3,'56101500','Muebles','bien','Muebles y mobiliario');
INSERT INTO "codigos_compra" VALUES(4,'56101700','Muebles de oficina','bien','Muebles y mobiliario');
INSERT INTO "codigos_compra" VALUES(5,'56111500','Sitios de trabajo y paquetes para la oficina','bien','Muebles y mobiliario');
INSERT INTO "codigos_compra" VALUES(6,'56111700','Casegood y sistemas no modulares','bien','Muebles y mobiliario');
INSERT INTO "codigos_compra" VALUES(7,'56112200','Sistemas de escritorios','bien','Muebles y mobiliario');
INSERT INTO "codigos_compra" VALUES(8,'56121700','Módulos de almacenamiento general y de libros para las aulas','bien','Muebles y mobiliario');
INSERT INTO "codigos_compra" VALUES(9,'72153500','Servicios de limpieza estructural externa','servicio','Construcción y mantenimiento de edificaciones');
CREATE TABLE contratistas (
    id_contratista     INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre             VARCHAR NOT NULL,
    tipo_documento     VARCHAR NOT NULL DEFAULT 'CC',
    numero_documento   VARCHAR NOT NULL UNIQUE,
    actividad          VARCHAR,
    telefono           VARCHAR,
    correo             VARCHAR,
    estado             VARCHAR NOT NULL DEFAULT 'activo' CHECK (estado IN ('activo','inactivo'))
);
INSERT INTO "contratistas" VALUES(1,'CONTRATISTA DEMO UNO','CC','30000001','CARPINTERO','3000000021',NULL,'activo');
INSERT INTO "contratistas" VALUES(2,'CONTRATISTA DEMO DOS','CC','30000002','PINTOR','3000000022',NULL,'activo');
INSERT INTO "contratistas" VALUES(3,'CONTRATISTA DEMO TRES','CC','30000003','INSTALADOR','3000000023',NULL,'activo');
CREATE TABLE contrato_codigos (id_contrato INTEGER NOT NULL REFERENCES contratos(id_contrato) ON DELETE CASCADE, id_codigo INTEGER NOT NULL REFERENCES codigos_compra(id_codigo), PRIMARY KEY (id_contrato, id_codigo));
INSERT INTO "contrato_codigos" VALUES(1,1);
INSERT INTO "contrato_codigos" VALUES(1,2);
INSERT INTO "contrato_codigos" VALUES(1,3);
INSERT INTO "contrato_codigos" VALUES(2,5);
INSERT INTO "contrato_codigos" VALUES(2,2);
INSERT INTO "contrato_codigos" VALUES(3,4);
INSERT INTO "contrato_codigos" VALUES(3,7);
CREATE TABLE contrato_documentos (id_documento INTEGER PRIMARY KEY AUTOINCREMENT, id_contrato INTEGER NOT NULL REFERENCES contratos(id_contrato) ON DELETE CASCADE, nombre_archivo VARCHAR NOT NULL, descripcion VARCHAR, tamano INTEGER NOT NULL, fecha_carga DATE NOT NULL DEFAULT (date('now')), pdf BLOB NOT NULL);
CREATE TABLE contratos (
    id_contrato              INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo_interno           VARCHAR NOT NULL UNIQUE,
    numero_contrato          VARCHAR,
    consecutivo_rup          INTEGER,
    id_cliente               INTEGER NOT NULL REFERENCES clientes(id_cliente),
    id_proyecto              INTEGER UNIQUE REFERENCES proyectos(id_proyecto),
    tipo_contrato            VARCHAR,
    objeto                   TEXT NOT NULL,
    figura_contratista       VARCHAR,
    porcentaje_participacion REAL NOT NULL DEFAULT 1
                             CHECK (porcentaje_participacion > 0 AND porcentaje_participacion <= 1),
    valor_contrato           NUMERIC NOT NULL CHECK (valor_contrato >= 0),
    valor_participacion      NUMERIC GENERATED ALWAYS AS
                             (ROUND(valor_contrato * porcentaje_participacion, 2)) STORED,
    fecha_inicio             DATE,
    fecha_inicio_texto       VARCHAR,
    fecha_terminacion        DATE,
    fecha_terminacion_texto  VARCHAR,
    ubicacion_obra           VARCHAR,
    estado                   VARCHAR NOT NULL DEFAULT 'en_ejecucion'
                             CHECK (estado IN ('en_ejecucion','terminado')),
    nombre_contratante_original   VARCHAR,
    nit_contratante_original      VARCHAR,
    telefono_contratante_original VARCHAR
);
INSERT INTO "contratos" VALUES(1,'SCINCO-D001','CD-001',NULL,1,1,'Suministro','Dotación de mobiliario para oficina (contrato demo)',NULL,1.0,13000000,'2025-04-01',NULL,'2025-05-15',NULL,NULL,'terminado',NULL,NULL,NULL);
INSERT INTO "contratos" VALUES(2,'SCINCO-D002','CD-002',NULL,2,2,'Suministro','Suministro de mobiliario escolar (contrato demo)',NULL,1.0,14000000,'2025-09-01',NULL,'2025-10-20',NULL,NULL,'terminado',NULL,NULL,NULL);
INSERT INTO "contratos" VALUES(3,'SCINCO-D003','CD-003',NULL,3,3,'Suministro e instalación','Adecuación de sala de juntas (contrato demo)',NULL,0.5,5000000,'2026-03-01',NULL,NULL,NULL,NULL,'en_ejecucion',NULL,NULL,NULL);
CREATE TABLE detalle (
    id_detalle      INTEGER PRIMARY KEY AUTOINCREMENT,
    id_proyecto     INTEGER REFERENCES proyectos(id_proyecto) ON DELETE CASCADE,
    id_contrato     INTEGER REFERENCES contratos(id_contrato) ON DELETE CASCADE,
    id_producto     INTEGER NOT NULL REFERENCES productos(id_producto),
    cantidad        REAL    NOT NULL CHECK (cantidad > 0),
    precio_unitario NUMERIC NOT NULL CHECK (precio_unitario >= 0),
    subtotal        NUMERIC GENERATED ALWAYS AS (ROUND(cantidad * precio_unitario, 2)) STORED,
    descuento_pct   REAL NOT NULL DEFAULT 0 CHECK (descuento_pct BETWEEN 0 AND 100),
    iva_pct         REAL NOT NULL DEFAULT 19 CHECK (iva_pct BETWEEN 0 AND 100),
    retefuente_pct  REAL NOT NULL DEFAULT 0 CHECK (retefuente_pct BETWEEN 0 AND 100),
    reteica_pct     REAL NOT NULL DEFAULT 0 CHECK (reteica_pct BETWEEN 0 AND 100),
    reteiva_pct     REAL NOT NULL DEFAULT 0 CHECK (reteiva_pct BETWEEN 0 AND 100),  -- % sobre el IVA
    CHECK ((id_proyecto IS NULL) <> (id_contrato IS NULL))
);
INSERT INTO "detalle" VALUES(1,1,NULL,1,10.0,450000,0.0,19.0,2.5,0.414,15.0);
INSERT INTO "detalle" VALUES(2,1,NULL,2,10.0,320000,0.0,19.0,2.5,0.414,15.0);
INSERT INTO "detalle" VALUES(3,1,NULL,3,2.0,680000,5.0,19.0,2.5,0.414,15.0);
INSERT INTO "detalle" VALUES(4,2,NULL,5,20.0,370000,0.0,19.0,0.0,0.0,0.0);
INSERT INTO "detalle" VALUES(5,2,NULL,2,20.0,320000,0.0,19.0,0.0,0.0,0.0);
INSERT INTO "detalle" VALUES(6,3,NULL,4,3.0,900000,0.0,19.0,2.5,0.414,15.0);
INSERT INTO "detalle" VALUES(7,3,NULL,7,1.0,400000,0.0,19.0,4.0,0.966,15.0);
INSERT INTO "detalle" VALUES(8,4,NULL,6,12.0,85000,0.0,19.0,0.0,0.0,0.0);
INSERT INTO "detalle" VALUES(9,4,NULL,8,10.0,65000,0.0,19.0,0.0,0.0,0.0);
INSERT INTO "detalle" VALUES(10,5,NULL,3,5.0,680000,0.0,19.0,2.5,0.414,15.0);
CREATE TABLE empleados (
    id_empleado        INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre             VARCHAR NOT NULL,
    tipo_documento     VARCHAR NOT NULL DEFAULT 'CC',
    numero_documento   VARCHAR NOT NULL UNIQUE,
    cargo              VARCHAR,
    telefono           VARCHAR,
    correo             VARCHAR,
    direccion          VARCHAR,
    fecha_ingreso      DATE,
    salario            NUMERIC,                    -- sensible
    eps                VARCHAR,                    -- sensible
    arl                VARCHAR,                    -- sensible
    fondo_pension      VARCHAR,                    -- sensible
    caja_compensacion  VARCHAR,                    -- sensible
    estado             VARCHAR NOT NULL DEFAULT 'activo' CHECK (estado IN ('activo','inactivo'))
);
INSERT INTO "empleados" VALUES(1,'EMPLEADA DE PRUEBA UNO','CC','20000001','Administradora','3000000011','empleada1@ejemplo.test',NULL,'2026-01-15',2000000,'EPS DEMO','ARL DEMO','PENSIÓN DEMO','CAJA DEMO','activo');
INSERT INTO "empleados" VALUES(2,'EMPLEADO DE PRUEBA DOS','CC','20000002','Obrero','3000000012','empleado2@ejemplo.test',NULL,'2026-02-01',1800000,'EPS DEMO','ARL DEMO','PENSIÓN DEMO','CAJA DEMO','activo');
CREATE TABLE productos (
    id_producto      INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo           VARCHAR UNIQUE,
    nombre           VARCHAR NOT NULL,
    descripcion      TEXT,
    tipo             VARCHAR NOT NULL CHECK (tipo IN ('mueble','material','servicio')),
    unidad_medida    VARCHAR NOT NULL DEFAULT 'unidad',   -- unidad, m2, metro lineal, global...
    precio_referencia NUMERIC CHECK (precio_referencia IS NULL OR precio_referencia >= 0),
    estado           VARCHAR NOT NULL DEFAULT 'activo' CHECK (estado IN ('activo','inactivo'))
, id_codigo INTEGER REFERENCES codigos_compra(id_codigo));
INSERT INTO "productos" VALUES(1,'PRD-0001','Escritorio en melamina 120x60',NULL,'mueble','unidad',450000,'activo',1);
INSERT INTO "productos" VALUES(2,'PRD-0002','Silla ergonómica con ruedas',NULL,'mueble','unidad',320000,'activo',2);
INSERT INTO "productos" VALUES(3,'PRD-0003','Archivador metálico 4 gavetas',NULL,'mueble','unidad',680000,'activo',3);
INSERT INTO "productos" VALUES(4,'PRD-0004','Biblioteca en madera 180x90',NULL,'mueble','unidad',900000,'activo',4);
INSERT INTO "productos" VALUES(5,'PRD-0005','Mesa plegable tipo maleta',NULL,'mueble','unidad',370000,'activo',5);
INSERT INTO "productos" VALUES(6,'PRD-0006','Tablero en MDF 15 mm',NULL,'material','unidad',85000,'activo',6);
INSERT INTO "productos" VALUES(7,'PRD-0007','Instalación de mobiliario',NULL,'servicio','global',400000,'activo',7);
INSERT INTO "productos" VALUES(8,'PRD-0008','Pintura vinilo (galón)',NULL,'material','unidad',65000,'activo',8);
CREATE TABLE proveedores (
    id_proveedor           INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre                 VARCHAR NOT NULL,
    tipo_documento         VARCHAR NOT NULL DEFAULT 'NIT' CHECK (tipo_documento IN ('NIT','CC')),
    numero_documento       VARCHAR NOT NULL UNIQUE,
    digito_verificacion    VARCHAR,
    telefono               VARCHAR,
    direccion              VARCHAR,
    ubicacion              VARCHAR,                -- texto libre
    tipo_ubicacion         VARCHAR NOT NULL DEFAULT 'ciudad'
                           CHECK (tipo_ubicacion IN ('ciudad','departamento')),
    correo                 VARCHAR,
    descripcion_suministro VARCHAR,
    estado                 VARCHAR NOT NULL DEFAULT 'activo' CHECK (estado IN ('activo','inactivo'))
);
INSERT INTO "proveedores" VALUES(1,'PROVEEDOR DEMO MADERAS','NIT','800000001','1','6000101','Calle 10 # 10-10','Bucaramanga','ciudad','maderas@ejemplo.test','MADERA','activo');
INSERT INTO "proveedores" VALUES(2,'PROVEEDOR DEMO FERRETERÍA','NIT','800000002','2','6000102','Calle 11 # 11-11','Bucaramanga','ciudad','ferreteria@ejemplo.test','FERRETERÍA','activo');
INSERT INTO "proveedores" VALUES(3,'PROVEEDOR DEMO HERRAJES','NIT','800000003','3','6000103','Calle 12 # 12-12','Bogotá','ciudad','herrajes@ejemplo.test','HERRAJES','activo');
CREATE TABLE proyectos (
    id_proyecto       INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo            VARCHAR UNIQUE,
    nombre            VARCHAR NOT NULL,
    descripcion       TEXT,
    tipo_proyecto     VARCHAR NOT NULL CHECK (tipo_proyecto IN ('formal','independiente')),
    id_cliente        INTEGER REFERENCES clientes(id_cliente),   -- opcional
    fecha_presentacion DATE,
    ubicacion         VARCHAR,
    estado            VARCHAR NOT NULL DEFAULT 'propuesta'
                      CHECK (estado IN ('propuesta','ganado','no_ganado','en_ejecucion','terminado'))
);
INSERT INTO "proyectos" VALUES(1,'PRY-D001','Dotación de mobiliario para oficina (demo)',NULL,'formal',1,'2025-03-10','Ejemplo','terminado');
INSERT INTO "proyectos" VALUES(2,'PRY-D002','Mobiliario escolar (demo)',NULL,'formal',2,'2025-08-01','Ejemplo','terminado');
INSERT INTO "proyectos" VALUES(3,'PRY-D003','Adecuación de sala de juntas (demo)',NULL,'formal',3,'2026-02-15','Ejemplo','ganado');
INSERT INTO "proyectos" VALUES(4,'PRY-D004','Closets y cocina integral (demo)',NULL,'independiente',NULL,'2026-09-20','Ejemplo','propuesta');
INSERT INTO "proyectos" VALUES(5,'PRY-D005','Archivo y estantería (demo)',NULL,'formal',5,'2026-10-05','Ejemplo','propuesta');
CREATE TABLE salario_minimo (
    anio  INTEGER PRIMARY KEY,
    valor NUMERIC NOT NULL CHECK (valor > 0)
);
INSERT INTO "salario_minimo" VALUES(2016,689455);
INSERT INTO "salario_minimo" VALUES(2017,737717);
INSERT INTO "salario_minimo" VALUES(2018,781242);
INSERT INTO "salario_minimo" VALUES(2019,828116);
INSERT INTO "salario_minimo" VALUES(2020,877803);
INSERT INTO "salario_minimo" VALUES(2021,908526);
INSERT INTO "salario_minimo" VALUES(2022,1000000);
INSERT INTO "salario_minimo" VALUES(2023,1160000);
INSERT INTO "salario_minimo" VALUES(2024,1300000);
INSERT INTO "salario_minimo" VALUES(2025,1423500);
INSERT INTO "salario_minimo" VALUES(2026,1750905);
CREATE TABLE usuarios (
    id_usuario     INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario        VARCHAR NOT NULL UNIQUE,
    clave_hash     VARCHAR NOT NULL,
    rol            VARCHAR NOT NULL DEFAULT 'superusuario'
                   CHECK (rol IN ('superusuario','administrador')),
    activo         INTEGER NOT NULL DEFAULT 1 CHECK (activo IN (0,1)),
    fecha_creacion DATE    NOT NULL DEFAULT (date('now'))
);
INSERT INTO "usuarios" VALUES(1,'demo','pbkdf2_sha256$600000$76254127b3c052df300ee944c7680ad1$5493cfbedd4200a8d90ca20578d56fd78d1c83c94e5014dfc144fa193b826965','superusuario',1,'2026-01-01');
INSERT INTO "usuarios" VALUES(2,'admin_demo','pbkdf2_sha256$600000$c505146cd10e1694521892dd1990a1e1$93122af11deb59031d39ca5dc47712225e253b557fcc1c5ae24a307b15419328','administrador',1,'2026-01-01');
CREATE INDEX ix_clientes_nombre      ON clientes(nombre);
CREATE INDEX ix_proveedores_nombre   ON proveedores(nombre);
CREATE INDEX ix_empleados_nombre     ON empleados(nombre);
CREATE INDEX ix_contratistas_nombre  ON contratistas(nombre);
CREATE INDEX ix_productos_nombre     ON productos(nombre);
CREATE INDEX ix_proyectos_cliente    ON proyectos(id_cliente);
CREATE INDEX ix_contratos_cliente    ON contratos(id_cliente);
CREATE INDEX ix_contratos_fecha_fin  ON contratos(fecha_terminacion);
CREATE INDEX ix_detalle_proyecto     ON detalle(id_proyecto);
CREATE INDEX ix_detalle_contrato     ON detalle(id_contrato);
CREATE INDEX ix_contrato_codigos_codigo ON contrato_codigos(id_codigo);
CREATE INDEX ix_contrato_docs_contrato ON contrato_documentos(id_contrato);
CREATE INDEX ix_asignaciones_contrato ON asignaciones(id_contrato);
CREATE VIEW v_detalle AS
SELECT d.id_detalle, d.id_proyecto, d.id_contrato, p.nombre AS producto, p.unidad_medida,
       d.cantidad, d.precio_unitario, d.subtotal,
       ROUND(d.subtotal * d.descuento_pct / 100.0, 2)                              AS descuento,
       ROUND(d.subtotal * (1 - d.descuento_pct / 100.0), 2)                        AS base_gravable,
       ROUND(d.subtotal * (1 - d.descuento_pct / 100.0) * d.iva_pct / 100.0, 2)    AS iva,
       ROUND(d.subtotal * (1 - d.descuento_pct / 100.0) * d.retefuente_pct / 100.0, 2) AS retefuente,
       ROUND(d.subtotal * (1 - d.descuento_pct / 100.0) * d.reteica_pct / 100.0, 2)    AS reteica,
       ROUND(d.subtotal * (1 - d.descuento_pct / 100.0) * d.iva_pct / 100.0 * d.reteiva_pct / 100.0, 2) AS reteiva
FROM detalle d JOIN productos p ON p.id_producto = d.id_producto;
CREATE VIEW v_totales_proyecto AS
SELECT pr.id_proyecto, pr.codigo, pr.nombre, pr.tipo_proyecto,
       SUM(v.subtotal) AS subtotal, SUM(v.descuento) AS descuento, SUM(v.base_gravable) AS base_gravable,
       SUM(v.iva) AS iva, SUM(v.base_gravable) + SUM(v.iva) AS total_con_iva,
       SUM(v.retefuente) AS retefuente, SUM(v.reteica) AS reteica, SUM(v.reteiva) AS reteiva,
       SUM(v.base_gravable) + SUM(v.iva) - SUM(v.retefuente) - SUM(v.reteica) - SUM(v.reteiva) AS neto_a_recibir
FROM proyectos pr JOIN v_detalle v ON v.id_proyecto = pr.id_proyecto
GROUP BY pr.id_proyecto;
CREATE VIEW v_totales_contrato AS
SELECT c.id_contrato, c.codigo_interno,
       SUM(v.subtotal) AS subtotal, SUM(v.descuento) AS descuento, SUM(v.base_gravable) AS base_gravable,
       SUM(v.iva) AS iva, SUM(v.base_gravable) + SUM(v.iva) AS total_con_iva,
       SUM(v.retefuente) AS retefuente, SUM(v.reteica) AS reteica, SUM(v.reteiva) AS reteiva,
       SUM(v.base_gravable) + SUM(v.iva) - SUM(v.retefuente) - SUM(v.reteica) - SUM(v.reteiva) AS neto_a_recibir
FROM contratos c JOIN v_detalle v ON v.id_contrato = c.id_contrato
GROUP BY c.id_contrato;
CREATE VIEW v_contratos AS
SELECT c.id_contrato, c.codigo_interno, c.numero_contrato, c.consecutivo_rup,
       cl.nombre AS cliente, c.tipo_contrato, c.objeto, c.figura_contratista,
       c.porcentaje_participacion, c.valor_contrato, c.valor_participacion,
       c.fecha_inicio, c.fecha_terminacion, c.ubicacion_obra, c.estado,
       sm.anio  AS anio_smmlv,
       sm.valor AS smmlv_del_anio,
       ROUND(c.valor_participacion / sm.valor, 2) AS valor_smmlv_participacion
FROM contratos c
JOIN clientes cl ON cl.id_cliente = c.id_cliente
LEFT JOIN salario_minimo sm
       ON sm.anio = COALESCE(CAST(strftime('%Y', c.fecha_terminacion) AS INTEGER),
                             CAST(strftime('%Y', c.fecha_inicio) AS INTEGER));
CREATE VIEW v_empleados_basico AS
SELECT id_empleado, nombre, tipo_documento, numero_documento, cargo, telefono, correo,
       direccion, fecha_ingreso, estado
FROM empleados;
CREATE TRIGGER trg_detalle_codigos_ai AFTER INSERT ON detalle
BEGIN
  INSERT OR IGNORE INTO contrato_codigos(id_contrato,id_codigo)
  SELECT c.id_contrato, p.id_codigo FROM productos p, contratos c
  WHERE p.id_producto = NEW.id_producto AND p.id_codigo IS NOT NULL
    AND (c.id_contrato = NEW.id_contrato OR (NEW.id_proyecto IS NOT NULL AND c.id_proyecto = NEW.id_proyecto));
END;
CREATE TRIGGER trg_detalle_codigos_au AFTER UPDATE OF id_producto ON detalle
BEGIN
  INSERT OR IGNORE INTO contrato_codigos(id_contrato,id_codigo)
  SELECT c.id_contrato, p.id_codigo FROM productos p, contratos c
  WHERE p.id_producto = NEW.id_producto AND p.id_codigo IS NOT NULL
    AND (c.id_contrato = NEW.id_contrato OR (NEW.id_proyecto IS NOT NULL AND c.id_proyecto = NEW.id_proyecto));
END;
CREATE TRIGGER trg_contrato_codigos_ai AFTER INSERT ON contratos WHEN NEW.id_proyecto IS NOT NULL
BEGIN
  INSERT OR IGNORE INTO contrato_codigos(id_contrato,id_codigo)
  SELECT NEW.id_contrato, p.id_codigo FROM detalle d JOIN productos p ON p.id_producto=d.id_producto
  WHERE d.id_proyecto = NEW.id_proyecto AND p.id_codigo IS NOT NULL;
END;
CREATE TRIGGER trg_contrato_codigos_au AFTER UPDATE OF id_proyecto ON contratos WHEN NEW.id_proyecto IS NOT NULL
BEGIN
  INSERT OR IGNORE INTO contrato_codigos(id_contrato,id_codigo)
  SELECT NEW.id_contrato, p.id_codigo FROM detalle d JOIN productos p ON p.id_producto=d.id_producto
  WHERE d.id_proyecto = NEW.id_proyecto AND p.id_codigo IS NOT NULL;
END;
DELETE FROM "sqlite_sequence";
INSERT INTO "sqlite_sequence" VALUES('usuarios',2);
INSERT INTO "sqlite_sequence" VALUES('codigos_compra',9);
INSERT INTO "sqlite_sequence" VALUES('clientes',5);
INSERT INTO "sqlite_sequence" VALUES('productos',8);
INSERT INTO "sqlite_sequence" VALUES('proyectos',5);
INSERT INTO "sqlite_sequence" VALUES('detalle',10);
INSERT INTO "sqlite_sequence" VALUES('contratos',3);
INSERT INTO "sqlite_sequence" VALUES('empleados',2);
INSERT INTO "sqlite_sequence" VALUES('contratistas',3);
INSERT INTO "sqlite_sequence" VALUES('proveedores',3);
INSERT INTO "sqlite_sequence" VALUES('asignaciones',3);
COMMIT;