package araproject;

import peersim.core.Network;
import peersim.core.Node;
import peersim.core.Protocol;
import peersim.edsim.EDSimulator;

public class MatrixTransport implements Protocol {
	private static long matrix[][];

	public MatrixTransport(String prefix) {
		System.out.println("Transport Layer Enabled");
		
		int size = Network.size();
		matrix = new long[size][];
		for(int i = 0; i < size; i++){
			matrix[i] = new long[size];
			for(int j = 0; j < size; j++){
				if(i != j){
					matrix[i][j] = 1;
				} else {
					matrix[i][j] = 0;
				}
			}
		}
	}

	public Object clone() {
		return this;
	}

	public void send(Node src, Node dest, Object msg, int pid) {
		long delay = getLatency(src, dest);
		EDSimulator.add(delay, msg, dest, pid);
	}

	public long getLatency(Node src, Node dest) {
		return matrix[(int) src.getID()][(int) dest.getID()];
	}
}
