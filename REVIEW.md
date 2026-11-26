# Review notes

Classic World Selector is a standalone plugin using the standard Plugin Hub build and public RuneLite APIs. It does not depend on a custom launcher. This update extends the accepted initial version with favourites, local search and grouped native configuration.

## Selection and input

- Controls are shown only at `GameState.LOGIN_SCREEN`. A selectable world requires an explicit left-click; one `Client.changeWorld` call is queued on ClientThread and checks the game state and plugin lifecycle again.
- Right-click toggles a favourite; dragging a favourite's grip only updates its saved position. Escape and outside drops cancel dragging. Neither operation selects a world.
- Search matches existing world snapshots by number, activity or mode, ignoring case, spacing and punctuation. It makes no network requests, reads no clipboard and does not persist queries.
- KeyManager handles canvas input while the selector is open. A component KeyAdapter handles Swing-panel input after a user mouse click focuses that component. No global Swing shortcuts intercept the settings sidebar. No application window activation, repositioning or minimisation is performed.
- The cog leaves the selector open and posts the public `RUNELITE_OVERLAY_CONFIG` action through OverlayMenuClicked/EventBus, targeting this plugin. RuneLite ConfigPlugin opens the native settings. No reflective access to configuration internals is used.
- No login attempt, character switch, logged-in hopping, synthetic input, packet construction or repeated switching automation is performed.

## Preferences and sidebar

Favourites are prioritised only within existing game-mode, region and membership preference groups. An optional left column keeps a manually ordered favourite list without duplicating tiles in the main grid; it scrolls independently. Explicit Members only/F2P only filters still apply. Disabling favourites retains their saved IDs and order.

Four native ConfigSection groups provide headings and separator lines: Button behaviour, World list, Favourite settings and Filters. Bold changes only compact hover tooltips using the existing b12_full bitmap font. Hover-size labels are Normal, Large and Extra large. Layout labels are On top and Separate; their stored enum values remain compatible. World info is now a boolean checkbox whose default falls back to the legacy hidden buttonLine enum so existing choices are preserved.

## Data and packaging

- World snapshots use Client.getWorldList or the injected RuneLite WorldService. USA coast data comes from Client.getEnum(EnumID.WORLD_LOCATIONS), without guesses or DNS lookups.
- Runtime sprites and fonts use public Client cache APIs. No client binaries, cache data, fonts or game graphics are bundled.
- Configuration is saved through ConfigManager. No account/session/credential/profile/recording access, production filesystem operations, own HTTP client/server, external processes, native integration or downloaded executable code is present.
- No new runtime dependencies or production reflection are introduced. Test/development classes and the isolated preview tooling are not in the production JAR.

## Validation

- Java 11 / RuneLite 1.13.8: check + jar passed; 57 tests, zero failures/errors.
- Production sources compile using the current Plugin Hub tooling standard-build template with RuneLite 1.13.8 supplied as its dependency constraint.
- Regressions cover the requested priority boundaries, favourite persistence/disable/restore, right-click/drag selection isolation, pagination and overlap, native config descriptor group order, legacy World info migration, tooltip-only bold, search editing/clear/refresh and configuration-event routing.
- AWT key redispatch and an isolated visible Swing window were tested with actual mouse/keyboard input. Search text arrived, the selector stayed visible beside a settings panel, and editing the sidebar left the query unchanged.
- Previews with actual cache bitmap glyphs/sprites were visually inspected at 150% DPI. The native ConfigPanel with RuneLiteLAF was rendered using a synthetic fixture to verify section order and separation. The helpers and their test-only reflection/cache dependency are outside this repository.
- The update was installed and checksum-verified in the local Moonga catalog before submission. Native official-client integration still requires validation; these checks do not establish Plugin Hub acceptance.

## References

- [Plugin Hub submission and standard build](https://github.com/runelite/plugin-hub)
- [Review scope](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review)
- [Rejected or rolled-back features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features)
- [Jagex client guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1)
