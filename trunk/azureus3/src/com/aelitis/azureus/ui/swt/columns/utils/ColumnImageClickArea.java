/**
 * Created on April 29, 2007 
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License only.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA 
 */

package com.aelitis.azureus.ui.swt.columns.utils;

import org.eclipse.swt.graphics.*;

import com.aelitis.azureus.ui.common.table.TableCellCore;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.swt.utils.ColorCache;
import com.aelitis.azureus.ui.swt.utils.ImageLoader;
import com.aelitis.azureus.ui.swt.utils.ImageLoaderFactory;

import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Apr 29, 2007
 *
 */
public class ColumnImageClickArea
	implements TableCellMouseMoveListener, TableRowMouseListener
{
	private String imageID;

	private final String columnID;

	private Rectangle area;

	private String id;

	private Image image;
	
	private Rectangle imageArea;
	
	private Image imgOnRow;
	
	private Image imgOffRow;

	private boolean mouseDownOn;

	private boolean containsMouse;
	
	private float scale = 1.0f;
	
	private String tooltip;

	/**
	 * @param id
	 */
	public ColumnImageClickArea(String columnID, String id, String imageID) {
		this.columnID = columnID;
		this.id = id;

		setImageID(imageID);
	}

	/**
	 * @param imageID2
	 *
	 * @since 3.0.1.5
	 */
	public void setImageID(String imageID) {
		this.imageID = imageID;
		if (imageID == null) {
			imgOffRow = null;
			imgOnRow = null;
		} else {
			ImageLoader imageLoader = ImageLoaderFactory.getInstance();
			imgOnRow = imageLoader.getImage(imageID + "-mouseonrow");
			imgOffRow = imageLoader.getImage(imageID);
			if (!ImageLoader.isRealImage(imgOnRow)) {
				imgOnRow = imgOffRow;
			}
		}
		setImage(containsMouse ? imgOnRow : imgOffRow);
	}

	public void addCell(TableCell cell) {
		cell.addListeners(this);
		TableRow row = cell.getTableRow();
		if (row != null) {
			row.addMouseListener(this);
		}
	}

	/**
	 * @return the area
	 */
	public Rectangle getArea() {
		if (area == null) {
			area = new Rectangle(0, 0, 0, 0);
		}
		return area;
	}

	/**
	 * @param area the area to set
	 */
	public void setArea(Rectangle area) {
		this.area = area;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the image
	 */
	public Image getImage() {
		return image;
	}

	/**
	 * @param image the image to set
	 */
	public void setImage(Image image) {
		if (!ImageLoader.isRealImage(image)) {
			this.image = null;
			imageArea = new Rectangle(0, 0, 0, 0);
		} else {
			this.image = image;
			imageArea = image.getBounds();
		}

		if (area == null) {
			area = new Rectangle(imageArea.x, imageArea.y, imageArea.width, imageArea.height);
			return;
		}
		area.width = (int) (imageArea.width * scale);
		area.height = (int) (imageArea.height * scale);
		//System.out.println("setImage scale " + scale + ";" + area + ";" + imageArea);
	}

	public void setPosition(int x, int y) {
		if (area == null) {
			area = new Rectangle(x, y, 0, 0);
			return;
		}
		area.x = x;
		area.y = y;
	}

	/**
	 * @param gcImage
	 *
	 * @since 3.0.1.7
	 */
	public void drawImage(GC gcImage) {
		if (containsMouse) {
			gcImage.setBackground(ColorCache.getColor(gcImage.getDevice(),
					mouseDownOn ? "#ffff00" : "#ff0000"));
			gcImage.fillRectangle(getArea());
		}
		//System.out.println("DI " + image + ";" + imageArea + ";" + area);
		if (image != null && !image.isDisposed()) {
			gcImage.drawImage(image, imageArea.x, imageArea.y, imageArea.width,
					imageArea.height, area.x, area.y, area.width, area.height);
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellMouseListener#cellMouseTrigger(org.gudy.azureus2.plugins.ui.tables.TableCellMouseEvent)
	public void cellMouseTrigger(TableCellMouseEvent event) {
		//		System.out.println(event.cell + ": " + event.eventType + ";" + event.x + "x" + event.y + "; b"
		//				+ event.button + "; " + event.keyboardState);

		if (event.eventType == TableCellMouseEvent.EVENT_MOUSEDOWN) {
			mouseDownOn = false;
			Point pt = new Point(event.x, event.y);
			mouseDownOn = getArea().contains(pt);
			TableCellCore cell = (TableCellCore) event.row.getTableCell(columnID);
			if (cell != null) {
				cell.invalidate();
				cell.refresh();
			}
		} else if (event.eventType == TableCellMouseEvent.EVENT_MOUSEUP
				&& mouseDownOn) {
			mouseDownOn = false;
			TableCellMouseEvent mouseEvent = new TableCellMouseEvent();
			mouseEvent.button = event.button;
			mouseEvent.cell = event.cell;
			mouseEvent.eventType = TableCellMouseEvent.EVENT_MOUSEUP; // EVENT_MOUSECLICK would be nice..
			mouseEvent.keyboardState = event.keyboardState;
			mouseEvent.skipCoreFunctionality = event.skipCoreFunctionality;
			mouseEvent.x = event.x; // TODO: Convert to coord relative to image?
			mouseEvent.y = event.y;
			mouseEvent.data = this;
			((TableColumnCore)event.cell.getTableColumn()).invokeCellMouseListeners(mouseEvent);
			((TableCellCore) event.cell).invokeMouseListeners(mouseEvent);
		} else if (event.eventType == TableCellMouseEvent.EVENT_MOUSEMOVE) {
			boolean contains = getArea().contains(event.x, event.y);
			setContainsMouse(event.cell, contains);
		} else if (event.eventType == TableCellMouseEvent.EVENT_MOUSEEXIT) {
			setContainsMouse(event.cell, false);
		} else if (event.eventType == TableCellMouseEvent.EVENT_MOUSEDOUBLECLICK) {
			event.skipCoreFunctionality = true;
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableRowMouseListener#rowMouseTrigger(org.gudy.azureus2.plugins.ui.tables.TableRowMouseEvent)
	public void rowMouseTrigger(TableRowMouseEvent event) {
		if (event.eventType == TableCellMouseEvent.EVENT_MOUSEEXIT) {
			setImage(imgOffRow);
			setContainsMouse(null, false);
			//System.out.println("d=" + image);
			TableCellCore cell = (TableCellCore) event.row.getTableCell(columnID);
			if (cell != null) {
				cell.invalidate();
				cell.refresh();
			}
		} else if (event.eventType == TableCellMouseEvent.EVENT_MOUSEENTER) {
			setImage(imgOnRow);
			//System.out.println("e=" + image);
			TableCellCore cell = (TableCellCore) event.row.getTableCell(columnID);
			if (cell != null) {
  			cell.invalidate();
  			cell.refresh();
			}
		}
	}
	
	private void setContainsMouse(TableCell cell, boolean contains) {
		if (containsMouse != contains) {
			containsMouse = contains;
			if (cell != null) {
				TableCellCore cellCore = (TableCellCore) cell;
				cellCore.invalidate();
				cellCore.refresh();
				if (tooltip != null) {
  				if (containsMouse) {
  					cellCore.setToolTip(tooltip);
  				} else {
  					Object oldTT = cellCore.getToolTip();
  					if (tooltip.equals(oldTT)) {
  						cellCore.setToolTip(null);
  					}
  				}
				}
			}
		}
	}

	public float getScale() {
		return scale;
	}

	public void setScale(float scale) {
		this.scale = scale;
		setImage(image);
	}

	public Rectangle getImageArea() {
		return new Rectangle(imageArea.x, imageArea.y, imageArea.width, imageArea.height);
	}

	public String getTooltip() {
		return tooltip;
	}

	public void setTooltip(String tooltip) {
		this.tooltip = tooltip;
	}
}
