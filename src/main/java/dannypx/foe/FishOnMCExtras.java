package dannypx.foe;

import dannypx.foe.config.Configs;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.Person;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class FishOnMCExtras implements ModInitializer {
	public static final String MOD_ID = "fishonmcextras";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final String SLUG = "iau5kAOK";
	public static String VERSION = FishOnMCExtras.getModVersion();
	public static List<String> AUTHORS = FishOnMCExtras.getAuthors();
	public static List<String> CONTRIBUTORS = FishOnMCExtras.getContributors();
	public static Integer HUD_VERSION = FishOnMCExtras.getVersion("hud_version");
	public static Integer HUD_ICON_VERSION = FishOnMCExtras.getVersion("hud_icon_version");
	public static Integer BUTTON_VERSION = FishOnMCExtras.getVersion("button_version");
	public static Integer NOTIFICATION_VERSION = FishOnMCExtras.getVersion("notification_version");
	public static Integer CHAT_NOTIFICATION_VERSION = FishOnMCExtras.getVersion("chat_notification_version");
	public static Integer CHAT_TRIGGER_VERSION = FishOnMCExtras.getVersion("chat_trigger_version");
	public static Integer TIMER_VERSION = FishOnMCExtras.getVersion("timer_version");
	public static Integer EVENT_TRIGGER_VERSION = FishOnMCExtras.getVersion("event_trigger_version");
	public static Integer TRACKER_VERSION = FishOnMCExtras.getVersion("tracker_version");
	public static Integer SNIPPET_VERSION = FishOnMCExtras.getVersion("snippet_version");

	@Override
	public void onInitialize() {
		Configs.init();
	}

	private static String getModVersion() {
		if(getModContainer().isPresent()) {
			return getModContainer().get().getMetadata().getVersion().getFriendlyString();
		}
		return "N/A";
	}

	private static List<String> getAuthors() {
		if(getModContainer().isPresent()) {
			return getModContainer().get().getMetadata().getAuthors().stream().map(Person::getName).toList();
		}
		return List.of();
	}

	private static List<String> getContributors() {
		if(getModContainer().isPresent()) {
			return getModContainer().get().getMetadata().getContributors().stream().map(Person::getName).toList();
		}
		return List.of();
	}

	private static Integer getVersion(String key) {
		if(getModContainer().isPresent()) {
			return getModContainer().get().getMetadata().getCustomValue(key).getAsNumber().intValue();
		}
		return -1;
	}

	private static Optional<ModContainer> getModContainer() {
		return FabricLoader.getInstance().getModContainer(MOD_ID);
	}

	public static String getMinecraftVersion() {
		return FabricLoader.getInstance()
				.getModContainer("minecraft")
				.map(container -> container.getMetadata().getVersion().getFriendlyString())
				.orElse(null);
	}
}