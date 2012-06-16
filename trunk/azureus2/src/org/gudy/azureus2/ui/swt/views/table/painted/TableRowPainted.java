package org.gudy.azureus2.ui.swt.views.table.painted;

import java.util.LinkedHashMap;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.impl.TableCellSWTBase;
import org.gudy.azureus2.ui.swt.views.table.impl.TableRowSWTBase;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnSWTUtils;

import com.aelitis.azureus.ui.common.table.TableCellCore;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableRowCore;

public class TableRowPainted
	extends TableRowSWTBase
{
	private static final boolean DEBUG_SUBS = true;

	private static Font fontBold;

	private Point drawOffset = new Point(0, 0);

	private int numSubItems;

	private Object[] subDataSources;

	private TableRowPainted[] subRows;

	private Object subRows_sync;

	private int subRowsHeight;
	
	TableCellCore cellSort;

	final static public Color[] alternatingColors = new Color[] {
		null,
		Colors.colorAltRow
	};

	private int height = 0;
	
	private boolean initializing = true;
	

	public TableRowPainted(TableRowCore parentRow, TableViewPainted tv,
			Object dataSource, boolean triggerHeightChange) {
		// in theory, TableRowPainted could have it's own sync
		// but in practice, I end up calling code within the sync which inevitably
		// calls the TableView and causes locks.  So, use the TableView's sync!

		super(tv.getSyncObject(), parentRow, tv, dataSource);
		subRows_sync = tv.getSyncObject();

		TableColumnCore sortColumn = tv.getSortColumn();
		if (parentRow == null
				|| sortColumn.handlesDataSourceType(getDataSource(false).getClass())) {
			cellSort = new TableCellPainted(TableRowPainted.this, sortColumn, sortColumn.getPosition());
		}
		//buildCells();

		if (height == 0) {
			setHeight(tv.getRowDefaultHeight(), false);
		}
		initializing = false;
		if (triggerHeightChange) {
			heightChanged(0, height);
		}
	}

	private void buildCells() {
		//debug("buildCells " + Debug.getCompressedStackTrace());
		TableColumnCore[] visibleColumns = getView().getVisibleColumns();
		synchronized (lock) {
  		mTableCells = new LinkedHashMap<String, TableCellCore>(
  				visibleColumns.length, 1);
  
  		TableColumn currentSortColumn = null;
  		if (cellSort != null) {
  			currentSortColumn = cellSort.getTableColumn();
  		}
  		TableRowCore parentRow = getParentRowCore();
  		// create all the cells for the column
  		for (int i = 0; i < visibleColumns.length; i++) {
  			if (visibleColumns[i] == null) {
  				continue;
  			}
  
  			if (parentRow != null
  					&& !visibleColumns[i].handlesDataSourceType(getDataSource(false).getClass())) {
  				mTableCells.put(visibleColumns[i].getName(), null);
  				continue;
  			}
  			
  			//System.out.println(dataSource + ": " + tableColumns[i].getName() + ": " + tableColumns[i].getPosition());
  			TableCellCore cell = (currentSortColumn != null && visibleColumns[i].equals(currentSortColumn))
  					? cellSort : new TableCellPainted(TableRowPainted.this,
  							visibleColumns[i], i);
  			mTableCells.put(visibleColumns[i].getName(), cell);
  			//if (i == 10) cell.bDebug = true;
  		}
		}
	}
	
	private void destroyCells() {
		synchronized (lock) {
			if (mTableCells != null) {
				for (TableCellCore cell : mTableCells.values()) {
					if (cell != null && cell != cellSort) {
						cell.dispose();
					}
				}
				mTableCells = null;
			}
		}
	}

	public TableViewPainted getViewPainted() {
		return (TableViewPainted) getView();
	}

	public void paintControl(GC gc, Region rgn, Rectangle drawBounds, int rowStartX, int rowStartY, int pos) {
		Color origBG = gc.getBackground();
		Color origFG = gc.getForeground();
		if (isSelected()) {
			Color color;
			if (isFocused()) {
				// TODO: Do something special for selected and focused (like a border)
				color = gc.getDevice().getSystemColor(SWT.COLOR_LIST_SELECTION);
			} else {
				color = getViewPainted().getTableComposite().isFocusControl()
						? Colors.blues[3] : Colors.blues[1];
			}
			gc.setBackground(color);
		} else {
			Color color;
			if (isFocused()) {
				color = gc.getDevice().getSystemColor(
						SWT.COLOR_LIST_SELECTION);
			} else {
				color = alternatingColors[pos >= 0 ? pos % 2 : 0];
			}

			if (color != null) {
				gc.setBackground(color);
			} else {
				gc.setBackground(gc.getDevice().getSystemColor(
						SWT.COLOR_LIST_BACKGROUND));
			}
		}

		Color bg = getBackground();
		if (bg == null) {
			bg = gc.getBackground();
		} else {
			gc.setBackground(bg);
		}
		Color fg = getForeground();
		if (fg == null) {
			if (isSelected()) {
				fg = gc.getDevice().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT);
				gc.setForeground(fg);
			} else {
				fg = gc.getForeground();
			}
		} else {
			gc.setForeground(fg);
		}

		int rowAlpha = getAlpha();
		gc.setAlpha(rowAlpha);

		Font font = gc.getFont();

		int x = rowStartX;
		boolean paintedRow = false;
		TableColumnCore[] visibleColumns = getView().getVisibleColumns();
		synchronized (lock) {
  		if (mTableCells != null) {
    		for (TableColumn tc : visibleColumns) {
    			TableCellCore cell = mTableCells.get(tc.getName());
    			int w = tc.getWidth();
    			if (cell == null || cell.isDisposed()) {
    				gc.fillRectangle(x, rowStartY, w, getHeight());
    				x += w;
    				continue;
    			}
    			TableCellSWTBase cellSWT = (TableCellSWTBase) cell;
    			if (!cellSWT.hasFlag(TableCellSWTBase.FLAG_PAINTED)) {
    				Rectangle r = new Rectangle(x, rowStartY, w, getHeight());
    				((TableCellPainted) cell).setBoundsRaw(r);
    				if (rgn.intersects(r)) {
    					paintedRow = true;
    					gc.fillRectangle(r);
    					if (paintCell(gc, cellSWT.getBounds(), cellSWT)) {
    						gc.setBackground(bg);
    						gc.setForeground(fg);
    						gc.setAlpha(rowAlpha);
    						gc.setFont(font);
    					}
    					if (DEBUG_ROW_PAINT) {
    						((TableCellSWTBase) cell).debug("painted "
    								+ (cell.getVisuallyChangedSinceRefresh() ? "VC" : "!UpToDate")
    								+ " @ " + r);
    					}
    					cellSWT.setFlag(TableCellSWTBase.FLAG_PAINTED);
    				} else {
    					if (DEBUG_ROW_PAINT) {
    						((TableCellSWTBase) cell).debug("Skip paintItem; no intersects; r="
    								+ r + ";rgn=" + rgn.getBounds() + " from "
    								+ Debug.getCompressedStackTrace(4));
    					}
    				}
    			} else {
    				if (DEBUG_ROW_PAINT) {
    					((TableCellSWTBase) cell).debug("Skip paintItem; up2date? "
    							+ cell.isUpToDate() + "; from "
    							+ Debug.getCompressedStackTrace(4));
    				}
    			}
    
    			x += w;
    		}
  		}
  		int w = drawBounds.width - x;
  		if (w > 0) {
  			gc.fillRectangle(x, rowStartY, w, getHeight());
  		}

		}
		
//		if (paintedRow) {
//			//debug("Paint " + e.x + "x" + e.y + " " + e.width + "x" + e.height + ".." + e.count + ";clip=" + e.gc.getClipping() +";drawOffset=" + drawOffset + " via " + Debug.getCompressedStackTrace());
//		}

		gc.setAlpha(255);
		gc.setBackground(origBG);
		gc.setForeground(origFG);
	}

	private boolean paintCell(GC gc, Rectangle cellBounds, TableCellSWTBase cell) {
		boolean gcChanged = false;
		try {
			
			gc.setTextAntialias(SWT.ON);

			TableViewSWT<?> view = (TableViewSWT<?>) getView();

			TableColumnCore column = (TableColumnCore) cell.getTableColumn();
			view.invokePaintListeners(gc, this, column, cellBounds);

			int fontStyle = getFontStyle();
			if (fontStyle == SWT.BOLD) {
				//gc.setFont(getFontBold(gc));
				gcChanged = true;
			}

			if (!cell.isUpToDate()) {
				//System.out.println("R " + rowIndex + ":" + iColumnNo);
				cell.refresh(true, true);
				//return;
			}

			String text = cell.getText();

			Color fg = cell.getForegroundSWT();
			if (fg != null) {
				gcChanged = true;
				gc.setForeground(fg);
			}
			Color bg = cell.getBackgroundSWT();
			if (bg != null) {
				gcChanged = true;
				gc.setBackground(bg);
			}

			//if (cell.getTableColumn().getClass().getSimpleName().equals("ColumnUnopened")) {
			//	System.out.println("FOOO" + cell.needsPainting());
			//}
			if (cell.needsPainting()) {
				Image graphicSWT = cell.getGraphicSWT();
				if (graphicSWT != null && !graphicSWT.isDisposed()) {
					Rectangle imageBounds = graphicSWT.getBounds();

					Rectangle graphicBounds = new Rectangle(cellBounds.x, cellBounds.y,
							cellBounds.width, cellBounds.height);
					if (cell.getFillCell()) {
						if (!graphicBounds.isEmpty()) {
							gc.setAdvanced(true);
							//System.out.println(imageBounds + ";" + graphicBounds);
							gc.drawImage(graphicSWT, 0, 0, imageBounds.width,
									imageBounds.height, graphicBounds.x, graphicBounds.y,
									graphicBounds.width, graphicBounds.height);
						}
					} else {

						if (imageBounds.width < graphicBounds.width) {
							int alignment = column.getAlignment();
							if ((alignment & TableColumn.ALIGN_CENTER) > 0) {
								graphicBounds.x += (graphicBounds.width - imageBounds.width) / 2;
							} else if ((alignment & TableColumn.ALIGN_TRAIL) > 0) {
								graphicBounds.x = (graphicBounds.x + graphicBounds.width)
										- imageBounds.width;
							}
						}

						if (imageBounds.height < graphicBounds.height) {
							graphicBounds.y += (graphicBounds.height - imageBounds.height) / 2;
						}

						gc.drawImage(graphicSWT, graphicBounds.x, graphicBounds.y);
					}

				}
				cell.doPaint(gc);
				gcChanged = true;
			}
			if (text.length() > 0) {
				int ofsx = 0;
				Image image = cell.getIcon();
				Rectangle imageBounds = null;
				if (image != null && !image.isDisposed()) {
					imageBounds = image.getBounds();
					int ofs = imageBounds.width;
					ofsx += ofs;
					cellBounds.x += ofs;
					cellBounds.width -= ofs;
				}
				//System.out.println("PS " + rowIndex + ";" + cellBounds + ";" + cell.getText());
				int style = TableColumnSWTUtils.convertColumnAlignmentToSWT(column.getAlignment());
				if (cellBounds.height > 20) {
					style |= SWT.WRAP;
				}
				int textOpacity = cell.getTextAlpha();
				if (textOpacity < 255) {
					gc.setTextAntialias(SWT.ON);
					gc.setAlpha(textOpacity);
					gcChanged = true;
				} else if (textOpacity > 255) {
					//gc.setFont(getFontBold(gc));
					gc.setTextAntialias(SWT.ON);
					//gc.setAlpha(textOpacity & 255);
					gcChanged = true;
				}
				// put some padding on text
				ofsx += 6;
				cellBounds.x += 3;
				cellBounds.width -= 6;
				cellBounds.y += 2;
				cellBounds.height -= 4;
				if (!cellBounds.isEmpty()) {
					GCStringPrinter sp = new GCStringPrinter(gc, text, cellBounds, true,
							cellBounds.height > 20, style);

					boolean fit = sp.printString();
					if (fit) {

						cell.setDefaultToolTip(null);
					} else {

						cell.setDefaultToolTip(text);
					}

					Point size = sp.getCalculatedSize();
					size.x += ofsx;

					TableColumn tableColumn = cell.getTableColumn();
					if (tableColumn != null && tableColumn.getPreferredWidth() < size.x) {
						tableColumn.setPreferredWidth(size.x);
					}

					if (imageBounds != null) {
						int drawToY = cellBounds.y + (cellBounds.height / 2)
								- (imageBounds.height / 2);
						if ((style & SWT.RIGHT) > 0) {
							int drawToX = cellBounds.x + cellBounds.width - size.x;
							gc.drawImage(image, drawToX, drawToY);
						} else {
							gc.drawImage(image, cellBounds.x - imageBounds.width - 3, drawToY);
						}
					}
				} else {
					cell.setDefaultToolTip(null);
				}
			}
			cell.clearVisuallyChangedSinceRefresh();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return gcChanged;
	}

	private static Font getFontBold(GC gc) {
		if (fontBold == null) {
			FontData[] fontData = gc.getFont().getFontData();
			for (int i = 0; i < fontData.length; i++) {
				FontData fd = fontData[i];
				fd.setStyle(SWT.BOLD);
			}
			fontBold = new Font(gc.getDevice(), fontData);
		}
		return fontBold;
	}

	@Override
	public List<TableCellCore> refresh(boolean bDoGraphics, boolean bVisible) {
		final List<TableCellCore> invalidCells = super.refresh(bDoGraphics,
				bVisible);
		if (invalidCells.size() > 0) {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					Composite composite = getViewPainted().getComposite();
					if (composite == null || composite.isDisposed()) {
						return;
					}
					for (Object o : invalidCells) {
						if (o instanceof TableCellPainted) {
							TableCellPainted cell = (TableCellPainted) o;
							cell.clearFlag(TableCellSWTBase.FLAG_PAINTED);
							Rectangle bounds = cell.getBoundsRaw();
							if (bounds != null) {
								getViewPainted().swt_updateCanvasImage(bounds, false);
							}
						}
					}
				}
			});
		}
		return invalidCells;
	}
	
	public void redraw(boolean doChildren) {
		redraw(doChildren, false);
	}

	public void redraw(boolean doChildren, boolean immediateRedraw) {
		clearCellFlag(TableCellSWTBase.FLAG_PAINTED, false);
		getViewPainted().redrawRow(this, immediateRedraw);

		if (!doChildren) {
			return;
		}
		synchronized (subRows_sync) {
			if (subRows != null) {
				for (TableRowPainted subrow : subRows) {
					subrow.redraw();
				}
			}
		}
	}

	protected void debug(String s) {
		AEDiagnosticsLogger diag_logger = AEDiagnostics.getLogger("table");
		String prefix = SystemTime.getCurrentTime() + ":" + getTableID() + ": r"
				+ getIndex();
		if (getParentRowCore() != null) {
			prefix += "of" + getParentRowCore().getIndex();
		}
		prefix += ": ";
		diag_logger.log(prefix + s);

		System.out.println(prefix + s);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.views.table.impl.TableRowSWTBase#getBounds()
	 */
	@Override
	public Rectangle getBounds() {
		//TableViewPainted view = (TableViewPainted) getView();
		//Rectangle clientArea = view.getClientArea();
		return new Rectangle(0, drawOffset.y, 9990, getHeight());
	}

	public Rectangle getDrawBounds() {
		TableViewPainted view = (TableViewPainted) getView();
		Rectangle clientArea = view.getClientArea();
		Rectangle bounds = new Rectangle(0, drawOffset.y - clientArea.y, 9990, getHeight());
		return bounds;
	}

	public int getFullHeight() {
		int h = getHeight();
		if (numSubItems > 0 && isExpanded()) {
			h += subRowsHeight;
		}
		return h;
	}

	public Point getDrawOffset() {
		return drawOffset;
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.views.table.impl.TableRowSWTBase#heightChanged(int, int)
	 */
	public void heightChanged(int oldHeight, int newHeight) {
		getViewPainted().rowHeightChanged(this, oldHeight, newHeight);
		TableRowCore row = getParentRowCore();
		if (row instanceof TableRowPainted) {
			((TableRowPainted) row).subRowHeightChanged(this, oldHeight, newHeight);
		}
	}
	
	public void subRowHeightChanged(TableRowCore row, int oldHeight, int newHeight) {
		subRowsHeight += (newHeight - oldHeight);
	}

	public boolean setDrawOffset(Point drawOffset) {
		if (drawOffset.equals(this.drawOffset)) {
			return false;
		}
		this.drawOffset = drawOffset;

//		synchronized (subRows_sync) {
//			if (subRows != null) {
//				int y = drawOffset.y + getHeight();
//				for (TableRowPainted subrow : subRows) {
//					subrow.setDrawOffset(new Point(drawOffset.x, y));
//					y += subrow.getHeight();
//				}
//			}
//		}

		return true;
		//debug("setDrawOffset " + drawOffset);
	}

	@Override
	public void setWidgetSelected(boolean selected) {
		redraw(false, true);
	}
	
	@Override
	public void setShown(boolean b, boolean force) {
		synchronized (lock) {
  		if (b && mTableCells == null) {
  			buildCells();
  		}
		}
		
		super.setShown(b, force);
		if (!b && mTableCells != null) {
			destroyCells();
		}
//		synchronized (subRows_sync) {
//			if (subRows != null) {
//				for (TableRowPainted subrow : subRows) {
//					subrow.setShown(b, force);
//				}
//			}
//		}
	}
	
	private void deleteExistingSubRows() {
		synchronized (subRows_sync) {
			if (subRows != null) {
				for (TableRowPainted subrow : subRows) {
					subrow.delete();
				}
			}
			subRows = null;
		}
	}

	public void setSubItemCount(int length) {
		numSubItems = length;
		if (isExpanded() && subDataSources.length == length) {
			if (DEBUG_SUBS) {
				debug("setSubItemCount to " + length);
			}
			
			deleteExistingSubRows();
			TableRowPainted[] newSubRows = new TableRowPainted[length];
			TableViewPainted tv = getViewPainted();
			int h = 0;
			for (int i = 0; i < newSubRows.length; i++) {
				newSubRows[i] = new TableRowPainted(this, tv, subDataSources[i], false);
				newSubRows[i].setTableItem(i, false);
				h += newSubRows[i].getHeight();
			}

			int oldHeight = getFullHeight();
			subRowsHeight = h;
			getViewPainted().rowHeightChanged(this, oldHeight, getFullHeight());
			getViewPainted().triggerListenerRowAdded(newSubRows);
			
			subRows = newSubRows;
		}
	}

	public int getSubItemCount() {
		return numSubItems;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.common.table.TableRowCore#linkSubItem(int)
	 */
	public TableRowCore linkSubItem(int indexOf) {
		// Not used by TableViewPainted
		return null;
	}

	public void setSubItems(Object[] datasources) {
		deleteExistingSubRows();
		subDataSources = datasources;
		subRowsHeight = 0;
		setSubItemCount(datasources.length);
	}

	public TableRowCore[] getSubRowsWithNull() {
		synchronized (subRows_sync) {
			return subRows == null ? new TableRowCore[0] : subRows;
		}
	}

	public void removeSubRow(Object datasource) {
		synchronized (subRows_sync) {

			for (int i = 0; i < subDataSources.length; i++) {
				Object ds = subDataSources[i];
				if (ds == datasource) { // use .equals instead?
					TableRowPainted rowToDel = subRows[i];
					TableRowPainted[] newSubRows = new TableRowPainted[subRows.length - 1];
					System.arraycopy(subRows, 0, newSubRows, 0, i);
					System.arraycopy(subRows, i + 1, newSubRows, i, subRows.length - i
							- 1);
					subRows = newSubRows;

					Object[] newDatasources = new Object[subRows.length];
					System.arraycopy(subDataSources, 0, newDatasources, 0, i);
					System.arraycopy(subDataSources, i + 1, newDatasources, i,
							subDataSources.length - i - 1);
					subDataSources = newDatasources;

					rowToDel.delete();

					setSubItemCount(subRows.length);

					break;
				}
			}
		}
	}
	
	@Override
	public void setExpanded(boolean b) {
		int oldHeight = getFullHeight();
		super.setExpanded(b);
		synchronized (subRows_sync) {
			TableRowPainted[] newSubRows = null;
  		if (b && (subRows == null || subRows.length != numSubItems)
  				&& subDataSources != null && subDataSources.length == numSubItems) {
  			if (DEBUG_SUBS) {
  				debug("building subrows " + numSubItems);
  			}
  
  			deleteExistingSubRows();
  			newSubRows = new TableRowPainted[numSubItems];
  			TableViewPainted tv = getViewPainted();
  			int h = 0;
  			for (int i = 0; i < newSubRows.length; i++) {
  				newSubRows[i] = new TableRowPainted(this, tv, subDataSources[i], false);
  				newSubRows[i].setTableItem(i, false);
  				h += newSubRows[i].getHeight();
  			}
  			
  			subRowsHeight = h;
  			
  			subRows = newSubRows;
  		}

			getViewPainted().rowHeightChanged(this, oldHeight, getFullHeight());
			
			if (newSubRows != null) {
				getViewPainted().triggerListenerRowAdded(newSubRows);
			}

		}
		if (isVisible()) {
			getViewPainted().visibleRowsChanged();
			getViewPainted().redrawTable();
		}
	}

	public TableRowCore getSubRow(int pos) {
		synchronized (subRows_sync) {
			if (subRows == null) {
				return null;
			}
			if (pos >= 0 && pos < subRows.length) {
				return subRows[pos];
			}
			return null;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.gudy.azureus2.ui.swt.views.table.impl.TableRowSWTBase#setForeground(org.eclipse.swt.graphics.Color)
	 */
	@Override
	public boolean setForeground(Color c) {
		//TODO
		return false;
	}
	

	@Override
	public boolean setIconSize(Point pt) {
		//TODO
		return false;
	}

	@Override
	public Color getForeground() {
		//TODO
		return null;
	}

	@Override
	public Color getBackground() {
		//TODO
		return null;
	}

	@Override
	public void setBackgroundImage(Image image) {
		//TODO
	}

	

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.common.table.TableRowCore#getHeight()
	 */
	public int getHeight() {
		return height == 0 ? getView().getRowDefaultHeight() : height;
	}

	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.common.table.TableRowCore#setHeight(int)
	 */
	public boolean setHeight(int newHeight) {
		return setHeight(newHeight, true);
	}
	
	public boolean setHeight(int newHeight, boolean trigger) {
		if (height == newHeight) {
			return false;
		}
		int oldHeight = height;
		height = newHeight;
		if (trigger && !initializing) {
			heightChanged(oldHeight, newHeight);
		}

		return true;
	}
	
	@Override
	public TableCellCore getTableCellCore(String name) {
		if (isRowDisposed()) {
			return null;
		}
		synchronized (lock) {
			if (mTableCells == null) {
  			if (cellSort != null && !cellSort.isDisposed()
  					&& cellSort.getTableColumn().getName().equals(name)) {
    			return cellSort;
    		} else {
    			return null;
    		}
			}
  		return mTableCells.get(name);
		}
	}
	
	@Override
	public TableCellSWT getTableCellSWT(String name) {
		TableCellCore cell = getTableCellCore(name);
		return (cell instanceof TableCellSWT) ? (TableCellSWT) cell : null;
	}
	
	@Override
	public TableCell getTableCell(String field) {
		return getTableCellCore(field);
	}
	
	public TableCellCore getSortColumnCell(String hint) {
		return cellSort;
	}
	
	public void setSortColumn(String columnID) {
		synchronized (lock) {
		
  		if (mTableCells == null) {
  			if (cellSort != null && !cellSort.isDisposed()) {
    			if (cellSort.getTableColumn().getName().equals(columnID)) {
    				return;
    			}
    			cellSort.dispose();
    			cellSort = null;
  			}
  			TableColumnCore sortColumn = (TableColumnCore) getView().getTableColumn(columnID);
  			if (getParentRowCore() == null
  					|| sortColumn.handlesDataSourceType(getDataSource(false).getClass())) {
  				cellSort = new TableCellPainted(TableRowPainted.this, sortColumn, sortColumn.getPosition());
  			} else {
  				cellSort = null;
  			}
  		} else {
  			cellSort = mTableCells.get(columnID);
  		}
  	}
	}
}
