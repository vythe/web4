package net.vbelev.utils;

import java.math.BigInteger;
import java.util.*;
import java.util.regex.*;

/**
 * This class is supposed to be immutable
 */
public class Decimal implements Comparable<Object>
{
	/** this is the sign , true = positive */
	private boolean sign;
	
	/** privately, mantissa is a very long integer
	 * stored as chars (9,8, 9, 2, 0, 1 etc);
	 *  //the order is human-readable, 105469 -> [1, 0, 5, 4, 6, 9]
	 * the order is now reversed to math: 105469 -> [9, 6, 4, 5, 0, 1]
	 */
	private char[] mantissa;
	
	/**
	 * Let us not rely on mantissa.length (in case we use a bigger array there)
	 */
	private int mantissaLength;
	/** power is the number of zeroes after the mantissa:
	 * 6.06573e10 -> 60657300000 -> 606573p5
	 */
	private int power;
	/** the engineering power, as 8  in 1.2345e8. Equals to mantissa.length - 1 + power */ 
	private int ePower;
	
	private static final char[] digitSymbols = new char[]{
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 
		'A', 'B', 'C', 'D', 'E', 'F', '*', '*', '*', '*',
		'*', '*', '*', '*', '*', '*', '*', '*', '*', '*',
		'*', '*', '*', '*', '*', '*', '*', '*', '*', '*',
		'*', '*', '*', '*', '*', '*', '*', '*', '*', '*',
		'*', '*', '*', '*', '*', '*', '*', '*', '*', '*',
		'*', '*', '*', '*', '*', '*', '*', '*', '*', '*',
		'*', '*', '*', '*', '*', '*', '*', '*', '*', '*',
		'*', '*', '*', '*', '*', '*', '*', '*', '*', '*',
		'*', '*', '*', '*', '*', '*', '*', '*', '*', '*'
	};
	private static char[] EMPTY_CHAR_ARR = new char[0];
	
	public final static Decimal ZERO = new Decimal();
	
	public final static Decimal ONE = new Decimal(1);

	public final static Decimal TWO = new Decimal(2);
	
//==== Constructors ====
	
	private void buildEmpty()
	{
		mantissa = EMPTY_CHAR_ARR;
		power = 0;
		ePower = 0;
		mantissaLength = 0;
		sign = true;
	}
	/**
	 * Returns zero value
	 * @category constructor
	 */
	public Decimal()
	{
		buildEmpty();
	}
	
	/**
	 * Note that this constructor make the object mutable, as it exposes the mantissa
	 */
	public Decimal(char[] mantissa, int power, boolean sign)
	{
		this.mantissa = mantissa;
		mantissaLength = mantissa.length;
		this.power = power;
		this.ePower = power + mantissa.length - 1;
		this.sign = sign;
	}
		
	private void trimMantissa()
	{
		int resPower = 0;
		while (resPower < mantissa.length && mantissa[resPower] == 0) 
			resPower++;
		if (resPower >= mantissa.length) // it's all empty!
		{
			this.mantissa = new char[0];
			this.mantissaLength = 0;
			this.power = 0;
			this.ePower = 0;
			this.sign = true;
			return;
		}
		int tail = 0;
		while (tail < mantissa.length && mantissa[mantissa.length - tail - 1] == 0) 
			tail++;
		if (resPower != 0 || tail != 0)
		{		
			char[] newMantissa = new char[mantissa.length - resPower - tail];
			System.arraycopy(mantissa, resPower, newMantissa, 0, newMantissa.length);
			this.mantissa = newMantissa;
			this.mantissaLength = newMantissa.length;
			this.power += resPower;
		}
		this.ePower = this.power + this.mantissa.length - 1;
	}
	
	/**
	 * @category constructor
	 */
	public Decimal(Decimal from)
	{
		mantissa = Arrays.copyOf(from.mantissa, from.mantissa.length);
		mantissaLength = mantissa.length;
		power = from.power;
		ePower = from.ePower;
		sign = from.sign;
	}
	
	/**
	 * @category constructor
	 */
	public Decimal(long val)
	{
		if (val == 0)
		{
			buildEmpty();
		}
		else
		{
			sign = (val >= 0);
			buildFromLong(sign? val : -val);
		}
	}
	
	/**
	 * @category constructor
	 * constructs val * 10^power
	 */
	public Decimal(long val, int power)
	{
		if (val == 0)
		{
			buildEmpty();
		}
		else
		{
			sign = (val >= 0);
			buildFromLong(sign? val : -val);
			this.power += power;
		}
	}
	
	/**
	 * @category constructor
	 * Uses the default number of digits = 26
	 */
	public Decimal(double val)
	{
		// An IEEE 754 float, as implemented by the Java language, has 32 bits. The first bit is the sign bit, 0 for positive and 1 for negative.
		// The next eight bits are the exponent and can hold a value from -125 to +127. 
		// The final 23 bits hold the mantissa (sometimes called the significand), which ranges from 0 to 33,554,431.
		// for double, boolean negative = (bits & 0x8000000000000000L) != 0; // 1 bit
		//long exponent = bits & 0x7ff0000000000000L; // 11 bits (power of 2)
		//long mantissa = bits & 0x000fffffffffffffL; // 42 bits

		if (val == 0)
		{
			buildEmpty();
		}
		else
		{
			sign = (val >= 0);
			//int maxprec = 26; // 8 for float, ~26 for double
			buildFromDouble(sign? val: -val, 26);
		}
	}

	/**
	 * @category constructor
	 */
	public Decimal(double val, int maxdigits)
	{
		if (maxdigits < 1)
		{
			throw new IllegalArgumentException("Decimal maxdigits must be 1 or greater");
		}
		if (val == 0)
		{
			buildEmpty();
		}
		else
		{
			sign = (val >= 0);
			//int maxprec = 26; // 8 for float, ~26 for double
			buildFromDouble(sign? val: -val, maxdigits);
		}
	}
	
	public static Decimal random(int digits, int ePower)
	{
		if (digits <= 0) return ZERO;
		
		char[] mantissa = new char[digits];
		for (int i = 0; i < digits - 1; i++)
			mantissa[i] = (char)Utils.random.nextInt(10);
		mantissa[digits - 1] = (char)(1 + Utils.random.nextInt(9));
		
		return new Decimal(mantissa, ePower - mantissa.length + 1, true);
	}
	
	/** regexp is slow, so it used for testing only */
	private static final Pattern decimalNumberRX 
	= Pattern.compile("^(?<sign>\\+|-)?(?<mant1>\\d+)((?:\\.)(?<mant2>\\d*))?((?:e|E)(?<powersign>\\+|-)?(?<power>\\d+))?$");
	
	/** regexp is slow, so it used for testing only */
	public void buildFromStringRegex(String from)
	{
		System.out.println("fromString matching " + from);
		Matcher m = decimalNumberRX.matcher(from);
		if (!m.find())
		{
			throw new IllegalArgumentException("Invalid decimal string: " + from);
		}
		else  
		{
			for (int i = 0; i < m.groupCount(); i++)
			{
				System.out.println(i + ": " + m.group(i));
			}
		}
		System.out.println("fromString total groups: " + m.groupCount());
		// this is unfinished
	}
	
	/** 
	 * @category constructor
	 * Parses the usual 10-based decimal string ("1.2345e-23") into Decimal.
	 * The full syntax: +1234.456e+123 ; signs are optional
	 */
	public Decimal(String from)
	{
		if (from == null || from.length() == 0) 
		{
			buildEmpty();
			return;
		}
		
		// 
		char[] valchars = from.toCharArray();
		char[] mant = new char[valchars.length];
		char[] powerChars = new char[valchars.length];
		int pos = 0;
		int mantpos = 0;
		int powerpos = 0;
		int mantpointpos = -1;
		//
		if (valchars[pos] == '+')
		{
			this.sign = true;
			pos++;
		}
		else if (valchars[pos] == '-')
		{
			this.sign = false;
			pos++;
		}
		else
		{
			this.sign = true; // no sign
		}
		// mantissa integer part
		while (pos < valchars.length && valchars[pos] != '.' && valchars[pos] != 'e' && valchars[pos] != 'E')
		{
			mant[mantpos++] = valchars[pos++];
		}
		// decimal point
		if (pos < valchars.length && valchars[pos] == '.')
		{
			mantpointpos = mantpos;
			pos++;
		}
		// mantissa decimal part
		while (pos < valchars.length && valchars[pos] != 'e' && valchars[pos] != 'E')
		{
			mant[mantpos++] = valchars[pos++];
		}
		if (pos < valchars.length)
		{
			pos++; // this is E
		}
		// power sign
		boolean powerIsPositive = true;
		if (pos < valchars.length && valchars[pos] == '+')
		{
			powerIsPositive = true;
			pos++;
		}
		else if (pos < valchars.length && valchars[pos] == '-')
		{
			powerIsPositive = false;
			pos++;
		}
		else if (pos < valchars.length)
		{
			powerIsPositive = true;
		}
		while (pos < valchars.length)
		{
			powerChars[powerpos++] = valchars[pos++];
		}
		
		// convert chars to digits and 
		for (int i = 0; i < mantpos; i++)
		{
			mant[i] = digitFromChar(mant[i]);
		}
		mant = CharMath.reverseArray(mant);
		for (int i = 0; i < powerpos; i++)
		{
			powerChars[i] = digitFromChar(powerChars[i]);
		}
		powerChars = CharMath.reverseArray(powerChars);
		this.mantissa = Arrays.copyOfRange(mant, mant.length - mantpos, mant.length);
		this.power = (int)digitsToLong(Arrays.copyOfRange(powerChars, powerChars.length - powerpos, powerChars.length), 10);
		if (!powerIsPositive)
		{
			this.power = -this.power;
		}
		if (mantpointpos >= 0)
		{
			this.power -= mantpos - mantpointpos;
		}
		this.ePower = this.power + this.mantissa.length - 1;
		this.mantissaLength = this.mantissa.length;
		this.trimMantissa();
	}

	/**
	 * @category constructor
	 */
	public static Decimal fromObject(Object arg0)
	{
		//treat null as 0
		Decimal other = null;
		if (arg0 == null)
		{
			other = new Decimal();
		}
		else if (arg0 instanceof Decimal)
		{
			other = (Decimal)arg0;
		}
		else if (arg0 instanceof String)
		{
			other = new Decimal((String)arg0);
		}
		else if (arg0 instanceof Long)
		{
			other = new Decimal((Long)arg0);
		}
		else if (arg0 instanceof Integer)
		{
			other = new Decimal((Integer)arg0);
		}
		else if (arg0 instanceof Short)
		{
			other = new Decimal((Short)arg0);
		}
		else if (arg0 instanceof Double)
		{
			other = new Decimal((Double)arg0);
		}
		else if (arg0 instanceof Float)
		{
			other = new Decimal((Float)arg0);
		}
		else
		{
			String otherStr = arg0.toString();
			other = new Decimal(otherStr);
		}
		/*
		if (other == null)
		{
			throw new ClassCastException("Cannot cast " + arg0.getClass().getName() + " to " + Decimal.class.getName());
		}
		*/
		return other;
	}
	
//==== Object methods ====
	@Override
	public boolean equals(Object arg0)
	{
		return this.compareTo(arg0) == 0;
	}
	
	@Override
	public int compareTo(Object arg0)
	{
		// return negative is this < arg0
		Decimal other = Decimal.fromObject(arg0);
		
		if (this.isZero())
		{
			if (other.isZero()) return 0;
			if (!other.sign) return 1;
			if (other.sign) return -1;
		}
		
		if (this.sign && !other.sign) return 1;
		if (!this.sign && other.sign) return -1;
		//int fact = this.isPositive? 1 : -1;
		
		if (this.ePower > other.ePower) return this.sign? 1 : -1;
		if (this.ePower < other.ePower) return this.sign? -1 : 1;
		
		int res = CharMath.compareArrays(this.mantissa, other.mantissa, this.power - other.power);
		return this.sign? res : -res;
		/*
		int thisPos = this.mantissa.length - 1;
		int otherPos = other.mantissa.length - 1;
		while (thisPos >=0 || otherPos >= 0)
		{
			if (thisPos < 0) return this.sign? -1 : 1;
			if (otherPos < 0) return this.sign? 1 : -1;
			if (this.mantissa[thisPos] < other.mantissa[otherPos]) return this.sign? -1 : 1;
			if (this.mantissa[thisPos] > other.mantissa[otherPos]) return this.sign? 1 : -1;
			thisPos--;
			otherPos--;
		}
		
		return 0;
		*/
	}
	
	/** 
	 * Simplified compareTo() that works for both positive this and to)
	 */
	public int compareTest(Decimal to)
	{
		return CharMath.compareArrays(this.mantissa, to.mantissa, (this.power) - (to.power));
	}
	
	/**
	 * Same as compareTo(), returns -1 if arr1 < arr2
	 *
	private static int compareArrays(char[] arr1, char arr2)
	{
		
	}*/
	
	@Override
	public String toString()
	{
		return this.isInteger()? toStringF() : toStringE();
	}
	
	//==== type cast methods ====
	
	public String toStringP()
	{
		StringBuilder sb = new StringBuilder();
		//sb.append(mantissa);
		if (!sign)
		{
			sb.append('-');
		}
		if (mantissa.length == 0)
		{
			sb.append('0');			
		}
		else
		{
			for (int i = mantissa.length - 1; i >= 0; i--)
			{
				sb.append(digitSymbols[mantissa[i]]);
			}
			if (power != 0)
			{
				sb.append("p");
				sb.append(power);
			}
		}		
		return sb.toString();
	}

	/**
	 * Converts this into a string in the standard (scientific) format 1.2345e10
	 * @return
	 */
	public String toStringE()
	{
		StringBuilder sb = new StringBuilder();
		//sb.append(mantissa);
		if (!sign)
		{
			sb.append('-');
		}
		if (mantissa.length == 0)
		{
			sb.append('0');			
		}
		else
		{
			sb.append(digitSymbols[mantissa[mantissa.length - 1]]);
			if (mantissa.length > 1)
			{
				sb.append('.');
				for (int i = mantissa.length - 2; i >= 0; i--)
				{
					sb.append(digitSymbols[mantissa[i]]);
				}
			}
			if (power + mantissa.length - 1 != 0)
			{
				sb.append("e");
				sb.append(power + mantissa.length - 1);
			}
		}
		
		return sb.toString();
	}
	
	/**
	 * Converts this into a string in the floating-point format 1234.3456 without the power
	 * @return
	 */
	public String toStringF()
	{
		StringBuilder sb = new StringBuilder();
		//sb.append(mantissa);
		char zero = digitSymbols[0];
		if (!sign)
		{
			sb.append('-');
		}
		if (mantissa.length == 0)
		{
			sb.append(zero);			
		}
		else
		{
			int decimalPos = this.mantissa.length + this.power;
			if (decimalPos <= 0)
			{
				sb.append(zero);
				sb.append('.');			
				for (int pos = 0; pos < -decimalPos; pos++)
				{
					sb.append(zero);
				}
			}
			
			for (int pos = mantissa.length - 1; pos >= 0; pos--)
			{
				sb.append(digitSymbols[mantissa[pos]]);
				if (this.mantissa.length - pos == decimalPos && pos > 0)
				{
					sb.append('.');
				}
			}

			for (int pos = mantissa.length; pos < decimalPos; pos++)
				sb.append(zero);
		}
		
		return sb.toString();
	}
	
	public long toLong()
	{
		long res = 0;
		for (int i = mantissa.length - 1; i >= 0; i--)
		{
			res = res * 10 + mantissa[i];
		}
		if (power < 0)
		{
			throw new IllegalArgumentException("Not a valid long: " + toString());
		}
		for (int i = 0; i < power; i++)
		{
			res *= 10;
		}
		
		if (sign)
		{
			return res;
		}
		else
		{
			return -res;
		}
	}
	
	private static BigInteger[] bigIntegerDigits = null;
	public BigInteger toBigInteger()
	{
		if (this.isZero()) return BigInteger.ZERO;
		BigInteger res = BigInteger.ONE;
		
		if (bigIntegerDigits == null)
		{
			BigInteger[] bid = new BigInteger[10];
			bid[0] = BigInteger.ZERO;
			for (int i = 1; i < 10; i++)
				bid[i] = bid[i - 1].add(BigInteger.ONE);
			
			bigIntegerDigits = bid;
		}
		
		for (int i = mantissa.length - 1; i >= 0; i--)
		{
			res = res.multiply(BigInteger.TEN).add(bigIntegerDigits[mantissa[i]]);
		}
		if (power < 0)
		{
			throw new IllegalArgumentException("Not a valid long: " + toString());
		}
		for (int i = 0; i < power; i++)
		{
			res = res.multiply(BigInteger.TEN);
		}
		
		if (sign)
		{
			return res;
		}
		else
		{
			return res.negate();
		}
	}
	
	public double toDouble()
	{
		double res = 0;
		for (int i = mantissa.length - 1; i >= 0; i--)
		{
			res = res * 10 + mantissa[i];
		}
		if (power > 0)
		{
			res *= Math.pow(10, power);
		}
		else if (power < 0)
		{
			res = res / Math.pow(10, -power); // it feels like this hack produces a better rounding-off
		}
		if (sign)
		{
			return res;
		}
		else
		{
			return -res;
		}
	}
	
//==== Properties ====		
	public boolean isZero()
	{
		return mantissa.length == 0;
	}
	
	public boolean isPositive()
	{
		return sign || mantissa.length == 0;
	}
	
	public boolean isInteger()
	{
		return this.power >= 0;
	}
	
	public int getPrecision()
	{
		return this.mantissa.length;
	}
	
	public int getEPower()
	{
		return this.ePower;
	}
	
	/** Returns the power from the P format, (integer mantissa) * 10^(power) */
	public int getPower()
	{
		return this.power;
	}
	
	public String printMantissa(int mindigits, int radix)
	{
		char[] chars;
		if (radix == 10)
		{
			chars = this.mantissa;
		}
		else
		{
			chars = convertRadix(this.mantissa, 10, 16);
		}
		if (chars.length < mindigits)
		{
			chars = CharMath.copyArray(chars, mindigits);
		}
		return (this.sign? "+" : "-") + digitsToString(chars);
	}
	
	public char[] getMantissa()
	{
		return CharMath.copyArray(mantissa, mantissa.length);
	}
	//==== Arithmetics ====
	
	public Decimal minus()
	{
		if (isZero()) return this;
		
		Decimal res = new Decimal(this);
		res.sign = !res.sign;
		return res;
	}
	
	/**
	 * round() trims the mantissa to the given number of digits with proper rounding,
	 * ignoring the sign: round(13.45) to 3 digits -> 13.5 and round(-13.45) -> -13.5.
	 * This is different from the standard Math.round
	 */
	public Decimal round(int maxdigits)
	{
		if (this.mantissa.length <= maxdigits) return new Decimal(this);
		
		Decimal res = new Decimal();
		res.sign = this.sign;
		res.power = this.power + mantissa.length - maxdigits;
		res.mantissa = new char[maxdigits];
		System.arraycopy(this.mantissa, mantissa.length - maxdigits, res.mantissa, 0, maxdigits);
		res.ePower = res.power + res.mantissa.length - 1;

		if (this.mantissa[mantissa.length - maxdigits - 1] >= 5) // remember that mantissa is always 10-based
		{
			res.mantissa = CharMath.arrAddSmall(res.mantissa, 1, 10);
			res.trimMantissa();
			/*
			// compact now
			int tail = 0;
			while (tail < mantissa.length && res.mantissa[tail] == 0) tail++;
			if (tail > 0)
			{
				char[] res2 = new char[maxdigits - tail];
				System.arraycopy(res.mantissa, tail, res2, 0, res2.length);
				res.mantissa = res2;
				res.power += tail;				
			}
			*/
		}
		return res;	
	}
	
	
	public Decimal floor()
	{
		return floor(this.mantissaLength + this.power);
	}
	/**
	 * floor() trims the mantissa to the given number of digits without rounding,
	 * ignoring the sign: round(13.45) to 3 digits -> 13.4 and round(-13.4) -> -13.4.
	 * This is faster than round() if you don't care about the last digit much
	 */
	public Decimal floor(int maxdigits)
	{
		if (maxdigits <= 0) return Decimal.ZERO;
		if (this.mantissa.length <= maxdigits) return new Decimal(this);
		
		Decimal res = new Decimal();
		res.sign = this.sign;
		res.power = this.power + mantissa.length - maxdigits;
		res.mantissa = new char[maxdigits];
		System.arraycopy(this.mantissa, mantissa.length - maxdigits, res.mantissa, 0, maxdigits);
		res.ePower = res.power + res.mantissa.length - 1;
		if (res.mantissa.length > 0 && res.mantissa[0] == 0)
		{
			res.trimMantissa();
		}
		return res;	
	}	
	public Decimal multiplySmall(long val)
	{
		if (val == 0) return ZERO;
		if (val < 0) 
		{
			Decimal r1 = multiplySmall(-val);
			r1.sign = !r1.sign;
			return r1;
		}
		
		char[] multRes = CharMath.arrMultiplySmall(this.mantissa, val, 10);

		int resPower = 0;
		while (resPower < multRes.length && multRes[resPower] == 0) resPower++;

		Decimal res = new Decimal();
		res.mantissa = new char[multRes.length - resPower];		
		System.arraycopy(multRes, resPower, res.mantissa, 0, res.mantissa.length); 
		res.power = resPower + this.power;
		res.ePower = res.power + res.mantissa.length - 1;
		res.sign = this.sign;
		
		return res;
	}
	
	private void printLongArr(String msg, char[] arr)
	{
		System.out.println(msg + " " + digitsToLong(arr, 10));
	}
	public Decimal multiply(Decimal val)
	{
		int chunkSize = 8;
		Decimal res = new Decimal();
		char[] valMantissa = val.mantissa;
		//System.out.println("mult this=" + digitsToLong(this.mantissa, 10) + ", val=" + digitsToLong(val.mantissa, 10));
		for (int offset = 0; offset < valMantissa.length; offset += chunkSize)
		{
			long chunk = digitsToLong(valMantissa, offset, chunkSize, 10);
			char[] multChunk = CharMath.arrMultiplySmall(this.mantissa, chunk, 10);
			//System.out.println("offset=" + offset + ", chunk=" + chunk);
			//printLongArr("chunk1=",  multChunk);
			if (offset > 0)
			{
				char[] mc2 = new char[multChunk.length + offset];
				System.arraycopy(multChunk, 0, mc2, offset, multChunk.length);
				multChunk = mc2;
				//printLongArr("chunk2=",  multChunk);
			}
			res.mantissa = CharMath.arrAdd(res.mantissa, multChunk, 10);
			//printLongArr("new mantissa=",  res.mantissa);
		}
		res.power = this.power + val.power;
		res.sign = !(this.sign ^ val.sign);
		res.ePower = res.power + res.mantissa.length - 1;
		res.trimMantissa();
		
		return res;
	}
	
	/**
	 * a simple holding bag to return division results from divide()
	 * @author Vythe
	 *
	 */
	public static class Division
	{
		public Decimal quotient;
		public Decimal remainder;
		
		public Division() {}
		public Division(Decimal quotient, Decimal remainder)
		{
			this.quotient = quotient == null? ZERO : quotient;
			this.remainder = remainder == null? ZERO : remainder;
		}
		
		@Override
		public String toString()
		{
			return quotient + " rem " + remainder;
		}
	}

	/**
	 * @deprecated Not worth it and not tested yet
	 */
	@Deprecated
	public Division divideWithCache(Decimal divisor, int extraDigits)
	{
		if (this.isZero()) 
		{
			return new Division(ZERO, ZERO);
		}
		if (divisor.isZero())
		{
			throw new ArithmeticException("Division by zero");
		}
		
		// division is separated into two stages: 
		// 2) convert this into base * 10^smth, where base is integer
		// 1) divide two integers 
		// 2) determine the ePower of the result and convert it into p-power in the end
		
		// 1. We know how many digits we need
		int resDigits = this.ePower - divisor.ePower + 1 + extraDigits;
		int quotientEPower = this.ePower - divisor.ePower;// this is the true EPower of the quotient
		
		// 2. to get that many digits we need either this
		//int baseLength = this.mantissa.length + divisor.mantissa.length + extraDigits;
		int baseLength = resDigits + divisor.mantissa.length;
		int baseDivisorLength = divisor.mantissa.length;
		
		CharMathCached cache = new CharMathCached(baseLength, 10);
		char[] base;
		int baseOffset = baseLength - this.mantissa.length;
		if (baseLength == this.mantissa.length)
		{
			baseOffset = 0;
			base = cache.copyArray(this.mantissa, this.mantissa.length, baseLength);
		}
		else if (baseOffset > 0)
		{
			base = cache.prependArray(this.mantissa, this.mantissa.length, (char)0, baseOffset);
		}
		else // baseOffset < 0
		{
			base = cache.copyArray(this.mantissa, this.mantissa.length, baseLength - baseOffset);
			baseLength = baseLength - baseOffset;
		}

		char[] baseDivisor;
		if (baseDivisorLength == divisor.mantissa.length)
		{
			baseDivisor = divisor.mantissa; //CharMath.copyArray(divisor.mantissa, baseDivisorLength);
		}
		else
		{
			baseDivisor = CharMath.prependArray(divisor.mantissa, (char)0, baseDivisorLength - divisor.mantissa.length);
			baseDivisorLength = baseDivisorLength - divisor.mantissa.length;
		}
		
		char[] resMantissa;
		if (resDigits < 0)
		{
			resMantissa = new char[0];
		}
		else
		{
			
			resMantissa = new char[resDigits];
			int divOffset = baseLength - baseDivisorLength;
			for (int i = resDigits - 1; i >= 0; i--, divOffset--)
			{
				//char[] divMantissa = CharMath.prependArray(baseDivisor, (char)0, divOffset);
				char[] divMantissa = cache.prependArray(baseDivisor, baseDivisorLength, (char)0, divOffset);
				resMantissa[i] = 0;
				int cmp;
				//while ((cmp = CharMath.compareArrays(base, divMantissa, 0)) >= 0)
				while ((cmp = cache.compareArrays0(base, baseLength, divMantissa, baseDivisorLength + divOffset)) >= 0)
				{
					try
					{
						char[] base1 = cache.arrSubtract(base, baseLength, divMantissa, baseDivisorLength + divOffset, 10);
						cache.stash(base);
						base = base1;
					}
					catch (CharMath.SubtractFailedException x)
					{// this shouldn't happen
						throw new ArithmeticException("CharMath.arrSubtract failed");
						//break; 
					}
					resMantissa[i]++;		
				}
				cache.stash(divMantissa);
			}
		}
		//Decimal quotient = new Decimal(resMantissa, this.ePower - divisor.ePower + trailingZeros - resDigits + 1, !(this.sign ^ divisor.sign));
		Decimal quotient = new Decimal(resMantissa, quotientEPower - resMantissa.length + 1, !(this.sign ^ divisor.sign));
		Decimal remainder = new Decimal(base, baseOffset > 0? this.power - baseOffset : this.power, this.sign);
		//Decimal remainder = new Decimal(base, quotientEPower - resMantissa.length + 1, this.sign);
		quotient.trimMantissa();
		remainder.trimMantissa();
		Division res = new Division(quotient, remainder);
		
		return res;
	}

	public Division divide (Decimal divisor) 
	{
		return divide(divisor, 0);
	}
	/** extraDigits == 0 means the returned quotient will be integer (power >= 0)
	 * extraDigits > 0 means the returned quotient will have "extgraDigits" decimal places (power == precision - extraDigits)
	 * extraDigits < 0 means the returned quotient will be rounded off, with extraDigits zeroes at the end (power == (-extraDigits))
	*/
	public Division divide(Decimal divisor, int extraDigits)
	{
		if (this.isZero()) 
		{
			return new Division(ZERO, ZERO);
		}
		if (divisor.isZero())
		{
			throw new ArithmeticException("Division by zero");
		}
		
		// division is separated into two stages: 
		// 2) convert this into base * 10^smth, where base is integer
		// 1) divide two integers 
		// 2) determine the ePower of the result and convert it into p-power in the end
		
		// 1. We know how many digits we need
		int resDigits = this.ePower - divisor.ePower + 1 + extraDigits;
		
		// 2. to get that many digits we need either this
		//int baseLength = this.mantissa.length + divisor.mantissa.length + extraDigits;
		int baseLength = resDigits + divisor.mantissa.length;
		int baseDivisorLength = divisor.mantissa.length;
		
		int quotientEPower = this.ePower - divisor.ePower;// this is the true EPower of the quotient
		char[] base;
		int baseOffset = baseLength - this.mantissa.length;
		if (baseLength == this.mantissa.length)
		{
			baseOffset = 0;
			base = CharMath.copyArray(this.mantissa, baseLength);
		}
		else if (baseOffset > 0)
		{
		base = CharMath.prependArray(this.mantissa, (char)0, baseOffset);
		}
		else // baseOffset < 0
		{
			base = CharMath.copyArray(this.mantissa, baseLength - baseOffset);
		}

		char[] baseDivisor;
		if (baseDivisorLength == divisor.mantissa.length)
		{
			baseDivisor = divisor.mantissa; //CharMath.copyArray(divisor.mantissa, baseDivisorLength);
		}
		else
		{
			baseDivisor = CharMath.prependArray(divisor.mantissa, (char)0, baseDivisorLength - divisor.mantissa.length);
		}
		
		char[] resMantissa;
		if (resDigits < 0)
		{
			resMantissa = new char[0];
		}
		else
		{
			resMantissa = new char[resDigits];
			int divOffset = base.length - baseDivisor.length;
			for (int i = resDigits - 1; i >= 0; i--, divOffset--)
			{
				//char[] divMantissa = CharMath.prependArray(baseDivisor, (char)0, i);
				char[] divMantissa = CharMath.prependArray(baseDivisor, (char)0, divOffset);
				resMantissa[i] = 0;
				int cmp;
				//while ((cmp = CharMath.compareArrays(base, divMantissa, 0)) >= 0)
				while ((cmp = CharMath.compareArrays0(base, divMantissa)) >= 0)
				{
					try
					{
						base = CharMath.arrSubtract(base, divMantissa, 10);
					}
					catch (CharMath.SubtractFailedException x)
					{// this shouldn't happen
						throw new ArithmeticException("CharMath.arrSubtract failed");
						//break; 
					}
					resMantissa[i]++;		
				}
			}
		}
		//Decimal quotient = new Decimal(resMantissa, this.ePower - divisor.ePower + trailingZeros - resDigits + 1, !(this.sign ^ divisor.sign));
		Decimal quotient = new Decimal(resMantissa, quotientEPower - resMantissa.length + 1, !(this.sign ^ divisor.sign));
		Decimal remainder = new Decimal(base, baseOffset > 0? this.power - baseOffset : this.power, this.sign);
		//Decimal remainder = new Decimal(base, quotientEPower - resMantissa.length + 1, this.sign);
		quotient.trimMantissa();
		remainder.trimMantissa();
		Division res = new Division(quotient, remainder);
		
		return res;
	}
	
	public Division divideOld(Decimal divisor, int extraDigits)
	{
		if (this.isZero()) 
		{
			return new Division(ZERO, ZERO);
		}
		if (divisor.isZero())
		{
			throw new ArithmeticException("Division by zero");
		}
		
		// division is separated into two stages: 
		// 2) convert this into base * 10^smth, where base is integer
		// 1) divide two integers 
		// 2) determine the ePower of the result and convert it into p-power in the end
		
		int reduction = (this.power <= 0 || divisor.power <= 0)? 0
			: this.power > divisor.power? divisor.power
			: this.power
		;
				
		int baseLength = this.mantissa.length;
		if (this.power > 0)
		{
			baseLength += this.power - reduction;
		}
		int baseDivisorLength = divisor.mantissa.length;
		if (divisor.power > 0)
		{
			baseDivisorLength += divisor.power - reduction;
		}
		
		baseLength += extraDigits;
		
		int quotientEPower; // this is the true EPower of the quotient
		char[] base;
		if (baseLength == this.mantissa.length)
		{
			base = CharMath.copyArray(this.mantissa, baseLength);
			quotientEPower = this.ePower - divisor.ePower;
		}
		else //if (baseLength > this.mantissa.length)
		{
			base = CharMath.prependArray(this.mantissa, (char)0, baseLength - this.mantissa.length);
			// we multiplied the base by 10^extra, so we reduce the ePower by the same extra
			quotientEPower = this.ePower - divisor.ePower; // - baseLength + this.mantissa.length; 
		}
		char[] baseDivisor;
		if (baseDivisorLength == divisor.mantissa.length)
		{
			baseDivisor = CharMath.copyArray(divisor.mantissa, baseDivisorLength);
		}
		else
		{
			baseDivisor = CharMath.prependArray(divisor.mantissa, (char)0, baseDivisorLength - divisor.mantissa.length);
		}
		//int resDigits = base.length - baseDivisor.length + 1; //baseLength - divisor.mantissa.length - divisor.power + 1;
		int resDigits = this.ePower - divisor.ePower + 1 + extraDigits;
		
		// preflight checks complete, now start dividing
		
		//char[] base = CharMath.prependArray(this.mantissa, (char)0, extraDigits);
		//int resDigits = this.ePower - div.ePower + 1 + trailingZeros;
		/*
		if (base.length < divisor.mantissa.length)
		{
			//base = CharMath.copyArray(base, divisor.mantissa.length);
			resDigits += divisor.mantissa.length - base.length;
			base = CharMath.prependArray(this.mantissa, (char)0, divisor.mantissa.length - base.length);
		}*/
		//char[] divMantissa = CharMath.prependArray(divisor.mantissa, (char)0, extraDigits);
		
		char[] resMantissa;
		if (resDigits < 0)
		{
			resMantissa = new char[0];
		}
		else
		{
			resMantissa = new char[resDigits];
			for (int i = resDigits - 1; i >= 0; i--)
			{
				char[] divMantissa = CharMath.prependArray(baseDivisor, (char)0, i);
				resMantissa[i] = 0;
				int cmp;
				while ((cmp = CharMath.compareArrays(base, divMantissa, 0)) >= 0)
				{
					try
					{
					base = CharMath.arrSubtract(base, divMantissa, 10);
					}
					catch (CharMath.SubtractFailedException x)
					{
						break; // this shouldn't happen
					}
					resMantissa[i]++;		
				}
			}
		}
		//Decimal quotient = new Decimal(resMantissa, this.ePower - divisor.ePower + trailingZeros - resDigits + 1, !(this.sign ^ divisor.sign));
		Decimal quotient = new Decimal(resMantissa, quotientEPower - resMantissa.length + 1, !(this.sign ^ divisor.sign));
		Decimal remainder = new Decimal(base, quotientEPower - resMantissa.length + 1, this.sign);
		quotient.trimMantissa();
		remainder.trimMantissa();
		Division res = new Division(quotient, remainder);
		
		return res;
	}
	
	/**
	 * A shortcut to return this+1, ignoring the sign
	 * @return
	 */
	public Decimal addOne()
	{
		char[] chars = new char[this.mantissa.length + this.power];
		System.arraycopy(this.mantissa, 0, chars, this.power, this.mantissa.length);
		char[] resChars = CharMath.arrAddOne(chars, 10);
		Decimal res = new Decimal(resChars, 0, this.sign);
		if (res.mantissa[0] == 0)
		{
			res.trimMantissa();
		}
		return res;			
	}
	
	/**
	 * This method uses the full add() when you substract a too large value,
	 * i.e. sign(this) != sign(val) and abs(this) < abs(val).
	 */
	public Decimal addSmall(long val)
	{
		boolean useSubtract = false;
		if (val == 0) 
		{
			return this;
		}
		else if (!this.sign)
		{
			Decimal d1 = this.minus();
			Decimal r1 = d1.addSmall(-val);
			r1.sign = !r1.sign;
			
			return r1;
		}
		else if  (val < 0)
		{
			val = -val;
			useSubtract = true;
/*
			if (this.compareTo(-val) > 0)
			{
			val = -val;
			useSubtract = true;
			}
			else
			{
				throw new IllegalArgumentException("full subtract not implemented yet, cannot do " + this.toStringE() + " - " + (-val));
			}
*/			
		}
		
		char[] chars = new char[this.mantissa.length + this.power];
		System.arraycopy(this.mantissa, 0, chars, this.power, this.mantissa.length);
		char[] resChars;
		boolean resPositive;
		
		if (useSubtract)
		{
			try
			{
				resChars = CharMath.arrSubtractSmall(chars, val, 10);
				resPositive = true;
			}
			catch (CharMath.SubtractFailedException x)
			{
				char[] arrVal = digitsFromLong(val, 10);
				try
				{
					resChars = CharMath.arrSubtract(arrVal, chars, 10);
				}
				catch (CharMath.SubtractFailedException x2)
				{
					throw new IllegalArgumentException("Failed to subtract " + digitsToString(chars) + " and " + val);
				}
				resPositive = false;				
			}
		}
		else
		{
			resChars = CharMath.arrAddSmall(chars, val, 10);
			resPositive = true;
		}
		
		/*
		int resPower = 0;
		while (resPower < resChars.length && resChars[resPower] == 0) 
			resPower++;
		int tail = 0;
		while (tail < resChars.length && resChars[chars.length - tail - 1] == 0) 
			tail++;
		
		Decimal res = new Decimal();
		res.mantissa = new char[resChars.length - resPower - tail];
		System.arraycopy(resChars, resPower, res.mantissa, 0, res.mantissa.length);
		res.power = resPower;
		res.sign = resPositive;
		*/
		Decimal res = new Decimal(resChars, 0, resPositive);
		res.trimMantissa();
		return res;	
	}
	
	public Decimal add(Decimal val)
	{
		boolean useSubtract = false;
		if (val.isZero()) 
		{
			return this;
		}
		else if (this.isZero())
		{
			return val;
		}
		else if (!this.sign)
		{
			Decimal d1 = this.minus();
			Decimal r1 = d1.add(val.minus());
			r1.sign = !r1.sign;
			
			return r1;
		}
		else if  (!val.isPositive())
		{
			val = val.minus();
			useSubtract = true;
		}
		
		// align powers
		int thisPOffset = this.power > val.power? this.power - val.power : 0;
		int valPOffset =  val.power > this.power? val.power - this.power : 0;
		
		char[] chars = new char[this.mantissa.length +  thisPOffset];
		System.arraycopy(this.mantissa, 0, chars, thisPOffset, this.mantissa.length);
		
		char[] valChars = new char[val.mantissa.length + valPOffset];
		System.arraycopy(val.mantissa, 0, valChars, valPOffset, val.mantissa.length);
		
		char[] resChars;
		boolean resPositive;
		
		if (useSubtract)
		{
			boolean useReverse = chars.length < valChars.length
					|| (chars.length == valChars.length &&  CharMath.compareArrays(chars, valChars, 0) < 0)
			;
			try
			{
				if (!useReverse)
				{
					resChars = CharMath.arrSubtract(chars, valChars, 10);
					resPositive = true;
				}
				else
				{
					resChars = CharMath.arrSubtract(valChars, chars, 10);
					resPositive = false;				
				}
			}
			catch (CharMath.SubtractFailedException x2)
			{
				throw new IllegalArgumentException("Failed to subtract " + digitsToString(chars) + " and " + digitsToString(valChars));
			}
				/*
			try
			{
				resChars = CharMath.arrSubtract(chars, valChars, 10);
				resPositive = true;
			}
			catch (CharMath.SubtractFailedException x)
			{
				try
				{
					resChars = CharMath.arrSubtract(valChars, chars, 10);
				}
				catch (CharMath.SubtractFailedException x2)
				{
					throw new IllegalArgumentException("Failed to subtract " + digitsToString(chars) + " and " + digitsToString(valChars));
				}
				resPositive = false;				
			}
			*/
		}
		else
		{
			resChars = CharMath.arrAdd(chars, valChars, 10);
			resPositive = true;
		}
		Decimal res = new Decimal(resChars, this.power - thisPOffset, resPositive);
		res.trimMantissa();
		/*
		int resPower = 0;
		while (resPower < resChars.length && resChars[resPower] == 0) 
			resPower++;
		int tail = 0;
		while (tail < resChars.length && resChars[chars.length - tail - 1] == 0) 
			tail++;
		
		Decimal res = new Decimal();
		res.mantissa = new char[resChars.length - resPower - tail];
		System.arraycopy(resChars, resPower, res.mantissa, 0, res.mantissa.length);
		res.power = resPower + this.power - thisPOffset;
		res.sign = resPositive;
		*/
		return res;	
	}
	
//==== private methods ====
	
	private void buildFromLong(long val)
	{
		// long is 64 bits, 8 bytes, 16 F's, so 32 digits should be enough
		char[] b1 = new char[32];
		long remainder = val;
		
		int len = 0;
		while (remainder != 0)
		{
			b1[len++] = (char)(remainder % 10);
			remainder = remainder / 10;
		}
		// compact for power
		power = 0;
		while (power < len && b1[power] == 0)
		{
			power++;
		}
		mantissa = new char[len - power];
		for (int i = 0; i < len - power; i++)
		{
			//mantissa[i] = b1[len - i - 1];
			mantissa[i] = b1[power + i];
		}
		ePower = power + mantissa.length - 1;
		this.mantissaLength = mantissa.length;
	}
		
	private void buildFromDouble(double val, int maxprec)
	{
		double lgBase = Math.log10(val); // val will be > 0 always
		int pow = (int)Math.floor(lgBase); // the true exponent of our value
		//double base = val / Math.pow(10, pow);
		double base = Math.pow(10, lgBase - pow); // original mantissa in the scientific format 3.12345
		char[] b1 = new char[maxprec];
		for (int i = 0; i < maxprec - 1; i++)
		{
			char nextDigit = 0;
			while (base > 1)
			{
				base -= 1;
				nextDigit++;
			}
			b1[i] = nextDigit;
			base = base * 10;
		}
		b1[maxprec - 1] = (char)Math.round(base);
		//  handle the rounding-up
		for (int i = maxprec - 1; i > 0; i--)
		{
			if (b1[i] > 9)
			{
				b1[i] -= 10;
				b1[i - 1]++;
			}
		}
		if (b1[0] > 9)
		{
			for (int i = maxprec - 1; i > 1; i--)
			{
				b1[i] = b1[i - 1];
			}
			b1[1] = (char) (b1[0] - 10);
			b1[0] = 1;
		}
		// end of rounding-up
		//System.out.println("pow=" + (pow - maxprec + 1) + ", res=" + res);
		// compact now
		int len = b1.length - 1; // - 1;
		while (len >= 0 && b1[len] == 0) len--;
		// end of compacting	
		/*
		if (len == b1.length - 1)
		{
			this.mantissa = b1;
		}
		else
		{
			this.mantissa = Arrays.copyOf(b1, len + 1);
		}
		*/
		len++; // it was not a length, but an index
		this.mantissa = new char[len];
		this.mantissaLength = mantissa.length;
		for (int i = 0; i <len; i++)
		{
			this.mantissa[i] = b1[len - i - 1];
		}
		this.power = pow - mantissa.length + 1;
		this.ePower = pow;
	}
	/// static methods
	
	/** Takes an array of digits in math order and returns a print string */
	public static String digitsToString(char[] digits)
	{
		StringBuilder sb = new StringBuilder();
		if (digits != null && digits.length > 0)
		{
			for (int i = digits.length - 1; i >= 0; i--)
			{
				sb.append(digitSymbols[digits[i]]);
			}
		}
		return sb.toString();
	}
	
	public static String digitsToString(char[] digits, int length)
	{
		StringBuilder sb = new StringBuilder();
		if (digits != null && length > 0)
		{
			for (int i = length - 1; i >= 0; i--)
			{
				sb.append(digitSymbols[digits[i]]);
			}
		}
		return sb.toString();
	}
	
	/** 
	 * Takes a print string and returns the array of digits in the "math" order: "123456" -> [6, 5, 4, 3, 2, 1]
	 */
	public static char[] digitsFromString(String val)
	{
		char[] b1 = val.toCharArray();
		char[] b2 = new char[b1.length];
		for (int i = 0; i < b1.length; i++)
		{
			for (int j = 0; j < digitSymbols.length; j++)
			{
				if (b1[i] == digitSymbols[j])
				{
					b2[b1.length - i - 1] = (char)j;
					break;
				}
			}
		}
		return b2;
	}
	
	/** converts chars (printable symbols) into digits in the array, keeping its order of elements */
	public static char[] digitsFromChars(char[] chars)
	{
		char[] b2 = new char[chars.length];
		for (int i = 0; i < chars.length; i++)
		{
			for (int j = 0; j < digitSymbols.length; j++)
			{
				if (chars[i] == digitSymbols[j])
				{
					//b2[val.length - i - 1] = (char)j;
					b2[i] = (char)j;
					break;
				}
			}
		}
		return b2;
	}
	
	/** converts a printable char to a digit */
	public static char digitFromChar(char c)
	{
		for (int j = 0; j < digitSymbols.length; j++)
		{
			if (c == digitSymbols[j])
			{
				return (char)j;
			}
		}
		throw new IllegalArgumentException("Unrecognized char: " + c);
	}

	/** converts a digit into a printable char */
	public static char digitToChar(char d)
	{
		if (d >= 0 && d < digitSymbols.length) return digitSymbols[d];
		throw new IllegalArgumentException("Unrecognized digit: " + (int)d);
	}
	
	/** Just this one, digits are in the print order, not math.
	 * It is used for parsing print strings */
	private static long digitsPrintToLong(char[] digits, int radix)
	{
		long res = 0;
		for (int i = 0; i < digits.length; i++)
		{
			res = res * radix + digits[i];
		}
		return res;
	}	
	
	private static long digitsToLong(char[] digits, int radix)
	{
		long res = 0;
		for (int i = digits.length - 1; i >= 0; i--)
		{
			res = res * radix + digits[i];
		}
		return res;
	}	
	
	/** 
	 * converts part of the char array to a long (to help with multiplication
	 */
	private static long digitsToLong(char[] digits, int offset, int len, int radix)
	{
		long res = 0;
		if (offset + len > digits.length)
		{
			len = digits.length - offset;
		}
		for (int i = offset + len - 1; i >= offset; i--)
		{
			res = res * radix + digits[i];
		}
		return res;
	}	

	/** parses a long value into an array of digits in the math order */
	public static char[] digitsFromLong(long val, int radix)
	{
		char[] b1 = new char[32];
		long remainder = val;
		
		int len = 0;
		while (remainder != 0)
		{
			b1[len++] = (char)(remainder % radix);
			remainder = remainder / radix;
		}
		char[] digits = new char[len];
		for (int i = 0; i < len; i++)
		{
			//digits[i] = b1[len - i - 1]; 
			digits[i] = b1[i]; 
		}
		return digits;
	}
	
	/** 
	 * just for fun, convert from one radix to another 
	 * value[] is an array of digits based "fromRadix";
	 * return is an array pof digits based "toRadix"; 
	 * @param value
	 * @param fromRadix
	 * @param toRadix
	 * @return
	 */
	public static char[] convertRadix(char[] value, int fromRadix, int toRadix)
	{
		if (value == null || value.length == 0) return new char[0];
		
		char[] res = new char[0]; 
		for (int i = value.length - 1; i >= 0; i--)
		{
			res = CharMath.arrMultiplySmall(res, fromRadix, toRadix);
			res = CharMath.arrAddSmall(res, value[i], toRadix);
		}
		return res;		
	}		

	/**
	 * Takes mantissa and power as radix-based strings and converts them into a Decimal (mantissa * 10^power)
	 * Note that the "power10" value is 10-based, whatever radix is used to encode the mantissa and power10 strings.
	 * @return
	 */
	public static Decimal fromString2(String mantissa, String power10, int radix)
	{
		Decimal res = new Decimal();
		
		//res.mantissa = getDigits(mantissa);
		char[] mant = digitsFromString(mantissa);
		if (mant.length > 0 && mantissa.charAt(0) == '+')
		{
			res.sign = true;
			res.mantissa = Arrays.copyOfRange(mant, 0, mant.length - 1);
		}
		else if (mant.length > 0 && mantissa.charAt(0) == '-')
		{
			res.sign = false;
			res.mantissa = Arrays.copyOfRange(mant, 0, mant.length - 1);
		}
		else
		{
			res.sign = true;
			res.mantissa = mant;
		}
		if (radix != 10)
		{
			res.mantissa = Decimal.convertRadix(res.mantissa, radix, 10);
		}
		if (power10 != null && power10.length() != 0)
		{
			char[] pow = digitsFromString(power10);
			if (pow.length > 0 && power10.charAt(0) == '+')
			{
				res.power = (int)digitsToLong(Arrays.copyOfRange(pow, 0, pow.length - 1), radix);
			}
			else if (pow.length > 0 && power10.charAt(0) == '-')
			{
				res.power = -(int)digitsToLong(Arrays.copyOfRange(pow, 0, pow.length - 1), radix);
			}
			else
			{
				res.power = (int)digitsToLong(pow, 10);
			}
		}
		else
		{
			res.power = 0;
		}
		res.ePower = res.power + res.mantissa.length - 1;
		return res;
	}
	
	public static String longToString(long val, int mindigits, int radix)
	{
		String sign = val < 0? "-" : "+"; 
		char[] chars1;
		if (val == Long.MIN_VALUE)
		{
			Decimal d = new Decimal(val + "");
			chars1 = convertRadix(d.mantissa, 10, 16);
		}
		else
		{
			chars1 = digitsFromLong(val < 0? -val : val, radix);
		}
		char[] chars;
		if (chars1.length < mindigits)
		{
			chars = new char[mindigits];
			System.arraycopy(chars1, 0, chars, 0, chars1.length);
		}
		else
		{
			chars = chars1;
		}
		return sign + digitsToString(chars);
	}

	public static String unsignedToString(long val, int mindigits, int radix)
	{
		char[] chars1;
		if (val == Long.MIN_VALUE)
		{
			Decimal d = new Decimal(val + "");
			chars1 = convertRadix(d.mantissa, 10, 16);
		}
		else
		{
			chars1 = digitsFromLong(val < 0? -val : val, radix);
		}
		char[] chars;
		if (chars1.length < mindigits)
		{
			chars = new char[mindigits];
			System.arraycopy(chars1, 0, chars, 0, chars1.length);
		}
		else
		{
			chars = chars1;
		}
		return digitsToString(chars);
	}
	
	public static long unsignedFromString(String s, int radix)
	{
		char[] chars = digitsFromString(s);
		return digitsToLong(chars, radix);
	}
}
