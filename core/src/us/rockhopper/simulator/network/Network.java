package us.rockhopper.simulator.network;

import us.rockhopper.simulator.network.Packet.Packet0TileChange;
import us.rockhopper.simulator.network.Packet.Packet1ScoreUpdate;
import us.rockhopper.simulator.network.Packet.Packet4Ready;
import us.rockhopper.simulator.network.Packet.Packet5GameStart;

import com.esotericsoftware.kryo.Kryo;

public class Network {
	public static void register(Kryo kryo) {
		kryo.register(Packet0TileChange.class);
		kryo.register(Packet1ScoreUpdate.class);
		kryo.register(Packet4Ready.class);
		kryo.register(Packet5GameStart.class);
	}
}