/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.systems.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import nl.oxod.nekoclient.events.entity.player.AttackEntityEvent;
import nl.oxod.nekoclient.mixininterface.IServerboundMovePlayerPacket;
import nl.oxod.nekoclient.settings.BoolSetting;
import nl.oxod.nekoclient.settings.DoubleSetting;
import nl.oxod.nekoclient.settings.Setting;
import nl.oxod.nekoclient.settings.SettingGroup;
import nl.oxod.nekoclient.systems.modules.Categories;
import nl.oxod.nekoclient.systems.modules.Module;
import nl.oxod.nekoclient.utils.player.InvUtils;

public class MaceDMG extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> fakeHeight = sgGeneral.add(new DoubleSetting.Builder()
        .name("fake-height")
        .description("Fall distance to spoof for mace damage.")
        .defaultValue(22.0)
        .min(1.0)
        .max(50.0)
        .sliderRange(1.0, 35.0)
        .build()
    );

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-switch")
        .description("Auto-switch to mace when hitting.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> onlyPlayers = sgGeneral.add(new BoolSetting.Builder()
        .name("only-players")
        .description("Only work on players.")
        .defaultValue(false)
        .build()
    );

    public MaceDMG() {
        super(Categories.Combat, "mace-dmg", "Spoofs fall height on mace hits.");
    }

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (mc.player == null || mc.getConnection() == null || mc.level == null) return;
        if (!(event.entity instanceof LivingEntity)) return;
        if (onlyPlayers.get() && !(event.entity instanceof Player)) return;

        if (!isHoldingMace(mc.player)) {
            if (!autoSwitch.get()) return;
            switchToMace();
            if (!isHoldingMace(mc.player)) return;
        }

        double height = fakeHeight.get();
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();

        mc.getConnection().send(newMovePacket(x, y + height, z, false));
        mc.getConnection().send(newMovePacket(x, y + height + 0.01, z, false));
        mc.getConnection().send(newMovePacket(x, y, z, false));
        mc.player.fallDistance = (float) height;
    }

    private ServerboundMovePlayerPacket newMovePacket(double x, double y, double z, boolean onGround) {
        ServerboundMovePlayerPacket packet = new ServerboundMovePlayerPacket.Pos(x, y, z, onGround, mc.player.horizontalCollision);
        ((IServerboundMovePlayerPacket) packet).neko$setTag(1337);
        return packet;
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
