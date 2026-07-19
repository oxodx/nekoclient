package meteordevelopment.meteorclient.systems.modules.combat.killaura.settings;

import java.util.Set;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EntityTypeListSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.combat.killaura.KillAura.EntityAge;
import meteordevelopment.meteorclient.systems.modules.combat.killaura.KillAura;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import net.minecraft.world.entity.EntityType;

public class Targeting {
  private SettingGroup sg;
  private KillAura parent;

  public Targeting(SettingGroup sg, KillAura parent) {
    this.sg = sg;
    this.parent = parent;
  }

  // Targeting
  public final Setting<Set<EntityType<?>>> entities = sg.add(new EntityTypeListSetting.Builder()
      .name("entities")
      .description("Entities to attack.")
      .onlyAttackable()
      .defaultValue(EntityType.PLAYER)
      .build());

  public final Setting<SortPriority> priority = sg.add(new EnumSetting.Builder<SortPriority>()
      .name("priority")
      .description("How to filter targets within range.")
      .defaultValue(SortPriority.ClosestAngle)
      .build());

  public final Setting<Integer> maxTargets = sg.add(new IntSetting.Builder()
      .name("max-targets")
      .description("How many entities to target at once.")
      .defaultValue(1)
      .min(1)
      .sliderRange(1, 5)
      .visible(() -> !parent.general.onlyOnLook.get())
      .build());

  public final Setting<Double> range = sg.add(new DoubleSetting.Builder()
      .name("range")
      .description("The maximum range the entity can be to attack it.")
      .defaultValue(4.5)
      .min(0)
      .sliderMax(6)
      .build());

  public final Setting<Double> wallsRange = sg.add(new DoubleSetting.Builder()
      .name("walls-range")
      .description("The maximum range the entity can be attacked through walls.")
      .defaultValue(3.5)
      .min(0)
      .sliderMax(6)
      .build());

  public final Setting<EntityAge> passiveMobAgeFilter = sg.add(new EnumSetting.Builder<EntityAge>()
      .name("passive-mob-age-filter")
      .description("Determines the age of passive mobs to target (animals, villagers).")
      .defaultValue(EntityAge.Adult)
      .build());

  public final Setting<EntityAge> hostileMobAgeFilter = sg.add(new EnumSetting.Builder<EntityAge>()
      .name("hostile-mob-age-filter")
      .description("Determines the age of hostile mobs to target (zombies, piglins, hoglins, zoglins).")
      .defaultValue(EntityAge.Both)
      .build());

  public final Setting<Boolean> ignoreNamed = sg.add(new BoolSetting.Builder()
      .name("ignore-named")
      .description("Whether or not to attack mobs with a name.")
      .defaultValue(false)
      .build());

  public final Setting<Boolean> ignorePassive = sg.add(new BoolSetting.Builder()
      .name("ignore-passive")
      .description("Will only attack sometimes passive mobs if they are targeting you.")
      .defaultValue(true)
      .build());

  public final Setting<Boolean> ignoreTamed = sg.add(new BoolSetting.Builder()
      .name("ignore-tamed")
      .description("Will avoid attacking mobs you tamed.")
      .defaultValue(false)
      .build());
}
