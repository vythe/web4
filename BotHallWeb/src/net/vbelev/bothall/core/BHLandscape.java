package net.vbelev.bothall.core;

import java.util.*;
import net.vbelev.utils.Utils;
//import net.vbelev.utils.Utils.EnumElement;
/**
 * The landscape for now will be an array of cells, forming a (hopefully) 
 * contiguous field of travel.
 * @author Vythe
 *
 */
public class BHLandscape
{
	public static enum TerrainEnum
	{
		@Utils.EnumElement(code = 0, description = "Out of bounds")
		VOID,
		@Utils.EnumElement(code = 1, description = "Good land")
		LAND,
		@Utils.EnumElement(code = 2, description = "Road")
		ROAD,
		@Utils.EnumElement(code = 3, description = "Water")
		WATER,
		@Utils.EnumElement(code = 4, description = "Underwater")
		UNDERWATER,
		@Utils.EnumElement(code = 5, description = "Stones")
		STONE
	}
	
	public static interface Coords 
	{
		int getX();
		int getY();
		int getZ();
	}
	
	public static class CoordsBase implements Coords
	{
		private int m_x;
		private int m_y;
		private int m_z;
		
		private CoordsBase() 
		{
		}
				
		public CoordsBase(Coords c)
		{
			m_x = c.getX();
			m_y = c.getY();
			m_z = c.getZ();
		}
		
		public CoordsBase(int x, int y, int z) 
		{
			m_x = x;
			m_y = y;
			m_z = z;
		}
				
		@Override
		public final int getX()
		{
			return m_x;
		}
		@Override
		public final int getY()
		{
			return m_y;
		}
		@Override
		public final int getZ()
		{
			return m_z;
		}
		
		public final CoordsBase setX(int x) 
		{
			CoordsBase res = new CoordsBase();
			res.m_x = x; //this.m_x;
			res.m_y = this.m_y;
			res.m_z = this.m_z;
			return res;
		}
		
		public final CoordsBase setY(int y) 
		{
			CoordsBase res = new CoordsBase();
			res.m_x = this.m_x;
			res.m_y = y; //this.m_y;
			res.m_z = this.m_z;
			return res;
		}
		
		public final CoordsBase setZ(int z) 
		{
			CoordsBase res = new CoordsBase();
			res.m_x = this.m_x;
			res.m_y = this.m_y;
			res.m_z = z; //this.m_z;
			return res;
		}
		
		public String toString()
		{
			return print(this);
		}
		
		public static String print(Coords c) 
		{
			if (c == null) return "[]";
			return "[" + c.getX() + ", " + c.getY() + ", "+ c.getZ() + "]";
		}
	}
	
	public static Coords coordsPoint(int x, int y, int z)
	{
		return new CoordsBase(x, y, z);
	}
	
	public static Coords coordsPoint(Coords c)
	{
		return new CoordsBase(c);
	}
	
	public static boolean equalCoords(Coords c1, Coords c2)
	{
		if (c1 == c2) return true;
		if (c1 == null || c2 == null) return false;
		return (c1.getX() == c2.getX()
				&& c1.getY() == c2.getY()
				&& c1.getZ() == c2.getZ()
		);
	}
	
	/** cellShifts[ind] = {dx, dx, dz, reverseDir} */
	public static final int[][] cellShifts = {
			{ 0,  0,  0,  0}, // 0 : self
			{ 1,  0,  0,  2}, // 1: x++
			{-1,  0,  0,  1}, // 2: x--
			{ 0,  1,  0,  6}, // 3: y++
			{ 1,  1,  0,  8},
			{-1,  1,  0,  7}, 
			{ 0, -1,  0,  3}, // 6: y--
			{ 1, -1,  0,  5},
			{-1, -1,  0,  4},
			{ 0,  0,  1, 18}, // 9 : z++
			{ 1,  0,  1, 20}, //10
			{-1,  0,  1, 19},
			{ 0,  1,  1, 24},
			{ 1,  1,  1, 26}, 
			{-1,  1,  1, 25}, 
			{ 0, -1,  1, 21}, // 15
			{ 1, -1,  1, 23},
			{-1, -1,  1, 22},
			{ 0,  0, -1,  9}, //18 : z--
			{ 1,  0, -1, 11},
			{-1,  0, -1, 10}, //20
			{ 0,  1, -1, 15},
			{ 1,  1, -1, 17},
			{-1,  1, -1, 16}, 
			{ 0, -1, -1, 12}, 
			{ 1, -1, -1, 14},
			{-1, -1, -1, 13} // 26
	};
	
	
	/** let's make the cell immutable */
	public static class Cell implements Coords
	{
		private int m_id;
		private int m_x;
		private int m_y;
		private int m_z;
		private long m_timecode;
		
		private TerrainEnum m_terrain;
		
		public int getID() { return m_id; }
		public int getX() { return m_x; }
		public int getY() { return m_y; }
		public int getZ() { return m_z; }
		public TerrainEnum getTerrain() { return m_terrain; }
		public long getTimecode() { return m_timecode; }
		
		public Cell()
		{
			m_terrain = TerrainEnum.VOID;
		}
		
		public Cell terrain(TerrainEnum t)
		{
			return new Cell(m_x, m_y, m_z, t);
		}
		
		public Cell(int x, int y, int z, TerrainEnum t)
		{
			this.m_x = x;
			this.m_y = y;
			this.m_z = z;
			this.m_terrain = t;
		}
		
		public String toString()
		{
			return "[" + m_x + ", " + m_y + ", " + m_z + " " + m_terrain + "]";
		}
	}

	/** probably to be renamed */
	public long timecode;

	private int latestCellID = 0;
	public final List<Cell> cells = new ArrayList<Cell>();
	private final List<Cell> newCells = new ArrayList<Cell>();
	
	private static Cell findCell(Collection<Cell> coll, int x, int y, int z, boolean withCreate)
	{
		Cell res = coll.stream()
				.filter(q -> q.m_x == x && q.m_y == y && q.m_z == z)
				.findAny()
				.orElse(null)
		;
		if (res == null && withCreate)
		{
			res = new Cell(x, y, z, TerrainEnum.VOID);
		}
		return res;
	}
	
	public Cell getCell(Coords c)
	{
		return findCell(cells, c.getX(), c.getY(), c.getZ(), true);
	}
	
	public Cell getCell(int x, int y, int z)
	{
		/*Cell res = cells.stream()
				.filter(q -> q.m_x == x && q.m_y == y && q.m_z == z)
				.findAny()
				.orElse(null)
		if (res == null)
		{
			return new Cell(x, y, z, TerrainEnum.VOID);
		}
		else
		{
			return res;
		}
		;*/
		Cell res = findCell(cells, x, y, z, true);
		return res;
	}	
	
	public Cell getNextCell(Coords c, int direction)
	{
		if (direction < 0 || direction >= cellShifts.length)
		{
			return new Cell(c.getX(), c.getY(), c.getZ(), TerrainEnum.VOID);
		}
		int[] shifts = cellShifts[direction];
		Cell res = findCell(cells, c.getX() + shifts[0], c.getY() + shifts[1], c.getZ() + shifts[2], true);
		return res;
	}
	
	public Cell getNextCell(int x, int y, int z, int direction)
	{
		/*Cell res = cells.stream()
				.filter(q -> q.m_x == x && q.m_y == y && q.m_z == z)
				.findAny()
				.orElse(null)
		if (res == null)
		{
			return new Cell(x, y, z, TerrainEnum.VOID);
		}
		else
		{
			return res;
		}
		;*/
		if (direction < 0 || direction >= cellShifts.length)
		{
			return new Cell(x, y, z, TerrainEnum.VOID);
		}
		int[] shifts = cellShifts[direction];
		Cell res = findCell(cells, x + shifts[0], y + shifts[1], z + shifts[2], true);
		return res;
	}

	public Cell getByID(int id)
	{
		Cell res = cells.stream()
			.filter(q -> q.m_id == id)
			.findAny()
			.orElse(null)
		;
		return res;

	}
	
	/**
	 * returns a cube of the cells around the given point and the point itself
	 * in the fixed order:
	 */
	public Cell[] closestCells(int x, int y, int z)
	{
		Cell[] res = new Cell[27]; 
		// shifts are 0, 1, -1
		// 0 -> 0, 1 -> 1, -1 -> 2
		// (x + 1) % 3 - 1
		cells.stream()
			.filter(q -> 
				q.m_x >= x - 1 && q.m_x <= x + 1 &&
				q.m_y >= y - 1 && q.m_y <= y + 1 &&
				q.m_z >= z - 1 && q.m_z <= z + 1)
			.forEach(q -> 
				//res[(q.m_x - x) + (q.m_y - y) * 3 + (q.m_z - z) * 9 + 13] = q
			res[(q.m_x - x + 3) % 3 + ((q.m_y - y + 3) % 3)* 3 + ((q.m_z - z + 3) % 3) * 9] = q
			)
		;
		for (int i = 0; i < 27; i++)
		{
			if (res[i] == null)
			{
				int[] shift = cellShifts[i];
				res[i] = new Cell(x + shift[0], y + shift[1], z + shift[2], TerrainEnum.VOID);
			}			
		}
		/*
		for (int ix = -1; ix < 2; ix++)
		{
			for (int iy = -1; iy < 2; iy++)
			{
				for (int iz = -1; iz < 2; iz++)
				{
					//int ind = ix + iy * 3  + iz * 9 + 13;
					int ind = ix % 3 + (iy % 3)* 3 + (iz % 3) * 9;
					if (res[ind] == null)
					{
						res[ind] = new Cell(x + ix, y + iy, z + iz, TerrainEnum.VOID);
					}
				}
			}
		}
		*/
		return res;
	}
	
	public Cell[] closestCells(Coords c)
	{
		return closestCells(c.getX(), c.getY(), c.getZ());
	}
	
	public void setCell(Cell cell)
	{
		synchronized (newCells)
		{
			Cell c1 = newCells.stream()
					.filter(q -> q.m_x == cell.m_x && q.m_y == cell.m_y && q.m_z == cell.m_z)
					.findAny()
					.orElse(null)
			;
			if (c1 != null)
			{
				newCells.remove(c1);
			}
			cell.m_id = ++latestCellID;
			newCells.add(cell);
		}
	}
	
	public boolean hasChanges()
	{
		return !newCells.isEmpty();
	}
	
	public BHLandscape publish(long newTimecode)
	{
		BHLandscape res = new BHLandscape();
		ArrayList<Cell> copyCells = new ArrayList<Cell>();
		synchronized (newCells)
		{
			copyCells.addAll(newCells);
		}
		//for(Cell cell : cells)
		//{
		//	cell.m_timecode = newTimecode;
		//}
		
		for(Cell cell : cells)
		{
			Cell c1 = copyCells.stream()
					.filter(q -> q.m_x == cell.m_x && q.m_y == cell.m_y && q.m_z == cell.m_z)
					.findAny()
					.orElse(null)
			;
			if (c1 == null)
			{
				res.cells.add(cell);
			}
			else
			{
				copyCells.remove(c1);
				res.cells.add(c1);
			}
		}
		
		for (Cell cell: copyCells)
		{
			cell.m_timecode = newTimecode;
			res.cells.add(cell);
		}
		//res.timecode = newTimecode; // not really needed
		return res;
	}
}
