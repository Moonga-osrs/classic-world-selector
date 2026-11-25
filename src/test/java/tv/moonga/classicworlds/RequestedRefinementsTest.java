package tv.moonga.classicworlds;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.SwingUtilities;
import net.runelite.api.*;
import net.runelite.client.config.ConfigManager;
import org.junit.Test;

public class RequestedRefinementsTest {
  private static WorldEntry world(int id,int region,int coast){return new WorldEntry(id,100,region,"Pest Control","oldschool"+id+".runescape.com",EnumSet.of(WorldType.MEMBERS),coast);}
  private static List<Integer> ordered(WorldPreferences.Region region,List<WorldEntry> source){return WorldPreferences.DEFAULT.with("preferredRegion",region).order(source,Comparator.comparingInt(w->w.id)).stream().map(w->w.id).collect(Collectors.toList());}
  @Test public void bothCustomCoastOrdersAreExactAndDoNotGuessUnknownUsWorlds(){
    List<WorldEntry> source=Arrays.asList(world(301,7,0),world(302,0,-73),world(303,0,-42),world(304,0,0));
    assertEquals(Arrays.asList(303,302,301,304),ordered(WorldPreferences.Region.USA_EAST_WEST,source));
    assertEquals(Arrays.asList(302,303,301,304),ordered(WorldPreferences.Region.USA_WEST_EAST,source));
    assertEquals(Arrays.asList(303,301,302,304),ordered(WorldPreferences.Region.USA_EAST,source));
    assertEquals(Arrays.asList(302,301,303,304),ordered(WorldPreferences.Region.USA_WEST,source));
    assertEquals(Arrays.asList(302,303,304,301),ordered(WorldPreferences.Region.USA,source));
  }
  @Test public void customOptionsAreTheFinalEntriesImmediatelyBelowJapan(){
    WorldPreferences.Region[] regions=WorldPreferences.Region.values();
    assertEquals(WorldPreferences.Region.JAPAN,regions[regions.length-3]);
    assertEquals(WorldPreferences.Region.USA_EAST_WEST,regions[regions.length-2]);assertEquals(WorldPreferences.Region.USA_WEST_EAST,regions[regions.length-1]);
  }
  @Test public void clientWorldLocationEnumEnrichesLiveWorldEntries()throws Exception{
    ClassicWorldSelectorPlugin plugin=new ClassicWorldSelectorPlugin();Client client=mock(Client.class);EnumComposition locations=mock(EnumComposition.class);World world=mock(World.class);
    when(world.getId()).thenReturn(301);when(world.getTypes()).thenReturn(EnumSet.of(WorldType.MEMBERS));when(world.getActivity()).thenReturn("Pest Control");when(world.getAddress()).thenReturn("oldschool301.runescape.com");
    when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);when(client.getWorldList()).thenReturn(new World[]{world});when(client.getEnum(EnumID.WORLD_LOCATIONS)).thenReturn(locations);when(locations.getIntValue(301)).thenReturn(-42);
    field(plugin,"client",client);field(plugin,"running",true);Method sample=plugin.getClass().getDeclaredMethod("sample");sample.setAccessible(true);sample.invoke(plugin);
    Field known=plugin.getClass().getDeclaredField("knownWorlds");known.setAccessible(true);WorldEntry entry=(WorldEntry)((Map<?,?>)known.get(plugin)).get(301);
    assertEquals(-42,entry.serverLocation);field(plugin,"running",false);SwingUtilities.invokeAndWait(()->{});
  }
  @Test public void activityViewHasNoTooltipWhileCompactKeepsIt()throws Exception{
    SwingUtilities.invokeAndWait(()->{
      ClassicWorldPanel p=new ClassicWorldPanel(w->{},()->{});p.setSize(765,503);p.fonts(BitmapFontTest.fixture(),BitmapFontTest.fixture());p.worlds(Collections.singletonList(world(301,7,0)));
      hover(p);assertTrue(yellow(p)>0);p.options(WorldPreferences.DEFAULT.with("compact",false));hover(p);assertEquals(0,yellow(p));
    });
  }
  @Test public void worldInfoMenuPreferenceUpdatesTheVisibleButtonBeforeConfigEvents()throws Exception{
    ClassicWorldSelectorPlugin plugin=new ClassicWorldSelectorPlugin();ClassicWorldButton button=new ClassicWorldButton(()->{});ConfigManager manager=mock(ConfigManager.class);
    field(plugin,"button",button);field(plugin,"configManager",manager);button.worldInfo(7,EnumSet.of(WorldType.MEMBERS),"Pest Control");
    Method save=plugin.getClass().getDeclaredMethod("savePreference",String.class,Object.class);save.setAccessible(true);
    save.invoke(plugin,"buttonLine",WorldPreferences.ButtonLine.GAME_MODE);assertEquals("Pest Control",button.subtitle());
    verify(manager).setConfiguration(ClassicWorldSelectorConfig.GROUP,"buttonLine",WorldPreferences.ButtonLine.GAME_MODE);
    button.worldInfo(7,EnumSet.of(WorldType.MEMBERS,WorldType.HARDCORE_MAIN),"Pest Control");assertEquals("Pest Control",button.subtitle());
    button.worldInfo(0,EnumSet.of(WorldType.MEMBERS),"Soul Wars");assertEquals("Soul Wars",button.subtitle());
    button.worldInfo(0,EnumSet.of(WorldType.MEMBERS),"-");assertEquals("Mode: Main game",button.subtitle());
    save.invoke(plugin,"buttonLine",WorldPreferences.ButtonLine.CLICK_TO_SWITCH);assertEquals("Click to switch",button.subtitle());
  }
  @Test public void hoverSizeStepsAreModestAndButtonRemainsFixed(){
    assertEquals(1,DisplaySize.NORMAL.factor,0);assertEquals(1.15,DisplaySize.LARGER.factor,0);assertEquals(1.3,DisplaySize.EXTRA_LARGE.factor,0);
    assertEquals(new Dimension(143,50),new ClassicWorldButton(()->{}).getPreferredSize());
  }
  private static void hover(ClassicWorldPanel p){Rectangle r=p.worldLayout().tile(0);p.dispatchEvent(new MouseEvent(p,MouseEvent.MOUSE_MOVED,1,0,r.x+10,r.y+5,0,false));}
  private static int yellow(ClassicWorldPanel p){BufferedImage image=new BufferedImage(765,503,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();p.printAll(g);g.dispose();int count=0;for(int y=0;y<503;y++)for(int x=0;x<765;x++)if(image.getRGB(x,y)==0xffffffa0)count++;return count;}
  private static void field(Object instance,String name,Object value)throws Exception{Field f=instance.getClass().getDeclaredField(name);f.setAccessible(true);f.set(instance,value);}
}
