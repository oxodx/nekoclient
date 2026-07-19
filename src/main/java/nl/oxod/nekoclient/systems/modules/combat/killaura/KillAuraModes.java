package nl.oxod.nekoclient.systems.modules.combat.killaura;

public enum KillAuraModes {
  Vannila,
  Matrix;

  @Override
  public String toString() {
    return super.toString().replaceAll("_", " ");
  }
}
