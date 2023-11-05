package net.vbelev.utils;
/**
 * Helper methods for array arithmetics.
 * All arrays are in the "math" order; all values are assumed positive,
 * and the arrays represent positive values.
 * When subtraction turns negative, it will throw the SubtractFailedException.
 */
public class CharMath
{
	public static class SubtractFailedException extends Exception
	{
		private static final long serialVersionUID = -9174289750449147728L;
	}

	/**
	 * Adds one element to the array's start and sets it to val.
	 * In Decimal, this is equivalent to multiplying the mantissa array by 10.
	 */
public static char[] prependArray(char[] arr, char val)
{
	char[] res;
	if (arr == null || arr.length == 0) 
		res = new char[1];
	else
	{
		res = new char[arr.length + 1];
		System.arraycopy(arr, 0, res, 1, arr.length);
	}
	res[0] = val;
	return res;		
}

/**
 * Adds numDigits elements to the array's start and sets them all to val.
 * In Decimal, this is equivalent to multiplying the mantissa array by 10^numDigits.
 * If numDigits <= 0, truncates the array by numDigits elements, 
 * which is equivalent in Decimal to integer division by 10^abs(numDigits). 
 */
public static char[] prependArray(char[] arr, char val, int numDigits)
{
	if (numDigits < 0 && arr.length <= -numDigits)
	{
		return new char[0];
	}
	else if (numDigits <= 0)
	{
		return copyArray(arr, arr.length + numDigits);
	}
	
	char[] res;
	if (arr == null || arr.length == 0) 
		res = new char[numDigits];
	else
	{
		res = new char[arr.length + numDigits];
		System.arraycopy(arr, 0, res, numDigits, arr.length);
	}
	if (val != 0)
	{
		for (int i = 0; i < numDigits; i++)
			res[i] = val;
	}
	return res;		
}

public static char[] appendArray(char[] arr, char val)
{
	char[] res;
	if (arr == null || arr.length == 0) 
		res = new char[1];
	else
	{
		res = new char[arr.length + 1];
		System.arraycopy(arr, 0, res, 0, arr.length);
	}
	res[res.length - 1] = val;
	return res;		
}

public static char[] copyArray(char[] from, int newlen)
{
	char[] res = new char[newlen];
	if (from != null && from.length > 0)
	{
		System.arraycopy(from, 0, res, 0, newlen <= from.length? newlen : from.length );
	}
	return res;
}

public static char[] reverseArray(char[] from)
{
	if (from == null) return null;
	char[] res = new char[from.length];
	int j = from.length - 1;
	for (int i = 0; i < from.length; i++)
		res[j--] = from[i];
	
	return res;
}

/**
 * compares two mantissas with the given offset (offset is a.power - b.power);
 * returns 1 if a > b;
 * returns -1 if a < b; 
 * return 0 if a == b; 
 * 
 * Offset is the difference between decimal point positions, needed to compare mantissas of different length, 
 */
public static int compareArrays(char[] a, char[] b, int offset)
{
	//int bShift = b.length - a.length + offset; // 
	int startPos = a.length - 1; // > b.length? a.length - 1 : b.length - 1;
	int bShift = offset;
	
	//if (a.length - 1 > startPos) return 1;
	
	if (b.length - 1 < startPos + bShift) return 1;
	if (b.length - 1 > startPos + bShift) return -1;
	//if (a.length - 1 > startPos + bShift) return -1;
	
	int i = startPos;
	for (; i >= 0 && i >= -bShift; i--)
	{
		if (a[i] > b[i + bShift]) return 1;
		if (a[i] < b[i + bShift]) return -1;
	}
	if (i >= 0) return 1;
	// a = 1.2345 , offset 0, 
	// a mantissa 54321  e=0 
	// b = 1.23, 
	// b mantissa 321    e=0
	// bshift = -2
	if (bShift > 0) return -1;
	return 0;
}

/**
 * Similar to compareArrays with offset = 0
 */
public static int compareArrays0(char[] a, char[] b)
{
	//int bShift = b.length - a.length + offset; // 
	int startPos = a.length - 1; // > b.length? a.length - 1 : b.length - 1;
	
	//if (a.length - 1 > startPos) return 1;
	
	if (b.length - 1 < startPos) return 1;
	if (b.length - 1 > startPos) return -1;
	//if (a.length - 1 > startPos + bShift) return -1;
	
	int i = startPos;
	for (; i >= 0; i--)
	{
		if (a[i] > b[i]) return 1;
		if (a[i] < b[i]) return -1;
	}
	if (i >= 0) return 1;
	return 0;
}

public static char[] arrMultiplySmall(char[] val, long factor, int radix)
{
	if (val == null || val.length == 0 || factor == 0)
	{
		return new char[0]; // zero is zero
	}
	long overflow = 0;
	char[] res = new char[val.length];
	for (int i = 0; i < val.length; i++)
	{
		long c1 = val[i] * factor + overflow;
		res[i] = (char)(c1 % radix);
		overflow = c1 / radix;
	}
	while(overflow > 0)
	{
		res = CharMath.appendArray(res, (char)(overflow % radix));
		overflow = overflow / radix;
	}	
	
	return res;
}

public static char[] arrAddSmall(char[] val, long add, int radix)
{
	long overflow = add;
	char[] res;
	if (val.length == 0)
	{
		res = new char[0];
	}
	else
	{
		res = CharMath.copyArray(val, val.length);
		
	}
	//for (int i = val.size() - 1; i >= 0; i--)
	for (int i = 0; i < val.length; i++)
	{
		long c1 = val[i] + overflow;
		res[i] = (char)(c1 % radix);
		overflow = c1 / radix;
	}
	
	while(overflow > 0)
	{
		res = CharMath.copyArray(res, res.length + 1);
		res[res.length - 1] = (char)(overflow % radix);
		overflow = overflow / radix;
	}
	return res;
}

/** 
 * a shortcut to get val + 1
 */
public static char[] arrAddOne(char[] val, int radix)
{
	char[] res;
	if (val.length == 0)
	{
		res = new char[1];
	}
	else
	{
		res = CharMath.copyArray(val, val.length);
		
	}
	//for (int i = val.size() - 1; i >= 0; i--)
	int t = res.length - 1;
	res[0]++;
	for (int i = 0; i < t; i++)
	{		
		if (res[i] < radix) break;
		res[i] -= radix;
		res[i+1]++;
	}
	if (res[t] >= radix)
	{
		res = CharMath.copyArray(res, res.length + 1);
		res[t] -= radix;
		res[val.length] = 1;
	}
	return res;
}


/** 
 * both val and add are in the "math" order.
 */
public static char[] arrAdd(char[] val, char[] add, int radix)
{
	if (add == null || add.length == 0) return CharMath.copyArray(val, val.length);
	if (val == null || val.length == 0) return CharMath.copyArray(add, add.length);
	
	int overflow = 0; //add[0];
	int len = Math.max(val.length, add.length);
	char[] res = CharMath.copyArray(val, len); // the tail will be zeros
	//for (int i = val.size() - 1; i >= 0; i--)
	for (int i = 0; i < len; i++)
	{
		int c = i < add.length? add[i] : 0;
		int c1 = (i < res.length? res[i] : 0) + c + overflow;
		res[i] = (char)(c1 % radix);
		overflow = c1 / radix;
	}
	while(overflow > 0)
	{
		res = CharMath.copyArray(res, res.length + 1);
		res[res.length - 1] = (char)(overflow % radix);
		overflow = overflow / radix;
	}
	return res;
}

public static char[] arrSubtractSmall(char[] val, long sub, int radix) throws SubtractFailedException
{
	long overflow = sub;
	boolean needReverse = false;
	char[] dest = null;
	
	if (sub == 0)
	{
		return CharMath.copyArray(val, val.length);
	}
	else if (val.length == 0)
	{
		needReverse = true;
	}
	else
	{
		dest = new char[val.length];
		for (int i = 0; i < val.length; i++)
		{
			char overflowLastDigit = (char)(overflow % radix);
			if (overflowLastDigit > val[i])
			{
				dest[i] = (char)(val[i] - overflowLastDigit + radix);
				overflow += radix;
			}
			else
			{
				dest[i] = (char)(val[i] - overflowLastDigit);
			}
			overflow = overflow / radix;				
		}
		if  (overflow > 0)
		{
			needReverse = true;
		}
	}
	/*
	while(overflow > 0)
	{
		val.add((char)(overflow % radix));
		overflow = overflow / radix;
	}
	*/
	if (needReverse)
	{
		//throw new IllegalArgumentException("subtraction turned negative, not supproted yet");
		throw new SubtractFailedException();
	}
	int tail = dest.length - 1;
	while (tail >= 0 && dest[tail] == 0) tail--;
	if (tail < dest.length - 1)
	{
		dest = CharMath.copyArray(dest, tail + 1);
	}
	return dest; 
}

public static char[] arrSubtract(char[] val, char[] sub, int radix) throws SubtractFailedException
{
	boolean needReverse = false;
	char[] dest = null;
	
	if (sub.length == 0)
	{
		return CharMath.copyArray(val, val.length);
	}
	else if (val.length < sub.length)
	{
		needReverse = true;
	}
	else
	{
		dest = new char[val.length];
		System.arraycopy(val, 0, dest, 0, val.length);
		//dest = copyArray(val, val.length);
		int overflow = 0; //sub[0];
		for (int i = 0; i < val.length; i++)
		{
			/*
			overflow += i < sub.length? sub[i] : 0;
			char overflowLastDigit = (char)(overflow % radix);
			if (overflowLastDigit > val[i])
			{
				dest[i] = (char)(val[i] - overflowLastDigit + radix);
				overflow += radix;
			}
			else
			{
				dest[i] = (char)(val[i] - overflowLastDigit);
			}
			overflow = overflow / radix;				
			*/
			//overflow += i < sub.length? sub[i] : 0;
			if (i < sub.length) overflow += sub[i];
			else if (overflow == 0) break; // this does not speed things up at all
			
			int overflowNew = overflow >= radix? 1 : 0;
			char overflowLastDigit = (char)(overflow >= radix? overflow - radix : overflow);
			/* could not make it faster
			//int overflowNew = overflow / radix;
			//char overflowLastDigit = (char)(overflow % radix);
			//char overflowLastDigit = (char)(overflow - overflowNew * radix);
			//int overflowNew = (char)(overflow >= radix? 1 : 0);
			//char overflowLastDigit = (char)(overflow >= radix? overflow - radix : overflow);
			char overflowLastDigit = overflow;
			char overflowNew = 0;
			if (overflow > radix)
			{
				overflowLastDigit -= radix;
				overflowNew++;
			}
			*/
			if (overflowLastDigit > val[i])
			{
				dest[i] += radix - overflowLastDigit;
				overflowNew++;
			}
			else if (overflowLastDigit != 0)
			{
				dest[i] -= overflowLastDigit;
			}
			//overflow = overflow / radix;
			overflow = overflowNew;		
		}
		
		if  (overflow > 0)
		{
			needReverse = true;
		}
	}

	if (needReverse)
	{
		//throw new IllegalArgumentException("subtraction turned negative, not supproted yet");
		throw new SubtractFailedException();
	}
	int tail = dest.length - 1;
	while (tail >= 0 && dest[tail] == 0) tail--;
	if (tail < dest.length - 1)
	{
		dest = CharMath.copyArray(dest, tail + 1);
	}
	return dest; 
}

public static char[] arrayFromLong(long val)
{
	if (val < 0)
	{
		throw new ArithmeticException("Negative val");
	}
	// long is 64 bits, 8 bytes, 16 F's, so 32 digits should be enough
	char[] b1 = new char[32];
	long remainder = val;
	
	int len = 0;
	while (remainder != 0)
	{
		b1[len++] = (char)(remainder % 10);
		remainder = remainder / 10;
	}
	int power = 0;
	// compact for power - here, arrays will not be compacted
	//while (power < len && b1[power] == 0)
	//{
	//	power++;
	//}
	char[] res = new char[len - power];
	for (int i = 0; i < len - power; i++)
	{
		//mantissa[i] = b1[len - i - 1];
		res[i] = b1[power + i];
	}
	return res;
}
public static int[] charToInt(char[] arr)
{
	int[] res = new int[arr.length];
	for (int i = 0; i < arr.length; i++) res[i] = arr[i];
	return res;
}
}
