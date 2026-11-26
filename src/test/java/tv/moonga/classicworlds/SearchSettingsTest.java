package tv.moonga.classicworlds;

import java.awt.Point;
import java.awt.KeyboardFocusManager;
import java.awt.event.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.swing.SwingUtilities;
import net.runelite.api.*;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.OverlayMenuClicked;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SearchSettingsTest {
  private static WorldEntry world(int id,String activity,WorldType... special){EnumSet<WorldType> types=EnumSet.of(WorldType.MEMBERS);Collections.addAll(types,special);return new WorldEntry(id,100,0,activity,"oldschool"+id+".runescape.com",types);}
  private static final List<WorldEntry> DATA=Arrays.asList(world(525,"Pest Control"),world(669,"Pest Control",WorldType.HARDCORE_MAIN),world(302,"Soul Wars"));
  private static void set(Object target,String name,Object value)throws Exception{Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);field.set(target,value);}
  @SuppressWarnings("unchecked") private static List<Integer> ids(ClassicWorldPanel panel,String name){try{Field field=panel.getClass().getDeclaredField(name);field.setAccessible(true);return ((List<WorldEntry>)field.get(panel)).stream().map(w->w.id).collect(Collectors.toList());}catch(Exception e){throw new AssertionError(e);}}
  private static ClassicWorldPanel panel(AtomicInteger selected){ClassicWorldPanel p=new ClassicWorldPanel(w->selected.set(w.id),()->{});p.setSize(765,503);p.worlds(DATA);return p;}
  @Test public void searchIgnoresCaseSpacesAndPunctuationAndRetainsPreferences()throws Exception{
    SwingUtilities.invokeAndWait(()->{
      ClassicWorldPanel p=panel(new AtomicInteger());p.options(WorldPreferences.DEFAULT.with("preferredMode",WorldPreferences.GameMode.HARDCORE));
      for(String term:new String[]{"pestcontrol","Pest Control","PEST-CONTROL"}){p.query(term);assertEquals(Arrays.asList(669,525),ids(p,"worlds"));}
      p.query("World 525");assertEquals(Arrays.asList(525),ids(p,"worlds"));p.query("soulwars");assertEquals(Arrays.asList(302),ids(p,"worlds"));
      p.query("no matching activity");assertTrue(ids(p,"worlds").isEmpty());p.worlds(DATA);assertTrue(ids(p,"worlds").isEmpty());p.query("");assertEquals(3,ids(p,"worlds").size());
    });
  }
  @Test public void sidebarSearchHidesWithoutErasingFavouritesAndClearRestoresBothLists()throws Exception{
    SwingUtilities.invokeAndWait(()->{
      ClassicWorldPanel p=panel(new AtomicInteger());p.options(WorldPreferences.DEFAULT.with("favouriteWorlds","302,525").with("favouriteView",WorldPreferences.FavouriteView.SEPARATE_COLUMN));
      p.query("pestcontrol");assertEquals(Arrays.asList(525),ids(p,"favouriteWorlds"));assertEquals(Arrays.asList(669),ids(p,"worlds"));
      p.query("");assertEquals(Arrays.asList(302,525),ids(p,"favouriteWorlds"));assertEquals(Arrays.asList(669),ids(p,"worlds"));
    });
  }
  @Test public void editingConsumesSearchNavigationAndEnterNeverSelectsAWorld()throws Exception{
    SwingUtilities.invokeAndWait(()->{
      AtomicInteger selected=new AtomicInteger();ClassicWorldPanel p=panel(selected);p.activate(-105);
      for(char c:"pestcontrol".toCharArray())p.handleTyped(c);assertEquals(Arrays.asList(525,669),ids(p,"worlds"));
      p.handleKey(KeyEvent.VK_A,InputEvent.CTRL_DOWN_MASK);p.handleTyped('5');p.handleTyped('2');p.handleTyped('5');assertEquals("525",p.query());
      p.handleKey(KeyEvent.VK_LEFT);p.handleKey(KeyEvent.VK_BACK_SPACE);assertEquals("55",p.query());p.handleKey(KeyEvent.VK_HOME);p.handleKey(KeyEvent.VK_DELETE);assertEquals("5",p.query());
      p.handleKey(KeyEvent.VK_ENTER);p.handleTyped('x');assertEquals("5",p.query());assertEquals(0,selected.get());
      p.activate(-105);p.handleKey(KeyEvent.VK_ESCAPE);assertEquals("",p.query());p.query("x".repeat(100));assertEquals(64,p.query().length());p.activate(-106);assertEquals("",p.query());
    });
  }
  @Test public void gearHasItsOwnRightEdgeTargetAndDoesNotOpenOldOptionsMenu()throws Exception{
    SwingUtilities.invokeAndWait(()->{
      AtomicInteger opened=new AtomicInteger(),selected=new AtomicInteger();ClassicWorldPanel p=panel(selected);p.settingsRequested(opened::incrementAndGet);
      assertEquals(-105,p.hit(new Point(20,38)));assertEquals(-104,p.hit(new Point(745,38)));
      p.activate(-100);p.activate(p.hit(new Point(745,38)));assertEquals(1,opened.get());assertEquals(0,selected.get());
      WorldMenus menus=new WorldMenus((k,v)->{},opened::incrementAndGet);menus.activate(-104);assertFalse(menus.isOpen());assertEquals(2,opened.get());
    });
  }
  @Test public void settingsRequestUsesRuneLiteConfigActionAndKeepsSelectorOpen()throws Exception{
    ClassicWorldSelectorPlugin plugin=new ClassicWorldSelectorPlugin();Client client=mock(Client.class);EventBus events=mock(EventBus.class);
    when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);set(plugin,"client",client);set(plugin,"eventBus",events);set(plugin,"running",true);set(plugin,"opened",true);
    SwingUtilities.invokeAndWait(plugin::openSettings);
    ArgumentCaptor<Object> event=ArgumentCaptor.forClass(Object.class);verify(events).post(event.capture());OverlayMenuClicked clicked=(OverlayMenuClicked)event.getValue();
    assertEquals(MenuAction.RUNELITE_OVERLAY_CONFIG,clicked.getEntry().getMenuAction());assertSame(plugin,clicked.getOverlay().getPlugin());
    Field opened=plugin.getClass().getDeclaredField("opened");opened.setAccessible(true);assertTrue(opened.getBoolean(plugin));
    set(plugin,"opened",false);plugin.openSettings();verifyNoMoreInteractions(events);verify(client,never()).changeWorld(any());
  }
  @Test public void focusedSwingPanelReceivesRealKeyEventsWithoutGlobalSidebarBindings()throws Exception{
    SwingUtilities.invokeAndWait(()->{
      AtomicInteger selected=new AtomicInteger();ClassicWorldPanel p=panel(selected);p.activate(-105);
      KeyboardFocusManager focus=KeyboardFocusManager.getCurrentKeyboardFocusManager();
      for(char letter:"pestcontrol".toCharArray()){
        KeyEvent event=new KeyEvent(p,KeyEvent.KEY_TYPED,1,0,KeyEvent.VK_UNDEFINED,letter);focus.redispatchEvent(p,event);assertTrue(event.isConsumed());
      }
      assertEquals("pestcontrol",p.query());assertEquals(Arrays.asList(525,669),ids(p,"worlds"));
      KeyEvent backspace=new KeyEvent(p,KeyEvent.KEY_PRESSED,1,0,KeyEvent.VK_BACK_SPACE,'\b');focus.redispatchEvent(p,backspace);assertEquals("pestcontro",p.query());assertTrue(backspace.isConsumed());
      assertNull(p.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).get(javax.swing.KeyStroke.getKeyStroke("LEFT")));
      assertNull(p.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).get(javax.swing.KeyStroke.getKeyStroke("ENTER")));
      assertEquals(0,selected.get());
    });
  }

}
