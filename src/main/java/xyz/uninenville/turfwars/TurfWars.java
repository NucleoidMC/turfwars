package xyz.uninenville.turfwars;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.uninenville.turfwars.attachment.ModAttachments;
import xyz.uninenville.turfwars.command.TurfWarsCommand;
import xyz.uninenville.turfwars.component.ModComponents;
import xyz.uninenville.turfwars.config.TurfWarsConfig;
import xyz.uninenville.turfwars.game.TurfWarsGame;
import xyz.uninenville.turfwars.game.TurfWarsPlayerDataStorage;
import xyz.uninenville.turfwars.game.TurfWarsWaiting;
import xyz.uninenville.turfwars.kit.KitRegistry;
import xyz.nucleoid.plasmid.api.game.GameAttachment;
import xyz.nucleoid.plasmid.api.game.GameTypes;

public class TurfWars implements ModInitializer {
	public static final String MOD_ID = "turfwars";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final GameAttachment<TurfWarsGame> GAME = GameAttachment.create(TurfWars.id("game"));
    public static final GameAttachment<TurfWarsWaiting> WAITING = GameAttachment.create(TurfWars.id("waiting"));

	@Override
	public void onInitialize() {
		GameTypes.register(TurfWars.id(MOD_ID), TurfWarsConfig.CODEC, TurfWarsWaiting::open);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			TurfWarsCommand.register(dispatcher);
		});
		KitRegistry.register();
		ModComponents.initialize();
		ModAttachments.initialize();
		TurfWarsPlayerDataStorage.initialize();
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

}
