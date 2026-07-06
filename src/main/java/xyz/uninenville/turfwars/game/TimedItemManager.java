package xyz.uninenville.turfwars.game;

import com.google.common.collect.Maps;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.team.TeamManager;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.util.ItemStackBuilder;
import xyz.nucleoid.plasmid.api.util.PlayerRef;
import xyz.uninenville.turfwars.component.ModComponents;
import xyz.uninenville.turfwars.kit.TimedItem;
import xyz.uninenville.turfwars.util.ColoredBlockUtil;
import xyz.uninenville.turfwars.util.InventoryUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TimedItemManager {
    private final GameSpace gameSpace;
    private final TeamManager teamManager;
    private final Map<PlayerRef, Map<TimedItem, Entry>> entries = Maps.newHashMap();
    private boolean shouldTick = true;

    public TimedItemManager(GameSpace gameSpace, TeamManager teamManager) {
        this.gameSpace = gameSpace;
        this.teamManager = teamManager;
    }

    public static TimedItemManager addTo(GameActivity activity, @Nullable TeamManager teamManager) {
        var manager = new TimedItemManager(activity.getGameSpace(), teamManager);

        activity.listen(GameActivityEvents.TICK, manager::tick);

        return manager;
    }

    public void tick() {
        if (!this.shouldTick) {
            return;
        }

        for (var playerRef : entries.keySet()) {
            playerRef.ifOnline(gameSpace, player -> {
                for (var entry : entries.get(playerRef).entrySet()) {
                    if (hasLessThanThresholdAmount(player, entry)) {
                        entry.getValue().tick();

                        if (shouldGiveItem(entry)) {
                            giveItem(player, entry.getKey());
                            entry.getValue().resetTicksUntilGive();
                        }
                    }
                }
            });
        }
    }

    public void setShouldTick(boolean shouldTick) {
        this.shouldTick = shouldTick;
    }

    private boolean hasLessThanThresholdAmount(ServerPlayerEntity player, Map.Entry<TimedItem, Entry> entry) {
        TimedItem item = entry.getKey();
        return InventoryUtil.countItems(player, getItemStack(player, item)) < item.thresholdAmount();
    }

    private boolean shouldGiveItem(Map.Entry<TimedItem, Entry> entry) {
        return entry.getValue().ticksUntilGive <= 0;
    }

    private void giveItem(ServerPlayerEntity player, TimedItem item) {
        ItemStack stack = getItemStack(player, item);
        int count = InventoryUtil.countItems(player, stack);
        int giveCount = count + stack.getCount() <= item.thresholdAmount() ? stack.getCount() : item.thresholdAmount() % count;

        player.giveItemStack(ItemStackBuilder.of(stack).setCount(giveCount).build());
    }

    private ItemStack getItemStack(ServerPlayerEntity player, TimedItem item) {
        ItemStack stack = item.stack().copy();
        if (stack.contains(ModComponents.APPLY_TEAM_COLOR)) {
            stack = ColoredBlockUtil.getStackWithTeamColor(stack, this.teamManager, PlayerRef.of(player));
            stack.remove(ModComponents.APPLY_TEAM_COLOR);
        }

        return stack;
    }

    public void resetItemGiveTimes(PlayerRef playerRef) {
        if (entries.containsKey(playerRef)) {
            entries.get(playerRef).forEach((timedItem, entry) -> entry.resetTicksUntilGive());
        }
    }

    public void add(PlayerRef playerRef, List<TimedItem> items) {
        items.forEach(item -> add(playerRef, item));
    }

    public void add(PlayerRef playerRef, TimedItem item) {
        var entry = new Entry(item.time());

        if (entries.containsKey(playerRef)) {
            entries.get(playerRef).put(item, entry);
        } else {
            entries.put(playerRef, Maps.newHashMap(Map.of(item, entry)));
        }
    }

    public void remove(PlayerRef playerRef, TimedItem item) {
        if (entries.containsKey(playerRef)) {
            entries.get(playerRef).remove(item);
        }
    }

    public void removeEqual(PlayerRef playerRef, TimedItem item) {
        if (entries.containsKey(playerRef)) {
            Set<TimedItem> entriesToRemove = new HashSet<>();
            for (var entry : entries.get(playerRef).keySet()) {
                if (entry.equals(item)) {
                    entriesToRemove.add(entry);
                }
            }

            entriesToRemove.forEach(entries.get(playerRef)::remove);
        }
    }

    public void removeAll(PlayerRef playerRef) {
        entries.remove(playerRef);
    }

    public static class Entry {
        private final int giveInternal;
        private long ticksUntilGive;

        private Entry(int giveInternal) {
            this.giveInternal = giveInternal;
            this.ticksUntilGive = giveInternal;
        }

        public void tick() {
            this.ticksUntilGive -= 1;
        }

        public void resetTicksUntilGive() {
            this.ticksUntilGive = giveInternal;
        }
    }
}
