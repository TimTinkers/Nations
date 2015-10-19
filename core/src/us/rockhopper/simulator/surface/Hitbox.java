package us.rockhopper.simulator.surface;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.math.Vector3;

public class Hitbox {

	float width;
	float height;
	float depth;

	Vector3 center;

	private Vector3 hitboxFrontBotRight = new Vector3(0.0f, 0.0f, 0.0f);
	private Vector3 hitboxBackBotRight = new Vector3(0.0f, 0.0f, 0.0f);
	private Vector3 hitboxBackBotLeft = new Vector3(0.0f, 0.0f, 0.0f);
	private Vector3 hitboxFrontBotLeft = new Vector3(0.0f, 0.0f, 0.0f);

	List<Vector3> pointList;

	public Hitbox(Vector3 center, float width, float height, float depth) {
		this.center = center;
		this.width = width;
		this.height = height;
		this.depth = depth;

		pointList = new ArrayList<Vector3>();
		pointList.add(hitboxFrontBotRight);
		pointList.add(hitboxBackBotRight);
		pointList.add(hitboxBackBotLeft);
		pointList.add(hitboxFrontBotLeft);
	}

	void updateBounds(float rotation) {
		float cosTheta = (float) (Math.cos(Math.toRadians(rotation)));
		float sinTheta = (float) (Math.sin(Math.toRadians(rotation)));

		hitboxFrontBotRight.x = (float) (center.x + (width * 0.3 * cosTheta) - (depth * 0.3 * sinTheta));
		hitboxFrontBotRight.z = (float) (center.z + (width * 0.3 * sinTheta) + (depth * 0.3 * cosTheta));
		hitboxBackBotRight.x = (float) (center.x + (width * 0.3 * cosTheta) - (-depth * 0.3 * sinTheta));
		hitboxBackBotRight.z = (float) (center.z + (width * 0.3 * sinTheta) + (-depth * 0.3 * cosTheta));
		hitboxBackBotLeft.x = (float) (center.x + (-width * 0.3 * cosTheta) - (-depth * 0.3 * sinTheta));
		hitboxBackBotLeft.z = (float) (center.z + (-width * 0.3 * sinTheta) + (-depth * 0.3 * cosTheta));
		hitboxFrontBotLeft.x = (float) (center.x + (-width * 0.3 * cosTheta) - (depth * 0.3 * sinTheta));
		hitboxFrontBotLeft.z = (float) (center.z + (-width * 0.3 * sinTheta) + (depth * 0.3 * cosTheta));
	}

	public List<Vector3> getPoints() {
		return pointList;
	}
}