package tv.moonga.classicworlds;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import net.runelite.api.*;
import net.runelite.client.callback.ClientThread;
import org.junit.Test;

public class WorldSelectionTest {
  @Test public void clientSamplingNeverWaitsForWorldServiceHttp()throws Exception{
    ClassicWorldSelectorPlugin plugin=new ClassicWorldSelectorPlugin();Client client=mock(Client.class);
    net.runelite.client.game.WorldService service=mock(net.runelite.client.game.WorldService.class);
    field(plugin,"client",client);field(plugin,"worldService",service);field(plugin,"running",true);
    Method sample=plugin.getClass().getDeclaredMethod("sample");sample.setAccessible(true);sample.invoke(plugin);
    verifyNoInteractions(service);field(plugin,"running",false);SwingUtilities.invokeAndWait(()->{});
  }
  private static void field(Object target,String name,Object value)throws Exception{
    Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);
  }
  private static void select(ClassicWorldSelectorPlugin plugin,WorldEntry entry)throws Exception{
    Method m=plugin.getClass().getDeclaredMethod("select",WorldEntry.class);m.setAccessible(true);m.invoke(plugin,entry);
  }
  @Test public void selectionWaitsForClientThreadAndRechecksLoginState()throws Exception{
    ClassicWorldSelectorPlugin plugin=new ClassicWorldSelectorPlugin();Client client=mock(Client.class);ClientThread thread=mock(ClientThread.class);
    AtomicReference<Runnable> queued=new AtomicReference<>();doAnswer(i->{queued.set(i.getArgument(0));return null;}).when(thread).invokeLater(any(Runnable.class));
    field(plugin,"client",client);field(plugin,"clientThread",thread);field(plugin,"running",true);
    SwingUtilities.invokeAndWait(()->{try{field(plugin,"panel",new ClassicWorldPanel(w->{},()->{}));select(plugin,ClassicWorldsTest.world(301,100));}catch(Exception e){throw new RuntimeException(e);}});
    verify(client,never()).changeWorld(any());assertNotNull(queued.get());
    when(client.getGameState()).thenReturn(GameState.LOGGED_IN);queued.get().run();
    verify(client,never()).createWorld();verify(client,never()).changeWorld(any());
    field(plugin,"running",false);SwingUtilities.invokeAndWait(()->{});
  }
  @Test public void currentLoginSelectionCarriesWorldAddressAndTypes()throws Exception{
    ClassicWorldSelectorPlugin plugin=new ClassicWorldSelectorPlugin();Client client=mock(Client.class);ClientThread thread=mock(ClientThread.class);World world=mock(World.class);
    AtomicReference<Runnable> queued=new AtomicReference<>();doAnswer(i->{queued.set(i.getArgument(0));return null;}).when(thread).invokeLater(any(Runnable.class));
    field(plugin,"client",client);field(plugin,"clientThread",thread);field(plugin,"running",true);
    when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);when(client.createWorld()).thenReturn(world);
    SwingUtilities.invokeAndWait(()->{try{field(plugin,"panel",new ClassicWorldPanel(w->{},()->{}));select(plugin,ClassicWorldsTest.world(301,100));}catch(Exception e){throw new RuntimeException(e);}});
    queued.get().run();field(plugin,"running",false);SwingUtilities.invokeAndWait(()->{});
    verify(world).setId(301);verify(world).setAddress("oldschool301.runescape.com");
    verify(world).setTypes(EnumSet.of(WorldType.MEMBERS));verify(client).changeWorld(world);
  }
}
