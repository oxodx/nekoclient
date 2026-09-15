package nl.oxod.nekoclient.systems.modules.movement.phase.modes;

import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.oxod.nekoclient.systems.modules.movement.phase.Phase;
import nl.oxod.nekoclient.systems.modules.movement.phase.PhaseMode;
import nl.oxod.nekoclient.systems.modules.movement.phase.PhaseModes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BlinkPhase extends PhaseMode {
	private enum State {
		WAITING(false),
		PHASING(false),
		WALKING(true),
		FLUSHING(true);

		final boolean boxCollisions;

		State(boolean boxCollisions) {
			this.boxCollisions = boxCollisions;
		}
	}

	private static final int MAX_FRUITLESS_CYCLES = 3;

	private volatile State state = State.WAITING;
	private volatile boolean inCollisionCheck;
	private volatile boolean standDown;
	private volatile boolean recycleRequested;

	private final List<ServerboundMovePlayerPacket> packets = new ArrayList<>();
	private boolean sending;
	private int currentTicks;
	private boolean reachedWalking;
	private int fruitlessCycles;

	public BlinkPhase(Phase settings) {
		super(PhaseModes.Blink, settings);
	}

	@Override
	public void onActivate() {
		state = State.WAITING;
		currentTicks = 0;
		standDown = false;
		recycleRequested = false;
		reachedWalking = false;
		fruitlessCycles = 0;
		packets.clear();
	}

	@Override
	public void onDeactivate() {
		reset();
	}

	@Override
	public void onTickEventPre(TickEvent.Pre event) {
		LocalPlayer player = mc.player;
		if (player == null) return;

		if (state == State.WAITING) {
			if (doesCollideAt(player, player.position())) state = State.PHASING;
		} else if (state == State.PHASING) {
			if (!doesCollideAt(player, player.position())) {
				state = State.WALKING;
				reachedWalking = true;
			}
		}

		if (state == State.PHASING || state == State.WALKING) {
			currentTicks++;
			if (currentTicks > settings.maxTicks.get()) {
				recycleRequested = true;
			}
		}
	}

	@Override
	public void onTickEventPost(TickEvent.Post event) {
		if (standDown) {
			standDown = false;
			settings.disable();
			settings.info("Phase disabled: block did not clear.");
			return;
		}
		if (recycleRequested) {
			recycleRequested = false;
			recycle();
			return;
		}
		if (state != State.WALKING) return;

		LocalPlayer player = mc.player;
		if (player == null) return;

		boolean collides = false;
		inCollisionCheck = true;
		try {
			for (ServerboundMovePlayerPacket p : packets) {
				double x = p.getX(player.getX());
				double y = p.getY(player.getY());
				double z = p.getZ(player.getZ());
				if (doesCollideAt(player, new Vec3(x, y, z))) {
					collides = true;
					break;
				}
			}
		} finally {
			inCollisionCheck = false;
		}

		if (!collides) {
			settings.disable();
			settings.info("Phase: no collision, disabling.");
		}
	}

	@Override
	public void onSendPacket(meteordevelopment.meteorclient.events.packets.PacketEvent.Send event) {
		if (sending) return;
		State current = state;
		if (current == State.WAITING || current == State.FLUSHING) return;

		if (!(event.packet instanceof ServerboundMovePlayerPacket p)) return;

		event.cancel();
		synchronized (packets) {
			packets.add(p);
		}
	}

	@Override
	public void onReceivePacket(meteordevelopment.meteorclient.events.packets.PacketEvent.Receive event) {
		if (event.packet instanceof ClientboundBlockUpdatePacket
			|| event.packet instanceof ClientboundBlockEventPacket
			|| event.packet instanceof ClientboundSectionBlocksUpdatePacket
			|| event.packet instanceof ClientboundLevelChunkWithLightPacket
			|| event.packet instanceof ClientboundChunkBatchFinishedPacket
			|| event.packet instanceof ClientboundSetTitleTextPacket) {
			return;
		}
	}

	@Override
	public void onCollisionShape(CollisionShapeEvent event) {
		if (inCollisionCheck || state.boxCollisions) return;

		LocalPlayer player = mc.player;
		if (player == null) return;

		if (event.pos.getY() >= player.position().y
			|| (player.isShiftKeyDown() && player.onGround())) {
			return;
		}

		event.shape = Shapes.empty();
	}

	@Override
	public String info() {
		State current = state;
		return current == State.WAITING ? "" : current.name().toLowerCase(Locale.ROOT);
	}

	private void recycle() {
		reset();
		if (reachedWalking) fruitlessCycles = 0;
		else if (++fruitlessCycles >= MAX_FRUITLESS_CYCLES) standDown = true;
		reachedWalking = false;
	}

	private void reset() {
		state = State.FLUSHING;
		flushPackets(true);
		state = State.WAITING;
		currentTicks = 0;
		standDown = false;
		recycleRequested = false;
		reachedWalking = false;
		fruitlessCycles = 0;
	}

	private void flushPackets(boolean send) {
		sending = true;
		synchronized (packets) {
			if (send && mc.player != null) packets.forEach(mc.player.connection::send);
			packets.clear();
		}
		sending = false;
	}

	private static boolean doesCollideAt(LocalPlayer player, Vec3 pos) {
		AABB box = player.getBoundingBox().move(pos.subtract(player.position()));
		for (VoxelShape shape : player.level().getBlockCollisions(player, box)) {
			if (!shape.isEmpty()) return true;
		}
		return false;
	}
}