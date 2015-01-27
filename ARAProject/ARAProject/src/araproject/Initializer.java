package araproject;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

public class Initializer implements Control {
	private String prefix;

	public Initializer(String prefix) {
		this.prefix = prefix;
	}

	@Override
	public boolean execute() {
		Constants.loadConstants();
		System.out.println("Initializer executed");
		
		/* Redirect output to new Stream */
		if(Constants.getOutputFile() != null){
			try {
				System.setOut(new PrintStream(Constants.getOutputFile()));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
			
		int appPid = Configuration.getPid(prefix + ".appProtocolPid");
		Visualizer.setSize(Network.size());
		int nodeNb = Network.size();
		
		/* Linking applicative and transport layers */
		Node n;
		App nodeApp;
		for (int i = 0; i < nodeNb; i++) {
		    n = Network.get(i);
		    nodeApp = (App)n.getProtocol(appPid);
		    nodeApp.setTransportLayer(i);
		    
		    EDSimulator.add(0, new Message(Message.Type.CHECKPOINT, 0, 0, -1), n, appPid);
		    EDSimulator.add(Constants.getHeartbeatDelay() + CommonState.r.nextInt(Constants.getFirstHeartbeat()), new Message(Message.Type.STEPHEARTBEAT, 0, 0, -1), n, appPid);
		    int delay = Constants.getStepDelayMin() + CommonState.r.nextInt(Constants.getStepDelayMax() - Constants.getStepDelayMin() + 1);
		    EDSimulator.add(delay, new Message(Message.Type.STEP, 0, 0, -1), n, appPid);
		}
		
		//EDSimulator.add(2000, new Message(Message.Type.ROLLBACKSTART, 0, 0, -1), Network.get(4), appPid);
		//EDSimulator.add(2800, new Message(Message.Type.KILL, 0, 0, -1), Network.get(6), appPid);
		
		return false;
	}

}
