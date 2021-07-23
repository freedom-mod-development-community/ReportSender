package xyz.fmdc.reportsender.mod.asm;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

import java.io.File;
import java.util.List;

/**
 * Reportsender Tweaker
 *
 * @author anatawa12
 * @author noyciy7037
 */
@SuppressWarnings("unused")
public class RSMTweaker implements ITweaker {
    private boolean isServer = false;

    @Override
    public void acceptOptions(List<String> args, File gameDir, final File assetsDir, String profile) {
        if (!args.contains("--accessToken") && !args.contains("--assetIndex")) isServer = true;
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        if (isServer) return;
        classLoader.registerTransformer(RSMCoreTransformer.class.getName());
    }

    @Override
    public String getLaunchTarget() {
        return null;
    }

    @Override
    public String[] getLaunchArguments() {
        return new String[0];
    }
}
