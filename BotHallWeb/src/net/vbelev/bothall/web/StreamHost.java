package net.vbelev.bothall.web;

import java.io.*;
import java.net.*;

import net.vbelev.bothall.client.*;
import net.vbelev.bothall.core.BHCollection.MessageTargetEnum;
import net.vbelev.utils.*;

/**
 * The server-side support for a client connection (StreamClient).
 * It sends out UpdateBin and receives commands. 
 * Command processing should be done in a subclass.
 * 
 * Note that StreamServer is created before there is a BHClientAgent (it is created to an opened socket)
 * so it either needs to be a subclass ot StreamServer that processes commands for the right session type,
 * or BHSession needs to expose a virtual method to process commands.
 * If we do a "create session" command, StreamServer will need to decide which bhsession subtype to create 
 * 
 * To start a server-side client we'll need a client key (from a BHClientAgent);
 * a (static) instance of BotHallServer to supply a ServerSocket;
 * [NB: sockets are needed to give us a pair of interruptible streams, and to use the same logic as external socket clients]
 * an instance of a subclass of StreamServer that will subscribe to engine.publishEvent, based on its client key;
 * an instance of a subclass of StreamClient that implements the monster intelligence.
 *  
 * It subscribes to the 
 * @author Vythe
 *
 */
public class StreamHost
{
	/** StreamListener should be a true singleton, but it's too much effort,
	 * so it is a static variable
	 */
	private static StreamHost socketServer = null;
	
	public synchronized static StreamHost getListener()
	{
		if (socketServer == null)
		{
			socketServer = new StreamHost();
		}
		if (!socketServer.isStarted())
		{
			socketServer.start();
		}
		
		return socketServer;		
	}
	
	
	private ServerSocket theSocket = null;
	private boolean isListening = false;
	
	private void registerSocket(Socket s) throws IOException
	{
		
		Worker ss = new Worker(s);
		ss.start();
	}

	public int getPort()
	{
		if (!isStarted()) return 0;
		return theSocket.getLocalPort();
	}
	
	public boolean isStarted()
	{
		return (theSocket != null && !theSocket.isClosed());
	}
	 
	public void stop()
	{
		isListening = false;
		try
		{
			if (theSocket != null && !theSocket.isClosed())
			{
				theSocket.close();
			}
		}
		catch (IOException x)
		{
		}
		theSocket = null;
	}
	
	/** for now, the port is 8082 */
	public void start()
	{
		if (theSocket != null)
		{
			stop();
		}
		try
		{
			theSocket = new ServerSocket(); //8082);
			theSocket.setSoTimeout(1000);
			theSocket.setReuseAddress(true);
			theSocket.bind(new InetSocketAddress(8082));
			
			isListening = true;
			new Thread(new Runnable() {
				@Override
				public void run()
				{
					while (isListening && theSocket != null && !theSocket.isClosed())
					{
						try
						{
							Socket s = theSocket.accept();							
							registerSocket(s);
						}
						catch  (java.net.SocketTimeoutException x)
						{
							// it's okay
						}
						catch (java.net.SocketException x)
						{
							// the socket was closed, exit
							isListening = false;
						}
						catch (IOException x)
						{
							isListening = false;
						}			
					}
				}
			}).start();;
		}
		catch (IOException x)
		{
		}
	}



public class Worker
{

	private final DryCereal dryWriter;
	private final DryCereal.Reader dryReader; //= new DryCereal.Reader();
	private final Socket clientSocket;
	private final PublishListener publishListener = new PublishListener();
	//private Thread runningThread = null;
	private boolean isRunning = false;
	private String clientKey;
	
	
	public Worker(InputStream in, OutputStream out)
	{
		dryReader = new DryCereal.Reader(in);
		dryWriter = new DryCereal(out);
		//BHClient.Error msg = new BHClient.Error(0, "Hello, world");

		clientSocket = null;
	}
	
	public Worker(Socket socket) throws IOException
	{
		if (socket == null || socket.isClosed()) throw new IOException("Invalid socket state");		
		socket.setSoTimeout(1000);
		
		dryReader = new DryCereal.Reader(socket.getInputStream());
		dryWriter = new DryCereal(socket.getOutputStream());
		clientSocket = socket;
	}
	
	public String getClientKey()
	{
		return clientKey;
	}
	
	protected void releaseClient()
	{
		if (clientKey == null) return;
		
		BHClientRegistration agent = BHClientRegistration.getClient(null, clientKey);
		if (agent != null) 
		{
			BHSession s = BHSession.getSession(agent.sessionID);
			if (s != null)
			{
				s.publishEvent.unsubscribe(publishListener);
				//s.getEngine().pub
			}
			
		}
	}
	
	public void setClientKey(String clientKey)
	{
		if (this.clientKey != null) 
			releaseClient();
		
		if (Utils.IsEmpty(clientKey))
			return; // continue living without a client key
		
		BHClientRegistration agent = BHClientRegistration.getClient(null, clientKey);
		if (agent != null) 
		{
			this.clientKey = clientKey;			
			BHSession s = BHSession.getSession(agent.sessionID);
			if (s != null)
			{
				s.publishEvent.subscribe(publishListener);
				//s.getEngine().pub
			}
		}	
	}
	
	private class PublishListener implements EventBox.EventHandler<BHSession.PublishEventArgs>
	{
	/** This is for the EventBox.EventHandler */
		@Override
		public boolean isListening()
		{
			//return !Utils.IsEmpty(clientKey);
			if (clientSocket != null) 
				return !clientSocket.isClosed();
			else
				return false;
		}
		
		private boolean invokeRunning = false;
		/** This is for the EventBox.EventHandler */
		@Override
		public  void invoke(BHSession.PublishEventArgs e)
		{
			if (invokeRunning)
			{
				//System.out.println("Invoke running for " + clientKey + ", timecode=" + e.timecode + ", skip");
				return;
			}
			if (Utils.IsEmpty(clientKey))
			{
				return; // continue living without a client key
			}
			invokeRunning = true;
			
			sendUpdate(e.timecode);
			invokeRunning = false;
		}
	}
	
	/**
	 * Grab an update from the current session and send it to the client
	 */
	public void sendUpdate(long sessionTimecode)
	{
		do
		{
			BHClientRegistration agent = BHClientRegistration.getClient(null, Worker.this.clientKey);
			if (agent == null)
			{
				System.out.println("Client not found for key: " + Worker.this.clientKey);
				break;
			}
			if (agent.timecode >= sessionTimecode) 
			{
				System.out.println("Client " + clientKey + " got old timecode " + sessionTimecode);
				break;
			}
			BHSession session = BHSession.getSession(agent.sessionID);
			if (session == null)
			{
				System.out.println("Invalid clientKey=" + Worker.this.clientKey + ", sessionID=" + agent.sessionID + ": session not found");
				Worker.this.clientKey = null;
				break;
			}
			BHClient.UpdateBin res = session.getUpdate(agent.timecode, agent.subscriptionID, agent.atomID);
			try
			{
				//System.out.println("StreamServer.PublishListener write update started, timecode " + res.status.timecode);
				synchronized(Worker.this.dryWriter)
				{
					Worker.this.dryWriter.addByte(BHClient.ElementCode.UPDATEBIN);
					res.toCereal(Worker.this.dryWriter);
					Worker.this.dryWriter.flush();
				}
				//System.out.println("StreamServer.PublishListener successful write update, timecode " + res.status.timecode);
			}
			catch (IOException x)
			{
				System.out.println("StreamServer.PublishListener failed to write update: " + x.getMessage());
				//StreamServer.this.releaseClient(); // this should stop the loop
			}
		} while (false);
	}
	
	/**
	 * The method processes incoming commands as received by mainLoop().
	 * Listening to the user (the client) is split between mainLoop() and process().
	 */
	public void processIncoming(BHClient.IElement element) throws IOException
	{
		BHClient.IElement res = null;
		if (element.getElementCode() == BHClient.ElementCode.ERROR)
		{
			element.toCereal(dryWriter);
			dryWriter.flush();
			return;
		}
				
		if (element.getElementCode() != BHClient.ElementCode.COMMAND)
		{
			throw new IllegalArgumentException("Invalid incoming element code: " + element.getElementCode());
		}
		
		BHClient.Command cmd = (BHClient.Command)element; //new BHClient.Command();
		//cmd.fromCereal(dryReader);
		
		if (BHSession.COMMAND.JOINCLIENT.equals(cmd.command))
		{
			setClientKey(cmd.stringArgs[0]);
			res = cmd;
			
		}
		else if (BHSession.COMMAND.CLOSE.equals(cmd.command))
		{
			stop();
			res = cmd;
			
		}
		else
		{
			res = BHSession.processCommand(this.clientKey, cmd);
		}
		/*
		if (this.clientKey == null)
		{
			res = BHSession.processServerCommand(cmd);
		}
		else
		{
			BHClientAgent agent = BHClientAgent.getClient(null, this.clientKey);
			if (agent == null)
			{
				throw new IllegalArgumentException("Invalid or missing client key");
			}
			
			BHSession session = BHSession.getSession(agent.sessionID);
			if  (session != null)
			{
				res = session.processCommand(agent.getID(), cmd);
			}
		}
		 */
		/*
		// do echo
		System.out.println("ECHO: " + Utils.formatDateTime(new Date()) + " " + element);
		dryWriter.addByte(element.getElementCode());
		element.toCereal(dryWriter);
		 */		

		if (res == null)
		{
			/*
			BHClient.Error err = new BHClient.Error(19, "No response for " + cmd.toString());
			dryWriter.addByte(BHClient.ElementCode.ERROR);
			err.toCereal(dryWriter);
			*/
			BHSession.postMessage(this.clientKey, MessageTargetEnum.ERROR, 0, "No response for " + cmd.toString());
		}
		else
		{
			/*
			dryWriter.addByte(res.getElementCode());
			try
			{
			res.toCereal(dryWriter);
			}
			catch (Exception x)
			{
				System.out.println("failed toCereal: " + x.getMessage());
			}
			*/
			BHSession.postMessage(this.clientKey, MessageTargetEnum.RECEIPT, 0, cmd.toString());
			
		}			
		dryWriter.flush();
	}
	
	/** This is for listening to the incoming commands */
	public void mainLoop()
	{
		try
		{
			while (isRunning && (clientSocket == null || !clientSocket.isClosed()))
			{				
				//System.out.println("mainloop " + Utils.formatDateTime(new Date()));
				// this is supposed to wait for 1000 msec (see setSoTimeout() above)  and kick out
				if (!dryReader.hasNext()) continue;
				try
				{
					DryCereal.Flake typeCodeFlake = dryReader.next();
					if (typeCodeFlake.type != DryCereal.CerealType.BYTE)
					{
						throw new IllegalArgumentException("Unexpected cereal element: " + typeCodeFlake);
					}
					byte typeCode = (Byte)typeCodeFlake.getByte();
					BHClient.IElement elem = BHClient.fromCereal(typeCode, dryReader);
					processIncoming(elem);
					
				}
				catch (IllegalArgumentException x)
				{
					String flak = dryReader.skipLine();
					System.out.println("invalid socket input: " + flak);
					BHClient.Error err = new BHClient.Error(18, x.getClass().getName() + ": " + x.getMessage());
					processIncoming(err);
					//err.toCereal(dryWriter);
					//dryWriter.flush();
				}
			}
		}
		catch (IOException x) 
		{
			System.out.println("failed to do IO: " + x.getMessage());				
		}
		catch (Exception x)
		{
			// just finish peacefully
			System.out.println("Exception in StreamHost.mainLoop: " + x.getMessage());				
		}
	}

	public void start()
	{
		if (isRunning) 
		{
			System.out.println("StreamHost.Worker is already running; start() ignored: " + this.toString());
			return;
		}
		System.out.println("StreamHost.Worker starting: " + this.toString());
		new Thread(new Runnable() {

			@Override
			public void run()
			{
				//r.mainLoop();
				Worker.this.isRunning = true;
				Worker.this.mainLoop();
				Worker.this.isRunning = false;
				System.out.println("StreamHost.Worker finished: " + this.toString());
			}
		}).start();
	}
	
	public void stop()
	{
		System.out.println("StreamHost.Worker stopping: " + this.toString());
		this.releaseClient();
		this.isRunning = false;
		if (this.clientSocket != null)
		{
			try
			{
			this.clientSocket.close();
			}
			catch (IOException x)
			{
				
			}
		}
	}
}
}