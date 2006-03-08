/*
 * Created on Mar 7, 2006 10:42:32 PM
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.shells;

import java.util.ArrayList;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;

/**
 * 
 * +============================+
 * | +----+                     |
 * | |Icon| Big Bold Title      |
 * | +----+                     |
 * | Wrapping message text      |
 * | with optional URL links    |
 * | +-----+                    |
 * | |BGImg|                    |
 * | | Icon| Closing in XX secs |
 * | +-----+    [Details] [Hide]|
 * +============================+ 
 * 
 * @author TuxPaper
 * @created Mar 7, 2006
 *
 */
public class MessageSlideShell {
	private final static boolean USE_SWT32_BG_SET = true && !(Constants.isLinux && SWT
			.getVersion() <= 3224);

	private final static int EDGE_GAP = 0;

	private final static int SHELL_DEF_WIDTH = 280;

	private final static int SHELL_MIN_HEIGHT = 150;

	private final static int DETAILS_WIDTH = 550;

	private final static int DETAILS_HEIGHT = 300;

	private final static ArrayList popupList = new ArrayList(2);

	private final static AEMonitor popupList_mon = new AEMonitor("popupList_mon");

	private Shell shell;

	private Display display;

	private Label lblCloseIn;

	private MessageSlideShell slideInAfter = null;

	private boolean delayPaused = false;

	private Rectangle slideInAfterEndBounds;

	private Button btnHideAll;

	/** Open a popup using resource keys for title/text
	 * 
	 * @param keyPrefix message bundle key prefix used to get title and text.  
	 *         Title will be keyPrefix + ".title", and text will be set to
	 *         keyPrefix + ".text"
	 * @param details actual text for details (not a key)
	 * @param textParams any parameters for text
	 * 
	 * @note Display moved to end to remove conflict in constructors
	 */
  public MessageSlideShell(Display display, int iconID, String keyPrefix,
			String details, String[] textParams) {
  	this(display, iconID, MessageText.getString(keyPrefix + ".title"),
				MessageText.getString(keyPrefix + ".text", textParams), details);
	}

	/**
	 * Default constructor
	 * 
	 * @param display
	 * @param iconID
	 * @param title
	 * @param text
	 * @param details
	 */
	public MessageSlideShell(final Display display, int iconID, String title,
			String text, String details) {
		GridData gridData;
		int shellWidth;

		// Load Images
		Image imgPopup = ImageRepository.getImage("popup");
		Rectangle imgPopupBounds;
		if (imgPopup != null) {
			shellWidth = imgPopup.getBounds().width;
			imgPopupBounds = imgPopup.getBounds();
		} else {
			shellWidth = SHELL_DEF_WIDTH;
			imgPopupBounds = null;
		}
		Image imgIcon = null;
		switch (iconID) {
			case SWT.ICON_ERROR:
				imgIcon = ImageRepository.getImage("error");
				break;

			case SWT.ICON_WARNING:
				imgIcon = ImageRepository.getImage("warning");
				break;

			case SWT.ICON_INFORMATION:
				imgIcon = ImageRepository.getImage("info");
				break;

			default:
				imgIcon = null;
				break;
		}

		final MouseTrackAdapter mouseAdapter = new MouseTrackAdapter() {
			public void mouseEnter(MouseEvent e) {
				delayPaused = true;
			}

			public void mouseExit(MouseEvent e) {
				delayPaused = false;
			}
		};

		// Create shell & widgets
		shell = new Shell(display, SWT.ON_TOP);
		this.display = shell.getDisplay();
		if (USE_SWT32_BG_SET) {
			try {
				shell.setBackgroundMode(SWT.INHERIT_DEFAULT);
			} catch (NoSuchMethodError e) {
				// Ignore
			}
		}
		Utils.setShellIcon(shell);

		FormLayout shellLayout = new FormLayout();
		shell.setLayout(shellLayout);

		final Composite cShell = new Composite(shell, SWT.NULL);
		GridLayout layout = new GridLayout(2, false);
		cShell.setLayout(layout);

		Label label = new Label(cShell, SWT.NONE);
		label.setImage(imgIcon);
		label.setLayoutData(new GridData());

		label = new Label(cShell, SWT.WRAP);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		label.setLayoutData(gridData);
		label.setText(title);
		FontData[] fontData = label.getFont().getFontData();
		fontData[0].setStyle(SWT.BOLD);
		fontData[0].setHeight((int) (fontData[0].getHeight() * 1.5));
		final Font boldFont = new Font(display, fontData);
		label.setFont(boldFont);

		try {
			Link linkLabel = new Link(cShell, SWT.WRAP);
			gridData = new GridData(GridData.FILL_BOTH);
			gridData.horizontalSpan = 2;
			linkLabel.setLayoutData(gridData);
			linkLabel.setText(text);
			linkLabel.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (e.text.endsWith(".torrent"))
						TorrentOpener.openTorrent(e.text);
					else
						Program.launch(e.text);
				}
			});
		} catch (Throwable t) {
			// 3.0
			Label linkLabel = new Label(cShell, SWT.WRAP);
			gridData = new GridData(GridData.FILL_BOTH);
			gridData.horizontalSpan = 2;
			linkLabel.setLayoutData(gridData);

			//<a href="http://atorre.s">test</A> and <a href="http://atorre.s">test2</A>
			
			text = java.util.regex.Pattern.compile("<A HREF=\"(.+?)\">(.+?)</A>",
					Pattern.CASE_INSENSITIVE).matcher(text).replaceAll("$2 ($1)");

			if (details == null) {
				details = text;
			} else {
				details = text + "\n" + details;
			}

			linkLabel.setText(text);
		}

		lblCloseIn = new Label(cShell, SWT.TRAIL);
		gridData = new GridData(SWT.FILL, SWT.TOP, true, false);
		gridData.horizontalSpan = 2;
		lblCloseIn.setLayoutData(gridData);

		final Composite cButtons = new Composite(cShell, SWT.NULL);
		RowLayout rowLayout = new RowLayout(SWT.HORIZONTAL);
		rowLayout.marginBottom = 0;
		rowLayout.marginLeft = 0;
		rowLayout.marginRight = 0;
		rowLayout.marginTop = 0;
		cButtons.setLayout(rowLayout);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
		gridData.horizontalSpan = 2;
		cButtons.setLayoutData(gridData);

		btnHideAll = new Button(cButtons, SWT.PUSH);
		Messages.setLanguageText(btnHideAll, "popup.error.hideall");
		btnHideAll.setVisible(false);
		btnHideAll.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				cButtons.setEnabled(false);
				try {
					popupList_mon.enter();

					MessageSlideShell[] slidies = (MessageSlideShell[]) popupList
							.toArray(new MessageSlideShell[popupList.size()]);
					for (int i = 0; i < slidies.length; i++) {
						slidies[i].shell.dispose();
					}

					popupList.clear();

				} finally {
					popupList_mon.exit();
				}
			}
		});

		final String sDetailsText = details;
		final Button btnDetails = new Button(cButtons, SWT.TOGGLE);
		Messages.setLanguageText(btnDetails, "popup.error.details");
		btnDetails.setEnabled(details != null);
		btnDetails.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				boolean bShow = btnDetails.getSelection();
				if (bShow) {
					Shell detailsShell = new Shell(display, SWT.BORDER | SWT.ON_TOP);
					Utils.setShellIcon(detailsShell);
					detailsShell.setLayout(new FillLayout());
					StyledText textDetails = new StyledText(detailsShell, SWT.READ_ONLY
							| SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
					textDetails.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
					textDetails.setWordWrap(true);
					textDetails.setText(sDetailsText);
					detailsShell.layout();
					Rectangle shellBounds = shell.getBounds();
					detailsShell.setBounds(shellBounds.x + shellBounds.width
							- DETAILS_WIDTH, shellBounds.y - DETAILS_HEIGHT, DETAILS_WIDTH,
							DETAILS_HEIGHT);
					detailsShell.open();
					shell.setData("detailsShell", detailsShell);
					shell.addDisposeListener(new DisposeListener() {
						public void widgetDisposed(DisposeEvent e) {
							Shell detailsShell = (Shell) shell.getData("detailsShell");
							if (detailsShell != null && !detailsShell.isDisposed()) {
								detailsShell.dispose();
							}
						}
					});
					addMouseTrackListener(detailsShell, mouseAdapter);
				} else {
					Shell detailsShell = (Shell) shell.getData("detailsShell");
					if (detailsShell != null && !detailsShell.isDisposed()) {
						detailsShell.dispose();
					}
				}
			}
		});

		final Button btnHide = new Button(cButtons, SWT.PUSH);
		Messages.setLanguageText(btnHide, "popup.error.hide");
		btnHide.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				cButtons.setEnabled(false);
				shell.dispose();
			}
		});

		// Image has gap for text at the top (with image at bottom left)
		// trim top to height of shell 
		Point bestSize = cShell.computeSize(shellWidth, SWT.DEFAULT);
		int bottomHeight = cButtons.computeSize(SWT.DEFAULT, SWT.DEFAULT).y
				+ lblCloseIn.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		// no text on the frog in the bottom left
		if (bottomHeight < 50)
			bestSize.y += 50 - bottomHeight;
		if (bestSize.y < SHELL_MIN_HEIGHT)
			bestSize.y = SHELL_MIN_HEIGHT;

		final Image imgBackground = new Image(display, bestSize.x, bestSize.y);
		GC gc = new GC(imgBackground);
		int dstY = imgPopupBounds.height - bestSize.y;
		if (dstY < 0)
			dstY = 0;
		gc.drawImage(imgPopup, 0, dstY, imgPopupBounds.width, imgPopupBounds.height
				- dstY, 0, 0, bestSize.x, bestSize.y);
		gc.dispose();

		boolean bAlternateDrawing = true;
		if (USE_SWT32_BG_SET) {
			try {
				shell.setBackgroundImage(imgBackground);
				bAlternateDrawing = false;
			} catch (NoSuchMethodError e) {
			}
		}

		if (bAlternateDrawing) {
			cShell.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent e) {
					// clipping handled by gc
					e.gc.drawImage(imgBackground, 0, 0);
				}
			});

			final RGB bgRGB = display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND)
					.getRGB();

			Control[] children = cShell.getChildren();
			for (int i = 0; i < children.length; i++) {
				Control control = children[i];
				control.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent e) {
						Control c = (Control) e.widget;
						Rectangle bounds = c.getBounds();

						Image img = new Image(display, e.width, e.height);
						e.gc.copyArea(img, e.x, e.y);

						e.gc.drawImage(imgBackground, -bounds.x, -bounds.y);

						// Set the background color to invisible.  img.setBackground
						// doesn't work, so change transparentPixel directly and roll
						// a new image
						ImageData data = img.getImageData();
						data.transparentPixel = data.palette.getPixel(bgRGB);
						Image imgTransparent = new Image(display, data);

						// This is an alternative way of setting the transparency.
						//int bgIndex = data.palette.getPixel(bgRGB);
						//ImageData transparencyMask = data.getTransparencyMask();
						//for (int y = 0; y < data.height; y++) {
						//	for (int x = 0; x < data.width; x++) {
						//		if (bgIndex == data.getPixel(x, y))
						//			transparencyMask.setPixel(x, y, 0);
						//	}
						//}
						//
						//Image imgTransparent = new Image(display, data, transparencyMask);

						e.gc.drawImage(imgTransparent, e.x, e.y);
					}
				});
			}
		}

		Rectangle bounds;
		try {
			bounds = MainWindow.getWindow().getShell().getMonitor().getClientArea();
		} catch (Exception e) {
			bounds = display.getClientArea();
		}

		final Rectangle endBounds = shell.computeTrim(bounds.width - bestSize.x,
				bounds.height - bestSize.y, bestSize.x, bestSize.y);
		//System.out.println("best: " + bestSize + ";bounds: " + bounds + ";end=" + endBounds);
		// bottom and right trim will be off the edge, calulate this trim
		// and adjust it up and left (trim may not be the same size on all sides)
		int diff = (endBounds.x + endBounds.width) - (bounds.x + bounds.width);
		if (diff >= 0)
			endBounds.x -= diff + EDGE_GAP;
		diff = (endBounds.y + endBounds.height) - (bounds.y + bounds.height);
		if (diff >= 0)
			endBounds.y -= diff + EDGE_GAP;

		FormData data = new FormData(bestSize.x, bestSize.y);
		cShell.setLayoutData(data);
		shell.layout();

		btnHide.setFocus();
		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				try {
					popupList_mon.enter();

					popupList.remove(MessageSlideShell.this);

				} finally {
					popupList_mon.exit();
				}

				imgBackground.dispose();
				boldFont.dispose();
			}
		});

		addMouseTrackListener(shell, mouseAdapter);

		int count = 0;
		try {
			popupList_mon.enter();

			count = popupList.size();
			if (count > 0) {
				MessageSlideShell lastSlidey = (MessageSlideShell) popupList
						.get(count - 1);
				lastSlidey.setSlideInAfter(this, endBounds);
			}
			popupList.add(this);

		} finally {
			popupList_mon.exit();
		}

		if (count == 0)
			startSliding(endBounds);
	}

	private void addMouseTrackListener(Composite parent,
			MouseTrackListener listener) {
		Control[] children = parent.getChildren();
		for (int i = 0; i < children.length; i++) {
			Control control = children[i];
			control.addMouseTrackListener(listener);
			if (control instanceof Composite)
				addMouseTrackListener((Composite) control, listener);
		}
	}

	private void startSliding(final Rectangle endBounds) {
		if (shell == null || shell.isDisposed())
			return;

		AEThread thread = new AEThread("Slidey", true) {
			private final static int PAUSE = 200;

			public void runSupport() {
				if (shell == null || shell.isDisposed())
					return;

				new SlideShell(shell, SWT.UP, endBounds).run();

				int delayLeft = COConfigurationManager
						.getIntParameter("Message Popup Autoclose in Seconds") * 1000;

				long lastDelaySecs = 0;
				while ((delayPaused || delayLeft > 0) && !shell.isDisposed()) {
					final long delaySecs = Math.round(delayLeft / 1000.0);
					if (lastDelaySecs != delaySecs) {
						lastDelaySecs = delaySecs;
						display.asyncExec(new AERunnable() {
							public void runSupport() {
								String sText = MessageText.getString("popup.closing.in",
										new String[] { String.valueOf(delaySecs) });

								int numPopups = popupList.size();
								boolean bHasMany = numPopups > 1;
								if (bHasMany) {
									sText += "\n"
											+ MessageText.getString("popup.more.waiting",
													new String[] { String.valueOf(numPopups - 1) });
								}

								lblCloseIn.setText(sText);

								if (btnHideAll.getVisible() != bHasMany) {
									btnHideAll.setVisible(bHasMany);
									lblCloseIn.getParent().layout(true);
								}

								// Need to redraw to cause a paint
								lblCloseIn.redraw();
							}
						});
					}

					if (!delayPaused)
						delayLeft -= PAUSE;
					try {
						Thread.sleep(PAUSE);
					} catch (InterruptedException e) {
						delayLeft = 0;
					}
				}

				if (this.isInterrupted())
					return;

				if (slideInAfter != null)
					slideInAfter.startSliding(slideInAfterEndBounds);

				new SlideShell(shell, SWT.RIGHT).run();
			}
		};
		thread.start();
	}

	/**
	 * @param slideInAfter The slideInAfter to set.
	 * @param slideInAfterEndBounds 
	 */
	public void setSlideInAfter(MessageSlideShell slideInAfter,
			Rectangle slideInAfterEndBounds) {
		this.slideInAfter = slideInAfter;
		this.slideInAfterEndBounds = slideInAfterEndBounds;
	}

	public void waitUntilClosed() {
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}

	private class SlideShell {
		private final static int STEP = 5;

		private final static int PAUSE = 20;

		private Shell shell;

		private Rectangle shellBounds = null;

		private Rectangle endBounds;

		private final int direction;

		private final boolean slideIn;

		/**
		 * Slide In
		 * 
		 * @param shell
		 * @param direction 
		 * @param endBounds 
		 */
		public SlideShell(final Shell shell, int direction,
				final Rectangle endBounds) {
			this.shell = shell;
			this.endBounds = endBounds;
			this.slideIn = true;
			this.direction = direction;

			if (shell == null || shell.isDisposed())
				return;

			shell.getDisplay().syncExec(new AERunnable() {
				public void runSupport() {
					if (shell == null || shell.isDisposed())
						return;

					shellBounds = new Rectangle(endBounds.x, endBounds.y
							+ endBounds.height, endBounds.width, 0);
					shell.setBounds(shellBounds);
					shell.setVisible(true);
				}
			});
		}

		/**
		 * Slide Out
		 * 
		 * @param shell
		 * @param direction
		 */
		public SlideShell(Shell shell, int direction) {
			this.shell = shell;
			this.slideIn = false;
			this.direction = direction;
		}

		private boolean canContinue() {
			if (shell == null || shell.isDisposed())
				return false;

			if (shellBounds == null)
				return true;

			//System.out.println((slideIn ? "In" : "Out") + ";" + direction + ";S:" + shellBounds + ";" + endBounds);
			if (slideIn) {
				if (direction == SWT.UP) {
					return shellBounds.y > endBounds.y;
				}
				// TODO: Other directions
			} else {
				if (direction == SWT.RIGHT) {
					return shellBounds.width > 1;
				}
			}
			return false;
		}

		public void run() {

			while (canContinue()) {
				shell.getDisplay().asyncExec(new AERunnable() {
					public void runSupport() {
						if (shell == null || shell.isDisposed()) {
							return;
						}

						if (shellBounds == null) {
							shellBounds = shell.getBounds();
						}

						int delta;
						if (slideIn) {
							switch (direction) {
								case SWT.UP:
									delta = Math.min(endBounds.height - shellBounds.height, STEP);
									shellBounds.height += delta;
									delta = Math.min(shellBounds.y - endBounds.y, STEP);
									shellBounds.y -= delta;
									break;

								default:
									break;
							}
						} else {
							switch (direction) {
								case SWT.RIGHT:
									delta = Math.min(shellBounds.width, STEP);
									shellBounds.width -= delta;
									shellBounds.x += delta;

									if (shellBounds.width == 0) {
										shell.dispose();
										return;
									}
									break;

								default:
									break;
							}
						}

						shell.setBounds(shellBounds);
					}
				});
				try {
					Thread.sleep(PAUSE);
				} catch (Exception e) {
				}
			}
		}
	}

	/**
	 * Test
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Display display = new Display();

		ImageRepository.loadImages(display);

		String title = "This is the title that never ends, never ends!";
		String text = "This is a very long message with lots of information and "
				+ "stuff you really should read.  Are you still reading? Good, because "
				+ "reading <a href=\"http://moo.com\">stimulates</a> the mind and grows "
				+ "hair on your chest.\n\n  Unless you are a girl, then it makes you want "
				+ "to read more.  It's an endless cycle of reading that will never "
				+ "end.  Cursed is the long text that is in this test and may it fill"
				+ "every last line of the shell until there is no more.";

		//		MessagePopupShell shell = new MessagePopupShell(display,
		//				MessagePopupShell.ICON_INFO, "Title", text, "Details");
		MessageSlideShell slide = new MessageSlideShell(display,
				SWT.ICON_INFORMATION, title, text, "Details: " + text);

		new MessageSlideShell(display, SWT.ICON_INFORMATION, "ShortTitle",
				"ShortText", "Details").waitUntilClosed();

		slide = new MessageSlideShell(display, SWT.ICON_INFORMATION, "ShortTitle3",
				"ShortText", "Details");
		for (int x = 0; x < 10; x++)
			text += "\n\n\n\n\n\n\n\nWow";
		slide = new MessageSlideShell(display, SWT.ICON_INFORMATION, title, text,
				"Details: " + text);

		slide.waitUntilClosed();
	}
}
