package org.adorsys.docusafe.service.impl.bucketpathencryption;

import de.adorsys.common.exceptions.BaseExceptionHandler;
import de.adorsys.common.utils.HexUtil;
import de.adorsys.dfs.connection.api.complextypes.BucketDirectory;
import de.adorsys.dfs.connection.api.complextypes.BucketPath;
import de.adorsys.dfs.connection.api.complextypes.BucketPathUtil;
import lombok.extern.slf4j.Slf4j;
import org.adorsys.docusafe.service.api.bucketpathencryption.BucketPathEncryptionService;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public class BucketPathEncryptionServiceImpl implements BucketPathEncryptionService {
    public static final String NO_BUCKETPATH_ENCRYPTION = "SC-NO-BUCKETPATH-ENCRYPTION";

    private static boolean active = isActive();

    @Override
    public BucketPath encrypt(SecretKey secretKey, BucketPath bucketPath) {
        if (! active) {
            return bucketPath;
        }
        Cipher cipher = createCipher(secretKey, Cipher.ENCRYPT_MODE);
        List<String> elements = BucketPathUtil.split(BucketPathUtil.getAsString(bucketPath));
        return new BucketPath(encryptStringList(elements, cipher).toLowerCase());
    }

    @Override
    public BucketPath decrypt(SecretKey secretKey, BucketPath bucketPath) {
        if (! active) {
            return bucketPath;
        }
        Cipher cipher = createCipher(secretKey, Cipher.DECRYPT_MODE);
        List<String> elements = BucketPathUtil.split(BucketPathUtil.getAsString(bucketPath));
        return new BucketPath(decryptStringList(elements, cipher));
    }


    @Override
    public BucketDirectory encrypt(SecretKey secretKey, BucketDirectory bucketDirectory) {
        if (! active) {
            return bucketDirectory;
        }
        Cipher cipher = createCipher(secretKey, Cipher.ENCRYPT_MODE);
        List<String> elements = BucketPathUtil.split(BucketPathUtil.getAsString(bucketDirectory));
        return new BucketDirectory(encryptStringList(elements, cipher).toLowerCase());
    }

    @Override
    public BucketDirectory decrypt(SecretKey secretKey, BucketDirectory bucketDirectory) {
        if (! active) {
            return bucketDirectory;
        }
        Cipher cipher = createCipher(secretKey, Cipher.DECRYPT_MODE);
        List<String> elements = BucketPathUtil.split(BucketPathUtil.getAsString(bucketDirectory));
        return new BucketDirectory(decryptStringList(elements, cipher));
    }

    private static Cipher createCipher(SecretKey secretKey, int cipherMode) {
        try {
            byte[] key = secretKey.getEncoded();
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            key = sha.digest(key);
            // nur die ersten 128 bit nutzen
            key = Arrays.copyOf(key, 16);
            // der fertige Schluessel
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(cipherMode, secretKeySpec);
            return cipher;
        } catch (Exception e) {
            throw BaseExceptionHandler.handle(e);
        }

    }

    private String encryptStringList(List<String> elements, Cipher cipher) {
        String first = elements.remove(0);
        StringBuilder encryptedPathString = new StringBuilder();
        for(String subdir : elements) {
            byte[] encrypt = new byte[0];
            try {
                encrypt = cipher.doFinal(subdir.getBytes(UTF_8));
            } catch (IllegalBlockSizeException | BadPaddingException e) {
                e.printStackTrace();
            }

            String encryptedString = HexUtil.convertBytesToHexString(encrypt);
            encryptedPathString.append(BucketPath.BUCKET_SEPARATOR).append(encryptedString);
        }
        return first + BucketPath.BUCKET_SEPARATOR + encryptedPathString.toString();
    }

    private String decryptStringList(List<String> elements, Cipher cipher) {
        String first = elements.remove(0);
        StringBuilder decryptedPathString = new StringBuilder();
        for(String subdir : elements) {
            byte[] decrypt = HexUtil.convertHexStringToBytes(subdir.toUpperCase());
            byte[] decryptedBytes = new byte[0];
            try {
                decryptedBytes = cipher.doFinal(decrypt);
            } catch (IllegalBlockSizeException | BadPaddingException e) {
                e.printStackTrace();
            }
            decryptedPathString.append(BucketPath.BUCKET_SEPARATOR).append(new String(decryptedBytes, UTF_8));
        }
        return first + BucketPath.BUCKET_SEPARATOR + decryptedPathString.toString();
    }

    private static boolean isActive() {
        if (System.getProperty(NO_BUCKETPATH_ENCRYPTION) != null) {
            log.info("encryption is off");
            return false;
        }
        log.info("encryption is on");
        return true;

    }
}
