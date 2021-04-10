package net.vbelev.bothall.client;

import net.vbelev.utils.*;

public class PacmanShambler
{
	private StreamClient client = null;
	public String clientKey;
	
	public void setClient(StreamClient c)
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
			if (this.client.collection.status == null)
			{
				System.out.println("Shambling: null status");
				this.client.collection.status = new BHClient.Status();
			}
			int myMobileID = this.client.collection.status.controlledMobileID;
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
			
			currentPos = closest[0];
			this.client.writeCommand("move", new int[] {selectedDir}, null);
			System.out.println("Shambling! pos=" + me.toString() + ", moved to " + selectedDir);
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
