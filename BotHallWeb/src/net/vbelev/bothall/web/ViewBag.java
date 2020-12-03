package net.vbelev.bothall.web;

import java.util.*;
import net.vbelev.utils.*;
/**
 * Our own little session storage
 */
public class ViewBag
{
	private static Map<String, ViewBag> bagList = new Hashtable<String, ViewBag>();
	
	private static Object lock = new Object();
	
	private static String createKey()
	{
		String res = null;
		ViewBag test = null;
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
	
	private static ViewBag getByKey(String key)
	{
		synchronized(lock)
		{
			ViewBag res = bagList.get(key);
			return res;
		}
	}
	
	private static void flush()
	{
		synchronized (lock)
		{
			List<String> toDelete = new ArrayList<String>();
			long flushTS = new Date().getTime() - 30 * 60 * 1000;
			for (Map.Entry<String, ViewBag> entry : bagList.entrySet())
			{
				if (entry.getValue().touchTS < flushTS) toDelete.add(entry.getKey());
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
			bagList.put(key, new ViewBag());
		}
		return key;
	}
	
	private long touchTS = 0;
	private Hashtable<String, String> vals = new Hashtable<String, String>();
	
	private ViewBag()
	{
		touch();
	}
	
	public static boolean isValid(String bag)
	{
		if (Utils.IsEmpty(bag)) return false;
		ViewBag b = getByKey(bag);
		if (b != null)
		{
			b.touch();
			return true;
		}
		return false;
	}
	
	public static String get(String bag, String name)
	{
		if (bag == null || name == null) return null;
		ViewBag b = getByKey(bag);
		if (b == null) return null;
		
		b.touch();
		return b.vals.get(name);
	}
	
	public static String put(String bag, String name, String val)
	{
		if (bag == null || name == null) return null;
		ViewBag b = getByKey(bag);
		if (b == null) return null;

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
		ViewBag b = getByKey(bag);
		if (b == null) return "";
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
}
