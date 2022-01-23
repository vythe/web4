package net.vbelev.bothall.core;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;

import net.vbelev.utils.*;
/**
 * The base class for implementing the bothall processes.
 * It implements the main process loop (actions, buffs, triggers) 
 * without any details of the business rules.
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
	
	/** 
	 * Actions are posted to the engine, then submitted to IClientCallback for processing.
	 */
	public static class Action
	{
		private static int instanceCounter = 0;
		public final int ID = ++instanceCounter;
		
		private static final String[] emptyStringProps = new String[0];
		private static final int[] emptyIntProps = new int[0];
		
		/** actionType should be interned or assigned from some constants */
		public String actionType;
		/** The acting entity ID - it should be clear from actionType as to what kind of entity this is*/
		public int actorID;
		/** Description can be used for debugging */
		public String description;
		// different action types will have different props, but we know which prop is where
		public int[] intProps = emptyIntProps;
		public String[] stringProps = emptyStringProps;
		// later
		
		public String toString()
		{
			return "[BHA " + this.ID + ":" + actionType + ", actorID=" + actorID + ", props=" + Arrays.toString(intProps) + "]"; 
		}
		
		public Action() 
		{
			intProps = emptyIntProps;
			stringProps = emptyStringProps;
		}
		
		public Action(String type, int id, int intPropCount, int strPropCount)
		{
			actionType = type;
			actorID = id;
			intProps = intPropCount <= 0? emptyIntProps : new int[intPropCount];
			stringProps = strPropCount <= 0? emptyStringProps : new String[strPropCount];
		}
	}
	
	/**
	 * Timers are delayed actions, submitted for processing at the specified engine timecode.
	 */
	public static class Timer implements Comparable<Timer>
	{
		public Action action;
		public long timecode;
		@Override
		public int compareTo(Timer arg0)
		{
			if (timecode < arg0.timecode) return -1;
			if (timecode > arg0.timecode) return 1;
			return action.ID - arg0.action.ID;
		}
	}	
	
	/**
	 * Buffs are repeated actions. The same action is submitted for processing every tick
	 * until the buff expires or is cancelled.
	 * If the ticks value is <= 0, the buff does not expire, otherwise it is repeated "ticks" times.
	 */
	public static class Buff 
	{
		public Action action;
		/** This is when the buff was created or changed, 
		 * for reporting it to the clients
		 */
		public long timecode;
		/** If ticks is <= 0, the buff does not expire; otherwise the buff's action is processed "ticks" times */
		public int ticks;
		/** buffs stay alive (processed every tick) until cancelled */
		public boolean isCancelled = false;
		/** If the buff is visible, it is reported to the clients*/
		public boolean isVisible = true;
		
		@Override
		public String toString()
		{
			return "[B ticks=" + ticks + " " + (action==null? "(no action)" : action.toString()) + "]"; 
		}
	}
		
	//public static final long CYCLE_MSEC = 2500;
	public static int engineInstanceCounter = 0;
	
	public final int engineInstance = ++engineInstanceCounter;
	
	//private ConcurrentLinkedDeque<BHOperations.BHAction> actionQueue = new ConcurrentLinkedDeque<BHOperations.BHAction>();
	//private ConcurrentLinkedDeque<BHOperations.BHAction> actionQueueNext = new ConcurrentLinkedDeque<BHOperations.BHAction>();
	private QueueHelper.QueueHolder<Action> actionQueueHolder; // = new QueueHelper.QueueHolder<Action>(new ConcurrentLinkedQueue<Action>());
	private QueueHelper.QueueHolder<Action> actionQueueHolderNext; // = new QueueHelper.QueueHolder<Action>(new ConcurrentLinkedQueue<Action>());
	private PriorityBlockingQueue<Timer> timers; // = new PriorityBlockingQueue<Timer>();
	/** Buffs should become immutable, but the list needs to be visible */
	public List<Buff> buffs; // = Collections.synchronizedList(new ArrayList<Buff>());
	private List<Buff> buffsNext; // = Collections.synchronizedList(new ArrayList<Buff>());
	
	private BHMessageList messages; // = new BHMessageList();

	public int timecode = 0;
	public long CYCLE_MSEC = 100;
	/** Processing load (share of the cycle time spent working) in percents */
	public int cycleLoad = 0;
	public int cycleCount = 0;
	
	public BHEngine()
	{
		_reset();
	}
	
	private void _reset()
	{
		isRunning = false;
	
		actionQueueHolder = new QueueHelper.QueueHolder<Action>(new ConcurrentLinkedQueue<Action>());
		actionQueueHolderNext = new QueueHelper.QueueHolder<Action>(new ConcurrentLinkedQueue<Action>());
		timers = new PriorityBlockingQueue<Timer>();
		buffs = Collections.synchronizedList(new ArrayList<Buff>());
		buffsNext = Collections.synchronizedList(new ArrayList<Buff>());	
		
		messages = new BHMessageList();
	}
	
	public void reset()
	{
		_reset();
	}
	/** Landscape property cannot be made final, because of re-publishing it,
	 * but it should be protected from damage.
	 */
	public BHMessageList getMessages() { return messages; }
	

	/** Returns the updated timecode.
	 * This method will be overridden in subclasses (BHBoard) to handle their publishing.
	 * Those implementations are expected to call super.publish() to let the engine to its part.  */
	public long publish()
	{
		timecode++;
		this.messages.publish();
		
		synchronized(this.buffsNext)
		{
			List<Buff> newBuffs = new ArrayList<Buff>(); //this.buffsNext);
			for (Buff b : this.buffs)
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
			this.buffsNext = Collections.synchronizedList(new ArrayList<Buff>());
//			System.out.println("moved buffs, buff count=" + buffs.size() + ", ts=" + this.timecode);
		}
		
		return timecode;
	}
	
	public void processAction(Action action)
	{
		System.out.println("Action: " + action.toString());
	}
	
	public boolean processBuff(Buff buff)
	{
		System.out.println("Buff: " + buff.toString());
		return false;
	}
	
	/**
	 * Triggeres are "triggered responses": various checks and situations 
	 * that need to be reviewed every cycle. Triggers complete the previous cycle; 
	 * this method is called after publishing, but before processing any actions in the queue. 
	 */
	public void processTriggers()
	{
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
		
		private QueueHelper.QueueHolder<Action> holder;
		//private Iterator<BHAction> queue;
		private IQueueProcessorDoneNotify notifyMethod;
		//private BHEngine myEngine;
		private boolean isRunning = false;

		public QueueProcessor(QueueHelper.QueueHolder<Action> holder, IQueueProcessorDoneNotify onNotify)
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
							//long wt = new Date().getTime();
							holder.wait(CYCLE_MSEC * 10);
							//System.out.println("queueProcessor " + instanceID + " waited for " + (new Date().getTime() - wt) + " msec, isopen=" + holder.isOpen);
						}
					}
					if (holder.iterator().hasNext())
					{
						Action action = holder.iterator().next();
						processAction(action);
						Thread.yield();
					}
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
		
	private class CycleRunnable implements Runnable
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
			processTriggers();
			 
			// 1.2) process timers - move their actions to the processing queue
			Timer t;
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
			for (Buff buff : this.buffs)
			{
				//if (!BHOperations.processBuff(this, buff))
				if (!processBuff(buff))
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
			this.actionQueueHolderNext = new QueueHelper.QueueHolder<Action>(new ConcurrentLinkedQueue<Action>());
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
					//long waitStart = new Date().getTime();
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
		if (cycleThread != null && cycleThread.isAlive())
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
	public void postAction(Action action, int delay)
	{
		if (action == null) return;
		/*
		BHCollection.Atom actor = this.getCollection().getItem(action.actorID);
		//System.out.println("postAction called for " + action + ", delay=" + delay + ", engine instance=" + this.engineInstance);
		System.out.println("postAction called for " + action.actionType + ", delay=" + delay + ", engine instance=" + this.engineInstance
				 + ", actor=" + (actor != null? actor.toString() : ("Not found actorID " + action.actorID))
				 + ", props[1]=" + action.intProps[1]
		);
		*/
		if (delay > 0)
		{
			Timer timer = new Timer();
			timer.action = action;
			timer.timecode = this.timecode + delay;
			this.timers.add(timer);
		}		
		else if (this.stage == BHEngine.CycleStageEnum.ROLLOVER || this.stage == BHEngine.CycleStageEnum.PUBLISH)
		{
			this.actionQueueHolderNext.getQueue().add(action);
			/* no processing the "next" queue?
			synchronized (this.actionQueueHolderNext) 
			{
				this.actionQueueHolderNext.notify();
			}
			*/
		}
		else
		{
			this.actionQueueHolder.getQueue().add(action);
			synchronized(this.actionQueueHolder)
			{
				this.actionQueueHolder.notify();
			}			
		}
	}
	
	public void postBuff(Buff buff)
	{
		
		if (buff != null)
		{
			buff.timecode = this.timecode;
			//System.out.println("postBuff called for " + buff + ", engine instance=" + this.engineInstance);
			buffsNext.add(buff);			
		}
	}
	
	public List<Buff> getBuffs(int entityID, String actionType)
	{
		ArrayList<Buff> res = new ArrayList<Buff>();
		synchronized (this.buffs)
		{
			for (Buff b : this.buffs)
			{
				if (b.action.actorID != entityID) continue;
				if (actionType == null || actionType == b.action.actionType)
					res.add(b);				
			}
		}
		return res;
	}
	
	/**
	 * returns the first matching buff record or null, serving as a "has buff" check.
	 * @param entityType
	 * @param entityID
	 * @param actionType
	 * @return
	 */
	public Buff getBuff(int entityID, String actionType)
	{
		Buff res = null;
		synchronized (this.buffs)
		{
			for (Buff b : this.buffs)
			{
				if (b.action.actorID != entityID) continue;
				if (actionType == null || actionType == b.action.actionType)
				{
					res = b;
					break;
				}
			}
		}
		return res;
	}	
}
