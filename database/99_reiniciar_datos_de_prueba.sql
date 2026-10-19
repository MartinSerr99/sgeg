-- =========================================================
-- SGEG - Reinicia los datos de PRUEBA para repetir el circuito de pedidos
-- Borra pedidos, ventas y pagos; vuelve las mesas y el stock a su estado
-- inicial (los mismos valores de 02_datos_iniciales.sql).
-- Además reactiva los usuarios y productos (por si se probó desactivarlos).
-- NO borra usuarios, clientes, productos ni recetas creados desde la aplicación,
-- ni revierte cambios de precios, mínimos de stock o contraseñas.
-- Usar solo en desarrollo.
-- =========================================================

SET NAMES utf8mb4;
USE sgeg;

-- MySQL Workbench trae activado el "modo seguro" (SQL_SAFE_UPDATES), que rechaza
-- UPDATE/DELETE cuyo WHERE no usa la clave primaria. Se desactiva solo para esta sesión.
SET SQL_SAFE_UPDATES = 0;

-- 1) Pedidos y todo lo que depende de ellos (de hijos a padres)
DELETE FROM pago;
DELETE FROM venta;
DELETE FROM detalle_pedido;
DELETE FROM pedido;

ALTER TABLE pago           AUTO_INCREMENT = 1;
ALTER TABLE venta          AUTO_INCREMENT = 1;
ALTER TABLE detalle_pedido AUTO_INCREMENT = 1;
ALTER TABLE pedido         AUTO_INCREMENT = 1;

-- 2) Mesas: todas libres, salvo la 5 que arranca reservada
UPDATE mesa
   SET id_estado_mesa = (SELECT id_estado_mesa FROM estado_mesa WHERE descripcion = 'Libre');

UPDATE mesa
   SET id_estado_mesa = (SELECT id_estado_mesa FROM estado_mesa WHERE descripcion = 'Reservada')
 WHERE numero = 5;

-- 3) Stock de insumos como en la carga inicial
UPDATE insumo SET stock_actual =  15.00 WHERE nombre = 'Carne vacuna';
UPDATE insumo SET stock_actual =  30.00 WHERE nombre = 'Papa';
UPDATE insumo SET stock_actual = 100.00 WHERE nombre = 'Masa para empanada';
UPDATE insumo SET stock_actual =  40.00 WHERE nombre = 'Gaseosa (botella)';
UPDATE insumo SET stock_actual =   8.00 WHERE nombre = 'Leche';

-- 4) Usuarios y productos activos / disponibles
UPDATE usuario  SET activo = TRUE;
UPDATE producto SET disponible = TRUE;

SET SQL_SAFE_UPDATES = 1;

-- Control rápido
SELECT m.numero AS mesa, em.descripcion AS estado
  FROM mesa m JOIN estado_mesa em ON em.id_estado_mesa = m.id_estado_mesa
 ORDER BY m.numero;
SELECT nombre, stock_actual, unidad_medida FROM insumo ORDER BY id_insumo;
