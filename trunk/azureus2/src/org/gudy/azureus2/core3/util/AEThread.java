/*
 * Created on 28-Jun-2004
 * Created by Paul Gardner
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

/**
 * @author parg
 *
 */

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.gudy.azureus2.core3.logging.*;

public class 
AEThread 
	extends Thread
{
	protected static boolean	LOG_TO_FILE	= true;

	protected static File		log_file;
	
	public
	AEThread(
		String	name )
	{
		super(name);
		
		LGLogger.log( "Thread:created '" + name + "'" );
		
		if ( LOG_TO_FILE ){
			
			synchronized( AEThread.class ){
				
				if ( log_file == null ){
										
					log_file = new File( System.getProperty("user.dir" ) + File.separator + "thread.log" );
				}
				
				PrintWriter pw = null;
				
				try{
					
					pw = new PrintWriter( new FileWriter( log_file, true ));
					
					String ts = new SimpleDateFormat("hh:mm:ss - ").format( new Date());

					pw.println( ts + ": created '" + name + "'" );
					
				}catch( Throwable e ){
					
				}finally{
					if ( pw != null ){
						try{
							pw.close();
						}catch( Throwable e ){
						}
					}
				}
			}
		}
	}
}
