package us.rockhopper.simulator.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;

import us.rockhopper.simulator.Planet;
import us.rockhopper.simulator.surface.Hex;
import us.rockhopper.simulator.surface.Player;
import us.rockhopper.simulator.surface.World;

public class SurfaceExplore extends ScreenAdapter {

	// Global objects
	Planet planet;
	private World world;
	private Player player;

	// View
	protected Stage stage;
	protected Label label;
	protected BitmapFont font;
	protected StringBuilder stringBuilder;
	private boolean cursor = true;

	// Rendering
	private Environment environment;
	ModelBatch modelBatch = new ModelBatch();

	public SurfaceExplore(Planet planet) {
		// Create the world
		this.planet = planet;
		world = new World(planet);
		for (Hex h : world.getHexes()) {
			h.draw();
			modelBatch.render(h.rendered, environment);
		}

		// Setup the player
		this.player = new Player();
	}

	@Override
	public void render(float delta) {
		// Update
		// camera.lookAt(camView);
		// camera.update();
		this.player.update();

		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		modelBatch.begin(player.camera);
		// Render hexels
		for (Hex h : world.getHexes()) {
			modelBatch.render(h.rendered, environment);
		}
		modelBatch.end();

		// Draw diagnostics
		stringBuilder.setLength(0);
		stringBuilder.append(" FPS: ").append(Gdx.graphics.getFramesPerSecond());
		stringBuilder.append(" pos: ").append(player.getPosition().toString());
		label.setText(stringBuilder);
		stage.draw();
	}

	@Override
	public void resize(int width, int height) {
	}

	@Override
	public void show() {
		// Catch the cursor
		Gdx.input.setCursorCatched(!cursor);

		// Intialize HUD
		stage = new Stage();
		font = new BitmapFont();
		label = new Label(" ", new Label.LabelStyle(font, Color.WHITE));
		stage.addActor(label);
		stringBuilder = new StringBuilder();

		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
	}

	@Override
	public void hide() {
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void dispose() {
	}
}