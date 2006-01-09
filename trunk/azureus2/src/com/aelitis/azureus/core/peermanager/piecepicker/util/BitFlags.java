/*
 * Created on Oct 30, 2005
 * Created by Joseph Bridgewater
 * Copyright (C) 2005 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.peermanager.piecepicker.util;

import java.util.Arrays;

/**
 * @author MjrTom
 * This provides a boolean array of bit flags with administrative fields and methods
 * Originaly designed as a boolean array to correspond to the pieces in a torrent,
 * for example to show which pieces are; downloading, high priority, rarest, available, or whatever
 * 
 */

public class BitFlags
{
	private int			start =Integer.MAX_VALUE;	// first one that is set
	private int			end =Integer.MIN_VALUE;		// last one that is set
	private int			size;
	private int			nb =-1;				// how many are set
	private boolean[]	flags;				// the array of the flags

	public BitFlags(int count)
	{
		size =count;
		flags =new boolean[size];
		start =0;
		end =0;
		nb =0;
	}

	public int getNbSet()
	{
		return nb;
	}
	
	public int getStartIndex()
	{
		return start;
	}
	
	public int getEndIndex()
	{
		return end;
	}
	
	public boolean[] getFlags()
	{
		return flags;
	}
	
	public boolean get(int i)
	{
		return flags[i];
	}

	public void copy(boolean[] b)
	{
		flags =b;
	}

	public void clear()
	{
		Arrays.fill(flags, false);
		start =0;
		end =0;
		nb =0;
	}

	public void set(int i)
	{
		flags[i] =true;
		nb++;
		if (start >i)
			start =i;
		if (end <i)
			end =i;
	}

	public void setOnly(int i)
	{
		Arrays.fill(flags, start, end, false);
		nb =1;
		start =i;
		flags[i] =true;
		end =i;
	}

}
