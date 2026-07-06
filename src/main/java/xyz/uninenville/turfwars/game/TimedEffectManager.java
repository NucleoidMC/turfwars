package xyz.uninenville.turfwars.game;

import com.google.common.collect.Maps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.util.PlayerRef;
import xyz.uninenville.turfwars.kit.TimedEffect;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TimedEffectManager {
    private final GameSpace gameSpace;
    private final Map<String, BlockBounds> regions;
    private final Map<PlayerRef, Map<TimedEffect, EffectInstance>> entries = Maps.newHashMap();
    private boolean shouldTick = true;

    public TimedEffectManager(GameSpace gameSpace, Map<String, BlockBounds> regions) {
        this.gameSpace = gameSpace;
        this.regions = regions;
    }

    public static TimedEffectManager addTo(GameActivity activity, Map<String, BlockBounds> regions) {
        var manager = new TimedEffectManager(activity.getGameSpace(), regions);

        activity.listen(GameActivityEvents.TICK, manager::tick);

        return manager;
    }

    public void tick() {
        if (!this.shouldTick) {
            return;
        }

        for (var playerRef : entries.keySet()) {
            playerRef.ifOnline(gameSpace, player -> {
                var playerPos = player.blockPosition();

                for (var entry : entries.get(playerRef).entrySet()) {
                    var timedEffect = entry.getKey();
                    boolean isInsideRegion = regions.entrySet().stream().anyMatch(region ->
                        timedEffect.getRegions().contains(region.getKey()) && region.getValue().contains(playerPos));

                    if (player.gameMode().isSurvival() && isInsideRegion) {
                        entry.getValue().tick();

                        if (shouldGiveEffect(entry)) {
                            giveEffect(player, timedEffect);
                            entry.getValue().onGive();
                        }
                    } else {
                        if (shouldRemoveEffect(entry)) {
                            removeEffect(player, timedEffect);
                        }

                        entry.getValue().reset();
                    }
                }
            });
        }
    }

    public void setShouldTick(boolean shouldTick) {
        this.shouldTick = shouldTick;
    }

    private boolean shouldGiveEffect(Map.Entry<TimedEffect, EffectInstance> entry) {
        return entry.getValue().ticksUntilGive == 0;
    }

    private boolean shouldRemoveEffect(Map.Entry<TimedEffect, EffectInstance> entry) {
        return entry.getValue().active && entry.getKey().removeOnRegionLeave();
    }

    private void giveEffect(ServerPlayer player, TimedEffect timedEffect) {
        player.addEffect(new MobEffectInstance(timedEffect.statusEffect()));
    }

    private void removeEffect(ServerPlayer player, TimedEffect timedEffect) {
        player.removeEffect(timedEffect.statusEffect().getEffect());
    }

    public void resetEffectGiveTimes(PlayerRef playerRef) {
        if (entries.containsKey(playerRef)) {
            entries.get(playerRef).forEach((timedEffect, entry) -> entry.reset());
        }
    }

    public void add(PlayerRef playerRef, List<TimedEffect> effects) {
        effects.forEach(effect -> add(playerRef, effect));
    }

    public void add(PlayerRef playerRef, TimedEffect effect) {
        var entry = new EffectInstance(effect.initialTime(), effect.renewalTime());

        if (entries.containsKey(playerRef)) {
            entries.get(playerRef).put(effect, entry);
        } else {
            entries.put(playerRef, Maps.newHashMap(Map.of(effect, entry)));
        }
    }

    public void remove(PlayerRef playerRef, TimedEffect effect) {
        if (entries.containsKey(playerRef)) {
            entries.get(playerRef).remove(effect);
        }
    }

    public void removeEqual(PlayerRef playerRef, TimedEffect effect) {
        if (entries.containsKey(playerRef)) {
            Set<TimedEffect> entriesToRemove = new HashSet<>();
            for (var entry : entries.get(playerRef).keySet()) {
                if (entry.equals(effect)) {
                    entriesToRemove.add(entry);
                }
            }

            entriesToRemove.forEach(entries.get(playerRef)::remove);
        }
    }

    public void removeAll(PlayerRef playerRef) {
        entries.remove(playerRef);
    }

    public static class EffectInstance {
        final int initialGiveTicks;
        final int renewalGiveTicks;
        /**
         * If {@code ticksUntilGive} is set to -1, effect should not be given again.
         */
        long ticksUntilGive;
        /**
         * Whether timed effect is active.
         */
        boolean active = false;

        public EffectInstance(int initialGiveTicks, int renewalGiveTicks) {
            this.initialGiveTicks = initialGiveTicks;
            this.renewalGiveTicks = renewalGiveTicks;
            this.ticksUntilGive = initialGiveTicks;
        }

        public void tick() {
            if (ticksUntilGive > 0) {
                this.ticksUntilGive -= 1;
            }
        }

        public void onGive() {
            this.ticksUntilGive = renewalGiveTicks;
            this.active = true;
        }

        public void reset() {
            this.ticksUntilGive = initialGiveTicks;
            this.active = false;
        }
    }
}
