package net.vbelev.utils;

import java.util.ArrayList;
import java.util.Arrays;

public class DecimalOld
{
	/** this is the sign */
	private boolean isPositive;
	
	/** privately, mantissa is a very long decimal
	 * stored as chars (9,8, 9, 2, 0, 1 etc);
	 * the order is human-readable, 105469 -> [1, 0, 5, 4, 6, 9]
	 */
	private char[] mantissa;
	/** power is the number of zeroes after the mantissa:
	 * 6.06573e10 -> 60657300000 -> 606573p5
	 */
	private int power;

	/** store mantissa as hexadecimal chars as well, just in case: the same number as mantissa, only in hex */
	private char[] hex;
	/** a number of hex zeroes after the hex mantissa */
	private int hexPower;
	
	private static final char[] digits = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
	private static char[] EMPTY_CHAR_ARR = new char[0];
	public DecimalOld()
	{
		mantissa = EMPTY_CHAR_ARR;
		power = 0;
		hex = EMPTY_CHAR_ARR;
		hexPower = 0;
		isPositive = true;
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		//sb.append(mantissa);
		if (!isPositive)
		{
			sb.append('-');
		}
		for (int i = 0; i < mantissa.length; i++)
		{
			sb.append(digits[mantissa[i]]);
		}
		sb.append("p");
		sb.append(power);
		
		return sb.toString();
	}

	public String toHexString()
	{
		StringBuilder sb = new StringBuilder();
		if (!isPositive)
		{
			sb.append('-');
		}
		sb.append("0x");
		//sb.append(hex);
		for (int i = 0; i < hex.length; i++)
		{
			sb.append(digits[hex[i]]);
		}		
		sb.append("p");
		sb.append(hexPower);
		
		return sb.toString();
	}

	public DecimalOld(long val)
	{
		isPositive = (val >= 0);
		fillDecs(isPositive? val : -val);
		fillHex(isPositive? val: -val);		
	}
	

	private static char[] getDigits(long val, int radix)
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
			//mantissa[i] = digits[b1[len - i - 1]]; 
			digits[i] = b1[len - i - 1]; 
		}
		return digits;
	}
	
	public static char[] getDigits(String val)
	{
		char[] b1 = val.toCharArray();
		for (int i = 0; i < b1.length; i++)
		{
			for (int j = 0; j < digits.length; j++)
			{
				if (b1[i] == digits[j])
				{
					b1[i] = (char)j;
					break;
				}
			}
		}
		return b1;
	}
	
	private static long buildLong(char[] digits, int radix)
	{
		long res = 0;
		for (int i = 0; i < digits.length; i++)
		{
			res = res * radix + digits[i];
		}
		return res;
	}
	
	private void fillDecs(long val)
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
			//mantissa[i] = digits[b1[len - i - 1]]; 
			mantissa[i] = b1[len - i - 1]; 
		}		
	}
	
	private void fillHex(long val)
	{
		long remainder = val;
		char[] b2 = new char[16];
		
		int len = 0;
		while (remainder != 0)
		{
			b2[len++] = (char)(remainder & 0xfL);
			remainder = remainder >>> 4;
		}
		// compact for power
		hexPower = 0;
		while (hexPower < len && b2[hexPower] == 0)
		{
			hexPower++;
		}
		hex = new char[len - hexPower];
		
		for (int i = 0; i < len - hexPower; i++)
		{
			//hex[i] = digits[b2[len - i - 1]]; 
			hex[i] = b2[len - i - 1]; 
		}		
	}

	public DecimalOld(double val)
	{
		// An IEEE 754 float, as implemented by the Java language, has 32 bits. The first bit is the sign bit, 0 for positive and 1 for negative.
		// The next eight bits are the exponent and can hold a value from -125 to +127. 
		// The final 23 bits hold the mantissa (sometimes called the significand), which ranges from 0 to 33,554,431.
		// for double, boolean negative = (bits & 0x8000000000000000L) != 0; // 1 bit
		//long exponent = bits & 0x7ff0000000000000L; // 11 bits (power of 2)
		//long mantissa = bits & 0x000fffffffffffffL; // 42 bits

		isPositive = (val >= 0);
		if (val == 0)
		{
			mantissa = EMPTY_CHAR_ARR;
			power = 0;
			hex = EMPTY_CHAR_ARR;
			hexPower = 0;
		}
		else
		{
			//int maxprec = 26; // 8 for float, ~26 for double
			fillDecs(isPositive? val: -val, 26);
			fillHex(isPositive? val: -val, 26);
		}
	}

	public DecimalOld(double val, int numdigits)
	{
		if (numdigits < 1)
		{
			throw new IllegalArgumentException("Decimal numdigits must be 1 or greater");
		}
		isPositive = (val >= 0);
		if (val == 0)
		{
			mantissa = EMPTY_CHAR_ARR;
			power = 0;
			hex = EMPTY_CHAR_ARR;
			hexPower = 0;
		}
		else
		{
			//int maxprec = 26; // 8 for float, ~26 for double
			fillDecs(isPositive? val: -val, numdigits);
			fillHex(isPositive? val: -val, numdigits);
		}
	}
	
	private void fillDecs(double val, int maxprec)
	{
		double lgBase = Math.log10(val); // val will be > 0 always
		int pow = (int)Math.floor(lgBase); // the true exponent of our value
		//double base = val / Math.pow(10, pow);
		double base = Math.pow(10, lgBase - pow); // mantissa in the scientific format 3.12345
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
		int len = b1.length - 1;
		while (len >= 0 && b1[len] == 0) len--;
		// end of compacting	
		if (len == b1.length - 1)
		{
			this.mantissa = b1;
		}
		else
		{
			this.mantissa = Arrays.copyOf(b1, len + 1);
		}
		this.power = pow - mantissa.length + 1;		
	}

	private void fillHex(double val, int maxprec)
	{
		double lgBase = Math.log10(val) / Math.log10(16); // val will be > 0 always
		int pow = (int)Math.floor(lgBase); // the true exponent of our value
		//double base = val / Math.pow(10, pow);
		double base = Math.pow(16, lgBase - pow); // mantissa in the scientific format 3.12345
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
			base = base * 16;
		}
		b1[maxprec - 1] = (char)Math.round(base);
		//  handle the rounding-up
		for (int i = maxprec - 1; i > 0; i--)
		{
			if (b1[i] > 15)
			{
				b1[i] -= 16;
				b1[i - 1]++;
			}
		}
		if (b1[0] > 15)
		{
			for (int i = maxprec - 1; i > 1; i--)
			{
				b1[i] = b1[i - 1];
			}
			b1[1] = (char) (b1[0] - 16);
			b1[0] = 1;
		}
		// end of rounding-up
		//System.out.println("pow=" + (pow - maxprec + 1) + ", res=" + res);
		// compact now
		int len = b1.length - 1;
		while (len >= 0 && b1[len] == 0) len--;
		// end of compacting	
		if (len == b1.length - 1)
		{
			this.hex = b1;
		}
		else
		{
			this.hex = Arrays.copyOf(b1, len + 1);
		}
		this.hexPower = pow - hex.length + 1;		
	}

	public boolean isPositive()
	{
		return isPositive;
	}
	
	public String getMantissa()
	{
		StringBuilder sb = new StringBuilder();
		//sb.append(mantissa);
		for (int i = 0; i < mantissa.length; i++)
		{
			sb.append(digits[mantissa[i]]);
		}
		return sb.toString();
	}

	public String getMantissaHex()
	{
		StringBuilder sb = new StringBuilder();
		if (isPositive)
		{
			sb.append('+');
		}
		else
		{
			sb.append('-');
		}
		for (int i = 0; i < hex.length; i++)
		{
			sb.append(digits[hex[i]]);
		}		
		return sb.toString();
	}

	public String getMantissaHex(int numdigits)
	{
		StringBuilder sb = new StringBuilder();
		if (isPositive)
		{
			sb.append('+');
		}
		else
		{
			sb.append('-');
		}
		for (int i = hex.length; i < numdigits; i++)
		{
			sb.append(digits[0]);
		}		
		for (int i = 0; i < hex.length && i < numdigits; i++)
		{
			sb.append(digits[hex[i]]);
		}		
		return sb.toString();
	}
	
	public String getPowerHex(int numdigits)
	{
		StringBuilder sb = new StringBuilder();
		char[] powerDigits;
		if (hexPower == 0)
		{
			powerDigits = EMPTY_CHAR_ARR;
		}
		else if (hexPower > 0)
		{
			sb.append('+');
			powerDigits = getDigits(hexPower, 16);
		}
		else
		{
			sb.append('-');
			powerDigits = getDigits(-hexPower, 16);			
		}
		
		for (int i = powerDigits.length; i < numdigits; i++)
		{
			sb.append(digits[0]);
		}
		for (int i = 0; i < powerDigits.length && i < numdigits; i++)
		{
			sb.append(digits[powerDigits[i]]);
		}			
		return sb.toString();
	}
	
	public static DecimalOld fromHex(String mantissa, String hexPower)
	{
		DecimalOld res = new DecimalOld();
		
		//res.mantissa = getDigits(mantissa);
		char[] mant = getDigits(mantissa);
		if (mant.length > 0 && mant[0] == '+')
		{
			res.isPositive = true;
			res.hex = Arrays.copyOfRange(mant, 1, mant.length);
		}
		else if (mant.length > 0 && mant[0] == '-')
		{
			res.isPositive = false;
			res.hex = Arrays.copyOfRange(mant, 1, mant.length);
		}
		else
		{
			res.isPositive = true;
			res.hex = mant;
		}
		
		char[] pow = getDigits(hexPower);
		if (pow.length > 0 && pow[0] == '+')
		{
			res.hexPower = (int)buildLong(Arrays.copyOfRange(pow, 1, pow.length), 16);
		}
		else if (pow.length > 0 && pow[0] == '-')
		{
			res.hexPower = -(int)buildLong(Arrays.copyOfRange(pow, 1, pow.length), 16);
		}
		else
		{
			res.hexPower = (int)buildLong(pow, 16);
		}
		
		return res;
	}
	
	public static String digitsToString(char[] value)
	{
		StringBuilder sb = new StringBuilder();
		for (char c : value)
		{
			sb.append(digits[c]);
		}
		return sb.toString();
	}
	
	public static String digitsToString(ArrayList<Character> value)
	{
		StringBuilder sb = new StringBuilder();
		for (Character c : value)
		{
			sb.append(digits[c.charValue()]);
		}
		return sb.toString();
	}
	
	public static void multiplySmall(ArrayList<Character> val, int factor, int radix)
	{
		int overflow = 0;
		for (int i = val.size() - 1; i >= 0; i--)
		{
			int c1 = val.get(i) * factor + overflow;
			val.set(i, (char)(c1 % radix));
			overflow = c1 / radix;
		}
		while(overflow > 0)
		{
			val.add(0, (char)(overflow % radix));
			overflow = overflow / radix;
		}	
	}
	
	public static void addSmall(ArrayList<Character> val, int add, int radix)
	{
		int overflow = add;
		for (int i = val.size() - 1; i >= 0; i--)
		{
			int c1 = val.get(i) + overflow;
			val.set(i, (char)(c1 % radix));
			overflow = c1 / radix;
		}
		while(overflow > 0)
		{
			val.add(0, (char)(overflow % radix));
			overflow = overflow / radix;
		}	
	}
	
	public static char[] decToHex(char[] dec)
	{
		// this is all wrong
		ArrayList<Character> hex = new ArrayList<Character>();
		
		for (char d : dec)
		{
			multiplySmall(hex, 10, 16);
			addSmall(hex, d, 16);
		}
		char[] res = new char[hex.size()];
		int pos = hex.size() - 1;
		for (Character c : hex)
		{
			res[pos--] = c;
		}
		return res;		
	}	
	
	public static char[] hexToDec(char[] hex)
	{
		ArrayList<Character> dec = new ArrayList<Character>();
		
		int curr = 0;
		int decPos = 0;
		for (char c : hex)
		{
			curr += c;
			if (curr >= 10)
			{
				
			}
		}
		char[] res = new char[dec.size()];
		int pos = 0;
		for (Character c : dec)
		{
			res[pos++] = c;
		}
		return res;
		
	}
}
