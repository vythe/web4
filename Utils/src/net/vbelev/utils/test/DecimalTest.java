package net.vbelev.utils.test;

//import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import junit.framework.Assert;
import net.vbelev.utils.*;

class DecimalTest
{
	@Test
	void convertRadix()
	{
		String orig = "1234000";
		String origHex = "12D450";
		char[] dec = Decimal.digitsFromString(orig);
		Assertions.assertEquals(Decimal.digitsToString(dec), orig, "test digitsFromString");
		char[] hex = Decimal.convertRadix(dec, 10, 16);
		System.out.println("convert 10 to 16, dec=" + Decimal.digitsToString(dec) + ", hex=" + Decimal.digitsToString(hex));
		Assertions.assertEquals(Decimal.digitsToString(hex), origHex, "test convertRadix 10 to 16");
		char[] dec2 = Decimal.convertRadix(hex, 16, 10);
		Assertions.assertEquals(Decimal.digitsToString(dec2), orig, "test convertRadix 16 to 10");
		System.out.println("convertRadix, dec: " + Decimal.digitsToString(dec) + ", hex: " + Decimal.digitsToString(hex) + ", dec: " + Decimal.digitsToString(dec2));
	}

	//@Test
	void testLong()
	{
		long val1 = -12345600;
		Decimal t1 = new Decimal(val1, 0);
		System.out.println("testLong: " + t1.toString());
		Assertions.assertEquals(val1, t1.toLong(), "toLong failed");
		
		double val2 = 123.4508e-8;
		Decimal t2 = new Decimal(val2, 7);
		System.out.println("testLong: " + t2.toString());
		Assertions.assertEquals(Math.round(val2 * 1e8), Math.round(t2.toDouble() * 1e8), "toDouble failed");		
	}
	
	
	//@Test
	void testMultiplySmall()
	{
		Decimal t1 = new Decimal(-1788);
		int factor = 357;
		Decimal r1 = t1.multiplySmall(factor);
		System.out.println("testMultiplySmall: " + t1.toString() + " * " + factor + " = " + r1.toString());
		Assertions.assertEquals(t1.toLong() * factor, r1.toLong());

		Decimal t2 = new Decimal(-1788e-3, 6);
		int factor2 = 357;
		Decimal r2 = t2.multiplySmall(factor2);
		System.out.println("testMultiplySmall: " + t2.toStringE() + " * " + factor + " = " + r2.toStringE());
		Assertions.assertEquals(t2.toDouble() * factor2, r2.toDouble());
		
	}
	
	//@Test
	void testMultiply()
	{
		String[][] testPairs = new String[][]{
			{"0.0673323", "1"}
			, {"0.0673323833453713938013152", "1"}
			,{"2.687782381712442436594301e-1", "1"}
			, {"0.268", "1", "0"}
			, {"3.35", "1.6", "0"}
			, {"24000", "17", "0"}
			, {"24000", "17", "3"}
			, {"65", "17", "0"}
			//, {"12.345", "1.23"}
			//, {"12.3", "1.2345"}
			//, {"1.2345", "12.3"}
			//, {"1.23", "12.345"}
		};
		
		for (int i = 0; i < testPairs.length; i++)
		{
			Decimal d1 = new Decimal(testPairs[i][0]);
			Decimal d2 = new Decimal(testPairs[i][1]);
			
			System.out.println("multiply d1=" + d1.toStringF() + ", d2=" + d2.toStringF());
			Decimal r1 = d1.multiply(d2);
			System.out.println("res=" + r1.toStringF());
			double tru = d1.toDouble() * d2.toDouble();
			System.out.println("tru=" + tru);
			Assertions.assertTrue(r1.toDouble() == tru,
					"multiply d1=" + d1.toStringF() + ", d2=" + d2.toStringF() + ", res=" + r1.toStringF() + ", true=" + tru
			);
		}
		System.out.println("testMultiply done");
	}
	
	private static final Decimal DECIMAL_TWO = new Decimal(2);
	private static final Decimal DECIMAL_M_ONE = new Decimal(-1);

	//@Test
	void testDivide2()
	{
		Decimal val = new Decimal("1.0673323833453713938013152");
		Decimal s1 = val.add(DECIMAL_M_ONE);
		s1 = new Decimal("0.0673323833453713938013152");
		Decimal fac = DECIMAL_M_ONE;		
		Decimal res = Decimal.ZERO;
		int i = 1;
		int numDigits = 3;
		fac = fac.minus().multiply(s1);
		Decimal.Division div = fac.divide(new Decimal("1"), numDigits);
		Decimal fac1 = new Decimal("0.0673323833453713938013152");
		Decimal fac2 = Decimal.ONE.multiply(s1);
		Decimal fac3 = s1.multiply(Decimal.ONE);
		Decimal.Division div1 = fac1.divide(new Decimal("1"), numDigits);
		
		res = res.add(div.quotient);
		
		System.out.println("res=" + res.toStringF());
	}
	
	//@Test
	void testDivide1()
	{
		Decimal d1 = new Decimal("63674155060671161576367415506067116157");
		//double dd1 = 1.87e-1; //6386900052318118525818e-1;
		//String ds1 = String.format("%e", dd1);
		//Decimal d1_1 = new Decimal(ds1); // this is broken!
		//Decimal d1 = new Decimal(dd1, 3);
		Decimal d2 = new Decimal("11");
		int extraDigits = 3;
		
		System.out.println("divide d1=" + d1.toStringF() + ", d2=" + d2.toStringF() + ", extra=" + extraDigits);
		
		Decimal.Division div = d1.divide(d2, extraDigits);

		Decimal rev1 = div.quotient.multiply(d2).add(div.remainder);	
		System.out.println("res quotient=" + div.quotient.toStringF() + ", remainder=" + div.remainder.toStringF() + ", rev=" + rev1.toStringF());

		double rev = div.quotient.toDouble() * d2.toDouble() + div.remainder.toDouble();
		Assertions.assertTrue(rev1.equals(d1), 
				"divide d1=" + d1.toStringF() + ", d2=" + d2.toStringF() + ", extra=" + extraDigits + ", rev=" + rev1.toStringF()
		);
	}
	//@Test
	void testDivide()
	{
		String[][] testPairs = new String[][]{
			{"2.687782381712442436594301e-1", "1", "20"}
			, {"0.268", "1", "0"}
			, {"3.35", "1.6", "0"}
			, {"24000", "17", "0"}
			, {"24000", "17", "3"}
			, {"65", "17", "0"}
			//, {"12.345", "1.23"}
			//, {"12.3", "1.2345"}
			//, {"1.2345", "12.3"}
			//, {"1.23", "12.345"}
		};
		
		for (int i = 0; i < testPairs.length; i++)
		{
			Decimal d1 = new Decimal(testPairs[i][0]);
			Decimal d2 = new Decimal(testPairs[i][1]);
			int extraDigits = Utils.tryParseInt(testPairs[i][2]);
			
			//System.out.println("divide d1=" + d1.toStringF() + ", d2=" + d2.toStringF() + ", extra=" + extraDigits);
			
			Decimal.Division div = d1.divide(d2, extraDigits);
			//double rev = div.quotient.toDouble() * d2.toDouble() + div.remainder.toDouble();
			Decimal rev1 = div.quotient.multiply(d2).add(div.remainder);
			Assertions.assertTrue(rev1.equals(d1), 
					"divide d1=" + d1.toStringF() + ", d2=" + d2.toStringF() + ", extra=" + extraDigits + ", rev=" + rev1.toStringF()
			);
			/*
			System.out.println("res quotient=" + div.quotient.toStringF() + ", remainder=" + div.remainder.toStringF() + ", rev=" + rev1.toStringF());
			//long rem = d1.toLong() % d2.toLong();
			long quot = (long)(d1.toDouble() / d2.toDouble());
			double rem = d1.toDouble() - quot * d2.toDouble();
			//System.out.println("tru quotient=" + quot + ", remainder=" + rem);
			 */
		}
		System.out.println("testDivide done");
		/*		
		
		//Decimal d1 = new Decimal("" + (int)(17 * 1.25));
		//Decimal d1 = new Decimal("2122439587"); // + (int)(2100));
		//Decimal d2 = new Decimal("83588090123");
		//Decimal d1 = new Decimal("3.35"); // + (int)(2100));
		//Decimal d2 = new Decimal("1.6");
		//Decimal d1 = new Decimal("986707205");
		//Decimal d2 = new Decimal("982118665");
		Decimal d1 = new Decimal("24000");
		Decimal d2 = new Decimal("170");
		//Decimal d1 = new Decimal(Utils.random.nextInt());
		//Decimal d2 = new Decimal(Utils.random.nextInt());
		System.out.println("d1=" + d1.toStringF() + ", d2=" + d2.toStringF());
		Decimal.Division div = d1.divide(d2, 0);
		long quot = (long)(d1.toDouble() / d2.toDouble());
		double rev = div.quotient.toDouble() * d2.toDouble() + div.remainder.toDouble();
		//long rem = d1.toLong() % d2.toLong();
		double rem = d1.toDouble() - quot * d2.toDouble();
		System.out.println("res quotient=" + div.quotient.toStringF() + ", remainder=" + div.remainder.toStringF());
		System.out.println("rev=" + rev);
		System.out.println("tru quotient=" + quot + ", remainder=" + rem);
		*/
		
		
	}
	
	//@Test
	void testLog1()
	{
		/*
		long val1 = 634976223169905016l; // Utils.random.nextLong();
		double d1 = Math.log10(val1);
		double d2 = Math.log10(val1 + 100000);
		double r1 = d2 - d1;
		System.out.println("val1=" + val1 + ", r1=" + r1);
		*/
		double s1 = 0.5;
		double fac = -1;
		double ln1 = 0;
		for (int i = 1; i <= 1000000; i++) 
		{
			fac = -fac * (s1 - 1);
			ln1 += fac / i;
		}
		System.out.println("ln1=" + ln1 + ", exp=" + Math.exp(ln1));
		System.out.println("tru=" + Math.log(s1) + ", exp=" + Math.exp(ln1));
		
		Decimal ln2 = DecimalLg.naturalSmall(new Decimal(s1 + ""), 10);
		System.out.println("ln2=" + ln2.toStringF()+ ", exp2=" + Math.exp(ln2.toDouble()));
	}
	
	//@Test
	void testLog2()
	{
		double val = 2.1;
		DecimalLg lg = new DecimalLg(300);
		lg.getE();
		Decimal v1 = new Decimal(val + "");
		Decimal r1 = lg.natural(v1);
		
		System.out.println("natural for val=" + val);
		System.out.println("res=" + r1.toStringF());
		System.out.println("tru=" + Math.log(val));
		/*
		Decimal big2 = Decimal.ONE;
		for (int j = 0; j < 1024; j++)
		{
			big2 = big2.multiplySmall(2);
		}
		*/
		double val2 = 12;
		Decimal v2 = new Decimal(val2 + "");
		Decimal r2 = lg.log10(v2);
		System.out.println("r2=" + r2.toStringE());
		System.out.println("t2=" + Math.log10(val2));
		
		for (int i = 0; i < 100; i++)
		{
			double v = Utils.random.nextDouble() * 100;
			Decimal vd = new Decimal(v + "");
			Decimal r = lg.log10(vd);
			double rn = Utils.round(r.toDouble(), 15);
			double t = Utils.round(Math.log10(v),15);
			if (t != rn)
			{
				System.out.println("*** v=" + v);
				System.out.println("r=" + r.toStringF());
				System.out.println("t=" + t);
			}
		}
		System.out.println("testLog2 done");
	}
	
	//@Test
	void testLog()
	{
		Decimal d1 = new Decimal("0.4978706837");
		double val1 = d1.toDouble();
		Decimal tVal1 = DecimalLg.naturalSmall(d1, 10);
		double rVal1 = Math.log(d1.toDouble());
		double diff1 = tVal1.toDouble() - rVal1;
		System.out.println("diff1=" + diff1);
		/*
		for (int i = 0; i < 10; i++)
		{
			double val = 1.5 - Utils.random.nextDouble(); // + 1.;
			System.out.println("**test " + val);
			Decimal tVal = DecimalLg.naturalSmall(new Decimal(val), 200);
			double rVal = Math.log(val);
			double diff = Math.abs(tVal.toDouble() - rVal);
			System.out.println("calc=" + tVal.toStringF());
			System.out.println("true=" + rVal + ", diff=" + Utils.round(diff / rVal, 24));
		}
		*/
	}
	
	
	//@Test
	void testCast()
	{
		long v1 = 123;
		Decimal d1 = Decimal.fromObject(v1);
		System.out.println("cast long " + v1 + ", got " + d1.toString());
		Assertions.assertEquals(v1, d1.toLong());

		Short v2 = 1234;
		Decimal d2 = Decimal.fromObject(v2);
		System.out.println("cast short " + v2 + ", got " + d2.toString());
		Assertions.assertEquals((long)v2, d2.toLong());
		
		String v3 = "-12.34e-5";
		Decimal d3 = Decimal.fromObject(v3);
		System.out.println("cast string " + v3 + ", got " + d3.toStringE());
		Assertions.assertEquals(-12.34e-5, d3.toDouble());			
	}
	
	//@Test 
	void testCompare()
	{
		String[][] testPairs = new String[][]{
			{"1746874230", 
			 "1425535767"},
			{"-987445270", "-954607249"},
			{"0", "1e-100"},
			{"1.2345", "1.23"},
			{"1.23", "1.2345"},
			{"12.345", "1.23"},
			{"12.3", "1.2345"},
			{"1.2345", "12.3"},
			{"1.23", "12.345"},
			{"-1.23", "12.345"},
			{"1.23", "-12.345"},
			{"-1.2345", "1.2345"},
			{"1.2345", "-1.2345"},
			{"10301372", "37750672"},
			{"-10301372", "-37750672"}
		};

		
		
		for (int i = 0; i < testPairs.length; i++)
		{
			Decimal d1 = new Decimal(testPairs[i][0]);
			Decimal d2 = new Decimal(testPairs[i][1]);
			System.out.println("compare d1=" + d1.toStringF() + ", d2=" + d2.toStringF());
			int r1 = d1.compareTo(d2);
			int truth = Double.compare(d1.toDouble(), d2.toDouble());
			System.out.println("res=" + r1 + ", truth=" + truth);
			Assertions.assertEquals(r1, truth, "compare d1=" + d1.toStringF() + ", d2=" + d2.toStringF() + ", res=" + r1 + ", truth=" + truth);
		}
		
		for (int j = 0; j < 1000; j++)
		{
			Decimal d1 = new Decimal(Utils.random.nextInt());
			Decimal d2 = new Decimal(Utils.random.nextInt());
			int r2 = d1.compareTo(d2);
			int truth = Double.compare(d1.toDouble(), d2.toDouble());
			//System.out.println("res=" + r1 + ", truth=" + truth);
			if (r2 != truth)
			{
				System.out.println("FAIL: " + "compare d1=" + d1.toStringF() + ", d2=" + d2.toStringF() + ", res=" + r2 + ", truth=" + truth);
			}
			Assertions.assertEquals(r2, truth, "compare d1=" + d1.toStringF() + ", d2=" + d2.toStringF() + ", res=" + r2 + ", truth=" + truth);
		}
	}
	
	//@Test
	void testCompareArr1()
	{
		Decimal d = new Decimal("1.2345");
		Decimal v = new Decimal("12.3");
		
		int r = d.compareTest(v);
		int truth = Double.compare(d.toDouble(), v.toDouble());
		System.out.println("compare d=" + d.toStringF() + ", v=" + v.toStringF() + ", res=" + r);
		System.out.println("truth=" + truth);
	}
	
	
	//@Test
	void testCompareArr()
	{
		String[][] testPairs = new String[][]{
			{"1.2345", "1.23"},
			{"1.23", "1.2345"},
			{"12.345", "1.23"},
			{"12.3", "1.2345"},
			{"1.2345", "12.3"},
			{"1.23", "12.345"}
		};
		
		for (int i = 0; i < testPairs.length; i++)
		{
			Decimal d1 = new Decimal(testPairs[i][0]);
			Decimal d2 = new Decimal(testPairs[i][1]);
			//System.out.println("compare d1=" + d1.toStringF() + ", d2=" + d2.toStringF());
			int r1 = d1.compareTest(d2);
			int truth = Double.compare(d1.toDouble(), d2.toDouble());
			System.out.println("res=" + r1 + ", truth=" + truth);
			Assertions.assertEquals(r1, truth, "compare d1=" + d1.toStringF() + ", d2=" + d2.toStringF() + ", res=" + r1 + ", truth=" + truth);
		}
		/*
		Decimal d = new Decimal("1.2345");
		Decimal v = new Decimal("12.3");
		
		int r = d.compareTest(v);
		int truth = Double.compare(d.toDouble(), v.toDouble());
		System.out.println("compare d=" + d.toStringF() + ", v=" + v.toStringF() + ", res=" + r);
		System.out.println("truth=" + truth);
		*/
	}
	
	//@Test
	void testDecimalLong()
	{
		/*
		//fail("Not yet implemented");
		//org.junit.jupiter.api.Assertions.
		Decimal t = new Decimal(0xa32dL);
		System.out.println(t);
		Assertions.assertEquals(t.toString(), "41773p0", "t not equal!");
		Decimal t2 = new Decimal(-12345600);
		System.out.println(t2 + " / " + t2.toHexString());
		Assertions.assertEquals(t2.toHexString(),"-0xBC61p2", "t2 not equal!"); 
		
		Decimal t3 = new Decimal((double)0xa0c7, 7);
		System.out.println(t3);
		System.out.println(t3.toHexString());
		
		Decimal t4 = Decimal.fromHex("-1CD54", "-002");
		System.out.println(t4 + " / " + t4.toHexString());

		Decimal t5 = new Decimal(-0.035, 7);
		Decimal t6 = Decimal.fromHex(t5.getMantissaHex(), t5.getPowerHex(3) );
		
		System.out.println("from t5=" + t5 + ", hex=" + t5.toHexString() + " to t5=" + t5.toHexString());
		*/
		
		/*
		ArrayList<Character> test = new ArrayList<Character>();
		test.add((char)1);
		for (int i = 0; i < 30; i++)
		{
			//Decimal.multiplySmall(test, 10, 16);
			//Decimal.addSmall(test,  10, 16);
			System.out.println(i + ": " + Decimal.digitsToString(test));
		}
		*/
		
		System.out.println(new Decimal("12345"));
		System.out.println(new Decimal("12345."));
		System.out.println(new Decimal("12345.678"));
		System.out.println(new Decimal("-12345.678e"));
		System.out.println(new Decimal("-12345.678e4"));
		System.out.println(new Decimal("12345.678e-23"));
		System.out.println(new Decimal("1234534534534534534564456457448758696789.678e-23"));
	}

	//@Test
	void testSubtractSmall()
	{
		/*
		long val = 170;
		long sub = 55;		
		try
		{
		char[] res = CharMath.arrSubtract(Decimal.digitsFromLong(val, 10), Decimal.digitsFromLong(sub, 10), 10);
		System.out.println("testSubtract, val=" + val + ", sub=" + sub + ", res=" + Decimal.digitsToString(res));
		}
		catch (CharMath.SubtractFailedException x)
		{
			throw new IllegalArgumentException("Failed to addSubtract " + val + " - " + sub);
		}
		*/
		//Decimal d1 = new Decimal(17);
		long d1 = 12324234;
		long v1 = -545645346;
		Decimal r1 = new Decimal(d1).addSmall(v1);
		System.out.println("testSubtract, d1=" + d1 + " add " + v1 + " = " + r1);
		Assertions.assertEquals(d1 + v1, r1.toLong());
	}
	
	//@Test
	void testAdd()
	{
		String[][] testPairs = new String[][]{
			{"5", "-10"}
			, {"5", "10"}
			, {"1.7", "3.14"}
			, {"0.0673323833453713938013152", "1"}
			,{"2.687782381712442436594301e-1", "1"}
			, {"0.268", "1"}
			, {"3.35", "1.6"}
			, {"24000", "17"}
			, {"65", "-17"}
			//, {"12.345", "1.23"}
			//, {"12.3", "1.2345"}
			//, {"1.2345", "12.3"}
			//, {"1.23", "12.345"}
		};
		
		for (int i = 0; i < testPairs.length; i++)
		{
			Decimal d1 = new Decimal(testPairs[i][0]);
			Decimal d2 = new Decimal(testPairs[i][1]);
			
			//System.out.println("add d1=" + d1.toStringF() + ", d2=" + d2.toStringF());
			Decimal r1 = d1.add(d2);
			//System.out.println("res=" + r1.toStringF());
			double tru = d1.toDouble() + d2.toDouble();
			//System.out.println("tru=" + tru);
			Assertions.assertTrue(r1.toDouble() == tru,
					"add d1=" + d1.toStringF() + ", d2=" + d2.toStringF() + ", res=" + r1.toStringF() + ", true=" + tru
			);
		}
		System.out.println("testAdd done");
		/*
		Decimal d1 = new Decimal("1.70");
		Decimal v1 = new Decimal("-3.1400");
		Decimal r1 = d1.add(v1);
		System.out.println("testAdd, d1=" + d1 + " add " + v1 + " = " + r1.toDouble());
		//System.out.println("test round=" + Utils.round(d1.toDouble() + v1.toDouble(), 6));
		Assertions.assertEquals(Utils.round(d1.toDouble() + v1.toDouble(), 6), r1.toDouble());
		
		Decimal d2 = new Decimal("-12");
		long v2 = -6;
		Decimal r2 = d2.addSmall(v2);
		System.out.println("testAdd, d2=" + d2 + " add " + v2 + " = " + r2.toDouble());
		Assertions.assertEquals(-12 -6, r2.toDouble());

		
		Decimal d3 = new Decimal("12");
		long v3 = -25;
		Decimal r3 = d3.addSmall(v3);
		System.out.println("testAdd, d3=" + d3 + " add " + v3 + " = " + r3.toDouble());
		Assertions.assertEquals(12 + v3, r3.toDouble());
		
		// test trimming
		Decimal d4 = new Decimal("-128");
		long v4 = 28;
		Decimal r4 = d4.addSmall(v4);
		System.out.println("testAdd, d4=" + d4 + " add " + v4 + " = " + r4.toString());
		Assertions.assertEquals(-128 + v4, r4.toDouble());
	*/	
	}
	
	//@Test
	void testRound()
	{
		//Decimal d1 = new Decimal("-163.493567789");
		//int rnd = 5;
		Decimal d1 = new Decimal("13.450056");
		int rnd = 6;
		Decimal r1 = d1.round(rnd);
		Decimal r1_1 = d1.floor(rnd);
		System.out.println("round from " + d1.toString() + " is " + r1.toString());
		System.out.println("d1=" + d1.toStringF() + ", math-round=" + Utils.round(d1.toDouble(), rnd - d1.getEPower() - 1) + ", r1=" + r1.toDouble());
		System.out.println("floor=" + r1_1.toDouble());
		Assertions.assertEquals(Utils.round(d1.toDouble(), rnd - d1.getEPower() - 1), r1.toDouble());
	}
	
	//@Test
	void testStress()
	{
		Decimal d1 = new Decimal(1);
		Decimal d2 = new Decimal(1);
		for (int i = 0; i < 1e6; i++) // for 1e6 this was 895 seconds
		{
			d1 = d1.add(d1);
			Decimal d3 = d1.round(3);
		}
		System.out.println("test stress final=" + d1.floor(18).toStringE());
	}

	String randomLong(int len)
	{
		char[] arr = new char[len];
		for (int i = 0; i < len; i++) arr[i] = (char)Utils.random.nextInt(10);

		return Decimal.digitsToString(arr);
	}
	//@Test
	void testStressMult()
	{
		int tt = 99999999;
		//Decimal d1 = new Decimal(Utils.random.nextInt());
		String longLong = randomLong(200); // 1e7 divs with this long is 485 sec
		Decimal d1 = new Decimal(longLong);
		Decimal d2 = new Decimal(Utils.random.nextInt());
		System.out.println("repeat mult: " + d1.toStringF() + " * " + d2.toStringF() + " = " + d1.multiply(d2).toStringF());
		for (int i = 0; i < 1e6; i++) // for 2e8 this is 129 sec
		{
			//long v1 = Utils.random.nextInt();
			long v2 = Utils.random.nextInt();
			//Decimal res = new Decimal(v1).multiply(new Decimal(v2));
			Decimal res = d1.multiply(new Decimal(v2));
			
			//Decimal res = d1.multiply(d2);
			//if (res.toLong() != v1 * v2)
			//{
			//	System.out.println("stressMult failed for v1=" + v1 + " v2=" + v2 + ", res=" + res.toStringF());
			//	System.out.println("true=" + (v1 * v2));
			//}
			//else
			//{
			//	System.out.println("stressMult success for v1=" + v1 + " v2=" + v2 + ", res=" + res.toLong());
			//}
		}
		/*
		Decimal d1 = new Decimal(1);
		Decimal d2 = new Decimal(1);
		for (int i = 0; i < 1e6; i++) // for 1e6 this was 895 seconds
		{
			d1 = d1.add(d1);
			Decimal d3 = d1.round(3);
		}
		System.out.println("test stress final=" + d1.floor(18).toStringE());
		*/
	}

	//@Test
	void testE()
	{
		String internetE = "2.7182818284590452353602874713526624977572470936999595749669676277240766303535475945713821785251664274274663919320030599218174135966290435729003342952605956307381323286279434907632338298807531952510190115738341879307021540891499348841675092447614606680822648001684774118537423454424371075390777449920695517027618386062613313845830007520449338265602976067371132007093287091274437470472306969772093101416928368190255151086574637721112523897844250569536967707854499699679468644549059879316368892300987931277361782154249992295763514822082698951936680331825288693984964651058209392398294887933203625094431173012381970684161403970198376793206832823764648042953118023287825098194558153017567173613320698112509961818815930416903515988885193458072738667385894228792284998920868058257492796104841984443634632449684875602336248270419786232090021609902353043699418491463140934317381436405462531520961836908887070167683964243781405927145635490613031072085103837505101157477041718986106873969655212671546889570350354";
		Decimal e = DecimalLg.calculateE(1000);
		Assertions.assertEquals(e.toStringF(), internetE);
		System.out.println("E is good");
	}
	//@Test
	void testStressDiv()
	{
		int len = 20;
		//Decimal d1 = Decimal.random(len, len - 1);
		Decimal d1 = new Decimal("14929505174280975716");
		System.out.println("repeat div: " + d1.toStringF()); 
		// stress 1e7 over *20 = 4.8 - 4.9 sec
		//System.out.println("repeat mult: " + d1.toStringF() + " * " + d2.toStringF() + " = " + d1.multiply(d2).toStringF());
		for (int i = 0; i < 1e7; i++) // for 1e7 over decimal*300 this is 50 - 80 sec; 1e7 over decimal*100 is 30 - 36 sec sec
		{
			//long v2 = Utils.random.nextInt();
			//if (v2 == 0) continue;
			//Decimal d2 = new Decimal(v2)
			Decimal d2 = Decimal.random(len, len - 1);
			if (d2.isZero()) continue;
			try
			{
				//Decimal.Division res = new Decimal(v1).divide(new Decimal(v2), 0);
				Decimal.Division res = d1.divide(d2, 0);
				/* to test or not to test...*/
				Decimal mult = res.quotient.multiply(d2).add(res.remainder);
				if (mult.compareTo(d1) != 0)
				{
					System.out.println("failed for d2=" + d2.toStringF());
				}
				/**/
			}
			catch (Exception x)
			{
				//System.out.println("error on v1=" + v1 + ", v2=" + v2);
				System.out.println(x);
				break;
			}
		}
		System.out.println("stressDiv done");
		/*
		Decimal d1 = new Decimal(1);
		Decimal d2 = new Decimal(1);
		for (int i = 0; i < 1e6; i++) // for 1e6 this was 895 seconds
		{
			d1 = d1.add(d1);
			Decimal d3 = d1.round(3);
		}
		System.out.println("test stress final=" + d1.floor(18).toStringE());
		*/
	}

	//@Test
	void testStressLog()
	{
		DecimalLg lg = new DecimalLg(300);
		for (int i = 0; i < 10000; i++) // for 1e4 cycles over decimal*300, it was 2831 sec
		{
			Decimal d2 = Decimal.random(300, 300);
			if (d2.isZero()) continue;
			try
			{
			//Decimal.Division res = new Decimal(v1).divide(new Decimal(v2), 0);
			Decimal res = lg.log10(d2);
			}
			catch (Exception x)
			{
				//System.out.println("error on v1=" + v1 + ", v2=" + v2);
				System.out.println(x);
				break;
			}
		}
		System.out.println("stressLog done");
		/*
		Decimal d1 = new Decimal(1);
		Decimal d2 = new Decimal(1);
		for (int i = 0; i < 1e6; i++) // for 1e6 this was 895 seconds
		{
			d1 = d1.add(d1);
			Decimal d3 = d1.round(3);
		}
		System.out.println("test stress final=" + d1.floor(18).toStringE());
		*/
	}

	//@Test
	void testEFull()
	{
//2.7182818284 5904523536 0287471352 6624977572 4709369995 9574966967 6277240766 3035354759 4571382178 5251664274 
//  2746639193 2003059921 8174135966 2904357290 0334295260 5956307381 3232862794 3490763233 8298807531 9525101901 
//  1573834187 9307021540 8914993488 4167509244 7614606680 8226480016 8477411853 7423454424 3710753907 7744992069 
//  5517027618 3860626133 1384583000 7520449338 2656029760 6737113200 7093287091 2744374704 7230696977 2093101416
//  9283681902 5515108657 4637721112 5238978442 5056953696 7707854499 6996794686 4454905987 9316368892 3009879312 
//  7736178215 4249992295 7635148220 8269895193 6680331825 2886939849 6465105820 9392398294 8879332036 2509443117 
//  3012381970 6841614039 7019837679 3206832823 7646480429 5311802328 7825098194 5581530175 6717361332 0698112509
//  9618188159 3041690351 5988885193 4580727386 6738589422 8792284998 9208680582 5749279610 4841984443 6346324496 
//  8487560233 6248270419 7862320900 2160990235 3043699418 4914631409 3431738143 6405462531 5209618369 0888707016 
//  7683964243 7814059271 4563549061 3031072085 1038375051 0115747704 1718986106 8739696552 1267154688 9570350354 019952038
// internet:
//2,7182818284 5904523536 0287471352 6624977572 4709369995 9574966967 6277240766 3035354759 4571382178 5251664274 
//  2746639193 2003059921 8174135966 2904357290 0334295260 5956307381 3232862794 3490763233 8298807531 9525101901 
//  1573834187 9307021540 8914993488 4167509244 7614606680 8226480016 8477411853 7423454424 3710753907 7744992069 
//  5517027618 3860626133 1384583000 7520449338 2656029760 6737113200 7093287091 2744374704 7230696977 2093101416 
//  9283681902 5515108657 4637721112 5238978442 5056953696 7707854499 6996794686 4454905987 9316368892 3009879312 
//  7736178215 4249992295 7635148220 8269895193 6680331825 2886939849 6465105820 9392398294 8879332036 2509443117 
//  3012381970 6841614039 7019837679 3206832823 7646480429 5311802328 7825098194 5581530175 6717361332 0698112509 
//  9618188159 3041690351 5988885193 4580727386 6738589422 8792284998 9208680582 5749279610 4841984443 6346324496 
//  8487560233 6248270419 7862320900 2160990235 3043699418 4914631409 3431738143 6405462531 5209618369 0888707016 
//  7683964243 7814059271 4563549061 3031072085 1038375051 0115747704 1718986106 8739696552 1267154688 9570350354 
		
		// e = sum(1 + 1 / n!)
		int numDigits = 1000;
		Decimal add = new Decimal(1);
		Decimal e = new Decimal(2);
		Decimal stop = new Decimal(new char[]{1}, -numDigits, true);
		int n = 1;
		Decimal fact = new Decimal(1);
		Decimal one = new Decimal(1);
		//while(n < 100) //add.compareTo(stop) > 0)
		while(add.compareTo(stop) > 0)
		{
			n++;
			//add = add.divide(new Decimal(n), numDigits).quotient;
			//e = e.add(add);
			fact = fact.multiplySmall(n);
			//Decimal.Division add1 = one.divide(fact, numDigits + 10);
			add = one.divide(fact, numDigits + 10).quotient;
			e = e.add(add);
			//System.out.println(n + ": " + add.toStringE() );
//			System.out.println(n + ": " + fact.toStringF() + ", e=" + e.toStringF());
//			System.out.println(n + ": " + fact.toStringF() + ", add=" + add.toStringF() + ", rem=" + add1.remainder.toStringF());
//			System.out.println(n + ":  e=" + e.toStringF());
		}
		System.out.println("n=" + n);
		System.out.println("add=" + add.toStringE());
		System.out.println("stop=" + stop.toStringE());
		//System.out.println("compare=" + add.compareTo(stop));
		e = e.floor(numDigits + 1);
		System.out.println("e=" + e.toStringF());
	}
	
	Decimal bruteSplit(Decimal d)
	{
		System.out.println("brute split " + d.toStringF() + " at " + Utils.formatDateTime(new java.util.Date()));
		Decimal v = new Decimal(2);
		//Decimal v = new Decimal("9953296"); // 99542969
		int limit = d.getEPower() / 2 + 1;
		int ep = 0;
		int prevEp = 0;
		long countStep = 0;
		long countStepNext = (long)1e7;
		Utils.StopWatch sw = new Utils.StopWatch();
		sw.start();
		while ((ep = v.getEPower()) < limit)
		{
			countStep++;
			if (prevEp != ep) // || countStep > countStepNext)
			{
				System.out.println("ep=" + ep + ", limit=" + limit + ", step=" + countStep + ", elapsed=" + sw + ", v=" + v.toString());
				prevEp = ep;
				if (countStep > countStepNext)
				{
					countStepNext += 1e6;
				}
			}
			Decimal.Division test = d.divide(v, 0);
			//if (v.toLong() > 1.2e7) //(99542969 - 10))// && v.toLong() < (99542969 + 10))
			//{
			//System.out.println("v=" + v.toString() + ", remainder=" + test.remainder.toString());
			//}
			if (test.remainder.isZero())
			{
				System.out.println("found " + v.toStringF()+ ", elapsed=" + sw);
				return v;
			}
			//v = v.addSmall(1);
			v = v.addOne();
			if (v.toLong() > 99542969 + 1)
			{
				System.out.println("missed!");
				return Decimal.ZERO;
			}
		}
		return v;
	}

	Decimal counterSplit(Decimal d)
	{
		System.out.println("counter split " + d.toStringF() + " at " + Utils.formatDateTime(new java.util.Date()));
		int maxCounter = 100;
		int counterPos = 0;
		long[] counterBins = new long[maxCounter];
		long[] counterVals = new long[maxCounter]; 
		
		Decimal v = new Decimal(2);
		int limit = d.getEPower() / 2 + 1;
		int ep = 0;
		int prevEp = 0;
		int countTest = 0;
		boolean skipIt = false;
		long counterB = 0;
		//Decimal.Division test;
		Utils.StopWatch sw = new Utils.StopWatch();
		sw.start();
		while ((ep = v.getEPower()) < limit)
		{
			if (prevEp != ep)
			{
				//java.util.Date swd = sw.getElapsedTime();
				//System.out.println(Utils.formatDateTime(new java.util.Date()) + " / " + Utils.formatDateTime(swd));
				System.out.println("ep=" + ep + ", limit=" + limit + ", elapsed=" + sw);
				prevEp = ep;
			}
			// advance counters
			skipIt = false;
			for (int c = 0; c < counterPos; c++)
			{
				if (++counterBins[c] >= counterVals[c]) 
				{
					counterBins[c] = 0;
					skipIt = true;
				}
			}
			if (skipIt) 
			{
			}
			else
			{
				if (counterPos < maxCounter)
				{
					counterVals[counterPos++] = v.toLong();
				}
				countTest++;
				//System.out.println("test " + countTest + " for " + v.toLong());
				Decimal.Division test = d.divide(v, 0);
				if (test.remainder.isZero())
				{
					System.out.println("found " + v.toStringF() + ", counterPos=" + counterPos + ", tests=" + countTest + ", elapsed=" + sw);
					return v;
				}
			}
			//v = v.addSmall(1);
			v = v.addOne();
		}
		sw.stop();
		System.out.println("total tests " + countTest + " , last v=" + v.toStringF());
		System.out.println("end at " + Utils.formatDateTime(sw.getStopTime()));
		System.out.println("total elapsed=" + sw);
		return v;
	}
	
	Decimal counterSplit2(Decimal d)
	{
		System.out.println("counter split2 " + d.toStringF() + " at " + Utils.formatDateTime(new java.util.Date()));
		int maxCounter = 100;
		int counterPos = 0;
		long[] counterBins = new long[maxCounter];
		long[] counterVals = new long[maxCounter]; 
		
		Decimal v = new Decimal(2);
		int limit = d.getEPower() / 2 + 1;
		int ep = 0;
		int prevEp = 0;
		int countTest = 0;
		boolean skipIt = false;
		long counterB = 0;
		//Decimal.Division test;
		Utils.StopWatch sw = new Utils.StopWatch();
		sw.start();
		while ((ep = v.getEPower()) < limit)
		{
			if (prevEp != ep)
			{
				//java.util.Date swd = sw.getElapsedTime();
				//System.out.println(Utils.formatDateTime(new java.util.Date()) + " / " + Utils.formatDateTime(swd));
				System.out.println("ep=" + ep + ", limit=" + limit + ", elapsed=" + sw);
				prevEp = ep;
			}
			// advance counters
			skipIt = false;
			for (int c = 0; c < counterPos; c++)
			{
				if (--counterBins[c] == 0) 
				{
					counterBins[c] = counterVals[c];
					skipIt = true;
				}
			}
			if (skipIt) 
			{
				//System.out.println("skip v=" + v);
			}
			else
			{
				if (counterPos < maxCounter)
				{
					long val = v.toLong();
					counterVals[counterPos] = val;
					counterBins[counterPos++] = val;
				}
				countTest++;
				//System.out.println("test " + countTest + " for " + v.toLong());
				Decimal.Division test = d.divide(v, 0);
				if (test.remainder.isZero())
				{
					System.out.println("found " + v.toStringF() + ", counterPos=" + counterPos + ", tests=" + countTest + ", elapsed=" + sw);
					return v;
				}
			}
			//v = v.addSmall(1);
			v = v.addOne();
		}
		sw.stop();
		System.out.println("total tests " + countTest + " , last v=" + v.toStringF());
		System.out.println("end at " + Utils.formatDateTime(sw.getStopTime()));
		System.out.println("total elapsed=" + sw);
		return v;
	}

	/**
	 * This split uses diff.divide() to reduce the number of cycles, and it is slow
	 */
	Decimal stepSplit(Decimal d)
	{
		System.out.println("stepSplit " + d + " at " + Utils.formatDateTime(new java.util.Date())); 
		if (d.isZero()) return Decimal.ZERO;
		Decimal d0 = d.isPositive()? d : d.minus();
		Decimal dMinus = d.isPositive()? d.minus() : d;
		
		Decimal d1 = Decimal.TWO;
		Decimal d2 = d0.divide(d1, 0).quotient; // d2 is always greater than d1
		Decimal mult = d1.multiply(d2);
		Decimal mOne = Decimal.ONE.minus();
		
		Utils.StopWatch sw = new Utils.StopWatch();
		sw.start();
		int ep = d1.getEPower();
		int prevEp = ep;
		long stepCount = 0;
		do
		{
			stepCount++;
			ep = d1.getEPower();
			Decimal diff = mult.add(dMinus);
			//System.out.println("d1=" + d1.toString() + ", d2=" + d2.toString() + ", mult=" + mult.toString() + ", diff=" + diff);
			if (prevEp != ep)
			{
				java.util.Date swd = sw.getElapsedTime();
				//System.out.println(Utils.formatDateTime(new java.util.Date()) + " / " + Utils.formatDateTime(swd));
				//System.out.println("ep=" + ep + ", elapsed=" + sw + ", steps=" + stepCount + ", divSteps=" + stepDivCount + ", share=" + Utils.round((double)stepDivCount / (double)stepCount, 2));
				System.out.println("ep=" + ep + ", elapsed=" + sw + ", steps=" + stepCount + ", d2=" + d2.toStringF());
				//System.out.println("ep=" + ep + ", limit=" + limit + ", elapsed=" + sw);
				prevEp = ep;
			}
			
			//int comp = mult.compareTest(d0);			
			if (diff.isZero())
			{
				System.out.println("return=" + d1 + ", elapsed=" + sw + ", steps=" + stepCount);
				return d1;
			}
			if (d1.compareTest(d2) >= 0) return Decimal.ZERO;
			
			if (diff.isPositive())
			{
				Decimal.Division divdiv = diff.divide(d1, 0);
				if (divdiv.quotient.isZero())
				{
					d2 = d2.add(mOne);
					mult = mult.add(d1.minus());
				}
				else
				{
					d2 = d2.add(divdiv.quotient.minus()); //.add(mOne)
					// this is the same as 
					// mult = mult.add(d1.multiply(divdiv.quotient).minus()).add(d1.minus());
					mult = mult.add(diff.minus());
					if (!divdiv.remainder.isZero())
					{
						mult = mult.add(divdiv.remainder).add(d1.minus());
						d2 = d2.add(mOne);
					}
					// mult = mult.add(diff.minus()).add(divdiv.remainder); // .add(d1.minus());
				}
			}
			else
			{
				d1 = d1.addOne();
				mult = mult.add(d2);
			}
			if (d1.toLong() > 99542969 + 1)
			{
				System.out.println("missed! d=" + d0);
				return Decimal.ZERO;
			}
		} while (true);
		
		//return Decimal.ZERO;
	}
	
	/** This step split always advances by ones, no dividing. 
	 * For larger numbers it never ends
	 * */
	Decimal stepSplitOne(Decimal d)
	{
		System.out.println("stepSplitOne " + d + " at " + Utils.formatDateTime(new java.util.Date())); 
		if (d.isZero()) return Decimal.ZERO;
		Decimal d0 = d.isPositive()? d : d.minus();
		
		Decimal d1 = Decimal.TWO;
		Decimal d2 = d0.divide(Decimal.TWO, 0).quotient; // d2 is always greater than d1
		Decimal mult = d1.multiply(d2);
		Decimal mOne = Decimal.ONE.minus();

		int ep = d1.getEPower();
		int prevEp = ep;
		long stepCount = 0;
		long stepCountNext = 100000000;
		Utils.StopWatch sw = new Utils.StopWatch();
		sw.start();

		do
		{
			ep = d1.getEPower();
			stepCount++;
			if (prevEp != ep || stepCount > stepCountNext)
			{
				java.util.Date swd = sw.getElapsedTime();
				//System.out.println(Utils.formatDateTime(new java.util.Date()) + " / " + Utils.formatDateTime(swd));
				//System.out.println("ep=" + ep + ", elapsed=" + sw + ", steps=" + stepCount + ", divSteps=" + stepDivCount + ", share=" + Utils.round((double)stepDivCount / (double)stepCount, 2));
				System.out.println("ep=" + ep + ", elapsed=" + sw + ", steps=" + stepCount + ", d2=" + d2.toStringF());
				//System.out.println("ep=" + ep + ", limit=" + limit + ", elapsed=" + sw);
				prevEp = ep;
				if (stepCount > stepCountNext)
				{
					stepCountNext += 100000000;
				}
			}
			//System.out.println("d1=" + d1.toString() + ", d2=" + d2.toString() + ", mult=" + mult.toString() + ", true mult=" + d1.multiply(d2));
			//System.out.println("d1=" + d1.toString() + ", d2=" + d2.toString() + ", mult=" + mult.toString() + ", diff=" + d0.add(mult.minus()));
			int comp = mult.compareTest(d0);
			if (comp == 0)
			{
				System.out.println("return=" + d1 + ", elapsed=" + sw + ", steps=" + stepCount);
				return d1;
			}
			if (d1.compareTest(d2) >= 0) 
			{
				System.out.println("returning, " + d0 + " is a prime");
				return Decimal.ZERO;
			}
			
			if (comp > 0)
			{
				d2 = d2.add(mOne);
				mult = mult.add(d1.minus());
			}
			else
			{
				d1 = d1.addOne();
				mult = mult.add(d2);
			}
			if (d1.toLong() > 99542969 + 1)
			{
				System.out.println("missed! d=" + d0);
				return Decimal.ZERO;
			}
		} while (true);
		
		//return Decimal.ZERO;
	}
	
	/** the same as stepSplitOne, but starting from the middle, from sqrt(d) */
	Decimal stepSplitOneMid(Decimal d)
	{
		System.out.println("stepSplitOne " + d + " at " + Utils.formatDateTime(new java.util.Date())); 
		if (d.isZero()) return Decimal.ZERO;
		Decimal d0 = d.isPositive()? d : d.minus();
		
		Decimal d1 = new DecimalLg(d.getPrecision()).sqrt(d);
		d1 = d1.floor(); //d1 always goes up
		Decimal d2 = d1; //d0.divide(Decimal.TWO, 0).quotient; // d2 always goes down 
		// d1 is always greater than d
		Decimal mult = d1.multiply(d2);
		Decimal mOne = Decimal.ONE.minus();

		int ep = d1.getEPower();
		int prevEp = ep;
		long stepCount = 0;
		long stepCountNext = 100000000;
		Utils.StopWatch sw = new Utils.StopWatch();
		sw.start();

		do
		{
			ep = d1.getEPower();
			stepCount++;
			if (prevEp != ep || stepCount > stepCountNext)
			{
				java.util.Date swd = sw.getElapsedTime();
				//System.out.println(Utils.formatDateTime(new java.util.Date()) + " / " + Utils.formatDateTime(swd));
				//System.out.println("ep=" + ep + ", elapsed=" + sw + ", steps=" + stepCount + ", divSteps=" + stepDivCount + ", share=" + Utils.round((double)stepDivCount / (double)stepCount, 2));
				System.out.println("ep=" + ep + ", elapsed=" + sw + ", steps=" + stepCount + ", d1=" + d1 + ", d2=" + d2.toStringF());
				//System.out.println("ep=" + ep + ", limit=" + limit + ", elapsed=" + sw);
				prevEp = ep;
				if (stepCount > stepCountNext)
				{
					stepCountNext += 100000000;
				}
			}
			//System.out.println("d1=" + d1.toString() + ", d2=" + d2.toString() + ", mult=" + mult.toString() + ", true mult=" + d1.multiply(d2));
			//System.out.println("d1=" + d1.toString() + ", d2=" + d2.toString() + ", mult=" + mult.toString() + ", diff=" + d0.add(mult.minus()));
			int comp = mult.compareTest(d0);
			if (comp == 0)
			{
				System.out.println("return=" + d1 + ", elapsed=" + sw + ", steps=" + stepCount);
				return d1;
			}
			if (d2.getEPower() <= 0) 
			{
				System.out.println("returning, " + d0 + " is a prime");
				return Decimal.ZERO;
			}
			
			if (comp > 0)
			{
				d2 = d2.add(mOne);
				mult = mult.add(d1.minus());
			}
			else
			{
				d1 = d1.addOne();
				mult = mult.add(d2);
			}
			/*
			if (d1.toLong() > 99542969 + 1)
			{
				System.out.println("missed! d=" + d0);
				return Decimal.ZERO;
			}
			*/
		} while (true);
		
		//return Decimal.ZERO;
	}

	Decimal stepSplitLab(Decimal d)
	{
		System.out.println("stepSplit " + d + " at " + Utils.formatDateTime(new java.util.Date())); 
		if (d.isZero()) return Decimal.ZERO;
		Decimal d0 = d.isPositive()? d : d.minus();
		
		Decimal d1 = Decimal.TWO;
		Decimal d2 = d0.divide(Decimal.TWO, 0).quotient; // d2 is always greater than d1
		Decimal mult = d1.multiply(d2);
		Decimal mOne = Decimal.ONE.minus();

		int ep = d1.getEPower();
		int prevEp = ep;
		long stepCount = 0;
		long stepCountNext = 100000000;
		int stepDivCount = 0;
		Utils.StopWatch sw = new Utils.StopWatch();
		sw.start();

		do
		{
			ep = d1.getEPower();
			stepCount++;
			if (prevEp != ep || stepCount > stepCountNext)
			{
				java.util.Date swd = sw.getElapsedTime();
				//System.out.println(Utils.formatDateTime(new java.util.Date()) + " / " + Utils.formatDateTime(swd));
				//System.out.println("ep=" + ep + ", elapsed=" + sw + ", steps=" + stepCount + ", divSteps=" + stepDivCount + ", share=" + Utils.round((double)stepDivCount / (double)stepCount, 2));
				System.out.println("ep=" + ep + ", elapsed=" + sw + ", steps=" + stepCount + ", d2=" + d2.toStringF());
				//System.out.println("ep=" + ep + ", limit=" + limit + ", elapsed=" + sw);
				prevEp = ep;
				if (stepCount > stepCountNext)
				{
					stepCountNext += 100000000;
				}
			}
			//System.out.println("d1=" + d1.toString() + ", d2=" + d2.toString() + ", mult=" + mult.toString() + ", true mult=" + d1.multiply(d2));
			//System.out.println("d1=" + d1.toString() + ", d2=" + d2.toString() + ", mult=" + mult.toString() + ", diff=" + d0.add(mult.minus()));
			int comp = mult.compareTest(d0);
			//if (diff.isZero())
			if (comp == 0)
			{
				System.out.println("return=" + d1 + ", elapsed=" + sw + ", steps=" + stepCount + ", divSteps=" + stepDivCount + ", share=" + Utils.round((double)stepDivCount / (double)stepCount, 2));
				return d1;
			}
			if (d1.compareTest(d2) >= 0) return Decimal.ZERO;
			
			if (comp > 0)
			{
				d2 = d2.add(mOne);
				mult = mult.add(d1.minus());
				//d1 = d1.addOne();
				//mult = mult.add(d2);
			}
			/*if (comp > 0)
			//if (diff.isPositive()) 
			{
				Decimal diff = mult.add(d0.minus());
				
				if (diff.getEPower() > d1.getEPower())
				{
					int divdigits = 3 - diff.getEPower() + d1.getEPower();
					Decimal div1 = diff.divide(d1,0).quotient.minus();
					Decimal div = diff.divide(d1, divdigits > 0? 0 : divdigits).quotient.minus();
					if (d1.toLong() > (99542969 - 10))
					{
						System.out.println("d1=" + d1.toString() + ", d2=" + d2.toString() + ", mult=" + mult.toString() + ", diff=" + d0.add(mult.minus()));
						System.out.println("reduce d2: diff=" + diff.toStringE() + ", div=" + div.toStringE() + ", div1=" + div1.toStringE());
					}
					if (div.isZero())
					{
						System.out.println("reduce error, dif=0! d2: diff=" + diff.toStringE() + ", div=" + div.toStringE()); // + ", div1=" + div1.toStringE());
						return Decimal.ZERO;
					}
					d2 = d2.add(div);
					mult = mult.add(d1.multiply(div));
					stepDivCount++;
				}
				else
				{
					d2 = d2.add(mOne);
					mult = mult.add(d1.minus());
				}			
			}*/
			else
			{
				d1 = d1.addOne();
				mult = mult.add(d2);
			}
			if (d1.toLong() > 99542969 + 1)
			{
				System.out.println("missed! d=" + d0);
				return Decimal.ZERO;
			}
		} while (true);
		
		//return Decimal.ZERO;
	}

	/** step split + counter */
	Decimal stepSplitCombo(Decimal d)
	{
		System.out.println("stepSplitCombo " + d + " at " + Utils.formatDateTime(new java.util.Date())); 
		if (d.isZero()) return Decimal.ZERO;
		Decimal d0 = d.isPositive()? d : d.minus();
		Decimal dMinus = d.isPositive()? d.minus() : d;
		
		Decimal d1 = Decimal.TWO;
		Decimal d2 = d0.divide(d1, 0).quotient; // d2 is always greater than d1
		Decimal mult = d1.multiply(d2);
		Decimal mOne = Decimal.ONE.minus();
		
		int maxCounter = 100;
		int counterPos = 0;
		long[] counterBins = new long[maxCounter];
		long[] counterVals = new long[maxCounter]; 
		
		counterVals[counterPos] = d1.toLong();
		counterBins[counterPos++] = d1.toLong();
		Utils.StopWatch sw = new Utils.StopWatch();
		sw.start();
		int ep = d1.getEPower();
		int prevEp = ep;
		long stepCount = 0;
		do
		{
			stepCount++;
			ep = d1.getEPower();
			Decimal diff = mult.add(dMinus);
			//System.out.println("d1=" + d1.toString() + ", d2=" + d2.toString() + ", mult=" + mult.toString() + ", diff=" + diff);
			if (prevEp != ep)
			{
				java.util.Date swd = sw.getElapsedTime();
				System.out.println("ep=" + ep + ", elapsed=" + sw + ", steps=" + stepCount + ", d2=" + d2.toStringF());
				prevEp = ep;
			}
			
			if (diff.isZero())
			{
				System.out.println("return=" + d1 + ", elapsed=" + sw + ", steps=" + stepCount);
				return d1;
			}
			if (d1.compareTest(d2) >= 0) return Decimal.ZERO;
			
			if (diff.isPositive())
			{
				Decimal.Division divdiv = diff.divide(d1, 0);
				if (divdiv.quotient.isZero())
				{
					d2 = d2.add(mOne);
					mult = mult.add(d1.minus());
				}
				else
				{
					d2 = d2.add(divdiv.quotient.minus()); //.add(mOne)
					// this is the same as 
					// mult = mult.add(d1.multiply(divdiv.quotient).minus()).add(d1.minus());
					mult = mult.add(diff.minus());
					if (!divdiv.remainder.isZero())
					{
						mult = mult.add(divdiv.remainder).add(d1.minus());
						d2 = d2.add(mOne);
					}
					// mult = mult.add(diff.minus()).add(divdiv.remainder); // .add(d1.minus());
				}
			}
			else // advance d1
			{
				// advance counters
				boolean skipIt = false;
				do
				{
					skipIt = false;
					d1 = d1.addOne();
					mult = mult.add(d2);
					for (int c = 0; c < counterPos; c++)
					{
						if (--counterBins[c] == 0) 
						{
							counterBins[c] = counterVals[c];
							skipIt = true;
						}
					}
					if (skipIt) 
					{
						//System.out.println("skipped d1=" + d1);
					}
					else
					{
					}
				} while (skipIt);
				if (counterPos < maxCounter)
				{
					long val = d1.toLong();
					counterBins[counterPos] = val;
					counterVals[counterPos++] = val;
				}
			}
			/*
			if (d1.toLong() > 147226901 + 1)
			{
				System.out.println("missed! d=" + d0);
				return Decimal.ZERO;
			}*/
		} while (true);
		
		//return Decimal.ZERO;
	}
	
	@Test
	void testFloor()
	{
		Decimal d;
		d = new Decimal("123456");
		System.out.println("floor of " + d.toStringF() + " is " + d.floor());
		d = new Decimal("123.456");
		System.out.println("floor of " + d.toStringF() + " is " + d.floor());
		d = new Decimal("123456e8");
		System.out.println("floor of " + d.toStringF() + " is " + d.floor());
		d = new Decimal("123456e-10");
		System.out.println("floor of " + d.toStringF() + " is " + d.floor());
		Assertions.assertEquals(d.toLong(), 17);
	}
	//@Test
	void testSplit()
	{
		//bruteSplit(new Decimal("6367415506067116157")); //Utils.random.nextLong()));
		//counterSplit(new Decimal("63674155060671"));		
		//counterSplit(new Decimal("63674155060673"));		
		//counterSplit(new Decimal("6367415506067116157"));		
		//counterSplit(new Decimal("63674155060671161576367415506067116157"));
		
		Decimal d1 = new Decimal("63674155060673");
		//d1 = d1.multiply(new Decimal("231241"));
		//d1 = d1.multiply(new Decimal("7814381")); // step: 45 sec, counter: 6 sec		
		//d1 = d1.multiply(new Decimal("99542969")); // step: 546 sec, counter: 81 sec
		//d1 = new Decimal("17").multiplySmall(19); // step: 546 sec, counter: 81 sec
		//d1 = new Decimal("99542969").multiply(new Decimal("147226901")); // length 9
		//d1 = d1.multiply(new Decimal("147226901")); // length 9
		//d1 = d1.multiply(new Decimal("6764430599")); // length 10		
		d1 = d1.multiply(new Decimal("32066729531")); // length 11
		//Decimal res = stepSplit(d1); // + (17 *41))); //1.778 sec
		//System.out.println("stepSplit: " + res);
		//stepSplitCombo(d1);
		//counterSplit2(d1);
		stepSplitOneMid(d1);
		//stepSplitOne(new Decimal("" + (17 * 43)));
		//stepSplitOne(d1);
		//bruteSplit(d1);
		
		//counterSplit(new Decimal(Utils.random.nextLong()));
		//counterSplit(new Decimal((593987) + ""));
		// 397971290 : 2, 5, 67
		
		
		// test 2565438975860849476 (2e18) - not finished
		// next try 308363006597831213 (3e17) - completed in 5900 sec (1h 40 min)
	}
	
	//@Test
	void testSqrt()
	{
		Decimal d1 = new Decimal("3.23456e9");
		DecimalLg lg = new DecimalLg(23);
		Decimal v1 = lg.sqrt(d1);
		System.out.println("testSqrt: from " + d1.toStringF() + ", true=" + Math.sqrt(d1.toDouble()));
	}
	
	//@Test
	void testAddOne()
	{
		Decimal v = Decimal.ZERO;
		for (int i = 1; i <= 1000; i++)
		{
			v = v.addOne();
			System.out.println(i + ": " + v.toStringF() + ", " + v.toLong());
		}
	}
	
	//@Test
	void testConvert()
	{
		Decimal d1 = new Decimal("1.234567");
		Decimal r1 = d1.round(5);
		System.out.println("testConvert/round from " + d1 + " round5 " + r1);
		Assertions.assertEquals("1.2346", r1.toStringE());
	}
	
	//@Test
	void testPrint()
	{
		Decimal d1 = new Decimal("1.234567");
		System.out.println("testPrint/F: " + d1.toStringE() + " printF: " + d1.toStringF());
		Assertions.assertEquals("1.234567", d1.toStringF());

		Decimal d2 = new Decimal("1234.567");
		System.out.println("testPrint/F: " + d2.toStringE() + " printF: " + d2.toStringF());
		Assertions.assertEquals("1234.567", d2.toStringF());
	
		Decimal d3 = new Decimal("1.234567e8");
		System.out.println("testPrint/F: " + d3.toStringE() + " printF: " + d3.toStringF());
		Assertions.assertEquals("123456700", d3.toStringF());
			
		Decimal d4 = new Decimal("1.234567e-3");
		String r4 = d4.toStringF();
		System.out.println("testPrint/F: " + d4.toStringE() + " printF: " + r4);
		Assertions.assertEquals("0.001234567", r4);

		Decimal d5 = new Decimal("1.234567e6");
		String r5 = d5.toStringF();
		System.out.println("testPrint/F: " + d5.toStringE() + " printF: " + r5);
		Assertions.assertEquals("1234567", r5);

		Decimal d6 = new Decimal("1.234567e-1");
		String r6 = d6.toStringF();
		System.out.println("testPrint/F: " + d6.toStringE() + " printF: " + r6);
		Assertions.assertEquals("0.1234567", r6);
		
		long d7 = 1234567;
		String r7 = Decimal.longToString(d7, 10, 10);
		String r7_1 = Decimal.longToString(d7, 10, 16);
		System.out.println("testPrint/Serial: " + d7 + " toString10: " + r7 + ", toString16: " + r7_1);
		Assertions.assertEquals("+0001234567", r7);
		Assertions.assertEquals("+000012D687", r7_1);
		
			
	}
	
}
