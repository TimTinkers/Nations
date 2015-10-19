package us.rockhopper.simulator.surface;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import us.rockhopper.simulator.Planet;

public class World {

	private Player player;
	private Map map = new Map();
	private Array<Wall> walls = new Array<Wall>();

	public World(Planet planet) {
		player = new Player(this);
		generateLevel(0);
	}

	private void generateLevel(int levelNumber) {
		map.load("Level" + levelNumber + ".map");
		walls.clear();
		for (int y = 0; y < map.getMap().size; y++) {
			for (int x = 0; x < map.getMap().get(y).length(); x++) {
				if (map.getTile(x, y).equals("S")) {
					// Set start position
					player.getCentrePos().set(x, 0, map.getMap().size - y);
				}
				if (map.getTile(x, y).equals("W")) {
					// Generate walls
					walls.add(new Wall(new Vector3(x, map.getMap().size - y, 0), 1f, 1f));
				}
			}
		}
	}

	public void update(float delta) {
		handleInput(delta);
	}

	// Called by player.tryMove() - returns true if collision with blocking
	// object, false if collision with non-blocking object
	public boolean collision() {
		for (int i = 0; i < walls.size; i++) {
			if (walls.get(i).bounds.contains(player.getHitBox())) {
				return true;
			}
		}
		return false;
	}

	private void handleInput(float delta) {
		// Desktop controls
		if (Gdx.app.getType() == ApplicationType.Desktop) {
			if (Gdx.input.isKeyPressed(Input.Keys.W))
				player.moveForward(delta);
			if (Gdx.input.isKeyPressed(Input.Keys.S))
				player.moveBackward(delta);
			if (Gdx.input.isKeyPressed(Input.Keys.LEFT))
				player.turnLeft(delta);
			if (Gdx.input.isKeyPressed(Input.Keys.RIGHT))
				player.turnRight(delta);
			if (Gdx.input.isKeyPressed(Input.Keys.A))
				player.strafeLeft(delta);
			if (Gdx.input.isKeyPressed(Input.Keys.D))
				player.strafeRight(delta);
		}
	}

	public Player getPlayer() {
		return player;
	}

	public Map getMap() {
		return map;
	}

	public Array<Wall> getWalls() {
		return walls;
	}
}
