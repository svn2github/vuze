/*
 * Created on Jun 14, 2005
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

package org.gudy.azureus2.core3.util;

import sun.misc.Signal;
import sun.misc.SignalHandler;



/**
 * Catches non-user-initiated VM shutdown events.
 * Ref: http://www-106.ibm.com/developerworks/ibm/library/i-signalhandling
 *      http://www.smotricz.com/kabutz/Issue043.html 
 *      http://www.geeksville.com/~kevinh/projects/javasignals/
 */
public class ShutdownHook {

  /**
   * Register a handler to be called upon VM shutdown/termination, i.e. SIGINT or SIGTERM signal events.
   * @param handler
   */
  public static void install( final Handler handler) {
    try{
      final SignalHandler[] old_handlers = new SignalHandler[2];
    
      //ctrl-c signal
      old_handlers[0] = Signal.handle( new Signal( "INT" ), new SignalHandler() {
        public void handle( Signal sig ) {
          try{
            handler.shutdown( sig.getName() );  //main ShutdownHook.Handler
            
            if( old_handlers[0] != null && old_handlers[0] != SIG_DFL && old_handlers[0] != SIG_IGN ) {  //chain back to previous handler if one exists
              old_handlers[0].handle( sig );
            }
          }
          catch( Throwable t ) {
            Debug.out( t );
          }
        }
      });
      
      //os termination signal
      old_handlers[1] = Signal.handle( new Signal( "TERM" ), new SignalHandler() {
        public void handle( Signal sig ) {
          try{
            handler.shutdown( sig.getName() );  //main ShutdownHook.Handler
            
            if( old_handlers[1] != null && old_handlers[1] != SIG_DFL && old_handlers[1] != SIG_IGN ) {  //chain back to previous handler if one exists
              old_handlers[1].handle( sig );
            }
          }
          catch( Throwable t ) {
            Debug.out( t );
          }
        }
      });
    }
    catch( Throwable t ) {
      Debug.out( t );
    }
    
  }
  


  public interface Handler {
    /**
     * VM shutdown event initiated.
     * @param signal_name source
     */
    public void shutdown( String signal_name );  
  }

  
}
         
