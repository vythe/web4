package net.vbelev.web4.ui;

import java.util.*;

/**
 * The website user - login, password, status, a list of profiles.
 * @author vythe
 *
 */
public class WebUser 
{

	public enum StatusEnum
	{
		VISITOR,
		ACTIVE,
		INACTIVE
	}
	
	public enum PasswordKindEnum
	{
		NONE,
		PLAIN
	}
	
	public static final java.util.regex.Pattern loginPattern = 
			java.util.regex.Pattern.compile("^[a-z][a-z\\d_]+$");
	
	public Integer ID;
	public String name;
	public String login;
	public String password;
	public StatusEnum status;
	public PasswordKindEnum passwordKind;
	
	public final List<Integer> profiles = new ArrayList<Integer>();
	/*
	public static class Model
	{
		public int ID;
		public String name;
		public String login;
		public StatusEnum status;
		public List<String> profiles;
		
		
	}
	*/
	public WebUser()
	{
		status = StatusEnum.VISITOR;
		passwordKind = PasswordKindEnum.NONE;
	}
	
}
