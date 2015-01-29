package araproject;

import java.util.Stack;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Fallible;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;

public class App implements EDProtocol{
	private int transportPid;
    private static MatrixTransport transport;
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
	
	// Fault Detector
	private boolean[] suspect;
	private int[] lastHeartbeat;
	private int restartCount;
	private int heartbeatCount;
	
	// true if killed and restarting
	private boolean restarting;

	public App(String prefix) {
		this.prefix = prefix;
		this.transportPid = Configuration.getPid(prefix + ".transport");
		this.mypid = Configuration.getPid(prefix + ".myself");
		transport = null;
		
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
		
		this.suspect = new boolean[size];
		this.lastHeartbeat = new int[size];
		for(int i = 0; i<size; i++){
			this.suspect[i] = false;
			this.lastHeartbeat[i] = 0;
		}
		this.restartCount = 0;
		this.heartbeatCount = 0;
		this.restarting = false;
	}

	@Override
	public void processEvent(Node node, int pid, Object event) {
		Message mess = (Message)event;
		switch(mess.getType()){
		case APPLICATIVE:
			int sender = mess.getSender();
			Visualizer.receive(sender, this.nodeId);
			if(this.inRollback){
				// Ignore messages during rollback
				break;
			}
			if(mess.getRollbackNbr() != this.rollbackNbr) { 
				// Ignore message coming from a previous rollback
				break;
			}
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
		case STEPHEARTBEAT:
			doStepHeartbeat();
			break;
		case HEARTBEAT:
			receiveHeartbeat(mess.getSender(), mess.getMsg());
			break;
		case CHECKHEARTBEAT:
			doCheckHeartbeat(mess.getMsg(), mess.getRollbackNbr());
			break;
		case KILL:
			System.out.printf("[%d %d] kill received from %d\n", CommonState.getTime(), this.nodeId, mess.getSender());
			this.restarting = true;
			this.inRollback = false;
			Visualizer.kill(this.nodeId);
			Network.get(this.nodeId).setFailState(Fallible.DOWN);
			break;
		case RESTART:
			System.out.printf("[%d %d] restart received, before condition\n", CommonState.getTime(), this.nodeId);
			if(!this.inRollback){
				System.out.printf("[%d %d] restart received, after 1st cond, before 2nd\n", CommonState.getTime(), this.nodeId);
				if(mess.getRollbackNbr() == this.heartbeatCount){
					System.out.printf("[%d %d] restart received, after 2nd cond, before 3rd\n", CommonState.getTime(), this.nodeId);
					if(++this.restartCount >= 3){
						System.out.printf("[%d %d] 3 restart received\n", CommonState.getTime(), this.nodeId);
						startRollback();
						this.restarting = false;
						doStepHeartbeat();
					}
				}
			}
			break;
		}
	}

	public void setTransportLayer(int nodeId) {
		this.nodeId = nodeId;
		transport = (MatrixTransport) Network.get(this.nodeId).getProtocol(this.transportPid);
	}
	
	public Object clone() {
		return new App(this.prefix);
	}
	
	private void send(Message msg, Node dest) {
		sent[(int) dest.getID()]++;
		transport.send(getMyNode(), dest, msg, this.mypid);
	}
	
	private void sendHeartbeat(Message msg, Node dest) {
		transport.send(getMyNode(), dest, msg, this.mypid);
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
				transport.send(getMyNode(), Network.get(i), new Message(Message.Type.ROLLBACKSTEP, sent[i], this.rollbackNbr, this.nodeId), this.mypid);
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
		if(this.checkpoints.size() == 0){
			this.checkpoints.add(c);
		}
		
		System.out.printf("[%d %d] ROLLBACK - Initiating rollback n°%d (State : %d -> %d)\n", CommonState.getTime(), this.nodeId, this.rollbackNbr, this.state, c.getState());
		
		Visualizer.rollbackstart(this.nodeId);
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
			
			Visualizer.rollback(this.nodeId, checkpoints.size());
			System.out.printf("[%d %d] ROLLBACK - Rollback n°%d, forced by node n°%d (State : %d -> %d)\n", CommonState.getTime(), this.nodeId, this.rollbackNbr, from, this.state, c.getState());
			
			rollbackTo(c);
		}
		
		planRestart();
	}
	
	private void doCheckPoint(int messRollbackNbr){
		if(messRollbackNbr != this.rollbackNbr){
			// Message to ignore, planned before previous rollback
			return;
		}
		
		if(!this.inRollback){
			planNextCheckpoint();
		}
		
		System.out.printf("[%d %d] Checkpoint n°%d, State %d\n", CommonState.getTime(), this.nodeId, checkpoints.size()+1, this.state);
		Visualizer.checkpoint(this.nodeId, checkpoints.size()+1);
		
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
		if(messRollbackNbr != this.rollbackNbr) { 
			// Message to ignore, planned before previous rollback
			return;
		}
		if(!this.inRollback){
			// Do not plan execution during a rollback
			planNextStep();
		}
		
		System.out.printf("[%d %d] State change : %d -> %d", CommonState.getTime(), this.nodeId, this.state, this.state+1);
		Visualizer.step(this.nodeId, this.state+1);
		
		state++;
		
		// Random sending
		double r = CommonState.r.nextDouble();
		if(r <= Constants.getProbaUnicast()) {
			// Unicast
			int dest;
			while((dest = CommonState.r.nextInt(Network.size())) == this.nodeId); // Get an id different from self
			System.out.printf(" and Sending message n°%d to %d", sent[dest], dest);
			Visualizer.send(this.nodeId, dest);
			send(new Message(Message.Type.APPLICATIVE, 0, rollbackNbr, this.nodeId), Network.get(dest));
		} else if(r >= 1 - Constants.getProbaBroadcast()){
			// Bcast
			System.out.printf(" and Broadcasting message");
			for(int i = 0; i < Network.size(); i++){
				if(i != this.nodeId){
					send(new Message(Message.Type.APPLICATIVE, 0, rollbackNbr, this.nodeId), Network.get(i));
					Visualizer.send(this.nodeId, i);
				}
			}
		}
		
		System.out.println();
	}
	
	public void doStepHeartbeat(){
		// plan next send
		EDSimulator.add(Constants.getHeartbeatDelay(), new Message(Message.Type.STEPHEARTBEAT, 0, rollbackNbr, this.nodeId), this.getMyNode(), this.mypid);
		this.heartbeatCount++;
		System.out.printf("[%d %d] Broadcasting heartbeats %d\n", CommonState.getTime(), this.nodeId, this.heartbeatCount);
		for(int i = 0; i < Network.size(); i++){
			if(i != this.nodeId){
				sendHeartbeat(new Message(Message.Type.HEARTBEAT, this.heartbeatCount, rollbackNbr, this.nodeId), Network.get(i));
			}
		}
	}
	
	
	private void receiveHeartbeat(int sender, int number) {
		//System.out.printf("[%d %d] Heartbeat from %d received\n", CommonState.getTime(), this.nodeId, sender);

		if(this.lastHeartbeat[sender] < number){
			if(this.suspect[sender]){
				this.suspect[sender] = false;
				System.out.printf("[%d %d] node %d not suspect anymore\n",CommonState.getTime(), this.nodeId, sender);
			}
			this.lastHeartbeat[sender] = number;
		}
		
		EDSimulator.add(Constants.getHeartbeatDelay()+Constants.getHeartbeatMargin(), new Message(Message.Type.CHECKHEARTBEAT, sender, this.lastHeartbeat[sender], this.nodeId), this.getMyNode(), this.mypid);
	}
	
	private void doCheckHeartbeat(int nodeId, int number){
		if(/*this.inRollback || */(Network.get(this.nodeId).getFailState()==Fallible.DOWN) || this.restarting){
			return;
		}else{
			if(this.lastHeartbeat[nodeId] <= number){
				this.suspect[nodeId] = true;
				System.out.printf("[%d %d] node %d suspect !!!\n",CommonState.getTime(), this.nodeId, nodeId);
				Network.get(nodeId).setFailState(Fallible.OK);
				sendHeartbeat(new Message(Message.Type.RESTART, 0, this.lastHeartbeat[nodeId], this.nodeId), Network.get(nodeId));
			}
		}
	}
}
