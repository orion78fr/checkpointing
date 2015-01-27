package araproject;

import peersim.edsim.*;
import peersim.core.*;
import peersim.config.*;

public class Controler implements peersim.core.Control {
    private String prefix;
    private int controlerPid;
    private boolean killed = false;
    private boolean rollbacked = false;

    public Controler(String prefix) {
	this.controlerPid = Configuration.getPid(prefix + ".controlerProtocolPid");
    }

    public boolean execute() {
    	
    	if(!killed){
    		killed = true;
    		EDSimulator.add(2800, new Message(Message.Type.KILL, 0, 0, -1), Network.get(6), controlerPid);
    	}
    	
    	if(!rollbacked){
    		rollbacked = true;
    		EDSimulator.add(2000, new Message(Message.Type.ROLLBACKSTART, 0, 0, -1), Network.get(4), controlerPid);
    	}
    	
		return false;
    }
}
