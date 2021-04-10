package net.vbelev.utils.test;

import java.io.*;
import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import net.vbelev.utils.*;

import net.vbelev.bothall.client.*;

public class CerealTest
{
	@Test
	void testString() throws IOException
	{
		DryCereal d1 = new DryCereal();
		String s1 = "simple string";
		d1.addString(s1);
		d1.addInt(17);
		d1.addInt(18);
		String s1_2 = "Надо понимать, что \"рациональность\" в терминах типа \"рациональный актор\" или \"рациональное действие\" означает не \"разумный\" и тем более не \"нравственный\", а всего лишь \"направленный на достижение той цели, которую актор ставит перед собой\".";
		d1.addString(s1_2);
		d1.addInt(19);
		d1.addFloat(3.1415);
		d1.flush();
		String r1 = d1.pull();
		System.out.println("TestString: [" + s1 + "] = " + r1);
		
		InputStream is1 = new ByteArrayInputStream(r1.getBytes());
		DryCereal.Reader rd1 = new DryCereal.Reader(is1);
		//for (DryCereal flake : rd1)
		//{
		//}
		while (rd1.hasNext())
		{
			DryCereal.Flake flake = rd1.next();
			System.out.println(flake);
		}
		System.out.println("Rd1 - end");
		
		String s2 = "Надо понимать, что \"рациональность\" в терминах типа \"рациональный актор\" или \"рациональное действие\" означает не \"разумный\" и тем более не \"нравственный\", а всего лишь \"направленный на достижение той цели, которую актор ставит перед собой\".";
		DryCereal d2 = new DryCereal(System.out);
		d2.addString(s2);
		d2.flush();
		System.out.println("TestString: [" + s2 + "] completed.");
		//System.out.println("TestString: [" + s2 + "] = " + d2.toString());
	}

	@Test
	void testSpeed() throws IOException
	{
		BHClient.Cell testCell = new BHClient.Cell();
		testCell.id = 17;
		testCell.terrain = "TERRA".intern();
		testCell.x = 1;
		testCell.y = 2;
		testCell.z = 3;
		testCell.buffs = new BHClient.Buff[3];
		 
		for (int i = 0; i < 3; i++)
		{
			BHClient.Buff b = new BHClient.Buff();
			b.id = 23;
			b.ticks = 4;
			b.timecode = new Date().getTime();
			b.type ="Buff";
			testCell.buffs[i] = b; 
		}
		String testString = "short test string";
		
		// write out
		DryCereal d = new DryCereal();
		String cer = "";
		for (int r = 0; r < 10000; r++)
		{
			testCell.toCereal(d);
			d.addString(testString);
			cer = d.pull();
			
			DryCereal.Reader dr = new DryCereal.Reader(cer);
			BHClient.Cell cOut = new BHClient.Cell();
			cOut.fromCereal(dr);
			String strOur = (String)dr.next().getString();
			
		}
		
		// read in
		/*
		ByteArrayInputStream bis =new ByteArrayInputStream(cer.getBytes());
		
		DryCereal.Reader dr = new DryCereal.Reader(bis);
		
		for (int r = 0; r < 100000; r++)
		{
			bis.reset();
			BHClient.Cell cOut = new BHClient.Cell();
			cOut.fromCereal(dr);
		}
		*/
	}

	@Test
	void commandTest() throws IOException
	{
		DryCereal d1 = new DryCereal();
		BHClient.Command cmd = new BHClient.Command();
		
		cmd.command = "my test";
		cmd.timecode = 17;
		cmd.intArgs = new int[] {1,2,3,4,5};
		cmd.stringArgs = new String[] {"arg A", "arg (B)", "ещё что-то"};
		
		System.out.println("original command: " + cmd.toString());
		
		cmd.toCereal(d1);
		//d1.flush();
		String cereal = d1.toString();
	
		System.out.println("cereal: " + cereal);
		
		InputStream is1 = new ByteArrayInputStream(cereal.getBytes());
		DryCereal.Reader rd1 = new DryCereal.Reader(is1);
		
		BHClient.Command r1 = new BHClient.Command();
		r1.fromCereal(rd1);

		System.out.println("restored command: " + cmd.toString());
	}
	
	@Test
	void cellTest() throws IOException
	{
		 PipedOutputStream pipeOut = new PipedOutputStream();
		 PipedInputStream pipeIn = new PipedInputStream(pipeOut);

		DryCereal d1 = new DryCereal(pipeOut);
		DryCereal.Reader r1 = new DryCereal.Reader(pipeIn);

		
		BHClient.Cell c = new BHClient.Cell();
		c.x = 17;
		c.y = 18;
		c.z = 19;
		c.terrain = "STONE".intern();
		c.buffs = new BHClient.Buff[0];
		c.toCereal(d1);
		d1.flush();
		//String cer = d1.toString();
		//DryCereal.Reader r1 = new DryCereal.Reader(pipeIn);
		BHClient.Cell c2 = new BHClient.Cell();
		c2.fromCereal(r1);
		System.out.println(c2.toString());
		
		 
	}
}
