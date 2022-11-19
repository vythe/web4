package net.vbelev.utils;

import net.vbelev.utils.*;

public class DecimalLg
{
	private int numDigits;
	private Decimal valueE = null;
	private Decimal valueEinv = null;
	/** ln(10) */
	private Decimal ln10 = null;
	private Decimal ln10inv = null;
	public DecimalLg()
	{
		numDigits = 100;
	}
			
	public DecimalLg(int numDigits)
	{
		this.numDigits = numDigits;
	}
	
	public int getNumDigits() { return numDigits; }
	
	public Decimal getE()
	{
		if (valueE == null)
		{
			init();
		}
		
		return valueE;			
	}
	
	private void init()
	{
		valueE = calculateE(numDigits);
		valueEinv = Decimal.ONE.divide(valueE, numDigits).quotient;
		ln10 = natural(new Decimal(10));
		ln10inv = Decimal.ONE.divide(ln10, numDigits).quotient;
	}
	
	/**
	 * Calculates ln(val).
	 * It works okay for values close to e, but the last few digits are wrong 
	 */
	public Decimal natural(Decimal val)
	{
		if (!val.isPositive() || val.isZero())
		{
			throw new ArithmeticException("Invalid value for a logarithm");
		}
		
		Decimal invPowerE = Decimal.ONE;
		Decimal powerE = Decimal.ONE;
		Decimal e = getE();
		int power = 0;
		if (val.compareTo(e) >= 0)
		{
			do
			{
				invPowerE = invPowerE.multiply(valueEinv);				
				powerE = powerE.multiply(valueE);				
				int cmp = powerE.compareTo(val);
				power++;
				if (cmp == 0) return new Decimal(power);
				if (cmp > 0) break;				
				
			} while (true);
		}
		else if (val.compareTo(DECIMAL_TWO) >= 0)
		{
			power = 1;
			powerE = e; //Decimal.ONE.divide(e, numDigits).quotient;
			invPowerE = valueEinv;
		}
		else if (val.compareTo(valueEinv) < 0)
		{
			power = -1;
			powerE = valueEinv; //Decimal.ONE.divide(e, numDigits).quotient;
			invPowerE = valueE;
		}
		else
		{
			power = 0;
			powerE = Decimal.ONE;
		}
		//Decimal.Division div = val.divide(powerE, numDigits);
		//Decimal remainder = div.quotient;
		Decimal remainder = val.multiply(invPowerE);
		//System.out.println("power=" + power + ", powerE=" + powerE.toStringF() + ", remainder=" + remainder.toStringF());
		//System.out.println("lost rem=" + remainder.toStringE());
		return this.naturalSmall(remainder).add(new Decimal(power)).floor(numDigits);
	}

	public Decimal log10(Decimal val)
	{
		if (!val.isPositive() || val.isZero())
		{
			throw new ArithmeticException("Invalid value for a logarithm");
		}
		char[] mantissa = val.getMantissa();
		Decimal core = new Decimal(mantissa, -mantissa.length, true); 
		Decimal ln = natural(core);
		Decimal l10 = ln.multiply(ln10inv);
		Decimal res = l10.add(new Decimal(1 + val.getEPower())).floor(numDigits);
		
		return res;
	}
	
// ==== misc statics ====
	public static Decimal calculateE(int numDigits)
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
		//int numDigits = 1000;
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
			fact = fact.multiplySmall(n);
			//Decimal.Division add1 = one.divide(fact, numDigits + 10);
			add = one.divide(fact, numDigits + 10).quotient;
			e = e.add(add);
		}
		//System.out.println("n=" + n);
		//System.out.println("add=" + add.toStringE());
		//System.out.println("stop=" + stop.toStringE());
		//System.out.println("compare=" + add.compareTo(stop));
		e = e.floor(numDigits + 1);
		//System.out.println("e=" + e.toStringF());
		return e;
	}	
	
	private static final Decimal DECIMAL_TWO = new Decimal("2");
	private static final Decimal DECIMAL_M_ONE = new Decimal("-1");
	
	private static final int NATURAL_SMALL_MAX_STEPS = 4000;
	private Decimal[] inverseIndex = new Decimal[NATURAL_SMALL_MAX_STEPS + 2];
	
	private Decimal getInverseIndex(int i)
	{
		Decimal res = inverseIndex[i];
		if (res == null)
		{
			res = Decimal.ONE.divide(new Decimal(i), numDigits + 4).quotient;
			inverseIndex[i] = res;
		}
		return res;
	}
	/**
	 * Calculates the natural logarithm ln(val) using the Taylor series.
	 * Works for 1 < val < 2.
	 * It is not particularly fast.
	 */
	public static Decimal naturalSmall(Decimal val, int numDigits)
	{
		DecimalLg lg = new DecimalLg(numDigits);
		return lg.naturalSmall(val);
	}
	public Decimal naturalSmall(Decimal val)
	{
		/*
		if (val == null || !val.isPositive() || val.compareTo(Decimal.ONE) <= 0 || val.compareTo(DECIMAL_TWO) >= 0)
		{
			throw new ArithmeticException("Value out of range (0, 1)");
		}
		*/
		Decimal s1 = val.add(DECIMAL_M_ONE);
		Decimal fac = DECIMAL_M_ONE;		
		Decimal res = Decimal.ZERO;
		int i = 1;
		//int maxSteps = 2000;
		int flr = numDigits + 4; //s1.getPrecision() * 2;
		Decimal.Division div = null;
		Decimal q = null;
		for (; i <= NATURAL_SMALL_MAX_STEPS; i++) 
		{
			fac = fac.minus().multiply(s1).floor(flr);
			//div = fac.divide(new Decimal(i), numDigits + 4); // we expect up to a 1000 of these additions (maxSteps), so we need 3 extra digits
			//q = div.quotient;
			q = fac.multiply(getInverseIndex(i)).floor(numDigits + 4);
			if (q.isZero())
				break;
			if (q.getEPower() < 0 && -q.getEPower() > numDigits + 1)
			{
				break;
			}
			res = res.add(q);
		}
		//System.out.println("NS break at i=" + i + ", div ePower=" + q.getEPower() + ", val=" + val.floor(4).toStringF()); 
		if (i > NATURAL_SMALL_MAX_STEPS)
		{
			throw new ArithmeticException("Failed to get " + numDigits + " digits in " + NATURAL_SMALL_MAX_STEPS + " steps");
		}
		return res;
	}
	
	/**
	 * A simpler version to use until we have power()
	 */
	public Decimal sqrt(Decimal d)
	{
		Decimal d1 = d.isPositive()? d : d.minus();
		int divideExtraDigits = numDigits - d1.getEPower();
		Decimal res = d1.divide(Decimal.TWO, divideExtraDigits).quotient;
		char[] mant = res.getMantissa();
		// the gap is roughly d1 * 10^(-numDigits)
		Decimal gap = new Decimal(new char[]{1}, d1.getEPower() - numDigits, true).minus();
		
		System.out.println("sqrt, d=" + d.toStringF() + ", gap=" + gap.toStringF());
		// starting value
		res = new Decimal(mant, res.getEPower() / 2 - mant.length + 1, true);
		Decimal prevRes = d1;
		int stepCount = 0;
		// newton's: x[k + 1] = (x[k] + d1 / x[k]) / 2
		while ((prevRes.add(gap).compareTest(res) > 0 || res.add(gap).compareTest(prevRes) > 0) && stepCount++ < numDigits * 10)
		{
			System.out.println("step " + stepCount + ", res=" + res.toStringF());
			prevRes = res;
			res = d1.divide(res, divideExtraDigits).quotient.add(res).divide(Decimal.TWO, divideExtraDigits).quotient;
		}
		
		System.out.println("sqrt returns " + res.toStringF() + ", reverse=" + res.multiply(res).toStringF());
		return res;
	}
	
	/**
	 * When calculating gcd(d1, d2), observe integer x1, x2 such that
	 * d1 * x1 + d2 * x2 = gcd(d1, d2). 
	 * Note that one of them is probably negative
	 */
	private static class gcdInternalData
	{
		public Decimal res;
//		public Decimal d2;
		public Decimal x1;
		public Decimal x2;
	}
	
	private static gcdInternalData gcdInternal(Decimal d1, Decimal d2)
	{
		gcdInternalData res = new gcdInternalData();
		if (d1.isZero()) 
		{
			res.res = d2;
			//res.d2 = d2;
			res.x1 = Decimal.ZERO;
			res.x2 = Decimal.ONE;
		}
		else
		{
			Decimal.Division dev = d2.divide(d1);
			gcdInternalData res2 = gcdInternal(dev.remainder, d1);
			res.res = res2.res;
			res.x2 = res2.x1; 
			res.x1 = res2.x2.add(dev.quotient.multiply(res2.x1).minus());
		}
		System.out.println("gdcInternal, d1=" + d1 + ", d2=" + d2);
		System.out.println("d1*x1 + d2 * x2=" + (d1.multiply(res.x1).add(d2.multiply(res.x2))));
		System.out.println("gdcInternal-d returns x1=" + res.x1 + ", x2=" + res.x2 + ", val=" + res.res);
		return res;
	}
	
	/**
	 * Greatest common divisor (with extended) as taken from https://e-maxx.ru/algo/extended_euclid_algorithm
	 */
	public static Decimal gcd(Decimal d1, Decimal d2)
	{
		if (d1.isZero() || d2.isZero()) return Decimal.ZERO;
		if (!d1.isPositive()) return gcd(d1.minus(), d2);
		if (!d2.isPositive()) return gcd(d1, d2.minus());
		
		gcdInternalData res = gcdInternal(d1, d2);
		
		return res.res;
	}
		
	/**
	 * returns x such that (x * d) mod m = 1 (or (x * d) % m = 1).
	 * it's the gcd(m, d) algorithm with some extras.
	 * 
	 * Inverse of d for mod m exists only if gcd(d, m) = 1.
	 */
	public static Decimal inverseModOne(Decimal d, Decimal m)
	{
		gcdInternalData res = gcdInternal(d, m);
		Decimal g = res.res; //gcd(d, m);
		//System.out.println("inverse mod, d=" + d + ", m=" + m + ", gcd=" + g);
		if (!g.equals(Decimal.ONE))
		{
			//System.out.println("gcd = " + g + ", returning zero");
			return Decimal.ZERO;
		}

		if (res.x1.isPositive())
			return res.x1;
		else
			return res.x1.add(m);
	}
	
	/**
	 * In RSA:
	 * p: a prime number;
	 * q: another prime number;
	 * n: modulus, n = p * q
	 * r aka phi(n): r = (p - 1) * (q - 1)
	 * e: public exponent, another (small) prime number. It's prime vs (p-1)(q-1), but basically a prime, and usualls 65537
	 * d: private exponent, calculated here, so that d*e mod r = 1.
	 * --
	 * public key includes e and n; private key includes d and n. 
	 */
	public static Decimal rsaPrivateExponent(Decimal p, Decimal q, Decimal e)
	{
		/*
		 * Calculate n = p × q

Calculate ϕ(n) = (p – 1) × (q – 1)

Select integer e with gcd(ϕ(n), e) = 1; 1 < e < ϕ(n)

Calculate d where (d × e) mod ϕ(n) = 1

The the public key file should have: {e, n} and the private key file should haveL {d, n} 
		 */
		Decimal n = p.multiply(q);
		Decimal r = p.addSmall(-1).multiply(q.addSmall(-1));
		//Decimal e = new Decimal("65537");
		Decimal d = DecimalLg.inverseModOne(e, r);
		return d;
	}

}
