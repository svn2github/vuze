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
  
  public File file;
  public RandomAccessFile raf;
  public int accessmode;
  public String name;
  public long length;
  public long downloaded;
  public int firstPieceNumber;
  public int nbPieces;
  public boolean pieces[];
}
