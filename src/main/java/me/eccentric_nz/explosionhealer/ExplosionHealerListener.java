package me.eccentric_nz.explosionhealer;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;
import java.util.Random;

public class ExplosionHealerListener implements Listener {

    private final ExplosionHealer plugin;
    private final Random random = new Random();
    private boolean isRestoring = false;

    ExplosionHealerListener(ExplosionHealer plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        if (entity == null) {
            return;
        }
        if ((event.getEntityType() == EntityType.PRIMED_TNT || event.getEntityType() == EntityType.MINECART_TNT) && !plugin.getConfig().getBoolean("restore.tnt")) {
            return;
        }
        if (entity instanceof Creeper && !plugin.getConfig().getBoolean("restore.creeper")) {
            return;
        }
        if ((entity instanceof Wither || entity instanceof WitherSkull) && !plugin.getConfig().getBoolean("restore.wither")) {
            return;
        }
        isRestoring = true;
        Iterator<Block> blockIterator = event.blockList().iterator();
        while (blockIterator.hasNext()) {
            Block block = blockIterator.next();
            if (block.getType() != Material.AIR) {
                restoreBlock(block);
            }
        }
        event.setYield(plugin.getConfig().getInt("yield"));
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> isRestoring = false, plugin.getConfig().getInt("delay.max"));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        Block block = event.getBlock();
        if (block == null) {
            return;
        }
        if (!(block.getBlockData() instanceof Bed) || !plugin.getConfig().getBoolean("restore.bed")) {
            return;
        }
        isRestoring = true;
        Iterator<Block> blockIterator = event.blockList().iterator();
        while (blockIterator.hasNext()) {
            Block next = blockIterator.next();
            if (next.getType() != Material.AIR) {
                restoreBlock(next);
            }
        }
        event.setYield(plugin.getConfig().getInt("yield"));
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> isRestoring = false, plugin.getConfig().getInt("delay.max"));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    private void onSandFall(EntityChangeBlockEvent event) {
        if (isRestoring && event.getEntityType() == EntityType.FALLING_BLOCK && event.getTo() == Material.AIR) {
            event.setCancelled(true);
            // update the block to fix a visual client bug, but don't apply physics
            event.getBlock().getState().update(false, false);
        }
    }

    private void restoreBlock(Block block) {
        int min = plugin.getConfig().getInt("delay.min");
        int max = plugin.getConfig().getInt("delay.max");
        BlockState state = block.getState();
        String[] signLines = state instanceof Sign ? ((Sign) state).getLines() : null;
        ItemStack[] items = state instanceof InventoryHolder ? ((InventoryHolder) state).getInventory().getContents() : null;
        if (items != null) {
            for (int i = 0; i < items.length; i++) {
                if (items[i] != null) {
                    items[i] = items[i].clone();
                }
            }
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            state.update(true);
            BlockState newState = block.getState();
            // restore inventory contents & signs lines
            if (signLines != null && newState instanceof Sign) {
                Sign sign = (Sign) newState;
                for (int i = 0; i < 4; i++) {
                    sign.setLine(i, signLines[i]);
                }
                sign.update();
            } else if (items != null && newState instanceof InventoryHolder) {
                ((InventoryHolder) newState).getInventory().setContents(items);
            }
        }, min + random.nextInt(max - min));
    }
}
