package net.vbelev.utils;
 
 
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
 
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
 
/**
 * Taken whole from jafetsanchez (https://gist.github.com/jafetsanchez/1080133) 
 */
public class RijndaelCrypt {
 
    //private static String TRANSFORMATION = "AES/CBC/PKCS7Padding";
    private static String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static String ALGORITHM = "AES";
    private static String DIGEST = "MD5";
     
    private static SecretKey _password;
    private static IvParameterSpec _IVParamSpec;
     
    //16-byte private key, the default value
    private static byte[] IV = "over the lazy dog.".getBytes();
     
    private void reportError(String msg, Exception e)
    {
    	throw new IllegalArgumentException(msg, e);
    }
     
    
    private Cipher _cipher;
	private final java.util.Base64.Encoder stringEncoder = java.util.Base64.getEncoder();    
	public final java.util.Base64.Decoder stringDecoder = java.util.Base64.getDecoder();
    private MessageDigest digest;            
	
    /**
     Constructor
     @password Public key
      
    */
    public RijndaelCrypt(String password) {
 
        try {
             
            //Encode digest
            digest = MessageDigest.getInstance(DIGEST);            
            _password = new SecretKeySpec(digest.digest(password.getBytes()), ALGORITHM);
             
            //Initialize objects
            _cipher = Cipher.getInstance(TRANSFORMATION);
            byte[] vectorMD5 = digest.digest(IV);
            _IVParamSpec = new IvParameterSpec(vectorMD5); //IV);
             
        } catch (NoSuchAlgorithmException e) {
            //Log.e(TAG, "No such algorithm " + ALGORITHM, e);
        	reportError("No such algorithm " + ALGORITHM, e);
        } catch (NoSuchPaddingException e) {
            //Log.e(TAG, "No such padding PKCS7", e);
        	reportError("No such padding PKCS7", e);
        }              
    }
 
    public RijndaelCrypt(String password, String iv) {
    	 
        try {
        	 digest = MessageDigest.getInstance(DIGEST);             
            //Encode digest
            _password = new SecretKeySpec(digest.digest(password.getBytes()), ALGORITHM);
             
            //Initialize objects
            _cipher = Cipher.getInstance(TRANSFORMATION);
            byte[] vectorMD5 = digest.digest(iv.getBytes());
            _IVParamSpec = new IvParameterSpec(vectorMD5); //IV);
             
        } catch (NoSuchAlgorithmException e) {
            //Log.e(TAG, "No such algorithm " + ALGORITHM, e);
        	reportError("No such algorithm " + ALGORITHM, e);
        } catch (NoSuchPaddingException e) {
            //Log.e(TAG, "No such padding PKCS7", e);
        	reportError("No such padding PKCS5", e);
        }              
    }
     
    /**
    Encryptor.
 
    @text String to be encrypted
    @return Base64 encrypted text
 
    */
    public String encrypt(byte[] text) {
         
        byte[] encryptedData;
         
        try {
             
            _cipher.init(Cipher.ENCRYPT_MODE, _password, _IVParamSpec);
            encryptedData = _cipher.doFinal(text);
             
        } catch (InvalidKeyException e) {
        	reportError("Invalid key  (invalid encoding, wrong length, uninitialized, etc).", e);
            return null;
        } catch (InvalidAlgorithmParameterException e) {
        	reportError("Invalid or inappropriate algorithm parameters for " + ALGORITHM, e);
            return null;
        } catch (IllegalBlockSizeException e) {
        	reportError("The length of data provided to a block cipher is incorrect", e);
            return null;
        } catch (BadPaddingException e) {
        	reportError("The input data but the data is not padded properly.", e);
            return null;
        }               
         
        return stringEncoder.encodeToString(encryptedData);
         
    }
     
    /**
    Decryptor.
 
    @text Base64 string to be decrypted
    @return decrypted text
 
    */   
    public String decrypt(String text) {
 
        try {
            _cipher.init(Cipher.DECRYPT_MODE, _password, _IVParamSpec);
             
            byte[] decodedValue = stringDecoder.decode(text);
            byte[] decryptedVal = _cipher.doFinal(decodedValue);
            return new String(decryptedVal);            
             
             
        } catch (InvalidKeyException e) {
        	reportError("Invalid key  (invalid encoding, wrong length, uninitialized, etc).", e);
            return null;
        } catch (InvalidAlgorithmParameterException e) {
        	reportError("Invalid or inappropriate algorithm parameters for " + ALGORITHM, e);
            return null;
        } catch (IllegalBlockSizeException e) {
        	reportError("The length of data provided to a block cipher is incorrect", e);
            return null;
        } catch (BadPaddingException e) {
        	reportError("The input data but the data is not padded properly.", e);
            return null;
        }                       
    }       


	//@Test
	void crypto()
	{
		try
		{
		java.security.KeyPairGenerator kg = java.security.KeyPairGenerator.getInstance("RSA");
		
		kg.initialize(512);
		java.security.KeyPair key = kg.generateKeyPair();
		
		java.security.interfaces.RSAPublicKey publicKey = (java.security.interfaces.RSAPublicKey)key.getPublic();
		java.security.interfaces.RSAPrivateCrtKey privateKey = (java.security.interfaces.RSAPrivateCrtKey)key.getPrivate();
		
		System.out.println("key=" + privateKey.toString());
		

		System.out.println("publicKey64=" + Utils.encodeBytes64(publicKey.getEncoded()));
		System.out.println("privateKey64=" + Utils.encodeBytes64(privateKey.getEncoded()));
		}
		catch (Exception x)
		{
			System.out.println("crypto failed: " + x.getMessage());
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
	
	@Test
	void crypto2()
	{
		try
		{
		String publicKey64 = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAJT8rmnsVSdQiRh3p+ptE2qkA9K3r0DYywj1BIXQo5UoC9B112VlxQZLJPT32FwDqbndCDy6up7x3bdd91GO79MCAwEAAQ==";
		String privateKey64 = "MIIBVAIBADANBgkqhkiG9w0BAQEFAASCAT4wggE6AgEAAkEAlPyuaexVJ1CJGHen6m0TaqQD0revQNjLCPUEhdCjlSgL0HXXZWXFBksk9PfYXAOpud0IPLq6nvHdt133UY7v0wIDAQABAkAyqBN5amStoGFs00phl8KxUKEIJXJOHygxnHV0NjNYhCdnjVX975WwbCdVUpHq91075wPncJw25ph1fYipT4+BAiEAxZOC0CKsSIVKSqQsNVmvd8bDhMKv/ODl7gvYqKeSjBMCIQDBCvocTfoLg3+HVvFlnpTxRA6LUXVZATAk4lUz7o8FQQIhAJHoI7ytPmm39Ws13mfvuYNMx+rtE6Y+N88Z9IBob/L9AiAKD+BpiUb3QqtrCoUanuF0ke+QI3bSZNV1lraKNm0OAQIgHeqBX1+e/BjfaF4xPxB+/xWlo8PV6fLgtZcFyOAQZ+o=";
		
		//java.security.interfaces.RSAPublicKey publicKey = new sun.security.rsa.RSAPublicKeyImpl(Utils.decodeBytes64(publicKey64));
		
		java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
		java.security.interfaces.RSAPublicKey publicKey = (java.security.interfaces.RSAPublicKey)kf.generatePublic(new java.security.spec.X509EncodedKeySpec(Utils.decodeBytes64(publicKey64)));
		java.security.spec.PKCS8EncodedKeySpec keySpec = new java.security.spec.PKCS8EncodedKeySpec(Utils.decodeBytes64(privateKey64));
//		sun.security.rsa.RSAPrivateCrtKeyImpl privateKey = kf.
		//java.security.PrivateKey privateKey = kf.generatePrivate(keySpec);
		java.security.interfaces.RSAPrivateCrtKey privateKey = (java.security.interfaces.RSAPrivateCrtKey)kf.generatePrivate(keySpec);
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
		//java.security.spec.RSAPrivateKeySpec keySpec2 = new java.security.spec.RSAPrivateKeySpec(new java.math.BigInteger(d.toString()), new java.math.BigInteger(e.toString()));
		java.security.spec.RSAPrivateKeySpec keySpec2 = new java.security.spec.RSAPrivateKeySpec(new java.math.BigInteger(privateMod.toString()), new java.math.BigInteger(d.toString()));
		java.security.interfaces.RSAPrivateCrtKey privateKey2 = (java.security.interfaces.RSAPrivateCrtKey)kf.generatePrivate(keySpec2);
		
		
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
	
}