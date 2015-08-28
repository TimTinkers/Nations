package us.rockhopper.simulator.screen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import us.rockhopper.simulator.Planet;
import us.rockhopper.simulator.Planet.Chunk;
import us.rockhopper.simulator.Planet.EdgePiece;
import us.rockhopper.simulator.Planet.Plate;
import us.rockhopper.simulator.Planet.Tile;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;

public class PlanetExplore extends ScreenAdapter {

	public PlanetExplore(Planet planet) {
		this.tiles = planet.tiles;
		this.adjacencies = planet.adjacencies;
		this.chunks = planet.chunks;
		this.plates = planet.plates;
		this.edges = planet.edges;
	}

	private final int MINIMUM_CHUNK_SIZE = 200;

	public Environment environment;
	public CameraInputController camController;
	public PerspectiveCamera cam;
	public ModelBatch modelBatch;

	public List<EdgePiece> edges = new ArrayList<EdgePiece>();
	public List<Tile> tiles = new ArrayList<Tile>();
	public List<Chunk> chunks = new ArrayList<Chunk>();
	public List<Plate> plates = new ArrayList<Plate>();

	HashMap<Tile, List<Tile>> adjacencies = new HashMap<Tile, List<Tile>>();

	protected Stage stage;
	protected Label label;
	protected BitmapFont font;
	protected StringBuilder stringBuilder;

	private int selected = -1;

	Random random = new Random();
	Color colors[] = { Color.BLUE, Color.GREEN, Color.PINK, Color.RED,
			Color.CYAN, Color.WHITE, Color.YELLOW, Color.ORANGE, Color.GRAY,
			Color.OLIVE, Color.DARK_GRAY, Color.LIGHT_GRAY };

	@Override
	public void show() {
		stage = new Stage();
		font = new BitmapFont();
		label = new Label(" ", new Label.LabelStyle(font, Color.WHITE));
		stage.addActor(label);
		stringBuilder = new StringBuilder();

		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f,
				0.4f, 0.4f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f,
				-0.8f, -0.2f));

		modelBatch = new ModelBatch();

		cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(),
				Gdx.graphics.getHeight());
		cam.position.set(10f, 10f, 10f);
		cam.lookAt(0, 0, 0);
		cam.near = 1f;
		cam.far = 300f;
		cam.update();

		camController = new CameraInputController(cam);

		// Initialize input processing
		InputMultiplexer multiplexer = new InputMultiplexer();
		multiplexer.addProcessor(new InputAdapter() {

			@Override
			public boolean touchDown(int screenX, int screenY, int pointer,
					int button) {
				if (button == Buttons.RIGHT) {
					selected = getObject(screenX, screenY);
					if (selected != -1) {
						for (Chunk ch : chunks) {
							ch.draw();
						}
					}
					return true;
				} else {
					return false;
				}
			}

			@Override
			public boolean keyDown(int keycode) {
				if (keycode == Keys.C) {
					partitionColor();
					drawChunks();
				}
				if (keycode == Keys.F) {
					randomize();
					drawChunks();
				}
				if (keycode == Keys.P) {
					colorPlates();
					drawChunks();
				}
				return true;
			}
		});
		multiplexer.addProcessor(camController);
		Gdx.input.setInputProcessor(multiplexer);

		drawChunks();
	}

	private void drawChunks() {
		for (Chunk c : chunks) {
			c.draw();
		}
	}

	private void colorPlates() {
		for (Plate p : plates) {
			Color color = new Color((float) Math.random(),
					(float) Math.random(), (float) Math.random(), 1f);

			for (Tile t : p.tiles) {
				t.setColor(color);
				for (int i = 0; i < adjacencies.get(t).size(); ++i) {
					Tile adj = adjacencies.get(t).get(i);
					if (!p.tiles.contains(adj)) {
						edges.get(t.edges[i]).setColor(Color.RED);
					}
				}
			}
		}
	}

	private void partitionColor() {
		for (Chunk c : chunks) {
			Color color = colors[random.nextInt(colors.length)];
			for (Tile t : c.tiles) {
				t.setColor(color);
			}
		}
	}

	private void randomize() {
		for (Tile t : tiles) {
			t.setColor(new Color((float) Math.random(), (float) Math.random(),
					(float) Math.random(), 1f));
		}

		// for (EdgePiece e : edges) {
		// e.setColor(new Color((float) Math.random(), (float) Math.random(),
		// (float) Math.random(), 1f));
		// }
	}

	@Override
	public void dispose() {
		modelBatch.dispose();
	}

	int visibleCount;

	@Override
	public void render(float delta) {
		// Update
		cam.update();
		camController.update();

		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(),
				Gdx.graphics.getHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		modelBatch.begin(cam);
		// Render the debug instance
		// modelBatch.render(testInstance, environment);

		// Render all visible chunks
		visibleCount = 0;
		for (Chunk c : chunks) {
			if (chunks.size() < MINIMUM_CHUNK_SIZE || isVisible(cam, c)) {
				modelBatch.render(c.rendered, environment);
				visibleCount++;
			}
		}
		modelBatch.end();

		// Draw diagnostics
		stringBuilder.setLength(0);
		stringBuilder.append(" FPS: ")
				.append(Gdx.graphics.getFramesPerSecond());
		stringBuilder.append(" Chunks visible: ").append(visibleCount);

		label.setText(stringBuilder);
		stage.draw();
	}

	private boolean isVisible(PerspectiveCamera cam, Chunk chunk) {
		// Get the center of the chunk
		Tile center = chunk.tiles.get(0);

		// Close-enough occlusion testing. This works so long as the camera
		// remains fixed to the center of the sphere.
		if (cam.direction.cpy().nor().dot(center.getNormal()) >= 0) {
			return false;
		}

		// Perform frustum culling
		Tile furthest = chunk.tiles.get(chunk.tiles.size() - 1);
		float radius = center.center.dst(furthest.center);
		return cam.frustum.sphereInFrustum(center.center, radius);
	}

	public int getObject(int screenX, int screenY) {
		Ray ray = cam.getPickRay(screenX, screenY);
		int result = -1;
		float distance = -1;
		for (int i = 0; i < tiles.size(); ++i) {
			final Tile instance = tiles.get(i);
			Vector3 position = instance.center;
			float dist2 = ray.origin.dst2(position);
			if (distance >= 0f && dist2 > distance)
				continue;
			if (Intersector.intersectRaySphere(ray, position,
					instance.getRadius(), null)) {
				result = i;
				distance = dist2;
			}
		}
		return result;
	}

	@Override
	public void resume() {
	}

	@Override
	public void resize(int width, int height) {
		stage.getViewport().update(width, height, true);
	}

	@Override
	public void pause() {
	}
}