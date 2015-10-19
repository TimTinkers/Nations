package us.rockhopper.simulator.screen;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;

import us.rockhopper.simulator.Planet;
import us.rockhopper.simulator.surface.World;

public class SurfaceExplore extends ScreenAdapter {

	Planet planet;
	private World world;
	Model cube;

	protected Stage stage;
	protected Label label;
	protected BitmapFont font;
	protected StringBuilder stringBuilder;

	private PerspectiveCamera camera;
	private Environment environment;
	private List<ModelInstance> cubes = new ArrayList<ModelInstance>();

	ModelBatch modelBatch = new ModelBatch();

	private void setupPlayArea() {
		for (int i = 0; i < world.getMap().getMap().size; i++) {
			for (int j = 0; j < world.getMap().getMap().get(i).length(); j++) {
				ModelInstance c = new ModelInstance(cube);
				c.transform.translate((j) * 2f, -2, i * 2f);
				cubes.add(c);
			}
		}

		for (int i = 0; i < world.getWalls().size; i++) {
			ModelInstance c = new ModelInstance(cube);
			c.transform.translate(world.getWalls().get(i).center.x * 2f, 0, world.getWalls().get(i).center.y * 2f);
			c.transform.scale(world.getWalls().get(i).width, world.getWalls().get(i).height,
					world.getWalls().get(i).width);
			cubes.add(c);
		}
	}

	public SurfaceExplore(Planet planet) {
		this.planet = planet;
		world = new World(planet);
		camera = new PerspectiveCamera(70, 6f, 4f);
		camera.near = 0.01f;
		// camera.direction.set(0, 2, -1);
	}

	private int mouseX = 0;
	private int mouseY = 0;
	private float rotSpeed = 0.5f;
	private float camYaw = 0.0f;
	private boolean cursor = false;
	float camPitch = 0.0f;

	@Override
	public void render(float delta) {
		// Update the world
		world.update(delta);

		// Render play area
		// Position camera on player
		camera.position.set(world.getPlayer().getCentrePos().x * 2f, 0.75f,
				(world.getPlayer().getCentrePos().z * 2f) - 0);
		world.getPlayer().setRotation(camYaw);
		camera.update();
		// camera.rotate(-world.getPlayer().getRotation(), 0, 0, 1);

		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		modelBatch.begin(camera);
		for (ModelInstance m : cubes) {
			modelBatch.render(m, environment);
		}
		modelBatch.end();

		// Draw diagnostics
		stringBuilder.setLength(0);
		stringBuilder.append(" FPS: ").append(Gdx.graphics.getFramesPerSecond());
		stringBuilder.append(" yaw: ").append(camYaw);
		stringBuilder.append(" pitch: ").append(camPitch);
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

		camera.rotate(world.getPlayer().getRotation(), 0, 1, 0);

		// Initialize input processing
		InputMultiplexer multiplexer = new InputMultiplexer();
		multiplexer.addProcessor(new InputAdapter() {
			@Override
			public boolean mouseMoved(int screenX, int screenY) {
				int magX = Math.abs(mouseX - screenX);
				int magY = Math.abs(mouseY - screenY);

				if (mouseX > screenX) {
					float angle = 1 * magX * rotSpeed;
					camera.rotate(Vector3.Y, angle);
					camYaw += angle;
					camera.update();
				}

				if (mouseX < screenX) {
					float angle = -1 * magX * rotSpeed;
					camera.rotate(Vector3.Y, angle);
					camYaw += angle;
					camera.update();
				}

				if (mouseY < screenY) {
					if (camera.up.y >= 0) {
						float angle = -1 * magY * rotSpeed;
						camPitch += angle;
						if (camPitch < -90) {
							camPitch -= angle;
						} else {
							camera.rotate(camera.direction.cpy().crs(Vector3.Y), angle);
						}
					}
					camera.update();
				}

				if (mouseY > screenY) {
					if (camera.up.y >= 0) {
						float angle = 1 * magY * rotSpeed;
						camPitch += angle;
						if (camPitch >= 90) {
							camPitch -= angle;
						} else {
							camera.rotate(camera.direction.cpy().crs(Vector3.Y), angle);
						}
					}
					camera.update();
				}

				mouseX = screenX;
				mouseY = screenY;
				camYaw = camYaw % 360;

				return false;
			}

			@Override
			public boolean keyDown(int keycode) {
				if (keycode == Keys.ESCAPE) {
					cursor = !cursor;
					Gdx.input.setCursorCatched(cursor);
				}
				return true;
			}
		});
		Gdx.input.setInputProcessor(multiplexer);

		// Create our cube
		ModelBuilder mb = new ModelBuilder();
		mb.begin();
		MeshPartBuilder mbp = mb.part("cube", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal,
				new Material(ColorAttribute.createDiffuse(Color.PINK)));
		mbp.box(1, 1, 1);
		cube = mb.end();

		// Setup play area
		setupPlayArea();
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