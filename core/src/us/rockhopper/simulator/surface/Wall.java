package us.rockhopper.simulator.surface;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;

public class Wall {
	public Vector3 center;
	public float width;
	public float height;
	
	public HexBounds bounds;
	public ModelInstance rendered;
	
	//public Rectangle bounds;

	public Wall(Vector3 center, float width, float height) {
		this.center = center;
		this.width = width;
		this.height = height;
		bounds = new HexBounds(center, width, height);
	}
}