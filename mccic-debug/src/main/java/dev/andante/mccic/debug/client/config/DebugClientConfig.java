package dev.andante.mccic.debug.client.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.andante.mccic.config.ConfigHolder;
import dev.andante.mccic.debug.MCCICDebug;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public record DebugClientConfig(boolean debugHud, boolean chatAllSounds, boolean rawChat, boolean displayNameSuffix) {
    public static final Codec<DebugClientConfig> CODEC = RecordCodecBuilder.create(
        instance -> {
            DebugClientConfig defaultConfig = createDefaultConfig();
            return instance.group(
                Codec.BOOL.fieldOf("debug_hud")
                          .orElse(defaultConfig.debugHud())
                          .forGetter(DebugClientConfig::debugHud),
                Codec.BOOL.fieldOf("chat_all_sounds")
                          .orElse(defaultConfig.chatAllSounds())
                          .forGetter(DebugClientConfig::chatAllSounds),
                Codec.BOOL.fieldOf("raw_chat")
                          .orElse(defaultConfig.rawChat())
                          .forGetter(DebugClientConfig::rawChat),
                Codec.BOOL.fieldOf("display_name_suffix")
                          .orElse(defaultConfig.displayNameSuffix())
                          .forGetter(DebugClientConfig::displayNameSuffix)
            ).apply(instance, DebugClientConfig::new);
        }
    );

    public static final ConfigHolder<DebugClientConfig> CONFIG_HOLDER = new ConfigHolder<>(MCCICDebug.ID, CODEC, createDefaultConfig());

    public static DebugClientConfig getConfig() {
        return CONFIG_HOLDER.get();
    }

    public static DebugClientConfig createDefaultConfig() {
        return new DebugClientConfig(false, false, false, false);
    }
}
