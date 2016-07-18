package us.rockhopper.simulator.surface;

import java.util.ArrayList;
import java.util.Random;

import com.badlogic.gdx.math.Vector3;

import us.rockhopper.simulator.Planet;

public class World {

	private ArrayList<Hex> hexes = new ArrayList<Hex>();
	private Random random = new Random();

	public World(Planet planet) {
		for (int i = -50; i < 50; ++i) {
			for (int j = -50; j < 50; ++j) {
				this.addHex(i, j);
			}
		}
	}

	public void addHex(int x, int z) {
		// Convert axial world coords to render coords
		Vector3 transpose = new Vector3((x * 0.86603f) + (0.433015f * z), 0, (0.75f * z));
		hexes.add(new Hex(transpose, 1, 1));
	}

	public ArrayList<Hex> getHexes() {
		return this.hexes;
	}
}