package nl.oxod.nekoclient.systems.modules.combat.killaura;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import nl.oxod.nekoclient.systems.modules.combat.killaura.modes.Matrix;
import nl.oxod.nekoclient.systems.modules.combat.killaura.modes.Vannila;
import nl.oxod.nekoclient.systems.modules.combat.killaura.settings.General;
import nl.oxod.nekoclient.systems.modules.combat.killaura.settings.Targeting;
import nl.oxod.nekoclient.systems.modules.combat.killaura.settings.Timing;

import java.util.ArrayList;
import java.util.List;

public class KillAura extends Module {
  public KillAura() {
    super(Categories.Neko_Combat, "kill-aura", "Attacks specified entities around you.");
  }

  private final SettingGroup sgGeneral = settings.getDefaultGroup();
  private final SettingGroup sgTargeting = settings.createGroup("Targeting");
  private final SettingGroup sgTiming = settings.createGroup("Timing");

  public final General general = new General(sgGeneral, this);
  public final Targeting targeting = new Targeting(sgTargeting, this);
  public final Timing timing = new Timing(sgTiming, this);

  public final static ArrayList<Item> FILTER = new ArrayList<>(List.of(Items.DIAMOND_SWORD, Items.DIAMOND_AXE, Items.DIAMOND_PICKAXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE, Items.MACE, Items.DIAMOND_SPEAR, Items.TRIDENT));
  public boolean attacking, swapped;
  public static int previousSlot;

  private KillAuraMode currentMode;

  public void onModeChanged(KillAuraModes mode) {
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
	public void onTickPre(TickEvent.Pre event) {
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
	public void onTickPost(TickEvent.Post event) {
		currentMode.onTickPost(event);
	}

	@EventHandler
	public void onSendPacket(PacketEvent.Send event) {
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
