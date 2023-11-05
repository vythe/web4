package net.vbelev.utils;

import java.io.*;
import java.util.*;

public class DryCereal
{
	public static enum CerealType
	{
		BYTE('B', 2), // unsigned, 0-255
		SHORT('H', 5), 
		INT('I', 9), 
		LONG('L', 17),
		FLOAT('F', 11), // 6 + 3 + 2 signs
		DOUBLE('D', 26), // 16 + 8 + 2 signs
		TIME('T', 16), // it's a timestamp
		MONIKER('M', 16), // a fixed-length string of printable ascii only, for enums and such
		STRING('S', 0), // a short string, 2-char length + up to 100 chars
		STRINGPART('P', 100), // longer strings somehow
		REF('R', 4), // an int (up to 65535_; the serializer will know it's a reference to another object
		START('J', 4), // object start with the obejct reference
		END('E', 4); // object end with the obejct reference

		public final char typeCode;
		public final int length;

		private CerealType(char typeCode, int length) {
			this.typeCode = typeCode;
			this.length = length;
		}

		public static CerealType fromChar(char c)
		{
			for (CerealType c1 : CerealType.values())
			{
				if (c1.typeCode == c) return c1;
			}
			throw new IllegalArgumentException("Invalid CerealType code " + c);
		}
	}

	public static class Flake
	{
		public CerealType type;
		private Object value;

		protected Flake()
		{
		}
		
		public Flake (CerealType t, Object val)
		{
			type = t;
			value = val;
			//System.out.println("flake " + t.name() + ": " + (val == null? "(null)" : val.toString()));
		}
		
		public String toString()
		{
			return type + ":" + value;
		}
		
		public byte getByte()
		{
			if (type != CerealType.BYTE)
			{
				throw new IllegalArgumentException("Flake " + type.name() + " is not byte"); 
			}
			if (value == null) return 0;
			return (byte)value;
		}
		
		public short getShort()
		{
			if (type != CerealType.SHORT && type != CerealType.START && type != CerealType.END)
			{
				throw new IllegalArgumentException("Flake " + type.name() + " is not short"); 
			}
			if (value == null) return 0;
			return (short)value;
		}
		
		public int getInteger()
		{
			if (type != CerealType.INT)
			{
				throw new IllegalArgumentException("Flake " + type.name() + " is not integer"); 
			}
			if (value == null) return 0;
			return (int)value;
		}
		
		public long getLong()
		{
			if (type != CerealType.LONG)
			{
				throw new IllegalArgumentException("Flake " + type.name() + " is not long"); 
			}
			if (value == null) return 0;
			return (long)value;
		}
		
		public String getString()
		{
			if (type != CerealType.STRING && type != CerealType.MONIKER)
			{
				throw new IllegalArgumentException("Flake " + type.name() + " is not string"); 
			}
			if (value == null) return null;
			return (String)value;
		}
	}

	public static class Reader implements Iterator<Flake>
	{

		private InputStreamReader reader;
		private int nextChar = -1;
		public final java.util.Base64.Decoder stringDecoder = java.util.Base64
				.getDecoder();

		public Reader(InputStream is) 
		{
			//reader = new BufferedReader(new InputStreamReader(is));
			reader = new InputStreamReader(is);
		}

		public Reader(String from)
		{
			InputStream is1 = new ByteArrayInputStream(from.getBytes());
			//reader = new BufferedReader(new InputStreamReader(is1));
			reader = new InputStreamReader(is1);
		}		
		
		@Override
		public boolean hasNext()
		{
			if (reader == null) return false;
			if (nextChar < 0)
			{
				
				try
				{
					do
					{
						nextChar = -1;
						nextChar = reader.read();
					}
					while (Character.isWhitespace((char)nextChar));
				}
				catch (IOException x)
				{
					return false;
				}
			}
			return (nextChar >= 0);
		}

		/** After en error, read everything from the input stream, return it as a string */
		public synchronized String skipToEnd()
		{
			StringBuilder b = new StringBuilder();
			if (nextChar >= 0)
			{
				b.append((char)nextChar);
			}
			nextChar = -1;
			try
			{
				int c;
				do
				{
					c = reader.read();
					if (c < 0) break;
					
					b.append((char)c);
				}
				while (c >= 0);
			}
			catch (IOException x)
			{
			}
			return b.toString();			
		}
		
		public synchronized String skipLine()
		{
			StringBuilder b = new StringBuilder();
			if (nextChar >= 0)
			{
				b.append((char)nextChar);
			}
			nextChar = -1;
			try
			{
				int c;
				do
				{
					c = reader.read();
					if (c < 0 || c == '\n') break;
					
					b.append((char)c);
				}
				while (c >= 0 );
			}
			catch (IOException x)
			{
			}
			return b.toString();			
		}
		
		private String getStringFromReader()
		{
			nextChar = -1; 
			char[] lenChars = getReaderChars(reader, 2);
			int len = Decimal.digitFromChar(lenChars[0]) * 16
					+ Decimal.digitFromChar(lenChars[1]);
			char[] strChars = getReaderChars(reader, len);
			// return new String(strChars);
			byte[] bytes = stringDecoder.decode(new String(strChars));
			/*
			char[] chars = new char[bytes.length];
			// System.arraycopy(bytes, 0, chars, 0, bytes.length);
			for (int i = 0; i < bytes.length; i++)
			{
				chars[i] = (char) bytes[i];
			}
			return new String(chars);
			*/
			return new String(bytes);
		}

		@Override
		public Flake next()
		{
			if (Character.isWhitespace((char)nextChar))
			{
				nextChar = -1;
			}
			if (!hasNext())
			{
				throw new NoSuchElementException("DryCereal.next - no next byte");
			}
						
			Flake res = null; //new Flake();
			CerealType type = CerealType.fromChar((char) nextChar);
			if (type == CerealType.STRING || type == CerealType.STRINGPART)
			{
				String resString = "";
				while (type == CerealType.STRINGPART)
				{
					resString += getStringFromReader();
					nextChar = -1;
					if (!hasNext())
					{
						throw new IllegalArgumentException("Failed to read string from stream");
					}
					type = CerealType.fromChar((char) nextChar);
				}
				if (type != CerealType.STRING)
				{
					throw new IllegalArgumentException(
							"Failed to read string from stream");
				}
				resString += getStringFromReader();
				//nextChar = getReaderChar(reader);

				//res.type = CerealType.STRING;
				//res.value = resString;
				res = new Flake(CerealType.STRING, resString);
			}
			else
			{
				//res.type = type;
				int len = type.length;
				long longValue;
				char[] chars = getReaderChars(reader, len);
				//nextChar = getReaderChar(reader);
				switch (type)
				{
					case BYTE :
						// longValue = Decimal.digitsToLong(chars, 16);
						longValue = Decimal
								.fromString2(new String(chars), null, 16)
								.toLong();
						//res.value = (byte) longValue;
						res = new Flake(CerealType.BYTE, (byte)longValue);
						break;
					case SHORT :
						// longValue = Decimal.digitsToLong(chars, 16);
						longValue = Decimal
								.fromString2(new String(chars), null, 16)
								.toLong();
						//res.value = (short) longValue;
						res = new Flake(CerealType.SHORT, (short)longValue);
						break;
					case INT :
						// longValue = Decimal.digitsToLong(chars, 16);
						longValue = Decimal
								.fromString2(new String(chars), null, 16)
								.toLong();
						//res.value = (int) longValue;
						res = new Flake(CerealType.INT, (int)longValue);
						break;
					case LONG :
						// longValue = Decimal.digitsToLong(chars, 16);
						longValue = Decimal
								.fromString2(new String(chars), null, 16)
								.toLong();
						//res.value = (long) longValue;
						res = new Flake(CerealType.LONG, (long)longValue);
						break;
					case TIME :
						// longValue = Decimal.digitsToLong(chars, 16);
						longValue = Decimal
								.fromString2(new String(chars), null, 16)
								.toLong();
						//res.value = new Date(longValue);
						res = new Flake(CerealType.TIME, (long)longValue);
						throw new IllegalArgumentException("CerealType.TIME is not supported");
						//break;
					case FLOAT :
						Decimal floatDecimal = Decimal.fromString2(
								new String(chars, 0, 7),
								new String(chars, 7, 4), 16);
						//res.value = (float) floatDecimal.toDouble();
						res = new Flake(CerealType.FLOAT, (float)floatDecimal.toDouble());
						break;
					case DOUBLE :
						Decimal doubleDecimal = Decimal.fromString2(
								new String(chars, 0, 17),
								new String(chars, 17, 9), 16);
						//res.value = (float) doubleDecimal.toDouble();
						res = new Flake(CerealType.DOUBLE, doubleDecimal.toDouble());
						break;
					case MONIKER :
						//res.value = new String(chars, 1, (int)(chars[0] - 'A'));
						res = new Flake(CerealType.MONIKER, new String(chars, 1, (int)(chars[0] - 'A')));
						break;
					case START :
						// longValue = Decimal.digitsToLong(chars, 16);
						longValue = Decimal
								.fromString2(new String(chars), null, 16)
								.toLong();
						//res.value = (short) longValue;
						res = new Flake(CerealType.START, (short)longValue);
						break;
					case END :
						// longValue = Decimal.digitsToLong(chars, 16);
						longValue = Decimal
								.fromString2(new String(chars), null, 16)
								.toLong();
						//res.value = (short) longValue;
						res = new Flake(CerealType.END, (short)longValue);
						break;
					default :
						throw new IllegalArgumentException(
								"Unexpected cereal type " + type
										+ " in CerealType.Reader.next()");
				}
			}
			nextChar = -1;
			//System.out.println("DC:" + res.toString());
			return res;
		}
	}

	private static final char[] monikerSpaces = new char[CerealType.MONIKER.length];

	static 
	{
		Arrays.fill(monikerSpaces, ' ');
	}
	
	private static char[] getReaderChars(InputStreamReader reader, int len)
	{
		char[] res = new char[len];
		for (int i = 0; i < len; i++)
		{
			try
			{
				int c = reader.read();
				if (c < 0 || (c != ' ' && Character.isWhitespace(c)))
				{
					throw new IllegalArgumentException("Failed to read " + len
							+ " chars from the Cereal stream");
				}
				res[i] = (char) c;
			}
			catch (IOException x)
			{
				throw new IllegalArgumentException("Failed to read " + len
						+ " chars from the Cereal stream");
			}
		}
		return res;
	}

	public final static int MAX_LINE_SIZE = 255;

	private final StringBuilder buff;
	private final ByteArrayOutputStream outStream;
	private final java.io.BufferedWriter writer;

	private int objSequence = 0;
	private final ArrayList<Integer> objStack = new ArrayList<Integer>();
	
	public final java.util.Base64.Encoder stringEncoder = java.util.Base64
			.getEncoder();

	//public int lineSize = 255;

	public DryCereal() {
		buff = new StringBuilder(); //MAX_LINE_SIZE);
		buff.setLength(0);
		outStream = new java.io.ByteArrayOutputStream();
		writer = new BufferedWriter(new OutputStreamWriter(outStream));
	}

	public DryCereal(OutputStream out) {
		buff = new StringBuilder(); //MAX_LINE_SIZE);
		buff.setLength(0);
		if (out == null)
		{
			outStream = null;
			new java.io.ByteArrayOutputStream();
			writer = new BufferedWriter(new OutputStreamWriter(outStream));
		}
		else
		{
			outStream = null;
			writer = new BufferedWriter(new OutputStreamWriter(out));
		}
	}

	public String toString()
	{
		return buff.toString();
	}

	/**
	 * for a DryCereal with its own memory buffer, returns the text serialized
	 * so far (and removes it from the buffer); for a DryCereal with an external
	 * output stream, returns null.
	 * 
	 * @return
	 */
	public String pull() throws IOException
	{
		if (outStream == null)
		{
			return null;
		}
		flush(-1);
		String res = outStream.toString();
		outStream.reset();
		return res;
	}

	/** Ends the current output line and flushes the output stream */
	public synchronized void flush() throws IOException
	{
		flush(-1);
	}
	
	/** Checks if there is least extraLength chars left in the current line;
	 * if there is not - ends the current line and flushes the output stream
	 */
	private synchronized void flush(int extraLength) throws IOException
	{
		if (buff.length() == 0) return;
		
		if (extraLength < 0 || buff.length() + extraLength + 1 > MAX_LINE_SIZE)
		{
			buff.append('\n');
			char[] line = new char[buff.length()];
			buff.getChars(0, line.length, line, 0);
			buff.setLength(0);
			//System.out.println("dry write [" + new String(line) + "]"); 
			writer.write(line);
			writer.flush();
		}
	}

	private static final char[] hex = new char[]{'0', '1', '2', '3', '4', '5',
			'6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

	private void addDigits(long val, int len)
	{
		long mask = 0xfL << ((len - 1) * 4);
		int shift = (len - 1) * 4;
		for (; shift >= 0; shift -= 4, mask = mask >>> 4)
		{
			byte c = (byte) ((val & mask) >>> shift);
			buff.append(hex[c]);
		}
	}

	public synchronized void addByte(int val) throws IOException
	{
		flush(3);
		buff.append(CerealType.BYTE.typeCode);
		addDigits(val, 2);
	}

	public synchronized void addShort(int val) throws IOException
	{
		flush(6);
		buff.append(CerealType.SHORT.typeCode);
		if (val > 0)
		{
			buff.append('+');
			addDigits(val, 4);
		}
		else
		{
			buff.append('-');
			addDigits(-val, 4);
		}
	}

	public synchronized void addInt(int val) throws IOException
	{
		flush(10);
		buff.append(CerealType.INT.typeCode);
		buff.append(Decimal.longToString(val, 8, 16));
	}

	public synchronized void addLong(long val) throws IOException
	{
		flush(18);
		buff.append(CerealType.LONG.typeCode);
		buff.append(Decimal.longToString(val, 16, 16));
	}

	public synchronized void addTime(Date d) throws IOException
	{
		long dt = d == null ? 0 : d.getTime();
		flush(18);
		buff.append(CerealType.TIME.typeCode);
		buff.append(Decimal.longToString(dt, 16, 16));

	}
	public synchronized void addFloat(double val) throws IOException
	{
		/*
		 * int maxprec = 6; int sign = (int)Math.signum(val); val =
		 * Math.abs(val); double lgBase = Math.log10(val); int pow =
		 * (int)Math.floor(lgBase); //double base = val / Math.pow(10, pow);
		 * double base = Math.pow(10, lgBase - pow); double step = 1.; int res =
		 * 0; for (int i = 0; i < maxprec; i++) { res = res * 10; while (base >
		 * step) { base -= step; res++; } //step = step / 10.; base = base * 10;
		 * } res += Math.round(base / 10); System.out.println("pow=" + (pow -
		 * maxprec + 1) + ", res=" + res); int exponent = Math.getExponent(val);
		 * double mant = val / Math.pow(2, exponent);
		 * //System.out.println("exponent=" + exponent + ", mant=" + mant);
		 * 
		 * buff.append(CerealType.FLOAT.typeCode); if (val > 0) {
		 * buff.append('+'); addDigits((long)mant, 8); } else {
		 * buff.append('-'); addDigits(-(long)mant, 8); }
		 */
		flush(12);
		Decimal d = new Decimal(val, 6);
		buff.append(CerealType.FLOAT.typeCode);
		buff.append(d.printMantissa(6, 16));
		buff.append(Decimal.longToString(d.getPower(), 3, 16));
	}

	public synchronized void addDouble(double val) throws IOException
	{
		flush(26);
		Decimal d = new Decimal(val, 16);
		buff.append(CerealType.DOUBLE.typeCode);
		buff.append(d.printMantissa(16, 16));
		buff.append(Decimal.longToString(d.getPower(), 8, 16));
	}

	public synchronized void addIntOld(int val)
	{
		int mask = 0xf000;
		int shift = 12;
		for (; shift >= 0; shift -= 4, mask = mask >> 4)
		{
			int c = (val & mask) >> shift;
			buff.append(hex[c]);
		}
	}
/*
	public static void testInt(int val) throws IOException
	{
		DryCereal c = new DryCereal();
		// int val = 17;
		c.addLong(val);
		System.out.printf("add int: %d hex %h cereal:[%s]\n", val, val,
				c.buff.toString());
	}

	public static void testFloat(double val) throws IOException
	{
		DryCereal c = new DryCereal();
		// int val = 17;
		c.addFloat((float) val);
		System.out.printf("add float: %f, cereal:[%s]\n", val,
				c.buff.toString());
	}
*/
	private void addSubstring(char prefix, String part) throws IOException
	{
		byte[] bytes = part == null? new byte[0] : stringEncoder.encode(part.getBytes());
		char[] chars = new char[bytes.length];
		// System.arraycopy(bytes, 0, chars, 0, bytes.length);
		for (int i = 0; i < bytes.length; i++)
		{
			chars[i] = (char) bytes[i];
		}

		flush(chars.length + 3);
		buff.append(prefix);
		buff.append(Decimal.unsignedToString(chars.length, 2, 16));
		buff.append(chars);
	}

	public synchronized void addString(String s) throws IOException
	{
		if (s != null)
		{
		while (s.length() > CerealType.STRINGPART.length)
		{
			String part = s.substring(0, CerealType.STRINGPART.length);
			s = s.substring(CerealType.STRINGPART.length);
			addSubstring(CerealType.STRINGPART.typeCode, part);
		}
		}
		addSubstring(CerealType.STRING.typeCode, s);
	}
	
	public synchronized void addMoniker(String s) throws IOException
	{
		char[] chars = s == null? new char[0] : s.toCharArray();
		if (chars.length >= CerealType.MONIKER.length)
		{
			throw new IllegalArgumentException("Not a valid moniker string"); 
		}
		flush(CerealType.MONIKER.length + 1);
		for (char c : chars)
		{
			if ((c & 0xff00) != 0 || c < ' ')
			{
				throw new IllegalArgumentException("Not a valid moniker string"); 
			}
		}
		buff.append(CerealType.MONIKER.typeCode);
		buff.append((char)(chars.length + 'A'));
		buff.append(chars);
		if (chars.length < CerealType.MONIKER.length)
		{
			buff.append(monikerSpaces, 0, CerealType.MONIKER.length - chars.length);
		}
	}
	
	public synchronized short addObjectStart() throws IOException
	{
		flush(CerealType.START.length + 1);
		buff.append(CerealType.START.typeCode);
		int id = ++objSequence;
		objStack.add(id);
		buff.append(Decimal.unsignedToString(id, CerealType.START.length, 16));
		
		return (short)id;
	}
	
	public synchronized short addObjectStart(short key) throws IOException
	{
		flush(CerealType.START.length + 1);
		buff.append(CerealType.START.typeCode);
		buff.append(Decimal.unsignedToString(key, CerealType.START.length, 16));
		
		return key;
	}
	
	public synchronized short addObjectEnd() throws IOException
	{
		if (objStack.size() == 0)
		{
			throw new IllegalArgumentException("Unmatched object end in DryCereal");
		}
		flush(CerealType.END.length + 1);
		buff.append(CerealType.END.typeCode);
		int id = objStack.remove(objStack.size() - 1);
		buff.append(Decimal.unsignedToString(id, CerealType.END.length, 16));
		
		return (short)id;
	}
	
	public synchronized short addObjectEnd(short key) throws IOException
	{
		flush(CerealType.END.length + 1);
		buff.append(CerealType.END.typeCode);
		buff.append(Decimal.unsignedToString(key, CerealType.END.length, 16));
		
		return key;
	}
	
	/** appends a previously constructed cereal as is, without validation */
	public synchronized void addRaw(String cereal) throws IOException
	{
		if (cereal == null || cereal.length() == 0) return;
		flush(-1);
		buff.append(cereal);
		flush(0);
	}
	
	//public static List<Flake> readList(Reader from, 
}
