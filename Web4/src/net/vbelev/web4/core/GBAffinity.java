package net.vbelev.web4.core;

import java.util.*;

/**
 * A small data holder for affinities.
 * The value of 0.3 means "30% of my members are in the group toID" 
 * @author vythe
 *
 */
public class GBAffinity implements Cloneable 
{

	public static enum QualityEnum
	{
		NONE,
		CALCULATED,
		SET
	}
	
	public static final GBAffinity EMPTY = new GBAffinity();
	
	private int m_toID;
	private long m_value;
	private QualityEnum m_quality;
	
	private GBAffinity()
	{
		m_quality = QualityEnum.NONE;
	}
	
	public GBAffinity(int toID, double value)
	{
		m_toID = toID;
		m_value = (int)(value * 1000);
		m_quality = QualityEnum.SET;
	}
	
	public GBAffinity(int toID, double value, QualityEnum quality)
	{
		m_toID = toID;
		m_value = (int)(value * 1000);
		m_quality = quality;
		
	}
	
	public synchronized Object clone()
	{
		GBAffinity res = new GBAffinity(); //this.m_toID, this.m_value, this.m_quality);
		res.m_toID = this.m_toID;
		res.m_value = this.m_value;
		res.m_quality = this.m_quality;
		
		return res;
	}
	
	public String toString() 
	{
		return "[to " + m_toID + ": " + value() + " " + m_quality.name() + "]"; 
	}
	
	public int toID()
	{
		return m_toID;
	}
	public double value()
	{
		return (double)m_value / 1000.;
	}
	public QualityEnum quality()
	{
		return m_quality;		
	}
	
	public boolean equals(Object obj)
	{
		if (obj == null) return m_quality == QualityEnum.NONE;
		if (!(obj instanceof GBAffinity)) return false;
		GBAffinity other = (GBAffinity)obj;
		
		return (
				this.m_toID == other.m_toID
				&& this.m_value == other.m_value
				&& this.m_quality == other.m_quality
				);
	}
	
	/**
	 * Calculates the affinity of "from" group to "to" group.
	 * @param listFrom list of affinities of the "from" group to everybody
	 * @param listTo list of affinities of all groups to the "to" group
	 * @return
	 */
	public static Double calculateAffinity(Double[] listFrom, Double[] listTo)
	{
		double nom = 0;
		double denom = 0;
		if (listFrom == null || listTo == null || listFrom.length != listTo.length)
		{
			throw new IllegalArgumentException("[from] and [to] lists do not match");
		}
		
		for (int i = 0; i < listFrom.length; i++)
		{
			if (listFrom[i] == null || listTo[i] == null) continue;
			//nom += listFrom[i] * listFrom[i];
			//denom += listFrom[i] * listFrom[i] * listTo[i];
			nom += listFrom[i] * listFrom[i] * listTo[i];
			denom += listFrom[i] * listFrom[i];
		}
		if (denom <= 0)
		{
			return null;
		}
		double res = nom / denom;
		if (res > 1)
		{
			System.out.print("weird affinity: " + res);
		}
		return res;
	}

}
