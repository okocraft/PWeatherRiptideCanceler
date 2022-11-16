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
import org.bukkit.WeatherType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class PWeatherRiptideCanceler extends JavaPlugin implements Listener {

    private final Set<UUID> mayRiptide = new HashSet<>();

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
                if (riptide == 0) {
                    return;
                }

                if (!player.getWorld().hasStorm() && player.getPlayerWeather() == WeatherType.DOWNFALL) {
                    mayRiptide.add(player.getUniqueId());
                }

            }
        });

        getServer().getPluginManager().registerEvents(new Listener() {

            @EventHandler
            private void onPlayerMove(PlayerMoveEvent event) {
                if (mayRiptide.contains(event.getPlayer().getUniqueId())) {
                    event.setCancelled(true);
                    mayRiptide.remove(event.getPlayer().getUniqueId());
                }
            }

        }, this);
    }
}
