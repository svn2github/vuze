/*
 * Created on 3 juil. 2003
 *
 */
package org.gudy.azureus2.core3.disk;

/**
 * @author Olivier
 * 
 */
public interface 
DiskManagerFileInfo 
{
	public static final int READ = 1;
	public static final int WRITE = 2;

		// set methods
		
	public void setPriority(boolean b);
	
	public void setSkipped(boolean b);
	 
	 	// get methods
	 	
	public int getAccessMode();
	
	public long getDownloaded();
	
	public String getExtension();
		
	public int getFirstPieceNumber();
	
	public long getLength();
	
	public String getName();
	
	public int getNbPieces();
		
	public String getPath();
	
	public boolean isPriority();
	
	public boolean isSkipped();
}
