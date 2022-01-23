package net.vbelev.bothall.web;

import java.io.IOException;
import java.util.*;
import net.vbelev.utils.*;
import net.vbelev.utils.DryCereal.Reader;
import net.vbelev.bothall.client.*;
import net.vbelev.bothall.core.*;

/**
 * Part of BHSession extracted to a separate class. 
 * It is used to get updates from BHEngine and put it into a BHStorage.UpdateBin instance. 
 */
public class BHStorage
{	
	public final Map<Integer, String> itemCereals = Collections.synchronizedMap(new TreeMap<Integer, String>());
	public final Map<Integer, BHClient.Item> itemExports = Collections.synchronizedMap(new TreeMap<Integer, BHClient.Item>());
	public final Map<Integer, String> mobileCereals = Collections.synchronizedMap(new TreeMap<Integer, String>());
	public final Map<Integer, BHClient.Mobile> mobileExports = Collections.synchronizedMap(new TreeMap<Integer, BHClient.Mobile>());
	public final Map<Integer, String> heroCereals = Collections.synchronizedMap(new TreeMap<Integer, String>());
	public final Map<Integer, BHClient.Mobile> heroExports = Collections.synchronizedMap(new TreeMap<Integer, BHClient.Mobile>());
	public final DryCereal dryer = new DryCereal();
	public final BHClient.ElementCerealCache cache = new BHClient.ElementCerealCache();


	
	public BHStorage()
	{
	}
	
	public void reset()
	{
		itemCereals.clear();
		itemExports.clear();
		mobileCereals.clear();
		mobileExports.clear();
		heroCereals.clear();
		heroExports.clear();		
		
		cache.reset();
	}
	
	public static BHClient.Item bhAtomToItem(BHCollection.Atom atom)
	{
		BHClient.Item item = new BHClient.Item();
		item.id = atom.getID();
		item.x = atom.getIntProp(BHCollection.Atom.INT_PROPS.X);
		item.y = atom.getIntProp(BHCollection.Atom.INT_PROPS.Y);
		item.z = atom.getIntProp(BHCollection.Atom.INT_PROPS.Z);
		item.itemtype = atom.getType();
		item.status = atom.getStatus();
		
		return item;
	}
	
	public static BHClient.Mobile bhAtomToMobile(BHCollection.Atom atom)
	{
		BHClient.Mobile mobile = new BHClient.Mobile();
		mobile.id = atom.getID();
		mobile.x = atom.getIntProp(BHCollection.Atom.INT_PROPS.X);
		mobile.y = atom.getIntProp(BHCollection.Atom.INT_PROPS.Y);
		mobile.z = atom.getIntProp(BHCollection.Atom.INT_PROPS.Z);
		mobile.dir = atom.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR);
		mobile.moveTick =atom.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC); // + BHOperations.MOVE_SPEED;
		mobile.status = atom.getStatus();
		mobile.mobiletype = atom.getType();
		mobile.name = atom.getStringProp(BHCollection.Atom.STRING_PROPS.NAME);
		
		return mobile;
	}	
	
	public static BHClient.Mobile bhAtomToHero(BHCollection.Atom atom)
	{
		BHClient.Mobile mobile = new BHClient.Mobile();
		mobile.id = atom.getID();
		mobile.x = atom.getIntProp(BHCollection.Atom.INT_PROPS.X);
		mobile.y = atom.getIntProp(BHCollection.Atom.INT_PROPS.Y);
		mobile.z = atom.getIntProp(BHCollection.Atom.INT_PROPS.Z);
		mobile.dir = atom.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_DIR);
		mobile.moveTick =atom.getIntProp(BHCollection.Atom.INT_PROPS.MOVE_TC); // + BHOperations.MOVE_SPEED;
		mobile.status = atom.getStatus();
		mobile.mobiletype = atom.getType();
		mobile.name = atom.getStringProp(BHCollection.Atom.STRING_PROPS.NAME);
		
		return mobile;
	}
	
	public static BHClient.Cell bhCellToClient(BHLandscape.Cell c)
	{
		BHClient.Cell cell = new BHClient.Cell();
		cell.id = c.getID();
		cell.terrain = c.getTerrain().name().intern();
		cell.x = c.getX();
		cell.y = c.getY();
		cell.z = c.getZ();
		
		return cell;
	}
	
	
	/** This works over the published atom */
	public BHClient.Item atomToItemCache(BHCollection.Atom atom)
	{
		BHClient.Item item = bhAtomToItem(atom);
		
		try
		{
			// we need to go through BHClient.Item to make sure the serialization is consistent.
			item.toCereal(dryer);
			String cereal = dryer.pull();
			itemExports.put(item.id, item);
			itemCereals.put(item.id, cereal);
			
			return item;
		}
		catch (IOException x)
		{
			return null;			
		}
	}
	
	/** This works over the published atom.
	 * If it's too slow, we'll go back to the methods inside BHCollection
	 *  */
	public BHClient.Mobile atomToMobileCache(BHCollection.Atom atom)
	{
		BHClient.Mobile mobile = bhAtomToMobile(atom);
		
		try
		{
			mobile.toCereal(dryer);
			String cereal = dryer.pull();
			mobileExports.put(mobile.id, mobile);
			mobileCereals.put(mobile.id, cereal);
			
			return mobile;
		}
		catch (IOException x)
		{
			return null;
			
		}
	}	
	
	public BHClient.Mobile atomToHeroCache(BHCollection.Atom atom)
	{
		BHClient.Mobile mobile = bhAtomToHero(atom);
		
		try
		{
			mobile.toCereal(dryer);
			String cereal = dryer.pull();
			mobileExports.put(mobile.id, mobile);
			mobileCereals.put(mobile.id, cereal);
			
			return mobile;
		}
		catch (IOException x)
		{
			return null;
			
		}
	}	
	
	public BHClient.UpdateBin getUpdate(BHBoard engine, long timecode, int subscriptionID, int mobileID)
	{
		BHClient.UpdateBin res = new BHClient.UpdateBin();
		res.status = new BHClient.Status();
	
		return getUpdate(res, engine, timecode, subscriptionID, mobileID);
	}	
	
	/** returns an update set from the given timecode until now */
	protected BHClient.UpdateBin getUpdate(BHClient.UpdateBin res, BHBoard engine, long timecode, int subscriptionID, int mobileID)
	{
	
		for(BHCollection.Atom a : engine.getCollection().allByTimecode(timecode + 1))
		{
			if (a.getGrade() == BHCollection.Atom.GRADE.ITEM)
			{
				BHClient.Item item = itemExports.get(a.getID());
				item = null; // disable the cache for now.
				if (item == null)
				{
					item = atomToItemCache(a);
				}
				if (item != null)
				{
					res.items.add(item);
				}
			}
			else if (a.getGrade() == BHCollection.Atom.GRADE.MONSTER || a.getGrade() == BHCollection.Atom.GRADE.HERO)
			{
				
				BHClient.Mobile mobile = mobileExports.get(a.getID());
				mobile = null; // disable the cache for now.
				if (mobile == null)
				{
					mobile = atomToMobileCache(a);
				}
				if (mobile != null)
				{
					res.mobiles.add(mobile);
				}
			}
		}
		
		for (BHLandscape.Cell c : engine.getLandscape().cells)
		{
			if (c.getTimecode() >= timecode)
			{
				BHClient.Cell cell = bhCellToClient(c);
				res.cells.add(cell);
			}
		}
		
		res.status.cycleLoad = engine.cycleLoad;
		res.status.cycleMsec = (int)engine.CYCLE_MSEC;
		//res.status.sessionID = this.sessionID;
		//res.status.controlledMobileID = 0; // we don't know the controlled id here. maybe it should be moved
		res.status.controlledMobileID = mobileID;
		res.status.timecode = engine.timecode;
		// sessionStatus should be set in the calling procedure - they know it better
		res.status.sessionStatus = engine.isRunning? BHClient.Status.SessionStatus.ACTIVE : BHClient.Status.SessionStatus.DEAD;
		//res.status.updateTS = engine.pub
		
		// everything is visible for now
		for (BHMessageList.Message m : engine.getMessages().getMessages(subscriptionID))
		{
			BHClient.Message message = new BHClient.Message();
			message.id = m.ID;
			message.message = m.message;
			message.targetID = m.targetID;
			message.targetType = m.target.name();
			res.messages.add(message);
		}

		for (BHEngine.Buff b : engine.buffs)
		{
			if (!b.isVisible || b.isCancelled) continue;
			BHClient.Buff b2 = new BHClient.Buff();
			b2.id = b.action.ID;
			//b2.isCancelled = b.isCancelled;
			b2.ticks = b.ticks;
			b2.timecode = b.timecode;
			b2.type = b.action.actionType;
			b2.actorID = b.action.actorID;
			b2.actorType = 0; //Utils.getEnumCode(b.actorType);
			
			res.buffs.add(b2);
			
		}
		return res;
	}

}
