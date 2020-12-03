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

//@Path("/")
public class BotHallAPIOld
{
	@Context 
	private HttpServletRequest request;

	@Context
	private ServletContext context;
	
	@Path("/hello")
    @GET
    @Produces(MediaType.TEXT_HTML) //.APPLICATION_JSON)
	public String Hello(@QueryParam("name") String name)
	{
		String res = "Hello, " + name + ", land=" + Utils.getEnumDescription(BHLandscape.TerrainEnum.LAND) + "<br/>";
		
		int size = 20;
		BHLandscape sc = BHEngine.testLandscape(size);
		
		res += "<h3>Test:</h3><br/>";
		
		for (int y = 0; y < size; y++)
		{
			for (int x = 0; x < size; x++)
			{
				res += " " + Utils.getEnumCode(sc.getCell(x, y, 0).getTerrain());
			}
			res += "<br/>\n";
		}
		return res;
	}
	
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
		BHWebSession session = BHWebSession.getSession(request);
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
	public BHSession.UpdateBin getUpdate() 
	{
		BHClientAgent agent = getAgent();
		BHSession s = agent == null? null : BHSession.getSession(agent.sessionID);

		if (s == null)
		{
			BHSession.UpdateBin dummy = new BHSession.UpdateBin();
			dummy.status = new BHClient.Status();
			dummy.status.sessionStatus = BHClient.Status.SessionStatus.NONE;
			return dummy;
		}
		
		BHSession.UpdateBin res = s.getUpdate(agent.timecode, agent.subscriptionID, agent.atomID);
		agent.timecode = s.getEngine().timecode;
		res.status.controlledMobileID = agent.atomID;
		
		return res;
}
	
	public UpdateSet getUpdateOld() 
	{
		BHWebContext app = BHWebContext.getApplication(request.getServletContext());
		BHWebSession session = BHWebSession.getSession(request);
		
		UpdateSet res = new UpdateSet();
		
		// everything is visible for now
		res.items = new ArrayList<BHCollection.Atom>();
		res.items.addAll(app.engine.getCollection().allByTimecode(session.timecode + 1));
		res.cycleMsec = (int)app.engine.CYCLE_MSEC;
		res.cycleLoad = app.engine.cycleLoad;
		res.timecode = app.engine.timecode;
		
		res.cells = new ArrayList<BHLandscape.Cell>();
		
		for (BHLandscape.Cell c : app.engine.getLandscape().cells)
		{
			if (c.getTimecode() > session.timecode)
			{
				res.cells.add(c);
			}
		}
		
		res.messages = app.engine.getMessages().getMessages(session.subscriptionID);
		if (res.messages == null)
		{
			session.subscriptionID = app.engine.getMessages().addSubscription();
			res.messages = new ArrayList<BHMessageList.Message>();
		}
		
		if (res.items.size() > 0 || res.messages.size() > 0 || res.cells.size() > 0)
		{
			session.timecode = app.engine.timecode;
		}
		
		return res;
	}	
	@Path("/moveit")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String moveit(@QueryParam("direction") int direction, @QueryParam("id")Integer id)
	{
		BHClientAgent agent = getAgent();
		BHSession s = agent == null? null : BHSession.getSession(agent.sessionID);

		if (s == null)
			return "";
		
		int mobileId;
		if (id != null) 
			mobileId = id;
		else
			mobileId = agent.atomID;
		
		if (mobileId <= 0)
		{
			//BHClient.Message msg = new BHClient.Message();
			return "";
		}
		
		Integer actionID = BHSession.doMove(s, mobileId, direction);
/*
		if (actionID != null && s.engine.stage == BHEngine.CycleStageEnum.IDLE)
		{
			s.engine.startCycling();
		}
*/		
		return "" + actionID;
	}

	public String moveitOld(@QueryParam("direction") int direction)
	{
		BHWebContext app = BHWebContext.getApplication(request.getServletContext());
		BHWebSession session = BHWebSession.getSession(request);
		BHCollection.Atom me = app.engine.getCollection().getItem(session.myID);
		if (me == null)
		{
			return "No atom id" + session.myID;
		}
		if (direction < 0 || direction >= BHLandscape.cellShifts.length)
		{
			return "Invalid direction: " + direction;
		}
		/*
		else
		{
			me.setY(me.getY() - 1);
			app.engine.publish();
			return "Moved";
		}
		*/
		
		BHOperations.BHAction action = new BHOperations.BHAction();
		action.actionType = BHOperations.ACTION_MOVE;
		action.actorID = session.myID;
		action.intProps = Utils.intArray(app.engine.timecode, direction, 1, BHOperations.MOVE_SPEED);
		
		if (app.engine.stage == BHEngine.CycleStageEnum.IDLE)
		{
			app.engine.startCycling();
		}
			
		app.engine.postAction(action, 0);
		return "action ID=" + action.ID;
		
		//BHOperations.doMove(app.engine, me, direction, 4);
		//return "Moved";
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

	@Path("/start")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String start(@QueryParam("doStart") String doStart)
	{
		BHWebContext app = BHWebContext.getApplication(request.getServletContext());
		//return "Engine id=" + app.engine.engineInstance + ", stage=" + app.engine.stage;
		if ("Y".equals(doStart) && !app.engine.isRunning)
		{
			app.engine.startCycling();
		}
		else if ("N".equals(doStart))
		{
			app.engine.stopCycling();
		}
		return app.engine.status();
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

	//@Path("/createSession")
	//@GET
	//@Produces(MediaType.APPLICATION_JSON)
	public String createSessionOld()
	{
		// if there is an old client - disconnect
		BHClientAgent oldAgent = getAgent();
		if (oldAgent != null)
		{
			oldAgent.detach();
		}				
		
		/*
		BHSession oldS = null;
		Object oid = request.getSession().getAttribute(BOTHALL_SESSION_ID_ATTR);
		if (oid instanceof Integer)
		{
			oldS = BHSession.getSession((int)oid);
		}
		if (oldS != null)
		{
			throw new IllegalArgumentException("Session " + oldS.getID() + " already exists");
		}		
		*/
		BHEngine e = BHEngine.loadFileEngine("/../data/pacman.txt");
		BHSession s = BHSession.createSession(e);
		BHClientAgent agent = createAgent(s);
		
		//agent.attachTo(s);
		// it's a pull client, we don't need to do anything with it
		//s.addAgent(agent);
		//request.getSession().setAttribute(BOTHALL_SESSION_ID_ATTR, s.getID());
		request.getSession().setAttribute(BOTHALL_AGENT_ID_ATTR, agent.getID());
		
		// it creates an agent, but it returns the session ID
		return s.getID() + "";
	}
	
	private BHClientAgent createAgent(BHSession s)
	{
		BHClientAgent agent = BHClientAgent.createAgent();
		agent.sessionID = s.getID();
		agent.subscriptionID = s.getEngine().getMessages().addSubscription();
		
		return agent;
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
		BHSession s = null;
		BHClientAgent agent = null;
		BHClientAgent oldAgent = getAgent();
		
		if (id == null && oldAgent != null)
		{
			s = BHSession.getSession(oldAgent.sessionID);
			agent = oldAgent;
			oldAgent = null;
			agent.timecode = 0; // force full update
		}
		else if (id == null)
		{
			s = BHSession.createSession();
			// there is no old client
		}
		else if ("N".equals(sessionID)) // new session; if there exists an old session - kill it
		{
			if (oldAgent != null)
			{
				s = BHSession.getSession(oldAgent.sessionID);
				if (s != null)
				{
					s.getEngine().stopCycling();
					s.getEngine().getMessages().flushSubscriptions(-1); // kick them all out
					s = null;
				}
				oldAgent = null;
			}
			s = BHSession.createSession();
		}
		else if ("S".equals(sessionID)) // stop the current session, do not create anything
		{
			if (oldAgent != null)
			{
				s = BHSession.getSession(oldAgent.sessionID);
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
			s = BHSession.getSession(id);
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
		BHSession s = agent == null? null : BHSession.getSession(agent.sessionID);

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
			BHCollection.Atom hero = null; 
			int nameGrade = BHCollection.Atom.GRADE.fromString(name);
			for (BHCollection.Atom a : s.getEngine().getCollection().all())
			{
				if (name.equals(a.getStringProp(BHCollection.Atom.STRING_PROPS.NAME))
						|| a.getGrade() == nameGrade) 
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
		BHSession s = agent == null? null : BHSession.getSession(agent.sessionID);
		
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
