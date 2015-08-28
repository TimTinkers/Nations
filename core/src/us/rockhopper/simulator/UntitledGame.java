package us.rockhopper.simulator;

import us.rockhopper.simulator.screen.PlanetExplore;

import com.badlogic.gdx.Game;

public class UntitledGame extends Game {
	public static final String VERSION = "0.01";
	public static final String TITLE = "Untitled Game";

	@Override
	public void create() {
		setScreen(new PlanetExplore(new Planet(4)));
		// setScreen(new MainMenu());
	}
}
