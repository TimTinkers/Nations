package us.rockhopper.simulator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Floats;

import us.rockhopper.simulator.util.Utility;

public class Planet {
	// implements ApplicationListener {

	private final float SCALE;
	private final float TAO = 1.61803399f;
	private final float RADIUS;

	// private final float borderThickness = 0.1f;

	public final long SEED;
	private final int THREADS = 8;
	public final int SUBDIVISIONS;
	private final int[] CHUNK_DIVISIONS = { 1, 1, 1, 12, 42, 162, 642, 642, 2562 };

	public Vector3 ORIGIN = new Vector3(0, 0, 0);
	public final int MAX_HEIGHT = 2000;
	public final int MAX_DEPTH = 4000;
	public final float SMOOTHING_FACTOR = 2f;

	Random random;
	Color colors[] = { Color.BLUE, Color.GREEN, Color.PINK, Color.RED, Color.CYAN, Color.WHITE, Color.YELLOW,
			Color.ORANGE, Color.GRAY, Color.OLIVE, Color.DARK_GRAY, Color.LIGHT_GRAY };

	public List<Vector3> vertices = new ArrayList<Vector3>();
	public List<Triangle> triangles = new ArrayList<Triangle>();

	public List<Tile> tiles = new ArrayList<Tile>();
	public List<EdgePiece> edges = new ArrayList<EdgePiece>();

	public List<Vector3> pentPoints = new ArrayList<Vector3>();

	public HashMap<Integer, List<Integer>> adjacencies = new HashMap<Integer, List<Integer>>();

	public List<Chunk> chunks = new ArrayList<Chunk>();

	public List<Plate> plates = new ArrayList<Plate>();

	private ModelBuilder mb = new ModelBuilder();

	public Planet(int subdivisions) {
		this.SCALE = 1f;
		this.RADIUS = SCALE * 2f * 0.95105f;
		this.SUBDIVISIONS = subdivisions;
		this.SEED = 0;
		this.random = new Random(SEED);
		createPlanet();
	}

	public Planet(int subdivisions, long seed, float scale, Vector3 origin) {
		this.ORIGIN = origin;
		this.SCALE = scale;
		this.RADIUS = SCALE * 2f * 0.95105f;
		this.SUBDIVISIONS = subdivisions;
		this.SEED = seed;
		this.random = new Random(SEED);
		createPlanet();
	}

	public Planet(int subdivisions, long seed) {
		this.SCALE = 1f;
		this.RADIUS = SCALE * 2f * 0.95105f;
		this.SUBDIVISIONS = subdivisions;
		this.random = new Random(seed);
		this.SEED = seed;
		createPlanet();
	}

	public class Plate {
		public List<Integer> tiles;
		public Vector3 axis;
		public float rotationAngle;
		public String type;
		public int elevation;
		public ModelInstance debug;

		Plate(Vector3 axis, float rotationAngle, String type, int elevation) {
			this.axis = axis;
			this.rotationAngle = rotationAngle;
			this.type = type;
			this.elevation = elevation;
			tiles = new ArrayList<Integer>();
		}

		public void calculateTileTectonics() {
			for (int tID : tiles) {
				Tile t = Planet.this.tiles.get(tID);
				Vector3 tileCenter = t.center;
				Vector3 tileNormal = t.getNormal();

				// Calculate the plate movement vector around the axis.
				float u = axis.x;
				float v = axis.y;
				float w = axis.z;
				float x = tileCenter.x;
				float y = tileCenter.y;
				float z = tileCenter.z;
				float a = ORIGIN.x;
				float b = ORIGIN.y;
				float c = ORIGIN.z;
				float L = u * u + v * v + w * w;
				float oneMinusCosTheta = (1 - (float) Math.cos(rotationAngle));
				float rootLSinTheta = (float) (Math.sqrt(L) * Math.sin(rotationAngle));
				float LxCosTheta = (float) (L * x * Math.cos(rotationAngle));
				float LyCosTheta = (float) (L * y * Math.cos(rotationAngle));
				float LzCosTheta = (float) (L * z * Math.cos(rotationAngle));
				float rotatedX = (float) (((a * (v * v + w * w) - u * (b * v + c * w - u * x - v * y - w * z))
						* oneMinusCosTheta + LxCosTheta + (rootLSinTheta * (-1 * c * v + b * w - w * y + v * z))) / L);
				float rotatedY = (float) (((b * (u * u + w * w) - v * (a * u + c * w - u * x - v * y - w * z))
						* oneMinusCosTheta + LyCosTheta + (rootLSinTheta * (c * u - a * w + w * x - u * z))) / L);
				float rotatedZ = (float) (((c * (u * u + v * v) - w * (a * u + b * v - u * x - v * y - w * z))
						* oneMinusCosTheta + LzCosTheta + (rootLSinTheta * (-1 * b * u + a * v - v * x + u * y))) / L);
				Vector3 rotatedVector = new Vector3(rotatedX, rotatedY, rotatedZ);

				// Project the plate movement onto the tiles:
				// proj_v = v - n(v dot n).
				Vector3 rSource = tileCenter.cpy().sub(rotatedVector).nor();
				float dot = rSource.cpy().dot(tileNormal);
				Vector3 rScale = tileNormal.cpy().scl(dot);
				Vector3 projection = rSource.cpy().sub(rScale);
				Vector3 r = tileCenter.cpy().add(projection);
				t.setTectonicDirection(r);
			}
		}

		public void drawDebug() {
			// Draw plate indicators for each plate.
			mb.begin();
			MeshPartBuilder mpbP = mb.part("plate", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal,
					new Material(ColorAttribute.createDiffuse(Color.PINK)));

			// For every tile in the chunk...
			for (int tID : tiles) {
				if (tiles.contains(tID)) {

					Tile t = Planet.this.tiles.get(tID);

					// Project the normal of each face.
					Vector3 tileCenter = t.center.cpy();
					Vector3 tileNormal = t.getNormal();
					Vector3 r = t.getTectonicDirection();
					mpbP.arrow(tileCenter.x + (tileNormal.x / 10), tileCenter.y + (tileNormal.y / 10),
							tileCenter.z + (tileNormal.z / 10), r.x + (tileNormal.x / 10), r.y + (tileNormal.y / 10),
							r.z + (tileNormal.z / 10), 0.2f, 0.2f, 4);
				}
			}

			debug = new ModelInstance(mb.end());
		}

		public void addTile(int tID) {
			tiles.add(tID);
		}

		public void smooth() {
			PriorityQueue<Integer> tileQueue = new PriorityQueue<Integer>(new Comparator<Integer>() {

				@Override
				public int compare(Integer xID, Integer yID) {
					Tile x = Planet.this.tiles.get(xID);
					Tile y = Planet.this.tiles.get(yID);
					if (x.getElevation() < y.getElevation()) {
						return 1;
					}
					if (x.getElevation() > y.getElevation()) {
						return -1;
					}
					return 0;
				}
			});
			int centerID = tiles.get(0);
			Tile centerTile = Planet.this.tiles.get(centerID);
			Vector3 center = centerTile.center.cpy();
			for (int i = 0; i < tiles.size(); i++) {
				int tID = tiles.get(tiles.size() - 1 - i);
				tileQueue.add(tID);
			}

			System.out.println(tileQueue.size() + " " + tiles.size());

			while (!tileQueue.isEmpty()) {
				int tID = tileQueue.poll();
				float elevation = 0;
				int adjCount = 0;
				List<Integer> adjacentTiles = adjacencies.get(tID);
				for (int adjID : adjacentTiles) {
					if (tiles.contains(adjID)) {
						Tile adjTile = Planet.this.tiles.get(adjID);
						float adjElevation = adjTile.getElevation();
						elevation += adjElevation;
						adjCount++;
					}
				}
				float avgElevation = (elevation / adjCount);

				Tile t = Planet.this.tiles.get(tID);
				System.out.println("tile: " + t.getElevation() + " " + avgElevation);

				// System.out.println("avg E: " + avgElevation);
				t.setElevation(avgElevation);
				t.setColor(Color.RED);
				return;
			}
		}
	}

	public class Chunk {
		public List<Integer> tiles;
		public ModelInstance rendered;
		public Planet planet;

		Chunk(List<Integer> tiles, Planet p) {
			this.planet = p;
			this.tiles = tiles;
		}

		public void draw() {
			// Group the tiles by their interior colors
			Map<Color, List<Integer>> colorMap = new HashMap<Color, List<Integer>>();
			for (int tID : tiles) {
				Tile t = Planet.this.tiles.get(tID);
				if (!colorMap.containsKey(t.color)) {
					List<Integer> coloredTiles = new ArrayList<Integer>();
					coloredTiles.add(tID);
					colorMap.put(t.color, coloredTiles);
				} else {
					colorMap.get(t.color).add(tID);
				}
			}

			// Group the edge pieces by their colors
			Map<Color, List<EdgePiece>> borderMap = new HashMap<Color, List<EdgePiece>>();
			for (int tID : tiles) {
				Tile t = Planet.this.tiles.get(tID);
				for (int i : t.edges) {
					EdgePiece e = edges.get(i);
					if (!borderMap.containsKey(e.color)) {
						List<EdgePiece> coloredEdges = new ArrayList<EdgePiece>();
						coloredEdges.add(e);
						borderMap.put(e.color, coloredEdges);
					} else {
						borderMap.get(e.color).add(e);
					}
				}
			}
			mb.begin();

			// Draw edge pieces
			for (Color c : borderMap.keySet()) {
				MeshPartBuilder mbpBorder = mb.part("border", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal,
						new Material(ColorAttribute.createDiffuse(c)));
				for (EdgePiece e : borderMap.get(c)) {
					e.draw(mbpBorder);
				}
			}

			// Draw tiles
			for (Color c : colorMap.keySet()) {
				MeshPartBuilder mbp = mb.part("tile", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal,
						new Material(ColorAttribute.createDiffuse(c)));
				for (int tID : colorMap.get(c)) {
					Tile t = Planet.this.tiles.get(tID);
					t.draw(mbp);
				}
			}

			Model m = mb.end();
			rendered = new ModelInstance(m);
		}
	}

	public class EdgePiece {
		public int fromID;
		public int toID;
		public int id;
		List<Vector3> vertices;
		public Vector3 center;
		Color color;

		EdgePiece(List<Vector3> verts, int id) {
			this.id = id;
			this.color = Color.WHITE;

			// Compute the center of the piece
			float totX = 0, totY = 0, totZ = 0;
			for (Vector3 v : verts) {
				totX += v.x;
				totY += v.y;
				totZ += v.z;
			}
			int size = verts.size();
			this.center = new Vector3(totX / size, totY / size, totZ / size);

			// Ensure proper vertex winding
			if (isWindingCCW(verts.get(0), verts.get(1), verts.get(2))) {
				vertices = new ArrayList<Vector3>(verts);
			} else {
				vertices = new ArrayList<Vector3>();
				for (int i = verts.size(); i > 0; --i) {
					vertices.add(verts.get(i - 1));
				}
			}
		}

		private boolean isWindingCCW(Vector3 a, Vector3 b, Vector3 c) {
			Vector3 axisX = b.cpy().sub(a).nor();
			Vector3 axisY = c.cpy().sub(a).nor();
			Vector3 normal = axisX.crs(axisY);

			Vector3 part = a.cpy().sub(ORIGIN);
			float res = normal.dot(part);

			if (res > 0) {
				return true;
			} else {
				return false;
			}
		}

		public void draw(MeshPartBuilder mpb) {
			Vector3 v1 = vertices.get(0);
			Vector3 v2 = vertices.get(1);
			Vector3 v3 = vertices.get(2);
			Vector3 v4 = vertices.get(3);

			mpb.triangle(v1, v2, v3);
			mpb.triangle(v3, v4, v1);
		}

		public void setColor(Color color) {
			this.color = color;
		}
	}

	public class Tile {
		public Vector3 center;
		private Vector3 tectonicDirection;
		private String type;
		private float totalElevation;
		private int elevationSums;
		List<Vector3> vertices;
		Color color;
		public int id;

		public int[] edges;

		Tile(List<Vector3> verts, int id) {
			this.id = id;
			this.color = Color.WHITE;

			this.edges = new int[verts.size()];

			// Compute the center of the tile
			float totX = 0, totY = 0, totZ = 0;
			for (Vector3 v : verts) {
				totX += v.x;
				totY += v.y;
				totZ += v.z;
			}
			int size = verts.size();
			this.center = new Vector3(totX / size, totY / size, totZ / size);

			// Ensure proper vertex winding
			if (isWindingCCW(verts.get(0), verts.get(1), verts.get(2))) {
				vertices = new ArrayList<Vector3>(verts);
			} else {
				vertices = new ArrayList<Vector3>();
				for (int i = verts.size(); i > 0; --i) {
					vertices.add(verts.get(i - 1));
				}
			}
		}

		public String getType() {
			return this.type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public float getElevation() {
			return this.totalElevation / this.elevationSums;
		}

		public void setElevation(float elevation) {
			this.totalElevation = elevation;
			this.elevationSums = 1;
		}

		public Vector3 getTectonicDirection() {
			return this.tectonicDirection.cpy();
		}

		public void setTectonicDirection(Vector3 r) {
			this.tectonicDirection = r;
		}

		public void draw(MeshPartBuilder middle) {
			Vector3 v1 = vertices.get(0);
			Vector3 v2 = vertices.get(1);
			Vector3 v3 = vertices.get(2);
			Vector3 v4 = vertices.get(3);
			Vector3 v5 = vertices.get(4);

			middle.triangle(v1, v2, v3);
			middle.triangle(v1, v3, v4);
			middle.triangle(v1, v4, v5);

			if (vertices.size() == 6) {
				Vector3 v6 = vertices.get(5);
				middle.triangle(v1, v5, v6);
			}
		}

		public void setColor(Color color) {
			this.color = color;
		}

		public float getRadius() {
			return center.dst(vertices.get(0));
		}

		private boolean isWindingCCW(Vector3 a, Vector3 b, Vector3 c) {
			Vector3 axisX = b.cpy().sub(a).nor();
			Vector3 axisY = c.cpy().sub(a).nor();
			Vector3 normal = axisX.crs(axisY);

			Vector3 part = a.cpy().sub(ORIGIN);
			float res = normal.dot(part);

			if (res > 0) {
				return true;
			} else {
				return false;
			}
		}

		public Vector3 getNormal() {
			Vector3 a = vertices.get(0);
			Vector3 b = vertices.get(1);
			Vector3 c = vertices.get(2);

			Vector3 axisX = b.cpy().sub(a).nor();
			Vector3 axisY = c.cpy().sub(a).nor();
			Vector3 normal = axisX.crs(axisY);
			return normal.nor();
		}

		public Color getColor() {
			return this.color;
		}

		public void addElevation(float elevation) {
			this.totalElevation += elevation;
			this.elevationSums++;
		}
	}

	class Triangle {
		Vector3[] vertices = new Vector3[3];

		Triangle(Vector3 p1, Vector3 p2, Vector3 p3) {
			this.vertices[0] = p1;
			this.vertices[1] = p2;
			this.vertices[2] = p3;
		}

		public Vector3 getCentroid() {
			Vector3 v1 = this.vertices[0];
			Vector3 v2 = this.vertices[1];
			Vector3 v3 = this.vertices[2];

			return new Vector3((v1.x + v2.x + v3.x) / 3f, (v1.y + v2.y + v3.y) / 3f, (v1.z + v2.z + v3.z) / 3f);
		}
	}

	public void subdivide() {
		System.out.print("Subdividing " + vertices.size());
		long time = System.currentTimeMillis();
		int total = triangles.size();
		int tenth = total / 10;
		int steps = 0;

		List<List<Triangle>> splitList = Utility.splitList(triangles, THREADS);
		List<Thread> threads = new ArrayList<Thread>();
		final List<List<Vector3>> threadVerts = new ArrayList<List<Vector3>>();
		final List<List<Triangle>> threadFaces = new ArrayList<List<Triangle>>();
		for (int i = 0; i < THREADS; ++i) {
			threadVerts.add(new ArrayList<Vector3>());
			threadFaces.add(new ArrayList<Triangle>());
		}

		// Launch threads to calculate triangle data.
		for (int i = 0; i < THREADS; ++i) {
			final int id = i;
			final List<Triangle> tris = splitList.get(i);
			final Thread t = new Thread(new Runnable() {

				int id_ = id;
				ArrayList<Triangle> newFaces = new ArrayList<Triangle>();
				List<Vector3> newVerts = new ArrayList<Vector3>();

				@Override
				public void run() {

					for (Triangle tri : tris) {
						// Find points from triangle
						Vector3[] vertices = tri.vertices;
						Vector3 p1 = vertices[0];
						Vector3 p2 = vertices[1];
						Vector3 p3 = vertices[2];

						// Find midpoints
						// Vector3[] mids = new Vector3[3];
						Vector3 mid1 = midpoint(p1, p2);
						Vector3 mid2 = midpoint(p1, p3);
						Vector3 mid3 = midpoint(p2, p3);
						newVerts.add(mid1.cpy());
						newVerts.add(mid2.cpy());
						newVerts.add(mid3.cpy());

						// Project midpoints onto sphere
						mid1.scl(RADIUS / mid1.len());
						mid2.scl(RADIUS / mid2.len());
						mid3.scl(RADIUS / mid3.len());

						// Connect points in triangles
						Triangle tri1 = new Triangle(p1, mid1, mid2);
						Triangle tri2 = new Triangle(p2, mid1, mid3);
						Triangle tri3 = new Triangle(p3, mid2, mid3);
						Triangle tri4 = new Triangle(mid1, mid2, mid3);

						newFaces.add(tri1);
						newFaces.add(tri2);
						newFaces.add(tri3);
						newFaces.add(tri4);
					}

					threadVerts.set(id_, newVerts);
					threadFaces.set(id_, newFaces);
				}
			});
			threads.add(t);
			t.start();
		}

		// Wait for the calculating threads to finish
		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
			}
		}

		// Update with data from the threads
		for (int i = 0; i < threadVerts.size(); ++i) {
			for (Vector3 tVert : threadVerts.get(i)) {
				// Add the midpoints to the list of all vertices if not
				// already there.
				boolean shouldAdd = true;
				for (Vector3 v : this.vertices) {
					if (v.epsilonEquals(tVert, .0001f)) {
						shouldAdd = false;
						break;
					}
				}
				if (shouldAdd) {
					this.vertices.add(tVert.cpy());
					// Loading bar
					steps++;
					if (steps >= tenth) {
						System.out.print(".");
						steps = 0;
					}
				}
			}
		}
		triangles.clear();
		for (int i = 0; i < threadFaces.size(); ++i) {
			triangles.addAll(threadFaces.get(i));
		}

		System.out.print(" complete. " + this.vertices.size() + " vertices in " + (System.currentTimeMillis() - time)
				+ " milliseconds.\n");
	}

	private Vector3 midpoint(Vector3 p1, Vector3 p2) {
		return new Vector3((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f, (p1.z + p2.z) / 2f);
	}

	private void createPlanet() {
		Vector3[] vertices = { new Vector3(1, TAO, 0), new Vector3(-1, TAO, 0), new Vector3(1, -TAO, 0),
				new Vector3(-1, -TAO, 0), new Vector3(0, 1, TAO), new Vector3(0, -1, TAO), new Vector3(0, 1, -TAO),
				new Vector3(0, -1, -TAO), new Vector3(TAO, 0, 1), new Vector3(-TAO, 0, 1), new Vector3(TAO, 0, -1),
				new Vector3(-TAO, 0, -1) };

		// ModelBuilder mb = new ModelBuilder();
		// mb.begin();

		for (Vector3 v : vertices) {
			this.vertices.add(v);
			v.scl(SCALE);
			pentPoints.add(v);
		}

		String[] vertGroups = { "0 4 8", "0 8 10", "0 10 6", "0 6 1", "0 1 4", "6 10 7", "6 7 11", "6 11 1", "11 7 3",
				"11 3 9", "11 9 1", "9 4 1", "9 5 4", "9 3 5", "2 10 8", "2 7 10", "2 3 7", "2 5 3", "2 8 5", "5 8 4" };

		for (String vertKey : vertGroups) {
			String[] verts = vertKey.split(" ");
			final Triangle tri = new Triangle(vertices[Integer.parseInt(verts[0])],
					vertices[Integer.parseInt(verts[1])], vertices[Integer.parseInt(verts[2])]);
			triangles.add(tri);
		}

		// Mesh and scene setup.
		long time = System.currentTimeMillis();
		System.out.println("Creating scene:");
		for (int i = 0; i < SUBDIVISIONS; ++i) {
			subdivide();
		}
		createDual();
		getTileAdjacencies();
		sortTileAdjacencies();
		generateChunks();
		generatePlates();
		calculateTectonics();
		System.out.println("Scene completed in " + (System.currentTimeMillis() - time) + " milliseconds.");
	}

	public void calculateTectonics() {
		System.out.print("Calculating tectonics");
		long time = System.currentTimeMillis();
		int total = plates.size();
		int tenth = total / 10;
		int steps = 0;

		for (Plate plate : plates) {
			plate.calculateTileTectonics();

			// Loading bar
			steps++;
			if (steps >= tenth) {
				System.out.print(".");
				steps = 0;
			}
		}

		System.out.print(" calculated in " + (System.currentTimeMillis() - time) + " milliseconds.\n");
	}

	public void generatePlates() {
		// The tiles to partition
		List<Tile> toCheck = new ArrayList<Tile>(tiles);

		System.out.print("Generating plates");
		long time = System.currentTimeMillis();
		int total = toCheck.size();
		int tenth = total / 10;
		int steps = 0;

		// Seed the plates
		int numPlates = 0;
		while (numPlates < 20) {
			numPlates = random.nextInt(50);
		}

		// Clamp the number of plates
		if (numPlates >= toCheck.size()) {
			numPlates = toCheck.size() - 1;
		}

		for (int i = 0; i < numPlates; ++i) {
			Vector3 axis = tiles.get(random.nextInt(tiles.size())).getNormal();
			float rotAngle = (float) Math.random() * 100f;
			String type = "ocean";
			// int elevation = -1 * random.nextInt(MAX_DEPTH);
			// TODO: make random
			int elevation = -1000;
			if (Math.random() < 0.99) {
				type = "land";
				// elevation = random.nextInt(MAX_HEIGHT);
				elevation = 1000;
			}
			System.out.println(type + " plate with elevation " + elevation);
			Plate plate = new Plate(axis, rotAngle, type, elevation);
			Tile t = toCheck.get(random.nextInt(toCheck.size()));
			t.setType(type);
			t.setElevation(elevation);
			plate.addTile(t.id);
			plates.add(plate);
			toCheck.remove(t);
		}

		// New alg.
		int plateIndex = 0;
		List<Queue<Tile>> plateQueues = new ArrayList<Queue<Tile>>();
		while (!toCheck.isEmpty()) {
			Plate p = plates.get(plateIndex);

			// If there is no queue for this tile
			if (plateQueues.size() == plateIndex) {
				// Create the queue
				Queue<Tile> pTiles = new LinkedList<Tile>();

				// Get all tiles adjacent to the seed tile
				int seedID = p.tiles.get(0);
				for (int adjID : adjacencies.get(seedID)) {
					Tile adjTile = Planet.this.tiles.get(adjID);
					if (toCheck.contains(adjTile)) {
						toCheck.remove(adjTile);
						adjTile.setType(p.type);
						adjTile.setElevation(p.elevation);
						p.addTile(adjID);

						// Add this to the plate-edge queue
						pTiles.add(adjTile);

						// Loading bar
						steps++;
						if (steps >= tenth) {
							System.out.print(".");
							steps = 0;
						}
					}
				}

				// Add the queue to the queue list
				plateQueues.add(plateIndex, pTiles);
			} else {
				Queue<Tile> pTiles = plateQueues.get(plateIndex);
				Queue<Tile> newBounds = new LinkedList<Tile>();

				// Get all tiles adjacent to the marked boundary tiles
				while (!pTiles.isEmpty()) {
					Tile t = pTiles.poll();
					for (int adjID : adjacencies.get(t.id)) {
						Tile adjTile = Planet.this.tiles.get(adjID);
						if (toCheck.contains(adjTile)) {
							toCheck.remove(adjTile);
							adjTile.setType(p.type);
							adjTile.setElevation(p.elevation);
							p.addTile(adjID);

							// Add this to the plate-edge queue
							newBounds.add(adjTile);

							// Loading bar
							steps++;
							if (steps >= tenth) {
								System.out.print(".");
								steps = 0;
							}
						}
					}
				}

				// Update the plate boundary queue.
				pTiles.clear();
				pTiles = null;
				plateQueues.remove(plateIndex);
				plateQueues.add(plateIndex, newBounds);
			}

			// Loop through plates by incrementing
			plateIndex++;
			if (plateIndex >= numPlates) {
				plateIndex = 0;
			}
		}

		System.out.print(" partitioned in " + (System.currentTimeMillis() - time) + " milliseconds.\n");
	}

	private void generateChunks() {
		// The tiles to partition
		List<Tile> toCheck = new ArrayList<Tile>(tiles);

		System.out.print("Partitioning chunks");
		long time = System.currentTimeMillis();
		int total = toCheck.size();
		int tenth = total / 10;
		int steps = 0;

		// Seed the regions
		for (int i = 0; i < CHUNK_DIVISIONS[SUBDIVISIONS]; ++i) {
			Tile p = tiles.get(i);
			List<Integer> region = new ArrayList<Integer>();
			region.add(p.id);
			chunks.add(new Chunk(region, this));
			toCheck.remove(p);
		}

		while (!toCheck.isEmpty()) {
			for (Chunk c : chunks) {
				List<Integer> newRegion = new ArrayList<Integer>();
				for (int tID : c.tiles) {
					List<Integer> adjIDs = adjacencies.get(tID);
					for (int aID : adjIDs) {
						Tile a = Planet.this.tiles.get(aID);
						if (toCheck.contains(a)) {
							toCheck.remove(a);
							newRegion.add(a.id);

							// Loading bar
							steps++;
							if (steps >= tenth) {
								System.out.print(".");
								steps = 0;
							}
						}
					}
				}
				c.tiles.addAll(newRegion);
			}
		}

		System.out.print(" partitioned in " + (System.currentTimeMillis() - time) + " milliseconds.\n");
	}

	private void sortTileAdjacencies() {
		System.out.print("Sorting tile adjacencies");
		long time = System.currentTimeMillis();
		int total = tiles.size();
		int tenth = total / 10;
		int steps = 0;

		// Organize the adjacency list so that it properly keys to the EdgePiece
		// array
		for (final Tile t : tiles) {
			List<Integer> sortedAdjacencies = new ArrayList<Integer>();
			for (int i : t.edges) {
				final EdgePiece e = edges.get(i);

				// Find which midpoint-with-adjacent-tile is closest to the
				// center of this EdgePiece, for each EdgePiece.
				Ordering<Integer> byDistance = new Ordering<Integer>() {
					@Override
					public int compare(Integer t1ID, Integer t2ID) {
						Tile t1 = Planet.this.tiles.get(t1ID);
						Tile t2 = Planet.this.tiles.get(t2ID);
						return Floats.compare(midpoint(t.center, t1.center).dst2(e.center),
								midpoint(t.center, t2.center).dst2(e.center));
					}
				};

				List<Integer> adj = new ArrayList<Integer>();
				adj = byDistance.leastOf(adjacencies.get(t.id), 1);
				sortedAdjacencies.add(adj.get(0));
			}
			adjacencies.put(t.id, sortedAdjacencies);

			// Loading bar
			steps++;
			if (steps >= tenth) {
				System.out.print(".");
				steps = 0;
			}
		}

		// TODO: 1. improve performance across the board (especially plate
		// generation), 2. complete the multithreading of the loading. 3.
		// further fragment the borders into equilateral triangles and
		// rectangles, for smooth borders. 4. proceed with the worldbuilding

		System.out.print(" sorted in " + (System.currentTimeMillis() - time) + " milliseconds.\n");
	}

	private void getTileAdjacencies() {
		System.out.print("Setting tile adjacencies");
		long time = System.currentTimeMillis();
		int total = tiles.size();
		int tenth = total / 10;
		int steps = 0;

		// Every tile knows of its adjacent tiles
		adjacencies.clear();
		for (final Tile t : tiles) {
			// Sort all tiles by distance and find the closest 5 or 6
			Ordering<Tile> byDistance = new Ordering<Tile>() {
				@Override
				public int compare(Tile t1, Tile t2) {
					return Floats.compare(t1.center.dst2(t.center), t2.center.dst2(t.center));
				}
			};

			List<Tile> adj = new ArrayList<Tile>();
			if (t.vertices.size() == 5) {
				adj = byDistance.leastOf(tiles, 6);
			} else {
				adj = byDistance.leastOf(tiles, 7);
			}
			List<Integer> adjIDs = new ArrayList<Integer>();
			for (Tile sortedT : adj) {
				adjIDs.add(sortedT.id);
			}
			adjacencies.put(t.id, adjIDs);

			// // TODO: fix this when switching over to mapping with strictly
			// IDs
			// for (int i = 0; i < adj.size(); ++i) {
			// Tile a = adj.get(i);
			// t.edges[i] = a.id;
			// }

			// Loading bar
			steps++;
			if (steps >= tenth) {
				System.out.print(".");
				steps = 0;
			}
		}
		System.out.print(" set in " + (System.currentTimeMillis() - time) + " milliseconds.\n");
	}

	ModelBuilder testMB = new ModelBuilder();
	ModelInstance testInstance;

	// Links an id to a Vector3
	class Centroid {
		Vector3 v;
		int id;

		Centroid(Vector3 v, int id) {
			this.v = v;
			this.id = id;
		}
	}

	int calls = 0;

	private void threadTenth() {
		calls++;
		if (calls == THREADS) {
			System.out.print(".");
			calls = 0;
		}
	}

	private void createDual() {
		System.out.println("Generating dual:");
		tiles.clear();
		final List<Centroid> centroids = new ArrayList<Centroid>();

		// Every centroid knows of its three adjacent centroids
		final List<List<Centroid>> adjacencies = new ArrayList<List<Centroid>>();

		// Loading
		System.out.print("Processing centroid adjacencies");
		long time = System.currentTimeMillis();

		// Get every centroid.
		for (int i = 0; i < triangles.size(); ++i) {
			Triangle tri = triangles.get(i);
			Vector3 cent = tri.getCentroid();
			centroids.add(new Centroid(cent, i));
			adjacencies.add(new ArrayList<Centroid>());
		}

		// Launch threads to aid in sorting
		List<List<Centroid>> splitList = Utility.splitList(centroids, THREADS);
		List<Thread> threads = new ArrayList<Thread>();

		for (int i = 0; i < THREADS; ++i) {
			final List<Centroid> cents = splitList.get(i);
			final Thread t = new Thread(new Runnable() {
				int steps = 0;
				int tenth = cents.size() / 10;

				@Override
				public void run() {
					for (final Centroid c : cents) {
						// Sort all centroids by distance and find the closest
						// three
						Ordering<Centroid> byDistance = new Ordering<Centroid>() {
							@Override
							public int compare(Centroid v1, Centroid v2) {
								return Floats.compare(v1.v.dst2(c.v), v2.v.dst2(c.v));
							}
						};

						List<Centroid> adj = byDistance.leastOf(centroids, 4);
						adjacencies.set(c.id, adj);

						// Loading bar
						steps++;
						if (steps >= tenth) {
							threadTenth();
							steps = 0;
						}
					}
				}
			});
			threads.add(t);
			t.start();
		}

		// Wait for the calculating threads to finish
		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
			}
		}

		System.out.print(" processed in " + (System.currentTimeMillis() - time) + " milliseconds.\n");

		threads.clear();

		// Find and group the tiles.
		System.out.print("Grouping tiles");
		time = System.currentTimeMillis();

		// Launch threads to aid in sorting
		List<List<Vector3>> splitListVertices = Utility.splitList(vertices, THREADS);

		final List<List<List<Centroid>>> seenData = new ArrayList<List<List<Centroid>>>();
		for (int i = 0; i < THREADS; ++i) {
			List<List<Centroid>> threadInternal = new ArrayList<List<Centroid>>();
			seenData.add(threadInternal);
		}

		for (int i = 0; i < THREADS; ++i) {
			final List<Vector3> verts = splitListVertices.get(i);
			final int id = i;

			final Thread t = new Thread(new Runnable() {
				int steps = 0;
				int tenth = verts.size() / 10;
				int id_ = id;
				List<List<Centroid>> seenTiles = new ArrayList<List<Centroid>>();

				@Override
				public void run() {
					for (int id = 0; id < verts.size(); ++id) {
						final Vector3 v = verts.get(id);

						// Find the 5 or 6 nearest centroids
						Ordering<Centroid> byDistance = new Ordering<Centroid>() {
							@Override
							public int compare(Centroid v1, Centroid v2) {
								return Floats.compare(v1.v.dst2(v), v2.v.dst2(v));
							}
						};

						List<Centroid> candidates;
						if (pentPoints.contains(v)) {
							candidates = byDistance.leastOf(centroids, 5);
						} else {
							candidates = byDistance.leastOf(centroids, 6);
						}

						// Sort the centroids into the order in which they
						// appear in the
						// face
						Centroid start = candidates.get(0);
						Centroid last = null;
						List<Centroid> seen = new ArrayList<Centroid>();
						while (seen.size() < candidates.size() - 1) {
							for (int i = 1; i < adjacencies.get(start.id).size(); ++i) {
								Centroid c = adjacencies.get(start.id).get(i);
								if (candidates.contains(c) && !seen.contains(c)) {
									seen.add(start);
									start = c;
									last = c;
									break;
								}
							}
						}

						seen.add(last);
						seenTiles.add(seen);

						// Loading bar
						steps++;
						if (steps >= tenth) {
							threadTenth();
							steps = 0;
						}
					}

					seenData.set(id_, seenTiles);
				}
			});
			threads.add(t);
			t.start();
		}

		// Wait for the calculating threads to finish
		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
			}
		}

		// Take the collected tile data and compile it
		int id = 0;
		int edgeID = 0;
		for (int i = 0; i < THREADS; ++i) {
			List<List<Centroid>> threadInternal = seenData.get(i);
			for (List<Centroid> centroidList : threadInternal) {
				List<Vector3> verts = new ArrayList<Vector3>();
				for (Centroid c : centroidList) {
					verts.add(c.v);
				}

				// TODO: calculate the better vertices and then only make one
				// tile
				Tile tile = new Tile(verts, id);

				List<Vector3> innerVerts = new ArrayList<Vector3>();

				Vector3 c = tile.center;
				float borderThickness = tile.getRadius() / 10f;

				Vector3 v1 = verts.get(0);
				Vector3 v1dist = c.cpy().sub(v1).nor().scl(borderThickness);
				Vector3 v1Interior = v1.cpy().add(v1dist);
				innerVerts.add(v1Interior.cpy().add(ORIGIN));

				Vector3 v2 = verts.get(1);
				Vector3 v2dist = c.cpy().sub(v2).nor().scl(borderThickness);
				Vector3 v2Interior = v2.cpy().add(v2dist);
				innerVerts.add(v2Interior.cpy().add(ORIGIN));

				Vector3 v3 = verts.get(2);
				Vector3 v3dist = c.cpy().sub(v3).nor().scl(borderThickness);
				Vector3 v3Interior = v3.cpy().add(v3dist);
				innerVerts.add(v3Interior.cpy().add(ORIGIN));

				Vector3 v4 = verts.get(3);
				Vector3 v4dist = c.cpy().sub(v4).nor().scl(borderThickness);
				Vector3 v4Interior = v4.cpy().add(v4dist);
				innerVerts.add(v4Interior.cpy().add(ORIGIN));

				Vector3 v5 = verts.get(4);
				Vector3 v5dist = c.cpy().sub(v5).nor().scl(borderThickness);
				Vector3 v5Interior = v5.cpy().add(v5dist);
				innerVerts.add(v5Interior.cpy().add(ORIGIN));

				Vector3 v6 = null;
				Vector3 v6dist = null;
				Vector3 v6Interior = null;
				if (verts.size() == 6) {
					v6 = verts.get(5);
					v6dist = c.cpy().sub(v6).nor().scl(borderThickness);
					v6Interior = v6.cpy().add(v6dist);
					innerVerts.add(v6Interior.cpy().add(ORIGIN));
				}

				tile = new Tile(innerVerts, id);
				id++;

				// TODO this seems like an awful lot of vector copying...

				List<Vector3> edgeVerts = new ArrayList<Vector3>();
				edgeVerts.add(v1.cpy().add(ORIGIN));
				edgeVerts.add(v1Interior.cpy().add(ORIGIN));
				edgeVerts.add(v2Interior.cpy().add(ORIGIN));
				edgeVerts.add(v2.cpy().add(ORIGIN));
				EdgePiece p1 = new EdgePiece(edgeVerts, edgeID);
				edges.add(p1);
				tile.edges[0] = edgeID;
				edgeID++;
				edgeVerts.clear();

				edgeVerts.add(v2.cpy().add(ORIGIN));
				edgeVerts.add(v2Interior.cpy().add(ORIGIN));
				edgeVerts.add(v3Interior.cpy().add(ORIGIN));
				edgeVerts.add(v3.cpy().add(ORIGIN));
				EdgePiece p2 = new EdgePiece(edgeVerts, edgeID);
				edges.add(p2);
				tile.edges[1] = edgeID;
				edgeID++;
				edgeVerts.clear();

				edgeVerts.add(v3.cpy().add(ORIGIN));
				edgeVerts.add(v3Interior.cpy().add(ORIGIN));
				edgeVerts.add(v4Interior.cpy().add(ORIGIN));
				edgeVerts.add(v4.cpy().add(ORIGIN));
				EdgePiece p3 = new EdgePiece(edgeVerts, edgeID);
				edges.add(p3);
				tile.edges[2] = edgeID;
				edgeID++;
				edgeVerts.clear();

				edgeVerts.add(v4.cpy().add(ORIGIN));
				edgeVerts.add(v4Interior.cpy().add(ORIGIN));
				edgeVerts.add(v5Interior.cpy().add(ORIGIN));
				edgeVerts.add(v5.cpy().add(ORIGIN));
				EdgePiece p4 = new EdgePiece(edgeVerts, edgeID);
				edges.add(p4);
				tile.edges[3] = edgeID;
				edgeID++;
				edgeVerts.clear();

				if (verts.size() == 6) {
					edgeVerts.add(v5.cpy().add(ORIGIN));
					edgeVerts.add(v5Interior.cpy().add(ORIGIN));
					edgeVerts.add(v6Interior.cpy().add(ORIGIN));
					edgeVerts.add(v6.cpy().add(ORIGIN));
					EdgePiece p5 = new EdgePiece(edgeVerts, edgeID);
					edges.add(p5);
					tile.edges[4] = edgeID;
					edgeID++;
					edgeVerts.clear();

					edgeVerts.add(v6.cpy().add(ORIGIN));
					edgeVerts.add(v6Interior.cpy().add(ORIGIN));
					edgeVerts.add(v1Interior.cpy().add(ORIGIN));
					edgeVerts.add(v1.cpy().add(ORIGIN));
					EdgePiece p6 = new EdgePiece(edgeVerts, edgeID);
					edges.add(p6);
					tile.edges[5] = edgeID;
					edgeID++;
					edgeVerts.clear();
				} else {
					edgeVerts.add(v5.cpy().add(ORIGIN));
					edgeVerts.add(v5Interior.cpy().add(ORIGIN));
					edgeVerts.add(v1Interior.cpy().add(ORIGIN));
					edgeVerts.add(v1.cpy().add(ORIGIN));
					EdgePiece p5 = new EdgePiece(edgeVerts, edgeID);
					edges.add(p5);
					tile.edges[4] = edgeID;
					edgeID++;
					edgeVerts.clear();
				}

				tiles.add(tile);
			}
		}

		System.out.print(" grouped in " + (System.currentTimeMillis() - time) + " milliseconds.\n");
	}
}