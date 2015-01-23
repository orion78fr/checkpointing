package araproject;

public class Message {
	public static enum Type{
		APPLICATIVE, // Message between nodes
		CHECKPOINT, // Auto-message to save state
		STEP, // Auto-message to increment state
		ROLLBACKSTART, // Message to initiate rollback
		ROLLBACKSTEP, // Message between nodes to have a consistent rollback
		ROLLBACKEND; // Message to restart execution
	}
	
	private final Type type;
	private final int msg;
	private final int rollbackNbr;
	private final int sender;
	
	public Message(Type type, int msg, int rollbackNbr, int sender) {
		this.type = type;
		this.msg = msg;
		this.rollbackNbr = rollbackNbr;
		this.sender = sender;
	}
	public Type getType() {
		return type;
	}

	public int getMsg() {
		return msg;
	}

	public int getRollbackNbr() {
		return rollbackNbr;
	}

	public int getSender() {
		return sender;
	}
}
