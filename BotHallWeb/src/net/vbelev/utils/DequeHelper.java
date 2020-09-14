package net.vbelev.utils;
import java.util.*;

public class DequeHelper
{
	/** Only static helper methods and classes here */
	private DequeHelper() {}

	/**
	 * A persistent iterator that skips over null elements and can continue from empty
	 * when the queue gets more elements.
	 * @author Vythe
	 *
	 * @param <T>
	 */
	public static class PersistentIterator<T> implements Iterator<T>
	{
		private Deque<T> queue;
		private T head = null;
		public PersistentIterator(Deque<T> queue)
		{
			this.queue = queue;
		}
		private void getHead()
		{
			while (head == null && !queue.isEmpty())
			{
				head = queue.poll();
			}
		}
		@Override
		public boolean hasNext()
		{
			getHead();
			return (head != null);			
		}

		@Override
		public T next()
		{
			getHead();
			T res = head;
			head = null;
			return res;
		}
	}

	
	public static class DequeHolder<T>
	{
		public boolean isOpen = true;
		private final Iterator<T> iter;
		private final Deque<T> deq;
		
		public DequeHolder(Deque<T> deq)
		{
			this.deq = deq;
			this.iter = new PersistentIterator<T>(deq);
		}
		
		public Deque<T> deque() { return this.deq; }
		public Iterator<T> iterator() { return iter; }
	}	
}
