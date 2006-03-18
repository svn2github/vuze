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
    protected ArrayList     torrentPriorityShapes;
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
    
    
    /**
     * @param shape the PriorityShape to remove.  This need not be the same shape object that was
     * inserted into the shaper; a congruent shape is sufficient. If there were somehow multiple
     * congruent shapes in the list, only the last congruent shape is removed.  Do not pass a null shape.
     * @return true if there was a list of shapes, and a shape congruent to the paramater
     * shape was in the list, and the shape was removed else returns false
     */
    public boolean removePriorityShape(final PriorityShape shape)
	{
        if (torrentPriorityShapes ==null ||torrentPriorityShapes.isEmpty())
            return false;
        try
        {   torrentPriorityShapesMon.enter();
            for (int i =torrentPriorityShapes.size() -1; i >=0; i--)
            {
                final PriorityShape listShape =(PriorityShape)torrentPriorityShapes.get(i);
                if (shape.equals(listShape))
                {
                    torrentPriorityShapes.remove(i);
                    return true;
                }
            }
        } finally { torrentPriorityShapesMon.exit(); }
		return false;
	}

    /**
     * @param shape the PriorityShape to remove from the shapes against the given peer.
     * This need not be the same shape object that was inserted into the shaper, but can
     * instead be a congruent shape.  Do not pass a null shape or null peer.  Only the list
     * of shapes for the given peer is checked; the list of torrent shapes is not affected.
     * @return true if there was a list of shapes for the peer, and a shape congruent to
     * the paramater shape was in the list, and the shape was removed else returns false
     */
	public boolean removePriorityShape(final PriorityShape shape, final PEPeer peer)
	{
        try
        {   peersShapesMapMon.enter();
            // check any/all per-peer shapes
            final List peerPriorityShapes =getPeerPriorityShapes(peer);
            if (peerPriorityShapes ==null ||peerPriorityShapes.isEmpty())
                return false;
            
            for (int i =peerPriorityShapes.size() -1; i >=0; i--)
            {
                final PriorityShape listShape =(PriorityShape)peerPriorityShapes.get(i);
                if (shape.equals(listShape))
                {
                    peerPriorityShapes.remove(i);
                    return true;
                }
            }
        } finally {peersShapesMapMon.exit();}
		return false;
	}
	
	
	public int getPriority(int pieceNumber)
	{
        if (torrentPriorityShapes ==null ||torrentPriorityShapes.isEmpty())
            return 0;
        int priority =0;
        // get the aggregate priority from the torrent-global shapes
        try
        {   torrentPriorityShapesMon.enter();
        	final int shapesSize =torrentPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShapeImpl shape =(PriorityShapeImpl)torrentPriorityShapes.get(i);
                // does the shape cover the piece of interest
                priority +=shape.getPriority(pieceNumber);
            }
        } finally { torrentPriorityShapesMon.exit(); }
        
        return priority;
	}
	
	public int getPriority(final int pieceNumber, final PEPeer peer)
	{
		int priority =getPriority(pieceNumber);
		
        try
        {   peersShapesMapMon.enter();
            // aggregate the peer's shapes' modes
            final List peerPriorityShapes =getPeerPriorityShapes(peer);
            if (peerPriorityShapes ==null ||peerPriorityShapes.isEmpty())
                return priority;
            
            final int shapesSize =peerPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShapeImpl shape =(PriorityShapeImpl)peerPriorityShapes.get(i);
                // does the shape cover the piece of interest
                priority +=shape.getPriority(pieceNumber);
            }
        } finally {peersShapesMapMon.exit();}

        return priority;
	}
	
	
	/** possibly this should never be used since the PEPeer version must be called when the
	 * peer is known and it probably should always be known whenever this might be called
	 */
    public long getMode(final int pieceNumber)
    {
        if (torrentPriorityShapes ==null ||torrentPriorityShapes.isEmpty())
            return 0;
        long mode =0;
        // get the aggregate mode from the torrent-global shapes
        try
        {   torrentPriorityShapesMon.enter();
        	final int shapesSize =torrentPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShapeImpl shape =(PriorityShapeImpl)torrentPriorityShapes.get(i);
                // does the shape cover the piece of interest
                if (shape.isSelected(pieceNumber))
                    mode |=shape.mode;
            }
        } finally { torrentPriorityShapesMon.exit(); }
        
        return mode;
    }
    
    /** possibly this should never be used since the PEPeer version must be called when the
     * peer is known and it probably should always be known whenever this might be called
     */
	public long getMode(final int start, final int end)
	{
        if (torrentPriorityShapes ==null ||torrentPriorityShapes.isEmpty())
			return 0;
		long mode =0;
		// get the aggregate mode from the torrent-global shapes
		try
		{	torrentPriorityShapesMon.enter();
    		final int shapesSize =torrentPriorityShapes.size();
			for (int i =0; i <shapesSize; i++)
			{
				final PriorityShapeImpl shape =(PriorityShapeImpl)torrentPriorityShapes.get(i);
				// does the shape cover the range of interest
				if (shape.getStart() <=start &&shape.getEnd() >=end)
					mode |=shape.mode;
			}
		} finally { torrentPriorityShapesMon.exit(); }
		
		return mode;
	}
	
    public long getMode(final int pieceNumber, final PEPeer peer)
    {
        long mode =getMode(pieceNumber);

        try
        {   peersShapesMapMon.enter();
            // aggregate the peer's shapes' modes
            final List peerPriorityShapes =getPeerPriorityShapes(peer);
            if (peerPriorityShapes ==null ||peerPriorityShapes.isEmpty())
                return mode;
    
            final int shapesSize =peerPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShapeImpl shape =(PriorityShapeImpl)peerPriorityShapes.get(i);
                // does the shape cover the piece of interest
                if (shape.isSelected(pieceNumber))
                    mode |=shape.mode;
            }
        } finally {peersShapesMapMon.exit();}

        return mode;
    }
    
	public long getMode(final int start, final int end, final PEPeer peer)
	{
	    long mode =getMode(start, end);

	    try
	    {   peersShapesMapMon.enter();
    	    // aggregate the peer's shapes' modes
    	    final List peerPriorityShapes =getPeerPriorityShapes(peer);
            if (peerPriorityShapes ==null ||peerPriorityShapes.isEmpty())
    	        return mode;
    
            final int shapesSize =peerPriorityShapes.size();
    	    for (int i =0; i <shapesSize; i++)
    	    {
    	        final PriorityShapeImpl shape =(PriorityShapeImpl)peerPriorityShapes.get(i);
    	        // does the shape cover the range of interest
    	        if (shape.getStart() <=start &&shape.getEnd() >=end)
    	            mode |=shape.mode;
    	    }
	    } finally {peersShapesMapMon.exit();}

	    return mode;
	}
	
    
    
    public boolean isNoRandom(final int pieceNumber)
    {
        if (torrentPriorityShapes ==null ||torrentPriorityShapes.isEmpty())
            return false;
        // get the aggregate mode from the torrent-global shapes
        try
        {   torrentPriorityShapesMon.enter();
        	final int shapesSize =torrentPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShape shape =(PriorityShape)torrentPriorityShapes.get(i);
                // does the shape specify the mode in question and does the selection criteria cover the piece
                if (shape.isNoRandom(pieceNumber))
                    return true;
            }
        } finally { torrentPriorityShapesMon.exit(); }
        
        return false;
    }
    
    public boolean isNoRandom(final int start, final int end)
    {
        if (torrentPriorityShapes ==null ||torrentPriorityShapes.isEmpty())
            return false;
        // get the aggregate mode from the torrent-global shapes
        try
        {   torrentPriorityShapesMon.enter();
			final int shapesSize =torrentPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShape shape =(PriorityShape)torrentPriorityShapes.get(i);
                // does the shape cover the range of interest and specify the mode in question
                if (shape.isNoRandom(start, end))
                    return true;
            }
        } finally { torrentPriorityShapesMon.exit(); }
        
        return false;
    }
    
    public boolean isNoRandom(final int pieceNumber, final PEPeer peer)
    {   // if it's globally true, it's true for this peer
        if (isNoRandom(pieceNumber))
            return true;

        try
        {   peersShapesMapMon.enter();
            // check any/all per-peer shapes
            final List peerPriorityShapes =getPeerPriorityShapes(peer);
            if (peerPriorityShapes ==null ||peerPriorityShapes.isEmpty())
                return false;
            
            final int shapesSize =peerPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShape shape =(PriorityShape)peerPriorityShapes.get(i);
                // does the shape specify the mode in question and does the selection criteria cover the piece
                if (shape.isNoRandom(pieceNumber))
                    return true;
            }
        } finally {peersShapesMapMon.exit();}
        
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
            if (peerPriorityShapes ==null ||peerPriorityShapes.isEmpty())
                return false;
            
			final int shapesSize =peerPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShape shape =(PriorityShape)peerPriorityShapes.get(i);
                // does the shape cover the range of interest and specify the mode in question
                if (shape.isNoRandom(start, end))
                    return true;
            }
        } finally {peersShapesMapMon.exit();}
        
        return false;
    }
    
    
    public boolean isIgnoreRarity(final int pieceNumber)
    {
        if (torrentPriorityShapes ==null ||torrentPriorityShapes.isEmpty())
            return false;
        // get the aggregate mode from the torrent-global shapes
        try
        {   torrentPriorityShapesMon.enter();
			final int shapesSize =torrentPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShape shape =(PriorityShape)torrentPriorityShapes.get(i);
                // does the shape specify the mode in question and does the selection criteria cover the piece
                if (shape.isIgnoreRarity(pieceNumber))
                    return true;
            }
        } finally { torrentPriorityShapesMon.exit(); }
        
        return false;
    }
    
    public boolean isIgnoreRarity(final int start, final int end)
    {
        if (torrentPriorityShapes ==null ||torrentPriorityShapes.isEmpty())
            return false;
        // get the aggregate mode from the torrent-global shapes
        try
        {   torrentPriorityShapesMon.enter();
			final int shapesSize =torrentPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShape shape =(PriorityShape)torrentPriorityShapes.get(i);
                // does the shape cover the range of interest and specify the mode in question
                if (shape.isIgnoreRarity(start, end))
                    return true;
            }
        } finally { torrentPriorityShapesMon.exit(); }
        
        return false;
    }
    
    public boolean isIgnoreRarity(final int pieceNumber, final PEPeer peer)
    {   // if it's globally true, it's true for this peer
        if (isIgnoreRarity(pieceNumber))
            return true;

        try
        {   peersShapesMapMon.enter();
            // check any/all per-peer shapes
            final List peerPriorityShapes =getPeerPriorityShapes(peer);
            if (peerPriorityShapes ==null ||peerPriorityShapes.isEmpty())
                return false;
            
            final int shapesSize =peerPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShape shape =(PriorityShape)peerPriorityShapes.get(i);
                // does the shape specify the mode in question and does the selection criteria cover the piece
                if (shape.isIgnoreRarity(pieceNumber))
                    return true;
            }
        } finally {peersShapesMapMon.exit();}
        
        return false;
    }

    public boolean isIgnoreRarity(final int start, final int end, final PEPeer peer)
    {   // if it's globally true, it's true for this peer
        if (isIgnoreRarity(start, end))
            return true;

        try
        {   peersShapesMapMon.enter();
            // check any/all per-peer shapes
            final List peerPriorityShapes =getPeerPriorityShapes(peer);
            if (peerPriorityShapes ==null ||peerPriorityShapes.isEmpty())
                return false;
            
            final int shapesSize =peerPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShape shape =(PriorityShape)peerPriorityShapes.get(i);
                // does the shape cover the range of interest and specify the mode in question
                if (shape.isIgnoreRarity(start, end))
                    return true;
            }
        } finally {peersShapesMapMon.exit();}
        
        return false;
    }
    
    
    public boolean isFullPieces(final int pieceNumber)
    {
        if (torrentPriorityShapes ==null ||torrentPriorityShapes.isEmpty())
            return false;
        // get the aggregate mode from the torrent-global shapes
        try
        {   torrentPriorityShapesMon.enter();
			final int shapesSize =torrentPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShape shape =(PriorityShape)torrentPriorityShapes.get(i);
                // does the shape specify the mode in question and does the selection criteria cover the piece
                if (shape.isFullPieces(pieceNumber))
                    return true;
            }
        } finally { torrentPriorityShapesMon.exit(); }
        
        return false;
    }
    
    public boolean isFullPieces(final int start, final int end)
    {
        if (torrentPriorityShapes ==null ||torrentPriorityShapes.isEmpty())
            return false;
        // get the aggregate mode from the torrent-global shapes
        try
        {   torrentPriorityShapesMon.enter();
			final int shapesSize =torrentPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShape shape =(PriorityShape)torrentPriorityShapes.get(i);
                // does the shape cover the range of interest and specify the mode in question
                if (shape.isFullPieces(start, end))
                    return true;
            }
        } finally { torrentPriorityShapesMon.exit(); }
        
        return false;
    }
    
    public boolean isFullPieces(final int pieceNumber, final PEPeer peer)
    {   // if it's globally true, it's true for this peer
        if (isFullPieces(pieceNumber))
            return true;

        try
        {   peersShapesMapMon.enter();
            // check any/all per-peer shapes
            final List peerPriorityShapes =getPeerPriorityShapes(peer);
            if (peerPriorityShapes ==null ||peerPriorityShapes.isEmpty())
                return false;
            
            final int shapesSize =peerPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShape shape =(PriorityShape)peerPriorityShapes.get(i);
                // does the shape specify the mode in question and does the selection criteria cover the piece
                if (shape.isFullPieces(pieceNumber))
                    return true;
            }
        } finally {peersShapesMapMon.exit();}
        
        return false;
    }

    public boolean isFullPieces(final int start, final int end, final PEPeer peer)
    {   // if it's globally true, it's true for this peer
        if (isFullPieces(start, end))
            return true;

        try
        {   peersShapesMapMon.enter();
            // check any/all per-peer shapes
            final List peerPriorityShapes =getPeerPriorityShapes(peer);
            if (peerPriorityShapes ==null ||peerPriorityShapes.isEmpty())
                return false;
            
            final int shapesSize =peerPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShape shape =(PriorityShape)peerPriorityShapes.get(i);
                // does the shape cover the range of interest and specify the mode in question
                if (shape.isFullPieces(start, end))
                    return true;
            }
        } finally {peersShapesMapMon.exit();}
        
        return false;
    }
    
    
    public boolean isAutoReserve(final int pieceNumber)
    {
        if (torrentPriorityShapes ==null ||torrentPriorityShapes.isEmpty())
            return false;
        // get the aggregate mode from the torrent-global shapes
        try
        {   torrentPriorityShapesMon.enter();
			final int shapesSize =torrentPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShape shape =(PriorityShape)torrentPriorityShapes.get(i);
                // does the shape specify the mode in question and does the selection criteria cover the piece
                if (shape.isAutoReserve(pieceNumber))
                    return true;
            }
        } finally { torrentPriorityShapesMon.exit(); }
        
        return false;
    }
    
    public boolean isAutoReserve(final int start, final int end)
    {
        if (torrentPriorityShapes ==null ||torrentPriorityShapes.isEmpty())
            return false;
        // get the aggregate mode from the torrent-global shapes
        try
        {   torrentPriorityShapesMon.enter();
			final int shapesSize =torrentPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShape shape =(PriorityShape)torrentPriorityShapes.get(i);
                // does the shape cover the range of interest and specify the mode in question
                if (shape.isAutoReserve(start, end))
                    return true;
            }
        } finally { torrentPriorityShapesMon.exit(); }
        
        return false;
    }
    
    public boolean isAutoReserve(final int pieceNumber, final PEPeer peer)
    {   // if it's globally true, it's true for this peer
        if (isAutoReserve(pieceNumber))
            return true;

        try
        {   peersShapesMapMon.enter();
            // check any/all per-peer shapes
            final List peerPriorityShapes =getPeerPriorityShapes(peer);
            if (peerPriorityShapes ==null ||peerPriorityShapes.isEmpty())
                return false;
            
            final int shapesSize =peerPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShape shape =(PriorityShape)peerPriorityShapes.get(i);
                // does the shape specify the mode in question and does the selection criteria cover the piece
                if (shape.isAutoReserve(pieceNumber))
                    return true;
            }
        } finally {peersShapesMapMon.exit();}
        
        return false;
    }

    public boolean isAutoReserve(final int start, final int end, final PEPeer peer)
    {   // if it's globally true, it's true for this peer
        if (isAutoReserve(start, end))
            return true;

        try
        {   peersShapesMapMon.enter();
            // check any/all per-peer shapes
            final List peerPriorityShapes =getPeerPriorityShapes(peer);
            if (peerPriorityShapes ==null ||peerPriorityShapes.isEmpty())
                return false;
            
            final int shapesSize =peerPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShape shape =(PriorityShape)peerPriorityShapes.get(i);
                // does the shape cover the range of interest and specify the mode in question
                if (shape.isAutoReserve(start, end))
                    return true;
            }
        } finally {peersShapesMapMon.exit();}
        
        return false;
    }
    
    
    public boolean isReverse(final int pieceNumber)
    {
        if (torrentPriorityShapes ==null ||torrentPriorityShapes.isEmpty())
            return false;
        // get the aggregate mode from the torrent-global shapes
        try
        {   torrentPriorityShapesMon.enter();
			final int shapesSize =torrentPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShape shape =(PriorityShape)torrentPriorityShapes.get(i);
                // does the shape specify the mode in question and does the selection criteria cover the piece
                if (shape.isReverse(pieceNumber))
                    return true;
            }
        } finally { torrentPriorityShapesMon.exit(); }
        
        return false;
    }
    
    public boolean isReverse(final int start, final int end)
    {
        if (torrentPriorityShapes ==null ||torrentPriorityShapes.isEmpty())
            return false;
        // get the aggregate mode from the torrent-global shapes
        try
        {   torrentPriorityShapesMon.enter();
			final int shapesSize =torrentPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShape shape =(PriorityShape)torrentPriorityShapes.get(i);
                // does the shape cover the range of interest and specify the mode in question
                if (shape.isReverse(start, end))
                    return true;
            }
        } finally { torrentPriorityShapesMon.exit(); }
        
        return false;
    }
    
    public boolean isReverse(final int pieceNumber, final PEPeer peer)
    {   // if it's globally true, it's true for this peer
        if (isReverse(pieceNumber))
            return true;

        try
        {   peersShapesMapMon.enter();
            // check any/all per-peer shapes
            final List peerPriorityShapes =getPeerPriorityShapes(peer);
            if (peerPriorityShapes ==null ||peerPriorityShapes.isEmpty())
                return false;
            
            final int shapesSize =peerPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShape shape =(PriorityShape)peerPriorityShapes.get(i);
                // does the shape specify the mode in question and does the selection criteria cover the piece
                if (shape.isReverse(pieceNumber))
                    return true;
            }
        } finally {peersShapesMapMon.exit();}
        
        return false;
    }

    public boolean isReverse(final int start, final int end, final PEPeer peer)
    {   // if it's globally true, it's true for this peer
        if (isReverse(start, end))
            return true;

        try
        {   peersShapesMapMon.enter();
            // check any/all per-peer shapes
            final List peerPriorityShapes =getPeerPriorityShapes(peer);
            if (peerPriorityShapes ==null ||peerPriorityShapes.isEmpty())
                return false;
            
            final int shapesSize =peerPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShape shape =(PriorityShape)peerPriorityShapes.get(i);
                // does the shape cover the range of interest and specify the mode in question
                if (shape.isReverse(start, end))
                    return true;
            }
        } finally {peersShapesMapMon.exit();}
        
        return false;
    }
    
    
    public boolean isAutoSlide(final int pieceNumber)
    {
        if (torrentPriorityShapes ==null ||torrentPriorityShapes.isEmpty())
            return false;
        // get the aggregate mode from the torrent-global shapes
        try
        {   torrentPriorityShapesMon.enter();
			final int shapesSize =torrentPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShape shape =(PriorityShape)torrentPriorityShapes.get(i);
                // does the shape specify the mode in question and does the selection criteria cover the piece
                if (shape.isAutoSlide(pieceNumber))
                    return true;
            }
        } finally { torrentPriorityShapesMon.exit(); }
        
        return false;
    }
    
    public boolean isAutoSlide(final int start, final int end)
    {
        if (torrentPriorityShapes ==null ||torrentPriorityShapes.isEmpty())
            return false;
        // get the aggregate mode from the torrent-global shapes
        try
        {   torrentPriorityShapesMon.enter();
			final int shapesSize =torrentPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShape shape =(PriorityShape)torrentPriorityShapes.get(i);
                // does the shape cover the range of interest and specify the mode in question
                if (shape.isAutoSlide(start, end))
                    return true;
            }
        } finally { torrentPriorityShapesMon.exit(); }
        
        return false;
    }
    
    public boolean isAutoSlide(final int pieceNumber, final PEPeer peer)
    {   // if it's globally true, it's true for this peer
        if (isAutoSlide(pieceNumber))
            return true;

        try
        {   peersShapesMapMon.enter();
            // check any/all per-peer shapes
            final List peerPriorityShapes =getPeerPriorityShapes(peer);
            if (peerPriorityShapes ==null ||peerPriorityShapes.isEmpty())
                return false;
            
            final int shapesSize =peerPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShape shape =(PriorityShape)peerPriorityShapes.get(i);
                // does the shape specify the mode in question and does the selection criteria cover the piece
                if (shape.isAutoSlide(pieceNumber))
                    return true;
            }
        } finally {peersShapesMapMon.exit();}
        
        return false;
    }

    public boolean isAutoSlide(final int start, final int end, final PEPeer peer)
    {   // if it's globally true, it's true for this peer
        if (isAutoSlide(start, end))
            return true;

        try
        {   peersShapesMapMon.enter();
            // check any/all per-peer shapes
            final List peerPriorityShapes =getPeerPriorityShapes(peer);
            if (peerPriorityShapes ==null ||peerPriorityShapes.isEmpty())
                return false;
            
            final int shapesSize =peerPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShape shape =(PriorityShape)peerPriorityShapes.get(i);
                // does the shape cover the range of interest and specify the mode in question
                if (shape.isAutoSlide(start, end))
                    return true;
            }
        } finally {peersShapesMapMon.exit();}
        
        return false;
    }
    
    
    public boolean isRamp(final int pieceNumber)
    {
        if (torrentPriorityShapes ==null ||torrentPriorityShapes.isEmpty())
            return false;
        // get the aggregate mode from the torrent-global shapes
        try
        {   torrentPriorityShapesMon.enter();
			final int shapesSize =torrentPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShape shape =(PriorityShape)torrentPriorityShapes.get(i);
                // does the shape specify the mode in question and does the selection criteria cover the piece
                if (shape.isRamp(pieceNumber))
                    return true;
            }
        } finally { torrentPriorityShapesMon.exit(); }
        
        return false;
    }
    
    public boolean isRamp(final int start, final int end)
    {
        if (torrentPriorityShapes ==null ||torrentPriorityShapes.isEmpty())
            return false;
        // get the aggregate mode from the torrent-global shapes
        try
        {   torrentPriorityShapesMon.enter();
			final int shapesSize =torrentPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShape shape =(PriorityShape)torrentPriorityShapes.get(i);
                // does the shape cover the range of interest and specify the mode in question
                if (shape.isRamp(start, end))
                    return true;
            }
        } finally { torrentPriorityShapesMon.exit(); }
        
        return false;
    }
    
    public boolean isRamp(final int pieceNumber, final PEPeer peer)
    {   // if it's globally true, it's true for this peer
        if (isRamp(pieceNumber))
            return true;

        try
        {   peersShapesMapMon.enter();
            // check any/all per-peer shapes
            final List peerPriorityShapes =getPeerPriorityShapes(peer);
            if (peerPriorityShapes ==null ||peerPriorityShapes.isEmpty())
                return false;
            
            final int shapesSize =peerPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShape shape =(PriorityShape)peerPriorityShapes.get(i);
                // does the shape specify the mode in question and does the selection criteria cover the piece
                if (shape.isRamp(pieceNumber))
                    return true;
            }
        } finally {peersShapesMapMon.exit();}
        
        return false;
    }

    public boolean isRamp(final int start, final int end, final PEPeer peer)
    {   // if it's globally true, it's true for this peer
        if (isRamp(start, end))
            return true;

        try
        {   peersShapesMapMon.enter();
            // check any/all per-peer shapes
            final List peerPriorityShapes =getPeerPriorityShapes(peer);
            if (peerPriorityShapes ==null ||peerPriorityShapes.isEmpty())
                return false;
            
            final int shapesSize =peerPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShape shape =(PriorityShape)peerPriorityShapes.get(i);
                // does the shape cover the range of interest and specify the mode in question
                if (shape.isRamp(start, end))
                    return true;
            }
        } finally {peersShapesMapMon.exit();}
        
        return false;
    }
    
    
    public boolean isStaticPriority(final int pieceNumber)
    {
        if (torrentPriorityShapes ==null ||torrentPriorityShapes.isEmpty())
            return false;
        // get the aggregate mode from the torrent-global shapes
        try
        {   torrentPriorityShapesMon.enter();
			final int shapesSize =torrentPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShape shape =(PriorityShape)torrentPriorityShapes.get(i);
                // does the shape specify the mode in question and does the selection criteria cover the piece
                if (shape.isStaticPriority(pieceNumber))
                    return true;
            }
        } finally { torrentPriorityShapesMon.exit(); }
        
        return false;
    }
    
    public boolean isStaticPriority(final int start, final int end)
    {
        if (torrentPriorityShapes ==null ||torrentPriorityShapes.isEmpty())
            return false;
        // get the aggregate mode from the torrent-global shapes
        try
        {   torrentPriorityShapesMon.enter();
			final int shapesSize =torrentPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShape shape =(PriorityShape)torrentPriorityShapes.get(i);
                // does the shape cover the range of interest and specify the mode in question
                if (shape.isStaticPriority(start, end))
                    return true;
            }
        } finally { torrentPriorityShapesMon.exit(); }
        
        return false;
    }
    
    public boolean isStaticPriority(final int pieceNumber, final PEPeer peer)
    {   // if it's globally true, it's true for this peer
        if (isStaticPriority(pieceNumber))
            return true;
        
        try
        {   peersShapesMapMon.enter();
            // check any/all per-peer shapes
            final List peerPriorityShapes =getPeerPriorityShapes(peer);
            if (peerPriorityShapes ==null ||peerPriorityShapes.isEmpty())
                return false;
            
            final int shapesSize =peerPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShape shape =(PriorityShape)peerPriorityShapes.get(i);
                // does the shape specify the mode in question and does the selection criteria cover the piece
                if (shape.isStaticPriority(pieceNumber))
                    return true;
            }
        } finally {peersShapesMapMon.exit();}
        
        return false;
    }

    public boolean isStaticPriority(final int start, final int end, final PEPeer peer)
    {   // if it's globally true, it's true for this peer
        if (isStaticPriority(start, end))
            return true;
        
        try
        {   peersShapesMapMon.enter();
            // check any/all per-peer shapes
            final List peerPriorityShapes =getPeerPriorityShapes(peer);
            if (peerPriorityShapes ==null ||peerPriorityShapes.isEmpty())
                return false;
            
            final int shapesSize =peerPriorityShapes.size();
            for (int i =0; i <shapesSize; i++)
            {
                final PriorityShape shape =(PriorityShape)peerPriorityShapes.get(i);
                // does the shape cover the range of interest and specify the mode in question
                if (shape.isStaticPriority(start, end))
                    return true;
            }
        } finally {peersShapesMapMon.exit();}
        
        return false;
    }
    
    
}
