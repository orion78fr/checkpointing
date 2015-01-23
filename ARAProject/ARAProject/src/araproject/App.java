package araproject;

import java.util.Stack;

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
	private Stack<Checkpoint> checkpoints;
	private int rollbackNbr;
	private boolean inRollback;
	
	// Used if forcing domino effect
	private static volatile int prevDominoId;

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
		this.checkpoints = new Stack<Checkpoint>();
	}

	@Override
	public void processEvent(Node node, int pid, Object event) {
		Message mess = (Message)event;
		switch(mess.getType()){
		case APPLICATIVE:
			if(this.inRollback){
				// Ignore messages during rollback
				break;
			}
			if(mess.getRollbackNbr() != this.rollbackNbr) { 
				// Ignore message coming from a previous rollback
				break;
			}
			int sender = mess.getSender();
			System.out.printf("[%d %d] Message n°%d from %d received", CommonState.getTime(), this.nodeId, received[sender], sender);
			received[sender]++;
			if(Constants.isDomino()){
				if(prevDominoId == sender){
					synchronized(App.class){
						if(prevDominoId == sender){
							prevDominoId = this.nodeId;
						}
					}
					System.out.printf(" (+ Forced checkpoint)", CommonState.getTime(), this.nodeId);
					doCheckPoint(this.rollbackNbr);
				}
			}
			System.out.println();
			break;
		case CHECKPOINT:
			doCheckPoint(mess.getRollbackNbr());
			break;
		case ROLLBACKSTART:
			startRollback();
			break;
		case ROLLBACKSTEP:
			doRollback(mess);
			break;
		case ROLLBACKEND:
			if(this.inRollback && mess.getRollbackNbr() == this.rollbackNbr){
				// No rollback in restart timeout
				this.inRollback = false;
				planNextStep();
				planNextCheckpoint();
				System.out.printf("[%d %d] Ended rollback n°%d, Restarting...\n", CommonState.getTime(), this.nodeId, this.rollbackNbr);
			}
			break;
		case STEP:
			doStep(mess.getRollbackNbr());
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
	
	private void send(Message msg, Node dest) {
		sent[(int) dest.getID()]++;
		this.transport.send(getMyNode(), dest, msg, this.mypid);
	}
	
	private Node getMyNode() {
		return Network.get(this.nodeId);
	}
	
	private void rollbackTo(Checkpoint c) {
		rollbackNbr++;
		
		state = c.getState();
		sent = c.getSent();
		received = c.getReceived();
		
		// Sending sent[i] to i
		for(int i = 0; i < Network.size(); i++){
			if(i != this.nodeId){
				this.transport.send(getMyNode(), Network.get(i), new Message(Message.Type.ROLLBACKSTEP, sent[i], this.rollbackNbr, this.nodeId), this.mypid);
			}
		}
	}
	
	private void planRestart(){
		// Planning execution restart
		EDSimulator.add(Constants.getRestartDelay(), new Message(Message.Type.ROLLBACKEND, 0, rollbackNbr, this.nodeId), this.getMyNode(), this.mypid);
	}
	
	private void startRollback(){
		this.inRollback = true;
		
		Checkpoint c = this.checkpoints.pop();
		
		System.out.printf("[%d %d] ROLLBACK - Initiating rollback n°%d (State : %d -> %d)\n", CommonState.getTime(), this.nodeId, this.rollbackNbr, this.state, c.getState());
		
		rollbackTo(c);
		
		planRestart();
	}
	
	private void doRollback(Message msg){
		this.rollbackNbr = Math.max(this.rollbackNbr, msg.getRollbackNbr());
		this.inRollback = true;
		
		int from = msg.getSender();
		if(received[from] > msg.getMsg()){
			// Rollback needed
			Checkpoint c;
			do {
				c = checkpoints.pop();
			} while (c.getReceived()[from] > msg.getMsg()); // Looking for a consistent rollback
			
			checkpoints.push(c); // The checkpoint is valid, so we can reuse it later
			
			System.out.printf("[%d %d] ROLLBACK - Rollback n°%d, forced by node n°%d (State : %d -> %d)\n", CommonState.getTime(), this.nodeId, this.rollbackNbr, from, this.state, c.getState());
			
			rollbackTo(c);
		}
		
		planRestart();
	}
	
	private void doCheckPoint(int messRollbackNbr){
		if(!this.inRollback){
			planNextCheckpoint();
		}
		
		if(messRollbackNbr != this.rollbackNbr){
			// Message to ignore, planned before previous rollback
			return;
		}
		
		System.out.printf("[%d %d] Checkpoint n°%d, State %d\n", CommonState.getTime(), this.nodeId, checkpoints.size()+1, this.state);
		
		this.checkpoints.push(new Checkpoint(state, sent, received));
	}
	
	private void planNextCheckpoint(){
		// Next checkpoint
		if(!Constants.isDomino()){ // If domino, we force checkpoint on message reception
			int delay = Constants.getCheckpointDelayMin() + CommonState.r.nextInt(Constants.getCheckpointDelayMax() - Constants.getCheckpointDelayMin() + 1);
			EDSimulator.add(delay, new Message(Message.Type.CHECKPOINT, 0, rollbackNbr, this.nodeId), this.getMyNode(), this.mypid);
		}
	}
	
	private void planNextStep(){
		// Next step
		int delay = Constants.getStepDelayMin() + CommonState.r.nextInt(Constants.getStepDelayMax() - Constants.getStepDelayMin() + 1);
		EDSimulator.add(delay, new Message(Message.Type.STEP, 0, rollbackNbr, this.nodeId), this.getMyNode(), this.mypid);
	}
	
	private void doStep(int messRollbackNbr){
		if(!this.inRollback){
			// Do not plan execution during a rollback
			planNextStep();
		}
		
		if(messRollbackNbr != this.rollbackNbr) { 
			// Message to ignore, planned before previous rollback
			return;
		}
		
		System.out.printf("[%d %d] State change : %d -> %d", CommonState.getTime(), this.nodeId, this.state, this.state+1);
		
		state++;
		
		// Random sending
		double r = CommonState.r.nextDouble();
		if(r <= Constants.getProbaUnicast()) {
			// Unicast
			int dest;
			while((dest = CommonState.r.nextInt(Network.size())) == this.nodeId); // Get an id different from self
			System.out.printf(" and Sending message n°%d to %d", sent[dest], dest);
			send(new Message(Message.Type.APPLICATIVE, 0, rollbackNbr, this.nodeId), Network.get(dest));
		} else if(r >= 1 - Constants.getProbaBroadcast()){
			// Bcast
			System.out.print(" and Broadcasting message");
			for(int i = 0; i < Network.size(); i++){
				if(i != this.nodeId){
					send(new Message(Message.Type.APPLICATIVE, 0, rollbackNbr, this.nodeId), Network.get(i));
				}
			}
		}
		
		System.out.println();
	}
}
