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

import java.util.*;

import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.util.AEMonitor;

import com.aelitis.azureus.core.peermanager.piecepicker.priority.*;

/**
 * @author MjrTom Jan 17, 2006
 */
public class PiecePriorityShaperImpl
	implements PiecePriorityShaper
{
	protected List			torrentPriorityShapes;
	protected static final	AEMonitor torrentPriorityShapesMon =new AEMonitor("torrentPriorityShapes");
	
	protected Map			peersShapesMap;
	protected static final	AEMonitor peersShapesMapMon =new AEMonitor("peersShapesMap");
	
    
	/**
     * @param shape the PriorityShape to globally add to the shaper.
     * Do not pass a null shape. 
	 */
    public void addPriorityShape(final PriorityShape shape)
	{
        try
        {   torrentPriorityShapesMon.enter();
            if (torrentPriorityShapes ==null)
                torrentPriorityShapes =new ArrayList();
			torrentPriorityShapes.add(shape);
		} finally { torrentPriorityShapesMon.exit(); }
	}

    /**
     * @param shape the PriorityShape to add to the shaper for the given peer.
     * Do not pass a null shape or null peer. 
     */
	public void addPriorityShape(final PriorityShape shape, final PEPeer peer)
	{
        try
        {   peersShapesMapMon.enter();
		    List ptPriorityShapes =getPeerPriorityShapes(peer);
		    if (ptPriorityShapes ==null)
		        ptPriorityShapes =new ArrayList();
		    ptPriorityShapes.add(shape);
		    peersShapesMap.put(peer, ptPriorityShapes);
        } finally {peersShapesMapMon.exit();}
	}
    
    /**
     * @param peer the PEPeer to get the list of shapes for, if any
     * @return null if no shapes else a List of Shapes
     */
    protected List getPeerPriorityShapes(final PEPeer peer)
    {
        if (peersShapesMap ==null)
            return null;
        if (peersShapesMap.containsKey(peer))
            return (List)peersShapesMap.get(peer);
        return null;
    }
    
    // TODO: implement
    public boolean removePriorityShape(final PriorityShape shape)
	{
		return false;
	}

    // TODO: implement
	public boolean removePriorityShape(final PriorityShape shape, final PEPeer peer)
	{
		return false;
	}


	/** possibly this should never be used since the PEPeer version must be called when the peer is known
	 * and it probably should always be known whenever this might be called
	 */
	public long getMode(final int start, final int end)
	{
		if (torrentPriorityShapes ==null)
			return 0;
		long mode =0;
		// get the aggregate mode from the torrent-global shapes
		try
		{	torrentPriorityShapesMon.enter();
			for (int i =0; i <torrentPriorityShapes.size(); i++)
			{
				final PriorityShapeImpl shape =(PriorityShapeImpl)torrentPriorityShapes.get(i);
				// does the shape cover the range of interest
				if (shape.getStart() <=start &&shape.getEnd() >=end)
					mode |=shape.mode;
			}
		} finally { torrentPriorityShapesMon.exit(); }
		
		return mode;
	}
	
	public long getMode(final int start, final int end, final PEPeer peer)
	{
	    long mode =getMode(start, end);

	    try
	    {   peersShapesMapMon.enter();
    	    // aggregate the peer's shapes' modes
    	    List peerPriorityShapes =getPeerPriorityShapes(peer);
    	    if (peerPriorityShapes ==null)
    	        return mode;
    
    	    for (int i =0; i <peerPriorityShapes.size(); i++)
    	    {
    	        final PriorityShapeImpl shape =(PriorityShapeImpl)peerPriorityShapes.get(i);
    	        // does the shape cover the range of interest
    	        if (shape.getStart() <=start &&shape.getEnd() >=end)
    	            mode |=shape.mode;
    	    }
	    } finally {peersShapesMapMon.exit();}

	    return mode;
	}
	
    public boolean isNoRandom(final int start, final int end)
    {
        if (torrentPriorityShapes ==null)
            return false;
        // get the aggregate mode from the torrent-global shapes
        try
        {   torrentPriorityShapesMon.enter();
            for (int i =0; i <torrentPriorityShapes.size(); i++)
            {
                final PriorityShape shape =(PriorityShape)torrentPriorityShapes.get(i);
                // does the shape cover the range of interest and specify the mode in question
                if (shape.getStart() <=start &&shape.getEnd() >=end &&shape.isNoRandom())
                    return true;
            }
        } finally { torrentPriorityShapesMon.exit(); }
        
        return false;
    }
    
    public boolean isNoRandom(final int start, final int end, final PEPeer peer)
    {   // if it's globally true, it's true for this peer
        if (isNoRandom(start, end))
            return true;

        try
        {   peersShapesMapMon.enter();
            // check any/all per-peer shapes
            final List peerPriorityShapes =getPeerPriorityShapes(peer);
            if (peerPriorityShapes ==null)
                return false;
            for (int i =0; i <peerPriorityShapes.size(); i++)
            {
                final PriorityShape shape =(PriorityShape)peerPriorityShapes.get(i);
                // does the shape cover the range of interest and specify the mode in question
                if (shape.getStart() <=start &&shape.getEnd() >=end &&shape.isNoRandom())
                    return true;
            }
        } finally {peersShapesMapMon.exit();}
        
        return false;
    }
    
}
