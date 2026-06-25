package xyz.uninenville.turfwars.kit;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resource.ResourceFinder;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import xyz.nucleoid.plasmid.api.util.TinyRegistry;
import xyz.uninenville.turfwars.TurfWars;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;

public class KitRegistry {
    private static final TinyRegistry<TurfWarsKit> KIT_REGISTRY = TinyRegistry.create();
    private static final ResourceFinder FINDER = ResourceFinder.json("kit");

    public static void register() {
        ResourceLoader serverData = ResourceLoader.get(ResourceType.SERVER_DATA);

        serverData.registerReloader(TurfWars.id("kit"), (SynchronousResourceReloader) manager -> {
            KIT_REGISTRY.clear();

            FINDER.findResources(manager).forEach((path, resource) -> {
                try {
                    try (Reader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
                        JsonElement json = JsonParser.parseReader(reader);

                        Identifier identifier = FINDER.toResourceId(path);

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
        return getKits().stream().toList().get(Random.create().nextInt(getKits().size()));
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
