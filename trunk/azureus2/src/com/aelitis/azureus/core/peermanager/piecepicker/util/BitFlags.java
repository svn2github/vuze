/*
 * Created on Oct 30, 2005
 * Created by Joseph Bridgewater
 * Copyright (C) 2005, 2006 Aelitis, All Rights Reserved.
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
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.aelitis.azureus.core.peermanager.piecepicker.util;

import java.util.Arrays;

/**
 * @author MjrTom
 * A fairly light-weight, versitle boolean array of bit flags with administrative fields and methods
 * Originaly designed as a boolean array to correspond to the pieces in a torrent,
 * for example to show which pieces are; downloading, high priority, rarest, available, or whatever
 */

public class BitFlags
{
	// These are public so they can be read quickly.  Please don't try to modify them outside of the given methods. 
	public int			nbSet;		// how many are set
	public int			start;		// first one that is set
	public int			end;		// last one that is set
	final public int	length;
	public boolean[]	flags;		// the array of the flags

	public BitFlags(int count)
	{
		length =count;
		flags =new boolean[length];
		start =0;
		end =0;
		nbSet =0;
	}

	public BitFlags( boolean[]	_flags )
	{
		flags	= _flags;
		length	= flags.length;
		for (int i=0;i<length;i++){
			if ( flags[i]){
				nbSet++;
				if ( i < start ){
					start = i;
				}
				end	= i;
			}
		}
	}
	
	public void clear()
	{
		Arrays.fill(flags, false);
		start =0;
		end =0;
		nbSet =0;
	}

	public void setStart(final int i)
	{
		flags[i] =true;
		nbSet++;
		start =i;
	}

	public void set(final int i)
	{
		if (!flags[i])
		{
			flags[i] =true;
			nbSet++;
			if (start >i)
				start =i;
			if (end <i)
				end =i;
		}
	}

	public void setEnd(final int i)
	{
		flags[i] =true;
		nbSet++;
		end =i;
	}

	public void setOnly(final int i)
	{
		Arrays.fill(flags, start, end, false);
		nbSet =1;
		start =i;
		end =i;
		flags[i] =true;
	}

	public void setAll()
	{
		start =0;
		end =length -1;
		Arrays.fill(flags, start, end, true);
		nbSet =length;
	}
	
	/**
	 * Experimental.  Returns a new BitFlags with the flags set as the logical and of
	 *  both BitFlags and a length the same as the larger (union) of the two.
	 * @param other BitFlags to be ANDed with this BitFlags
	 * @return new BitFlags
	 */
	public BitFlags andUnion(final BitFlags other)
	{
		if (other ==null)
			return null;
		int resultSize =Math.max(this.length, other.length);
		BitFlags result =new BitFlags(resultSize);
		if (this.nbSet >0 &&other.nbSet >0)
		{
			int startI =Math.max(this.start, other.start);
			int endI =Math.min(this.end, other.end);
			int i =startI;
			for (; i <=endI; i++)
			{
				if (this.flags[i] &&other.flags[i])
				{
					result.set(i);
					break;
				}
			}
			for (; i <=endI; i++)
			{
				if (this.flags[i] &&other.flags[i])
				{
					result.setEnd(i);
				}
			}
		}
		return result;
	}

}
