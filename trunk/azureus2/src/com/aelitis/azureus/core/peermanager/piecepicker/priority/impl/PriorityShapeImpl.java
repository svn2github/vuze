/*
 * Created by Joseph Bridgewater
 * Created on Jan 17, 2006
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

package com.aelitis.azureus.core.peermanager.piecepicker.priority.impl;

import com.aelitis.azureus.core.peermanager.piecepicker.priority.PriorityShape;
import com.aelitis.azureus.core.util.HashCodeUtils;

/**
 * @author MjrTom Jan 17, 2006
 */
abstract public class PriorityShapeImpl
	implements PriorityShape, Cloneable
{
    public long mode;
    public int  priority;	

	protected PriorityShapeImpl(final long m, final int p)
	{
        mode =m;
        priority =p;
	}
    
    public int hashCode()
    {
        return HashCodeUtils.hashMore(priority, mode);
    }
	
    public boolean equals(Object other)
    {
        if (other ==null ||!(other instanceof PriorityShape))
            return false;
        final PriorityShapeImpl priorityShape =(PriorityShapeImpl)other;
        if (mode !=priorityShape.mode)
            return false;
        if (priority !=priorityShape.priority)
            return false;
        return true;
    }

    public void setPriority(final int i)
    {
        priority =i;
    }
    
    public int getPriority()
    {
        return priority;
    }
    
    public int getPriority(int pieceNumber)
    {
    	if (!isSelected(pieceNumber))
    		return 0;
    	if (!isRamp(pieceNumber))
    		return priority;
    	final int start =getStart();
    	final int end =getEnd();
		final float per =priority /(start -end);
    	if (!isReverse(pieceNumber))
    		return (int)(per *(end -pieceNumber));
    	return (int)(per *(pieceNumber -start));
    }
    
    
    public void setMode(final long i)
    {
        mode =i;
    }
    
    public long getMode()
	{
		return mode;
	}
    
    
    public boolean isNoRandom()
    {
        return (mode &PRIORITY_MODE_NO_RANDOM) ==PRIORITY_MODE_NO_RANDOM;
    }

    public boolean isIgnoreRarity()
    {
        return (mode &PRIORITY_MODE_IGNORE_RARITY) ==PRIORITY_MODE_IGNORE_RARITY;
    }

    public boolean isFullPieces()
    {
        return (mode &PRIORITY_MODE_FULL_PIECES) ==PRIORITY_MODE_FULL_PIECES;
    }

    public boolean isAutoReserve()
    {
        return (mode &PRIORITY_MODE_AUTO_RESERVE) ==PRIORITY_MODE_AUTO_RESERVE;
    }

    public boolean isReverse()
    {
        return (mode &PRIORITY_MODE_REVERSE_ORDER) ==PRIORITY_MODE_REVERSE_ORDER;
    }
    
    public boolean isAutoSlide()
    {
        return (mode &PRIORITY_MODE_AUTO_SLIDE) ==PRIORITY_MODE_AUTO_SLIDE;
    }
    
    public boolean isRamp()
    {
        return (mode &PRIORITY_MODE_RAMP) ==PRIORITY_MODE_RAMP;
    }
    
    public boolean isStaticPriority()
    {
        return (mode &PRIORITY_MODE_STATIC_PRIORITY) ==PRIORITY_MODE_STATIC_PRIORITY;
    }
    
}
