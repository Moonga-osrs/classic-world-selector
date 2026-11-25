package tv.moonga.classicworlds;

import java.awt.*;
import java.util.function.BiConsumer;

/** Bitmap dropdowns share the selector's surface, avoiding heavyweight-canvas popup clipping. */
final class WorldMenus {
  private static final Rectangle[] BUTTONS={new Rectangle(8,27,175,23),new Rectangle(188,27,155,23),new Rectangle(348,27,183,23),new Rectangle(536,27,104,23),new Rectangle(645,27,112,23)};
  static final int DISMISS=-199;
  private final BiConsumer<String,Object> changed;
  private WorldPreferences options=WorldPreferences.DEFAULT;
  private DisplaySize textSize=DisplaySize.NORMAL;
  private int open=-1,keyboardRow=-1;
  private Point mouse=new Point(-1,-1);
  WorldMenus(BiConsumer<String,Object> changed){this.changed=changed;}
  void settings(WorldPreferences value,DisplaySize size){options=value;textSize=size;}
  boolean isOpen(){return open>=0;}
  boolean dismiss(){if(open<0)return false;open=-1;keyboardRow=-1;return true;}
  void mouse(Point point){mouse=point;keyboardRow=-1;}
  private Object[] values(){
    switch(open){case 0:return WorldPreferences.GameMode.values();case 1:return WorldPreferences.Region.values();case 2:return WorldPreferences.Membership.values();case 3:return new Boolean[]{true,false};default:return new Object[]{0,1,2,3,4};}
  }
  Rectangle menuBounds(){
    if(open<0)return new Rectangle();
    int width=Math.max(BUTTONS[open].width,open==4?190:180);
    return new Rectangle(Math.min(BUTTONS[open].x,765-width-4),52,width,values().length*20+2);
  }
  Integer hit(Point p){
    if(open>=0){Rectangle menu=menuBounds();if(menu.contains(p)){
      int row=(p.y-menu.y-1)/20;return row>=0&&row<values().length?-2000-row:DISMISS;
    }}
    for(int i=0;i<BUTTONS.length;i++)if(BUTTONS[i].contains(p))return -100-i;
    return open>=0?DISMISS:null;
  }
  boolean activate(int target){
    if(target==DISMISS){dismiss();return true;}
    if(target<=-100&&target>=-104){int next=-target-100;open=open==next?-1:next;keyboardRow=-1;return true;}
    if(target<=-2000&&target>=-2100&&open>=0){
      int row=-target-2000;Object[] values=values();if(row>=values.length)return true;
      int selectedMenu=open;Object value=values[row];dismiss();
      if(selectedMenu==0)changed.accept("preferredMode",value);
      else if(selectedMenu==1)changed.accept("preferredRegion",value);
      else if(selectedMenu==2)changed.accept("membership",value);
      else if(selectedMenu==3)changed.accept("compact",value);
      else if(row==0)changed.accept("showFlag",!options.showFlag);
      else if(row==1)changed.accept("buttonLine",options.buttonLine==WorldPreferences.ButtonLine.GAME_MODE?WorldPreferences.ButtonLine.CLICK_TO_SWITCH:WorldPreferences.ButtonLine.GAME_MODE);
      else changed.accept("displaySize",DisplaySize.values()[row-2]);
      return true;
    }
    return false;
  }
  void step(int direction){if(open>=0)keyboardRow=keyboardRow<0?(direction<0?values().length-1:0):Math.floorMod(keyboardRow+direction,values().length);}
  void choose(){if(open>=0&&keyboardRow>=0)activate(-2000-keyboardRow);}
  private String label(int row){
    if(open==3)return row==0?"Compact":"With activity";
    if(open==4){
      if(row==0)return "Country flag: "+(options.showFlag?"On":"Off");
      if(row==1)return "World info: "+(options.buttonLine==WorldPreferences.ButtonLine.GAME_MODE?"On":"Off");
      DisplaySize size=DisplaySize.values()[row-2];return (textSize==size?"(*) ":"( ) ")+"Hover text: "+size;
    }
    return values()[row].toString();
  }
  private boolean selected(int row){
    Object value=values()[row];
    switch(open){case 0:return value==options.mode;case 1:return value==options.region;case 2:return value==options.membership;case 3:return (Boolean)value==options.compact;default:return false;}
  }
  void paint(Graphics2D g,ClientBitmapFont font){
    g.setColor(new Color(0x171719));g.fillRect(0,23,765,32);
    String[] labels={"Mode: "+options.mode,"Region: "+options.region,"Membership: "+options.membership,"View: "+(options.compact?"Compact":"Activity"),"Options"};
    for(int i=0;i<BUTTONS.length;i++){
      Rectangle r=BUTTONS[i];g.setColor(new Color(open==i?0x48453a:0x303034));g.fillRect(r.x,r.y,r.width,r.height);
      g.setColor(new Color(0x857f68));g.drawRect(r.x,r.y,r.width-1,r.height-1);
      Shape clip=g.getClip();g.clipRect(r.x+5,r.y+2,r.width-22,r.height-4);
      if(font!=null)font.draw(g,labels[i],r.x+6,r.y+15,0xffffff,-1);g.setClip(clip);
      g.setColor(new Color(0xd7c580));g.fillPolygon(new int[]{r.x+r.width-15,r.x+r.width-7,r.x+r.width-11},new int[]{r.y+9,r.y+9,r.y+14},3);
    }
    if(open<0)return;
    Rectangle r=menuBounds();g.setColor(new Color(0x252527));g.fillRect(r.x,r.y,r.width,r.height);g.setColor(new Color(0xb1a078));g.drawRect(r.x,r.y,r.width-1,r.height-1);
    Integer target=hit(mouse);int hovered=keyboardRow>=0?keyboardRow:target!=null&&target<=-2000?-target-2000:-1;
    for(int row=0;row<values().length;row++){
      int y=r.y+1+row*20;if(row==hovered){g.setColor(new Color(0x514b36));g.fillRect(r.x+1,y,r.width-2,20);}
      if(font!=null)font.draw(g,label(row),r.x+7,y+14,selected(row)?0xffff00:0xffffff,-1);
    }
  }
}
