# evilFX client (Fabric, Minecraft 1.21.11)

Client-only PvP HUD, crystal PvP utilities and performance helpers. Created by evilFX.
Noir "Godfather" theme: black/slate glass, white borders, crimson accents, marionette-string logo with a rose.

## IMPORTANT: this is source code, not a ready-to-use mod
You must compile it once to get the `.jar` that goes into `.minecraft/mods`.

### Easiest: let GitHub build it
1. Create a free GitHub repo and upload the contents of this folder (keep the `.github` folder).
2. Open the repo's Actions tab -> "build" -> download the `evilfx-client-jar` artifact.

### Or build locally
JDK 21, then `gradle wrapper --gradle-version 9.2.1` and `./gradlew build`.
Result: `build/libs/evilfx-client-1.0.0.jar`

## Needed in the mods folder
Fabric Loader 0.18.5+, Fabric API. Mod Menu optional.

## Keys (rebind in Controls)
Right Shift = menu, C = hold to zoom. Hitbox toggle and crystal-stats reset are unbound by default.
The menu is also reachable from the Escape menu and from Mod Menu.

## If the build fails
Most likely spots (1.21.11 changed a lot): `feature/CustomHitboxes.java` (delete it and its `register()` line in
EvilFXClient), the Escape-menu button in EvilFXClient, and the three mixins. Paste the error to get it fixed.

## Optional serif logo
Vanilla has no serif font. To use one, add an OFL-licensed TTF as a font provider (assets/evilfx/font/) and set the
logo text style to that font.

## Discord link
`src/main/java/dev/evilfx/Branding.java` -> DISCORD_URL.
