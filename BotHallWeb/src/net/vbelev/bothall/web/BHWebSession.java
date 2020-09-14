package net.vbelev.bothall.web;

import javax.servlet.ServletContext;
import javax.servlet.http.*;

import net.vbelev.bothall.core.*;

/** 
 * Session storage object for whoever
 * @author Vythe
 *
 */
public class BHWebSession
{

	//public BHCollection Self = new BHCollection();
	public int timecode = 0;
	public int myID = 0;
	public int subscriptionID = 0;

	public static final String BOTHALL_SESSION_ATTR = "botHall";
	
	public static BHWebSession getSession(HttpServletRequest req)
	{
		BHWebSession res = null;
		HttpSession session = req.getSession();
		Object o = session.getAttribute(BOTHALL_SESSION_ATTR);
		if (o instanceof BHWebSession)
		{
			res =  (BHWebSession)o;
		}
		else
		{
			res = reset(req);
		}
		return res;
	}

	public static BHWebSession reset(HttpServletRequest req)
	{
		HttpSession session = req.getSession();
		BHWebSession res = new BHWebSession();		
		session.setAttribute(BOTHALL_SESSION_ATTR, res);
		BHWebContext app = BHWebContext.getApplication(req.getServletContext());
		//BHCollection.Atom me = app.engine.getCollection().addAtom("mob1");
		BHCollection.Atom me = app.engine.getCollection().all().stream().filter(q -> "HERO".equals(q.getType())).findFirst().orElse(null);
		if (me == null)
		{
			me = app.engine.getCollection().addAtom("HERO", BHCollection.Atom.GRADE.HERO);
			app.engine.publish();
		}
		res.myID = me.getID();
		res.subscriptionID = app.engine.getMessages().addSubscription();
		
		return res;
	}
}
