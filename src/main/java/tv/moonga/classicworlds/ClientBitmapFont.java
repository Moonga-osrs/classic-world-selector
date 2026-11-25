package tv.moonga.classicworlds;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.*;
import net.runelite.api.SpritePixels;

/** The client's own glyph masks, offsets, advances and baseline, without TrueType substitution. */
final class ClientBitmapFont {
  static final class Glyph {
    final int width,height,x,y;
    final int[] mask;
    Glyph(int width,int height,int x,int y,int[] pixels){
      this.width=width;this.height=height;this.x=x;this.y=y;this.mask=pixels.clone();
    }
  }
  private final Glyph[] glyphs;
  private final int[] advances=new int[256];
  final int ascent;
  private final Map<Integer,BufferedImage[]> colours=new HashMap<>();
  ClientBitmapFont(byte[] metrics,Glyph[] glyphs){
    if(metrics==null||metrics.length!=257||glyphs==null||glyphs.length!=256)
      throw new IllegalArgumentException("Unsupported client bitmap font");
    for(int i=0;i<256;i++)advances[i]=metrics[i]&255;
    ascent=metrics[256]&255;this.glyphs=glyphs.clone();
  }
  static ClientBitmapFont from(byte[] metrics,SpritePixels[] sprites){
    if(metrics==null||sprites==null||sprites.length!=256)return null;
    Glyph[] glyphs=new Glyph[256];
    for(int i=0;i<256;i++){
      SpritePixels s=sprites[i];if(s==null)return null;
      glyphs[i]=new Glyph(s.getWidth(),s.getHeight(),s.getOffsetX(),s.getOffsetY(),s.getPixels());
    }
    return new ClientBitmapFont(metrics,glyphs);
  }
  int width(String text){int width=0;for(int i=0;i<text.length();i++)width+=advances[code(text.charAt(i))];return width;}
  void center(Graphics2D g,String text,int x,int baseline,int colour,int shadow){draw(g,text,x-width(text)/2,baseline,colour,shadow);}
  void draw(Graphics2D g,String text,int x,int baseline,int colour,int shadow){
    if(shadow>=0)draw(g,text,x+1,baseline+1,shadow,-1);
    BufferedImage[] images=colours.computeIfAbsent(colour,c->new BufferedImage[256]);
    for(int i=0;i<text.length();i++){
      int code=code(text.charAt(i));Glyph glyph=glyphs[code];
      if(code!=32&&glyph!=null&&glyph.width>0&&glyph.height>0){
        BufferedImage image=images[code];
        if(image==null){
          image=new BufferedImage(glyph.width,glyph.height,BufferedImage.TYPE_INT_ARGB);
          int[] pixels=new int[glyph.mask.length];for(int j=0;j<pixels.length;j++)if(glyph.mask[j]!=0)pixels[j]=0xff000000|(colour&0xffffff);
          image.setRGB(0,0,glyph.width,glyph.height,pixels,0,glyph.width);images[code]=image;
        }
        g.drawImage(image,x+glyph.x,baseline-ascent+glyph.y,null);
      }
      x+=advances[code];
    }
  }
  private static int code(char c){
    if(c==160)return 32;
    if(c>0&&c<128||c>=160&&c<=255)return c;
    String special="€\u0000‚ƒ„…†‡ˆ‰Š‹Œ\u0000Ž\u0000\u0000‘’“”•–—˜™š›œ\u0000žŸ";
    int at=special.indexOf(c);return at<0?63:128+at;
  }
}
