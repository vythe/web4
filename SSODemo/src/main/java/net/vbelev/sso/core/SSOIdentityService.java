package net.vbelev.sso.core;

/**
 * SSO Identity is everything we need to know about the user or application that we will authenticate.
 */
public class SSOIdentityService 
{	
	public static class IdentityInfo
	{
		public long identityId;
		public String login;
		public SSOPasswordService.PasswordInfo password;
	}
	private long _id;	
	private String _login;
	private String _salt;
	private String _password;
	private long _passwordKeyId;
	
	public String getLogin()
	{
		return _login == null? "" : _login;
	}

}
