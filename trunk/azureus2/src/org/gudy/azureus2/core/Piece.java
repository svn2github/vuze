package org.gudy.azureus2.core;


/**
 * Represents a Piece and the status of its different chunks (un-requested, requested, downloaded, written).
 * 
 * @author Olivier
 *
 */
public class Piece {
  private static final int blocSize = 16384;

  public int length;
  public int nbBlocs;
  public int pieceNumber;

  public int lastBlocSize;

  public boolean[] downloaded;
  public boolean[] requested;
  public boolean[] written;
  public int completed;
  public boolean isBeingChecked = false;

  // Note by Moti:
  // TODO: find some way of removing this! The only place it's actually accessed is in 1 place in the UI (org.gudy.azureus2.ui.swt.PiecesView)  
  public PeerManager manager;

  public Piece(PeerManager manager, int length) {
    this.manager = manager;
    //System.out.println("Creating Piece of Size " + length); 
    this.length = length;
    nbBlocs = (length + blocSize - 1) / blocSize;
    downloaded = new boolean[nbBlocs];
    requested = new boolean[nbBlocs];
    written = new boolean[nbBlocs];

    if ((length % blocSize) != 0)
      lastBlocSize = length % blocSize;
    else
      lastBlocSize = blocSize;

  }

  public Piece(PeerManager manager, int length, int pieceNumber) {
    this(manager, length);
    this.pieceNumber = pieceNumber;
  }

  public void setWritten(int blocNumber) {
    written[blocNumber] = true;
    completed++;
  }

  public boolean isComplete() {
    boolean complete = true;
    for (int i = 0; i < nbBlocs; i++) {
      complete = complete && written[i];
    }
    return complete;
  }

  public boolean isWritten(int blockNumber) {
    return written[blockNumber];
  }

  public void setBloc(int blocNumber) {
    downloaded[blocNumber] = true;    
  }

  // This method is used to clear the requested information
  public void clearRequested(int blocNumber) {
    requested[blocNumber] = false;
  }

  // This method will return the first non requested bloc and
  // will mark it as requested
  public synchronized int getAndMarkBlock() {
    int blocNumber = -1;
    for (int i = 0; i < nbBlocs; i++) {
      if (!requested[i] && !written[i]) {
        blocNumber = i;
        requested[i] = true;

        //To quit loop.
        i = nbBlocs;
      }
    }
    return blocNumber;
  }

  public synchronized void unmarkBlock(int blocNumber) {
    if (!downloaded[blocNumber])
      requested[blocNumber] = false;
  }

  public int getBlockSize(int blocNumber) {
    if (blocNumber == (nbBlocs - 1))
      return lastBlocSize;
    return blocSize;
  }

  public void free() {
  }

  public int getCompleted() {
    return completed;
  }

  public void setBeingChecked() {
    this.isBeingChecked = true;
  }

  public boolean isBeingChecked() {
    return this.isBeingChecked;
  }

  /**
   * @param manager
   */
  public void setManager(PeerManager manager) {
    this.manager = manager;
  }

}