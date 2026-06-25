package xyz.uninenville.turfwars.game;

import xyz.uninenville.turfwars.TurfWars;
import xyz.nucleoid.plasmid.api.game.stats.StatisticKey;

public class StatisticKeys {
    public static final StatisticKey<Integer> GAMES_PLAYED = StatisticKey.intKey(TurfWars.id("games_played"));
    public static final StatisticKey<Integer> GAMES_WON = StatisticKey.intKey(TurfWars.id("games_won"));
    public static final StatisticKey<Integer> GAMES_LOST = StatisticKey.intKey(TurfWars.id("games_lost"));
    public static final StatisticKey<Integer> KILLS = StatisticKey.intKey(TurfWars.id("kills"));
    public static final StatisticKey<Integer> MELEE_KILLS = StatisticKey.intKey(TurfWars.id("kills_melee"));
    public static final StatisticKey<Integer> RANGED_KILLS = StatisticKey.intKey(TurfWars.id("kills_ranged"));
    public static final StatisticKey<Integer> HIGHEST_KILLSTREAK = StatisticKey.intKey(TurfWars.id("highest_killstreak"));
    public static final StatisticKey<Integer> DEATHS = StatisticKey.intKey(TurfWars.id("deaths"));
    public static final StatisticKey<Integer> ARROWS_SHOT = StatisticKey.intKey(TurfWars.id("arrows_shot"));
    public static final StatisticKey<Integer> ARROWS_HIT = StatisticKey.intKey(TurfWars.id("arrows_hit"));
    public static final StatisticKey<Integer> TURF_LINES_CONQUERED = StatisticKey.intKey(TurfWars.id("turf_lines_conquered"));
}
