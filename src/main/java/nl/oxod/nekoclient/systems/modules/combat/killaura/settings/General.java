package nl.oxod.nekoclient.systems.modules.combat.killaura.settings;

import static nl.oxod.nekoclient.systems.modules.combat.killaura.KillAura.FILTER;

import java.util.List;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import nl.oxod.nekoclient.systems.modules.combat.killaura.KillAura;
import nl.oxod.nekoclient.systems.modules.combat.killaura.KillAura.AttackItems;
import nl.oxod.nekoclient.systems.modules.combat.killaura.KillAura.RotationMode;
import nl.oxod.nekoclient.systems.modules.combat.killaura.KillAura.RotationType;
import nl.oxod.nekoclient.systems.modules.combat.killaura.KillAura.ShieldMode;

public class General {
  private SettingGroup sg;
  private KillAura parent;

  public final Setting<AttackItems> attackWhenHolding;
  public final Setting<List<Item>> weapons;
  public final Setting<RotationMode> rotation;
  public final Setting<RotationType> rotationType;
  public final Setting<Boolean> speedUpRotationWhenAttacking;
  public final Setting<Boolean> autoSwitch;
  public final Setting<Boolean> swapBack;
  public final Setting<ShieldMode> shieldMode;
  public final Setting<Boolean> onlyOnClick;
  public final Setting<Boolean> onlyOnLook;
  public final Setting<Boolean> pauseOnCombat;

  public General(SettingGroup sg, KillAura parent) {
    this.sg = sg;
    this.parent = parent;

    attackWhenHolding = sg.add(new EnumSetting.Builder<AttackItems>()
        .name("attack-when-holding")
        .description("Only attacks an entity when a specified item is in your hand.")
        .defaultValue(AttackItems.Weapons)
        .build());

    weapons = sg.add(new ItemListSetting.Builder()
        .name("selected-weapon-types")
        .description(
            "Which types of weapons to attack with (if you select the diamond sword, any type of sword may be used to attack).")
        .defaultValue(Items.DIAMOND_SWORD, Items.DIAMOND_AXE, Items.TRIDENT)
        .filter(FILTER::contains)
        .visible(() -> attackWhenHolding.get() == AttackItems.Weapons)
        .build());

    rotation = sg.add(new EnumSetting.Builder<RotationMode>()
        .name("rotate")
        .description("Determines when you should rotate towards the target.")
        .defaultValue(RotationMode.Always)
        .build());

    rotationType = sg.add(new EnumSetting.Builder<RotationType>()
        .name("rotation-type")
        .description("How rotations are applied to the target: Smooth uses gradual interpolation with GCD, Fast snaps directly.")
        .defaultValue(RotationType.Smooth)
        .build());

    speedUpRotationWhenAttacking = sg.add(new BoolSetting.Builder()
        .name("speed-up-the-rotation-when-attacking")
        .description("Uses full pitch speed instead of reduced speed when switching to a new target.")
        .defaultValue(false)
        .build());

    autoSwitch = sg.add(new BoolSetting.Builder()
        .name("auto-switch")
        .description("Switches to an acceptable weapon when attacking the target.")
        .defaultValue(false)
        .build());

    swapBack = sg.add(new BoolSetting.Builder()
        .name("swap-back")
        .description("Switches to your previous slot when done attacking the target.")
        .defaultValue(false)
        .visible(autoSwitch::get)
        .build());

    shieldMode = sg.add(new EnumSetting.Builder<ShieldMode>()
        .name("shield-mode")
        .description("""
              What to do when your target is blocking with a shield:
              - Ignore:   Don't attack them if they are blocking
              - Break:  Swap to an axe to disable the shield (Only if Auto Switch is enabled)
              - None:   Attack them as normal
            """)
        .defaultValue(ShieldMode.None)
        .build());

    onlyOnClick = sg.add(new BoolSetting.Builder()
        .name("only-on-click")
        .description("Only attacks when holding left click.")
        .defaultValue(false)
        .build());

    onlyOnLook = sg.add(new BoolSetting.Builder()
        .name("only-on-look")
        .description("Only attacks when looking at an entity.")
        .defaultValue(false)
        .build());

    pauseOnCombat = sg.add(new BoolSetting.Builder()
        .name("pause-baritone")
        .description("Freezes Baritone temporarily until you are finished attacking the entity.")
        .defaultValue(true)
        .build());
  }
}
