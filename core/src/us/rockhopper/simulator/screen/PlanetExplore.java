package us.rockhopper.simulator.screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

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

import us.rockhopper.simulator.Planet;
import us.rockhopper.simulator.Planet.Chunk;
import us.rockhopper.simulator.Planet.EdgePiece;
import us.rockhopper.simulator.Planet.Plate;
import us.rockhopper.simulator.Planet.Tile;
import us.rockhopper.simulator.util.Utility;

public class PlanetExplore extends ScreenAdapter {

	ArrayList<Planet> planets;

	public PlanetExplore(Planet... planets) {
		this.planets = new ArrayList<Planet>();
		for (Planet p : planets) {
			this.planets.add(p);
		}
		// this.tiles = planet.tiles;
		// this.adjacencies = planet.adjacencies;
		// this.chunks = planet.chunks;
		// this.plates = planet.plates;
		// this.edges = planet.edges;
	}

	private final int MINIMUM_CHUNK_SIZE = 200;
	private boolean PLATE_DEBUG = false;
	private boolean DRAW_TILES = true;

	public Environment environment;
	public CameraInputController camController;
	public PerspectiveCamera cam;
	public ModelBatch modelBatch;

	// public List<Tile> tiles = new ArrayList<Tile>();
	// HashMap<Tile, List<Tile>> adjacencies = new HashMap<Tile, List<Tile>>();
	// public List<Chunk> chunks = new ArrayList<Chunk>();
	// public List<Plate> plates = new ArrayList<Plate>();
	// public List<EdgePiece> edges = new ArrayList<EdgePiece>();

	protected Stage stage;
	protected Label label;
	protected BitmapFont font;
	protected StringBuilder stringBuilder;

	private int selected = -1;

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

			// TODO: implement the concept of an "active planet" -- the planet
			// which is being focused on. This is the one the user can interact
			// with. Currently only the first planet is active.
			@Override
			public boolean mouseMoved(int screenX, int screenY) {
				selected = getObject(screenX, screenY);
				if (selected != -1) {
					// Get the tile selected and set the display elevation
					Tile t = planets.get(0).tiles.get(selected);
					mouseElevation = t.getElevation();
				}
				return false;
			}

			@Override
			public boolean touchDown(int screenX, int screenY, int pointer, int button) {
				if (button == Buttons.RIGHT) {
					selected = getObject(screenX, screenY);
					if (selected != -1) {
						// Get the tile selected
						Tile t = planets.get(0).tiles.get(selected);
						t.setColor(Color.BLUE);

						// Update the rendering
						for (Chunk ch : planets.get(0).chunks) {
							if (ch.tiles.contains(t)) {
								ch.draw();
								break;
							}
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
				if (keycode == Keys.D) {
					// Toggle plate debug.
					PLATE_DEBUG = !PLATE_DEBUG;
					drawPlates();
				}
				if (keycode == Keys.H) {
					// Hide tiles.
					DRAW_TILES = !DRAW_TILES;
				}
				if (keycode == Keys.E) {
					colorElevation();
					drawChunks();
				}
				if (keycode == Keys.B) {
					hideBorders();
					drawChunks();
				}
				if (keycode == Keys.T) {
					colorBorders();
					drawChunks();
				}
				// View focus
				if (keycode == Keys.L) {
					cam.position.set(planets.get(0).ORIGIN.x + 10f, planets.get(0).ORIGIN.y + 10f,
							planets.get(0).ORIGIN.z + 10f);
					camView = planets.get(0).ORIGIN;
					// cam.lookAt(planets.get(0).ORIGIN);
					cam.update();
					// camController = new CameraInputController(cam);
				}
				return true;
			}
		});
		multiplexer.addProcessor(camController);
		Gdx.input.setInputProcessor(multiplexer);

		drawChunks();
	}

	private void drawChunks() {
		for (Planet p : planets) {
			for (Chunk c : p.chunks) {
				c.draw();
			}
		}
	}

	private void drawPlates() {
		for (Planet p : planets) {
			for (Plate plate : p.plates) {
				plate.drawDebug();
			}
		}
	}

	private void colorBorders() {
		// Get all tiles in all plates of all planets
		for (Planet planet : planets) {
			for (Plate plate : planet.plates) {
				List<Integer> plateTileIDs = plate.tiles;

				// For every tile in the plate, find its adjacent tiles
				for (int tileID : plateTileIDs) {
					Tile tile = planet.tiles.get(tileID);
					List<Integer> tileAdjacencies = planet.adjacencies.get(tileID);

					// For every adjacent tile not in the same plate
					for (int i = 0; i < tileAdjacencies.size(); ++i) {
						int adjID = tileAdjacencies.get(i);
						if (!plateTileIDs.contains(adjID)) {
							Tile adj = planet.tiles.get(adjID);
							
							// Get the tectonic force between the two tiles
							Vector3 forceA = tile.getTectonicDirection();
							Vector3 forceB = adj.getTectonicDirection();

							Vector3 midLineA = adj.center.cpy().sub(tile.center.cpy());
							Vector3 midLineB = tile.center.cpy().sub(adj.center.cpy());

							float magA = (forceA.dot(midLineA.cpy()) / midLineA.cpy().nor().len());
							float magB = (forceB.dot(midLineB.cpy()) / midLineB.cpy().nor().len());
							float combMag = (magA + magB);
							// System.out.println(forceA + " " + forceB + " x "
							// + magA + " " + magB + " = " + combMag);

							// Set the edge color to the tectonic force
							int edgeID = tile.edges[i];
							EdgePiece edge = planet.edges.get(edgeID);

							// Clamp magnitude.
							if (combMag > 2f) {
								combMag = 2f;
							}
							if (combMag < -2) {
								combMag = -2;
							}

							// Set color of border: mountains green, rifts red
							Color color = Utility.colorFromHSV((combMag - 2) * -0.1f, 0.9f, 0.9f, 1);
							edge.setColor(color);

							// Change the elevation based on collision.
							String typeA = tile.getType();
							String typeB = adj.getType();
							if (typeA.equals("land") && typeB.equals("land") && combMag > 0) {
								float elevationScale = (combMag / 2f);
								float heightA = tile.getElevation() + (elevationScale * planet.MAX_HEIGHT);
								float heightB = tile.getElevation() + (elevationScale * planet.MAX_HEIGHT);
								tile.setElevation(heightA);
								adj.setElevation(heightB);
							}
						}
					}
				}

				if (plate.type.equals("land")) {
					plate.smooth();
					// return;
				}
			}
		}
	}

	private void colorElevation() {
		for (Planet p : planets) {

			for (Tile t : p.tiles) {
				// Define the color based on type and elevation.
				Color elevationColor = new Color(0f, 0f, 0f, 1f);
				String type = t.getType();
				float elevation = t.getElevation();
				if (elevation >= 0) { // Land
					float elevationScale = elevation / p.MAX_HEIGHT;
					elevationColor = new Color((109 + 62 * elevationScale) / 255f, (97 + 80 * elevationScale) / 255f,
							(57 + 75 * elevationScale) / 255f, 1f);
				} else if (elevation < 0) { // Below sea level
					float elevationScale = elevation / p.MAX_DEPTH;
					elevationColor = new Color((62 + 73 * elevationScale) / 255f, (72 + 74 * elevationScale) / 255f,
							(102 + 80 * elevationScale) / 255f, 1f);
				}
				t.setColor(elevationColor);
			}

			// Color in all plates.
			// for (Plate plate : p.plates) {
			// int elevation = plate.elevation;
			// int lowElevation =
			//
			// // Define the color based on type and elevation.
			// Color elevationColor = new Color(0f, 0f, 0f, 1f);
			// String type = plate.type;
			// if (type.equals("land")) {
			// float elevationScale = elevation / 2000f;
			// elevationColor = new Color((109 + 62 * elevationScale) / 255f,
			// (97 + 80 * elevationScale) / 255f,
			// (57 + 75 * elevationScale) / 255f, 1f);
			// } else if (type.equals("ocean")) {
			// float elevationScale = elevation / -4000f;
			// elevationColor = new Color((62 + 73 * elevationScale) / 255f, (72
			// + 74 * elevationScale) / 255f,
			// (102 + 80 * elevationScale) / 255f, 1f);
			// }
			//
			// System.out.println(elevationColor.toString());
			//
			// for (Tile t : plate.tiles) {
			// t.setColor(elevationColor);
			// for (int i = 0; i < p.adjacencies.get(t).size(); ++i) {
			// Tile adj = p.adjacencies.get(t).get(i);
			// if (!plate.tiles.contains(adj)) {
			// p.edges.get(t.edges[i]).setColor(Color.RED);
			// }
			// }
			// }
			//
			// // plate.tiles.get(0).setColor(Color.WHITE);
			// }
		}
	}

	private void hideBorders() {
		// Color in all edges.
		for (Planet planet : planets) {
			for (Tile tile : planet.tiles) {
				int[] tileEdgeIDs = tile.edges;
				for (int i = 0; i < tileEdgeIDs.length; ++i) {
					int edgeID = tile.edges[i];
					planet.edges.get(edgeID).setColor(tile.getColor());
				}
			}
		}
	}

	private void colorPlates() {
		for (Planet p : planets) {
			// Color in all plates.
			for (Plate plate : p.plates) {
				Color color = new Color((float) Math.random(), (float) Math.random(), (float) Math.random(), 1f);

				for (int tID : plate.tiles) {
					Tile t = p.tiles.get(tID);
					t.setColor(color);
					for (int i = 0; i < p.adjacencies.get(t).size(); ++i) {
						int adjID = p.adjacencies.get(t).get(i);
						if (!plate.tiles.contains(adjID)) {
							p.edges.get(t.edges[i]).setColor(Color.RED);
						}
					}
				}

				int centerID = plate.tiles.get(0);
				p.tiles.get(centerID).setColor(Color.WHITE);
			}
		}
	}

	private void partitionColor() {
		for (Planet p : planets) {
			for (Chunk c : p.chunks) {
				Color color = colors[random.nextInt(colors.length)];
				for (int tID : c.tiles) {
					Tile t = p.tiles.get(tID);
					t.setColor(color);
				}
			}
		}
	}

	private void randomize() {
		for (Planet p : planets) {
			for (Tile t : p.tiles) {
				t.setColor(new Color((float) Math.random(), (float) Math.random(), (float) Math.random(), 1f));
			}
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
	float mouseElevation = 0;
	Vector3 camView = new Vector3(0, 0, 0);

	@Override
	public void render(float delta) {

		// Update
		cam.lookAt(camView);
		cam.update();
		camController.update();

		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		modelBatch.begin(cam);
		// Render the debug instance
		// modelBatch.render(testInstance, environment);

		// Render all visible chunks
		visibleCount = 0;
		for (Planet p : planets) {
			if (DRAW_TILES) {
				for (Chunk c : p.chunks) {
					if (p.chunks.size() < MINIMUM_CHUNK_SIZE || isVisible(cam, c)) {
						modelBatch.render(c.rendered, environment);
						visibleCount++;
					}
				}
			}

			// Draw plate debugs if on
			if (PLATE_DEBUG) {
				for (Plate plate : p.plates) {
					modelBatch.render(plate.debug, environment);
				}
			}
		}
		modelBatch.end();

		// Draw diagnostics
		stringBuilder.setLength(0);
		stringBuilder.append(" FPS: ").append(Gdx.graphics.getFramesPerSecond());
		stringBuilder.append(" Chunks visible: ").append(visibleCount);
		stringBuilder.append(" Tile elevation: ").append(mouseElevation);

		label.setText(stringBuilder);
		stage.draw();
	}

	// TODO: work with the focused planet--non-focused planets are rendered in
	// some lower LOD
	private boolean isVisible(PerspectiveCamera cam, Chunk chunk) {
		// Get the center of the chunk
		Planet p = chunk.planet;
		int centerID = chunk.tiles.get(0);
		Tile center = p.tiles.get(centerID);
		
		// Close-enough occlusion testing. This works so long as the camera
		// remains fixed to the center of the sphere.
		if (cam.direction.cpy().nor().dot(center.getNormal()) >= 0) {
			return false;
		}

		// Perform frustum culling
		int furthestID = chunk.tiles.get(chunk.tiles.size() - 1);
		Tile furthest = p.tiles.get(furthestID);
		float radius = center.center.dst(furthest.center);
		return cam.frustum.sphereInFrustum(center.center, radius);
	}

	public int getObject(int screenX, int screenY) {
		Ray ray = cam.getPickRay(screenX, screenY);
		int result = -1;
		float distance = -1;
		for (int i = 0; i < planets.get(0).tiles.size(); ++i) {
			final Tile instance = planets.get(0).tiles.get(i);
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