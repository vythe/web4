package net.vbelev.filebox;
import java.util.*;
import java.io.*;
import org.htmlcleaner.*;
/**
 * 
 */
public class TextConstellation
{
	public final List<TextStar> _texts = new ArrayList<TextStar>();
	// the [0] domain is the "unassigned" start
	public final List<WordMetric.Vector> _domains = new ArrayList<WordMetric.Vector>();
	
	private final WordMetric _metric;
	private WordMetric.Vector _centerVector;
	//public List<String> lines = new ArrayList<String>();
	public String error;
	
	public String weightStr;
	
	public TextConstellation()
	{
		_metric = new WordMetric();
		_centerVector = _metric.new Vector();
	}
	
	public WordMetric getMetric() { return _metric; }
	
	public WordMetric.Vector getCenter() { return _centerVector; }
	
	public List<TextStar> texts()
	{
		return _texts;
	}
	
	public int domainSize(int domainIndex)
	{
		if (domainIndex < 0)
			return _texts.size();
		int cnt = 0;
		for (TextStar text : _texts) 
		{
			if (text.domainIndex == domainIndex)
			{
				cnt++;
			}
		}
		return cnt;
	}
	
	public WordMetric.Vector domainCenter(int domainIndex)
	{
		int cnt = 0;
		WordMetric.Vector res = _metric.new Vector();
		for (TextStar text : _texts) 
		{
			if (domainIndex < 0 || text.domainIndex == domainIndex)
			{
				res = res.add(text.vector);
				cnt++;
			}
		}
		if (cnt > 0)
		{
			for (WordMetric.VectorValue entry: res.values(false))
			{
				res.put(entry.index, entry.value / cnt);
			}
		}
		return res;
	}
	
	public void recalcDomains()
	{
		int[] counts = new int[_domains.size()];
		WordMetric.Vector[] centers = new WordMetric.Vector[_domains.size()];
		for (int i = 0; i < _domains.size(); i++)
		{
			counts[i] = 0;
			centers[i] = _metric.new Vector();
		}
		
		for (TextStar text : _texts) 
		{
			if (text.domainIndex < 0 || text.domainIndex >= centers.length)
				continue;
			centers[text.domainIndex] = centers[text.domainIndex].add(text.vector);
			counts[text.domainIndex]++;
		}

		for (int i = 0; i < _domains.size(); i++)
		{
			if (counts[i] > 0)
			{
				for (WordMetric.VectorValue entry: centers[i].values(false))
				{
					centers[i].put(entry.index, entry.value / counts[i]);
				}
			}
			_domains.set(i, centers[i]);
		}
	}
	
	public WordMetric.Vector recalcDomain(int domainIndex)
	{
		int count = 0;
		WordMetric.Vector center = _metric.new Vector();
		
		for (TextStar text : _texts) 
		{
			if (text.domainIndex != domainIndex)
				continue;
			center = center.add(text.vector);
			count++;
		}

		if  (count > 0)
		{
			for (WordMetric.VectorValue entry: center.values(false))
			{
				center.put(entry.index, entry.value / count);
			}
		}
		return center;
	}
	
	public int addDomain(WordMetric.Vector vector)
	{
		synchronized(_domains)
		{
			_domains.add(vector.clone());
			return _domains.size() - 1;
		}
	}
	
	/**
	 * The floating domains method: if the star is closer to the center than to any domain,
	 * then form a new domain for that star.
	 */
	public int chooseDomain(TextStar ts)
	{
		float centerDist = ts.vector.diff(getCenter()).norm();
		int bestIndex = 0;
		float bestDist = centerDist;
		for (int i = 1; i < _domains.size(); i++)
		{
			//if (i == ts.domainIndex)
			//	continue; // try to pull apart existing domains 
			long timeStart = System.currentTimeMillis();
			float domainDist = ts.vector.diff(_domains.get(i)).norm();
			long timeEnd = System.currentTimeMillis();
			//System.out.println("chooseDomain, i=" + i + ", elapsed=" + ((timeEnd - timeStart) / 1000) + ", source=" + ts.source);
			if (domainDist < bestDist - WordMetric.MIN_VECTOR_VALUE)
			{
				bestIndex = i;
				bestDist = domainDist;
			}
		}
		if (bestIndex == 0 && ts.domainIndex == 0)
		{
			ts.domainIndex = addDomain(ts.vector); //test
		}
		else if (bestIndex > 0)
		{
			ts.domainIndex = bestIndex;
		}
		return ts.domainIndex;
	}
	
	public TextConstellation TestInit()
	{
		TextConstellation res =  new TextConstellation();
	/*
		try
		{
		File f = new File("E:\\Download\\Texts\\Wiki1\\en.wikipedia.org\\wiki\\Apollo_1.html");
		
		InputStreamReader reader = new InputStreamReader(new FileInputStream(f));
		BufferedReader br = new BufferedReader(reader);
		String line;
		HtmlCleaner cleaner = new HtmlCleaner();
		while ((line = br.readLine()) != null)
		{
			line = cleaner.clean(line).getText().toString();
			
			res.lines.add(line);
		}
		br.close();
		}
		catch (IOException x)
		{
			res.error = x.getMessage();
		}
		*/
		try
		{
			File d = new File("E:\\Download\\Texts\\Wiki1\\en.wikipedia.org\\wiki\\");
			//File d = new File("E:\\Download\\Texts\\Wiki1\\en.wikipedia.org\\");
			for (File f : d.listFiles((File dir, String name) -> { return name.toLowerCase().endsWith(".html"); }))
			{
				TextStar ts = new TextStar();
				ts.source = f.getName();
				ts.fromStream(_metric, new FileInputStream(f));
				_texts.add(ts);
				System.out.println("added file " + ts.source);
			}				
			
			// try to cut off some entries
			WordMetric.Vector weights = _metric.new Vector();
			for (TextStar ts : _texts)
			{
				weights = weights.add(ts.vector);
			}
			String topWeights = weights.TopEntriesText(10);
			float cutoff = _texts.size() * 0.5f;
			List<WordMetric.VectorValue> vals = new ArrayList<WordMetric.VectorValue>();
			for (WordMetric.VectorValue v : weights.values(false))
			{
				if (v.value >= cutoff)
					vals.add(v);
			}
			
			for (WordMetric.VectorValue v : vals)
			{
				_metric.setEnabled(v.index,  false);
				System.out.println("disabled: " + _metric.getWord(v.index));
			}
			
			weightStr = weights.TopEntriesText(30);
			
			_centerVector = domainCenter(-1);
			addDomain(_centerVector); // this is the domain 0
			ArrayList<TextStar> textList = new ArrayList<TextStar>();
			textList.addAll(_texts);
			Collections.reverse(textList);
			for (TextStar ts : textList)
			{
				chooseDomain(ts);
				System.out.println("domain set " + ts.domainIndex + " for " + ts.source);
				if (ts.domainIndex == 0)
				{
					System.out.println("domain remained " + ts.domainIndex + " for " + ts.source);
				}
				recalcDomains();
			}
			System.out.println("pass 1 complete");
			int changeCount = 0;
			for (int pass = 2; pass <= 10; pass++)
			{
				changeCount = 0;
			for (TextStar ts : textList)
			{
				int oldDomain = ts.domainIndex;
				chooseDomain(ts);
				if (ts.domainIndex != oldDomain)
				{
					changeCount++;
					System.out.println("domain changed from " + oldDomain + " to " + ts.domainIndex + " for " + ts.source);
				}
				recalcDomains();
			}
			System.out.println("pass " + pass + " complete, changes: " + changeCount);
			if (changeCount == 0)
				break;
			}
			System.out.println("all done");
		}
		catch (IOException x)
		{
			res.error = x.getMessage();
		}
		
		return res;
	}
}
