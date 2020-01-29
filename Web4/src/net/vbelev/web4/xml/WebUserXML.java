package net.vbelev.web4.xml;
import java.util.*;
import javax.xml.bind.*;
import javax.xml.bind.annotation.*;

import net.vbelev.web4.ui.*;
import net.vbelev.web4.utils.*;

@XmlRootElement(name = "web_user")
public class WebUserXML {
	/** users are stored in individual files as [ID].xml */
	public static final String STORAGE_NAME = "web_users"; 

	@XmlAttribute
	public Integer ID;
	public String name;
	@XmlAttribute
	public String login;
	public String password;
	@XmlAttribute
	public String status;
	public List<Integer> profiles;// = new ArrayList<Integer>();


	public WebUserXML()
	{
		
	}
	
	public void fromWebUser(WebUser u)
	{
		if (u == null) {return; }
		
		this.ID = u.ID;
		this.name = Utils.NVL(u.name, "");
		this.login = Utils.NVL(u.login, "");
		this.password = Utils.NVL(u.password, "");
		status = u.status.name();
		profiles = new ArrayList<Integer>(u.profiles);
	}
	
	public WebUser toWebUser(WebUser u)
	{
		if (u == null) { u = new WebUser(); }
		
		u.ID = this.ID;
		u.name = Utils.IsEmpty(this.name)? null : this.name;
		u.login = Utils.IsEmpty(this.login)? null : this.login;
		u.password = Utils.IsEmpty(this.password)? null : this.password;
		u.status = Utils.tryParseEnum(this.status, WebUser.StatusEnum.VISITOR);
		u.profiles.clear();
		if (!this.profiles.isEmpty())
		{
			u.profiles.addAll(this.profiles);
		}

		return u;
	}
}
