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
import java.util.ArrayList;
import java.util.HashMap;
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
  private int pieceLength;
  private int lastPieceLength;

  private String piecesHash;
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
  private List priorityLists[];

  private FileInfo[] files;

  //long[] filesDone;
  //RandomAccessFile[] fileArray;

  private PeerManager manager;

  private boolean bContinue = true;

  public DiskManager(Map metaData, String path) {
    this.state = INITIALIZING;
    this.percentDone = 0;
    this.metaData = metaData;
    this.path = path;
    Thread init = new Thread() {
      public void run() {
        initialize();
      }
    };
    init.setPriority(Thread.MIN_PRIORITY);
    init.start();
  }

  private void initialize() {

    //1. parse the metaData object.
    Map info = (Map) metaData.get("info");
    pieceLength = (int) ((Long) info.get("piece length")).longValue();

    try {
      piecesHash = new String((byte[]) info.get("pieces"), "ISO-8859-1");
    }
    catch (UnsupportedEncodingException e) {
      this.state = FAULTY;
      return;
    }

    nbPieces = piecesHash.length() / 20;
    //  create the pieces map
    pieceMap = new ArrayList[nbPieces];
    pieceCompletion = new int[nbPieces];
    priorityLists = new List[10];

    pieceDone = new boolean[nbPieces];

    fileName = "";
    try {
      fileName = new String((byte[]) info.get("name"), "ISO-8859-1");
    }
    catch (UnsupportedEncodingException e) {
      this.state = FAULTY;
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
      int currentPiece = 0;

      //get the root
      rootPath = fileName;
      rootPath = rootPath + System.getProperty("file.separator");

      //:: Directory patch 08062003 - Tyler
      //check for a user selecting the full path
      String fullPath = path + System.getProperty("file.separator");
      if (fullPath.lastIndexOf(rootPath) == (fullPath.length() - rootPath.length())) {
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

        StringBuffer pathBuffer = new StringBuffer();
        //build the path string
        for (int j = 0; j < fileList.size(); j++) {
          //attach every element
          if (j != (fileList.size() - 1)) //are we the filename?
            {
            try {
              pathBuffer.append(new String((byte[]) fileList.get(j), "ISO-8859-1"));
            }
            catch (UnsupportedEncodingException e) {
              this.state = FAULTY;
              return;
            }

            pathBuffer.append(System.getProperty("file.separator"));
          }
          else //no, then we must be a part of the path
            {
            //add the file entry to the file holder list 
            try {
              btFileList.add(
                new BtFile(pathBuffer.toString(), new String((byte[]) fileList.get(j), "ISO-8859-1"), fileLength));
            }
            catch (UnsupportedEncodingException e) {
              this.state = FAULTY;
              return;
            }
          }
        }

        //increment the global length 
        totalLength += fileLength;
        //clear the memory
        fileList = null;
      }
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
    for (int i = 0; i < nbPieces - 1; i++) {
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
    ConfigurationManager config = ConfigurationManager.getInstance();
    boolean resumeEnabled = config.getBooleanParameter("Use Resume", false);
    byte[] resumeArray = null;
    Map resumeMap = (Map) metaData.get("resume");
    if (resumeMap != null) {
      Map resumeDirectory = (Map) resumeMap.get(this.path);
      if (resumeDirectory != null)
        resumeArray = (byte[]) resumeDirectory.get("resume data");
    }

    if (resumeEnabled && (resumeArray != null) && (resumeArray.length == pieceDone.length)) {
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
    }
    else //no resume data.. rebuild it
      {
      for (int i = 0; i < nbPieces && bContinue; i++) {
        percentDone = ((i + 1) * 1000) / nbPieces;
        checkPiece(i);
      }
      //Patch:: dump the newly built resume data to the disk / torrent --Tyler
      if (bContinue && ConfigurationManager.getInstance().getBooleanParameter("Use Resume", false))
        this.dumpResumeDataToDisk();
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

  private class BtFile {
    private FileInfo _file;
    private String _path;
    private String _name;
    private long _length;

    public BtFile(String path, String name, long length) {
      _path = path;
      _length = length;
      _name = name;
    }
    public long getLength() {
      return _length;
    }
    public String getPath() {
      return _path;
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

  private class PieceMapEntry {
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

  public class WriteElement {
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
        while (readQueue.size() > 0) {
          DataQueueItem item = (DataQueueItem) readQueue.remove(0);
          Request request = item.getRequest();
          item.setBuffer(readBlock(request.getPieceNumber(), request.getOffset(), request.getLength()));
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
    }
  }

  public class DiskWriteThread extends Thread {
    private boolean bContinue = true;

    public DiskWriteThread() {
      super("Disk Writer & Checker");
    }

    public void run() {
      while (bContinue) {
        while (writeQueue.size() > 0) {
          WriteElement elt = (WriteElement) writeQueue.remove(0);
          dumpBlockToDisk(elt);
          manager.blockWritten(elt.getPieceNumber(), elt.getOffset());
        }
        if (checkQueue.size() > 0) {
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
    }
  }

  private boolean allocateFiles(String rootPath, List fileList) {
    this.state = ALLOCATING;
    allocated = 0;
    boolean newFiles = false;
    for (int i = 0; i < fileList.size(); i++) {
      //get the BtFile
      BtFile tempFile = (BtFile) fileList.get(i);
      //get the path
      String tempPath = path + System.getProperty("file.separator") + rootPath + tempFile.getPath();
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
          raf = new RandomAccessFile(f, "rw");
          raf.setLength(length);
        }
        catch (Exception e) {
          this.state = FAULTY;
          return false;
        }

        ConfigurationManager config = ConfigurationManager.getInstance();
        boolean allocateNew = config.getBooleanParameter("Allocate New", true);
        if (allocateNew)
          clearFile(raf);
        newFiles = true;
      }
      else {
        try {
          raf = new RandomAccessFile(f, "rw");
        }
        catch (FileNotFoundException e) {
          this.state = FAULTY;
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

    int length = 0;
    if (pieceNumber < nbPieces - 1) {
      length = pieceLength;
    }
    else {
      length = lastPieceLength;
    }

    allocateAndTestBuffer.position(0);

    if (pieceNumber < nbPieces - 1) {
      allocateAndTestBuffer.limit(pieceLength);
    }
    else {
      allocateAndTestBuffer.limit(lastPieceLength);
    }

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
          fc.position(tempPiece.getOffset());
          fc.read(allocateAndTestBuffer);
        }
        catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    try {
      SHA1Hasher hasher = new SHA1Hasher();
      allocateAndTestBuffer.position(0);
      String testHash = new String(hasher.calculateHash(allocateAndTestBuffer), "ISO-8859-1");
      String correctHash = piecesHash.substring(pieceNumber * 20, pieceNumber * 20 + 20);
      if (testHash.equals(correctHash)) {
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

  public void dumpResumeDataToDisk() {
    //TODO CLEAN UP
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
    Map resumeMap = (Map) metaData.get("resume");
    if (resumeMap == null) {
      resumeMap = new HashMap();
      metaData.put("resume", resumeMap);
    }
    Map resumeDirectory = new HashMap();
    resumeMap.put(path, resumeDirectory);
    resumeDirectory.put("resume data", resumeData);

    //TODO:: CLEAN UP - fix the conversion to a string...
    //open the torrent file    		
    File torrent = null;
    try {
      torrent = new File(new String((byte[]) metaData.get("torrent filename"), "ISO-8859-1"));
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
    }
    catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    //write the data out
    try {
      fos.write(torrentData);
      fos.close();
    }
    catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
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
        byte[] data = new byte[(int) tempPiece.getLength()];
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
          fc.position(fileOffset + (long) (offset - previousFilesLength));
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
          fc.position(fileOffset + (long) (offset - previousFilesLength));
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
      File torrent = new File(new String((byte[]) metaData.get("torrent filename"), "ISO-8859-1"));
      //re-encode the data
      byte[] torrentData = BEncoder.encode(metaData);
      //open a file stream
      FileOutputStream fos = new FileOutputStream(torrent);
      //write the data out
      fos.write(torrentData);
    }
    catch (Exception e) {
      e.printStackTrace();
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
    writeThread.stopIt();
    readThread.stopIt();
    this.bContinue = false;
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
        if (fileInfo.getLength() > 0)
          percent = (int) ((fileInfo.getDownloaded() * 10) / fileInfo.getLength());
        if (percent > completion && percent < 10)
          completion = percent;
      }
      pieceCompletion[i] = completion;
    }

    for (int i = 0; i < priorityLists.length; i++) {
      ArrayList list = new ArrayList();
      for (int j = 0; j < pieceCompletion.length; j++) {
        if (pieceCompletion[j] == i) {
          list.add(new Integer(j));
        }
      }
      priorityLists[i] = list;
    }
  }

  public List[] getPriorityLists() {
    return priorityLists;
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

}