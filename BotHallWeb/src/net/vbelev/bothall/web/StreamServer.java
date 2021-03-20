package net.vbelev.bothall.web;

import java.util.*;
import java.io.*;
import java.net.*;

import net.vbelev.bothall.client.*;
import net.vbelev.bothall.core.BHCollection.EntityTypeEnum;
import net.vbelev.bothall.web.*;
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
public class StreamServer //implements EventBox.EventHandler<BHSession.PublishEventArgs>
{

	private final DryCereal dryWriter;
	private final DryCereal.Reader dryReader; //= new DryCereal.Reader();
	private final Socket clientSocket;
	private final PublishListener publishListener = new PublishListener();
	//private Thread runningThread = null;
	
	private String clientKey;
	
	
	public StreamServer(InputStream in, OutputStream out)
	{
		//reader = new BufferedReader(new InputStreamReader(in));
		//writer = new BufferedWriter(new OutputStreamWriter(out));
		dryReader = new DryCereal.Reader(in);
		dryWriter = new DryCereal(out);
		//BHClient.Error msg = new BHClient.Error(0, "Hello, world");
		/*
		BHClient.Cell c = new BHClient.Cell();
		c.x = 17;
		c.y = 18;
		c.z = 19;
		c.terrain = "STONE";
		c.buffs = new BHClient.Buff[0];

		try
		{
			dryWriter.addByte(c.getElementCode());
			c.toCereal(dryWriter);
			System.out.println("drywriter-1 success: " + c.toString());
		}
		catch (IOException x)
		{
			System.out.println("drywriter-1 failed: " + x.getMessage());
		}
		*/
		clientSocket = null;
	}
	
	public StreamServer(Socket socket) throws IOException
	{
		if (socket == null || socket.isClosed()) throw new IOException("Invalid socket state");		
		socket.setSoTimeout(1000);
		
		dryReader = new DryCereal.Reader(socket.getInputStream());
		dryWriter = new DryCereal(socket.getOutputStream());
		clientSocket = socket;
		
		//BHClient.Error msg = new BHClient.Error(0, "Hello, world");
		/*
		BHClient.Cell c = new BHClient.Cell();
		c.x = 17;
		c.y = 18;
		c.z = 19;
		c.terrain = "STONE";
		c.buffs = new BHClient.Buff[0];
		try
		{
			dryWriter.addByte(c.getElementCode());
			c.toCereal(dryWriter);
			System.out.println("drywriter-2 success: " + c.toString());
		}
		catch (IOException x)
		{
			System.out.println("drywriter-2 failed: " + x.getMessage());
		}
		*/
	}
	
	public String getClientKey()
	{
		return clientKey;
	}
	
	protected void releaseClient()
	{
		if (clientKey == null) return;
		
		BHClientAgent agent = BHClientAgent.getClient(null, clientKey);
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
		
		BHClientAgent agent = BHClientAgent.getClient(null, clientKey);
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
			if (clientSocket != null) return !clientSocket.isClosed();
			return true;
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
			invokeRunning = true;
			
			BHClientAgent agent = BHClientAgent.getClient(null, StreamServer.this.clientKey);
			if (agent == null)
			{
				System.out.println("Client not found for key: " + StreamServer.this.clientKey);
				return;
			}
			if (agent.timecode >= e.timecode) 
			{
				System.out.println("Client " + clientKey + " got old timecode " + e.timecode);
				return;
			}
			BHSession session = BHSession.getSession(agent.sessionID);
			if (session == null)
			{
				System.out.println("Invalid clientKey=" + StreamServer.this.clientKey + ", sessionID=" + agent.sessionID + ": session not found");
				StreamServer.this.clientKey = null;
				return;
			}
			BHStorage.UpdateBin res = session.storage.getUpdate(session.getEngine(), agent.timecode, agent.subscriptionID, agent.atomID);
			try
			{
				//System.out.println("StreamServer.PublishListener write update started, timecode " + res.status.timecode);
				StreamServer.this.dryWriter.addByte(BHClient.ElementCode.UPDATEBIN);
				res.toCereal(StreamServer.this.dryWriter);
				StreamServer.this.dryWriter.flush();
				//System.out.println("StreamServer.PublishListener successful write update, timecode " + res.status.timecode);
			}
			catch (IOException x)
			{
				System.out.println("StreamServer.PublishListener failed to write update: " + x.getMessage());
				//StreamServer.this.releaseClient(); // this should stop the loop
			}
			invokeRunning = false;
		}
	}
	
	/**
	 * The method processes incoming commands as received by mainLoop().
	 * Listening to the user (the client) is split between mainLoop() and process().
	 */
	public void process(BHClient.Element element) throws IOException
	{
		BHClient.Element res = null;
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
		
		if ("JOINCLIENT".equals(cmd.command))
		{
			setClientKey(cmd.stringArgs[0]);
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
			BHSession.postMessage(this.clientKey, EntityTypeEnum.ERROR, 0, "No response for " + cmd.toString());
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
			BHSession.postMessage(this.clientKey, EntityTypeEnum.RECEIPT, cmd.timecode, cmd.toString());
			
		}			
		dryWriter.flush();
	}
	
	/** This is for listening to the incoming commands */
	public void mainLoop()
	{
		try
		{
			while (clientSocket == null || !clientSocket.isClosed())
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
					BHClient.Element elem = BHClient.fromCereal(typeCode, dryReader);
					process(elem);
					
				}
				catch (IllegalArgumentException x)
				{
					String flak = dryReader.skipLine();
					System.out.println("invalid socket input: " + flak);
					BHClient.Error err = new BHClient.Error(18, x.getClass().getName() + ": " + x.getMessage());
					process(err);
					//err.toCereal(dryWriter);
					//dryWriter.flush();
				}
			}
		}
		catch (IOException x) 
		{
			System.out.println("failed to do IO: " + x.getMessage());				
		}
	}

	public void start()
	{
		new Thread(new Runnable() {

			@Override
			public void run()
			{
				//r.mainLoop();
				StreamServer.this.mainLoop();
			}
		}).start();
	}
}
