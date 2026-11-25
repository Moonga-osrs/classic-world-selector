package tv.moonga.classicworlds;

import net.runelite.client.config.*;

@ConfigGroup(ClassicWorldSelectorConfig.GROUP)
public interface ClassicWorldSelectorConfig extends Config {
  String GROUP="moongaClassicWorldSelector";
  @ConfigItem(keyName="displaySize",name="World hover text size",
      description="Only the compact world tooltip: Normal 100%, Larger 115%, Extra Large 130%. The login button stays the same size.",position=0)
  default DisplaySize displaySize(){return DisplaySize.NORMAL;}
  @ConfigItem(keyName="showFlag",name="Login button country flag",description="Show the current world's country flag beside World ###",position=1)
  default boolean showFlag(){return true;}
  @ConfigItem(keyName="buttonLine",name="World info on login button",description="On shows the current game mode or activity. Off shows Click to switch.",position=2)
  default WorldPreferences.ButtonLine buttonLine(){return WorldPreferences.ButtonLine.CLICK_TO_SWITCH;}
  @ConfigItem(keyName="preferredMode",name="Game mode first",description="Place this game mode first without hiding other worlds",position=3)
  default WorldPreferences.GameMode preferredMode(){return WorldPreferences.GameMode.ALL;}
  @ConfigItem(keyName="preferredRegion",name="Region first",description="Place this region first within each game-mode group",position=4)
  default WorldPreferences.Region preferredRegion(){return WorldPreferences.Region.ALL;}
  @ConfigItem(keyName="membership",name="Membership",description="Show all, show only members/F2P, or place members/F2P first within preference groups",position=5)
  default WorldPreferences.Membership membership(){return WorldPreferences.Membership.ALL;}
  @ConfigItem(keyName="compact",name="Compact view",description="Use the classic grid; turn off to show the activity beside each world",position=6)
  default boolean compact(){return true;}
}
