/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.systems.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import nl.oxod.nekoclient.events.world.TickEvent;
import nl.oxod.nekoclient.mixininterface.IServerboundMovePlayerPacket;
import nl.oxod.nekoclient.settings.BoolSetting;
import nl.oxod.nekoclient.settings.DoubleSetting;
import nl.oxod.nekoclient.settings.IntSetting;
import nl.oxod.nekoclient.settings.Setting;
import nl.oxod.nekoclient.settings.SettingGroup;
import nl.oxod.nekoclient.systems.modules.Categories;
import nl.oxod.nekoclient.systems.modules.Module;
import nl.oxod.nekoclient.utils.player.ChatUtils;

public class MaceDMG extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> fakeHeight = sgGeneral.add(new DoubleSetting.Builder()
        .name("fake-height")
        .description("Fake height in blocks sent before the hit.")
        .defaultValue(22.0)
        .min(1.0)
        .max(50.0)
        .sliderRange(1.0, 35.0)
        .build()
    );

    private final Setting<Integer> attackInterval = sgGeneral.add(new IntSetting.Builder()
        .name("attack-interval")
        .description("Attack interval in ms.")
        .defaultValue(55)
        .min(25)
        .max(2000)
        .sliderRange(25, 500)
        .build()
    );

    private final Setting<Boolean> globalDetection = sgGeneral.add(new BoolSetting.Builder()
        .name("global-attack-detect")
        .description("Intercepts all attack events when enabled.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-switch")
        .description("Auto-switch to mace when one is in the inventory.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> normalPackets = sgGeneral.add(new IntSetting.Builder()
        .name("normal-packets")
        .description("Normal position packets before the high packet.")
        .defaultValue(4)
        .min(1)
        .max(10)
        .sliderRange(1, 8)
        .build()
    );

    private final Setting<Boolean> chatInfo = sgGeneral.add(new BoolSetting.Builder()
        .name("chat-info")
        .description("Show trigger info in chat.")
        .defaultValue(false)
        .build()
    );

    private long lastAttackTime;
    private float lastCooldown;

    public MaceDMG() {
        super(Categories.Combat, "mace-dmg", "Fakes fall height on mace hits for max damage.");
    }

    @Override
    public void onActivate() {
        lastAttackTime = 0;
        lastCooldown = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null) return;
        if (nl.oxod.nekoclient.utils.player.PlayerUtils.getGameMode() == GameType.SPECTATOR) return;

        if (autoSwitch.get() && !isHoldingMace(mc.player)) {
            switchToMace();
        }

        if (!isHoldingMace(mc.player)) return;

        boolean shouldAttack = false;

        if (globalDetection.get()) {
            float currentCooldown = mc.player.getAttackStrengthScale(0);
            if (lastCooldown > 0.5f && currentCooldown < 0.01f) {
                shouldAttack = true;
            }
            lastCooldown = currentCooldown;
        } else {
            if (mc.options.keyAttack.isDown()) {
                // Manual trigger - rely on player clicking
            }
            if (mc.options.keyAttack.consumeClick()) {
                shouldAttack = true;
            }
        }

        if (!shouldAttack) return;

        long now = System.currentTimeMillis();
        if (lastAttackTime == 0) lastAttackTime = now;
        long elapsed = now - lastAttackTime;
        if (elapsed < attackInterval.get()) return;
        lastAttackTime = now;

        performMaceDMG();
    }

    private void performMaceDMG() {
        if (mc.player == null || mc.getConnection() == null) return;
        double height = fakeHeight.get();
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();

        sendPacket(x, y, z, false);
        sendPacket(x, y + height, z, false);
        sendPacket(x, y, z, false);
        mc.player.fallDistance = (float) height;

        if (chatInfo.get()) {
            info("MaceDMG triggered | height=" + String.format("%.1f", height));
        }
    }

    private void sendPacket(double x, double y, double z, boolean onGround) {
        if (mc.player == null || mc.getConnection() == null) return;
        ServerboundMovePlayerPacket packet = new ServerboundMovePlayerPacket.Pos(x, y, z, onGround, mc.player.horizontalCollision);
        ((IServerboundMovePlayerPacket) packet).neko$setTag(1337);
        mc.getConnection().send(packet);
    }

    private boolean isHoldingMace(Player player) {
        return player.getMainHandItem().is(Items.MACE) || player.getOffhandItem().is(Items.MACE);
    }

    private void switchToMace() {
        Player player = mc.player;
        if (player == null) return;
        if (player.getMainHandItem().is(Items.MACE)) return;
        if (player.getOffhandItem().is(Items.MACE)) return;

        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getItem(i).is(Items.MACE)) {
                player.getInventory().setSelectedSlot(i);
                return;
            }
        }

        for (int i = 9; i < 36; i++) {
            if (player.getInventory().getItem(i).is(Items.MACE)) {
                nl.oxod.nekoclient.utils.player.InvUtils.move().from(i).toHotbar(player.getInventory().getSelectedSlot());
                return;
            }
        }
    }
}
