package info.u_team.music_player.init;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import info.u_team.music_player.musicplayer.YoutubeLoginCoordinator;
public final class NativeClientEvents {
 private NativeClientEvents() {}
 public static void register(IEventBus modBus) {
  modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(MusicPlayerClientConstruct::construct));
  modBus.addListener(MusicPlayerKeys::register);
  
  final IEventBus bus = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
  bus.addListener(NativeClientEvents::tick);
  bus.addListener(NativeClientEvents::hud);
  bus.addListener(NativeClientEvents::beforeInit);
  bus.addListener(NativeClientEvents::afterInit);
  bus.addListener(NativeClientEvents::render);
  bus.addListener(NativeClientEvents::key);
  bus.addListener(NativeClientEvents::mouse);
  bus.addListener((net.neoforged.neoforge.event.GameShuttingDownEvent event) -> MusicPlayerClientConstruct.shutdown());
 }
 private static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
  
  if (!MusicPlayerClientConstruct.isReady()) return;
  final Minecraft client = Minecraft.getInstance();
  YoutubeLoginCoordinator.showStartupPrompt(client);
  final Screen screen = client.gui.screen();
  if (screen == null) MusicPlayerEventHandler.onKeyInput();
  else if (screen instanceof PauseScreen) MusicPlayerEventHandler.onClientTick(screen);
 }
 private static void hud(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event) {
  if (MusicPlayerClientConstruct.isReady()) MusicPlayerEventHandler.onRenderGameOverlay(event.getGuiGraphics(), event.getPartialTick());
 }
 private static void beforeInit(ScreenEvent.Init.Pre event) {
  if (MusicPlayerClientConstruct.isReady()) MusicPlayerEventHandler.onPreInitScreen(Minecraft.getInstance(), event.getScreen(), event.getScreen().width, event.getScreen().height);
 }
 private static void afterInit(ScreenEvent.Init.Post event) {
  if (MusicPlayerClientConstruct.isReady()) MusicPlayerEventHandler.onPostInitScreen(Minecraft.getInstance(), event.getScreen(), event.getScreen().width, event.getScreen().height);
 }
 private static void render(ScreenEvent.Render.Post event) {
  if (MusicPlayerClientConstruct.isReady() && event.getScreen() instanceof PauseScreen) MusicPlayerEventHandler.onDrawScreenPost(event.getScreen(), event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
 }
 private static void key(ScreenEvent.KeyPressed.Post event) {
  if (MusicPlayerClientConstruct.isReady()) MusicPlayerEventHandler.onKeyboardPressed(event.getScreen(), event.getKeyEvent());
 }
 private static void mouse(ScreenEvent.MouseButtonReleased.Pre event) {
  if (MusicPlayerClientConstruct.isReady() && event.getScreen() instanceof PauseScreen) MusicPlayerEventHandler.onMouseReleasePre(event.getScreen(), event.getMouseButtonEvent());
 }
}
