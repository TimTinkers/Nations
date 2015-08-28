package us.rockhopper.simulator.screen;

import java.util.HashMap;

import us.rockhopper.simulator.Planet;
import us.rockhopper.simulator.network.MultiplayerClient;
import us.rockhopper.simulator.network.Packet.Packet4Ready;
import us.rockhopper.simulator.network.Packet.Packet5GameStart;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

public class Lobby extends ScreenAdapter {
	private Stage stage;
	private Table table;
	private Skin skin;

	private MultiplayerClient client;

	private HashMap<String, Boolean> players;
	private String clientPlayerName;
	private boolean isReady = false;

	Lobby(MultiplayerClient client) {
		players = new HashMap<String, Boolean>();
		this.client = client;
		clientPlayerName = client.user;
	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		stage.act(delta);
		stage.draw();
	}

	public void updateTable() {
		for (String name : players.keySet()) {
			if (table.findActor(name) == null) {
				Label playerEntry = new Label(name, skin);
				playerEntry.setName(name);
				table.add(playerEntry).row();
			}
		}
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

		TextButton toggleReady = new TextButton("Ready", skin, "default");

		ClickListener toggleReadyListener = new ClickListener() {
			@Override
			public void clicked(final InputEvent event, float x, float y) {
				if (isReady) {
					isReady = false;
					table.findActor(clientPlayerName).setColor(1, 1, 1, 1);
					TextButton button = (TextButton) event.getListenerActor();
					button.setText("Go Ready");
					client.sendReady(isReady);
				} else {
					isReady = true;
					table.findActor(clientPlayerName).setColor(0, 1, 0, 1);
					TextButton button = (TextButton) event.getListenerActor();
					button.setText("Unready");
					client.sendReady(isReady);
				}
			}
		};

		toggleReady.addListener(toggleReadyListener);
		table.defaults().fillX();
		table.add(toggleReady);
		stage.addActor(table);

		// Client listeners
		this.client.addListener(new Listener() {

			@Override
			public void connected(Connection arg0) {
				System.out.println("[CLIENT] You connected.");
			}

			@Override
			public void disconnected(Connection arg0) {
				System.out.println("[CLIENT] You disconnected.");
			}

			@Override
			public void received(Connection c, Object o) {
				if (o instanceof Packet4Ready) {
					Packet4Ready packet = (Packet4Ready) o;

					// Add player labels to list upon them joining
					if (!players.keySet().contains(packet.name)) {
						System.out.println("[CLIENT] " + clientPlayerName
								+ " received information from " + packet.name);
						players.put(packet.name, false);
					} else {
						System.out.println("[CLIENT] " + packet.name
								+ " is ready " + packet.ready);
						players.put(packet.name, packet.ready);
					}
					updateTable();

					// Set color of all player slots to indicate readiness.
					if (packet.ready) {
						table.findActor(packet.name).setColor(0, 1, 0, 1);
					} else {
						table.findActor(packet.name).setColor(1, 1, 1, 1);
					}
				} else if (o instanceof Packet5GameStart) {
					Packet5GameStart packet = (Packet5GameStart) o;
					Planet planet = new Planet(packet.subdivisions, packet.seed);
					((Game) Gdx.app.getApplicationListener()).setScreen(new PlanetView(
							planet, client));
				}
			}
		});

		client.sendReady(false);
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