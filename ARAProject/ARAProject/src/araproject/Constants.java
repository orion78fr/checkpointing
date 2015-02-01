package araproject;

import peersim.config.Configuration;

public class Constants {
	private Constants() {
		throw new RuntimeException();
	}

	private static double probaUnicast;
	private static double probaBroadcast;
	private static int stepDelayMin;
	private static int stepDelayMax;
	private static int checkpointDelayMin;
	private static int checkpointDelayMax;
	private static int restartDelay;
	private static String outputFile;
	private static boolean isDomino;
	private static int heartbeatDelay;
	private static int heartbeatMargin;
	private static int firstHeartbeat;
	private static int test;

	public static void loadConstants() {
		probaUnicast = Configuration.getDouble("simulation.probaUnicast");
		probaBroadcast = Configuration.getDouble("simulation.probaBroadcast");

		stepDelayMin = Configuration.getInt("simulation.stepDelayMin");
		stepDelayMax = Configuration.getInt("simulation.stepDelayMax");
		checkpointDelayMin = Configuration.getInt("simulation.checkpointDelayMin");
		checkpointDelayMax = Configuration.getInt("simulation.checkpointDelayMax");

		restartDelay = Configuration.getInt("simulation.restartDelay");

		isDomino = Configuration.getBoolean("simulation.isDomino");

		outputFile = Configuration.getString("simulation.outputFile", null);

		heartbeatDelay = Configuration.getInt("simulation.heartbeatDelay");
		heartbeatMargin = Configuration.getInt("simulation.heartbeatMargin");
		firstHeartbeat = Configuration.getInt("simulation.firstHeartbeat");

		test = Configuration.getInt("simulation.test");
	}

	public static double getProbaUnicast() {
		return probaUnicast;
	}

	public static double getProbaBroadcast() {
		return probaBroadcast;
	}

	public static int getStepDelayMin() {
		return stepDelayMin;
	}

	public static int getStepDelayMax() {
		return stepDelayMax;
	}

	public static int getCheckpointDelayMin() {
		return checkpointDelayMin;
	}

	public static int getCheckpointDelayMax() {
		return checkpointDelayMax;
	}

	public static boolean isDomino() {
		return isDomino;
	}

	public static String getOutputFile() {
		return outputFile;
	}

	public static int getRestartDelay() {
		return restartDelay;
	}

	public static int getHeartbeatDelay() {
		return heartbeatDelay;
	}

	public static int getHeartbeatMargin() {
		return heartbeatMargin;
	}

	public static int getFirstHeartbeat() {
		return firstHeartbeat;
	}

	public static int getTest() {
		return test;
	}
}