/*
 * Created on Apr 30, 2004
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

package com.aelitis.azureus.core.peermanager.messages;

import org.gudy.azureus2.core3.util.DirectByteBuffer;

/**
 * A peer protocol message.
 */
public interface ProtocolMessage {
  
  public static final int PRIORITY_LOW    = 0;
  public static final int PRIORITY_NORMAL = 1;
  public static final int PRIORITY_HIGH   = 2;
  public static final int PRIORITY_URGENT = 3;
  
  
  /**
   * Get the message's type.
   * @return type
   */
  public int getType();
  
  /**
   * Get the message's data payload.
   * @return data payload
   */
  public DirectByteBuffer getPayload();
  
  /**
   * Get the total size (in bytes) of the message, i.e. headers + data.
   * @return total size in bytes
   */
  public int getTotalMessageByteSize();
  
  /**
   * Get the message's textual description.
   * @return description
   */
  public String getDescription();
  
  /**
   * Get the message's queue priority.
   * @return priority
   */
  public int getPriority();
  
  /**
   * Destroy the message; i.e. perform cleanup actions.
   */
  public void destroy();
  
  /**
   * Get the types of yet-unsent messages that should be removed
   * before queueing this message for sending.
   * @return message types; null if no types
   */
  public int[] typesToRemove();
  
  
}
