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
	
	public static class UpdateSet
	{
		public List<BHCollection.Atom> items;
		public List<BHLandscape.Cell> cells;
		public List<BHMessageList.Message> messages;
		//public List<BHOperations.BHBuff> buffs;
		public int cycleMsec;
		public int cycleLoad;
		public int timecode;
	}
	
	@Path("/getUpdate")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public BHStorage.UpdateBin getUpdate() 
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
		
		BHStorage.UpdateBin res = s.storage.getUpdate(s.getEngine(), agent.timecode, agent.subscriptionID, agent.controlledMobileID);
		agent.timecode = s.getEngine().timecode;
		res.status.controlledMobileID = agent.controlledMobileID;
		
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
			mobileId = agent.controlledMobileID;
		
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
			mobileId = agent.controlledMobileID;
		
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
	
	private BHClientAgent createAgent(PacmanSession s)
	{
		BHClientAgent agent = BHClientAgent.createAgent();
		agent.sessionID = s.getID();
		agent.subscriptionID = s.getEngine().getMessages().addSubscription();
		
		return agent;
	}

	private void detachAgent(BHClientAgent agent)
	{
		if (agent == null  || agent.getID() == 0) return;
		
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
	@Path("/joinSession")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String joinSession(@QueryParam("id") String sessionID)
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
			 if (agent.controlledMobileID == 0) 
			 {
				 return "";
			 }
			 else
			 {				 
				agent.controlledMobileID = 0;
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
					.filter((q) -> q.controlledMobileID == hID)
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
				agent.controlledMobileID = hero.getID();
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
