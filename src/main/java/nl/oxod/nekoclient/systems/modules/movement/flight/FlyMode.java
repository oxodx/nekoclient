package nl.oxod.nekoclient.systems.modules.movement.flight;

import meteordevelopment.meteorclient.events.entity.player.CanWalkOnFluidEvent;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import net.minecraft.client.Minecraft;

public abstract class FlyMode {
	protected final Minecraft mc = Minecraft.getInstance();
	protected final Flight settings;
	protected final FlyModes mode;

	public FlyMode(FlyModes mode, Flight settings) {
		this.mode = mode;
		this.settings = settings;
	}

	public void onActivate() {}
	public void onDeactivate() {}
	public void onTickEventPre(TickEvent.Pre event) {}
	public void onTickEventPost(TickEvent.Post event) {}
	public void onSendPacket(PacketEvent.Send event) {}
	public void onSentPacket(PacketEvent.Sent event) {}
	public void onReceivePacket(PacketEvent.Receive event) {}
	public void onCanWalkOnFluid(CanWalkOnFluidEvent event) {}
	public void onCollisionShape(CollisionShapeEvent event) {}
	public void onPlayerMoveEvent(PlayerMoveEvent event) {}
	public void onPlayerMoveSendPre(SendMovementPacketsEvent.Pre event) {}
}
