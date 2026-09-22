package nl.oxod.nekoclient.security;

public interface ProtectorFromPacketAccess {
  void protector$setFromPacket();

  default void protector$setSilent() {
  }
}
