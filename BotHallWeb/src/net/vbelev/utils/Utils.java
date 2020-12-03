package net.vbelev.utils;


import java.lang.annotation.*;
import java.util.*;
//import java.nio.charset.Charset;

//import com.sun.media.jfxmedia.track.Track.Encoding;

/** 
 * Copied from net.vbelev.web4.utils ; will be moved to a shared library later.
 * @author Vythe
 *
 */
public class Utils
{
	@Retention(value=RetentionPolicy.RUNTIME)
	public @interface EnumElement
	{
		int code() default 0;
		String description() default "";
	}
	
	public static final Random random = new Random();
	
	
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
	
	private static Hashtable<String, Integer> s_enumCodes = new Hashtable<String, Integer>();
	/** this thing is rather slow
	 */
	public static <T extends Enum<T>>int getEnumCode(T enumValue)
	{
		if (enumValue == null) return 0;
		int res = 0;
		String enumName = enumValue.getClass().getName() + ":" + enumValue.name();		
		if (s_enumCodes.containsKey(enumName))
		{
			 return s_enumCodes.get(enumName);
		}
		EnumElement ann = null;
		try
		{
			// getDeclaredField only checks the class itself (no hierarchy), so maybe it is faster
			ann = enumValue.getClass().getDeclaredField(enumValue.name()).getAnnotation(EnumElement.class);
		}
		catch (NoSuchFieldException x)
		{
			ann = null;
		}
		if (ann == null) res = enumValue.ordinal();
		else res = ann.code();
		s_enumCodes.put(enumName, res);
		
		return res;

	}
	
	/** this thing is rather slow
	 */
	public static <T extends Enum<T>>String getEnumDescription(T enumValue)
	{
		if (enumValue == null) return "";
		EnumElement ann = null;
		try
		{
			// getDeclaredField only checks the class itself (no hierarchy), so maybe it is faster
			ann = enumValue.getClass().getDeclaredField(enumValue.name()).getAnnotation(EnumElement.class);
		}
		catch (NoSuchFieldException x)
		{
			ann = null;
		}
		if (ann == null) return enumValue.name();
		
		return ann.description();
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
	
	public static int[] intArray( int... args)
	{
		if (args == null) return new int[0];
		return Arrays.copyOf(args, args.length);
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
	
	public static String encodeJSON(String src)
	{
		if (src == null || src.length() == 0) return "\"\"";
		StringBuilder out = new StringBuilder();
		char[] chars = src.toCharArray();
		out.append("\"");
	    for (int i = 0; i < chars.length; i++) {
	        char c = chars[i];
	        switch (c)
	        {
	        	case '\"': out.append("\\\""); break;
	        	case '\n': out.append("\\n"); break;
	        	default: out.append(c); break;
	        }	       
	    }
	    out.append("\"");
	    
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
	
	private final static char[] randomStringLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();	
	public static String randomString(int length)
	{
		char[] res = new char[length];
		for (int i = 0; i < length; i++) 
			res[i] = randomStringLetters[random.nextInt(randomStringLetters.length)];
		return new String(res);
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
	
	public static <T>boolean InList(T val, T... list)
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
