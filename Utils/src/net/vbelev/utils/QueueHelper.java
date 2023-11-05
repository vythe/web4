package net.vbelev.utils;
import java.util.*;

public class QueueHelper
{
	/** Only static helper methods and classes here */
	private QueueHelper() {}

	/**
	 * A persistent iterator that skips over null elements and can continue from empty
	 * when the queue gets more elements.
	 * @author Vythe
	 *
	 * @param <T>
	 */
	public static class PersistentIterator<T> implements Iterator<T>
	{
		private Queue<T> queue;
		private T head = null;
		public PersistentIterator(Queue<T> queue)
		{
			this.queue = queue;
		}
		private void getHead()
		{
			// elements cannot be null in normal queues, but things happen
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

	
	public static class QueueHolder<T>
	{
		public boolean isOpen = true;
		private final Iterator<T> iter;
		private final Queue<T> qq;
		
		public QueueHolder(Queue<T> deq)
		{
			this.qq = deq;
			this.iter = new PersistentIterator<T>(deq);
		}
		
		public Queue<T> getQueue() { return this.qq; }
		public Iterator<T> iterator() { return iter; }
	}	
}
