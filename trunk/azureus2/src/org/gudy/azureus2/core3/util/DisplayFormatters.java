/*
 * File    : DisplayFormatters.java
 * Created : 07-Oct-2003
 * By      : gardnerpar
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
 
package org.gudy.azureus2.core3.util;

/**
 * @author gardnerpar
 *
 */

import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.internat.*;

public class 
DisplayFormatters 
{
	public static String formatByteCountToKBEtc(int n) {
	  if (n < 1024)
		return n + " B";
	  if (n < 1024 * 1024)
		return (n / 1024) + "." + ((n % 1024) / 103) + " KB";
	  if (n < 1024 * 1024 * 1024)
		return (n / (1024 * 1024))
		  + "."
		  + ((n % (1024 * 1024)) / (103 * 1024))
		  + " MB";
	  if (n < 1024 * 1024 * 1024 * 1024)
		return (n / (1024 * 1024 * 1024))
		  + "."
		  + ((n % (1024 * 1024 * 1024)) / (103 * 1024 * 1024))
		  + " GB";
	  return "A lot";
	}

	public static String formatByteCountToKBEtc(long n) {
	  if (n < 1024)
		return n + " B";
	  if (n < 1024 * 1024)
		return (n / 1024) + "." + ((n % 1024) / 103) + " KB";
	  if (n < 1024 * 1024 * 1024)
		return (n / (1024 * 1024))
		  + "."
		  + ((n % (1024 * 1024)) / (103 * 1024))
		  + " MB";
	  if (n < 1024l * 1024l * 1024l * 1024l)
		return (n / (1024l * 1024l * 1024l))
		  + "."
		  + ((n % (1024l * 1024l * 1024l)) / (103l * 1024l * 1024l))
		  + " GB";
	  return "A lot !!!";
	}
	
	public static String
	formatByteCountToKBEtcPerSec(
		long		n )
	{
		return( formatByteCountToKBEtc(n) + "/s");
	}
  
  
  
   public static String formatETA(long eta) {
     if (eta == 0) return MessageText.getString("PeerManager.status.finished");
     if (eta == -1) return "";
     if (eta > 0) return TimeFormater.format(eta);
     
     return MessageText.getString("PeerManager.status.finishedin")
            + " " + TimeFormater.format(eta * -1);
   }
  
	
	public static String
	formatDownloaded(
		DownloadManagerStats	stats )
	{
		long	total_discarded = stats.getDiscarded();
		long	total_received 	= stats.getDownloaded();
		
		if(total_discarded == 0){
		
			return formatByteCountToKBEtc(total_received);
			
		}else{
			
			return formatByteCountToKBEtc(total_received) + " ( " + DisplayFormatters.formatByteCountToKBEtc(total_discarded) + " " + MessageText.getString("discarded") + " )"; 
		}
	}
	
	public static String
	formatHashFails(
		DownloadManager		download_manager )
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		DiskManager		dm = download_manager.getDiskManager();
			
	  	if(pm != null){
	  	
			int nbFails = pm.getNbHashFails();
		
			long size = nbFails * dm.getPieceLength();
			
			String result = nbFails + " ( ~ " + formatByteCountToKBEtc(size) + " )";
			
			return result;
  		}
  		
  		return "";
	}	
	
	public static String
	formatDownloadStatus(
		DownloadManager		manager )
	{	
		int state = manager.getState();
		
		String	tmp = "";
		
		switch (state) {
		  case DownloadManager.STATE_WAITING :
			tmp = MessageText.getString("ManagerItem.waiting"); //$NON-NLS-1$
			break;
		  case DownloadManager.STATE_ALLOCATING :
			tmp = MessageText.getString("ManagerItem.allocating"); //$NON-NLS-1$
			break;
		  case DownloadManager.STATE_CHECKING :
			tmp = MessageText.getString("ManagerItem.checking"); //$NON-NLS-1$
			break;
		  case DownloadManager.STATE_FINISHING :
		    tmp = MessageText.getString("ManagerItem.finishing"); //$NON-NLS-1$
			 break;
		  case DownloadManager.STATE_READY :
			tmp = MessageText.getString("ManagerItem.ready"); //$NON-NLS-1$
			break;
		  case DownloadManager.STATE_DOWNLOADING :
			tmp = MessageText.getString("ManagerItem.downloading"); //$NON-NLS-1$
			break;
		  case DownloadManager.STATE_SEEDING :
			tmp = MessageText.getString("ManagerItem.seeding"); //$NON-NLS-1$
			break;
		  case DownloadManager.STATE_STOPPED :
			tmp = MessageText.getString("ManagerItem.stopped"); //$NON-NLS-1$
			break;
		  case DownloadManager.STATE_ERROR :
			tmp = MessageText.getString("ManagerItem.error") + " : " + manager.getErrorDetails(); //$NON-NLS-1$ //$NON-NLS-2$
			break;
		}
	
		return( tmp );
	}
	
	public static String
	formatDownloadStatusDefaultLocale(
		DownloadManager		manager )
	{	
		int state = manager.getState();
		
		String	tmp = "";
		
		switch (state) {
		  case DownloadManager.STATE_WAITING :
			tmp = MessageText.getDefaultLocaleString("ManagerItem.waiting"); //$NON-NLS-1$
			break;
		  case DownloadManager.STATE_ALLOCATING :
			tmp = MessageText.getDefaultLocaleString("ManagerItem.allocating"); //$NON-NLS-1$
			break;
		  case DownloadManager.STATE_CHECKING :
			tmp = MessageText.getDefaultLocaleString("ManagerItem.checking"); //$NON-NLS-1$
			break;
		  case DownloadManager.STATE_FINISHING :
		    tmp = MessageText.getDefaultLocaleString("ManagerItem.finishing"); //$NON-NLS-1$
		    break;  
        case DownloadManager.STATE_READY :
			tmp = MessageText.getDefaultLocaleString("ManagerItem.ready"); //$NON-NLS-1$
			break;
		  case DownloadManager.STATE_DOWNLOADING :
			tmp = MessageText.getDefaultLocaleString("ManagerItem.downloading"); //$NON-NLS-1$
			break;
		  case DownloadManager.STATE_SEEDING :
			tmp = MessageText.getDefaultLocaleString("ManagerItem.seeding"); //$NON-NLS-1$
			break;
		  case DownloadManager.STATE_STOPPED :
			tmp = MessageText.getDefaultLocaleString("ManagerItem.stopped"); //$NON-NLS-1$
			break;
		  case DownloadManager.STATE_ERROR :
			tmp = MessageText.getDefaultLocaleString("ManagerItem.error") + " : " + manager.getErrorDetails(); //$NON-NLS-1$ //$NON-NLS-2$
			break;
		}
	
		return( tmp );
	}
  
  
}