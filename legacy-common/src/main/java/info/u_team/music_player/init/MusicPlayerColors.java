package info.u_team.music_player.init;

import info.u_team.music_player.util.RGBA;

public class MusicPlayerColors {
	
	public static volatile RGBA GREY = new RGBA(0x555555FF);
	
	public static volatile RGBA GREEN = new RGBA(0x3E9100FF);
	
	public static volatile RGBA YELLOW = new RGBA(0xFFFF00FF);
	
	public static volatile RGBA LIGHT_GREEN = new RGBA(0x80FF00FF);

	public static void apply(int grey, int green, int yellow, int lightGreen) {
		GREY = new RGBA(grey);
		GREEN = new RGBA(green);
		YELLOW = new RGBA(yellow);
		LIGHT_GREEN = new RGBA(lightGreen);
	}
	
}
