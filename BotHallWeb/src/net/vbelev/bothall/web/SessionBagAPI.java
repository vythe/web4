package net.vbelev.bothall.web;

import java.util.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
//import javax.xml.bind.annotation.XmlRootElement;

import net.vbelev.utils.*;

@Path("/")
public class SessionBagAPI
{
	/**
	 * Checks the provided bag key and returns the same key if it is valid;
	 * creates and returns a new key if the old key is empty or invalid;
	 * returns "" if they user key is not valid
	 * @param userKey
	 * @param bag
	 * @return
	 */
	@Path("/bag/create")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String createBag(
			@QueryParam("user") String userKey,
			@QueryParam("bag") String bag
			)
	{
		String res = "";
		if (!BHUser.isValidUserKey(userKey))
		{
			res = "";
		}
		else if (SessionBag.isValid(bag))
		{
			res = bag;			
		}
		else
		{
			res = SessionBag.create();
		}
		return Utils.encodeJSON(res);
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
	@Path("/bag/get")
	@GET
	@Produces(MediaType.APPLICATION_JSON)	
	public List<String> getValue(
			@QueryParam("bag") String bag, 
			@QueryParam("name") String name
			)
	{
		ArrayList<String> res = new ArrayList<String>();
		if (Utils.IsEmpty(name))
		{
			return res;
		}
		else
		{
			for (String n : name.split(","))
			{
				String val = SessionBag.get(bag, n);
				if (val == null)
					res.add("");
				else
					res.add(val);
			}
		}
		return res;
	}
	
	@Path("/bag/put")
	@GET
	@Produces(MediaType.APPLICATION_JSON)	
	public String putValue(
			@QueryParam("bag") String bag, 
			@QueryParam("name") String name, 
			@QueryParam("value") String value
			)
	{
		String ret = "";
		if (Utils.IsEmpty(bag) || Utils.IsEmpty(name))
		{
			ret = "";
		}
		else if (SessionBag.isValid(bag))
		{
			//String res = ViewBag.put(bag, name, value);
			//ret = Utils.NVL(res, "null");
			ret = SessionBag.put(bag, name, value);
		}
		else				
		{
			ret = "";
		}
		return Utils.encodeJSON(ret);
	}
	
	@Path("/bag/summary")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String summary(@QueryParam("bag") String bag)
	{
		return Utils.encodeJSON(SessionBag.summary(bag));
	}
	
	
	@Path("/bag")
	@GET
	@Produces(MediaType.APPLICATION_JSON)	
	public Map<String, String> getAll(@QueryParam("bag") String bag)
	{
		Map<String, String> res = SessionBag.all(bag);
		return res;
	}		
}
