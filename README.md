# Classic World Selector

A compact world selector for the RuneLite login screen. It runs inside the official RuneLite client and does not require a separate launcher.

## Features

- Classic column-based world grid with the client's original sprites and bitmap fonts.
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

## Review status

Prepared for submission to the RuneLite Plugin Hub. A passing build is not approval; acceptance is decided by RuneLite reviewers. See [REVIEW.md](REVIEW.md) for the review notes.

## License

BSD 2-Clause; see [LICENSE](LICENSE). RuneScape graphics accessed at runtime belong to Jagex and are not redistributed in this repository.
