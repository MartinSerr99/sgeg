# SGEG - Sistema de Gestión para Emprendimientos Gastronómicos

Java 17+ · JavaFX 21 · MySQL 8 · JDBC · arquitectura en capas (Vista → Servicio → DAO → Modelo).

## Estado actual
- CU01 Iniciar sesión.
- CU06 Registrar pedido (mozo): plano de mesas, pedido por mesa o para llevar, control de stock al agregar (RF19),
  total en pantalla (RF16), cliente opcional. **Al confirmar** se ocupa la mesa y se descuentan los insumos (RF23),
  todo en una sola transacción.
- CU08/CU09 Seguimiento: la cocina ve los pedidos del más antiguo al más nuevo y los pasa a «En preparación» y «Listo»;
  el mozo marca «Entregado». La pantalla se actualiza sola cada 10 segundos.
- Pendientes: CU07 (modificar/cancelar pedido, con devolución de stock), CU13 (cobro: «Entregado» → «Facturado»
  y liberación de la mesa), productos, stock, usuarios y reportes. Esos módulos aparecen como marcadores.

## Cómo ejecutarlo
1. Instalar JDK 17 o superior, MySQL 8 y VS Code con "Extension Pack for Java".
2. En MySQL ejecutar, en este orden: `database/01_schema.sql` y `database/02_datos_iniciales.sql`.
   (Si ya existía una base `sgeg`, borrarla antes con `DROP DATABASE sgeg;`).
   Para repetir las pruebas de pedidos sin recrear la base: `database/99_reiniciar_datos_de_prueba.sql`.
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

## Cómo probar el módulo de pedidos
Se necesitan dos sesiones en momentos distintos (cerrar sesión y entrar con el otro usuario).
1. **mozo1** → «Mesas y pedidos» → elegir una mesa libre (verde) → agregar productos → «Confirmar pedido».
   Resultado esperado: aparece el aviso «Pedido #1…», la mesa pasa a ocupada (violeta) y el stock baja.
2. **cocina1** → «Pedidos en cocina» → «Iniciar preparación» → «Marcar como listo».
3. **mozo1** → «Pedidos en seguimiento» → «Marcar como entregado» (el pedido sale de la lista).
4. Comprobar en MySQL:
   `SELECT * FROM pedido;` `SELECT * FROM detalle_pedido;` `SELECT nombre, stock_actual FROM insumo;`

Casos de borde útiles:
- **Stock insuficiente**: `UPDATE insumo SET stock_actual = 0.30 WHERE nombre = 'Carne vacuna';` y pedir 2 milanesas
  (necesitan 0,50 kg): el sistema avisa qué insumo falta y no agrega nada.
- **Insumo compartido**: con 1,00 kg de carne, 4 milanesas (1,00 kg) entran; una más (o 1 empanada más) no.
- **Mesa ocupada por otro mozo**: con dos ventanas abiertas, elegir la misma mesa en ambas y confirmar en las dos;
  la segunda recibe «La mesa N ya no está libre» y no descuenta stock.
- **Para llevar**: botón «Pedido para llevar» (no ocupa ninguna mesa).
- Cada rol ve solo los botones de avance que le corresponden (cocinero: Pendiente y En preparación; mozo: Listo).

## Estructura
- `com.sgeg.modelo`: entidades (Rol, Usuario, Mesa, Producto, Pedido, DetallePedido, Cliente…) y estados.
- `com.sgeg.util`: ConexionBD (Singleton; `abrirConexionNueva()` para transacciones), SeguridadUtil (PBKDF2), Formatos.
- `com.sgeg.dao`: interfaces DAO y su implementación JDBC. `PedidoDAOImpl` ejecuta la confirmación en una transacción.
- `com.sgeg.servicio`: reglas de negocio (SesionService, PedidoService).
- `com.sgeg.excepcion`: NegocioException (mensaje para el usuario) y StockInsuficienteException.
- `com.sgeg.vista`: controladores JavaFX, navegación (Navegador), diálogos y tareas en segundo plano.
- `src/main/resources/vista`: pantallas FXML. `css/estilos.css`: estilos.
