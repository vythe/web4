/**
 * 
 */
package net.vbelev.filebox.test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.*;
import net.vbelev.filebox.*;

/**
 * @author Vythe
 *
 */
class WordMetricTest {

	private static WordMetric _metric;
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		_metric = new WordMetric();
		
		for (int i = 1; i <= 100000; i ++)
			_metric.getWordIndex("WORD" + i);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	//@Test
	void TestEnabledFlag() 
	{
		//fail("Not yet implemented");
		WordMetric metric = new WordMetric();
		for (int i = 1; i <= 10; i ++)
			metric.getWordIndex("WORD" + i);
		
		String test1 = metric.print(10, false,  false);
		String trueVal1 = "0:, 1:WORD1, 2:WORD2, 3:WORD3, 4:WORD4, 5:WORD5, 6:WORD6, 7:WORD7, 8:WORD8, 9:WORD9, 10:WORD10";
		assertEquals(trueVal1, test1);
		
		metric.setEnabled(10,  false);
		metric.setEnabled(4,  false);
		String test2 = metric.print(10, true,  false);
		String trueVal2 = "1:WORD1, 2:WORD2, 3:WORD3, 5:WORD5, 6:WORD6, 7:WORD7, 8:WORD8, 9:WORD9";
		assertEquals(trueVal2, test2);
	}

	private WordMetric.Vector randomVector(double shrink)
	{
		WordMetric.Vector res = _metric.new Vector();
		int size = (int)(_metric.size() * shrink);
		for (int i = 1; i < size ; i++)
		{
			int ind = (int)Math.floor(Math.random() * _metric.size()); 
			res.put(ind,  (float)Math.random());
		}
		return res;
	}
	
	/**
	 * Test what WordMetric.values() returns all its values properly 
	 */
	//@Test
	void testMetricValues()
	{
		List<WordMetric.IndexedWord> wordValues = new ArrayList<WordMetric.IndexedWord>(
				WordMetric.iterableToCollection(_metric.values())
		);
		Collection<WordMetric.IndexedWord> wordAllValues = _metric.allValues();
		//wordAllValues.add(new WordMetric.IndexedWord(999999, "test"));
		for(WordMetric.IndexedWord w : wordValues)
		{
			assertTrue(wordAllValues.contains(w), "wordValues has extra " + w);
		}
		for(WordMetric.IndexedWord w : wordAllValues)
		{
			assertTrue(wordValues.contains(w), "wordAllValues has extra " + w);
		}
	}
	
	/**
	 * Test what WordMetric.values() returns all its values properly 
	 */
	@Test
	void testVectorValues()
	{
		WordMetric.Vector v1 = randomVector(0.1);
		List<WordMetric.VectorValue> vectorValues = new ArrayList<WordMetric.VectorValue>(
				WordMetric.iterableToCollection(v1.values(false))
		);
		Collection<WordMetric.VectorValue> vectorAllValues = v1.allValues();
		//vectorAllValues.add(new WordMetric.VectorValue(999999, 0.17f));
		for(WordMetric.VectorValue w : vectorValues)
		{
			assertTrue(vectorAllValues.contains(w), "vectorValues has extra " + w);
		}
		for(WordMetric.VectorValue w : vectorAllValues)
		{
			assertTrue(vectorValues.contains(w), "vectorAllValues has extra " + w);
		}
	}

	@Test
	void TestDiff()
	{
		WordMetric.Vector v1 = randomVector(0.5);
		for (int i = 0; i < 100; i++)
		{
			WordMetric.Vector v2 = randomVector(0.5); // 4.7 sec to create the vectors at 0.1
			WordMetric.Vector d = v1.diff(v2); // 11.5 sec with diff() at 10000
			//WordMetric.Vector d1 = v1.diff1(v2); // 76.5 sec with diff1() at 10000
			//WordMetric.Vector d2 = v1.diff2(v2); // 10.8 sec with diff2() at 10000
			//org.junit.jupiter.api.Assertions.assertTrue(d.equals(d1));
			//org.junit.jupiter.api.Assertions.assertTrue(d.equals(d2));
			
			for (WordMetric.VectorValue v : v1.values(true))
			{
				float d1 = v.value - v2.get(v.index);
				if (d1 < 0) d1 = -d1;
				float d2 = d.get(v.index);
				assertTrue(Math.abs(d1 - d2) < WordMetric.MIN_VECTOR_VALUE, "failed at i=" + v.index);
			}
		}
	}

	//@Test
	void TestDiffPerformance()
	{
		WordMetric.Vector v1 = randomVector(0.05);
		float best = 0;
		for (int i = 0; i < 1000; i++)
		{
			WordMetric.Vector v2 = randomVector(0.05); // 4.7 sec to create the vectors at 0.1
			WordMetric.Vector d = v1.diff(v2); // 11.5 sec with diff() at 10000
			float dnorm = d.norm();
			if (dnorm > best)
				best = dnorm;
			//WordMetric.Vector d1 = v1.diff1(v2); // 76.5 sec with diff1() at 10000
			//WordMetric.Vector d2 = v1.diff2(v2); // 10.8 sec with diff2() at 10000
		}
		System.out.println("best=" + best);
	}
	
	//@Test
	void TestAdd()
	{
		WordMetric metric = new WordMetric();
		for (int i = 1; i <= 10; i ++)
			metric.getWordIndex("WORD" + i);

		WordMetric.Vector v1 = metric.new Vector();
		v1.put(1, 0.5f);
		v1.put(2, 0.5f);
		v1.put(3, 0.5f);

		WordMetric.Vector v2 = metric.new Vector();
		v2.put(2, 0.5f);
		v2.put(4, 0.5f);
		v2.put(5, 0.5f);
		
		//metric.setEnabled(5,  false);
		
		WordMetric.Vector d = v1.add(v2);
	
		System.out.println("keys=" + d.keys());
		System.out.println("vals=" + d.toString());
		System.out.println("top=" + d.TopEntriesText(100));
		assertEquals("1,2,3,4,5", d.keys());
		assertEquals("1:0.5,2:1.0,3:0.5,4:0.5,5:0.5", d.toString());
	}
	
	void TestAdd2()
	{
		WordMetric.Vector v1 = randomVector(0.3);
		WordMetric.Vector v2 = randomVector(0.3);
		WordMetric.Vector d = v1.add(v2);
		
	}
	
	//@Test
	void TestLoad() throws IOException
	{
		File f = new File("E:\\Download\\Texts\\Wiki1\\en.wikipedia.org\\wiki\\Milt_Windler.html");
		WordMetric metric = new WordMetric();
		
		TextStar ts = new TextStar();
		ts.source = f.getName();
		try (InputStream s = new FileInputStream(f))
		{
			ts.fromStream(metric, s);
		}
		finally
		{
		}
		Collection<WordMetric.VectorValue> vals = WordMetric.iterableToCollection(ts.vector.values(false));
		System.out.println("size=" + ts.vector.size() + ", top300=" + ts.vector.TopEntries(300).size());
		//System.out.println(ts.vector.toString());
	}
	
}
