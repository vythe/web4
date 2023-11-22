package net.vbelev.utils;


import java.lang.annotation.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
//import java.nio.charset.Charset;

//import com.sun.jmx.snmp.internal.SnmpSecuritySubSystem;

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
	
	@SafeVarargs
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
	
	private static java.util.Base64.Encoder encodeBytes64Encoder = null;    
	
	public static String encodeBytes64(byte[] bytes)
	{
		if (bytes == null || bytes.length == 0) return "";
		
		if (encodeBytes64Encoder == null)
		{
			encodeBytes64Encoder = java.util.Base64.getEncoder();
		}
		
		byte[] bytes64 = encodeBytes64Encoder.encode(bytes);
		return new String(bytes64, StandardCharsets.UTF_8);
	}

	private static java.util.Base64.Decoder decodeBytes64Decoder = null;

	public static byte[] decodeBytes64(String str)
	{
		if (str == null || str.length() == 0) return new byte[0];
		
		if (decodeBytes64Decoder == null)
		{
			decodeBytes64Decoder = java.util.Base64.getDecoder();
		}
		
		byte[] bytes64 = str.getBytes(StandardCharsets.UTF_8);
		byte[] bytes = decodeBytes64Decoder.decode(bytes64);
		return bytes;
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
	
	public static String formatTime(Date d)
	{
		if (d == null) return "";
		return String.format("%tH:%tM:%tS", d, d, d);		
	}
	
	public static String formatMilliseconds(long ms)
	{
		String res = "";
		
		/*
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		c.setTime(d);
		int year = c.get(Calendar.YEAR) - 1970;
		int month = c.get(Calendar.MONTH);
		int day = c.get(Calendar.DATE) - 1;
		int hour = c.get(Calendar.HOUR_OF_DAY);
		int minute = c.get(Calendar.MINUTE);
		int second = c.get(Calendar.SECOND);
		int msec = c.get(Calendar.MILLISECOND);
		
		if (year > 0) res += year + " years ";
		if (month > 0) res += month + " months ";
		if (day > 0) res += day + " days ";
		//if (hour > 0) res += hour + ":";
		//if (hour > 0 || minute > 0) res += minute + ":";
		res = String.format("%s%02d:%02d:%02d.%03d", res, hour, minute, second, msec);
*/
		long ts = ms;
		long msec_in_hour = 60 * 60 * 1000;
		long msec_in_minute = 60 * 1000;
		long msec_in_second = 1000;
		int hour = (int)(ts / msec_in_hour);
		ts = ts % msec_in_hour;
		int minute = (int)(ts / msec_in_minute);
		ts = ts % msec_in_minute;
		int second = (int)(ts / msec_in_second);
		ts = ts % msec_in_second;
		return String.format("%02d:%02d:%02d.%03d",hour, minute, second, ts);
	}
	
	private final static char[] randomStringLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();	
	public static String randomString(int length)
	{
		char[] res = new char[length];
		for (int i = 0; i < length; i++) 
			res[i] = randomStringLetters[random.nextInt(randomStringLetters.length)];
		return new String(res);
	}
	
	public static String randomString64(int length)
	{
		if (length < 4) length = 4;
		byte[] res = new byte[length];
		random.nextBytes(res);
		return encodeBytes64(res).substring(0, length - 1);
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
	
	@SafeVarargs
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
	
	public static <T>T FirstOrDefault(Iterable<T> elems, Predicate<T> condition)
	{
		return FirstOrDefault(elems, condition, null);
	}

	public static <T>T FirstOrDefault(Iterable<T> elems, Predicate<T> condition, T defaultValue)
	{
		if (elems == null) return defaultValue;

		/*
		return elems.stream()
				.filter(condition)
				.findFirst()
				.orElse(defaultValue)
				;
		*/
		Iterator<T> it = elems.iterator();
		while (it.hasNext())
		{
			T item = it.next();
			if (condition == null || condition.test(item)) return item;
		}
		return defaultValue;
	}
	
	public static Integer[] box(int... args)
	{
		if (args == null) return new Integer[0];
		Integer[] res = new Integer[args.length];
		for (int pos = 0; pos < args.length; pos++) res[pos] = args[pos];
		
		return res;
	}
	
	public static Double[] box(double... args)
	{
		if (args == null) return new Double[0];
		Double[] res = new Double[args.length];
		for (int pos = 0; pos < args.length; pos++) res[pos] = args[pos];
		
		return res;
	}
	
	public static double round(double val, int floatingPos)
	{
		if (floatingPos == 0) return (double)Math.round(val);
		if (floatingPos < 0)
		{
			int pow = (int)Math.pow(10, -floatingPos);
			return pow * (double)Math.round(val / pow);
		}
		else
		{
		int pow = (int)Math.pow(10, floatingPos);
		return (double)Math.round(val * pow) / pow;
		}
	}
	
	private static class StringIterator<T> implements Iterator<String>
	{

		Iterator<T> iter;
		public StringIterator(Iterator<T> from)
		{
			iter = from;
		}
		
		@Override
		public boolean hasNext()
		{
			// TODO Auto-generated method stub
			return iter == null? false : iter.hasNext();
		}

		@Override
		public String next()
		{
			// TODO Auto-generated method stub
			if (iter == null) return null;
			
			T n = iter.next();
			return n == null? "" : n.toString();
		}		
	}
	
	public static class StringIterable<T> implements Iterable<String>
	{
		private Iterable<T> from;
		public StringIterable(Iterable<T> from)
		{
			this.from = from;
		}
		
		public StringIterable(T[] from)
		{
			this.from = from == null? null : Arrays.asList(from);
		}
		
		@Override
		public Iterator<String> iterator()
		{
			Iterator<T> fromIterator = from == null? null : from.iterator();
			
			return new StringIterator<T>(fromIterator); 
		}
	}
	
	/**
	 * The class it not thread-safe!
	 */
	public static class StopWatch
	{
		private long startTime = 0;
		private long stopTime = 0;
		private boolean running = false;
		private long totalElapsed = 0;
		private long startMark = 0;

		@Override
		public String toString()
		{
			return Utils.formatMilliseconds(getElapsedNano() / 1000000);
		}
		public void reset()
		{
			startTime = 0;
			stopTime = 0;
			running = false;
			totalElapsed = 0;
		}

		/**
		 * Starts of continues the stopwatch. If it is already running, then
		 * does nothing.
		 */
		public void start()
		{
			if (!running)
			{
				this.startMark = System.nanoTime();
				if (this.startTime == 0)
					this.startTime = System.currentTimeMillis();
				this.running = true;
			}
		}

		public void stop()
		{
			if (running)
			{
				this.totalElapsed += System.nanoTime() - this.startMark;
				this.running = false;
				this.stopTime = System.currentTimeMillis();
			}
		}

		public Date getStartTime()
		{
			return new Date(this.startTime);
		}

		public Date getStopTime()
		{
			return new Date(this.stopTime);
		}

		public boolean isRunning()
		{
			return running;
		}

		public long getElapsedNano()
		{
			if (running)
			{
				return totalElapsed + System.nanoTime() - this.startMark;
			}
			else
			{
				return this.totalElapsed;
			}
		}

		public java.util.Date getElapsedTime()
		{
			long elp;
			if (running)
			{
				elp = totalElapsed + System.nanoTime() - this.startMark;
			}
			else
			{
				elp = totalElapsed;
			}
			return new Date(elp / 1000000);
		}
	}
}
