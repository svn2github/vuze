package org.gudy.azureus2.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.gudy.azureus2.core2.DataQueueItem;

/**
 * 
 * The disk Wrapper.
 * 
 * @author Tdv_VgA
 *
 */
public class DiskManager {
  public static final int INITIALIZING = 1;
  public static final int ALLOCATING = 2;
  public static final int CHECKING = 3;
  public static final int READY = 4;
  public static final int FAULTY = 10;

  private int state;
  private String errorMessage = "";

  private int pieceLength;
  private int lastPieceLength;

  //  private int[] _priorityPieces;

  private byte[] piecesHash;
  private int nbPieces;
  private long totalLength;
  private boolean pieceDone[];
  private int percentDone;
  private long allocated;

  private long remaining;

  private String path;
  private String fileName = "";
  private Map metaData;

  private ByteBuffer allocateAndTestBuffer;

  private Vector writeQueue;
  private Vector checkQueue;
  private Vector readQueue;

  private DiskWriteThread writeThread;
  private DiskReadThread readThread;

  private String rootPath = null;

  //The map that associate
  private List[] pieceMap;
  private int pieceCompletion[];
  private BitSet[] priorityLists;
  //private int[][] priorityLists;

  private FileInfo[] files;

  //long[] filesDone;
  //RandomAccessFile[] fileArray;

  private PeerManager manager;
  private SHA1Hasher hasher;

  private boolean bContinue = true;
  
  private Piece[] pieces;

  public DiskManager(Map metaData, String path) {
    this.state = INITIALIZING;
    this.percentDone = 0;
    this.metaData = metaData;
    this.path = path;
    try {
      hasher = new SHA1Hasher();
    }
    catch (NoSuchAlgorithmException ignore) {}
    Thread init = new Thread() {
      public void run() {
        initialize();
        if (state == DiskManager.FAULTY) {
          stopIt();
        }
      }
    };
    init.setPriority(Thread.MIN_PRIORITY);
    init.start();
  }

  private void initialize() {

    //1. parse the metaData object.
    Map info = (Map) metaData.get("info");
    pieceLength = (int) ((Long) info.get("piece length")).longValue();

    piecesHash = (byte[]) info.get("pieces");
    nbPieces = piecesHash.length / 20;

    //  create the pieces map
    pieceMap = new ArrayList[nbPieces];
    pieceCompletion = new int[nbPieces];
    priorityLists = new BitSet[10];
    //    priorityLists = new int[10][nbPieces + 1];

    // the piece numbers for getPiecenumberToDownload
    //    _priorityPieces = new int[nbPieces + 1];

    pieceDone = new boolean[nbPieces];
    LocaleUtil localeUtil = LocaleUtil.getLocaleUtil(metaData.get("encoding"));

    fileName = "";
    try {
      File f = new File(path);
      if (f.isDirectory()) {
        fileName = localeUtil.getChoosableCharsetString((byte[]) info.get("name"));
      }
      else {
        fileName = f.getName();
        path = f.getParent();
      }
    }
    catch (UnsupportedEncodingException e) {
      this.state = FAULTY;
      this.errorMessage = e.getMessage();
      return;
    }

    //build something to hold the filenames/sizes
    ArrayList btFileList = new ArrayList();

    //Create the ByteBuffer for checking (size : pieceLength)
    allocateAndTestBuffer = ByteBuffer.allocateDirect(pieceLength);
    allocateAndTestBuffer.mark();
    for (int i = 0; i < allocateAndTestBuffer.limit(); i++) {
      allocateAndTestBuffer.put((byte) 0);
    }
    allocateAndTestBuffer.reset();

    //Create the new Queue
    writeQueue = new Vector();
    checkQueue = new Vector();
    readQueue = new Vector();
    writeThread = new DiskWriteThread();
    writeThread.start();
    readThread = new DiskReadThread();
    readThread.start();

    //2. Distinguish between simple file
    Object test = info.get("length");
    if (test != null) {
      totalLength = ((Long) test).longValue();
      rootPath = "";
      btFileList.add(new BtFile("", fileName, totalLength));
    }
    else {
      //define a variable to keep track of what piece we're on
      //      int currentPiece = 0;

      StringBuffer pathBuffer = new StringBuffer(256);
      final char separator = System.getProperty("file.separator").charAt(0);

      //get the root
      rootPath = fileName + separator;

      //:: Directory patch 08062003 - Tyler
      //check for a user selecting the full path
      String fullPath = path + separator;
      int fullPathIndex = fullPath.lastIndexOf(rootPath);
      if (fullPathIndex >= 0 && fullPathIndex == (fullPath.length() - rootPath.length())) {
        rootPath = ""; //null out rootPath
      }

      //get the files object
      List files = (List) info.get("files");
      //for each file
      for (int i = 0; i < files.size(); i++) {
        //build the file lookup table
        Map fileDictionay = (Map) files.get(i);
        //get the length
        long fileLength = ((Long) fileDictionay.get("length")).longValue();
        //build the path
        List fileList = (List) fileDictionay.get("path");

        int size = fileList.size();
        //build the path string

        pathBuffer.setLength(0);
        int j;
        for (j = 0; j < size - 1; j++) {
          //attach every element
          try {
            pathBuffer.append(localeUtil.getChoosableCharsetString((byte[]) fileList.get(j)));
          }
          catch (UnsupportedEncodingException e) {
            this.state = FAULTY;
            this.errorMessage = e.getMessage();
            return;
          }
          pathBuffer.append(separator);
        }

        //no, then we must be a part of the path
        //add the file entry to the file holder list 
        try {
          btFileList.add(
            new BtFile(
              pathBuffer.toString(),
              localeUtil.getChoosableCharsetString((byte[]) fileList.get(j)),
              fileLength));
        }
        catch (UnsupportedEncodingException e) {
          this.state = FAULTY;
          this.errorMessage = e.getMessage();
          return;
        }

        //increment the global length 
        totalLength += fileLength;
        //clear the memory
        fileList = null;
      }
    }

    if (localeUtil.canEncodingBeSaved() && !localeUtil.getLastChoosedEncoding().equals(metaData.get("encoding"))) {
      metaData.put("encoding", localeUtil.getLastChoosedEncoding());
      saveTorrent();
    }

    remaining = totalLength;
    lastPieceLength = (int) (totalLength - ((long) (nbPieces - 1) * (long) pieceLength));

    //we now have a list of files and their lengths
    //allocate / check every file
    //fileArray = new RandomAccessFile[btFileList.size()];
    files = new FileInfo[btFileList.size()];
    boolean newFiles = this.allocateFiles(rootPath, btFileList);
    if (this.state == FAULTY)
      return;
    //for every piece, except the last one
    //add files to the piece list until we have built enough space to hold the piece
    //see how much space is available in the file
    //if the space available isnt 0
    //add the file to the piece->file mapping list
    //if there is enough space available, stop  

    //fix for 1 piece torrents
    if (totalLength < pieceLength) {
      pieceLength = (int) totalLength; //ok to convert
    }

    int fileOffset = 0;
    int currentFile = 0;
    for (int i = 0;(1 == nbPieces && i < nbPieces) || i < nbPieces - 1; i++) {
      ArrayList pieceToFileList = new ArrayList();
      int usedSpace = 0;
      while (pieceLength > usedSpace) {
        BtFile tempFile = (BtFile) btFileList.get(currentFile);
        long length = tempFile.getLength();

        //get the available space
        long availableSpace = length - fileOffset;

        PieceMapEntry tempPieceEntry = null;

        //how much space do we need to use?                               
        if (availableSpace < (pieceLength - usedSpace)) {
          //use the rest of the file's space
            tempPieceEntry =
              new PieceMapEntry(tempFile.getFileInfo(), fileOffset, (int) availableSpace //safe to convert here
  );

          //update the used space
          usedSpace += availableSpace;
          //update the file offset
          fileOffset = 0;
          //move the the next file
          currentFile++;
        }
        else //we don't need to use the whole file
          {
          tempPieceEntry = new PieceMapEntry(tempFile.getFileInfo(), fileOffset, pieceLength - usedSpace);

          //update the file offset
          fileOffset += pieceLength - usedSpace;
          //udate the used space
          usedSpace += pieceLength - usedSpace;
        }

        //add the temp pieceEntry to the piece list
        pieceToFileList.add(tempPieceEntry);
      }

      //add the list to the map
      pieceMap[i] = pieceToFileList;
    }

    //take care of final piece if there was more than 1 piece in the torrent
    if (nbPieces > 1) {
      pieceMap[nbPieces - 1] = this.buildPieceToFileList(btFileList, currentFile, fileOffset, lastPieceLength);
    }

    constructFilesPieces();

    //if all files existed, check pieces
    if (!newFiles)
      this.checkAllPieces();

    //3.Change State   
    state = READY;
  }

  private void checkAllPieces() {
    state = CHECKING;
    boolean resumeEnabled = ConfigurationManager.getInstance().getBooleanParameter("Use Resume", false);
    boolean resumeValid = false;
    byte[] resumeArray = null;
    Map partialPieces = null;
    Map resumeMap = (Map) metaData.get("resume");
    if (resumeMap != null) {
      Map resumeDirectory = (Map) resumeMap.get(this.path);
      if (resumeDirectory != null) {
        try {      
          resumeArray = (byte[]) resumeDirectory.get("resume data");
          partialPieces = (Map) resumeDirectory.get("blocks");        
          resumeValid = ((Long) resumeDirectory.get("valid")).intValue() == 1;
          resumeDirectory.put("valid", new Long(0));
          saveTorrent();
        }
        catch (Exception ignore) {}
      }
    }

    if (resumeValid && resumeEnabled && (resumeArray != null) && (resumeArray.length == pieceDone.length)) {
      for (int i = 0; i < resumeArray.length; i++) //parse the array
        {
        //mark the pieces
        if (resumeArray[i] == 0) {
          pieceDone[i] = false;
        }
        else {
          computeFilesDone(i);
          pieceDone[i] = true;
          if (i < nbPieces - 1) {
            remaining -= pieceLength;
          }
          if (i == nbPieces - 1) {
            remaining -= lastPieceLength;
          }
        }
      }
      if(partialPieces != null) {
        pieces = new Piece[nbPieces];
        Iterator iter = partialPieces.entrySet().iterator();
        while(iter.hasNext()) {
          Map.Entry key = (Map.Entry) iter.next();
          int pieceNumber = Integer.parseInt((String)key.getKey());
          Piece piece;
          if (pieceNumber < nbPieces - 1)
            piece = new Piece(null, getPieceLength(), pieceNumber);
          else
            piece = new Piece(null, getLastPieceLength(), pieceNumber);
          List blocks = (List) partialPieces.get(key.getKey());
          Iterator iterBlock = blocks.iterator();
          while(iterBlock.hasNext()) {
            piece.setWritten(((Long)iterBlock.next()).intValue());
          }
          pieces[pieceNumber] = piece;          
        }        
      }       
    }
    else //no resume data.. rebuild it
      {
      for (int i = 0; i < nbPieces && bContinue; i++) {
        percentDone = ((i + 1) * 1000) / nbPieces;
        checkPiece(i);
      }
      //Patch:: dump the newly built resume data to the disk / torrent --Tyler
      if (bContinue && resumeEnabled)
        this.dumpResumeDataToDisk(false);
    }
  }

  private List buildPieceToFileList(List btFileList, int currentFile, int fileOffset, int pieceSize) {
    ArrayList pieceToFileList = new ArrayList();
    int usedSpace = 0;
    while (pieceSize > usedSpace) {
      BtFile tempFile = (BtFile) btFileList.get(currentFile);
      long length = tempFile.getLength();

      //get the available space
      long availableSpace = length - fileOffset;

      PieceMapEntry tempPieceEntry = null;

      //how much space do we need to use?                               
      if (availableSpace < (pieceLength - usedSpace)) {
        //use the rest of the file's space
        tempPieceEntry = new PieceMapEntry(tempFile.getFileInfo(), fileOffset, (int) availableSpace);

        //update the used space
        usedSpace += availableSpace;
        //update the file offset
        fileOffset = 0;
        //move the the next file
        currentFile++;
      }
      else //we don't need to use the whole file
        {
        tempPieceEntry = new PieceMapEntry(tempFile.getFileInfo(), fileOffset, pieceSize - usedSpace);

        //update the file offset
        fileOffset += pieceLength - usedSpace;
        //udate the used space
        usedSpace += pieceLength - usedSpace;
      }

      //add the temp pieceEntry to the piece list
      pieceToFileList.add(tempPieceEntry);
    }

    return pieceToFileList;
  }

  private static class FlyWeightInteger {
    private static Vector array = new Vector(1024);

    public static synchronized Integer getInteger(int value) {
      Integer tmp = null;
      if (value >= array.size()) {
        array.setSize(value + 256);
      }
      else {
        tmp = (Integer) array.get(value);
      }
      if (tmp == null) {
        tmp = new Integer(value);
        array.set(value, tmp);
      }
      return tmp;

    }
  }

  private static class BtFile {
    private FileInfo _file;
    private String _path;
    private String _name;
    private String _originalName = null;
    private long _length;
    private static final String[] unsupportedChars = { "[\\/:?*]" };
    // 0 = Windows: \ / : ? * and any other Unicode letters ('?')

    public BtFile(String path, String name, long length) {
      _path = path;
      _length = length;
      _name = name;

      String newName = name.replace('"', '\'');

      if (System.getProperty("os.name").startsWith("Windows")) {
        newName = newName.replaceAll(unsupportedChars[0], "_");
      }

      if (!name.equals(newName)) {
        _name = newName;
        _originalName = name;
      }

    }
    public long getLength() {
      return _length;
    }
    public String getPath() {
      return _path;
    }
    public boolean isNameOriginal() {
      return _originalName == null;
    }
    public String getOriginalName() {
      return _originalName == null ? _name : _originalName;
    }
    public String getName() {
      return _name;
    }
    public FileInfo getFileInfo() {
      return _file;
    }
    public void setFileInfo(FileInfo file) {
      _file = file;
    }
  }

  private static class PieceMapEntry {
    private FileInfo _file;
    private int _offset;
    private int _length;

    public PieceMapEntry(FileInfo file, int offset, int length) {
      _file = file;
      _offset = offset;
      _length = length;
    }
    public FileInfo getFile() {
      return _file;
    }
    public void setFile(FileInfo file) {
      _file = file;
    }
    public int getOffset() {
      return _offset;
    }
    public int getLength() {
      return _length;
    }

  }

  public static class WriteElement {
    private int pieceNumber;
    private int offset;
    private ByteBuffer data;

    public WriteElement(int pieceNumber, int offset, ByteBuffer data) {
      this.pieceNumber = pieceNumber;
      this.offset = offset;
      this.data = data;
    }

    public int getPieceNumber() {
      return this.pieceNumber;
    }

    public int getOffset() {
      return this.offset;
    }

    public ByteBuffer getData() {
      return this.data;
    }
  }

  public class DiskReadThread extends Thread {
    private boolean bContinue = true;

    public DiskReadThread() {
      super("Disk Reader");
    }

    public void run() {
      while (bContinue) {
        while (readQueue.size() != 0) {
          DataQueueItem item = (DataQueueItem) readQueue.remove(0);
          Request request = item.getRequest();

          // temporary fix for bug 784306
          ByteBuffer buffer = readBlock(request.getPieceNumber(), request.getOffset(), request.getLength());
          if (buffer != null)
            item.setBuffer(buffer);
        }
        try {
          Thread.sleep(15);
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    }

    public void stopIt() {
      this.bContinue = false;
      while (readQueue.size() != 0) {
        DataQueueItem item = (DataQueueItem) readQueue.remove(0);
        item.setLoading(false);
      }
    }
  }

  public class DiskWriteThread extends Thread {
    private boolean bContinue = true;

    public DiskWriteThread() {
      super("Disk Writer & Checker");
    }

    public void run() {
      while (bContinue) {
        while (writeQueue.size() != 0) {
          WriteElement elt = (WriteElement) writeQueue.remove(0);
          dumpBlockToDisk(elt);
          manager.blockWritten(elt.getPieceNumber(), elt.getOffset());
        }
        if (checkQueue.size() != 0) {
          WriteElement elt = (WriteElement) checkQueue.remove(0);
          manager.pieceChecked(elt.getPieceNumber(), checkPiece(elt.getPieceNumber()));
        }
        try {
          Thread.sleep(15);
        }
        catch (Exception e) {
          e.printStackTrace();
        }

      }
    }

    public void stopIt() {
      this.bContinue = false;
      while (writeQueue.size() != 0) {
        WriteElement elt = (WriteElement) writeQueue.remove(0);
        ByteBufferPool.getInstance().freeBuffer(elt.data);
      }
    }
  }

  private boolean allocateFiles(String rootPath, List fileList) {
    this.state = ALLOCATING;
    allocated = 0;
    boolean newFiles = false;
    String basePath = path + System.getProperty("file.separator") + rootPath;
    for (int i = 0; i < fileList.size(); i++) {
      //get the BtFile
      BtFile tempFile = (BtFile) fileList.get(i);
      //get the path
      String tempPath = basePath + tempFile.getPath();
      //get file name
      String tempName = tempFile.getName();
      //get file length
      long length = tempFile.getLength();

      File f = new File(tempPath, tempName);
      //Test if files exists
      RandomAccessFile raf = null;

      if (!(f.exists() && f.length() == length)) {
        //File doesn't exist
        buildDirectoryStructure(tempPath);
        try {
          // throw Exception if filename is not supported by os
          f.getCanonicalPath();
          raf = new RandomAccessFile(f, "rw");
          raf.setLength(length);
        }
        catch (Exception e) {
          this.state = FAULTY;
          this.errorMessage = e.getMessage();
          return false;
        }

        if (ConfigurationManager.getInstance().getBooleanParameter("Allocate New", true))
          clearFile(raf);
        newFiles = true;
      }
      else {
        try {
          raf = new RandomAccessFile(f, "rw");
        }
        catch (FileNotFoundException e) {
          this.state = FAULTY;
          this.errorMessage = e.getMessage();
          return false;
        }
        allocated += length;
      }

      //add the file to the array

      FileInfo fileInfo = new FileInfo();
      fileInfo.setPath(tempPath);
      fileInfo.setName(tempName);
      int separator = tempName.lastIndexOf(".");
      if (separator == -1)
        separator = 0;
      fileInfo.setExtension(tempName.substring(separator));
      fileInfo.setLength(length);
      fileInfo.setDownloaded(0);
      fileInfo.setFile(f);
      fileInfo.setRaf(raf);
      fileInfo.setAccessmode(FileInfo.WRITE);
      files[i] = fileInfo;

      //setup this files RAF reference
      tempFile.setFileInfo(files[i]);
    }
    return newFiles;
  }

  private void clearFile(RandomAccessFile file) {
    FileChannel fc = file.getChannel();
    long length = 0;
    try {
      length = file.length();
    }
    catch (IOException e) {
      this.state = FAULTY;
      this.errorMessage = e.getMessage();
      return;
    }
    long writen = 0;
    synchronized (file) {
      try {
        fc.position(0);
        while (writen < length && bContinue) {
          allocateAndTestBuffer.limit(allocateAndTestBuffer.capacity());
          if ((length - writen) < allocateAndTestBuffer.remaining())
            allocateAndTestBuffer.limit((int) (length - writen));
          int deltaWriten = fc.write(allocateAndTestBuffer);
          allocateAndTestBuffer.position(0);
          writen += deltaWriten;
          allocated += deltaWriten;
          percentDone = (int) ((allocated * 1000) / totalLength);
        }
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void buildDirectoryStructure(String file) {
    File tempFile = new File(file);
    tempFile.mkdirs();
  }

  public void aSyncCheckPiece(int pieceNumber) {
    checkQueue.add(new WriteElement(pieceNumber, 0, null));
  }

  public synchronized boolean checkPiece(int pieceNumber) {
    allocateAndTestBuffer.position(0);

    int length = pieceNumber < nbPieces - 1 ? pieceLength : lastPieceLength;

    allocateAndTestBuffer.limit(length);

    //get the piece list
    List pieceList = pieceMap[pieceNumber];
    //for each piece
    for (int i = 0; i < pieceList.size(); i++) {
      //get the piece and the file 
      PieceMapEntry tempPiece = (PieceMapEntry) pieceList.get(i);
      synchronized (tempPiece.getFile()) {
        //grab it's data and return it
        try {
          RandomAccessFile raf = tempPiece.getFile().getRaf();
          FileChannel fc = raf.getChannel();
          if (fc.isOpen()) {
            fc.position(tempPiece.getOffset());
            fc.read(allocateAndTestBuffer);
          }
        }
        catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    try {
      allocateAndTestBuffer.position(0);

      byte[] testHash = hasher.calculateHash(allocateAndTestBuffer);
      int i = 0;
      int pieceLocation = pieceNumber * 20;
      for (i = 0; i < 20; i++) {
        if (testHash[i] != piecesHash[pieceLocation + i])
          break;
      }
      if (i >= 20) {
        //mark the piece as done..
        if (!pieceDone[pieceNumber]) {
          pieceDone[pieceNumber] = true;
          remaining -= length;
          computeFilesDone(pieceNumber);
        }
        return true;
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  public void dumpResumeDataToDisk(boolean savePartialPieces) {
    //TODO CLEAN UP
    //build the piece byte[] 
    byte[] resumeData = new byte[pieceDone.length];
    for (int i = 0; i < resumeData.length; i++) {
      resumeData[i] = pieceDone[i] ? (byte) 1 : (byte) 0;
    }

    //Attach the resume data
    Map resumeMap = (Map) metaData.get("resume");
    if (resumeMap == null) {
      resumeMap = new HashMap();
      metaData.put("resume", resumeMap);
    }
    Map resumeDirectory = new HashMap();
    resumeMap.put(path, resumeDirectory);
    resumeDirectory.put("resume data", resumeData);
    Map partialPieces = new HashMap();
    if (savePartialPieces) {      
      for (int i = 0; i < pieces.length; i++) {
        Piece piece = pieces[i];
        if (piece != null && piece.getCompleted() > 0) {
          boolean[] downloaded = piece.written;
          List blocks = new ArrayList();
          for (int j = 0; j < downloaded.length; j++) {
            if (downloaded[j])
              blocks.add(new Long(j));
          }
          partialPieces.put("" + i, blocks);
        }
      }
      resumeDirectory.put("blocks", partialPieces);
      resumeDirectory.put("valid", new Long(1));
    }
    else {
      resumeDirectory.put("valid", new Long(0));
    }
    saveTorrent();
  }

  private void saveTorrent() {
    //TODO:: CLEAN UP - fix the conversion to a string...
    //open the torrent file       
    File torrent = null;
    try {
      torrent = new File(new String((byte[]) metaData.get("torrent filename"), Constants.DEFAULT_ENCODING));
    }
    catch (UnsupportedEncodingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    //re-encode the data
    byte[] torrentData = BEncoder.encode(metaData);
    //open a file stream
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(torrent);
      //write the data out
      fos.write(torrentData);
    }
    catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    finally {
      try {
        if (fos != null)
          fos.close();
      }
      catch (Exception e) {}
    }
  }

  public void enqueueReadRequest(DataQueueItem item) {
    readQueue.add(item);
  }

  //MODIFY THIS TO WORK WITH PATH/FILES
  public byte[] readPiece(int pieceNumber) {
    //get the piece list
    List pieceList = pieceMap[pieceNumber];
    //build the byte buffer
    ByteArrayOutputStream bos = new ByteArrayOutputStream();

    //for each piece
    for (int i = 0; i < pieceList.size(); i++) {
      //get the piece and the file 
      PieceMapEntry tempPiece = (PieceMapEntry) pieceList.get(i);
      RandomAccessFile raf = tempPiece.getFile().getRaf();

      //grab it's data and return it
      try {
        byte[] data = new byte[tempPiece.getLength()];
        //create a databuffer
        raf.seek(tempPiece.getOffset());
        //seek to the correct point
        raf.read(data); //read the data
        bos.write(data); //write it to the stream
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }
    return bos.toByteArray();
  }

  public ByteBuffer readBlock(int pieceNumber, int offset, int length) {
    ByteBuffer buffer = ByteBufferPool.getInstance().getFreeBuffer();
    buffer.position(0);
    buffer.limit(length + 13);
    buffer.putInt(length + 9);
    buffer.put((byte) 7);
    buffer.putInt(pieceNumber);
    buffer.putInt(offset);

    int previousFilesLength = 0;
    int currentFile = 0;
    List pieceList = pieceMap[pieceNumber];

    // temporary fix for bug 784306
    if (pieceList.size() == 0) {
      System.out.println("no pieceList entries for " + pieceNumber);
      return buffer;
    }

    PieceMapEntry tempPiece = (PieceMapEntry) pieceList.get(currentFile);
    long fileOffset = tempPiece.getOffset();
    while ((previousFilesLength + tempPiece.getLength()) < offset) {
      previousFilesLength += tempPiece.getLength();
      currentFile++;
      fileOffset = 0;
      tempPiece = (PieceMapEntry) pieceList.get(currentFile);
    }

    //System.out.println(pieceNumber + "," + offset + "," + length + ":" + fileOffset + "," + previousFilesLength);
    //Now tempPiece points to the first file that contains data for this block
    boolean noError = true;
    while (buffer.hasRemaining() && noError) {
      tempPiece = (PieceMapEntry) pieceList.get(currentFile);
      synchronized (tempPiece.getFile()) {
        RandomAccessFile raf = tempPiece.getFile().getRaf();
        FileChannel fc = raf.getChannel();
        try {
          fc.position(fileOffset + (offset - previousFilesLength));
          fc.read(buffer);
        }
        catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
          System.out.println(path + fileName + " :: " + tempPiece.getFile().getName());
          noError = false;
        }
      }
      currentFile++;
      fileOffset = 0;
      previousFilesLength = offset;
      if (buffer.hasRemaining() && currentFile > pieceList.size()) {
        noError = false;
        System.out.println("Error in read Block : *Debug Information*");
        System.out.println("PieceNumber : " + pieceNumber);
        System.out.println("Offset : " + offset);
        System.out.println("Length : " + length);
        System.out.println("Buffer - limit : " + buffer.limit() + ", remaining : " + buffer.remaining());
        System.out.println("PieceList - size : " + pieceList.size());
        System.out.println("currentFile : " + currentFile);
        System.out.println("tempPiece - length : " + tempPiece.getLength() + ", offset : " + tempPiece.getOffset());

      }

    }

    buffer.position(0);
    return buffer;
  }

  public void writeBlock(int pieceNumber, int offset, ByteBuffer data) {
    writeQueue.add(new WriteElement(pieceNumber, offset, data));
  }

  public boolean checkBlock(int pieceNumber, int offset, ByteBuffer data) {
    if (pieceNumber < 0)
      return false;
    if (pieceNumber >= this.nbPieces)
      return false;
    int length = this.pieceLength;
    if (pieceNumber == nbPieces - 1)
      length = this.lastPieceLength;
    if (offset < 0)
      return false;
    if (offset > length)
      return false;
    int size = data.remaining();
    if (offset + size > length)
      return false;
    return true;
  }

  public boolean checkBlock(int pieceNumber, int offset, int length) {
    if (length > 65536)
      return false;
    if (pieceNumber < 0)
      return false;
    if (pieceNumber >= this.nbPieces)
      return false;
    int pLength = this.pieceLength;
    if (pieceNumber == this.nbPieces - 1)
      pLength = this.lastPieceLength;
    if (offset < 0)
      return false;
    if (offset > pLength)
      return false;
    if (offset + length > pLength)
      return false;
    return true;
  }

  private void dumpBlockToDisk(WriteElement e) {
    int pieceNumber = e.getPieceNumber();
    int offset = e.getOffset();
    ByteBuffer buffer = e.getData();

    int previousFilesLength = 0;
    int currentFile = 0;
    List pieceList = pieceMap[pieceNumber];
    PieceMapEntry tempPiece = (PieceMapEntry) pieceList.get(currentFile);
    long fileOffset = tempPiece.getOffset();
    while ((previousFilesLength + tempPiece.getLength()) < offset) {
      previousFilesLength += tempPiece.getLength();
      currentFile++;
      fileOffset = 0;
      tempPiece = (PieceMapEntry) pieceList.get(currentFile);
    }

    //Now tempPiece points to the first file that contains data for this block
    while (buffer.hasRemaining()) {
      //System.out.println(pieceNumber + "," + offset + " : " + previousFilesLength + " : " + currentFile + "r:" + buffer.remaining());      
      tempPiece = (PieceMapEntry) pieceList.get(currentFile);
      //System.out.println(pieceNumber + "," + offset + " : " + previousFilesLength + " : " + currentFile + "," + tempPiece.getLength());

      synchronized (tempPiece.getFile()) {
        try {
          RandomAccessFile raf = tempPiece.getFile().getRaf();
          FileChannel fc = raf.getChannel();
          fc.position(fileOffset + (offset - previousFilesLength));
          //System.out.print(" remaining:" + buffer.remaining());
          //System.out.print(" position:" + buffer.position());
          int realLimit = buffer.limit();
          //System.out.print(" realLimit:" + realLimit);
          int limit =
            buffer.position() + (int) ((raf.length() - tempPiece.getOffset()) - (offset - previousFilesLength));
          //System.out.print(" limit:" + limit);
          if (limit < realLimit)
            buffer.limit(limit);
          //System.out.print(" Blimit:" + buffer.limit());
          fc.write(buffer);
          buffer.limit(realLimit);
          //System.out.print(" remaining:" + buffer.remaining());
          //System.out.println(" position:" + buffer.position());
        }
        catch (IOException ex) {
          // TODO Auto-generated catch block
          ex.printStackTrace();
        }
      }
      currentFile++;
      fileOffset = 0;
      previousFilesLength = offset;
    }

    ByteBufferPool.getInstance().freeBuffer(buffer);
  }

  public void updateResumeInfo() {
    FileOutputStream fos = null;
    try {
      //  TODO CLEAN UP
      //build the piece byte[] 
      byte[] resumeData = new byte[pieceDone.length];
      for (int i = 0; i < resumeData.length; i++) {
        if (pieceDone[i] == false) {
          resumeData[i] = (byte) 0;
        }
        else {
          resumeData[i] = (byte) 1;
        }
      }

      //Attach the resume data
      metaData.put("resume data", resumeData);

      //TODO:: CLEAN UP - fix the conversion to a string...
      //open the torrent file       
      File torrent = new File(new String((byte[]) metaData.get("torrent filename"), Constants.DEFAULT_ENCODING));
      //re-encode the data
      byte[] torrentData = BEncoder.encode(metaData);
      //open a file stream
      fos = new FileOutputStream(torrent);
      //write the data out
      fos.write(torrentData);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      try {
        if (fos != null)
          fos.close();
      }
      catch (Exception e) {}
    }
  }

  public byte[] readDataBloc(int pieceNumber, int offset, int length) {
    /*
    try{
    long fileOffset = (long) pieceNumber * (long) pieceLength + offset;
    byte[] data = new byte[length];
    raf.seek(offset);
    raf.read(data,0,length);
    return data;
    } catch (Exception e) {
    e.printStackTrace();
    }
    return null;
    */
    byte[] data = this.readPiece(pieceNumber);
    byte[] outputData = new byte[length];
    System.arraycopy(data, offset, outputData, 0, length);
    return outputData;
  }

  public ByteBuffer readDataBloc2(int pieceNumber, int offset, int length) {
    ByteBuffer buffer = ByteBufferPool.getInstance().getFreeBuffer();
    buffer.limit(length + 13);
    buffer.putInt(length + 9);
    //BT PIECE byte
    buffer.put((byte) 7);
    buffer.putInt(pieceNumber);
    buffer.putInt(offset);

    return buffer;
  }

  public int getPiecesNumber() {
    return nbPieces;
  }

  public boolean[] getPiecesStatus() {
    return pieceDone;
  }

  public int getPercentDone() {
    return percentDone;
  }

  public long getRemaining() {
    return remaining;
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
    return state;
  }

  public String getFileName() {
    return fileName;
  }

  /*public void changeToReadOnly() {
    for (int i = 0; i < files.length; i++) {
      synchronized (files[i]) {
        try {
          RandomAccessFile oldRaf = files[i].getRaf();
          oldRaf.close();
          files[i].setRaf(new RandomAccessFile(files[i].getFile(), "r"));
          files[i].setAccessmode(FileInfo.READ);
          //Now changes all pieces ...
          for (int j = 0; j < nbPieces; j++) {
            //Get the piece list for this piece
            List pieceList = pieceMap[j];
            //for each piece
            for (int k = 0; k < pieceList.size(); k++) {
              //get the piece and the file 
              PieceMapEntry tempPiece = (PieceMapEntry) pieceList.get(k);
              RandomAccessFile raf = tempPiece.getFile().getRaf();
              if (raf == oldRaf)
                tempPiece.setFile(files[i]);
            }
          }
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }*/

  public void setManager(PeerManager manager) {
    this.manager = manager;
  }

  public void stopIt() {
    if (writeThread != null)
      writeThread.stopIt();
    if (readThread != null)
      readThread.stopIt();
    this.bContinue = false;
    if (files != null) {
      for (int i = 0; i < files.length; i++) {
        try {
          if (files[i] != null)
            files[i].getRaf().close();
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  public void computeFilesDone(int pieceNumber) {
    for (int i = 0; i < files.length; i++) {
      RandomAccessFile raf = files[i].getRaf();
      List pieceList = pieceMap[pieceNumber];
      //for each piece

      for (int k = 0; k < pieceList.size(); k++) {
        //get the piece and the file 
        PieceMapEntry tempPiece = (PieceMapEntry) pieceList.get(k);
        if (raf == tempPiece.getFile().getRaf()) {
          long done = files[i].getDownloaded();
          done += tempPiece.getLength();
          files[i].setDownloaded(done);
          if (done == files[i].getLength())
            try {
              synchronized (files[i]) {
                RandomAccessFile newRaf = new RandomAccessFile(files[i].getFile(), "r");
                files[i].setRaf(newRaf);
                raf.close();
                files[i].setAccessmode(FileInfo.READ);
              }
            }
            catch (Exception e) {
              e.printStackTrace();
            }
        }
      }
    }
  }

  public String[][] getFilesStatus() {
    String[][] result = new String[files.length][2];
    for (int i = 0; i < result.length; i++) {
      result[i][0] = files[i].getFile().getAbsolutePath();
      RandomAccessFile raf = files[i].getRaf();
      result[i][1] = "";
      long length = files[i].getLength();
      long done = 0;
      for (int j = 0; j < nbPieces; j++) {
        if (!pieceDone[j])
          continue;
        //get the piece list
        List pieceList = pieceMap[j];
        //for each piece

        for (int k = 0; k < pieceList.size(); k++) {
          //get the piece and the file 
          PieceMapEntry tempPiece = (PieceMapEntry) pieceList.get(k);
          if (raf == tempPiece.getFile().getRaf()) {
            done += tempPiece.getLength();
          }
        }
      }
      int percent = 1000;
      if (length != 0)
        percent = (int) ((1000 * done) / length);
      result[i][1] = done + "/" + length + " : " + (percent / 10) + "." + (percent % 10) + " % ";
    }
    return result;
  }

  /**
   * @return
   */
  public FileInfo[] getFiles() {
    return files;
  }

  public void computePriorityIndicator() {
    for (int i = 0; i < pieceCompletion.length; i++) {
      List pieceList = pieceMap[i];
      int completion = 0;
      for (int k = 0; k < pieceList.size(); k++) {
        //get the piece and the file 
        FileInfo fileInfo = ((PieceMapEntry) pieceList.get(k)).getFile();
        //If the file is started but not completed
        if (fileInfo.isPriority())
          completion = 9;
        int percent = 0;
        if (fileInfo.getLength() != 0)
          percent = (int) ((fileInfo.getDownloaded() * 10) / fileInfo.getLength());
        if (percent > completion && percent < 10)
          completion = percent;
      }
      pieceCompletion[i] = completion;
    }

    for (int i = 0; i < priorityLists.length; i++) {
      BitSet list = priorityLists[i];
      if (list == null) {
        list = new BitSet(pieceCompletion.length + 1);
      }
      else {
        list.clear();
      }

      for (int j = 0; j < pieceCompletion.length; j++) {
        if (pieceCompletion[j] == i) {
          list.set(j);
        }
      }
      priorityLists[i] = list;
      /*      priorityLists[i][priorityLists[i][nbPieces]] = 0;
            for (int j = 0; j < pieceCompletion.length; j++) {
              if (pieceCompletion[j] == i) {
                priorityLists[i][priorityLists[i][nbPieces]++] = j;
              }
            }
      */
    }
  }

  private void constructFilesPieces() {
    for (int i = 0; i < pieceMap.length; i++) {
      List pieceList = pieceMap[i];
      //for each piece

      for (int j = 0; j < pieceList.size(); j++) {
        //get the piece and the file 
        FileInfo fileInfo = ((PieceMapEntry) pieceList.get(j)).getFile();
        if (fileInfo.getFirstPieceNumber() == -1)
          fileInfo.setFirstPieceNumber(i);
        fileInfo.setNbPieces(fileInfo.getNbPieces() + 1);
      }
    }
  }

  /**
   * @return
   */
  public String getPath() {
    return path;
  }

  /**
   * @return
   */
  public String getErrorMessage() {
    return errorMessage;
  }

  // searches from 0 to searchLength-1
  public static int binarySearch(int[] a, int key, int searchLength) {
    int low = 0;
    int high = searchLength - 1;

    while (low <= high) {
      int mid = (low + high) >> 1;
      int midVal = a[mid];

      if (midVal < key)
        low = mid + 1;
      else if (midVal > key)
        high = mid - 1;
      else
        return mid; // key found
    }
    return - (low + 1); // key not found.
  }

  public int getPiecenumberToDownload(boolean[] _piecesRarest) {
    //Added patch so that we try to complete most advanced files first.
    List pieces = new ArrayList();
    Integer pieceInteger;
    for (int i = 9; i >= 0; i--) {
      for (int j = 0; j < nbPieces; j++) {
        if (_piecesRarest[j] && priorityLists[i].get(j)) {
          pieceInteger = FlyWeightInteger.getInteger(j);
          pieces.add(pieceInteger);
        }
      }
      if (pieces.size() != 0)
        break;
    }

    if (pieces.size() == 0)
      System.out.println("Size 0");

    return ((Integer) pieces.get((int) (Math.random() * pieces.size()))).intValue();
  }

  /*
    public int getPiecenumberToDownload(boolean[] _piecesRarest) {
      int pieceNumber;
      //Added patch so that we try to complete most advanced files first.
      _priorityPieces[nbPieces] = 0;
      for (int i = priorityLists.length - 1; i >= 0; i--) {
        for (int j = 0; j < nbPieces; j++) {
          if (_piecesRarest[j] && binarySearch(priorityLists[i], j, priorityLists[i][nbPieces]) >= 0) {
            _priorityPieces[_priorityPieces[nbPieces]++] = j;
          }
        }
        if (_priorityPieces[nbPieces] != 0)
          break;
      }
    
      if (_priorityPieces[nbPieces] == 0)
        System.out.println("Size 0");
    
      int nPiece = (int) (Math.random() * _priorityPieces[nbPieces]);
      pieceNumber = _priorityPieces[nPiece];
      return pieceNumber;
    }
  */
  /**
   * @return
   */
  public Piece[] getPieces() {
    return pieces;
  }

}