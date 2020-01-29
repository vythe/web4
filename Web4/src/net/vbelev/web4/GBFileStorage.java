package net.vbelev.web4;

import java.util.*;
import java.io.*;
import java.nio.file.Paths;

import net.vbelev.web4.ui.WebUser;
import net.vbelev.web4.utils.*;
import net.vbelev.web4.xml.*;
import net.vbelev.web4.core.*;


public class GBFileStorage implements GBEngine.Storage
{
	public final XMLiser xmliser;
	
	private String dataFolder;
	
	public boolean ping(boolean force) 
	{
		return true;
	}
	
	public GBFileStorage(String root)
	{
		//xmliser = new XMLiser("net.vbelev.web4.xml");
		xmliser = new XMLiser(
				GBGroupListXML.class, 
				GBBillXML.class, 
				GBProfileXML.class, 
				WebUserXML.class);
		
		dataFolder = root;
	}
	
	public GBGroupListXML getGroups()
	{
		File groupList = Paths.get(dataFolder, GBGroupListXML.STORAGE_NAME + ".xml").toFile();
		if (!groupList.exists())
		{
			return null;
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
				return xml;
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("Failed to load engine from lost file " + groupList.getAbsolutePath(), e);
			} catch (IOException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("Failed to load engine from "+ groupList.getAbsolutePath(), e);
			}
			finally
			{
			}				
		}		
	}
	
	public void saveGroups(GBGroupListXML xml)
	{
		//storage
		File groupList = Paths.get(dataFolder,  GBGroupListXML.STORAGE_NAME + ".xml").toFile();
		if (groupList.exists() && !groupList.isFile())
		{
			throw new IllegalArgumentException("Failed to save engine, this is not a file: " + groupList.getAbsolutePath());
		}
		
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
	
	public List<GBBillXML> loadBills()
	{
		String errName = dataFolder;
		ArrayList<GBBillXML> res = new ArrayList<GBBillXML>();
		
		try
		{
		File billList = Paths.get(dataFolder, GBBillXML.STORAGE_NAME).toFile();
		if (!billList.exists())
		{
			return res;
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
					res.add(xml);
				}
				finally
				{
				}
			}
			return res;
		}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Failed to load bill(s) from " + errName, e);
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Failed to load bill(s) from " + errName, e);
		}		
	}
	

	public GBBillXML loadBill(int billID)
	{
		File billFile = Paths.get(dataFolder, GBBillXML.STORAGE_NAME, billID + ".xml").toFile();
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
				
				return xml;
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("Failed to load bill from " + billFile.getAbsolutePath(), e);
			} catch (IOException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("Failed to load bill from " + billFile.getAbsolutePath(), e);
			}
		}
	}	
	
	public int getNewBillID(boolean withCreate) {
		
		//int newID = Utils.NVL(Utils.Max(this.bills.keys()), 0);
		File billList = Paths.get(dataFolder, GBBillXML.STORAGE_NAME).toFile();
		//if (!f.exists() || !f.isDirectory()) return newID + 1;
		int newID = 1;
		if (!billList.exists())
		{
			return 1;
		}
		else if (!billList.isDirectory())
		{
			throw new IllegalArgumentException("Failed to load bills, this is not a directory: " + billList.getAbsolutePath());
		}
		
		String errName = billList.getAbsolutePath();
		
		try
		{
			errName = billList.getAbsolutePath();
			for (File billFile : billList.listFiles((d, s) -> (s.toLowerCase().matches("^\\d+\\.xml"))))
			{
				errName = billFile.getAbsolutePath();
				
				try(InputStream ioBill = new FileInputStream(billFile))
				{				
					GBBillXML xml = this.xmliser.fromXML(GBBillXML.class, ioBill);
					if (xml.ID > newID)
					{
						newID = xml.ID + 1;
					}
					ioBill.close();
				}
				finally
				{
				}
			}
			return newID;
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Failed to load bill(s) from " + errName, e);
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Failed to load bill(s) from " + errName, e);
		}		
	}

	public void saveBill(GBBillXML xml) {
		if (xml == null) return;
		
		if (xml.ID == null)
		{
			xml.ID = this.getNewBillID(true);
		}
		
		File billFolder = Paths.get(dataFolder, GBBillXML.STORAGE_NAME).toFile();
		if (!billFolder.exists())
		{
			billFolder.mkdirs();
		}
		File billFile = Paths.get(dataFolder, GBBillXML.STORAGE_NAME, xml.ID + ".xml").toFile();
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
	
	//==== GBProfile storage ====
	public GBProfileXML loadProfile(int profileID)
	{
		File ProfileFile = Paths.get(dataFolder, GBProfileXML.STORAGE_NAME, profileID + ".xml").toFile();
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
				
				return xml;
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("Failed to load Profile from " + ProfileFile.getAbsolutePath(), e);
			} catch (IOException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("Failed to load Profile from " + ProfileFile.getAbsolutePath(), e);
			}
		}
	}

	public int getNewProfileID(boolean withCreate) {
		
		int maxID = 0;
		File profileFolder = Paths.get(dataFolder, GBProfileXML.STORAGE_NAME).toFile();
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
		if (withCreate)
		{
			File newFile = Paths.get(dataFolder, GBProfileXML.STORAGE_NAME, (maxID + 1) + ".xml").toFile();
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
	
	public void saveProfile(GBProfileXML xml) {
		if (xml == null) return;
		
		if (xml.ID == null)
		{
			xml.ID = this.getNewProfileID(true);
		}
		
		File profileFolder = Paths.get(dataFolder, GBProfileXML.STORAGE_NAME).toFile();
		if (!profileFolder.exists())
		{
			profileFolder.mkdirs();
		}
		File ProfileFile = Paths.get(dataFolder, GBProfileXML.STORAGE_NAME, xml.ID + ".xml").toFile();
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
	public WebUserXML loadWebUser(int webUserID)
	{
		File webUserFile = Paths.get(dataFolder, WebUserXML.STORAGE_NAME, webUserID + ".xml").toFile();
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
				
				return xml;
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("Failed to load WebUser from " + webUserFile.getAbsolutePath(), e);
			} catch (IOException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("Failed to load WebUser from " + webUserFile.getAbsolutePath(), e);
			}
		}
	}
	
	public static final java.util.regex.Pattern xmlIdPattern = java.util.regex.Pattern.compile("^(\\d+).xml$");
	
	public int loadWebUserIndex(Hashtable<String, Integer> index)
	{
		index.clear();
		
		File webUserFolder = Paths.get(dataFolder, WebUserXML.STORAGE_NAME).toFile();
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
	
	public int getNewWebUserID(boolean withCreate) 
	{
		int maxID = 0;
		File webUserFolder = Paths.get(dataFolder, WebUserXML.STORAGE_NAME).toFile();
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
		if (withCreate)
		{
			File newFile = Paths.get(dataFolder, WebUserXML.STORAGE_NAME, (maxID + 1) + ".xml").toFile();
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
	
	public void saveWebUser(WebUserXML xml) {
		if (xml == null) return;
		
		if (xml.ID == null)
		{
			xml.ID = this.getNewWebUserID(true);
		}
		File webUserFolder = Paths.get(dataFolder, WebUserXML.STORAGE_NAME).toFile();
		if (!webUserFolder.exists())
		{
			webUserFolder.mkdirs();
		}
		File webUserFile = Paths.get(dataFolder, WebUserXML.STORAGE_NAME, xml.ID + ".xml").toFile();
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
