package net.vbelev.bothall.web;

import javax.servlet.*;
import javax.servlet.jsp.*;
import net.vbelev.bothall.core.*;
/** 
 * The application context container
 * @author Vythe
 *
 */
public class BHWebContext
{
	public BHBoard engine = new BHBoard(); 
	//public BHLandscape landscape = new BHLandscape();
	
	public static final String CONTEXT_APPLICATION_ATTR = "BH_ENGINE";
	public static BHWebContext getApplication(ServletContext context)
	{
		BHWebContext res = null;
		Object ap = context.getAttribute(CONTEXT_APPLICATION_ATTR);
		if (ap instanceof BHWebContext)
		{
			res = (BHWebContext)ap;			
		}
		else
		{
			res = reset(context);
		}
		return res;
	}
	public static BHWebContext getApplication(PageContext page)
	{
		return getApplication(page.getServletContext());
		/*
		BotHallApplication res = null;
		//Object ap = page.getServletContext().getAttribute(CONTEXT_APPLICATION_ATTR);
		Object ap = page.getAttribute(CONTEXT_APPLICATION_ATTR, PageContext.APPLICATION_SCOPE);
		if (ap instanceof BotHallApplication)
		{
			res = (BotHallApplication)ap;			
		}
		else
		{
			res = new BotHallApplication();
			res.engine.testLandscape(20);
			//page.getServletContext().setAttribute(CONTEXT_APPLICATION_ATTR, res);
			page.setAttribute(CONTEXT_APPLICATION_ATTR, res, PageContext.APPLICATION_SCOPE);
		}
		return res;
		*/
	}
	
	public static BHWebContext reset(ServletContext context) 
	{
		BHWebContext res = new BHWebContext();
		//res.landscape = res.engine.testLandscape(20);
		//res.engine = BHEngine.testEngine(20);
		res.engine = new BHBoard();
		res.engine.loadFileEngine("/../data/pacman.txt");
		context.setAttribute(CONTEXT_APPLICATION_ATTR, res);
		
		return res;
	}
}
