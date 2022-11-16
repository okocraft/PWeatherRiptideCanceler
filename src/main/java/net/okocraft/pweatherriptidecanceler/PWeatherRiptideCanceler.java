package net.okocraft.pweatherriptidecanceler;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class PWeatherRiptideCanceler extends JavaPlugin implements Listener {

    private final Set<UUID> mayClientSideRiptide = new HashSet<>();

    private ProtocolManager protocol;

    @Override
    public void onLoad() {
        protocol = ProtocolLibrary.getProtocolManager();
    }

    @Override
    public void onEnable() {
        protocol.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Client.BLOCK_DIG) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (event.getPacket().getPlayerDigTypes().readSafely(0) != EnumWrappers.PlayerDigType.RELEASE_USE_ITEM) {
                    return;
                }

                Player player = event.getPlayer();
                int riptide = player.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.RIPTIDE);
                if (riptide == 0 || player.isInWater()) {
                    return;
                }

                if (!player.isInRain() && isInRainClientSide(player)) {
                    mayClientSideRiptide.add(player.getUniqueId());
                }

            }
        });

        getServer().getPluginManager().registerEvents(new Listener() {

            @EventHandler
            private void onPlayerMove(PlayerMoveEvent event) {
                if (mayClientSideRiptide.contains(event.getPlayer().getUniqueId())) {
                    event.setCancelled(true);
                    mayClientSideRiptide.remove(event.getPlayer().getUniqueId());
                }
            }

        }, this);
    }

    public boolean isInRainClientSide(Player client) {
        Location pos = client.getLocation();
        return isRainingClientSideAt(client, pos) || isRainingClientSideAt(client, new Location(
                client.getWorld(),
                pos.getX(),
                client.getBoundingBox().getMaxY(),
                pos.getZ()
        ));
    }

    public boolean isRainingClientSideAt(Player client, Location pos) {
        World world = pos.getWorld();
        if (world == null) {
            return false;
        }

        Block block = world.getBlockAt(pos);

        if (client.getPlayerWeather() != WeatherType.DOWNFALL) {
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
