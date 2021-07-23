package xyz.fmdc.reportsender.mod;

import net.minecraft.launchwrapper.Launch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.fmdc.reportsender.Hook;

import java.io.*;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class HookRegistry {
    public static final Map<String, Hook> hookDataList = new HashMap<>();
    public static final String propertiesDir = "/xyz/fmdc/reportsender/properties";
    public static Logger logger = LogManager.getLogger("ReportSender");

    public static void registration() {
        try {
            List<URL> jarList = Launch.classLoader.getSources();
            PathMatcher propMatcher = FileSystems.getDefault().getPathMatcher("glob:*.properties");
            for (URL jar : jarList) {
                if (!jar.getProtocol().equals("file")) continue;
                File jarFile = new File(jar.getPath());
                if (jarFile.isFile()) {
                    FileSystem fileSystem = FileSystems.newFileSystem(jarFile.toPath(), null);
                    Path myPath = fileSystem.getPath(propertiesDir);
                    if (!Files.exists(myPath)) continue;
                    Stream<Path> walk = Files.walk(myPath, 1);
                    for (Iterator<Path> it = walk.iterator(); it.hasNext(); ) {
                        Path path = it.next();
                        if (!Files.isDirectory(path) && propMatcher.matches(path.getFileName()))
                            readPropertiesFromStream(Files.newInputStream(path), path.toString());
                    }
                } else {
                    InputStream is = HookRegistry.class.getResourceAsStream(propertiesDir);
                    if (is == null) continue;
                    for (String filename : getResourceFiles(is)) {
                        URL file = HookRegistry.class.getResource(propertiesDir + "/" + filename);
                        if (file == null) continue;
                        if (file.getProtocol().equals("file")) {
                            File fileF = new File(file.getPath());
                            if (!fileF.isDirectory() && fileF.getName().endsWith(".properties"))
                                readPropertiesFromStream(file.openStream(), file.getFile());
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void registration(String name, String developer, String description, String hookUrl, String rsaPubKey) {
        if (hookDataList.containsKey(name)) {
            HookRegistry.logger.warn("The hook " + name + " has already been registered.");
            return;
        }
        Hook data = new Hook(name, developer, description, hookUrl, rsaPubKey);
        hookDataList.put(name, data);
        HookRegistry.logger.info("Hook " + name + " is now registered.");
    }

    private static List<String> getResourceFiles(InputStream is) throws IOException {
        List<String> filenames = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(is)))) {
            String resource;
            while ((resource = br.readLine()) != null) {
                filenames.add(resource);
            }
        }
        return filenames;
    }

    public static void readPropertiesFromStream(InputStream stream, String fileName) {
        if (stream != null) {
            Properties properties = new Properties();
            try {
                properties.load(stream);
                registration(
                        properties.getProperty("name"),
                        properties.getProperty("developer-name"),
                        properties.getProperty("description"),
                        properties.getProperty("webhook-url"),
                        properties.getProperty("rsa-public-key"));
            } catch (IOException e) {
                e.printStackTrace();
                HookRegistry.logger.error("cant load " + fileName + " ReportSender .properties file.");
            }
        }
    }
}
