package info.u_team.music_player.init;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import info.u_team.music_player.musicplayer.YoutubeLoginCoordinator;
public final class NativeClientEvents {
 private NativeClientEvents() {}
 public static void register(FMLJavaModLoadingContext context) {
  final var modBus = context.getModBusGroup();
  FMLClientSetupEvent.getBus(modBus).addListener(event -> event.enqueueWork(MusicPlayerClientConstruct::construct));
  net.minecraftforge.client.event.RegisterKeyMappingsEvent.BUS.addListener(MusicPlayerKeys::register);
  net.minecraftforge.client.event.AddGuiOverlayLayersEvent.BUS.addListener(event -> event.getLayeredDraw().add(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("musicplayer", "player_overlay"), (graphics, delta) -> { if (MusicPlayerClientConstruct.isReady()) MusicPlayerEventHandler.onRenderGameOverlay(graphics, delta); }));
  net.minecraftforge.event.TickEvent.ClientTickEvent.Post.BUS.addListener(NativeClientEvents::tick);
  ScreenEvent.Init.Pre.BUS.addListener(NativeClientEvents::beforeInit);
  ScreenEvent.Init.Post.BUS.addListener(NativeClientEvents::afterInit);
  ScreenEvent.Render.Post.BUS.addListener(NativeClientEvents::render);
  ScreenEvent.KeyPressed.Post.BUS.addListener(NativeClientEvents::key);
  ScreenEvent.MouseButtonReleased.Pre.BUS.addListener(NativeClientEvents::mouse);
  net.minecraftforge.event.GameShuttingDownEvent.BUS.addListener(event -> MusicPlayerClientConstruct.shutdown());
 }
 private static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent.Post event) {
  
  if (!MusicPlayerClientConstruct.isReady()) return;
  final Minecraft client = Minecraft.getInstance();
  YoutubeLoginCoordinator.showStartupPrompt(client);
  final Screen screen = client.screen;
  if (screen == null) MusicPlayerEventHandler.onKeyInput();
  else if (screen instanceof PauseScreen) MusicPlayerEventHandler.onClientTick(screen);
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
  if (MusicPlayerClientConstruct.isReady()) MusicPlayerEventHandler.onKeyboardPressed(event.getScreen(), event.getInfo());
 }
 private static void mouse(ScreenEvent.MouseButtonReleased.Pre event) {
  if (MusicPlayerClientConstruct.isReady() && event.getScreen() instanceof PauseScreen) MusicPlayerEventHandler.onMouseReleasePre(event.getScreen(), new net.minecraft.client.input.MouseButtonEvent(event.getMouseX(), event.getMouseY(), new net.minecraft.client.input.MouseButtonInfo(event.getButton(), 0)));
 }
}
