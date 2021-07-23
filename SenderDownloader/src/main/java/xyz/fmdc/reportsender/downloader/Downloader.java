package xyz.fmdc.reportsender.downloader;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.apache.logging.log4j.LogManager;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reportsender Downloader
 *
 * @author anatawa12
 * @author noyciy7037
 */
public class Downloader implements ITweaker {
    public static final String latestFileLinkFileURL = "https://raw.githubusercontent.com/freedom-mod-development-community/ReportSender/master/LatestModUrl.txt";
    private Downloading downloadWindow;
    private boolean downloaded;
    private File downloadedFile;
    static SSLContext sslcontext;

    @Override
    public void acceptOptions(List<String> args, File gameDir, final File assetsDir, String profile) {
        if(!args.contains("--accessToken") && !args.contains("--assetIndex"))return;
        try {
            Class.forName("xyz.fmdc.reportsender.VersionInfo");
            return;
        } catch (ClassNotFoundException ignored) {
        }
        try {
            File rep_modDir = new File((gameDir != null ? gameDir : new File(".")).getAbsolutePath() + File.separator + "mods" + File.separator + "reportsender");
            if (rep_modDir.exists()) {
                try {
                    for (File f : Objects.requireNonNull(rep_modDir.listFiles())) {
                        if (!f.getName().endsWith(".jar")) continue;
                        Launch.classLoader.addURL(f.toURI().toURL());
                        addClassPath(f.toURI().toURL());
                    }
                    Class.forName("xyz.fmdc.reportsender.VersionInfo");
                    Launch.classLoader.registerTransformer("xyz.fmdc.reportsender.mod.asm.RSMCoreTransformer");
                    return;
                } catch (MalformedURLException | ClassNotFoundException | NullPointerException e) {
                    e.printStackTrace();
                }
            }
            URL lFL = new URL(latestFileLinkFileURL);
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
            setupSSLContext();
            HttpURLConnection connection;
            if (lFL.getProtocol().equals("https")) {
                HttpsURLConnection conS = (HttpsURLConnection) lFL.openConnection();
                conS.setSSLSocketFactory(sslcontext.getSocketFactory());
                connection = conS;
            } else {
                connection = (HttpURLConnection) lFL.openConnection();
            }
            connection.setRequestProperty("accept", "text/plain");
            InputStream responseStream = connection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(responseStream);
            Stream<String> streamOfString = new BufferedReader(inputStreamReader).lines();
            List<String> data = streamOfString.collect(Collectors.toList());
            URL fileLink = new URL(data.get(1));
            downloadWindow = new Downloading();
            downloadWindow.run();
            if (!rep_modDir.exists() && !rep_modDir.mkdirs()) return;
            downloadedFile = download(fileLink, rep_modDir);
            downloaded = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        if (downloaded) {
            try {
                classLoader.addURL(downloadedFile.toURI().toURL());
                addClassPath(downloadedFile.toURI().toURL());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            classLoader.registerTransformer("xyz.fmdc.reportsender.mod.asm.RSMCoreTransformer");
        }
    }

    @Override
    public String getLaunchTarget() {
        return null;
    }

    @Override
    public String[] getLaunchArguments() {
        return new String[0];
    }

    public File download(URL url, File modsFolder) throws IOException {
        String path = url.getPath();
        String name = modsFolder.getAbsolutePath() + File.separator + path.substring(path.lastIndexOf("/") + 1);
        int size = 0;
        HttpURLConnection connection;
        if (url.getProtocol().equals("https")) {
            HttpsURLConnection conS = (HttpsURLConnection) url.openConnection();
            conS.setSSLSocketFactory(sslcontext.getSocketFactory());
            connection = conS;
        } else {
            connection = (HttpURLConnection) url.openConnection();
        }
        try (DataInputStream in = new DataInputStream(connection.getInputStream());
             DataOutputStream out = new DataOutputStream(new FileOutputStream(name))) {
            int contentLength = connection.getContentLength();
            byte[] buf = new byte[8192];
            int len;
            if (contentLength == -1)
                downloadWindow.setProgress(-1);
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
                size += len;
                if (contentLength != -1)
                    downloadWindow.setProgress((double) size / contentLength);
            }
            out.flush();
        }

        LogManager.getLogger().info(name + " - " + size + " bytes");
        downloadWindow.dispose();
        return new File(name);
    }

    /**
     * ClassPathの追加。<br />
     *
     * @param path 追加するPATH
     */
    private static void addClassPath(URL path) {
        URLClassLoader loader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        try {
            Method m = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            m.setAccessible(true);
            m.invoke(loader, path);
        } catch (Exception e) {
            throw new RuntimeException("Error adding to classpath: " + path, e);
        }
    }

    private void setupSSLContext() {
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
    }
}
