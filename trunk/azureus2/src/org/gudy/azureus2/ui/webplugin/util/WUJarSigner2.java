/*
 * Created on 04-Oct-2004
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

package org.gudy.azureus2.ui.webplugin.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.*;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.jar.*;
// import sun.security.tools.JarSigner;
import sun.security.tools.JarSigner;

import org.gudy.azureus2.core3.util.Debug;

/**
 * @author parg
 *
 */
public class 
WUJarSigner2 
{
	protected String		keystore_name;
	protected String		keystore_password;
	protected String		alias;
	
	public
	WUJarSigner2(
		String		_alias,
		String		_keystore_name,
		String		_keystore_password )
	{
		alias				= _alias;
		keystore_name		= _keystore_name;
		keystore_password	= _keystore_password;
	}
	
	protected void
	signJarFile(
		File		input_file )
	{
		JarSigner	js = new JarSigner();
		
		String[]	args = {	"-keystore",
								keystore_name,
								"-storepass",
								keystore_password,
								input_file.toString(),
								alias };
		
		js.run(args);
	}
	
	public void
	signJarFile(
		File			file,
		OutputStream	os )
	
		throws 	NoSuchAlgorithmException, InvalidKeyException, CertificateException, 
				SignatureException, IOException
	{
		signJarFile( file );
	}
	
	public void
	signJarStream(
		InputStream		is,
		OutputStream	os )
		
		throws 	NoSuchAlgorithmException, InvalidKeyException, CertificateException, 
				SignatureException, IOException
	{
		File	temp_file = File.createTempFile("AZU", null );
		
		FileOutputStream	fos = null;
		
		try{
			
			byte[]	buffer = new byte[8192];
		
			fos = new FileOutputStream( temp_file );
			
			while(true){
				
				int	len = is.read( buffer );
				
				if ( len <= 0 ){
					
					break;
				}
				
				fos.write( buffer, 0, len );
			}
			
			fos.close();
			
			fos	= null;
			
			signJarFile( temp_file, os );
			
		}finally{
			
			try{
				is.close();
				
			}catch( Throwable e ){
				
				Debug.printStackTrace( e );
			}
			
			if ( fos != null ){
				
				try{
					fos.close();
					
				}catch( Throwable e ){
					
					Debug.printStackTrace( e );
				}
			}
			temp_file.delete();
			
		}
	}
}
