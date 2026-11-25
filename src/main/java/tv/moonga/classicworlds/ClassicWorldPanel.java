package tv.moonga.classicworlds;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import javax.swing.*;

/** A single opaque surface stays above the game canvas and other login overlays. */
final class ClassicWorldPanel extends JPanel {
  private final Consumer<WorldEntry> select;
  private final Runnable close;
  private List<WorldEntry> worlds=Collections.emptyList();
  private List<WorldEntry> allWorlds=Collections.emptyList();
  private WorldPreferences options=WorldPreferences.DEFAULT;
  private BiConsumer<String,Object> settingsChanged=(key,value)->{};
  private final WorldMenus menus=new WorldMenus(this::preferenceChanged);
  private BufferedImage[] backgrounds,flags,stars,arrows;
  private BufferedImage leftArrow,rightArrow;
  private ClientBitmapFont smallFont,boldFont;
  private DisplaySize displaySize=DisplaySize.NORMAL;
  private final BufferedImage pixels=new BufferedImage(765,503,BufferedImage.TYPE_INT_RGB);
  private int page,sort,hover=-1;
  private boolean ascending=true, busy;
  private Point mouse=new Point(-1,-1);
  private String message="Loading worlds...";
  private static final int[] HEADINGS={280,390,500,610};

  ClassicWorldPanel(Consumer<WorldEntry> select,Runnable close) {
    this.select=select;this.close=close;setOpaque(true);setBackground(Color.BLACK);setFocusable(true);setDoubleBuffered(false);
    MouseAdapter input=new MouseAdapter(){
      private int pressed=-1;
      @Override public void mouseMoved(MouseEvent e){mouse=logical(e.getPoint());menus.mouse(mouse);hover=hit(mouse);repaint();}
      @Override public void mouseExited(MouseEvent e){hover=-1;mouse=new Point(-1,-1);menus.mouse(mouse);repaint();}
      @Override public void mousePressed(MouseEvent e){if(SwingUtilities.isLeftMouseButton(e))pressed=hit(logical(e.getPoint()));}
      @Override public void mouseReleased(MouseEvent e){
        if(!SwingUtilities.isLeftMouseButton(e))return;
        int target=hit(logical(e.getPoint()));
        if(target==pressed)activate(target);
        pressed=-1;
      }
      @Override public void mouseWheelMoved(MouseWheelEvent e){if(menus.isOpen())menus.step(Integer.signum(e.getWheelRotation()));else turn(Integer.signum(e.getWheelRotation()));repaint();e.consume();}
    };
    addMouseListener(input);addMouseMotionListener(input);addMouseWheelListener(input);
    bind("ESCAPE",()->handleKey(KeyEvent.VK_ESCAPE));bind("LEFT",()->handleKey(KeyEvent.VK_LEFT));bind("RIGHT",()->handleKey(KeyEvent.VK_RIGHT));
    bind("UP",()->handleKey(KeyEvent.VK_UP));bind("DOWN",()->handleKey(KeyEvent.VK_DOWN));bind("ENTER",()->handleKey(KeyEvent.VK_ENTER));
  }
  void handleKey(int key){
    switch(key){
      case KeyEvent.VK_ESCAPE:if(!menus.dismiss())close.run();break;
      case KeyEvent.VK_LEFT:if(!menus.isOpen())turn(-1);break;
      case KeyEvent.VK_RIGHT:if(!menus.isOpen())turn(1);break;
      case KeyEvent.VK_UP:menus.step(-1);break;
      case KeyEvent.VK_DOWN:menus.step(1);break;
      case KeyEvent.VK_ENTER:menus.choose();break;
      default:return;
    }
    repaint();
  }
  private void bind(String key,Runnable callback){
    getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key),key);
    getActionMap().put(key,new AbstractAction(){
      @Override public boolean isEnabled(){return isVisible();}
      @Override public void actionPerformed(ActionEvent e){callback.run();}
    });
  }
  void assets(BufferedImage[] backgrounds,BufferedImage[] flags,BufferedImage[] stars,BufferedImage[] arrows){
    this.backgrounds=backgrounds;this.flags=flags;this.stars=stars;this.arrows=arrows;repaint();
  }
  void pageArrows(BufferedImage left,BufferedImage right){leftArrow=left;rightArrow=right;repaint();}
  void fonts(ClientBitmapFont small,ClientBitmapFont bold){smallFont=small;boldFont=bold;repaint();}
  void displaySize(DisplaySize size){displaySize=size==null?DisplaySize.NORMAL:size;menus.settings(options,displaySize);repaint();}
  void preferencesChanged(BiConsumer<String,Object> callback){settingsChanged=callback;}
  void options(WorldPreferences value){
    boolean reorder=value.mode!=options.mode||value.region!=options.region||value.membership!=options.membership||value.compact!=options.compact;
    options=value;menus.settings(options,displaySize);if(reorder){page=0;sortWorlds();}repaint();
  }
  private void preferenceChanged(String key,Object value){
    if(key.equals("displaySize"))displaySize((DisplaySize)value);else options(options.with(key,value));
    settingsChanged.accept(key,value);hover=-1;repaint();
  }
  void worlds(List<WorldEntry> values){allWorlds=new ArrayList<>(values);sortWorlds();hover=hit(mouse);message=allWorlds.isEmpty()?"Loading worlds...":worlds.isEmpty()?"No worlds match this membership filter.":"";repaint();}
  ClassicLayout worldLayout(){return new ClassicLayout(worlds.size(),options.compact,57);}
  void status(String text){message=text;repaint();}
  void busy(boolean value){busy=value;repaint();}
  private void sortWorlds(){
    Comparator<WorldEntry> comparator;
    switch(sort){
      case 1: comparator=Comparator.comparingInt(w->w.players<0?2001:w.players);break;
      case 2: comparator=Comparator.comparingInt(w->w.location);break;
      case 3: comparator=options.compact?Comparator.comparing(WorldEntry::members).thenComparing(WorldEntry::typeLabel):Comparator.comparing(w->w.activity);break;
      default: comparator=Comparator.comparingInt(w->w.id);
    }
    if(!ascending)comparator=comparator.reversed();
    worlds=options.order(allWorlds,comparator.thenComparingInt(w->w.id));
    message=allWorlds.isEmpty()?"Loading worlds...":worlds.isEmpty()?"No worlds match this membership filter.":"";
    page=Math.min(page,worldLayout().maxPage);hover=-1;
  }
  private void turn(int direction){page=Math.max(0,Math.min(worldLayout().maxPage,page+direction));hover=-1;repaint();}
  private double scale(){return Math.min(1,Math.min(getWidth()/765.0,getHeight()/503.0));}
  private int originX(){return (getWidth()-(int)(765*scale()))/2;}
  Point logical(Point point){double s=scale();return new Point((int)((point.x-originX())/s),(int)(point.y/s));}
  int hit(Point p){
    Integer menu=menus.hit(p);if(menu!=null)return menu;
    if(new Rectangle(708,4,50,16).contains(p))return -2;
    for(int i=0;i<HEADINGS.length;i++){
      if(new Rectangle(HEADINGS[i],0,15,23).contains(p))return -10-i*2;
      if(new Rectangle(HEADINGS[i]+15,0,95,23).contains(p))return -11-i*2;
    }
    ClassicLayout layout=worldLayout();
    if(page>0&&pageArrowBounds(true).contains(p))return -3;
    if(page<layout.maxPage&&pageArrowBounds(false).contains(p))return -4;
    int first=layout.first(page);
    for(int i=0;i<layout.columns*layout.rows&&first+i<worlds.size();i++)
      if(layout.tile(i).contains(p))return first+i;
    return -1;
  }
  void activate(int target){
    if(target==-2){close.run();return;}
    if(busy)return;
    if(menus.activate(target)){hover=-1;repaint();return;}
    if(target==-3){turn(-1);return;}if(target==-4){turn(1);return;}
    if(target<=-10&&target>=-17){int action=-target-10;sort=action/2;ascending=action%2==0;page=0;sortWorlds();repaint();return;}
    if(target>=0&&target<worlds.size()&&worlds.get(target).selectable())select.accept(worlds.get(target));
  }
  @Override protected void paintComponent(Graphics graphics){
    super.paintComponent(graphics);
    Graphics2D g=PixelDrawing.raster(pixels);
    try{
      g.setColor(Color.BLACK);g.fillRect(0,0,765,503);
      g.setPaint(new GradientPaint(0,0,new Color(12425273),0,23,new Color(9135624)));g.fillRect(0,0,125,23);
      g.setPaint(new GradientPaint(0,0,new Color(0x4f4f4f),0,23,new Color(0x292929)));g.fillRect(125,0,640,23);
      center(g,boldFont,"Select a world",62,15,Color.BLACK);
      sprite(g,stars,1,140,1);sprite(g,stars,0,140,12);
      text(g,smallFont,"Members only world",152,10,Color.WHITE);text(g,smallFont,"Free world",152,21,Color.WHITE);
      String[] labels={"World","Players","Location",options.compact?"Type":"Activity"};
      for(int i=0;i<labels.length;i++){
        arrow(g,HEADINGS[i],4,true,sort==i&&ascending);
        arrow(g,HEADINGS[i]+15,4,false,sort==i&&!ascending);
        text(g,boldFont,labels[i],HEADINGS[i]+32,17,Color.WHITE);
      }
      g.setColor(Color.BLACK);g.fillRect(708,4,50,16);center(g,smallFont,"Cancel",733,16,Color.WHITE);
      ClassicLayout layout=worldLayout();int first=layout.first(page);
      for(int i=0;i<layout.columns*layout.rows&&first+i<worlds.size();i++){
        WorldEntry w=worlds.get(first+i);Rectangle r=layout.tile(i);
        boolean nativeBackground=sprite(g,backgrounds,w.background(),r.x,r.y);
        if(!nativeBackground){
          Color top=w.members()?new Color(0xb8a670):new Color(0xa6a6a6);
          g.setPaint(new GradientPaint(0,r.y,top,0,r.y+19,new Color(0x545454)));g.fillRect(r.x,r.y,88,19);
        }
        if(hover==first+i&&w.selectable()&&!busy){g.setColor(new Color(255,255,255,128));g.fillRect(r.x,r.y,88,19);}
        // The original renderer used half the flag sheet, not a fixed eight-region offset.
        sprite(g,flags,w.location+(w.members()&&flags!=null?flags.length/2:0),r.x+29,r.y);
        center(g,boldFont,Integer.toString(w.id),r.x+15,r.y+14,w.types.contains(net.runelite.api.WorldType.DEADMAN)?Color.RED:Color.BLACK);
        center(g,smallFont,w.population(),r.x+60,r.y+14,Color.WHITE);
        if(!options.compact){
          if(hover==first+i){g.setColor(new Color(192,169,70,65));g.fillRect(r.x+90,r.y,r.width-90,19);}
          Shape clip=g.getClip();g.clipRect(r.x+93,r.y,r.width-95,r.height);
          text(g,smallFont,w.activity.equals("-")?WorldPreferences.GameMode.of(w.types).toString():w.activity,r.x+94,r.y+14,Color.WHITE);g.setClip(clip);
        }
      }
      if(worlds.isEmpty()||!message.isEmpty())center(g,boldFont,message,382,worlds.isEmpty()?253:498,Color.YELLOW);
      menus.paint(g,smallFont);
    }finally{g.dispose();}
    PixelDrawing.blit(graphics,pixels,originX(),0,(int)(765*scale()),(int)(503*scale()));
    ClassicLayout layout=worldLayout();
    if(page>0)paintPageArrow(graphics,true);
    if(page<layout.maxPage)paintPageArrow(graphics,false);
    paintTooltip(graphics);
  }
  private void paintTooltip(Graphics graphics){
    if(!options.compact||menus.isOpen()||hover<0||hover>=worlds.size()||mouse.y<57||smallFont==null)return;
    PixelDrawing.DeviceGrid grid=new PixelDrawing.DeviceGrid(graphics,displaySize);
    Point topLeft=grid.point(0,0),bottomRight=grid.point(getWidth(),getHeight());
    int available=Math.max(8,(int)((bottomRight.x-topLeft.x)/grid.scale)-4);
    String text=worlds.get(hover).tooltip();int width=Math.min(available,smallFont.width(text)+6);
    int height=smallFont.ascent+8;
    BufferedImage tooltip=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);Graphics2D g=PixelDrawing.raster(tooltip);
    try{
      g.setColor(new Color(0xffffa0));g.fillRect(0,0,width,height);g.setColor(Color.BLACK);g.drawRect(0,0,width-1,height-1);
      g.clipRect(2,1,width-4,height-2);smallFont.center(g,text,width/2,smallFont.ascent+4,0,-1);
    }finally{g.dispose();}
    Point cursor=grid.point(originX()+mouse.x*scale(),mouse.y*scale());
    int deviceWidth=(int)Math.round(width*grid.scale),deviceHeight=(int)Math.round(height*grid.scale);
    int x=Math.max(topLeft.x,Math.min(bottomRight.x-deviceWidth,cursor.x-deviceWidth/2));
    int y=cursor.y+(int)Math.round(25*grid.scale);
    if(y+deviceHeight>bottomRight.y)y=cursor.y-(int)Math.round(25*grid.scale)-deviceHeight;
    grid.draw(graphics,tooltip,x,Math.max(topLeft.y,y));
  }
  private void arrow(Graphics2D g,int x,int y,boolean up,boolean selected){
    if(sprite(g,arrows,up?(selected?2:0):(selected?3:1),x,y))return;
    g.setColor(selected?Color.YELLOW:Color.GRAY);
    g.fillPolygon(new int[]{x,x+7,x+14},up?new int[]{y+13,y,y+13}:new int[]{y,y+13,y},3);
  }
  Rectangle pageArrowBounds(boolean left){
    Rectangle r=pageArrowComponentBounds(left);Point p=logical(r.getLocation());
    return new Rectangle(p.x,p.y,(int)(r.width/scale()),(int)(r.height/scale()));
  }
  private Rectangle pageArrowComponentBounds(boolean left){
    BufferedImage sprite=left?leftArrow:rightArrow;
    int width=(int)((sprite==null?45:sprite.getWidth())*scale()),height=(int)((sprite==null?37:sprite.getHeight())*scale());
    return new Rectangle(left?8:getWidth()-width-8,getHeight()/2-height/2,width,height);
  }
  private void paintPageArrow(Graphics graphics,boolean left){
    BufferedImage sprite=left?leftArrow:rightArrow;
    if(sprite!=null){Rectangle r=pageArrowComponentBounds(left);PixelDrawing.blit(graphics,sprite,r.x,r.y,r.width,r.height);}
  }
  private static boolean sprite(Graphics2D g,BufferedImage[] sprites,int i,int x,int y){
    if(sprites==null||i<0||i>=sprites.length||sprites[i]==null)return false;
    g.drawImage(sprites[i],x,y,null);return true;
  }
  private static void center(Graphics2D g,ClientBitmapFont font,String text,int x,int y,Color color){if(font!=null)font.center(g,text,x,y,color.getRGB(),-1);}
  private static void text(Graphics2D g,ClientBitmapFont font,String text,int x,int y,Color color){if(font!=null)font.draw(g,text,x,y,color.getRGB(),-1);}
}
