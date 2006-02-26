/*
 * Created by Joseph Bridgewater
 * Created on Jan 26, 2006
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
import com.aelitis.azureus.core.peermanager.piecepicker.util.BitFlags;
import com.aelitis.azureus.core.util.HashCodeUtils;

/**
 * @author MjrTom Jan 26, 2006
 */
public class PriorityShapeBitFlagsImpl
	extends PriorityShapeImpl
	implements PriorityShape, Cloneable
{
    /**  the selection criteria for the shape */ 
	public BitFlags	field;
	
	public PriorityShapeBitFlagsImpl(final long m, final int p, final int size)
	{
        super(m, p);
		field =new BitFlags(size);
	}

    public PriorityShapeBitFlagsImpl(final long m, final int p, final BitFlags bitFlags)
    {
        super(m, p);
        field =bitFlags;
    }
    
    public int hashCode()
    {
        return HashCodeUtils.hashMore(super.hashCode(), field.hashCode());
    }
    
    public boolean equals(Object other)
    {
        if (!super.equals(other))
            return false;
        final PriorityShapeBitFlagsImpl priorityShape =(PriorityShapeBitFlagsImpl)other;
        if (!this.field.equals(priorityShape.field))
            return false;
        return true;
    }
    
    public boolean isSelected(final int pieceNumber)
    {
        return field.flags[pieceNumber];
    }

	public int getStart()
	{
		return field.start;
	}

	public int getEnd()
	{
		return field.end;
	}
    
    /**
     * @return a reference to the shape's BitFlags selection criteria
     */ 
    public BitFlags getBitFlags()
    {
        return field;
    }

    /**
     * Sets the shape's selection criteria BitFlags to
     * a reference to the paramater BitFlags
     * @param bitFlags the BitFlags to set the selection criteria to.
     * Passing a null BitFlags isn't recommended.
     */
    public void setBitFlags(final BitFlags bitFlags)
    {
        field =bitFlags;
    }
    
    
    
    public boolean isNoRandom(final int pieceNumber)
    {
        return (mode &PRIORITY_MODE_NO_RANDOM) ==PRIORITY_MODE_NO_RANDOM
            &&field.flags[pieceNumber];
    }
    
    public boolean isNoRandom(final int start, final int end)
    {
        return (mode &PRIORITY_MODE_NO_RANDOM) ==PRIORITY_MODE_NO_RANDOM
            &&field.start <=start &&field.end >=end;
    }
    
    
    public boolean isIgnoreRarity(final int pieceNumber)
    {
        return (mode &PRIORITY_MODE_IGNORE_RARITY) ==PRIORITY_MODE_IGNORE_RARITY
            &&field.flags[pieceNumber];
    }
    
    public boolean isIgnoreRarity(final int start, final int end)
    {
        return (mode &PRIORITY_MODE_IGNORE_RARITY) ==PRIORITY_MODE_IGNORE_RARITY
            &&field.start <=start &&field.end >=end;
    }
    
    
    public boolean isFullPieces(final int pieceNumber)
    {
        return (mode &PRIORITY_MODE_FULL_PIECES) ==PRIORITY_MODE_FULL_PIECES
            &&field.flags[pieceNumber];
    }
    
    public boolean isFullPieces(final int start, final int end)
    {
        return (mode &PRIORITY_MODE_FULL_PIECES) ==PRIORITY_MODE_FULL_PIECES
            &&field.start <=start &&field.end >=end;
    }
    
    
    public boolean isAutoReserve(final int pieceNumber)
    {
        return (mode &PRIORITY_MODE_AUTO_RESERVE) ==PRIORITY_MODE_AUTO_RESERVE
            &&field.flags[pieceNumber];
    }
    
    public boolean isAutoReserve(final int start, final int end)
    {
        return (mode &PRIORITY_MODE_AUTO_RESERVE) ==PRIORITY_MODE_AUTO_RESERVE
            &&field.start <=start &&field.end >=end;
    }
    
    
    public boolean isReverse(final int pieceNumber)
    {
        return (mode &PRIORITY_MODE_REVERSE_ORDER) ==PRIORITY_MODE_REVERSE_ORDER
            &&field.flags[pieceNumber];
    }
    
    public boolean isReverse(final int start, final int end)
    {
        return (mode &PRIORITY_MODE_REVERSE_ORDER) ==PRIORITY_MODE_REVERSE_ORDER
            &&field.start <=start &&field.end >=end;
    }
    
    
    public boolean isAutoSlide(final int pieceNumber)
    {
        return (mode &PRIORITY_MODE_AUTO_SLIDE) ==PRIORITY_MODE_AUTO_SLIDE
            &&field.flags[pieceNumber];
    }
    
    public boolean isAutoSlide(final int start, final int end)
    {
        return (mode &PRIORITY_MODE_AUTO_SLIDE) ==PRIORITY_MODE_AUTO_SLIDE
            &&field.start <=start &&field.end >=end;
    }
    
    
    public boolean isRamp(final int pieceNumber)
    {
        return (mode &PRIORITY_MODE_RAMP) ==PRIORITY_MODE_RAMP
            &&field.flags[pieceNumber];
    }
    
    public boolean isRamp(final int start, final int end)
    {
        return (mode &PRIORITY_MODE_RAMP) ==PRIORITY_MODE_RAMP
            &&field.start <=start &&field.end >=end;
    }
    
    
    public boolean isStaticPriority(final int pieceNumber)
    {
        return (mode &PRIORITY_MODE_STATIC_PRIORITY) ==PRIORITY_MODE_STATIC_PRIORITY
            &&field.flags[pieceNumber];
    }
    
    public boolean isStaticPriority(final int start, final int end)
    {
        return (mode &PRIORITY_MODE_STATIC_PRIORITY) ==PRIORITY_MODE_STATIC_PRIORITY
            &&field.start <=start &&field.end >=end;
    }
    
    
}
