import java.lang.instrument.Instrumentation;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicBoolean;

/** Test agent: invokes Minecraft GUI input handlers on the render thread, not Windows input. */
public class GuiScenario {
    static Path root;
    static Class<?> mcClass;
    static ClassLoader loader;
    static volatile boolean finished;
    static int attempts, phase, waitTicks, playbackStage, pickerStage, rowStage, playlistStage;
    static Object playlist, settings, movedUri;
    static final List<String> checks = new ArrayList<>();
    static final AtomicBoolean pending = new AtomicBoolean();
    public static void premain(String args, Instrumentation inst) {
        root=Path.of(args);log("AGENT STARTED");
        if(Boolean.getBoolean("musicplayer.test.requireJar"))System.clearProperty("musicplayer.dev");
        if(Boolean.getBoolean("musicplayer.test.pickers"))inst.addTransformer(new PickerFixture(inst,root.getParent().resolve("fixture.wav").toString(),root.getParent().resolve("fixture-folder").toString()));
        Thread t=new Thread(()->{
            try {
                int maxAttempts=Math.max(1,Integer.getInteger("musicplayer.test.timeoutSeconds",300))*2;
                while(!finished && attempts++<maxAttempts){
                    Thread.sleep(500);
                    if(mcClass==null)for(Class<?> c:inst.getAllLoadedClasses())if(c.getName().equals("net.minecraft.client.Minecraft")){mcClass=c;loader=c.getClassLoader();break;}
                    if(mcClass==null)continue;
                    Thread.currentThread().setContextClassLoader(loader);
                    Object mc=call(mcClass,"getInstance");
                    if(mc!=null && pending.compareAndSet(false,true))call(mc,"execute",(Runnable)()->{try{step(mc);}finally{pending.set(false);}});
                }
                if(!finished)fail(new Exception("Timed out waiting for client/GUI"));
            }catch(Throwable e){fail(e);}
        },"musicplayer-gui-scenario");t.setDaemon(true);t.start();
    }
    static Class<?> type(String n)throws Exception{return Class.forName(n,true,loader);}
    static Object screen(Object mc)throws Exception{
        try{return get(mc,"screen");}catch(NoSuchFieldException e){return call(get(mc,"gui"),"screen");}
    }
    static Object overlay(Object mc)throws Exception{
        try{return call(mc,"getOverlay");}catch(NoSuchMethodException e){return call(get(mc,"gui"),"overlay");}
    }
    static void setScreen(Object mc,Object screen)throws Exception{
        try{call(mc,"setScreen",screen);}catch(NoSuchMethodException e){call(get(mc,"gui"),"setScreen",screen);}
    }
    static Object renderTarget(Object mc)throws Exception{
        try{return call(mc,"getMainRenderTarget");}catch(NoSuchMethodException e){return call(get(mc,"gameRenderer"),"mainRenderTarget");}
    }
    static Object manager(String n)throws Exception{return call(type("info.u_team.music_player.musicplayer.MusicPlayerManager"),n);}
    static void step(Object mc){
        if(finished)return;
        try{
            Object s=screen(mc);
            if(phase==0){
                if(s==null||overlay(mc)!=null||manager("getPlayer")==null)return;
                if(s.getClass().getSimpleName().equals("GuiYoutubeLogin")){
                    snapshot(mc,"startup-login");click(mc,children(s).get(0));pass("YouTube prompt Skip click");return;
                }
                if(!s.getClass().getSimpleName().equals("TitleScreen"))return;
                settings=call(manager("getSettingsManager"),"getSettings");
                String origin=type("info.u_team.music_player.gui.GuiMusicPlayer").getProtectionDomain().getCodeSource().getLocation().toString();
                log("MOD ORIGIN "+origin);
                if(Boolean.getBoolean("musicplayer.test.requireJar")){
                    require(origin.contains("music_player-")&&origin.contains(".jar"),"GUI was not loaded from release JAR: "+origin);
                    require(System.getProperty("musicplayer.dev")==null,"Release JAR test still has the development audio path enabled");
                    String audioOrigin=manager("getPlayer").getClass().getProtectionDomain().getCodeSource().getLocation().toString();
                    require(audioOrigin.contains("musicplayer-lavaplayer.jar.packed"),"Audio player was not loaded from the packed dependency: "+audioOrigin);
                    log("AUDIO ORIGIN "+audioOrigin);
                    pass("Release JAR and packed audio libraries loaded");
                }
                setScreen(mc,type("info.u_team.music_player.gui.GuiMusicPlayer").getConstructor().newInstance());
                pass("Main GUI opens");phase++;return;
            }
            if(++waitTicks>100)throw new AssertionError("Step timed out: "+phase);
            switch(phase){
                case 1 -> {
                    expectScreen(s,"GuiMusicPlayer");snapshot(mc,"main");
                    call(get(s,"namePlaylistField"),"setValue","GUI smoke test");
                    clickAt(mc,number(get(s,"width"))-30,70);
                    List<?> entries=children(get(s,"playlistsList"));require(entries.size()==1,"Create playlist button did not add one playlist");
                    playlist=call(entries.get(0),"getPlaylist");pass("Create playlist button");next();
                }
                case 2 -> {click(mc,get(children(get(s,"playlistsList")).get(0),"openPlaylistButton"));expectScreen(screen(mc),"GuiMusicPlaylist");pass("Open playlist row button");next();}
                case 3 -> {
                    if(!(Boolean)call(playlist,"isLoaded"))return;
                    snapshot(mc,"playlist-empty");click(mc,get(s,"addTracksButton"));expectScreen(screen(mc),"GuiMusicSearch");pass("Add tracks button");next();
                }
                case 4 -> {
                    snapshot(mc,"search-empty");Object before=get(s,"searchProvider");clickAt(mc,22,88);require(!Objects.equals(before,get(s,"searchProvider")),"Search provider button unchanged");
                    pass("Search provider toggle");submit(mc,s,root.getParent().resolve("fixture.wav").toString());next();
                }
                case 5 -> {if(!loaded(s,"Local WAV"))return;snapshot(mc,"local-track-loaded");click(mc,get(s,"addAllButton"));require((Integer)call(playlist,"getEntrySize")==1,"Add all did not add local track");pass("Add all button");next();}
                case 6 -> {submit(mc,s,Files.readString(root.getParent().resolve("http-url.txt")).trim());next();}
                case 7 -> {
                    if(!loaded(s,"HTTP WAV"))return;snapshot(mc,"http-track-loaded");
                    click(mc,get(children(get(s,"searchList")).get(0),"addTrackButton"));
                    require((Integer)call(playlist,"getEntrySize")==2,"Add track row did not add HTTP track");pass("Add individual track button");next();
                }
                case 8 -> {
                    if(Boolean.getBoolean("musicplayer.test.pickers")){
                        if(pickerStage==0){
                            boolean sdl=false;try{type("org.lwjgl.sdl.SDLDialog");sdl=true;}catch(ClassNotFoundException ignored){type("org.lwjgl.util.tinyfd.TinyFileDialogs");}
                            require(sdl?PickerFixture.sdlReady:PickerFixture.tinyReady,"OS chooser fixture hook is missing; refusing to open a native dialog");
                            click(mc,children(s).get(2));pickerStage++;waitTicks=0;return;
                        }
                        if(pickerStage==1){
                            if(!loaded(s,"Load file button WAV"))return;click(mc,get(s,"addAllButton"));require((Integer)call(playlist,"getEntrySize")==3,"File chooser track not added");pass("Load file GUI button with fixture chooser reply");
                            click(mc,children(s).get(3));pickerStage++;waitTicks=0;return;
                        }
                        if(pickerStage==2){
                            if(!loaded(s,"Load folder WAV files",2))return;click(mc,get(s,"addAllButton"));require((Integer)call(playlist,"getEntrySize")==5,"Folder chooser tracks not added");pass("Load folder GUI button with fixture chooser reply");snapshot(mc,"folder-track-results");pickerStage++;
                        }
                    }
                    clickAt(mc,8,8);expectScreen(screen(mc),"GuiMusicPlaylist");pass("Search back button");next();
                }
                case 9 -> {
                    if(playbackStage==0){snapshot(mc,"playlist-loaded");require(children(get(s,"trackList")).size()>=2,"Loaded playlist rows missing");click(mc,get(children(get(s,"trackList")).get(0),"playTrackButton"));pass("Track row play button");playbackStage++;waitTicks=0;return;}
                    Object tm=call(manager("getPlayer"),"getTrackManager");if(call(tm,"getCurrentTrack")==null)return;
                    Object controls=get(s,"controls");Object pause=get(controls,"playButton");if(!(Boolean)get(pause,"active"))return;
                    boolean b=(Boolean)call(tm,"isPaused");click(mc,pause);require(b!=(Boolean)call(tm,"isPaused"),"Pause button unchanged");pass("Playback pause button");
                    Object currentTrack=call(tm,"getCurrentTrack");Object progress=get(controls,"songProgress");
                    clickAt(mc,number(call(progress,"getX"))+number(call(progress,"getWidth"))/2,number(call(progress,"getY"))+2);
                    require(number(call(currentTrack,"getPosition"))>1000,"Track progress click did not seek");pass("Track progress seek click");
                    for(Object widget:children(controls))if(widget.getClass().getSimpleName().equals("GuiVolumeSlider")){
                        click(mc,widget);require(number(call(settings,"getVolume"))>0,"Volume slider unchanged");pass("Volume slider click");
                        clickAt(mc,number(call(widget,"getX"))+1,number(call(widget,"getY"))+5);require(number(call(settings,"getVolume"))==0,"Volume slider zero reset failed");
                    }
                    List<?> buttons=(List<?>)get(controls,"buttons");boolean shuffle=(Boolean)call(settings,"isShuffle");click(mc,buttons.get(3));require(shuffle!=(Boolean)call(settings,"isShuffle"),"Shuffle unchanged");pass("Shuffle button");
                    Object repeat=call(settings,"getRepeat");click(mc,buttons.get(4));require(!repeat.equals(call(settings,"getRepeat")),"Repeat unchanged");pass("Repeat button");
                    click(mc,buttons.get(1));pass("Skip forward button");click(mc,buttons.get(2));pass("Skip back button");
                    call(tm,"setPaused",true);
                    click(mc,get(s,"sortButton"));expectScreen(screen(mc),"GuiMusicPlaylistSort");pass("Sort screen button");next();
                }
                case 10 -> {
                    snapshot(mc,"sort");for(int i=0;i<6;i++){
                        click(mc,children(screen(mc)).get(i));expectScreen(screen(mc),"GuiMusicPlaylist");pass("Sort order button "+i+" and return");
                        if(i<5)click(mc,get(screen(mc),"sortButton"));
                    }next();
                }
                case 11 -> {clickAt(mc,number(get(s,"width"))-8,8);expectScreen(screen(mc),"GuiMusicPlayerSettings");pass("Settings button");next();}
                case 12 -> {
                    snapshot(mc,"settings");Object sign=children(s).get(1);require(number(call(sign,"getY"))>=43,"YouTube sign-in button overlaps playback timeline");
                    click(mc,sign);expectScreen(screen(mc),"GuiYoutubeLogin");click(mc,children(screen(mc)).get(0));expectScreen(screen(mc),"GuiMusicPlayerSettings");pass("Settings sign-in screen and Skip return");
                    boolean b=(Boolean)call(settings,"isKeyWorkInGui");clickAt(mc,25,70);require(b!=(Boolean)call(settings,"isKeyWorkInGui"),"GUI key toggle unchanged");pass("Settings GUI-key toggle");next();
                }
                case 13 -> {boolean b=(Boolean)call(settings,"isShowIngameOverlay");clickAt(mc,25,100);require(b!=(Boolean)call(settings,"isShowIngameOverlay"),"HUD toggle unchanged");pass("Settings HUD toggle");next();}
                case 14 -> {boolean b=(Boolean)call(settings,"isShowIngameMenueOverlay");clickAt(mc,number(get(s,"width"))/2+25,70);require(b!=(Boolean)call(settings,"isShowIngameMenueOverlay"),"Pause overlay toggle unchanged");pass("Settings pause-overlay toggle");next();}
                case 15 -> {
                    Object b=call(settings,"getIngameOverlayPosition");clickAt(mc,number(get(s,"width"))/2+25,100);require(!b.equals(call(settings,"getIngameOverlayPosition")),"Overlay position unchanged");pass("Overlay position cycle");
                    for(Object widget:children(s))if(widget.getClass().getSimpleName().equals("USlider")&&number(call(widget,"getY"))==120){
                        double before=number(call(widget,"getValue"));clickAt(mc,number(call(widget,"getX"))+number(call(widget,"getWidth"))-8,130);
                        require(before!=number(call(widget,"getValue")),"Settings slider value unchanged");pass("Settings speed/pitch slider");
                        clickAt(mc,number(call(widget,"getX"))+number(call(widget,"getWidth"))+20,130);require(number(call(widget,"getValue"))==1,"1x reset button failed");pass("Settings 1x reset button");
                    }
                    double scale=number(call(settings,"getOverlayScale"));
                    clickAt(mc,number(get(s,"width"))-25,160);require(scale!=number(call(settings,"getOverlayScale")),"Overlay scale slider unchanged");pass("Overlay scale slider click");
                    Object devices=get(s,"mixerDeviceList");require(!children(devices).isEmpty(),"Audio device list empty");
                    clickAt(mc,25,203,false);Object selected=call(devices,"getSelected");require(selected!=null&&Objects.equals(call(settings,"getMixer"),call(selected,"getMixerName")),"Audio device row not selected");pass("Audio device row selection");
                    Object lang=call(settings,"getLanguage");clickAt(mc,25,160);require(!lang.equals(call(settings,"getLanguage")),"Language button unchanged");pass("Language button");next();
                }
                case 16 -> {clickAt(mc,number(get(s,"width"))-60,182);expectScreen(screen(mc),"GuiMusicEqualizer");pass("Equalizer screen button");next();}
                case 17 -> {snapshot(mc,"equalizer");boolean b=(Boolean)call(settings,"isEqualizerEnabled");click(mc,children(s).get(1));require(b!=(Boolean)call(settings,"isEqualizerEnabled"),"Equalizer toggle unchanged");pass("Equalizer toggle");next();}
                case 18 -> {
                    boolean b=(Boolean)call(settings,"isBassBoostEnabled");click(mc,children(s).get(2));require(b!=(Boolean)call(settings,"isBassBoostEnabled"),"Bass toggle unchanged");pass("Bass toggle");
                    for(int i=0;i<10;i++){
                        Object band=children(s).get(3+i);clickAt(mc,number(call(band,"getX"))+number(call(band,"getWidth"))/2,number(call(band,"getY"))+5);
                        require(((float[])call(settings,"getEqualizerGains"))[i]!=0,"Equalizer band "+i+" did not react to click");pass("Equalizer band slider "+i);
                    }next();
                }
                case 19 -> {click(mc,children(s).get(children(s).size()-1));expectScreen(screen(mc),"GuiMusicEqualizer");float[] gains=(float[])call(settings,"getEqualizerGains");for(float f:gains)require(f==0,"Equalizer reset left nonzero gain");pass("Equalizer reset button");next();}
                case 20 -> {click(mc,children(s).get(0));expectScreen(screen(mc),"GuiMusicPlayerSettings");pass("Equalizer back button");next();}
                case 21 -> {clickAt(mc,8,8);expectScreen(screen(mc),"GuiMusicPlaylist");pass("Settings back button");next();}
                case 22 -> {
                    Object list=get(s,"trackList");List<?> entries=children(list);
                    if(rowStage==0){snapshot(mc,"playlist-after-settings");movedUri=get(entries.get(0),"uri");click(mc,get(entries.get(0),"downButton"));pass("Track row move-down button");rowStage++;waitTicks=0;return;}
                    if(rowStage==1){require(movedUri==get(entries.get(1),"uri"),"Move-down did not reorder track");click(mc,get(entries.get(1),"upButton"));pass("Track row move-up button");rowStage++;waitTicks=0;return;}
                    if(rowStage==2){require(movedUri==get(entries.get(0),"uri"),"Move-up did not restore track order");click(mc,get(entries.get(0),"deleteTrackButton"));require((Integer)call(playlist,"getEntrySize")==4,"Delete track row did not remove track");pass("Delete track row button");rowStage++;waitTicks=0;return;}
                    if(rowStage==3){Object filter=get(s,"trackFilterField");click(mc,filter);String query=entries.stream().anyMatch(e->{try{return call(get(e,"uri"),"get").toString().contains("track10.wav");}catch(Exception x){throw new RuntimeException(x);}})?"track10.wav":"track2.wav";call(filter,"setValue",query);require(children(list).size()==1,"Track search field did not filter folder track");pass("Track list search field");rowStage++;waitTicks=0;return;}
                    call(get(s,"trackFilterField"),"setValue","");require(children(list).size()==4,"Clearing track filter did not restore rows");
                    clickAt(mc,8,8);expectScreen(screen(mc),"GuiMusicPlayer");pass("Playlist back button");next();
                }
                case 23 -> {
                    Object list=get(s,"playlistsList");Object row=children(list).get(0);Object play=get(row,"playPlaylistButton");
                    Object playlists=call(manager("getPlaylistManager"),"getPlaylists");
                    if(playlistStage==0){if((Boolean)call(play,"isToggled")){click(mc,play);require(call(playlists,"getPlaying")==null,"Playlist stop button failed");pass("Playlist row stop button");}playlistStage++;waitTicks=0;return;}
                    if(playlistStage==1){if(!(Boolean)get(play,"active"))return;click(mc,play);require(call(playlists,"getPlaying")==playlist,"Playlist play button failed");pass("Playlist row play button");playlistStage++;waitTicks=0;return;}
                    if(playlistStage==2){if(!(Boolean)get(play,"active"))return;click(mc,play);require(call(playlists,"getPlaying")==null,"Playlist stop button failed");pass("Playlist row stop button");playlistStage++;waitTicks=0;return;}
                    if(playlistStage==3){Object filter=get(s,"playlistFilterField");click(mc,filter);call(filter,"setValue","no such GUI playlist");require(children(list).isEmpty(),"Playlist search field did not filter rows");call(filter,"setValue","");require(children(list).size()==1,"Clearing playlist filter did not restore row");pass("Playlist list search field");
                    playlistStage++;waitTicks=0;return;}
                    click(mc,get(children(list).get(0),"deletePlaylistButton"));require(children(list).isEmpty(),"Delete playlist did not remove row");pass("Delete test playlist button");next();
                }
                case 24 -> {snapshot(mc,"main-after-delete");Files.writeString(root.resolve("gui-result.txt"),"PASS\n"+String.join("\n",checks)+"\nOS chooser UI not tested; fixture chooser replies exercised the real file/folder GUI callbacks. Authenticated network providers not tested.\n");next();}
                case 25 -> {finished=true;call(mc,"stop");}
                default -> throw new AssertionError("Unknown step "+phase);
            }
        }catch(Throwable e){fail(e);try{call(mc,"stop");}catch(Exception ignored){}}
    }
    static void next(){phase++;waitTicks=0;}
    static void expectScreen(Object s,String name){require(s!=null&&s.getClass().getSimpleName().equals(name),"Expected "+name+", got "+(s==null?null:s.getClass().getName()));}
    static void require(boolean b,String s){if(!b)throw new AssertionError(s);}
    static void pass(String s){checks.add(s);log("PASS "+s);}
    static double number(Object o){return ((Number)o).doubleValue();}
    static List<?> children(Object o)throws Exception{return (List<?>)call(o,"children");}
    static boolean loaded(Object s,String label)throws Exception{
        return loaded(s,label,1);
    }
    static boolean loaded(Object s,String label,int count)throws Exception{
        if((Boolean)call(get(s,"trackLoader"),"isLoading"))return false;
        List<?> entries=children(get(s,"searchList"));
        require(entries.size()==count,"Expected "+count+" "+label+" results, got "+entries.size()+"; info="+get(s,"information"));
        Object track=call(entries.get(0),"getTrack");require(track!=null,"Loaded result has no track");
        pass(label+" loaded through GUI: "+call(call(track,"getInfo"),"getURI"));return true;
    }
    public static void fixtureDialog(Object callback,long userdata,String path){
        try{
            Class<?> memory=type("org.lwjgl.system.MemoryUtil");
            Object text=call(memory,"memUTF8",path);Object files=call(memory,"memAllocPointer",2);
            call(files,"put",0,call(memory,"memAddress",text));call(files,"put",1,0L);
            try{call(callback,"invoke",userdata,call(files,"address"),0);}finally{call(memory,"memFree",files);call(memory,"memFree",text);}
        }catch(Exception e){throw new RuntimeException("SDL chooser fixture reply failed",e);}
    }
    static boolean sdlInput()throws Exception{try{type("org.lwjgl.sdl.SDLDialog");return true;}catch(ClassNotFoundException e){return false;}}
    static void submit(Object mc,Object s,String value)throws Exception{
        Object edit=get(s,"urlField");click(mc,edit);call(edit,"setValue",value);
        Object current=screen(mc);
        try{call(current,"keyPressed",257,0,0);}catch(NoSuchMethodException e){Class<?> key=type("net.minecraft.client.input.KeyEvent");Object event=key.getConstructor(int.class,int.class,int.class).newInstance(sdlInput()?40:257,0,0);call(current,"keyPressed",event);}
        log("SUBMITTED track URI "+value);
    }
    static void click(Object mc,Object button)throws Exception{
        require((Boolean)get(button,"active"),"Button disabled: "+button.getClass().getName());
        clickAt(mc,number(call(button,"getX"))+number(call(button,"getWidth"))/2,number(call(button,"getY"))+number(call(button,"getHeight"))/2);
    }
    static void clickAt(Object mc,double x,double y)throws Exception{clickAt(mc,x,y,true);}
    static void clickAt(Object mc,double x,double y,boolean requireAccepted)throws Exception{
        Object s=screen(mc);boolean accepted;
        try{accepted=(Boolean)call(s,"mouseClicked",x,y,0);call(s,"mouseReleased",x,y,0);}
        catch(NoSuchMethodException e){
            Class<?> input=type("net.minecraft.client.input.MouseButtonInfo");
            Object info=input.getConstructor(int.class,int.class).newInstance(sdlInput()?1:0,0);
            Class<?> ev=type("net.minecraft.client.input.MouseButtonEvent");
            Object event=ev.getConstructor(double.class,double.class,input).newInstance(x,y,info);
            accepted=(Boolean)call(s,"mouseClicked",event,false);call(s,"mouseReleased",event);
        }
        require(!requireAccepted||accepted,"Screen rejected click at "+x+","+y+" on "+s.getClass().getName());
    }
    static void snapshot(Object mc,String name)throws Exception{
        Object target=renderTarget(mc);Class<?> cls=type("net.minecraft.client.Screenshot");
        Method selected=null;
        for(Method m:cls.getMethods())if(m.getName().equals("grab")&&Modifier.isStatic(m.getModifiers())&&(selected==null||m.getParameterCount()>selected.getParameterCount()))selected=m;
        if(selected==null)throw new NoSuchMethodException("Screenshot.grab");
        Object[] args=new Object[selected.getParameterCount()];
        for(int i=0;i<args.length;i++){
            Class<?> p=selected.getParameterTypes()[i];
            if(p==java.io.File.class)args[i]=root.toFile();else if(p==String.class)args[i]=String.format("%02d-%s.png",phase,name);
            else if(p.isInstance(target))args[i]=target;else if(Consumer.class.isAssignableFrom(p))args[i]=(Consumer<Object>)(c->log("SCREENSHOT "+name));
            else if(p==int.class)args[i]=selected.getParameterTypes()[1]==String.class?1:((Number)get(target,i==1?"width":"height")).intValue();else throw new IllegalArgumentException("Screenshot parameter: "+p);
        }
        selected.invoke(null,args);
    }
    static Object get(Object obj,String name)throws Exception{
        for(Class<?> c=obj.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f.get(obj);}catch(NoSuchFieldException ignored){}
        throw new NoSuchFieldException(obj.getClass().getName()+"."+name);
    }
    static Object call(Object obj,String name,Object...args)throws Exception{
        Class<?> c=obj instanceof Class<?>?(Class<?>)obj:obj.getClass();
        for(Method m:c.getMethods())if(m.getName().equals(name)&&m.getParameterCount()==args.length){
            Object[] converted=args.clone();boolean okay=true;
            for(int i=0;i<args.length;i++){
                Class<?> p=m.getParameterTypes()[i];Object a=args[i];
                if(p==int.class&&a instanceof Number n)converted[i]=n.intValue();else if(p==long.class&&a instanceof Number n)converted[i]=n.longValue();
                else if(p==float.class&&a instanceof Number n)converted[i]=n.floatValue();else if(p==double.class&&a instanceof Number n)converted[i]=n.doubleValue();
                else if(a!=null&&!p.isInstance(a)&&!(p==boolean.class&&a instanceof Boolean)){okay=false;break;}
            }
            if(okay){m.setAccessible(true);try{return m.invoke(obj instanceof Class<?>?null:obj,converted);}catch(InvocationTargetException e){throw new RuntimeException(e.getCause());}}
        }
        throw new NoSuchMethodException(c.getName()+"."+name+Arrays.toString(args));
    }
    static void log(String s){System.out.println("GUI_SCENARIO: "+s);try{Files.createDirectories(root);Files.writeString(root.resolve("gui.log"),s+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception ignored){}}
    static void fail(Throwable e){finished=true;log("FAIL step "+phase+": "+e);e.printStackTrace();try{java.io.StringWriter sw=new java.io.StringWriter();e.printStackTrace(new java.io.PrintWriter(sw));Files.writeString(root.resolve("gui-error.txt"),sw.toString());}catch(Exception ignored){}}
}
