package us.rockhopper.simulator.network;

import java.io.IOException;

import us.rockhopper.simulator.network.Packet.Packet0TileChange;
import us.rockhopper.simulator.network.Packet.Packet4Ready;

import com.badlogic.gdx.Gdx;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;

public class MultiplayerClient {
	private Client client;
	// private Account user;
	public String user;
	public int id;

	public MultiplayerClient(String user, String ip, String port) {
		Log.set(Log.LEVEL_DEBUG);
		client = new Client();
		this.user = user;
		this.registerPackets();
		// Client listening on its own thread.
		new Thread(client).start();

		try {
			client.connect(5000, ip, Integer.parseInt(port));
		} catch (IOException e) {
			e.printStackTrace();
			client.stop();
		}
	}

	private void registerPackets() {
		Kryo kryo = client.getKryo();
		Network.register(kryo);
	}

	public void addListener(Listener listener) {
		client.addListener(new Listener.QueuedListener(listener) {
			protected void queue(Runnable runnable) {
				Gdx.app.postRunnable(runnable);
			}
		});
	}

	public void sendTile(String playerID, int tileID) {
		Packet0TileChange packet = new Packet0TileChange();
		packet.tileID = tileID;
		packet.playerID = playerID;
		client.sendTCP(packet);
	}

	public void sendReady(boolean ready) {
		Packet4Ready packet = new Packet4Ready();
		packet.name = user;
		packet.ready = ready;
		client.sendTCP(packet);
	}
}
