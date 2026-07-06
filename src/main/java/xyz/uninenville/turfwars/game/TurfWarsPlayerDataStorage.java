package xyz.uninenville.turfwars.game;

import eu.pb4.playerdata.api.PlayerDataApi;
import eu.pb4.playerdata.api.storage.JsonDataStorage;
import eu.pb4.playerdata.api.storage.PlayerDataStorage;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import xyz.uninenville.turfwars.kit.KitRegistry;
import xyz.uninenville.turfwars.kit.PreferredItemSlots;
import xyz.uninenville.turfwars.kit.TurfWarsKit;

import java.util.HashMap;
import java.util.Map;

public class TurfWarsPlayerDataStorage {
    public static final PlayerDataStorage<TurfWarsPlayerData> STORAGE = new JsonDataStorage<>("turfwars_player_data", TurfWarsPlayerData.class);

    public static void initialize() {
        PlayerDataApi.register(STORAGE);
    }

    public static TurfWarsPlayerData get(ServerPlayerEntity player) {
        var data = PlayerDataApi.getCustomDataFor(player, STORAGE);
        if (data == null) {
            data = new TurfWarsPlayerData();
            PlayerDataApi.setCustomDataFor(player, STORAGE, data);
        }

        return data;
    }

    public static void set(ServerPlayerEntity player, TurfWarsPlayerData data) {
        PlayerDataApi.setCustomDataFor(player, STORAGE, data);
    }

    public static class TurfWarsPlayerData {
        public Identifier kit = null;
        public final Map<Identifier, PreferredItemSlots> preferredKitItemSlots = new HashMap<>();

        public TurfWarsKit getSelectedKit() {
            return KitRegistry.getKit(this.kit);
        }

        public void setSelectedKit(TurfWarsKit kit) {
            this.kit = KitRegistry.getKitId(kit);
        }

        public PreferredItemSlots getPreferredKitItemSlots(Identifier kit) {
            if (preferredKitItemSlots.containsKey(kit)) {
                return preferredKitItemSlots.get(kit);
            }

            return new PreferredItemSlots();
        }

        public void setPreferredKitItemSlots(Identifier kit, PreferredItemSlots inventory) {
            preferredKitItemSlots.put(kit, inventory);
        }
    }
}
