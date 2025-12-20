package net.woadwizard;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.woadwizard.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmacsInputClient implements ClientModInitializer {

	private static final Logger LOGGER = LoggerFactory.getLogger(EmacsInputClient.class);

	@Override
	public void onInitializeClient() {
		// Register configuration
		AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
		LOGGER.info("Emacs Input mod initialized");
	}
}
