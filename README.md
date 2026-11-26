# Classic World Selector

A compact world selector for the RuneLite login screen. It runs inside the official RuneLite client and does not require a separate launcher.

## Features

- Classic column-based world grid with the client's original sprites and bitmap fonts.
- Right-click any world to add or remove a favourite. Favourites rise within the current mode, region and membership priority groups.
- Optional separate favourites column on the left, with drag handles to save a custom order and independent mouse-wheel scrolling.
- Game-mode and region priorities: for example, German Hardcore worlds, other Hardcore worlds, other German worlds, then the remaining worlds.
- USA East and West preferences using the client's world-location data, including East + West and West + East orders.
- Members/F2P only filters, or Members/F2P first without hiding other worlds.
- Optional activity view showing the activity beside each world tile.
- On-screen dropdowns and matching RuneLite settings.
- Country flag and activity text on the login world button.
- Tooltip sizes Normal, Larger and Extra Large; the login button retains a fixed size.

Open the World button at the login screen, choose preferences, then click a world. This selects its login world only. It does not log in, switch characters, hop a logged-in character or automate any subsequent action.

Escape dismisses a dropdown or closes the selector. Arrow keys and Enter navigate the dropdowns. Keyboard input is consumed only while the user has opened this selector, preventing input from reaching the login form behind it. The plugin does not change window or component focus.

## Development

Use Java 11 or later:

```text
./gradlew check jar
./gradlew run
```

Development defaults to RuneLite `latest.release`. To reproduce a specific build, use `-PruneliteVersion=1.13.8`. The Plugin Hub manifest requests `build=standard`; the Hub replaces the local build scripts with its standard build and supplies its RuneLite version.

The production JAR contains only this plugin's classes and license. It has no bundled RuneLite client, game cache, graphics, fonts, native code or additional runtime libraries. Original graphics and glyphs are obtained through public RuneLite cache APIs while the client is running. Configuration is stored through RuneLite ConfigManager.

## Favourites

Right-click a world to toggle its favourite status without switching worlds. A gold outline marks favourites. By default, preferences take priority: selecting Hardcore puts your favourite Hardcore worlds first, then other Hardcore worlds, then your other favourites and the remaining worlds. Region and membership preferences also stay ahead of favourites. Members only/F2P only still hide worlds of the opposite membership type.

Choose **Options > Favourites: left column**, or **Favourite worlds layout** in RuneLite settings, to reserve a separate column. Favourite worlds move out of the main grid into this column and keep their saved order. Drag the small grip to the left of a favourite up or down to reorder it; dropping outside the column or pressing Escape cancels the drag. Left-click the world tile to select it normally. Scroll over the column to reach additional favourites. This works in both Compact and With activity views; activity tooltips remain disabled in With activity view. The saved list survives restarts and layout changes.

## Plugin Hub

The initial version was accepted in [Plugin Hub PR #21344](https://github.com/runelite/plugin-hub/pull/21344). Updates are submitted separately; see [REVIEW.md](REVIEW.md) for implementation and validation notes.

## License

BSD 2-Clause; see [LICENSE](LICENSE). RuneScape graphics accessed at runtime belong to Jagex and are not redistributed in this repository.
