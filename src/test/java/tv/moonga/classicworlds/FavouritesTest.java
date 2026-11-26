package tv.moonga.classicworlds;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.swing.SwingUtilities;
import net.runelite.api.WorldType;
import org.junit.Test;
import static org.junit.Assert.*;

public class FavouritesTest {
  private static WorldEntry world(int id,boolean hardcore,int region,boolean member){
    EnumSet<WorldType> types=EnumSet.noneOf(WorldType.class);if(member)types.add(WorldType.MEMBERS);if(hardcore)types.add(WorldType.HARDCORE_MAIN);
    return new WorldEntry(id,100,region,"Pest Control","oldschool"+id+".runescape.com",types);
  }
  private static final List<WorldEntry> WORLDS=Arrays.asList(world(301,false,0,false),world(302,false,7,true),world(525,false,0,true),world(601,true,0,true),world(669,true,7,true));
  private static List<Integer> ids(List<WorldEntry> worlds){return worlds.stream().map(w->w.id).collect(Collectors.toList());}
  private static WorldPreferences prefs(){return WorldPreferences.DEFAULT.with("favouriteWorlds","525,669");}
  @Test public void favouritesRiseWithinModeGroupsWithoutOutrankingFilters(){
    WorldPreferences p=prefs().with("preferredMode",WorldPreferences.GameMode.HARDCORE);
    assertEquals(Arrays.asList(669,601,525,301,302),ids(p.order(WORLDS,Comparator.comparingInt(w->w.id))));
    assertEquals(Arrays.asList(669,601,525,302,301),ids(p.order(WORLDS,Comparator.comparingInt((WorldEntry w)->w.id).reversed())));
    assertEquals(Arrays.asList(525,669,301,302,601),ids(prefs().order(WORLDS,Comparator.comparingInt(w->w.id))));
    assertEquals(Arrays.asList(301,302,525,601,669),ids(WORLDS));
  }
  @Test public void regionAndMembershipPreferencesRemainStrongerThanFavourites(){
    WorldPreferences p=prefs().with("preferredRegion",WorldPreferences.Region.GERMANY);
    assertEquals(Arrays.asList(669,302,525,301,601),ids(p.order(WORLDS,Comparator.comparingInt(w->w.id))));
    p=prefs().with("membership",WorldPreferences.Membership.F2P_FIRST);
    assertEquals(301,p.order(WORLDS,Comparator.comparingInt(w->w.id)).get(0).id);
    assertEquals(Arrays.asList(301),ids(prefs().with("membership",WorldPreferences.Membership.F2P_ONLY).order(WORLDS,Comparator.comparingInt(w->w.id))));
  }
  @Test public void savedIdsAreDeduplicatedValidatedAndRoundTrip(){
    assertEquals(Arrays.asList(525,669),WorldPreferences.parseFavourites("525,669,525,garbage,0,-1,9999999999"));
    assertEquals("669",prefs().toggleFavourite(525));assertEquals("525,669,301",prefs().toggleFavourite(301));
    ClassicWorldSelectorConfig config=new ClassicWorldSelectorConfig(){@Override public String favouriteWorlds(){return "669,525";}@Override public WorldPreferences.FavouriteView favouriteView(){return WorldPreferences.FavouriteView.SEPARATE_COLUMN;}};
    WorldPreferences restored=WorldPreferences.from(config);assertEquals(Arrays.asList(669,525),restored.favourites);assertEquals(WorldPreferences.FavouriteView.SEPARATE_COLUMN,restored.favouriteView);
    assertEquals(WorldPreferences.FavouriteView.WITH_FILTERS,new ClassicWorldSelectorConfig(){}.favouriteView());
  }
  private static void event(ClassicWorldPanel p,int kind,int button,Point point){
    p.dispatchEvent(new MouseEvent(p,kind,System.currentTimeMillis(),kind==MouseEvent.MOUSE_DRAGGED?InputEvent.BUTTON1_DOWN_MASK:0,point.x,point.y,1,false,button));
  }
  private static void click(ClassicWorldPanel p,Rectangle tile,int button){Point point=new Point(tile.x+40,tile.y+8);event(p,MouseEvent.MOUSE_PRESSED,button,point);event(p,MouseEvent.MOUSE_RELEASED,button,point);}
  @SuppressWarnings("unchecked") private static List<WorldEntry> entries(ClassicWorldPanel p,String name){try{Field f=p.getClass().getDeclaredField(name);f.setAccessible(true);return (List<WorldEntry>)f.get(p);}catch(Exception e){throw new AssertionError(e);}}
  private static ClassicWorldPanel panel(AtomicInteger selected){ClassicWorldPanel p=new ClassicWorldPanel(w->selected.set(w.id),()->{});p.setSize(765,503);p.worlds(WORLDS);return p;}
  @Test public void rightClickTogglesAndPersistsWithoutSelectingAndMenusBlockIt()throws Exception{
    SwingUtilities.invokeAndWait(()->{
      AtomicInteger selected=new AtomicInteger();AtomicReference<String> saved=new AtomicReference<>();ClassicWorldPanel p=panel(selected);
      p.preferencesChanged((key,value)->{assertEquals("favouriteWorlds",key);saved.set((String)value);});
      click(p,p.worldLayout().tile(2),MouseEvent.BUTTON3);assertEquals("525",saved.get());assertEquals(0,selected.get());assertEquals(525,entries(p,"worlds").get(0).id);
      click(p,p.worldLayout().tile(0),MouseEvent.BUTTON3);assertEquals("",saved.get());assertEquals(0,selected.get());
      p.activate(-100);saved.set(null);click(p,p.worldLayout().tile(0),MouseEvent.BUTTON3);assertNull(saved.get());assertEquals(0,selected.get());
    });
  }
  @Test public void separateColumnKeepsSavedOrderAndAvoidsDuplicatesAndRespectsOnlyFilter()throws Exception{
    SwingUtilities.invokeAndWait(()->{
      AtomicInteger selected=new AtomicInteger();ClassicWorldPanel p=panel(selected);
      p.options(prefs().with("favouriteView",WorldPreferences.FavouriteView.SEPARATE_COLUMN));
      assertEquals(Arrays.asList(525,669),ids(entries(p,"favouriteWorlds")));assertEquals(Arrays.asList(301,302,601),ids(entries(p,"worlds")));
      click(p,p.favouriteTile(1),MouseEvent.BUTTON1);assertEquals(669,selected.get());
      p.options(prefs().with("favouriteView",WorldPreferences.FavouriteView.SEPARATE_COLUMN).with("membership",WorldPreferences.Membership.F2P_ONLY));
      assertTrue(entries(p,"favouriteWorlds").isEmpty());assertEquals(Arrays.asList(301),ids(entries(p,"worlds")));
      p.options(prefs());assertEquals(5,entries(p,"worlds").size());
    });
  }
  @Test public void handleDragReordersPersistsAndNeverSelectsOrSwitchesOnOutsideDrop()throws Exception{
    SwingUtilities.invokeAndWait(()->{
      AtomicInteger selected=new AtomicInteger();AtomicReference<String> saved=new AtomicReference<>();ClassicWorldPanel p=panel(selected);
      p.options(prefs().with("favouriteView",WorldPreferences.FavouriteView.SEPARATE_COLUMN));p.preferencesChanged((k,v)->saved.set((String)v));
      Point from=new Point(8,p.favouriteTile(0).y+8),to=new Point(8,p.favouriteTile(1).y+8);
      event(p,MouseEvent.MOUSE_PRESSED,MouseEvent.BUTTON1,from);event(p,MouseEvent.MOUSE_DRAGGED,MouseEvent.NOBUTTON,to);event(p,MouseEvent.MOUSE_RELEASED,MouseEvent.BUTTON1,to);
      assertEquals("669,525",saved.get());assertEquals(Arrays.asList(669,525),ids(entries(p,"favouriteWorlds")));assertEquals(0,selected.get());
      saved.set(null);event(p,MouseEvent.MOUSE_PRESSED,MouseEvent.BUTTON1,from);Point outside=new Point(400,400);event(p,MouseEvent.MOUSE_DRAGGED,MouseEvent.NOBUTTON,outside);event(p,MouseEvent.MOUSE_RELEASED,MouseEvent.BUTTON1,outside);
      assertNull(saved.get());assertEquals(0,selected.get());
      event(p,MouseEvent.MOUSE_PRESSED,MouseEvent.BUTTON1,from);event(p,MouseEvent.MOUSE_DRAGGED,MouseEvent.NOBUTTON,to);p.handleKey(KeyEvent.VK_ESCAPE);event(p,MouseEvent.MOUSE_RELEASED,MouseEvent.BUTTON1,to);
      assertNull(saved.get());assertEquals(Arrays.asList(669,525),ids(entries(p,"favouriteWorlds")));assertEquals(0,selected.get());
    });
  }
  @Test public void sidebarScrollingAndPaginationReachEveryWorldWithoutOverlapping()throws Exception{
    SwingUtilities.invokeAndWait(()->{
      for(boolean compact:new boolean[]{true,false}){
        ClassicWorldPanel p=panel(new AtomicInteger());List<WorldEntry> data=new ArrayList<>();List<Integer> favs=new ArrayList<>();for(int i=301;i<701;i++){data.add(world(i,false,0,true));if(i<331)favs.add(i);}
        p.worlds(data);p.options(WorldPreferences.DEFAULT.with("favouriteView",WorldPreferences.FavouriteView.SEPARATE_COLUMN).with("favouriteWorlds",WorldPreferences.serialise(favs)).with("compact",compact));
        ClassicLayout layout=p.worldLayout();Set<Integer> reached=new HashSet<>();for(int page=0;page<=layout.maxPage;page++)for(int i=0;i<layout.columns*layout.rows&&layout.first(page)+i<370;i++)reached.add(layout.first(page)+i);assertEquals(370,reached.size());
        for(int i=0;i<layout.columns*layout.rows;i++){Rectangle tile=layout.tile(i);assertTrue(new Rectangle(110,77,655,426).contains(tile));for(int j=0;j<21;j++)assertFalse(tile.intersects(p.favouriteTile(j)));assertFalse(tile.intersects(p.pageArrowBounds(true)));assertFalse(tile.intersects(p.pageArrowBounds(false)));}
        p.dispatchEvent(new MouseWheelEvent(p,MouseEvent.MOUSE_WHEEL,1,0,40,100,0,false,MouseWheelEvent.WHEEL_UNIT_SCROLL,1,30));
        click(p,p.favouriteTile(20),MouseEvent.BUTTON3);assertEquals(29,entries(p,"favouriteWorlds").size());assertFalse(ids(entries(p,"favouriteWorlds")).contains(322));
      }
    });
  }
}
