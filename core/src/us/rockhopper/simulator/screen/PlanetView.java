package us.rockhopper.simulator.screen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import us.rockhopper.simulator.Planet;
import us.rockhopper.simulator.Planet.Chunk;
import us.rockhopper.simulator.Planet.Tile;
import us.rockhopper.simulator.network.MultiplayerClient;
import us.rockhopper.simulator.network.Packet.Packet0TileChange;
import us.rockhopper.simulator.network.Packet.Packet1ScoreUpdate;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
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
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

public class PlanetView extends ScreenAdapter {

	MultiplayerClient client;

	public PlanetView(Planet planet, MultiplayerClient client) {
		this.tiles = planet.tiles;
		this.adjacencies = planet.adjacencies;
		this.chunks = planet.chunks;
		this.client = client;
		this.planet = planet;
	}

	private final int MINIMUM_CHUNK_SIZE = 200;

	public Environment environment;
	public CameraInputController camController;
	public PerspectiveCamera cam;
	public ModelBatch modelBatch;

	Planet planet;

	public List<Tile> tiles = new ArrayList<Tile>();
	public List<Chunk> chunks = new ArrayList<Chunk>();
	HashMap<Tile, List<Tile>> adjacencies = new HashMap<Tile, List<Tile>>();

	HashMap<String, Integer> playerColors = new HashMap<String, Integer>();

	HashMap<String, Integer> playerScores = new HashMap<String, Integer>();

	protected Stage stage;
	protected Label label;
	protected BitmapFont font;
	protected StringBuilder stringBuilder;

	private int selected = -1;
	private int activeTile = -1;

	Random random = new Random();
	Color colors[] = { Color.BLUE, Color.GREEN, Color.PINK, Color.RED, Color.CYAN, Color.WHITE, Color.YELLOW,
			Color.ORANGE, Color.GRAY, Color.OLIVE, Color.DARK_GRAY, Color.LIGHT_GRAY };

	@Override
	public void show() {
		stage = new Stage();
		font = new BitmapFont();
		label = new Label(" ", new Label.LabelStyle(font, Color.WHITE));
		stage.addActor(label);
		stringBuilder = new StringBuilder();

		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

		modelBatch = new ModelBatch();

		cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
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
			public boolean touchDown(int screenX, int screenY, int pointer, int button) {
				if (button == Buttons.RIGHT) {
					selected = getObject(screenX, screenY);
					if (selected != -1) {
						client.sendTile(client.user, selected);
						getChunkFromTileID(selected).draw();
					}
					return true;
				} else {
					return false;
				}
			}

			@Override
			public boolean touchDragged(int x, int y, int pointer) {
				selected = getObject(x, y);
				if (selected != -1 && selected != activeTile) {

					// Add select color
					Tile t = planet.tiles.get(selected);
					for (int i = 0; i < adjacencies.get(t).size(); ++i) {
						planet.edges.get(t.edges[i]).setColor(Color.YELLOW);
					}
					getChunkFromTileID(selected).draw();

					if (activeTile != -1) {
						// Remove previous select color
						Tile p = planet.tiles.get(activeTile);
						for (int i = 0; i < adjacencies.get(p).size(); ++i) {
							planet.edges.get(p.edges[i]).setColor(Color.BLACK);
						}
						getChunkFromTileID(activeTile).draw();
					}

					activeTile = selected;
				}
				return false;
			}

			@Override
			public boolean mouseMoved(int x, int y) {
				selected = getObject(x, y);
				if (selected != -1 && selected != activeTile) {

					// Add select color
					Tile t = planet.tiles.get(selected);
					for (int i = 0; i < adjacencies.get(t).size(); ++i) {
						planet.edges.get(t.edges[i]).setColor(Color.YELLOW);
					}
					getChunkFromTileID(selected).draw();

					if (activeTile != -1) {
						// Remove previous select color
						Tile p = planet.tiles.get(activeTile);
						for (int i = 0; i < adjacencies.get(p).size(); ++i) {
							planet.edges.get(p.edges[i]).setColor(Color.BLACK);
						}
						getChunkFromTileID(activeTile).draw();
					}

					activeTile = selected;
				}
				return false;
			}
		});
		multiplexer.addProcessor(camController);
		Gdx.input.setInputProcessor(multiplexer);

		drawChunks();

		// Client listeners
		this.client.addListener(new Listener() {

			@Override
			public void received(Connection c, Object o) {
				if (o instanceof Packet0TileChange) {
					Packet0TileChange packet = (Packet0TileChange) o;
					String name = packet.playerID;

					if (name == null || name.equals("SERVER")) {
						tiles.get(packet.tileID).setColor(Color.WHITE);
					} else if (name.equals("HOLE")) {
						tiles.get(packet.tileID).setColor(Color.BLACK);
					} else {
						if (playerColors.containsKey(name)) {
							tiles.get(packet.tileID).setColor(colors[playerColors.get(name)]);
						} else {
							playerColors.put(name, playerColors.size());
							tiles.get(packet.tileID).setColor(colors[playerColors.get(name)]);
						}
					}

					for (Chunk ch : chunks) {
						ch.draw();
					}
				} else if (o instanceof Packet1ScoreUpdate) {
					Packet1ScoreUpdate packet = (Packet1ScoreUpdate) o;
					String name = packet.playerID;
					int score = packet.newScore;
					playerScores.put(name, score);
				}
			}
		});

		// Default colors
		defaultColor();
	}

	private void drawChunks() {
		for (Chunk c : chunks) {
			c.draw();
		}
	}

	private Chunk getChunkFromTileID(int ID) {
		Tile t = planet.tiles.get(ID);
		for (Chunk c : chunks) {
			if (c.tiles.contains(t)) {
				return c;
			}
		}

		// Returns null if there is no tile with this ID.
		return null;
	}

	private void defaultColor() {
		for (Chunk c : chunks) {
			for (Tile t : c.tiles) {
				t.setColor(Color.GRAY);
				for (int i = 0; i < adjacencies.get(t).size(); ++i) {
					planet.edges.get(t.edges[i]).setColor(Color.BLACK);
				}
			}
		}
		drawChunks();
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

		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
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
		stringBuilder.append(" FPS: ").append(Gdx.graphics.getFramesPerSecond());
		stringBuilder.append(" Chunks visible: ").append(visibleCount);

		for (String player : playerScores.keySet()) {
			int score = playerScores.get(player);
			stringBuilder.append(" ").append(player).append(" ").append(score);
		}

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
			if (Intersector.intersectRaySphere(ray, position, instance.getRadius(), null)) {
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