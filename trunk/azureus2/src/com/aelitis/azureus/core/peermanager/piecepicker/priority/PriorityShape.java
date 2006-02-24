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



/**
 * @author MjrTom Jan 17, 2006
 */
public interface PriorityShape
{
    /** random selection will not occur */
	public static final long PRIORITY_MODE_NO_RANDOM		=0x00000001;
    /** priority boosts for rarity will not be applied */
	public static final long PRIORITY_MODE_IGNORE_RARITY	=0x00000002;
    /** requests of full pieces are prefered over blocks requests */
    public static final long PRIORITY_MODE_FULL_PIECES      =0x00000004;
    /** when a request is made, the piece will automatically be reserved to the peer
     * if neither the piece is already reserved nor the peer has a piece reserved to it already */
    public static final long PRIORITY_MODE_AUTO_RESERVE     =0x00000008;
    /** inverse ordering (ie end to front and/or falling ramp) */
	public static final long PRIORITY_MODE_REVERSE_ORDER	=0x00000010;
    /** I don't know if this can be practically implemented */
    public static final long PRIORITY_MODE_AUTO_SLIDE       =0x00000020;
    /** priority adjustment ramps (otherwise flat) */
    public static final long PRIORITY_MODE_RAMP             =0x00000040;
    /** base (start) priority is not further modified */
    public static final long PRIORITY_MODE_STATIC_PRIORITY  =0x00000080;
    
    /** how many bits used in modes defined above (used in hash calculation) */
    public static final long PRIORITY_MODES_BITS            =8;
    
    /** returns the first piece number in the selection criteria */
	public int getStart();
    /** returns the last piece number in the selection criteria */
	public int getEnd();
	public long getMode();
	public int getPriority();
    
    /** @param pieceNumber int piece number to check for selection
     * @return true if this piece is selected by the shape's selection criteria,
     * or false if not selected
     */
    public boolean isSelected(final int pieceNumber);
    
    
    public boolean isNoRandom();
    public boolean isReverse();
    
}
