package org.gudy.azureus2.ui.swt;

import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import org.gudy.azureus2.core2.PeerSocket;
import org.gudy.azureus2.core.PeerStats;

/**
 * This class (GUI) represents a row into the the peers table.
 * 
 * @author Olivier
 *
 */
public class PeerTableItem {

	public static final HashMap tableItems = new HashMap();

	private final Display display;
	private final Table table;
	private final PeerSocket pc;
	private TableItem item;
	private Listener listener;
	//This is used for caching purposes of the Image
	private boolean valid;
	private Image image;
	private Color colorGrey;
	private Color[] blues;
	private String[] oldTexts;

	public PeerTableItem(Table table, PeerSocket pc) {
		if (table.isDisposed()) {
			this.display = null;
			this.table = null;
			this.pc = null;
			return;
		}
		this.display = table.getDisplay();
		this.table = table;
		this.pc = pc;
		this.valid = false;
		this.blues = new Color[5];
		this.oldTexts = new String[18];
		for (int i = 0; i < oldTexts.length; i++)
			oldTexts[i] = "";

		final Table _table = table;
		display.syncExec(new Runnable() {
			public void run() {
				if (_table.isDisposed())
					return;
				item = new TableItem(_table, SWT.NULL);
				_table.getColumn(
					5).addListener(SWT.Resize, listener = new Listener() {
					public void handleEvent(Event e) {
						valid = false;
					}
				});

				colorGrey =
					new Color(_table.getDisplay(), new RGB(170, 170, 170));
				blues[4] = new Color(display, new RGB(0, 128, 255));
				blues[3] = new Color(display, new RGB(64, 160, 255));
				blues[2] = new Color(display, new RGB(128, 192, 255));
				blues[1] = new Color(display, new RGB(192, 224, 255));
				blues[0] = new Color(display, new RGB(255, 255, 255));

				item.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent e) {
						if (image != null && !image.isDisposed())
							image.dispose();
						if (colorGrey != null && !colorGrey.isDisposed())
							colorGrey.dispose();
						if (blues[0] != null && !blues[0].isDisposed())
							blues[0].dispose();
						if (blues[1] != null && !blues[1].isDisposed())
							blues[1].dispose();
						if (blues[2] != null && !blues[2].isDisposed())
							blues[2].dispose();
						if (blues[3] != null && !blues[3].isDisposed())
							blues[3].dispose();
						if (blues[4] != null && !blues[4].isDisposed())
							blues[4].dispose();

					}
				});

			}

		});

		tableItems.put(item, this);
	}

	public synchronized void updateImage() {

		if (display == null || display.isDisposed())
			return;

		//A small hack to insure valid won't pass twice with false value in the loop.
		final boolean _valid = this.valid;
		this.valid = true;

		if (item == null || item.isDisposed())
			return;
		//Compute bounds ...
		Rectangle bounds = item.getBounds(5);
		int width = bounds.width - 1;
		int x0 = bounds.x;
		int y0 = bounds.y + 1;
		int height = bounds.height - 3;
		if (width < 10 || height < 3)
			return;
		//Get the table GC
		GC gc = new GC(table);
		gc.setClipping(table.getClientArea());
		if (_valid) {
			//If the image is still valid, simply copy it :)
			gc.setForeground(colorGrey);
			gc.drawImage(image, x0, y0);
			gc.drawRectangle(new Rectangle(x0, y0, width, height));
			gc.dispose();
		} else {
			//Image is not valid anymore ... so 1st free it :)
			if (image != null && !image.isDisposed())
				image.dispose();
			image = new Image(display, width, height);

			//System.out.println(table.getHeaderHeight());

			GC gcImage = new GC(image);
			boolean available[] = pc.getAvailable();
			if (available != null) {
				int nbPieces = available.length;

				for (int i = 0; i < width; i++) {
					int a0 = (i * nbPieces) / width;
					int a1 = ((i + 1) * nbPieces) / width;
					if (a1 == a0)
						a1++;
					if (a1 > nbPieces)
						a1 = nbPieces;
					int nbAvailable = 0;
					for (int j = a0; j < a1; j++)
						if (available[j])
							nbAvailable++;
					int index = (nbAvailable * 4) / (a1 - a0);
					//System.out.print(index);
					gcImage.setBackground(blues[index]);
					Rectangle rect = new Rectangle(i, 1, 1, height);
					gcImage.fillRectangle(rect);
				}
			}
			gcImage.dispose();
			gc.setForeground(colorGrey);
			gc.drawImage(image, x0, y0);
			gc.drawRectangle(new Rectangle(x0, y0, width, height));
			gc.dispose();
		}
	}

	public void updateAll() {

		if (display == null || display.isDisposed())
			return;
		/*display.asyncExec( new Runnable() {
		  public void run()
		  {*/
		if (item == null || item.isDisposed())
			return;

		String tmp;

		tmp = "";
		if (pc.isSnubbed())
			tmp = "*";
		if (!(oldTexts[14].equals(tmp))) {
			item.setText(14, tmp);
			oldTexts[14] = tmp;
			if (pc.isSnubbed())
				item.setForeground(colorGrey);
			else
				item.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
		}

		if ((oldTexts[0].equals(""))) {
			tmp = pc.getIp();
			item.setText(0, tmp);
			oldTexts[0] = tmp;
		}

		if ((oldTexts[1].equals(""))) {
			tmp = "" + pc.getPort();
			item.setText(1, tmp);
			oldTexts[1] = tmp;
		}

		if (oldTexts[2].equals("")) {
			tmp = "l";
			boolean isIcoming = pc.isIncoming();
			if (isIcoming)
				tmp = "r";
			item.setText(2, tmp);
			oldTexts[2] = tmp;
		}

		tmp = "";
		if (pc.isInterested())
			tmp = "*";
		if (!(oldTexts[3].equals(tmp))) {
			item.setText(3, tmp);
			oldTexts[3] = tmp;
		}

		tmp = "";
		if (pc.isChoked())
			tmp = "*";
		if (!(oldTexts[4].equals(tmp))) {
			item.setText(4, tmp);
			oldTexts[4] = tmp;
		}

		tmp = "";
		if (pc.isInteresting())
			tmp = "*";
		if (!(oldTexts[9].equals(tmp))) {
			item.setText(9, tmp);
			oldTexts[9] = tmp;
		}

		tmp = "";
		if (pc.isChoking())
			tmp = "*";
		if (!(oldTexts[10].equals(tmp))) {
			item.setText(10, tmp);
			oldTexts[10] = tmp;
		}

		boolean available[] = pc.getAvailable();
		int sum = 0;
		int availabilityLevel = 0;
		int availability[] = pc.getManager().getAvailability();
		for (int i = 0; i < available.length; i++) {
			if (available[i]) {
				if (availability[i] > 0)
					availabilityLevel += 100 / availability[i];
				sum++;
			}
		}
		if (sum > 0)
			availabilityLevel /= sum;
		sum = (sum * 1000) / (available.length);
		tmp = (sum / 10) + "." + (sum % 10) + " %";
		if (!(oldTexts[6].equals(tmp))) {
			item.setText(6, tmp);
			oldTexts[6] = tmp;
		}

		tmp = "" + availabilityLevel;
		if (!(oldTexts[15].equals(tmp))) {
			item.setText(15, tmp);
			oldTexts[15] = tmp;
		}

		tmp = "" + pc.getClient();
		if (!(oldTexts[17].equals(tmp))) {
			item.setText(17, tmp);
			oldTexts[17] = tmp;
		}

	}

	public void updateStats() {

		if (display == null || display.isDisposed())
			return;

		if (item == null || item.isDisposed())
			return;
		String tmp;
		PeerStats stats = pc.getStats();

		tmp = stats.getReceptionSpeed();
		if (!(oldTexts[7].equals(tmp))) {
			item.setText(7, tmp);
			oldTexts[7] = tmp;
		}

		tmp = stats.getTotalReceived();
		if (!(oldTexts[8].equals(tmp))) {
			item.setText(8, tmp);
			oldTexts[8] = tmp;
		}

		tmp = stats.getSendingSpeed();
		if (!(oldTexts[11].equals(tmp))) {
			item.setText(11, tmp);
			oldTexts[11] = tmp;
		}

		tmp = stats.getTotalSent();
		if (!(oldTexts[12].equals(tmp))) {
			item.setText(12, tmp);
			oldTexts[12] = tmp;

		}

		tmp = stats.getStatisticSent();
		if (!(oldTexts[13].equals(tmp))) {
			item.setText(13, tmp);
			oldTexts[13] = tmp;
		}
		/*
		tmp = "";
		if(pc.isOptimisticUnchoke()) tmp = "*";
		    if (!(oldTexts[16].equals(tmp))) {
		      item.setText(16, tmp);
		      oldTexts[16] = tmp;
		    }*/

	}

	public void remove() {
		if (display == null || display.isDisposed())
			return;
		display.syncExec(new Runnable() {
			public void run() {
				if (colorGrey != null && !colorGrey.isDisposed())
					colorGrey.dispose();
				if (blues != null) {
					for (int i = 0; i < blues.length; i++) {
						if (blues[i] != null && !blues[i].isDisposed())
							blues[i].dispose();
					}
				}
				if (table == null || table.isDisposed())
					return;
				if (item == null || item.isDisposed())
					return;
				table.getColumn(5).removeListener(SWT.Resize, listener);
				table.remove(table.indexOf(item));
				item.dispose();
				tableItems.remove(item);
			}
		});
	}

	public void invalidate() {
		this.valid = false;
	}

	public boolean isSnubbed() {
		return this.pc.isSnubbed();
	}

	public void setSnubbed(boolean value) {
		this.pc.setSnubbed(value);
	}

}