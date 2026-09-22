package nl.oxod.nekoclient.systems.modules.movement.phase;

import meteordevelopment.meteorclient.events.entity.player.CanWalkOnFluidEvent;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.entity.player.SendMovementPacketsEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import net.minecraft.client.Minecraft;

public abstract class PhaseMode {
	protected final Minecraft mc = Minecraft.getInstance();
	protected final Phase settings;
	protected final PhaseModes mode;

	public PhaseMode(PhaseModes mode, Phase settings) {
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
	public void onTeleportPacket() {}
	public String info() { return ""; }
}
