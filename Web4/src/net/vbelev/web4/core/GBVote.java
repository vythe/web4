package net.vbelev.web4.core;

import java.util.*;

/**
 * A vote on a specified bill (yes/no), or a choice between two actions
 * if we follow the choises paradigm 
 * @author vythe
 *
 */
public class GBVote {

	public enum SayEnum
	{
		NONE,
		AYE,
		NAY,
		PASS
	}
	
	public int billID;
	public SayEnum say;
	public Date sayDate;
	
	public GBVote()
	{
	}
	
	public GBVote(GBVote from)
	{
		this.billID = from.billID;
		this.say = from.say;
		this.sayDate = from.sayDate;
	}
}
