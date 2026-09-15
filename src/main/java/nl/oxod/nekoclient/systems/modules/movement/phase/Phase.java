package nl.oxod.nekoclient.systems.modules.movement.phase;

import meteordevelopment.meteorclient.events.entity.player.CanWalkOnFluidEvent;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;
import nl.oxod.nekoclient.systems.modules.movement.phase.modes.BlinkPhase;
import nl.oxod.nekoclient.systems.modules.movement.phase.modes.Clip;
import nl.oxod.nekoclient.systems.modules.movement.phase.modes.Intave;
import nl.oxod.nekoclient.systems.modules.movement.phase.modes.SpiderPhase;

public class Phase extends Module {
	private final SettingGroup sgGeneral = settings.getDefaultGroup();

	public final Setting<PhaseModes> mode = sgGeneral.add(new EnumSetting.Builder<PhaseModes>()
		.name("mode")
		.description("The method of phasing through blocks.")
		.defaultValue(PhaseModes.Clip)
		.onChanged(this::switchMode)
		.build()
	);

	public final Setting<Integer> maxTicks = sgGeneral.add(new IntSetting.Builder()
		.name("maximum")
		.description("Blink tick budget.")
		.defaultValue(120)
		.min(1)
		.max(300)
		.sliderMax(300)
		.visible(() -> mode.get() == PhaseModes.Blink)
		.build()
	);

	private PhaseMode currentMode;

	public Phase() {
		super(Categories.Movement, "phase", "Phase through blocks.");
		currentMode = createMode(mode.get());
	}

	@Override
	public void onActivate() {
		currentMode = createMode(mode.get());
		currentMode.onActivate();
	}

	@Override
	public void onDeactivate() {
		currentMode.onDeactivate();
	}

	@Override
	public String getInfoString() {
		String info = currentMode.info();
		return info.isEmpty() ? mode.get().name() : mode.get().name() + " " + info;
	}

	@EventHandler
	private void onPreTick(TickEvent.Pre event) {
		currentMode.onTickEventPre(event);
	}

	@EventHandler
	private void onPostTick(TickEvent.Post event) {
		currentMode.onTickEventPost(event);
	}

	@EventHandler
	private void onSendPacket(PacketEvent.Send event) {
		currentMode.onSendPacket(event);
	}

	@EventHandler
	public void onSentPacket(PacketEvent.Sent event) {
		currentMode.onSentPacket(event);
	}

	@EventHandler
	private void onReceivePacket(PacketEvent.Receive event) {
		currentMode.onReceivePacket(event);
	}

	@EventHandler
	public void onCanWalkOnFluid(CanWalkOnFluidEvent event) {
		currentMode.onCanWalkOnFluid(event);
	}

	@EventHandler
	public void onCollisionShape(CollisionShapeEvent event) {
		currentMode.onCollisionShape(event);
	}

	@EventHandler
	private void onPlayerMoveEvent(PlayerMoveEvent event) {
		currentMode.onPlayerMoveEvent(event);
	}

	@EventHandler
	private void onPlayerMoveSendPre(SendMovementPacketsEvent.Pre event) {
		currentMode.onPlayerMoveSendPre(event);
	}

	private void switchMode(PhaseModes newMode) {
		if (isActive() && Utils.canUpdate()) {
			currentMode.onDeactivate();
			onActivate();
		} else {
			currentMode = createMode(newMode);
		}
	}

	private PhaseMode createMode(PhaseModes mode) {
		return switch (mode) {
			case Clip -> new Clip(this);
			case Intave -> new Intave(this);
			case Blink -> new BlinkPhase(this);
			case Spider -> new SpiderPhase(this);
		};
	}
}