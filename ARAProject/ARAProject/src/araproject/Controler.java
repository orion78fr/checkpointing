package araproject;

import peersim.edsim.*;
import peersim.core.*;
import peersim.config.*;

public class Controler implements peersim.core.Control {
    private int controlerPid;
    private double probaKill;
	private long minKillInterval;
	private long lastKill = 0;

    public Controler(String prefix) {
		this.controlerPid = Configuration.getPid(prefix + ".controlerProtocolPid");
		this.probaKill = Configuration.getDouble(prefix + ".probaKill");
		this.minKillInterval = Configuration.getLong(prefix + ".minKillInterval");
    }

    public boolean execute() {
    	if(lastKill + minKillInterval < CommonState.getTime()){
	    	if(CommonState.r.nextDouble() < this.probaKill){
	    		EDSimulator.add(0, new Message(Message.Type.KILL, 0, 0, -1), Network.get(CommonState.r.nextInt(Network.size())), controlerPid);
	    		lastKill = CommonState.getTime();
	    	}
    	}
    	
		return false;
    }
}
