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
  private List<WorldEntry> favouriteWorlds=Collections.emptyList();
  private int favouriteOffset;
  private int draggingFavourite=-1;
  private List<WorldEntry> allWorlds=Collections.emptyList();
  private WorldPreferences options=WorldPreferences.DEFAULT;
  private BiConsumer<String,Object> settingsChanged=(key,value)->{};
  private Runnable settingsRequested=()->{};
  private final WorldMenus menus=new WorldMenus(this::preferenceChanged,()->settingsRequested.run());
  static final Rectangle SEARCH=new Rectangle(8,27,165,23);
  private String query="";
  private int caret;
  private boolean searchFocused,selectAll;
  void settingsRequested(Runnable action){settingsRequested=action;}
  String query(){return query;}
  static String normaliseSearch(String text){return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");}
  private boolean matchesSearch(WorldEntry world){
    String term=normaliseSearch(query);
    return term.isEmpty()||normaliseSearch("World "+world.id+" "+world.activity+" "+WorldPreferences.GameMode.of(world.types)).contains(term);
  }
  void query(String value){query=value.length()>64?value.substring(0,64):value;caret=query.length();selectAll=false;page=0;favouriteOffset=0;sortWorlds();repaint();}
  void handleTyped(char value){
    if(!searchFocused||Character.isISOControl(value)||value<32||value>126)return;
    if(selectAll){query="";caret=0;selectAll=false;}
    if(query.length()>=64)return;
    int next=caret+1;String updated=query.substring(0,caret)+value+query.substring(caret);query(updated);caret=next;
  }
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
      private int pressed=-1,pressedWorld=-1;
      @Override public void mouseMoved(MouseEvent e){mouse=logical(e.getPoint());menus.mouse(mouse);hover=hit(mouse);setCursor(Cursor.getPredefinedCursor(!busy&&!menus.isOpen()&&separateFavourites()&&mouse.x>=3&&mouse.x<16&&mouse.y>=79&&mouse.y<499&&favouriteOffset+(mouse.y-79)/20<favouriteWorlds.size()?Cursor.MOVE_CURSOR:SEARCH.contains(mouse)?Cursor.TEXT_CURSOR:Cursor.DEFAULT_CURSOR));repaint();}
      @Override public void mouseExited(MouseEvent e){hover=-1;mouse=new Point(-1,-1);menus.mouse(mouse);setCursor(Cursor.getDefaultCursor());repaint();}
      @Override public void mousePressed(MouseEvent e){
        if(!SwingUtilities.isLeftMouseButton(e))return;
        requestFocusInWindow();
        Point point=logical(e.getPoint());if(!SEARCH.contains(point)){searchFocused=false;selectAll=false;}pressed=hit(point);WorldEntry world=entry(pressed);pressedWorld=world==null?-1:world.id;
        if(!busy&&!menus.isOpen()&&separateFavourites()&&point.x>=3&&point.x<16){
          int row=(point.y-79)/20;
          if(point.y>=79&&row<favouriteRows()&&favouriteOffset+row<favouriteWorlds.size()){
            draggingFavourite=favouriteWorlds.get(favouriteOffset+row).id;pressed=-1;pressedWorld=-1;e.consume();repaint();
          }
        }
      }
      @Override public void mouseDragged(MouseEvent e){
        if(draggingFavourite>=0){mouse=logical(e.getPoint());e.consume();repaint();}
      }
      @Override public void mouseReleased(MouseEvent e){
        if(draggingFavourite>=0&&SwingUtilities.isLeftMouseButton(e)){
          Point point=logical(e.getPoint());
          if(separateFavourites()&&point.x>=0&&point.x<110&&point.y>=79&&point.y<499){
            int row=Math.min(favouriteRows()-1,(point.y-79)/20);
            int index=Math.min(favouriteWorlds.size()-1,favouriteOffset+row);
            if(index>=0){
              int target=options.favourites.indexOf(favouriteWorlds.get(index).id);
              List<Integer> order=new ArrayList<>(options.favourites);order.remove(Integer.valueOf(draggingFavourite));
              order.add(Math.min(target,order.size()),draggingFavourite);
              preferenceChanged("favouriteWorlds",WorldPreferences.serialise(order));
            }
          }
          draggingFavourite=-1;pressed=-1;pressedWorld=-1;e.consume();repaint();return;
        }
        if(SwingUtilities.isRightMouseButton(e)){
          if(!busy&&options.favouritesEnabled){WorldEntry world=entry(hit(logical(e.getPoint())));if(world!=null)preferenceChanged("favouriteWorlds",options.toggleFavourite(world.id));}
          pressed=-1;e.consume();return;
        }
        if(!SwingUtilities.isLeftMouseButton(e))return;
        int target=hit(logical(e.getPoint()));
        WorldEntry world=entry(target);
        if(target==pressed&&(world==null?pressedWorld<0:world.id==pressedWorld))activate(target);
        pressed=-1;
      }
      @Override public void mouseWheelMoved(MouseWheelEvent e){if(menus.isOpen())menus.step(Integer.signum(e.getWheelRotation()));else if(separateFavourites()&&logical(e.getPoint()).x<110){favouriteOffset=Math.max(0,Math.min(Math.max(0,favouriteWorlds.size()-favouriteRows()),favouriteOffset+Integer.signum(e.getWheelRotation())));}else turn(Integer.signum(e.getWheelRotation()));repaint();e.consume();}
    };
    addMouseListener(input);addMouseMotionListener(input);addMouseWheelListener(input);
    // Mouse interaction focuses this component; Canvas hooks do not receive its keys.
    addKeyListener(new KeyAdapter(){
      @Override public void keyPressed(KeyEvent event){handleKey(event.getKeyCode(),event.getModifiersEx());event.consume();}
      @Override public void keyTyped(KeyEvent event){handleTyped(event.getKeyChar());event.consume();}
      @Override public void keyReleased(KeyEvent event){event.consume();}
    });
  }
  void handleKey(int key){handleKey(key,0);}
  void handleKey(int key,int modifiers){
    if(searchFocused){
      if(key==KeyEvent.VK_A&&(modifiers&InputEvent.CTRL_DOWN_MASK)!=0){selectAll=true;repaint();return;}
      if(key==KeyEvent.VK_ESCAPE){if(!query.isEmpty())query("");else searchFocused=false;repaint();return;}
      if(key==KeyEvent.VK_ENTER){searchFocused=false;selectAll=false;repaint();return;}
      if(key==KeyEvent.VK_BACK_SPACE||key==KeyEvent.VK_DELETE){
        if(selectAll)query("");
        else if(key==KeyEvent.VK_BACK_SPACE&&caret>0){int next=caret-1;query(query.substring(0,next)+query.substring(caret));caret=next;}
        else if(key==KeyEvent.VK_DELETE&&caret<query.length()){int next=caret;query(query.substring(0,caret)+query.substring(caret+1));caret=next;}
        repaint();return;
      }
      if(key==KeyEvent.VK_LEFT||key==KeyEvent.VK_RIGHT||key==KeyEvent.VK_HOME||key==KeyEvent.VK_END){
        caret=key==KeyEvent.VK_HOME?0:key==KeyEvent.VK_END?query.length():Math.max(0,Math.min(query.length(),caret+(key==KeyEvent.VK_LEFT?-1:1)));
        selectAll=false;repaint();return;
      }
      return;
    }
    if(key==KeyEvent.VK_ESCAPE&&draggingFavourite>=0){draggingFavourite=-1;repaint();return;}
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
  void assets(BufferedImage[] backgrounds,BufferedImage[] flags,BufferedImage[] stars,BufferedImage[] arrows){
    this.backgrounds=backgrounds;this.flags=flags;this.stars=stars;this.arrows=arrows;repaint();
  }
  void pageArrows(BufferedImage left,BufferedImage right){leftArrow=left;rightArrow=right;repaint();}
  void fonts(ClientBitmapFont small,ClientBitmapFont bold){smallFont=small;boldFont=bold;repaint();}
  void displaySize(DisplaySize size){displaySize=size==null?DisplaySize.NORMAL:size;menus.settings(options,displaySize);repaint();}
  void preferencesChanged(BiConsumer<String,Object> callback){settingsChanged=callback;}
  void options(WorldPreferences value){
    boolean reorder=value.mode!=options.mode||value.region!=options.region||value.membership!=options.membership||value.compact!=options.compact||value.favouritesEnabled!=options.favouritesEnabled||value.favouriteView!=options.favouriteView||!value.favourites.equals(options.favourites);
    if(reorder)draggingFavourite=-1;
    options=value;menus.settings(options,displaySize);if(reorder){page=0;sortWorlds();}repaint();
  }
  private void preferenceChanged(String key,Object value){
    if(key.equals("displaySize"))displaySize((DisplaySize)value);else options(options.with(key,value));
    settingsChanged.accept(key,value);hover=-1;repaint();
  }
  void worlds(List<WorldEntry> values){allWorlds=new ArrayList<>(values);sortWorlds();hover=hit(mouse);repaint();}
  boolean separateFavourites(){return options.favouritesEnabled&&options.favouriteView==WorldPreferences.FavouriteView.SEPARATE_COLUMN;}
  ClassicLayout worldLayout(){return separateFavourites()?new ClassicLayout(worlds.size(),options.compact,77,options.compact?110:160,options.compact?655:550):new ClassicLayout(worlds.size(),options.compact,57);}
  private int favouriteRows(){return 21;}
  Rectangle favouriteTile(int visibleIndex){return new Rectangle(16,79+visibleIndex*20,88,19);}
  private WorldEntry entry(int target){
    if(target<=-30000){int index=-target-30000;return index<favouriteWorlds.size()?favouriteWorlds.get(index):null;}
    return target>=0&&target<worlds.size()?worlds.get(target):null;
  }
  void status(String text){message=text;repaint();}
  void busy(boolean value){busy=value;draggingFavourite=-1;repaint();}
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
    worlds.removeIf(world->!matchesSearch(world));
    favouriteWorlds=new ArrayList<>();
    if(separateFavourites()){
      Map<Integer,WorldEntry> available=new HashMap<>();for(WorldEntry world:worlds)available.put(world.id,world);
      for(int id:options.favourites){WorldEntry world=available.get(id);if(world!=null)favouriteWorlds.add(world);}
      worlds.removeIf(options::isFavourite);
    }
    favouriteOffset=Math.min(favouriteOffset,Math.max(0,favouriteWorlds.size()-favouriteRows()));
    message=allWorlds.isEmpty()?"Loading worlds...":worlds.isEmpty()&&favouriteWorlds.isEmpty()?(!normaliseSearch(query).isEmpty()?"No worlds match your search.":"No worlds match this membership filter."):"";
    page=Math.min(page,worldLayout().maxPage);hover=-1;
  }
  private void turn(int direction){page=Math.max(0,Math.min(worldLayout().maxPage,page+direction));hover=-1;repaint();}
  private double scale(){return Math.min(1,Math.min(getWidth()/765.0,getHeight()/503.0));}
  private int originX(){return (getWidth()-(int)(765*scale()))/2;}
  Point logical(Point point){double s=scale();return new Point((int)((point.x-originX())/s),(int)(point.y/s));}
  int hit(Point p){
    if(SEARCH.contains(p))return p.x>=SEARCH.x+SEARCH.width-18&&!query.isEmpty()?-106:-105;
    Integer menu=menus.hit(p);if(menu!=null)return menu;
    if(new Rectangle(708,4,50,16).contains(p))return -2;
    for(int i=0;i<HEADINGS.length;i++){
      if(new Rectangle(HEADINGS[i],0,15,23).contains(p))return -10-i*2;
      if(new Rectangle(HEADINGS[i]+15,0,95,23).contains(p))return -11-i*2;
    }
    ClassicLayout layout=worldLayout();
    if(page>0&&pageArrowBounds(true).contains(p))return -3;
    if(page<layout.maxPage&&pageArrowBounds(false).contains(p))return -4;
    if(separateFavourites())for(int i=0;i<favouriteRows()&&favouriteOffset+i<favouriteWorlds.size();i++)
      if(favouriteTile(i).contains(p))return -30000-favouriteOffset-i;
    int first=layout.first(page);
    for(int i=0;i<layout.columns*layout.rows&&first+i<worlds.size();i++)
      if(layout.tile(i).contains(p))return first+i;
    return -1;
  }
  void activate(int target){
    if(target==-2){close.run();return;}
    if(busy)return;
    if(target==-105||target==-106){menus.dismiss();draggingFavourite=-1;searchFocused=true;selectAll=false;if(target==-106)query("");caret=query.length();repaint();return;}
    searchFocused=false;selectAll=false;
    if(menus.activate(target)){hover=-1;repaint();return;}
    if(target==-3){turn(-1);return;}if(target==-4){turn(1);return;}
    if(target<=-10&&target>=-17){int action=-target-10;sort=action/2;ascending=action%2==0;page=0;sortWorlds();repaint();return;}
    WorldEntry world=entry(target);if(world!=null&&world.selectable())select.accept(world);
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
        drawWorld(g,worlds.get(first+i),layout.tile(i),first+i,!options.compact);
      }
      if(separateFavourites()){
        g.setColor(new Color(0x857f68));g.drawLine(109,57,109,501);
        text(g,smallFont,"Favourites",16,71,new Color(0xffd65a));
        text(g,smallFont,"Worlds",layout.startX,71,Color.WHITE);
        for(int i=0;i<favouriteRows()&&favouriteOffset+i<favouriteWorlds.size();i++){
          int target=-30000-favouriteOffset-i;
          Rectangle tile=favouriteTile(i);WorldEntry world=favouriteWorlds.get(favouriteOffset+i);
          drawWorld(g,world,tile,target,false);
          g.setColor(new Color(world.id==draggingFavourite?0xffd65a:0x8c8879));
          for(int line=0;line<3;line++)g.drawLine(5,tile.y+6+line*3,12,tile.y+6+line*3);

        }
        if(favouriteWorlds.isEmpty()){text(g,smallFont,"Right-click",12,99,Color.GRAY);text(g,smallFont,"a world",12,113,Color.GRAY);}
        if(favouriteWorlds.size()>favouriteRows())text(g,smallFont,"Scroll for more",8,501,Color.GRAY);
      }
      if(draggingFavourite>=0){
        int row=Math.max(0,Math.min(favouriteRows()-1,(mouse.y-79)/20));
        g.setColor(new Color(0xffd65a));g.drawRect(3,79+row*20,102,19);
      }
      if(worlds.isEmpty()||!message.isEmpty())center(g,boldFont,message,382,worlds.isEmpty()?253:498,Color.YELLOW);
      menus.paint(g,smallFont);paintSearch(g);
    }finally{g.dispose();}
    PixelDrawing.blit(graphics,pixels,originX(),0,(int)(765*scale()),(int)(503*scale()));
    ClassicLayout layout=worldLayout();
    if(page>0)paintPageArrow(graphics,true);
    if(page<layout.maxPage)paintPageArrow(graphics,false);
    paintTooltip(graphics);
  }
  private void paintSearch(Graphics2D g){
    Rectangle r=SEARCH;g.setColor(new Color(0x202024));g.fillRect(r.x,r.y,r.width,r.height);
    g.setColor(new Color(searchFocused?0xd7c580:0x857f68));g.drawRect(r.x,r.y,r.width-1,r.height-1);
    if(smallFont==null)return;
    Shape clip=g.getClip();g.clipRect(r.x+5,r.y+2,r.width-26,r.height-4);
    if(query.isEmpty()&&!searchFocused)text(g,smallFont,"Search worlds...",r.x+6,r.y+15,Color.GRAY);
    else{
      int offset=Math.max(0,smallFont.width(query.substring(0,caret))-(r.width-31));
      if(selectAll){g.setColor(new Color(0x4b6080));g.fillRect(r.x+5,r.y+3,smallFont.width(query)+2,17);}
      text(g,smallFont,query,r.x+6-offset,r.y+15,Color.WHITE);
      if(searchFocused){int x=r.x+6+smallFont.width(query.substring(0,caret))-offset;g.setColor(Color.WHITE);g.drawLine(x,r.y+5,x,r.y+17);}
    }
    g.setClip(clip);
    if(!query.isEmpty()){int x=r.x+r.width-12,y=r.y+11;g.setColor(new Color(0xd7c580));g.drawLine(x-3,y-3,x+3,y+3);g.drawLine(x+3,y-3,x-3,y+3);}
  }
  private void drawWorld(Graphics2D g,WorldEntry w,Rectangle r,int target,boolean activity){
        boolean nativeBackground=sprite(g,backgrounds,w.background(),r.x,r.y);
        if(!nativeBackground){
          Color top=w.members()?new Color(0xb8a670):new Color(0xa6a6a6);
          g.setPaint(new GradientPaint(0,r.y,top,0,r.y+19,new Color(0x545454)));g.fillRect(r.x,r.y,88,19);
        }
        if(hover==target&&w.selectable()&&!busy){g.setColor(new Color(255,255,255,128));g.fillRect(r.x,r.y,88,19);}
        // The original renderer used half the flag sheet, not a fixed eight-region offset.
        sprite(g,flags,w.location+(w.members()&&flags!=null?flags.length/2:0),r.x+29,r.y);
        center(g,boldFont,Integer.toString(w.id),r.x+15,r.y+14,w.types.contains(net.runelite.api.WorldType.DEADMAN)?Color.RED:Color.BLACK);
        center(g,smallFont,w.population(),r.x+60,r.y+14,Color.WHITE);
        if(activity){
          if(hover==target){g.setColor(new Color(192,169,70,65));g.fillRect(r.x+90,r.y,r.width-90,19);}
          Shape clip=g.getClip();g.clipRect(r.x+93,r.y,r.width-95,r.height);
          text(g,smallFont,w.activity.equals("-")?WorldPreferences.GameMode.of(w.types).toString():w.activity,r.x+94,r.y+14,Color.WHITE);g.setClip(clip);
        }
        if(options.isFavourite(w)){g.setColor(new Color(0xffd65a));g.drawRect(r.x,r.y,87,18);}
  }
  private void paintTooltip(Graphics graphics){
    WorldEntry world=entry(hover);
    if(draggingFavourite>=0||!options.compact||menus.isOpen()||world==null||mouse.y<57||smallFont==null)return;
    ClientBitmapFont tooltipFont=options.boldHover&&boldFont!=null?boldFont:smallFont;
    PixelDrawing.DeviceGrid grid=new PixelDrawing.DeviceGrid(graphics,displaySize);
    Point topLeft=grid.point(0,0),bottomRight=grid.point(getWidth(),getHeight());
    int available=Math.max(8,(int)((bottomRight.x-topLeft.x)/grid.scale)-4);
    String text=world.tooltip();int width=Math.min(available,tooltipFont.width(text)+6);
    int height=tooltipFont.ascent+8;
    BufferedImage tooltip=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);Graphics2D g=PixelDrawing.raster(tooltip);
    try{
      g.setColor(new Color(0xffffa0));g.fillRect(0,0,width,height);g.setColor(Color.BLACK);g.drawRect(0,0,width-1,height-1);
      g.clipRect(2,1,width-4,height-2);tooltipFont.center(g,text,width/2,tooltipFont.ascent+4,0,-1);
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
    return new Rectangle(left?(separateFavourites()?originX()+(int)(110*scale())+8:8):getWidth()-width-8,getHeight()/2-height/2,width,height);
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
