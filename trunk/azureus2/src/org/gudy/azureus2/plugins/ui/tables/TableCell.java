/*
 * File    : TableCell.java
 * Created : 29 nov. 2003
 * By      : Olivier
 * Adapted to MyTorrents by TuxPaper 2004/02/16
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
 
package org.gudy.azureus2.plugins.ui.tables;

import org.gudy.azureus2.plugins.ui.Graphic;

/** This interface provides access to an Azureus table cell.
 *
 * @see TableManager
 *
 * @author Oliver (Original PeerTableItem Code)
 * @author TuxPaper (Generic-izing)
 *
 * @since 2.0.8.5
 */
// Modified from MyTorrentsTableItem/PeerTableItem
public interface TableCell {
  
  /** Retrieve the data object associated with the current table row and cell.
   * The results of this method MUST NOT BE CACHED.
   * The link between a table cell and a DataSource is not persistent and can 
   * change from call to call (for example when the table is re-ordered, the 
   * link may be modified)
   *
   * @return The return type is dependent upon which table the cell is for:<br>
   *   TABLE_MYTORRENTS_*: {@link org.gudy.azureus2.plugins.download.Download}
   *                       object for the current row<br>
   *   TABLE_TORRENT_PEERS: {@link org.gudy.azureus2.plugins.peers.Peer} 
   *                        object for the current row<br>
   *   TABLE_TORRENT_FILES: {@link org.gudy.azureus2.plugins.disk.DiskManagerFileInfo}
   *                        object for the current row<br>
   *   TABLE_MYTRACKER: {@link org.gudy.azureus2.plugins.tracker.TrackerTorrent}
   *                    object for the current row<br>
   *   TABLE_MYSHARES: {@link org.gudy.azureus2.plugins.sharing.ShareResource}
   *                    object for the current row<br>
   *   remaining TABLE_* constants: undefined or null<br>
   */
  Object getDataSource();
  
  /** Retreive the TableColumn that this cell belongs to
   *
   * @return this cell's TableColumn
   */
  TableColumn getTableColumn();
  
  /** Retrieve the TableRow that this cell belongs to
   *
   * @return this cell's TableRow
   */
  TableRow getTableRow();
  
  /** Returns which table the cell is being displayed in.
   *
   * @return {@link TableManager}.TABLE_* constant
   */
  String getTableID();
  
  /**
   * This method is called to set the cell's text.
   * Caching is done, so that if same text is used several times,
   * there won't be any 'flickering' effect. Ie the text is only updated if
   * it's different from current value.
   *
   * @param text the text to be set
   * @return True - the text was updated.<br>
   *         False - the text was the same and not modified.
   */
  boolean setText(String text);

  /** Retrieve the Cell's text
   *
   * @return Cell's text
   */
  String getText();

  /** Change the cell's foreground color.
   *
   * @param red red value (0 - 255)
   * @param green green value (0 - 255)
   * @param blue blue value (0 - 255)
   * @return True - Color changed. <br>
   *         False - Color was already set.
   */
  boolean setForeground(int red, int green, int blue);

  /** Sets a Comparable object that column sorting will act on.  If you never 
   * call setSortValue, your column will be sorted by the cell's text.
   *
   * @param valueToSort the object that will be used when the column cell's
   *                    are compared to each other
   * @return True - Sort Value changed. <br>
   *         False - Sort Value was already set to object supplied.
   */
  public boolean setSortValue(Comparable valueToSort);

  /** Sets a long value that the column sorting will act on. 
   *
   * @param valueToSort sorting value.
   * @return True - Sort Value changed. <br>
   *         False - Sort Value was already set to value supplied.
   */
  public boolean setSortValue(long valueToSort);

  /** Retrieves the sorting value
   *
   * @return Object that will be sorted on
   */
  public Comparable getSortValue();

  /** Determines if the user has chosen to display the cell
   *
   * @return True - User has chosen to display cell
   */
  boolean isShown();

  /** Validility of the cell's text.
   *
   * @return True - Text is the same as last call.  You do not need to update
   *                unless you have new text to display. <br>
   *         False - Cell-to-Datasource link has changed, and the text is
   *                 definitely not valid.
   */
  boolean isValid();

  //////////////////////////////////
  // Start TYPE_GRAPHIC functions //
  //////////////////////////////////

  /** Retrieve the width of the cell's drawing area (excluding any margin) for
   * TableColumn objects of TYPE_GRAPHIC only.
   *
   * @return if you are filling the cell, this is the width your image should be
   */
  public int getWidth();

  /** Retrieve the height of the cell's drawing area (excluding any margin) for
   * TableColumn objects of TYPE_GRAPHIC only.
   *
   * @return if you are filling the cell, this is the height your image should be
   */
  public int getHeight();
  
  /** Sets the image to be drawn.
   *
   * @param img image to be stored & drawn
   * @return true - image was changed.<br>
   *         false = image was the same
   */
  public boolean setGraphic(Graphic img);

  /** Retrieve the SWT graphic related to this table item for
   * TableColumn objects of TYPE_GRAPHIC only.
   *
   * @return the Image that is draw in the cell, or null if there is none.
   */
  public Graphic getGraphic();
  
  /** TODO:
  /** Sets the image to be drawn to the file specified for
   * TableColumn objects of TYPE_GRAPHIC only.
   * 
   * @param imageLocation URI of image
   * @return true - image was changed.<br>
   *         false = image was the same
   *
  public boolean setGraphic(String imageLocation);
  */

  
  /** Sets whether the graphic fills the whole cell for
   * TableColumn objects of TYPE_GRAPHIC only. This may effect how often
   * a refresh of the cell is needed, and effects alignment.
   *
   * @param bFillCell true - the whole cell is filled by the graphic
   */
  public void setFillCell(boolean bFillCell);

  /**
   * Specifies the number of pixels of vertical margin that will
   * be placed along the top and bottom edges of the layout for
   * TableColumn objects of TYPE_GRAPHIC only.
   * <p>
   * The default is 1.
   *
   * @param height new margin height
   */
  public void setMarginHeight(int height);

  /**
   * Specifies the number of pixels of horizontal margin that will
   * be placed along the left and right edges of the layout for
   * TableColumn object of TYPE_GRAPHIC only.
   * <p>
   * The default is 1.
   *
   * @param width new margin width
   */
  public void setMarginWidth(int width);
  
  // End TYPE_GRAPHIC functions
  
  /** Adds a listener that triggers when the TableCell needs refreshing
   *
   * @param listener Listener Object to be called when refresh is needed.
   */
  public void addRefreshListener(TableCellRefreshListener listener);
  /** Removed a previously added TableCellRefreshListener
   *
   * @param listener Previously added listener
   */
  public void removeRefreshListener(TableCellRefreshListener listener);

  /** Adds a listener that triggers when the TableCell has been disposed
   *
   * @param listener listener object to be called
   */
  public void addDisposeListener(TableCellDisposeListener listener);
  /** Removed a previously added TableCellDisposeListener
   *
   * @param listener Previously added listener
   */
  public void removeDisposeListener(TableCellDisposeListener listener);
}
