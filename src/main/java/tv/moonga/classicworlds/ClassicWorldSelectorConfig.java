package tv.moonga.classicworlds;

import net.runelite.client.config.*;

@ConfigGroup(ClassicWorldSelectorConfig.GROUP)
public interface ClassicWorldSelectorConfig extends Config {
  String GROUP="moongaClassicWorldSelector";

  @ConfigSection(name="Button behaviour",description="Appearance of the login screen world button",position=0,closedByDefault=false)
  String buttonBehaviour="buttonBehaviour";
  @ConfigSection(name="World list",description="World list hover text",position=10,closedByDefault=false)
  String worldList="worldList";
  @ConfigSection(name="Favourite settings",description="Enable favourites and choose their layout",position=20,closedByDefault=false)
  String favouriteSettings="favouriteSettings";
  @ConfigSection(name="Filters",description="Priorities, membership visibility and list view",position=30,closedByDefault=false)
  String filters="filters";

  @ConfigItem(keyName="showFlag",name="Country flag",description="Show the current world's country flag beside World ###",section=buttonBehaviour,position=0)
  default boolean showFlag(){return true;}
  @ConfigItem(keyName="worldInfo",name="World info",description="On shows the current world's activity or mode. Off shows Click to switch.",section=buttonBehaviour,position=1)
  default boolean worldInfo(){return buttonLine()==WorldPreferences.ButtonLine.GAME_MODE;}
  // Keep the original enum key as a migration fallback for existing configurations.
  @ConfigItem(keyName="buttonLine",name="Legacy world info",description="Previous world info preference",hidden=true)
  default WorldPreferences.ButtonLine buttonLine(){return WorldPreferences.ButtonLine.CLICK_TO_SWITCH;}

  @ConfigItem(keyName="displaySize",name="World hover text size",description="Only the compact world tooltip: Normal 100%, Large 115%, Extra large 130%",section=worldList,position=0)
  default DisplaySize displaySize(){return DisplaySize.NORMAL;}
  @ConfigItem(keyName="boldHover",name="Bold",description="Use the client's bold bitmap font for world hover text only",section=worldList,position=1)
  default boolean boldHover(){return false;}

  @ConfigItem(keyName="favouritesEnabled",name="Favourites",description="Enable favourite highlighting, ordering and right-click toggling. Turning this off keeps your saved favourites.",section=favouriteSettings,position=0)
  default boolean favouritesEnabled(){return true;}
  @ConfigItem(keyName="favouriteView",name="Layout",description="On top keeps favourites first within preference groups; Separate uses a column on the left",section=favouriteSettings,position=1)
  default WorldPreferences.FavouriteView favouriteView(){return WorldPreferences.FavouriteView.WITH_FILTERS;}
  @ConfigItem(keyName="favouriteWorlds",name="Favourite worlds",description="Ordered favourite world IDs, managed by right-clicking worlds",hidden=true)
  default String favouriteWorlds(){return "";}

  @ConfigItem(keyName="preferredMode",name="Game mode",description="Place this game mode first without hiding other worlds",section=filters,position=0)
  default WorldPreferences.GameMode preferredMode(){return WorldPreferences.GameMode.ALL;}
  @ConfigItem(keyName="preferredRegion",name="Region",description="Place this region first within each game-mode group",section=filters,position=1)
  default WorldPreferences.Region preferredRegion(){return WorldPreferences.Region.ALL;}
  @ConfigItem(keyName="membership",name="Membership",description="Show all, show only members/F2P, or place members/F2P first within preference groups",section=filters,position=2)
  default WorldPreferences.Membership membership(){return WorldPreferences.Membership.ALL;}
  @ConfigItem(keyName="compact",name="Compact view",description="Use the classic grid; turn off to show the activity beside each world",section=filters,position=3)
  default boolean compact(){return true;}

}
