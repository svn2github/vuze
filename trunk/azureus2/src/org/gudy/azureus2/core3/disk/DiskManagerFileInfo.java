/*
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
 *
 * Created on 3 juil. 2003
 *
 */
package org.gudy.azureus2.core3.disk;

import java.io.File;

import org.gudy.azureus2.core3.disk.DiskManager;

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
	
	public DiskManager getDiskManager();

	public File getFile();
	
	public void
	flushCache()
	
		throws	Exception;
}
