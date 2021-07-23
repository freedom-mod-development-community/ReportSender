package xyz.fmdc.reportsender.mod.asm;

import xyz.fmdc.reportsender.Hook;
import xyz.fmdc.reportsender.Main;
import xyz.fmdc.reportsender.mod.HookRegistry;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("unused")
public class RSMCoreHook {
    public static Process process = null;
    private static PrintStream os = null;

    public synchronized static void crash(File oldFile, File newFile, String body) {
        if (oldFile != null) return;
        if (process == null) {
            Runtime r = Runtime.getRuntime();
            String javaPath = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            if (!new File(javaPath).exists()) {
                javaPath += ".exe";
                if (!new File(javaPath).exists()) {
                    HookRegistry.logger.error("The java executable could not be found.");
                    return;
                }
            }
            try {
                String senderJar = getJarFile(Main.class);
                if (senderJar == null) {
                    HookRegistry.logger.error("Classpath acquisition error.");
                    return;
                }
                String classpath = "." + File.pathSeparatorChar + senderJar;
                process = r.exec(new String[]{new File(javaPath).getAbsolutePath(), "-cp", classpath, Main.class.getName()});
                InputStream is = process.getInputStream();
                CompletableFuture.runAsync(() -> {
                    int i = 0;
                    try {
                        do {
                            if (i == 'k') break;
                        } while ((i = is.read()) >= 0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).get(5, TimeUnit.SECONDS);
            } catch (URISyntaxException | IOException | InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
                if (process != null) process.destroy();
                HookRegistry.logger.error("External process call failed.");
                return;
            }
            os = new PrintStream(process.getOutputStream());
            try {
                for (Hook hook : HookRegistry.hookDataList.values())
                    os.println("HOOK "
                            + URLEncoder.encode(hook.name, "UTF-8") + " "
                            + URLEncoder.encode(hook.developer, "UTF-8") + " "
                            + URLEncoder.encode(hook.description, "UTF-8") + " "
                            + URLEncoder.encode(hook.hookUrl, "UTF-8") + " "
                            + URLEncoder.encode(hook.rsaPubKey, "UTF-8"));
                os.println("END_HOOKS");
                os.flush();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        if (os == null) return;
        try {
            os.println("REPORT " + URLEncoder.encode(newFile.getName(), "UTF-8") + " " + URLEncoder.encode(body, "UTF-8"));
            os.flush();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public static String getJarFile(Class<?> clazz) throws URISyntaxException, UnsupportedEncodingException {
        String path = clazz.getProtectionDomain().getCodeSource().getLocation().getPath();
        if (path.endsWith("!/" + clazz.getName().replace(".", "/") + ".class") && path.startsWith("file:/"))
            return new File(path.substring(6, path.indexOf("!/" + clazz.getName().replace(".", "/") + ".class"))).getAbsolutePath();
        else if (path.startsWith("/") && path.endsWith(".jar"))
            return new File(path.substring(1)).getAbsolutePath();
        return null;
    }
}
