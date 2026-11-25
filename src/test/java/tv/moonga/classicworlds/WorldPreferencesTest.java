package tv.moonga.classicworlds;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.*;
import java.util.stream.Collectors;
import javax.swing.SwingUtilities;
import net.runelite.api.WorldType;
import org.junit.Test;
import static org.junit.Assert.*;

public class WorldPreferencesTest {
  private static WorldEntry world(int id,int region,boolean member,WorldType... special){
    EnumSet<WorldType> types=EnumSet.noneOf(WorldType.class);if(member)types.add(WorldType.MEMBERS);Collections.addAll(types,special);
    return new WorldEntry(id,100,region,"Pest Control","oldschool"+id+".runescape.com",types);
  }
  private static List<Integer> ids(List<WorldEntry> worlds){return worlds.stream().map(w->w.id).collect(Collectors.toList());}
  private static final Comparator<WorldEntry> BY_ID=Comparator.comparingInt(w->w.id);
  @Test public void hardcoreGermanyUsesTheExactRequestedPriorityAndKeepsAllWorlds(){
    List<WorldEntry> source=Arrays.asList(world(301,1,true),world(302,7,true),world(501,1,true,WorldType.HARDCORE_MAIN),world(502,7,true,WorldType.HARDCORE_MAIN));
    WorldPreferences p=WorldPreferences.DEFAULT.with("preferredMode",WorldPreferences.GameMode.HARDCORE).with("preferredRegion",WorldPreferences.Region.GERMANY);
    assertEquals(Arrays.asList(502,501,302,301),ids(p.order(source,BY_ID)));
    assertEquals(Arrays.asList(502,501,302,301),ids(p.order(source,BY_ID.reversed())));
    assertEquals(Arrays.asList(301,302,501,502),ids(source));
  }
  @Test public void mainGamePvpDoesNotIncludeHardcoreOrDeadmanPvp(){
    List<WorldEntry> worlds=Arrays.asList(world(301,0,true),world(302,0,true,WorldType.PVP,WorldType.DEADMAN),world(303,0,true,WorldType.PVP),world(304,0,true,WorldType.HARDCORE_MAIN));
    assertEquals(Arrays.asList(303,301,302,304),ids(WorldPreferences.DEFAULT.with("preferredMode",WorldPreferences.GameMode.PVP).order(worlds,BY_ID)));
  }
  @Test public void membersFirstAndF2pFirstRetainBothKinds(){
    List<WorldEntry> worlds=Arrays.asList(world(301,0,false),world(302,0,true),world(303,0,false));
    assertEquals(Arrays.asList(302,301,303),ids(WorldPreferences.DEFAULT.with("membership",WorldPreferences.Membership.MEMBERS_FIRST).order(worlds,BY_ID)));
    assertEquals(Arrays.asList(301,303,302),ids(WorldPreferences.DEFAULT.with("membership",WorldPreferences.Membership.F2P_FIRST).order(worlds,BY_ID)));
  }
  @Test public void onlyOptionsFilterAndSwitchingBackRestoresTheCompleteSource(){
    List<WorldEntry> worlds=Arrays.asList(world(301,0,false),world(302,0,true));
    assertEquals(Arrays.asList(302),ids(WorldPreferences.DEFAULT.with("membership",WorldPreferences.Membership.MEMBERS_ONLY).order(worlds,BY_ID)));
    assertEquals(Arrays.asList(301),ids(WorldPreferences.DEFAULT.with("membership",WorldPreferences.Membership.F2P_ONLY).order(worlds,BY_ID)));
    assertEquals(Arrays.asList(301,302),ids(WorldPreferences.DEFAULT.order(worlds,BY_ID)));
  }
  @Test public void regionAndModeStayAheadOfMembershipWithinTheCombinedOrder(){
    List<WorldEntry> worlds=Arrays.asList(world(301,7,true),world(501,1,true,WorldType.HARDCORE_MAIN),world(502,7,false,WorldType.HARDCORE_MAIN));
    WorldPreferences p=WorldPreferences.DEFAULT.with("preferredMode",WorldPreferences.GameMode.HARDCORE).with("preferredRegion",WorldPreferences.Region.GERMANY).with("membership",WorldPreferences.Membership.MEMBERS_FIRST);
    assertEquals(Arrays.asList(502,501,301),ids(p.order(worlds,BY_ID)));
  }
  @Test public void compactAndActivityLayoutsFitAndReachEveryWorld(){
    for(boolean compact:new boolean[]{true,false}){
      ClassicLayout layout=new ClassicLayout(500,compact,57);Set<Integer> seen=new HashSet<>();List<Rectangle> rectangles=new ArrayList<>();
      for(int i=0;i<layout.columns*layout.rows;i++){
        Rectangle r=layout.tile(i);assertTrue(new Rectangle(0,57,765,446).contains(r));for(Rectangle other:rectangles)assertFalse(other.intersects(r));rectangles.add(r);
      }
      for(int p=0;p<=layout.maxPage;p++)for(int i=0;i<layout.rows*layout.columns&&layout.first(p)+i<500;i++)seen.add(layout.first(p)+i);
      assertEquals(500,seen.size());if(!compact)assertEquals(330,layout.tileWidth);
    }
  }
  @Test public void dropdownDismissalNeverClicksThroughToAWorldAndSelectionReportsItsSetting(){
    AtomicReference<String> key=new AtomicReference<>();AtomicReference<Object> value=new AtomicReference<>();
    WorldMenus menu=new WorldMenus((k,v)->{key.set(k);value.set(v);});menu.activate(-100);
    assertEquals(Integer.valueOf(WorldMenus.DISMISS),menu.hit(new Point(300,350)));menu.activate(WorldMenus.DISMISS);assertNull(key.get());
    menu.activate(-100);menu.activate(-2003);assertEquals("preferredMode",key.get());assertEquals(WorldPreferences.GameMode.HARDCORE,value.get());assertFalse(menu.isOpen());
  }
  @Test public void switchingDropdownsDoesNotEraseOrReorderTheUnderlyingList()throws Exception{
    SwingUtilities.invokeAndWait(()->{
      ClassicWorldPanel p=new ClassicWorldPanel(w->{},()->{});p.setSize(765,503);p.fonts(BitmapFontTest.fixture(),BitmapFontTest.fixture());
      List<WorldEntry> data=new ArrayList<>();for(int i=0;i<300;i++)data.add(world(301+i,7,true,WorldType.HARDCORE_MAIN));p.worlds(data);
      p.options(WorldPreferences.DEFAULT.with("compact",false));Rectangle tile=p.worldLayout().tile(10);
      BufferedImage before=panelImage(p);p.activate(-100);panelImage(p);p.activate(-104);BufferedImage after=panelImage(p);
      assertEquals(before.getRGB(tile.x+2,tile.y+2),after.getRGB(tile.x+2,tile.y+2));
      assertNotEquals(0xff000000,after.getRGB(tile.x+2,tile.y+2));
    });
  }
  private static BufferedImage panelImage(ClassicWorldPanel p){BufferedImage image=new BufferedImage(765,503,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();p.paint(g);g.dispose();return image;}
  @Test public void activityRowClickChoosesTheCorrectPrioritisedWorld()throws Exception{
    SwingUtilities.invokeAndWait(()->{
      AtomicInteger selected=new AtomicInteger();ClassicWorldPanel p=new ClassicWorldPanel(w->selected.set(w.id),()->{});p.setSize(765,503);
      p.worlds(Arrays.asList(world(301,0,true),world(501,7,true,WorldType.HARDCORE_MAIN)));
      p.options(WorldPreferences.DEFAULT.with("compact",false).with("preferredMode",WorldPreferences.GameMode.HARDCORE));
      Rectangle first=p.worldLayout().tile(0);p.activate(p.hit(new Point(first.x+200,first.y+5)));assertEquals(501,selected.get());
    });
  }
  @Test public void defaultsKeepCompactOnAndTheLoginLineCanShowTheActualMode(){
    ClassicWorldSelectorConfig config=new ClassicWorldSelectorConfig(){};assertTrue(config.compact());assertTrue(config.showFlag());
    ClassicWorldButton button=new ClassicWorldButton(()->{});assertEquals("Click to switch",button.subtitle());
    button.options(WorldPreferences.DEFAULT.with("buttonLine",WorldPreferences.ButtonLine.GAME_MODE));
    button.worldInfo(7,EnumSet.of(WorldType.MEMBERS,WorldType.HARDCORE_MAIN));assertEquals("Mode: Hardcore",button.subtitle());
    button.worldInfo(0,EnumSet.of(WorldType.PVP));assertEquals("Mode: Main game (PvP)",button.subtitle());
  }
  @Test public void flagToggleRemovesOnlyTheCountryFlagPixels()throws Exception{
    SwingUtilities.invokeAndWait(()->{
      ClassicWorldButton b=new ClassicWorldButton(()->{});b.setSize(b.getPreferredSize());b.fonts(BitmapFontTest.fixture(),BitmapFontTest.fixture());
      BufferedImage flag=new BufferedImage(17,19,BufferedImage.TYPE_INT_ARGB);Graphics2D g=flag.createGraphics();g.setColor(Color.MAGENTA);g.fillRect(0,0,17,19);g.dispose();
      BufferedImage[] flags=new BufferedImage[58];flags[36]=flag;b.flagSprites(flags);b.worldInfo(7,EnumSet.of(WorldType.MEMBERS));
      assertTrue(magenta(b)>0);b.options(WorldPreferences.DEFAULT.with("showFlag",false));assertEquals(0,magenta(b));
    });
  }
  private static int magenta(ClassicWorldButton button){BufferedImage image=new BufferedImage(button.getWidth(),button.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();button.paint(g);g.dispose();int count=0;for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if(image.getRGB(x,y)==0xffff00ff)count++;return count;}
}
