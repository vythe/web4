package net.vbelev.utils.test;

//import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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

	@Test
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
		System.out.println("testMultiply: " + t1.toString() + " * " + factor + " = " + r1.toString());
		Assertions.assertEquals(t1.toLong() * factor, r1.toLong());

		Decimal t2 = new Decimal(-1788e-3, 6);
		int factor2 = 357;
		Decimal r2 = t2.multiplySmall(factor2);
		System.out.println("testMultiply: " + t2.toStringE() + " * " + factor + " = " + r2.toStringE());
		Assertions.assertEquals(t2.toDouble() * factor2, r2.toDouble());
		
		Decimal t3 = new Decimal(17);
		int add3 = 353;
		Decimal r3 = t3.addSmall(add3);
		
		System.out.println("testMultiply/addSmall: " + t3.toString() + " + " + add3 + " = " + r3.toString());
		Assertions.assertEquals(t3.toLong() + add3, r3.toLong());
		
		Decimal t4 = new Decimal(17000);
		int add4 = -55;
		Decimal r4 = t4.addSmall(add4);
		
		System.out.println("testMultiply/addSmall: " + t4.toString() + " + " + add4 + " = " + r4.toString());
		Assertions.assertEquals(t4.toLong() + add4, r4.toLong());
	}
	
	@Test
	void testCompare()
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
		
		Decimal d4 = new Decimal(1230);
		int v4 = 1234;
		int comp1 = d4.compareTo(v4);
		System.out.println("compare " + d4.toStringE() + " to " + v4 + " = " + comp1);
		Assertions.assertEquals(-1, comp1);
		
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

	@Test
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
	
	@Test
	void testAdd()
	{
		Decimal d1 = new Decimal("170");
		Decimal v1 = new Decimal("3.14");
		Decimal r1 = d1.add(v1);
		System.out.println("testAdd, d1=" + d1 + " add " + v1 + " = " + r1.toDouble());
		Assertions.assertEquals(170 + 3.14, r1.toDouble());
	}
	
	@Test
	void testConvert()
	{
		Decimal d1 = new Decimal("1.234567");
		Decimal r1 = d1.round(5);
		System.out.println("testConvert/round from " + d1 + " round5 " + r1);
		Assertions.assertEquals("1.2346", r1.toStringE());
	}
	
	@Test
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
