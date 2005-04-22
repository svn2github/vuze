/*
 * Created on Apr 21, 2005
 * Created by Alon Rohter
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

package com.aelitis.azureus.core.networkmanager.impl;


/**
 * Interface designation for rate-limited entities handled by a read controller.
 */
public interface RateControlledReadEntity {

    /**
     * Uses fair round-robin scheduling of read ops.
     */
    public static final int PRIORITY_NORMAL = 0;
    
    /**
     * Guaranteed scheduling of read ops, with preference over normal-priority entities.
     */
    public static final int PRIORITY_HIGH   = 1;
    
    /**
     * Is ready for a read op.
     * @return true if it can read >0 bytes, false if not ready
     */
    public boolean canRead();
    
    /**
     * Attempt to do a read operation.
     * @return true if >0 bytes were read (success), false if 0 bytes were read (failure)
     */
    public boolean doRead();
    
    /**
     * Get this entity's priority level.
     * @return priority
     */
    public int getPriority();
}
