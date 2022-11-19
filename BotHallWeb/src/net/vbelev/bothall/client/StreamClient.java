package net.vbelev.bothall.client;

import java.io.*;
import java.util.*;
import java.net.*;

import net.vbelev.utils.*;
/**
 * This is the client ("browser") side of the connection. 
 * It maintains a BHClient.Client collection by reading from a stream (a pipe)
 * and sends commands out through the other pipe.
 * 
 * It does not have any business logic and invokes its onUpdate method once every update cycle.
 * 
 * Note that the client depends on the provided socket (in and out streams). 
 * Once the connection is terminated, the client cannot be reconnected again - 
 * you'll need to create a new StreamClient instance with the new connection.
 */
public class StreamClient<BHC extends PacmanClient> //BHClient.Client>
{
	/** We keep onUpdate() as a delegate
	 * to decouple the client creation, which is a networking operation, 
	 * from the consumer logic.
	 * We do not need a multi-consumer event here, at least not yet. 
	 */
	public interface OnUpdate
	{
		void onUpdate();
	}
	
	/** 
	 * For reasons similar to OnUpdate, this callback is a delegate.
	 * Commands from the server to the client are technical, so they can be processed out of cycle.
	 */
	public interface OnCommand
	{
		void onCommand(BHClient.Command cmd);
	}
	
	/* updateEvent is triggered after a successful update from the server. It runs synchronously 
	public final EventBox.Event<EventBox.EventArgs> updateEvent = new EventBox.Event<EventBox.EventArgs>(false);	
	 */
	
	public final BHC collection; // = new BHClient.Client();
	/** This is a queue of elements that were received by readUp() 
	 * but were not used to update the collection. This includes Message and Error elements.
	 */
	public final Queue<BHClient.IElement> elementQueue = new java.util.concurrent.ConcurrentLinkedQueue<BHClient.IElement>();
	
	//private final BufferedReader reader;
	//private final BufferedWriter writer;
	private final DryCereal dryWriter;
	private final DryCereal.Reader dryReader; //= new DryCereal.Reader();
	private Thread runningThread = null;
	private OnUpdate onUpdate = null;
	private OnCommand onCommand = null;
	
	private final Socket s;
	public StreamClient(InputStream in, OutputStream out, BHC collection)
	{
		//reader = new BufferedReader(new InputStreamReader(in));
		//writer = new BufferedWriter(new OutputStreamWriter(out));
		dryReader = new DryCereal.Reader(in);
		dryWriter = new DryCereal(out);
		s = null;
		this.collection = collection;
	}
	
	public StreamClient(Socket socket, BHC collection) throws IOException
	{
		s = socket;
		dryReader = new DryCereal.Reader(s.getInputStream());
		dryWriter = new DryCereal(s.getOutputStream());
		this.collection = collection;
	}
	
	public void setOnUpdate(OnUpdate o)
	{
		onUpdate = o;
	}
	/** synchronously reads from the input stream until the Status record is encountered
	 * or until the stream is ended.
	 * Returns false if there was nothing to read; 
	 * returns true if an update was received; 
	 * throws IOException if there was a problem  
	 */
	public boolean readUp() throws IOException
	{
		
		if (!dryReader.hasNext())
		{
			//System.out.println("StreamClient.readUp: no next, exit");
			return false;
		}
		do
		{
			boolean isUpdateBin = false;
			if (s != null && s.isClosed()) break;
			BHClient.IElement gotIt = null;
			DryCereal.Flake typeCodeFlake = dryReader.next();
			if (typeCodeFlake.type != DryCereal.CerealType.BYTE)
			{
				System.out.println("StreamClient.readUp throws unexpected cereal element: " + typeCodeFlake);
				throw new IOException("Unexpected cereal element: " + typeCodeFlake);
			}
			byte typeCode = (Byte)typeCodeFlake.getByte();
			if (typeCode == BHClient.ElementCode.UPDATEBIN)
			{
				isUpdateBin = true;
			}
			else if (typeCode == BHClient.ElementCode.CELL)
			{
				try {
				BHClient.Cell c = new BHClient.Cell();
				c.fromCereal(dryReader);
				//System.out.println("SC Cell: " + c.toString());
				collection.putCell(c);
				gotIt = c;
				}
				catch (Throwable r)
				{
					System.out.println("cast1? " + r.getLocalizedMessage());
					throw r;
				}

			}
			else if (typeCode == BHClient.ElementCode.ITEM)
			{
				try {
				BHClient.Item i = new BHClient.Item();
				i.fromCereal(dryReader);
				collection.items.put(i.id, i);
				gotIt = i;
				}
				catch (Throwable r)
				{
					System.out.println("cast2? " + r.getLocalizedMessage());
					throw r;
				}

			}
			else if (typeCode == BHClient.ElementCode.MOBILE)
			{
				try {
				BHClient.Mobile m = new BHClient.Mobile();
				m.fromCereal(dryReader);
				collection.mobiles.put(m.id, m);
				gotIt = m;
				}
				catch (Throwable r)
				{
					System.out.println("cast3? " + r.getLocalizedMessage());
					throw r;
				}

			}
			else if (typeCode == BHClient.ElementCode.STATUS)
			{
				try {
				BHClient.Status s = collection.getStatus();
				s.fromCereal(dryReader);

				//return dryReader.hasNext();
				isUpdateBin = false;
				//System.out.println("StreamClient got status");
				return true;
				}
				catch (Throwable r)
				{
					System.out.println("cast4? " + r.getLocalizedMessage());
					throw r;
				}

			}
			else if (typeCode == BHClient.ElementCode.BUFF)
			{
				try {
				BHClient.Buff b = new BHClient.Buff();
				b.fromCereal(dryReader);
				gotIt = b;
				//collection.items.put(i.id, i);
				}
				catch (Throwable r)
				{
					System.out.println("cast5? " + r.getLocalizedMessage());
					throw r;
				}

			}
			else if (typeCode == BHClient.ElementCode.MESSAGE)
			{
				try {
				BHClient.Message msg = new BHClient.Message();
				msg.fromCereal(dryReader);
				gotIt = msg;
				elementQueue.add(msg);
				//System.out.println("SC: Message" + msg.toString());
				//collection.items.put(i.id, i);
				}
				catch (Throwable r)
				{
					System.out.println("cast6? " + r.getLocalizedMessage());
					throw r;
				}

			}
			else if (typeCode == BHClient.ElementCode.COMMAND)
			{
				BHClient.Command cmd = new BHClient.Command();
				cmd.fromCereal(dryReader);
				if (this.onCommand != null)
				{
					this.onCommand.onCommand(cmd);
				}
			}
			else
			{
				try {
				//return false;
				//throw new IOException("Unexpected element type: " + typeCode);
				BHClient.IElement elem = BHClient.fromCereal(typeCode, dryReader);
				if (elem != null)
				{
				elementQueue.add(elem);
				gotIt = elem;
				System.out.println("Got other elem: " + gotIt.toString());
				}
				}
				catch (Throwable r)
				{
					System.out.println("cast6? " + r.getLocalizedMessage());
					throw r;
				}

			}
			/*
			if (gotIt != null)
			{
				System.out.println("StreamClient got elem "+ gotIt.toString());
			}
			else
			{
				System.out.println("StreamClient got null elem, typeCode="+ typeCode);
			}
			*/
			if (!isUpdateBin && !dryReader.hasNext())
			{
				return true; // one extra element at a time
			}
		} 
		while (true);
		return false;
	}

	/** Send a command to the server (the listener on the server side will know who is sending)*/
	public void writeCommand(BHClient.Command cmd) throws IOException
	{
		synchronized (dryWriter)
		{
		dryWriter.addByte(BHClient.ElementCode.COMMAND);
		cmd.toCereal(dryWriter);
		dryWriter.flush();
		}
	}

	public void writeCommand(String cmd, int[] intArgs, String[] stringArgs) throws IOException
	{
		if (intArgs == null) intArgs = new int[0];
		if (stringArgs == null) stringArgs = new String[0];
		BHClient.Command sendCmd = new BHClient.Command(intArgs.length, stringArgs.length);
		sendCmd.intArgs = intArgs;
		sendCmd.stringArgs = stringArgs;
		sendCmd.command = cmd;
		
		synchronized (dryWriter)
		{
		dryWriter.addByte(BHClient.ElementCode.COMMAND);
		sendCmd.toCereal(dryWriter);
		dryWriter.flush();
		}
	}

	public void writeMessage(BHClient.Message msg) throws IOException
	{
		synchronized (dryWriter)
		{
		msg.toCereal(dryWriter);
		dryWriter.flush();
		}
	}
	
	public void start()
	{
		System.out.println("StreamClient starting: " + this.toString());
		if (runningThread != null && runningThread.isAlive())
		{
			return; // we are already running 
		}
		runningThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				//runningThread = Thread.currentThread();
				while (runningThread != null && !Thread.interrupted())
				{
					if (s != null && s.isClosed()) break;
					
					try
					{
						boolean hasUpdate = readUp();
						if (hasUpdate)
						{
							//updateEvent.trigger(new EventBox.EventArgs());
							if (onUpdate != null) onUpdate.onUpdate();
						}
					}
					catch (java.net.SocketTimeoutException x)
					{
						// good, keep going
					}
					catch (InterruptedIOException x)
					{
						System.out.println("StreamClient.runnable: interrupted, stopping: " + x.getMessage());
						stop();
					}
					catch (Exception x)
					{
						System.out.println("StreamClient.runnable: errot, stopping: " + x.getClass().getName() + ", " + x.getMessage());
						//String dump = dryReader.skipToEnd(); // skipToEnd locks: the server sends updates faster than we read them
						String dump = dryReader.skipLine();
						System.out.println("dump: " + dump);
						// not so good
						BHClient.Error err = new BHClient.Error();
						err.message = x.getMessage();
						elementQueue.add(err);
						//updateEvent.trigger(new EventBox.EventArgs());
						if (onUpdate != null) onUpdate.onUpdate();
						//stop();
					}
				}	
				System.out.println("StreamClient stopped: " + this.toString());
				
			}
		});
		Thread t = runningThread;
		if (t != null)
		{	
			t.start();
		}
	}
	
	public void stop()
	{
		Thread t = runningThread;
		
		if (t != null)
		{
			runningThread = null;
			t.interrupt();
		}
	}
}
