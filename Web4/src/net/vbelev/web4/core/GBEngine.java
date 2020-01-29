package net.vbelev.web4.core;

import java.util.*;
import java.io.*;
import java.nio.file.Paths;

import net.vbelev.web4.ui.WebUser;
import net.vbelev.web4.utils.*;
import net.vbelev.web4.xml.*;

/**
 * A coherent storage for profiles and groups
 * @author vythe
 *
 */
public class GBEngine {

	public interface Storage
	{
		boolean ping(boolean force);
		
		GBGroupListXML getGroups();
		void saveGroups(GBGroupListXML xml);
		
		List<GBBillXML> loadBills();
		GBBillXML loadBill(int billID);
		int getNewBillID(boolean withCreate);
		void saveBill(GBBillXML xml);
		
		GBProfileXML loadProfile(int profileID);
		int getNewProfileID(boolean withCreate);
		void saveProfile(GBProfileXML xml);
		
		WebUserXML loadWebUser(int webUserID);
		int loadWebUserIndex(Hashtable<String, Integer> index);
		public int getNewWebUserID(boolean withCreate);
		void saveWebUser(WebUserXML xml);
	}
	
	private GBGroup[] groups = new GBGroup[0];
	public final Hashtable<Integer, GBBill> bills = new Hashtable<Integer, GBBill>();
	
	public final XMLiser xmliser = null;
	public final Storage storage;

	public final Random random = new Random();
	
	//public static final java.util.regex.Pattern xmlIdPattern = java.util.regex.Pattern.compile("^(\\d+).xml$");
	public static final java.util.regex.Pattern webUserLoginPattern = 
			java.util.regex.Pattern.compile("^[a-z][a-z\\d_]+$");
	/*
	private GBEngine(String root)
	{
		//xmliser = new XMLiser("net.vbelev.web4.xml");
		xmliser = new XMLiser(
				GBGroupListXML.class, 
				GBBillXML.class, 
				GBProfileXML.class, 
				WebUserXML.class);
		dataFolder = root;
	}
	*/
	private GBEngine(Storage st)
	{
		storage = st;
	}

	/*
	public static GBEngine loadEngine(String root)
	{
		//GBEngine res = new GBEngine(root);
		GBEngine res = new GBEngine(null);
		res.loadGroups();
		res.loadBills();
		return res;
	}
	*/
	
	public static GBEngine loadEngine(Storage st)
	{
		GBEngine res = new GBEngine(st);
		res.loadGroups();
		res.loadBills();
		return res;
		
	}
	
	public void loadGroups()
	{
		GBGroupListXML xml = storage.getGroups();
		if (xml ==  null)
		{
			this.testSet();
			this.saveGroups();
		}
		else
		{
			xml.toEngine(this);
			this.calculateAll();
		}
	}
	
	public void saveGroups()
	{
		GBGroupListXML xml = new GBGroupListXML();
		xml.fromEngine(this);

		storage.saveGroups(xml);
	}
	
	/** 
	 * for now, we'll load all bills at once and worry later.
	 */
	public void loadBills()
	{
		List<GBBillXML> bills = storage.loadBills();
		
		this.bills.clear();
		for (GBBillXML xml : bills)
		{
			if (xml == null || xml.ID == null) continue;
			
			GBBill bill = xml.toGBBill(this, null);
			bill.calculateInvAffinities(this);
			this.bills.put(bill.ID, bill);
		}
	}

	public GBBill loadBill(int billID)
	{
		GBBillXML xml = storage.loadBill(billID);
		if (xml == null) return null;
		
		GBBill bill = xml.toGBBill(this, null);
		bill.calculateInvAffinities(this);
		//this.bills.put(bill.ID, bill);
		return bill;
	}

	public GBBill getBill(Integer billID, boolean forceReload)
	{
		if (billID == null) return null;
		
		GBBill bill = bills.getOrDefault(billID, null);
		if (bill == null || forceReload)
		{
			bill = loadBill(billID.intValue());
			if (bill == null)
			{
				bills.remove(billID);
			}
			else
			{
				bills.put(billID, bill);
			}
		}
		return bill;
	}

	public int getNewBillID(boolean withCreate) {
		return storage.getNewBillID(withCreate);
	}
		
	public void saveBill(GBBill bill) {
		if (bill == null) return;
		
		if (bill.ID == null)
		{
			bill.ID = this.getNewBillID(true);
		}
		GBBillXML xml = new GBBillXML();
		xml.fromGBBill(this, bill);
		
		storage.saveBill(xml);
	}

	public int getSize()
	{
		if (groups == null) return 0;
		return groups.length;
	}
	
	public void setSize(int size)
	{
		int copySize = (groups == null)? null 
				: groups.length < size? groups.length 
				: size
		;
	
		GBGroup[] newGroups = new GBGroup[size];
		for (int i = 0; i < copySize; i++)
		{
			newGroups[i] = groups[i];
			newGroups[i].ID = i;
		}
		for (int i = copySize; i < size; i++)
		{
			newGroups[i] = new GBGroup();
			newGroups[i].ID = i;
		}
		this.groups = newGroups;
	}
	
	public void testSet()
	{
		groups = new GBGroup[4];
				
		GBGroup g1 = new GBGroup();
		g1.moniker = "g1";
		g1.name = "Group1";
		g1.ID = 0;
		g1.setAffinity(1,  0.2);
		g1.setAffinity(2,  0.9);
		groups[0] = g1;
				
		GBGroup g2 = new GBGroup();
		g2.moniker = "g2";
		g2.name = "Group2";
		g2.ID = 1;
		g2.setAffinity(2,  0.6);
		g2.setAffinity(3,  0.3);
		groups[1] = g2;
		
		
		GBGroup g3 = new GBGroup();
		g3.moniker = "g3";
		g3.name = "Group3";
		g3.ID = 2;
		g3.setAffinity(3, 0.5);
		g3.setAffinity(0, 0.5);
		groups[2] = g3;
		
		GBGroup g4 = new GBGroup();
		g4.moniker = "g4";
		g4.name = "Group4";
		g4.ID = 3;
		g4.setAffinity(0, 0.8);
		g4.setAffinity(1,0.3);
		groups[3] = g4;
	}

	public boolean hasGroup(String moniker)
	{
		if (moniker == null) return false;
		for (GBGroup g : groups)
		{
			if (g.moniker.equals(moniker)) return true;
		}
		return false;
	}
	
	public GBGroup getGroup(int ID)
	{
		if (groups == null || ID < 0 || ID >= groups.length)
		{
			return null;
		}
		return groups[ID];
	}
	
	public GBGroup getGroup(String moniker)
	{
		if (moniker == null) return null;
		for (GBGroup g : groups)
		{
			if (g.moniker.equals(moniker)) return g;
		}
		return null;
	}

	public String getGroupMoniker(int ID)
	{
		if (groups == null || ID < 0 || ID >= groups.length)
		{
			return null;
		}
		return groups[ID].moniker;
	}
	
	public List<GBGroup> getGroups()
	{
		ArrayList<GBGroup> res = new ArrayList<GBGroup>();
		for (GBGroup g : groups)
		{
			res.add(g);
		}
		return res;
	}
	
	public void calculateAll()
	{
		// clear all calculated values first
		for (GBGroup g: groups)
		{
			for (int g2 = 0; g2 < groups.length; g2++)
			{
				//if (g2 == g.ID) continue;
				GBAffinity aff = g.getAffinity(g2);
				if (aff == null || aff.quality() != GBAffinity.QualityEnum.SET)
				{
					g.setAffinity(g2,  0,  GBAffinity.QualityEnum.NONE);
				}
			}
				
		}
		// recalculate it all three times
		for (int repeat = 0; repeat < 3; repeat++)
		{
		for (GBGroup g: groups)
		{
			for (int g2 = 0; g2 < groups.length; g2++)
			{
				//if (g2 == g.ID) continue;
				GBAffinity aff = g.getAffinity(g2);
				if (aff == null || aff.quality() != GBAffinity.QualityEnum.SET)
				{
					aff = getAffinity(g, g2, true);
				}
			}
				
		}
		}
	}
	public Double calculateAffinity(GBGroup forGroup, int toGroup)
	{
		if (forGroup.ID == toGroup) return 1.;
		
		Double[] listFrom = this.getAffinitesFor(forGroup.ID);
		Double[] listTo = this.getAffinitiesTo(toGroup);
		listFrom[forGroup.ID] = null; // exclude the "self" link
		
		return GBAffinity.calculateAffinity(listFrom, listTo);
	}	
	
	public Double calculateAffinityOld(GBGroup forGroup, int toGroup)
	{
		double nom = 0;
		double denom = 0;
		for (GBAffinity aff : forGroup.getAffinities(true))
		{
			if (aff.toID() == forGroup.ID)
			{
				continue;
			}
			double coef = aff.value() * aff.value();
			GBAffinity aff2 = groups[aff.toID()].getAffinity(toGroup);
			if (aff2.quality() != GBAffinity.QualityEnum.NONE)
			{
				nom += coef * aff2.value();
				denom += coef;
			}
		}
		if (denom <= 0)
		{
			return null;
		}
		return nom / denom;
	}
	
	/**
	 * returns an array of affinity values of group[forID] to all other groups.
	 * values will be null for qualities NONE.
	 * @return
	 */
	public Double[] getAffinitesFor(int forID)
	{
		Double[] res = new Double[groups.length];
		GBGroup g = getGroup(forID);
		
		for(GBAffinity aff : g.getAffinities(true))
		{
			res[aff.toID()] = aff.value();
		}	
		
		return res;
	}
	
	/**
	 * returns an array of affinity values of all groups to the group[forID].
	 * values will be null for qualities NONE.
	 * @return
	 */
	public Double[] getAffinitiesTo(int toID)
	{
		Double[] res = new Double[groups.length];
		for (GBGroup g : groups)
		{
			GBAffinity aff = g.getAffinity(toID);
			if (aff != null && aff.quality() != GBAffinity.QualityEnum.NONE)
			{
				res[g.ID] = aff.value();
			}	
		}
		return res;
	}
		
	public GBAffinity getAffinity(GBGroup group, int toGroup, boolean forceRecalc)
	{
		//GBGroup toGroup = getGroup(toMoniker);
		//if (toGroup == null) return GBAffinity.EMPTY
		if (toGroup < 0 || toGroup >= groups.length)
		{
			return GBAffinity.EMPTY;
		}
		
		GBAffinity aff = group.getAffinity(toGroup);
		if (aff.quality() != GBAffinity.QualityEnum.SET
				 && (forceRecalc || aff.quality() == GBAffinity.QualityEnum.NONE)
		)
		{
			Double newValue = calculateAffinity(group, toGroup);
			if (newValue == null)
			{
				aff = group.setAffinity(toGroup, 0, GBAffinity.QualityEnum.NONE);
			}
			else
			{
			aff = group.setAffinity(toGroup, newValue, GBAffinity.QualityEnum.CALCULATED);
			}
		}
		
		return aff;
	}

	//==== GBProfile storage ====
	public GBProfile loadProfile(int profileID)
	{
		GBProfileXML xml = storage.loadProfile(profileID);
		
		if (xml == null) return null;
		
		GBProfile profile = xml.toGBProfile(this, null);
		return profile;
	}

	public GBProfile getProfile(Integer profileID)
	{
		if (profileID == null) return null;
		
		GBProfile profile = loadProfile(profileID.intValue());
		return profile;
	}
	
	public int getNewProfileID(boolean createFile) {

		return storage.getNewProfileID(createFile);
	}
	
	public void saveProfile(GBProfile profile) {
		if (profile == null) return;
		
		if (profile.ID == null)
		{
			profile.ID = storage.getNewProfileID(true);
		}
		GBProfileXML xml = new GBProfileXML();
		profile.saveDate = new Date();
		xml.fromGBProfile(this, profile);
		storage.saveProfile(xml);
	}
	
	//==== WebUser storage ====
	public WebUser loadWebUser(int webUserID)
	{
		WebUserXML xml = storage.loadWebUser(webUserID);
		if (xml == null) return null;
		
		WebUser webUser = xml.toWebUser(null);
		return webUser;
	}

	public int loadWebUserIndex(Hashtable<String, Integer> index)
	{
		return storage.loadWebUserIndex(index);
	}
	
	public WebUser getWebUser(Integer webUserID)
	{
		if (webUserID == null) return null;
		
		WebUser webUser = loadWebUser(webUserID.intValue());
		return webUser;
	}
	
	public int getNewWebUserID(boolean createFile) 
	{
		return storage.getNewWebUserID(createFile);
	}
	
	public void saveWebUser(WebUser webUser) {
		if (webUser == null) return;
		
		if (webUser.ID == null)
		{
			webUser.ID = storage.getNewWebUserID(true);
		}
		WebUserXML xml = new WebUserXML();
		xml.fromWebUser(webUser);
		storage.saveWebUser(xml);
	}
	
}
