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
		/** how many coins left on the field */
		public int stageGold;
		public int sessionScore;
		public int sessionLives;
		
		public void toCereal(DryCereal to) throws IOException
		{
			super.toCereal(to);
			to.addShort(pacmanStage);
			to.addInt(stageGold);
			to.addInt(sessionScore);
			to.addShort(sessionLives);
		}

		public void fromCereal(DryCereal.Reader from)
		{
			super.fromCereal(from);
			pacmanStage = (int)from.next().getShort();
			stageGold = (int)from.next().getInteger();
			sessionScore = (int)from.next().getInteger();
			sessionLives = (int)from.next().getShort();
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
