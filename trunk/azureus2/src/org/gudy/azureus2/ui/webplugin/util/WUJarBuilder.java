/*
 * File    : WUJarBuilder.java
 * Created : 10-Feb-2004
 * By      : parg
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.ui.webplugin.util;

/**
 * @author parg
 *
 */

import java.io.*;
import java.util.jar.*;

import org.gudy.azureus2.core3.util.*;

public class 
WUJarBuilder 
{
	public static void
	buildFromResources(
		JarOutputStream		jos,
		ClassLoader			class_loader,
		String[]			resource_names )
	
		throws IOException
	{
		buildFromResources( jos, class_loader, null, resource_names );
	}
	
	public static void
	buildFromResources(
		JarOutputStream		jos,
		ClassLoader			class_loader,
		String				resource_prefix,
		String[]			resource_names )
	
		throws IOException
	{
		for (int i=0;i<resource_names.length;i++){
	
			String	resource_name = resource_names[i];
	
			if ( resource_prefix != null ){
				
				resource_name = resource_prefix + "/" + resource_name;
			}
			
			InputStream	is = null;
			
			try{
				is	= class_loader.getResourceAsStream(resource_name);
			
				if ( is == null ){
				
					Debug.out( "WUJarBuilder: failed to find resource '" + resource_name + "'");
	
				}else{
					
					JarEntry entry = new JarEntry(resource_name);
			
					writeEntry( jos, entry, is );
				}
			}finally{
				if ( is != null ){
					
					is.close();
				}
			}
		}
		
		JarEntry entry = new JarEntry("META-INF/MANIFEST.MF");
		
		ByteArrayInputStream bais = new ByteArrayInputStream("Manifest-Version: 1.0\r\n\r\n".getBytes());
		
		writeEntry( jos, entry, bais );
		
		jos.flush();
	}
	
	

	private static void 
	writeEntry(
		JarOutputStream 	jos, 
		JarEntry 			entry,
		InputStream 		data ) 
	
		throws IOException 
	{
		jos.putNextEntry(entry);

		byte[]	newBytes = new byte[4096];
		
		int size = data.read(newBytes);

		while (size != -1){
			
			jos.write(newBytes, 0, size);
			
			size = data.read(newBytes);
		}
	}
}

