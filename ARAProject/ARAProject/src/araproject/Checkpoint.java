package araproject;

import java.util.Arrays;

public class Checkpoint {
	private final int state;
	private final int[] sent;
	private final int[] received;

	public Checkpoint(int state, int[] sent, int[] received) {
		this.state = state;
		// Defensives copy to prevent modifications
		this.sent = Arrays.copyOf(sent, sent.length);
		this.received = Arrays.copyOf(received, received.length);
	}

	public int getState() {
		return state;
	}

	public int[] getSent() {
		return Arrays.copyOf(sent, sent.length);
	}

	public int[] getReceived() {
		return Arrays.copyOf(received, received.length);
	}
}
