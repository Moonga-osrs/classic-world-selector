package tv.moonga.classicworlds;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.events.ConfigChanged;
import org.junit.Test;

public class DisplaySizeConfigTest {
  @Test public void originalSizeIsTheDefaultAndNamesMatchTheDropdown(){
    assertEquals(DisplaySize.NORMAL,new ClassicWorldSelectorConfig(){}.displaySize());
    assertEquals("Normal",DisplaySize.NORMAL.toString());assertEquals("Large",DisplaySize.LARGER.toString());assertEquals("Extra large",DisplaySize.EXTRA_LARGE.toString());
  }
  @Test public void hoverSizeChangesNeverResizeTheLoginButton()throws Exception{
    AtomicReference<DisplaySize> selected=new AtomicReference<>(DisplaySize.NORMAL);
    ClassicWorldSelectorPlugin plugin=new ClassicWorldSelectorPlugin();Client client=mock(Client.class);
    Canvas canvas=new Canvas();canvas.setSize(765,503);JRootPane root=new JRootPane();root.getContentPane().add(canvas);
    when(client.getCanvas()).thenReturn(canvas);when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
    ClassicWorldButton button=new ClassicWorldButton(()->{});ClassicWorldPanel panel=new ClassicWorldPanel(w->{},()->{});
    set(plugin,"client",client);set(plugin,"panel",panel);set(plugin,"button",button);set(plugin,"running",true);
    set(plugin,"config",new ClassicWorldSelectorConfig(){@Override public DisplaySize displaySize(){return selected.get();}});
    ConfigChanged event=new ConfigChanged();event.setGroup(ClassicWorldSelectorConfig.GROUP);event.setKey("displaySize");
    for(DisplaySize size:DisplaySize.values()){
      selected.set(size);plugin.onConfigChanged(event);SwingUtilities.invokeAndWait(()->{});
      assertEquals(ClassicWorldButton.WIDTH,button.getWidth());
      assertEquals(ClassicWorldButton.HEIGHT,button.getHeight());
      assertEquals(498,button.getY()+button.getHeight());
    }
    selected.set(DisplaySize.NORMAL);plugin.onConfigChanged(event);SwingUtilities.invokeAndWait(()->{});
    assertEquals(ClassicWorldButton.HEIGHT,button.getHeight());
    set(plugin,"running",false);
  }
  private static void set(Object instance,String name,Object value)throws Exception{Field field=instance.getClass().getDeclaredField(name);field.setAccessible(true);field.set(instance,value);}
}
