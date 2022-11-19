package net.vbelev.utils.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import net.vbelev.utils.*;

public class DecimalLog10Test
{
	
	
	//@Test
	public void testLog()
	{
		double lg1 = 0;
		int end = 90000000;
		double top = 100000000;
		double step = 1. / top;
		double val = 1.0;
		for (int i = 0; i <= end; i++)
		{
			double lg2 = Math.log10(val);
			val += step;
			//double lg2 = Math.log10((double)i / top + 1.);
			if (lg1 < lg2) lg1 = lg2;
		}
		System.out.println("last log-1: " + lg1);
					
	}
	
	/** 
	 * val must be between 1 and 10.
	 * grid[i] = log10(i / 100  + 1.)
	 */
	private double gridLog(double[] grid, double val)
	{
		double arg = (val - 1.) * 100.;
		//double argFloor = Math.floor(arg);
		//int pos = (int)argFloor;
		int pos = (int)arg;
		//double argFloo
		double frac = arg - pos;
		return grid[pos] * (1 - frac) + grid[pos + 1] * frac;
	}

	//@Test
	public void testLog2()
	{
		double[] grid = new double[901];
		for (int i = 0; i <= 900; i++)
		{
			grid[i] = Math.log10((double)i / 100. + 1.);
		}
		/*
		for (int t = 0; t < 10; t++) 
		{
			double val = Math.random() * 9 + 1.;
			double lg1 = Math.log10(val);
			double lg2 = gridLog(grid, val);
			double val2 = Math.pow(10., lg2);
			System.out.println("val=" + val + ", diff=" + (lg2 - lg1) + ", val2=" + val2);
		}*/

		double lg1 = 0;
		int end = 90000000;
		double top = 100000000;
		double step = 1. / top;
		double val = 1.0;
		for (int i = 0; i <= end; i++)
		{
			//double lg2 = Math.log10((double)i / top + 1.);
			//double lg2 = gridLog(grid, (double)i / top + 1.);
			double lg2 = gridLog(grid, val);
			val += step;
			if (lg1 < lg2) lg1 = lg2;
		}
		System.out.println("last log-2: " + lg1);
					
	}

	public static class GridLog
	{
		public double[] grid;
		public double[] offset;
		public double[] grad;
		public int gridSize;
		public double step;
		public double step1;
		public GridLog(int size)
		{
			gridSize = size;
			grid = new double[size + 1];
			step = 9. / size;
			step1 = size / 9.;
			double val = 1.;
			for (int i = 0; i <= size; i++)
			{
				grid[i] = Math.log10(1. + i * step);
				val += step;
			}			
		}
		public double log(double val)
		{
			double arg = (val - 1.) * step1;
			int pos = (int)arg;
			double frac = arg - pos;
			return grid[pos] * (1 - frac) + grid[pos + 1] * frac;
		}		

		/** 
		 * log3 is never better than log
		 * @return
		 */
		public double log3(double val)
		{
			double arg = (val - 1.) * step1;
			int pos = (int)arg;
			double frac = arg - pos;
			if (pos == 0 || pos == grid.length - 1)
			{
				return grid[pos] * (1 - frac) + grid[pos + 1] * frac;
				//return r1;
			}

			double r1 = grid[pos] * (1 - frac) + grid[pos + 1] * frac;
			// frac -> frac + 1
			double r2 = grid[pos - 1] + (grid[pos] - grid[pos - 1]) * (frac + 1);
			// frac -> frac - 1
			double r3 = grid[pos + 1] + (grid[pos + 2] - grid[pos + 1]) * (frac - 1);

			return (r1 + r2 + r3) / 3.;
		}		
	}

	//@Test
	public void testLog3()
	{
		GridLog g = new GridLog(9000);
		
		for (int t = 0; t < 10; t++) 
		{
			double val = Math.random() * 9 + 1.;
			double lg1 = Math.log10(val);
			double lg2 = g.log(val);
			double lg3 = g.log3(val);
			//double val2 = Math.pow(10., lg2);
			//double val3 = Math.pow(10., lg2);
			System.out.println("val=" + val + ", diff=" + (lg2 - lg1) + ", diff3=" + (lg3 - lg1));
		}
		
		
		double lg1 = 0;
		int end = 90000000;
		double top = 100000000;
		double step = 1. / top;
		double val = 1.0;
		for (int i = 0; i <= end; i++)
		{
			//double lg2 = Math.log10((double)i / top + 1.);
			//double lg2 = gridLog(grid, (double)i / top + 1.);
			double lg2 = g.log(val);
			val += step;
			if (lg1 < lg2) lg1 = lg2;
		}
		System.out.println("last log-3: " + lg1);				
	}

	//@Test
	public void testLog4()
	{
		GridLog g = new GridLog(900);
		System.out.println("test log 4");
		for (int t = 0; t < 10; t++) 
		{
			double val = Math.random() * 9 + 1.;
			double lg1 = Math.log10(val);
			double lg2 = g.log(val);
			double lg3 = g.log3(val);
			//double val2 = Math.pow(10., lg2);
			//double val3 = Math.pow(10., lg2);
			System.out.println("val=" + val + ", diff=" + (lg2 - lg1) + ", diff3=" + (lg3 - lg1));
		}
		
		
		double lg1 = 0;
		double end = 90000000;
		double top = 100000000;
		double step = 1. / top;
		double val = 1.0;
		for (int i = 0; i <= end; i++)
		{
			//double lg2 = Math.log10((double)i / top + 1.);
			//double lg2 = gridLog(grid, (double)i / top + 1.);
			double lg2 = g.log3(val);
			val += step;
			if (lg1 < lg2) lg1 = lg2;
		}
		System.out.println("last log-4: " + lg1);				
	}	

	@Test
	public void rsaTestD() 
	{
		try
		{
			int keySize = 512;
		java.security.KeyPairGenerator kg = java.security.KeyPairGenerator.getInstance("RSA");
		kg.initialize(keySize);
		java.security.KeyPair key = kg.generateKeyPair();
		
		//sun.security.rsa.RSAPublicKeyImpl publicKey = (sun.security.rsa.RSAPublicKeyImpl)key.getPublic();
		sun.security.rsa.RSAPrivateCrtKeyImpl privateKey = (sun.security.rsa.RSAPrivateCrtKeyImpl)key.getPrivate();
		
		Decimal p = new Decimal(privateKey.getPrimeP().toString());
		Decimal q = new Decimal(privateKey.getPrimeQ().toString());
		Decimal e = new Decimal(privateKey.getPublicExponent().toString());
		Decimal d = DecimalLg.rsaPrivateExponent(p, q, e);
		Decimal keyD = new Decimal(privateKey.getPrivateExponent().toString());
		System.out.println("p=" + p);
		System.out.println("q=" + q);
		System.out.println("n len=" + p.multiply(q).toString().length());
		System.out.println("key D=" + privateKey.getPrivateExponent().toString());
		System.out.println("clc D=" + d.toString());

		//System.out.println("publicKey64=" + Utils.encodeBytes64(publicKey.encode()));
		//System.out.println("privateKey64=" + Utils.encodeBytes64(privateKey.encode()));
		}
		catch (Exception x)
		{
			System.out.println("crypto failed: " + x.getMessage());
		}
		
	}	
}
