# Revisión de posibles bugs del addon

Revisión estática del código, con foco en errores sencillos de lógica, límites, estado y mixins. Los hallazgos siguientes quedan pendientes de corrección; no se reprodujeron en una sesión de Minecraft. Las referencias usan los nombres nuevos de los mixins.

| Prioridad | Ubicación | Problema y condición de fallo | Corrección sugerida |
| --- | --- | --- | --- |
| P2 | `src/main/java/kassuk/addon/blackout/mixins/PlayerEntityMixin.java:29` | SoundModifier intercepta todos los sonidos de `PlayerEntity.attack`. Guarda `attackEntity` pero nunca comprueba que sea un `EndCrystalEntity`, por lo que los ajustes de crystal-hit modifican o silencian también ataques a otras entidades. | Comprobar el objetivo y limitar el cambio al jugador local. |
| P2 | `src/main/java/kassuk/addon/blackout/mixins/EntityMixin.java:102` | ForceSneak compara objetos `Text` por identidad (`!=`) para identificar al jugador. Los nombres no identifican la entidad y la condición incluye a entidades ajenas al jugador local. | Comparar `(Object) this == mc.player` y preservar el resultado original para las demás entidades. |
| P2 | `src/main/java/kassuk/addon/blackout/mixins/EntityMixin.java:108` | AntiCrawl fuerza `doesNotCollide(Box)` a true en cualquier entidad mientras está activo. Cambia comprobaciones de colisión de entidades remotas y mobs, además del jugador. | Restringir el efecto al jugador local y a la comprobación de pose necesaria. |
| P2 | `src/main/java/kassuk/addon/blackout/managers/PingSpoofManager.java:57` | `delayed1` y `delayed2` solo almacenan un paquete de cada tipo. Dos keepalives/pings antes del siguiente Render3DEvent sobrescriben la respuesta anterior. Además, compartir esos campos entre callbacks de red y render carece de sincronización. | Usar una cola segura para los callbacks o confinar la cola al hilo del cliente. |
| P2 | `src/main/java/kassuk/addon/blackout/managers/PingSpoofManager.java:49` | La cola no se limpia al desconectarse/cambiar de servidor y usa el network handler actual al enviar. Puede reenviar respuestas de una conexión antigua a otra; si el handler desaparece, tampoco hay protección contra null. | Asociar los paquetes a su conexión y limpiar la cola al salir del mundo. |
| P2 | `src/main/java/kassuk/addon/blackout/modules/StrictNoSlow.java:78` | `onlyGap.get() || ...` acepta cualquier item cuando only-gapples está activado. Al desactivarlo solo acepta gapples. La última comparación de `stack.getItem()` queda fuera de la protección contra null. | Usar `!onlyGap.get() || (stack != null && ... && (enchanted || golden))`. |
| P2 | `src/main/java/kassuk/addon/blackout/modules/StrictNoSlow.java:65` | Con single-packet desactivado, `onMove` envía cambios de slot periódicamente aunque no se esté usando ningún item ni se cumpla only-gapples. | Comprobar que el jugador esté usando un item y aplicar `shouldSend` al stack de la mano activa. |
| P2 | `src/main/java/kassuk/addon/blackout/modules/RPC.java:87` | Si una lista de mensajes está vacía, o todos sus scripts fallan al compilar, `messages1/2.get(0)` produce IndexOutOfBoundsException al estar dentro del mundo. | Añadir un mensaje de respaldo cuando el resultado de getMessages esté vacío. |
| P2 | `src/main/java/kassuk/addon/blackout/modules/AutoEz.java:376` | Con exactamente un mensaje de pop, `nextInt(0, size - 1)` equivale a nextInt(0, 0) y lanza IllegalArgumentException. Con más mensajes el sorteo excluye el último índice, aunque la lógica antirrepetición puede alcanzarlo. | Usar `nextInt(size)` y tratar la repetición sin excluir posiciones. |
| P2 | `src/main/java/kassuk/addon/blackout/modules/Fog.java:59` | El shader recibe RGB de 0 a 255 donde espera componentes normalizados. Además thickness=100, permitido por el ajuste, divide el alpha entre cero y produce infinito o NaN. | Normalizar los componentes y definir una fórmula de opacidad con límites finitos. |
| P2 | `src/main/java/kassuk/addon/blackout/utils/PacketNames.java:266` | El logger desreferencia `unsignedContent()` y `signature()` sin comprobar null, aunque ambos pueden faltar en mensajes de chat válidos. Un paquete seleccionado puede lanzar NullPointerException desde su callback. | Formatear explícitamente los campos opcionales. Revisar también `getNbt()` en las líneas 251 y 440 antes de acceder a sus entradas. |
| P3 | `src/main/java/kassuk/addon/blackout/mixins/ClientConnectionMixin.java:20` | El primer envío que inicializa packetLogger no se registra: el `else if` impide ejecutar onSent en esa misma llamada. | Separar la inicialización del chequeo de actividad, incluyendo una comprobación de null. |
| P3 | `src/main/java/kassuk/addon/blackout/BlackOutModule.java` (`sendSequenced`) | Si `predict` o `sendPacket` lanza una excepción, `PendingUpdateManager.close()` no se ejecuta y queda sin cerrar el estado de la secuencia. | Usar try-with-resources como en el envío secuenciado vanilla. |

Se comprobó en el bytecode local de Minecraft 1.21.1 que `Explosion.affectWorld` contiene la llamada playSound usada por ExplosionMixin; no se considera un target inexistente. El build no comprueba la aplicación real de todas las inyecciones: falta una prueba dentro del juego.

## Cambios aplicados en esta tarea

- Prefijo `[BlackOut]` registrado por paquete mediante `ChatUtils.registerCustomPrefix` y construido con `Text.literal(...).formatted(...)`.
- Eliminada la sobrescritura del feedback de activación y la sobrecarga personalizada; los avisos adicionales usan `info`, sin construir ON/OFF propios.
- Los helpers de mensajes pasan por ChatUtils/Meteor en lugar de escribir directamente en IChatHud.
- Ajustes literales y nombres construidos por RotationSettings convertidos a kebab-case, incluidos HUD, ajustes globales y nombres con puntuación.
- Los 16 métodos @Accessor usan `blackoutAddon$`; se actualizaron todas sus llamadas.
- Los 10 mixins se llaman `<Target>Mixin` y las 11 interfaces `<Target>Accessor`; se actualizaron los 21 registros JSON.

Los nombres de los ajustes son claves persistidas por Meteor. Las configuraciones guardadas con los nombres anteriores pueden necesitar reconfigurarse: esta tarea no incorpora una migración de claves antiguas.
