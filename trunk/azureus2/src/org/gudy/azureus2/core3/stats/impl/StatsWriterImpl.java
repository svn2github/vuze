/*
 * File    : StatsWriterImpl.java
 * Created : 30-Oct-2003
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

package org.gudy.azureus2.core3.stats.impl;

/**
 * @author parg
 *
 */

import java.util.*;
import java.io.*;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.xml.util.*;
import org.gudy.azureus2.core3.global.*;
import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.peer.*;
import org.gudy.azureus2.core3.torrent.*;

public class 
StatsWriterImpl
	extends XUXmlWriter
{
	protected GlobalManager		global;
	
	protected
	StatsWriterImpl(
		GlobalManager		_global )
	{			
		global		= _global;
	}
	
	protected void
	write(
		String		file_name )
	
		throws IOException
	{
		try{
			setOutputStream( new FileOutputStream( file_name ));
			
			writeSupport();
		}finally{
			
			closeOutputStream();
		}
	}	
	
	protected void
	write(
		OutputStream		os )
	
		throws IOException
	{
		try{
			setOutputStream( os );
			
			writeSupport();
		}finally{
			
			flushOutputStream();
		}
	}
	
	protected void
	writeSupport()
	{
		writeLineRaw( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" );

		boolean	export_peer_stats = COConfigurationManager.getBooleanParameter("Stats Export Peer Details");
		
		String xsl = COConfigurationManager.getStringParameter( "Stats XSL File" );
		
		if ( xsl.length() > 0 ){
			
			writeLineRaw( "<?xml-stylesheet type=\"text/xsl\" href=\"" + xsl + "\"?>" );
		}
		
		writeLineRaw( "<STATS>");
	
		try{
			indent();
		
			writeTag( "AZUREUS_VERSION", Constants.AZUREUS_VERSION );
			
			writeLineRaw( "<GLOBAL>" );
			
			try{
				indent();
				
				GlobalManagerStats	gm_stats = global.getStats();
									
				writeRawCookedAverageTag( "DOWNLOAD_SPEED", gm_stats.getDownloadAverage() );
				writeRawCookedAverageTag( "UPLOAD_SPEED", 	gm_stats.getUploadAverage() );
				
			}finally{
				
				exdent();
			}
		
			writeLineRaw( "</GLOBAL>" );
			
			writeLineRaw( "<DOWNLOADS>");
			
			try{
				
				indent();
				
				List	_dms = global.getDownloadManagers();
			
				DownloadManager[]	dms = new DownloadManager[_dms.size()];
				
					// sort by index, downloads then seeders
				
				_dms.toArray( dms );
				
				Arrays.sort(
					dms,
					new Comparator()
					{
						public int 
						compare(
							Object o1, 
							Object o2)
						{
							DownloadManager	d1 = (DownloadManager)o1;
							DownloadManager	d2 = (DownloadManager)o2;
							
							int	d1_index 	= d1.getIndex();
							int d2_index	= d2.getIndex();
							
							if ( d1.getStats().getDownloadCompleted(false) == 1000 ){
								
								d1_index	+= 1000000;
							}
							
							if ( d2.getStats().getDownloadCompleted(false) == 1000 ){
								
								d2_index	+= 1000000;
							}

							return( d1_index - d2_index );
						}
					});
				
				for (int i=0;i<dms.length;i++){
					
					DownloadManager	dm = (DownloadManager)dms[i];
					
					DownloadManagerStats	dm_stats = dm.getStats();
					
					writeLineRaw( "<DOWNLOAD>");
					
					try{
						indent();
						
						writeLineRaw( "<TORRENT>" );

							// torrent can be null if broken torrent!
						
						TOTorrent torrent = dm.getTorrent();
															
						try{
							indent();
					
							writeTag( "NAME", dm.getDisplayName());
                                                                
							writeTag( "TORRENT_FILE", dm.getTorrentFileName());
							
							if ( torrent != null ){
								
								writeTag( "HASH", ByteFormatter.nicePrintTorrentHash(torrent, true));
							
								writeRawCookedTag( "SIZE", torrent.getSize());
								
								writeTag( "PIECE_LENGTH", torrent.getPieceLength());
								
								writeTag( "PIECE_COUNT", torrent.getPieces().length );
								
								writeTag( "FILE_COUNT", torrent.getFiles().length );
								
								writeTag( "COMMENT", dm.getTorrentComment());
								
								writeTag( "CREATED_BY", dm.getTorrentCreatedBy());
								
								writeTag( "CREATION_DATE", torrent.getCreationDate());
							}
							
						}finally{
							
							exdent();
						}
						
						writeLineRaw( "</TORRENT>");
						
						writeTag( "DOWNLOAD_STATUS", DisplayFormatters.formatDownloadStatusDefaultLocale( dm));
						
						writeTag( "DOWNLOAD_DIR", dm.getTorrentSaveDirAndFile());
						
						if ( torrent != null ){
								
							if ( torrent.isSimpleTorrent()){
							
								writeTag( "TARGET_FILE", dm.getTorrentSaveDirAndFile());
								
							}else{
								
								writeTag( "TARGET_DIR", dm.getTorrentSaveDirAndFile());
							}
						}
						
						writeTag( "TRACKER_STATUS", dm.getTrackerStatus());
					
						writeTag( "COMPLETED", 		dm_stats.getCompleted());
						
						writeRawCookedTag( "DOWNLOADED", 		dm_stats.getDownloaded());
						writeRawCookedTag( "UPLOADED", 			dm_stats.getUploaded());
						writeRawCookedTag( "DISCARDED", 		dm_stats.getDiscarded());
						
						writeRawCookedAverageTag( "DOWNLOAD_SPEED", 	dm_stats.getDownloadAverage());
						writeRawCookedAverageTag( "UPLOAD_SPEED", 		dm_stats.getUploadAverage());
						writeRawCookedAverageTag( "TOTAL_SPEED", 		dm_stats.getTotalAverage());
																				
						writeTag( "ELAPSED", 		dm_stats.getElapsedTime());
						writeTag( "ETA", 			DisplayFormatters.formatETA(dm_stats.getETA()));
						writeTag( "HASH_FAILS", 	dm_stats.getHashFails());
						writeTag( "SHARE_RATIO", 	dm_stats.getShareRatio());
			
						writeTag( "TOTAL_SEEDS", dm.getNbSeeds());
						writeTag( "TOTAL_LEECHERS", dm.getNbPeers());
						
						if ( export_peer_stats ){
							
							try{
								writeLineRaw( "<PEERS>");
								
								indent();
							
								PEPeerManager pm = dm.getPeerManager();
								
								if ( pm != null ){
									
									List	peers = pm.getPeers();
									
									for (int j=0;j<peers.size();j++){
										
										PEPeer	peer = (PEPeer)peers.get(j);
										
										PEPeerStats	peer_stats = peer.getStats();
										
										byte[]	id	= peer.getId();
										
										if ( id == null ){
											
											continue;
										}
										
										try{
											String	peer_id = Identification.getPrintablePeerID( id );
											
											peer_id = escapeXML(peer_id);
											
											String	type = escapeXML( peer.getClient());
											
											writeLineRaw( "<PEER hex_id=\"" + ByteFormatter.encodeString( id ) + "\" printable_id=\""+ peer_id + "\" type=\"" + type + "\">");
										
											indent();
										
											writeTag( "IP", peer.getIp());
											
											writeTag( "IS_SEED", peer.isSeed());
											
											writeRawCookedTag( "DOWNLOADED", peer_stats.getTotalReceived());
											writeRawCookedTag( "UPLOADED", peer_stats.getTotalSent());
											
											writeRawCookedAverageTag( "DOWNLOAD_SPEED", peer_stats.getDownloadAverage());
											writeRawCookedAverageTag( "UPLOAD_SPEED", peer_stats.getUploadAverage());
											
										}catch( Throwable e ){
											
											e.printStackTrace();
											
										}finally{
										
											exdent();
											
											writeLineRaw( "</PEER>");
										}
									}
								}
							}finally{
								
								exdent();
								
								writeLineRaw( "</PEERS>");
							}
						}
					}finally{
						
						exdent();
					}
					
					writeLineRaw( "</DOWNLOAD>");
				}
				
			}finally{
				
				exdent();
			}
				
			writeLineRaw( "</DOWNLOADS>" );
			
		}finally{
			
			exdent();
		}
		writeLineRaw( "</STATS>");	

	}
	
	protected void
	writeRawCookedTag(
		String	tag,
		long	raw )
	{
		writeLineRaw( "<" + tag + ">");
						
		try{
			indent();
							
			writeTag( "TEXT",	DisplayFormatters.formatByteCountToKiBEtc( raw ));
			writeTag( "RAW",	raw);
							
		}finally{
							
			exdent();
		}
						
		writeLineRaw( "</" + tag + ">");
	}
	
	protected void
	writeRawCookedAverageTag(
		String	tag,
		long	raw )
	{
		writeLineRaw( "<" + tag + ">");
						
		try{
			indent();
							
			writeTag( "TEXT",	DisplayFormatters.formatByteCountToKiBEtcPerSec( raw ));
			writeTag( "RAW",	raw);
							
		}finally{
							
			exdent();
		}
								
		writeLineRaw( "</" + tag + ">");
	}
}
