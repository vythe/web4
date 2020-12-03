package net.vbelev.bothall.web;

import java.util.*;
import java.util.stream.Stream;

import net.vbelev.utils.Utils;

import java.io.*;

/**
 * A layer between a (remote) client and BHSession.
 * Later, it will be split into a base class and a specifis "pacman" client
 * NB: so far, the client is session-type-agnostic, it's just a client.
 */
public class BHClientAgent
{
	private static int agentInstanceSeq = 0;
	
	private int agentID;
	
	public int timecode;
	public int subscriptionID;
	public int sessionID;
	/** A little security: external calls to the client should present this password.
	 * I am not sure about TCP-connected clients... but there should be a way to reconnect a client.*/
	public String password;
	
	/** If it's a push client, this stream will receive updates */
	public OutputStream pushStream = null;
	
	/** this is for pacman: which atom (mobile) this client controls */
	public int atomID = 0;
	
	private static final ArrayList<BHClientAgent> agentList = new ArrayList<BHClientAgent>();
	
	public static final Object lock = new Object();
	
	private BHClientAgent()
	{
	}
	
	public int getID() { return agentID; }
	
	public static BHClientAgent getClient(int id)
	{
		synchronized(lock)
		{
			for (BHClientAgent a : agentList)
			{
				if (a.agentID == id) return a;
			}
		}
		return null;		
	}
	
	public static BHClientAgent getClient(int sessionID, String password)
	{
		if (password == null) return null;
		
		synchronized(lock)
		{
			for (BHClientAgent a : agentList)
			{
				if ((sessionID <= 0 || sessionID == a.sessionID) && password.equals(a.password)) return a;
			}
		}
		return null;
	}
	
	public static List<BHClientAgent> agentList()
	{
		ArrayList<BHClientAgent> res = new ArrayList<BHClientAgent>();
		
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
	
	
	public static List<BHClientAgent> agentList(int sessionID)
	{
		ArrayList<BHClientAgent> res = new ArrayList<BHClientAgent>();
		
		synchronized(lock)
		{
			for (BHClientAgent a : agentList)
			{
				if (a.sessionID == sessionID)
					res.add(a);
			}
		}
		return res;
	}	

	public static Stream<BHClientAgent> getAgents() 
	{
		return agentList.stream();
	}

	public static BHClientAgent createAgent()
	{
		BHClientAgent agent = new BHClientAgent();
		agent.agentID = ++agentInstanceSeq;
		synchronized(lock)
		{
			agent.password = generatePassword(6);
			agentList.add(agent);
		}
		return agent;
	}

	public static String generatePassword(int length) 
	{
		int cnt = 0;
		if (length <= 0) return "";
		
		do
		{
			String res = Utils.randomString(length);			
			BHClientAgent a = getClient(0, res);
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
		this.password = "";
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
