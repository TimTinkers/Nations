package us.rockhopper.simulator.screen;

import us.rockhopper.simulator.Planet;
import us.rockhopper.simulator.network.MultiplayerClient;
import us.rockhopper.simulator.network.MultiplayerServer;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class ConnectionScreen extends ScreenAdapter {

	private Stage stage;
	private Table table;
	private Skin skin;
	private MultiplayerClient client;

	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		stage.act(delta);
		stage.draw();
	}

	@Override
	public void resize(int width, int height) {
		stage.getViewport().update(width, height, true);
		table.invalidateHierarchy();
	}

	@Override
	public void show() {
		skin = new Skin(Gdx.files.internal("assets/ui/uiskin.json"),
				new TextureAtlas("assets/ui/uiskin.pack"));
		stage = new Stage();
		table = new Table(skin);
		table.setFillParent(true);
		table.debug();

		Gdx.input.setInputProcessor(stage);

		final TextField ip = new TextField("Enter an IP", skin);
		final TextField nameField = new TextField("Your name?", skin);
		final TextField portField = new TextField("Port", skin);
		final TextField subdivisions = new TextField("Subdivisions", skin);
		final TextField seed = new TextField("Seed", skin);
		final TextField attrition = new TextField("Attrition", skin);

		TextButton serverStartButton = new TextButton("Host a Game", skin,
				"default");
		ClickListener serverStartListener = new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				// TODO: add some sort of loading screen/bar for server start
				// here.
				Planet planet = new Planet(Integer.parseInt(subdivisions
						.getText()), Long.parseLong(seed.getText()));

				new MultiplayerServer(portField.getText(), planet, Integer.parseInt(attrition.getText()));
				// TODO move logging in to the very first post-Splash page
				// Account user = new Account(nameField.getText());

				client = new MultiplayerClient(nameField.getText(),
						ip.getText(), portField.getText());
				// TODO: move this planet creation and specification to the
				// lobby itself
				((Game) Gdx.app.getApplicationListener()).setScreen(new Lobby(
						client));
			}
		};

		TextButton clientStartButton = new TextButton("Join a Game", skin,
				"default");
		ClickListener clientStartListener = new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				// Account user = new Account(nameField.getText());
				client = new MultiplayerClient(nameField.getText(),
						ip.getText(), portField.getText());
				((Game) Gdx.app.getApplicationListener()).setScreen(new Lobby(
						client));
			}
		};

		serverStartButton.addListener(serverStartListener);
		clientStartButton.addListener(clientStartListener);
		table.add(ip);
		table.add(portField);
		table.add(nameField);
		table.add(clientStartButton).row();
		table.add(subdivisions);
		table.add(seed);
		table.add(attrition);
		table.add(serverStartButton).row();

		stage.addActor(table);
	}

	@Override
	public void hide() {
		dispose();
	}

	@Override
	public void dispose() {
		stage.dispose();
		skin.dispose();
	}
}
