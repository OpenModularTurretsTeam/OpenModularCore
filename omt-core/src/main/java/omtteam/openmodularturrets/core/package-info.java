/**
 * Core game logic of Open Modular Turrets: turret base behaviour, upgrades and addons, ammo storage, targets and
 * targeting, turret and projectile maths, the damage rules for trusted players, the generator, and the base part of
 * the memory card ({@code card.BaseCardRules}).
 * Trust lists, access levels, machine modes and the memory card data and trust rules are in OMLib's core
 * ({@code omtteam.omlib.core}).
 * <p>
 * Everything in this package (and its subpackages) must stay free of Minecraft, Fabric and other game imports so it
 * can be unit-tested in isolation. The game is reached only through small interfaces (adapters) that the
 * Minecraft layer implements.
 */
package omtteam.openmodularturrets.core;
