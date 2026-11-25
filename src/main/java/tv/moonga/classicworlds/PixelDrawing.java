package tv.moonga.classicworlds;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.geom.*;

/** Rasterise game-style text before Windows DPI scaling, then preserve its pixel edges. */
final class PixelDrawing {
  static Graphics2D raster(BufferedImage image){
    Graphics2D g=image.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
    g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    return g;
  }
  static void blit(Graphics graphics,BufferedImage image,int x,int y,int width,int height){
    Graphics2D g=(Graphics2D)graphics.create();
    try{
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_OFF);
      g.drawImage(image,x,y,width,height,null);
    }finally{g.dispose();}
  }
  /** Preserve the original DPI scale; enlargement is an explicit user preference. */
  static final class DeviceGrid {
    final AffineTransform transform;
    final double scale;
    DeviceGrid(Graphics graphics){this(graphics,DisplaySize.NORMAL);}
    DeviceGrid(Graphics graphics,DisplaySize size){
      transform=((Graphics2D)graphics).getTransform();
      scale=Math.hypot(transform.getScaleX(),transform.getShearY())*size.factor;
    }
    Point point(double x,double y){
      Point2D p=transform.transform(new Point2D.Double(x,y),null);
      return new Point((int)Math.round(p.getX()),(int)Math.round(p.getY()));
    }
    void draw(Graphics graphics,BufferedImage image,int deviceX,int deviceY){
      Graphics2D g=(Graphics2D)graphics.create();
      try{
        Shape clip=g.getClip();Shape deviceClip=clip==null?null:transform.createTransformedShape(clip);
        g.setTransform(new AffineTransform());if(deviceClip!=null)g.setClip(deviceClip);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(image,deviceX,deviceY,(int)Math.round(image.getWidth()*scale),(int)Math.round(image.getHeight()*scale),null);
      }finally{g.dispose();}
    }
    void centered(Graphics graphics,BufferedImage image,double x,double y){
      Point center=point(x,y);draw(graphics,image,center.x-(int)Math.round(image.getWidth()*scale)/2,center.y-(int)Math.round(image.getHeight()*scale)/2);
    }
  }
}
