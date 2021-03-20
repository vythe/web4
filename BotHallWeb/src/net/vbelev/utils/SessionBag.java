package net.vbelev.utils;

import java.util.*;
import net.vbelev.utils.*;
/**
 * Our own little session storage
 */
public class SessionBag
{
	private static Map<String, SessionBag> bagList = new Hashtable<String, SessionBag>();
	
	private static Object lock = new Object();
	
	private static String createKey()
	{
		String res = null;
		SessionBag test = null;
		synchronized(lock)
		{
			int count = 0;
			do
			{
				res = Utils.randomString(12);
				test = getByKey(res);
			} 
			while (test != null && count++ < 100);
		}

		if (test != null)
		{
			throw new RuntimeException("Failed to build a unique viewbag key");
		}
		return res;
	}
	
	private static SessionBag getByKey(String key)
	{
		synchronized(lock)
		{
			SessionBag res = bagList.get(key);
			return res;
		}
	}
	
	private static void flush()
	{
		synchronized (lock)
		{
			List<String> toDelete = new ArrayList<String>();
			//long flushTS = new Date().getTime() - 30 * 60 * 1000;
			for (Map.Entry<String, SessionBag> entry : bagList.entrySet())
			{
				if (entry.getValue().isValid(false)) toDelete.add(entry.getKey());
				//if (entry.getValue().touchTS < flushTS) toDelete.add(entry.getKey());
			}
			for (String key : toDelete)
			{
				bagList.remove(key);
			}
		}
	}
	
	private void touch()
	{
		touchTS = new Date().getTime();
	}

	public static String create()
	{
		String key = null;
		synchronized(lock)
		{
			flush();
			key = createKey();
			bagList.put(key, new SessionBag());
		}
		return key;
	}
	
	private long touchTS = 0;
	private long expireTS = 0;
	private Hashtable<String, String> vals = new Hashtable<String, String>();
	
	private SessionBag()
	{
		expireTS = new Date().getTime() + 60 * 60 * 1000;
		touch();
	}
	
	public boolean isValid(boolean touch)
	{
		long ts = new Date().getTime();
		long flushTS = ts - 30 * 60 * 1000;
		if (this.expireTS < ts || this.touchTS < flushTS) return false;
		if (touch)
		{
			this.touch();
		}
		return true;

	}
	
	public static boolean isValid(String bag)
	{
		if (Utils.IsEmpty(bag)) return false;
		SessionBag b = getByKey(bag);
		if (b == null) return false;

		return b.isValid(false);
	}
	
	public static String get(String bag, String name)
	{
		if (bag == null || name == null) return null;
		SessionBag b = getByKey(bag);
		
		if (b == null || !b.isValid(true)) return null;
		
		return b.vals.get(name);
	}
	
	public static String put(String bag, String name, String val)
	{
		if (bag == null || name == null) return null;
		SessionBag b = getByKey(bag);
		if (b == null || !b.isValid(true)) return null;

		synchronized(b)
		{
			b.touch();
			if (val == null)
			{
				String old = b.vals.get(name);
				b.vals.remove(name);
				return old;
			}
			else
			{
				return b.vals.put(name, val);		
			}
		}
	}
	
	public static String summary(String bag)
	{
		if (bag == null) return "";
		SessionBag b = getByKey(bag);
		if (b == null || !b.isValid(true)) return "";
		String res = "";
		synchronized(b)
		{
			for (String key : b.vals.keySet())
			{
				res += key + "=" + b.vals.get(key) + ";\n"; 
			}
		}
		return res;
	}
	
	public static Hashtable<String, String> all(String bag)
	{
		Hashtable<String, String> res = new Hashtable<String, String>();
		if (bag == null) return res;
		SessionBag b = getByKey(bag);
		if (b == null || !b.isValid(true)) return res;
		
		res.putAll(b.vals);
		return res;
	}
}
