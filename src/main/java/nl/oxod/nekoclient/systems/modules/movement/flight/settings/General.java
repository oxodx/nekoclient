package nl.oxod.nekoclient.systems.modules.movement.flight.settings;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.Utils;
import nl.oxod.nekoclient.systems.modules.movement.flight.FlyModes;
import nl.oxod.nekoclient.systems.modules.movement.flight.Flight;

public class General {
  private SettingGroup sg;
  private Flight parent;

  public final Setting<FlyModes> flyMode;
  public final Setting<Double> speed;
  public final Setting<Boolean> verticalSpeedMatch;
  public final Setting<Boolean> noSneak;
  public final Setting<Double> speed1;
  public final Setting<Double> speed2;
  public final Setting<Boolean> canClip;
  public final Setting<Boolean> showInfo;

  public General(SettingGroup sg, Flight parent) {
    this.sg = sg;
    this.parent = parent;

    flyMode = sg.add(new EnumSetting.Builder<FlyModes>()
        .name("mode")
        .description("The method of applying fly.")
        .defaultValue(FlyModes.Abilities)
        .onChanged(mode -> {
          if (!parent.isActive() || !Utils.canUpdate())
            return;
          parent.abilitiesOff();
          parent.onFlyModeChanged(mode);
        })
        .build());

    speed = sg.add(new DoubleSetting.Builder()
        .name("speed")
        .description("Your speed when flying.")
        .defaultValue(0.1)
        .min(0.0)
        .visible(() -> flyMode.get() == FlyModes.Abilities || flyMode.get() == FlyModes.Velocity)
        .build());

    verticalSpeedMatch = sg.add(new BoolSetting.Builder()
        .name("vertical-speed-match")
        .description("Matches your vertical speed to your horizontal speed, otherwise uses vanilla ratio.")
        .defaultValue(false)
        .visible(() -> flyMode.get() == FlyModes.Velocity)
        .build());

    noSneak = sg.add(new BoolSetting.Builder()
        .name("no-sneak")
        .description("Prevents you from sneaking while flying.")
        .defaultValue(false)
        .visible(() -> flyMode.get() == FlyModes.Velocity)
        .build());

    speed1 = sg.add(new DoubleSetting.Builder()
        .name("speed")
        .description("Fly speed.")
        .defaultValue(1.25)
        .max(2500)
        .sliderRange(0, 2500)
        .visible(() -> flyMode.get() == FlyModes.Matrix_Exploit)
        .build());

    speed2 = sg.add(new DoubleSetting.Builder()
        .name("speed-2")
        .description("Fly speed.")
        .defaultValue(0.3)
        .max(5)
        .sliderRange(0, 5)
        .visible(() -> flyMode.get() == FlyModes.Matrix_Exploit_2)
        .build());

    canClip = sg.add(new BoolSetting.Builder()
        .name("can-clip")
        .description("Whether to clip into the ground on activation.")
        .defaultValue(true)
        .visible(() -> flyMode.get() == FlyModes.Vulcan_Clip)
        .build());

    showInfo = sg.add(new BoolSetting.Builder()
        .name("show-info")
        .description("Displays information about whether this mode is running on the server.")
        .defaultValue(true)
        .visible(() -> flyMode.get() == FlyModes.Vulcan_Clip)
        .build());
  }
}
