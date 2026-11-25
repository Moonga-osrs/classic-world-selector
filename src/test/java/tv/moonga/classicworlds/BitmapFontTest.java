package tv.moonga.classicworlds;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class BitmapFontTest {
  static ClientBitmapFont fixture(){
    byte[] metrics=new byte[257];Arrays.fill(metrics,(byte)4);metrics[256]=6;
    ClientBitmapFont.Glyph[] glyphs=new ClientBitmapFont.Glyph[256];
    for(int i=0;i<256;i++)glyphs[i]=new ClientBitmapFont.Glyph(3,3,1,2,new int[]{1,0,1,1,1,1,1,0,1});
    return new ClientBitmapFont(metrics,glyphs);
  }
  @Test public void glyphPixelsUseNativeOffsetsAndBaselineExactly(){
    ClientBitmapFont font=fixture();BufferedImage image=new BufferedImage(20,20,BufferedImage.TYPE_INT_ARGB);
    Graphics2D g=image.createGraphics();font.draw(g,"A",3,10,0xffffff,-1);g.dispose();
    assertEquals(0xffffffff,image.getRGB(4,6));assertEquals(0,image.getRGB(5,6));assertEquals(0xffffffff,image.getRGB(6,6));
    assertEquals(0xffffffff,image.getRGB(5,7));assertEquals(0,image.getRGB(4,5));assertEquals(0,image.getRGB(4,9));
    int lit=0;for(int y=0;y<20;y++)for(int x=0;x<20;x++)if(image.getRGB(x,y)!=0)lit++;
    assertEquals(7,lit);
  }
  @Test public void centeringAndSpaceUseAdvanceMetricsRatherThanAwtFontMeasurements(){
    ClientBitmapFont font=fixture();assertEquals(12,font.width("A A"));assertEquals(font.width("A A"),font.width("A\u00a0A"));
    BufferedImage image=new BufferedImage(20,20,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();font.center(g,"AA",10,10,0xffffff,-1);g.dispose();
    assertEquals(0xffffffff,image.getRGB(7,6));assertEquals(0xffffffff,image.getRGB(11,6));
  }
  @Test public void nativeBlackShadowUsesOnePixelOffset(){
    BufferedImage image=new BufferedImage(20,20,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();fixture().draw(g,"A",3,10,0xffff00,0);g.dispose();
    assertEquals(0xffffff00,image.getRGB(4,6));assertEquals(0xff000000,image.getRGB(7,9));
  }
  @Test(expected=IllegalArgumentException.class) public void unsupportedMetricsAreRejected(){new ClientBitmapFont(new byte[3],new ClientBitmapFont.Glyph[256]);}
}
