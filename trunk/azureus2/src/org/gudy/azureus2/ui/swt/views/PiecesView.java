/*
 * Created on 2 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.peer.PEPiece;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.views.tableitems.PieceTableItem;

/**
 * @author Olivier
 * 
 */
public class PiecesView extends AbstractIView implements DownloadManagerListener {

  DownloadManager manager;
  Table table;
  HashMap items;

  public PiecesView(DownloadManager manager) {
    this.manager = manager;
    items = new HashMap();
    lastField = "#";
    ascending = true;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#initialize(org.eclipse.swt.widgets.Composite)
   */
  public void initialize(Composite composite) {
    table = new Table(composite, SWT.SINGLE | SWT.FULL_SELECTION);
    table.setLinesVisible(false);
    table.setHeaderVisible(true);
    String[] titlesPieces =
      { "#", "size", "numberofblocks", "blocks", "completed", "availability" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
    int[] alignPieces =
      { SWT.LEFT, SWT.RIGHT, SWT.RIGHT, SWT.CENTER, SWT.RIGHT, SWT.RIGHT };
    for (int i = 0; i < titlesPieces.length; i++) {
      TableColumn column = new TableColumn(table, alignPieces[i]);
      Messages.setLanguageText(column, "PiecesView." + titlesPieces[i]); //$NON-NLS-1$
    }
    table.getColumn(0).setWidth(50);
    table.getColumn(1).setWidth(60);
    table.getColumn(2).setWidth(65);
    table.getColumn(3).setWidth(300);
    table.getColumn(4).setWidth(80);
    table.getColumn(5).setWidth(80);
    
    table.getColumn(0).addListener(SWT.Selection, new IntColumnListener("#")); //$NON-NLS-1$
    table.getColumn(1).addListener(SWT.Selection, new IntColumnListener("size")); //$NON-NLS-1$
    table.getColumn(2).addListener(SWT.Selection, new IntColumnListener("nbBlocs")); //$NON-NLS-1$
    table.getColumn(3).addListener(SWT.Selection, new IntColumnListener("done")); //$NON-NLS-1$
    table.getColumn(4).addListener(SWT.Selection, new IntColumnListener("done")); //$NON-NLS-1$
    table.getColumn(5).addListener(SWT.Selection, new IntColumnListener("availability")); //$NON-NLS-1$

//    manager.addListener(this);
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getComposite()
   */
  public Composite getComposite() {
    return table;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#refresh()
   */
  public void refresh() {
    if(getComposite() == null || getComposite().isDisposed())
      return;
      
    reOrder(false);

    synchronized (items) {
      Iterator iter = items.values().iterator();
      while (iter.hasNext()) {
        PieceTableItem pti = (PieceTableItem) iter.next();
        pti.updateDisplay();
      }
    }
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#delete()
   */
  public void delete() {
    manager.removeListener(this);
    Iterator iter = items.values().iterator();
    while (iter.hasNext()) {
      PieceTableItem item = (PieceTableItem) iter.next();
      item.remove();
    }
    if(table != null && ! table.isDisposed())
        table.dispose();
  }

  public String getData() {
    return "PiecesView.title.short"; //$NON-NLS-1$
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getFullTitle()
   */
  public String getFullTitle() {
    return MessageText.getString("PiecesView.title.full"); //$NON-NLS-1$
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IComponentListener#objectAdded(java.lang.Object)
   */
  public void pieceAdded(PEPiece created) {
    synchronized (items) {
      if (items.containsKey(created))
        return;
      PieceTableItem item = new PieceTableItem(table, (PEPiece) created);
      items.put(created, item);
    }
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IComponentListener#objectRemoved(java.lang.Object)
   */
  public void pieceRemoved(PEPiece removed) {
    //System.out.println("removed : " + removed.getClass() + ":" + removed);
    PieceTableItem item;
    synchronized (items) {
      item = (PieceTableItem) items.remove(removed);
    }
    if (item == null)
      return;
    item.remove();
  }
  
  public void
  peerAdded(
	  PEPeer 		peer )
  {
  }
		
  public void
  peerRemoved(
	  PEPeer		peer )
  {
  }

  //Ordering
   private boolean ascending;
   private String lastField;

   private int loopFactor;
  
   private void reOrder(boolean force) {
     if(!force && loopFactor++ < 20)
       return;
     loopFactor = 0;
     if(lastField != null) {
       ascending = !ascending;
       orderInt(lastField);
     }    
   }

   private void orderInt(String field) {
     computeAscending(field);
     synchronized (items) {      
       List selected = getSelection();                
       List ordered = new ArrayList(items.size());
       PieceTableItem _items[] = new PieceTableItem[items.size()];
       Iterator iter = items.keySet().iterator();
       while (iter.hasNext()) {
         PEPiece piece = (PEPiece) iter.next();
         PieceTableItem item = (PieceTableItem) items.get(piece);
         int index = item.getIndex();
         _items[index] = item;
         long value = getIntField(piece, field);
         int i;
         for (i = 0; i < ordered.size(); i++) {
           PEPiece piecei = (PEPiece) ordered.get(i);
           long valuei = getIntField(piecei, field);
           if (ascending) {
             if (valuei >= value)
               break;
           }
           else {
             if (valuei <= value)
               break;
           }
         }
         ordered.add(i, piece);
       }

       sort(_items, ordered, selected);
           
     }
     refresh();
   }
  
   private List getSelection() {
     TableItem[] selection = table.getSelection();
     List selected = new ArrayList(selection.length);
     for(int i = 0 ; i < selection.length ; i++) {                
       PEPiece piece = (PEPiece) items.get(selection[i]);
       if(piece != null)
         selected.add(piece);
     }
     return selected;
   }

   private void sort(PieceTableItem[] _items, List ordered, List selected) {
     for (int i = 0; i < ordered.size(); i++) {
       PEPiece piece = (PEPiece) ordered.get(i);

       _items[i].setPiece(piece);

       items.put(piece, _items[i]);
       //managers.put(items[i].getTableItem(), manager);
       if(selected.contains(piece)) {
         table.select(i);
       } else {
         table.deselect(i);
       }         
     }
   }

   private void computeAscending(String field) {
     if (lastField.equals(field))
       ascending = !ascending;
     else {
       lastField = field;
       ascending = true;
     }
   }

   private class IntColumnListener implements Listener {

     private String field;

     public IntColumnListener(String field) {
       this.field = field;
     }

     public void handleEvent(Event e) {
       orderInt(field);
     }
   }
   
  private long getIntField(PEPiece piece, String field) {

      if (field.equals("#")) //$NON-NLS-1$
        return piece.getPieceNumber();

      if (field.equals("size")) //$NON-NLS-1$
        return piece.getLength();

      if (field.equals("nbBlocs")) //$NON-NLS-1$
        return piece.getNbBlocs();

      if (field.equals("done")) //$NON-NLS-1$
        return piece.getCompleted();

      if (field.equals("availability")) //$NON-NLS-1$
        return piece.getManager().getAvailability(piece.getPieceNumber());

      return 0;
    }

}
