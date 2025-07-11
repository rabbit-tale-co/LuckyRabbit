package co.RabbitTale.luckyRabbit.animations;

import co.RabbitTale.luckyRabbit.utils.Logger;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

public class AnimationLicenseEncryption {

    private static final byte[] ENCRYPTION_KEY = {
        0x52, 0x61, 0x62, 0x62, 0x69, 0x74, 0x54, 0x61,
        0x6C, 0x65, 0x50, 0x72, 0x65, 0x6D, 0x69, 0x75
    };

    private static SecretKeySpec secretKey;

    static {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            byte[] key = sha.digest(ENCRYPTION_KEY);
            key = Arrays.copyOf(key, 16);
            secretKey = new SecretKeySpec(key, "AES");
        } catch (Exception e) {
            Logger.error("Failed to initialize encryption: " + e.getMessage());
        }
    }

    public static String encrypt(String data) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            Logger.error("Encryption failed: " + e.getMessage());
            return null;
        }
    }

    public static String decrypt(String encryptedData) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedData)));
        } catch (Exception e) {
            Logger.error("Decryption failed: " + e.getMessage());
            return null;
        }
    }

    public static String generateAnimationKey(String animationId, String pluginVersion) {
        String licenseData = String.format("%s:%s", animationId, pluginVersion);
        return encrypt(licenseData);
    }

    public static boolean verifyAnimationKey(String encryptedKey, String animationId, String currentPluginVersion) {
        try {
            String decrypted = decrypt(encryptedKey);
            if (decrypted == null) {
                return false;
            }

            String[] parts = decrypted.split(":");
            if (parts.length != 2) {
                return false;
            }

            String storedAnimationId = parts[0];
            String storedVersion = parts[1];

            // Sprawdz czy ID animacji i wersja pluginu sie zgadzaja
            return storedAnimationId.equals(animationId)
                    && storedVersion.equals(currentPluginVersion);
        } catch (Exception e) {
            return false;
        }
    }
}
