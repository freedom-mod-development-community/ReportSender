package xyz.fmdc.reportsender;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.UUID;

public class Sender {
    private static final String EOL = "\r\n";
    static SSLContext sslcontext;

    public static void send(Map<String, Hook> hooks) {
        TrustManager[] tm = {new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }
        }};
        try {
            sslcontext = SSLContext.getInstance("SSL");
            sslcontext.init(null, tm, null);
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        StringBuilder errMsg = new StringBuilder();
        try {
            for (Hook hook : hooks.values()) {
                PublicKey pubKey = EncryptionUtil.readPublicKeyFromPem(hook.rsaPubKey);
                for (int i = 0; i < Main.reports.size(); ++i) {
                    Report report = Main.reports.get(i);
                    SecretKey aesKey = EncryptionUtil.generateAESKey();
                    IvParameterSpec iv = EncryptionUtil.generateAESIV();
                    IvParameterSpec iv2 = EncryptionUtil.generateAESIV();
                    String strAesKey = EncryptionUtil.encode(aesKey.getEncoded());
                    String strIv = EncryptionUtil.encode(iv.getIV());
                    String strIv2 = EncryptionUtil.encode(iv2.getIV());
                    String encryptedAesKey = EncryptionUtil.encryptWithPublicKey(strAesKey, pubKey);
                    String encryptedIv = EncryptionUtil.encryptWithPublicKey(strIv, pubKey);
                    String encryptedIv2 = EncryptionUtil.encryptWithPublicKey(strIv2, pubKey);
                    String encryptedAesKeyAndIvAndIv2 = encryptedAesKey + "\n" + encryptedIv + "\n" + encryptedIv2;
                    String encryptedReport = EncryptionUtil.encryptAES(report.editedReport, aesKey, iv);
                    String encryptedInput = null;
                    if (Main.dialog.getUserInput() != null)
                        encryptedInput = EncryptionUtil.encryptAES(Main.dialog.getUserInput(), aesKey, iv2);
                    int res = postSend(report.fileName, encryptedReport,
                            hook.hookUrl, "POST", i == 0 && encryptedInput != null ? encryptedInput : null, encryptedAesKeyAndIvAndIv2);
                    if (res != 200) {
                        errMsg.append(hook.name).append(" ResCode: ").append(res).append("\n");
                        break;
                    }
                }
            }
        } catch (IOException | GeneralSecurityException e) {
            JOptionPane.showMessageDialog(Main.dialog, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        if (errMsg.length() != 0)
            JOptionPane.showMessageDialog(Main.dialog, "Failed to send to:" + errMsg, "Error", JOptionPane.ERROR_MESSAGE);
        else
            System.exit(0);
    }

    public static int postSend(String filename, String report, String urlStr, String method, String text, String keys) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection con;
        if (url.getProtocol().equals("https")) {
            HttpsURLConnection conS = (HttpsURLConnection) url.openConnection();
            conS.setSSLSocketFactory(sslcontext.getSocketFactory());
            con = conS;
        } else {
            con = (HttpURLConnection) url.openConnection();
        }
        final String boundary = UUID.randomUUID().toString();
        con.setDoOutput(true);
        con.setRequestMethod(method);
        con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        try (OutputStream out = con.getOutputStream()) {
            if (text != null) {
                out.write(("--" + boundary + EOL +
                        "Content-Disposition: form-data; name=\"text\"" + EOL + EOL +
                        text + EOL)
                        .getBytes(StandardCharsets.UTF_8)
                );
            }
            out.write(("--" + boundary + EOL +
                    "Content-Disposition: form-data; name=\"keys\"" + EOL + EOL +
                    keys + EOL)
                    .getBytes(StandardCharsets.UTF_8)
            );
            out.write(("--" + boundary + EOL +
                    "Content-Disposition: form-data; name=\"report\"; " +
                    "filename=\"" + filename + "\"" + EOL +
                    "Content-Type: application/octet-stream" + EOL + EOL)
                    .getBytes(StandardCharsets.UTF_8)
            );
            out.write(report.getBytes(StandardCharsets.UTF_8));
            out.write((EOL + "--" + boundary + "--" + EOL).getBytes(StandardCharsets.UTF_8));
            out.flush();

            return con.getResponseCode();
        } finally {
            con.disconnect();
        }
    }
}
