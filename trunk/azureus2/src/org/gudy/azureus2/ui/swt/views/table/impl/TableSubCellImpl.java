package org.gudy.azureus2.ui.swt.views.table.impl;

import org.eclipse.swt.graphics.*;

import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.BufferedTableItem;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;
import org.gudy.azureus2.ui.swt.views.table.TableRowSWT;

import com.aelitis.azureus.ui.common.table.TableCellCore;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableRowCore;

public class TableSubCellImpl
	implements TableCellSWT
{
	private final TableRowImpl row;

	private final TableColumnCore tableColumn;

	private Comparable sortValue;

	private final int position;

	private String text;

	private int textAlpha = 255;

	private int marginWidth;

	private int marginHeight;

	private boolean disposed;

	private Object oToolTip;

	public TableSubCellImpl(TableRowImpl tableRow, TableColumnCore tableColumn,
			int position) {
		this.row = tableRow;
		this.tableColumn = tableColumn;
		this.position = position;
	}

	public Object getDataSource() {
		return row.getDataSource(tableColumn.getUseCoreDataSource());
	}

	public TableColumn getTableColumn() {
		return tableColumn;
	}

	public TableRow getTableRow() {
		return row;
	}

	public String getTableID() {
		return row.getTableID();
	}

	public boolean setText(String new_text) {
		
		if ( text == new_text ){
			
			return( false );
			
		}else if ( text == null || new_text == null ){
			
			text = new_text;
			
			invalidate();
			
			return( true );
			
		}else if ( text.equals( new_text )){
			
			return( false );
			
		}else{
		
			text = new_text;
			
			invalidate();
			
			return( true );
		}
	}

	public String getText() {
		return text == null ? "" : text;
	}

	public boolean setForeground(int red, int green, int blue) {
		return false;
	}

	public boolean setForeground(int[] rgb) {
		return false;
	}

	public boolean setForegroundToErrorColor() {
		return false;
	}

	public int[] getForeground() {
		return null;
	}

	public int[] getBackground() {
		return null;
	}

  public boolean setSortValue(Comparable valueToSort) {
    if (sortValue == valueToSort)
      return false;

    if ((valueToSort instanceof String) && (sortValue instanceof String)
				&& sortValue.equals(valueToSort)) {
			return false;
		}

    if ((valueToSort instanceof Number) && (sortValue instanceof Number)
				&& sortValue.equals(valueToSort)) {
			return false;
		}

  	//tableColumn.setLastSortValueChange(SystemTime.getCurrentTime());
    sortValue = valueToSort;

    // Columns with SWT Paint Listeners usually rely on a repaint whenever the
    // sort value changes
  	if (tableColumn.hasCellOtherListeners("SWTPaint")) {
  		redraw();
  	}

    return true;
  }
  
  public boolean setSortValue(long valueToSort) {
		if ((sortValue instanceof Long)
				&& ((Long) sortValue).longValue() == valueToSort)
			return false;

		return setSortValue(Long.valueOf(valueToSort));
  }
  
  public boolean setSortValue( float valueToSort ) {
		if (sortValue instanceof Float
				&& ((Float) sortValue).floatValue() == valueToSort)
			return false;

		return setSortValue(new Float(valueToSort));
  }

	public Comparable getSortValue() {
		return sortValue;
	}

	public boolean isShown() {
		return false;
	}

	public boolean isValid() {
		return false;
	}

	public void invalidate() {
		redraw();
	}

	public void setToolTip(Object tooltip) {
    oToolTip = tooltip;
	}

	public Object getToolTip() {
		return oToolTip;
	}

	public boolean isDisposed() {
		return disposed;
	}

	public int getMaxLines() {
		return 1;
	}

	public int getWidth() {
		return getBounds().width;
	}

	public int getHeight() {
		return getBounds().height;
	}

	public boolean setGraphic(Graphic img) {
		return false;
	}

	public Graphic getGraphic() {
		return null;
	}

	public void setFillCell(boolean bFillCell) {
	}

	public int getMarginHeight() {
		return marginHeight;
	}

	public void setMarginHeight(int height) {
		marginHeight = height;
	}

	public int getMarginWidth() {
		return marginWidth;
	}

	public void setMarginWidth(int width) {
		marginWidth = width;
	}

	public void addRefreshListener(TableCellRefreshListener listener) {
	}

	public void removeRefreshListener(TableCellRefreshListener listener) {
	}

	public void addDisposeListener(TableCellDisposeListener listener) {
	}

	public void removeDisposeListener(TableCellDisposeListener listener) {
	}

	public void addToolTipListener(TableCellToolTipListener listener) {
	}

	public void removeToolTipListener(TableCellToolTipListener listener) {
	}

	public void addMouseListener(TableCellMouseListener listener) {
	}

	public void removeMouseListener(TableCellMouseListener listener) {
	}

	public void addListeners(Object listenerObject) {
	}

	public Graphic getBackgroundGraphic() {
		return null;
	}

	public int[] getMouseOffset() {
		return new int[] { 0, 0 };
	}

	public String getClipboardText() {
		return null;
	}

	public int compareTo(Object o) {
		return 0;
	}

	public void invalidate(boolean bMustRefresh) {
	}

	public boolean refresh(boolean bDoGraphics) {
		return refresh();
	}

	public boolean refresh() {
		if (isDisposed()) {
			return false;
		}

		try {
			tableColumn.invokeCellRefreshListeners(this, false);
		} catch (Throwable e) {
			Debug.out(e);
		}
		return true;
	}

	public boolean refresh(boolean bDoGraphics, boolean bRowVisible,
			boolean bCellVisible) {
		return refresh();
	}

	public boolean refresh(boolean bDoGraphics, boolean bRowVisible) {
		return refresh();
	}

	public void dispose() {
		disposed = true;
	}

	public boolean needsPainting() {
		return true;
	}

	public void locationChanged() {
	}

	public TableRowCore getTableRowCore() {
		return row;
	}

	public void invokeToolTipListeners(int type) {
  	if (tableColumn == null) {
  		return;
  	}

    tableColumn.invokeCellToolTipListeners(this, type);
	}

	public void invokeMouseListeners(TableCellMouseEvent event) {
	}

	public void invokeVisibilityListeners(int visibility,
			boolean invokeColumnListeners) {
	}

	public void setUpToDate(boolean upToDate) {
	}

	public boolean isUpToDate() {
		return false;
	}

	public String getObfusticatedText() {
		return null;
	}

	public int getCursorID() {
		return 0;
	}

	public void setCursorID(int cursorID) {
	}

	public boolean isMouseOver() {
		return false;
	}

	public boolean getVisuallyChangedSinceRefresh() {
		return false;
	}

	public void refreshAsync() {
	}

	public void redraw() {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				Rectangle r = getBounds();
				if (r == null) {
					return;
				}
				row.getItem().getParent().redraw(r.x, r.y, r.width, r.height, true);
			}
		});
	}

	public void setDefaultToolTip(Object tt) {
	}

	public Object getDefaultToolTip() {
		return null;
	}

	public BufferedTableItem getBufferedTableItem() {
		return null;
	}

	public boolean setForeground(Color color) {
		return false;
	}

	public Image getIcon() {
		return null;
	}

	public boolean setIcon(Image img) {
		return false;
	}

	public void doPaint(GC gc) {
  	if (tableColumn != null) {
			Object[] swtPaintListeners = tableColumn.getCellOtherListeners("SWTPaint");
			if (swtPaintListeners != null) { 
  			for (int i = 0; i < swtPaintListeners.length; i++) {
  				try {
  					TableCellSWTPaintListener l = (TableCellSWTPaintListener) swtPaintListeners[i];
  
  					l.cellPaint(gc, this);
  
  				} catch (Throwable e) {
  					Debug.printStackTrace(e);
  				}
  			}
			}
		}
	}

	public Point getSize() {
		return null;
	}

	public Rectangle getBounds() {
		if (isDisposed()) {
			return new Rectangle(0, 0, 0, 0);
		}

		Rectangle bounds = row.getBounds(position);
		if (bounds == null) {
			return null;
		}
		if (marginWidth > 0) {
  		bounds.x += marginWidth;
  		bounds.width -= (marginWidth * 2);
		}
		if (marginHeight > 0) {
  		bounds.y += marginHeight;
  		bounds.height -= (marginHeight * 2);
		}
		return bounds;
	}

	public Rectangle getBoundsOnDisplay() {
		return null;
	}

	public boolean setGraphic(Image img) {
		return false;
	}

	public Image getGraphicSWT() {
		return null;
	}

	public Image getBackgroundImage() {
		return null;
	}

	public Color getForegroundSWT() {
		return null;
	}

	public TableRowSWT getTableRowSWT() {
		return null;
	}

	public Color getBackgroundSWT() {
		return null;
	}

	public int getTextAlpha() {
		return textAlpha;
	}

	public void setTextAlpha(int textOpacity) {
		textAlpha = textOpacity;
	}
}
