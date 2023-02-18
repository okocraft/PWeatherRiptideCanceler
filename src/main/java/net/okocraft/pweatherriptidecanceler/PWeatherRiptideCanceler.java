package net.okocraft.pweatherriptidecanceler;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

public final class PWeatherRiptideCanceler extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    private void onPlayerInteract(PlayerInteractEvent event) {
        handlePlayerUseTrident(event.getPlayer(), event.getHand());
    }

    private void handlePlayerUseTrident(Player player, EquipmentSlot hand) {
        if (hand != EquipmentSlot.HAND && hand != EquipmentSlot.OFF_HAND) {
            return;
        }
        ItemStack trident = player.getInventory().getItem(hand);
        if (trident == null || trident.getType() != Material.TRIDENT) {
            return;
        }

        trident = trident.clone();

        int riptide = trident.getEnchantmentLevel(Enchantment.RIPTIDE);
        if (riptide == 0 || player.isInWater()) {
            return;
        }

        if (isInRain(player, true) && !isInRain(player, false)) {
            if (hand == EquipmentSlot.OFF_HAND) {
                PlayerInventory inv = player.getInventory();
                ItemStack mainHandItem = inv.getItemInMainHand();
                inv.setItemInMainHand(inv.getItemInOffHand());
                inv.setItemInOffHand(null);
                player.dropItem(true);
                inv.setItemInMainHand(mainHandItem);
            } else {
                player.dropItem(true);
            }
            player.updateInventory();
        }
    }

    public boolean isInRain(Player client, boolean clientSide) {
        Location pos = client.getLocation();

        return isRaining(client, pos, clientSide) || isRaining(client, new Location(
                client.getWorld(),
                pos.getX(),
                client.getBoundingBox().getMaxY(),
                pos.getZ()
        ), clientSide);
    }

    public boolean isRaining(Player client, Location pos, boolean clientSide) {
        World world = pos.getWorld();
        if (world == null) {
            return false;
        }

        Block block = world.getBlockAt(pos);

        if ((clientSide && client.getPlayerWeather() != WeatherType.DOWNFALL) || (!clientSide && !world.hasStorm())) {
            return false;
        } else if (block.getLightFromSky() < 15) {
            return false;
        } else if (world.getHighestBlockYAt(pos.getBlockX(), pos.getBlockZ()) > pos.getY()) {
            return false;
        } else {
            return canRainAtPositionedBiome(pos) && block.getTemperature() >= 0.15F;
        }
    }

    public boolean canRainAtPositionedBiome(Location pos) {
        World world = pos.getWorld();
        if (world == null || world.getEnvironment() != World.Environment.NORMAL) {
            return false;
        }
        return switch (world.getBiome(pos)) {
            case DESERT,
                    SAVANNA, SAVANNA_PLATEAU, WINDSWEPT_SAVANNA,
                    BADLANDS, ERODED_BADLANDS, WOODED_BADLANDS,
                    THE_VOID -> false;
            default -> true;
        };
    }
}
