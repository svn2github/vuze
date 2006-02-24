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

package com.aelitis.azureus.core.peermanager.piecepicker.priority;

import com.aelitis.azureus.core.peermanager.piecepicker.priority.impl.*;
import com.aelitis.azureus.core.peermanager.piecepicker.util.BitFlags;


/**
 * @author MjrTom Jan 17, 2006
 */
public class PriorityShapeFactory
{
    /**
     * Creates a basic range selection criteria based shape
     * @param mode the mode effect of this shape
     * @param priority the priority associated with this shape
     * @return PriorityShapeRangedImpl created (hint; put it into the shaper)
     */
	public static PriorityShapeRangedImpl
	create(final long mode, final int priority, final int start, final int end)
	{
		return new PriorityShapeRangedImpl(mode, priority, start, end);
	}
	
    
    /**
     * Creates a ready to go bit flag selection criteria based shape
     * @param mode the mode effect of this shape
     * @param priority the priority associated with this shape 
     * @param bitFlags your pre-made BitFlags object (which is NOT cloned
     * into the shape; a reference is used)
     * @return PriorityShapeBitFlagsImpl created (hint; put it in the shaper)
     */
    public static PriorityShapeBitFlagsImpl
    create(final long mode, final int priority, final BitFlags bitFlags)
    {
        return new PriorityShapeBitFlagsImpl(mode, priority, bitFlags);
    }
    
	/**
     * Creates a basic bit flag selection criteria based shape 
     * @param mode the mode effect of this shape
     * @param priority the priority associated with this shape
     * @param size the size of the required BitFlags
     * @return PriorityShapeBitFlagsImpl with empty BitFlags
     * (Hint; set the BitFlags as desired then give it to the shaper)
	 */
    public static PriorityShapeBitFlagsImpl
	create(final long mode, final int priority, final int size)
	{
		return new PriorityShapeBitFlagsImpl(mode, priority, size);
	}

}
