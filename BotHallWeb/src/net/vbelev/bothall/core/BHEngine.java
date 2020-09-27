package net.vbelev.bothall.core;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;

import com.sun.xml.internal.ws.api.pipe.Engine;

import net.vbelev.utils.*;
/**
 * The first place to store and handle the bothall processes
 * @author Vythe
 *
 */
public class BHEngine
{


	public enum CycleStageEnum
	{
		IDLE,
		/** In the active stage, incoming actions are added to the current queue */
		ACTIVE,
		/** In the rollover stage, incoming actions are added to the future queue */
		ROLLOVER,
		/** A short stage when the state is being published; actions are not processed, and incoming actions are added to the future queue*/
		PUBLISH
	}
	
	public interface IClientCallback
	{
		void onPublish(int timecode);
		void processAction(BHOperations.BHAction action);
		void processTriggers();
		boolean processBuff(BHOperations.BHBuff buff);
	}
	
	//public static final long CYCLE_MSEC = 2500;
	public static int engineInstanceCounter = 0;
	
	public final int engineInstance = ++engineInstanceCounter;
	
	private BHLandscape landscape = null;
	private BHCollection items = null;
	//private ConcurrentLinkedDeque<BHOperations.BHAction> actionQueue = new ConcurrentLinkedDeque<BHOperations.BHAction>();
	//private ConcurrentLinkedDeque<BHOperations.BHAction> actionQueueNext = new ConcurrentLinkedDeque<BHOperations.BHAction>();
	private DequeHelper.DequeHolder<BHOperations.BHAction> actionQueueHolder = new DequeHelper.DequeHolder<BHOperations.BHAction>(new ConcurrentLinkedDeque<BHOperations.BHAction>());
	private DequeHelper.DequeHolder<BHOperations.BHAction> actionQueueHolderNext = new DequeHelper.DequeHolder<BHOperations.BHAction>(new ConcurrentLinkedDeque<BHOperations.BHAction>());
	private PriorityBlockingQueue<BHOperations.BHTimer> timers = new PriorityBlockingQueue<BHOperations.BHTimer>();
	/** Buffs should become immutable, but the list needs to be visible */
	public List<BHOperations.BHBuff> buffs = Collections.synchronizedList(new ArrayList<BHOperations.BHBuff>());
	private List<BHOperations.BHBuff> buffsNext = Collections.synchronizedList(new ArrayList<BHOperations.BHBuff>());
	
	private BHMessageList messages = new BHMessageList();

	public int timecode = 0;
	public long CYCLE_MSEC = 100;
	/** Processing load (share of the cycle time spent working) in percents */
	public int cycleLoad = 0;
	public int cycleCount = 0;

	public IClientCallback clientCallback = null;
	
	/** Landscape property cannot be made final, because of re-publishing it,
	 * but it should be protected from damage.
	 */
	public BHLandscape getLandscape() { return landscape; }
	
	public BHCollection getCollection() { return items; }
	
	public BHMessageList getMessages() { return messages; }
	
	public static BHEngine testEngine(int size)
	{
		BHEngine res = new BHEngine();
		
		res.landscape = testLandscape(size);
		BHLandscape.Cell[] landCells = res.landscape.cells.stream()
				.filter(q -> q.getTerrain() == BHLandscape.TerrainEnum.LAND)
				.toArray(BHLandscape.Cell[]::new)
		;
		int heroCell = Utils.random.nextInt(landCells.length - 1);
		int m1Cell = Utils.random.nextInt(landCells.length - 1);
		int m2Cell = Utils.random.nextInt(landCells.length - 1);
		int m3Cell = Utils.random.nextInt(landCells.length - 1);
		
		res.items = new BHCollection();
		BHCollection.Atom hero = res.items.addAtom("HERO", BHCollection.Atom.GRADE.HERO);
		hero.setCoords(landCells[heroCell]);
		BHCollection.Atom m1 = res.items.addAtom("MONSTER", BHCollection.Atom.GRADE.MONSTER);
		m1.setCoords(landCells[m1Cell]);
		BHCollection.Atom m2 = res.items.addAtom("MONSTER", BHCollection.Atom.GRADE.MONSTER);
		m2.setCoords(landCells[m2Cell]);
		BHCollection.Atom m3 = res.items.addAtom("MONSTER", BHCollection.Atom.GRADE.MONSTER);
		m3.setCoords(landCells[m3Cell]);
		
		res.publish();
		return res;
	}
	
	public static BHEngine loadFileEngine(String fileName)
	{
		BHEngine res = new BHEngine();
		res.landscape = new BHLandscape();
		res.items = new BHCollection();
		
		try
		{
		InputStream is = BHEngine.class.getResourceAsStream(fileName);
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line = null;
		int x = -1;
		int y = -1;
		int z = 0;
		//BHCollection.Atom hero = null;
		while ((line = br.readLine()) != null)
		{
			y++;
			StringReader sr = new StringReader(line);
			int c;
			x = -1;
			while ((c = sr.read())!= -1)
			{
				x++;
				BHLandscape.TerrainEnum terrain;
				if (c == '#')
				{
					res.landscape.setCell(new BHLandscape.Cell(x, y, z, BHLandscape.TerrainEnum.STONE));					
				}
				else if (c == ' ')
				{
					res.landscape.setCell(new BHLandscape.Cell(x, y, z, BHLandscape.TerrainEnum.LAND));					
				}
				else if (c == '@')
				{
					res.landscape.setCell(new BHLandscape.Cell(x, y, z, BHLandscape.TerrainEnum.LAND));
					BHCollection.Atom hero = res.items.addAtom("HERO", BHCollection.Atom.GRADE.HERO);
					hero.setX(x);
					hero.setY(y);
					hero.setZ(z);
				}
				else if (c == 'M')
				{
					res.landscape.setCell(new BHLandscape.Cell(x, y, z, BHLandscape.TerrainEnum.LAND));
					BHCollection.Atom hero = res.items.addAtom("MONSTER", BHCollection.Atom.GRADE.MONSTER);
					hero.setX(x);
					hero.setY(y);
					hero.setZ(z);
				}
				else if (c == '.')
				{
					res.landscape.setCell(new BHLandscape.Cell(x, y, z, BHLandscape.TerrainEnum.LAND));
					BHCollection.Atom item = res.items.addAtom("GOLD", BHCollection.Atom.GRADE.ITEM);
					item.setX(x);
					item.setY(y);
					item.setZ(z);
				}
				else
				{
					//res.landscape.setCell(new BHLandscape.Cell(x, y, z, BHLandscape.TerrainEnum.VOID));
					System.out.println("Unsupported mark [" + (char)c + "] when reading " + fileName);
				}
			}
				
		}
		}
		catch (IOException x)
		{
			System.out.println("Failed to read " + fileName);
			x.printStackTrace();
		}
		res.publish();
		res.timecode++; // make it at least 1 so the clients will load it
		return res;
	}
	
	public static BHLandscape testLandscape(int size)
	{
		BHLandscape res = new BHLandscape();
		
		for (int x = 0; x < size; x++)
		{
			for (int y = 0; y < size; y++)
			{
				res.setCell(new BHLandscape.Cell(x, y, 0, BHLandscape.TerrainEnum.VOID));
			}
		}
		
		
		// add a mountain
		res.setCell(new BHLandscape.Cell(Utils.random.nextInt(size - 1), Utils.random.nextInt(size - 1), 0, BHLandscape.TerrainEnum.STONE));
		res = res.publish(0); // no need to advance the timecode at the initialization 		
		boolean hasMore = true;
		int rounds = 0;
		while (hasMore && rounds < 100)
		{
			hasMore = false;
			rounds++;
			for (int x = 0; x < size; x++)
			{
				for (int y = 0; y < size; y++)
				{
					BHLandscape.Cell c = res.getCell(x, y, 0);
					if (c.getTerrain() == BHLandscape.TerrainEnum.STONE)
					{
						BHLandscape.Cell[] closest = res.closestCells(c.getX(), c.getY(), c.getZ());
						for (BHLandscape.Cell c1 : closest)
						{
							
							if (c1.getTerrain() != BHLandscape.TerrainEnum.VOID
								|| c1.getX() < 0 || c1.getX() >= size
								|| c1.getY() < 0 || c1.getY() >= size
								|| c1.getZ() != 0
								)
							{
								continue;
							}
							BHLandscape.Cell[] closest2 = res.closestCells(c1.getX(), c1.getY(), c1.getZ());
							int stoneCount = 0;
							int rndMark = 0;
							for (BHLandscape.Cell c2 : closest2)
							{
								if (c2.getTerrain() == BHLandscape.TerrainEnum.STONE) stoneCount++;
							}
							if (stoneCount == 1) rndMark = 70;
							else if  (stoneCount == 2) rndMark = 50;
							else if (stoneCount == 3) rndMark = 30;
							
							int rnd = Utils.random.nextInt(100);
							
							//System.out.println("for x=" + c1.getX() + ", y=" + c1.getY() + ", rnd=" + rnd);
							if (rnd <= rndMark)
							{
								res.setCell(c1.terrain(BHLandscape.TerrainEnum.STONE));
								hasMore = true;
							}
							else
							{
								res.setCell(c1.terrain(BHLandscape.TerrainEnum.LAND));
							}
						}
					}
				}
			}	
			res = res.publish(0); // no need to advance the timecode at the initialization 			
		}
		for (int x = 0; x < size; x++)
		{
			for (int y = 0; y < size; y++)
			{
				if (res.getCell(x, y, 0).getTerrain() == BHLandscape.TerrainEnum.VOID)
				res.setCell(new BHLandscape.Cell(x, y, 0, BHLandscape.TerrainEnum.LAND));
			}
		}
		
		
		return res;
	}

	public long publish()
	{
		timecode++;
		this.landscape = this.landscape.publish(timecode);
		this.items = items.publish(timecode);
		this.messages.publish();
		
		synchronized(this.buffsNext)
		{
			List<BHOperations.BHBuff> newBuffs = new ArrayList<BHOperations.BHBuff>(); //this.buffsNext);
			for (BHOperations.BHBuff b : this.buffs)
			{
				if (!b.isCancelled && b.ticks >= 0)
				{
					if (b.ticks > 0) // ticks == 0 are immortal
					{
						b.ticks--;
					}
					newBuffs.add(b);
				}
			}
			newBuffs.addAll(this.buffsNext);
			this.buffs = Collections.unmodifiableList(newBuffs);
			this.buffsNext = Collections.synchronizedList(new ArrayList<BHOperations.BHBuff>());
		}
		
		if (clientCallback != null)
		{
			clientCallback.onPublish(timecode);
		}
		return timecode;
	}
	
	/** java8+ : interface with a single method can be instantiated as a lambda */
	private static interface IQueueProcessorDoneNotify
	{
		void done(boolean complete);
	}
	
	

	private static int queueProcessorCounter = 0;

	/** To process incoming actions in a separate thread, 
	 * the processor loop is set up at this Runnable class
	 */
	public class QueueProcessor implements Runnable
	{
		public final int instanceID = ++queueProcessorCounter;
		
		private DequeHelper.DequeHolder<BHOperations.BHAction> holder;
		//private Iterator<BHAction> queue;
		private IQueueProcessorDoneNotify notifyMethod;
		//private BHEngine myEngine;
		private boolean isRunning = false;

		public QueueProcessor(DequeHelper.DequeHolder<BHOperations.BHAction> holder, IQueueProcessorDoneNotify onNotify)
		{
			this.holder = holder;
			this.notifyMethod = onNotify;
			//this.myEngine = BHEngine.this;
		}
		
		public boolean running() { return isRunning; }
		
		@Override
		public void run()
		{
			if (holder == null) return;
			//System.out.println("QueueProcessor " + instanceID + " runs");
			boolean workIsDone = true;
			isRunning = true;
			while (holder.isOpen)
			{
				if (Thread.interrupted())
				{
					workIsDone = holder.iterator().hasNext();
					break;
				}
				//BHAction action = null;
				try
				{
					synchronized(holder)
					{
						while (holder.isOpen && !holder.iterator().hasNext())
						{
							long wt = new Date().getTime();
							holder.wait(CYCLE_MSEC * 10);
							//System.out.println("queueProcessor " + instanceID + " waited for " + (new Date().getTime() - wt) + " msec, isopen=" + holder.isOpen);
						}
					}
					if (holder.iterator().hasNext())
					{
						BHOperations.BHAction action = holder.iterator().next();
					//myEngine.getMessages().addMessage(action.targetType, action.targetID, "Action " + action.ID + ": " + action.message);
						//BHOperations.processAction(BHEngine.this, action);
						if (clientCallback != null)
						{
							clientCallback.processAction(action);
						}
						//System.out.println("Action processed by queueProcessor " + instanceID + ": #" + action.ID + " " + action.message);
						Thread.yield();
					}
					//else
					//{
					//	workIsDone = true;
					//	break;
					//			
					//}
				}
				catch (InterruptedException x)
				{
					holder.isOpen = false;
					workIsDone = holder.iterator().hasNext();
					break;
				}
			}
			
			if (notifyMethod != null)
			{
				notifyMethod.done(workIsDone);
			}
			isRunning = false;
			//System.out.println("QueueProcessor " + instanceID + " finishes with " + workIsDone);
			
		}		
	}
		
	public class CycleRunnable implements Runnable
	{
		public void run()
		{
			try
			{
			cycle();
			}
			catch (InterruptedException x)
			{
			}
			stage = CycleStageEnum.IDLE;
		}
	}
	/** System time (milliseconds) of the last published cycle; the cycle start point
	 * 
	 */
	public long cycleTS = 0;
	//public String statusMessage = "Not started";
	public CycleStageEnum stage = CycleStageEnum.IDLE;
	public boolean isRunning = false;
	
	public String status() 
	{
		return stage + " - " + Utils.formatDateTime(new Date(cycleTS));
	}
	
	public void cycle() throws InterruptedException
	{
		/*
		 * Here is the cycle idea.
		 * The main point: the client's view is always one cycle behind the server's view - sometimes even two cycles behind. 
		 * The client always acts on a stale view and can hit a fallback ("your target is already dead").
		 * 1) Mark 0: We start with the collection of items, fully published and delivered to all clients;
		 * 2) clients send in "actions"; the action is targeted either at an item or at a tile 
		 * (multi-target actions will be represented by multiple single-target actions); 
		 * actions are added to the cycle's action list;
		 * 3) the list of actions is processed as they come in another thread;
		 * if there are multiple actions targeting the same item (a "collision"), they are processed together, as a small list;
		 * this means that an action will be processed several times if there is a collision;
		 * actions are processed off the previous cycle's snapshot, so they can be processed repeatedly;
		 * it is assumed that  actions are processed faster than they come.
		 * 4) Mark 1: the cycle closes for input; incoming actions are queued for the next cycle;
		 * 5) Mark 2: the action thread finishes processing the actions; the new snapshot is published and the timecode is incremented;
		 * Updates are not pushed to the clients; the client processes must request an update.
		 * 6) Mark 0+: the backlog of actions from after Mark 1 is submitted to the new cycle's action list, 
		 * and we start receiving actions from the clients.
		 * The clients will be receiving updates in their own time. 
		 * Note that even if the action list is empty (fully processed), the cycle will wait the standard time to Mark 1, 
		 * to give clients time to receive their updates. If there were no actions at all during the cycle, 
		 * we can skip publishing.
		 * 
		 *  So, we have parallel processes:
		 *  1) action listener that receives actions from clients and puts them into the primary queue (the web server);
		 *  2) state publisher that serves state updates to the clients (the same web server);
		 *  3) action handler to process the action queue;
		 *  4) dispatcher to manage the main cycle - periodic publishing as described above.
		 *  
		 *  BHEngine instance is shared between all these threads to hold the data
		 */
		


		// initialize the first loop; 
		// it will start with actionQueue possibly holding some actions already 
		String cycleLock = "boo";
				
		this.isRunning = true;
		this.stage = CycleStageEnum.PUBLISH;
		cycleTS = new Date().getTime();
		//publish(); // it should be published (pre-published) after initialization, not here
		
		//DequeHelper.DequeHolder<BHOperations.BHAction> activeHolder 
		//this.actionQueueHolder = new DequeHelper.DequeHolder<BHOperations.BHAction>(new ConcurrentLinkedDeque<BHOperations.BHAction>());
		this.actionQueueHolder.isOpen = true; 
		QueueProcessor actor = new QueueProcessor(this.actionQueueHolder, (boolean isDone) -> {
			// what should we do if it's interrupted (isDone false?			
			synchronized(cycleLock)
			{
				cycleLock.notifyAll();
			}
		});
		this.stage = CycleStageEnum.ACTIVE;
		new Thread(actor).start(); // now we are listening to the actionQueue

		while (this.isRunning)
		{
			// 1) active stage: listen to the actionqueue and process it for CYCLE_MSEC milliseconds;
			// 1.1) triggers - triggers complete the previous cycle, so they are processed first here
			if (clientCallback != null)
			{
				clientCallback.processTriggers();
				//BHOperations.processTriggers(this);
			}
			 
			// 1.2) process timers - move their actions to the processing queue
			BHOperations.BHTimer t;
			synchronized (this.timers)
			{
				while ((t = this.timers.peek()) != null)
				{
					//System.out.println("timers peek, timecode="+ t.timecode + ", this.timecde=" + this.timecode);
					if (t.timecode <= this.timecode)
					{
						t = this.timers.poll();
						this.postAction(t.action, 0);
					}
					else
					{
						break; // leave the times loop
					}
				}
			}
			// 1.3) buffs - to be implemented later
			if (clientCallback != null)
			{
				for (BHOperations.BHBuff buff : this.buffs)
				{
					//if (!BHOperations.processBuff(this, buff))
					if (!clientCallback.processBuff(buff))
					{
						buff.isCancelled = true;
					}					
				}
			}
			else
			{
				for (BHOperations.BHBuff buff : this.buffs)
				{
					buff.isCancelled = true;
				}
			}
			
			// this tick absorbs the time of the previous rollover stage 
			long sleepDelta = new Date().getTime() - cycleTS;
			cycleCount++;
			this.cycleLoad = (int)Math.floorDiv(sleepDelta * 100,  CYCLE_MSEC);
			/*
			System.out.println("Cycle " 
			+ this.engineInstance + ":" + cycleCount 
			+ ", timers.empty=" + timers.isEmpty() 
			+ ", buff.count=" + this.buffs.size()
			+ ", sleep " + (CYCLE_MSEC - sleepDelta) + ", load%: " + cycleLoad );
			*/
			if (sleepDelta < CYCLE_MSEC)
			{
				Thread.sleep(CYCLE_MSEC - sleepDelta);
			}
			cycleTS = new Date().getTime();
			
			// 2) rollover stage: new actions go to actionQueueNext and accumulate there;
			// the world remains unpublished
			// we continue processing actionQueue to completion
			///actionQueueNext = new ConcurrentLinkedDeque<BHOperations.BHAction>();
			//DequeHelper.DequeHolder<BHOperations.BHAction> nextHolder
			this.actionQueueHolderNext = new DequeHelper.DequeHolder<BHOperations.BHAction>(new ConcurrentLinkedDeque<BHOperations.BHAction>());
			QueueProcessor actorNext = new QueueProcessor(this.actionQueueHolderNext, (boolean isDone) -> {
				// what should we do if it's interrupted (isDone false)?
				synchronized(cycleLock)
				{
					cycleLock.notifyAll();
				}
			});			
			this.stage = CycleStageEnum.ROLLOVER;  // actions will go to the next queue without processing them
			synchronized (this.actionQueueHolder)
			{
				this.actionQueueHolder.isOpen = false; // the actor will process remaining actions and gracefully finish
				//System.out.println("rollover: notify queueProcessor " + actor.instanceID);
				this.actionQueueHolder.notify(); // make them stop waiting
			}
			// wait until actionQueue is emptied out
			synchronized (cycleLock)
			{
				while (actor.isRunning && this.stage != CycleStageEnum.IDLE)
				{
					long waitStart = new Date().getTime();
					cycleLock.wait(CYCLE_MSEC);
					//System.out.println("waited for QueueProcessor : " + actor.instanceID + " for "+ (new Date().getTime() - waitStart) +  " msec");
				}
			}
			// assume it's finished
			// 3) publish stage: new actions still go into actionQueueNext; there is no active action processing
			this.stage = CycleStageEnum.PUBLISH;
			this.publish();
			actor = actorNext;
			this.actionQueueHolder = this.actionQueueHolderNext;
			new Thread(actor).start(); // now we are processing the actionQueueNext
			this.stage = CycleStageEnum.ACTIVE;
		}
		this.isRunning = false;
		synchronized(this.actionQueueHolder)
		{
		this.actionQueueHolder.isOpen = false;
		this.actionQueueHolderNext.isOpen = false;
			this.actionQueueHolder.notify();
		}
		this.stage = CycleStageEnum.IDLE;
	}

	private Thread cycleThread = null;
	public void startCycling()
	{
		if (cycleThread != null)
		{
			return;
		}
		CycleRunnable cr = new CycleRunnable();
		cycleThread = new Thread(cr);
		cycleThread.start();				
	}
	
	public void stopCycling()
	{
		this.isRunning = false;
		this.cycleThread = null;
	}
	
	/** delay is in cycle ticks (over the timecode value); if delay is zero,
	 * add action directly to the cycle queue
	 */
	public void postAction(BHOperations.BHAction action, int delay)
	{
		if (action == null) return;
		BHCollection.Atom actor = this.getCollection().getItem(action.actorID);
		//System.out.println("postAction called for " + action + ", delay=" + delay + ", engine instance=" + this.engineInstance);
		/*
		System.out.println("postAction called for " + action.actionType + ", delay=" + delay + ", engine instance=" + this.engineInstance
				 + ", actor=" + (actor != null? actor.toString() : ("Not found actorID " + action.actorID))
				 + ", props[1]=" + action.intProps[1]
		);
		*/
		if (delay > 0)
		{
			BHOperations.BHTimer timer = new BHOperations.BHTimer();
			timer.action = action;
			timer.timecode = this.timecode + delay;
			this.timers.add(timer);
		}		
		else if (this.stage == BHEngine.CycleStageEnum.ROLLOVER || this.stage == BHEngine.CycleStageEnum.PUBLISH)
		{
			this.actionQueueHolderNext.deque().add(action);
			/* no processing the "next" queue?
			synchronized (this.actionQueueHolderNext) 
			{
				this.actionQueueHolderNext.notify();
			}
			*/
		}
		else
		{
			this.actionQueueHolder.deque().add(action);
			synchronized(this.actionQueueHolder)
			{
				this.actionQueueHolder.notify();
			}			
		}
	}
	
	public void postBuff(BHOperations.BHBuff buff)
	{
		
		if (buff != null)
		{
			buff.timecode = this.timecode;
			System.out.println("postBuff called for " + buff + ", engine instance=" + this.engineInstance);
			buffsNext.add(buff);
			
		}
	}
}
