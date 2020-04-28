package net.vbelev.web4.core;

import java.util.*;
import java.io.*;
import java.nio.file.Paths;

import net.vbelev.web4.ui.WebUser;
import net.vbelev.web4.utils.*;
import net.vbelev.web4.xml.*;

/**
 * A coherent storage for profiles, groups and bills
 * @author vythe
 *
 */
public class GBEngine {

	public interface Storage
	{
		boolean ping(boolean force);
		
		/** get all groupsets at once */
		List<GBGroupListXML> getGroups();
		//void saveGroups(List<GBGroupListXML> xml);
		/** save one groupset at a time */
		void saveGroupList(GBGroupListXML xml);
		
		List<GBBillXML> loadBills();
		GBBillXML loadBill(int billID);
		int getNewBillID(boolean withCreate);
		void saveBill(GBBillXML xml);
		boolean deleteBill(int billID);
		
		GBProfileXML loadProfile(int profileID);
		int getNewProfileID(boolean withCreate);
		void saveProfile(GBProfileXML xml);
		
		WebUserXML loadWebUser(int webUserID);
		int loadWebUserIndex(Hashtable<String, Integer> index);
		public int getNewWebUserID(boolean withCreate);
		void saveWebUser(WebUserXML xml);
	}

	public final List<GBGroupSet> groupList = new ArrayList<GBGroupSet>();
	//private GBGroup[] groups = new GBGroup[0];
	public final Hashtable<Integer, GBBill> bills = new Hashtable<Integer, GBBill>();
	
	public final XMLiser xmliser = null;
	public final Storage storage;

	public final Random random = new Random();
	
	//public static final java.util.regex.Pattern xmlIdPattern = java.util.regex.Pattern.compile("^(\\d+).xml$");
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
	
	public GBGroupSet getGroupSet(int setID)
	{
		if (groupList == null)
		{
			return null;
		}
		for (GBGroupSet set : groupList)
		{
			if (set.ID == setID) return set;
		}
		return null;
	}
	
	public int getSize(int setID)
	{
		GBGroupSet set = getGroupSet(setID);
		if (set == null) return 0;
		
		return set.getSize();
	}
	
	public void loadGroups()
	{
		List<GBGroupListXML> xml = storage.getGroups();
		
		groupList.clear();
		if (xml ==  null || xml.size() == 0)
		{
			groupList.add(this.testSet());
			this.saveGroup(groupList.get(0));
		}
		else
		{
			for (GBGroupListXML xmlSet  : xml)
			{
				GBGroupSet set = new GBGroupSet();
				xmlSet.toGroupSet(set);
				set.calculateAll();
				groupList.add(set);
			}
		}
	}
	
	public void saveGroup(GBGroupSet set)
	{
		/*
		List<GBGroupListXML> xml = new ArrayList<GBGroupListXML>();
		for (GBGroupSet set : groupList)
		{
			GBGroupListXML xmlSet = new GBGroupListXML();
			xmlSet.fromGroupSet(set);
			xml.add(xmlSet);
		}
		storage.saveGroups(xml);
		*/
		GBGroupSet engineSet =this.getGroupSet(set.ID);
		if (engineSet == null)
		{
			synchronized(this.groupList)
			{
				this.groupList.add(set);
			}			
		}
		else if (engineSet != set) // raw object comparison
		{
			synchronized(this.groupList)
			{
				this.groupList.remove(engineSet);
				this.groupList.add(set);
			}						
		}
		
		GBGroupListXML xmlSet = new GBGroupListXML();
		xmlSet.fromGroupSet(set);
		storage.saveGroupList(xmlSet);
	}

	public GBGroupSet testSet()
	{
		GBGroupSet set = new GBGroupSet(4);
				
		GBGroup g1 = set.getGroup(0);
		g1.moniker = "g1";
		g1.name = "Group1";
		g1.ID = 0;
		g1.setAffinity(1,  0.2);
		g1.setAffinity(2,  0.9);
		//groups[0] = g1;
				
		GBGroup g2 = set.getGroup(1);
		g2.moniker = "g2";
		g2.name = "Group2";
		g2.ID = 1;
		g2.setAffinity(2,  0.6);
		g2.setAffinity(3,  0.3);
		//groups[1] = g2;
		
		
		GBGroup g3 = set.getGroup(2);
		g3.moniker = "g3";
		g3.name = "Group3";
		g3.ID = 2;
		g3.setAffinity(3, 0.5);
		g3.setAffinity(0, 0.5);
		//groups[2] = g3;
		
		GBGroup g4 = set.getGroup(3);
		g4.moniker = "g4";
		g4.name = "Group4";
		g4.ID = 3;
		g4.setAffinity(0, 0.8);
		g4.setAffinity(1,0.3);
		//groups[3] = g4;
		
		return set;
	}

	
	//==== GBBill storage ====
	
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
			
			GBGroupSet set = getGroupSet(Utils.NVL(xml.setID, 0));
			GBBill bill = xml.toGBBill(set, null);
			bill.calculateInvAffinities(set);
			this.bills.put(bill.ID, bill);
		}
	}

	public GBBill loadBill(int billID)
	{
		GBBillXML xml = storage.loadBill(billID);
		if (xml == null) return null;
		
		GBGroupSet set = getGroupSet(Utils.NVL(xml.setID, 0));
		GBBill bill = xml.toGBBill(set, null);
		
		bill.calculateInvAffinities(set);
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

	public List<GBBill> getBills(int setID)
	{
		List<GBBill> res = new ArrayList<GBBill>();
		for (GBBill bill : bills.values())
		{
			if (bill.setID == setID)
			{
				res.add(bill);
			}
		}
		return res;
	}
	
	public int getNewBillID(boolean withCreate) {
		return storage.getNewBillID(withCreate);
	}
		
	public void saveBill(GBBill bill) {
		if (bill == null || bill.setID == null)
		{
			throw new IllegalArgumentException("Bill missing");
		}
		
		if (bill.ID == null)
		{
			bill.ID = this.getNewBillID(true);
		}
		if (bill.status == null)
		{
			bill.status = GBBill.StatusEnum.NEW;
		}
		GBBillXML xml = new GBBillXML();
		GBGroupSet set = getGroupSet(bill.setID);
		xml.fromGBBill(set, bill);
		
		storage.saveBill(xml);		
	}
	
	public boolean deleteBill(Integer billID) {
		if (billID == null) return false;
		
		boolean res = storage.deleteBill(billID.intValue());
		if (res)
		{
			bills.remove(billID);
		}
		return res;
	}


	/*
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
		int listSize = groupList.getSize();
		for (GBGroup g: groupList.getGroups())
		{
			for (int g2 = 0; g2 < listSize; g2++)
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
		for (GBGroup g: groupList.getGroups())
		{
			for (int g2 = 0; g2 < listSize; g2++)
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
			GBAffinity aff2 = groupList.getGroup(aff.toID()).getAffinity(toGroup);
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
*/	
	
	//==== GBProfile storage ====
	public GBProfile loadProfile(int profileID)
	{
		GBProfileXML xml = storage.loadProfile(profileID);
		
		if (xml == null) return null;
		GBGroupSet set = getGroupSet(Utils.NVL(xml.setID, 0));
		
		GBProfile profile = xml.toGBProfile(set, null);
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
		GBGroupSet set = getGroupSet(profile.setID);
		GBProfileXML xml = new GBProfileXML();
		profile.saveDate = new Date();
		xml.fromGBProfile(set, profile);
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
