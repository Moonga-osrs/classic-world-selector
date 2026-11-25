package tv.moonga.classicworlds;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import net.runelite.api.GameState;
import net.runelite.api.WorldType;
import org.junit.Test;
import static org.junit.Assert.*;

public class ClassicWorldsTest {
  static WorldEntry world(int id,int players){return new WorldEntry(id,players,0,"-","oldschool"+id+".runescape.com",EnumSet.of(WorldType.MEMBERS));}

  @Test public void nativeGridFitsWithoutOverlappingTiles(){
    for(int count:new int[]{0,1,20,168,300,500}){
      ClassicLayout l=new ClassicLayout(count);List<Rectangle> seen=new ArrayList<>();
      assertTrue(l.rows>=1);assertTrue(l.columns>=1);
      for(int i=0;i<Math.min(count,l.rows*l.columns);i++){
        Rectangle r=l.tile(i);assertTrue(new Rectangle(0,23,765,480).contains(r));
        for(Rectangle previous:seen)assertFalse(previous.intersects(r));seen.add(r);
      }
    }
  }
  @Test public void paginationCanReachEveryWorldExactlyByColumn(){
    int count=420;ClassicLayout l=new ClassicLayout(count);Set<Integer> visible=new HashSet<>();
    for(int page=0;page<=l.maxPage;page++)for(int i=0;i<l.columns*l.rows&&l.first(page)+i<count;i++)visible.add(l.first(page)+i);
    assertEquals(count,visible.size());assertEquals(0,l.first(-4));assertEquals(l.maxPage*l.rows,l.first(10000));
  }
  @Test public void worldSwitchingIsRestrictedToLoggedOutState(){
    for(GameState state:GameState.values())assertEquals(state==GameState.LOGIN_SCREEN,ClassicWorldSelectorPlugin.canSelect(state));
    assertFalse(ClassicWorldSelectorPlugin.canSelect(null));
  }
  @Test public void offlineAndFullWorldsCannotBeSelected(){
    assertFalse(world(301,-1).selectable());assertEquals("OFF",world(301,-1).population());
    assertFalse(world(301,1981).selectable());assertEquals("FULL",world(301,1981).population());
    assertTrue(world(301,1980).selectable());
  }
  @Test public void gridClickAndSortingSelectTheDisplayedWorld()throws Exception{
    SwingUtilities.invokeAndWait(()->{
      AtomicInteger selected=new AtomicInteger();ClassicWorldPanel p=new ClassicWorldPanel(w->selected.set(w.id),()->{});p.setSize(765,503);
      p.worlds(Arrays.asList(world(302,50),world(301,100)));
      ClassicLayout l=p.worldLayout();Rectangle tile=l.tile(0);int hit=p.hit(new Point(tile.x+3,tile.y+3));
      p.activate(hit);assertEquals(301,selected.get());
      p.activate(-12);p.activate(p.hit(new Point(tile.x+3,tile.y+3)));assertEquals(302,selected.get());
      p.busy(true);p.activate(1);assertEquals(302,selected.get());
    });
  }
  @Test public void cancelWorksWhileBusyAndOutsideClicksDoNothing()throws Exception{
    SwingUtilities.invokeAndWait(()->{
      AtomicInteger closed=new AtomicInteger();ClassicWorldPanel p=new ClassicWorldPanel(w->fail(),closed::incrementAndGet);p.setSize(765,503);
      p.worlds(Collections.singletonList(world(301,-1)));p.activate(0);p.activate(-1);assertEquals(0,closed.get());
      p.busy(true);p.activate(p.hit(new Point(733,10)));assertEquals(1,closed.get());
    });
  }
  @Test public void riskLabelsRemainVisible(){
    WorldEntry pvp=new WorldEntry(318,500,0,"-","oldschool318.runescape.com",EnumSet.of(WorldType.PVP,WorldType.MEMBERS));
    assertTrue(pvp.tooltip().contains("PvP"));assertEquals(3,pvp.background());
  }
}
