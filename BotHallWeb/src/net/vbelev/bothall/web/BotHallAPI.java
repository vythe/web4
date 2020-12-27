package net.vbelev.bothall.web;

import java.util.*;
import javax.servlet.ServletContext;
import javax.servlet.http.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.xml.bind.annotation.XmlRootElement;

import net.vbelev.utils.Utils;
import net.vbelev.bothall.client.*;
import net.vbelev.bothall.core.*;

@Path("/")
public class BotHallAPI
{
	@Context 
	private HttpServletRequest request;

	@Context
	private ServletContext context;	
	
	public static class SessionClientInfo
	{
		public int atomID;
		public String atomType;
		public String controlledBy;
	}
	
	public static class SessionInfo
	{
		public int sessionId;
		public String sessionKey;
		public String status;
		public String isProtected;
		public String description;
		public String createdDate;
		public final List<SessionClientInfo> clients = new ArrayList<SessionClientInfo>();
	}
	
	private static BHClientAgent createAgent(PacmanSession s)
	{
		BHClientAgent agent = BHClientAgent.createAgent();
		agent.sessionID = s.getID();
		agent.subscriptionID = s.getEngine().getMessages().addSubscription();
		return agent;
	}

	private void detachAgent(BHClientAgent agent)
	{
		if (agent == null  || agent.getID() == 0) return;
		// we need to check the session and possibly stop it...
		
		PacmanSession s = PacmanSession.getSession(agent.sessionID);	
		if (s != null)
		{
			s.getEngine().getMessages().removeSubscription(agent.subscriptionID);
		}
		agent.subscriptionID = 0;
		agent.sessionID = 0;
		agent.detach();
	}
	
	/**
	 * 
	 * @return
	 */
	@Path("/getuserkey")
	@GET
	@Produces(MediaType.APPLICATION_JSON)	
	public String getUserKey()
	{
		if (userKeyTS + 3 * 60 * 60 * 1000 < new Date().getTime())
		{
			prevUserKey = userKey;
			userKey = Utils.randomString(8);
		}
		return Utils.encodeJSON(userKey);
	}
	
	/**
	 * Creates a new session. Anybody can create a new session.
	 * @return
	 */
	@Path("/create")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public SessionInfo createSession(
			@QueryParam("user") String userKey,
			@QueryParam("protected") String isProtected
			)
	{
		if (PacmanSession.getSessionCount() > 10)
		{
			throw new IllegalArgumentException("Too many open sessions");
		}
		
		PacmanSession s = PacmanSession.createSession();
		if ("Y".equals(isProtected))
		{			
			s.isProtected = true;			
		}

		SessionInfo item = new SessionInfo();
		item.sessionId = s.getID();
		item.sessionKey = s.getSessionKey();
		item.status = s.getEngine().isRunning? "Running" : "Idle";
		item.createdDate = Utils.formatDateTime(s.createdDate);
		item.description = "Session #" + s.getID();
		item.isProtected = s.isProtected? "Y": "";
		return item;
	}
	
	@Path("/destroy")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String destroySession(
			@QueryParam("session") String sessionKey
			)
	{
		PacmanSession s = PacmanSession.getSession(sessionKey);
		if (s == null)
		{
			return "";
		}
		String res = "";
		synchronized(s)
		{
			s.getEngine().stopCycling();
			
			if (PacmanSession.destroySession(s))
			{
				res = sessionKey; //s.getSessionKey();
			}
			else
			{
				res = "";
			}			
		}

		return res;
	}
	
	@Path("/list")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<SessionInfo> sessionList()
	{
		ArrayList<SessionInfo> list = new ArrayList<SessionInfo>();
		List<BHClientAgent> agentList = BHClientAgent.agentList();
		
		for (PacmanSession s : PacmanSession.sessionList())
		{
			SessionInfo item = new SessionInfo();
			item.sessionId = s.getID();
			item.status = s.getEngine().isRunning? "Running" : "Idle";
			item.createdDate = Utils.formatDateTime(s.createdDate);
			item.description = "Session #" + s.getID();
			item.isProtected = s.isProtected? "Y": "";
			// build the list of mobiles open for driving
			Collection<BHCollection.Atom> atoms = s.getEngine().getCollection().all();
			for (BHCollection.Atom a : atoms)
			{
				if (a.getGrade() == BHCollection.Atom.GRADE.MONSTER || a.getGrade() == BHCollection.Atom.GRADE.HERO)
				{
					SessionClientInfo ci = new SessionClientInfo();
					ci.atomID = a.getID();
					ci.atomType = a.getType();
					ci.controlledBy = null;
					for (BHClientAgent g : agentList)
					{
						if (g.sessionID == item.sessionId && g.atomID == ci.atomID)
						{
							ci.controlledBy = "Client #" + g.getID();
							break;
						}
					}
					item.clients.add(ci);
				}
			}
			list.add(item);
		}
		return list;
	}
	
	@Path("/cycle")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String cycle(//@QueryParam("id") String sessionID,
			@QueryParam("session") String sessionKey,
			@QueryParam("run") String run)
	{
		//Integer sID = Utils.tryParseInt(sessionID);
		//if (sID == null || run == null) return "0";
		//PacmanSession s = PacmanSession.getSession(sID);
		PacmanSession s = PacmanSession.getSession(sessionKey);
		if (s == null || s.getEngine() == null)
		{
			return "0";
		}
		
		if ("Y".equals(run) && !s.getEngine().isRunning)
		{
			s.getEngine().startCycling();
		}
		else if ("N".equals(run) && s.getEngine().isRunning)
		{
			s.getEngine().stopCycling();
		}
		else if ("O".equals(run) & !s.getEngine().isRunning)
		{
			BHOperations.BHAction stopAction = new BHOperations.BHAction();
			stopAction.actionType  = BHOperations.ACTION_STOPCYCLING;
			
			s.getEngine().postAction(stopAction, 0);
			s.getEngine().startCycling();
		}
		return "" + s.getEngine().timecode;
	}
	
	/**
	 * This creates an agent object and returns its clientKey.
	 * Protected sessions will require the session key; if atomID is empty, it will create an observer client
	 * @param sessionID
	 * @param atomID
	 * @return
	 */
	@Path("/join")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String createClient(
			@QueryParam("sid") String sessionID, 
			@QueryParam("atom") String atomID,
			@QueryParam("session") String sessionKey)
	{
		Integer sID = Utils.NVL(Utils.tryParseInt(sessionID), null);
		Integer aID = Utils.tryParseInt(atomID);
		
		PacmanSession s = null;
		if (!Utils.IsEmpty(sessionKey))
		{
			s = PacmanSession.getSession(sessionKey);
		}
		else if (sID != null)
		{
			s = PacmanSession.getSession(sID);
		}
		
		if (s == null || (sID != null && sID != s.getID())) return "";
		
		if (s.isProtected)
		{
			if (Utils.IsEmpty(sessionKey) || !sessionKey.equals(s.getSessionKey()))
			{
				return "";
			}
		}
		
		BHClientAgent agent = null;
/*
		BHClientAgent oldAgent = getAgent();
		
		if (oldAgent != null)
		{
			//PacmanSession oldS = PacmanSession.getSession(oldAgent.sessionID);
			detachAgent(oldAgent);
		}
*/		
		synchronized(BHClientAgent.lock)
		{
			if (aID != null)
			{
				// check that the atom is available
				List<BHClientAgent> agents = BHClientAgent.agentList(sID);
				for (BHClientAgent a : agents)
				{
					if (a.atomID == aID)
						return ""; // already used
				}
			}
			agent = createAgent(s);
			agent.atomID = aID;
			agent.controlledBy = "Somebody";
			
		}
		//request.getSession().setAttribute(BOTHALL_AGENT_ID_ATTR, agent.getID());
		return Utils.encodeJSON(agent.clientKey);				
	}	

	
	/**
	 * Release the client and return the session ID. If the client does not exist, return "".
	 */
	@Path("/leave")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String releaseClient(@QueryParam("client") String clientKey)
	{
		BHClientAgent agent = BHClientAgent.getClient(null, clientKey);
		if (agent == null)
		{
			return "";
		}

		detachAgent(agent);
		
		return Utils.encodeJSON("" + agent.sessionID);				
	}	
	
	
	//private static long lastUpdateTS = 0;
	
	@Path("/update")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public BHStorage.UpdateBin getUpdate(
			@QueryParam("client") String clientKey, 
			@QueryParam("full") String full
			) 
	{
		BHClientAgent agent = BHClientAgent.getClient(null, clientKey);
		if (agent == null)
		{
			throw new IllegalArgumentException("Client not found");
		}
		
		PacmanSession s = PacmanSession.getSession(agent.sessionID);

		if (s == null)
		{
			BHStorage.UpdateBin dummy = new BHStorage.UpdateBin();
			dummy.status = new BHClient.Status();
			dummy.status.sessionStatus = BHClient.Status.SessionStatus.NONE;
			return dummy;
		}
		
		int timecode = ("Y".equals(full)? 0 : agent.timecode);
		if (!s.getEngine().isRunning && timecode > 0)
		{
			BHStorage.UpdateBin dummy = new BHStorage.UpdateBin();
			dummy.status = new BHClient.Status();
			dummy.status.sessionStatus = BHClient.Status.SessionStatus.STOPPED;
			return dummy;
		}
		
		BHStorage.UpdateBin res = s.storage.getUpdate(s.getEngine(), timecode, agent.subscriptionID, agent.atomID);
		agent.timecode = s.getEngine().timecode;
		res.status.controlledMobileID = agent.atomID;
		return res;
	}
	
	/**
	 * Returns the list of terrain cells, visible to the current session.
	 * Uses either sessionID (not the key) or the client key.
	 * @return
	 */
	@Path("/map")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<BHClient.Cell> getMap(
			@QueryParam("sid") String sessionID, 
			@QueryParam("client") String clientKey
			) 
	{
		PacmanSession s = null;
		
		Integer sID = Utils.tryParseInt(sessionID);
		if (sID != null)
		{
			s = PacmanSession.getSession(sID);
		}
		
		if (!Utils.IsEmpty(clientKey))
		{
			BHClientAgent agent = BHClientAgent.getClient(sID, clientKey);
			if (agent == null)
			{
				throw new IllegalArgumentException("Session not found");
			}
			if (s == null)
			{
				s = PacmanSession.getSession(agent.sessionID);
			}
			else if (s.getID() != agent.sessionID)
			{
				throw new IllegalArgumentException("Session not found");
			}
		}
		
		if (s == null || s.getEngine() == null)
		{
			throw new IllegalArgumentException("Session not found");
		}
		// everything is visible for pacman
		ArrayList<BHClient.Cell> res = new ArrayList<BHClient.Cell>();
		
		for (BHLandscape.Cell c : s.getEngine().getLandscape().cells)
		{
			res.add(BHStorage.bhCellToClient(c));		
		}
		return res;
	}

	
	@Path("/command")
	@GET
	@Produces(MediaType.APPLICATION_JSON)	
	public String command(
			@QueryParam("client") String clientKey, 
			@QueryParam("cmd") String cmd,
			@QueryParam("args") List<String> args
			)
	{
		try
		{
		//Integer sID = Utils.tryParseInt(sessionID);
		BHClientAgent agent = BHClientAgent.getClient(null, clientKey);
		if (agent == null || Utils.IsEmpty(cmd))
		{
			throw new IllegalArgumentException("Client not found");
		}
		if (agent.atomID <= 0)
		{
			throw new IllegalArgumentException("Client is an observer");
		}
		
		PacmanSession s = PacmanSession.getSession(agent.sessionID);
		String res = "";
		if (PacmanSession.COMMAND.REFRESH.equals(cmd))
		{
			agent.timecode = 0;
		}
		else
		{
			res = s.command(cmd, agent.atomID, args);
		}
		
		//return "action called for action=" + action + ", args=" + String.join("/", args);
		return Utils.encodeJSON(res);
		}
		catch (Exception x)
		{
			return Utils.encodeJSON(x.getMessage());
		}
	}

	private static String userKey = Utils.randomString(8);
	private static String prevUserKey = Utils.randomString(8);
	private static long userKeyTS = new Date().getTime();

	
	/** for now, keep user validation here. It is for protections from robots only. 
	 */
	public static boolean isValidUserKey(String key)
	{
		return key != null && (key.equals(userKey) || key.equals(prevUserKey));
	}
}
