/*
 * Created on 29-Oct-2004
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

import java.io.File;
import java.io.IOException;


/**
 * @author parg
 *
 */

public class 
AETemporaryFileHandler 
{
	private static boolean	started_up;
	private static File		tmp_dir;

	public static synchronized void
	startup()
	{
		if ( started_up ){
			
			return;
		}
		
		started_up	= true;
		
		try{
			tmp_dir		= FileUtil.getUserFile( "tmp" );
						
			if ( tmp_dir.exists()){
				
				File[]	files = tmp_dir.listFiles();
				
				if ( files != null ){
					
					boolean	file_moved	= false;
					
					long	now = SystemTime.getCurrentTime();
					
					for (int i=0;i<files.length;i++){
						
						File	file = files[i];
						
						if ( file.isDirectory()){
							
							continue;
						}
						
						file.delete();
					}
				}
			}else{
				
				tmp_dir.mkdir();
			}
		}catch( Throwable e ){
			
				// with webui we don't have the file stuff so this fails with class not found
			
			if ( !(e instanceof NoClassDefFoundError )){
				
				Debug.printStackTrace( e );
			}
		}
	}
	
	public static File 
	createTempFile(
		String prefix, 
		String suffix )
	
			throws IOException
	{
		startup();
		
		return( File.createTempFile( prefix, suffix, tmp_dir ));
	}
}
