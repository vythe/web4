package net.vbelev.web4.core;

/**
 * A small data holder for affinities.
 * The value of 0.3 means "30% of my members are in the group toID" 
 * @author vythe
 *
 */
public class GBAffinity {

	public enum QualityEnum
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
}
