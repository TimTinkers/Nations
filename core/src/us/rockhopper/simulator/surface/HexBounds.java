package us.rockhopper.simulator.surface;

import com.badlogic.gdx.math.Vector3;

public class HexBounds {
	/* @formatter:off */
	/*	Diagram of prism structure:
	 *					+x, +z 
	 * 			t1	t2
	 *		t0			t3
	 *	   |	t5	t4 	  |
	 *	   |   |	  |	  |	
	 *	   |   |b1	b2|	  |
	 *		b0 | 	  |	b3
	 *			b5	b4
	 *	-x, -z
	 */
	/* @formatter:on */

	// TODO: can use simple float tuples instead of Vector3's in order to save
	// memory
	public Vector3 center;
	public Vector3 t0;
	public Vector3 t1;
	public Vector3 t2;
	public Vector3 t3;
	public Vector3 t4;
	public Vector3 t5;
	public Vector3 b0;
	public Vector3 b1;
	public Vector3 b2;
	public Vector3 b3;
	public Vector3 b4;
	public Vector3 b5;

	float xCon = 0.5f;
	float zCon = (float) (Math.sqrt(3) / 2);
	float width;
	float height;

	HexBounds(Vector3 center, float width, float height) {
		this.center = center;
		this.width = width;
		this.height = height;
		// Radius
		float r = (width / 2f);

		// Top
		Vector3 topPlane = center.cpy().add(0, height / 2f, 0);
		t0 = topPlane.cpy().add(-r, 0, 0);
		t1 = topPlane.cpy().add(-r * xCon, 0, r * zCon);
		t2 = topPlane.cpy().add(r * xCon, 0, r * zCon);
		t3 = topPlane.cpy().add(r, 0, 0);
		t4 = topPlane.cpy().add(r * xCon, 0, -r * zCon);
		t5 = topPlane.cpy().add(-r * xCon, 0, -r * zCon);

		// Bottom
		Vector3 botPlane = center.cpy().add(0, -height / 2f, 0);
		b0 = botPlane.cpy().add(-r, 0, 0);
		b1 = botPlane.cpy().add(-r * xCon, 0, r * zCon);
		b2 = botPlane.cpy().add(r * xCon, 0, r * zCon);
		b3 = botPlane.cpy().add(r, 0, 0);
		b4 = botPlane.cpy().add(r * xCon, 0, -r * zCon);
		b5 = botPlane.cpy().add(-r * xCon, 0, -r * zCon);
	}

	// TODO create a generic interface for colliding bounds, and make the
	// contains method operate on one of those.

	public boolean contains(Hitbox hitBox) {
		for (Vector3 v : hitBox.getPoints()) {
			if (this.contains(v)) {
				return true;
			}
		}
		return false;
	}

	// TODO: generalize this code for potentially rotated hexes
	// Right now it assumes hexes which are static in their positions
	public boolean contains(Vector3 point) {
		//System.out.println(center.toString() + " " + point.toString());
		// Rough check of the bounding cube

		// TODO: for some reason these y's are never in bounds.

		if (point.y > t0.y || point.y < b0.y) {
			//System.out.println("y: " + t0.y);
			return false;
		}
		//System.out.println("Y in!");
		if (point.x < t0.x || point.x > t3.x) {
			return false;
		}
		if (point.z > t1.z || point.z < t5.z) {
			return false;
		}

		// This shouldn't run.
		// TODO: finish debugging this--it looks like it's giving some impossible output...
		System.out.println(t0.x + ", " + t0.y + ", " + t0.z + " Oops!");
		
		// Finer check to rule out the corners
		// Translate into hexagon space
		float _hori = 0.25f * width;
		float _vert = 0.5f * height;

		float inX = Math.abs(point.x - center.x);
		float inZ = Math.abs(point.z - center.z);

		if (inX > _hori || inZ > _vert * 2) {
			return false; // bounding test (since q2 is in quadrant 2 only 2
			// // tests are needed)
		}

		return (2 * _vert * _hori - _vert * inX - _hori * inZ) >= 0; // finally
																		// the
																		// dot
																		// product
																		// can
																		// be
																		// reduced
																		// to
																		// this
																		// due
																		// to
																		// the
																		// hexagon
																		// symmetry

		// return false;
	}
}