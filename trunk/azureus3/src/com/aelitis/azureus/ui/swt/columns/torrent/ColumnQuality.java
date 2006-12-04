/**
 * 
 */
package com.aelitis.azureus.ui.swt.columns.torrent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTGraphic;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTGraphicImpl;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.table.TableRowCore;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;
import com.aelitis.azureus.ui.swt.columns.torrent.ColumnMediaThumb.disposableUISWTGraphic;
import com.aelitis.azureus.ui.swt.skin.SWTSkinFactory;
import com.aelitis.azureus.ui.swt.skin.SWTSkinProperties;
import com.aelitis.azureus.ui.swt.utils.ImageLoader;
import com.aelitis.azureus.ui.swt.utils.ImageLoaderFactory;

import org.gudy.azureus2.plugins.ui.Graphic;
import org.gudy.azureus2.plugins.ui.tables.*;

/**
 * @author TuxPaper
 * @created Jun 13, 2006
 *
 */
public class ColumnQuality extends CoreTableColumn implements
		TableCellAddedListener
{
	public Font font;

	/**
	 * 
	 */
	public ColumnQuality(String sTableID) {
		super("Quality", sTableID);
		initializeAsGraphic(POSITION_LAST, 40);
		setAlignment(ALIGN_CENTER);
	}

	public void cellAdded(TableCell cell) {
		new Cell(cell);
	}

	private class Cell implements TableCellRefreshListener, TableCellDisposeListener
	{
		public Cell(TableCell cell) {
			cell.addListeners(this);
			cell.setMarginWidth(0);
			cell.setMarginHeight(0);
		}
		
		public void dispose(TableCell cell) {
			disposeOld(cell);
		}

		public void refresh(TableCell cell) {
			DownloadManager dm = (DownloadManager) cell.getDataSource();
			if (dm == null) {
				return;
			}

			TOTorrent torrent = dm.getTorrent();
			if (torrent == null) {
				return;
			}
			
			String quality = PlatformTorrentUtils.getContentQuality(torrent);
			if (!cell.setSortValue(quality) && cell.isValid()) {
				return;
			}
			
			if (quality == null) {
				disposeOld(cell);
				cell.setGraphic(null);
				return;
			}

			Image img = ImageLoaderFactory.getInstance().getImage("icon.quality." + quality);
			if (ImageLoader.isRealImage(img)) {
  			UISWTGraphicImpl graphic = new UISWTGraphicImpl(img);
  			cell.setGraphic(graphic);
			} else {
				int width = cell.getWidth();
				int height = cell.getHeight();
				img = new Image(Display.getDefault(), width, height);

				GC gcImage = new GC(img);

				Color background = ((TableRowCore) cell.getTableRow()).getBackground();
				if (background != null) {
					gcImage.setBackground(background);
					gcImage.fillRectangle(0, 0, width, height);
				}

				if (font == null) {
					// no sync required, SWT is on single thread
					FontData[] fontData = gcImage.getFont().getFontData();
					fontData[0].setHeight(Utils.pixelsToPoint(12,
							Display.getDefault().getDPI().y));
					fontData[0].setStyle(SWT.BOLD);
					fontData[0].setName("Sans Serif");
					font = new Font(Display.getDefault(), fontData);
				}

				gcImage.setFont(font);

				Color color1;
				Color color2;
				int iPosition = ((TableRowCore) cell.getTableRow()).getIndex();
				boolean bOdd = ((iPosition + 1) % 2) == 0;
				String prefix = "color.rating." + ((bOdd) ? "odd" : "even");
				SWTSkinProperties skinProperties = SWTSkinFactory.getInstance().getSkinProperties();
				color1 = skinProperties.getColor(prefix + ".good.darker");
				color2 = skinProperties.getColor("color.rating.good");

				Rectangle r = img.getBounds();
				r.x += 2;
				r.y += 2;
				gcImage.setForeground(color1);

				GCStringPrinter.printString(gcImage, quality, r, true, false, SWT.CENTER);

				gcImage.setForeground(color2);
				GCStringPrinter.printString(gcImage, quality, img.getBounds(), true, false,
						SWT.CENTER);

				gcImage.dispose();

				Graphic graphic = new disposableUISWTGraphic(img);

				disposeOld(cell);

				cell.setGraphic(graphic);
			}
		}
		
		private void disposeOld(TableCell cell) {
			Graphic oldGraphic = cell.getGraphic();
			if (oldGraphic instanceof disposableUISWTGraphic) {
				Image oldImage = ((UISWTGraphic) oldGraphic).getImage();
				Utils.disposeSWTObjects(new Object[] { oldImage
				});
			}
		}
	}

	public class disposableUISWTGraphic extends UISWTGraphicImpl
	{
		public disposableUISWTGraphic(Image newImage) {
			super(newImage);
		}
	}
}
