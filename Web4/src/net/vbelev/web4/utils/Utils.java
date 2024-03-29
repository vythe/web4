package net.vbelev.web4.utils;

import java.util.*;
//import java.nio.charset.Charset;

//import com.sun.media.jfxmedia.track.Track.Encoding;

public class Utils
{
	public static Integer tryParseInt(String arg)
	{
		try
		{
			return Integer.parseInt(arg);
		}
		catch (Exception x)
		{
		}
		return null;
	}

	public static <T extends Enum<T>>T parseEnum(Class<T> c, String val)
	{
		//,c.ise
		return Enum.valueOf(c, val);		
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Enum<T>>T tryParseEnum(String val, T defaultVal)
	{
		try
		{
			if (val == null || val.length() == 0) return defaultVal;
			
			return Enum.valueOf((Class<T>)defaultVal.getClass(), val);
		}
		catch (IllegalArgumentException e)
		{
			return defaultVal;
		}
	}
	
	public static <T extends Object>T NVL(T... vals)
	{
		for (T v : vals)
		{
			if (v != null) return v;
		}
		return null;
	}
	
	public static <T>boolean equals(T t1, T t2)
	{
		if (t1 == null && t2 == null) return true;
		else if (t1 == null || t2 == null) return false;
		else return t1.equals(t2);
	}
	
	@SuppressWarnings("unchecked")
	public static <T>T AsClass(Object o, Class<T> c)
	{
		if (o == null || !c.isAssignableFrom(o.getClass()))
			return null;
		return (T)o;
		
	}
	
	public static boolean IsEmpty(String arg)
	{
		if (arg == null || arg.length() == 0) return true;
		return arg.trim().length() == 0;
	}
	
	public static String encodeHTML(String src)
	{
		if (src == null || src.length() == 0) return "";
		
		StringBuilder out = new StringBuilder();
		char[] chars = src.toCharArray();
	    for (int i = 0; i < chars.length; i++) {
	        char c = chars[i];
	        switch (c)
	        {
	        	case '<': out.append("&lt;"); break;
	        	case '>': out.append("&gt;"); break;
	        	case '&': out.append("&amp;"); break;
	        	default:
	        		if (c > 127)
	        		{
	        			out.append("&#" + (int)c + ";");
	        		}
	        		else
	        		{
	        			out.append(c);
	        		}
	        }	       
	    }
	    return out.toString();
	}
	public static String encodeHTMLAttr(String src)
	{
		if (src == null || src.length() == 0) return "";
		
		StringBuilder out = new StringBuilder();
		char[] chars = src.toCharArray();
	    for (int i = 0; i < chars.length; i++) {
	        char c = chars[i];
	        switch (c)
	        {
	        	case '<': out.append("&lt;"); break;
	        	case '>': out.append("&gt;"); break;
	        	case '&': out.append("&amp;"); break;
	        	case '"': out.append("&quot;"); break;
	        	case '\'': out.append("&apos;"); break;
	        	default:
	        		if (c > 127 || c < 32)
	        		{
	        			out.append("&#" + (int)c + ";");
	        		}
	        		else
	        		{
	        			out.append(c);
	        		}
	        }	       
	    }
	    return out.toString();
	}
	
	public static String formatDate(Date d)
	{
		if (d == null) return "";
		return String.format("%td/%tm/%tY", d, d, d);		
	}
	
	public static String formatDateTime(Date d)
	{
		if (d == null) return "";
		return String.format("%td/%tm/%tY %tH:%tM:%tS", d, d, d, d, d, d);		
	}
	
	public static <T extends Comparable<T>>T Max(Enumeration<T> vals)
	{
		T res = null;
		if (vals == null || !vals.hasMoreElements()) return null;
		
		while (res == null && vals.hasMoreElements())
		{
			res = vals.nextElement();
		}
		while (vals.hasMoreElements())
		{
			T r2 = vals.nextElement();
			if (r2 == null) continue;
			if (res.compareTo(r2) < 0)
			{
				res = r2;
			}
		}
		return res;
	}
	
	public static <T>boolean InList(T val, Iterable<T> list)
	{
		if (list == null) return false;
		if (val == null)
		{
			for(T elem : list)
			{
				if (elem == null) return true;
			}
			return false;
		}
		else
		{
			for(T elem : list)
			{
				if (elem == null) continue;
				if (val.equals(elem)) return true;
			}
			return false;
		}
	}
}
