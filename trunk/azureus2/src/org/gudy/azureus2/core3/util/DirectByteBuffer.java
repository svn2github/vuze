/*
 * Created on Apr 21, 2004
 * Created by Alon Rohter
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
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

package org.gudy.azureus2.core3.util;

import java.lang.ref.Reference;
import java.nio.ByteBuffer;

/**
 * Virtual direct byte buffer given out and tracker
 * by the buffer pool.
 */
public class DirectByteBuffer {
  public final ByteBuffer buff;
  protected Reference ref;
  
  public DirectByteBuffer( ByteBuffer buffer ) {
    this.buff = buffer;
  }
  
  public void returnToPool() {
    if ( ref != null ) {
      DirectByteBufferPool.registerReturn( ref );
      ref.enqueue();
    }
  }
}
