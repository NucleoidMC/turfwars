package xyz.uninenville.turfwars.kit;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.RandomSource;
import xyz.nucleoid.plasmid.api.util.TinyRegistry;
import xyz.uninenville.turfwars.TurfWars;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;

public class KitRegistry {
    private static final TinyRegistry<TurfWarsKit> KIT_REGISTRY = TinyRegistry.create();
    private static final FileToIdConverter FINDER = FileToIdConverter.json("kit");

    public static void register() {
        ResourceLoader serverData = ResourceLoader.get(PackType.SERVER_DATA);

        serverData.registerReloadListener(TurfWars.id("kit"), (ResourceManagerReloadListener) manager -> {
            KIT_REGISTRY.clear();

            FINDER.listMatchingResources(manager).forEach((path, resource) -> {
                try {
                    try (Reader reader = new BufferedReader(new InputStreamReader(resource.open()))) {
                        JsonElement json = JsonParser.parseReader(reader);

                        Identifier identifier = FINDER.fileToId(path);

                        DataResult<TurfWarsKit> result = TurfWarsKit.CODEC.decode(JsonOps.INSTANCE, json).map(Pair::getFirst);

                        result.result().ifPresent(kit -> KIT_REGISTRY.register(identifier, kit));

                        result.error().ifPresent(error -> TurfWars.LOGGER.error("Failed to decode kit at {}: {}", path, error));
                    }
                } catch (IOException e) {
                    TurfWars.LOGGER.error("Failed to load kit at {}", path, e);
                }
            });
        });
    }

    public static TurfWarsKit getKit(Identifier id) {
        return KIT_REGISTRY.get(id);
    }

    public static TurfWarsKit getRandomKit() {
        return getKits().stream().toList().get(RandomSource.create().nextInt(getKits().size()));
    }

    public static Identifier getKitId(TurfWarsKit kit) {
        return KIT_REGISTRY.getIdentifier(kit);
    }

    public static Collection<Identifier> getKitIdentifiers() {
        return KIT_REGISTRY.keySet();
    }

    public static Collection<TurfWarsKit> getKits() {
        return KIT_REGISTRY.values();
    }
}
