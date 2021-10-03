package net.vbelev.utils.test;

//import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import net.vbelev.utils.*;

class DecimalTest
{
	//@Test
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
	void testMultiply()
	{
		Decimal t1 = new Decimal(-1788);
		int factor = 357;
		Decimal r1 = t1.multiplySmall(factor);
		System.out.println("testMultiply small: " + t1.toString() + " * " + factor + " = " + r1.toString());
		Assertions.assertEquals(t1.toLong() * factor, r1.toLong());

		Decimal t2 = new Decimal(-1788e-3, 6);
		int factor2 = 357;
		Decimal r2 = t2.multiplySmall(factor2);
		System.out.println("testMultiply small: " + t2.toStringE() + " * " + factor + " = " + r2.toStringE());
		Assertions.assertEquals(t2.toDouble() * factor2, r2.toDouble());
		
		
		Decimal t3 = new Decimal("171.34"); //Utils.random.nextInt());
		Decimal f3 = new Decimal("-21602"); //Utils.random.nextInt());
		Decimal r3 = t3.multiply(f3);
		
		System.out.println("testMultiply large: " + t3.toStringF() + " * " + f3.toStringF() + " = " + r3.toStringF());
		System.out.println("true=" + (t3.toDouble() * f3.toDouble()));
		Assertions.assertEquals(t3.toDouble() * f3.toDouble(), r3.toDouble());
		
		/*
		Decimal t4 = new Decimal(17000);
		int add4 = -55;
		Decimal r4 = t4.addSmall(add4);
		
		System.out.println("testMultiply/addSmall: " + t4.toString() + " + " + add4 + " = " + r4.toString());
		Assertions.assertEquals(t4.toLong() + add4, r4.toLong());
		*/
	}
	
	@Test
	void testDivide()
	{
		//Decimal d1 = new Decimal("" + (int)(17 * 1.25));
		Decimal d1 = new Decimal("2100"); // + (int)(2100));
		Decimal d2 = new Decimal("17");
		//Decimal d1 = new Decimal("986707205");
		//Decimal d2 = new Decimal("982118665");
		//Decimal d1 = new Decimal(Utils.random.nextInt());
		//Decimal d2 = new Decimal(Utils.random.nextInt());
		System.out.println("d1=" + d1.toStringF() + ", d2=" + d2.toStringF());
		Decimal.Division div = d1.divide(d2, 2);
		long quot = (long)(d1.toDouble() / d2.toDouble());
		//long rem = d1.toLong() % d2.toLong();
		long rem = (long)d1.toDouble() - quot * (long)d2.toDouble();
		System.out.println("res quotient=" + div.quotient.toStringF() + ", remainder=" + div.remainder.toStringF());
		System.out.println("tru quotient=" + quot + ", remainder=" + rem);
		
		
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

	//@Test
	void testStressMult()
	{
		int tt = 99999999;
		Decimal d1 = new Decimal(Utils.random.nextInt());
		Decimal d2 = new Decimal(Utils.random.nextInt());
		System.out.println("repeat mult: " + d1.toStringF() + " * " + d2.toStringF() + " = " + d1.multiply(d2).toStringF());
		for (int i = 0; i < 1e8; i++) // for 2e8 this is 129 sec
		{
			//long v1 = Utils.random.nextInt();
			//long v2 = Utils.random.nextInt();
			//Decimal res = new Decimal(v1).multiply(new Decimal(v2));
			Decimal res = d1.multiply(d2);
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
