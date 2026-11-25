package tv.moonga.classicworlds;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Development only; not included in the Plugin Hub artifact. */
public final class ClassicWorldSelectorDevLauncher {
  public static void main(String[] args) throws Exception {
    ExternalPluginManager.loadBuiltin(ClassicWorldSelectorPlugin.class);
    RuneLite.main(args);
  }
}
