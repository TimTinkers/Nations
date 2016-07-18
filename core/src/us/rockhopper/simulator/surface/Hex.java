package us.rockhopper.simulator.surface;

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

public class Hex {
	public Vector3 center;
	public float width;
	public float height;

	private ModelBuilder mb = new ModelBuilder();
	public ModelInstance rendered;

	private Random random = new Random();
	private Color[] colors = { Color.PINK, Color.BLUE, Color.GREEN, Color.WHITE, Color.CORAL, Color.FIREBRICK,
			Color.GOLD, Color.GOLDENROD, Color.ORANGE, Color.LIGHT_GRAY, Color.OLIVE, Color.RED, Color.CHARTREUSE,
			Color.CYAN };

	public Hex(Vector3 center, float width, float height) {
		this.center = center;
		this.width = width;
		this.height = height;
	}

	public void draw() {
		mb.begin();
		MeshPartBuilder hexDraw = mb.part("hex", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal,
				new Material(ColorAttribute.createDiffuse(colors[random.nextInt(colors.length)])));

		// Radius
		float r = (width / 2f);
		float xCon = 0.5f;
		float zCon = (float) (Math.sqrt(3) / 2);

		// Top
		Vector3 topPlane = center.cpy().add(0, height / 2f, 0);
		Vector3 t0 = topPlane.cpy().add(0, 0, -r);
		Vector3 t1 = topPlane.cpy().add(r * zCon, 0, -r * xCon);
		Vector3 t2 = topPlane.cpy().add(r * zCon, 0, r * xCon);
		Vector3 t3 = topPlane.cpy().add(0, 0, r);
		Vector3 t4 = topPlane.cpy().add(-r * zCon, 0, r * xCon);
		Vector3 t5 = topPlane.cpy().add(-r * zCon, 0, -r * xCon);

		// Bottom
		Vector3 botPlane = center.cpy().add(0, -height / 2f, 0);
		Vector3 b0 = botPlane.cpy().add(0, 0, -r);
		Vector3 b1 = botPlane.cpy().add(r * zCon, 0, -r * xCon);
		Vector3 b2 = botPlane.cpy().add(r * zCon, 0, r * xCon);
		Vector3 b3 = botPlane.cpy().add(0, 0, r);
		Vector3 b4 = botPlane.cpy().add(-r * zCon, 0, r * xCon);
		Vector3 b5 = botPlane.cpy().add(-r * zCon, 0, -r * xCon);

		// Top
		hexDraw.triangle(t5, t1, t0);
		hexDraw.triangle(t5, t2, t1);
		hexDraw.triangle(t5, t4, t2);
		hexDraw.triangle(t4, t3, t2);

		// Sides
		hexDraw.triangle(b0, t5, t0);
		hexDraw.triangle(b0, b5, t5);
		hexDraw.triangle(b5, t4, t5);
		hexDraw.triangle(b5, b4, t4);
		hexDraw.triangle(b4, t3, t4);
		hexDraw.triangle(b4, b3, t3);
		hexDraw.triangle(b3, t2, t3);
		hexDraw.triangle(b3, b2, t2);
		hexDraw.triangle(b2, t1, t2);
		hexDraw.triangle(b2, b1, t1);
		hexDraw.triangle(b1, t0, t1);
		hexDraw.triangle(b1, b0, t0);

		Model m = mb.end();
		rendered = new ModelInstance(m);
	}
}