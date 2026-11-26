package tv.moonga.classicworlds;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.swing.*;
import net.runelite.api.*;
import net.runelite.api.events.ClientTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.WorldService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.WorldUtil;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import com.google.inject.Provides;
import java.awt.event.KeyEvent;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.KeyListener;

@PluginDescriptor(name="Classic World Selector",description="Restore the compact classic login world grid",tags={"world","login","classic","region"})
public class ClassicWorldSelectorPlugin extends Plugin {
  @Inject private Client client;
  @Inject private ClientThread clientThread;
  @Inject private WorldService worldService;
  @Inject private ScheduledExecutorService executor;
  @Inject private ClassicWorldSelectorConfig config;
  @Inject private ConfigManager configManager;
  @Inject private KeyManager keyManager;
  @Inject private net.runelite.client.eventbus.EventBus eventBus;
  private ClassicWorldPanel panel;
  private ClassicWorldButton button;
  private JLayeredPane layer;
  private javax.swing.Timer timer;
  private volatile boolean running,opened;
  private volatile List<WorldEntry> serviceWorlds=Collections.emptyList();
  private final java.util.concurrent.atomic.AtomicBoolean refreshing=new java.util.concurrent.atomic.AtomicBoolean();
  private boolean assetsLoaded;
  private int ticks;
  private volatile Map<Integer,WorldEntry> knownWorlds=Collections.emptyMap();
  private WorldPreferences currentPreferences=WorldPreferences.DEFAULT;
  private final KeyListener selectorKeys=new KeyListener(){
    @Override public boolean isEnabledOnLoginScreen(){return true;}
    @Override public void keyPressed(KeyEvent event){
      if(!selectorInputActive())return;
      event.consume();
      int key=event.getKeyCode(),modifiers=event.getModifiersEx();
      SwingUtilities.invokeLater(()->{if(selectorInputActive()&&panel!=null)panel.handleKey(key,modifiers);});
    }
    @Override public void keyReleased(KeyEvent event){if(selectorInputActive())event.consume();}
    @Override public void keyTyped(KeyEvent event){
      if(!selectorInputActive())return;event.consume();char value=event.getKeyChar();
      SwingUtilities.invokeLater(()->{if(selectorInputActive()&&panel!=null)panel.handleTyped(value);});
    }
  };
  private boolean selectorInputActive(){return running&&opened&&canSelect(client.getGameState());}

  @Override protected void startUp(){
    running=true;
    keyManager.registerKeyListener(selectorKeys);
    SwingUtilities.invokeLater(()->{
      if(!running)return;
      panel=new ClassicWorldPanel(this::select,this::close);
      button=new ClassicWorldButton(this::open);button.world(client.getWorld());
      panel.preferencesChanged(this::savePreference);
      panel.settingsRequested(this::openSettings);
      applyDisplaySize();
      panel.setVisible(false);timer=new javax.swing.Timer(100,e->position());timer.start();position();
      clientThread.invokeLater(()->{if(running)sample();});
    });
    refreshWorlds();
  }
  static boolean canSelect(GameState state){return state==GameState.LOGIN_SCREEN;}
  @Provides ClassicWorldSelectorConfig provideConfig(ConfigManager manager){return manager.getConfig(ClassicWorldSelectorConfig.class);}
  @Subscribe public void onConfigChanged(ConfigChanged event){
    if(ClassicWorldSelectorConfig.GROUP.equals(event.getGroup()))SwingUtilities.invokeLater(()->{if(running&&panel!=null){applyDisplaySize();position();}});
  }
  private void applyDisplaySize(){
    DisplaySize size=config==null?DisplaySize.NORMAL:config.displaySize();
    panel.displaySize(size);
    currentPreferences=WorldPreferences.from(config);panel.options(currentPreferences);button.options(currentPreferences);
  }
  private void savePreference(String key,Object value){
    // Apply to the visible button immediately, independently of ConfigChanged dispatch timing.
    if(!key.equals("displaySize")){currentPreferences=currentPreferences.with(key,value);button.options(currentPreferences);}
    configManager.setConfiguration(ClassicWorldSelectorConfig.GROUP,key,value);
  }
  @Subscribe public void onClientTick(ClientTick event){if(running&&++ticks%50==0&&canSelect(client.getGameState()))sample();}
  private void sample(){
    if(!running)return;
    List<WorldEntry> snapshot=new ArrayList<>();
    net.runelite.api.World[] nativeWorlds=client.getWorldList();
    if(nativeWorlds!=null&&nativeWorlds.length>0){for(net.runelite.api.World w:nativeWorlds)if(w!=null)snapshot.add(WorldEntry.from(w));}
    else snapshot.addAll(serviceWorlds);
    EnumComposition locations=canSelect(client.getGameState())?client.getEnum(EnumID.WORLD_LOCATIONS):null;
    for(int i=0;i<snapshot.size();i++){
      WorldEntry world=snapshot.get(i);
      if(locations!=null)snapshot.set(i,world.withServerLocation(locations.getIntValue(world.id)));
    }
    if(!assetsLoaded&&client.getIndexSprites()!=null){
      BufferedImage[] bg=images(814),flags=images(815),stars=images(817),arrows=images(816);
      BufferedImage[] left=images(308),right=images(309),worldButton=images(818);
      ClientBitmapFont small=font(494),bold=font(496);
      assetsLoaded=bg!=null&&flags!=null&&stars!=null&&arrows!=null&&first(left)!=null&&first(right)!=null&&first(worldButton)!=null&&small!=null&&bold!=null;
      SwingUtilities.invokeLater(()->{if(running&&panel!=null){panel.assets(bg,flags,stars,arrows);panel.pageArrows(first(left),first(right));panel.fonts(small,bold);button.background(first(worldButton));button.fonts(small,bold);button.flagSprites(flags);}});
    }
    Map<Integer,WorldEntry> worldMap=new HashMap<>();for(WorldEntry world:snapshot)worldMap.put(world.id,world);knownWorlds=Collections.unmodifiableMap(worldMap);
    SwingUtilities.invokeLater(()->{if(running&&panel!=null)panel.worlds(snapshot);});
  }
  private static BufferedImage first(BufferedImage[] images){return images==null||images.length==0?null:images[0];}
  private ClientBitmapFont font(int archive){
    IndexDataBase metrics=client.getIndex(13);if(metrics==null)return null;
    byte[] data=metrics.loadData(archive,0);if(data==null)return null;
    return ClientBitmapFont.from(data,client.getSprites(client.getIndexSprites(),archive,0));
  }
  private void refreshWorlds(){
    if(!refreshing.compareAndSet(false,true))return;
    executor.execute(()->{
      try{
        if(!running)return;
        // WorldService can wait for its first HTTP result. Never call it on the game thread.
        worldService.refresh();
        net.runelite.http.api.worlds.WorldResult result=worldService.getWorlds();
        List<WorldEntry> values=new ArrayList<>();
        if(result!=null&&result.getWorlds()!=null)for(net.runelite.http.api.worlds.World w:result.getWorlds())
          values.add(new WorldEntry(w.getId(),w.getPlayers(),w.getRegion()==null?0:w.getRegion().getId(),w.getActivity(),w.getAddress(),WorldUtil.toWorldTypes(w.getTypes())));
        if(running){serviceWorlds=Collections.unmodifiableList(values);clientThread.invokeLater(()->{if(running)sample();});}
      }catch(RuntimeException error){
        org.slf4j.LoggerFactory.getLogger(getClass()).warn("Could not refresh the classic world list",error);
        SwingUtilities.invokeLater(()->{if(running&&panel!=null)panel.status("World list unavailable. Close and reopen to retry.");});
      }finally{refreshing.set(false);}
    });
  }
  private BufferedImage[] images(int archive){
    try{
      SpritePixels[] sprites=client.getSprites(client.getIndexSprites(),archive,0);
      if(sprites==null)return null;
      BufferedImage[] images=new BufferedImage[sprites.length];
      for(int i=0;i<sprites.length;i++)if(sprites[i]!=null){
        SpritePixels sprite=sprites[i];
        // Native drawAt includes the transparent canvas and sprite offsets.
        BufferedImage image=new BufferedImage(Math.max(sprite.getMaxWidth(),sprite.getWidth()+sprite.getOffsetX()),Math.max(sprite.getMaxHeight(),sprite.getHeight()+sprite.getOffsetY()),BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();try{g.drawImage(sprite.toBufferedImage(),sprite.getOffsetX(),sprite.getOffsetY(),null);}finally{g.dispose();}
        images[i]=image;
      }
      return images;
    }catch(RuntimeException e){return null;}
  }
  private void open(){
    if(!running||!canSelect(client.getGameState()))return;
    opened=true;position();clientThread.invokeLater(()->{if(running)sample();});refreshWorlds();
  }
  void openSettings(){
    if(!selectorInputActive())return;
    // Keep the grid open; native sidebar controls receive their own focus and keys.
    // Use the same public configuration event handled by RuneLite's ConfigPlugin.
    net.runelite.client.ui.overlay.Overlay settingsTarget=new net.runelite.client.ui.overlay.Overlay(this){
      @Override public Dimension render(Graphics2D graphics){return null;}
    };
    eventBus.post(new net.runelite.client.events.OverlayMenuClicked(
        new net.runelite.client.ui.overlay.OverlayMenuEntry(MenuAction.RUNELITE_OVERLAY_CONFIG,"Configure","Classic World Selector"),settingsTarget));
  }
  private void close(){opened=false;if(panel!=null)panel.busy(false);position();}
  private void position(){
    if(panel==null||button==null)return;
    Canvas canvas=client.getCanvas();JRootPane root=SwingUtilities.getRootPane(canvas);
    if(root==null){panel.setVisible(false);button.setVisible(false);return;}
    if(layer!=root.getLayeredPane()){
      if(layer!=null){layer.remove(panel);layer.remove(button);}
      layer=root.getLayeredPane();layer.add(button,JLayeredPane.PALETTE_LAYER);layer.add(panel,JLayeredPane.POPUP_LAYER);
    }
    java.awt.Point origin=SwingUtilities.convertPoint(canvas,0,0,layer);
    boolean login=running&&canSelect(client.getGameState());if(!login)opened=false;
    panel.setBounds(origin.x,origin.y,canvas.getWidth(),canvas.getHeight());panel.setVisible(login&&opened);
    Dimension size=button.getPreferredSize();
    button.setBounds(origin.x+Math.max(0,(canvas.getWidth()-765)/2)+5,origin.y+498-size.height,size.width,size.height);
    button.world(client.getWorld());button.setVisible(login&&!opened);
    WorldEntry current=knownWorlds.get(client.getWorld());
    button.worldInfo(current==null?-1:current.location,current==null?client.getWorldType():current.types,current==null?"-":current.activity);
  }
  private void select(WorldEntry entry){
    if(!entry.selectable())return;panel.busy(true);
    clientThread.invokeLater(()->{
      if(!running)return;
      if(!canSelect(client.getGameState())){SwingUtilities.invokeLater(()->{if(running)close();});return;}
      try{
        net.runelite.api.World world=client.createWorld();
        world.setId(entry.id);world.setAddress(entry.address);world.setActivity(entry.activity);
        world.setLocation(entry.location);world.setPlayerCount(entry.players);world.setTypes(entry.types.clone());
        client.changeWorld(world);
        SwingUtilities.invokeLater(()->{if(running)close();});
      }catch(RuntimeException error){SwingUtilities.invokeLater(()->{if(running){panel.busy(false);panel.status("Could not select this world. Please try again.");}});}
    });
  }
  @Override protected void shutDown(){
    running=false;opened=false;
    keyManager.unregisterKeyListener(selectorKeys);
    SwingUtilities.invokeLater(()->{if(timer!=null)timer.stop();if(layer!=null){if(panel!=null)layer.remove(panel);if(button!=null)layer.remove(button);layer.revalidate();layer.repaint();}panel=null;button=null;});
  }
}
