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
import net.vbelev.bothall.core.BHOperations.BHAction;
//import net.vbelev.bothall.core.BHLandscape.Cell;
import net.vbelev.bothall.web.*;

@Path("/")
public class BotHallAPI
{
	@Context 
	private HttpServletRequest request;

	@Context
	private ServletContext context;	
	
	/**
	 * Returns the list of terrain cells, visible to the current session.
	 * If full is not empty, returns all cells; otherwise checks the session timecode.
	 * The argument "full" doesn't do anything yet
	 * @return
	 */
	@Path("/getTerrain")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<BHLandscape.Cell> getTerrain(@QueryParam("full") String full) 
	{
		BHWebContext app = BHWebContext.getApplication(request.getServletContext());
		BHWebSession.getSession(request); // this will create the session if it is missing
		// everything is visible for now
		boolean isFull = !Utils.IsEmpty(full);
		ArrayList<BHLandscape.Cell> res = new ArrayList<BHLandscape.Cell>();
		
		for (BHLandscape.Cell c : app.engine.getLandscape().cells)
		{
			res.add(c);		
		}
		return res;
	}

	public static class SessionClientInfo
	{
		public int atomID;
		public String atomType;
		public String controlledBy;
	}
	
	public static class SessionInfo
	{
		public int sessionID;
		public String sessionKey;
		public String status;
		public String description;
		public String createdDate;
		public final List<SessionClientInfo> clients = new ArrayList<SessionClientInfo>();
	}
	
	@Path("/create")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public SessionInfo createSession()
	{
		PacmanSession s = PacmanSession.createSession();

		SessionInfo item = new SessionInfo();
		item.sessionID = s.getID();
		item.sessionKey = s.getSessionKey();
		item.status = s.getEngine().isRunning? "Running" : "Idle";
		item.createdDate = Utils.formatDateTime(s.createdDate);
		item.description = "Session #" + s.getID();

		return item;
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
			item.sessionID = s.getID();
			item.status = s.getEngine().isRunning? "Running" : "Idle";
			item.createdDate = Utils.formatDateTime(s.createdDate);
			item.description = "Session #" + s.getID();
			
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
						if (g.sessionID == item.sessionID && g.atomID == ci.atomID)
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
	public String cycle(@QueryParam("id") String sessionID,
			@QueryParam("key") String sessionKey,
			@QueryParam("run") String run)
	{
		Integer sID = Utils.tryParseInt(sessionID);
		if (sID == null || run == null) return "0";
		PacmanSession s = PacmanSession.getSession(sID);
		if (s == null || s.getEngine() == null)
		{
			return "0";
		}
		
		if (sessionKey == null || !sessionKey.equals(s.getSessionKey()))
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
			BHAction stopAction = new BHAction();
			stopAction.actionType  = BHOperations.ACTION_STOPCYCLING;
			
			s.getEngine().postAction(stopAction, 0);
			s.getEngine().startCycling();
		}
		return "" + s.getEngine().timecode;
	}
	
	/**
	 * This should return the agent password...
	 * @param sessionID
	 * @param atomID
	 * @return
	 */
	@Path("/join")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String joinSession(@QueryParam("id") String sessionID, @QueryParam("atom") String atomID)
	{
		Integer sID = Utils.tryParseInt(sessionID);
		Integer aID = Utils.tryParseInt(atomID);
		
		if (sID == null || aID == null) return "";
		
		PacmanSession s = PacmanSession.getSession(sID);
		if (s == null) return "";
		
		BHClientAgent agent = null;
		BHClientAgent oldAgent = getAgent();
		
		if (oldAgent != null)
		{
			//PacmanSession oldS = PacmanSession.getSession(oldAgent.sessionID);
			detachAgent(oldAgent);
		}
		
		// check that the atom is available
		synchronized(BHClientAgent.lock)
		{
			List<BHClientAgent> agents = BHClientAgent.agentList(sID);
			for (BHClientAgent a : agents)
			{
				if (a.atomID == aID)
					return ""; // already used
			}
			agent = createAgent(s);
			agent.atomID = aID;
			
		}
		//request.getSession().setAttribute(BOTHALL_AGENT_ID_ATTR, agent.getID());
		return Utils.encodeJSON(agent.password);				
	}	

	
	
	@Path("/leaveSession")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String leaveSession(@QueryParam("id") String sessionID, @QueryParam("key") String clientKey)
	{
		Integer sID = Utils.tryParseInt(sessionID);
		BHClientAgent agent = BHClientAgent.getClient(sID, clientKey);
		if (sID == null || agent == null)
		{
			return "";
		}

		detachAgent(agent);
		
		return Utils.encodeJSON("" + agent.sessionID);				
	}	
	@Path("/getUpdate")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public BHStorage.UpdateBin getUpdate(
			@QueryParam("id") String sessionID, 
			@QueryParam("pwd") String clientKey, 
			@QueryParam("full") String full) 
	{
		Integer sID = Utils.tryParseInt(sessionID);
		if (sID == null || sID <= 0)
		{
			throw new IllegalArgumentException("sessionID=" + sessionID);
		}
		BHClientAgent agent = BHClientAgent.getClient(sID, clientKey);
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
	
	@Path("/action")
	@GET
	@Produces(MediaType.APPLICATION_JSON)	
	public String action(
			@QueryParam("id") String sessionID, 
			@QueryParam("key") String clientKey, 
			@QueryParam("action") String action,
			@QueryParam("args") List<String> args
			)
	{
		try
		{
		Integer sID = Utils.tryParseInt(sessionID);
		if (sID == null || sID <= 0)
		{
			throw new IllegalArgumentException("sessionID=" + sessionID);
		}
		BHClientAgent agent = BHClientAgent.getClient(sID, clientKey);
		if (agent == null)
		{
			throw new IllegalArgumentException("Client not found");
		}
		
		PacmanSession s = PacmanSession.getSession(agent.sessionID);
		if ("move".equals(action)) 
		{
			Integer direction = Utils.tryParseInt(args.get(0));			
			Integer actionID = s.commandMove(agent.atomID, direction);
			return Utils.encodeJSON("move called for atom=" + agent.atomID + ", dir=" + direction);
		}
		else if ("pacman".equals(action))
		{
			s.triggerPacman();
		}
		else if ("die".equals(action))
		{
			s.actionDie(agent.atomID);
		}
		else if ("refresh".equals(action))
		{
			agent.timecode = 0;
		}
		
		//return "action called for action=" + action + ", args=" + String.join("/", args);
		return Utils.encodeJSON("");
		}
		catch (Exception x)
		{
			return Utils.encodeJSON(x.getMessage());
		}
	}
	
	
	/**
	 * A temporary (?) way to have session storage for stateless clients.
	 * They still need to carry the viewbag key with them.
	 * 
	 * If the bag value is empty, creates and returns a viewbag key;
	 * If the name is empty, returns the same bag key if it's valid; creates and returns a new key otherwise;
	 * Otherwise, returns the bag value of "".
	 * @param bag
	 * @param name
	 * @return
	 */
	@Path("/getval")
	@GET
	@Produces(MediaType.APPLICATION_JSON)	
	public List<String> getViewBag(@QueryParam("bag") String bag, @QueryParam("name") String name)
	{
		ArrayList<String> res = new ArrayList<String>();
		if (Utils.IsEmpty(name))
		{
			if (ViewBag.isValid(bag))
			{
				res.add(bag);
			}
			else				
			{
				String newKey = ViewBag.create();
				res.add(newKey);
			}			
		}
		else
		{
			for (String n : name.split(","))
			{
				String val = ViewBag.get(bag, n);
				if (val == null)
					res.add("");
				else
					res.add(val);
			}
		}
		return res;
	}
	
	@Path("/putval")
	@GET
	@Produces(MediaType.APPLICATION_JSON)	
	public String putViewBag(@QueryParam("bag") String bag, @QueryParam("name") String name, @QueryParam("value") String value)
	{
		String ret = "";
		if (Utils.IsEmpty(bag) || Utils.IsEmpty(name))
		{
			ret = "";
		}
		else if (ViewBag.isValid(bag))
		{
			//String res = ViewBag.put(bag, name, value);
			//ret = Utils.NVL(res, "null");
			ret = ViewBag.put(bag, name, value);
		}
		else				
		{
			ret = "";
		}
		return Utils.encodeJSON(ret);
	}
	
	@Path("/bagsummary")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String getViewbagSummary(@QueryParam("bag") String bag)
	{
		return Utils.encodeJSON(ViewBag.summary(bag));
	}
	
/*
	public static class SessionInfo
	{
		public int myID;
		public int timecode;
		public List<BHLandscape.Cell> cells;
	}
	
	
	
	@Path("/reset")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public SessionInfo reset() 
	{
		
		Object ap = context.getAttribute(BHWebContext.CONTEXT_APPLICATION_ATTR);
		if (ap instanceof BHWebContext)
		{
			BHWebContext app1 = (BHWebContext)ap;
			app1.engine.stopCycling();
		}
		
		BHWebContext app = BHWebContext.reset(request.getServletContext());
		
		BHWebSession session = BHWebSession.reset(request);
		// everything is visible for now

		SessionInfo res = new SessionInfo();
		res.myID = session.myID;
		res.timecode = session.timecode;
		res.cells = new ArrayList<BHLandscape.Cell>();
		
		for (BHLandscape.Cell c : app.engine.getLandscape().cells)
		{
			res.cells.add(c);		
		}
		
		if (app.engine.stage == BHEngine.CycleStageEnum.IDLE)
		{
			app.engine.startCycling();
		}
		
		return res;
	}
	*/
	
	/** returns an update on all changed items
	 *
	 * @return
	 */
	@Path("/getItems")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<BHCollection.Atom> getItems() 
	{
		BHWebContext app = BHWebContext.getApplication(request.getServletContext());
		BHWebSession session = BHWebSession.getSession(request);
		// everything is visible for now
		ArrayList<BHCollection.Atom> res = new ArrayList<BHCollection.Atom>();
		/*
		long newTimecode = session.timecode;
		for (BHCollection.Atom c : app.engine.getCollection().all())
		{
			long cTimecode = c.getTimecode();
			if (session.timecode < cTimecode)
			{
				res.add(c);
				if (newTimecode < cTimecode)
				{
					newTimecode = cTimecode;
				}
			}
		}
		*/
		res.addAll(app.engine.getCollection().allByTimecode(session.timecode + 1));
		if (res.size() > 0)
		{
			session.timecode = app.engine.timecode;
		}
		return res;
	}
	
	@Path("/getUpdateOld")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public BHStorage.UpdateBin getUpdateOld(@QueryParam("full") String full) 
	{
		BHClientAgent agent = getAgent();
		PacmanSession s = agent == null? null : PacmanSession.getSession(agent.sessionID);

		if (s == null)
		{
			BHStorage.UpdateBin dummy = new BHStorage.UpdateBin();
			dummy.status = new BHClient.Status();
			dummy.status.sessionStatus = BHClient.Status.SessionStatus.NONE;
			return dummy;
		}
		
		BHStorage.UpdateBin res = s.storage.getUpdate(s.getEngine(), agent.timecode, agent.subscriptionID, agent.atomID);
		agent.timecode = s.getEngine().timecode;
		res.status.controlledMobileID = agent.atomID;
		
		return res;
	}
	
	@Path("/moveit")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String moveit(@QueryParam("direction") int direction, @QueryParam("id")Integer id)
	{
		BHClientAgent agent = getAgent();
		PacmanSession s = agent == null? null : PacmanSession.getSession(agent.sessionID);

		if (s == null)
			return "";
		
		int mobileId;
		if (id != null) 
			mobileId = id;
		else
			mobileId = agent.atomID;
		
		if (mobileId <= 0)
		{
			return "";
		}
		
		Integer actionID = s.commandMove(mobileId, direction);
/*
		if (actionID != null && s.engine.stage == BHEngine.CycleStageEnum.IDLE)
		{
			s.engine.startCycling();
		}
*/		
		return "" + actionID;
	}

	@Path("/dieit")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String dieit(@QueryParam("id")Integer id)
	{
		BHClientAgent agent = getAgent();
		PacmanSession s = agent == null? null : PacmanSession.getSession(agent.sessionID);

		if (s == null)
			return "";
		
		int mobileId;
		if (id != null) 
			mobileId = id;
		else
			mobileId = agent.atomID;
		
		if (mobileId <= 0)
		{
			return "";
		}
		
		int actionID = s.actionDie(mobileId);
/*
		if (actionID != null && s.engine.stage == BHEngine.CycleStageEnum.IDLE)
		{
			s.engine.startCycling();
		}
*/		
		return "" + actionID;
	}

	@Path("/pacmanit")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String pacmanit(@QueryParam("id")Integer id)
	{
		BHClientAgent agent = getAgent();
		PacmanSession s = agent == null? null : PacmanSession.getSession(agent.sessionID);

		if (s == null)
			return "0";
		
		s.triggerPacman();
		
		return "0";
	}	
	
	@Path("/engineInfo")
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String engineInfo()
	{
		BHWebContext app = BHWebContext.getApplication(request.getServletContext());
		return "Engine id=" + app.engine.engineInstance + ", stage=" + app.engine.stage;
	}


	@Path("/postAction")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String postAction(@QueryParam("msg") String msg)
	{
		BHWebContext app = BHWebContext.getApplication(request.getServletContext());
		//return "Engine id=" + app.engine.engineInstance + ", stage=" + app.engine.stage;
		BHOperations.BHAction action = new BHOperations.BHAction();
		action.message = msg + " at " + Utils.formatDateTime(new Date());
		app.engine.postAction(action, 0);
		return "OK";
	}

	public static final String BOTHALL_SESSION_ID_ATTR = "botHall";
	public static final String BOTHALL_AGENT_ID_ATTR = "botHallAgent";

	private BHClientAgent getAgent()
	{
		Object agentID = request.getSession().getAttribute(BOTHALL_AGENT_ID_ATTR);
		if (agentID instanceof Integer)
		{
			return BHClientAgent.getClient((int)agentID);
		}
		return null;
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
	 * If sessionID is empty, it will reconnect to the existing client 
	 * or create a new session if there is no current session and client
	 * sessionID = "N" - force-create a new session
	 * sessionID = "S" - kill the current session
	 * @return
	 */
	//@Path("/joinSession")
	//@GET
	//@Produces(MediaType.APPLICATION_JSON)
	public String joinSessionOld(@QueryParam("sessionID") String sessionID)
	{
		Integer id = Utils.tryParseInt(sessionID);
		PacmanSession s = null;
		BHClientAgent agent = null;
		BHClientAgent oldAgent = getAgent();
		
		if (id == null && oldAgent != null)
		{
			s = PacmanSession.getSession(oldAgent.sessionID);
			agent = oldAgent;
			oldAgent = null;
			agent.timecode = 0; // force full update
		}
		else if (id == null)
		{
			s = PacmanSession.createSession();
			// there is no old client
		}
		else if ("N".equals(sessionID)) // new session; if there exists an old session - kill it
		{
			if (oldAgent != null)
			{
				s = PacmanSession.getSession(oldAgent.sessionID);
				if (s != null)
				{
					s.getEngine().stopCycling();
					s.getEngine().getMessages().flushSubscriptions(-1); // kick them all out
					s = null;
				}
				oldAgent = null;
			}
			s = PacmanSession.createSession();
		}
		else if ("S".equals(sessionID)) // stop the current session, do not create anything
		{
			if (oldAgent != null)
			{
				s = PacmanSession.getSession(oldAgent.sessionID);
				if (s != null)
				{
					s.getEngine().stopCycling();
					s.getEngine().getMessages().flushSubscriptions(-1); // kick them all out
					s = null;
				}
				oldAgent = null;
			}
		}
		else
		{
			s = PacmanSession.getSession(id);
		}
		
		if  (s == null)
		{
			throw new IllegalArgumentException("Session " + sessionID + " does not exist");
		}
				
		if (oldAgent != null)
		{
			oldAgent.detach();
		}				
		
		if (agent == null)
		{
			agent = createAgent(s);
		}

		request.getSession().setAttribute(BOTHALL_AGENT_ID_ATTR, agent.getID());
		return s.getID() + "";
	}
	
	@Path("/joinMobile")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String joinMobile(@QueryParam("name") String name)
	{
		BHClientAgent agent = getAgent();
		PacmanSession s = agent == null? null : PacmanSession.getSession(agent.sessionID);

		if (s == null)
		{
			return "";
		}
		else if (Utils.IsEmpty(name))
		{
			 if (agent.atomID == 0) 
			 {
				 return "";
			 }
			 else
			 {				 
				agent.atomID = 0;
				return "0";
			 }
		}
		else
		{
			Integer id = Utils.tryParseInt(name);
			BHCollection.Atom hero = null; 
			int nameGrade = BHCollection.Atom.GRADE.fromString(name);
			for (BHCollection.Atom a : s.getEngine().getCollection().all())
			{
				
				boolean isGood = 
						(id != null && a.getID() == id)
						|| name.equals(a.getStringProp(BHCollection.Atom.STRING_PROPS.NAME))
						|| a.getGrade() == nameGrade
				;
				if (isGood)
				{
					int hID = a.getID();
					BHClientAgent heroAgent = BHClientAgent.getAgents()
					.filter((q) -> q.atomID == hID)
					.findFirst()
					.orElse(null)
					;
					if (heroAgent == null)
					{
						hero = a;
						break;
					}
				}
			}
			if (hero != null)
			{
				agent.atomID = hero.getID();
				return "" + hero.getID();
			}
			else return "0";
		}			
	}
	
	/** starts ("Y"), stops ("N") or does one cycle ("O") of the current session;
	 * returns the current timecode.
	 * @param run
	 * @return
	 */
	@Path("/cycleMode")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String cycleMode(@QueryParam("run") String run)
	{
		BHClientAgent agent = getAgent();
		PacmanSession s = agent == null? null : PacmanSession.getSession(agent.sessionID);
		
		if (s == null || s.getEngine() == null || run == null)
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
			BHAction stopAction = new BHAction();
			stopAction.actionType  = BHOperations.ACTION_STOPCYCLING;
			
			s.getEngine().postAction(stopAction, 0);
			s.getEngine().startCycling();
		}
		return "" + s.getEngine().timecode;
	}
}
