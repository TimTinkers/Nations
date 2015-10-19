package us.rockhopper.simulator.surface;

import com.badlogic.gdx.math.Vector3;

public class Player {

	private World world;

	// Character dimensions
	private float width = 0.5f;
	private float height = 1f;
	private float depth = 0.3f;
	private Vector3 centrePos = new Vector3(0.0f, 0.0f, 0.0f);

	private Hitbox hitbox = new Hitbox(centrePos, width, height, depth);

	private float rotation = 0.0f;
	private float rotationModifier;

	private float speed = 3.0f;
	private Vector3 velocity = new Vector3(0.0f, 0.0f, 0.0f);

	public Player(World world) {
		this.world = world;
	}

	public void moveForward(float delta) {
		velocity.set(-(float) (Math.sin(Math.toRadians(getRotation())) * speed), 0,
				-(float) (Math.cos(Math.toRadians(getRotation())) * speed));
		rotationModifier = 0;
		tryMove(delta);
	}

	public void moveBackward(float delta) {
		velocity.set((float) (Math.sin(Math.toRadians(getRotation())) * speed), 0,
				(float) (Math.cos(Math.toRadians(getRotation())) * speed));
		rotationModifier = 0;
		tryMove(delta);
	}

	public void strafeLeft(float delta) {
		velocity.set(-(float) (Math.cos(Math.toRadians(getRotation())) * speed), 0,
				(float) (Math.sin(Math.toRadians(getRotation())) * speed));
		rotationModifier = 0;
		tryMove(delta);
	}

	public void strafeRight(float delta) {
		velocity.set((float) (Math.cos(Math.toRadians(getRotation())) * speed), 0,
				-(float) (Math.sin(Math.toRadians(getRotation())) * speed));
		rotationModifier = 0;
		tryMove(delta);
	}

	public void turnLeft(float delta) {
		velocity.set(0.0f, 0.0f, 0.0f);
		rotationModifier = speed * 50 * delta;
		tryMove(delta);
	}

	public void turnRight(float delta) {
		velocity.set(0.0f, 0.0f, 0.0f);
		rotationModifier = speed * -50 * delta;
		tryMove(delta);
	}

	public void stopMoving() {
		velocity.set(0.0f, 0.0f, 0.0f);
		rotationModifier = 0;
	}

	private void tryMove(float delta) {
		// create temporary backups of centrePos, velocity, and rotation...
		Vector3 centrePosBackup = new Vector3(centrePos);
		Vector3 velocityBackup = new Vector3(velocity);
		float rotationBackup = getRotation();
		// apply movement
		centrePos.add(velocity.x * delta, velocity.y * delta, velocity.z * delta);
		setRotation(getRotation() + rotationModifier);
		hitbox.updateBounds(getRotation());
		// if blocking collision at new position...
		if (world.collision()) {
			// ...undo move
			centrePos = centrePosBackup;
			velocity = velocityBackup;
			setRotation(rotationBackup);
			hitbox.updateBounds(getRotation());
		}
	}

	public void setRotation(float rotation) {
		this.rotation = rotation;
	}

	public Vector3 getCentrePos() {
		return centrePos;
	}

	// public Vector2 getHitboxFrontRight() {
	// return hitboxFrontRight;
	// }
	//
	// public Vector2 getHitboxBackRight() {
	// return hitboxBackRight;
	// }
	//
	// public Vector2 getHitboxBackLeft() {
	// return hitboxBackLeft;
	// }
	//
	// public Vector2 getHitboxFrontLeft() {
	// return hitboxFrontLeft;
	// }

	public float getWidth() {
		return width;
	}

	public float getDepth() {
		return depth;
	}

	public float getRotation() {
		return rotation;
	}

	public float getRotationModifier() {
		return rotationModifier;
	}

	public Hitbox getHitBox() {
		return this.hitbox;
	}
}
