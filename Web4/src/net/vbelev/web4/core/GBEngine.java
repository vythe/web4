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

	private String dataFolder;
	
	private GBGroup[] groups = new GBGroup[0];
	public final Hashtable<Integer, GBBill> bills = new Hashtable<Integer, GBBill>();
	
	public final XMLiser xmliser;
	public final Random random = new Random();
	
	public static final java.util.regex.Pattern xmlIdPattern = java.util.regex.Pattern.compile("^(\\d+).xml$");
	public static final java.util.regex.Pattern webUserLoginPattern = 
			java.util.regex.Pattern.compile("^[a-z][a-z\\d_]+$");
	
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
	
	public static GBEngine loadEngine(String root)
	{
		GBEngine res = new GBEngine(root);
		res.loadGroups();
		res.loadBills();
		return res;
	}
	
	public void loadGroups()
	{
		//File groupList = new File(dataFolder + "/" + GBGroupListXML.STORAGE_NAME);
		File groupList = Paths.get(dataFolder, GBGroupListXML.STORAGE_NAME).toFile();
		if (!groupList.exists())
		{
			this.testSet();
			this.saveGroups();
		}
		else if (!groupList.isFile())
		{
			throw new IllegalArgumentException("Failed to load engine, this is not a file: " + groupList.getAbsolutePath());
		}
		else
		{
			try(InputStream ioGroupList = new FileInputStream(groupList))
			{
				GBGroupListXML xml = this.xmliser.fromXML(GBGroupListXML.class, ioGroupList);
				ioGroupList.close();
				
				xml.toEngine(this);
				this.calculateAll();
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("Failed to load engine from lost file " + groupList.getAbsolutePath(), e);
			} catch (IOException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("Failed to load engine from " + groupList.getAbsolutePath(), e);
			}
			finally
			{
			}
		}
	}
	
	public void saveGroups()
	{
		File groupList = Paths.get(dataFolder,  GBGroupListXML.STORAGE_NAME).toFile();
		if (groupList.exists() && !groupList.isFile())
		{
			throw new IllegalArgumentException("Failed to save engine, this is not a file: " + groupList.getAbsolutePath());
		}
		
		GBGroupListXML xml = new GBGroupListXML();
		xml.fromEngine(this);
		try(FileOutputStream ioGroupList = new FileOutputStream(groupList))
		{
			xmliser.toXML(ioGroupList, xml);
			ioGroupList.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Failed to save engine to " + groupList.getAbsolutePath()
			+ "FNFException: " + e.getMessage(), e);
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Failed to save engine to " + groupList.getAbsolutePath()
			+ "IOException: " + e.getMessage(), e);
		}
		finally
		{
		}
	}
	
	/** 
	 * for now, we'll load all bills at once and worry later.
	 */
	public void loadBills()
	{
		String errName = dataFolder;
		try
		{
			this.bills.clear();
		File billList = Paths.get(dataFolder, GBBillXML.STORAGE_FOLDER).toFile();
		if (!billList.exists())
		{
		this.testSet();
		}
		else if (!billList.isDirectory())
		{
			throw new IllegalArgumentException("Failed to load bills, this is not a directory: " + billList.getAbsolutePath());
		}
		else
		{
			errName = billList.getAbsolutePath();
			for (File billFile : billList.listFiles((d, s) -> (s.toLowerCase().matches("^\\d+\\.xml"))))
			{
				errName = billFile.getAbsolutePath();
				
				try(InputStream ioBill = new FileInputStream(billFile))
				{
					GBBillXML xml = this.xmliser.fromXML(GBBillXML.class, ioBill);
					ioBill.close();
					
					GBBill bill = xml.toGBBill(this, null);
					bill.calculateInvAffinities(this);
					this.bills.put(bill.ID, bill);
				}
				finally
				{
				}
			}
		}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Failed to load bill(s) from " + errName, e);
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Failed to load bill(s) from " + errName, e);
		}		
	}

	public GBBill loadBill(int billID)
	{
		File billFile = Paths.get(dataFolder, GBBillXML.STORAGE_FOLDER, billID + ".xml").toFile();
		if (!billFile.exists() || !billFile.isFile())
		{
			return null;
		}
		else
		{
			try(InputStream ioBill = new FileInputStream(billFile))
			{
				GBBillXML xml = this.xmliser.fromXML(GBBillXML.class, ioBill);
				ioBill.close();
				
				GBBill bill = xml.toGBBill(this, null);
				bill.calculateInvAffinities(this);
				//this.bills.put(bill.ID, bill);
				return bill;
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("Failed to load bill from " + billFile.getAbsolutePath(), e);
			} catch (IOException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("Failed to load bill from " + billFile.getAbsolutePath(), e);
			}
		}
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
	
	public int getNewBillID(boolean createFile) {
		
		int newID = Utils.NVL(Utils.Max(this.bills.keys()), 0);
		File f = Paths.get(dataFolder, GBBillXML.STORAGE_FOLDER).toFile();
		if (!f.exists() || !f.isDirectory()) return newID + 1;
		
		try
		{
		do
		{
			newID++;
			f = Paths.get(dataFolder, GBBillXML.STORAGE_FOLDER, newID + ".xml").toFile();
		}
		while ((createFile && !f.createNewFile())
				|| (!createFile && f.exists())
		);
		}
		catch (IOException x)
		{
			throw new IllegalArgumentException("Failed to check for file " + f);
		}
		return newID;
	}
	
	public void saveBill(GBBill bill) {
		if (bill == null) return;
		
		if (bill.ID == null)
		{
			bill.ID = this.getNewBillID(true);
		}
		GBBillXML xml = new GBBillXML();
		xml.fromGBBill(this, bill);
		
		File billFolder = Paths.get(dataFolder, GBBillXML.STORAGE_FOLDER).toFile();
		if (!billFolder.exists())
		{
			billFolder.mkdirs();
		}
		File billFile = Paths.get(dataFolder, GBBillXML.STORAGE_FOLDER, xml.ID + ".xml").toFile();
		try(FileOutputStream ioBill = new FileOutputStream(billFile))
		{
			xmliser.toXML(ioBill, xml);
			ioBill.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Failed to save engine, this is not a file: " + billFile.getAbsolutePath(), e);
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Failed to save engine to " + billFile.getAbsolutePath(), e);
		}
		finally
		{
		}
		
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
		Double[] listTo = this.getAffinitesTo(toGroup);
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
	public Double[] getAffinitesTo(int toID)
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
		File ProfileFile = Paths.get(dataFolder, GBProfileXML.STORAGE_FOLDER, profileID + ".xml").toFile();
		if (!ProfileFile.exists() || !ProfileFile.isFile())
		{
			return null;
		}
		else
		{
			try(InputStream ioProfile = new FileInputStream(ProfileFile))
			{
				GBProfileXML xml = this.xmliser.fromXML(GBProfileXML.class, ioProfile);
				ioProfile.close();
				
				GBProfile profile = xml.toGBProfile(this, null);
				return profile;
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("Failed to load Profile from " + ProfileFile.getAbsolutePath(), e);
			} catch (IOException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("Failed to load Profile from " + ProfileFile.getAbsolutePath(), e);
			}
		}
	}

	public GBProfile getProfile(Integer profileID)
	{
		if (profileID == null) return null;
		
		GBProfile profile = loadProfile(profileID.intValue());
		return profile;
	}
	
	public int getNewProfileID(boolean createFile) {
		
		int maxID = 0;
		File profileFolder = Paths.get(dataFolder, GBProfileXML.STORAGE_FOLDER).toFile();
		if (!profileFolder.exists() || !profileFolder.isDirectory()) return maxID + 1;
		
		java.util.regex.Pattern p = java.util.regex.Pattern.compile("^(\\d+).xml$");
		for (File f : profileFolder.listFiles())
		{
			java.util.regex.Matcher pm = p.matcher(f.getName().toLowerCase());
			if (pm.matches())
			{
				Integer fileID = Utils.tryParseInt(pm.group(1));
				if (fileID != null && fileID > maxID)
				{
					maxID = fileID.intValue();
				}
			}									
		}
		if (createFile)
		{
			File newFile = Paths.get(dataFolder, GBProfileXML.STORAGE_FOLDER, (maxID + 1) + ".xml").toFile();
			try
			{
				newFile.createNewFile();
			}
			catch (IOException x)
			{
				throw new IllegalArgumentException("Failed to create profile file: " + newFile.getAbsolutePath(), x);
			}
		}
		return maxID + 1;
	}
	
	public void saveProfile(GBProfile profile) {
		if (profile == null) return;
		
		if (profile.ID == null)
		{
			profile.ID = this.getNewProfileID(true);
		}
		GBProfileXML xml = new GBProfileXML();
		xml.fromGBProfile(this, profile);
		
		File profileFolder = Paths.get(dataFolder, GBProfileXML.STORAGE_FOLDER).toFile();
		if (!profileFolder.exists())
		{
			profileFolder.mkdirs();
		}
		File ProfileFile = Paths.get(dataFolder, GBProfileXML.STORAGE_FOLDER, xml.ID + ".xml").toFile();
		try(FileOutputStream ioProfile = new FileOutputStream(ProfileFile))
		{
			xmliser.toXML(ioProfile, xml);
			ioProfile.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Failed to save profile, this is not a file: " + ProfileFile.getAbsolutePath(), e);
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Failed to save profile to " + ProfileFile.getAbsolutePath(), e);
		}
		finally
		{
		}		
	}
	
	//==== WebUser storage ====
	public WebUser loadWebUser(int webUserID)
	{
		File webUserFile = Paths.get(dataFolder, WebUserXML.STORAGE_FOLDER, webUserID + ".xml").toFile();
		if (!webUserFile.exists() || !webUserFile.isFile())
		{
			return null;
		}
		else
		{
			try(InputStream ioWebUser = new FileInputStream(webUserFile))
			{
				WebUserXML xml = this.xmliser.fromXML(WebUserXML.class, ioWebUser);
				ioWebUser.close();
				
				WebUser webUser = xml.toWebUser(null);
				return webUser;
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("Failed to load WebUser from " + webUserFile.getAbsolutePath(), e);
			} catch (IOException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("Failed to load WebUser from " + webUserFile.getAbsolutePath(), e);
			}
		}
	}

	public int loadWebUserIndex(Hashtable<String, Integer> index)
	{
		index.clear();
		
		File webUserFolder = Paths.get(dataFolder, WebUserXML.STORAGE_FOLDER).toFile();
		if (!webUserFolder.exists() || !webUserFolder.isDirectory()) return 0;
		
		for (File f : webUserFolder.listFiles())
		{
			java.util.regex.Matcher pm = xmlIdPattern.matcher(f.getName().toLowerCase());
			if (f.isFile() && pm.matches())
			{
				//Integer fileID = Utils.tryParseInt(pm.group(1));
				try(InputStream ioWebUser = new FileInputStream(f))
				{
					WebUserXML xml = this.xmliser.fromXML(WebUserXML.class, ioWebUser);
					ioWebUser.close();
					index.put(Utils.NVL(xml.login, "#" + xml.ID).trim().toLowerCase(), xml.ID);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					//throw new IllegalArgumentException("Failed to load WebUser from " + webUserFile.getAbsolutePath(), e);
				} catch (IOException e) {
					e.printStackTrace();
					//throw new IllegalArgumentException("Failed to load WebUser from " + webUserFile.getAbsolutePath(), e);
				}
			}									
		}
		return index.size();
	}
	
	public WebUser getWebUser(Integer webUserID)
	{
		if (webUserID == null) return null;
		
		WebUser webUser = loadWebUser(webUserID.intValue());
		return webUser;
	}
	
	public int getNewWebUserID(boolean createFile) {
		
		int maxID = 0;
		File webUserFolder = Paths.get(dataFolder, WebUserXML.STORAGE_FOLDER).toFile();
		if (!webUserFolder.exists() || !webUserFolder.isDirectory()) return maxID + 1;
		
		for (File f : webUserFolder.listFiles())
		{
			java.util.regex.Matcher pm = xmlIdPattern.matcher(f.getName().toLowerCase());
			if (pm.matches())
			{
				Integer fileID = Utils.tryParseInt(pm.group(1));
				if (fileID != null && fileID > maxID)
				{
					maxID = fileID.intValue();
				}
			}									
		}
		if (createFile)
		{
			File newFile = Paths.get(dataFolder, WebUserXML.STORAGE_FOLDER, (maxID + 1) + ".xml").toFile();
			try
			{
				newFile.createNewFile();
			}
			catch (IOException x)
			{
				throw new IllegalArgumentException("Failed to create web user file: " + newFile.getAbsolutePath(), x);
			}
		}
		return maxID + 1;
	}
	
	public void saveWebUser(WebUser webUser) {
		if (webUser == null) return;
		
		if (webUser.ID == null)
		{
			webUser.ID = this.getNewWebUserID(true);
		}
		WebUserXML xml = new WebUserXML();
		xml.fromWebUser(webUser);
		
		File webUserFolder = Paths.get(dataFolder, WebUserXML.STORAGE_FOLDER).toFile();
		if (!webUserFolder.exists())
		{
			webUserFolder.mkdirs();
		}
		File webUserFile = Paths.get(dataFolder, WebUserXML.STORAGE_FOLDER, xml.ID + ".xml").toFile();
		try(FileOutputStream ioWebUser = new FileOutputStream(webUserFile))
		{
			xmliser.toXML(ioWebUser, xml);
			ioWebUser.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Failed to save profile, this is not a file: " + webUserFile.getAbsolutePath(), e);
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Failed to save profile to " + webUserFile.getAbsolutePath(), e);
		}
		finally
		{
		}
		
	}
	
}
