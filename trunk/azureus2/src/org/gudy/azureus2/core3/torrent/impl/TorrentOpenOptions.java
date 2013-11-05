/**
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
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
 */

package org.gudy.azureus2.core3.torrent.impl;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.impl.ConfigurationDefaults;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.LocaleTorrentUtil;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;
import org.gudy.azureus2.core3.util.TorrentUtils;

import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.tag.Tag;
import com.aelitis.azureus.core.util.CopyOnWriteList;


/**
 * Class to store one Torrent file's info.  Used to populate table and store
 * user's choices.
 * <P>
 * This was copied out of the UI code, and still contains some crap code
 */
public class TorrentOpenOptions
{
	private final static String PARAM_DEFSAVEPATH = "Default save path";

	private final static String PARAM_MOVEWHENDONE = "Move Completed When Done";

	public final static int QUEUELOCATION_BOTTOM = 1;

	public final static int QUEUELOCATION_TOP = 0;

	public final static int STARTMODE_FORCESTARTED = 2;

	public final static int STARTMODE_QUEUED = 0;

	public final static int STARTMODE_SEEDING = 3;

	public final static int STARTMODE_STOPPED = 1;

	/** Where the torrent came from.  Could be a file, URL, or some other text */
	/** @todo: getter/setters */
	public String sOriginatingLocation;

	/** Filename the .torrent is saved to */
	/** @todo: getter/setters */
	public String sFileName;


	private String sDestDir;

	/** for multifiletorrents and change location */
	/** @todo: getter/setters */
	private String sDestSubDir;

	/** @todo: getter/setters */
	private TOTorrent torrent;

	private long	totalSize;
	
	/** @todo: getter/setters */
	public int iStartID;

	/** @todo: getter/setters */
	public int iQueueLocation;

	/** @todo: getter/setters */
	public boolean isValid;

	/** @todo: getter/setters */
	public boolean bDeleteFileOnCancel;

	private TorrentOpenFileOptions[] files = null;

	/** @todo: getter/setters */
	public boolean disableIPFilter = false;

	private Map<Integer, File> initial_linkage_map = null;

	private CopyOnWriteList<FileListener> fileListeners = new CopyOnWriteList<FileListener>(1);

	public Map<String, Boolean> peerSource = new HashMap<String, Boolean>();
	
	private List<Tag>	initialTags = new ArrayList<Tag>();

	private List<List<String>>	updatedTrackers;
	
		// add stuff here -> update the clone constructor
	
	/**
	 * Init
	 * 
	 * @param sFileName
	 * @param torrent
	 * @param bDeleteFileOnCancel 
	 */
	public TorrentOpenOptions(String sFileName, TOTorrent torrent,
			boolean bDeleteFileOnCancel) {
		this();
		this.bDeleteFileOnCancel = bDeleteFileOnCancel;
		this.sFileName = sFileName;
		this.sOriginatingLocation = sFileName;
		this.setTorrent(torrent, true);
	}

	public TorrentOpenOptions() {
		iStartID = getDefaultStartMode();
		iQueueLocation = QUEUELOCATION_BOTTOM;
		isValid = true;
		this.sDestDir = COConfigurationManager.getStringParameter(PARAM_DEFSAVEPATH);
	}

	/**
	 * clones everything except files and torrent
	 * @param toBeCloned
	 */
	public TorrentOpenOptions(TorrentOpenOptions toBeCloned) {
		this.sOriginatingLocation = toBeCloned.sOriginatingLocation;
		this.sFileName = toBeCloned.sFileName;
		this.sDestDir = toBeCloned.sDestDir;
		this.sDestSubDir = toBeCloned.sDestSubDir;
		this.iStartID = toBeCloned.iStartID;
		this.iQueueLocation = toBeCloned.iQueueLocation;
		this.isValid = toBeCloned.isValid;
		this.bDeleteFileOnCancel = toBeCloned.bDeleteFileOnCancel;
		this.disableIPFilter = toBeCloned.disableIPFilter;
		// this.torrent = ... // no clone
		// this.initial_linkage_map = ... // no clone
		// this.files = ... // no clone
		this.peerSource = toBeCloned.peerSource == null ? null : new HashMap<String, Boolean>(toBeCloned.peerSource);
		this.initialTags = toBeCloned.initialTags == null ? null : new ArrayList<Tag>(toBeCloned.initialTags);
		
		if ( toBeCloned.updatedTrackers != null ){
			updatedTrackers = new ArrayList<List<String>>();
			for (List<String> l: toBeCloned.updatedTrackers){
				updatedTrackers.add( new ArrayList<String>( l ));
			}
		}
	}

	public static int getDefaultStartMode() {
		return (COConfigurationManager.getBooleanParameter("Default Start Torrents Stopped"))
				? STARTMODE_STOPPED : STARTMODE_QUEUED;
	}

	public File getInitialLinkage(int index) {
		return initial_linkage_map == null ? null : (initial_linkage_map.get(index));
	}

	public String getParentDir() {
		return sDestDir;
	}

	public void setParentDir(String parentDir) {
		sDestDir = parentDir;
		parentDirChanged();
	}

	public String
	getSubDir()
	{
		return( sDestSubDir );
	}
	
	public void
	setSubDir(
		String		dir )
	{
		sDestSubDir	= dir;
	}
	
	public boolean
	isSimpleTorrent()
	{
		return( torrent.isSimpleTorrent());
	}
	
	public String getDataDir() {
		if (torrent.isSimpleTorrent())
			return sDestDir;
		return new File(sDestDir, sDestSubDir == null
				? FileUtil.convertOSSpecificChars(getTorrentName(), true) : sDestSubDir).getPath();
	}

	private String getSmartDestDir() {
		String sSmartDir = sDestDir;
		try {
			String name = getTorrentName();
			String torrentFileName = sFileName == null ? ""
					: new File(sFileName).getName().replaceFirst("\\.torrent$", "");
			int totalSegmentsLengths = 0;

			String[][] segments = {
				name.split("[^a-zA-Z]+"),
				torrentFileName.split("[^a-zA-Z]+")
			};
			List downloadManagers = AzureusCoreFactory.getSingleton().getGlobalManager().getDownloadManagers();

			for (int x = 0; x < segments.length; x++) {
				String[] segmentArray = segments[x];
				for (int i = 0; i < segmentArray.length; i++) {
					int l = segmentArray[i].length();
					if (l <= 1) {
						continue;
					}
					segmentArray[i] = segmentArray[i].toLowerCase();
					totalSegmentsLengths += l;
				}
			}

			int maxMatches = 0;
			DownloadManager match = null;
			for (Iterator iter = downloadManagers.iterator(); iter.hasNext();) {
				DownloadManager dm = (DownloadManager) iter.next();

				if (dm.getState() == DownloadManager.STATE_ERROR) {
					continue;
				}

				int numMatches = 0;

				String dmName = dm.getDisplayName().toLowerCase();

				for (int x = 0; x < segments.length; x++) {
					String[] segmentArray = segments[x];
					for (int i = 0; i < segmentArray.length; i++) {
						int l = segmentArray[i].length();
						if (l <= 1) {
							continue;
						}

						String segment = segmentArray[i];

						if (dmName.indexOf(segment) >= 0) {
							numMatches += l;
						}
					}
				}

				if (numMatches > maxMatches) {
					maxMatches = numMatches;
					match = dm;
				}
			}
			if (match != null) {
				//System.out.println(match + ": " + (maxMatches * 100 / totalSegmentsLengths) + "%\n");
				int iMatchLevel = (maxMatches * 100 / totalSegmentsLengths);
				if (iMatchLevel >= 30) {
					File f = match.getSaveLocation();
					if (!f.isDirectory() || match.getDiskManagerFileInfo().length > 1) {
						// don't place data within another torrent's data dir
						f = f.getParentFile();
					}

					if (f != null && f.isDirectory()) {
						sSmartDir = f.getAbsolutePath();
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return sSmartDir;
	}

	public List<Tag>
	getInitialTags()
	{
		return( new ArrayList<Tag>( initialTags ));
	}
	
	public void
	setInitialTags(
		List<Tag>		tags )
	{
		initialTags = tags;
	}
	
	public List<List<String>>
	getTrackers(
		boolean	if_updated )
	{
		if ( updatedTrackers != null ){
			
			return( updatedTrackers );
		}
		
		if ( if_updated ){
			
			return( null );
		}
		
		if ( torrent == null ){
			
			return( new ArrayList<List<String>>(0));
			
		}else{
		
			return( TorrentUtils.announceGroupsToList(torrent));
		}
	}
	
	public void
	setTrackers(
		List<List<String>>	trackers )
	{
		updatedTrackers = trackers;
	}
	
	public TorrentOpenFileOptions[] getFiles() {
		if (files == null && torrent != null) {
			TOTorrentFile[] tfiles = torrent.getFiles();
			files = new TorrentOpenFileOptions[tfiles.length];
			for (int i = 0; i < files.length; i++) {
				files[i] = new TorrentOpenFileOptions(this, tfiles[i], i);
			}
		}

		return files;
	}

	public long
	getTotalSize()
	{
		if ( totalSize == 0 ){
			
			TorrentOpenFileOptions[] files = getFiles();
			
			if ( files != null ){
			
				for ( TorrentOpenFileOptions file: files ){
					
					totalSize += file.lSize;
				}
			}
		}
		
		return( totalSize );
	}
	public String getTorrentName() {
		return TorrentUtils.getLocalisedName(torrent);
	}

	public boolean allFilesMoving() {
		TorrentOpenFileOptions[] files = getFiles();
		for (int j = 0; j < files.length; j++) {
			if (files[j].isLinked()) {
				return false;
			}
		}
		return true;
	}

	public boolean allFilesExist() {
		// check if all selected files exist
		TorrentOpenFileOptions[] files = getFiles();
		for (int i = 0; i < files.length; i++) {
			TorrentOpenFileOptions fileInfo = files[i];
			if (!fileInfo.isToDownload())
				continue;

			File file = fileInfo.getDestFileFullName();
			if (!file.exists() || file.length() != fileInfo.lSize) {
				return false;
			}
		}
		return true;
	}

	public void renameDuplicates() {
		if (iStartID == STARTMODE_SEEDING
				|| !COConfigurationManager.getBooleanParameter("DefaultDir.AutoSave.AutoRename")
				|| allFilesExist()) {
			return;
		}

		if (!torrent.isSimpleTorrent()) {
			if (new File(getDataDir()).isDirectory()) {
				File f;
				int idx = 0;
				do {
					idx++;
					f = new File(getDataDir() + "-" + idx);
				} while (f.isDirectory());

				sDestSubDir = f.getName();
			}
		} else {
			// should only be one file
			TorrentOpenFileOptions[] fileInfos = getFiles();
			for (int i = 0; i < fileInfos.length; i++) {
				TorrentOpenFileOptions info = fileInfos[i];

				File file = info.getDestFileFullName();
				int idx = 0;
				while (file.exists()) {
					idx++;
					file = new File(info.getDestPathName(), idx + "-"
							+ info.getDestFileName());
				}

				info.setDestFileName(file.getName());
			}
		}
	}

	/*
	private Boolean has_multiple_small_files = null; 
	private boolean hasMultipleSmallFiles() {
		TorrentFileInfo[] tfi_files = getFiles();
		if (tfi_files.length <= MAX_NODOWNLOAD_COUNT)
			return false;
		
		int small_files_counted = 0;
		for (int i=0; i<tfi_files.length; i++) {
			if (tfi_files[i].lSize < MIN_NODOWNLOAD_SIZE) {
				small_files_counted++;
				if (small_files_counted > MAX_NODOWNLOAD_COUNT) {
					return true;
				}
			}
		}
		
		return false;
	}
	*/

	// Indicates whether all files in this torrent can be deselected
	// (if not, then it occurs on a per-file basis).
	public boolean okToDisableAll() {
		return true;

		/*
		if (iStartID == STARTMODE_SEEDING)
			return true;
		
		// Do we have multiple small files? We'll allow all of them to
		// be disabled if we do.
		if (has_multiple_small_files == null) {
			has_multiple_small_files = new Boolean(hasMultipleSmallFiles());
		}
		
		// You can disable all files if there are lots of small files.
		return has_multiple_small_files.booleanValue();
		*/
	}

	public TOTorrent getTorrent() {
		return torrent;
	}

	public void setTorrent(TOTorrent torrent, boolean updateDestDir) {
		this.torrent = torrent;

		if (updateDestDir) {
			if (COConfigurationManager.getBooleanParameter("DefaultDir.BestGuess")
					&& !COConfigurationManager.getBooleanParameter(PARAM_MOVEWHENDONE)) {
				this.sDestDir = getSmartDestDir();
			}
		}
		
		if (torrent == null) {
			initial_linkage_map = null;
		} else {
			initial_linkage_map = TorrentUtils.getInitialLinkage(torrent);

			// Force a check on the encoding, will prompt user if we dunno
			try {
				LocaleTorrentUtil.getTorrentEncoding(torrent);
			} catch (Exception e) {
				e.printStackTrace();
			}

			renameDuplicates();
		}
	}

	
	public void addListener(FileListener l) {
		fileListeners.add(l);
	}
	
	public void removeListener(FileListener l) {
		fileListeners.remove(l);
	}
	
	public interface FileListener {
		public void toDownloadChanged(TorrentOpenFileOptions torrentOpenFileOptions, boolean toDownload);
		public void priorityChanged(TorrentOpenFileOptions torrentOpenFileOptions, int priority );
		public void parentDirChanged();
	}

	public void fileDownloadStateChanged(
			TorrentOpenFileOptions torrentOpenFileOptions, boolean toDownload) 
	{
		for ( FileListener l : fileListeners) {
			try{
				l.toDownloadChanged(torrentOpenFileOptions, toDownload);
			}catch( Throwable e ){
				Debug.out( e );
			}
		}
	}

	public void filePriorityStateChanged(
			TorrentOpenFileOptions torrentOpenFileOptions, int priority) 
	{
		for ( FileListener l : fileListeners) {
			try{
				l.priorityChanged(torrentOpenFileOptions, priority);
			}catch( Throwable e ){
				Debug.out( e );
			}
		}
	}
	
	public void parentDirChanged()
	{
		for ( FileListener l : fileListeners) {
			try{
				l.parentDirChanged();
			}catch( Throwable e ){
				Debug.out( e );
			}
		}
	}
}