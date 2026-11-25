package tv.moonga.classicworlds;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class PixelRenderingTest {
  @Test public void normalPreservesOriginalDpiAndEnlargementIsExplicit(){
    BufferedImage glyph=new BufferedImage(3,2,BufferedImage.TYPE_INT_ARGB);
    glyph.setRGB(0,0,0xffffffff);glyph.setRGB(2,0,0xffffffff);glyph.setRGB(1,1,0xffffffff);
    for(double dpi:new double[]{1.25,1.5,1.75,2}){
      BufferedImage actual=new BufferedImage(30,30,BufferedImage.TYPE_INT_RGB);Graphics2D g=actual.createGraphics();g.translate(.25,.5);g.scale(dpi,dpi);
      PixelDrawing.DeviceGrid grid=new PixelDrawing.DeviceGrid(g);assertEquals(dpi,grid.scale,.00001);
      assertEquals(dpi*1.15,new PixelDrawing.DeviceGrid(g,DisplaySize.LARGER).scale,.00001);
      assertEquals(dpi*1.3,new PixelDrawing.DeviceGrid(g,DisplaySize.EXTRA_LARGE).scale,.00001);
      grid.draw(g,glyph,3,4);g.dispose();
      for(int y=0;y<30;y++)for(int x=0;x<30;x++){
        assertTrue("No smoothing colours at dpi="+dpi,actual.getRGB(x,y)==0xffffffff||actual.getRGB(x,y)==0xff000000);
      }
    }
  }
  @Test public void hoverRestoresOriginalSizeAndDoesNotFlickerOnWorldRefresh()throws Exception{
    SwingUtilities.invokeAndWait(()->{
      ClassicWorldPanel panel=new ClassicWorldPanel(w->{},()->{});panel.setSize(765,503);
      ClientBitmapFont font=BitmapFontTest.fixture();panel.fonts(font,font);
      java.util.List<WorldEntry> worlds=Collections.singletonList(ClassicWorldsTest.world(301,100));panel.worlds(worlds);
      Rectangle tile=panel.worldLayout().tile(0);
      panel.dispatchEvent(new MouseEvent(panel,MouseEvent.MOUSE_MOVED,1,0,tile.x+20,tile.y+5,0,false));
      BufferedImage first=render(panel,1.5);Rectangle yellow=null;
      for(int y=0;y<first.getHeight();y++)for(int x=0;x<first.getWidth();x++)if(first.getRGB(x,y)==0xffffffa0){
        if(yellow==null)yellow=new Rectangle(x,y,1,1);else yellow.add(new Rectangle(x,y,1,1));
      }
      // At fractional DPI, each one-pixel border occupies either one or two device pixels.
      assertNotNull(yellow);assertTrue(yellow.height>=18&&yellow.height<=19);
      assertTrue(Math.abs(yellow.width-(font.width("Members")+4)*1.5)<=1);
      panel.worlds(worlds);BufferedImage refreshed=render(panel,1.5);
      assertArrayEquals(first.getRGB(0,0,first.getWidth(),first.getHeight(),null,0,first.getWidth()),refreshed.getRGB(0,0,refreshed.getWidth(),refreshed.getHeight(),null,0,refreshed.getWidth()));
    });
  }
  @Test public void textAndTilesRemainNearestNeighbourPixelsAtWindowsDpiScales()throws Exception{
    SwingUtilities.invokeAndWait(()->{
      ClassicWorldPanel panel=new ClassicWorldPanel(w->{},()->{});panel.setSize(765,503);
      panel.fonts(BitmapFontTest.fixture(),BitmapFontTest.fixture());
      panel.worlds(Arrays.asList(ClassicWorldsTest.world(301,123),ClassicWorldsTest.world(302,456)));
      BufferedImage base=render(panel,1);
      for(double scale:new double[]{1.25,1.5,2}){
        BufferedImage expected=new BufferedImage((int)Math.ceil(765*scale),(int)Math.ceil(503*scale),BufferedImage.TYPE_INT_RGB);
        Graphics2D g=expected.createGraphics();g.scale(scale,scale);g.clipRect(0,0,765,503);PixelDrawing.blit(g,base,0,0,765,503);g.dispose();
        BufferedImage actual=render(panel,scale);
        assertArrayEquals("DPI scale "+scale,expected.getRGB(0,0,expected.getWidth(),expected.getHeight(),null,0,expected.getWidth()),actual.getRGB(0,0,actual.getWidth(),actual.getHeight(),null,0,actual.getWidth()));
      }
    });
  }
  @Test public void originalArrowBoundsAreAtTheCanvasEdgesAndHitTheirActualSprites()throws Exception{
    SwingUtilities.invokeAndWait(()->{
      ClassicWorldPanel p=new ClassicWorldPanel(w->{},()->{});p.setSize(1000,650);
      BufferedImage arrow=new BufferedImage(45,37,BufferedImage.TYPE_INT_ARGB);p.pageArrows(arrow,arrow);
      java.util.List<WorldEntry> worlds=new ArrayList<>();for(int i=0;i<300;i++)worlds.add(ClassicWorldsTest.world(301+i,100));p.worlds(worlds);
      Rectangle right=p.pageArrowBounds(false);assertEquals(-4,p.hit(new Point(right.x+10,right.y+10)));
      p.activate(-4);Rectangle left=p.pageArrowBounds(true);assertEquals(-3,p.hit(new Point(left.x+10,left.y+10)));
      assertEquals(new Point(8,650/2-37/2),new Point(left.x+(1000-765)/2,left.y));
    });
  }
  @Test public void paintedWorldButtonOpensOnlyOnACompletedClick()throws Exception{
    SwingUtilities.invokeAndWait(()->{
      AtomicInteger clicks=new AtomicInteger();ClassicWorldButton button=new ClassicWorldButton(clicks::incrementAndGet);button.setSize(ClassicWorldButton.WIDTH,ClassicWorldButton.HEIGHT);button.world(525);
      button.dispatchEvent(new MouseEvent(button,MouseEvent.MOUSE_PRESSED,1,0,20,20,1,false,MouseEvent.BUTTON1));
      assertEquals(0,clicks.get());button.dispatchEvent(new MouseEvent(button,MouseEvent.MOUSE_RELEASED,2,0,20,20,1,false,MouseEvent.BUTTON1));assertEquals(1,clicks.get());
      button.dispatchEvent(new MouseEvent(button,MouseEvent.MOUSE_PRESSED,3,0,20,20,1,false,MouseEvent.BUTTON3));
      button.dispatchEvent(new MouseEvent(button,MouseEvent.MOUSE_RELEASED,4,0,20,20,1,false,MouseEvent.BUTTON3));assertEquals(1,clicks.get());
    });
  }
  private static BufferedImage render(JComponent component,double scale){
    BufferedImage image=new BufferedImage((int)Math.ceil(component.getWidth()*scale),(int)Math.ceil(component.getHeight()*scale),BufferedImage.TYPE_INT_RGB);
    Graphics2D g=image.createGraphics();g.scale(scale,scale);component.paint(g);g.dispose();return image;
  }
}
