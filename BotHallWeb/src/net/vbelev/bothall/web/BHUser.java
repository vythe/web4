package net.vbelev.bothall.web;

import java.util.*;
import net.vbelev.utils.*;

public class BHUser
{

	private static String userKey = Utils.randomString(8);
	private static String prevUserKey = Utils.randomString(8);
	private static long userKeyTS = new Date().getTime();


	public static String getUserKey()
	{
		if (userKeyTS + 3 * 60 * 60 * 1000 < new Date().getTime())
		{
			prevUserKey = userKey;
			userKey = Utils.randomString(8);
		}
		
		return userKey;
	}
	
	/** for now, keep user validation here. It is for protections from robots only. 
	 */
	public static boolean isValidUserKey(String key)
	{
		return key != null && (key.equals(userKey) || key.equals(prevUserKey));
	}
	
}
