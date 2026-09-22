package nl.oxod.nekoclient.util.security;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

public final class MmCrypto {
  public static final SecureRandom RNG = new SecureRandom();

  private MmCrypto() {
  }

  public static final class MmCryptoException extends RuntimeException {
    public MmCryptoException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  public static byte[] randomBytes(int n) {
    byte[] out = new byte[n];
    RNG.nextBytes(out);
    return out;
  }

  public static byte[] sha256(byte[]... parts) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      for (byte[] p : parts) md.update(p);
      return md.digest();
    } catch (Exception e) {
      throw new MmCryptoException("sha256", e);
    }
  }

  public static byte[] utf8(String s) {
    return s.getBytes(StandardCharsets.UTF_8);
  }

  public static byte[] aesGcmSeal(byte[] key32, byte[] nonce12, byte[] plaintext, byte[] aad) {
    try {
      Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
      c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key32, "AES"), new GCMParameterSpec(128, nonce12));
      if (aad != null) c.updateAAD(aad);
      return c.doFinal(plaintext);
    } catch (Exception e) {
      throw new MmCryptoException("aesGcmSeal", e);
    }
  }

  public static byte[] aesGcmOpen(byte[] key32, byte[] nonce12, byte[] ciphertext, byte[] aad) {
    try {
      Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
      c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key32, "AES"), new GCMParameterSpec(128, nonce12));
      if (aad != null) c.updateAAD(aad);
      return c.doFinal(ciphertext);
    } catch (javax.crypto.AEADBadTagException badTag) {
      return null;
    } catch (Exception e) {
      return null;
    }
  }

  public static String hex(byte[] data) {
    StringBuilder sb = new StringBuilder(data.length * 2);
    for (byte b : data) sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
    return sb.toString();
  }
}
