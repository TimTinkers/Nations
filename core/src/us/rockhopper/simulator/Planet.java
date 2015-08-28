package us.rockhopper.simulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import us.rockhopper.simulator.util.Utility;

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

public class Planet {
	// implements ApplicationListener {

	private final float SCALE = 10f;
	private final float TAO = 1.61803399f;
	private final float RADIUS = SCALE * 2f * 0.95105f;

	// private final float borderThickness = 0.1f;

	public final long SEED;
	private final int THREADS = 8;
	public final int SUBDIVISIONS;
	private final int[] CHUNK_DIVISIONS = { 1, 1, 1, 12, 42, 162, 642, 642,
			2562 };

	private Vector3 ORIGIN = new Vector3(0, 0, 0);

	Random random;
	Color colors[] = { Color.BLUE, Color.GREEN, Color.PINK, Color.RED,
			Color.CYAN, Color.WHITE, Color.YELLOW, Color.ORANGE, Color.GRAY,
			Color.OLIVE, Color.DARK_GRAY, Color.LIGHT_GRAY };

	public List<Vector3> vertices = new ArrayList<Vector3>();
	public List<Triangle> triangles = new ArrayList<Triangle>();

	public List<Tile> tiles = new ArrayList<Tile>();
	public List<EdgePiece> edges = new ArrayList<EdgePiece>();

	public List<Vector3> pentPoints = new ArrayList<Vector3>();

	public HashMap<Tile, List<Tile>> adjacencies = new HashMap<Tile, List<Tile>>();

	public List<Chunk> chunks = new ArrayList<Chunk>();

	public List<Plate> plates = new ArrayList<Plate>();

	private ModelBuilder mb = new ModelBuilder();

	public Planet(int subdivisions) {
		this.SUBDIVISIONS = subdivisions;
		this.SEED = 0;
		this.random = new Random(SEED);
		createPlanet();
	}

	public Planet(int subdivisions, long seed) {
		this.SUBDIVISIONS = subdivisions;
		this.random = new Random(seed);
		this.SEED = seed;
		createPlanet();
	}

	public class Plate {
		public List<Tile> tiles;
		public Vector3 axis;
		public float rotationAngle;

		Plate(Vector3 axis, float rotationAngle) {
			this.axis = axis;
			this.rotationAngle = rotationAngle;
			tiles = new ArrayList<Tile>();
		}

		public void addTile(Tile tile) {
			tiles.add(tile);
		}
	}

	public class Chunk {
		public List<Tile> tiles;
		public ModelInstance rendered;

		Chunk(List<Tile> tiles) {
			this.tiles = tiles;
		}

		public void draw() {
			// Group the tiles by their interior colors
			Map<Color, List<Tile>> colorMap = new HashMap<Color, List<Tile>>();
			for (Tile t : tiles) {
				if (!colorMap.containsKey(t.color)) {
					List<Tile> coloredTiles = new ArrayList<Tile>();
					coloredTiles.add(t);
					colorMap.put(t.color, coloredTiles);
				} else {
					colorMap.get(t.color).add(t);
				}
			}

			// Group the edge pieces by their colors
			Map<Color, List<EdgePiece>> borderMap = new HashMap<Color, List<EdgePiece>>();
			for (Tile t : tiles) {
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
				MeshPartBuilder mbpBorder = mb.part("border",
						GL20.GL_TRIANGLES, Usage.Position | Usage.Normal,
						new Material(ColorAttribute.createDiffuse(c)));
				for (EdgePiece e : borderMap.get(c)) {
					e.draw(mbpBorder);
				}
			}

			// Draw tiles
			for (Color c : colorMap.keySet()) {
				MeshPartBuilder mbp = mb.part("tile", GL20.GL_TRIANGLES,
						Usage.Position | Usage.Normal, new Material(
								ColorAttribute.createDiffuse(c)));
				for (Tile t : colorMap.get(c)) {
					t.draw(mbp);
				}
			}

			// Draw plate indicators
			MeshPartBuilder mpbP = mb.part("plate", GL20.GL_TRIANGLES,
					Usage.Position | Usage.Normal,
					new Material(ColorAttribute.createDiffuse(Color.PINK)));

			for (Plate p : plates) {
				for (Tile t : p.tiles) {
					if (tiles.contains(t)) {
						Vector3 dir = p.axis.cpy().crs(t.getNormal()).nor()
								.add(t.center);

						Vector3 quant = dir.cpy().sub(t.center);
						float q = quant.dot(dir);
						Vector3 q1 = t.getNormal().cpy().scl(q);
						Vector3 dirProj = dir.cpy().sub(q1);

						Vector3 side = p.axis.cpy();

						// TODO: figure out the rendering of these arrows

						// mpbP.triangle(
						// t.center.cpy().add(side.cpy().scl(-0.5f)),
						// t.center.cpy().add(side.cpy().scl(0.5f)), dirProj);
						// mpbP.triangle(
						// t.center.cpy().add(side.cpy().scl(0.5f)),
						// t.center.cpy().add(side.cpy().scl(-0.5f)), dirProj);

//						mpbP.box(dirProj.x, dirProj.y, dirProj.z, 0.1f, 0.1f,
//								0.1f);
//						mpbP.box(dir.x, dir.y, dir.z, 0.1f, 0.1f, 0.1f);
					}
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
			return normal;
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

			return new Vector3((v1.x + v2.x + v3.x) / 3f,
					(v1.y + v2.y + v3.y) / 3f, (v1.z + v2.z + v3.z) / 3f);
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

		System.out.print(" complete. " + this.vertices.size() + " vertices in "
				+ (System.currentTimeMillis() - time) + " milliseconds.\n");
	}

	private Vector3 midpoint(Vector3 p1, Vector3 p2) {
		return new Vector3((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f,
				(p1.z + p2.z) / 2f);
	}

	private void createPlanet() {
		Vector3[] vertices = { new Vector3(1, TAO, 0), new Vector3(-1, TAO, 0),
				new Vector3(1, -TAO, 0), new Vector3(-1, -TAO, 0),
				new Vector3(0, 1, TAO), new Vector3(0, -1, TAO),
				new Vector3(0, 1, -TAO), new Vector3(0, -1, -TAO),
				new Vector3(TAO, 0, 1), new Vector3(-TAO, 0, 1),
				new Vector3(TAO, 0, -1), new Vector3(-TAO, 0, -1) };

		// ModelBuilder mb = new ModelBuilder();
		// mb.begin();

		for (Vector3 v : vertices) {
			this.vertices.add(v);
			v.scl(SCALE);
			pentPoints.add(v);
		}

		String[] vertGroups = { "0 4 8", "0 8 10", "0 10 6", "0 6 1", "0 1 4",
				"6 10 7", "6 7 11", "6 11 1", "11 7 3", "11 3 9", "11 9 1",
				"9 4 1", "9 5 4", "9 3 5", "2 10 8", "2 7 10", "2 3 7",
				"2 5 3", "2 8 5", "5 8 4" };

		for (String vertKey : vertGroups) {
			String[] verts = vertKey.split(" ");
			final Triangle tri = new Triangle(
					vertices[Integer.parseInt(verts[0])],
					vertices[Integer.parseInt(verts[1])],
					vertices[Integer.parseInt(verts[2])]);
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
		System.out.println("Scene completed in "
				+ (System.currentTimeMillis() - time) + " milliseconds.");
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
			Plate plate = new Plate(tiles.get(random.nextInt(tiles.size()))
					.getNormal(), (float) Math.random());
			Tile t = toCheck.get(random.nextInt(toCheck.size()));
			plate.addTile(t);
			plates.add(plate);
			toCheck.remove(t);
		}

		int plateIndex = 0;
		while (!toCheck.isEmpty()) {
			Plate p = plates.get(plateIndex);
			for (Tile t : p.tiles) {
				boolean shouldBreak = false;
				for (Tile adj : adjacencies.get(t)) {
					if (toCheck.contains(adj)) {
						toCheck.remove(adj);
						p.addTile(adj);
						shouldBreak = true;

						// Loading bar
						steps++;
						if (steps >= tenth) {
							System.out.print(".");
							steps = 0;
						}

						break;
					}
				}
				if (shouldBreak) {
					break;
				}
			}
			plateIndex++;
			if (plateIndex >= numPlates) {
				plateIndex = 0;
			}
		}

		System.out.print(" partitioned in "
				+ (System.currentTimeMillis() - time) + " milliseconds.\n");
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
			List<Tile> region = new ArrayList<Tile>();
			region.add(p);
			chunks.add(new Chunk(region));
			toCheck.remove(p);
		}

		while (!toCheck.isEmpty()) {
			for (Chunk c : chunks) {
				List<Tile> newRegion = new ArrayList<Tile>();
				for (Tile t : c.tiles) {
					List<Tile> adj = adjacencies.get(t);
					for (Tile a : adj) {
						if (toCheck.contains(a)) {
							toCheck.remove(a);
							newRegion.add(a);

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

		System.out.print(" partitioned in "
				+ (System.currentTimeMillis() - time) + " milliseconds.\n");
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
			List<Tile> sortedAdjacencies = new ArrayList<Tile>();
			for (int i : t.edges) {
				final EdgePiece e = edges.get(i);

				// Find which midpoint-with-adjacent-tile is closest to the
				// center of this EdgePiece, for each EdgePiece.
				Ordering<Tile> byDistance = new Ordering<Tile>() {
					@Override
					public int compare(Tile t1, Tile t2) {
						return Floats.compare(midpoint(t.center, t1.center)
								.dst2(e.center), midpoint(t.center, t2.center)
								.dst2(e.center));
					}
				};

				List<Tile> adj = new ArrayList<Tile>();
				adj = byDistance.leastOf(adjacencies.get(t), 1);
				sortedAdjacencies.add(adj.get(0));
			}
			adjacencies.put(t, sortedAdjacencies);

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

		System.out.print(" sorted in " + (System.currentTimeMillis() - time)
				+ " milliseconds.\n");
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
					return Floats.compare(t1.center.dst2(t.center),
							t2.center.dst2(t.center));
				}
			};

			List<Tile> adj = new ArrayList<Tile>();
			if (t.vertices.size() == 5) {
				adj = byDistance.leastOf(tiles, 6);
			} else {
				adj = byDistance.leastOf(tiles, 7);
			}
			adjacencies.put(t, adj);

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
		System.out.print(" set in " + (System.currentTimeMillis() - time)
				+ " milliseconds.\n");
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
								return Floats.compare(v1.v.dst2(c.v),
										v2.v.dst2(c.v));
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

		System.out.print(" processed in " + (System.currentTimeMillis() - time)
				+ " milliseconds.\n");

		threads.clear();

		// Find and group the tiles.
		System.out.print("Grouping tiles");
		time = System.currentTimeMillis();

		// Launch threads to aid in sorting
		List<List<Vector3>> splitListVertices = Utility.splitList(vertices,
				THREADS);

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
								return Floats.compare(v1.v.dst2(v),
										v2.v.dst2(v));
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
							for (int i = 1; i < adjacencies.get(start.id)
									.size(); ++i) {
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
				innerVerts.add(v1Interior);

				Vector3 v2 = verts.get(1);
				Vector3 v2dist = c.cpy().sub(v2).nor().scl(borderThickness);
				Vector3 v2Interior = v2.cpy().add(v2dist);
				innerVerts.add(v2Interior);

				Vector3 v3 = verts.get(2);
				Vector3 v3dist = c.cpy().sub(v3).nor().scl(borderThickness);
				Vector3 v3Interior = v3.cpy().add(v3dist);
				innerVerts.add(v3Interior);

				Vector3 v4 = verts.get(3);
				Vector3 v4dist = c.cpy().sub(v4).nor().scl(borderThickness);
				Vector3 v4Interior = v4.cpy().add(v4dist);
				innerVerts.add(v4Interior);

				Vector3 v5 = verts.get(4);
				Vector3 v5dist = c.cpy().sub(v5).nor().scl(borderThickness);
				Vector3 v5Interior = v5.cpy().add(v5dist);
				innerVerts.add(v5Interior);

				Vector3 v6 = null;
				Vector3 v6dist = null;
				Vector3 v6Interior = null;
				if (verts.size() == 6) {
					v6 = verts.get(5);
					v6dist = c.cpy().sub(v6).nor().scl(borderThickness);
					v6Interior = v6.cpy().add(v6dist);
					innerVerts.add(v6Interior);
				}

				tile = new Tile(innerVerts, id);
				id++;

				List<Vector3> edgeVerts = new ArrayList<Vector3>();
				edgeVerts.add(v1);
				edgeVerts.add(v1Interior);
				edgeVerts.add(v2Interior);
				edgeVerts.add(v2);
				EdgePiece p1 = new EdgePiece(edgeVerts, edgeID);
				edges.add(p1);
				tile.edges[0] = edgeID;
				edgeID++;
				edgeVerts.clear();

				edgeVerts.add(v2);
				edgeVerts.add(v2Interior);
				edgeVerts.add(v3Interior);
				edgeVerts.add(v3);
				EdgePiece p2 = new EdgePiece(edgeVerts, edgeID);
				edges.add(p2);
				tile.edges[1] = edgeID;
				edgeID++;
				edgeVerts.clear();

				edgeVerts.add(v3);
				edgeVerts.add(v3Interior);
				edgeVerts.add(v4Interior);
				edgeVerts.add(v4);
				EdgePiece p3 = new EdgePiece(edgeVerts, edgeID);
				edges.add(p3);
				tile.edges[2] = edgeID;
				edgeID++;
				edgeVerts.clear();

				edgeVerts.add(v4);
				edgeVerts.add(v4Interior);
				edgeVerts.add(v5Interior);
				edgeVerts.add(v5);
				EdgePiece p4 = new EdgePiece(edgeVerts, edgeID);
				edges.add(p4);
				tile.edges[3] = edgeID;
				edgeID++;
				edgeVerts.clear();

				if (verts.size() == 6) {
					edgeVerts.add(v5);
					edgeVerts.add(v5Interior);
					edgeVerts.add(v6Interior);
					edgeVerts.add(v6);
					EdgePiece p5 = new EdgePiece(edgeVerts, edgeID);
					edges.add(p5);
					tile.edges[4] = edgeID;
					edgeID++;
					edgeVerts.clear();

					edgeVerts.add(v6);
					edgeVerts.add(v6Interior);
					edgeVerts.add(v1Interior);
					edgeVerts.add(v1);
					EdgePiece p6 = new EdgePiece(edgeVerts, edgeID);
					edges.add(p6);
					tile.edges[5] = edgeID;
					edgeID++;
					edgeVerts.clear();
				} else {
					edgeVerts.add(v5);
					edgeVerts.add(v5Interior);
					edgeVerts.add(v1Interior);
					edgeVerts.add(v1);
					EdgePiece p5 = new EdgePiece(edgeVerts, edgeID);
					edges.add(p5);
					tile.edges[4] = edgeID;
					edgeID++;
					edgeVerts.clear();
				}

				tiles.add(tile);
			}
		}

		System.out.print(" grouped in " + (System.currentTimeMillis() - time)
				+ " milliseconds.\n");
	}
}