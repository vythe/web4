package net.vbelev.bothall.web;

import java.io.IOException;
import java.net.SocketException;
import java.util.*;
import java.util.stream.Stream;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import net.vbelev.utils.Utils;
import net.vbelev.bothall.client.*;
import net.vbelev.bothall.core.*;

/**
 * WebAPI is not used to interact with a BHSession. It provides the web front
 * for starting and connecting to sessions, up to the point where the client (browser)
 * takes the clientKey and goes to work with BotHallAPI directly.
 * 
 * The idea is enable creating new sessions without exposing the user key to the client.
 * BotHallAPI lets you create a session if you have a user key, but we want them to log in first.
 */
@Path("/")
public class WebAPI
{
	public static class BHSessionInfo
	{
		public int sessionID;
		public String sessionKey;
		//public int atomID;
		//public String agentKey;
	}
	
	public static class BHAgentInfo
	{
		public int sessionID;
		
		/**
		 * Who holds the agent key, holds everything.
		 */
		public String agentKey; // do i need the agent id?
	}
	
	public static enum BHSessionMessageKind
	{
		INFO,
		ERROR
	}
	
	public static class BHSessionMessage
	{
		public String message;
		public BHSessionMessageKind kind;
		public String key;
		public boolean isPersistent;
	}
	/**
	 * Local user session in one class
	 */
	public static class WebSessionPack
	{
		//public String userName;
		//public String userKey;
		public BHUser user;
	
		/**
		 * Only the sessions he created (use PacmanSession.sessionList() to get the list of all sessions) 
		 */
		public final List<BHSessionInfo> sessionList = new ArrayList<BHSessionInfo>();
		/** 
		 * Agents he controls
		 */
		public final List<BHAgentInfo> agentList = new ArrayList<BHAgentInfo>();
		
		/** Messages directed to this web user */
		private List<BHSessionMessage> messages = new ArrayList<BHSessionMessage>();
		
		public synchronized List<BHSessionMessage> getMessages()
		{
			return new ArrayList<BHSessionMessage>(messages);
		}
		
		public synchronized BHSessionMessage addMessage(BHSessionMessageKind kind, boolean isPersistent, String message)
		{
			BHSessionMessage msg = new BHSessionMessage();
			msg.kind = kind;
			msg.message = message;
			msg.isPersistent = isPersistent;
			msg.key = null;
						
			ArrayList<BHSessionMessage> newList = new ArrayList<BHSessionMessage>(messages);			
			newList.add(msg);
			messages = newList;
			
			return msg;
		}
		
		public synchronized BHSessionMessage addMessage(BHSessionMessageKind kind, boolean isPersistent, String message, String key)
		{
			BHSessionMessage msg = new BHSessionMessage();
			msg.kind = kind;
			msg.message = message;
			msg.isPersistent = isPersistent;
			msg.key = key;
			messages.add(msg);
			
			ArrayList<BHSessionMessage> newList = new ArrayList<BHSessionMessage>(messages);			
			newList.add(msg);
			messages = newList;
			
			return msg;
		}
		
		public synchronized void flushMessages()
		{
			
			ArrayList<BHSessionMessage> newList = new ArrayList<BHSessionMessage>();
			for (BHSessionMessage msg : messages)
			{
				if (msg.isPersistent)
					newList.add(msg);
			}
			messages = newList;			
		}
		
		public synchronized void removeMessage(String key)
		{
			if  (key == null) return;
			
			ArrayList<BHSessionMessage> newList = new ArrayList<BHSessionMessage>();
			for (BHSessionMessage msg : messages)
			{
				if (msg.key == null || !msg.key.equals(key))
					newList.add(msg);
			}
			messages = newList;			
		}
		
		public void addAgent(BHClientRegistration agent)
		{
			
			BHAgentInfo info = new BHAgentInfo();
			info.sessionID = agent.sessionID;
			info.agentKey = agent.clientKey;
		
			synchronized (this.agentList)
			{
			this.agentList.add(info);
			}
		}
		
		public void removeAgent(BHClientRegistration agent)
		{
			if (agent.clientKey == null) return;
			
			synchronized (this.agentList)
			{
				BHAgentInfo info = Utils.FirstOrDefault(this.agentList, q -> q.agentKey.equals(agent.clientKey));
				if (info != null)
				{
					this.agentList.remove(info);
				}
			}
		}
	}
	
	@Context
	HttpServletRequest request;
	@Context
	HttpServletResponse response;
	
	private static final String SessionInfoAttributeName = "session_info";
	
	public static synchronized WebSessionPack getWebSessionPack(HttpServletRequest req)
	{
		synchronized (req)
		{
			Object val = req.getSession().getAttribute(SessionInfoAttributeName);
			WebSessionPack res = null;
			if (!(val instanceof WebSessionPack))
			{
				val = new WebSessionPack();
				req.getSession().setAttribute(SessionInfoAttributeName, val);
			}
			res = (WebSessionPack)val;
			if (res.user == null)
			{
				res.user = new BHUser();
			}
			return res;
		}
	}
	private void forwardToJSP(String jsp) throws IOException, ServletException
	{
		RequestDispatcher requestDispatcher = request.getRequestDispatcher(jsp);
		request.setAttribute("elName", "elValue");
		requestDispatcher.forward(request, response);
	}

	@Path("/user/auth")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public String auth(@FormParam("name")String name) throws IOException
	{
		WebSessionPack pack = getWebSessionPack(request);
		pack.user.userName = name;
		response.sendRedirect("../../home.jsp");
		
		return "";
	}
	
	@Path("/user/createsession")
	@GET
	public void createSession() throws IOException
	{
		WebSessionPack pack = getWebSessionPack(request);
		BHSession session = PacmanSession.createSession();
		BHSessionInfo info = new BHSessionInfo();
		info.sessionID = session.getID();
		info.sessionKey = session.getSessionKey();
		pack.sessionList.add(info);
		//pack.
		response.sendRedirect("../../home.jsp");
	}
	

	/**
	 * We need our own createClient to make use of our own user key
	 */
	@Path("/user/createclient")
	@GET
	public void createClient(
			@QueryParam("sid") String sessionID, 
			@QueryParam("atom") String atomID
			) throws IOException
	{
		do
		{
			WebSessionPack pack = getWebSessionPack(request);
		Integer sID = Utils.NVL(Utils.tryParseInt(sessionID), null);
		Integer aID = Utils.tryParseInt(atomID);
		
		if (sID == null)
		{
			pack.addMessage(BHSessionMessageKind.ERROR, false, "Invalid call to createClient");
			break;			
		}
		PacmanSession s = PacmanSession.getSession(sID);
		if (s == null)
		{
			pack.addMessage(BHSessionMessageKind.ERROR, false, "Invalid session ID: " + sID);
			break;			
		}
		BHSessionInfo sInfo = Utils.FirstOrDefault(pack.sessionList, q -> q.sessionID == sID);
		boolean isOwner = (sInfo != null && sInfo.sessionKey != null && sInfo.sessionKey.equals(s.getSessionKey()));
		
		if (s.isProtected && !isOwner) // check that we have the session key
		{
			pack.addMessage(BHSessionMessageKind.ERROR, false, "Access denied to createClient for this session");
			break;			
		}

		
		synchronized(BHClientRegistration.lock)
		{			
			if (aID != null)
			{
				BHClientRegistration oldAgent = Utils.FirstOrDefault(BHClientRegistration.agentList(sID), q -> q.atomID == aID);
				if (oldAgent != null)
				{
					if (isOwner)
					{
						s.detachAgent(oldAgent);
					}
					else
					{
						pack.addMessage(BHSessionMessageKind.ERROR, false, "Monster " + aID + " is already under control");
						break;			
					}
				}
			}
			BHClientRegistration agent = s.createAgent();
			agent.atomID = aID;
			agent.userKey = pack.user.getUserKey();
			agent.userName = Utils.NVL(pack.user.userName, "Somebody"); // we'll need to add the user key to this method.
			
			pack.addAgent(agent);
		}
		//request.getSession().setAttribute(BOTHALL_AGENT_ID_ATTR, agent.getID());
		}
		while  (false);
		response.sendRedirect("../../home.jsp");
	}	
	
	
	@Path("/user/startsession")
	@GET
	public void start(@QueryParam("sessionID") int sessionID) throws IOException
	{
		WebSessionPack pack = getWebSessionPack(request);
		pack.addMessage(BHSessionMessageKind.INFO, false, "not implemented");
		boolean isReady = true;
		do
		{
			PacmanSession s = PacmanSession.getSession(sessionID);
			if (s == null)
			{
				pack.addMessage(BHSessionMessageKind.ERROR, false, "Invalid session ID");
				break;
			}
			if (s.sessionStatus != BHSession.PS_STATUS.NEW)
			{
				pack.addMessage(BHSessionMessageKind.ERROR, false, "Session " + sessionID + " is already started");
				break;
			}

			BHCollection.Atom hero = null;
			BHClientRegistration heroAgent = null;

			// check all monsters - everybody needs a bot
			List<BHClientRegistration> agents = BHClientRegistration.agentList(sessionID);

			for (BHCollection.Atom a : s.getCollection().all())
			{
				if (a.getGrade() == BHCollection.Atom.GRADE.HERO)
				{
					hero = a;
					heroAgent = Utils.FirstOrDefault(agents, q -> q.atomID == a.getID());
					continue;
				}
				if (a.getGrade() != BHCollection.Atom.GRADE.MONSTER)
				{
					continue;
				}
				BHClientRegistration agent = Utils.FirstOrDefault(agents, q -> q.atomID == a.getID());
				if (agent == null)
				{
					agent = s.createAgent();
					attachBot(a, agent);
					pack.addMessage(BHSessionMessageKind.INFO, false, "Attached a bot to " + a.getID());
					pack.addAgent(agent);
				}
			}

			if (hero != null && (heroAgent == null || heroAgent.userKey == null))
			{
				isReady = false;
				pack.addMessage(BHSessionMessageKind.INFO, false, "Do not forget to link the hero " + hero.getID());
			}
		}
		while (false);

		response.sendRedirect("../../home.jsp");
	}
	
	private void attachBot(BHCollection.Atom monster, BHClientRegistration agent) throws IOException
	{
		StreamHost bhs = StreamHost.getListener(); // this will start the server if needed
		
		java.net.Socket s = new java.net.Socket();
		s.setSoTimeout(1000);
		s.connect(new java.net.InetSocketAddress(java.net.InetAddress.getLoopbackAddress(), bhs.getPort()));
		
		StreamClient<PacmanClient> client = new StreamClient<PacmanClient>(s, new PacmanClient());
		/*
		Shambler shambler = new Shambler();
		shambler.clientKey = agent.clientKey;
		shambler.setClient(new StreamClient(s));
		*/
		net.vbelev.bothall.client.PacmanShambler shambler = new net.vbelev.bothall.client.PacmanShambler();
		shambler.clientKey = agent.clientKey;
		agent.userName = "Shambler";
		agent.atomID = monster.getID();
		shambler.setClient(client);
		//shambler.client
		
		BHClient.Command joinCmd = new BHClient.Command(PacmanSession.COMMAND.JOINCLIENT, null, new String[] {agent.clientKey});
		client.writeCommand(joinCmd);	
	}
	
	public void test()
	{
		Integer[] testint = new Integer[10];
		Utils.FirstOrDefault(PacmanSession.getSessionList(), q -> q.getID() == 1);
	}
}
