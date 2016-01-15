package us.rockhopper.simulator;

import com.badlogic.gdx.Game;

import us.rockhopper.simulator.screen.MainMenu;

public class UntitledGame extends Game {
	public static final String VERSION = "0.01";
	public static final String TITLE = "Untitled Game";

	public static final int WIDTH = 480;
	public static final int HEIGHT = 320;

	@Override
	public void create() {
		// setScreen(new SurfaceExplore(new Planet(4)));
		//setScreen(new PlanetExplore(new Planet(5, 10, new Vector3(0, 0, 0))));

		setScreen(new MainMenu());
	}
}
