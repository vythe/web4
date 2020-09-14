package net.vbelev.utils;

import java.util.ArrayList;

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

private static void arrMultiplySmall1(ArrayList<Character> val, long factor, int radix)
{
	long overflow = 0;
	if (val.size() == 0)
	{
		val.add((char)0);
	}
	//for (int i = val.size() - 1; i >= 0; i--)
	for (int i = 0; i < val.size(); i++)
	{
		long c1 = val.get(i) * factor + overflow;
		val.set(i, (char)(c1 % radix));
		overflow = c1 / radix;
	}
	while(overflow > 0)
	{
		val.add((char)(overflow % radix));
		overflow = overflow / radix;
	}
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


private static void arrAddSmall1(ArrayList<Character> val, long add, int radix)
{
	long overflow = add;
	if (val.size() == 0)
	{
		val.add((char)0);
	}
	//for (int i = val.size() - 1; i >= 0; i--)
	for (int i = 0; i < val.size(); i++)
	{
		long c1 = val.get(i) + overflow;
		val.set(i, (char)(c1 % radix));
		overflow = c1 / radix;
	}
	while(overflow > 0)
	{
		val.add((char)(overflow % radix));
		overflow = overflow / radix;
	}	
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

private static void arrAdd1(ArrayList<Character> val, char[] add, int radix)
{
	if (add == null || add.length == 0) return;
	
	int overflow = add[0];
	while (val.size() < add.length)
	{
		val.add((char)0);
	}
	//for (int i = val.size() - 1; i >= 0; i--)
	for (int i = 0; i < val.size() || i < add.length; i++)
	{
		int c = i < add.length? add[i] : 0;
		int c1 = val.get(i) + c + overflow;
		val.set(i, (char)(c1 % radix));
		overflow = c1 / radix;
	}
	while(overflow > 0)
	{
		val.add((char)(overflow % radix));
		overflow = overflow / radix;
	}	
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

/** returns true if the result is positive, false if the result is negative */
private static boolean arrSubtractSmall1(ArrayList<Character> val, long sub, int radix) throws SubtractFailedException
{
	long overflow = sub;
	if (val.size() == 0)
	{
		val.add((char)0);
	}
	//for (int i = val.size() - 1; i >= 0; i--)
	for (int i = 0; i < val.size(); i++)
	{
		char overflowLastDigit = (char)(overflow % radix);
		if (overflowLastDigit > val.get(i))
		{
			val.set(i, (char)(val.get(i) + radix));
			overflow += radix;
		}
		val.set(i, (char)(val.get(i) - overflowLastDigit));
		overflow = overflow / radix;				
	}
	/*
	while(overflow > 0)
	{
		val.add((char)(overflow % radix));
		overflow = overflow / radix;
	}
	*/
	if (overflow > 0)
	{
		//throw new IllegalArgumentException("subtraction turned negative, not supproted yet");
		throw new SubtractFailedException();
		/*
		char[] neg = digitsFromLong(overflow, 10);
		val.clear();
		for (int i = 0; i < neg.length; i++)
			val.add(neg[i]);
		
		return false;
		*/
	}
	return true; 
}

public static char[] arrSubtract(char[] val, char[] sub, int radix) throws SubtractFailedException
{
	boolean needReverse = false;
	char[] dest = null;
	
	if (sub.length == 0)
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
		long overflow = 0; //sub[0];
		for (int i = 0; i < val.length; i++)
		{
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
}
