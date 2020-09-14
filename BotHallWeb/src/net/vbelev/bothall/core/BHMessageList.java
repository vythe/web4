package net.vbelev.bothall.core;

import java.util.*;
import java.util.concurrent.*;

/**
 * Unlike BHCollection, the list of messages is emptied and started again at every publish.
 * 
 * A broadcast: each session (each "hero" atom?) must subscribe to messages; we don't flush
 * the queue until everybody got the messages, or until it times out
 * @author Vythe
 *
 */
public class BHMessageList
{
	
	public static class Message
	{
		public static int instanceCounter = 0;
		public final int ID; 

		public final BHCollection.EntityTypeEnum target;
		public final int targetID;
		public final String message;
		
		private Message(int testID) 
		{
			this.ID = testID;
			this.target = BHCollection.EntityTypeEnum.GLOBAL;
			this.targetID = 0;
			this.message = "";
		}
		
		public Message(BHCollection.EntityTypeEnum target, int targetID, String message)
		{
			this.ID = ++instanceCounter;
			this.target = target;
			this.targetID = targetID;
			this.message = message;
		}
	}
	
	public static class Subscription
	{
		public static int instanceCounter = 0;
		public final int ID = ++instanceCounter;
		
		private int lastMessageID = 0;
		private long lastQueryTS = 0; 		
	}
	
	private final SortedSet<Message> messages = Collections.synchronizedSortedSet(new TreeSet<Message>(new Comparator<Message>() {

		@Override
		public int compare(Message arg0, Message arg1)
		{
			if (arg0.ID < arg1.ID) return -1;
			if (arg0.ID > arg1.ID) return 1;
			return 0;
		}
	}));
	private final List<Message> newMessages = new ArrayList<Message>();

	private final TreeMap<Integer, Subscription> subscriptions = new TreeMap<Integer, Subscription>();
	
	public void addMessage(Message msg)
	{
		newMessages.add(msg);
	}
	
	public Message addMessage(BHCollection.EntityTypeEnum target, int targetID, String message)
	{
		Message msg = new Message(target, targetID, message);
		newMessages.add(msg);
		return msg;
	}
	
	/** returns the number of deleted new messages */
	public int resetMessages(BHCollection.EntityTypeEnum target, int targetID)
	{
		ArrayList<Message> toDelete = new ArrayList<Message>();
		int count = 0;
		synchronized(newMessages)
		{
			for (Message m : newMessages)
			{
				if (m.target == target && m.targetID == targetID)
				{
					toDelete.add(m);
				}
			}
			for (Message m : toDelete)
			{
				this.newMessages.remove(m);
				count++;
			}
		}
		return count;
	}
	
	/** Returns the new subscription id */
	public int addSubscription()
	{
		Subscription s = new Subscription();
		s.lastMessageID = Message.instanceCounter;
		this.subscriptions.put(s.ID, s);
		return s.ID;
	}
	
	public void removeSubscription(int subscriptionID)
	{
		this.subscriptions.remove(subscriptionID);
	}
	
	/** Returns the last message ID that is safe to remove 
	 * (that is, min(lastMessageID) of the remaining subscriptions)
	 * @return
	 */
	public int flushSubscriptions(long timeoutMS)
	{
		long cutoffTime = new Date().getTime() - timeoutMS;
		List<Integer> toDelete = new ArrayList<Integer>();
		int res = Integer.MAX_VALUE;
		synchronized (this.subscriptions)
		{
			for (Subscription s : subscriptions.values())
			{
				if (s.lastQueryTS < cutoffTime)
				{
					toDelete.add(s.ID);
				}
				else if (res > s.lastMessageID)
				{
					res = s.lastMessageID;
				}					
			}
			for (Integer key : toDelete)
			{
				subscriptions.remove(key);
			}
		}
		return res;
	}
	
	public void flushMessages(int deleteID)
	{
		messages.removeIf(q -> {return q.ID <= deleteID; });
	}
	
	/** If this method returns null, your subscription is loat and you need a new subscription.
	 * If there are no messages, it will return an empty list.
	 * The result list is unfiltered; the main loop will decide which messages to send out.
	 */
	public List<Message> getMessages(int subscriptionID)
	{
		Subscription s = subscriptions.get(subscriptionID);
		if (s == null) return null;
		synchronized (s) 
		{
			Message test = new Message(s.lastMessageID + 1);
			SortedSet<Message> tail = messages.tailSet(test);
			if (tail.isEmpty())
			{
				return new ArrayList<Message>();
			}
			s.lastMessageID = tail.last().ID;
			return new ArrayList<Message>(tail);
		}
	}
	
	public void publish()
	{
	/*
		BHMessageList res = new BHMessageList();
		if (this.newMessages.size() > 0)
		{
			res.messages.addAll(this.newMessages);
		}
		res.newMessages.clear();
		
		return res;
		*/
		if (this.newMessages.size() > 0)
		{
			this.messages.addAll(this.newMessages);
		}
		this.newMessages.clear();
	}
}
