/*
 * Created on 16 juin 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.gudy.azureus2.core3.util;

/**
 *  
 * @author Olivier
 *
 */

public class 
Constants 
{
  public static final String SF_WEB_SITE		= "http://azureus.sourceforge.net/";
  public static final String AELITIS_WEB_SITE    = "http://azureus.aelitis.com/";
  
  public static final String DEFAULT_ENCODING 	= "UTF8";
  public static final String BYTE_ENCODING 		= "ISO-8859-1";
  
  public static final String INFINITY_STRING	= "\u221E"; // "oo";
  public static final int    INFINITY_AS_INT = 31536000; // seconds (365days)
  
  	// keep the CVS style constant coz version checkers depend on it!
  	// e.g. 2.0.8.3
    //      2.0.8.3_CVS
    //      2.0.8.3_Bnn       // incremental build
  
  public static final String AZUREUS_NAME	    = "Azureus";
  public static final String AZUREUS_VERSION  = "2.1.0.5_CVS";  //2.1.0.5_CVS
  public static final byte[] VERSION_ID       = ("-" + "AZ" + "2105" + "-").getBytes();  //MUST be 8 chars long!
  
  
  public static final String  OSName = System.getProperty("os.name");
  
  public static final boolean isOSX				= OSName.equalsIgnoreCase("Mac OS X");
  public static final boolean isLinux			= OSName.equalsIgnoreCase("Linux");
  public static final boolean isWindowsXP		= OSName.equalsIgnoreCase("Windows XP");
  public static final boolean isWindows95		= OSName.equalsIgnoreCase("Windows 95");
  public static final boolean isWindows98		= OSName.equalsIgnoreCase("Windows 98");
  public static final boolean isWindowsME		= OSName.equalsIgnoreCase("Windows ME");
  public static final boolean isWindows9598ME	= isWindows95 || isWindows98 || isWindowsME;
  
  public static final boolean isWindows	= !(isOSX || isLinux); 
 
  
  	/**
  	 * Gets the current version, or if a CVS version, the one on which it is based 
  	 * @return
  	 */
  
  public static String
  getBaseVersion()
  {
  	return( getBaseVersion( AZUREUS_VERSION ));
  }
  
  public static String
  getBaseVersion(
  	String	version )
  {
  	int	p1 = version.indexOf("_");	// _CVS or _Bnn
  	
  	if ( p1 == -1 ){
  		
  		return( version );
  	}
  	
  	return( version.substring(0,p1));
  }
  
  	/**
  	 * is this a formal build or CVS/incremental 
  	 * @return
  	 */
  
  public static boolean
  isCVSVersion()
  {
  	return( isCVSVersion( AZUREUS_VERSION )); 
  }
  
  public static boolean
  isCVSVersion(
  	String	version )
  {
  	return( version.indexOf("_") != -1 );  
  }
  
  	/**
  	 * For CVS builds this returns the incremental build number. For people running their own
  	 * builds this returns -1 
  	 * @return
  	 */
  
  public static int
  getIncrementalBuild()
  {
  	return( getIncrementalBuild( AZUREUS_VERSION ));
  }
  
  public static int
  getIncrementalBuild(
  	String	version )
  {
  	if ( !isCVSVersion(version)){
  		
  		return( 0 );
  	}
  	
  	int	p1 = version.indexOf( "_B" );
  	
  	if ( p1 == -1 ){
  		
  		return( -1 );
  	}
  	
  	try{
  		return( Integer.parseInt( version.substring(p1+2)));
  		
  	}catch( Throwable e ){
  		
  		System.out.println("can't parse version");
  		
  		return( -1 );
  	}
  }
  
		/**
		 * compare two version strings of form n.n.n.n (e.g. 1.2.3.4)
		 * @param version_1	
		 * @param version_2
		 * @return -ve -> version_1 lower, 0 = same, +ve -> version_1 higher
		 */
	
	public static int
	compareVersions(
		String		version_1,
		String		version_2 )
	{		
		for (int j=0;j<Math.min(version_2.length(), version_1.length());j++){
			
			char	v1_c	= version_1.charAt(j);
			char	v2_c	= version_2.charAt(j);
			
			if ( v1_c == v2_c ){
				
				continue;
			}
			
			if ( v2_c == '.' ){
				
					// version1 higher (e.g. 10.2 -vs- 1.2)
				
				return( +1 );
				
			}else if ( v1_c == '.' ){
				
					// version2 higher ( e.g. 1.2 -vs- 10.2 )
				
				return( -1 );
								
			}else{
				
				return( v1_c - v2_c );
			}
		}
		
			// longest one wins. e.g. 1.2.1 -vs- 1.2
		
		return( version_1.length() - version_2.length());
	}
}
