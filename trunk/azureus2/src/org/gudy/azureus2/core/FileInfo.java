/*
 * Created on 3 juil. 2003
 *
 */
package org.gudy.azureus2.core;

import java.io.File;
import java.io.RandomAccessFile;

/**
 * @author Olivier
 * 
 */
public class FileInfo {
  
  public static final int READ = 1;
  public static final int WRITE = 2;
  
  private File file;  
  private RandomAccessFile raf;
  private int accessmode;
  private String path;
  private String name;
  private String extension;
  private long length;
  private long downloaded;
  private int firstPieceNumber = -1;
  private int nbPieces = 0;
  
  private boolean priority = false;  

  /**
   * @return
   */
  public int getAccessmode() {
    return accessmode;
  }

  /**
   * @return
   */
  public long getDownloaded() {
    return downloaded;
  }

  /**
   * @return
   */
  public String getExtension() {
    return extension;
  }

  /**
   * @return
   */
  public File getFile() {
    return file;
  }

  /**
   * @return
   */
  public int getFirstPieceNumber() {
    return firstPieceNumber;
  }

  /**
   * @return
   */
  public long getLength() {
    return length;
  }

  /**
   * @return
   */
  public String getName() {
    return name;
  }

  /**
   * @return
   */
  public int getNbPieces() {
    return nbPieces;
  }


  /**
   * @return
   */
  public RandomAccessFile getRaf() {
    return raf;
  }

  /**
   * @param i
   */
  public void setAccessmode(int i) {
    accessmode = i;
  }

  /**
   * @param l
   */
  public void setDownloaded(long l) {
    downloaded = l;
  }

  /**
   * @param string
   */
  public void setExtension(String string) {
    extension = string;
  }

  /**
   * @param file
   */
  public void setFile(File file) {
    this.file = file;
  }

  /**
   * @param i
   */
  public void setFirstPieceNumber(int i) {
    firstPieceNumber = i;
  }

  /**
   * @param l
   */
  public void setLength(long l) {
    length = l;
  }

  /**
   * @param string
   */
  public void setName(String string) {
    name = string;
  }

  /**
   * @param i
   */
  public void setNbPieces(int i) {
    nbPieces = i;
  }

  /**
   * @param file
   */
  public void setRaf(RandomAccessFile file) {
    raf = file;
  }

  /**
   * @return
   */
  public String getPath() {
    return path;
  }

  /**
   * @param string
   */
  public void setPath(String string) {
    path = string;
  }

  /**
   * @return
   */
  public boolean isPriority() {
    return priority;
  }

  /**
   * @param b
   */
  public void setPriority(boolean b) {
    priority = b;
  }

}
