package meteordevelopment.meteorclient.systems.modules.combat.killaura;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.combat.killaura.modes.Matrix;
import meteordevelopment.meteorclient.systems.modules.combat.killaura.modes.Vannila;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class KillAura extends Module {
  public KillAura() {
    super(Categories.Combat, "kill-aura", "Attacks specified entities around you.");
  }

  private final SettingGroup sgGeneral = settings.getDefaultGroup();
  private final SettingGroup sgTargeting = settings.createGroup("Targeting");
  private final SettingGroup sgTiming = settings.createGroup("Timing");

  // General
  public final Setting<AttackItems> attackWhenHolding = sgGeneral.add(new EnumSetting.Builder<AttackItems>()
    .name("attack-when-holding")
    .description("Only attacks an entity when a specified item is in your hand.")
    .defaultValue(AttackItems.Weapons)
    .build()
  );

  public final Setting<List<Item>> weapons = sgGeneral.add(new ItemListSetting.Builder()
    .name("selected-weapon-types")
    .description("Which types of weapons to attack with (if you select the diamond sword, any type of sword may be used to attack).")
    .defaultValue(Items.DIAMOND_SWORD, Items.DIAMOND_AXE, Items.TRIDENT)
    .filter(FILTER::contains)
    .visible(() -> attackWhenHolding.get() == AttackItems.Weapons)
    .build()
  );

  public final Setting<RotationMode> rotation = sgGeneral.add(new EnumSetting.Builder<RotationMode>()
    .name("rotate")
    .description("Determines when you should rotate towards the target.")
    .defaultValue(RotationMode.Always)
    .build()
  );

  public final Setting<RotationType> rotationType = sgGeneral.add(new EnumSetting.Builder<RotationType>()
    .name("rotation-type")
    .defaultValue(RotationType.Smooth)
    .build()
  );

  public final Setting<Boolean> speedUpRotationWhenAttacking = sgGeneral.add(new BoolSetting.Builder()
		.name("speed-up-the-rotation-when-attacking")
		.defaultValue(false)
		.build()
	);

  public final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
    .name("auto-switch")
    .description("Switches to an acceptable weapon when attacking the target.")
    .defaultValue(false)
    .build()
  );

  public final Setting<Boolean> swapBack = sgGeneral.add(new BoolSetting.Builder()
    .name("swap-back")
    .description("Switches to your previous slot when done attacking the target.")
    .defaultValue(false)
    .visible(autoSwitch::get)
    .build()
  );

  public final Setting<ShieldMode> shieldMode = sgGeneral.add(new EnumSetting.Builder<ShieldMode>()
    .name("shield-mode")
    .description("""
        What to do when your target is blocking with a shield:
        - Ignore:   Don't attack them if they are blocking
        - Break:  Swap to an axe to disable the shield (Only if Auto Switch is enabled)
        - None:   Attack them as normal
      """)
    .defaultValue(ShieldMode.None)
    .build()
  );

  public final Setting<Boolean> onlyOnClick = sgGeneral.add(new BoolSetting.Builder()
    .name("only-on-click")
    .description("Only attacks when holding left click.")
    .defaultValue(false)
    .build()
  );

  public final Setting<Boolean> onlyOnLook = sgGeneral.add(new BoolSetting.Builder()
    .name("only-on-look")
    .description("Only attacks when looking at an entity.")
    .defaultValue(false)
    .build()
  );

  public final Setting<Boolean> pauseOnCombat = sgGeneral.add(new BoolSetting.Builder()
    .name("pause-baritone")
    .description("Freezes Baritone temporarily until you are finished attacking the entity.")
    .defaultValue(true)
    .build()
  );

  private final Setting<KillAuraModes> mode = sgGeneral.add(new EnumSetting.Builder<KillAuraModes>()
    .name("mode")
    .description("Kill Aura mode.")
    .defaultValue(KillAuraModes.Matrix)
    .onModuleActivated(modeSetting -> onModeChanged(modeSetting.get()))
    .onChanged(this::onModeChanged)
    .build()
  );

  // Targeting
  public final Setting<Set<EntityType<?>>> entities = sgTargeting.add(new EntityTypeListSetting.Builder()
    .name("entities")
    .description("Entities to attack.")
    .onlyAttackable()
    .defaultValue(EntityType.PLAYER)
    .build()
  );

  public final Setting<SortPriority> priority = sgTargeting.add(new EnumSetting.Builder<SortPriority>()
    .name("priority")
    .description("How to filter targets within range.")
    .defaultValue(SortPriority.ClosestAngle)
    .build()
  );

  public final Setting<Integer> maxTargets = sgTargeting.add(new IntSetting.Builder()
    .name("max-targets")
    .description("How many entities to target at once.")
    .defaultValue(1)
    .min(1)
    .sliderRange(1, 5)
    .visible(() -> !onlyOnLook.get())
    .build()
  );

  public final Setting<Double> range = sgTargeting.add(new DoubleSetting.Builder()
    .name("range")
    .description("The maximum range the entity can be to attack it.")
    .defaultValue(4.5)
    .min(0)
    .sliderMax(6)
    .build()
  );

  public final Setting<Double> wallsRange = sgTargeting.add(new DoubleSetting.Builder()
    .name("walls-range")
    .description("The maximum range the entity can be attacked through walls.")
    .defaultValue(3.5)
    .min(0)
    .sliderMax(6)
    .build()
  );

  public final Setting<EntityAge> passiveMobAgeFilter = sgTargeting.add(new EnumSetting.Builder<EntityAge>()
    .name("passive-mob-age-filter")
    .description("Determines the age of passive mobs to target (animals, villagers).")
    .defaultValue(EntityAge.Adult)
    .build()
  );

  public final Setting<EntityAge> hostileMobAgeFilter = sgTargeting.add(new EnumSetting.Builder<EntityAge>()
    .name("hostile-mob-age-filter")
    .description("Determines the age of hostile mobs to target (zombies, piglins, hoglins, zoglins).")
    .defaultValue(EntityAge.Both)
    .build()
  );

  public final Setting<Boolean> ignoreNamed = sgTargeting.add(new BoolSetting.Builder()
    .name("ignore-named")
    .description("Whether or not to attack mobs with a name.")
    .defaultValue(false)
    .build()
  );

  public final Setting<Boolean> ignorePassive = sgTargeting.add(new BoolSetting.Builder()
    .name("ignore-passive")
    .description("Will only attack sometimes passive mobs if they are targeting you.")
    .defaultValue(true)
    .build()
  );

  public final Setting<Boolean> ignoreTamed = sgTargeting.add(new BoolSetting.Builder()
    .name("ignore-tamed")
    .description("Will avoid attacking mobs you tamed.")
    .defaultValue(false)
    .build()
  );

  // Timing
  public final Setting<Boolean> pauseOnLag = sgTiming.add(new BoolSetting.Builder()
    .name("pause-on-lag")
    .description("Pauses if the server is lagging.")
    .defaultValue(true)
    .build()
  );

  public final Setting<Boolean> pauseOnUse = sgTiming.add(new BoolSetting.Builder()
    .name("pause-on-use")
    .description("Does not attack while using an item.")
    .defaultValue(false)
    .build()
  );

  public final Setting<Boolean> pauseOnCA = sgTiming.add(new BoolSetting.Builder()
    .name("pause-on-CA")
    .description("Does not attack while CA is placing.")
    .defaultValue(true)
    .build()
  );

  public final Setting<Boolean> tpsSync = sgTiming.add(new BoolSetting.Builder()
    .name("TPS-sync")
    .description("Tries to sync attack delay with the server's TPS.")
    .defaultValue(true)
    .build()
  );

  public final Setting<Boolean> customDelay = sgTiming.add(new BoolSetting.Builder()
    .name("custom-delay")
    .description("Use a custom delay instead of the vanilla cooldown.")
    .defaultValue(false)
    .build()
  );

  public final Setting<Integer> hitDelay = sgTiming.add(new IntSetting.Builder()
    .name("hit-delay")
    .description("How fast you hit the entity in ticks.")
    .defaultValue(11)
    .min(0)
    .sliderMax(60)
    .visible(customDelay::get)
    .build()
  );

  public final Setting<Integer> switchDelay = sgTiming.add(new IntSetting.Builder()
    .name("switch-delay")
    .description("How many ticks to wait before hitting an entity after switching hotbar slots.")
    .defaultValue(0)
    .min(0)
    .sliderMax(10)
    .build()
  );

  private final static ArrayList<Item> FILTER = new ArrayList<>(List.of(Items.DIAMOND_SWORD, Items.DIAMOND_AXE, Items.DIAMOND_PICKAXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE, Items.MACE, Items.DIAMOND_SPEAR, Items.TRIDENT));
  public boolean attacking, swapped;
  public static int previousSlot;

  private KillAuraMode currentMode;

  private void onModeChanged(KillAuraModes mode) {
    switch (mode) {
      case Vannila -> currentMode = new Vannila();
      case Matrix -> currentMode = new Matrix();
    }
  }

  public Entity getTarget() {
    if (!currentMode.targets.isEmpty()) return currentMode.targets.getFirst();
    return null;
  }

  @EventHandler
	private void onTickPre(TickEvent.Pre event) {
    currentMode.isAllowedToAttack();

    Entity target = getTarget();
    if (target == null) return;

    currentMode.autoSwitch();

    if (!currentMode.acceptableWeapon(mc.player.getMainHandItem())) {
      currentMode.stopAttacking();
      return;
    }

    attacking = true;

		currentMode.onTickPre(event, target);
	}

	@EventHandler
	private void onTickPost(TickEvent.Post event) {
		currentMode.onTickPost(event);
	}

	@EventHandler
	private void onSendPacket(PacketEvent.Send event) {
		currentMode.onSendPacket(event);
	}

	@Override
	public void onDeactivate() {
		currentMode.onDeactivate();
	}

	@Override
	public void onActivate() {
		currentMode.onActivate();
	}

	@Override
	public String getInfoString() {
		return currentMode.getInfoString();
	}

  public enum AttackItems {
    Weapons,
    All
  }

  public enum RotationMode {
    Always,
    OnHit,
    None
  }

  public enum RotationType {
    Smooth,
    Fast
  }

  public enum ShieldMode {
    Ignore,
    Break,
    None
  }

  public enum EntityAge {
    Baby,
    Adult,
    Both
  }
}
