/**
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.vuze.tests.swt;

import java.io.*;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.util.TorrentUtils;

import com.aelitis.azureus.core.torrent.PlatformTorrentUtils;

/**
 * @author TuxPaper
 * @created Sep 28, 2006
 *
 */
public class TorrentThumbnail
{
	public static byte[] b;

	public static TOTorrent torrent;

	public static String sFileName;

	public static void main(String[] args) {
		final Display display = new Display();
		final Shell shell = new Shell(display, SWT.DIALOG_TRIM);

		shell.setLayout(new FillLayout());

		final Label image = new Label(shell, SWT.NONE);

		Button btnGetTorrent = new Button(shell, SWT.PUSH);
		btnGetTorrent.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(shell);
				sFileName = fd.open();
				try {
					torrent = TorrentUtils.readFromFile(new File(sFileName), false);
				} catch (TOTorrentException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				b = PlatformTorrentUtils.getContentThumbnail(torrent);
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		btnGetTorrent.setText("Get Torrent");

		Button btnShowImage = new Button(shell, SWT.PUSH);
		btnShowImage.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				if (b == null) {
					System.out.println("null");
					return;
				}
				ByteArrayInputStream bis = new ByteArrayInputStream(b);
				Image img = new Image(display, bis);
				image.setImage(img);
				shell.layout(true, true);
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		btnShowImage.setText("Show Image");

		Button btnGetFile = new Button(shell, SWT.PUSH);
		btnGetFile.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(shell);
				String sTFileName = fd.open();
				b = getFileContents(sTFileName);
				PlatformTorrentUtils.setContentThumbnail(torrent, b);
				System.out.println("yay");
				try {
					TorrentUtils.writeToFile(torrent, new File(sFileName));
				} catch (TOTorrentException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		btnGetFile.setText("Add thumb to torrent");

		shell.open();

		while (!shell.isDisposed()) {
			if (display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	public static byte[] getFileContents(String sFile) {
		File f = new File(sFile);

		try {
			FileInputStream fs = new FileInputStream(f);

			byte b[] = new byte[(int) f.length()];

			fs.read(b);

			fs.close();

			return b;

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}
}
