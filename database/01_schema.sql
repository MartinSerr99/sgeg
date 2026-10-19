-- =========================================================
-- Sistema de Gestión para Emprendimientos Gastronómicos (SGEG)
-- Script de creación de base de datos y tablas - MySQL
-- =========================================================

CREATE DATABASE IF NOT EXISTS sgeg
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_spanish_ci;

USE sgeg;

-- =========================================================
-- TABLAS DE CATALOGO (valores fijos del dominio)
-- =========================================================

CREATE TABLE rol (
    id_rol      INT AUTO_INCREMENT PRIMARY KEY,
    descripcion VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE estado_mesa (
    id_estado_mesa INT AUTO_INCREMENT PRIMARY KEY,
    descripcion    VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE estado_pedido (
    id_estado_pedido INT AUTO_INCREMENT PRIMARY KEY,
    descripcion       VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE forma_pago (
    id_forma_pago INT AUTO_INCREMENT PRIMARY KEY,
    descripcion   VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE categoria (
    id_categoria INT AUTO_INCREMENT PRIMARY KEY,
    nombre       VARCHAR(50) NOT NULL UNIQUE
);

-- =========================================================
-- ENTIDADES PRINCIPALES
-- =========================================================

CREATE TABLE usuario (
    id_usuario       INT AUTO_INCREMENT PRIMARY KEY,
    nombre           VARCHAR(100) NOT NULL,
    nombre_usuario   VARCHAR(50)  NOT NULL UNIQUE,
    contrasenia_hash VARCHAR(255) NOT NULL,
    id_rol           INT NOT NULL,
    activo           BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES rol(id_rol)
);

CREATE TABLE mesa (
    id_mesa        INT AUTO_INCREMENT PRIMARY KEY,
    numero         INT NOT NULL UNIQUE,
    id_estado_mesa INT NOT NULL,
    CONSTRAINT fk_mesa_estado FOREIGN KEY (id_estado_mesa) REFERENCES estado_mesa(id_estado_mesa)
);

CREATE TABLE cliente (
    id_cliente INT AUTO_INCREMENT PRIMARY KEY,
    nombre     VARCHAR(100) NOT NULL,
    telefono   VARCHAR(30)
);

CREATE TABLE producto (
    id_producto  INT AUTO_INCREMENT PRIMARY KEY,
    nombre       VARCHAR(100) NOT NULL,
    precio       DECIMAL(10,2) NOT NULL,
    disponible   BOOLEAN NOT NULL DEFAULT TRUE,
    id_categoria INT NOT NULL,
    CONSTRAINT fk_producto_categoria FOREIGN KEY (id_categoria) REFERENCES categoria(id_categoria)
);

CREATE TABLE insumo (
    id_insumo     INT AUTO_INCREMENT PRIMARY KEY,
    nombre        VARCHAR(100) NOT NULL,
    stock_actual  DECIMAL(10,2) NOT NULL DEFAULT 0,
    stock_minimo  DECIMAL(10,2) NOT NULL DEFAULT 0,
    unidad_medida VARCHAR(20) NOT NULL
);

-- Relación N:M entre producto e insumo (receta): qué insumos y en qué
-- cantidad requiere la elaboración de una unidad de cada producto.
CREATE TABLE producto_insumo (
    id_producto        INT NOT NULL,
    id_insumo          INT NOT NULL,
    cantidad_necesaria DECIMAL(10,2) NOT NULL,
    PRIMARY KEY (id_producto, id_insumo),
    CONSTRAINT fk_pi_producto FOREIGN KEY (id_producto) REFERENCES producto(id_producto) ON DELETE CASCADE,
    CONSTRAINT fk_pi_insumo   FOREIGN KEY (id_insumo)   REFERENCES insumo(id_insumo)   ON DELETE CASCADE
);

CREATE TABLE pedido (
    id_pedido        INT AUTO_INCREMENT PRIMARY KEY,
    fecha_hora       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_mesa          INT NULL,
    id_cliente       INT NULL,
    id_usuario       INT NOT NULL,
    id_estado_pedido INT NOT NULL,
    total            DECIMAL(10,2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_pedido_mesa    FOREIGN KEY (id_mesa)          REFERENCES mesa(id_mesa),
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (id_cliente)       REFERENCES cliente(id_cliente),
    CONSTRAINT fk_pedido_usuario FOREIGN KEY (id_usuario)       REFERENCES usuario(id_usuario),
    CONSTRAINT fk_pedido_estado  FOREIGN KEY (id_estado_pedido) REFERENCES estado_pedido(id_estado_pedido)
);

-- id_mesa e id_cliente son NULL-ables: un pedido "para llevar" puede no
-- tener mesa asociada, y un cliente registrado es siempre opcional.

CREATE TABLE detalle_pedido (
    id_detalle      INT AUTO_INCREMENT PRIMARY KEY,
    id_pedido       INT NOT NULL,
    id_producto     INT NOT NULL,
    cantidad        INT NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    subtotal        DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_detalle_pedido   FOREIGN KEY (id_pedido)   REFERENCES pedido(id_pedido)   ON DELETE CASCADE,
    CONSTRAINT fk_detalle_producto FOREIGN KEY (id_producto) REFERENCES producto(id_producto)
);

-- precio_unitario se copia desde producto.precio al momento de la carga
-- del detalle, para no alterar ventas históricas si el precio cambia luego.

CREATE TABLE venta (
    id_venta   INT AUTO_INCREMENT PRIMARY KEY,
    id_pedido  INT NOT NULL UNIQUE,
    fecha_hora DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total      DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_venta_pedido FOREIGN KEY (id_pedido) REFERENCES pedido(id_pedido)
);

CREATE TABLE pago (
    id_pago       INT AUTO_INCREMENT PRIMARY KEY,
    id_venta      INT NOT NULL,
    id_forma_pago INT NOT NULL,
    monto         DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_pago_venta FOREIGN KEY (id_venta)      REFERENCES venta(id_venta) ON DELETE CASCADE,
    CONSTRAINT fk_pago_forma FOREIGN KEY (id_forma_pago) REFERENCES forma_pago(id_forma_pago)
);

-- =========================================================
-- CARGA INICIAL DE CATALOGOS (valores fijos del dominio,
-- no son datos transaccionales del negocio)
-- =========================================================

INSERT INTO rol (descripcion) VALUES
    ('Administrador'), ('Mozo'), ('Cocinero'), ('Cajero');

INSERT INTO estado_mesa (descripcion) VALUES
    ('Libre'), ('Ocupada'), ('Reservada');

INSERT INTO estado_pedido (descripcion) VALUES
    ('Pendiente'), ('En preparación'), ('Listo'), ('Entregado'), ('Facturado');

INSERT INTO forma_pago (descripcion) VALUES
    ('Efectivo'), ('Tarjeta'), ('Transferencia');
