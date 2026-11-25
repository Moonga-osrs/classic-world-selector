package tv.moonga.classicworlds;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import java.util.EnumSet;
import net.runelite.api.WorldType;

/** Replaces the native login world button's entire hit area without a Swing button skin. */
final class ClassicWorldButton extends JComponent {
  // Preserve the classic 100:35 silhouette while covering the client's 120x50 control.
  static final int HEIGHT=50,WIDTH=(100*HEIGHT+34)/35;
  private final Runnable open;
  private final BufferedImage pixels=new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_INT_RGB);
  private BufferedImage background;
  private ClientBitmapFont smallFont,boldFont;
  private int world;
  private boolean pressed;
  private WorldPreferences options=WorldPreferences.DEFAULT;
  private BufferedImage[] flags;
  private int location=-1;
  private EnumSet<WorldType> types=EnumSet.noneOf(WorldType.class);
  private String activity="-";
  ClassicWorldButton(Runnable open){
    this.open=open;setOpaque(true);setFocusable(true);setDoubleBuffered(false);setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    addMouseListener(new MouseAdapter(){
      @Override public void mousePressed(MouseEvent e){pressed=SwingUtilities.isLeftMouseButton(e);}
      @Override public void mouseReleased(MouseEvent e){boolean activate=pressed&&SwingUtilities.isLeftMouseButton(e)&&contains(e.getPoint());pressed=false;if(activate)open.run();}
      @Override public void mouseExited(MouseEvent e){pressed=false;}
    });
    for(String key:new String[]{"SPACE","ENTER"}){
      getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key),"open");
    }
    getActionMap().put("open",new AbstractAction(){@Override public void actionPerformed(ActionEvent e){open.run();}});
  }
  void world(int id){if(world!=id){world=id;repaint();}}
  void background(BufferedImage image){background=image;repaint();}
  void fonts(ClientBitmapFont small,ClientBitmapFont bold){smallFont=small;boldFont=bold;repaint();}
  void flagSprites(BufferedImage[] images){flags=images;repaint();}
  void options(WorldPreferences value){options=value;repaint();}
  void worldInfo(int region,EnumSet<WorldType> worldTypes){
    worldInfo(region,worldTypes,"-");
  }
  void worldInfo(int region,EnumSet<WorldType> worldTypes,String worldActivity){
    EnumSet<WorldType> next=worldTypes==null?EnumSet.noneOf(WorldType.class):worldTypes;
    String nextActivity=worldActivity==null?"-":worldActivity;
    if(location!=region||!types.equals(next)||!activity.equals(nextActivity)){location=region;types=next.clone();activity=nextActivity;repaint();}
  }
  String subtitle(){
    if(options.buttonLine!=WorldPreferences.ButtonLine.GAME_MODE)return "Click to switch";
    WorldPreferences.GameMode mode=WorldPreferences.GameMode.of(types);
    return !activity.isBlank()&&!activity.equals("-")?activity:"Mode: "+mode;
  }
  private BufferedImage flag(){
    if(!options.showFlag||flags==null||location<0)return null;
    int index=location+(types.contains(WorldType.MEMBERS)?flags.length/2:0);
    return index<flags.length?flags[index]:null;
  }
  @Override public Dimension getPreferredSize(){return new Dimension(WIDTH,HEIGHT);}
  @Override protected void paintComponent(Graphics graphics){
    Graphics2D g=PixelDrawing.raster(pixels);
    try{
      g.setPaint(new GradientPaint(0,0,new Color(0x494b54),0,HEIGHT,new Color(0x202127)));g.fillRect(0,0,WIDTH,HEIGHT);
      if(background!=null)g.drawImage(background,0,0,WIDTH,HEIGHT,null);
      else{g.setColor(new Color(0x77766e));g.drawRect(1,1,WIDTH-3,HEIGHT-3);g.setColor(Color.BLACK);g.drawRect(0,0,WIDTH-1,HEIGHT-1);}
    }finally{g.dispose();}
    PixelDrawing.blit(graphics,pixels,0,0,getWidth(),getHeight());
    if(boldFont!=null&&smallFont!=null){
      String title="World "+world,subtitle=fit(subtitle(),WIDTH-12);BufferedImage flag=flag();
      int titleWidth=boldFont.width(title)+(flag==null?0:flag.getWidth()+4);
      int width=Math.max(titleWidth,smallFont.width(subtitle))+4;
      BufferedImage text=new BufferedImage(width,36,BufferedImage.TYPE_INT_ARGB);
      Graphics2D label=PixelDrawing.raster(text);
      try{
        int x=(width-titleWidth)/2;boldFont.draw(label,title,x,16,0xffffff,0);
        if(flag!=null)label.drawImage(flag,x+boldFont.width(title)+4,2,null);
        smallFont.center(label,subtitle,width/2,30,0xffffff,0);
      }finally{label.dispose();}
      new PixelDrawing.DeviceGrid(graphics).centered(graphics,text,getWidth()/2,getHeight()/2);
    }
  }
  private String fit(String text,int width){
    if(smallFont.width(text)<=width)return text;
    while(!text.isEmpty()&&smallFont.width(text+"...")>width)text=text.substring(0,text.length()-1);
    return text+"...";
  }
}
