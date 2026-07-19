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
import nl.oxod.nekoclient.systems.modules.combat.killaura.KillAuraModes;
import nl.oxod.nekoclient.systems.modules.combat.killaura.KillAura.AttackItems;
import nl.oxod.nekoclient.systems.modules.combat.killaura.KillAura.RotationMode;
import nl.oxod.nekoclient.systems.modules.combat.killaura.KillAura.RotationType;
import nl.oxod.nekoclient.systems.modules.combat.killaura.KillAura.ShieldMode;

public class General {
  private SettingGroup sg;
  private KillAura parent;

  public General(SettingGroup sg, KillAura parent) {
    this.sg = sg;
    this.parent = parent;
  }

  public final Setting<AttackItems> attackWhenHolding = sg.add(new EnumSetting.Builder<AttackItems>()
      .name("attack-when-holding")
      .description("Only attacks an entity when a specified item is in your hand.")
      .defaultValue(AttackItems.Weapons)
      .build());

  public final Setting<List<Item>> weapons = sg.add(new ItemListSetting.Builder()
      .name("selected-weapon-types")
      .description(
          "Which types of weapons to attack with (if you select the diamond sword, any type of sword may be used to attack).")
      .defaultValue(Items.DIAMOND_SWORD, Items.DIAMOND_AXE, Items.TRIDENT)
      .filter(FILTER::contains)
      .visible(() -> attackWhenHolding.get() == AttackItems.Weapons)
      .build());

  public final Setting<RotationMode> rotation = sg.add(new EnumSetting.Builder<RotationMode>()
      .name("rotate")
      .description("Determines when you should rotate towards the target.")
      .defaultValue(RotationMode.Always)
      .build());

  public final Setting<RotationType> rotationType = sg.add(new EnumSetting.Builder<RotationType>()
      .name("rotation-type")
      .defaultValue(RotationType.Smooth)
      .build());

  public final Setting<Boolean> speedUpRotationWhenAttacking = sg.add(new BoolSetting.Builder()
      .name("speed-up-the-rotation-when-attacking")
      .defaultValue(false)
      .build());

  public final Setting<Boolean> autoSwitch = sg.add(new BoolSetting.Builder()
      .name("auto-switch")
      .description("Switches to an acceptable weapon when attacking the target.")
      .defaultValue(false)
      .build());

  public final Setting<Boolean> swapBack = sg.add(new BoolSetting.Builder()
      .name("swap-back")
      .description("Switches to your previous slot when done attacking the target.")
      .defaultValue(false)
      .visible(autoSwitch::get)
      .build());

  public final Setting<ShieldMode> shieldMode = sg.add(new EnumSetting.Builder<ShieldMode>()
      .name("shield-mode")
      .description("""
            What to do when your target is blocking with a shield:
            - Ignore:   Don't attack them if they are blocking
            - Break:  Swap to an axe to disable the shield (Only if Auto Switch is enabled)
            - None:   Attack them as normal
          """)
      .defaultValue(ShieldMode.None)
      .build());

  public final Setting<Boolean> onlyOnClick = sg.add(new BoolSetting.Builder()
      .name("only-on-click")
      .description("Only attacks when holding left click.")
      .defaultValue(false)
      .build());

  public final Setting<Boolean> onlyOnLook = sg.add(new BoolSetting.Builder()
      .name("only-on-look")
      .description("Only attacks when looking at an entity.")
      .defaultValue(false)
      .build());

  public final Setting<Boolean> pauseOnCombat = sg.add(new BoolSetting.Builder()
      .name("pause-baritone")
      .description("Freezes Baritone temporarily until you are finished attacking the entity.")
      .defaultValue(true)
      .build());

  public final Setting<KillAuraModes> mode = sg.add(new EnumSetting.Builder<KillAuraModes>()
      .name("mode")
      .description("Kill Aura mode.")
      .defaultValue(KillAuraModes.Vannila)
      .onModuleActivated(modeSetting -> parent.onModeChanged(modeSetting.get()))
      .onChanged(parent::onModeChanged)
      .build());
}
