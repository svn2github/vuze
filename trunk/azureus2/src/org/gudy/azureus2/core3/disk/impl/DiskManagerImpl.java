/*
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
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
 * Created on Oct 18, 2003
 * Created by Paul Gardner
 * Modified Apr 13, 2004 by Alon Rohter
 * Copyright (C) 2004, 2005, 2006 Aelitis, All Rights Reserved.
 *
 */

package org.gudy.azureus2.core3.disk.impl;

import java.io.*;
import java.util.*;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.disk.impl.access.*;
import org.gudy.azureus2.core3.disk.impl.piecemapper.*;
import org.gudy.azureus2.core3.disk.impl.resume.RDResumeHandler;
import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.internat.*;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.platform.*;
import org.gudy.azureus2.plugins.platform.PlatformManagerException;

import com.aelitis.azureus.core.diskmanager.access.*;
import com.aelitis.azureus.core.diskmanager.cache.*;
import com.aelitis.azureus.core.diskmanager.file.FMFileManagerFactory;
import com.aelitis.azureus.core.util.CaseSensitiveFileMap;


/**
 *
 * The disk Wrapper.
 *
 * @author Tdv_VgA
 * @author MjrTom
 *          2005/Oct/08: new piece-picking support changes
 *          2006/Jan/02: refactoring piece picking related code
 *
 */

public class
DiskManagerImpl
    extends LogRelation
    implements DiskManagerHelper, DMReaderAdapter
{
    private static final LogIDs LOGID = LogIDs.DISK;

    private static DiskAccessController disk_access_controller;

    static {
        int max_read_threads        = COConfigurationManager.getIntParameter( "diskmanager.perf.read.maxthreads" );
        int max_read_mb             = COConfigurationManager.getIntParameter( "diskmanager.perf.read.maxmb" );
        int max_write_threads       = COConfigurationManager.getIntParameter( "diskmanager.perf.write.maxthreads" );
        int max_write_mb            = COConfigurationManager.getIntParameter( "diskmanager.perf.write.maxmb" );

        disk_access_controller =
            DiskAccessControllerFactory.create(
                    max_read_threads, max_read_mb,
                    max_write_threads, max_write_mb );

        if (Logger.isEnabled()){
            Logger.log(
                    new LogEvent(
                            LOGID,
                            "Disk access controller params: " +
                                max_read_threads + "/" + max_read_mb + "/" + max_write_threads + "/" + max_write_mb ));

        }
    }

    private static DiskManagerRecheckScheduler      recheck_scheduler       = new DiskManagerRecheckScheduler();
    private static DiskManagerAllocationScheduler   allocation_scheduler    = new DiskManagerAllocationScheduler();

    private static AEMonitor    cache_read_mon  = new AEMonitor( "DiskManager:cacheRead" );

    private boolean used    = false;

    private boolean started = false;
    private AESemaphore started_sem = new AESemaphore( "DiskManager::started" );
    private boolean starting;
    private boolean stopping;


    private int state_set_via_method;
    protected String errorMessage = "";

    private int pieceLength;
    private int lastPieceLength;

    private int         nbPieces;       // total # pieces in this torrent
    private long        totalLength;    // total # bytes in this torrent
    private int         percentDone;
    private long        allocated;
    private long        remaining;


    private TOTorrent       torrent;


    private DMReader                reader;
    private DMChecker               checker;
    private DMWriter                writer;

    private RDResumeHandler         resume_handler;
    private DMPieceMapper           piece_mapper;

    private DiskManagerPieceImpl[]  pieces;
    private DMPieceList[]           pieceMap;

    private DiskManagerFileInfoImpl[]   files;
    protected DownloadManager       download_manager;

    private boolean alreadyMoved = false;

    private boolean             skipped_file_set_changed =true; // go over them once when starting
    private long                skipped_file_set_size;
    private long                skipped_but_downloaded;


        // DiskManager listeners

    private static final int LDT_STATECHANGED           = 1;
    private static final int LDT_PRIOCHANGED            = 2;
    private static final int LDT_PIECE_DONE_CHANGED     = 3;
    private static final int LDT_ACCESS_MODE_CHANGED    = 4;

    protected static ListenerManager    listeners_aggregator    = ListenerManager.createAsyncManager(
            "DiskM:ListenAggregatorDispatcher",
            new ListenerManagerDispatcher()
            {
                public void
                dispatch(
                    Object      _listener,
                    int         type,
                    Object      value )
                {
                    DiskManagerListener listener = (DiskManagerListener)_listener;

                    if (type == LDT_STATECHANGED){

                        int params[] = (int[])value;

                        listener.stateChanged(params[0], params[1]);

                    }else if (type == LDT_PRIOCHANGED) {

                        listener.filePriorityChanged((DiskManagerFileInfo)value);

                    }else if (type == LDT_PIECE_DONE_CHANGED) {

                        listener.pieceDoneChanged((DiskManagerPiece)value);

                    }else if (type == LDT_ACCESS_MODE_CHANGED) {

                        Object[]    o = (Object[])value;

                        listener.fileAccessModeChanged(
                            (DiskManagerFileInfo)o[0],
                            ((Integer)o[1]).intValue(),
                            ((Integer)o[2]).intValue());
                    }
                }
            });

    private ListenerManager listeners   = ListenerManager.createManager(
            "DiskM:ListenDispatcher",
            new ListenerManagerDispatcher()
            {
                public void
                dispatch(
                    Object      listener,
                    int         type,
                    Object      value )
                {
                    listeners_aggregator.dispatch( listener, type, value );
                }
            });

    private AEMonitor   start_stop_mon  = new AEMonitor( "DiskManager:startStop" );
    private AEMonitor   file_piece_mon  = new AEMonitor( "DiskManager:filePiece" );

    protected static int        max_read_block_size;

    static{

        ParameterListener param_listener = new ParameterListener() {
                public void
                parameterChanged(
                    String  str )
                {
                    max_read_block_size = COConfigurationManager.getIntParameter( "BT Request Max Block Size" );
                }
        };

        COConfigurationManager.addAndFireParameterListener( "BT Request Max Block Size", param_listener);
    }

    public
    DiskManagerImpl(
        TOTorrent           _torrent,
        DownloadManager     _dmanager)
    {
        torrent             = _torrent;
        download_manager    = _dmanager;

        pieces      = new DiskManagerPieceImpl[0];  // in case things go wrong later

        setState( INITIALIZING );

        percentDone = 0;

        if ( torrent == null ){

            errorMessage     = "Torrent not available";

            setState( FAULTY );

            return;
        }

        LocaleUtilDecoder   locale_decoder = null;

        try{
            locale_decoder = LocaleTorrentUtil.getTorrentEncoding( torrent );

        }catch( TOTorrentException e ){

            Debug.printStackTrace( e );

            errorMessage = TorrentUtils.exceptionToText(e);

            setState( FAULTY );

            return;

        }catch( Throwable e ){

            Debug.printStackTrace( e );

            errorMessage = "Initialisation failed - " + Debug.getNestedExceptionMessage(e);

            setState( FAULTY );

            return;
        }

        piece_mapper    = DMPieceMapperFactory.create( torrent );

        try{
            piece_mapper.construct( locale_decoder, download_manager.getAbsoluteSaveLocation().getName());

        }catch( Throwable e ){

            Debug.printStackTrace( e );

            errorMessage = "Failed to build piece map - " + Debug.getNestedExceptionMessage(e);

            setState( FAULTY );

            return;
        }

        totalLength = piece_mapper.getTotalLength();
        remaining   = totalLength;

        nbPieces    = torrent.getNumberOfPieces();

        pieceLength     = (int)torrent.getPieceLength();
        lastPieceLength = piece_mapper.getLastPieceLength();

        pieces      = new DiskManagerPieceImpl[nbPieces];
        for (int i =0; i <nbPieces; i++)
        {
            pieces[i] =new DiskManagerPieceImpl(this, i);
        }

        reader          = DMAccessFactory.createReader(this);

        checker         = DMAccessFactory.createChecker(this);

        writer          = DMAccessFactory.createWriter(this);

        resume_handler  = new RDResumeHandler( this, checker );

    }

    public void
    start()
    {
        try{
            start_stop_mon.enter();

            if ( used ){

                Debug.out( "DiskManager reuse not supported!!!!" );
            }

            used    = true;

            if ( getState() == FAULTY ){

                Debug.out( "starting a faulty disk manager");

                return;
            }

            started     = true;
            starting    = true;

            Thread init =
                new AEThread("DiskManager:start")
                {
                    public void
                    runSupport()
                    {
                        try{
                            startSupport();

                        }catch( Throwable e ){

                            errorMessage = Debug.getNestedExceptionMessage(e) + " (start)";

                            setState( FAULTY );

                        }finally{

                            started_sem.release();
                        }

                        boolean stop_required;

                        try{
                            start_stop_mon.enter();

                            stop_required = DiskManagerImpl.this.getState() == DiskManager.FAULTY || stopping;

                            starting    = false;

                        }finally{

                            start_stop_mon.exit();
                        }

                        if ( stop_required ){

                            DiskManagerImpl.this.stop( false );
                        }
                    }
                };

            init.setPriority(Thread.MIN_PRIORITY);

            init.start();

        }finally{

            start_stop_mon.exit();
        }
    }

    private void
    startSupport()
    {
            //if the data file is already in the completed files dir, we want to use it

        boolean moveWhenDone = COConfigurationManager.getBooleanParameter("Move Completed When Done", false);

        String moveToDir = COConfigurationManager.getStringParameter("Completed Files Directory", "");

        if ( moveWhenDone && moveToDir.length() > 0 && download_manager.isPersistent()){

                //if the data file already resides in the completed files dir

            if ( filesExist( moveToDir )){

                alreadyMoved = true;

                download_manager.setTorrentSaveDir( moveToDir );
            }
        }

        reader.start();

        checker.start();

        writer.start();

            //allocate / check every file

        int newFiles = allocateFiles();

        if ( getState() == FAULTY ){

                // bail out if broken in the meantime
                // state will be "faulty" if the allocation process is interrupted by a stop

            return;
        }

        pieceMap = piece_mapper.getPieceMap();

        constructFilesPieces();

        if ( getState() == FAULTY  ){

                // bail out if broken in the meantime

            return;
        }

        setState( DiskManager.CHECKING );

        resume_handler.start();

        if ( newFiles == 0 ){

            resume_handler.checkAllPieces(false);

        }else if ( newFiles != files.length ){

                //  if not a fresh torrent, check pieces ignoring fast resume data

            resume_handler.checkAllPieces(true);
        }

        if ( getState() == FAULTY  ){

            return;
        }

            // in all the above cases we want to continue to here if we have been "stopped" as
            // other components require that we end up either FAULTY or READY

            //3.Change State

        setState( READY );
    }

    public void
    stop(
    	boolean	closing )
    {
        try{
            start_stop_mon.enter();

            if ( !started ){

                return;
            }

                // we need to be careful if we're still starting up as this may be
                // a re-entrant "stop" caused by a faulty state being reported during
                // startup. Defer the actual stop until starting is complete

            if ( starting ){

                stopping    = true;

                    // we can however safely stop things at this point - this is important
                    // to interrupt an alloc/recheck process that might be holding up the start
                    // operation

                checker.stop();

                writer.stop();

                reader.stop();

                resume_handler.stop( closing );

                return;
            }

            started     = false;

            stopping    = false;

        }finally{

            start_stop_mon.exit();
        }

        started_sem.reserve();

        checker.stop();

        writer.stop();

        reader.stop();

        resume_handler.stop( closing );

        if ( files != null ){

            for (int i = 0; i < files.length; i++){

                try{
                    if (files[i] != null) {

                        files[i].getCacheFile().close();
                    }
                }catch ( Throwable e ){

                    setFailed( "File close fails: " + Debug.getNestedExceptionMessage(e));
                }
            }
        }

        if ( getState() == DiskManager.READY ){

            try{

                saveResumeData( false );

            }catch( Exception e ){

                setFailed( "Resume data save fails: " + Debug.getNestedExceptionMessage(e));
            }
        }

        saveState();

        // can't be used after a stop so we might as well clear down the listeners
        listeners.clear();
    }

    public boolean
    filesExist()
    {
        return( filesExist( download_manager.getAbsoluteSaveLocation().getParent()));
    }

    protected boolean
    filesExist(
        String  root_dir )
    {
        if ( !torrent.isSimpleTorrent()){

            root_dir += File.separator + download_manager.getAbsoluteSaveLocation().getName();
        }

        if ( !root_dir.endsWith( File.separator )){

            root_dir    += File.separator;
        }

        // System.out.println( "root dir = " + root_dir_file );

        DMPieceMapperFile[] pm_files = piece_mapper.getFiles();

        String[]    storage_types = getStorageTypes();

        for (int i = 0; i < pm_files.length; i++) {

            DMPieceMapperFile pm_info = pm_files[i];

            File    relative_file = pm_info.getDataFile();

            long target_length = pm_info.getLength();

                // use the cache file to ascertain length in case the caching/writing algorithm
                // fiddles with the real length
                // Unfortunately we may be called here BEFORE the disk manager has been
                // started and hence BEFORE the file info has been setup...
                // Maybe one day we could allocate the file info earlier. However, if we do
                // this then we'll need to handle the "already moved" stuff too...

            DiskManagerFileInfoImpl file_info = pm_info.getFileInfo();

            boolean close_it    = false;

            try{
                if ( file_info == null ){

                    boolean linear = storage_types[i].equals("L");

                    file_info = new DiskManagerFileInfoImpl(
                                        this,
                                        new File( root_dir + relative_file.toString()),
                                        i,
                                        pm_info.getTorrentFile(),
                                        linear );

                    close_it    = true;
                }

                try{
                    CacheFile   cache_file  = file_info.getCacheFile();
                    File        data_file   = file_info.getFile(true);

                    if ( !cache_file.exists()){

                            // look for something sensible to report

                          File current = data_file;

                          while( !current.exists()){

                            File    parent = current.getParentFile();

                            if ( parent == null ){

                                break;

                            }else if ( !parent.exists()){

                                current = parent;

                            }else{

                                if ( parent.isDirectory()){

                                    errorMessage = current.toString() + " not found.";

                                }else{

                                    errorMessage = parent.toString() + " is not a directory.";
                                }

                                return( false );
                            }
                          }

                          errorMessage = data_file.toString() + " not found.";

                          return false;
                    }

                        // only test for too big as if incremental creation selected
                        // then too small is OK

                    long    existing_length = file_info.getCacheFile().getLength();

                    if ( existing_length > target_length ){

                        if ( COConfigurationManager.getBooleanParameter("File.truncate.if.too.large")){

                            file_info.setAccessMode( DiskManagerFileInfo.WRITE );

                            file_info.getCacheFile().setLength( target_length );

                            Debug.out( "Existing data file length too large [" +existing_length+ ">" +target_length+ "]: " + data_file.getAbsolutePath() + ", truncating" );

                        }else{

                            errorMessage = "Existing data file length too large [" +existing_length+ ">" +target_length+ "]: " + data_file.getAbsolutePath();

                            return false;
                        }
                    }
                }finally{

                    if ( close_it ){

                        file_info.getCacheFile().close();
                    }
                }
            }catch( Throwable e ){

                errorMessage = Debug.getNestedExceptionMessage(e) + " (filesExist:" + relative_file.toString() + ")";

                return( false );
            }
        }

        return true;
    }

    private int
    allocateFiles()
    {
        Set file_set    = new HashSet();

        DMPieceMapperFile[] pm_files = piece_mapper.getFiles();

        DiskManagerFileInfoImpl[] allocated_files = new DiskManagerFileInfoImpl[pm_files.length];

        try{
            allocation_scheduler.register( this );

            setState( ALLOCATING );

            allocated = 0;

            int numNewFiles = 0;

            String  root_dir = download_manager.getAbsoluteSaveLocation().getParent();

            if ( !torrent.isSimpleTorrent()){

                root_dir += File.separator + download_manager.getAbsoluteSaveLocation().getName();
            }

            root_dir    += File.separator;

            String[]    storage_types = getStorageTypes();

            for ( int i=0;i<pm_files.length;i++ ){

                final DMPieceMapperFile pm_info = pm_files[i];

                final long target_length = pm_info.getLength();

                File relative_data_file = pm_info.getDataFile();

                DiskManagerFileInfoImpl fileInfo;

                try{
                    boolean linear = storage_types[i].equals("L");

                    fileInfo = new DiskManagerFileInfoImpl(
                                    this,
                                    new File( root_dir + relative_data_file.toString()),
                                    i,
                                    pm_info.getTorrentFile(),
                                    linear );

                    allocated_files[i] = fileInfo;

                    pm_info.setFileInfo( fileInfo );

                }catch ( CacheFileManagerException e ){

                    this.errorMessage = Debug.getNestedExceptionMessage(e) + " (allocateFiles:" + relative_data_file.toString() + ")";

                    setState( FAULTY );

                    return( -1 );
                }

                CacheFile   cache_file      = fileInfo.getCacheFile();
                File        data_file       = fileInfo.getFile(true);
                String      data_file_name  = data_file.getName();

                String  file_key = data_file.getAbsolutePath();

                if ( Constants.isWindows ){

                    file_key = file_key.toLowerCase();
                }

                if ( file_set.contains( file_key )){

                    this.errorMessage = "File occurs more than once in download: " + data_file.toString();

                    setState( FAULTY );

                    return( -1 );
                }

                file_set.add( file_key );

                int separator = data_file_name.lastIndexOf(".");

                if ( separator == -1 ){

                    separator = 0;
                }

                fileInfo.setExtension(data_file_name.substring(separator));

                    //Added for Feature Request
                    //[ 807483 ] Prioritize .nfo files in new torrents
                    //Implemented a more general way of dealing with it.

                String extensions = COConfigurationManager.getStringParameter("priorityExtensions","");

                if(!extensions.equals("")) {
                    boolean bIgnoreCase = COConfigurationManager.getBooleanParameter("priorityExtensionsIgnoreCase");
                    StringTokenizer st = new StringTokenizer(extensions,";");
                    while(st.hasMoreTokens()) {
                        String extension = st.nextToken();
                        extension = extension.trim();
                        if(!extension.startsWith("."))
                            extension = "." + extension;
                        boolean bHighPriority = (bIgnoreCase) ?
                                              fileInfo.getExtension().equalsIgnoreCase(extension) :
                                              fileInfo.getExtension().equals(extension);
                        if (bHighPriority)
                            fileInfo.setPriority(true);
                    }
                }

                fileInfo.setLength(target_length);

                fileInfo.setDownloaded(0);

                if ( cache_file.exists() ){

                    try {

                        //make sure the existing file length isn't too large

                        long    existing_length = fileInfo.getCacheFile().getLength();

                        if(  existing_length > target_length ){

                            if ( COConfigurationManager.getBooleanParameter("File.truncate.if.too.large")){

                                fileInfo.setAccessMode( DiskManagerFileInfo.WRITE );

                                cache_file.setLength( target_length );

                                Debug.out( "Existing data file length too large [" +existing_length+ ">" +target_length+ "]: " +data_file.getAbsolutePath() + ", truncating" );

                            }else{

                                this.errorMessage = "Existing data file length too large [" +existing_length+ ">" +target_length+ "]: " + data_file.getAbsolutePath();

                                setState( FAULTY );

                                return( -1 );
                            }
                        }

                        fileInfo.setAccessMode( DiskManagerFileInfo.READ );

                    }catch (CacheFileManagerException e) {

                        this.errorMessage = Debug.getNestedExceptionMessage(e) +
                                                " (allocateFiles existing:" + data_file.getAbsolutePath() + ")";
                        setState( FAULTY );

                        return( -1 );
                    }

                    allocated += target_length;

                }else{  //we need to allocate it

                        //make sure it hasn't previously been allocated

                    if ( download_manager.isDataAlreadyAllocated() ){

                        this.errorMessage = "Data file missing: " + data_file.getAbsolutePath();

                        setState( FAULTY );

                        return( -1 );
                    }

                    while( started ){

                        if ( allocation_scheduler.getPermission( this )){

                            break;
                        }
                    }

                    if ( !started ){

                            // allocation interrupted

                        return( -1 );
                    }

                    try{
                        fileInfo.setAccessMode( DiskManagerFileInfo.WRITE );

                        if( COConfigurationManager.getBooleanParameter("Enable incremental file creation") ) {

                                //  do incremental stuff

                            fileInfo.getCacheFile().setLength( 0 );

                        }else {

                                //fully allocate

                            if( COConfigurationManager.getBooleanParameter("Zero New") ) {  //zero fill

                                if ( !writer.zeroFile( fileInfo, target_length )) {

                                    try{
                                            // failed to zero it, delete it so it gets done next start

                                        fileInfo.getCacheFile().close();

                                        fileInfo.getCacheFile().delete();

                                    }catch( Throwable e ){

                                    }

                                    setState( FAULTY );

                                    return( -1 );
                                }
                            }else{

                                    //reserve the full file size with the OS file system

                                fileInfo.getCacheFile().setLength( target_length );

                                allocated += target_length;
                            }
                        }
                    }catch ( Exception e ) {

                        this.errorMessage = Debug.getNestedExceptionMessage(e)
                                    + " (allocateFiles new:" + data_file.toString() + ")";

                        setState( FAULTY );

                        return( -1 );
                    }

                    numNewFiles++;
                }
            }

                // make sure that "files" doens't become visible to the rest of the world until all
                // entries have been populated

            files   = allocated_files;

            loadFilePriorities();

            download_manager.setDataAlreadyAllocated( true );

            return( numNewFiles );

        }finally{

            allocation_scheduler.unregister( this );

                // if we failed to do the allocation make sure we close all the files that
                // we might have opened

            if ( files == null ){

                for (int i=0;i<allocated_files.length;i++){

                    if ( allocated_files[i] != null ){

                        try{
                            allocated_files[i].getCacheFile().close();

                        }catch( Throwable e ){
                        }
                    }
                }
            }
        }
    }

    public DiskAccessController
    getDiskAccessController()
    {
        return( disk_access_controller );
    }

    public void
    enqueueReadRequest(
        DiskManagerReadRequest request,
        DiskManagerReadRequestListener listener )
    {
        reader.readBlock( request, listener );
    }


    public int
    getNbPieces()
    {
        return nbPieces;
    }

    public int
    getPercentDone()
    {
        return percentDone;
    }

    public void
    setPercentDone(
        int         num )
    {
        percentDone = num;
    }

    public long
    getRemaining() {
        return remaining;
    }

    public long
    getRemainingExcludingDND()
    {
        if ( skipped_file_set_changed ){

            DiskManagerFileInfoImpl[]   current_files = files;

            if ( current_files != null ){

                skipped_file_set_changed    = false;

                try{
                    file_piece_mon.enter();

                    skipped_file_set_size   = 0;
                    skipped_but_downloaded  = 0;

                    for (int i=0;i<current_files.length;i++){

                        DiskManagerFileInfoImpl file = current_files[i];

                        if ( file.isSkipped()){

                            skipped_file_set_size   += file.getLength();
                            skipped_but_downloaded  += file.getDownloaded();
                        }
                    }
                }finally{

                    file_piece_mon.exit();
                }
            }
        }

        long rem = ( remaining - ( skipped_file_set_size - skipped_but_downloaded ));

        if ( rem < 0 ){

            rem = 0;
        }

        return( rem );
    }

    public long
    getAllocated()
    {
        return( allocated );
    }

    public void
    setAllocated(
        long        num )
    {
        allocated   = num;
    }

    // called when status has CHANGED and should only be called by DiskManagerPieceImpl
    protected void
    setPieceDone(
        DiskManagerPieceImpl    dmPiece,
        boolean                 done )
    {
        int piece_number =dmPiece.getPieceNumber();
        int piece_length =dmPiece.getLength();
        DMPieceList piece_list =pieceMap[piece_number];

        try
        {
            file_piece_mon.enter();

            if (dmPiece.isDone() !=done)
            {
                dmPiece.setDoneSupport(done);

                if (done)
                    remaining -=piece_length;
                else
                    remaining +=piece_length;

                for (int i =0; i <piece_list.size(); i++)
                {

                    DMPieceMapEntry piece_map_entry =piece_list.get(i);

                    DiskManagerFileInfoImpl this_file =piece_map_entry.getFile();

                    long file_length =this_file.getLength();

                    long file_done =this_file.getDownloaded();

                    long file_done_before =file_done;

                    if (done)
                        file_done +=piece_map_entry.getLength();
                    else
                        file_done -=piece_map_entry.getLength();

                    if (file_done <0)
                    {
                        Debug.out("piece map entry length negative");

                        file_done =0;

                    } else if (file_done >file_length)
                    {
                        Debug.out("piece map entry length too large");

                        file_done =file_length;
                    }

                    if (this_file.isSkipped())
                    {
                        skipped_but_downloaded +=(file_done -file_done_before);
                    }

                    this_file.setDownloaded(file_done);

                    // change file modes based on whether or not the file is complete or not
                    if (file_done ==file_length &&this_file.getAccessMode() ==DiskManagerFileInfo.WRITE)
                    {
                        try
                        {
                            this_file.setAccessMode(DiskManagerFileInfo.READ);

                        } catch (Exception e)
                        {
                            setFailed("Disk access error - " +Debug.getNestedExceptionMessage(e));

                            Debug.printStackTrace(e);
                        }

                        // note - we don't set the access mode to write if incomplete as we may
                        // be rechecking a file and during this process the "file_done" amount
                        // will not be file_length until the end. If the file is read-only then
                        // changing to write will cause trouble!
                    }
                }
                listeners.dispatch(LDT_PIECE_DONE_CHANGED, dmPiece);
            }
        } finally
        {
            file_piece_mon.exit();
        }

    }

    public void
    accessModeChanged(
        DiskManagerFileInfoImpl     file,
        int                         old_mode,
        int                         new_mode )
    {
        listeners.dispatch(
            LDT_ACCESS_MODE_CHANGED,
            new Object[]{ file, new Integer(old_mode), new Integer(new_mode)});
    }

    public DiskManagerPiece[] getPieces()
    {
        return pieces;
    }

    public DiskManagerPiece getPiece(int PieceNumber)
    {
        return pieces[PieceNumber];
    }

    public int getPieceLength() {
        return pieceLength;
    }

    public long getTotalLength() {
        return totalLength;
    }

    public int getLastPieceLength() {
        return lastPieceLength;
    }

    public int getState() {
        return state_set_via_method;
    }

    protected void
    setState(
        int     _state )
    {
            // we never move from a faulty state

        if ( state_set_via_method == FAULTY ){

            if ( _state != FAULTY ){

                Debug.out( "DiskManager: attempt to move from faulty state to " + _state );
            }

            return;
        }

        if ( state_set_via_method != _state ){

            int params[] = {state_set_via_method, _state};

            state_set_via_method = _state;

            listeners.dispatch( LDT_STATECHANGED, params);
        }
    }


    public DiskManagerFileInfo[]
    getFiles()
    {
        return files;
    }


    private void
    constructFilesPieces()
    {
        for (int i = 0; i < pieceMap.length; i++) {
            DMPieceList pieceList = pieceMap[i];
            //for each piece

            for (int j = 0; j < pieceList.size(); j++) {
                //get the piece and the file
                DiskManagerFileInfoImpl fileInfo = (pieceList.get(j)).getFile();
                if (fileInfo.getFirstPieceNumber() == -1)
                    fileInfo.setFirstPieceNumber(i);
                fileInfo.setNbPieces(fileInfo.getNbPieces() + 1);
            }
        }
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void
    setFailed(
        final String        reason )
    {
            /**
             * need to run this on a separate thread to avoid deadlock with the stopping
             * process - setFailed tends to be called from within the read/write activities
             * and stopping these requires this.
             */

        new AEThread("DiskManager:setFailed")
        {
            public void
            runSupport()
            {
                errorMessage    = reason;

                Logger.log(new LogAlert(LogAlert.UNREPEATABLE, LogAlert.AT_ERROR,
                            errorMessage));


                setState( DiskManager.FAULTY );

                DiskManagerImpl.this.stop( false );
            }
        }.start();

    }

    public void
    setFailed(
        final DiskManagerFileInfo       file,
        final String                    reason )
    {
            /**
             * need to run this on a separate thread to avoid deadlock with the stopping
             * process - setFailed tends to be called from within the read/write activities
             * and stopping these requires this.
             */

        new AEThread("DiskManager:setFailed")
        {
            public void
            runSupport()
            {
                errorMessage    = reason;

                Logger.log(new LogAlert(LogAlert.UNREPEATABLE, LogAlert.AT_ERROR,
                        errorMessage));


                setState( DiskManager.FAULTY );

                DiskManagerImpl.this.stop( false );

                RDResumeHandler.recheckFile( download_manager, file );
            }
        }.start();

    }

    public DMPieceList getPieceList(int piece_number)
    {
        return (pieceMap[piece_number]);
    }

    public byte[]
    getPieceHash(
        int piece_number )

        throws TOTorrentException
    {
        return( torrent.getPieces()[ piece_number ]);
    }

    public DiskManagerReadRequest
    createReadRequest(
        int pieceNumber,
        int offset,
        int length )
    {
        return( reader.createRequest( pieceNumber, offset, length ));
    }

    public DiskManagerCheckRequest
    createCheckRequest(
        int     pieceNumber,
        Object  user_data )
    {
        return( checker.createRequest( pieceNumber, user_data ));
    }

    public void
    enqueueCompleteRecheckRequest(
        DiskManagerCheckRequest             request,
        DiskManagerCheckRequestListener     listener )

    {
        checker.enqueueCompleteRecheckRequest( request, listener );
    }

    public void
    enqueueCheckRequest(
        DiskManagerCheckRequest         request,
        DiskManagerCheckRequestListener listener )
    {
        checker.enqueueCheckRequest( request, listener );
    }

    public int getCompleteRecheckStatus()
    {
      return ( checker.getCompleteRecheckStatus());
    }

    public DirectByteBuffer
    readBlock(
        int pieceNumber,
        int offset,
        int length )
    {
        return( reader.readBlock( pieceNumber, offset, length ));
    }

    public DiskManagerWriteRequest
    createWriteRequest(
        int                 pieceNumber,
        int                 offset,
        DirectByteBuffer    data,
        Object              user_data )
    {
        return( writer.createWriteRequest( pieceNumber, offset, data, user_data ));
    }

    public void
    enqueueWriteRequest(
        DiskManagerWriteRequest         request,
        DiskManagerWriteRequestListener listener )
    {
        writer.writeBlock( request, listener );
    }


    public boolean
    checkBlockConsistency(
        int pieceNumber,
        int offset,
        DirectByteBuffer data )
    {
        if (pieceNumber < 0) {
            if (Logger.isEnabled())
                Logger.log(new LogEvent(this, LOGID, LogEvent.LT_ERROR,
                        "CHECKBLOCK1: pieceNumber=" + pieceNumber + " < 0"));
            return false;
        }
        if (pieceNumber >= this.nbPieces) {
            if (Logger.isEnabled())
                Logger.log(new LogEvent(this, LOGID, LogEvent.LT_ERROR,
                        "CHECKBLOCK1: pieceNumber=" + pieceNumber + " >= this.nbPieces="
                                + this.nbPieces));
            return false;
        }
        int length = this.pieceLength;
        if (pieceNumber == nbPieces - 1) {
            length = this.lastPieceLength;
        }
        if (offset < 0) {
            if (Logger.isEnabled())
                Logger.log(new LogEvent(this, LOGID, LogEvent.LT_ERROR,
                        "CHECKBLOCK1: offset=" + offset + " < 0"));
            return false;
        }
        if (offset > length) {
            if (Logger.isEnabled())
                Logger.log(new LogEvent(this, LOGID, LogEvent.LT_ERROR,
                        "CHECKBLOCK1: offset=" + offset + " > length=" + length));
            return false;
        }
        int size = data.remaining(DirectByteBuffer.SS_DW);
        if (size <= 0) {
            if (Logger.isEnabled())
                Logger.log(new LogEvent(this, LOGID, LogEvent.LT_ERROR,
                        "CHECKBLOCK1: size=" + size + " <= 0"));
            return false;
        }
        if (offset + size > length) {
            if (Logger.isEnabled())
                Logger.log(new LogEvent(this, LOGID, LogEvent.LT_ERROR,
                        "CHECKBLOCK1: offset=" + offset + " + size=" + size + " > length="
                                + length));
            return false;
        }
        return true;
    }

    public boolean
    checkBlockConsistency(
        int pieceNumber,
        int offset,
        int length)
    {
        if (length > max_read_block_size) {
            if (Logger.isEnabled())
                Logger.log(new LogEvent(this, LOGID, LogEvent.LT_ERROR,
                        "CHECKBLOCK2: length=" + length + " > " + max_read_block_size));
          return false;
        }
        if (length <= 0 ) {
            if (Logger.isEnabled())
                Logger.log(new LogEvent(this, LOGID, LogEvent.LT_ERROR,
                        "CHECKBLOCK2: length=" + length + " <= 0"));
            return false;
        }
        if (pieceNumber < 0) {
            if (Logger.isEnabled())
                Logger.log(new LogEvent(this, LOGID, LogEvent.LT_ERROR,
                        "CHECKBLOCK2: pieceNumber=" + pieceNumber + " < 0"));
          return false;
        }
        if (pieceNumber >= this.nbPieces) {
            if (Logger.isEnabled())
                Logger.log(new LogEvent(this, LOGID, LogEvent.LT_ERROR,
                        "CHECKBLOCK2: pieceNumber=" + pieceNumber + " >= this.nbPieces="
                                + this.nbPieces));
          return false;
        }
        int pLength = this.pieceLength;
        if (pieceNumber == this.nbPieces - 1)
            pLength = this.lastPieceLength;
        if (offset < 0) {
            if (Logger.isEnabled())
                Logger.log(new LogEvent(this, LOGID, LogEvent.LT_ERROR,
                        "CHECKBLOCK2: offset=" + offset + " < 0"));
          return false;
        }
        if (offset > pLength) {
            if (Logger.isEnabled())
                Logger.log(new LogEvent(this, LOGID, LogEvent.LT_ERROR,
                        "CHECKBLOCK2: offset=" + offset + " > pLength=" + pLength));
          return false;
        }
        if (offset + length > pLength) {
            if (Logger.isEnabled())
                Logger.log(new LogEvent(this, LOGID, LogEvent.LT_ERROR,
                        "CHECKBLOCK2: offset=" + offset + " + length=" + length
                                + " > pLength=" + pLength));
          return false;
        }

        if(!pieces[pieceNumber].isDone()) {
            Logger.log(new LogEvent(this, LOGID, LogEvent.LT_ERROR,
                "CHECKBLOCK2: piece #" + pieceNumber + " not done"));
          return false;
        }
        return true;
    }


    public void
    saveResumeData(
        boolean interim_save )

        throws Exception
    {
        resume_handler.saveResumeData( interim_save );
    }

  /**
   * Moves files to the CompletedFiles directory.
   * Returns a string path to the new torrent file.
   */

    public static class MoveDownloadInfo {
        public String location = null;
        public boolean move_torrent = false;
    }

    public static MoveDownloadInfo getMoveDownloadInfoOnCompletion(
        DownloadManager dm, LogRelation log_object) {
        return getMoveDownloadInfo(false, dm, log_object);
    }

    public static MoveDownloadInfo getMoveDownloadInfoOnRemoval(
        DownloadManager dm, LogRelation log_object) {
        return getMoveDownloadInfo(true, dm, log_object);
    }

    private static MoveDownloadInfo getMoveDownloadInfo(
        final boolean removing, final DownloadManager dm,
        final LogRelation log_object) {
    	
        // We don't move non-persistent files as these aren't managed by us.
        if (!dm.isPersistent()) {return null;}
        final boolean ending = !removing; // Just a friendly alias.

        /**
         *  We currently only move removed torrents when they are complete. So we must either
         *  be complete, or we must be complete excluding DND files, if that option has been
         *  enabled.
         */
        if (removing) {
            if (dm.isDownloadComplete(true)) {
                /* This is OK. */
            }
            else if (!dm.isDownloadComplete(false)) {
                // Download is not complete in any form.
                return null;
            }
            else {
                // Download is complete, but only all non-DND files.
                if (!COConfigurationManager.getBooleanParameter("File.move.download.removed.move_partial", false)) {
                    return null;
                }
            }
        }

        /**
         * Is the automatic moving of files enabled?
         */
        String move_when_done_param = (ending) ? "Move Completed When Done" : "File.move.download.removed.enabled";
        boolean move_when_done = COConfigurationManager.getBooleanParameter(move_when_done_param, false);
        if (!move_when_done) {
        	return null;
        }

        /**
         * Do we have a path to move to?
         */
        String move_path_param = (ending) ? "Completed Files Directory" : "File.move.download.removed.path";
        String move_path = COConfigurationManager.getStringParameter(move_path_param, "");
        if (move_path.length() == 0) {
        	return null;
        }

        /**
         * Do we only move if the files are in the default directory?
         *
         * If we are trying to move a completed download, then we only check
         * the default save path directory. If we are checking a removed
         * download, then we either check the default save path, or the
         * completed file path.
         */
        String move_only_in_default_param = (ending) ? "Move Only When In Default Save Dir" : "File.move.download.removed.only_in_default";
        boolean move_only_in_default = COConfigurationManager.getBooleanParameter(move_only_in_default_param, true);

        String save_dir = dm.getSaveLocation().getParent();

        if (move_only_in_default) {
            boolean use_completed_dir = removing;
            String[] params_to_check = (use_completed_dir) ?
            		new String[] {"Completed Files Directory", "Default save path"} :
            		new String[] {"Default save path"};
            
            String dir_to_check = null;
            boolean dir_matches = false;
            for (int i=0; i<params_to_check.length; i++) {
            	dir_to_check = COConfigurationManager.getStringParameter(params_to_check[i]);

	            /**
	             * Canonicalise is as the rpath is canonicalised so links have been
	             * followed etc.
	             */
	            try{
	            	dir_to_check = new File(dir_to_check).getCanonicalPath();
	            }
	
	            // XXX: Shouldn't we restrict the types of exception we catch?
	            catch(Throwable e){Debug.printStackTrace(e);}
	            
	            if (save_dir.equals(dir_to_check)) {
	            	dir_matches = true;
	            	break;
	            }
	            
            }
            
            if (!dir_matches) {
                if (Logger.isEnabled()) {
                    String log_message = "Not moving-on-" +
                    ((removing) ? "remove" : "complete") + " since data is" +
                    " not within " + use_completed_dir;

                    Logger.log(new LogEvent(
                        log_object, LOGID, LogEvent.LT_WARNING, log_message));
                }
                return null;
            }
        }

        String move_torrent_param = (ending) ? "Move Torrent When Done" : "File.move.download.removed.move_torrent";
        boolean move_torrent = COConfigurationManager.getBooleanParameter(move_torrent_param, true);

        // Debug.out("Moving data files: " + save_dir + " -> " + move_path);
        MoveDownloadInfo result = new MoveDownloadInfo();
        result.location = move_path;
        result.move_torrent = move_torrent;
        return result;
    }

    public void downloadEnded() {
        moveDownloadFilesWhenEndedOrRemoved(false, true);
    }

    public void downloadRemoved () {
        moveDownloadFilesWhenEndedOrRemoved(true, true);
    }

    private boolean moveDownloadFilesWhenEndedOrRemoved(final boolean removing, final boolean torrent_file_exists) {
      try {
        start_stop_mon.enter();
        final boolean ending = !removing; // Just a friendly alias.

        /**
         * It doesn't matter if we set alreadyMoved, but don't end up moving the files.
         * This is because we only get called once (when it matters), which is when the
         * download has finished. We only want this to apply when the download has finished,
         * not if the user restarts the (already completed) download.
         */
        if (ending) {
            if (this.alreadyMoved) {return false;}
            this.alreadyMoved = true;
        }

        MoveDownloadInfo mdi = getMoveDownloadInfo(removing, this.download_manager, this);

        if (mdi == null) {return false;}

        //Debug.out("Moving data files: -> " + mdi.location);
        moveFiles(mdi.location, mdi.move_torrent && torrent_file_exists, true);
        return true;

      }
      finally{
          start_stop_mon.exit();
          if (!removing) {
              try{
                  saveResumeData(false);
              }catch( Throwable e ){
                  setFailed("Resume data save fails: " + Debug.getNestedExceptionMessage(e));
              }
          }

      }
    }


    public void
    moveDataFiles(
        File    new_parent_dir )
    {
        moveFiles( new_parent_dir.toString(), false, false );
    }

    protected void moveFiles(String move_to_dir, boolean move_torrent, boolean change_to_read_only) {

        boolean move_files = !isFileDestinationIsItself(move_to_dir);
        try {
            start_stop_mon.enter();

            /**
             * The 0 suffix is indicate that these are quite internal, and are
             * only intended for use within this method.
             */
            boolean files_moved = true;
            if (move_files) {
                files_moved = moveDataFiles0(move_to_dir, change_to_read_only);
            }

            if (move_torrent && files_moved) {
                moveTorrentFile0(move_to_dir);
            }
        }
        catch(Exception e) {
            Debug.printStackTrace(e);
        }
        finally{

        start_stop_mon.exit();
    }
  }

  private boolean isFileDestinationIsItself(String move_to_dir) {

        File save_location = download_manager.getAbsoluteSaveLocation();
        String move_from_dir = save_location.getParent();

         // sanity check - never move a dir into itself
        try{
            File from_file = new File(move_from_dir).getCanonicalFile();
            File to_file   = new File(move_to_dir).getCanonicalFile();

            save_location   = save_location.getCanonicalFile();

            move_from_dir   = from_file.getPath();
            move_to_dir     = to_file.getPath();

            if (from_file.equals(to_file)){
                return true;
            }else{

                if ( !download_manager.getTorrent().isSimpleTorrent()){

                    if ( to_file.getPath().startsWith( save_location.getPath())){

                        String msg = "Target is sub-directory of files";

                        Logger.log(new LogEvent(this, LOGID, LogEvent.LT_ERROR, msg));

                        Logger.logTextResource(new LogAlert(LogAlert.REPEATABLE,
                                        LogAlert.AT_ERROR, "DiskManager.alert.movefilefails"),
                                        new String[] {save_location.toString(), msg });

                        return true;
                    }
                }
            }

        }catch( Throwable e ){

                // carry on

            Debug.out(e);
        }
        return false;
    }

    private boolean moveDataFiles0(String move_to_dir, boolean change_to_read_only) throws Exception {
        File save_location = download_manager.getAbsoluteSaveLocation();
        String move_from_dir = save_location.getParent();

         // sanity check - never move a dir into itself
        try{
            File from_file = new File(move_from_dir).getCanonicalFile();
            File to_file   = new File(move_to_dir).getCanonicalFile();

            save_location   = save_location.getCanonicalFile();

            move_from_dir   = from_file.getPath();
            move_to_dir     = to_file.getPath();

            if (from_file.equals(to_file)){
                return true;
            }else{

                if ( !download_manager.getTorrent().isSimpleTorrent()){

                    if ( to_file.getPath().startsWith( save_location.getPath())){

                        String msg = "Target is sub-directory of files";

                        Logger.log(new LogEvent(this, LOGID, LogEvent.LT_ERROR, msg));

                        Logger.logTextResource(new LogAlert(LogAlert.REPEATABLE,
                                        LogAlert.AT_ERROR, "DiskManager.alert.movefilefails"),
                                        new String[] {save_location.toString(), msg });

                        return true;
                    }
                }
            }

        }catch( Throwable e ){

                // carry on

            Debug.out(e);
        }

        // first of all check that no destination files already exist
        File[]    new_files   = new File[files.length];
        File[]    old_files   = new File[files.length];
        boolean[] link_only   = new boolean[files.length];

        for (int i=0; i < files.length; i++) {

            File old_file = files[i].getFile(false);

            File linked_file = FMFileManagerFactory.getSingleton().getFileLink( torrent, old_file );

            if ( linked_file != old_file ){

                if ( save_location.isDirectory()){

                      // if we are linked to a file outside of the torrent's save directory then we don't
                      // move the file

                    if ( linked_file.getCanonicalPath().startsWith( save_location.getCanonicalPath())){

                        old_file  = linked_file;

                    }else{

                        link_only[i] = true;
                    }
                }else{

                      // simple torrent, only handle a link if its a simple rename

                    if ( linked_file.getParentFile().getCanonicalPath().equals( save_location.getParentFile().getCanonicalPath())){

                        old_file  = linked_file;

                    }else{

                        link_only[i] = true;
                    }
                }
            }

            old_files[i]  = old_file;

              //get old file's parent path

            String fullPath = old_file.getParent();

              //compute the file's sub-path off from the default save path

            String subPath = fullPath.substring(fullPath.indexOf(move_from_dir) + move_from_dir.length());

              //create the destination dir

            if ( subPath.startsWith( File.separator )){

                subPath = subPath.substring(1);
            }

            File destDir = new File(move_to_dir, subPath);

              //create the destination file pointer

            File newFile = new File(destDir, old_file.getName());

            new_files[i]  = newFile;

            if ( !link_only[i] ){

                if ( newFile.exists()){

                    String msg = "" + linked_file.getName() + " already exists in MoveTo destination dir";

                    Logger.log(new LogEvent(this, LOGID, LogEvent.LT_ERROR, msg));

                    Logger.logTextResource(new LogAlert(LogAlert.REPEATABLE,
                              LogAlert.AT_ERROR, "DiskManager.alert.movefileexists"),
                              new String[] { old_file.getName() });


                    Debug.out(msg);

                    return false;
                }

                destDir.mkdirs();
            }
        }

        for (int i=0; i < files.length; i++){

            File new_file = new_files[i];

            try{

              files[i].moveFile( new_file, link_only[i] );

              if ( change_to_read_only ){

                  files[i].setAccessMode(DiskManagerFileInfo.READ);
              }

            }catch( CacheFileManagerException e ){

              String msg = "Failed to move " + old_files[i].toString() + " to destination dir";

              Logger.log(new LogEvent(this, LOGID, LogEvent.LT_ERROR, msg));

              Logger.logTextResource(new LogAlert(LogAlert.REPEATABLE,
                              LogAlert.AT_ERROR, "DiskManager.alert.movefilefails"),
                              new String[] { old_files[i].toString(),
                                      Debug.getNestedExceptionMessage(e) });

                  // try some recovery by moving any moved files back...

              for (int j=0;j<i;j++){

                  try{
                      files[j].moveFile( old_files[j],  link_only[j]);

                  }catch( CacheFileManagerException f ){

                      Logger.logTextResource(new LogAlert(LogAlert.REPEATABLE,
                                      LogAlert.AT_ERROR,
                                      "DiskManager.alert.movefilerecoveryfails"),
                                      new String[] { old_files[j].toString(),
                                              Debug.getNestedExceptionMessage(f) });

                  }
              }

              return false;
            }
        }

              //remove the old dir

            if (  save_location.isDirectory()){

                TorrentUtils.recursiveEmptyDirDelete( save_location, false );
            }

              // NOTE: this operation FIXES up any file links

            download_manager.setTorrentSaveDir( move_to_dir );

        return true;

    }

    private void moveTorrentFile0(String move_to_dir) throws Exception {
              String oldFullName = download_manager.getTorrentFileName();

              File oldTorrentFile = new File(oldFullName);

              if ( oldTorrentFile.exists()){

                  String oldFileName = oldTorrentFile.getName();

                  File newTorrentFile = new File(move_to_dir, oldFileName);

                  if (!newTorrentFile.equals(oldTorrentFile)){

                    if ( TorrentUtils.move( oldTorrentFile, newTorrentFile )){

                        download_manager.setTorrentFileName(newTorrentFile.getCanonicalPath());

                    }else{

                        String msg = "Failed to move " + oldTorrentFile.toString() + " to " + newTorrentFile.toString();

                        if (Logger.isEnabled())
                            Logger.log(new LogEvent(this, LOGID, LogEvent.LT_ERROR, msg));

                        Logger.logTextResource(new LogAlert(LogAlert.REPEATABLE,
                                        LogAlert.AT_ERROR, "DiskManager.alert.movefilefails"),
                                        new String[] { oldTorrentFile.toString(),
                                                newTorrentFile.toString() });

                        Debug.out(msg);
                    }
                  }
              }else{
                    // torrent file's been removed in the meantime, just log a warning

                  if (Logger.isEnabled())
                        Logger.log(new LogEvent(this, LOGID, LogEvent.LT_WARNING, "Torrent file '" + oldFullName + "' has been deleted, move operation ignored" ));

              }
          }

    public TOTorrent
    getTorrent()
    {
        return( torrent );
    }


    public void
    addListener(
        DiskManagerListener l )
    {
        listeners.addListener( l );

        int params[] = {getState(), getState()};

        listeners.dispatch( l, LDT_STATECHANGED, params);
    }

    public void
    removeListener(
        DiskManagerListener l )
    {
        listeners.removeListener(l);
    }

          /** Deletes all data files associated with torrent.
           * Currently, deletes all files, then tries to delete the path recursively
           * if the paths are empty.  An unexpected result may be that a empty
           * directory that the user created will be removed.
           *
           * TODO: only remove empty directories that are created for the torrent
           */

    public static void
    deleteDataFiles(
        TOTorrent   torrent,
        String      torrent_save_dir,       // enclosing dir, not for deletion
        String      torrent_save_file )     // file or dir for torrent
    {
        if (torrent == null || torrent_save_file == null ){

            return;
        }

        try{
            if (torrent.isSimpleTorrent()){

                File    target = new File( torrent_save_dir, torrent_save_file );

                target = FMFileManagerFactory.getSingleton().getFileLink( torrent, target.getCanonicalFile());

                FileUtil.deleteWithRecycle( target );

            }else{

                PlatformManager mgr = PlatformManagerFactory.getPlatformManager();
                if( Constants.isOSX &&
                      torrent_save_file.length() > 0 &&
                      COConfigurationManager.getBooleanParameter("Move Deleted Data To Recycle Bin" ) &&
                      mgr.hasCapability(PlatformManagerCapabilities.RecoverableFileDelete) ) {

                    try
                    {
                        String  dir = torrent_save_dir + File.separatorChar + torrent_save_file + File.separatorChar;

                            // only delete the dir if there's only this torrent's files in it!

                        if ( countFiles( new File(dir)) == countDataFiles( torrent, torrent_save_dir, torrent_save_file )){

                            mgr.performRecoverableFileDelete( dir );

                        }else{

                            deleteDataFileContents( torrent, torrent_save_dir, torrent_save_file );
                    }
                    }
                    catch(PlatformManagerException ex)
                    {
                        deleteDataFileContents( torrent, torrent_save_dir, torrent_save_file );
                    }
                }
                else{
                    deleteDataFileContents(torrent, torrent_save_dir, torrent_save_file);
                }

            }
        }catch( Throwable e ){

            Debug.printStackTrace( e );
        }
    }

    private static int
    countFiles(
        File    f )
    {
        if ( f.isFile()){

            return( 1 );
        }else{

            int res = 0;

            File[]  files = f.listFiles();

            if ( files != null ){

                for (int i=0;i<files.length;i++){

                    res += countFiles( files[i] );
                }
            }

            return( res );
        }
    }

    private static int
    countDataFiles(
        TOTorrent torrent,
        String torrent_save_dir,
        String torrent_save_file )
    {
        try{
            int res = 0;

            LocaleUtilDecoder locale_decoder = LocaleTorrentUtil.getTorrentEncoding( torrent );

            TOTorrentFile[] files = torrent.getFiles();

            for (int i=0;i<files.length;i++){

                byte[][]path_comps = files[i].getPathComponents();

                String  path_str = torrent_save_dir + File.separator + torrent_save_file + File.separator;

                for (int j=0;j<path_comps.length;j++){

                    String comp = locale_decoder.decodeString( path_comps[j] );

                    comp = FileUtil.convertOSSpecificChars( comp );

                    path_str += (j==0?"":File.separator) + comp;
                }

                File file = new File(path_str).getCanonicalFile();

                File linked_file = FMFileManagerFactory.getSingleton().getFileLink( torrent, file );

                boolean skip = false;

                if ( linked_file != file ){

                    if ( !linked_file.getCanonicalPath().startsWith(new File( torrent_save_dir ).getCanonicalPath())){

                        skip = true;
                    }
                }

                if ( !skip && file.exists() && !file.isDirectory()){

                    res++;
                }
            }

            return( res );

        }catch( Throwable e ){

            Debug.printStackTrace(e);

            return( -1 );
        }
    }

    private static void
    deleteDataFileContents(
        TOTorrent torrent,
        String torrent_save_dir,
        String torrent_save_file )

            throws TOTorrentException, UnsupportedEncodingException, LocaleUtilEncodingException
    {
        LocaleUtilDecoder locale_decoder = LocaleTorrentUtil.getTorrentEncoding( torrent );

        TOTorrentFile[] files = torrent.getFiles();

        String  root_path = torrent_save_dir + File.separator + torrent_save_file + File.separator;

        // delete all files, then empty directories

        for (int i=0;i<files.length;i++){

            byte[][]path_comps = files[i].getPathComponents();

            String  path_str    = root_path;

            for (int j=0;j<path_comps.length;j++){

                try{

                    String comp = locale_decoder.decodeString( path_comps[j] );

                    comp = FileUtil.convertOSSpecificChars( comp );

                    path_str += (j==0?"":File.separator) + comp;

                }catch( UnsupportedEncodingException e ){

                    Debug.out( "file - unsupported encoding!!!!");
                }
            }

            File file = new File(path_str);

            File linked_file = FMFileManagerFactory.getSingleton().getFileLink( torrent, file );

            boolean delete;

            if ( linked_file == file ){

                delete  = true;

            }else{

                    // only consider linked files for deletion if they are in the torrent save dir
                    // i.e. a rename probably instead of a retarget to an existing file elsewhere

                try{
                    if ( linked_file.getCanonicalPath().startsWith(new File( root_path ).getCanonicalPath())){

                        file    = linked_file;

                        delete  = true;

                    }else{

                        delete = false;
                    }
                }catch( Throwable e ){

                    Debug.printStackTrace(e);

                    delete = false;
                }
            }

            if ( delete && file.exists() && !file.isDirectory()){

                try{
                    FileUtil.deleteWithRecycle( file );

                }catch (Exception e){

                    Debug.out(e.toString());
                }
            }
        }

        TorrentUtils.recursiveEmptyDirDelete(new File( torrent_save_dir, torrent_save_file ));
    }

    public void
    skippedFileSetChanged(
        DiskManagerFileInfo file )
    {
        skipped_file_set_changed    = true;
        listeners.dispatch(LDT_PRIOCHANGED, file);
    }

    public void
    priorityChanged(
        DiskManagerFileInfo file )
    {
        listeners.dispatch(LDT_PRIOCHANGED, file);
    }

  private void
  loadFilePriorities()
  {
      loadFilePriorities( download_manager, files );
  }

  private static void
  loadFilePriorities(
    DownloadManager         download_manager,
    DiskManagerFileInfo[]   files )
  {
    //  TODO: remove this try/catch.  should only be needed for those upgrading from previous snapshot
    try {
        if ( files == null ) return;
        List file_priorities = (List)download_manager.getData( "file_priorities" );
        if ( file_priorities == null ) return;
        for (int i=0; i < files.length; i++) {
            DiskManagerFileInfo file = files[i];
            if (file == null) return;
            int priority = ((Long)file_priorities.get( i )).intValue();
            if ( priority == 0 ) file.setSkipped( true );
            else if (priority == 1) file.setPriority( true );
        }
    }
    catch (Throwable t) {Debug.printStackTrace( t );}
  }

  protected void
  storeFilePriorities()
  {
      storeFilePriorities( download_manager, files );
  }

  protected static void
  storeFilePriorities(
    DownloadManager         download_manager,
    DiskManagerFileInfo[]   files )
  {
    if ( files == null ) return;
    List file_priorities = new ArrayList();
    for (int i=0; i < files.length; i++) {
      DiskManagerFileInfo file = files[i];
      if (file == null) return;
      boolean skipped = file.isSkipped();
      boolean priority = file.isPriority();
      int value = -1;
      if ( skipped ) value = 0;
      else if ( priority ) value = 1;
      file_priorities.add( i, new Long(value));
    }
    download_manager.setData( "file_priorities", file_priorities );
  }

  protected static void
  storeFileDownloaded(
    DownloadManager         download_manager,
    DiskManagerFileInfo[]   files )
  {
      DownloadManagerState  state = download_manager.getDownloadState();

      Map   details = new HashMap();

      List  downloaded = new ArrayList();

      details.put( "downloaded", downloaded );

      for (int i=0;i<files.length;i++){

          downloaded.add( new Long( files[i].getDownloaded()));
      }

      state.setMapAttribute( DownloadManagerState.AT_FILE_DOWNLOADED, details );

      state.save();
  }

  protected static void
  loadFileDownloaded(
    DownloadManager             download_manager,
    DiskManagerFileInfoHelper[] files )
  {
      DownloadManagerState  state = download_manager.getDownloadState();

      Map   details = state.getMapAttribute( DownloadManagerState.AT_FILE_DOWNLOADED );

      if ( details == null ){

          return;
      }

      List  downloaded = (List)details.get( "downloaded" );

      if ( downloaded == null ){

          return;
      }

      try{
          for (int i=0;i<files.length;i++){

              files[i].setDownloaded(((Long)downloaded.get(i)).longValue());
          }
     }catch( Throwable e ){

         Debug.printStackTrace(e);
     }

  }

  public void
  saveState()
  {
      if ( files != null ){

        storeFileDownloaded( download_manager, files );

        storeFilePriorities();
    }
  }

  public DownloadManager getDownloadManager() {
    return download_manager;
  }

    public String
    getInternalName()
    {
        return( download_manager.getInternalName());
    }

    public DownloadManagerState
    getDownloadState()
    {
        return( download_manager.getDownloadState());
    }

    public File
    getSaveLocation()
    {
        return( download_manager.getSaveLocation());
    }

    public String[]
    getStorageTypes()
    {
        return( getStorageTypes( download_manager ));
    }

    protected static String[]
    getStorageTypes(
        DownloadManager     download_manager )
    {
        DownloadManagerState    state = download_manager.getDownloadState();

        String[]    types = state.getListAttribute( DownloadManagerState.AT_FILE_STORE_TYPES );

        if ( types.length == 0 ){

            types = new String[download_manager.getTorrent().getFiles().length];

            for (int i=0;i<types.length;i++){

                types[i] = "L";
            }
        }

        return( types );
    }

    protected static boolean
    setFileLink(
        DownloadManager         download_manager,
        DiskManagerFileInfo[]   info,
        DiskManagerFileInfo     file_info,
        File                    from_file,
        File                    to_link )
    {
            // existing link is that for the TO_LINK and will come back as TO_LINK if no link is defined

        File    existing_link = FMFileManagerFactory.getSingleton().getFileLink( download_manager.getTorrent(), to_link );

        if ( !existing_link.equals( to_link )){

                // where we're mapping to is already linked somewhere else. Only case we support
                // is where this is a remapping of the same file back to where it came from

            if ( !from_file.equals( to_link )){

                Logger.log(new LogAlert(LogAlert.REPEATABLE, LogAlert.AT_ERROR,
                                "Attempt to link to existing link '" + existing_link.toString()
                                        + "'"));

                return( false );
            }
        }

        File    existing_file = file_info.getFile( true );

        if ( to_link.equals( existing_file )){

                // already pointing to the right place

            return( true );
        }

        for (int i=0;i<info.length;i++){

            if ( to_link.equals( info[i].getFile( true ))){

                Logger.log(new LogAlert(LogAlert.REPEATABLE, LogAlert.AT_ERROR,
                                "Attempt to link to existing file '" + info[i].getFile(true)
                                        + "'"));

                return( false );
            }
        }

        if ( to_link.exists()){

            if ( !existing_file.exists()){

                    // using a new file, make sure we recheck

                download_manager.recheckFile( file_info );

            }else{

                if ( FileUtil.deleteWithRecycle( existing_file )){

                        // new file, recheck

                    download_manager.recheckFile( file_info );

                }else{

                    Logger.log(new LogAlert(LogAlert.REPEATABLE, LogAlert.AT_ERROR,
                            "Failed to delete '" + existing_file.toString() + "'"));

                    return( false );
                }
            }
        }else{

            if ( existing_file.exists()){

                if ( !FileUtil.renameFile( existing_file, to_link )){

                    Logger.log(new LogAlert(LogAlert.REPEATABLE, LogAlert.AT_ERROR,
                        "Failed to rename '" + existing_file.toString() + "'" ));

                    return( false );
                }
            }
        }

        DownloadManagerState    state = download_manager.getDownloadState();

        state.setFileLink( from_file, to_link );

        state.save();

        return( true );
    }

    public static DiskManagerFileInfo[]
    getFileInfoSkeleton(
        final DownloadManager       download_manager,
        final DiskManagerListener   listener )
    {
        TOTorrent   torrent = download_manager.getTorrent();

        if ( torrent == null ){

            return( new DiskManagerFileInfo[0]);
        }

        String  root_dir = download_manager.getAbsoluteSaveLocation().getParent();

        if ( !torrent.isSimpleTorrent()){

            root_dir += File.separator + download_manager.getAbsoluteSaveLocation().getName();
        }

        root_dir    += File.separator;

        try{
            LocaleUtilDecoder locale_decoder = LocaleTorrentUtil.getTorrentEncoding( torrent );

            TOTorrentFile[] torrent_files = torrent.getFiles();

            long    piece_size  = torrent.getPieceLength();
            long    size_so_far = 0;

            final DiskManagerFileInfoHelper[]   res = new DiskManagerFileInfoHelper[ torrent_files.length ];

            for (int i=0;i<res.length;i++){

                final TOTorrentFile torrent_file    = torrent_files[i];

                final int file_index = i;

                long    file_length = torrent_file.getLength();

                final int   first_piece = (int)(size_so_far/piece_size);

                size_so_far += file_length;

                final int   last_piece  = (int)((size_so_far-1)/piece_size);

                String  path_str = root_dir + File.separator;

                    // for a simple torrent the target file can be changed

                if ( torrent.isSimpleTorrent()){

                    path_str = path_str + download_manager.getAbsoluteSaveLocation().getName();

                }else{
                    byte[][]path_comps = torrent_file.getPathComponents();

                    for (int j=0;j<path_comps.length;j++){

                        String comp = locale_decoder.decodeString( path_comps[j] );

                        comp = FileUtil.convertOSSpecificChars( comp );

                        path_str += (j==0?"":File.separator) + comp;
                    }
                }

                final File      data_file   = new File( path_str );

                final String    data_name   = data_file.getName();

                int separator = data_name.lastIndexOf(".");

                if (separator == -1){

                    separator = 0;
                }

                final String    data_extension  = data_name.substring(separator);

                DiskManagerFileInfoHelper   info =
                    new DiskManagerFileInfoHelper()
                    {
                        private boolean priority;
                        private boolean skipped;
                        private long    downloaded;

                        private CacheFile   read_cache_file;

                        public void
                        setPriority(boolean b)
                        {
                            priority    = b;

                            storeFilePriorities( download_manager, res );

                            listener.filePriorityChanged( this );
                        }

                        public void
                        setSkipped(boolean _skipped)
                        {
                            if ( !_skipped && getStorageType() == ST_COMPACT ){

                                if ( !setStorageType( ST_LINEAR )){

                                    return;
                                }
                            }

                            skipped = _skipped;

                            storeFilePriorities( download_manager, res );

                            listener.filePriorityChanged( this );
                        }

                        public int
                        getAccessMode()
                        {
                            return( READ );
                        }

                        public long
                        getDownloaded()
                        {
                            return( downloaded );
                        }

                        public void
                        setDownloaded(
                            long    l )
                        {
                            downloaded  = l;
                        }

                        public String
                        getExtension()
                        {
                            return( data_extension );
                        }

                        public int
                        getFirstPieceNumber()
                        {
                            return( first_piece );
                        }

                        public int
                        getLastPieceNumber()
                        {
                            return( last_piece );
                        }

                        public long
                        getLength()
                        {
                            return( torrent_file.getLength());
                        }

                        public int
                        getIndex()
                        {
                            return( file_index );
                        }

                        public int
                        getNbPieces()
                        {
                            return( last_piece - first_piece + 1 );
                        }

                        public boolean
                        isPriority()
                        {
                            return( priority );
                        }

                        public boolean
                        isSkipped()
                        {
                            return( skipped );
                        }

                        public DiskManager
                        getDiskManager()
                        {
                            return( null );
                        }

                        public DownloadManager
                        getDownloadManager()
                        {
                            return( download_manager );
                        }

                        public File
                        getFile(
                            boolean follow_link )
                        {
                            if ( follow_link ){

                                File link = getLink();

                                if ( link != null ){

                                    return( link );
                                }
                            }
                            return( data_file );
                        }

                        public TOTorrentFile
                        getTorrentFile()
                        {
                            return( torrent_file );
                        }

                        public boolean
                        setLink(
                            File    link_destination )
                        {
                            return( setFileLink( download_manager, res, this, data_file, link_destination ));
                        }

                        public File
                        getLink()
                        {
                            return( download_manager.getDownloadState().getFileLink( data_file ));
                        }

                        public boolean
                        setStorageType(
                            int     type )
                        {
                            String[]    types = getStorageTypes( download_manager );

                            int old_type = types[file_index].equals( "L")?ST_LINEAR:ST_COMPACT;

                            if ( type == old_type ){

                                return( true );
                            }

                            boolean set_skipped = false;

                            try{
                                File    target_file = getFile( true );

                                    // if the file doesn't exist then this is the start-of-day, most likely
                                    // being called from the torrent-opener, so we don't need to do any
                                    // file fiddling (in fact, if we do, we end up leaving zero length
                                    // files for dnd files which then force a recheck when the download
                                    // starts for the first time)

                                if ( target_file.exists()){

                                    CacheFile cache_file =
                                        CacheFileManagerFactory.getSingleton().createFile(
                                                new CacheFileOwner()
                                                {
                                                    public String
                                                    getCacheFileOwnerName()
                                                    {
                                                        return( download_manager.getInternalName());
                                                    }

                                                    public TOTorrentFile
                                                    getCacheFileTorrentFile()
                                                    {
                                                        return( torrent_file );
                                                    }

                                                    public File
                                                    getCacheFileControlFile(String name)
                                                    {
                                                        return( download_manager.getDownloadState().getStateFile( name ));
                                                    }
                                                },
                                                target_file,
                                                type==ST_LINEAR?CacheFile.CT_LINEAR:CacheFile.CT_COMPACT );

                                    cache_file.close();

                                    set_skipped = type == ST_COMPACT && !isSkipped();

                                        // download's not running, update resume data as necessary

                                    int cleared = RDResumeHandler.storageTypeChanged( download_manager, this );

                                        // try and maintain reasonable figures for downloaded. Note that because
                                        // we don't screw with the first and last pieces of the file during
                                        // storage type changes we don't have the problem of dealing with
                                        // the last piece being smaller than torrent piece size

                                    if ( cleared > 0 ){

                                        downloaded = downloaded - cleared * torrent_file.getTorrent().getPieceLength();

                                        if ( downloaded < 0 ){

                                            downloaded = 0;
                                        }

                                        storeFileDownloaded( download_manager, res );
                                    }
                                }

                                return( true );

                            }catch( Throwable e ){

                                Debug.printStackTrace(e);

                                Logger.log(
                                    new LogAlert(
                                            LogAlert.REPEATABLE,
                                            LogAlert.AT_ERROR,
                                            "Failed to change storage type for '" + getFile(true) +"': " + Debug.getNestedExceptionMessage(e)));

                                    // download's not running - tag for recheck

                                RDResumeHandler.recheckFile( download_manager, this );

                                return( false );

                            }finally{

                                types[file_index] = type==ST_LINEAR?"L":"C";

                                DownloadManagerState    dm_state = download_manager.getDownloadState();

                                dm_state.setListAttribute( DownloadManagerState.AT_FILE_STORE_TYPES, types );

                                dm_state.save();

                                if ( set_skipped ){

                                    setSkipped( true );
                                }
                            }
                        }

                        public int
                        getStorageType()
                        {
                            String[]    types = getStorageTypes( download_manager );

                            return( types[file_index].equals( "L")?ST_LINEAR:ST_COMPACT );
                        }

                        public void
                        flushCache()
                        {
                        }

                        public DirectByteBuffer
                        read(
                            long    offset,
                            int     length )

                            throws IOException
                        {
                            try{
                                cache_read_mon.enter();

                                if ( read_cache_file == null ){

                                    try{
                                        String[]    types = getStorageTypes( download_manager );

                                        int type = types[file_index].equals( "L")?ST_LINEAR:ST_COMPACT;

                                        read_cache_file =
                                            CacheFileManagerFactory.getSingleton().createFile(
                                                    new CacheFileOwner()
                                                    {
                                                        public String
                                                        getCacheFileOwnerName()
                                                        {
                                                            return( download_manager.getInternalName());
                                                        }

                                                        public TOTorrentFile
                                                        getCacheFileTorrentFile()
                                                        {
                                                            return( torrent_file );
                                                        }

                                                        public File
                                                        getCacheFileControlFile(String name)
                                                        {
                                                            return( download_manager.getDownloadState().getStateFile( name ));
                                                        }
                                                    },
                                                    getFile( true ),
                                                    type==ST_LINEAR?CacheFile.CT_LINEAR:CacheFile.CT_COMPACT );

                                    }catch( Throwable e ){

                                        Debug.printStackTrace(e);

                                        throw( new IOException( e.getMessage()));
                                    }
                                }
                            }finally{

                                cache_read_mon.exit();
                            }

                            DirectByteBuffer    buffer =
                                DirectByteBufferPool.getBuffer( DirectByteBuffer.AL_DM_READ, length );

                            try{
                                read_cache_file.read( buffer, offset, CacheFile.CP_READ_CACHE );

                            }catch( Throwable e ){

                                buffer.returnToPool();

                                Debug.printStackTrace(e);

                                throw( new IOException( e.getMessage()));
                            }

                            return( buffer );
                        }

                        public void
                        close()
                        {
                            if ( read_cache_file != null ){

                                try{
                                    read_cache_file.close();

                                }catch( Throwable e ){

                                    Debug.printStackTrace(e);
                                }

                                read_cache_file = null;
                            }
                        }

                        public void
                        addListener(
                            DiskManagerFileInfoListener listener )
                        {
                            if ( getDownloaded() == getLength()){

                                try{
                                    listener.dataWritten( 0, getLength());

                                    listener.dataChecked( 0, getLength());

                                }catch( Throwable e ){

                                    Debug.printStackTrace(e);
                                }
                            }
                        }

                        public void
                        removeListener(
                            DiskManagerFileInfoListener listener )
                        {
                        }
                    };

                res[i]  = info;
            }

            loadFilePriorities( download_manager, res );

            loadFileDownloaded( download_manager, res );

            return( res );

        }catch( Throwable e ){

            Debug.printStackTrace(e);

            return( new DiskManagerFileInfo[0]);

        }
    }

    public static void
    setFileLinks(
        DownloadManager         download_manager,
        CaseSensitiveFileMap    links )
    {
        try{
            CacheFileManagerFactory.getSingleton().setFileLinks( download_manager.getTorrent(), links );

        }catch( Throwable e ){

            Debug.printStackTrace(e);
        }
    }

    /* (non-Javadoc)
     * @see org.gudy.azureus2.core3.logging.LogRelation#getLogRelationText()
     */
    public String getRelationText() {
        return "TorrentDM: '" + download_manager.getDisplayName() + "'";
    }


    /* (non-Javadoc)
     * @see org.gudy.azureus2.core3.logging.LogRelation#queryForClass(java.lang.Class)
     */
    public Object[] getQueryableInterfaces() {
        return new Object[] { download_manager, torrent };
    }

    public DiskManagerRecheckScheduler
    getRecheckScheduler()
    {
        return( recheck_scheduler );
    }

    public boolean isInteresting(int pieceNumber)
    {
        return pieces[pieceNumber].isInteresting();
    }

    public boolean isDone(int pieceNumber)
    {
        return pieces[pieceNumber].isDone();
    }

/*
    public boolean isRequestable(int pieceNumber)
    {
        return pieces[pieceNumber].isRequestable();
    }

    public boolean calcNeeded(int pieceNumber)
    {
        return pieces[pieceNumber].calcNeeded();
    }

    public void clearNeeded(int pieceNumber)
    {
        pieces[pieceNumber].clearNeeded();
    }

    public void setRequested(int pieceNumber)
    {
        pieces[pieceNumber].setRequested();
    }

    public boolean isRequested(int pieceNumber)
    {
        return pieces[pieceNumber].isRequested();
    }

    public long getLastWriteTime(int pieceNumber)
    {
        return pieces[pieceNumber].getLastWriteTime();
    }

    public long getNbBlocks(int pieceNumber)
    {
        return pieces[pieceNumber].getNbBlocks();
    }

    public long getNbWritten(int pieceNumber)
    {
        return pieces[pieceNumber].getNbWritten();
    }
*/
}
