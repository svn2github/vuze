 /*
 * Created on Oct 10, 2003
 * Modified Apr 14, 2004 by Alon Rohter
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 * 
 */

package org.gudy.azureus2.core3.util;

import java.io.*;
import java.util.*;
import java.nio.channels.*;

import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFactory;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.logging.*;

/**
 * File utility class.
 */
public class FileUtil {
  
  public static final String DIR_SEP = System.getProperty("file.separator");
  
  private static final int	RESERVED_FILE_HANDLE_COUNT	= 4;
	
  private static List	reserved_file_handles = new ArrayList();
	

  public static String getCanonicalFileName(String filename) {
    // Sometimes Windows use filename in 8.3 form and cannot
    // match .torrent extension. To solve this, canonical path
    // is used to get back the long form

    String canonicalFileName = filename;
    try {
      canonicalFileName = new File(filename).getCanonicalPath();
    }
    catch (IOException ignore) {}
    return canonicalFileName;
  }

  
  public static File getUserFile(String filename) {
    return new File(SystemProperties.getUserPath(), filename);
  }
  
  public static File getApplicationFile(String filename) {
      
    String path = SystemProperties.getApplicationPath();
      
      if(Constants.isOSX) {
        path = path + "/Azureus.app/Contents/";
      }
      
      return new File(path, filename);
  }
  
  
  
  public static boolean isTorrentFile(String filename) throws FileNotFoundException, IOException {
    File check = new File(filename);
    if (!check.exists())
      throw new FileNotFoundException("File "+filename+" not found.");
    if (!check.canRead())
      throw new IOException("File "+filename+" cannot be read.");
    if (check.isDirectory())
      throw new FileIsADirectoryException("File "+filename+" is a directory.");
    try {
      TOTorrentFactory.deserialiseFromBEncodedFile(check);
      return true;
    } catch (TOTorrentException e) {
      return false;
    }
  }

  
  /**
   * Deletes the given dir and all files/dirs underneath
   */
  public static void recursiveDelete(File f) {
    String defSaveDir = COConfigurationManager.getStringParameter("Default save path", "");
    String moveToDir = COConfigurationManager.getStringParameter("Completed Files Directory", "");
    
    try {
      if (f.getCanonicalPath().equals(moveToDir)) {
        System.out.println("FileUtil::recursiveDelete:: not allowed to delete the MoveTo dir !");
        return;
      }
      if (f.getCanonicalPath().equals(defSaveDir)) {
        System.out.println("FileUtil::recursiveDelete:: not allowed to delete the default data dir !");
        return;
      }
      
      if (f.isDirectory()) {
        File[] files = f.listFiles();
        for (int i = 0; i < files.length; i++) {
          recursiveDelete(files[i]);
        }
        f.delete();
      }
      else {
        f.delete();
      }
    } catch (Exception ignore) {/*ignore*/}
  }
  
  
  /**
   * Deletes the given dir and all dirs underneath if empty.
   * Don't delete default save path or completed files directory, however,
   * allow deletion of their empty subdirectories
   */
  public static void recursiveEmptyDirDelete(File f) {
    String defSaveDir = COConfigurationManager.getStringParameter("Default save path", "");
    String moveToDir = COConfigurationManager.getStringParameter("Completed Files Directory", "");
    
    try {
      if (f.isDirectory()) {
        File[] files = f.listFiles();
        for (int i = 0; i < files.length; i++) {
          recursiveEmptyDirDelete(files[i]);
        }

        if (f.getCanonicalPath().equals(moveToDir)) {
          System.out.println("FileUtil::recursiveEmptyDirDelete:: not allowed to delete the MoveTo dir !");
          return;
        }
        if (f.getCanonicalPath().equals(defSaveDir)) {
          System.out.println("FileUtil::recursiveEmptyDirDelete:: not allowed to delete the default data dir !");
          return;
        }

        if (f.listFiles().length == 0) {
          f.delete();
        } else {
          System.out.println("FileUtil::recursiveEmptyDirDelete:: "+f.listFiles().length+" file(s)/folder(s) still in " + f + ". Not removing.");
        }
      }

    } catch (Exception e) { Debug.out(e.toString()); }
  }
  
  
  /**
   * Takes a path and a file/dir name and returns the full path,
   * including file/dir name, removing 'name' duplicate if already
   * represented by 'path'
   * 
   * Example: smartFullName("c:\windows\thedir", "thedir") -> "c:\windows\thedir"
   * Example: smartFullName("c:\windows", "thedir") -> "c:\windows\thedir"
   * Example: smartFullName("c:\windows", "filename.txt") -> "c:\windows\filename.txt"
   * Example: smartFullName("c:\windows\filename.txt", "filename.txt") -> "c:\windows\filename.txt"
   */
  public static String smartFullName(String path, String name) {
    //if 'name' is already represented by 'path'
    if (path.endsWith(name)) return path;
    else return path + System.getProperty("file.separator") + name;
  }
  
  
   /**
   * Takes a path and a file/dir name and returns the full dir path,
   * removing 'name' dir duplicate if already represented by 'path'
   * 
   * Example: smartPath("c:\windows\thedir", "thedir") -> "c:\windows\thedir"
   * Example: smartPath("c:\windows", "thedir") -> "c:\windows\thedir"
   * Example: smartPath("c:\windows\thedir", "filename.txt") -> "c:\windows\thedir"
   */
  public static String smartPath(String path, String name) {
    String fullPath = path + System.getProperty("file.separator") + name;
    //if 'name' is already represented by 'path'
    if (path.endsWith(name)) {
      File dirTest = new File(path);
      if (dirTest.isDirectory()) return path;
      else return dirTest.getParent();
    }
    //otherwise use path + name
    else {
      File dirTest = new File (fullPath);
      if (dirTest.isDirectory()) return fullPath;
      else return path;
    }
  }

  public static String
  convertOSSpecificChars(
  	String	file_name_in )
  {
  	String	file_name_out	= file_name_in;
	
  	boolean mac = System.getProperty("os.name").equals("Mac OS X");
  	
  	if ( !mac ){
  		
  			// '/' is valid in mac file names, replace with space
  			// so it seems are cr/lf
  		
  		file_name_out = file_name_out.replace('/',' ');
  		file_name_out = file_name_out.replace('\r',' ');
  		file_name_out = file_name_out.replace('\n',' ');
  	}

	try{
		
			// mac file names can end in space - fix this up by getting
			// the canonical form which removes this on Windows
		
		String str = new File(file_name_out).getCanonicalFile().toString();
	
		int	p = str.lastIndexOf( File.separator );
		
		file_name_out = str.substring(p+1);
		
	}catch( Throwable e ){
		// ho hum, carry on, it'll fail later
		//e.printStackTrace();
	}
	
	//System.out.println( "convertOSSpecificChars: " + file_name_in + " ->" + file_name_out );
	
	return( file_name_out );
  }
  
  public static void
  writeResilientConfigFile(
  	String		file_name,
	Map			data )
  {
  	try{
  		getReservedFileHandles();
      File temp = new File( SystemProperties.getUserPath() + file_name + ".saving");
	    BufferedOutputStream	baos = null;
	    
	    try{
	    	byte[] encoded_data = BEncoder.encode(data);
	    	baos = new BufferedOutputStream( new FileOutputStream( temp, false ), 8192 );
	    	baos.write( encoded_data );
	    	baos.flush();
        baos.close();
        baos = null;
           
        //only use newly saved file if it got this far, i.e. it saved successfully
        if ( temp.length() > 1L ) {
        	File file = new File( SystemProperties.getUserPath() + file_name );
        	if ( file.exists() ){
        		file.delete();
        	}
        	temp.renameTo( file );
        }

	    }catch (Exception e) {
	    
	    	LGLogger.logAlert( "Save of '" + file_name + "' fails", e );
	    	
	    }finally{
	    	
	    	try {
	    		if (baos != null){
	    			
	    			baos.close();
	    		}
	    	}catch( Exception e){
	    		
	        	LGLogger.logAlert( "Save of '" + file_name + "' fails", e ); 
	    	}
	    }
  	}finally{
  		
  		releaseReservedFileHandles();
  	}
  }
  
  	public static Map
	readResilientConfigFile(
		String		file_name )
	{
  		try{
  			getReservedFileHandles();
  	
  			return(readResilientConfigFile( file_name, 0 ));
  			
  		}finally{
  			
  			releaseReservedFileHandles();
  		}
  	}
  	
	private static Map
	readResilientConfigFile(
		String		file_name,
		int			fail_count )
	{	  
  			// open the file
  	
  		boolean	using_backup	= file_name.endsWith(".saving");
  		
  		File file = new File( SystemProperties.getUserPath() + file_name );
	    
	   		//make sure the file exists and isn't zero-length
	    
  		if ( (!file.exists()) || file.length() <= 1L ){

  			if ( using_backup ){
	     
  				if ( fail_count == 1 ){
  					
  						// we only alert the user if at least one file was found and failed
  						// otherwise it could be start of day when neither file exists yet
  					
  					LGLogger.logAlert( 	LGLogger.AT_ERROR,
  										"Load of '" + file_name + "' fails, no usable file or backup" );
  				}else{
  					
  					LGLogger.log( 	LGLogger.INFORMATION,
									"Load of '" + file_name + "' fails, file not found" );
				
  				}
	       
  				return( new HashMap());
  			}
        
        LGLogger.log("Load of '" + file_name + "' failed, file not found or 0-sized." );
  			
  			return( readResilientConfigFile( file_name + ".saving", 0 ));
  		}

  		BufferedInputStream bin = null;
   
  		try{
  			bin = new BufferedInputStream( new FileInputStream(file), 8192 );
	      
	    	Map	res = BDecoder.decode(bin);
	    	
	    	if ( using_backup ){
  		
	    		LGLogger.logAlert( 
	    					LGLogger.AT_WARNING,
							"Load of '" + file_name.substring(0,file_name.length()-4) + "' had to revert to backup file" ); 
	    	}
	    	
	    	return( res );
	    	
	    }catch( Throwable e ){
        e.printStackTrace();
	    	// Occurs when file is there but b0rked
        
        //rename it in case it actually contains useful data, so it won't be overwritten next save
        LGLogger.log("Read of '" + file_name + "' failed, b-decoding error. Renaming to *.bad" );      
        File bad = new File( file.getParentFile(), file.getName() + ".bad" );
        try {  if (bin != null) bin.close();  } catch (Exception x) { x.printStackTrace(); }
        file.renameTo( bad );
	    	
	    	if ( using_backup ){
		
	    		LGLogger.logAlert( LGLogger.AT_ERROR,
							"Load of '" + file_name + "' fails, no usable file or backup" ); 
	    		
	    		return( new HashMap());
	    	}
	    	
	    	return( readResilientConfigFile( file_name + ".saving", 1 ));
 			 
	    }finally{
	    	
	    	try {
	    		
	    		if (bin != null){
	    			
	    			bin.close();
	    		}
	    	}catch (Exception e) {
	    		
	    		e.printStackTrace();
	    	}
	    }
	}
  

	private static synchronized void
	getReservedFileHandles()
	{
		while( reserved_file_handles.size() > 0 ){
			
			// System.out.println( "releasing reserved file handle");
			
			InputStream	is = (InputStream)reserved_file_handles.remove(0);
			
			try{
				is.close();
				
			}catch( Throwable e ){
				
				e.printStackTrace();
			}
		}
	}
			
	private static synchronized void
	releaseReservedFileHandles()
	{
		try{
			File	lock_file	= new File(SystemProperties.getUserPath() + ".lock");
							
			lock_file.createNewFile();
		
			while(  reserved_file_handles.size() < RESERVED_FILE_HANDLE_COUNT ){
				
				// System.out.println( "getting reserved file handle");
				
				InputStream	is = new FileInputStream( lock_file );
				
				reserved_file_handles.add(is);
			}
		}catch( Throwable e ){
		
			e.printStackTrace();
		}
	}
	
    /**
     * Backup the given file to filename.bak, removing the old .bak file if necessary.
     * If _make_copy is true, the original file will copied to backup, rather than moved.
     * @param _filename name of file to backup
     * @param _make_copy copy instead of move
     */
    public static void backupFile( final String _filename, final boolean _make_copy ) {
      backupFile( new File( _filename ), _make_copy );
    }
      
    /**
     * Backup the given file to filename.bak, removing the old .bak file if necessary.
     * If _make_copy is true, the original file will copied to backup, rather than moved.
     * @param _file file to backup
     * @param _make_copy copy instead of move
     */
    public static void backupFile( final File _file, final boolean _make_copy ) {
      if ( _file.length() > 0L ) {
        File bakfile = new File( _file.getAbsolutePath() + ".bak" );
        if ( bakfile.exists() ) bakfile.delete();
        if ( _make_copy ) {
          copyFile( _file, bakfile );
        }
        else {
          _file.renameTo( bakfile );
        }
      }
    }
    
    
    /**
     * Copy the given source file to the given destination file.
     * Returns file copy success or not.
     * @param _source_name source file name
     * @param _dest_name destination file name
     * @return true if file copy successful, false if copy failed
     */
    public static boolean copyFile( final String _source_name, final String _dest_name ) {
      return copyFile( new File(_source_name), new File(_dest_name));
    }
    
    /**
     * Copy the given source file to the given destination file.
     * Returns file copy success or not.
     * @param _source source file
     * @param _dest destination file
     * @return true if file copy successful, false if copy failed
     */
    /*
    // FileChannel.transferTo() seems to fail under certain linux configurations.
    public static boolean copyFile( final File _source, final File _dest ) {
      FileChannel source = null;
      FileChannel dest = null;
      try {
        if( _source.length() < 1L ) {
          throw new IOException( _source.getAbsolutePath() + " does not exist or is 0-sized" );
        }
        source = new FileInputStream( _source ).getChannel();
        dest = new FileOutputStream( _dest ).getChannel();
      
        source.transferTo(0, source.size(), dest);
        return true;
      }
      catch (Exception e) {
        Debug.out( e );
        return false;
      }
      finally {
        try {
          if (source != null) source.close();
          if (dest != null) dest.close();
        }
        catch (Exception ignore) {}
      }
    }
    */
    
    public static boolean copyFile( final File _source, final File _dest ) {
      try {
        copyFile( new FileInputStream( _source ), new FileOutputStream( _dest ) );
        return true;
      }
      catch( Throwable t ) {
        t.printStackTrace();
        return false;
      }
    }
    
    
    public static void 
    copyFile( 
      InputStream   is,
      OutputStream  os ) 
    throws IOException {
      copyFile(is,os,true);
    }
    
    public static void 
	copyFile( 
		InputStream		is,
		OutputStream	os,
    boolean closeInputStream)
	
		throws IOException
	{
    	try{
    		
    		if ( !(is instanceof BufferedInputStream )){
    			
    			is = new BufferedInputStream(is);
    		}
    		
    		byte[]	buffer = new byte[65536];
			
    		while(true){
    			
    			int	len = is.read(buffer);
    			
    			if ( len == -1 ){
    				
    				break;
    			}
    			
    			os.write( buffer, 0, len );
    		}
    	}finally{
    		try{
        if(closeInputStream)
    			  is.close();
    		}catch( IOException e ){
    			
    		}
    		
    		os.close();
    	}
	}
    
    
    /**
     * Returns the file handle for the given filename or it's
     * equivalent .bak backup file if the original doesn't exist
     * or is 0-sized.  If neither the original nor the backup are
     * available, a null handle is returned.
     * @param _filename root name of file
     * @return file if successful, null if failed
     */
    public static File getFileOrBackup( final String _filename ) {
      try {
        File file = new File( _filename );
        //make sure the file exists and isn't zero-length
        if ( file.length() <= 1L ) {
          //if so, try using the backup file
          File bakfile = new File( _filename + ".bak" );
          if ( bakfile.length() <= 1L ) {
            return null;
          }
          else return bakfile;
        }
        else return file;
      }
      catch (Exception e) {
        Debug.out( e );
        return null;
      }
    }

    
  

}
