package us.rockhopper.simulator.desktop;

import us.rockhopper.simulator.UntitledGame;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

public class DesktopLauncher {
	public static void main(String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.vSyncEnabled = false; // Setting to false disables vertical sync
		config.foregroundFPS = 0; // Setting to 0 disables foreground fps
									// throttling
		config.backgroundFPS = 0; // Setting to 0 disables background fps
									// throttling
		new LwjglApplication(new UntitledGame(), config);
	}
}