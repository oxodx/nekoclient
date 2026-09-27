package nl.oxod.nekoclient.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;

public class ElytraUtils {
	private static final Minecraft mc = Minecraft.getInstance();

	public static void startFly() {
		mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
	}
}
