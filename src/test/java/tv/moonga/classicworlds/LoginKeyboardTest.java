package tv.moonga.classicworlds;

import java.awt.Canvas;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LoginKeyboardTest {
  private static void set(Object target,String name,Object value)throws Exception{
    Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);field.set(target,value);
  }
  private static KeyListener listener(ClassicWorldSelectorPlugin plugin)throws Exception{
    Field field=plugin.getClass().getDeclaredField("selectorKeys");field.setAccessible(true);return (KeyListener)field.get(plugin);
  }
  @Test public void consumesInputOnlyForAnOpenLoginSelector()throws Exception{
    ClassicWorldSelectorPlugin plugin=new ClassicWorldSelectorPlugin();Client client=mock(Client.class);
    set(plugin,"client",client);set(plugin,"running",true);
    KeyListener keys=listener(plugin);assertTrue(keys.isEnabledOnLoginScreen());
    for(GameState state:new GameState[]{GameState.LOGIN_SCREEN,GameState.LOGGED_IN,GameState.LOGGING_IN}){
      when(client.getGameState()).thenReturn(state);
      for(boolean open:new boolean[]{false,true}){
        set(plugin,"opened",open);
        KeyEvent pressed=new KeyEvent(new Canvas(),KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_ENTER,'\n');
        KeyEvent released=new KeyEvent(new Canvas(),KeyEvent.KEY_RELEASED,0,0,KeyEvent.VK_ENTER,'\n');
        KeyEvent typed=new KeyEvent(new Canvas(),KeyEvent.KEY_TYPED,0,0,KeyEvent.VK_UNDEFINED,'\n');
        keys.keyPressed(pressed);keys.keyReleased(released);keys.keyTyped(typed);
        boolean expected=open&&state==GameState.LOGIN_SCREEN;
        assertEquals(expected,pressed.isConsumed());assertEquals(expected,released.isConsumed());assertEquals(expected,typed.isConsumed());
        SwingUtilities.invokeAndWait(()->{});
      }
    }
    when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);set(plugin,"running",false);
    KeyEvent event=new KeyEvent(new Canvas(),KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_ENTER,'\n');
    keys.keyPressed(event);assertFalse(event.isConsumed());
  }
  @Test public void shutdownUnregistersListenerAndDisablesQueuedInput()throws Exception{
    ClassicWorldSelectorPlugin plugin=new ClassicWorldSelectorPlugin();KeyManager manager=mock(KeyManager.class);
    set(plugin,"keyManager",manager);set(plugin,"running",true);set(plugin,"opened",true);
    plugin.shutDown();verify(manager).unregisterKeyListener(listener(plugin));
    KeyEvent event=new KeyEvent(new Canvas(),KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_ENTER,'\n');
    listener(plugin).keyPressed(event);assertFalse(event.isConsumed());SwingUtilities.invokeAndWait(()->{});
  }
}
