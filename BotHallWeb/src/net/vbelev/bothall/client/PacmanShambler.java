package net.vbelev.bothall.client;

import net.vbelev.utils.*;

/**
 * This is a pacman bot entity.
 * 1) It is stateful (in theory), and may even have some async activity, so we want to keep a reference to StreamClient;
 * 2) it issues commands to the server, so we need a reference to the StreamClient, not just the collection;
 * 3) it is okay to issue commands asynchronously, not from onUpdate(), as long as the bot
 * 		deals with the non-thread-safe StreamClient.
 */
public class PacmanShambler
{
	private StreamClient<PacmanClient> client = null;
	
	public String clientKey;
	
	public void setClient(StreamClient<PacmanClient> c)
	{
		if (client != null)
		{
			client.setOnUpdate(null);
		}
		client = c;
		if (c != null)
		{
			client.setOnUpdate(new StreamClient.OnUpdate()
			{
				
				@Override
				public void onUpdate()
				{
					//System.out.println("Shambler onUpdate for " + this.toString());
					PacmanClient.PacmanStatus status = (PacmanClient.PacmanStatus)client.collection.getStatus();
					if (status != null && BHClient.Status.SessionStatus.DEAD.equals(status.sessionStatus))
					{
						stop();
						return;
					}
					shamble();
				}
			});
			c.start();
		}
		/*
		client.updateEvent.subscribe(new EventBox.EventHandler<EventBox.EventArgs>()
		{

			@Override
			public boolean isListening()
			{
				// TODO Auto-generated method stub
				return true;
			}

			@Override
			public void invoke(EventBox.EventArgs e)
			{
				// TODO Auto-generated method stub
				shamble();
			}
		});
		*/
	}

	private static int[] pacmanValidDirs = new int[]{1,2,3,6};

	private static final String TERRAIN_LAND = "LAND".intern();
	private static final String TERRAIN_VOID = "VOID".intern();
	
	public static boolean mayTurnMonster(BHClient.Cell[] closest, int currentDir) 
	{
		if (currentDir == 0) return true;
		//if (!closest[currentDir]) {
		//	console.log("INVALID currentDir: " + currentDir);
		//	return true;
		//}
		
		if (closest[currentDir].terrain != TERRAIN_LAND) return true;
		
		int cnt = 0;
		for (int k = 0; k < pacmanValidDirs.length && cnt < 3; k++) {
			if (closest[pacmanValidDirs[k]].terrain == TERRAIN_LAND) cnt++;		
		}
		return cnt != 2;		 
	}
	
	
	private BHClient.Cell currentPos = null;
	
	public void stop()
	{
		PacmanClient.PacmanStatus status = (PacmanClient.PacmanStatus)client.collection.getStatus();
		System.out.println("Stopping pac client " + this.toString() + " for #" + status.controlledMobileID);
		try
		{
			client.writeCommand("CLOSE", null, null);
		}
		catch (Exception x)
		{
		}
		client.stop();
	}
	/**
	 * Choose a valid direction at random
	 */
	public void shamble()
	{
		
		try
		{
			if (this.client== null || this.client.collection == null)
			{
				System.out.println("Shambling: null client");
				return;
			}
			PacmanClient.PacmanStatus status = (PacmanClient.PacmanStatus)client.collection.getStatus();
			
			if (status == null)
			{
				System.out.println("Shambling: null status");
				status = new PacmanClient.PacmanStatus();
			}
			int myMobileID = status.controlledMobileID;
			//System.out.println("I am shambling with the key " + this.clientKey + ", controlled ID=" + myMobileID);
			BHClient.Mobile me = null;
			for (BHClient.Mobile m : this.client.collection.mobiles.values())
			{
				if  (m.id == myMobileID) me = m;
				//System.out.println("mobile " + m.toString());
			}
			if (me == null)
			{
				//System.out.println("Shamble, mobile ID=" + myMobileID + " not found");
				return;
			}
			
			BHClient.Cell[] closest = this.client.collection.closestCells(me.x, me.y, me.z, TERRAIN_VOID);
			
			
			if (me.dir > 0 && closest[me.dir].terrain != TERRAIN_LAND) 
			{
				me.dir = 0;
			}
			
			// if we haven't finished a step, don't start a new one
			if (currentPos != null && me.dir > 0 && currentPos.compareTo(closest[0]) == 0)
			{
				return;
			}
			
			BHClient.Mobile[] mobs = this.client.collection.getClosestMobiles(me.x, me.y, me.z);
			
			if (!mayTurnMonster(closest, me.dir))
			{
				//System.out.println("Shamble, may not turn");
				return;
			}
			// test all valid dirs and select one at random
			int cnt = 0;
			int[] goodDirs = new int[pacmanValidDirs.length];
			for (int k = 0; k < pacmanValidDirs.length; k++) 
			{
				BHClient.Cell c = closest[pacmanValidDirs[k]];
				boolean goodDir = true;
				if (c.terrain != TERRAIN_LAND) 
					goodDir = false;
				for (BHClient.Mobile m : mobs)
				{
					if (m.x == c.x && m.y == c.y && m.z == c.z
						&& (m.dir == 0 || m.dir == BHClient.cellShifts[k][3])
					)
					{
						goodDir = false;
						break;
					}						
				}
				if (goodDir)
				{
					goodDirs[cnt++] = pacmanValidDirs[k];
				}
			}
			if (cnt == 0) return; // no good options
			int selectedDir = goodDirs[Utils.random.nextInt(cnt)];
			
			if (me.dir > 0 && me.dir == selectedDir)
			{
				//System.out.println("shamler selected dir "+ selectedDir + ", already moving that way");				
			}
			else
			{
			currentPos = closest[0];
			this.client.writeCommand("move", new int[] {selectedDir}, null);
			}
			//System.out.println("Shambling! pos=" + me.toString() + ", moved to " + selectedDir);
		//this.client.
//		System.out.println("I am shambling! timecode=" + 
//				client.collection.status.timecode
//		);
		}
		catch (Exception x)
		{
			System.out.println("I am shambling, exception=" + Utils.NVL(x.getMessage(), x.getClass().getName()));
		}
	}
	
}
