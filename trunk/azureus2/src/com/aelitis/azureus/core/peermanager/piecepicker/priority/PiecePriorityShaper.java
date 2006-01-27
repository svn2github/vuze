/*
 * Created by Joseph Bridgewater
 * Created on Jan 17, 2006
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

package com.aelitis.azureus.core.peermanager.piecepicker.priority;

import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;

/**
 * @author MjrTom Jan 17, 2006
 * This defines an interface for adjusting the PiecePicker
 * such as adding and removing priority ranges
 * THIS IS EXTREMELLY EARLY AND RAW CODE - DON'T COUNT ON ANYTHING
*/
public interface PiecePriorityShaper
{
	public void addPriorityShape(final PriorityShape shape);
	public void addPriorityShape(final PriorityShape shape, final PEPeerTransport pt);
	/**
	 * Removes a priority shape that was set with addPriorityShape
	 * @param shape the shape reference to remove
	 * @return true if the shape was removed, false if not
	 */
	public boolean removePriorityShape(final PriorityShape shape);
	
	public int getMode(final int start, final int end, final PEPeerTransport pt);
}
