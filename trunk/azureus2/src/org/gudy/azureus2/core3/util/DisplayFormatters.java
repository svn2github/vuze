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

import java.util.Calendar;

import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.internat.*;

public class 
DisplayFormatters 
{
	protected static String	k_unit;
	protected static String	m_unit;
	protected static String	g_unit;
	
	static{
		boolean si = COConfigurationManager.getBooleanParameter("config.style.useSIUnits", false);
	
		COConfigurationManager.addParameterListener( "config.style.useSIUnits",
				new ParameterListener()
				{
					public void
					parameterChanged(
						String	value )
					{
						setUnits(COConfigurationManager.getBooleanParameter("config.style.useSIUnits", false));
					}
				});
		
		setUnits(si);
	}
	
	protected static void
	setUnits(
		boolean	si )
	{
		if ( si ){
			k_unit = " KiB";
			m_unit = " MiB";
			g_unit = " GiB";
		}else{
			k_unit = " KB";
			m_unit = " MB";
			g_unit = " GB";
		}
	}
	public static String formatByteCountToKiBEtc(int n) {
	  if (n < 1024)
		return String.valueOf(n).concat(" B");
	  if (n < 1024 * 1024)
		return String.valueOf((n / 1024)).concat(".").concat(String.valueOf(((n % 1024) / 102))).concat(k_unit);
	  if (n < 1024 * 1024 * 1024)
		return String.valueOf((n / (1024 * 1024))).concat(
      ".").concat(
      String.valueOf(((n % (1024 * 1024)) / 104857))).concat(
      m_unit);
	  if (n < 1024 * 1024 * 1024 * 1024)
		return String.valueOf((n / (1024 * 1024 * 1024))).concat(
      ".").concat(
      String.valueOf(
      ((n % (1024 * 1024 * 1024)) / 107374182))).concat(
      g_unit);
	  return "A lot";
	}

	public static String formatByteCountToKiBEtc(long n) {
	  if (n < 1024)
		return String.valueOf(n).concat(" B");
	  if (n < 1024 * 1024)
		return String.valueOf((n / 1024)).concat(".").concat(String.valueOf(((n % 1024) / 102))).concat(k_unit);
	  if (n < 1024 * 1024 * 1024)
		return String.valueOf(n / (1024 * 1024)).concat(
		  ".").concat(
		  String.valueOf((n % (1024 * 1024)) / 104857)).concat(
		  m_unit);
	  if (n < 1024l * 1024l * 1024l * 1024l)
		return String.valueOf(n / (1024l * 1024l * 1024l)).concat(
		  ".").concat(
		  String.valueOf((n % (1024l * 1024l * 1024l)) / 107374182l)).concat(
		  g_unit);
	  return "A lot !!!";
	}
	
	public static String
	formatByteCountToKiBEtcPerSec(
		long		n )
	{
		return( formatByteCountToKiBEtc(n).concat("/s"));
	}
  
	
		// base 10 ones
	
	public static String formatByteCountToBase10KBEtc(long n) {
		if (n < 1000)
			return String.valueOf(n).concat(" B");
		if (n < 1000 * 1000)
			return String.valueOf(n / 1000).concat(".").concat(String.valueOf((n % 1000) / 100)).concat(" KB");
		if (n < 1000 * 1000 * 1000)
			return String.valueOf(n / (1000 * 1000)).concat(
			".").concat(
			String.valueOf((n % (1000 * 1000)) / (100 * 1000))).concat(
			" MB");
		if (n < 1000l * 1000l * 1000l * 1000l)
			return String.valueOf(n / (1000l * 1000l * 1000l)).concat(
			".").concat(
			String.valueOf((n % (1000l * 1000l * 1000l)) / (100l * 1000l * 1000l))).concat(
			" GB");
		return "A lot !!!";
	}
	
	public static String
	formatByteCountToBase10KBEtcPerSec(
			long		n )
	{
		return( formatByteCountToBase10KBEtc(n).concat("/s"));
	}
	
   public static String formatETA(long eta) {
     if (eta == 0) return MessageText.getString("PeerManager.status.finished");
     if (eta == -1) return "";
     if (eta > 0) return TimeFormater.format(eta);
     
     return MessageText.getString("PeerManager.status.finishedin").concat(
            " ").concat(TimeFormater.format(eta * -1));
   }
  
	
	public static String
	formatDownloaded(
		DownloadManagerStats	stats )
	{
		long	total_discarded = stats.getDiscarded();
		long	total_received 	= stats.getDownloaded();
		
		if(total_discarded == 0){
		
			return formatByteCountToKiBEtc(total_received);
			
		}else{
			
			return formatByteCountToKiBEtc(total_received).concat(" ( ").concat(DisplayFormatters.formatByteCountToKiBEtc(total_discarded)).concat(" ").concat(MessageText.getString("discarded")).concat(" )"); 
		}
	}
	
	public static String
	formatHashFails(
		DownloadManager		download_manager )
	{
		PEPeerManager	pm = download_manager.getPeerManager();
		DiskManager		dm = download_manager.getDiskManager();
			
	  	if(pm != null){
	  	
			long nbFails = pm.getNbHashFails();
		
				// size can exceed int so ensure longs used in multiplication
			
			long size = nbFails * (long)dm.getPieceLength();
			
			String result = String.valueOf(nbFails).concat(" ( ~ ").concat(formatByteCountToKiBEtc(size)).concat(" )");
			
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
         if (manager.getDiskManager().isChecking()) {
           tmp = MessageText.getString("ManagerItem.seeding").concat(
                 " + ").concat(
                 MessageText.getString("ManagerItem.checking"));
         }
         else if(manager.getPeerManager()!= null && manager.getPeerManager().isSuperSeedMode()){
           tmp = MessageText.getString("ManagerItem.superseeding"); //$NON-NLS-1$
         }
         else {
           tmp = MessageText.getString("ManagerItem.seeding"); //$NON-NLS-1$
         }
			break;
		  case DownloadManager.STATE_STOPPED :
			tmp = MessageText.getString("ManagerItem.stopped"); //$NON-NLS-1$
			break;
		  case DownloadManager.STATE_ERROR :
			tmp = MessageText.getString("ManagerItem.error").concat(" : ").concat(manager.getErrorDetails()); //$NON-NLS-1$ //$NON-NLS-2$
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
		  	if (manager.getDiskManager().isChecking()) {
		  		tmp = MessageText.getDefaultLocaleString("ManagerItem.seeding").concat(
		  		" + ").concat(
		  		MessageText.getDefaultLocaleString("ManagerItem.checking"));
		  	}
		  	else if(manager.getPeerManager()!= null && manager.getPeerManager().isSuperSeedMode()){
		  		
		  		tmp = MessageText.getDefaultLocaleString("ManagerItem.superseeding"); //$NON-NLS-1$
		  	}
		  	else {
		  		tmp = MessageText.getDefaultLocaleString("ManagerItem.seeding"); //$NON-NLS-1$
		  	}
		  	break;
		  case DownloadManager.STATE_STOPPED :
			tmp = MessageText.getDefaultLocaleString("ManagerItem.stopped"); //$NON-NLS-1$
			break;
		  case DownloadManager.STATE_ERROR :
			tmp = MessageText.getDefaultLocaleString("ManagerItem.error").concat(" : ").concat(manager.getErrorDetails()); //$NON-NLS-1$ //$NON-NLS-2$
			break;
		}
	
		return( tmp );
	}
  
  public static String formatPercentFromThousands(int thousands) {
    return (thousands / 10) + "." + (thousands % 10) + " %";
  }
  
  public static String formatTimeStamp(long time) {
    StringBuffer sb = new StringBuffer();
    Calendar calendar = Calendar.getInstance();    
    calendar.setTimeInMillis(time);
    sb.append('[');
    sb.append(formatIntToTwoDigits(calendar.get(Calendar.DAY_OF_MONTH)));
    sb.append('.');
    sb.append(formatIntToTwoDigits(calendar.get(Calendar.MONTH)));
    sb.append('.');
    sb.append(calendar.get(Calendar.YEAR));
    sb.append(' ');
    sb.append(formatIntToTwoDigits(calendar.get(Calendar.HOUR_OF_DAY)));
    sb.append(':');
    sb.append(formatIntToTwoDigits(calendar.get(Calendar.MINUTE)));
    sb.append(':');
    sb.append(formatIntToTwoDigits(calendar.get(Calendar.SECOND)));
    sb.append(']');
    return sb.toString();
  }
  
  public static String formatIntToTwoDigits(int n) {
    return n < 10 ? "0".concat(String.valueOf(n)) : String.valueOf(n);
  }
  
}