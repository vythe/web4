package net.vbelev.utils;

import java.util.*;
import java.util.concurrent.*;

public class EventBox
{
	/**
	 * The basic EventArgs is immutable, so its clone() method return itself.
	 * @author Vythe
	 *
	 */
	public static class EventArgs implements Cloneable
	{
		@Override
		public EventArgs clone()
		{
			return this;
		}
	}
	
	public static interface EventHandler<T extends EventArgs>
	{
		boolean isListening();
		void invoke(T e);
	}
	
	public static class Event<T extends EventArgs>
	{
		private Object m_lock = new Object();
		//private boolean m_cloneArgs = true;
		private boolean m_triggerAsync = true;
		
		@SuppressWarnings("unchecked")
		private EventHandler<? extends T>[] subscribers = new EventHandler[0];
		private final Queue<EventHandler<? extends T>> inQueue = new ConcurrentLinkedQueue<EventHandler<? extends T>>();
		private final Queue<EventHandler<? extends T>> outQueue = new ConcurrentLinkedQueue<EventHandler<? extends T>>();
		private final List<Thread> threads = Collections.synchronizedList(new ArrayList<Thread>());
		/* In this version, EventArgs.clone() returns itself without any extra work.
		 * If you want the args to be safely cloned, you need to implement your own clone().
		public Event<T> setCloneArgs(boolean clone)
		{
			synchronized (m_lock)
			{
				m_cloneArgs = clone;
			}
			
			return this;
		}
		 */
	
		public Event()
		{
			m_triggerAsync = true;
		}
		
		public Event(boolean triggerAsync)
		{
			m_triggerAsync = triggerAsync;
		}
		
		public void trigger(T args)
		{
			if (!flush()) return;
			//System.out.println("trigger called, subscribers=" + subscribers.length);
			Runnable r = new Runnable() 
			{
				@SuppressWarnings("unchecked")
				@Override
				public void run()
				{
					EventHandler<? extends T>[] ss;
					synchronized(m_lock)
					{
						//ss = (EventHandler<T>[])Arrays.copyOf(subscribers, subscribers.length);
						ss = Arrays.copyOf(subscribers, subscribers.length);
					}					
					for (EventHandler<? extends T> s : ss)
					{
						//System.out.println("call event for " + s.toString() + " with " + args.toString());
						T e = args == null? null : (T)args.clone();
						if (!s.isListening())
						{
							outQueue.add(s);														
						}
						else if (!m_triggerAsync || ss.length == 1)
						{
							((EventHandler<T>)s).invoke(e);
						}
						else
						{
							Thread subT = new Thread(new Runnable() 
							{
								@Override
								public void run()
								{
									
									((EventHandler<T>)s).invoke((T)e);
									//threads.remove(Thread.currentThread());
								}
							});
							//threads.add(subT);
							subT.start();
						}
					}									
					//threads.remove(Thread.currentThread());
					//System.out.println("trigger called, threads=" + threads.size());
				}
			};
			if (m_triggerAsync)
			{
				Thread t = new Thread(r);
				//threads.add(t);
				t.start();
			} 
			else
			{
				r.run();
			}
		}
		
		/**
		 * adds new subscribers, kicks out excluded subscribers, returns true if there are any subscribers left.
		 * It is called from inside trigger() and from unsubscribe(), so you don't need to call it at all. 
		 * @return
		 */
		@SuppressWarnings("unchecked")
		public boolean flush()
		{
			synchronized(m_lock)
			{
				if (inQueue.peek() != null || outQueue.peek() != null)
				{
					List<EventHandler<? extends T>> newsubs = new ArrayList<EventHandler<? extends T>>(subscribers.length);
					for (EventHandler<? extends T> e : subscribers)
					{
						if (e.isListening())
							newsubs.add(e);
					}
					if (outQueue.peek() != null)
					{
					newsubs.removeAll(outQueue);
					outQueue.clear();
					}
					EventHandler<? extends T> e;
					while ((e = inQueue.poll()) != null)
					{
						if (newsubs.contains(e)) continue;
						newsubs.add(e);
					}
					EventHandler<? extends T>[] subArray = (EventHandler<? extends T>[])new EventHandler[newsubs.size()];
					newsubs.toArray(subArray);
					subscribers = subArray;
				}
			}
			return (subscribers.length > 0);
		}
		
		public void completeTrigger()
		{
			List<Thread> tt;
			synchronized (threads)
			{
				tt = new ArrayList<Thread>(threads);
			}
			if (tt.size() == 0) return;
			for (Thread t : tt)
			{
				if (t.isAlive())
				{
					try
					{
						t.join();
						threads.remove(t);
					}
					catch (InterruptedException e)
					{
					}
				}
				else
				{
					threads.remove(t);
				}
			}
		}
		
		public void subscribe(EventHandler<? extends T> handler)
		{
			if (handler != null && handler.isListening())
			{
				inQueue.add(handler);
			}
		}
		
		/**
		 * Normally, you can change handler.isListening() to return false, and that will 
		 * remove it from the subscription at the next call to trigger() or flush().
		 * To release the resource immediately, you can call this method. 
		 * 
		 * Note that the method cannot change handler.isListening(), and that should be updated
		 * from the consumer side
		 */
		public void unsubscribe(EventHandler<? extends T> handler)
		{
			if (handler != null)
			{
				outQueue.add(handler);
				flush();
			}
			if (inQueue.size() > 0)
			{
				inQueue.remove(handler);
			}
		}
	}
	
	public class myeventargs extends EventArgs
	{
		public int val;
	}
	
	private final Map<String, Event<EventArgs>> events = new ConcurrentHashMap<String, Event<EventArgs>>();
	
	public void subscribe(String eventName, EventHandler<? extends EventArgs> handler)
	{
		if (eventName == null || eventName.length() == 0)
		{
			throw new IllegalArgumentException("Empty eventName");
		}
		if (handler == null || !handler.isListening()) return;
		
		Event<EventArgs> event = null;
		synchronized(events)
		{
			event = events.get(eventName);
			if (event == null)
			{
				event = new Event<EventArgs>();
				events.put(eventName, event);
			}
		}
		event.subscribe(handler);
	}
	
	/**
	 * Note that this method cannot change handler.isListening(), and that should be updated
	 * from the consumer side
	 */
	public void unsubscribe(String eventName, EventHandler<? extends EventArgs> handler)
	{
		if (eventName == null || eventName.length() == 0)
		{
			throw new IllegalArgumentException("Empty eventName");
		}
		if (handler == null) return;
		
		Event<EventArgs> event = events.get(eventName);
		if (event != null)
		{
			event.unsubscribe(handler);
		}
	}

	public void trigger(String eventName, EventArgs args)
	{
		if (eventName == null || eventName.length() == 0)
		{
			throw new IllegalArgumentException("Empty eventName");
		}
		
		Event<EventArgs> event = events.get(eventName);
		if (event != null)
		{
			event.trigger(args);
		}
	}

	public void completeTrigger(String eventName)
	{
		if (eventName == null || eventName.length() == 0)
		{
			throw new IllegalArgumentException("Empty eventName");
		}
		
		Event<EventArgs> event = events.get(eventName);
		if (event != null)
		{
			event.completeTrigger();
		}
	}
	
}
