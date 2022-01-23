package net.vbelev.utils;
/**
 * Helper methods for array arithmetics.
 * All arrays are in the "math" order; all values are assumed positive,
 * and the arrays represent positive values.
 * When subtraction turns negative, it will throw the SubtractFailedException.
 * 
 * The cached version is slower because of getClean() and only slightly faster with getClean() disabled.
 * Not worth it.
 */
public class CharMathCached
{
	public static class SubtractFailedException extends CharMath.SubtractFailedException
	{
		private static final long serialVersionUID = -9174289750449147728L;
	}

	public static final int BUFFER_INCREASE_STEP = 10;
	
	private char[][] arrayBuffer;
	private char[] zeros;
	
	private int arrayLength = 0;
	private int bufferPos = 0;
	
	public int cacheHit = 0;
	public int cacheMiss = 0;
	
	public CharMathCached()
	{
		arrayLength = 0;
		arrayBuffer = new char[0][];
		zeros = new char[0];
	}
	
	public CharMathCached(int arrayLength)
	{
		init(arrayLength, BUFFER_INCREASE_STEP);
	}
	
	public CharMathCached(int arrayLength, int initialSize)
	{
		init(arrayLength, initialSize);
	}
	
	private void init(int arrayLength, int initialSize)
	{
		this.arrayLength = arrayLength;
		arrayBuffer = new char[initialSize][arrayLength];
		bufferPos = initialSize;
		zeros = new char[arrayLength];
	}
	
	public int getLength() { return arrayLength; }
	
	public char[] get()
	{
		if (bufferPos > 0) 
		{
			cacheHit++;
			return arrayBuffer[--bufferPos];
		}
		cacheMiss++;
		return new char[arrayLength];
	}
	
	public char[] getClean()
	{
		char[] res = get();
		//for (int i = 0; i < arrayLength; i++) res[i] = 0;
		System.arraycopy(zeros, 0, res, 0, arrayLength);
		
		return res;
	}
	
	/**
	 * returns the last non-zero element's position + 1. As in, the length of the "real" array
	 */
	public int getLength(char[] arr)
	{
		for (int i = arrayLength - 1; i >= 0; i --)
			if (arr[i] != 0) return i + 1;
		
		return 0;
	}
	
	public void stash(char[] arr)
	{
		if (bufferPos >= arrayBuffer.length)
		{
			char[][] newBuff = new char[arrayBuffer.length + BUFFER_INCREASE_STEP][];
			System.arraycopy(arrayBuffer, 0, newBuff, 0, arrayBuffer.length);
			arrayBuffer = newBuff;			
		}
		arrayBuffer[bufferPos++] = arr;
	}
	
	/**
	 * Adds one element to the array's start and sets it to val.
	 * In Decimal, this is equivalent to multiplying the mantissa array by 10.
	 */
public char[] prependArray(char[] arr, int length, char val)
{
	char[] res;
	if (arr == null || length <= 0)
	{
		//res = new char[1];
		res = get();
	}
	else
	{
		//res = new char[arr.length + 1];
		res = get();
		System.arraycopy(arr, 0, res, 1, length);
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
public char[] prependArray(char[] arr, int length, char val, int numDigits)
{
	if (numDigits < 0 && length <= -numDigits)
	{
		//return new char[0];
		return get();
	}
	else if (numDigits <= 0)
	{
		return copyArray(arr, length, length + numDigits);
	}
	
	char[] res;
	if (arr == null || arr.length == 0) 
	{		
		res = new char[numDigits];
		res = get();
	}
	else
	{
		//res = new char[arr.length + numDigits];
		res = get();
		System.arraycopy(arr, 0, res, numDigits, length);
	}
	if (val != 0)
	{
		for (int i = 0; i < numDigits; i++)
			res[i] = val;
	}
	return res;		
}

public char[] appendArray(char[] arr, int length, char val)
{
	char[] res;
	if (arr == null || length <= 0)
	{
		//res = new char[1];
		res = get();
	}
	else
	{
		//res = new char[arr.length + 1];
		res = get();
		System.arraycopy(arr, 0, res, 0, length);
	}
	res[length - 1] = val;
	return res;		
}

public char[] copyArray(char[] from, int length, int newlength)
{
	//char[] res = new char[newlen];
	char[] res = get();
	if (from != null && length > 0)
	{
		System.arraycopy(from, 0, res, 0, newlength <= length? newlength : length );
	}
	return res;
}

public char[] copyArrayClean(char[] from, int length, int newlength)
{
	//char[] res = new char[newlen];
	char[] res = getClean();
	if (from != null && length > 0)
	{
		System.arraycopy(from, 0, res, 0, newlength <= length? newlength : length );
	}
	return res;
}

public char[] reverseArray(char[] from, int length)
{
	if (from == null) return null;
	//char[] res = new char[from.length];
	char[] res = get();
	int j = length - 1;
	for (int i = 0; i < length; i++)
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
public int compareArrays(char[] a, int lengthA, char[] b, int lengthB, int offsetB)
{
	int startPos = lengthA - 1;
	int bShift = offsetB;
	
	//if (a.length - 1 > startPos) return 1;
	
	if (lengthB - 1 < startPos + bShift) return 1;
	if (lengthB - 1 > startPos + bShift) return -1;
	//if (a.length - 1 > startPos + bShift) return -1;
	
	int i = startPos;
	for (; i >= 0 && i >= -bShift; i--)
	{
		if (a[i] > b[i + bShift]) return 1;
		if (a[i] < b[i + bShift]) return -1;
	}
	if (i >= 0) return 1;
	if (bShift > 0) return -1;
	return 0;
}

/**
 * Similar to compareArrays with offset = 0
 */
public int compareArrays0(char[] a, int lengthA, char[] b, int lengthB)
{
	//int bShift = b.length - a.length + offset; // 
	int startPos = lengthA - 1; // > b.length? a.length - 1 : b.length - 1;
	
	//if (a.length - 1 > startPos) return 1;
	
	if (lengthB - 1 < startPos) return 1;
	if (lengthB - 1 > startPos) return -1;
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

public char[] arrMultiplySmall(char[] val, int length, long factor, int radix)
{
	if (val == null || length <= 0 || factor == 0)
	{
		//return new char[0]; // zero is zero
		return getClean();
	}
	long overflow = 0;
	//char[] res = new char[val.length];
	char[] res = getClean();
	int i = 0;
	for (; i < length; i++)
	{
		long c1 = val[i] * factor + overflow;
		res[i] = (char)(c1 % radix);
		overflow = c1 / radix;
	}
	while(overflow > 0)
	{
		//res = CharMath.appendArray(res, (char)(overflow % radix));
		res[i++] = (char)(overflow % radix);
		overflow = overflow / radix;
	}	
	
	return res;
}

public char[] arrAddSmall(char[] val, int length, long add, int radix)
{
	long overflow = add;
	char[] res;
	if (length == 0)
	{
		//res = new char[0];
		res = getClean();
	}
	else
	{
		res = copyArrayClean(val, length, length);
		
	}
	int i = 0;
	//for (int i = val.size() - 1; i >= 0; i--)
	for (; i < length; i++)
	{
		long c1 = val[i] + overflow;
		res[i] = (char)(c1 % radix);
		overflow = c1 / radix;
	}
	
	while(overflow > 0)
	{
		//res = CharMath.copyArray(res, res.length + 1);
		//res[res.length - 1] = (char)(overflow % radix);
		res[i++] = (char)(overflow % radix);
		overflow = overflow / radix;
	}
	return res;
}

/** 
 * a shortcut to get val + 1
 */
public char[] arrAddOne(char[] val, int length, int radix)
{
	char[] res;
	if (length <= 0)
	{
		//res = new char[1];
		res = getClean();
	}
	else
	{
		res = copyArrayClean(val, length, length);
		
	}
	//for (int i = val.size() - 1; i >= 0; i--)
	int t = length - 1;
	res[0]++;
	for (int i = 0; i < t; i++)
	{		
		if (res[i] < radix) break;
		res[i] -= radix;
		res[i+1]++;
	}
	if (res[t] >= radix)
	{
		//res = CharMath.copyArray(res, res.length + 1);
		res[t] -= radix;
		res[length] = 1;
	}
	return res;
}

/**
 * Add 1 to the existing array val
 */
public void arrAddOne(char[] val, int radix)
{
	val[0]++;
	for (int i = 0; i < arrayLength; i++)
	{		
		if (val[i] < radix) break;
		val[i] -= radix;
		val[i+1]++;
	}
}


/** 
 * both val and add are in the "math" order.
 */
public char[] arrAdd(char[] val, int valLength, char[] add, int addLength, int radix)
{
	//if (add == null || add.length == 0) return CharMath.copyArray(val, val.length);
	//if (val == null || val.length == 0) return CharMath.copyArray(add, add.length);
	
	int overflow = 0; //add[0];
	int len = Math.max(valLength, addLength);
	char[] res = getClean(); //CharMath.copyArray(val, len); // the tail will be zeros
	//for (int i = val.size() - 1; i >= 0; i--)
	for (int i = 0; i < len || overflow > 0; i++)
	{
		int c = i < addLength? add[i] : 0;
		int c1 = (i < valLength? val[i] : 0) + c + overflow;
		res[i] = (char)(c1 % radix);
		overflow = c1 / radix;
	}
	return res;
}

public char[] arrSubtractSmall(char[] val, int length, long sub, int radix) throws SubtractFailedException
{
	boolean needReverse = false;
	char[] dest = null;
	
	if (sub == 0)
	{
		return copyArray(val, length, length);
	}
	else if (length <= 0)
	{
		needReverse = true;
	}
	else
	{
		long overflow = sub;
		dest = getClean(); //new char[val.length];
		for (int i = 0; i < length; i++)
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
	/*
	int tail = dest.length - 1;
	while (tail >= 0 && dest[tail] == 0) tail--;
	if (tail < dest.length - 1)
	{
		dest = CharMath.copyArray(dest, tail + 1);
	}
	*/
	return dest; 
}

public char[] arrSubtract(char[] val, int valLength, char[] sub, int subLength, int radix) throws SubtractFailedException
{
	boolean needReverse = false;
	char[] dest = null;
	
	if (subLength <= 0)
	{
		return copyArray(val, valLength, valLength);
	}
	else if (valLength < subLength)
	{
		needReverse = true;
	}
	else
	{
		//dest = new char[val.length];
		dest = getClean();
		System.arraycopy(val, 0, dest, 0, valLength);
		//dest = copyArray(val, val.length);
		int overflow = 0; //sub[0];
		for (int i = 0; i < valLength; i++)
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
			if (i < subLength) overflow += sub[i];
			
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
	/*
	int tail = dest.length - 1;
	while (tail >= 0 && dest[tail] == 0) tail--;
	if (tail < dest.length - 1)
	{
		dest = CharMath.copyArray(dest, tail + 1);
	}
	*/
	return dest; 
}
public char[] arrayFromLong(long val)
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
	int power = 0;
	// compact for power - here, arrays will not be compacted
	//while (power < len && b1[power] == 0)
	//{
	//	power++;
	//}
	char[] res = getClean();
	for (int i = 0; i < len - power; i++)
	{
		//mantissa[i] = b1[len - i - 1];
		res[i] = b1[power + i];
	}
	return res;
}
}
