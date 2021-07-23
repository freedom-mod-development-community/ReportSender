package xyz.fmdc.reportsender;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class EncryptionUtil {
    public static final Logger logger = Logger.getLogger("ReportSender");
    public static Base64.Encoder encoder = Base64.getEncoder();

    static {
        try {
            Field field = Class.forName("javax.crypto.JceSecurity").getDeclaredField("isRestricted");
            field.setAccessible(true);

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            field.set(null, false);
        } catch (ClassNotFoundException | NoSuchFieldException | SecurityException | IllegalArgumentException
                | IllegalAccessException ex) {
            ex.printStackTrace(System.err);
        }
    }

    /**
     * 公開鍵によるRSA暗号化
     */
    public static String encryptWithPublicKey(String plain, PublicKey key) throws IllegalBlockSizeException,
            BadPaddingException, InvalidKeyException,
            NoSuchAlgorithmException, NoSuchPaddingException {

        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] plainBytes = plain.getBytes(StandardCharsets.UTF_8);
        return encoder.encodeToString(cipher.doFinal(plainBytes));
    }

    /**
     * 公開鍵を pem ファイルから読み込む
     */
    public static PublicKey readPublicKeyFromPem(String pubKey) {
        PublicKey key = null;

        try {
            BufferedReader br = new BufferedReader(new StringReader(pubKey));
            StringBuilder sb = new StringBuilder();
            boolean inKey = false;

            for (String line = br.readLine(); line != null; line = br.readLine()) {
                if (!inKey) {
                    if (line.startsWith("-----BEGIN ") && line.endsWith(" PUBLIC KEY-----")) {
                        inKey = true;
                    }
                } else {
                    if (line.startsWith("-----BEGIN ") && line.endsWith(" PUBLIC KEY-----")) {
                        break;
                    } else {
                        sb.append(line);
                    }
                }
            }
            byte[] decoded = DatatypeConverter.parseBase64Binary(sb.toString());
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            key = kf.generatePublic(keySpec);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.log(Level.SEVERE, "ERROR:\n" + sw);
        }
        return key;
    }

    public static SecretKey generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);

        return keyGen.generateKey();
    }

    public static IvParameterSpec generateAESIV() {
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[16];
        random.nextBytes(iv);

        return new IvParameterSpec(iv);
    }

    public static String encryptAES(String plainText, SecretKey key, IvParameterSpec iv) throws GeneralSecurityException {
        // 書式:"アルゴリズム/ブロックモード/パディング方式"
        Cipher encryptor = Cipher.getInstance("AES/CBC/PKCS5Padding");
        encryptor.init(Cipher.ENCRYPT_MODE, key, iv);

        return encoder.encodeToString(encryptor.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
    }

    public static String encode(byte[] data) {
        return encoder.encodeToString(data);
    }
}