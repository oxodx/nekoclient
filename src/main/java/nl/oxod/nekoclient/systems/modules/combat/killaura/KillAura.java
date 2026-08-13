package nl.oxod.nekoclient.systems.modules.combat.killaura;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Attackable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;
import nl.oxod.nekoclient.mixin.KillAuraLivingEntityAccessor;
import nl.oxod.nekoclient.mixin.KillAuraMinecraftAccessor;
import nl.oxod.nekoclient.mixin.KillAuraMultiPlayerGameModeAccessor;
import nl.oxod.nekoclient.mixin.KillAuraPlayerAccessor;
import nl.oxod.nekoclient.util.ChamsHit;
import nl.oxod.nekoclient.util.CpsTracker;
import nl.oxod.nekoclient.util.InventoryHelper;
import nl.oxod.nekoclient.util.KillAuraRenderer;
import nl.oxod.nekoclient.util.KillAuraRotation;
import nl.oxod.nekoclient.util.RotationUtil;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.BooleanSupplier;

public class KillAura extends Module {
  static final int HURT_TIME = 10;

  static final double SCAN_ADDITION_MIN = 2.0D;
  static final double SCAN_ADDITION_MAX = 3.0D;

  static final int CLICK_CYCLE = 20;
  static final int CLICK_ITERATIONS = 2;
  static final long ENFORCED_CLICK_INTERVAL_MS = 1_000L;

  static final int POST_USE_SUPPRESS_TICKS = 3;

  static final int HIT_CONFIRM_TICKS = 8;

  private static final double[] ITERATION_PROPORTIONS = {
    0.05D, 0.15D, 0.25D, 0.35D, 0.45D, 0.55D, 0.65D, 0.75D, 0.85D, 0.95D
  };
  private static final int POINT_TRACKER_SAMPLES = 128;
  private static final int RAYTRACE_SAMPLES = 256;

  private static long lastClickTime;

  private static final SoundEvent HITSOUND =
    SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("nekoclient", "hitsound"));

  public enum TargetingMode {
    Type, Hp, Distance, Fov, HurtTime, Age
  }

  private final SettingGroup sgGeneral = settings.getDefaultGroup();

  private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
    .name("entities")
    .description("Entities to attack.")
    .defaultValue(EntityTypes.PLAYER)
    .onlyAttackable()
    .build()
  );
  private final Setting<TargetingMode> targeting = sgGeneral.add(new EnumSetting.Builder<TargetingMode>()
    .name("targeting")
    .description("How to select the primary target.")
    .defaultValue(TargetingMode.Fov)
    .build()
  );
  private final Setting<Integer> fov = sgGeneral.add(new IntSetting.Builder()
    .name("fov")
    .description("Attack cone in degrees.")
    .defaultValue(180)
    .min(10)
    .max(360)
    .sliderMax(360)
    .build()
  );
  private final Setting<Boolean> criticals = sgGeneral.add(new BoolSetting.Builder()
    .name("criticals")
    .description("Smart critical hits.")
    .defaultValue(true)
    .build()
  );
  private final Setting<Boolean> autoSword = sgGeneral.add(new BoolSetting.Builder()
    .name("auto-sword")
    .description("Automatically switch to the best weapon.")
    .defaultValue(false)
    .onChanged(v -> {
      if (!v) resetAutoSword();
    })
    .build()
  );
  private final Setting<Boolean> switchBack = sgGeneral.add(new BoolSetting.Builder()
    .name("switch-back")
    .description("Switch back to the previous hotbar slot after attacking.")
    .defaultValue(true)
    .visible(() -> autoSword.get())
    .onChanged(v -> {
      if (!v) {
        previousSlot = -1;
        switchBackTicks = 0;
      }
    })
    .build()
  );
  private final Setting<Boolean> keepSprint = sgGeneral.add(new BoolSetting.Builder()
    .name("keep-sprint")
    .description("Do not stop sprinting when attacking. Can flag on some servers.")
    .defaultValue(false)
    .build()
  );
  private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
    .name("range")
    .description("Maximum attack range in blocks.")
    .defaultValue(3.0)
    .min(1.0)
    .max(6.0)
    .sliderMax(6.0)
    .build()
  );
  private final Setting<Boolean> throughWalls = sgGeneral.add(new BoolSetting.Builder()
    .name("through-walls")
    .description("Attack targets even when they are behind walls.")
    .defaultValue(false)
    .build()
  );
  private final Setting<Integer> cpsMin = sgGeneral.add(new IntSetting.Builder()
    .name("cps-min")
    .description("Minimum clicks per second.")
    .defaultValue(5)
    .min(1)
    .max(20)
    .sliderMax(20)
    .build()
  );
  private final Setting<Integer> cpsMax = sgGeneral.add(new IntSetting.Builder()
    .name("cps-max")
    .description("Maximum clicks per second.")
    .defaultValue(8)
    .min(1)
    .max(20)
    .sliderMax(20)
    .build()
  );
  private final Setting<Double> turnSpeed = sgGeneral.add(new DoubleSetting.Builder()
    .name("rotation-speed")
    .description("Silent aim rotation speed in degrees per tick.")
    .defaultValue(180)
    .min(5)
    .max(360)
    .sliderMax(360)
    .build()
  );
  private final Setting<Integer> switchBackDelay = sgGeneral.add(new IntSetting.Builder()
    .name("switch-back-delay")
    .description("Ticks before switching back to the previous hotbar slot.")
    .defaultValue(20)
    .min(1)
    .max(100)
    .sliderMax(100)
    .visible(() -> autoSword.get() && switchBack.get())
    .build()
  );
  private final Setting<Boolean> render = sgGeneral.add(new BoolSetting.Builder()
    .name("render")
    .description("Render the target box and hit marker.")
    .defaultValue(true)
    .build()
  );
  private final Setting<Boolean> hitsound = sgGeneral.add(new BoolSetting.Builder()
    .name("hitsound")
    .description("Play a sound when a target is hit.")
    .defaultValue(true)
    .build()
  );

  private final SettingGroup sgRender = settings.createGroup("Render");

  private final Setting<SettingColor> renderColor = sgRender.add(new ColorSetting.Builder()
    .name("color")
    .description("Accent color for the target box and hit marker.")
    .defaultValue(new SettingColor(255, 59, 59))
    .build()
  );
  private final Setting<Boolean> renderTarget = sgRender.add(new BoolSetting.Builder()
    .name("target-box")
    .description("Draw an ESP box around the current target.")
    .defaultValue(true)
    .visible(() -> render.get())
    .build()
  );
  private final Setting<Boolean> renderHitmarker = sgRender.add(new BoolSetting.Builder()
    .name("hit-marker")
    .description("Draw a box flash where the target is hit.")
    .defaultValue(true)
    .visible(() -> render.get())
    .build()
  );
  private final Setting<Integer> renderHitmarkerDuration = sgRender.add(new IntSetting.Builder()
    .name("hit-marker-duration")
    .description("How long the hit marker is visible in milliseconds.")
    .defaultValue(500)
    .min(50)
    .max(3000)
    .sliderMax(3000)
    .visible(() -> render.get() && renderHitmarker.get())
    .build()
  );

  private final Random random = new Random();
  private final Clicker clicker = new Clicker();

  private LivingEntity currentTarget;

  public boolean attacking;

  private double closestSquaredEnemyDistance;

  private double scanAddition = nextScanAddition();

  private int previousSlot = -1;
  private int switchBackTicks;

  private int postUseSuppressTicks;

  private int pendingHitEntityId = -1;
  private int pendingHitPrevHurtTime;
  private int pendingHitTicks;

  public KillAura() {
    super(Categories.Combat, "kill-aura", "Attacks specified entities around you.");
  }

  @Override
  public void onActivate() {
    resetRuntime(false);
  }

  @Override
  public void onDeactivate() {
    resetRuntime(true);
  }

  @Override
  public String getInfoString() {
    return currentTarget != null ? currentTarget.getType().getDescription().getString() : null;
  }

  @EventHandler
  private void onTickPre(TickEvent.Pre event) {
    if (mc.player == null || mc.level == null) return;

    boolean usingItem = mc.player.isUsingItem();
    if (usingItem) postUseSuppressTicks = POST_USE_SUPPRESS_TICKS;
    else if (postUseSuppressTicks > 0) postUseSuppressTicks--;

    if (isBreakingBlock() || usingItem) {
      currentTarget = null;
      attacking = false;
      clicker.tick();
      confirmHitFeedback();
      KillAuraRotation.update(mc.player, rotationSpeed());
      return;
    }

    tickAutoSwordReset();
    clicker.tick();
    confirmHitFeedback();

    if (canRun()) {
      updateTargetRotation();
    } else {
      currentTarget = null;
    }

    KillAuraRotation.update(mc.player, rotationSpeed());

    attacking = canRun() && currentTarget != null;
    if (attacking) {
      attackPhase();
    }
  }

  @EventHandler
  private void onRender(Render3DEvent event) {
    if (render.get()) KillAuraRenderer.render(event, currentTarget, renderSettings());
  }

  private KillAuraRenderer.RenderSettings renderSettings() {
    return new KillAuraRenderer.RenderSettings(
      renderColor.get(), renderTarget.get(), renderHitmarker.get(),
      renderHitmarkerDuration.get());
  }

  private boolean isBreakingBlock() {
    return mc.gameMode != null && mc.gameMode.isDestroying();
  }

  private boolean canRun() {
    return mc.player != null
      && mc.level != null
      && mc.gameMode != null
      && mc.getConnection() != null
      && !mc.player.isDeadOrDying()
      && !mc.player.isSpectator()
      && !mc.player.isUsingItem();
  }

  private static KillAura activeInstance() {
    KillAura aura = Modules.get().get(KillAura.class);
    return aura != null && aura.isActive() ? aura : null;
  }

  public static Input modifyMovementInput(ClientInput source, Input input) {
    Minecraft mc = Minecraft.getInstance();
    if (input == null || mc == null || mc.player == null || mc.player.input != source) return input;
    KillAura aura = activeInstance();
    if (aura == null) return input;
    RotationUtil.Rotation rotation = KillAuraRotation.getCurrentRotation();
    if (rotation != null && aura.canRun()) {
      return transformSilentMovementInput(input, mc.player.getYRot(), rotation.yaw());
    }
    return input;
  }

  private static Input transformSilentMovementInput(Input input, float vanillaYaw, float silentYaw) {
    if (!input.forward() && !input.backward() && !input.left() && !input.right()) return input;

    float delta = RotationUtil.angleDifference(vanillaYaw, silentYaw);
    if (Math.abs(delta) < 1.0E-4F) return input;

    float x = (input.left() ? 1.0F : 0.0F) - (input.right() ? 1.0F : 0.0F);
    float z = (input.forward() ? 1.0F : 0.0F) - (input.backward() ? 1.0F : 0.0F);

    double rad = delta * Mth.DEG_TO_RAD;
    float cos = (float) Math.cos(rad);
    float sin = (float) Math.sin(rad);
    float rotatedX = x * cos - z * sin;
    float rotatedZ = x * sin + z * cos;

    return new Input(
      rotatedZ > 0.0F, rotatedZ < 0.0F,
      rotatedX > 0.0F, rotatedX < 0.0F,
      input.jump(), input.shift(), input.sprint());
  }

  public static float correctedMovementYaw(Entity entity, float vanillaYaw) {
    Minecraft mc = Minecraft.getInstance();
    if (entity == null || mc == null || entity != mc.player) return vanillaYaw;
    KillAura aura = activeInstance();
    RotationUtil.Rotation rotation = KillAuraRotation.getCurrentRotation();
    return aura == null || rotation == null || !aura.canRun() ? vanillaYaw : rotation.yaw();
  }

  public static float outgoingMovementYaw(LocalPlayer player, float vanillaYaw) {
    if (player == null) return vanillaYaw;
    return correctedMovementYaw(player, vanillaYaw);
  }

  public static float outgoingMovementPitch(LocalPlayer player, float vanillaPitch) {
    Minecraft mc = Minecraft.getInstance();
    if (player == null || mc == null || player != mc.player) return vanillaPitch;
    KillAura aura = activeInstance();
    RotationUtil.Rotation rotation = KillAuraRotation.getCurrentRotation();
    return aura == null || rotation == null || !aura.canRun() ? vanillaPitch : rotation.pitch();
  }

  public static Vec3 silentViewVector(LocalPlayer player, Vec3 vanillaVector) {
    Minecraft mc = Minecraft.getInstance();
    if (player == null || mc == null || player != mc.player) return vanillaVector;
    KillAura aura = activeInstance();
    RotationUtil.Rotation rotation = KillAuraRotation.getCurrentRotation();
    return aura == null || rotation == null || !aura.canRun()
      ? vanillaVector : Vec3.directionFromRotation(rotation.pitch(), rotation.yaw());
  }

  public static Vec3 correctedJumpImpulse(LivingEntity entity, Vec3 vanillaImpulse) {
    RotationUtil.Rotation rotation = activeMovementRotation(entity);
    if (rotation == null) return vanillaImpulse;
    float yaw = rotation.yaw() * Mth.DEG_TO_RAD;
    return new Vec3(-Mth.sin(yaw) * 0.2F, vanillaImpulse.y, Mth.cos(yaw) * 0.2F);
  }

  public static float correctedFallFlyingPitch(LivingEntity entity, float vanillaPitch) {
    RotationUtil.Rotation rotation = activeMovementRotation(entity);
    return rotation == null ? vanillaPitch : rotation.pitch();
  }

  public static Vec3 correctedFallFlyingLook(LivingEntity entity, Vec3 vanillaLook) {
    RotationUtil.Rotation rotation = activeMovementRotation(entity);
    return rotation == null ? vanillaLook
      : Vec3.directionFromRotation(rotation.pitch(), rotation.yaw());
  }

  private static RotationUtil.Rotation activeMovementRotation(Entity entity) {
    Minecraft mc = Minecraft.getInstance();
    if (entity == null || mc == null || entity != mc.player) return null;
    KillAura aura = activeInstance();
    RotationUtil.Rotation rotation = KillAuraRotation.getCurrentRotation();
    return aura == null || rotation == null || !aura.canRun() ? null : rotation;
  }

  public static boolean blocksSprintForCrit() {
    KillAura aura = activeInstance();
    return aura != null && aura.shouldStopSprintingForCrit();
  }

  public LivingEntity getTarget() {
    return currentTarget;
  }

  private void updateTargetRotation() {
    double interactionRange = interactionRange();
    double normalRangeSq = interactionRange * interactionRange;

    double maximumRange = closestSquaredEnemyDistance > normalRangeSq ? scanRange() : interactionRange;
    double maximumRangeSq = maximumRange * maximumRange;

    List<LivingEntity> targets = collectTargets();

    List<LivingEntity> filtered = new ArrayList<>();
    for (LivingEntity entity : targets) {
      if (boxedDistanceToPlayerSqr(entity) <= maximumRangeSq) filtered.add(entity);
    }

    filtered.sort(Comparator.<LivingEntity>comparingInt(entity ->
      boxedDistanceToPlayerSqr(entity) <= normalRangeSq ? 0 : 1));

    LivingEntity chosen = null;
    for (LivingEntity entity : filtered) {
      RotationUtil.Rotation rotation = findRotation(entity, maximumRange);
      if (rotation != null) {
        KillAuraRotation.setTarget(rotation);
        chosen = entity;
        break;
      }
    }
    currentTarget = chosen;
  }

  private List<LivingEntity> collectTargets() {
    List<LivingEntity> entities = new ArrayList<>();
    Vec3 eyes = mc.player.getEyePosition();
    for (Entity entity : mc.level.entitiesForRendering()) {
      if (entity instanceof LivingEntity living && validate(living, eyes)) {
        entities.add(living);
      }
    }
    if (entities.isEmpty()) {
      return entities;
    }

    entities.sort(targetComparator());

    double closest = Double.MAX_VALUE;
    for (LivingEntity entity : entities) {
      closest = Math.min(closest, boxedDistanceToPlayerSqr(entity));
    }
    closestSquaredEnemyDistance = closest;
    return entities;
  }

  private boolean validate(LivingEntity entity, Vec3 eyes) {
    if (entity == mc.player) return false;
    if (entity.isRemoved()) return false;
    if (entity.hurtTime > HURT_TIME) return false;
    if (!shouldBeAttacked(entity)) return false;

    return crosshairAngleToEntity(entity, eyes) <= fov.get() * 0.5F;
  }

  private boolean shouldBeAttacked(LivingEntity entity) {
    if (!(entity instanceof Attackable)) return false;
    if (entity == mc.player || entity.hasPassenger(mc.player)) return false;

    if (!entity.isAlive()) return false;

    if (entity instanceof Player player) {
      if (player.isSleeping()) return false;
      if (player.isCreative()) return false;
      if (!Friends.get().shouldAttack(player)) return false;
      if (player instanceof FakePlayerEntity fakePlayer && fakePlayer.noHit) return false;
    }
    return entities.get().contains(entity.getType());
  }

  private float crosshairAngleToEntity(Entity entity, Vec3 eyes) {
    RotationUtil.Rotation toCenter =
      RotationUtil.lookingAt(entity.getBoundingBox().getCenter(), eyes);
    return RotationUtil.rotationAngleTo(RotationUtil.playerRotation(mc.player), toCenter);
  }

  private int typeWeight(LivingEntity entity) {
    if (entity instanceof Player) return 0;
    if (entity instanceof Enemy) return 1;
    if (entity instanceof NeutralMob neutral) {
      if (neutral.getPersistentAngerTarget() != null
        && neutral.getPersistentAngerTarget().matches(mc.player)) return 2;
    }
    return Integer.MAX_VALUE;
  }

  private Comparator<LivingEntity> targetComparator() {
    return switch (targeting.get()) {
      case Hp -> Comparator.comparingDouble(this::actualHealth);
      case Distance -> Comparator.comparingDouble(this::boxedDistanceToPlayerSqr);
      case Fov -> Comparator.comparingDouble(entity ->
        crosshairAngleToEntity(entity, mc.player.getEyePosition()));
      case HurtTime -> Comparator.comparingInt(entity -> entity.hurtTime);
      case Age -> Comparator.comparingInt(entity -> -entity.tickCount);
      default -> Comparator.comparingInt(this::typeWeight);
    };
  }

  private float actualHealth(LivingEntity entity) {
    try {
      var scoreboard = entity.level().getScoreboard();
      var objective = scoreboard.getDisplayObjective(DisplaySlot.BELOW_NAME);
      if (objective != null) {
        String displayName = objective.getDisplayName().getString();
        if (displayName.contains("❤") || displayName.contains("HP")
          || displayName.contains("Health") || displayName.contains("Здоровья")
          || displayName.contains("Здоровье")) {
          var score = scoreboard.getPlayerScoreInfo(entity, objective);
          if (score != null) return score.value();
        }
      }
    } catch (Throwable ignored) {
    }
    return entity.getHealth();
  }

  private double boxedDistanceToPlayerSqr(Entity entity) {
    return entity.getBoundingBox().inflate(entity.getPickRadius())
      .distanceToSqr(mc.player.getEyePosition());
  }

  private RotationUtil.Rotation findRotation(Entity entity, double range) {
    Vec3 eyes = mc.player.getEyePosition();
    AABB box = entity.getBoundingBox().inflate(entity.getPickRadius());

    Vec3 preferredPoint = closestProjectedPoint(eyes, box, POINT_TRACKER_SAMPLES);
    if (preferredPoint == null) preferredPoint = nearestPointOnBox(eyes, box);

    preferredPoint = nearestPointOnBox(preferredPoint, box);

    RotationUtil.Rotation preference = RotationUtil.lookingAt(preferredPoint, eyes);
    return raytraceBox(eyes, box, range, wallsRange(), preference, preferredPoint);
  }

  private RotationUtil.Rotation raytraceBox(Vec3 eyes, AABB box, double range, double wallsRange,
                                            RotationUtil.Rotation preference, Vec3 preferredSpot) {
    double rangeSq = range * range;
    double wallsRangeSq = wallsRange * wallsRange;

    Vec3 preferredSpotOnBox = firstHit(box, eyes, preferredSpot);
    if (preferredSpotOnBox != null) {
      double distanceSq = eyes.distanceToSqr(preferredSpotOnBox);
      boolean visible = aimVisibility(eyes, preferredSpotOnBox);
      if (distanceSq < (visible ? rangeSq : wallsRangeSq)) {
        return RotationUtil.lookingAt(preferredSpotOnBox, eyes);
      }
    }

    RotationAccumulator accumulator = new RotationAccumulator();
    considerSpot(accumulator, eyes, box, preferredSpot, rangeSq, wallsRangeSq, preference);
    considerSpot(accumulator, eyes, box, nearestPointOnBox(eyes, box), rangeSq, wallsRangeSq, preference);
    scanBoxPoints(eyes, box, spot ->
      considerSpot(accumulator, eyes, box, spot, rangeSq, wallsRangeSq, preference));
    return accumulator.result();
  }

  private void considerSpot(RotationAccumulator accumulator, Vec3 eyes, AABB box, Vec3 spot,
                            double rangeSq, double wallsRangeSq, RotationUtil.Rotation preference) {
    Vec3 raycastTarget = fma(eyes, 2.0D, spot.subtract(eyes));
    Vec3 spotOnBox = firstHit(box, eyes, raycastTarget);
    if (spotOnBox == null) return;

    double distanceSq = eyes.distanceToSqr(spotOnBox);
    boolean visible = aimVisibility(eyes, spotOnBox);

    if (!(distanceSq < (visible ? rangeSq : wallsRangeSq))) return;

    RotationUtil.Rotation rotation = RotationUtil.lookingAt(spot, eyes);
    accumulator.consider(rotation, visible, RotationUtil.rotationAngleTo(preference, rotation));
  }

  private void scanBoxPoints(Vec3 eyes, AABB box, java.util.function.Consumer<Vec3> consumer) {
    boolean outsideBox = projectBoxPoints(eyes, box, RAYTRACE_SAMPLES, consumer);
    if (!outsideBox) {
      for (double x : ITERATION_PROPORTIONS)
        for (double y : ITERATION_PROPORTIONS) {
          for (double z : ITERATION_PROPORTIONS) {
            consumer.accept(new Vec3(
              Math.fma(box.getXsize(), x, box.minX),
              Math.fma(box.getYsize(), y, box.minY),
              Math.fma(box.getZsize(), z, box.minZ)));
          }
        }
    }
  }

  static Vec3 closestProjectedPoint(Vec3 eyes, AABB box, int maxPoints) {
    Vec3[] best = new Vec3[1];
    double[] bestDistance = {Double.POSITIVE_INFINITY};
    boolean projected = projectBoxPoints(eyes, box, maxPoints, point -> {
      double distance = point.distanceToSqr(eyes);
      if (distance < bestDistance[0]) {
        bestDistance[0] = distance;
        best[0] = point;
      }
    });
    return projected ? best[0] : null;
  }

  static boolean projectBoxPoints(Vec3 eyes, AABB box, int maxPoints,
                                  java.util.function.Consumer<Vec3> consumer) {
    if (box.contains(eyes)) return false;

    Vec3 centerDirection = box.getCenter().subtract(eyes);
    double directionLengthSq = centerDirection.lengthSqr();

    if (Mth.equal(directionLengthSq, 0.0D)) return false;
    Vec3 normal = centerDirection.normalize();

    Vec3[] vertices = boxVertices(box);
    Vec3 frameProjection = null;
    double frameDistance = Double.POSITIVE_INFINITY;
    for (Vec3 vertex : vertices) {
      double parameter = vertex.subtract(eyes).dot(centerDirection) / directionLengthSq;
      Vec3 projected = eyes.add(centerDirection.scale(parameter));
      double distance = projected.distanceToSqr(eyes);
      if (distance < frameDistance) {
        frameDistance = distance;
        frameProjection = projected;
      }
    }
    if (frameProjection == null) return false;
    Vec3 frameOrigin = frameProjection.lerp(eyes, 0.1D);

    float yaw = (float) Math.atan2(normal.z, normal.x);
    float pitch = (float) Math.atan2(normal.y, normal.horizontalDistance());
    Matrix3f toMatrix = new Matrix3f().rotateY(-yaw).mul(new Matrix3f().rotateZ(pitch));
    Matrix3f backMatrix = new Matrix3f().rotateZ(-pitch).mul(new Matrix3f().rotateY(yaw));

    float minZ = 0.0F;
    float maxZ = 0.0F;
    float minY = 0.0F;
    float maxY = 0.0F;
    double planeDistance = frameOrigin.dot(normal);
    for (Vec3 vertex : vertices) {
      Vec3 direction = vertex.subtract(eyes);
      double divisor = direction.dot(normal);

      if (Mth.equal(divisor, 0.0D)) continue;
      double parameter = (planeDistance - eyes.dot(normal)) / divisor;
      Vec3 intersection = eyes.add(direction.scale(parameter));
      Vector3f local = intersection.subtract(frameOrigin).toVector3f().mul(backMatrix);
      minZ = Math.min(minZ, local.z);
      maxZ = Math.max(maxZ, local.z);
      minY = Math.min(minY, local.y);
      maxY = Math.max(maxY, local.y);
    }

    Vector3f originF = frameOrigin.toVector3f();
    Vector3f positionF = new Vector3f(0.0F, minY, minZ).mul(toMatrix).add(originF);
    Vector3f dirYF = new Vector3f(0.0F, maxY - minY, 0.0F).mul(toMatrix);
    Vector3f dirZF = new Vector3f(0.0F, 0.0F, maxZ - minZ).mul(toMatrix);
    Vec3 position = new Vec3(positionF.x, positionF.y, positionF.z);
    Vec3 dirY = new Vec3(dirYF.x, dirYF.y, dirYF.z);
    Vec3 dirZ = new Vec3(dirZF.x, dirZF.y, dirZF.z);

    double[] steps = fairPlaneSteps(dirY, dirZ, maxPoints);
    int yCount = (int) Math.floor((1.0D + 1.0E-10D) / steps[1]) + 1;
    int zCount = (int) Math.floor((1.0D + 1.0E-10D) / steps[0]) + 1;
    for (int yi = 0; yi < yCount; yi++) {
      double y = yi * steps[1];
      for (int zi = 0; zi < zCount; zi++) {
        double z = zi * steps[0];
        Vec3 point = fma(fma(position, y, dirY), z, dirZ);
        Vec3 extended = point.lerp(eyes, -100.0D);
        box.clip(eyes, extended).ifPresent(consumer);
      }
    }
    return true;
  }

  private static double[] fairPlaneSteps(Vec3 dirY, Vec3 dirZ, int maxPoints) {
    boolean yZero = Mth.equal(dirY.lengthSqr(), 0.0D);
    boolean zZero = Mth.equal(dirZ.lengthSqr(), 0.0D);
    if (!yZero && !zZero) {
      double aspectRatio = dirZ.length() / dirY.length();
      return new double[]{
        Math.sqrt(1.0D / (aspectRatio * maxPoints)),
        Math.sqrt(aspectRatio / maxPoints)
      };
    }
    if (yZero && zZero) return new double[]{1.0D, 1.0D};
    if (yZero) return new double[]{1.0D, 2.0D / maxPoints};
    return new double[]{2.0D / maxPoints, 1.0D};
  }

  private static Vec3[] boxVertices(AABB box) {
    return new Vec3[]{
      new Vec3(box.minX, box.minY, box.minZ), new Vec3(box.minX, box.minY, box.maxZ),
      new Vec3(box.minX, box.maxY, box.minZ), new Vec3(box.minX, box.maxY, box.maxZ),
      new Vec3(box.maxX, box.minY, box.minZ), new Vec3(box.maxX, box.minY, box.maxZ),
      new Vec3(box.maxX, box.maxY, box.minZ), new Vec3(box.maxX, box.maxY, box.maxZ)
    };
  }

  private static Vec3 nearestPointOnBox(Vec3 point, AABB box) {
    return new Vec3(
      Mth.clamp(point.x, box.minX, box.maxX),
      Mth.clamp(point.y, box.minY, box.maxY),
      Mth.clamp(point.z, box.minZ, box.maxZ));
  }

  private static Vec3 firstHit(AABB box, Vec3 from, Vec3 to) {
    return (box.contains(from) ? box.clip(to, from) : box.clip(from, to)).orElse(null);
  }

  private static Vec3 fma(Vec3 base, double scale, Vec3 other) {
    return new Vec3(
      Math.fma(scale, other.x, base.x),
      Math.fma(scale, other.y, base.y),
      Math.fma(scale, other.z, base.z));
  }

  private boolean aimVisibility(Vec3 eyes, Vec3 point) {
    return hasLineOfSight(eyes, point, mc.player);
  }

  private boolean hasLineOfSight(Vec3 eyes, Vec3 point, Entity entity) {
    return mc.level.clip(new ClipContext(
        eyes, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity))
      .getType() == HitResult.Type.MISS;
  }

  private EntityHitResult findEntityInCrosshair(double range, RotationUtil.Rotation rotation,
                                                java.util.function.Predicate<Entity> predicate) {
    Entity camera = mc.getCameraEntity();
    if (camera == null) return null;
    Vec3 eyes = camera.getEyePosition();
    Vec3 direction = Vec3.directionFromRotation(rotation.pitch(), rotation.yaw());
    Vec3 end = eyes.add(direction.x * range, direction.y * range, direction.z * range);
    AABB search = camera.getBoundingBox().expandTowards(direction.scale(range)).inflate(1.0D, 1.0D, 1.0D);
    return ProjectileUtil.getEntityHitResult(
      camera, eyes, end, search, EntitySelector.CAN_BE_PICKED.or(predicate), range * range);
  }

  private EntityHitResult isLookingAtEntity(Entity target, RotationUtil.Rotation rotation,
                                            double range, double wallsRange) {
    Entity camera = mc.getCameraEntity();
    if (camera == null) return null;
    EntityHitResult hit = findEntityInCrosshair(range, rotation, entity -> entity == target);
    if (hit == null) return null;
    Vec3 eyes = camera.getEyePosition();
    double distanceSq = eyes.distanceToSqr(hit.getLocation());
    return distanceSq <= wallsRange * wallsRange
      || distanceSq <= range * range && hasLineOfSight(eyes, hit.getLocation(), camera)
      ? hit : null;
  }

  private void attackPhase() {
    LivingEntity target = currentTarget;
    if (target == null) return;

    RotationUtil.Rotation rotation = KillAuraRotation.getCurrentRotation();
    if (rotation == null) rotation = RotationUtil.playerRotation(mc.player);

    EntityHitResult crosshairHit = findEntityInCrosshair(interactionRange(), rotation, entity -> true);
    Entity crosshairTarget = crosshairHit != null ? crosshairHit.getEntity() : target;
    if (crosshairTarget instanceof LivingEntity living && living != target && shouldBeAttacked(living)) {
      currentTarget = living;
    }

    attackTarget(crosshairTarget, rotation);
  }

  private void attackTarget(Entity target, RotationUtil.Rotation rotation) {
    EntityHitResult attackHit =
      isLookingAtEntity(target, rotation, interactionRange(), wallsRange());

    boolean isInRange = attackHit != null
      && attackRangeIsInRange(mc.player.getMainHandItem(), attackHit.getLocation());
    if (!isInRange) return;

    if (prepareWeaponSwitchTick(target)) return;
    if (performDueSwitchBack()) return;

    ItemStack mainHandStack = mc.player.getMainHandItem();
    if (!clicker.isClickTick() || !canAttackNow(target, mainHandStack)) return;

    clickerPrepareForAttack(() -> {
      if (!canAttackNow(target, mainHandStack)) return false;
      attackEntity(target);

      if (autoSword.get() && switchBack.get() && previousSlot >= 0) {
        switchBackTicks = switchBackDelay.get();
      }
      scanAddition = nextScanAddition();
      return true;
    });
  }

  private boolean prepareWeaponSwitchTick(Entity target) {
    if (!autoSword.get()) return false;
    if (!(target instanceof LivingEntity living)) return false;
    Integer slot = determineWeaponSlot(living, false);
    if (slot == null || isAutoWeaponBusy()) return false;
    int selected = mc.player.getInventory().getSelectedSlot();
    if (selected == slot) return false;

    if (switchBack.get()) {
      if (previousSlot < 0) previousSlot = selected;
      switchBackTicks = switchBackDelay.get();
    }

    InventoryHelper.selectHotbarSlot(mc, slot);
    return true;
  }

  private boolean performDueSwitchBack() {
    if (!autoSword.get() || !switchBack.get() || previousSlot < 0 || switchBackTicks > 0) return false;
    int back = previousSlot;
    previousSlot = -1;
    if (mc.player.getInventory().getSelectedSlot() == back) return false;

    InventoryHelper.selectHotbarSlot(mc, back);
    return true;
  }

  private boolean canAttackNow(Entity target, ItemStack stack) {
    if (!stack.isItemEnabled(mc.level.enabledFeatures())) return false;
    if (mc.player.cannotAttackWithItem(stack, 0)) return false;

    if (postUseSuppressTicks > 0) return false;

    return !(criticals.get() && target instanceof LivingEntity
      && !mc.player.isFallFlying()
      && !(keepSprint.get() && mc.player.isSprinting())
      && shouldWaitForCrit());
  }

  private boolean shouldWaitForCrit() {
    double motionY = mc.player.getDeltaMovement().y;
    if (!allowsCriticalHit() || motionY < -0.08D) return false;
    float ticksTillCrit = Math.max(ticksUntilNextCrit(), (float) (motionY / 0.08D));
    float damageOnCrit = 0.5F * 0.75F;
    if (damageOnCrit <= cooldownDamageFactor(ticksTillCrit)) return false;
    return willStayAirborne((int) (ticksTillCrit * 1.3F));
  }

  private float ticksUntilNextCrit() {
    return Math.max(currentItemAttackStrengthDelay() * 0.9F - 0.5F - attackStrengthTicker(), 0.0F);
  }

  private float cooldownDamageFactor(float ticks) {
    float base = (ticks + 0.5F) / currentItemAttackStrengthDelay();
    return Math.min(0.2F + base * base * 0.8F, 1.0F);
  }

  private boolean willStayAirborne(int ticks) {
    double motionY = mc.player.getDeltaMovement().y;
    AABB box = mc.player.getBoundingBox();
    for (int i = 0; i < ticks; i++) {
      motionY = (motionY - 0.08D) * 0.98D;
      box = box.move(0.0D, motionY, 0.0D);
      if (mc.level.getBlockStates(box).anyMatch(state -> !state.isAir())) return false;
    }
    return true;
  }

  private boolean shouldStopSprintingForCrit() {
    return criticals.get() && !keepSprint.get()
      && mc.player != null && !mc.player.onGround()
      && currentTarget != null && clicker.willClickAt(1);
  }

  private void clickerPrepareForAttack(BooleanSupplier attack) {
    if (!clicker.canExecuteClickNow()) return;
    if (mc.player.isBlocking()) return;
    if (mc.player.isUsingItem()) return;
    clicker.click(attack);
  }

  private void attackEntity(Entity target) {
    ItemStack stack = mc.player.getMainHandItem();
    var piercing = stack.get(DataComponents.PIERCING_WEAPON);

    if (piercing != null && !mc.gameMode.isSpectator()) {
      mc.gameMode.piercingAttack(piercing);
      mc.player.swing(InteractionHand.MAIN_HAND);
      CpsTracker.recordLeft();
      queueHitFeedback(target);
      return;
    }

    if (!canBeAttackedWithVanillaPacket(target)) return;

    ((KillAuraMultiPlayerGameModeAccessor) mc.gameMode).killAura$ensureHasSentCarriedItem();
    mc.getConnection().send(new ServerboundAttackPacket(target.getId()));
    CpsTracker.recordLeft();
    queueHitFeedback(target);

    if (keepSprint.get()) {
      float genericDamage = mc.player.isAutoSpinAttack()
        ? ((KillAuraLivingEntityAccessor) mc.player).killAura$getAutoSpinAttackDmg()
        : (float) mc.player.getAttributeValue(Attributes.ATTACK_DAMAGE);
      DamageSource damageSource = mc.player.damageSources().playerAttack(mc.player);
      float enchantDamage = ((KillAuraPlayerAccessor) mc.player)
        .killAura$getEnchantedDamage(target, genericDamage, damageSource) - genericDamage;
      float attackCooldown = mc.player.getAttackStrengthScale(0.5F);
      genericDamage *= 0.2F + attackCooldown * attackCooldown * 0.8F;
      enchantDamage *= attackCooldown;

      if (genericDamage > 0.0F || enchantDamage > 0.0F) {
        if (enchantDamage > 0.0F) {
          mc.player.magicCrit(target);
        }
        if (wouldDoCriticalHit()) {
          mc.level.playSound(null, mc.player.getX(), mc.player.getY(), mc.player.getZ(),
            SoundEvents.PLAYER_ATTACK_CRIT, mc.player.getSoundSource(), 1.0F, 1.0F);
          mc.player.crit(target);
        }
      }
    } else if (!mc.gameMode.isSpectator()) {
      mc.player.attack(target);
    }

    mc.player.resetAttackStrengthTicker();
    mc.player.swing(InteractionHand.MAIN_HAND);
  }

  private boolean canBeAttackedWithVanillaPacket(Entity target) {
    return target != null
      && target != mc.player
      && !(target instanceof ItemEntity)
      && !(target instanceof ExperienceOrb)
      && (!(target instanceof AbstractArrow) || target.isAttackable());
  }

  private void queueHitFeedback(Entity target) {
    if (!(target instanceof LivingEntity living)) return;
    if (!hitsound.get() && !render.get()) return;
    pendingHitEntityId = living.getId();
    pendingHitPrevHurtTime = living.hurtTime;
    pendingHitTicks = HIT_CONFIRM_TICKS;
  }

  private void confirmHitFeedback() {
    if (pendingHitEntityId < 0) return;
    Entity entity = mc.level.getEntity(pendingHitEntityId);
    boolean landed = entity instanceof LivingEntity living
      && (living.hurtTime > pendingHitPrevHurtTime
      || pendingHitPrevHurtTime >= HURT_TIME && living.hurtTime >= HURT_TIME);
    if (landed) {
      showHitMarker(entity);
      playHitsound();
      ChamsHit.mark(entity);
    }
    if (landed || entity == null || --pendingHitTicks <= 0) {
      pendingHitEntityId = -1;
    }
  }

  private void showHitMarker(Entity target) {
    if (!render.get() || !renderHitmarker.get()) return;
    KillAuraRenderer.show(target.getBoundingBox().inflate(target.getPickRadius()), renderSettings());
  }

  private void playHitsound() {
    if (!hitsound.get()) return;
    mc.getSoundManager().play(SimpleSoundInstance.forUI(HITSOUND, 1.0F, 0.7F));
  }

  private boolean wouldDoCriticalHit() {
    return canDoCriticalHit() && mc.player.fallDistance > 0.0F;
  }

  private boolean canDoCriticalHit() {
    return allowsCriticalHit() && mc.player.getAttackStrengthScale(0.5F) > 0.9F;
  }

  private boolean allowsCriticalHit() {
    return !mc.player.isInLiquid()
      && !mc.player.isPassenger()
      && !insideWebBlock()
      && !mc.player.hasEffect(MobEffects.LEVITATION)
      && !mc.player.hasEffect(MobEffects.BLINDNESS)
      && !mc.player.hasEffect(MobEffects.SLOW_FALLING)
      && !mc.player.onClimbable()
      && !mc.player.isNoGravity()
      && !mc.player.isHandsBusy()
      && !mc.player.getAbilities().flying
      && !mc.player.onGround();
  }

  private boolean insideWebBlock() {
    return mc.level.getBlockStates(mc.player.getBoundingBox())
      .anyMatch(state -> state.getBlock() instanceof WebBlock);
  }

  private double interactionRange() {
    return Math.max(mc.player.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE), range.get());
  }

  private float rotationSpeed() {
    return turnSpeed.get().floatValue();
  }

  private double scanRange() {
    return interactionRange() + scanAddition;
  }

  private double wallsRange() {
    return throughWalls.get() ? scanRange() : 0.0D;
  }

  private boolean attackRangeIsInRange(ItemStack stack, Vec3 pos) {
    AttackRange attackRange = stack.get(DataComponents.ATTACK_RANGE);
    if (attackRange == null) attackRange = AttackRange.defaultFor(mc.player);
    return attackRange.isInRange(mc.player, pos);
  }

  private double nextScanAddition() {
    return SCAN_ADDITION_MIN + random.nextDouble() * (SCAN_ADDITION_MAX - SCAN_ADDITION_MIN);
  }

  private boolean hasCooldown() {
    return mc.player.getAttributeValue(Attributes.ATTACK_SPEED) < 20.0D;
  }

  private float currentItemAttackStrengthDelay() {
    double attackSpeed = mc.player.getAttributeValue(Attributes.ATTACK_SPEED);
    if (autoSword.get() && hasCooldown()) {
      Integer slot = determineWeaponSlot(null, false);
      if (slot != null) {
        attackSpeed = attributeValue(mc.player.getInventory().getItem(slot),
          Attributes.ATTACK_SPEED, mc.player.getAttributeBaseValue(Attributes.ATTACK_SPEED));
      }
    }
    return (float) (1.0D / attackSpeed * 20.0D);
  }

  private int attackStrengthTicker() {
    return ((KillAuraLivingEntityAccessor) mc.player).killAura$getAttackStrengthTicker();
  }

  private boolean isCooldownPassed(int ticks) {
    float delay = currentItemAttackStrengthDelay();
    return (attackStrengthTicker() + ticks) / delay >= nextCooldown + clickOffsetTicks / delay;
  }

  private float nextCooldown = 1.0F;

  private float clickOffsetTicks = rollClickOffsetTicks();

  private void newCooldown() {
    nextCooldown = 1.0F;
    clickOffsetTicks = rollClickOffsetTicks();
  }

  private float rollClickOffsetTicks() {
    double magnitude = (random.nextDouble() + random.nextDouble()) * 0.5D;
    return (float) (random.nextDouble() < 0.2D ? -magnitude : magnitude);
  }

  private boolean wouldBlockHit(LivingEntity target) {
    DamageSource source = target.level().damageSources().playerAttack(mc.player);
    return getBlockedDamage(target, source, 1.0F) > 0.0F;
  }

  private float getBlockedDamage(LivingEntity target, DamageSource source, float amount) {
    if (amount <= 0.0F) return 0.0F;
    ItemStack blockingStack = target.getItemBlockingWith();
    if (blockingStack == null) return 0.0F;
    var blocksAttacks = blockingStack.get(DataComponents.BLOCKS_ATTACKS);
    if (blocksAttacks == null) return 0.0F;
    if (blocksAttacks.bypassedBy().map(tag -> tag.contains(source.typeHolder())).orElse(false)) return 0.0F;
    if (source.getDirectEntity() instanceof AbstractArrow arrow && arrow.getPierceLevel() > 0) return 0.0F;

    double horizontalAngle = Math.PI;
    Vec3 sourcePosition = source.getSourcePosition();
    if (sourcePosition != null) {
      Vec3 view = target.calculateViewVector(0.0F, target.getYHeadRot());
      Vec3 to = sourcePosition.subtract(target.position());
      Vec3 sourceDirection = new Vec3(to.x, 0.0D, to.z).normalize();
      horizontalAngle = Math.acos(sourceDirection.dot(view));
    }
    return blocksAttacks.resolveBlockedDamage(source, amount, horizontalAngle);
  }

  static final class RollingClickArray {
    private final int cycleLength;
    final int iterations;
    private final int[] array;
    private int head;

    RollingClickArray(int cycleLength, int iterations) {
      this.cycleLength = cycleLength;
      this.iterations = iterations;
      this.array = new int[cycleLength * iterations];
    }

    int get(int relativeIndex) {
      return array[(head + relativeIndex) % array.length];
    }

    boolean advance(int amount) {
      head = (head + amount) % array.length;
      return head % cycleLength == 0;
    }

    void clear() {
      Arrays.fill(array, 0);
      head = 0;
    }

    void push(int[] cycle) {
      if (cycle.length != cycleLength) {
        throw new IllegalArgumentException("Array size must match cycle length");
      }
      if (head == 0) {
        System.arraycopy(cycle, 0, array, cycleLength, cycleLength);
      } else if (head == cycleLength) {
        System.arraycopy(cycle, 0, array, 0, cycleLength);
      } else {
        throw new IllegalStateException("Head must be at 0 or cycle length");
      }
    }

    int cycleClickCount(int offset) {
      int total = 0;
      for (int index = offset; index < offset + cycleLength; index++) total += array[index];
      return total;
    }
  }

  private void stabilizedFill(int[] cycle) {
    int spread = Math.max(cpsMax.get() - cpsMin.get(), 0);
    int clicks = cpsMin.get() + random.nextInt(spread + 1);
    int interval = clicks > 0 ? cycle.length / clicks : 0;
    int remainder = clicks > 0 ? cycle.length % clicks : 0;
    int index = 0;
    for (int i = 0; i < clicks; i++) {
      cycle[index % cycle.length]++;
      index += Math.max(interval, 1);
      if (remainder > 0) {
        index++;
        remainder--;
      }
    }
  }

  private final class Clicker {
    private final RollingClickArray clickArray = new RollingClickArray(CLICK_CYCLE, CLICK_ITERATIONS);
    private int ticksSinceLastClick;

    Clicker() {
      fill();
    }

    void tick() {
      ticksSinceLastClick++;
      if (clickArray.advance(1)) {
        int[] cycle = new int[CLICK_CYCLE];
        stabilizedFill(cycle);
        clickArray.push(cycle);
      }
    }

    private void fill() {
      clickArray.clear();
      int[] cycle = new int[CLICK_CYCLE];
      for (int i = 0; i < clickArray.iterations; i++) {
        Arrays.fill(cycle, 0);
        stabilizedFill(cycle);
        clickArray.push(cycle);
        clickArray.advance(CLICK_CYCLE);
      }
    }

    int getClickAmount(int tick) {
      if (isEnforcedClick()) return 1;
      return clickArray.get(tick);
    }

    private boolean isEnforcedClick() {
      if (hasCooldown() && isCooldownPassed(0)) return true;
      return System.currentTimeMillis() - lastClickTime >= ENFORCED_CLICK_INTERVAL_MS;
    }

    boolean willClickAt(int tick) {
      return getClickAmount(tick) > 0;
    }

    boolean isClickTick() {
      return willClickAt(0);
    }

    boolean canExecuteClickNow() {
      if (getClickAmount(0) <= 0) return false;

      if (((KillAuraMinecraftAccessor) mc).killAura$getMissTime() > 0) return false;
      return isCooldownPassed(0);
    }

    void click(BooleanSupplier attack) {
      int amount = getClickAmount(0);
      for (int i = 0; i < amount; i++) {
        if (((KillAuraMinecraftAccessor) mc).killAura$getMissTime() > 0) continue;
        if (isCooldownPassed(0) && attack.getAsBoolean()) {
          newCooldown();
          lastClickTime = System.currentTimeMillis();
          ticksSinceLastClick = 0;
        }
      }
    }
  }

  private Integer determineWeaponSlot(LivingEntity target, boolean enforceShield) {
    boolean requiresShield = enforceShield || target != null && wouldBlockHit(target);

    boolean requiresMace = canMaceSmash();

    Integer bestSlot = null;
    ItemStack bestStack = null;
    for (int slot = 0; slot < 9; slot++) {
      ItemStack stack = mc.player.getInventory().getItem(slot);
      if (stack.isEmpty()) continue;
      boolean eligible = requiresMace ? stack.getItem() instanceof MaceItem
        : requiresShield ? stack.is(ItemTags.AXES)
        : stack.is(ItemTags.SWORDS);
      if (!eligible) continue;
      if (bestStack == null || (requiresMace
        ? compareMaces(stack, bestStack) > 0
        : compareWeapons(stack, bestStack) > 0)) {
        bestSlot = slot;
        bestStack = stack;
      }
    }
    return bestSlot;
  }

  private boolean canMaceSmash() {
    return MaceItem.canSmashAttack(mc.player);
  }

  private boolean isAutoWeaponBusy() {
    return mc.player.isUsingItem()
      && mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND
      && mc.player.getUseItem().has(DataComponents.CONSUMABLE);
  }

  private void tickAutoSwordReset() {
    if (!autoSword.get() || !switchBack.get()) return;
    if (previousSlot < 0 || switchBackTicks <= 0) return;
    switchBackTicks--;
    if (switchBackTicks == 0 && (currentTarget == null || !canRun())) {
      int back = previousSlot;
      previousSlot = -1;
      InventoryHelper.selectHotbarSlot(mc, back);
    }
  }

  private void resetAutoSword() {
    if (previousSlot >= 0 && mc.player != null) {
      InventoryHelper.selectHotbarSlot(mc, previousSlot);
    }
    previousSlot = -1;
    switchBackTicks = 0;
  }

  private int compareWeapons(ItemStack first, ItemStack second) {
    int result = Double.compare(estimatedWeaponDamage(first), estimatedWeaponDamage(second));
    if (result != 0) return result;
    result = Double.compare(secondaryWeaponValue(first), secondaryWeaponValue(second));
    if (result != 0) return result;
    result = Boolean.compare(first.is(ItemTags.SWORDS), second.is(ItemTags.SWORDS));
    if (result != 0) return result;
    result = Integer.compare(durability(first), durability(second));
    if (result != 0) return result;
    result = Integer.compare(enchantableValue(first), enchantableValue(second));
    if (result != 0) return result;
    return Integer.compare(first.hashCode(), second.hashCode());
  }

  private int compareMaces(ItemStack first, ItemStack second) {
    int result = Double.compare(estimatedMaceDamage(first), estimatedMaceDamage(second));
    if (result != 0) return result;
    result = Integer.compare(durability(first), durability(second));
    if (result != 0) return result;
    result = Integer.compare(enchantableValue(first), enchantableValue(second));
    if (result != 0) return result;
    return Integer.compare(first.hashCode(), second.hashCode());
  }

  private double estimatedWeaponDamage(ItemStack stack) {
    double damage = attributeValue(stack, Attributes.ATTACK_DAMAGE,
      mc.player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE));
    int sharpness = enchantmentLevel(stack, Enchantments.SHARPNESS);
    if (sharpness > 0) damage += 0.5D * sharpness + 0.5D;
    double speed = attributeValue(stack, Attributes.ATTACK_SPEED,
      mc.player.getAttributeBaseValue(Attributes.ATTACK_SPEED));
    double probability = Math.pow(0.85D, 1.0D / 20.0D);
    double adjusted = Math.pow(probability, Math.ceil((20.0D / speed) * 0.9D));
    double fire = Math.max(0.0D, enchantmentLevel(stack, Enchantments.FIRE_ASPECT) * 4.0D - 1.0D) * 0.33D;
    double factor = enchantmentLevel(stack, Enchantments.SMITE) * 0.2D
      + enchantmentLevel(stack, Enchantments.BANE_OF_ARTHROPODS) * 0.2D
      + enchantmentLevel(stack, Enchantments.KNOCKBACK) * 0.2D;
    return damage * speed * adjusted * (1.0D + factor) + fire;
  }

  private double estimatedMaceDamage(ItemStack stack) {
    double damage = attributeValue(stack, Attributes.ATTACK_DAMAGE,
      mc.player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE));
    int sharpness = enchantmentLevel(stack, Enchantments.SHARPNESS);
    if (sharpness > 0) damage += 0.5D * sharpness + 0.5D;
    double speed = attributeValue(stack, Attributes.ATTACK_SPEED,
      mc.player.getAttributeBaseValue(Attributes.ATTACK_SPEED));
    double probability = Math.pow(0.85D, 1.0D / 20.0D);
    double adjusted = Math.pow(probability, Math.ceil((20.0D / speed) * 0.9D));
    double factor = enchantmentLevel(stack, Enchantments.DENSITY) * 0.5D
      + enchantmentLevel(stack, Enchantments.BREACH) * 0.15D
      + enchantmentLevel(stack, Enchantments.SMITE) * 0.2D
      + enchantmentLevel(stack, Enchantments.BANE_OF_ARTHROPODS) * 0.2D
      + enchantmentLevel(stack, Enchantments.WIND_BURST) * 0.2D;

    return damage * speed * adjusted + factor + 29.0D;
  }

  private double secondaryWeaponValue(ItemStack stack) {
    return enchantmentLevel(stack, Enchantments.LOOTING) * 0.05D
      + enchantmentLevel(stack, Enchantments.UNBREAKING) * 0.05D
      + enchantmentLevel(stack, Enchantments.MENDING) * 0.1D
      - enchantmentLevel(stack, Enchantments.VANISHING_CURSE) * 0.1D
      + enchantmentLevel(stack, Enchantments.SWEEPING_EDGE) * 0.2D
      + enchantmentLevel(stack, Enchantments.KNOCKBACK) * 0.25D;
  }

  private static int durability(ItemStack stack) {
    return stack.getMaxDamage() - stack.getDamageValue();
  }

  private static int enchantableValue(ItemStack stack) {
    return stack.has(DataComponents.ENCHANTABLE) ? stack.get(DataComponents.ENCHANTABLE).value() : 0;
  }

  private double attributeValue(ItemStack stack, Holder<Attribute> attribute, double base) {
    var modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
    return modifiers == null ? attribute.value().sanitizeValue(base)
      : attribute.value().sanitizeValue(modifiers.compute(attribute, base, EquipmentSlot.MAINHAND));
  }

  private int enchantmentLevel(ItemStack stack,
                               ResourceKey<Enchantment> enchantment) {
    try {
      Holder<Enchantment> holder = mc.level.registryAccess()
        .lookupOrThrow(Registries.ENCHANTMENT)
        .getOrThrow(enchantment);
      return EnchantmentHelper.getItemEnchantmentLevel(holder, stack);
    } catch (Throwable ignored) {
      return 0;
    }
  }

  private void resetRuntime(boolean restoreSlot) {
    if (restoreSlot && previousSlot >= 0 && mc.player != null && switchBack.get()) {
      InventoryHelper.selectHotbarSlot(mc, previousSlot);
    }
    currentTarget = null;
    attacking = false;
    previousSlot = -1;
    switchBackTicks = 0;
    postUseSuppressTicks = 0;
    pendingHitEntityId = -1;
    KillAuraRotation.reset();
    KillAuraRenderer.clear();
  }

  private static final class RotationAccumulator {
    private RotationUtil.Rotation bestVisible;
    private double bestVisibleAngle;
    private RotationUtil.Rotation bestInvisible;
    private double bestInvisibleAngle;

    private void consider(RotationUtil.Rotation rotation, boolean visible, double preferenceAngle) {
      if (visible) {
        if (bestVisible == null || preferenceAngle < bestVisibleAngle) {
          bestVisible = rotation;
          bestVisibleAngle = preferenceAngle;
        }
      } else if (bestInvisible == null || preferenceAngle < bestInvisibleAngle) {
        bestInvisible = rotation;
        bestInvisibleAngle = preferenceAngle;
      }
    }

    private RotationUtil.Rotation result() {
      return bestVisible != null ? bestVisible : bestInvisible;
    }
  }
}
