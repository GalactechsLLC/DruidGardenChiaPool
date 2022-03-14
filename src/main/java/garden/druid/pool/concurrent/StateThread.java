package garden.druid.pool.concurrent;

import java.util.UUID;
import java.util.logging.Level;

import javax.servlet.annotation.WebListener;

import garden.druid.base.logging.Logger;
import garden.druid.base.threads.threadpools.ManagedRunnable;
import garden.druid.chia.rpc.FullNodeRpcClient;
import garden.druid.chia.types.blockchain.State;
import garden.druid.pool.Pool;

/**
 * @author Galactechs LLC.
 *
 */
@WebListener
public class StateThread extends ManagedRunnable  {

	private static final long serialVersionUID = -345208693983966462L;

	/**
	 * 
	 */
	public StateThread(){
		this.uuid = UUID.randomUUID().toString();
		this.name = "StateThread";
	}

	public void run() {
		this.status = "Not Started";
		this.started = true;
		this.done = false;
		State state = null;
		try {
			this.status = "Loading State";
			FullNodeRpcClient client = Pool.getInstance().getFullNodeClient();
			state = client.get_blockchain_state();
			if (state != null) {
				this.status = "Saving State";
				Pool.getInstance().setState(state);
			} else {
				this.status = "Null State";
			}
		} catch(Exception e) {
			Logger.getInstance().log(Level.SEVERE, "Error in StateThread.run", e);
 		} finally {
			this.status = "Sleeping";
			this.done = true;
		}
	}

}