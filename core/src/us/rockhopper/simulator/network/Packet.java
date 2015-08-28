package us.rockhopper.simulator.network;

public class Packet {
	public static class Packet0TileChange {
		public int tileID;
		public String playerID;
	}

	public static class Packet1ScoreUpdate {
		public String playerID;
		public int newScore;
	}
	
	public static class Packet4Ready {
		public String name;
		public boolean ready;
	}

	// TODO could probably fix this with a screenSwitch packet determining which
	// screen to switch to
	public static class Packet5GameStart {
		public int subdivisions;
		public long seed;
	}
}