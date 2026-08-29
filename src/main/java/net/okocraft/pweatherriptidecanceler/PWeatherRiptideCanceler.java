package net.okocraft.pweatherriptidecanceler;

import java.lang.reflect.Field;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import org.bukkit.Material;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.util.CraftLocation;
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

    private static final Field PLUGIN_RAIN_POSITION = getPluginRainPositionField();

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
        if (trident.getType() != Material.TRIDENT) {
            return;
        }

        int riptide = trident.getEnchantmentLevel(Enchantment.RIPTIDE);
        if (riptide == 0 || player.isInWater()) {
            return;
        }

        if (!(player.getWorld() instanceof CraftWorld craftWorld)) {
            return;
        }

        Level level = craftWorld.getHandle();
        BlockPos pos1 = CraftLocation.toBlockPos(player.getLocation());
        BlockPos pos2 = new BlockPos(pos1.getX(), (int) Math.floor(player.getBoundingBox().getMaxY()), pos1.getZ());

        if (!isRainingAtForClient(player, level, pos1, pos2) || isRainingAtForServer(level, pos1, pos2)) {
            return;
        }

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

    public boolean isRainingAtForServer(Level level, BlockPos pos1, BlockPos pos2) {
        return level.isRainingAt(pos1) || level.isRainingAt(pos2);
    }

    // See: net.minecraft.world.level.Level#isRainingAt
    public boolean isRainingAtForClient(Player client, Level level, BlockPos pos1, BlockPos pos2) {
        return precipitationAtForClient(client, level, pos1) == Biome.Precipitation.RAIN ||
               precipitationAtForClient(client, level, pos2) == Biome.Precipitation.RAIN;
    }

    // See: net.minecraft.world.level.Level#precipitationAt
    public Biome.Precipitation precipitationAtForClient(Player client, Level level, BlockPos pos) {
        if (!isRainingForClient(client, level)) {
            return Biome.Precipitation.NONE;
        } else if (!level.canSeeSky(pos)) {
            return Biome.Precipitation.NONE;
        } else if (level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY() > pos.getY()) {
            return Biome.Precipitation.NONE;
        } else {
            Biome biome = level.getBiome(pos).value();
            return biome.getPrecipitationAt(pos, level.getSeaLevel());
        }
    }

    // See: net.minecraft.world.level.Level#isRaining
    private boolean isRainingForClient(Player client, Level level) {
        return level.canHaveWeather() && getRainLevelForClient(client, level) > 0.2F;
    }

    private float getRainLevelForClient(Player client, Level level) {
        if (client.getPlayerWeather() == null || !(client instanceof CraftPlayer craftPlayer)) {
            return level.getRainLevel(1.0F);
        }

        try {
            return PLUGIN_RAIN_POSITION.getFloat(craftPlayer.getHandle());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not read the client rain level", e);
        }
    }

    private static Field getPluginRainPositionField() {
        try {
            Field field = ServerPlayer.class.getDeclaredField("pluginRainPosition");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
