package net.vbelev.sso.core;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import javax.crypto.NoSuchPaddingException;

import net.vbelev.utils.*;
/**
 * Generates new passwords and validates user-provided passwords.
 */
public class SSOPasswordService 
{
	private static final int SALT_LENGTH = 8;
	
	/**
	 * For easier handling of passwords, they are grouped in this structure
	 */
	public static class PasswordInfo
	{
		public String salt;
		public String password;
		public long passwordKeyId;
		
		public PasswordInfo() {}
		
		public PasswordInfo(String salt, String password, long passwordKeyId)
		{
			this.salt = salt;
			this.password = password;
			this.passwordKeyId = passwordKeyId;
		}
	}
	
	public static class PasswordKeyInfo
	{
		public long keyId;
		public long generationTime;
		//java.security.interfaces.RSAPublicKey key;
		private java.security.Key key;
		
		private javax.crypto.Cipher encryptCipher;
		
		public synchronized void setKey(java.security.Key key)
		{
			this.key = key;
			encryptCipher = null;
		}
		
		public synchronized void setKey(String keyEncoded, boolean isPublic)
		{
			if (keyEncoded == null || keyEncoded.length() == 0)
			{
				key = null;
				encryptCipher = null;
				return;
			}

			try
			{
				java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
				byte[] keyBytes = Utils.decodeBytes64(keyEncoded);
				if (isPublic)
				{
					key = (java.security.interfaces.RSAPublicKey)kf.generatePublic(new java.security.spec.X509EncodedKeySpec(keyBytes));					
				}
				else
				{
					java.security.spec.PKCS8EncodedKeySpec keySpec = new java.security.spec.PKCS8EncodedKeySpec(keyBytes);
					//java.security.interfaces.RSAPrivateCrtKey privateKey 
					key = (java.security.interfaces.RSAPrivateCrtKey)kf.generatePrivate(keySpec);				
				}
			}
			catch (Exception x)
			{
				key = null;
			}
			encryptCipher = null;
		}
		
		public synchronized String getKeyString()
		{
			if (key == null) return "";
			return Utils.encodeBytes64(key.getEncoded());
		}
		
		public synchronized String encrypt(String txt) throws GeneralSecurityException
		{
			if (txt == null || txt.length() == 0)
				throw new IllegalArgumentException("Encrypting empty strings is not allowed");
			if (encryptCipher == null)
			{
				encryptCipher = javax.crypto.Cipher.getInstance("RSA");
				encryptCipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key);
			}
			byte[] encryptedBytes = encryptCipher.doFinal(txt.getBytes(StandardCharsets.UTF_8));
			String encrypted = Utils.encodeBytes64(encryptedBytes);
			
			return encrypted;
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
		synchronized (_keyInfoTable)
		{
			long maxKey = 0;
			for (Enumeration<Long> keyEnum = _keyInfoTable.keys(); keyEnum.hasMoreElements();)
			{
				long key = keyEnum.nextElement();
				if (key > maxKey) maxKey = key;
			}
			maxKey++;
			PasswordKeyInfo info = new PasswordKeyInfo();
			info.keyId = maxKey;
			try
			{
				java.security.KeyPairGenerator kg = java.security.KeyPairGenerator.getInstance("RSA");
				
				kg.initialize(512);
				java.security.KeyPair key = kg.generateKeyPair();
				
				java.security.interfaces.RSAPublicKey publicKey = (java.security.interfaces.RSAPublicKey)key.getPublic();
				info.key = publicKey;
			}
			catch (java.security.NoSuchAlgorithmException x)
			{
				throw new UnsupportedOperationException(x);
			}
			info.generationTime = new Date().getTime();
			_keyInfoTable.put(info.keyId, info);
			_currentKeyInfo = info;
			return maxKey;
		}
	}

	public synchronized PasswordInfo bakePassword(String password, long keyId) throws GeneralSecurityException
	{
		PasswordKeyInfo keyInfo = _keyInfoTable.get(keyId);
		if (keyInfo == null)
			throw new IllegalArgumentException("keyId not found: " + keyId);
		
		return bakePassword(password, keyInfo);
	}
	
	public synchronized PasswordInfo bakePassword(String password) throws GeneralSecurityException
	{
		if (_currentKeyInfo == null)
			generateKeyInfo();
		return bakePassword(password, _currentKeyInfo);
	}
	
	private synchronized PasswordInfo bakePassword(String password, PasswordKeyInfo keyInfo) throws GeneralSecurityException
	{
		PasswordInfo res = new PasswordInfo();
		res.passwordKeyId = keyInfo.keyId;
		res.salt = Utils.randomString64(SALT_LENGTH);
		res.password = keyInfo.encrypt(res.salt + password);

		//javax.crypto.Cipher encryptCipher = javax.crypto.Cipher.getInstance("RSA");
		//encryptCipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keyInfo.key);
		//byte[] encryptedBytes = encryptCipher.doFinal((res.salt + password).getBytes(StandardCharsets.UTF_8));
		//res.password = Utils.encodeBytes64(encryptedBytes);
		
		return res;
	}
}
