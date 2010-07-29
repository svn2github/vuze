package org.gudy.azureus2.ui.swt.views.table.impl;

import java.util.Random;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Widget;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemTime;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.table.*;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnSWTUtils;

import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.swt.utils.ColorCache;

public class TableViewSWT_PaintItem
	implements Listener
{
	Widget lastItem;

	int lastRowIndex = -1;

	private final TableOrTreeSWT table;

	private TableViewSWTImpl<?> tv;

	private Font fontBold;

	public TableViewSWT_PaintItem(TableViewSWTImpl<?> tv, TableOrTreeSWT table) {
		this.table = table;
		this.tv = tv;
	}

	public void handleEvent(Event event) {

		if (event.gc.getClipping().isEmpty()) {
			return;
		}

		if (!table.isEnabled()) {
			// added disable affect
			event.gc.setAlpha(100);
		}

		table.setData("inPaintItem", event.item);
		table.setData("curCellIndex", event.index);

		if (event.item != lastItem) {
			table.setData("lastIndex", null);
			lastRowIndex = table.indexOf(event.item);
			table.setData("lastIndex", lastRowIndex);
		}

		//visibleRowsChanged();
		paintItem(event, lastRowIndex);

		lastItem = event.item;
		table.setData("inPaintItem", null);
		table.setData("curCellBounds", null);
	}

	/**
	 * @param event
	 * @param rowIndex 
	 */
	protected void paintItem(Event event, int rowIndex) {
		//if (event.index == 1 && rowIndex == 0) {
		//	System.out.println("paintItem " + event.gc.getClipping() +":" + rowIndex + ":" + event.detail + ": " + Debug.getCompressedStackTrace());
		//}
		try {
			//System.out.println(event.gc.getForeground().getRGB().toString());
			//System.out.println("paintItem " + event.gc.getClipping());
			if (TableViewSWTImpl.DEBUG_CELL_CHANGES) {
				Random random = new Random(SystemTime.getCurrentTime() / 500);
				event.gc.setBackground(ColorCache.getColor(event.gc.getDevice(),
						210 + random.nextInt(45), 210 + random.nextInt(45),
						210 + random.nextInt(45)));
				event.gc.fillRectangle(event.gc.getClipping());
			}

			TableItemOrTreeItem item = TableOrTreeUtils.getEventItem(event.item);
			if (item == null || item.isDisposed()) {
				return;
			}
			int iColumnNo = event.index;

			//System.out.println(SystemTime.getCurrentTime() + "] paintItem " + table.indexOf(item) + ":" + iColumnNo);
			if (tv.bSkipFirstColumn) {
				if (iColumnNo == 0) {
					return;
				}
				iColumnNo--;
			}

			TableColumnCore[] columnsOrdered = tv.getColumnsOrdered();

			if (iColumnNo >= columnsOrdered.length) {
				System.out.println("Col #" + iColumnNo + " >= " + columnsOrdered.length
						+ " count");
				return;
			}

			if (!tv.isColumnVisible(columnsOrdered[iColumnNo])) {
				//System.out.println("col not visible " + iColumnNo);
				return;
			}

			//if (rowIndex < tv.lastTopIndex || rowIndex > tv.lastBottomIndex) {
				// this refreshes whole row (perhaps multiple), saving the many
				// cell.refresh calls later because !cell.isUpToDate()
				//tv.visibleRowsChanged();
			//}

			Rectangle cellBounds = item.getBounds(event.index);

			//System.out.println("cb=" + cellBounds + ";b=" + event.getBounds() + ";clip=" + event.gc.getClipping());
			Rectangle origClipping = event.gc.getClipping();

			if (origClipping.isEmpty()
					|| (origClipping.width >= cellBounds.width && origClipping.height >= cellBounds.height)) {
				table.setData("fullPaint", Boolean.TRUE);
			} else {
				table.setData("fullPaint", Boolean.FALSE);
				//System.out.println("not full paint: " + origClipping + ";cellbounds=" + cellBounds);
			}

			table.setData("curCellBounds", cellBounds);

			TableRowSWT row = (TableRowSWT) tv.getRow(item);
			if (row == null) {
				//System.out.println("no row");
				return;
			}
			
			if (!tv.isRowVisible(row)) {
				tv.visibleRowsChanged();
			}

			tv.invokePaintListeners(event.gc, row, columnsOrdered[iColumnNo],
					cellBounds);

			int rowAlpha = row.getAlpha();

			int fontStyle = row.getFontStyle();
			if (fontStyle == SWT.BOLD) {
				event.gc.setFont(getFontBold(event.gc));
			}

			//if (item.getImage(event.index) != null) {
			//	cellBounds.x += 18;
			//	cellBounds.width -= 18;
			//}

			if (cellBounds.width <= 0 || cellBounds.height <= 0) {
				//System.out.println("no bounds");
				return;
			}
			
			TableCellSWT cell = row.getTableCellSWT(columnsOrdered[iColumnNo].getName());

			if (cell == null) {
				return;
			}

			if (!cell.isUpToDate()) {
				//System.out.println("R " + table.indexOf(item) + ":" + iColumnNo);
				cell.refresh(true, true);
				//return;
			}

			String text = cell.getText();

			Rectangle clipping = new Rectangle(cellBounds.x, cellBounds.y,
					cellBounds.width, cellBounds.height);
			// Cocoa calls paintitem while row is below tablearea, and painting there
			// is valid!
			if (!Utils.isCocoa) {
				int iMinY = tv.headerHeight + tv.clientArea.y;

				if (clipping.y < iMinY) {
					clipping.height -= iMinY - clipping.y;
					clipping.y = iMinY;
				}
				int iMaxY = tv.clientArea.height + tv.clientArea.y;
				if (clipping.y + clipping.height > iMaxY) {
					clipping.height = iMaxY - clipping.y + 1;
				}
			}

			if (clipping.width <= 0 || clipping.height <= 0) {
				//System.out.println(row.getIndex() + " clipping="+clipping + ";" );
				return;
			}

			event.gc.setClipping(clipping);

			if (rowAlpha < 255) {
				event.gc.setAlpha(rowAlpha);
			}

			if (cell.needsPainting()) {
				cell.doPaint(event.gc);
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
				//System.out.println("PS " + table.indexOf(item) + ";" + cellBounds + ";" + cell.getText());
				int style = TableColumnSWTUtils.convertColumnAlignmentToSWT(columnsOrdered[iColumnNo].getAlignment());
				if (cellBounds.height > 20) {
					style |= SWT.WRAP;
				}
				int textOpacity = cell.getTextAlpha();
				if (textOpacity < 255) {
					event.gc.setTextAntialias(SWT.ON);
					event.gc.setAlpha(textOpacity);
				} else if (textOpacity > 255) {
					event.gc.setFont(getFontBold(event.gc));
					event.gc.setTextAntialias(SWT.ON);
					event.gc.setAlpha(textOpacity & 255);
				}
				// put some padding on text
				ofsx += 6;
				cellBounds.x += 3;
				cellBounds.width -= 6;
				if (!cellBounds.isEmpty()) {
					GCStringPrinter sp = new GCStringPrinter(event.gc, text, cellBounds,
							true, cellBounds.height > 20, style);

					boolean fit = sp.printString();
					if (fit) {

						cell.setDefaultToolTip(null);
					} else {

						cell.setDefaultToolTip(text);
					}

					Point size = sp.getCalculatedSize();
					size.x += ofsx;

					if (cell.getTableColumn().getPreferredWidth() < size.x) {
						cell.getTableColumn().setPreferredWidth(size.x);
					}
					
					if (imageBounds != null) {
						int drawToY = cellBounds.y + (cellBounds.height / 2)
								- (imageBounds.height / 2);
						if ((style & SWT.RIGHT) > 0) {
							int drawToX = cellBounds.x + cellBounds.width - size.x;
							event.gc.drawImage(image, drawToX, drawToY);
						} else {
							event.gc.drawImage(image, cellBounds.x - imageBounds.width - 3, drawToY);
						}
					}
				} else {
					cell.setDefaultToolTip(null);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Font getFontBold(GC gc) {
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

}
