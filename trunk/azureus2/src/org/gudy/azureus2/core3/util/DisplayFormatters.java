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
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.NumberFormat;
import java.text.DecimalFormat;

import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.internat.*;

public class
DisplayFormatters
{
	final protected static int UNIT_B = 0;
	final protected static int UNIT_KB = 1;
	final protected static int UNIT_MB = 2;
	final protected static int UNIT_GB = 3;
	final protected static int UNIT_TB = 4;
	final protected static String UNITS_NUMBER_FORMAT[] = { "0", // B
	                                                        "0.0", //KB
	                                                        "0.0", //MB
	                                                        "0.00", //GB
	                                                        "0.000" //TB
	                                                       };
	protected static String[] units;
	protected static String[] units_rate;
	protected static int unitsStopAt = UNIT_TB;

	protected static boolean use_si_units;
	protected static boolean use_units_rate_bits;
    protected static boolean not_use_GB_TB;

	private static String lastDecimalFormat = "";

	static{
		use_si_units = COConfigurationManager.getBooleanParameter("config.style.useSIUnits", false);

		COConfigurationManager.addParameterListener( "config.style.useSIUnits",
				new ParameterListener()
				{
					public void
					parameterChanged(
						String	value )
					{
						use_si_units = COConfigurationManager.getBooleanParameter("config.style.useSIUnits", false);

						setUnits();
					}
				});

		use_units_rate_bits = COConfigurationManager.getBooleanParameter("config.style.useUnitsRateBits", false);

		COConfigurationManager.addParameterListener( "config.style.useUnitsRateBits",
				new ParameterListener()
				{
					public void
					parameterChanged(
						String	value )
					{
						use_units_rate_bits = COConfigurationManager.getBooleanParameter("config.style.useUnitsRateBits", false);

						setUnits();
					}
				});

    not_use_GB_TB = COConfigurationManager.getBooleanParameter("config.style.doNotUseGB", false);
    unitsStopAt = (not_use_GB_TB) ? UNIT_MB : UNIT_TB;

    COConfigurationManager.addParameterListener( "config.style.doNotUseGB",
        new ParameterListener()
        {
          public void
          parameterChanged(
            String  value )
          {
            not_use_GB_TB = COConfigurationManager.getBooleanParameter("config.style.doNotUseGB", false);
            unitsStopAt = (not_use_GB_TB) ? UNIT_MB : UNIT_TB;

						setUnits();
          }
        });

		setUnits();
	}

  protected static void
  setUnits()
  {
      // (1) http://physics.nist.gov/cuu/Units/binary.html
      // (2) http://www.isi.edu/isd/LOOM/documentation/unit-definitions.text

    units = new String[unitsStopAt + 1];
    units_rate = new String[unitsStopAt + 1];
    
    if ( use_si_units ){
      // fall through intentional
      switch (unitsStopAt) {
        case UNIT_TB:
          units[UNIT_TB] = " TiB";
          units_rate[UNIT_TB] = (use_units_rate_bits) ? " Tibit"  : " TiB";
        case UNIT_GB:
          units[UNIT_GB]= " GiB";
          units_rate[UNIT_GB] = (use_units_rate_bits) ? " Gibit"  : " GiB";
        case UNIT_MB:
          units[UNIT_MB] = " MiB";
          units_rate[UNIT_MB] = (use_units_rate_bits) ? " Mibit"  : " MiB";
        case UNIT_KB:
          // can be upper or lower case k
          units[UNIT_KB] = " KiB"; 
          // can be upper or lower case k, upper more consistent
          units_rate[UNIT_KB] = (use_units_rate_bits) ? " Kibit"  : " KiB";
        case UNIT_B:
          units[UNIT_B] = " B";
          units_rate[UNIT_B] = (use_units_rate_bits)  ?   " bit"  :   " B";
      }
    }else{
      switch (unitsStopAt) {
        case UNIT_TB:
          units[UNIT_TB] = " TB";
          units_rate[UNIT_TB] = (use_units_rate_bits) ? " Tbit"  : " TB";
        case UNIT_GB:
          units[UNIT_GB]= " GB";
          units_rate[UNIT_GB] = (use_units_rate_bits) ? " Gbit"  : " GB";
        case UNIT_MB:
          units[UNIT_MB] = " MB";
          units_rate[UNIT_MB] = (use_units_rate_bits) ? " Mbit"  : " MB";
        case UNIT_KB:
          // yes, the k should be lower case
          units[UNIT_KB] = " kB";
          units_rate[UNIT_KB] = (use_units_rate_bits) ? " kbit"  : " kB";
        case UNIT_B:
          units[UNIT_B] = " B";
          units_rate[UNIT_B] = (use_units_rate_bits)  ?  " bit"  :  " B";
      }
    }

    
    for (int i = 0; i <= unitsStopAt; i++) {
      units[i] = UNITS_NUMBER_FORMAT[i] + units[i];
      units_rate[i] = UNITS_NUMBER_FORMAT[i] + units_rate[i] + "/s";
    }
    NumberFormat.getPercentInstance().setMinimumFractionDigits(1);
    NumberFormat.getPercentInstance().setMaximumFractionDigits(1);
  }

	public static String
	getKiloBytePerSecUnit()
	{
		return( units_rate[UNIT_KB].substring(units_rate[UNIT_KB].indexOf(" ") + 1, units_rate[UNIT_KB].length()) );
	}

	public static String
	formatByteCountToKiBEtc(int n)
	{
		return( formatByteCountToKiBEtc((long)n));
	}

	public static
	String formatByteCountToKiBEtc(
		long n )
	{
		return( formatByteCountToKiBEtc( n, false ));
	}

	protected static
	String formatByteCountToKiBEtc(
		long	n,
		boolean	rate )
	{
	  double dbl = (rate && use_units_rate_bits) ? n * 8 : n;

    int unitIndex = UNIT_B;
		while (dbl >= 1024 && unitIndex < unitsStopAt) {
		  dbl /= 1024L;
		  unitIndex++;
		}
		
		return formatDecimal(dbl, rate ? units_rate[unitIndex] : units[unitIndex]);
	}

	public static String
	formatByteCountToKiBEtcPerSec(
		long		n )
	{
		return( formatByteCountToKiBEtc(n,true));
	}


		// base 10 ones

	public static String formatByteCountToBase10KBEtc(long n) {
		if (n < 1000)
			return String.valueOf(n).concat(" B");
		if (n < 1000 * 1000)
			return String.valueOf(n / 1000).concat(".").concat(String.valueOf((n % 1000) / 100)).concat(" KB");
		if (n < 1000L * 1000L * 1000L  || not_use_GB_TB)
			return String.valueOf(n / (1000L * 1000L)).concat(
			".").concat(
			String.valueOf((n % (1000L * 1000L)) / (1000L * 100L))).concat(
			" MB");
		if (n < 1000L * 1000L * 1000L * 1000L)
			return String.valueOf(n / (1000L * 1000L * 1000L)).concat(
			".").concat(
			String.valueOf((n % (1000L * 1000L * 1000L)) / (1000L * 1000L * 100L))).concat(
			" GB");
		if (n < 1000L * 1000L * 1000L * 1000L* 1000L)
			return String.valueOf(n / (1000L * 1000L * 1000L* 1000L)).concat(
			".").concat(
			String.valueOf((n % (1000L * 1000L * 1000L* 1000L)) / (1000L * 1000L * 1000L* 100L))).concat(
			" TB");
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
			tmp = MessageText.getString("ManagerItem.waiting");
			break;
      case DownloadManager.STATE_INITIALIZING :
        tmp = MessageText.getString("ManagerItem.initializing");
        break;
      case DownloadManager.STATE_INITIALIZED :
        tmp = MessageText.getString("ManagerItem.initializing");
        break;
		  case DownloadManager.STATE_ALLOCATING :
			tmp = MessageText.getString("ManagerItem.allocating");
			break;
		  case DownloadManager.STATE_CHECKING :
			tmp = MessageText.getString("ManagerItem.checking");
			break;
		  case DownloadManager.STATE_FINISHING :
		    tmp = MessageText.getString("ManagerItem.finishing");
			 break;
		  case DownloadManager.STATE_READY :
			tmp = MessageText.getString("ManagerItem.ready");
			break;
		  case DownloadManager.STATE_DOWNLOADING :
			tmp = MessageText.getString("ManagerItem.downloading");
			break;
		  case DownloadManager.STATE_SEEDING :
         DiskManager diskManager = manager.getDiskManager();
         if ((diskManager != null) && diskManager.isChecking()) {
           tmp = MessageText.getString("ManagerItem.seeding").concat(
                 " + ").concat(
                 MessageText.getString("ManagerItem.checking"));
         }
         else if(manager.getPeerManager()!= null && manager.getPeerManager().isSuperSeedMode()){
           tmp = MessageText.getString("ManagerItem.superseeding");
         }
         else {
           tmp = MessageText.getString("ManagerItem.seeding");
         }
			break;
		case DownloadManager.STATE_STOPPING :
			tmp = MessageText.getString("ManagerItem.stopping");
			break;
		case DownloadManager.STATE_STOPPED :
			tmp = MessageText.getString("ManagerItem.stopped");
			break;
		  case DownloadManager.STATE_QUEUED :
			tmp = MessageText.getString("ManagerItem.queued"); //$NON-NLS-1$
			break;
		  case DownloadManager.STATE_ERROR :
			tmp = MessageText.getString("ManagerItem.error").concat(": ").concat(manager.getErrorDetails()); //$NON-NLS-1$ //$NON-NLS-2$
			break;
			default :
			tmp = String.valueOf(state);
		}

		if (manager.isForceStart() &&
		    (state == DownloadManager.STATE_SEEDING ||
		     state == DownloadManager.STATE_DOWNLOADING))
			tmp = MessageText.getString("ManagerItem.forced") + " " + tmp;
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
      case DownloadManager.STATE_INITIALIZING :
        tmp = MessageText.getDefaultLocaleString("ManagerItem.initializing");
        break;
      case DownloadManager.STATE_INITIALIZED :
        tmp = MessageText.getDefaultLocaleString("ManagerItem.initializing");
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
		  case DownloadManager.STATE_STOPPING :
		  	tmp = MessageText.getDefaultLocaleString("ManagerItem.stopping");
		  	break;
		  case DownloadManager.STATE_STOPPED :
			tmp = MessageText.getDefaultLocaleString("ManagerItem.stopped"); //$NON-NLS-1$
			break;
		  case DownloadManager.STATE_QUEUED :
			tmp = MessageText.getDefaultLocaleString("ManagerItem.queued"); //$NON-NLS-1$
			break;
		  case DownloadManager.STATE_ERROR :
			tmp = MessageText.getDefaultLocaleString("ManagerItem.error").concat(": ").concat(manager.getErrorDetails()); //$NON-NLS-1$ //$NON-NLS-2$
			break;
			default :
			tmp = String.valueOf(state);
		}

		return( tmp );
	}

  public static String formatPercentFromThousands(int thousands) {
    NumberFormat nf = NumberFormat.getPercentInstance();
    nf.setMinimumFractionDigits(1);
    nf.setMaximumFractionDigits(1);
    return nf.format(thousands / 1000.0);
  }

  public static String formatTimeStamp(long time) {
    StringBuffer sb = new StringBuffer();
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(time);
    sb.append('[');
    sb.append(formatIntToTwoDigits(calendar.get(Calendar.DAY_OF_MONTH)));
    sb.append('.');
    sb.append(formatIntToTwoDigits(calendar.get(Calendar.MONTH)+1));	// 0 based
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

  public static String
  formatDate(
  	long		date )
  {
  	if ( date == 0 ){
  		return( "" );
  	}

  	SimpleDateFormat temp = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");

  	return( temp.format(new Date(date)));
  }

  public static String
  formatDateShort(
    long    date )
  {
    if ( date == 0 ){
      return( "" );
    }

    	// 24 hour clock, no point in including AM/PM

    SimpleDateFormat temp = new SimpleDateFormat("MMM dd, HH:mm");

    return( temp.format(new Date(date)));
  }

  public static String
  formatTime(
    long    time )
  {
    if ( time == 0 ){
      return( "" );
    }

    	// 24 hour clock, no point in including AM/PM

    SimpleDateFormat temp = new SimpleDateFormat("HH:mm:ss");

    return( temp.format(new Date(time)));
  }

  public static String
  formatDecimal(double value, String sFormat) {
    // this call returns a cached instance.. however, it might be worth
    // checking if caching the object ourselves gives any noticable perf gains.
    NumberFormat nf = NumberFormat.getInstance();
    if (!lastDecimalFormat.equals(sFormat) && (nf instanceof DecimalFormat)) {
      ((DecimalFormat)nf).applyPattern(sFormat);
    }
    return nf.format(value);
  }
}