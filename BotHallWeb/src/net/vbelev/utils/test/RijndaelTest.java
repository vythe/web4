package net.vbelev.utils.test;

import java.nio.charset.StandardCharsets;

//import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import net.vbelev.utils.*;


import java.sql.*;
import java.util.ArrayList;
import java.util.Enumeration;

class RijndaelTest
{

	//@Test
	void test()
	{
		String val1 = "testing"; //"My test string";
		RijndaelCrypt c = new RijndaelCrypt("The quick brown fox jumps", "over the lazy dog.");
		String encrypted = c.encrypt(val1.getBytes());
		String decrypted = c.decrypt(encrypted);
		System.out.println("val1=" + val1 + ", encrypted=" + encrypted);
		Assertions.assertEquals(val1, decrypted);
	}

	//@Test
	void crypto()
	{
		try
		{
		java.security.KeyPairGenerator kg = java.security.KeyPairGenerator.getInstance("RSA");
		
		kg.initialize(512);
		java.security.KeyPair key = kg.generateKeyPair();
		
		sun.security.rsa.RSAPublicKeyImpl publicKey = (sun.security.rsa.RSAPublicKeyImpl)key.getPublic();
		sun.security.rsa.RSAPrivateCrtKeyImpl privateKey = (sun.security.rsa.RSAPrivateCrtKeyImpl)key.getPrivate();
		
		System.out.println("key=" + privateKey.toString());
		

		System.out.println("publicKey64=" + Utils.encodeBytes64(publicKey.encode()));
		System.out.println("privateKey64=" + Utils.encodeBytes64(privateKey.encode()));
		}
		catch (Exception x)
		{
			System.out.println("crypto failed: " + x.getMessage());
		}
	}

	/**
	 * d1 and d2 positive
	 */
	private Decimal gcdInternal(Decimal d1, Decimal d2)
	{
		//System.out.println("gcdInternal: " + d1 + ", " + d2);
		Decimal.Division div = d1.divide(d2, 0);
		if (div.remainder.isZero()) return d2;
		
		return gcdInternal(d2, div.remainder);
	}
	
	public Decimal gcd (Decimal d1, Decimal d2)
	{
		if (d1.isZero() || d2.isZero()) return Decimal.ZERO;
		if (!d1.isPositive()) return gcd(d1.minus(), d2);
		if (!d2.isPositive()) return gcd(d1, d2.minus());

		return gcdInternal(d1, d2);
	}

	/**
	 * When calculating gcd(d1, d2), observe integer x1, x2 such that
	 * d1 * x1 + d2 * x2 = gcd(d1, d1). 
	 * Note that one of them is probably negative
	 */
	private static class gcdInternalData
	{
		public Decimal res;
//		public Decimal d2;
		public Decimal x1;
		public Decimal x2;
	}
	
	private gcdInternalData gcdInternal1(Decimal d1, Decimal d2)
	{
		gcdInternalData res = new gcdInternalData();
		if (d2.isZero()) 
		{
			res.res = d1;
			//res.d2 = d2;
			res.x1 = Decimal.ONE;
			res.x2 = Decimal.ZERO;
			return res;
		}

		Decimal.Division dev = d1.divide(d2);
		gcdInternalData res2 = gcdInternal1(d2, dev.remainder);
		res.res = res2.res;
		//res.x1 = res2.x2; 
		//res.x2 = res2.x1.add(dev.quotient.multiply(res2.x2).minus());
		res.x2 = res2.x1; 
		res.x1 = res2.x2.add(dev.quotient.multiply(res2.x1).minus());
		return res;
	}
	
	public Decimal gcd1(Decimal d1, Decimal d2)
	{
		if (d1.isZero() || d2.isZero()) return Decimal.ZERO;
		if (!d1.isPositive()) return gcd1(d1.minus(), d2);
		if (!d2.isPositive()) return gcd1(d1, d2.minus());
		
		gcdInternalData res = gcdInternal1(d1, d2);
		
		return res.res;
	}
	
	/** 
	 * inverse Euclid: p0 = 0, p1 = 1, p(n) = [p(n-2) - p(n-1) * q(n-2)] mod d1
	 * q(n) is the quotient, p(n) is a special value 
	 * n: d1 = d2 * q(n) + rem(n)
	 */
	public Decimal gcdInternalExt(Decimal d1, Decimal d2, Decimal m, Decimal pPrev, Decimal qPrev, Decimal pPrev2, Decimal qPrev2)
	{
		Decimal.Division div = d1.divide(d2, 0);
		
		Decimal p = pPrev2.add(pPrev.multiply(qPrev2).minus());
		p = p.divide(m, 0).remainder;
		if (!p.isPositive())
		{
			p = p.add(m);
		}

		System.out.println("gcdInternalExt: " + d1 + ", " + d2 + ", p=" + p + ", q=" + div.quotient);

		if (div.remainder.isZero()) 
		{
			// one more calculation of p
			Decimal pFinal = pPrev.add(p.multiply(qPrev).minus());
			pFinal = pFinal.divide(m, 0).remainder;
			if (!pFinal.isPositive())
			{
				pFinal = pFinal.add(m);
			}
			System.out.println("gcdInternalExt return " + pFinal);
			return pFinal;
		}

		return gcdInternalExt(d2, div.remainder, m, p, div.quotient, pPrev, qPrev);
	}
	
	/**
	 * returns x such that (x * d) mod m = 1 (or (x * d) % m = 1).
	 * it's the gcd(m, d) algorithm with some extras.
	 * 
	 * Inverse of d for mod m exists only if gcd(d, m) = 1.
	 */
	public Decimal inverseModOne(Decimal d, Decimal m)
	{
		Decimal g = gcd(d, m);
		System.out.println("inverse mod, d=" + d + ", m=" + m + ", gcd=" + g);
		if (!g.equals(Decimal.ONE))
		{
			System.out.println("gcd = " + g + ", returning zero");
			return Decimal.ZERO;
		}
			
		
		Decimal.Division firstDiv =  m.divide(d); // q0 = firstDiv.quotient, p0 = 0
		System.out.println("firstDiv=" + firstDiv);
		Decimal.Division secondDiv = d.divide(firstDiv.remainder); // q1 = secondDiv.quotient
		System.out.println("secondDiv=" + secondDiv);
		return gcdInternalExt(firstDiv.remainder, secondDiv.remainder, m, Decimal.ONE, secondDiv.quotient, Decimal.ZERO, firstDiv.quotient);
	}
	
	//@Test
	void invTest()
	{
		Decimal d1 = new Decimal("16");
		Decimal m = new Decimal("27");
		//Decimal d0 = inverseModOne(d1, m);
		//System.out.println("d1=" + d1 + ", m=" + m + ", inverse-a=" + d0 + ", div=" + d0.multiply(d1).divide(m, 0)); 
		
		Decimal d = DecimalLg.inverseModOne(d1, m);
		System.out.println("d1=" + d1 + ", m=" + m + ", inverse-b=" + d + ", div=" + d.multiply(d1).divide(m, 0)); 
	}
	
	//@Test
	void gcdTest()
	{
		Decimal d1 = new Decimal("87");
		Decimal d2 = new Decimal("51");
		Decimal val = gcd1(d1, d2);
		System.out.println("R: d1=" + d1 + ", d2=" + d2 + ", gcd=" + val + ", div=" + d1.divide(val, 0)); 
		Decimal val1 = DecimalLg.gcd(d1, d2);
		if (val1.isZero()) {
			System.out.println("DecimalLf failed");
		}
		else
		{
		System.out.println("D: d1=" + d1 + ", d2=" + d2 + ", gcd=" + val1 + ", div=" + d1.divide(val1, 0));
		}
	}
	public Decimal rsaFindD(Decimal p, Decimal q, Decimal e)
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
	
	//@Test
	void crypto2()
	{
		try
		{
		String publicKey64 = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJT8rmnsVSdQiRh3p+ptE2qkA9K3r0DYywj1BIXQo5UoC9B112VlxQZLJPT32FwDqbndCDy6up7x3bdd91GO79MCAwEAAQ==";
		String privateKey64 = "MIIBVAIBADANBgkqhkiG9w0BAQEFAASCAT4wggE6AgEAAkEAlPyuaexVJ1CJGHen6m0TaqQD0revQNjLCPUEhdCjlSgL0HXXZWXFBksk9PfYXAOpud0IPLq6nvHdt133UY7v0wIDAQABAkAyqBN5amStoGFs00phl8KxUKEIJXJOHygxnHV0NjNYhCdnjVX975WwbCdVUpHq91075wPncJw25ph1fYipT4+BAiEAxZOC0CKsSIVKSqQsNVmvd8bDhMKv/ODl7gvYqKeSjBMCIQDBCvocTfoLg3+HVvFlnpTxRA6LUXVZATAk4lUz7o8FQQIhAJHoI7ytPmm39Ws13mfvuYNMx+rtE6Y+N88Z9IBob/L9AiAKD+BpiUb3QqtrCoUanuF0ke+QI3bSZNV1lraKNm0OAQIgHeqBX1+e/BjfaF4xPxB+/xWlo8PV6fLgtZcFyOAQZ+o=";
		
		sun.security.rsa.RSAPublicKeyImpl publicKey = new sun.security.rsa.RSAPublicKeyImpl(Utils.decodeBytes64(publicKey64));
		
		java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA"); 		
		java.security.spec.PKCS8EncodedKeySpec keySpec = new java.security.spec.PKCS8EncodedKeySpec(Utils.decodeBytes64(privateKey64));
//		sun.security.rsa.RSAPrivateCrtKeyImpl privateKey = kf.
		//java.security.PrivateKey privateKey = kf.generatePrivate(keySpec);
		sun.security.rsa.RSAPrivateCrtKeyImpl privateKey = (sun.security.rsa.RSAPrivateCrtKeyImpl)kf.generatePrivate(keySpec);
		System.out.println("publicKey=" + publicKey.toString());
		System.out.println("privateKey=" + privateKey.toString());
		
		//Decimal mod = new Decimal(publicKey.getModulus().toString());
		//System.out.println("mod1=" + mod.toStringF());
		
		Decimal privateMod = new Decimal(privateKey.getModulus().toString());
		System.out.println("mod2=" + privateMod.toStringF()); // len=154 + ", len=" + privateMod.toStringF().length());

		Decimal privateP = new Decimal(privateKey.getPrimeP().toString());
		Decimal privateQ = new Decimal(privateKey.getPrimeQ().toString());		
		System.out.println("mult=" + privateP.multiply(privateQ).toStringF());
		Decimal e = new Decimal(privateKey.getPublicExponent().toString());
		Decimal d = this.rsaFindD(privateP, privateQ, e);
		System.out.println("calculated d=" + d);
		System.out.println("key D=" + privateKey.getPrivateExponent().toString());
		
		javax.crypto.Cipher encryptCipher = javax.crypto.Cipher.getInstance("RSA");
		encryptCipher.init(javax.crypto.Cipher.ENCRYPT_MODE, publicKey);
		
		//new java.math.BigInteger(d.toString());
//		java.security.spec.PKCS8EncodedKeySpec keySpec = new java.security.spec.PKCS8EncodedKeySpec(Utils.decodeBytes64(privateKey64));
		java.security.spec.RSAPrivateKeySpec keySpec2 = new java.security.spec.RSAPrivateKeySpec(new java.math.BigInteger(d.toString()), new java.math.BigInteger(e.toString()));
		sun.security.rsa.RSAPrivateCrtKeyImpl privateKey2 = (sun.security.rsa.RSAPrivateCrtKeyImpl)kf.generatePrivate(keySpec);
		
		
		String testStr = "Hello, world";
		//String encodedMessage = java.util.Base64.getEncoder().encodeToString(testStr.getBytes(StandardCharsets.UTF_8));
		byte[] encryptedBytes = encryptCipher.doFinal(testStr.getBytes(StandardCharsets.UTF_8));
		String encodedMessage = Utils.encodeBytes64(encryptedBytes);
		System.out.println("encoded: "+ encodedMessage);
		javax.crypto.Cipher decryptCipher = javax.crypto.Cipher.getInstance("RSA");
		decryptCipher.init(javax.crypto.Cipher.DECRYPT_MODE, privateKey2);
		
		//Utils.decodeBytes64();
		String detestStr = new String(//
			decryptCipher.doFinal(
				java.util.Base64.getDecoder().decode(
					encodedMessage.getBytes(StandardCharsets.UTF_8)
				) //encryptedBytes
			)
		);//, StandardCharsets.UTF_8);

		

		System.out.println("decoded: " + detestStr);
		}
		catch (Exception x)
		{
			System.out.println("crypto failed: " + x.getMessage());
		}
	}
	
	@Test
	void dbTest()
	{
		String dbUrl = "jdbc:sqlserver://asb-sql-dev.studbook.local;encrypt=false";
		String user = "reins";
		String pwd = "reins";
		
		for (Enumeration<Driver> ed = DriverManager.getDrivers(); ed.hasMoreElements();)
		{
			Driver d = ed.nextElement();
			System.out.println(d.toString());
		}
		 try(Connection conn = DriverManager.getConnection(dbUrl, user, pwd);
	         Statement stmt = conn.createStatement();
	         ResultSet rs = stmt.executeQuery("select top 10 hor_horse_id, hor_dam_id, hor_sire_id from reins.dbo.horse");) {
	         // Extract data from result set
		 	ResultSetMetaData rsmd = rs.getMetaData();
	         while (rs.next()) {
	            // Retrieve by column name
	        	for (int c = 1; c <= rsmd.getColumnCount(); c++)
	        	{
	        		String label = rsmd.getColumnLabel(c);
	        		System.out.print(label + "=" + rs.getString(c) + " ");
	        	}
	        	System.out.println("");
	         }
	      } catch (SQLException e) {
	         e.printStackTrace();
	      }

	}
	
}
