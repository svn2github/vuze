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
import java.util.*;

import org.gudy.azureus2.core3.security.SEKeyDetails;
import org.gudy.azureus2.core3.security.SESecurityManager;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.logging.*;

public class 
WUJarBuilder 
{	
	public static long
	buildFromPackages(
		JarOutputStream		jos,
		ClassLoader			class_loader,
		String[]			package_names,
		Map					package_map,
		String				sign_alias )
		
			throws IOException
	{
		List	resource_names = new ArrayList();
		
		for (int i=0;i<package_names.length;i++){
			
			List	entries = (List)package_map.get(package_names[i]);
			
			for (int j=0;j<entries.size();j++){
				
				resource_names.add( package_names[i] + "/" + entries.get(j));
			}
		}
		
		String[]	res = new String[resource_names.size()];
		
		resource_names.toArray( res );
		
		return( buildFromResources2( jos, class_loader, null, res, sign_alias ));	
	}
	
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
	
	public static void
	buildFromResources(
		JarOutputStream		jos,
		ClassLoader			class_loader,
		String				resource_prefix,
		String[]			resource_names,
		String				sign_alias )
		
			throws IOException
	{
		buildFromResources2( jos, class_loader, resource_prefix, resource_names, sign_alias );	
	}
	
	public static long
	buildFromResources2(
		JarOutputStream		jos,
		ClassLoader			class_loader,
		String[]			resource_names )
	
		throws IOException
	{
		return( buildFromResources2( jos, class_loader, null, resource_names, null ));
	}
	
	public static long
	buildFromResources2(
		JarOutputStream		jos,
		ClassLoader			class_loader,
		String				resource_prefix,
		String[]			resource_names )
	
		throws IOException
	{
		return( buildFromResources2( jos, class_loader, resource_prefix, resource_names, null ));
	}
	
	public static long
	buildFromResources2(
		JarOutputStream		jos,
		ClassLoader			class_loader,
		String				resource_prefix,
		String[]			resource_names,
		String				sign_alias )
		
		throws IOException
	{
		if ( sign_alias != null ){
			
			ByteArrayOutputStream	baos = new ByteArrayOutputStream(65536);
			
			long tim = buildFromResourcesSupport( new JarOutputStream( baos ),class_loader,resource_prefix,resource_names );
						
			try{
				SEKeyDetails	kd = SESecurityManager.getKeyDetails( sign_alias );
			
				if ( kd == null ){
			
					LGLogger.logAlert(
						LGLogger.AT_ERROR,
						"Certificate alias '" + sign_alias + "' not found, jar signing fails" );
					
					throw( new Exception( "Certificate alias '" + sign_alias + "' not found "));
				}
				
				WUJarSigner signer = new WUJarSigner(sign_alias, (PrivateKey)kd.getKey(), kd.getCertificateChain());

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

