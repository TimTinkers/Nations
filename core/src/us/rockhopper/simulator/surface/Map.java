package us.rockhopper.simulator.surface;

import com.badlogic.gdx.utils.Array;

public class Map {
	// private String file;
	private Array<String> map;

	public void load(String mapFile) {
		// file = mapFile;
		map = new Array<String>();

		map.add("WWWWWWWWWWWW");
		map.add("W S        W");
		map.add("W      W   W");
		map.add("W          W");
		map.add("W    W     W");
		map.add("W          W");
		map.add("WWWWWWWWWWWW");

		// BufferedReader in = null;
		// try {
		// in = new BufferedReader(new
		// InputStreamReader(Gdx.files.internal("maps/" + file).read()));
		// String mapRowString;
		// while ((mapRowString = in.readLine()) != null)
		// map.add(mapRowString);
		// } catch (Throwable e) {
		// System.out.println("Error loading map!");
		// }
	}

	public String getTile(int x, int y) {
		String tile = map.get(y).substring(x, x + 1);
		return tile;
	}

	public Array<String> getMap() {
		return map;
	}
}