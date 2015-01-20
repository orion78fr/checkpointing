package araproject;

import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

public class Initializer implements Control {
	private String prefix;

	public Initializer(String prefix) {
		this.prefix = prefix;
	}

	@Override
	public boolean execute() {
		System.out.println("Initializer executed");
		
		int appPid = Configuration.getPid(prefix + ".appProtocolPid");
		int nodeNb = Network.size();
		
		/* Linking applicative and transport layers */
		Node n;
		App nodeApp;
		for (int i = 1; i < nodeNb; i++) {
		    n = Network.get(i);
		    nodeApp = (App)n.getProtocol(appPid);
		    nodeApp.setTransportLayer(i);
		}
		
		return false;
	}

}
