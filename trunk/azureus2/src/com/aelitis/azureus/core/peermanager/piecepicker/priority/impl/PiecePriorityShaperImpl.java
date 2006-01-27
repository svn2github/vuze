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

package com.aelitis.azureus.core.peermanager.piecepicker.priority.impl;

import java.util.*;

import org.gudy.azureus2.core3.peer.impl.PEPeerTransport;
import org.gudy.azureus2.core3.util.AEMonitor;

import com.aelitis.azureus.core.peermanager.piecepicker.priority.*;

/**
 * @author MjrTom Jan 17, 2006
 */
public class PiecePriorityShaperImpl
	implements PiecePriorityShaper
{
	protected List			torrentPriorityShapes =new ArrayList();
	protected static final	AEMonitor torrentPriorityShapesMon = new AEMonitor("torrentPriorityShapes");

	protected Map			peersShapesMap =new HashMap();
	protected static final	AEMonitor peersShapesMapMon = new AEMonitor("peersShapesMap");

	public void addPriorityShape(final PriorityShape shape)
	{
		try
		{	torrentPriorityShapesMon.enter();
			torrentPriorityShapes.add(shape);
		} finally { torrentPriorityShapesMon.exit(); }
	}

	public void addPriorityShape(final PriorityShape shape, final PEPeerTransport pt)
	{
		ArrayList ptPriorityShapes =getPeerPriorityShapes(pt);
		if (ptPriorityShapes ==null)
			return;
		ptPriorityShapes.add(shape);
		peersShapesMap.put(pt, ptPriorityShapes);
	}


	public boolean removePriorityShape(final PriorityShape shape)
	{
		return false;
	}

	public boolean removePriorityShape(final PriorityShape shape, final PEPeerTransport pt)
	{
		return false;
	}


	/** possibly this should never be used since the PEPeerTransport version must be called when the peer is know
	 * and it probably should always be known whenever this might be called
	 */
	protected int getMode(int start, int end)
	{
		if (torrentPriorityShapes ==null)
			return 0;
		int mode =0;
		// get the aggregate mode from the torrent-global shapes
		try
		{	torrentPriorityShapesMon.enter();
			for (int i =0; i <=torrentPriorityShapes.size(); i++)
			{
				final PriorityShape shape =(PriorityShape)torrentPriorityShapes.get(i);
				// does the shape cover the range we're interested in
				if (shape.getStart() <=start &&shape.getEnd() >=end)
					mode |=shape.getMode();
			}
		} finally { torrentPriorityShapesMon.exit(); }
		
		return mode;
	}
	
	public int getMode(int start, int end, final PEPeerTransport pt)
	{
		int mode =getMode(start, end);
		
		// aggregate the peer's shapes' modes
		List peerPriorityShapes =getPeerPriorityShapes(pt);
		for (int i =0; i <=peerPriorityShapes.size(); i++)
		{
			final PriorityShape shape =(PriorityShape)peerPriorityShapes.get(i);
			// does the shape cover the range we're interested in
			if (shape.getStart() <=start &&shape.getEnd() >=end)
				mode |=shape.getMode();
		}
		
		return mode;
	}
	

	protected ArrayList getPeerPriorityShapes(final PEPeerTransport pt)
	{
		final ArrayList ptPriorityShapes;
		try
		{	peersShapesMapMon.enter();
			if (!peersShapesMap.containsKey(pt))
			{
				ptPriorityShapes =new ArrayList();
				peersShapesMap.put(pt, ptPriorityShapes);
			} else
				ptPriorityShapes =(ArrayList)peersShapesMap.get(pt);
			return ptPriorityShapes;
		} finally {peersShapesMapMon.exit();}
	}
}
