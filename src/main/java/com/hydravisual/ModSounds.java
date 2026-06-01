package com.hydravisual;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * Registers all custom sounds for SeladalaVisual.
 * Must be called during onInitializeClient().
 */
public class ModSounds {
    public static SoundEvent HITSOUND_POP;
    public static SoundEvent HITSOUND_UWU;
    public static SoundEvent HITSOUND_CRIT1;
    public static SoundEvent HITSOUND_CRIT2;

    public static void register() {
        HITSOUND_POP   = reg("hitsound.pop");
        HITSOUND_UWU   = reg("hitsound.uwu");
        HITSOUND_CRIT1 = reg("hitsound.crit1");
        HITSOUND_CRIT2 = reg("hitsound.crit2");
    }

    private static SoundEvent reg(String name) {
        Identifier id = Identifier.of("hydravisual", name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }
}
