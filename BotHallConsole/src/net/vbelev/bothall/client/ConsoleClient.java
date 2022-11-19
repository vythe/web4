package net.vbelev.bothall.client;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;

//import net.vbelev.bothall.client.BHClient.IElement;
import net.vbelev.utils.*;

public class ConsoleClient
{
	private Socket s = null;
	public StreamClient<PacmanClient> client = null;
	
	public void connect(String host, int port) throws IOException
	{
		s = new Socket();
		s.setReuseAddress(true);
		s.setSoTimeout(1000);
		s.connect(new InetSocketAddress(host, port));
		//s = new Socket(host, port);
		client = new StreamClient<PacmanClient>(s.getInputStream(), s.getOutputStream(), new PacmanClient());
	}

	public static void main(String[] args)
	{
		// TODO Auto-generated method stub
		System.out.println("hello, world1 " + Utils.formatDateTime(new Date()));
		
		try
		{
			BufferedReader rd = new BufferedReader(new InputStreamReader(System.in));
			
			ConsoleClient cc = new ConsoleClient();
			
			cc.connect("localhost", 8082);
			cc.client.start();
			cc.client.setOnUpdate(new StreamClient.OnUpdate()
					{

						@Override
						public void onUpdate()
						{
							// TODO Auto-generated method stub
							// TODO Auto-generated method stub
							BHClient.IElement queuedElem = null;
							while ((queuedElem = (BHClient.IElement)cc.client.elementQueue.poll()) != null)
							{
								System.out.println("queue: " + queuedElem.toString());
							}
							
							//System.out.println(Utils.formatDateTime(new Date()) + " timecode: " + cc.client.collection.status.timecode);
							String summary = Utils.formatDateTime(new Date()) + " timecode: ";
							if (cc.client == null) summary += " no client";
							else if (cc.client.collection == null) summary += " no collection";
							else if (cc.client.collection.getStatus() == null) summary += " no status";
							else summary += cc.client.collection.getStatus().timecode;
							System.out.println(summary);
							
						}
					});
			/*
			cc.client.updateEvent.subscribe(new EventBox.EventHandler<EventBox.EventArgs>()
			{

				@Override
				public boolean isListening()
				{
					// TODO Auto-generated method stub
					return true;
				}

				@Override
				public void invoke(EventArgs e)
				{
					// TODO Auto-generated method stub
					BHClient.Element queuedElem = null;
					while ((queuedElem = cc.client.elementQueue.poll()) != null)
					{
						System.out.println("queue: " + queuedElem.toString());
					}
					
					//System.out.println(Utils.formatDateTime(new Date()) + " timecode: " + cc.client.collection.status.timecode);
					String summary = Utils.formatDateTime(new Date()) + " timecode: ";
					if (cc.client == null) summary += " no client";
					else if (cc.client.collection == null) summary += " no collection";
					else if (cc.client.collection.status == null) summary += " no status";
					else summary += cc.client.collection.status.timecode;
					System.out.println(summary);
				}
			});
			*/
			String input = "";
			do
			{
				input = rd.readLine();
				if (input == null) break;
				else if (input.equals("x")) break;
				else if (input.equals("s")) 
				{
					BHClient.Command cmd = new BHClient.Command();
					cmd.command = "from_client";
					cmd.intArgs = new int[] {17, 18, 19};
					cmd.stringArgs = new String[] {"strArg"};
					cc.client.writeCommand(cmd);
				}
				else
				{
					System.out.println("input not supported: " + input);
				}
				
			} while(input != null);
			
			cc.client.stop();
		}
		catch (Exception x)
		{
			System.out.println("ConsoleClient failed: " + x.getMessage());
			x.printStackTrace();
		}
	}

}
