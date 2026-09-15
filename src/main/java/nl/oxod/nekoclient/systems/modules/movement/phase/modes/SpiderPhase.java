package nl.oxod.nekoclient.systems.modules.movement.phase.modes;

import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.BlockPos;
import nl.oxod.nekoclient.systems.modules.movement.phase.Phase;
import nl.oxod.nekoclient.systems.modules.movement.phase.PhaseMode;
import nl.oxod.nekoclient.systems.modules.movement.phase.PhaseModes;

public class SpiderPhase extends PhaseMode {
	private int spiderTicks = 1;

	public SpiderPhase(Phase settings) {
		super(PhaseModes.Spider, settings);
	}

	@Override
	public void onActivate() {
		spiderTicks = 1;
		LocalPlayer player = mc.player;
		if (player != null) setDeltaY(player, 0.0);
	}

	@Override
	public void onDeactivate() {
		if (mc.player != null) mc.player.noPhysics = false;
	}

	@Override
	public void onTickEventPre(TickEvent.Pre event) {
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null) return;

		switch (spiderTicks) {
			case 1 -> {
				if (mc.options.keyJump.isDown() && !mc.level.getBlockState(player.blockPosition()).isAir()) {
					setDeltaY(player, 0.42);
					spiderTicks++;
				}
				player.setOnGround(true);
			}
			case 2 -> {
				setDeltaY(player, 0.33);
				spiderTicks++;
			}
			case 3 -> {
				setDeltaY(player, 0.25);
				spiderTicks++;
			}
		}
		if (spiderTicks > 3) spiderTicks = 1;

		player.noPhysics = true;
		if (player.isShiftKeyDown()) {
			Vec3 velocity = player.getDeltaMovement();
			double speed = 0.179;
			float yaw = (float) Math.toRadians(player.getYRot());
			double sin = Math.sin(yaw);
			double cos = Math.cos(yaw);

			double strafeX = 0, strafeZ = 0;
			if (mc.options.keyUp.isDown()) { strafeX += sin; strafeZ -= cos; }
			if (mc.options.keyDown.isDown()) { strafeX -= sin; strafeZ += cos; }
			if (mc.options.keyLeft.isDown()) { strafeX -= cos; strafeZ -= sin; }
			if (mc.options.keyRight.isDown()) { strafeX += cos; strafeZ += sin; }

			double len = Math.sqrt(strafeX * strafeX + strafeZ * strafeZ);
			if (len > 0) {
				strafeX = strafeX / len * speed;
				strafeZ = strafeZ / len * speed;
			}
			player.setDeltaMovement(strafeX, velocity.y, strafeZ);
		}
	}

	@Override
	public void onCollisionShape(CollisionShapeEvent event) {
		BlockState state = event.state;
		if (state.getBlock() instanceof LiquidBlock) return;

		LocalPlayer player = mc.player;
		if (player == null) return;

		event.shape = event.pos.getY() >= player.getY() ? Shapes.empty() : Shapes.block();
	}

	private static void setDeltaY(LocalPlayer player, double y) {
		Vec3 velocity = player.getDeltaMovement();
		player.setDeltaMovement(velocity.x, y, velocity.z);
	}
}
