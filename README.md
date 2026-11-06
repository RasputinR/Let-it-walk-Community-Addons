# Let it Walk! Community Addons

Add-ons for the **Let it Walk!** Create addon (Minecraft 1.21.1, NeoForge).

## Walker Controller Compat

**Download:** [`releases/walker_controller_compat-1.1.0.jar`](releases/walker_controller_compat-1.1.0.jar). Open it, then click the download button (↓) at the top right.

Requires Let it Walk! 0.1.1+, Create Tweaked Controllers 1.2.7+, Create 6.0.10+ and Sable 2.0.x.
Put it in your `mods` folder. On a multiplayer server, install it on the server too.

What it does:
- **Use a Tweaked Controller while piloting.** Right-click works for the controller or a Tweaked Lectern while seated, and right-clicking Pilot Controls no longer switches the controller off.
- **Limbs 1 to 10.** Keys 1 to 9 move limbs 1 to 9, and **0** moves limb 10 (rebind it in Controls under "Move limb 10").
- **Drive walkers over Create frequencies.** Shift-right-click the Pilot Controls and use the arrows to reach the receive pages (Move, Body, Abilities, Gears). Put two items in an action's slots to set its frequency, then bind a Tweaked Controller (or any Redstone Link) to it. A partial stick push turns smoothly, and Gear Up/Gear Down shift one gear per press.

## Building from source

The build compiles against the mods' own jars, which are not included here. Copy these into `walker-controller-compat/libs/`:
`let_it_walk-0.1.1.jar`, `create_tweaked_controllers-1.21.1-1.2.7.jar`, `create-1.21.1-6.0.10.jar`, `sable-neoforge-1.21.1-2.0.6.jar`.
Then copy these into `libs/bundled/`; they're the jars bundled inside the Create and Sable jars, under `META-INF/jarjar`:
`sable-companion-common-*.jar`, `ponder-neoforge-*.jar`, `Registrate-*.jar`.
Run `./gradlew build`.
