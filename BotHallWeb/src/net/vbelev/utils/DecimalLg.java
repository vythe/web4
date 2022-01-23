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
}
