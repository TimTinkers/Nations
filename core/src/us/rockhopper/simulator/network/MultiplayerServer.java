package us.rockhopper.simulator.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;

import us.rockhopper.simulator.Planet;
import us.rockhopper.simulator.Planet.Tile;
import us.rockhopper.simulator.network.Packet.Packet0TileChange;
import us.rockhopper.simulator.network.Packet.Packet1ScoreUpdate;
import us.rockhopper.simulator.network.Packet.Packet4Ready;
import us.rockhopper.simulator.network.Packet.Packet5GameStart;

public class MultiplayerServer extends Listener {
	private Server server;
	private HashMap<String, Boolean> players;
	private Planet planet;
	private int attrition;

	Color colors[] = { Color.BLUE, Color.GREEN, Color.DARK_GRAY, Color.RED, Color.CYAN, Color.WHITE, Color.YELLOW,
			Color.ORANGE, Color.GRAY, Color.OLIVE, Color.PINK, Color.LIGHT_GRAY };

	List<String> turns = new ArrayList<String>();
	HashMap<Tile, String> tileControl = new HashMap<Tile, String>();
	HashMap<String, Integer> scores = new HashMap<String, Integer>();
	int turn = 0;

	// Game loop logic.
	// private final float TIMESTEP = 1 / 60f;
	// private boolean start;
	// private boolean gameLogic;

	public MultiplayerServer(String port, Planet planet, int attrition) {
		this.attrition = attrition;
		this.planet = planet;
		Log.set(Log.LEVEL_DEBUG);
		server = new Server();
		this.registerPackets();

		Random random = new Random();

		// Randomly block a lot of tiles.
		// int blocked = planet.tiles.size() / 3;
		// while (blocked > 0) {
		// int rID = random.nextInt(planet.tiles.size());
		// tileControl.put(planet.tiles.get(rID), "HOLE");
		// Packet0TileChange tc = new Packet0TileChange();
		// tc.playerID = "HOLE";
		// tc.tileID = rID;
		// server.sendToAllTCP(tc);
		// blocked--;
		// }

		// Server listening on its own thread.
		server.addListener(new Listener.QueuedListener(this) {
			protected void queue(Runnable runnable) {
				Gdx.app.postRunnable(runnable);
			}
		});

		try {
			server.bind(Integer.parseInt(port));

			// Start the server listening for input
			new Thread(server).start();

			// // In case you needed a gameloop, this would be it.
			// Timer t = new Timer();
			//
			// final Runnable runnable = new Runnable() {
			// int step = 0;
			//
			// @Override
			// public void run() {
			// if (!gameLogic) {
			// // Setup logic before update.
			// gameLogic = true;
			// }
			//
			// update();
			// }
			// };
			// t.schedule(new TimerTask() {
			//
			// @Override
			// public void run() {
			// if (start) {
			// // Schedule the server loop to run.
			// Gdx.app.postRunnable(runnable);
			// }
			// }
			// }, 0L, 15);

			System.out.println("[SERVER] Started new server.");
			players = new HashMap<String, Boolean>();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void registerPackets() {
		Kryo kryo = server.getKryo();
		Network.register(kryo);
	}

	@Override
	public void connected(Connection c) {
		System.out.println("[SERVER] " + c.getID() + " connected.");
	}

	@Override
	public void disconnected(Connection c) {
		System.out.println("[SERVER] " + c.getID() + " disconnected.");
	}

	@Override
	public void received(Connection c, Object o) {
		if (o instanceof Packet0TileChange) {
			Packet0TileChange player = ((Packet0TileChange) o);
			System.out.println("[SERVER] Player " + player.playerID + " goes for tile " + player.tileID + "...");

			// Check if a tile can be placed here
			// TODO: players able to override other tiles? FIXXX
			if (turns.get(turn).equals(player.playerID) && !tileControl.containsKey(player.tileID)) {
				tileControl.put(planet.tiles.get(player.tileID), player.playerID);

				// Check for kills on nearby tiles
				ArrayList<Tile> dead = new ArrayList<Tile>();
				for (Tile adjacent : planet.adjacencies.get(planet.tiles.get(player.tileID))) {
					int localAttrition = 0;
					for (Tile adjacentOther : planet.adjacencies.get(adjacent)) {
						if (tileControl.get(adjacentOther) != null
								&& !tileControl.get(adjacentOther).equals(tileControl.get(adjacent))) {
							localAttrition++;
						}
					}
					if (tileControl.containsKey(adjacent) && localAttrition >= attrition && !dead.contains(adjacent)) {
						dead.add(adjacent);
						scores.put(player.playerID, scores.get(player.playerID) + 1);
					}
				}

				// Send packets to cull the dead
				for (Tile t : dead) {
					Packet0TileChange pack = new Packet0TileChange();
					pack.playerID = null;
					pack.tileID = t.id;

					// Update the scores
					Packet1ScoreUpdate score = new Packet1ScoreUpdate();
					int oldScore = scores.get(tileControl.get(t));
					score.playerID = tileControl.get(t);
					score.newScore = oldScore - 1;
					scores.put(tileControl.get(t), oldScore - 1);

					tileControl.remove(t);

					server.sendToAllTCP(score);
					server.sendToAllTCP(pack);
				}

				// TODO: should probably reorder all of this
				// Check if this tile dies too
				int localAttrition = 0;
				boolean diesToo = false;
				for (Tile t : planet.adjacencies.get(planet.tiles.get(player.tileID))) {
					if (tileControl.get(t) != null && !tileControl.get(t).equals(player.playerID)) {
						localAttrition++;
					}
				}
				if (localAttrition >= attrition) {
					tileControl.remove(planet.tiles.get(player.tileID));
					int oldScore = scores.get(player.playerID);
					scores.put(player.playerID, oldScore - 1);
					diesToo = true;
				}

				// Update the scores
				Packet1ScoreUpdate score = new Packet1ScoreUpdate();
				score.playerID = player.playerID;
				score.newScore = scores.get(player.playerID);
				server.sendToAllTCP(score);

				if (diesToo) {
					player.playerID = null;
				}
				server.sendToAllTCP(player);

				// Loop the turn
				turn++;
				if (turn == turns.size()) {
					turn = 0;
				}
			}
		} else if (o instanceof Packet4Ready) {
			Packet4Ready packet = (Packet4Ready) o;

			// If this is a player just entering the lobby
			if (!players.keySet().contains(packet.name)) {
				System.out.println("[SERVER] " + packet.name + " is joining the lobby.");

				// TODO: compensate for leaving the lobby
				turns.add(packet.name);
				scores.put(packet.name, 0);
				players.put(packet.name, false);
				server.sendToAllExceptTCP(c.getID(), packet);

				System.out.println("[SERVER] Getting " + packet.name + " up to speed.");
				for (String playerOtherName : players.keySet()) {
					Packet4Ready playerOther = new Packet4Ready();
					playerOther.name = playerOtherName;
					playerOther.ready = players.get(playerOtherName);
					server.sendToTCP(c.getID(), playerOther);
				}
			} else {
				System.out.println("[SERVER] " + packet.name + " is ready " + packet.ready + ".");
				players.put(packet.name, packet.ready);
				server.sendToAllExceptTCP(c.getID(), o);
			}

			// If every player is ready
			boolean ready = true;
			for (String playerName : players.keySet()) {
				if (!players.get(playerName)) {
					ready = false;
				}
			}
			if (ready) {
				Packet5GameStart planetPacket = new Packet5GameStart();
				planetPacket.subdivisions = planet.SUBDIVISIONS;
				planetPacket.seed = planet.SEED;
				server.sendToAllTCP(planetPacket);

				// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
				// TODO: find a way to reliably transmit the state of the map
				// from the server to the clients.
				// for (int i = 0; i < planet.tiles.size(); ++i) {
				// planet.tiles.get(i).setColor(Color.WHITE);
				// //TODO: this can be greatly streamlined
				// Packet0TileChange tc = new Packet0TileChange();
				// tc.playerID = "SERVER";
				// tc.tileID = i;
				// server.sendToAllTCP(tc);
				// }
				// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			}
		}
	}
}