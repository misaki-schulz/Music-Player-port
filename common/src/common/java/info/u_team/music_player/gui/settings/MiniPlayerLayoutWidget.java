package info.u_team.music_player.gui.settings;

import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.settings.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;

final class MiniPlayerLayoutWidget implements Renderable, GuiEventListener, NarratableEntry {
	private final int screenWidth, screenHeight;
	private int x,y,width,height; private boolean resizing,dragging,focused; private double offsetX,offsetY;
	MiniPlayerLayoutWidget(int screenWidth,int screenHeight){this.screenWidth=screenWidth;this.screenHeight=screenHeight;final Settings s=MusicPlayerManager.getSettingsManager().getSettings();width=Math.clamp(s.getMiniPlayerWidth(),80,Math.max(80,screenWidth-6));height=Math.max(30,Math.round(width*35F/120F));x=s.getMiniPlayerX()<0?(screenWidth-width)/2:Math.clamp(s.getMiniPlayerX(),3,Math.max(3,screenWidth-width-3));y=s.getMiniPlayerY()<0?(screenHeight-height)/2:Math.clamp(s.getMiniPlayerY(),3,Math.max(3,screenHeight-height-3));}
	@Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float pt){g.fill(x,y,x+width,y+height,0xE521262C);g.fill(x,y,x+width,y+1,0xFF75E0B5);g.fill(x,y+height-1,x+width,y+height,0xFF75E0B5);g.text(Minecraft.getInstance().font,getTranslation("gui.layout.track"),x+6,y+5,0xFFFFD166,false);g.text(Minecraft.getInstance().font,getTranslation("gui.layout.artist"),x+8,y+16,0xFFD86D1C,false);g.text(Minecraft.getInstance().font,"|<   >   >|   +1   ☆",x+6,y+height-11,0xFFFFFFFF,false);g.fill(x+width-7,y+height-7,x+width,y+height,0xFFFFCC66);}
	@Override public boolean mouseClicked(MouseButtonEvent e,boolean dc){final Settings s=MusicPlayerManager.getSettingsManager().getSettings();if(e.button()!=0||s.isMiniPlayerLocked()||!isMouseOver(e.x(),e.y()))return false;resizing=e.x()>=x+width-10&&e.y()>=y+height-10;dragging=!resizing;offsetX=e.x()-x;offsetY=e.y()-y;return true;}
	@Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy){if(e.button()!=0)return false;if(resizing){width=Math.clamp((int)e.x()-x,80,Math.min(640,screenWidth-x-3));height=Math.max(30,Math.round(width*35F/120F));return true;}if(dragging){x=Math.clamp((int)(e.x()-offsetX),3,Math.max(3,screenWidth-width-3));y=Math.clamp((int)(e.y()-offsetY),3,Math.max(3,screenHeight-height-3));return true;}return false;}
	@Override public boolean mouseReleased(MouseButtonEvent e){if(e.button()!=0||(!dragging&&!resizing))return false;final Settings s=MusicPlayerManager.getSettingsManager().getSettings();if(s.isMiniPlayerSnapToEdges()){if(x<16)x=3;if(y<16)y=3;if(screenWidth-x-width<16)x=screenWidth-width-3;if(screenHeight-y-height<16)y=screenHeight-height-3;}s.setMiniPlayerLayout(x,y,width);dragging=false;resizing=false;return true;}
	@Override public boolean isMouseOver(double mx,double my){return mx>=x&&mx<x+width&&my>=y&&my<y+height;}@Override public void setFocused(boolean v){focused=v;}@Override public boolean isFocused(){return focused;}@Override public NarrationPriority narrationPriority(){return focused?NarrationPriority.FOCUSED:NarrationPriority.NONE;}@Override public void updateNarration(NarrationElementOutput o){}
}
