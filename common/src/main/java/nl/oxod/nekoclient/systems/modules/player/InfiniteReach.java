/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.systems.modules.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.oxod.nekoclient.events.render.Render3DEvent;
import nl.oxod.nekoclient.events.world.TickEvent;
import nl.oxod.nekoclient.mixininterface.IServerboundMovePlayerPacket;
import nl.oxod.nekoclient.renderer.ShapeMode;
import nl.oxod.nekoclient.settings.*;
import nl.oxod.nekoclient.systems.modules.Categories;
import nl.oxod.nekoclient.systems.modules.Module;
import nl.oxod.nekoclient.utils.player.SwingMode;
import nl.oxod.nekoclient.utils.render.color.SettingColor;
import nl.oxod.nekoclient.utils.world.BlockUtils;

public class InfiniteReach extends Module {
  private final SettingGroup sgGeneral = settings.getDefaultGroup();
  private final SettingGroup sgRender = settings.createGroup("Render");

  private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
    .name("mode")
    .description("Vanilla is conservative; Paper permits a longer packet path.")
    .defaultValue(Mode.Vanilla)
    .build());

  private final Setting<Double> vanillaRange = sgGeneral.add(new DoubleSetting.Builder()
    .name("vanilla-range")
    .description("Maximum target distance in Vanilla mode.")
    .defaultValue(22)
    .min(1)
    .sliderRange(1, 22)
    .visible(() -> mode.get() == Mode.Vanilla)
    .build());

  private final Setting<Double> paperRange = sgGeneral.add(new DoubleSetting.Builder()
    .name("paper-range")
    .description("Maximum target distance in Paper mode.")
    .defaultValue(59)
    .min(1)
    .sliderRange(1, 99)
    .visible(() -> mode.get() == Mode.Paper)
    .build());

  private final Setting<Integer> movementPackets = sgGeneral.add(new IntSetting.Builder()
    .name("movement-packets")
    .description("Ground-state packets sent before the teleport path.")
    .defaultValue(4)
    .min(1)
    .sliderRange(1, 10)
    .build());

  private final Setting<Boolean> clipUp = sgGeneral.add(new BoolSetting.Builder()
    .name("clip-up")
    .description("Routes Paper teleports above the start and target to get around obstacles.")
    .defaultValue(true)
    .visible(() -> mode.get() == Mode.Paper)
    .build());

  private final Setting<Boolean> returnHome = sgGeneral.add(new BoolSetting.Builder()
    .name("return-home")
    .description("Returns to your original position after the action.")
    .defaultValue(true)
    .build());

  private final Setting<Boolean> mineBlocks = sgGeneral.add(new BoolSetting.Builder()
    .name("mine-blocks")
    .description("Allows the attack key to mine the targeted block.")
    .defaultValue(true)
    .build());

  private final Setting<Boolean> useBlocks = sgGeneral.add(new BoolSetting.Builder()
    .name("use-blocks")
    .description("Allows the use key to interact with the targeted block.")
    .defaultValue(true)
    .build());

  private final Setting<Boolean> attackEntities = sgGeneral.add(new BoolSetting.Builder()
    .name("attack-entities")
    .description("Allows the attack key to hit the targeted entity.")
    .defaultValue(true)
    .build());

  private final Setting<Integer> actionDelay = sgGeneral.add(new IntSetting.Builder()
    .name("action-delay")
    .description("Ticks between remote actions while a key is held.")
    .defaultValue(5)
    .min(1)
    .sliderRange(1, 20)
    .build());

  private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
    .name("swing")
    .description("Swings the main hand when performing an action.")
    .defaultValue(true)
    .build());

  private final Setting<Boolean> renderTarget = sgRender.add(new BoolSetting.Builder()
    .name("render-target")
    .description("Renders the current remote target.")
    .defaultValue(true)
    .build());

  private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
    .name("side-color")
    .defaultValue(new SettingColor(255, 0, 180, 40))
    .visible(renderTarget::get)
    .build());

  private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
    .name("line-color")
    .defaultValue(new SettingColor(255, 0, 180, 180))
    .visible(renderTarget::get)
    .build());

  private Entity targetEntity;
  private BlockHitResult targetBlock;
  private int cooldown;

  public InfiniteReach() {
    super(Categories.Player, "infinite-reach", "Performs attacks and block actions through short packet teleports.");
  }

  @Override
  public void onActivate() {
    targetEntity = null;
    targetBlock = null;
    cooldown = 0;
  }

  @EventHandler
  private void onTick(TickEvent.Pre event) {
    if (mc.player == null || mc.level == null || mc.getConnection() == null) return;

    updateTarget();
    if (cooldown > 0) cooldown--;
    if (cooldown > 0) return;

    boolean attack = mc.options.keyAttack.isDown();
    boolean use = mc.options.keyUse.isDown();
    if (!attack && !use) return;

    if (attack && targetEntity != null && attackEntities.get()) {
      performAt(targetEntity.position(), () -> mc.getConnection().send(new ServerboundAttackPacket(targetEntity.getId())));
    } else if (targetBlock != null && attack && mineBlocks.get()) {
      BlockPos pos = targetBlock.getBlockPos();
      performAt(Vec3.atCenterOf(pos), () -> {
        mc.gameMode.startPrediction(mc.level, sequence -> new ServerboundPlayerActionPacket(
          ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, targetBlock.getDirection(), sequence));
        mc.gameMode.startPrediction(mc.level, sequence -> new ServerboundPlayerActionPacket(
          ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, targetBlock.getDirection(), sequence));
      });
    } else if (targetBlock != null && use && useBlocks.get() && !mc.player.isUsingItem()) {
      performAt(Vec3.atCenterOf(targetBlock.getBlockPos()), () ->
        BlockUtils.interact(targetBlock, InteractionHand.MAIN_HAND, false));
    } else return;

    cooldown = actionDelay.get();
  }

  private void updateTarget() {
    double range = mode.get() == Mode.Vanilla ? vanillaRange.get() : paperRange.get();
    Vec3 eyes = mc.player.getEyePosition();
    Vec3 look = mc.player.getViewVector(1);
    Vec3 end = eyes.add(look.scale(range));

    EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
      mc.player,
      eyes,
      end,
      mc.player.getBoundingBox().expandTowards(look.scale(range)).inflate(1),
      entity -> entity != mc.player && entity.isAlive() && entity.isAttackable() && !entity.isInvulnerable(),
      range * range
    );
    targetEntity = entityHit == null ? null : entityHit.getEntity();

    HitResult hit = mc.level.clip(new ClipContext(eyes, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
    targetBlock = hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK ? blockHit : null;
  }

  private void performAt(Vec3 target, Runnable action) {
    Vec3 start = mc.player.position();
    Vec3 destination = findSafePosition(target);
    if (destination == null) {
      warning("No collision-free position was found near the target.");
      return;
    }

    for (int i = 0; i < movementPackets.get(); i++) {
      mc.getConnection().send(new ServerboundMovePlayerPacket.StatusOnly(false, mc.player.horizontalCollision));
    }

    if (mode.get() == Mode.Paper && clipUp.get()) {
      double height = Math.min(paperRange.get(), 64);
      sendMove(start.add(0, height, 0));
      sendMove(destination.add(0, height, 0));
    }
    sendMove(destination);
    action.run();
    SwingMode.of(swing.get()).attackSwing(InteractionHand.MAIN_HAND);

    if (returnHome.get()) {
      if (mode.get() == Mode.Paper && clipUp.get()) {
        double height = Math.min(paperRange.get(), 64);
        sendMove(destination.add(0, height, 0));
        sendMove(start.add(0, height, 0));
      }
      sendMove(start);
      mc.player.setPos(start);
    } else mc.player.setPos(destination);
  }

  private Vec3 findSafePosition(Vec3 target) {
    for (int radius = 0; radius <= 3; radius++) {
      for (int x = -radius; x <= radius; x++) {
        for (int z = -radius; z <= radius; z++) {
          for (int y = -1; y <= 2; y++) {
            Vec3 candidate = new Vec3(target.x + x, Math.floor(target.y) + y, target.z + z);
            AABB box = mc.player.getBoundingBox().move(
              candidate.x - mc.player.getX(), candidate.y - mc.player.getY(), candidate.z - mc.player.getZ());
            if (mc.level.noCollision(mc.player, box)) return candidate;
          }
        }
      }
    }
    return null;
  }

  private void sendMove(Vec3 pos) {
    ServerboundMovePlayerPacket packet = new ServerboundMovePlayerPacket.PosRot(
      pos.x, pos.y, pos.z, mc.player.getYRot(), mc.player.getXRot(), false, mc.player.horizontalCollision);
    ((IServerboundMovePlayerPacket) packet).neko$setTag(1337);
    mc.getConnection().send(packet);
  }

  @EventHandler
  private void onRender(Render3DEvent event) {
    if (!renderTarget.get()) return;
    if (targetEntity != null) {
      event.renderer.box(targetEntity.getBoundingBox(), sideColor.get(), lineColor.get(), ShapeMode.Both, 0);
    } else if (targetBlock != null) {
      event.renderer.box(targetBlock.getBlockPos(), sideColor.get(), lineColor.get(), ShapeMode.Both, 0);
    }
  }

  public enum Mode {
    Vanilla,
    Paper
  }
}
