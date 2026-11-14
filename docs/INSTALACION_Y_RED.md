# Instalación y red

REP funciona en cualquier red IP entre equipos, **con o sin internet**:

- LAN por cable o Wi-Fi.
- Red Wi-Fi propia creada por el servidor (hotspot).
- Bluetooth PAN.

Un equipo hace de servidor y puede usar el cliente al mismo tiempo. Los demás equipos
solo instalan el cliente.

## 1. Servidor preconfigurado (perfil `local`)

Los instaladores arrancan el servidor con `--spring.profiles.active=local`, que no
requiere configurar nada:

| Qué | Dónde (`REP_HOME`, por defecto `~/.rep`) |
|---|---|
| Base de datos embebida (H2, modo MySQL) | `datos/` |
| Archivos (material, adjuntos) | `archivos/` |
| Registros | `logs/rep-servidor.log` |
| Secreto JWT (se genera la primera vez) | `jwt.secret` |
| Identidad del servidor (UUID) | `servidor.id` |
| Nombre y descubrimiento | `red.properties` (se edita en Panel › Red) |

**Copia de seguridad:** haga copia de la carpeta `REP_HOME` completa, con el servidor
detenido.

Para usar MySQL, arranque sin el perfil `local` y defina `DB_HOST`, `DB_NAME`,
`DB_USERNAME` y `DB_PASSWORD`.

## 2. Primer acceso del administrador

1. Al instalar, el servidor crea el usuario `22` con una contraseña **provisional
   aleatoria**. La contraseña también aparece en el registro (`logs/`).
2. Abra `http://localhost:8080/admin/` **en el propio servidor**. La página de acceso
   muestra la contraseña **una sola vez**, con un aviso. Desde otro equipo nunca se
   muestra.
3. Al entrar, el panel solo permite cambiar la contraseña. La API responde
   `CAMBIO_CONTRASENA_OBLIGATORIO` (403) hasta que se cambia, también desde el
   cliente de escritorio.
4. Si no la copió a tiempo, reinicie el servidor: mientras la contraseña siga siendo
   provisional, se genera otra en cada arranque.

Si define `ADMIN_INITIAL_PASSWORD`, esa contraseña se usa y no es provisional.

## 3. Cómo se encuentran los equipos

| Mecanismo | Puerto | Detalle |
|---|---|---|
| Búsqueda | UDP 47800 | El cliente envía `{"rep":"buscar"}` por broadcast (todas las redes del equipo), por multicast `239.255.47.80` y a localhost. El servidor responde con su identidad. |
| Anuncio | UDP 47801 | El servidor publica su identidad cada 5 s, por broadcast en cada red y por multicast. |
| Aplicación | TCP 8080 | API y panel web |

- El cliente usa la IP desde la que llegó la respuesta, que es la que puede alcanzar.
  En el propio servidor usa `localhost`.
- Cada servidor tiene un `id` permanente. Si cambia de IP (DHCP, otra red), el cliente
  lo reconoce, se reconecta solo y mueve a la nueva dirección los envíos pendientes.
- La primera vez:
  - si el cliente encuentra **un** servidor, lo usa sin preguntar;
  - si encuentra varios o ninguno, muestra el selector.

  En el selector se puede escribir la dirección a mano (`192.168.1.10`).
- **Bluetooth:** empareje los equipos y active "Compartir red / PAN". Así se crea una
  interfaz IP (`bnep0` en Linux, "Conexión de red Bluetooth" en Windows) y el
  descubrimiento funciona igual. No hay soporte para Bluetooth sin red IP: el ancho de
  banda no alcanza para archivos y obligaría a otro protocolo.
- **Red propia (hotspot):**
  - en Linux con NetworkManager se crea desde Panel › Red (`nmcli`);
  - en Windows, desde Configuración › Red e Internet › Zona con cobertura inalámbrica
    móvil (el panel muestra estas instrucciones).

### Cortafuegos

En el servidor: TCP 8080 y UDP 47800/47801 de entrada (redes privadas). En los
clientes: UDP 47801 de entrada (anuncios). Sin esa regla, el cliente igualmente
encuentra el servidor buscándolo.

Ejemplo en Windows, en un símbolo del sistema como administrador:

```bat
netsh advfirewall firewall add rule name="REP Servidor TCP" dir=in action=allow protocol=TCP localport=8080 profile=private,domain
netsh advfirewall firewall add rule name="REP Descubrimiento" dir=in action=allow protocol=UDP localport=47800-47801 profile=private,domain
```

### Variables

| Variable | Defecto | Uso |
|---|---|---|
| `REP_HOME` | `~/.rep` | Directorio de datos |
| `REP_NOMBRE` | `REP · <equipo>` | Nombre inicial visible para los clientes |
| `REP_DESCUBRIMIENTO` | `true` | Responder y anunciarse por UDP |
| `REP_DESCUBRIMIENTO_PUERTO` / `REP_ANUNCIO_PUERTO` | 47800 / 47801 | Puertos UDP |
| `REP_HOTSPOT` | `true` | Permitir crear la red Wi-Fi desde el panel |
| `SERVER_PORT` | 8080 | Puerto HTTP |

## 4. Empaquetado

- **Linux (`.deb` / `.rpm` / portable):** `empaquetado/servidor-linux.sh`. Requiere
  JDK 17+ con `jpackage`, y `fakeroot` + `dpkg-deb` (o `rpmbuild`). El paquete incluye
  su propio Java.
- **Windows:** use `jpackage --type app-image` con los mismos argumentos que el script
  de Linux (`--arguments --spring.profiles.active=local`, icono
  `empaquetado/recursos/rep-servidor.ico`). Después empaquete la carpeta con Inno Setup
  y agregue las reglas de cortafuegos de arriba. `jpackage --type msi` también sirve,
  pero requiere WiX 3.
