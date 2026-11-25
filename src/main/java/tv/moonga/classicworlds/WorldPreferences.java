package tv.moonga.classicworlds;

import java.util.*;
import net.runelite.api.WorldType;

/** Shared state for on-screen menus, persisted RuneLite settings and world ordering. */
public final class WorldPreferences {
  public enum GameMode {
    ALL("All modes"),MAIN_GAME("Main game"),PVP("Main game (PvP)"),HARDCORE("Hardcore"),DEADMAN("Deadman (Perm.)"),SPEEDRUNNING("Speedrunning");
    final String label;GameMode(String label){this.label=label;}@Override public String toString(){return label;}
    static GameMode of(EnumSet<WorldType> types){
      if(types.contains(WorldType.HARDCORE_MAIN))return HARDCORE;
      if(types.contains(WorldType.DEADMAN))return DEADMAN;
      if(types.contains(WorldType.QUEST_SPEEDRUNNING))return SPEEDRUNNING;
      return types.contains(WorldType.PVP)?PVP:MAIN_GAME;
    }
  }
  public enum Region {
    ALL("All regions",-1),USA("United States",0),USA_EAST("USA East",0),USA_WEST("USA West",0),UK("United Kingdom",1),AUSTRALIA("Australia",3),GERMANY("Germany",7),BRAZIL("Brazil",8),SINGAPORE("Singapore",10),SOUTH_AFRICA("South Africa",12),JAPAN("Japan",9),USA_EAST_WEST("USA East + West",0),USA_WEST_EAST("USA West + East",0);
    final String label;final int id;Region(String label,int id){this.label=label;this.id=id;}@Override public String toString(){return label;}
    int rank(WorldEntry world){
      if(this==ALL)return 0;
      // These WORLD_LOCATIONS values are also used by RuneLite's World Hopper.
      boolean east=world.location==0&&world.serverLocation==-42;
      boolean west=world.location==0&&world.serverLocation==-73;
      switch(this){
        case USA_EAST:return east?0:1;
        case USA_WEST:return west?0:1;
        case USA_EAST_WEST:return east?0:west?1:2;
        case USA_WEST_EAST:return west?0:east?1:2;
        default:return world.location==id?0:1;
      }
    }
  }
  public enum Membership {
    ALL("All worlds"),MEMBERS_ONLY("Members only"),F2P_ONLY("F2P only"),MEMBERS_FIRST("Members first"),F2P_FIRST("F2P first");
    final String label;Membership(String label){this.label=label;}@Override public String toString(){return label;}
  }
  public enum ButtonLine {
    CLICK_TO_SWITCH("Off"),GAME_MODE("On");
    final String label;ButtonLine(String label){this.label=label;}@Override public String toString(){return label;}
  }
  final GameMode mode;final Region region;final Membership membership;
  final boolean compact,showFlag;final ButtonLine buttonLine;
  static final WorldPreferences DEFAULT=new WorldPreferences(GameMode.ALL,Region.ALL,Membership.ALL,true,true,ButtonLine.CLICK_TO_SWITCH);
  WorldPreferences(GameMode mode,Region region,Membership membership,boolean compact,boolean showFlag,ButtonLine line){
    this.mode=mode==null?GameMode.ALL:mode;this.region=region==null?Region.ALL:region;this.membership=membership==null?Membership.ALL:membership;
    this.compact=compact;this.showFlag=showFlag;buttonLine=line==null?ButtonLine.CLICK_TO_SWITCH:line;
  }
  static WorldPreferences from(ClassicWorldSelectorConfig config){
    return config==null?DEFAULT:new WorldPreferences(config.preferredMode(),config.preferredRegion(),config.membership(),config.compact(),config.showFlag(),config.buttonLine());
  }
  WorldPreferences with(String key,Object value){
    return new WorldPreferences(key.equals("preferredMode")?(GameMode)value:mode,key.equals("preferredRegion")?(Region)value:region,
        key.equals("membership")?(Membership)value:membership,key.equals("compact")?(Boolean)value:compact,
        key.equals("showFlag")?(Boolean)value:showFlag,key.equals("buttonLine")?(ButtonLine)value:buttonLine);
  }
  List<WorldEntry> order(List<WorldEntry> source,Comparator<WorldEntry> base){
    List<WorldEntry> result=new ArrayList<>();
    for(WorldEntry world:source){
      if(membership==Membership.MEMBERS_ONLY&&!world.members())continue;
      if(membership==Membership.F2P_ONLY&&world.members())continue;
      result.add(world);
    }
    Comparator<WorldEntry> priority=Comparator.comparingInt((WorldEntry w)->mode==GameMode.ALL||GameMode.of(w.types)==mode?0:1)
        .thenComparingInt(region::rank)
        .thenComparingInt(w->membership==Membership.MEMBERS_FIRST&&!w.members()||membership==Membership.F2P_FIRST&&w.members()?1:0);
    result.sort(priority.thenComparing(base));return result;
  }
}
