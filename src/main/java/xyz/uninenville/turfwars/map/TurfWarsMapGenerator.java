package xyz.uninenville.turfwars.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.MapTemplateSerializer;
import xyz.nucleoid.plasmid.api.game.GameOpenException;

import java.io.IOException;

public record TurfWarsMapGenerator(Identifier mapId) {
    public TurfWarsMap create(MinecraftServer server, boolean lobby) throws GameOpenException {
        try {
            MapTemplate template = MapTemplateSerializer.loadFromResource(server, mapId);

            return new TurfWarsMap(template, mapId, lobby);
        } catch (IOException e) {
            throw new GameOpenException(Text.literal(String.format("Failed to load map template %s", mapId)));
        }
    }
}
