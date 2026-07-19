package nl.oxod.nekoclient.systems.modules.combat.killaura.settings;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import nl.oxod.nekoclient.systems.modules.combat.killaura.KillAura;

public class Timing {
  private SettingGroup sg;
  private KillAura parent;

  public final Setting<Boolean> pauseOnLag;
  public final Setting<Boolean> pauseOnUse;
  public final Setting<Boolean> pauseOnCA;
  public final Setting<Boolean> tpsSync;
  public final Setting<Boolean> customDelay;
  public final Setting<Integer> hitDelay;
  public final Setting<Integer> switchDelay;

  public Timing(SettingGroup sg, KillAura parent) {
    this.sg = sg;
    this.parent = parent;

    pauseOnLag = sg.add(new BoolSetting.Builder()
        .name("pause-on-lag")
        .description("Pauses if the server is lagging.")
        .defaultValue(true)
        .build());

    pauseOnUse = sg.add(new BoolSetting.Builder()
        .name("pause-on-use")
        .description("Does not attack while using an item.")
        .defaultValue(false)
        .build());

    pauseOnCA = sg.add(new BoolSetting.Builder()
        .name("pause-on-CA")
        .description("Does not attack while CA is placing.")
        .defaultValue(true)
        .build());

    tpsSync = sg.add(new BoolSetting.Builder()
        .name("TPS-sync")
        .description("Tries to sync attack delay with the server's TPS.")
        .defaultValue(true)
        .build());

    customDelay = sg.add(new BoolSetting.Builder()
        .name("custom-delay")
        .description("Use a custom delay instead of the vanilla cooldown.")
        .defaultValue(false)
        .build());

    hitDelay = sg.add(new IntSetting.Builder()
        .name("hit-delay")
        .description("How fast you hit the entity in ticks.")
        .defaultValue(11)
        .min(0)
        .sliderMax(60)
        .visible(customDelay::get)
        .build());

    switchDelay = sg.add(new IntSetting.Builder()
        .name("switch-delay")
        .description("How many ticks to wait before hitting an entity after switching hotbar slots.")
        .defaultValue(0)
        .min(0)
        .sliderMax(10)
        .build());
  }
}
