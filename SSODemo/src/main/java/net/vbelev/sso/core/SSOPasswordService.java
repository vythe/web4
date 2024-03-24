package net.vbelev.sso.core;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;

import net.vbelev.utils.*;
/**
 * Generates new passwords and validates user-provided passwords.
 */
public class SSOPasswordService 
{
	private static final int SALT_LENGTH = 8;
	private static final int KEY_LENGTH = 512;
	/**
	 * For easier handling of passwords, they are grouped in this structure
	 */
	public static class PasswordInfo
	{
		private String _salt;
		private String _password;
		private long _passwordKeyId;
		
		public PasswordInfo(String salt, String password, long passwordKeyId)
		{
			_salt = salt;
			_password = password;
			_passwordKeyId = passwordKeyId;
		}
		
		public String getSalt() { return _salt;}
		public String getPassword() { return _password; }
		public long getPasswordKeyId() { return _passwordKeyId; } 
	}
	
	/**
	 * The class needs to be public to support saving and loading from storage. Keep it simple.
	 */
	public static class PasswordKeyInfo
	{
		private long _keyId;
		private long _generationTime;
		private java.security.Key _key;
		
		private javax.crypto.Cipher encryptCipher;
		
		public long getKeyId() { return _keyId;}
		public long getGenerationTime() { return _generationTime; }
		public Date getGenerationDateTime() { return new Date(_generationTime); }
		
		public PasswordKeyInfo(long keyId, long generationTime, java.security.Key key)
		{
			_keyId = keyId;
			_generationTime = generationTime;
			_key = key;
		}
		
		public PasswordKeyInfo(long keyId, long generationTime, String keyEncoded, boolean isPublic) throws GeneralSecurityException
		{
			_keyId = keyId;
			_generationTime = generationTime;
			setKey(keyEncoded, isPublic);
		}
		
		private synchronized void setKey(String keyEncoded, boolean isPublic) throws GeneralSecurityException
		{
			if (keyEncoded == null || keyEncoded.length() == 0)
			{
				_key = null;
				encryptCipher = null;
				return;
			}

			java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
			byte[] keyBytes = Utils.decodeBytes64(keyEncoded);
			if (isPublic)
			{
				//java.security.interfaces.RSAPublicKey
				_key = kf.generatePublic(new java.security.spec.X509EncodedKeySpec(keyBytes));					
			}
			else
			{
				java.security.spec.PKCS8EncodedKeySpec keySpec = new java.security.spec.PKCS8EncodedKeySpec(keyBytes);
				//java.security.interfaces.RSAPrivateCrtKey 
				_key = kf.generatePrivate(keySpec);				
			}
			encryptCipher = null;
		}
		
		public synchronized String getKeyString()
		{
			if (_key == null) return "";
			return Utils.encodeBytes64(_key.getEncoded());
		}
		
		/**
		 * Converts {@code txt} into an undecryptable(?) string.
		 * Note that salting and padding is the responsibility of the consumer, not of this method.
		 * @throws GeneralSecurityException
		 */
		public synchronized String encrypt(String txt) throws GeneralSecurityException
		{
			if (txt == null || txt.length() == 0)
				throw new IllegalArgumentException("Encrypting empty strings is not allowed");
			if (encryptCipher == null)
			{
				encryptCipher = javax.crypto.Cipher.getInstance("RSA/ECB/NoPadding");
				encryptCipher.init(javax.crypto.Cipher.ENCRYPT_MODE, _key);
			}
			try
			{
				byte[] txtBytes = txt.getBytes(StandardCharsets.UTF_8);
				int blocksize = KEY_LENGTH / 8;
				int offset = 0;
				String encrypted = "";
				for (; offset + blocksize < txtBytes.length; offset += blocksize)
				{
					encrypted += Utils.encodeBytes64(encryptCipher.doFinal(txtBytes, offset, blocksize), true);
				}
				encrypted += Utils.encodeBytes64(encryptCipher.doFinal(txtBytes, offset, txtBytes.length - offset), true);
/*
			encryptCipher = javax.crypto.Cipher.getInstance("RSA/ECB/NoPadding");
			encryptCipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key);
			String encrypted2 = Utils.encodeBytes64(encryptCipher.doFinal(txt.getBytes(StandardCharsets.UTF_8)));
			encryptCipher = javax.crypto.Cipher.getInstance("RSA/ECB/NoPadding");
			encryptCipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key);
			String encrypted3 = Utils.encodeBytes64(encryptCipher.doFinal(txt.getBytes(StandardCharsets.UTF_8)));
	*/		
			return encrypted;
			}
			catch (Exception x)
			{
				System.out.println(x);
			}
			return "";
		}
	}
	
	private final Hashtable<Long, PasswordKeyInfo> _keyInfoTable = new Hashtable<Long, PasswordKeyInfo>(); 
	private PasswordKeyInfo _currentKeyInfo = null;
	
	/** 
	 * Generates a new PasswordKeyInfo to be used for new encrypting new passwords and sets it as the current key.
	 * @return the new key Id to use for baking password infos
	 */
	public long generateKeyInfo()
	{
		java.security.Key key;
		try
		{
			java.security.KeyPairGenerator kg = java.security.KeyPairGenerator.getInstance("RSA");
			
			kg.initialize(KEY_LENGTH);
			java.security.KeyPair keyPair = kg.generateKeyPair();
			
			java.security.interfaces.RSAPublicKey publicKey = (java.security.interfaces.RSAPublicKey)keyPair.getPublic();
			key = publicKey;
		}
		catch (java.security.NoSuchAlgorithmException x)
		{
			throw new UnsupportedOperationException(x);
		}		

		synchronized (this)
		{
			long maxKey = 0;
			for (Enumeration<Long> keyEnum = _keyInfoTable.keys(); keyEnum.hasMoreElements();)
			{
				long keyId = keyEnum.nextElement();
				if (keyId > maxKey) maxKey = keyId;
			}
			maxKey++;
			
			PasswordKeyInfo info = new PasswordKeyInfo(maxKey, new Date().getTime(), key);
		
			_keyInfoTable.put(info.getKeyId(), info);
			_currentKeyInfo = info;
			return info.getKeyId();
		}
	}
	
	public synchronized void setPasswordKeyInfo(PasswordKeyInfo info)
	{
		if (_keyInfoTable.contains(info.getKeyId()))
			throw new IllegalArgumentException("Info with this keyId already exists: " + info.getKeyId());
		_keyInfoTable.put(info.getKeyId(), info);
		if (_currentKeyInfo == null || _currentKeyInfo.getKeyId() < info.getKeyId())
			_currentKeyInfo = info;
	}
	
	public synchronized PasswordKeyInfo getPasswordKeyInfo(long keyId)
	{
		return _keyInfoTable.get(keyId);
	}

	public PasswordInfo bakePassword(String password, long keyId) throws GeneralSecurityException
	{
		PasswordKeyInfo keyInfo = getPasswordKeyInfo(keyId); 
		if (keyInfo == null)
			throw new IllegalArgumentException("keyId not found: " + keyId);
		
		return bakePassword(password, keyInfo);
	}
	
	public synchronized PasswordInfo bakePassword(String password) throws GeneralSecurityException
	{
		PasswordKeyInfo keyInfo = _currentKeyInfo;
		if (keyInfo == null)
			generateKeyInfo();
		return bakePassword(password, keyInfo);
	}
	
	private synchronized PasswordInfo bakePassword(String password, PasswordKeyInfo keyInfo) throws GeneralSecurityException
	{
		String salt = Utils.randomString64(SALT_LENGTH);
		String encrypted = keyInfo.encrypt(salt + password);

		//javax.crypto.Cipher encryptCipher = javax.crypto.Cipher.getInstance("RSA/ECB/NoPadding");
		//encryptCipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keyInfo.key);
		//byte[] encryptedBytes = encryptCipher.doFinal((res.salt + password).getBytes(StandardCharsets.UTF_8));
		//res.password = Utils.encodeBytes64(encryptedBytes);
		
		return new PasswordInfo(salt, encrypted, keyInfo._keyId);
	}
	
	public boolean testPassword(String test, PasswordInfo pwd) throws GeneralSecurityException
	{
		PasswordKeyInfo keyInfo = _keyInfoTable.get(pwd.getPasswordKeyId());
		if (keyInfo == null)
			return false;
		
		String encryptedTest = keyInfo.encrypt(pwd.getSalt() + test);
		return encryptedTest.equals(pwd.getPassword());
	}
}
