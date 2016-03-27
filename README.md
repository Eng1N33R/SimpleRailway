# SimpleRailway
A dead simple Spigot plugin that implements minecart spawners and despawners. Spawners are also able to propel carts forward along a specified direction. As soon as a minecart comes close enough to a despawner, its passenger is ejected, and the cart itself is removed.

## Usage
* **/spawner** - Interacts with spawners
 * **/spawner create _name [x y z] direction_** - Creates a minecart spawner with the given name and the given direction. Direction may be `n`, `w`, `s` or `e`, according to Minecraft's cardinal directions. `x`, `y` and `z` are coordinates - if they are not given, the player's current coordinates are used.
 * **/spawner delete _name_** - Deletes the spawner with the given name.
 * **/spawner edit _name field value_** - Changes a value of the spawner with the given name. `field` may be `x`, `y`, `z`, `direction` or `world`, where `world` takes a world name.
 * **/spawner activate _name [push]_** - Activates the spawner with the given name. If `push` is `true`, the minecart is also pushed in the direction given in **/spawner create**.
 * **/spawner list** - Displays a list of all currently existing spawners.
* **/despawner** - Interacts with despawners
 * **/despawner create _name [x y z]_** - Creates a minecart despawner with the given name. Similar to **/spawner create**.
 * **/despawner delete _name_** - Deletes the despawner with the given name.
 * **/despawner edit _name field value_** - Changes a value of the despawner with the given name. `field` may be `x`, `y`, `z` or `world`, where `world` takes a world name.
 * **/despawner list** - Displays a list of all currently existing despawners.

All commands have corresponding permissions. A player _must_ have the `simplerailway.[spawner/despawner].root` permission to be able to use the respective command.

All minecarts spawned by a spawner are untouchable by non-ops unless they have the `simplerailway.cartdamage` permission.
