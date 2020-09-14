package net.vbelev.bothall.web;

import java.util.*;
import java.util.stream.Stream;
import java.io.*;

/**
 * A layer between a (remote) client and BHSession.
 * Later, it will be split into a base class and a specifis "pacman" client
 *
 */
public class BHClientAgent
{
	private static int agentInstanceSeq = 0;
	
	public final int agentID;
	
	public int timecode;
	public int subscriptionID;
	public int sessionID;
	
	/** If it's a push client, this stream will receive updates */
	public OutputStream pushStream = null;
	
	/** this is for pacman: which atom (mobile) this client controls */
	public int controlledMobileID = 0;
	
	private static final ArrayList<BHClientAgent> agentList = new ArrayList<BHClientAgent>();
	
	
	private BHClientAgent()
	{
		agentID = ++agentInstanceSeq;
	}
	
	public void detach()
	{
		synchronized(agentList)
		{
			agentList.remove(this);
		}
		BHSession s = BHSession.getSession(this.sessionID);
		if (s != null && this.subscriptionID > 0)
		{
			s.engine.getMessages().removeSubscription(this.subscriptionID);
		}
	}
	
	public void attachTo(BHSession s)
	{
		this.sessionID = s.getID();
		this.subscriptionID = s.engine.getMessages().addSubscription();
	}
	
	public static BHClientAgent getClient(int id)
	{
		synchronized(agentList)
		{
			for (BHClientAgent a : agentList)
			{
				if (a.agentID == id) return a;
			}
		}
		return null;		
	}
	
	public static Stream<BHClientAgent> getAgents() 
	{
		return agentList.stream();
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
}
