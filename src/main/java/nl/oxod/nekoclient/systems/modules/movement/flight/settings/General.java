package nl.oxod.nekoclient.systems.modules.movement.flight.settings;

import java.util.List;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.Utils;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import nl.oxod.nekoclient.systems.modules.combat.killaura.KillAura;
import nl.oxod.nekoclient.systems.modules.combat.killaura.KillAura.AttackItems;
import nl.oxod.nekoclient.systems.modules.combat.killaura.KillAura.RotationMode;
import nl.oxod.nekoclient.systems.modules.combat.killaura.KillAura.RotationType;
import nl.oxod.nekoclient.systems.modules.combat.killaura.KillAura.ShieldMode;
import nl.oxod.nekoclient.systems.modules.movement.flight.Flight;
import nl.oxod.nekoclient.systems.modules.movement.flight.Flight.Mode;

public class General {
  private SettingGroup sg;
  private Flight parent;

  public final Setting<Mode> mode;
  public final Setting<Double> speed;
  public final Setting<Boolean> verticalSpeedMatch;
  public final Setting<Boolean> noSneak;

  public General(SettingGroup sg, Flight parent) {
    this.sg = sg;
    this.parent = parent;

    mode = sg.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("The mode for Flight.")
        .defaultValue(Mode.Abilities)
        .onChanged(mode -> {
          if (!parent.isActive() || !Utils.canUpdate())
            return;
          parent.abilitiesOff();
        })
        .build());

    speed = sg.add(new DoubleSetting.Builder()
        .name("speed")
        .description("Your speed when flying.")
        .defaultValue(0.1)
        .min(0.0)
        .build());

    verticalSpeedMatch = sg.add(new BoolSetting.Builder()
        .name("vertical-speed-match")
        .description("Matches your vertical speed to your horizontal speed, otherwise uses vanilla ratio.")
        .defaultValue(false)
        .build());

    noSneak = sg.add(new BoolSetting.Builder()
        .name("no-sneak")
        .description("Prevents you from sneaking while flying.")
        .defaultValue(false)
        .visible(() -> mode.get() == Mode.Velocity)
        .build());
  }
}
