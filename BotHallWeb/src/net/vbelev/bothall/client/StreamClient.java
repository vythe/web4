package net.vbelev.bothall.client;

import java.io.*;

import net.vbelev.bothall.client.*;
import net.vbelev.utils.*;
/** Maintains a BHClient.Collectio
 * by reading from a stream (a pipe). Sends commands out through another pipe.
 * @author Vythe
 *
 */
public class StreamClient
{

	public final BHClient.Collection collection = new BHClient.Collection();
	//private final BufferedReader reader;
	private final BufferedWriter writer;
	private final DryCereal.Reader dryReader; //= new DryCereal.Reader();
	
	public StreamClient(InputStream in, OutputStream out)
	{
		//reader = new BufferedReader(new InputStreamReader(in));
		dryReader = new DryCereal.Reader(in);
		writer = new BufferedWriter(new OutputStreamWriter(out));
	}
	
	/** synchronously reads from tne input stream until the Status record is encountered
	 * or until the stream is ended.
	 * Returns false if there was nothing to read; 
	 * returns true if an update was received; 
	 * throws IOException if there was a problem  
	 */
	public boolean readUp() throws IOException
	{
		if (!dryReader.hasNext())
		{
			return false;
		}
		do
		{
			DryCereal.Flake typeCodeFlake =dryReader.next();
			if (typeCodeFlake.type != DryCereal.CerealType.BYTE)
			{
				throw new IOException("Unexpected cereal element: " + typeCodeFlake);
			}
			byte typeCode = (Byte)typeCodeFlake.value;
			if (typeCode == BHClient.TypeCode.CELL)
			{
				BHClient.Cell c = new BHClient.Cell();
				c.fromCereal(dryReader);
				collection.putCell(c);
			}
			if (typeCode == BHClient.TypeCode.ITEM)
			{
				BHClient.Item i = new BHClient.Item();
				i.fromCereal(dryReader);
				collection.items.put(i.id, i);
			}
			if (typeCode == BHClient.TypeCode.MOBILE)
			{
				BHClient.Mobile m = new BHClient.Mobile();
				m.fromCereal(dryReader);
				collection.mobiles.put(m.id, m);
			}
			else if (typeCode == BHClient.TypeCode.STATUS)
			{
				BHClient.Status s = new BHClient.Status();
				collection.status = s;
				//return dryReader.hasNext();
				return true;
			}
			if (typeCode == BHClient.TypeCode.BUFF)
			{
				BHClient.Buff b = new BHClient.Buff();
				b.fromCereal(dryReader);
				//collection.items.put(i.id, i);
			}
			else
			{
				//return false;
				throw new IOException("Unexpected element type: " + typeCode);
			}
			
		} 
		while (true);		
	}
}
