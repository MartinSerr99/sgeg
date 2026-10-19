-- =========================================================
-- SGEG - Datos iniciales para desarrollo y pruebas
-- Ejecutar DESPUÉS de 01_schema.sql
-- =========================================================

SET NAMES utf8mb4;
USE sgeg;

-- Usuarios de prueba (solo para desarrollo; en un local real se cambian).
--   admin / admin123      -> Administrador
--   mozo1 / mozo123       -> Mozo
--   cocina1 / cocina123   -> Cocinero
--   caja1 / caja123       -> Cajero
-- Las contraseñas se guardan como hash PBKDF2 (formato de SeguridadUtil.java).
INSERT INTO usuario (nombre, nombre_usuario, contrasenia_hash, id_rol) VALUES
    ('Administrador General', 'admin', 'pbkdf2_sha256$600000$PrAiOUxm2R+1+Gu3hgex+Q==$zzarcbpvkC2TEpoKAV2jGyfBwW8YLJjJ0K4t87fb4JM=', (SELECT id_rol FROM rol WHERE descripcion = 'Administrador')),
    ('Juan Pérez', 'mozo1', 'pbkdf2_sha256$600000$d2puj75pIMGnfiR/K8S5iw==$NPSfTIyzLP/2totuo5kkV8ATLlHHLVclalouZ7diQjQ=', (SELECT id_rol FROM rol WHERE descripcion = 'Mozo')),
    ('Ana Gómez', 'cocina1', 'pbkdf2_sha256$600000$S6krkEUCGTIOrng8K/sbDQ==$XDqrGZDQyhHfg5oiOqdFuU+WQCMvfVtKGoQNmNqHq98=', (SELECT id_rol FROM rol WHERE descripcion = 'Cocinero')),
    ('Lucía Fernández', 'caja1', 'pbkdf2_sha256$600000$4KiXi1kULwYFEeeXbJE0hA==$io8cEhfoI6dg4vJsvQH8kzR/2vFYQ1jaRDfJm5RALM4=', (SELECT id_rol FROM rol WHERE descripcion = 'Cajero'));

-- Mesas
INSERT INTO mesa (numero, id_estado_mesa) VALUES
    (1, (SELECT id_estado_mesa FROM estado_mesa WHERE descripcion = 'Libre')),
    (2, (SELECT id_estado_mesa FROM estado_mesa WHERE descripcion = 'Libre')),
    (3, (SELECT id_estado_mesa FROM estado_mesa WHERE descripcion = 'Libre')),
    (4, (SELECT id_estado_mesa FROM estado_mesa WHERE descripcion = 'Libre')),
    (5, (SELECT id_estado_mesa FROM estado_mesa WHERE descripcion = 'Reservada')),
    (6, (SELECT id_estado_mesa FROM estado_mesa WHERE descripcion = 'Libre'));

-- Clientes frecuentes
INSERT INTO cliente (nombre, telefono) VALUES
    ('Carlos Ibáñez', '3511234567'),
    ('Marina Suárez', '3517654321');

-- Categorías
INSERT INTO categoria (nombre) VALUES
    ('Entradas'), ('Platos principales'), ('Bebidas'), ('Postres');

-- Productos
INSERT INTO producto (nombre, precio, disponible, id_categoria) VALUES
    ('Empanada de carne',  800.00,  TRUE, (SELECT id_categoria FROM categoria WHERE nombre = 'Entradas')),
    ('Milanesa con papas', 5200.00, TRUE, (SELECT id_categoria FROM categoria WHERE nombre = 'Platos principales')),
    ('Gaseosa 500ml',      1500.00, TRUE, (SELECT id_categoria FROM categoria WHERE nombre = 'Bebidas')),
    ('Flan casero',        1800.00, TRUE, (SELECT id_categoria FROM categoria WHERE nombre = 'Postres'));

-- Insumos
INSERT INTO insumo (nombre, stock_actual, stock_minimo, unidad_medida) VALUES
    ('Carne vacuna',        15.00,  5.00, 'kg'),
    ('Papa',                30.00, 10.00, 'kg'),
    ('Masa para empanada', 100.00, 20.00, 'unidad'),
    ('Gaseosa (botella)',   40.00, 10.00, 'unidad'),
    ('Leche',                8.00,  5.00, 'litro');

-- Receta: insumos que consume cada producto
INSERT INTO producto_insumo (id_producto, id_insumo, cantidad_necesaria) VALUES
    ((SELECT id_producto FROM producto WHERE nombre = 'Empanada de carne'),  (SELECT id_insumo FROM insumo WHERE nombre = 'Masa para empanada'), 1.00),
    ((SELECT id_producto FROM producto WHERE nombre = 'Empanada de carne'),  (SELECT id_insumo FROM insumo WHERE nombre = 'Carne vacuna'),       0.08),
    ((SELECT id_producto FROM producto WHERE nombre = 'Milanesa con papas'), (SELECT id_insumo FROM insumo WHERE nombre = 'Carne vacuna'),       0.25),
    ((SELECT id_producto FROM producto WHERE nombre = 'Milanesa con papas'), (SELECT id_insumo FROM insumo WHERE nombre = 'Papa'),               0.30),
    ((SELECT id_producto FROM producto WHERE nombre = 'Gaseosa 500ml'),      (SELECT id_insumo FROM insumo WHERE nombre = 'Gaseosa (botella)'), 1.00),
    ((SELECT id_producto FROM producto WHERE nombre = 'Flan casero'),        (SELECT id_insumo FROM insumo WHERE nombre = 'Leche'),             0.15);
