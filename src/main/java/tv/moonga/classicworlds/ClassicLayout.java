package tv.moonga.classicworlds;

import java.awt.Rectangle;

/** Dimensions and column pagination from the pre-redesign login renderer. */
final class ClassicLayout {
  static final int WIDTH=765, HEIGHT=503, TILE_WIDTH=88, TILE_HEIGHT=19;
  final int columns, rows, gapX, gapY, startX, startY, maxPage;
  final int tileWidth;
  ClassicLayout(int count){this(count,true,23);}
  ClassicLayout(int count,boolean compact,int top) {
    int height=HEIGHT-top;
    tileWidth=compact?TILE_WIDTH:330;
    int cols=compact?WIDTH/(TILE_WIDTH+1)-1:2, rs=height/(TILE_HEIGHT+1);
    if(count>0) {
      int oldCols, oldRows;
      do {
        oldCols=cols;oldRows=rs;
        if(rs*(cols-1)>=count && cols>1)cols--;
        if(cols*(rs-1)>=count && rs>1)rs--;
        if(cols*(rs-1)>=count && rs>1)rs--;
      } while(cols!=oldCols||rs!=oldRows);
    }
    columns=cols;rows=rs;
    gapX=Math.min(5,(WIDTH-cols*tileWidth)/(cols+1));
    gapY=Math.min(5,(height-rs*TILE_HEIGHT)/(rs+1));
    startX=(WIDTH-cols*tileWidth-gapX*(cols-1))/2;
    startY=top+(height-rs*TILE_HEIGHT-gapY*(rs-1))/2;
    maxPage=Math.max(0,(count+rs-1)/rs-cols);
  }
  Rectangle tile(int visibleIndex) {
    return new Rectangle(startX+(visibleIndex/rows)*(tileWidth+gapX),startY+(visibleIndex%rows)*(TILE_HEIGHT+gapY),tileWidth,TILE_HEIGHT);
  }
  int first(int page) { return Math.max(0,Math.min(maxPage,page))*rows; }
}
