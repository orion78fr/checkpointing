package araproject;

import java.util.ArrayList;
import java.util.List;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;

public class App implements EDProtocol{
	private int transportPid;
    private MatrixTransport transport;
	private int mypid;
	private int nodeId;
	private String prefix;
	
	// Node State
	private int state;
	private int[] sent;
	private int[] received;
	private List<Checkpoint> checkpoints;
	private int rollbackNbr;

	public App(String prefix) {
		this.prefix = prefix;
		this.transportPid = Configuration.getPid(prefix + ".transport");
		this.mypid = Configuration.getPid(prefix + ".myself");
		this.transport = null;
		
		this.state = 0;
		this.rollbackNbr = 0;
		
		int size = Network.size();
		this.sent = new int[size];
		this.received = new int[size];
		for(int i = 0; i<size; i++){
			this.sent[i] = 0;
			this.received[i] = 0;
		}
		this.checkpoints = new ArrayList<Checkpoint>();
	}

	@Override
	public void processEvent(Node node, int pid, Object event) {
		Message mess = (Message)event;
		switch(mess.getType()){
		case APPLICATIVE:
			int sender = mess.getSender();
			System.out.printf("[%d %d] Message n°%d from %d received\n", CommonState.getTime(), this.nodeId, received[sender], sender);
			received[sender]++;
			break;
		case CHECKPOINT:
			System.out.printf("[%d %d] Checkpoint n°%d, State %d\n", CommonState.getTime(), this.nodeId, checkpoints.size()+1, this.state);
			doCheckPoint();
			break;
		case ROLLBACKSTART:
			
			break;
		case ROLLBACKSTEP:
			
			break;
		case STEP:
			System.out.printf("[%d %d] State change : %d -> %d", CommonState.getTime(), this.nodeId, this.state, this.state+1);
			doStep();
			System.out.println();
			break;
		}
	}
	
	public void setTransportLayer(int nodeId) {
		this.nodeId = nodeId;
		this.transport = (MatrixTransport) Network.get(this.nodeId).getProtocol(this.transportPid);
	}
	
	public Object clone() {
		return new App(this.prefix);
	}
	
	public void send(Message msg, Node dest) {
		sent[(int) dest.getID()]++;
		this.transport.send(getMyNode(), dest, msg, this.mypid);
	}
	
	private Node getMyNode() {
		return Network.get(this.nodeId);
	}
	
	private void doCheckPoint(){
		// Next checkpoint
		int delay = Constants.getCheckpointDelayMin() + CommonState.r.nextInt(Constants.getCheckpointDelayMax() - Constants.getCheckpointDelayMin() + 1);
		EDSimulator.add(delay, new Message(Message.Type.CHECKPOINT, 0, rollbackNbr, this.nodeId), this.getMyNode(), this.mypid);
		
		this.checkpoints.add(new Checkpoint(state, sent, received));
	}
	
	private void doStep(){
		// Next step
		int delay = Constants.getStepDelayMin() + CommonState.r.nextInt(Constants.getStepDelayMax() - Constants.getStepDelayMin() + 1);
		EDSimulator.add(delay, new Message(Message.Type.STEP, 0, rollbackNbr, this.nodeId), this.getMyNode(), this.mypid);
		
		state++;
		
		// Random sending
		double r = CommonState.r.nextDouble();
		if(r <= Constants.getProbaUnicast()) {
			// Unicast
			int dest;
			while((dest = CommonState.r.nextInt(Network.size())) == this.nodeId); // Tirer un id différent du sien
			System.out.printf(" and Sending message n°%d to %d", sent[dest], dest);
			send(new Message(Message.Type.APPLICATIVE, 0, rollbackNbr, this.nodeId), Network.get(dest));
		} else if(r >= 1 - Constants.getProbaBroadcast()){
			// Bcast
			System.out.printf(" and Broadcasting message");
			for(int i = 0; i < Network.size(); i++){
				if(i != this.nodeId){
					send(new Message(Message.Type.APPLICATIVE, 0, rollbackNbr, this.nodeId), Network.get(i));
				}
			}
		}
	}

}
