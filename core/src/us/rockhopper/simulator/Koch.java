package us.rockhopper.simulator;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

public class Koch implements ApplicationListener {

	class Line {
		float x, y, x1, y1;
		float length;

		Line(float x, float y, float x1, float y1) {
			this.x = x;
			this.y = y;
			this.x1 = x1;
			this.y1 = y1;
			length = (float) Math.sqrt(Math.pow((x1 - x), 2)
					+ Math.pow((y1 - y), 2));
		}

		public void split() {
			float xa = (2f / 3f) * x + (1f / 3f) * x1;
			float ya = (2f / 3f) * y + (1f / 3f) * y1;

			float xb = (1f / 3f) * x + (2f / 3f) * x1;
			float yb = (1f / 3f) * y + (2f / 3f) * y1;

			// float height = 0.288675f * length;
			float height = (float) (((length / 3f) / 2f) * Math.sqrt(3));
			float midX = (1f / 2f) * x + (1f / 2f) * x1;
			float midY = (1f / 2f) * y + (1f / 2f) * y1;

			float slope = (y1 - y) / (x1 - x);

			float xt, yt;

			if (slope == 0) {
				xt = midX;
				yt = midY + height;
			} else {
				xt = midX;
				yt = midY;

				float perp = -1f / slope;
				float hypot = (float) Math.sqrt(Math.pow(perp, 2) + 1);
				float ratio = (height / hypot);

				System.out.println("Slope " + slope + " " + perp + " " + hypot
						+ " height " + height + " ratio " + ratio); // TODO:
																	// figure
																	// out the
																	// right
																	// length?

				// // float ratio = 10;
				// if (perp < 0) {
				// xt = midX - 1 * ratio;
				// yt = midY + Math.abs(perp) * ratio;
				// } else {
				// xt = midX + 1 * ratio;
				// yt = midY + perp * ratio;
				// }
			}

			tempLines.add(new Line(x, y, xa, ya));
			tempLines.add(new Line(xa, ya, xt, yt));
			tempLines.add(new Line(xt, yt, xb, yb));
			tempLines.add(new Line(xb, yb, x1, y1));
		}
	}

	List<Line> lines = new ArrayList<Line>();
	List<Line> tempLines = new ArrayList<Line>();

	OrthographicCamera camera;
	ShapeRenderer shapeRenderer;
	int step = 1;
	int scale = 300;

	boolean up, down, left, right = false;

	@Override
	public void render() {
		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		Gdx.gl.glDisable(GL20.GL_BLEND);

		camera.update();
		shapeRenderer.setProjectionMatrix(camera.combined);

		// Camera movement
		if (up) {
			camera.position.set(camera.position.x, camera.position.y + 1f, 0);
		}
		if (down) {
			camera.position.set(camera.position.x, camera.position.y - 1f, 0);
		}
		if (left) {
			camera.position.set(camera.position.x - 1f, camera.position.y, 0);
		}
		if (right) {
			camera.position.set(camera.position.x + 1f, camera.position.y, 0);
		}

		shapeRenderer.begin(ShapeType.Filled);
		shapeRenderer.setColor(0, 0, 0, 1);

		for (Line l : lines) {
			shapeRenderer.line(l.x, l.y, l.x1, l.y1);
		}

		shapeRenderer.end();
	}

	@Override
	public void create() {
		shapeRenderer = new ShapeRenderer();

		lines.add(new Line(0, 0, 1 * scale, 0));

		camera = new OrthographicCamera(scale * 2, scale * 2);
		camera.position.set(scale / 2f, scale / 2f, 0);

		// Initialize input processing
		InputMultiplexer multiplexer = new InputMultiplexer();
		multiplexer.addProcessor(new InputAdapter() {

			@Override
			public boolean scrolled(int amount) {
				camera.zoom += amount / 20f;
				return true;
			}

			@Override
			public boolean keyDown(int keycode) {
				if (keycode == Keys.W) {
					up = true;
				}
				if (keycode == Keys.A) {
					left = true;
				}
				if (keycode == Keys.S) {
					down = true;
				}
				if (keycode == Keys.D) {
					right = true;
				}
				if (keycode == Keys.T) {
					fractal();
				}
				return true;
			}

			@Override
			public boolean keyUp(int keycode) {
				if (keycode == Keys.W) {
					up = false;
				}
				if (keycode == Keys.A) {
					left = false;
				}
				if (keycode == Keys.S) {
					down = false;
				}
				if (keycode == Keys.D) {
					right = false;
				}
				return true;
			}
		});
		Gdx.input.setInputProcessor(multiplexer);
	}

	private void fractal() {
		System.out.println("Stepping fractal.");

		for (Line l : lines) {
			l.split();
		}
		lines.clear();
		lines.addAll(tempLines);
		tempLines.clear();

		// camera.position.set(scale / 2f, scale / 2f, 0);
		// step++;
		// scale /= 1.4f;
	}

	@Override
	public void dispose() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void resize(int width, int height) {
	}

	@Override
	public void pause() {
	}
}
