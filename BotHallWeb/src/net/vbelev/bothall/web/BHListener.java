package net.vbelev.bothall.web;

import java.util.*;
import java.io.*;
import java.net.*;

import net.vbelev.bothall.client.*;
import net.vbelev.bothall.web.*;
import net.vbelev.utils.*;
/**
 * Listents to a ServerSocket, creates StreamServer instances for incoming connections.
 */
public class BHListener
{
	/** BotHallServer should be a true singleton, but it's too much effort,
	 * so it is a static variable
	 */
	private static BHListener socketServer = null;
	
	public synchronized static BHListener getSocketServer()
	{
		if (socketServer == null)
		{
			socketServer = new BHListener();
		}
		if (!socketServer.isStarted())
		{
			socketServer.startIt();
		}
		
		return socketServer;		
	}
	
	
	private ServerSocket theSocket = null;
	private boolean isListening = false;
	
	/**
	 * Registration is created before we get the client key.
	 * Clients can work with different sessions (engines), so each 
	 * should subscribe to his own updates.
	 * 
	 * This SocketRegistration is not used, having been replaced with StreamServer.
	 */
	private static class SocketRegistration implements EventBox.EventHandler<BHSession.PublishEventArgs>
	{
		public String clientKey;
		public StreamClient stream;
		public Socket registeredSocket;
		private final DryCereal.Reader dryReader; //= new DryCereal.Reader();
		private final DryCereal dryWriter;
		//private final BufferedWriter writer;
		
		public SocketRegistration(Socket s) throws IOException
		{
			//java.io.PipedInputStream pi; // would be nice to use pipes for local listeners
			s.setSoTimeout(1000);
			this.registeredSocket = s;
			dryReader = new DryCereal.Reader(s.getInputStream());
			dryWriter = new DryCereal(s.getOutputStream());
			//writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
		}
		
		@Override
		public boolean isListening()
		{
			return !Utils.IsEmpty(clientKey);
		}
		
		@Override
		public void invoke(BHSession.PublishEventArgs e)
		{
			// TODO Auto-generated method stub
			BHClientRegistration agent = BHClientRegistration.getClient(null, this.clientKey);
			if (agent == null)
			{
				System.out.println("Client not found for key: " + this.clientKey);
				return;
			}
			if (agent.timecode >= e.timecode) 
			{
				System.out.println("Client " + clientKey + " got old timecode " + e.timecode);
				return;
			}
			PacmanSession session = PacmanSession.getSession(agent.sessionID);
			if (session == null)
			{
				System.out.println("Invalid clientKey=" + this.clientKey + ", sessionID=" + agent.sessionID + ": session not found");
				this.clientKey = null;
				return;
			}
			BHStorage.UpdateBin res = session.storage.getUpdate(session.getEngine(), agent.timecode, agent.subscriptionID, agent.atomID);
			try
			{
				this.dryWriter.addByte(BHClient.ElementCode.UPDATEBIN);
				res.toCereal(this.dryWriter);
				this.dryWriter.flush();
			}
			catch (IOException x)
			{
				System.out.println("BotHallServer.SocketRegistration.invoke failed to write update: " + x.getMessage());
				//this.releaseClient(); // this should stop the loop
			}
		}
		
		/**
		 * 
		 */
		public void setClient(BHClientRegistration agent)
		{
			PacmanSession ps = PacmanSession.getSession(agent.sessionID);
			if (ps == null || ps.getEngine() == null)
			{
				throw new IllegalArgumentException("Invalid session ID " + agent.sessionID); 
			}
			this.clientKey = agent.clientKey;
			ps.publishEvent.subscribe(this);
		}
		
		public void releaseClient()
		{
			BHClientRegistration agent = BHClientRegistration.getClient(null, clientKey);
			if (agent != null)
			{
				PacmanSession ps = PacmanSession.getSession(agent.sessionID);
				if (ps != null)
				{
					ps.publishEvent.unsubscribe(this);
					ps.detachAgent(agent);
				}
				agent.detach();
			}
			clientKey = null;
			
			if (registeredSocket != null && !registeredSocket.isClosed())
			{
				try
				{
					registeredSocket.close();
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				registeredSocket = null;
			}
		}
		
		public void mainLoop()
		{
			try
			{
				while (registeredSocket != null && !registeredSocket.isClosed())
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
						if (typeCode != BHClient.ElementCode.COMMAND)
						{
							throw new IllegalArgumentException("Invalid incoming element code: " + typeCode);
						}
						
						BHClient.Command cmd = new BHClient.Command();
						cmd.fromCereal(dryReader);
						
						// do echo
						/*
						BHClient.Error err = new BHClient.Error(0, "ECHO: " + cmd.toString());
						System.out.println(Utils.formatDateTime(new Date()) + " " + err);
						dryWriter.addByte(BHClient.ElementCode.ERROR);
						err.toCereal(dryWriter);
						*/
						BHSession.postMessage(this.clientKey, net.vbelev.bothall.core.BHCollection.EntityTypeEnum.ERROR, 0, "ECHO: " + cmd.toString());						
						dryWriter.flush();
						
					}
					catch (Exception x)
					{
						String flak = dryReader.skipToEnd();
						System.out.println("invalid socket input: " + flak);
						BHClient.Error err = new BHClient.Error(0, x.getMessage());
						err.toCereal(dryWriter);
						dryWriter.flush();
					}
				}
			}
			catch (IOException x) 
			{
				System.out.println("failed to do IO: " + x.getMessage());				
			}
		}
	}
	
	private final List<SocketRegistration> registrationList = new ArrayList<SocketRegistration>();
	
	private void registerSocket(Socket s) throws IOException
	{
		StreamServer ss = new StreamServer(s);
		
		//SocketRegistration r = new SocketRegistration(s);
		//synchronized(registrationList)
		//{
		//	registrationList.add(r);
		//}		
		
		ss.start();
		/*
		new Thread(new Runnable() {

			@Override
			public void run()
			{
				//r.mainLoop();
				ss.mainLoop();
			}
		}).start();
		*/
	}
	
	/* maybe later
	private static BHClientAgent createAgent(PacmanSession s)
	{
		BHClientAgent agent = BHClientAgent.createAgent();
		agent.sessionID = s.getID();
		agent.subscriptionID = s.getEngine().getMessages().addSubscription();
		return agent;
	}

	private static void detachAgent(BHClientAgent agent)
	{
		if (agent == null  || agent.getID() == 0) return;
		// we need to check the session and possibly stop it...
		
		PacmanSession s = PacmanSession.getSession(agent.sessionID);	
		if (s != null)
		{
			s.getEngine().getMessages().removeSubscription(agent.subscriptionID);
		}
		agent.subscriptionID = 0;
		agent.sessionID = 0;
		agent.detach();
	}	
	*/
	
	public int getPort()
	{
		if (!isStarted()) return 0;
		return theSocket.getLocalPort();
	}
	
	public boolean isStarted()
	{
		return (theSocket != null && !theSocket.isClosed());
	}
	 
	public void stopIt()
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
	public void startIt()
	{
		if (theSocket != null)
		{
			stopIt();
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

}
