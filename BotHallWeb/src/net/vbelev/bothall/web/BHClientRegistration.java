package net.vbelev.bothall.web;

import java.util.*;
import java.util.stream.Stream;

import net.vbelev.utils.Utils;

import java.io.*;

/**
 * A layer between a (remote) client and BHSession.
 * All clients, internal and external, are registered in the global list,
 * where api calls can find them.
 */
public class BHClientRegistration
{
	private static int agentInstanceSeq = 0;
	
	private int agentID;
	/** Externally, client connections are identified by this client key. 
	 * TCP clients use the key once, to establish the connection; API clients use it with every call.*/
	public String clientKey;
	
	public int sessionID;
	public int atomID = 0;
	public int timecode;
	public int subscriptionID;
	
	/** If it's a push client, this stream will receive updates */
	//public OutputStream pushStream = null;
	
	public String userKey;
	
	private static final ArrayList<BHClientRegistration> agentList = new ArrayList<BHClientRegistration>();
	
	public static final Object lock = new Object();
	
	private BHClientRegistration()
	{
	}
	
	public int getID() { return agentID; }
	
	public static BHClientRegistration getClient(int id)
	{
		synchronized(lock)
		{
			for (BHClientRegistration a : agentList)
			{
				if (a.agentID == id) return a;
			}
		}
		return null;		
	}
	
	public static BHClientRegistration getClient(Integer sessionID, String clientKey)
	{
		if (clientKey == null) return null;
		
		synchronized(lock)
		{
			for (BHClientRegistration a : agentList)
			{
				if ((sessionID == null || sessionID <= 0 || sessionID == a.sessionID) && clientKey.equals(a.clientKey)) return a;
			}
		}
		return null;
	}
	
	public static List<BHClientRegistration> agentList()
	{
		ArrayList<BHClientRegistration> res = new ArrayList<BHClientRegistration>();
		
		synchronized(lock)
		{
			res.addAll(agentList);
			//for (PacmanSession s : sessionList)
			//{
			//	res.add(s);
			//}
		}
		return res;
	}	
	
	
	public static List<BHClientRegistration> agentList(int sessionID)
	{
		ArrayList<BHClientRegistration> res = new ArrayList<BHClientRegistration>();
		
		synchronized(lock)
		{
			for (BHClientRegistration a : agentList)
			{
				if (a.sessionID == sessionID)
					res.add(a);
			}
		}
		return res;
	}	

	public static Stream<BHClientRegistration> getAgents() 
	{
		return agentList.stream();
	}

	public static BHClientRegistration createAgent()
	{
		BHClientRegistration agent = new BHClientRegistration();
		agent.agentID = ++agentInstanceSeq;
		synchronized(lock)
		{
			agent.clientKey = generateClientKey(6);
			agentList.add(agent);
		}
		return agent;
	}

	public static String generateClientKey(int length) 
	{
		int cnt = 0;
		if (length <= 0) return "";
		
		do
		{
			String res = Utils.randomString(length);			
			BHClientRegistration a = getClient(0, res);
			if (a == null)
			{
				return res;
			}
		} while (cnt < 10);
		throw new Error("Failed to create a unique password of length " + length);
	}
	
	public void detach()
	{
		synchronized(lock)
		{
			agentList.remove(this);
		}
		this.sessionID = 0;
		this.agentID = 0;
		this.clientKey = "";
		//BHSession s = BHSession.getSession(this.sessionID);
		//if (s != null && this.subscriptionID > 0)
		//{
		//	s.getEngine().getMessages().removeSubscription(this.subscriptionID);
		//}
	}
	
	/*
	public void detach()
	{
		synchronized(agentList)
		{
			agentList.remove(this);
		}
		BHSession s = BHSession.getSession(this.sessionID);
		if (s != null && this.subscriptionID > 0)
		{
			s.getEngine().getMessages().removeSubscription(this.subscriptionID);
		}
	}
	
	public void attachTo(BHSession s)
	{
		this.sessionID = s.getID();
		this.subscriptionID = s.getEngine().getMessages().addSubscription();
	}
	
	public static BHClientAgent createAgent(BHSession s)
	{
		BHClientAgent agent = new BHClientAgent();
		agent.attachTo(s);
		synchronized(agentList)
		{
			agentList.add(agent);
		}
		return agent;
	}
	*/
}
