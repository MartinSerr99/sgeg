# SGEG - Sistema de Gestión para Emprendimientos Gastronómicos

Java 17+ · JavaFX 21 · MySQL 8 · JDBC · arquitectura en capas (Vista → Servicio → DAO → Modelo).

## Estado actual: todos los módulos implementados
| Caso de uso | Rol | Qué hace |
|---|---|---|
| CU01 | Todos | Inicio de sesión (contraseñas con PBKDF2). El menú muestra solo los módulos del rol. |
| CU02 | Administrador | Usuarios: alta, edición, cambio de contraseña, baja y reactivación (la baja conserva el historial). No deja desactivar ni quitar el rol al último administrador activo. |
| CU03–05 | Administrador | Productos: alta, edición, precio, baja/reactivación, categorías y receta (insumos que consume). |
| CU10–12 | Administrador (edita) · Cocinero (consulta) | Stock de insumos: nuevo insumo, edición, registro de ingresos de stock y mínimo. Alerta de stock bajo al entrar. |
| CU06 | Mozo | Registrar pedido por mesa o para llevar. Controla stock al agregar y descuenta los insumos **al confirmar**. |
| CU07 | Mozo | Modificar o cancelar un pedido **Pendiente**. Se devuelve o descuenta solo la diferencia de stock. |
| CU08/09 | Cocinero · Mozo | Seguimiento: Pendiente → En preparación → Listo → Entregado. Se actualiza solo cada 10 s. |
| CU13/14 | Cajero | Cobro de pedidos Entregados, con una o varias formas de pago (deben sumar el total exacto). Genera el comprobante (se puede guardar como .txt), pasa el pedido a Facturado y libera la mesa. |
| CU15–17 | Administrador · Cajero | Cierre de caja (resumen del día por forma de pago) y reportes por período: ventas, totales por forma de pago, productos más vendidos. |

Decisiones de diseño:
- El cliente no tiene cuenta: el mozo hace de mediador.
- Cancelar un pedido lo **elimina** (no existe estado «Cancelado», para no cambiar el esquema) y devuelve el stock.
- El comprobante es un «documento no válido como factura»; no es un comprobante fiscal.
- Cierre de caja y reportes son consultas: cuentan solo ventas ya cobradas.
- Todas las operaciones críticas (confirmar, modificar, cancelar, cobrar) son transacciones con bloqueo de filas.
- Se requiere **MySQL 8** (se usa `FOR UPDATE OF`).

## Cómo ejecutarlo
1. Instalar JDK 17 o superior, MySQL 8 y VS Code con "Extension Pack for Java".
2. En MySQL ejecutar, en este orden: `database/01_schema.sql` y `database/02_datos_iniciales.sql`.
   (Si ya existía una base `sgeg`, borrarla antes con `DROP DATABASE sgeg;`).
   Para repetir las pruebas sin recrear la base: `database/99_reiniciar_datos_de_prueba.sql`.
3. Editar `src/main/resources/config.properties` con el usuario y la clave de MySQL.
4. VS Code: Run and Debug → "Probar conexión a la BD" (debe mostrar varios [OK]).
5. VS Code: Run and Debug → "Ejecutar SGEG".
   Alternativa por terminal (requiere Maven): `mvn javafx:run`

## Usuarios de prueba
| Usuario | Contraseña | Rol |
|---|---|---|
| admin | admin123 | Administrador |
| mozo1 | mozo123 | Mozo |
| cocina1 | cocina123 | Cocinero |
| caja1 | caja123 | Cajero |

## Guía de pruebas (circuito completo)
Cerrar sesión y entrar con cada usuario en el orden indicado.

**1. admin — gestión**
- *Productos*: crear «Agua 500ml» (categoría Bebidas, precio 1000) con receta de 1 «Gaseosa (botella)». Darlo de baja y comprobar que el mozo ya no lo ve en el menú.
- *Stock*: «Registrar ingreso de stock» sobre un insumo; poner el stock por debajo del mínimo y reentrar: aparece el aviso «Ver stock».
- *Usuarios*: crear un mozo, cambiarle la contraseña, darlo de baja (no debe poder entrar). Intentar dar de baja a `admin` siendo el único administrador: debe rechazarlo.

**2. mozo1 — pedidos**
- «Mesas» → mesa libre → agregar productos → «Confirmar pedido». La mesa pasa a ocupada y el stock baja.
- «Seguimiento» → seleccionar un pedido Pendiente → **Modificar** (cambiar cantidades) o **Cancelar**. Verificar en el stock que se devolvió/descontó solo la diferencia.
- «Pedido para llevar» no ocupa mesa.

**3. cocina1 — cocina**
- «Pedidos en cocina» → «Iniciar preparación» → «Marcar como listo». Ver además «Stock» (solo lectura).

**4. mozo1 — entrega**
- «Seguimiento» → «Marcar como entregado».

**5. caja1 — cobro**
- «Cobro» → elegir el pedido Entregado → «Cobrar». Probar un pago en una sola forma y otro dividido (ej. Efectivo + Tarjeta). Si no suman el total, rechaza. Al confirmar se muestra el comprobante (guardable en .txt), la mesa queda libre y el pedido Facturado.
- «Cierre de caja» → el total del día coincide con lo cobrado.

**6. admin — reportes**
- «Reportes» → elegir período: ventas, totales por forma de pago y productos más vendidos.

Casos de borde:
- **Stock insuficiente**: `UPDATE insumo SET stock_actual = 0.30 WHERE nombre = 'Carne vacuna';` y pedir 2 milanesas (necesitan 0,50 kg): avisa qué insumo falta y no agrega nada.
- **Insumo compartido**: con 1,00 kg de carne entran 4 milanesas; una más no.
- **Mesa tomada por otro mozo**: con dos ventanas, confirmar la misma mesa en ambas; la segunda recibe «La mesa N ya no está libre».
- **Modificar con poco stock**: al agregar unidades a un pedido, solo se exige stock para el aumento.

Controles en MySQL:
`SELECT * FROM pedido;` `SELECT * FROM venta;` `SELECT * FROM pago;` `SELECT nombre, stock_actual FROM insumo;`

## Estructura
- `com.sgeg.modelo`: entidades (Usuario, Mesa, Producto, Pedido, DetallePedido, Venta, Pago, Insumo…).
- `com.sgeg.util`: ConexionBD (Singleton; `abrirConexionNueva()` para transacciones), SeguridadUtil (PBKDF2), Formatos, Comprobantes.
- `com.sgeg.dao`: interfaces DAO y su implementación JDBC.
- `com.sgeg.servicio`: reglas de negocio (SesionService, PedidoService, VentaService, ReporteService, ProductoService, InsumoService, UsuarioService) y `Servicios` que los agrupa.
- `com.sgeg.excepcion`: NegocioException (mensaje para el usuario) y StockInsuficienteException.
- `com.sgeg.vista`: controladores JavaFX, navegación (Navegador), diálogos y tareas en segundo plano.
- `src/main/resources/vista`: pantallas FXML. `css/estilos.css`: estilos.

## Subir a GitHub
No subir la clave de MySQL: `config.properties` contiene la contraseña. Antes de subir, agregar `src/main/resources/config.properties` al `.gitignore` o reemplazar la clave por un valor de ejemplo.
