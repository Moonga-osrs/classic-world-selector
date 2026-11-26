package tv.moonga.classicworlds;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.SwingUtilities;
import net.runelite.api.WorldType;
import net.runelite.client.config.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SidebarSettingsTest {
  @Test public void nativeDescriptorBuildsFourOrderedSectionsWithTheRequestedItems(){
    ConfigManager manager=mock(ConfigManager.class,CALLS_REAL_METHODS);
    ConfigDescriptor descriptor=manager.getConfigDescriptor(new ClassicWorldSelectorConfig(){});
    List<ConfigSectionDescriptor> groups=new ArrayList<>(descriptor.getSections());groups.sort(Comparator.comparingInt(g->g.getSection().position()));
    assertEquals(Arrays.asList("Button behaviour","World list","Favourite settings","Filters"),groups.stream().map(g->g.getSection().name()).collect(Collectors.toList()));
    List<List<String>> expected=Arrays.asList(Arrays.asList("Country flag","World info"),Arrays.asList("World hover text size","Bold"),Arrays.asList("Favourites","Layout"),Arrays.asList("Game mode","Region","Membership","Compact view"));
    for(int i=0;i<groups.size();i++){
      ConfigSectionDescriptor group=groups.get(i);assertFalse(group.getSection().closedByDefault());
      List<ConfigItemDescriptor> items=descriptor.getItems().stream().filter(item->!item.getItem().hidden()&&item.getItem().section().equals(group.getKey())).sorted(Comparator.comparingInt(item->item.getItem().position())).collect(Collectors.toList());
      assertEquals(expected.get(i),items.stream().map(item->item.getItem().name()).collect(Collectors.toList()));
    }
  }
  @Test public void worldInfoCheckboxPreservesTheLegacyPreferenceAndNewOverride(){
    ClassicWorldSelectorConfig legacy=new ClassicWorldSelectorConfig(){@Override public WorldPreferences.ButtonLine buttonLine(){return WorldPreferences.ButtonLine.GAME_MODE;}};
    assertTrue(legacy.worldInfo());assertEquals(WorldPreferences.ButtonLine.GAME_MODE,WorldPreferences.from(legacy).buttonLine);
    ClassicWorldSelectorConfig overridden=new ClassicWorldSelectorConfig(){@Override public WorldPreferences.ButtonLine buttonLine(){return WorldPreferences.ButtonLine.GAME_MODE;}@Override public boolean worldInfo(){return false;}};
    assertEquals(WorldPreferences.ButtonLine.CLICK_TO_SWITCH,WorldPreferences.from(overridden).buttonLine);
    assertEquals(WorldPreferences.ButtonLine.GAME_MODE,WorldPreferences.DEFAULT.with("worldInfo",true).buttonLine);
    assertEquals("On top",WorldPreferences.FavouriteView.WITH_FILTERS.toString());assertEquals("Separate",WorldPreferences.FavouriteView.SEPARATE_COLUMN.toString());
  }
  private static WorldEntry world(int id){return new WorldEntry(id,100,0,"Pest Control","oldschool"+id+".runescape.com",EnumSet.of(WorldType.MEMBERS));}
  @SuppressWarnings("unchecked") private static List<Integer> ids(ClassicWorldPanel panel,String key){try{Field field=panel.getClass().getDeclaredField(key);field.setAccessible(true);return ((List<WorldEntry>)field.get(panel)).stream().map(w->w.id).collect(Collectors.toList());}catch(Exception e){throw new AssertionError(e);}}
  @Test public void disablingFavouritesRestoresNormalGridAndPreservesSavedIdsWithoutToggling()throws Exception{
    SwingUtilities.invokeAndWait(()->{
      ClassicWorldPanel p=new ClassicWorldPanel(w->{},()->{});p.setSize(765,503);p.worlds(Arrays.asList(world(301),world(525),world(669)));
      WorldPreferences saved=WorldPreferences.DEFAULT.with("favouriteWorlds","669,525").with("favouriteView",WorldPreferences.FavouriteView.SEPARATE_COLUMN);
      p.options(saved);assertTrue(p.separateFavourites());assertEquals(Arrays.asList(669,525),ids(p,"favouriteWorlds"));
      WorldPreferences disabled=saved.with("favouritesEnabled",false);p.options(disabled);assertFalse(p.separateFavourites());assertEquals(Arrays.asList(301,525,669),ids(p,"worlds"));assertFalse(disabled.isFavourite(world(669)));assertEquals("669,525",disabled.toggleFavourite(301));
      p.preferencesChanged((k,v)->fail("Disabled favourites must not be changed by right-click"));Rectangle tile=p.worldLayout().tile(0);p.dispatchEvent(new MouseEvent(p,MouseEvent.MOUSE_RELEASED,1,0,tile.x+40,tile.y+8,1,false,MouseEvent.BUTTON3));
      p.options(disabled.with("favouritesEnabled",true));assertTrue(p.separateFavourites());assertEquals(Arrays.asList(669,525),ids(p,"favouriteWorlds"));
    });
  }
  private static BufferedImage image(ClassicWorldPanel panel){BufferedImage image=new BufferedImage(765,503,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();panel.printAll(g);g.dispose();return image;}
  private static int yellow(BufferedImage image){int count=0;for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if(image.getRGB(x,y)==0xffffffa0)count++;return count;}
  @Test public void boldChangesOnlyHoverFontAndNotWorldTiles()throws Exception{
    SwingUtilities.invokeAndWait(()->{
      ClassicWorldPanel p=new ClassicWorldPanel(w->{},()->{});p.setSize(765,503);p.worlds(Collections.singletonList(world(525)));
      byte[] metrics=new byte[257];Arrays.fill(metrics,(byte)6);metrics[256]=8;ClientBitmapFont.Glyph[] glyphs=new ClientBitmapFont.Glyph[256];int[] mask=new int[25];Arrays.fill(mask,1);for(int i=0;i<256;i++)glyphs[i]=new ClientBitmapFont.Glyph(5,5,0,1,mask);
      p.fonts(BitmapFontTest.fixture(),new ClientBitmapFont(metrics,glyphs));Rectangle tile=p.worldLayout().tile(0);p.dispatchEvent(new MouseEvent(p,MouseEvent.MOUSE_MOVED,1,0,tile.x+40,tile.y+5,0,false));BufferedImage normal=image(p);
      p.options(WorldPreferences.DEFAULT.with("boldHover",true));BufferedImage bold=image(p);assertTrue(yellow(bold)>yellow(normal));
      for(int y=tile.y;y<tile.y+tile.height;y++)for(int x=tile.x;x<tile.x+tile.width;x++)assertEquals(normal.getRGB(x,y),bold.getRGB(x,y));
      p.options(WorldPreferences.DEFAULT.with("boldHover",true).with("compact",false));assertEquals(0,yellow(image(p)));
    });
  }
}
