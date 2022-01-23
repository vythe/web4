package net.vbelev.utils.test;

//import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import net.vbelev.utils.*;


public class CharMathTest
{
	@Test
	void testAddSmall()
	{
		long d1 = Utils.random.nextInt();
		if (d1 <0) d1 = -d1;
		char[] arr1 = CharMath.arrayFromLong(d1);
		for (int i = 0; i < 1e7; i++)
		{
			long d2 = Utils.random.nextInt();
			if (d2 <0) d2 = -d2;
			//char[] arr1 = CharMath.arrayFromLong(d1);
			char[] res1 = CharMath.arrAddSmall(arr1, d2, 10);
			String res = ""; //Decimal.digitsToString(res1, res1.length);
			//System.out.println("d1=" + (d1 + d2));
			//System.out.println("v1=" + res);
			//Assertions.assertEquals("" + (d1 + d2), res, "Failed to " + d1 + " + " + d2 + " = " + (d1 + d2) + " (got " + res + ")" );
		}
	}

	@Test
	void testAddSmallCached()
	{
		CharMathCached cache = new CharMathCached(40, 10);
		
		long d1 = Utils.random.nextInt();
		if (d1 <0) d1 = -d1;
		char[] arr1 = cache.arrayFromLong(d1);
		int arrLength = cache.getLength(arr1);

		for (int i = 0; i < 1e7; i++)
		{
			long d2 = Utils.random.nextInt();
			if (d2 <0) d2 = -d2;
			//char[] arr1 = CharMath.arrayFromLong(d1);
			char[] res1 = cache.arrAddSmall(arr1, arrLength, d2, 10);
			String res = Decimal.digitsToString(res1, cache.getLength(res1));
			cache.stash(res1);
			Assertions.assertEquals("" + (d1 + d2), res, "Failed to " + d1 + " + " + d2 + " = " + (d1 + d2) + " (got " + res + ")" );
			//System.out.println("d1=" + (d1 + d2));
			//System.out.println("v1=" + res);
		}
		//cache.stash(arr1);
		System.out.println("cache hit=" + cache.cacheHit + ", miss=" + cache.cacheMiss);
	}

	//@Test
	void testAdd()
	{
		for (int i = 0; i < 1000; i++)
		{
			long d1 = Utils.random.nextInt();
			if (d1 <0) d1 = -d1;
			long d2 = Utils.random.nextInt();
			if (d2 <0) d2 = -d2;
			char[] arr1 = CharMath.arrayFromLong(d1);
			//char[] arr1 = CharMath.arrayFromLong(d1);
			char[] res1 = CharMath.arrAddSmall(arr1, d2, 10);
			String res = Decimal.digitsToString(res1);
			Assertions.assertEquals("" + (d1 + d2), Decimal.digitsToString(res1), "Failed to " + d1 + " + " + d2 + " = " + (d1 + d2) + " (got " + res + ")" );
			//System.out.println("d1=" + (d1 + d2));
			//System.out.println("v1=" + Decimal.digitsToString(res1));
		}
	}
	
}
