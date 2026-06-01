package com.hydravisual.module.modules;

import com.hydravisual.ModSounds;
import com.hydravisual.module.Module;
import com.hydravisual.module.Setting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.client.MinecraftClient;

import java.util.List;

/**
 * HitSound — звук при ударе по сущности.
 *
 * Настройки:
 *   Звук     : pop / uwu / crit1 / crit2  (ENUM)
 *   Только крит: toggle
 *   Цели     : [Mobs] [ZloyMobs] [Players]  (TOGGLE_ROW, можно несколько)
 */
public class HitSound extends Module {

    private final Setting soundChoice;   // ENUM
    private final Setting onlyCrit;      // TOGGLE
    private final Setting targets;       // TOGGLE_ROW: 0=Mobs, 1=ZloyMobs, 2=Players

    public HitSound() {
        super("HitSound", "Звук при ударе", Category.UTILITY);

        soundChoice = addSetting(new Setting("Звук",
                List.of("pop", "uwu", "crit1", "crit2"), 0));

        onlyCrit = addSetting(new Setting("Только крит", false));

        targets = addSetting(new Setting("Цели",
                new String[]{"Mobs", "ZloyMobs", "Players"},
                new boolean[]{true, true, false}));
    }

    @Override public void onEnable() {}
    @Override public void onDisable() {}

    /**
     * Called from PlayerAttackMixin when the player attacks an entity.
     */
    public void onHit(Entity target, boolean isCrit) {
        if (!isEnabled()) return;

        // Crit filter
        if (onlyCrit.isEnabled() && !isCrit) return;

        // Target filter — if nothing enabled, skip
        if (!targets.anyRowEnabled()) return;

        // Check each enabled category
        boolean hits = false;
        if (targets.getRowState(0) && isMob(target))     hits = true;   // Mobs
        if (targets.getRowState(1) && isHostile(target)) hits = true;   // ZloyMobs
        if (targets.getRowState(2) && target instanceof PlayerEntity) hits = true; // Players

        if (!hits) return;

        playSound();
    }

    /** Passive/neutral mobs — animals, villagers, non-hostile */
    private boolean isMob(Entity e) {
        return (e instanceof AnimalEntity || e instanceof VillagerEntity
                || (e instanceof MobEntity && !isHostile(e)));
    }

    /** Hostile / злые мобы */
    private boolean isHostile(Entity e) {
        return e instanceof HostileEntity;
    }

    private void playSound() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null) return;

        SoundEvent sound = switch (soundChoice.getSelected()) {
            case "uwu"   -> ModSounds.HITSOUND_UWU;
            case "crit1" -> ModSounds.HITSOUND_CRIT1;
            case "crit2" -> ModSounds.HITSOUND_CRIT2;
            default      -> ModSounds.HITSOUND_POP;
        };

        if (sound == null) return; // not yet registered

        client.world.playSound(
            client.player.getX(),
            client.player.getY(),
            client.player.getZ(),
            sound, SoundCategory.PLAYERS,
            1.0f, 1.0f, false
        );
    }
}
