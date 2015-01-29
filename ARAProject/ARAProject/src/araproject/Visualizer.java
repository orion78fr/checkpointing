package araproject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import peersim.core.CommonState;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxGraph;
import com.sun.xml.internal.bind.v2.Messages;

public class Visualizer extends JFrame {
	private static final long serialVersionUID = 2107967669924972959L;

	private static int hOffset = 10;
	private static int hStep = 30;
	private static int vOffset = 50;
	private static int vStep = 50;
	private static int hSize = 20;
	private static int vSize = 20;

	private static class Msg {
		private int nodeE;
		private long tE;
		private int nodeR;
		private long tR;

		public Msg(int nodeE, long tE, int nodeR) {
			this.nodeE = nodeE;
			this.tE = tE;
			this.nodeR = nodeR;
		}

		public void settR(long tR) {
			this.tR = tR;
		}

		public int getNodeE() {
			return nodeE;
		}

		public long gettE() {
			return tE;
		}

		public int getNodeR() {
			return nodeR;
		}

		public long gettR() {
			return tR;
		}
	}

	private static class Event {
		private long t;
		private int node;
		private int num;
		private Message.Type type;

		public long getT() {
			return t;
		}

		public int getNode() {
			return node;
		}

		public int getNum() {
			return num;
		}

		public Event(long t, int node, int num, Message.Type type) {
			super();
			this.t = t;
			this.node = node;
			this.num = num;
			this.type = type;
		}

		public Message.Type getType() {
			return type;
		}
	}

	private static Map<Integer, List<Msg>> pendingMsg = new HashMap<Integer, List<Msg>>();
	private static List<Msg> messages = new ArrayList<Msg>();
	private static List<Event> nodes = new ArrayList<Event>();

	public static void send(int from, int to) {
		if (!pendingMsg.containsKey(to)) {
			pendingMsg.put(to, new ArrayList<Msg>());
		}
		pendingMsg.get(to).add(new Msg(from, CommonState.getTime(), to));
	}

	public static void receive(int from, int to) {
		List<Msg> l = pendingMsg.get(to);
		Msg m;
		for (int i = 0; i < l.size(); i++) {
			m = l.get(i);
			if (m.getNodeE() == from) {
				l.remove(m);
				m.settR(CommonState.getTime());
				messages.add(m);
			}
		}
	}

	public static void step(int from, int num) {
		nodes.add(new Event(CommonState.getTime(), from, num, Message.Type.STEP));
	}

	public static void checkpoint(int from, int num) {
		nodes.add(new Event(CommonState.getTime(), from, num, Message.Type.CHECKPOINT));
	}

	public static void rollback(int from, int num) {
		nodes.add(new Event(CommonState.getTime(), from, num, Message.Type.ROLLBACKSTEP));
	}

	public static void rollbackstart(int from) {
		// We may have lost messages sent by others while dead that may do bad things, so we clear our pending messages
		pendingMsg.get(from).clear();
		nodes.add(new Event(CommonState.getTime(), from, 0, Message.Type.ROLLBACKSTART));
	}
	public static void kill(int from){
		nodes.add(new Event(CommonState.getTime(), from, 0, Message.Type.KILL));
	}

	private static int size;

	public static void setSize(int newSize) {
		size = newSize;
	}

	public Visualizer() {
		mxGraph graph = new mxGraph();
		Object parent = graph.getDefaultParent();

		graph.getModel().beginUpdate();

		try {
			Object[] olds = new Object[size];
			Object prev = null;
			for (int i = 0; i <= 3600; i += 10) {
				Object o = graph.insertVertex(parent, null, i, hOffset + i * hStep, 10, hSize, vSize, mxConstants.STYLE_SHAPE + "=" + mxConstants.SHAPE_HEXAGON + ";" + mxConstants.STYLE_FILLCOLOR + "=#cccccc");
				graph.insertEdge(parent, null, "", prev, o, mxConstants.STYLE_ENDARROW + "=" + mxConstants.NONE);
				prev = o;
			}
			for (Event e : nodes) {
				String nodeStyle = "";
				if (e.getType() == Message.Type.CHECKPOINT) {
					nodeStyle = mxConstants.STYLE_SHAPE + "=" + mxConstants.SHAPE_CLOUD + ";" + mxConstants.STYLE_FILLCOLOR + "=#00ff00";
				} else if (e.getType() == Message.Type.ROLLBACKSTEP) {
					nodeStyle = mxConstants.STYLE_FILLCOLOR + "=#ff8800";
				} else if (e.getType() == Message.Type.ROLLBACKSTART) {
					nodeStyle = mxConstants.STYLE_FILLCOLOR + "=#ff0000";
				} else if (e.getType() == Message.Type.KILL){
					// Draw a cross at crash point
					nodeStyle = mxConstants.STYLE_STROKECOLOR + "=#ff0000;" + mxConstants.STYLE_ENDARROW + "=" + mxConstants.NONE;
					Object a = graph.insertVertex(parent, null, null, hOffset + e.getT() * hStep - 5, vOffset + e.getNode() * vStep - 5, 0, 0);
					Object b = graph.insertVertex(parent, null, null, hOffset + e.getT() * hStep + hSize + 5, vOffset  + e.getNode() * vStep + vSize + 5, 0, 0);
					graph.insertEdge(parent, null, null, a, b, nodeStyle);
					a = graph.insertVertex(parent, null, null, hOffset + e.getT() * hStep + hSize + 5, vOffset + e.getNode() * vStep - 5, 0, 0);
					b = graph.insertVertex(parent, null, null, hOffset + e.getT() * hStep - 5, vOffset + e.getNode() * vStep + vSize + 5, 0, 0);
					graph.insertEdge(parent, null, null, a, b, nodeStyle);
					continue;
				}

				Object o = graph.insertVertex(parent, null, e.getNum(), hOffset + e.getT() * hStep, vOffset + e.getNode() * vStep, hSize, vSize, nodeStyle);
				Object old = olds[e.getNode()];

				graph.insertEdge(parent, null, null, old, o, mxConstants.STYLE_ENDARROW + "=" + mxConstants.NONE);

				olds[e.getNode()] = o;
			}

			for (Msg m : messages) {
				Object a = graph.insertVertex(parent, null, null, hOffset + hSize + m.gettE() * hStep, vOffset + vSize / 2 + m.getNodeE() * vStep, 0, 0);
				Object b = graph.insertVertex(parent, null, null, hOffset + m.gettR() * hStep, vOffset + vSize / 2 + m.getNodeR() * vStep, 0, 0);
				graph.insertEdge(parent, null, null, a, b);
			}
		      for(int i = 0; i < size; i++){
		    	  Object a = graph.insertVertex(parent, null, "", hOffset + hSize + 3600 * hStep, vOffset + vSize/2 + i * vStep, 0, 0);
		    			  graph.insertEdge(parent, null, null, olds[i], a,
				    			  mxConstants.STYLE_ENDARROW + "=" + mxConstants.NONE);
		      }

		} finally {
			graph.getModel().endUpdate();
		}

		mxGraphComponent graphComponent = new mxGraphComponent(graph);
		graphComponent.setEnabled(false);
		getContentPane().add(graphComponent);
	}

	public static void main(String[] args) {
		peersim.Simulator.main(new String[] { "config_file.cfg" });
		JFrame frame = new Visualizer();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(800, 600);
		frame.setVisible(true);
	}

}
