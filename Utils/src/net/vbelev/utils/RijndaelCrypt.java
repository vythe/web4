package net.vbelev.utils;
 
 
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
}