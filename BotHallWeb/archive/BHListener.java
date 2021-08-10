package net.vbelev.bothall.web;

import java.io.*;
import java.net.*;

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
	
	private void registerSocket(Socket s) throws IOException
	{
		/*
		StreamListener ss = new StreamListener(s);
		ss.start();
		*/
		
		//SocketRegistration r = new SocketRegistration(s);
		//synchronized(registrationList)
		//{
		//	registrationList.add(r);
		//}		
		
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
