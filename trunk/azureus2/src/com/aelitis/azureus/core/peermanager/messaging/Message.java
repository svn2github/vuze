/*
 * Created on Jan 8, 2005
 * Created by Alon Rohter
 * Copyright (C) 2004-2005 Aelitis, All Rights Reserved.
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

package com.aelitis.azureus.core.peermanager.messaging;

import org.gudy.azureus2.core3.util.DirectByteBuffer;



/**
 * Basic peer message.
 * A message type is uniquely identified by the combination of ID and version.
 */
public interface Message {

  /**
   * Get message type id.
   * @return id
   */
  public String getID();
  
  /**
   * Get message type version.
   * @return version
   */
  public byte getVersion();
    
  /**
   * Get textual description of this particular message.
   * @return description
   */
  public String getDescription();
  
  /**
   * Get message payload data.
   * @return message data buffers
   */
  public DirectByteBuffer[] getData();
}
