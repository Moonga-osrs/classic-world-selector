# Review notes

This is a standalone plugin for the official RuneLite client. It does not load, install, update or depend on a custom launcher or another plugin.

## User actions and scope

- The custom controls are visible only at `GameState.LOGIN_SCREEN`.
- A real user click on a selectable world queues one `Client.changeWorld` call on `ClientThread`.
- The game state and plugin lifecycle are checked again inside that callback. Logged-in world hopping is never performed.
- No login attempt, character switch, synthetic input, game packet construction, menu-entry modification or repeated world-switch automation is used.
- The selector consumes keyboard input through RuneLite KeyManager only while open. It never calls focus, window activation, minimisation or positioning APIs.
- The Swing controls position themselves within the existing RuneLite layered pane; they do not modify the application window or gameplay interfaces.

## Data and dependencies

- World data comes from `Client.getWorldList` or the injected RuneLite `WorldService`.
- USA coast information comes from `Client.getEnum(EnumID.WORLD_LOCATIONS)`, as in RuneLite's own World Hopper. Unknown locations are not guessed.
- Sprites, glyph masks and metrics use public `Client.getSprites`, `getIndexSprites`, `getIndex` and `IndexDataBase.loadData`. No obfuscated names, reflection or hash/version allowlists are used.
- No account sessions, authentication variables, player profiles, credentials or recordings are accessed.
- No production filesystem access, own HTTP client/server, sockets, external processes, executable downloads or native integration is present.
- Runtime classes are compile-only RuneLite dependencies. The cache-preview utility and its cache-library dependency are absent. Test-only reflection in unit fixtures and the development launcher are excluded from the production JAR.

## Packaging and differences from the local prototype

The manifest uses the standard Plugin Hub build. The repository includes a BSD 2-Clause license, public README, a development entry point under test sources, and automated tests. No cache files or account-specific data are included.

The existing feature set is retained. Component-focus requests from the local prototype were replaced with RuneLite KeyManager input handling. This avoids focus manipulation and prevents Enter or typed input leaking to the underlying login form while the selector is open.

## References checked

- [Plugin Hub submission and standard-build instructions](https://github.com/runelite/plugin-hub)
- [Plugin Hub review scope](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review)
- [Rejected or rolled-back features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features)
- [Jagex third-party client guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1)

These notes document implementation and validation; they do not claim review approval.

## Local validation

- Java 11, RuneLite 1.13.8: `./gradlew check jar -PruneliteVersion=1.13.8` passed; 40 tests, zero failures or errors.
- Production sources also compiled using the current Plugin Hub tooling's standard build template with RuneLite 1.13.8 supplied as its dependency constraint.
- Keyboard regressions verify that input is consumed only by an open selector at the login screen and that shutdown unregisters its listener.
- The production JAR was checked for accidental test/development classes or bundled dependencies. The publication file list was checked for private paths, credential patterns, runtime profiles and caches.
- Interactive behavior of the earlier local prototype was tested separately. This standalone input adapter still requires testing in the official client; a compilation or test pass does not establish review acceptance.
