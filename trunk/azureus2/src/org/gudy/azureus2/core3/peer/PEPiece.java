package org.gudy.azureus2.core3.peer;

/**
 * Represents a Piece and the status of its different chunks (un-requested, requested, downloaded, written).
 * 
 * @author Olivier
 *
 */

public interface 
PEPiece 
{  
  public PEPeerManager
  getManager();

  public void setWritten(int blocNumber);
 
  public boolean isComplete();


  public boolean isWritten(int blockNumber);
 

  public void setBloc(int blocNumber);


  // This method is used to clear the requested information
  public void clearRequested(int blocNumber);


  // This method will return the first non requested bloc and
  // will mark it as requested
  public int getAndMarkBlock();


  public void unmarkBlock(int blocNumber);
  

  public int getBlockSize(int blocNumber);
  public int getPieceNumber();
  public int getLength();
  public int getNbBlocs();  

  public void free();
 
  public int getCompleted();
 
  public boolean[] getWritten();
  public boolean[] getRequested();

  public void setBeingChecked();
 

  public boolean isBeingChecked();
 
  public void setManager(PEPeerManager manager);
 
}