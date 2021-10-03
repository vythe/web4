package net.vbelev.bothall.web;

import java.io.IOException;
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
 * @author Vythe
 *
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
		/**
		 * Who holds the agent key, holds everything.
		 */
		public String agentKey; // do i need the agent id?
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
		 * Sessions he created
		 */
		public final List<BHSessionInfo> sessionList = new ArrayList<BHSessionInfo>();
		/** 
		 * Agents he 
		 */
		public final List<BHAgentInfo> agentList = new ArrayList<BHAgentInfo>();
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
	public String Auth(@FormParam("name")String name) throws IOException
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
	
	public void test()
	{
		Integer[] testint = new Integer[10];
		Utils.FirstOrDefault(PacmanSession.getSessionList(), q -> q.getID() == 1);
	}
}
