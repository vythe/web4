package net.vbelev.filebox;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * WordMetric is a collection of words that form a reference system for word vectors.
 */
public class WordMetric
{
	public static final String EMPTY_STRING = "".intern();
	public static final int EMPTY_INDEX = 0;
	public static final float MIN_VECTOR_VALUE = 0.00000001f;
	
	private final java.util.Map<Integer, String> _words = new TreeMap<Integer, String>(); 
	private final java.util.Map<String, Integer> _wordIndex = new TreeMap<String, Integer>(); 
	private final java.util.Map<Integer, Boolean> _wordEnabled = new TreeMap<Integer, Boolean>(); 
	private int _wordWatermark = EMPTY_INDEX;
	
	public WordMetric()
	{
		_words.put(EMPTY_INDEX, EMPTY_STRING);
		_wordIndex.put(EMPTY_STRING, EMPTY_INDEX);
	}
	
	/**
	 * Returns null if the index is not valid
	 */
	public synchronized String getWord(int index)
	{
		return _words.get(index);
	}
	
	/**
	 * Returns the word's index. If the word is not found, it is added to the metric and the new index is returned.
	 * Note that 0 is always the empty string EMPTY_STRING, and it is never enabled
	 */
	public synchronized int getWordIndex(String word)
	{
		if (word == null || word.isEmpty())
			return EMPTY_INDEX;
		
		if (_wordIndex.get(word) instanceof Integer res)
		{
			return res;
		}
		int idx = ++_wordWatermark;
		_words.put(idx, word);
		_wordIndex.put(word,  idx);
		_wordEnabled.put(idx, true);
		return idx;
	}
	
	public boolean isEnabled(int index)
	{
		return _wordEnabled.containsKey(index);
	}
	
	public void setEnabled(int index, boolean isEnabled)
	{
		if (index <= 0)
			return;
		else if (isEnabled)
			_wordEnabled.put(index,  true);
		else
			_wordEnabled.remove(index);		
	}
	
	public static class IndexedWord
	{
		public final int index;
		public final String word;
		
		public IndexedWord(int index, String word)
		{
			this.index = index;
			this.word = word;
		}
		
		@Override
		public boolean equals(Object o)
		{
			if (o instanceof IndexedWord other)
			{
				return index == other.index && Objects.equals(this.word, other.word);
			}
			
			return false;
		}
		
		@Override
		public int hashCode()
		{
			if (word == null)
				return index;
			else
				return index ^ word.hashCode();
		}
		
		@Override
		public String toString()
		{
			return index + ":" + ((word == null)? "" : word);
		}
	}
	
	/**
	 * Provides a sequence of either all words in the metric or all enabled words in the metric -
	 * either in the index order or in the alphabetical order
	 */
	private class WordIteratorAllByIndex implements Iterator<IndexedWord>
	{
		private final Iterator<Map.Entry<Integer, String>> _iterator;
		
		public WordIteratorAllByIndex()
		{
			_iterator = _words.entrySet().iterator();
		}
		
		public boolean hasNext() { return _iterator.hasNext(); }
		
		public IndexedWord next() 
		{
			Map.Entry<Integer, String> val = _iterator.next();
			return new IndexedWord(val.getKey(), val.getValue());
		}
	}
	
	private class WordIteratorEnabledByIndex implements Iterator<IndexedWord>
	{
		private final Iterator<Map.Entry<Integer, String>> _iterator;
		private final Iterator<Map.Entry<Integer, Boolean>> _iteratorE;
		
		private Map.Entry<Integer, String> _nextEntry;
		private Map.Entry<Integer, Boolean> _nextEnabled;
		
		private void findNextEntry()
		{
			while(_iterator.hasNext())
			{
				_nextEntry = _iterator.next();
				
				while (_nextEnabled != null && _nextEntry.getKey().intValue() > _nextEnabled.getKey().intValue())
					_nextEnabled = _iteratorE.hasNext()? _iteratorE.next() : null;					
				
				if (_nextEnabled == null)
				{
					_nextEntry = null; 
					return; // no more enabled entries, we are done
				}
				if (_nextEnabled.getKey().intValue() == _nextEntry.getKey().intValue())
				{
					return; // found an enabled entry
				}				
			}
			_nextEntry = null;
		}

		public WordIteratorEnabledByIndex()
		{
			_iterator = _words.entrySet().iterator();
			_iteratorE = _wordEnabled.entrySet().iterator();
			_nextEnabled = _iteratorE.hasNext()? _iteratorE.next() : null;
			findNextEntry();
			
		}
		
		public boolean hasNext() { return _nextEntry != null; }
		
		public IndexedWord next() 
		{
			if (_nextEntry == null)
			{
				throw new NoSuchElementException();
			}
			IndexedWord res = new IndexedWord(_nextEntry.getKey(), _nextEntry.getValue());
			findNextEntry();
			
			return res;
		}
	}

	
	public Iterable<IndexedWord> values()
	{
		return values(false, false);
	}
	
	public Iterable<IndexedWord> values(boolean enabledOnly, boolean sortByWord)
	{
		if (!enabledOnly && !sortByWord)
		{
			return (Iterable<IndexedWord>)() -> new WordIteratorAllByIndex();
		}
		else if (enabledOnly && !sortByWord)
		{
			return (Iterable<IndexedWord>)() -> new WordIteratorEnabledByIndex();
		}
		else
		{
			throw new UnsupportedOperationException();
		}
	}
	
	/**
	 * Returns the whole collection of the vector values - this may be good for testing
	 */
	public Collection<IndexedWord> allValues()
	{
		return _words.entrySet()
				.stream()
				.map(q -> new IndexedWord(q.getKey(), q.getValue()))
				.collect(Collectors.toList())
		;
	}
	
	public int size()
	{
		return size(false);
	}
	
	public int size(boolean enabledOnly)
	{
		if (enabledOnly)
			return _wordEnabled.size();
		return _words.size();
	}
	
	public String print(int maxElements, boolean enabledOnly, boolean sortByWord)
	{
		return String.join(", ", 
			iterableToStream(values(enabledOnly, sortByWord))
			.limit((long)maxElements)
			.map(q -> q.index + ":" + q.word)
			.collect(Collectors.toList())
		);
	}
	
	/**
	 * Splits a string into words and returns a sequence of words.
	 */
	private static class WordFromStringIterator implements Iterator<String>
	{
		private char[] _src;
		private Predicate<Character> _comparer;
		private int pos = 0;
		
		public WordFromStringIterator(String src, Predicate<Character> isWordCharacter)
		{
			if (src == null)
				_src = new char[0];
			else
				_src = src.toCharArray();
			_comparer = isWordCharacter;
			
			while (pos < _src.length && !_comparer.test(_src[pos]))
				pos++;			
		}
		
		public  boolean hasNext()
		{
			return pos < _src.length;
		}
		
		/**
	     * Returns the next element in the iteration.
	     *
	     * @return the next element in the iteration
	     * @throws NoSuchElementException if the iteration has no more elements
	     */
		public String next()
		{
			if (pos >= _src.length)
				throw new NoSuchElementException();
			
			int pos1 = pos + 1;
			
			while (pos1 < _src.length && _comparer.test(_src[pos1]))
				pos1++;
			String res = new String(_src, pos, pos1 - pos);
			
			pos = pos1;
			while (pos < _src.length && !_comparer.test(_src[pos]))
				pos++;
			
			return res;
		}		
	}
	
	public static class WordFromStringIterable implements Iterable<String>
	{
		private String _src;
		private Predicate<Character> _comparer;
		
		public WordFromStringIterable(String src)
		{
			_src = src == null? null : src.trim();
			_comparer = (c) -> {return (Character.isLetterOrDigit(c) || c == '-');};
		}

		public WordFromStringIterable(String src, Predicate<Character> wordTest)
		{
			_src = src == null? null : src.trim();
			_comparer = (wordTest == null)? (c) -> true : wordTest;
		}

		public Iterator<String> iterator()
		{
			return new WordFromStringIterator(_src, _comparer);
		}
	}
	
	public static class VectorValue
	{
		public final int index;
		public final float value;
		
		public VectorValue(int index, float value)
		{
			this.index = index;
			this.value = value;
		}
		
		
		@Override
		public boolean equals(Object o)
		{
			if (o instanceof VectorValue other)
			{
				return index == other.index && value == other.value;
			}
			
			return false;
		}
		
		@Override
		public int hashCode()
		{
			return index ^ Float.hashCode(value);
		}

		@Override
		public String toString()
		{
			return index + ":" + value;
		}
	}
	
	/**
	 * vector values can be made integer if performance becomes an issue.
	 * But there is no need.
	 */
	public class Vector
	{		
		/**
		 * The property is made public for testing
		 */
		public final java.util.Map<Integer, Float> _vector = new TreeMap<Integer, Float>(); 
	
		public WordMetric metric()
		{
			return WordMetric.this;
		}
		
		public int size()
		{
			return _vector.size();
		}
		
		public String keys()
		{
			return String.join(",", 
			_vector.keySet()
			.stream()
			.map(q -> q.intValue() + "")
			.collect(Collectors.toList())
			);
		}

		@Override
		public String toString()
		{
			return String.join(",", 
			_vector.entrySet()
			.stream()
			.map(q -> q.getKey() + ":" + q.getValue())
			.collect(Collectors.toList())
			);
		}
		
		@Override
		public Vector clone()
		{
			Vector res = new Vector();
			res._vector.putAll(_vector);
			return res;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (obj instanceof Vector other)
			{
				return this.metric().equals(other.metric())
					&& this.values(false).equals(other.values(false))
				;
			}
			return false;
		}
		
		public float get(int wordIndex)
		{
			if (_wordEnabled.containsKey(wordIndex) && _vector.get(wordIndex) instanceof Float res)
			{
				return res;
			}
			return 0;
		}
		public float get(String word)
		{
			//AbstractMap.SimpleImmutableEntry<K, V> a;
			return get(getWordIndex(word));
		}
		
		public int put(int wordIndex, float val)
		{
			if (val < MIN_VECTOR_VALUE || !_wordEnabled.containsKey(wordIndex))
			{
				_vector.remove(wordIndex);
			}
			else
			{
				_vector.put(wordIndex, val);
			}
			return wordIndex;
		}
		
		public int put(String word, float val)
		{
			int wordIndex = getWordIndex(word);
			return put(wordIndex, val);
		}
		
		public float norm()
		{
			float res = 0;
			for (Map.Entry<Integer, Float> entry : _vector.entrySet())
			{
				res += entry.getValue() * entry.getValue();
			}			
			return res;
		}
		
		/**
		 * Returns a sequence of all elements in the word metric, with 0 values where the word is not present in the vector
		 */
		private class VectorIteratorFull implements Iterator<VectorValue>
		{
			private Iterator<IndexedWord> _wordIterator;
			private Iterator<Map.Entry<Integer, Float>> _valuesIterator;
			private Map.Entry<Integer, Float> _lastValue = null;
			public VectorIteratorFull()
			{
				_wordIterator = WordMetric.this.values(true,  false).iterator();
				_valuesIterator = _vector.entrySet().iterator();
				if (_valuesIterator.hasNext())
					_lastValue = _valuesIterator.next();
			}
			
			public boolean hasNext()
			{
				return _wordIterator.hasNext();
			}
			
			public VectorValue next()
			{
				int wordIndex = _wordIterator.next().index; // this will throw NoSuchElementException if needed
				while (_lastValue != null && _lastValue.getKey().intValue() < wordIndex)
				{
					_lastValue = _valuesIterator.hasNext()? _valuesIterator.next() : null;
				}
				
				if (_lastValue != null && _lastValue.getKey().intValue() == wordIndex)
					return new VectorValue(_lastValue.getKey(), _lastValue.getValue());
				else
					return new VectorValue(wordIndex, 0f);
			}
		}
		
		private class VectorIteratorOwn implements Iterator<VectorValue>
		{
			private Iterator<Map.Entry<Integer, Float>> _valuesIterator;
			private Iterator<Map.Entry<Integer, Boolean>> _enabledIterator;
			private Map.Entry<Integer, Float> _nextEntry = null;
			private Map.Entry<Integer, Boolean> _nextEnabled = null;
			
			public VectorIteratorOwn()
			{
				_valuesIterator = _vector.entrySet().iterator();
				_enabledIterator = _wordEnabled.entrySet().iterator();

				_nextEnabled = _enabledIterator.hasNext()? _enabledIterator.next() : null;	
				findNextEntry();
			}
			
			public boolean hasNext()
			{
				return _nextEntry != null; 
			}
			
			public VectorValue next()
			{
				if (_nextEntry == null)
					throw new NoSuchElementException();
			
				VectorValue res = new VectorValue(_nextEntry.getKey(), _nextEntry.getValue());
				findNextEntry();
				return res;
			}
			
			private void findNextEntry()
			{
				while(_valuesIterator.hasNext())
				{
					_nextEntry = _valuesIterator.next();
					
					while (_nextEnabled != null && _nextEntry.getKey().intValue() > _nextEnabled.getKey().intValue())
						_nextEnabled = _enabledIterator.hasNext()? _enabledIterator.next() : null;					
					
					if (_nextEnabled == null)
					{
						_nextEntry = null; 
						return; // no more enabled entries, we are done
					}
					if (_nextEnabled.getKey().intValue() == _nextEntry.getKey().intValue())
					{
						return; // found an enabled entry
					}				
				}
				_nextEntry = null;
			}
		}
		
		public Iterable<VectorValue> values(boolean fullMetric)
		{
			if (fullMetric)
			{
				return new Iterable<VectorValue>()
				{
					public Iterator<VectorValue> iterator()
					{
						return new VectorIteratorFull();				
					}				
				};			
			}
			else
			{
				return new Iterable<VectorValue>()
				{
					public Iterator<VectorValue> iterator()
					{
						return new VectorIteratorOwn();				
					}				
				};
			}
		}		
	
		/**
		 * Returns the whole collection of the vector values - this may be good for testing
		 */
		public Collection<VectorValue> allValues()
		{
			return _vector.entrySet()
					.stream()
					.map(q -> new VectorValue(q.getKey(), q.getValue()))
					.collect(Collectors.toList())
			;
		}
		
		public String print(int maxElements)
		{
			return String.join(", ", 
				iterableToStream(values(false))
				.limit((long)maxElements)
				.map(q -> q.index + ":" + q.value)
				.collect(Collectors.toList())
			);
		}
		
		
		
		public Collection<VectorValue> TopEntries(int count)
		{
			if (count <= 0)
				return new ArrayList<VectorValue>();
			
			List<VectorValue> entries = new ArrayList<VectorValue>();
			for(VectorValue v : values(false))
				entries.add(v);
			//entries.removeIf(q -> q.getValue() > 0.8);
			Collections.sort(entries,  (q1, q2) -> Float.compare(q2.value, q1.value));
			
			if (count > entries.size())
				return entries;
			
			return entries.subList(0,  count);
		}
		
		public String TopEntriesText(int count)
		{
			Collection<VectorValue> entries = TopEntries(count);
			if (entries.size() == 0) return "";
			List<String> vals = new ArrayList<String>();
			for (VectorValue entry : entries)
			{
				vals.add(getWord(entry.index) + ":" + entry.value);
			}
			return String.join(", ", vals);
		}
	
		/**
		 * simple diff, using b.get()
		 */
		public Vector diff1(Vector b)
		{
			if (!metric().equals(b.metric()))
			{
				throw new IllegalArgumentException("vectors from different metrics cannot be compared");
			}
			Vector res = new Vector();
			for (VectorValue entry : values(true)) {
				int key = entry.index;
				float d = entry.value - b.get(key);
				if (d != 0)
					res.put(key, d > 0? d : -d);
			}
			return res;
		}
		
		/**
		 * faster(?) diff, using two iterators, ignoring enabled flags
		 * @param b
		 * @return
		 */
		public Vector diff2(Vector b)
		{
			if (!metric().equals(b.metric()))
			{
				throw new IllegalArgumentException("vectors from different metrics cannot be compared");
			}
			Vector res = new Vector();
			
			Iterator<VectorValue> iterator_a = values(false).iterator();
			Iterator<VectorValue> iterator_b = b.values(false).iterator();
			
			VectorValue entry_a = iterator_a.hasNext()? iterator_a.next() : null;
			VectorValue entry_b = iterator_b.hasNext()? iterator_b.next() : null;
			while (entry_a != null || entry_b != null)
			{
				int key_a = entry_a == null? Integer.MAX_VALUE : entry_a.index;
				int key_b = entry_b == null? Integer.MAX_VALUE : entry_b.index;
				
				if (key_a < key_b)
				{
					res.put(key_a, entry_a.value);
					entry_a = iterator_a.hasNext()? iterator_a.next() : null;
				}
				else if (key_b < key_a)
				{
					res.put(key_b, entry_b.value);
					entry_b = iterator_b.hasNext()? iterator_b.next() : null;
				}
				else // key_a == key_b
				{
					float d = entry_a.value - entry_b.value;
					res.put(key_a, d > 0? d : -d);
					entry_a = iterator_a.hasNext()? iterator_a.next() : null;
					entry_b = iterator_b.hasNext()? iterator_b.next() : null;
				}
			}
			return res;
		}
		
		/**
		 * proper diff, using two iterators and checking enabled flags
		 */
		public Vector diff(Vector b)
		{
			if (!metric().equals(b.metric()))
			{
				throw new IllegalArgumentException("vectors from different metrics cannot be compared");
			}
			Vector res = new Vector();
			
			Iterator<VectorValue> iterator_a = values(false).iterator();
			Iterator<VectorValue> iterator_b = b.values(false).iterator();
			Iterator<Map.Entry<Integer, Boolean>> iterator_enabled = _wordEnabled.entrySet().iterator();
			
			VectorValue entry_a = iterator_a.hasNext()? iterator_a.next() : null;
			VectorValue entry_b = iterator_b.hasNext()? iterator_b.next() : null;
			Map.Entry<Integer, Boolean> entry_enabled = iterator_enabled.hasNext()? iterator_enabled.next() : null;
			while (entry_enabled != null && (entry_a != null || entry_b != null))
			{
				int key_a = entry_a == null? Integer.MAX_VALUE : entry_a.index;
				int key_b = entry_b == null? Integer.MAX_VALUE : entry_b.index;
				int key_e = entry_enabled.getKey(); 
				while (key_e < key_a && key_e < key_b && iterator_enabled.hasNext())
				{
					entry_enabled = iterator_enabled.next();
					key_e = entry_enabled.getKey();
				}
				if (key_e < key_a && key_e < key_b)
					break; // no more enabled words in this metric
				
				if (key_a < key_b)
				{
					if (key_e == key_a)
					{
						res.put(key_a, entry_a.value);
					}
					entry_a = iterator_a.hasNext()? iterator_a.next() : null;
				}
				else if (key_b < key_a)
				{
					if (key_e == key_b)
					{
						res.put(key_b, entry_b.value);
					}
					entry_b = iterator_b.hasNext()? iterator_b.next() : null;
				}
				else // key_a == key_b
				{
					if (key_e == key_a)
					{
						float d = entry_a.value - entry_b.value;
						res.put(key_a, d > 0? d : -d);
					}
					entry_a = iterator_a.hasNext()? iterator_a.next() : null;
					entry_b = iterator_b.hasNext()? iterator_b.next() : null;
				}
			}
			return res;
		}
		
		/** 
		 * simple add, using b.get()
		 */
		public Vector add1(Vector b)
		{
			if (!metric().equals(b.metric()))
			{
				throw new IllegalArgumentException("vectors from different metrics cannot be compared");
			}
			Vector res = new Vector();
			for (VectorValue entry : values(true)) {
				int key = entry.index;
				float d = entry.value + b.get(key);
				if (d != 0)
					res.put(key, d > 0? d : -d);
			}
			return res;
		}
		
		public Vector add2(Vector b)
		{
			if (!metric().equals(b.metric()))
			{
				throw new IllegalArgumentException("vectors from different metrics cannot be compared");
			}
			Vector res = new Vector();
			
			Iterator<VectorValue> iterator_a = values(false).iterator();
			Iterator<VectorValue> iterator_b = b.values(false).iterator();
			VectorValue entry_a = iterator_a.hasNext()? iterator_a.next() : null;
			VectorValue entry_b = iterator_b.hasNext()? iterator_b.next() : null;
			
			while (entry_a != null || entry_b != null)
			{
				int key_a = entry_a == null? Integer.MAX_VALUE : entry_a.index;
				int key_b = entry_b == null? Integer.MAX_VALUE : entry_b.index;
				if (key_a < key_b)
				{
					res.put(key_a, entry_a.value);
					entry_a = iterator_a.hasNext()? iterator_a.next() : null;
				}
				else if (key_b < key_a)
				{
					res.put(key_b, entry_b.value);
					entry_b = iterator_b.hasNext()? iterator_b.next() : null;
				}
				else // key_a == key_b
				{
					float d = entry_a.value + entry_b.value;
					res.put(key_a, d > 0? d : -d);
					entry_a = iterator_a.hasNext()? iterator_a.next() : null;
					entry_b = iterator_b.hasNext()? iterator_b.next() : null;
				}
			}
			return res;
		}
		
		/**
		 * proper add, using two iterators and checking enabled flags
		 */
		public Vector add(Vector b)
		{
			if (!metric().equals(b.metric()))
			{
				throw new IllegalArgumentException("vectors from different metrics cannot be compared");
			}
			Vector res = new Vector();
			
			Iterator<VectorValue> iterator_a = values(false).iterator();
			Iterator<VectorValue> iterator_b = b.values(false).iterator();
			Iterator<Map.Entry<Integer, Boolean>> iterator_enabled = _wordEnabled.entrySet().iterator();
			
			VectorValue entry_a = iterator_a.hasNext()? iterator_a.next() : null;
			VectorValue entry_b = iterator_b.hasNext()? iterator_b.next() : null;
			Map.Entry<Integer, Boolean> entry_enabled = iterator_enabled.hasNext()? iterator_enabled.next() : null;
			while (entry_enabled != null && (entry_a != null || entry_b != null))
			{
				int key_a = entry_a == null? Integer.MAX_VALUE : entry_a.index;
				int key_b = entry_b == null? Integer.MAX_VALUE : entry_b.index;
				int key_e = entry_enabled.getKey(); 
				while (key_e < key_a && key_e < key_b && iterator_enabled.hasNext())
				{
					entry_enabled = iterator_enabled.next();
					key_e = entry_enabled.getKey();
				}
				if (key_e < key_a && key_e < key_b)
					break; // no more enabled words in this metric
				
				if (key_a < key_b)
				{
					if (key_e == key_a)
					{
						res.put(key_a, entry_a.value);
					}
					entry_a = iterator_a.hasNext()? iterator_a.next() : null;
				}
				else if (key_b < key_a)
				{
					if (key_e == key_b)
					{
						res.put(key_b, entry_b.value);
					}
					entry_b = iterator_b.hasNext()? iterator_b.next() : null;
				}
				else // key_a == key_b
				{
					if (key_e == key_a)
					{
						float d = entry_a.value + entry_b.value;
						res.put(key_a, d > 0? d : -d);
					}
					entry_a = iterator_a.hasNext()? iterator_a.next() : null;
					entry_b = iterator_b.hasNext()? iterator_b.next() : null;
				}
			}
			return res;
		}
		
	}
	
	private float[] countVals = new float[]
	{
		0,
		0.5f,
		0.7f,
		0.85f,
		0.95f,
		1f
	};
	
	private float countToVal(int count)
	{
		if (count < 0) return countVals[0];
		else if (count >= countVals.length) return countVals[countVals.length - 1];
		return countVals[count];
	}
	
	private int valToCount(float val)
	{
		for (int i = 0; i < countVals.length; i++)
		{
			if (val <= countVals[i] + 0.0001f) return i;
		}
		return countVals.length - 1;
	}
	
	public Vector parse(String text)
	{
		Vector res = new Vector();
		WordMetric.WordFromStringIterable iter = new WordMetric.WordFromStringIterable(text);
		for (String s : iter)
		{
			s = wordToStandard(s);
			int wordIndex = getWordIndex(s);	
			float val = res.get(wordIndex);
			int cnt = valToCount(val);
			res.put(wordIndex, countToVal(cnt + 1));
		}
		return res;
	}
	
	public static String wordToStandard(String word)
	{
		if (word == null) return EMPTY_STRING;
		return word.toUpperCase().trim();
	}
	
	public String test()
	{
		Map<Integer, Integer> tMap = new TreeMap<Integer, Integer>();
		int cnt = 1000;
		String res = "";
		for (int i = 0 ; i < cnt; i++)
		{
			int ind = (int)(Math.random() * 10000.);
			int val = (int)(Math.random() * 10000.);			
			tMap.put(ind,  val);
		}
		
		int prevKey = -1;
		/*
		for (Integer key : tMap.keySet())
		{
			if (prevKey >= key)
			{
				res += prevKey + " -> " + key;
				res += " WRONG!";
				res += "<br/>";
			}
			prevKey = key;
		}
		*/
		for (Map.Entry<Integer, Integer> key : tMap.entrySet())
		{
			res += prevKey + " -> " + key.getKey();
			if (prevKey >= key.getKey().intValue())
			{
				//res += prevKey + " -> " + key.getKey();
				res += " WRONG!";
				//res += "<br/>";
			}
			res += "<br/>";
			prevKey = key.getKey();
		}
		return res;
	}

	public static <T> Stream<T> iterableToStream(Iterable<T> from)
	{
		return java.util.stream.StreamSupport.stream(from.spliterator(), false);		
	    //Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(from.iterator(), 0);
	    //return StreamSupport.stream(spliterator, false);
	}
	
	public static <T> Iterable<T> streamToIterable(Stream<T> st)
	{
		return (Iterable<T>)() -> st.iterator();
	}
	
	public static <T> Collection<T> iterableToCollection(Iterable<T> from)
	{
		ArrayList<T> res = new ArrayList<T>();
		from.forEach(q -> res.add(q));
		
		return res;
	}
}
