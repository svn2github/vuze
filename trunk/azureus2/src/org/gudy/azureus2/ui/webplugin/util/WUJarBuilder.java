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
import java.net.URL;
import java.net.URI;
import java.security.PrivateKey;
import java.util.jar.*;

import org.gudy.azureus2.core3.security.SEKeyDetails;
import org.gudy.azureus2.core3.security.SESecurityManager;
import org.gudy.azureus2.core3.util.*;

public class 
WUJarBuilder 
{
	public static final boolean SIGN_JAR	= false;
	
	public static void
	buildFromResources(
		JarOutputStream		jos,
		ClassLoader			class_loader,
		String[]			resource_names )
	
		throws IOException
	{
		buildFromResources2( jos, class_loader, null, resource_names );
	}
	
	public static void
	buildFromResources(
		JarOutputStream		jos,
		ClassLoader			class_loader,
		String				resource_prefix,
		String[]			resource_names )
	
		throws IOException
	{
		buildFromResources2( jos, class_loader, resource_prefix, resource_names );	
	}
	
	public static long
	buildFromResources2(
		JarOutputStream		jos,
		ClassLoader			class_loader,
		String[]			resource_names )
	
		throws IOException
	{
		return( buildFromResources2( jos, class_loader, null, resource_names ));
	}
	
	public static long
	buildFromResources2(
		JarOutputStream		jos,
		ClassLoader			class_loader,
		String				resource_prefix,
		String[]			resource_names )
	
		throws IOException
	{
		if ( SIGN_JAR ){
			
			ByteArrayOutputStream	baos = new ByteArrayOutputStream(65536);
			
			long tim = buildFromResourcesSupport( new JarOutputStream( baos ),class_loader,resource_prefix,resource_names );
			
			String	alias = "SomeAlias"; // SESecurityManager.DEFAULT_ALIAS;
			
			try{
				SEKeyDetails	kd = SESecurityManager.getKeyDetails( alias );
			
				WUJarSigner signer = new WUJarSigner(alias, (PrivateKey)kd.getKey(), kd.getCertificateChain());

				signer.signJarStream( new ByteArrayInputStream(baos.toByteArray()), jos );
			
				return( tim );
				
			}catch( Throwable e ){
				
				e.printStackTrace();
				
				throw( new IOException( e.getMessage()));
			}
			
		}else{
			
			return( buildFromResourcesSupport( jos,class_loader,resource_prefix,resource_names ));
		}
	}
	
	public static long
	buildFromResourcesSupport(
		JarOutputStream		jos,
		ClassLoader			class_loader,
		String				resource_prefix,
		String[]			resource_names )
	
		throws IOException
	{		
		long	latest_time	= 0;
		long	now			= SystemTime.getCurrentTime();
		
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
				
					URL	url = class_loader.getResource( resource_name );
					
					try{
						File	file = null;
						
						if ( url != null ){
							
							String	url_str = url.toString();
							
							if ( url_str.startsWith("jar:file:" )){
								
								file	= FileUtil.getJarFileFromURL( url_str );
								
							}else if ( url_str.startsWith( "file:")){
								
								file	= new File( URI.create( url_str ));
							}
						}
						
						if ( file == null ){
							
							latest_time	= now;
							
						}else{
						
							long	time = file.lastModified();
							
							if ( time > latest_time ){
								
								latest_time	= time;
							}
						}
					}catch( Throwable e ){
						
						e.printStackTrace();
					}
					
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
		
		jos.finish();
		
		return( latest_time );
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

