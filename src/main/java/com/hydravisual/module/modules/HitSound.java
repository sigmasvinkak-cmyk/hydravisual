package com.hydravisual.module.modules;

import com.hydravisual.module.Module;
import com.hydravisual.module.Setting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;

import java.util.List;

/**
 * HitSound — воспроизводит звук при ударе по сущности.
 * Настройки:
 *   - Звук: pop / uwu / crit1 / crit2
 *   - Только крит: если включено, звук только при критических ударах
 *   - Цель: Все / Mobs / Players / ZloyMobs
 */
public class HitSound extends Module {

    private final Setting soundChoice;
    private final Setting onlyCrit;
    private final Setting targetFilter;

    public HitSound() {
        super("HitSound", "Звук при ударе", Category.UTILITY);

        soundChoice = addSetting(new Setting("Звук",
                List.of("pop", "uwu", "crit1", "crit2"), 0));

        onlyCrit = addSetting(new Setting("Только крит", false));

        targetFilter = addSetting(new Setting("Цель",
                List.of("Все", "Mobs", "Players", "ZloyMobs"), 0));
    }

    @Override public void onEnable() {}
    @Override public void onDisable() {}

    /**
     * Вызывается из мixin когда игрок бьёт сущность.
     * @param target   — сущность которую ударили
     * @param isCrit   — был ли удар критическим
     */
    public void onHit(Entity target, boolean isCrit) {
        if (!isEnabled()) return;

        // Проверка "только крит"
        if (onlyCrit.isEnabled() && !isCrit) return;

        // Проверка фильтра цели
        if (!matchesFilter(target)) return;

        playSound();
    }

    private boolean matchesFilter(Entity e) {
        return switch (targetFilter.getSelected()) {
            case "Все" -> true;
            case "Mobs" -> e instanceof AnimalEntity || e instanceof MobEntity;
            case "Players" -> e instanceof PlayerEntity;
            case "ZloyMobs" -> isHostile(e) || e instanceof PlayerEntity;
            default -> true;
        };
    }

    private boolean isHostile(Entity e) {
        return e instanceof HostileEntity ||
               e instanceof ZombieEntity ||
               e instanceof SkeletonEntity ||
               e instanceof SpiderEntity ||
               e instanceof CreeperEntity ||
               e instanceof EndermanEntity ||
               e instanceof WitchEntity;
    }

    private void playSound() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null) return;

        String soundId = switch (soundChoice.getSelected()) {
            case "pop"   -> "hydravisual:hitsound.pop";
            case "uwu"   -> "hydravisual:hitsound.uwu";
            case "crit1" -> "hydravisual:hitsound.crit1";
            case "crit2" -> "hydravisual:hitsound.crit2";
            default      -> "hydravisual:hitsound.pop";
        };

        SoundEvent sound = SoundEvent.of(Identifier.of(soundId));
        // Используем playSound через мир — работает на стороне клиента
        client.world.playSound(
            client.player.getX(), client.player.getY(), client.player.getZ(),
            sound, SoundCategory.PLAYERS, 1.0f, 1.0f, false
        );
    }

    // Getters для мixin
    public Setting getSoundChoice() { return soundChoice; }
    public Setting getOnlyCrit() { return onlyCrit; }
    public Setting getTargetFilter() { return targetFilter; }
}
