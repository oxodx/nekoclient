package nl.oxod.nekoclient.systems.modules.movement.phase.modes;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;
import nl.oxod.nekoclient.systems.modules.movement.phase.Phase;
import nl.oxod.nekoclient.systems.modules.movement.phase.PhaseMode;
import nl.oxod.nekoclient.systems.modules.movement.phase.PhaseModes;

public class Clip extends PhaseMode {
	private static final double GRAVITY = 0.07840000152;

	public Clip(Phase settings) {
		super(PhaseModes.Clip, settings);
	}

	@Override
	public void onTickEventPre(meteordevelopment.meteorclient.events.world.TickEvent.Pre event) {
		if (mc.player == null || mc.getConnection() == null) return;

		LocalPlayer player = mc.player;
		Vec3 center = Vec3.atCenterOf(player.blockPosition());

		mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(
			center.x, player.getY() - GRAVITY, center.z,
			player.onGround(), player.horizontalCollision));

		settings.disable();
		settings.info("Phase: clip packet sent.");
	}
}
