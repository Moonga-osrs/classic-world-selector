package tv.moonga.classicworlds;

import java.util.EnumSet;
import net.runelite.api.World;
import net.runelite.api.WorldType;

final class WorldEntry {
  final int id, players, location;
  final String activity, address;
  final EnumSet<WorldType> types;
  final int serverLocation;

  WorldEntry(int id, int players, int location, String activity, String address, EnumSet<WorldType> types) {
    this(id,players,location,activity,address,types,0);
  }
  WorldEntry(int id,int players,int location,String activity,String address,EnumSet<WorldType> types,int serverLocation) {
    this.id=id; this.players=players; this.location=location;
    this.activity=activity==null?"-":activity; this.address=address;
    this.types=types.clone();
    this.serverLocation=serverLocation;
  }
  WorldEntry withServerLocation(int value){return new WorldEntry(id,players,location,activity,address,types,value);}
  static WorldEntry from(World w) {
    return new WorldEntry(w.getId(),w.getPlayerCount(),w.getLocation(),w.getActivity(),w.getAddress(),w.getTypes());
  }
  boolean members() { return types.contains(WorldType.MEMBERS); }
  boolean selectable() { return players>=0 && players<=1980 && address!=null && !address.isBlank(); }
  String population() { return players<0?"OFF":players>1980?"FULL":Integer.toString(players); }
  String typeLabel() {
    if(types.contains(WorldType.DEADMAN))return "Deadman";
    if(types.contains(WorldType.HARDCORE_MAIN))return "Hardcore";
    if(types.contains(WorldType.PVP))return "PvP";
    if(types.contains(WorldType.HIGH_RISK))return "High Risk";
    if(types.contains(WorldType.BETA_WORLD))return "Beta";
    if(types.contains(WorldType.QUEST_SPEEDRUNNING))return "Speedrunning";
    if(types.contains(WorldType.SKILL_TOTAL))return "Skill total";
    return members()?"Members":"Free";
  }
  int background() {
    int base=0;
    if(types.contains(WorldType.HARDCORE_MAIN))base=16;
    else if(types.contains(WorldType.PVP))base=2;
    else if(types.contains(WorldType.DEADMAN))base=4;
    else if(types.contains(WorldType.BETA_WORLD))base=6;
    else if(types.contains(WorldType.SEASONAL))base=8;
    else if(types.contains(WorldType.HIGH_RISK))base=10;
    else if(types.contains(WorldType.QUEST_SPEEDRUNNING))base=12;
    return base+(members()?1:0);
  }
  String tooltip() {
    String risk=types.contains(WorldType.PVP)||types.contains(WorldType.HIGH_RISK)||types.contains(WorldType.DEADMAN)
        ?" — "+typeLabel():"";
    return (activity.equals("-")?typeLabel():activity)+risk;
  }
}
