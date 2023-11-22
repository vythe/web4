package net.vbelev.sso.core;
import java.util.*;
import net.vbelev.utils.*;
/**
 * Generates new passwords and validates user-provided passwords.
 */
public class SSOPasswordService {

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
		public java.security.Key key;
		
		public String getKeyString()
		{
			if (key == null) return "";
			return Utils.encodeBytes64(key.getEncoded());
		}
	}
	
	private final Hashtable<Long, PasswordKeyInfo> _keyInfoTable = new Hashtable<Long, PasswordKeyInfo>(); 
	private PasswordKeyInfo _currentKeyInfo = null;
	
	/** 
	 * Generates a new PasswordKeyInfo to be used for new encrypting new passwords.
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
}
