package nl.oxod.nekoclient.systems.modules.movement.phase.modes;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.phys.Vec3;
import nl.oxod.nekoclient.systems.modules.movement.phase.Phase;
import nl.oxod.nekoclient.systems.modules.movement.phase.PhaseMode;
import nl.oxod.nekoclient.systems.modules.movement.phase.PhaseModes;

public class Intave extends PhaseMode {
	private boolean mining;

	public Intave(Phase settings) {
		super(PhaseModes.Intave, settings);
	}

	@Override
	public void onTickEventPre(meteordevelopment.meteorclient.events.world.TickEvent.Pre event) {
		if (mc.player == null || mc.getConnection() == null) return;

		LocalPlayer player = mc.player;
		boolean check = mc.options.keyAttack.isDown() && player.getXRot() > 80.0f;
		BlockPos below = player.blockPosition().offset(0, -1, 0);

		if (check) {
			mc.getConnection().send(new ServerboundPlayerActionPacket(
				ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, below, Direction.UP));
			mining = true;
		} else if (mining) {
			mining = false;
		}

		if (mining) {
			player.setPos(player.getX(), player.getY() - 0.0052, player.getZ());
		}

		if (player.isShiftKeyDown()) {
			float distance = 0.005f;
			double rotation = Math.toRadians(player.getYRot());
			if (mc.options.keyUp.isDown()) move(player, rotation, distance, 1, 1);
			else if (mc.options.keyDown.isDown()) move(player, rotation, -distance, 1, -1);
			else if (mc.options.keyLeft.isDown()) move(player, rotation, distance, -1, 1);
			else if (mc.options.keyRight.isDown()) move(player, rotation, -distance, -1, -1);
		}
	}

	@Override
	public String info() {
		if (mc.player == null) return "";
		if (mining) return "sinking";
		return mc.options.keyAttack.isDown() ? "look down" : "hold attack";
	}

	private static void move(LocalPlayer player, double rotation, float distance, int xMultiplier, int zMultiplier) {
		double xx = Math.cos(rotation) * distance * xMultiplier;
		double zz = Math.sin(rotation) * distance * zMultiplier;
		player.setPos(player.getX() + xx, player.getY(), player.getZ() + zz);
	}
}
