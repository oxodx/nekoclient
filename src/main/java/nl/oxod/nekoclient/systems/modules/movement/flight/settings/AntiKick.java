package nl.oxod.nekoclient.systems.modules.movement.flight.settings;

import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import nl.oxod.nekoclient.systems.modules.movement.flight.Flight;
import nl.oxod.nekoclient.systems.modules.movement.flight.Flight.AntiKickMode;

public class AntiKick {
  private SettingGroup sg;
  private Flight parent;

  public final Setting<AntiKickMode> antiKickMode;
  public final Setting<Integer> delay;
  public final Setting<Integer> offTime;

  public AntiKick(SettingGroup sg, Flight parent) {
    this.sg = sg;
    this.parent = parent;

    antiKickMode = sg.add(new EnumSetting.Builder<AntiKickMode>()
        .name("mode")
        .description("The mode for anti kick.")
        .defaultValue(AntiKickMode.Packet)
        .build());

    delay = sg.add(new IntSetting.Builder()
        .name("delay")
        .description("The amount of delay, in ticks, between flying down a bit and return to original position")
        .defaultValue(20)
        .min(1)
        .sliderMax(200)
        .build());

    offTime = sg.add(new IntSetting.Builder()
        .name("off-time")
        .description("The amount of delay, in ticks, to fly down a bit to reset floating ticks.")
        .defaultValue(1)
        .min(1)
        .sliderRange(1, 20)
        .build());
  }
}
