package net.vbelev.bothall.client;

import java.io.*;
import java.util.*;

import net.vbelev.utils.DryCereal;
import net.vbelev.utils.Utils;

public class PacmanClient extends BHClient.Client
{
	
	public static class PacmanStatus extends BHClient.Status
	{
		public int pacmanStage;
		
		public void toCereal(DryCereal to) throws IOException
		{
			super.toCereal(to);
			to.addInt(pacmanStage);
		}

		public void fromCereal(DryCereal.Reader from)
		{
			super.fromCereal(from);
			pacmanStage = (int)from.next().getInteger();
		}		
	}
	
	private PacmanStatus _status = null;

	@Override
	public BHClient.Status getStatus()
	{
		if (_status == null) _status = new PacmanStatus();
		return _status;
	}
	

}
