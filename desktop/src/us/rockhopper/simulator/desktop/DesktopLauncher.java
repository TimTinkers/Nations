package us.rockhopper.simulator.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

import us.rockhopper.simulator.UntitledGame;

public class DesktopLauncher {
	public static void main(String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.vSyncEnabled = true; // Setting to false disables vertical sync
		config.foregroundFPS = 0; // Setting to 0 disables foreground fps
									// throttling
		config.backgroundFPS = 0; // Setting to 0 disables background fps
									// throttling
		new LwjglApplication(new UntitledGame(), config);
	}
}
