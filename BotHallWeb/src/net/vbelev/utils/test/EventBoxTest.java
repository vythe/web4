package net.vbelev.utils.test;

//import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

//import net.vbelev.bothall.client.BHClient;
import net.vbelev.bothall.web.BHListener;
import net.vbelev.utils.*;

class EventBoxTest
{

	public class MyEventArgs extends EventBox.EventArgs
	{
		public String val;
		
		public MyEventArgs(String val)
		{
			this.val = val;
		}
		
		@Override
		public String toString()
		{
			return "[Args " + val + "]";
		}
	}
	
	public class MyEventHandler implements EventBox.EventHandler<MyEventArgs>
	{
		public String name;
		public boolean m_isListening = true; 
		
		public MyEventHandler(String name)
		{
			this.name = name;
		}
		
		@Override
		public String toString()
		{
			return "[Handler " + name + "]";
		}
		
		@Override
		public void invoke(MyEventArgs e)
		{
			System.out.println("Invoked " + name + " with " + e.val);
		}

		@Override
		public boolean isListening()
		{
			// TODO Auto-generated method stub
			return m_isListening;
		}
	}
	
	EventBox myBox = new EventBox();
	
	//@Test
	void test()
	{
		//Assertions.fail("Not yet implemented");
		MyEventHandler h1 = new MyEventHandler("H1");
		MyEventHandler h2 = new MyEventHandler("H2");
		MyEventHandler h3 = new MyEventHandler("H3");
		
		myBox.subscribe("test", h1);
		myBox.subscribe("test", h2);
		myBox.subscribe("test", h3);
		myBox.trigger("test", new MyEventArgs("call value-1"));
		System.out.println("post call value");
		h2.m_isListening = false;
		myBox.trigger("test", new MyEventArgs("another call value-2"));
		System.out.println("post call value-2");
		myBox.trigger("test", new MyEventArgs("another call value-3"));
		System.out.println("post call value-3");
		myBox.unsubscribe("test", h3);
		myBox.trigger("test", new MyEventArgs("third  call value-4"));
		System.out.println("post call value-4");
		myBox.completeTrigger("test");
	}

	@Test
	public void serverTest()
	{
		BHListener server = new BHListener();
		server.startIt();
		//System.console().printf("press any key");
		//System.console().readLine();
		try
		{
			Thread.sleep(1000000000);
			//Thread.currentThread(wait();
		}
		catch (InterruptedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("serverTest finished");
	}
}
