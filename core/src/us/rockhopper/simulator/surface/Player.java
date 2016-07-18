package us.rockhopper.simulator.surface;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;

public class Player {

	// Camera
	public PerspectiveCamera camera;
	public PlayerCameraController camController;

	// Physical properties
	private Vector3 position;
	private float height;
	private float width;
	private float depth;

	// TODO test gravity
	private Vector3 gravity = new Vector3(0, -1, 0);

	public Player() {
		this.position = new Vector3(0, 0, 0);
		this.height = 2f;
		this.width = 1f;
		this.depth = 1f;

		// Camera
		camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		camera.position.set(0f, 2f, 0f);
		camera.near = 0.5f;
		camera.far = 1000f;
		camera.update();
		camController = new PlayerCameraController(camera);

		// Initialize input processing
		InputMultiplexer multiplexer = new InputMultiplexer();
		multiplexer.addProcessor(camController);
		Gdx.input.setInputProcessor(multiplexer);
	}

	public void setPosition(Vector3 pos) {
		this.position = pos;
	}

	public Vector3 getPosition() {
		return this.position.cpy();
	}

	public void update() {
		setPosition(camera.position.cpy().sub(new Vector3(0f, 1.5f, 0f)));
		camController.update(position);
	}
}