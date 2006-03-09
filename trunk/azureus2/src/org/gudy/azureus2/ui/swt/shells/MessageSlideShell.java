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
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
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
 * +=====================================+
 * | +----+                              |
 * | |Icon| Big Bold Title               |
 * | +----+                              |
 * | Wrapping message text               |
 * | with optional URL links             |
 * | +-----+                             |
 * | |BGImg|           XX more slideys.. |
 * | | Icon|          Closing in XX secs |
 * | +-----+  [HideAll] [Details] [Hide] |
 * +=====================================+ 
 * 
 * @author TuxPaper
 * @created Mar 7, 2006
 *
 */
public class MessageSlideShell {
	private final static boolean USE_SWT32_BG_SET = true && !(Constants.isLinux && SWT
			.getVersion() <= 3224);

	/** Slide until there's this much gap between shell and edge of screen */
	private final static int EDGE_GAP = 0;

	/** Width used when BG image can't be loaded */
	private final static int SHELL_DEF_WIDTH = 280;

	/** Standard height of the shell.  Shell may grow depending on text */
	private final static int SHELL_MIN_HEIGHT = 150;

	/** Maximimum height of popup.  If text is too long, the full text will be
	 * put into details.
	 */
	private final static int SHELL_MAX_HEIGHT = 450;

	/** Width of the details shell */
	private final static int DETAILS_WIDTH = 550;

	/** Height of the details shell */
	private final static int DETAILS_HEIGHT = 300;

	/** List of popups currently in queue.  Maintained so we can have the HideAll
	 * and ""XX more slideys" text
	 */
	private final static ArrayList popupList = new ArrayList(2);

	/** Synchronization for popupList */
	private final static AEMonitor popupList_mon = new AEMonitor("popupList_mon");

	/** Shell for popup */
	private Shell shell;

	/** popup could and closing in xx seconds label */
	private Label lblCloseIn;

	/** Button that hides all slideys in the popupList.  Visible only when there's
	 * more than 1 slidey
	 */
	private Button btnHideAll;

	/** shell to start sliding in once this one starts unsliding */
	private MessageSlideShell slideInAfter = null;

	/** location to slide in next shell to */
	private Rectangle slideInAfterEndBounds;

	/** paused state of auto-close delay */
	private boolean bDelayPaused = false;

	/** List of SWT objects needing disposal */
	private ArrayList disposeList = new ArrayList();

	/** Text to put into details popup */
	private String sDetails;

	/** Open a popup using resource keys for title/text
	 * 
	 * @param display Display to create the shell on
	 * @param iconID SWT.ICON_* constant for icon in top left
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
	 * Open Mr Slidey
	 * 
	 * @param display Display to create the shell on
	 * @param iconID SWT.ICON_* constant for icon in top left
	 * @param title Text to put in the title
	 * @param text Text to put in the body
	 * @param details Text displayed when the Details button is pressed.  Null
	 *                 for disabled Details button.
	 */
	public MessageSlideShell(Display display, int iconID, String title,
			String text, String details) {
		try {
			create(display, iconID, title, text, details);
		} catch (Exception e) {
			Logger.log(new LogEvent(LogIDs.GUI, "Mr. Slidey Init", e));
			if (shell != null && shell.isDisposed()) {
				shell.dispose();
			}
			Utils.disposeSWTObjects(disposeList);
		}
	}

	private void create(final Display display, int iconID, String title,
			String text, String details) {
		GridData gridData;
		int shellWidth;

		sDetails = details;

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

		// Create shell & widgets
		shell = new Shell(display, SWT.ON_TOP);
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
		Font boldFont = new Font(display, fontData);
		disposeList.add(boldFont);
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

			if (sDetails == null) {
				sDetails = text;
			} else {
				sDetails = text + "\n---------\n" + sDetails;
			}

			linkLabel.setText(text);
		}

		// if there's a link, disable timer and mouse watching
		bDelayPaused = TorrentOpener.parseTextForURL(text) != null;
		// Pause the auto-close delay when mouse is over slidey
		// This will be applies to every control
		final MouseTrackAdapter mouseAdapter = bDelayPaused ? null
				: new MouseTrackAdapter() {
					public void mouseEnter(MouseEvent e) {
						bDelayPaused = true;
					}

					public void mouseExit(MouseEvent e) {
						bDelayPaused = false;
					}
				};

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

				} catch (Exception e) {
					Logger.log(new LogEvent(LogIDs.GUI, "Mr. Slidey HideAll", e));
				} finally {
					popupList_mon.exit();
				}
			}
		});

		final Button btnDetails = new Button(cButtons, SWT.TOGGLE);
		Messages.setLanguageText(btnDetails, "popup.error.details");
		btnDetails.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				try {
					boolean bShow = btnDetails.getSelection();
					if (bShow) {
						Shell detailsShell = new Shell(display, SWT.BORDER | SWT.ON_TOP);
						Utils.setShellIcon(detailsShell);
						detailsShell.setLayout(new FillLayout());
						StyledText textDetails = new StyledText(detailsShell, SWT.READ_ONLY
								| SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
						textDetails.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
						textDetails.setWordWrap(true);
						textDetails.setText(sDetails);
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
						if (mouseAdapter != null)
							addMouseTrackListener(detailsShell, mouseAdapter);
					} else {
						Shell detailsShell = (Shell) shell.getData("detailsShell");
						if (detailsShell != null && !detailsShell.isDisposed()) {
							detailsShell.dispose();
						}
					}
				} catch (Exception e) {
					Logger.log(new LogEvent(LogIDs.GUI, "Mr. Slidey DetailsButton", e));
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
		if (bestSize.y < SHELL_MIN_HEIGHT)
			bestSize.y = SHELL_MIN_HEIGHT;
		else if (bestSize.y > SHELL_MAX_HEIGHT) {
			bestSize.y = SHELL_MAX_HEIGHT;
			if (sDetails == null) {
				sDetails = text;
			} else {
				sDetails = text + "\n===============\n" + sDetails;
			}
		}

		if (imgPopup != null) {
			// no text on the frog in the bottom left
			int bottomHeight = cButtons.computeSize(SWT.DEFAULT, SWT.DEFAULT).y
					+ lblCloseIn.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
			if (bottomHeight < 50)
				bestSize.y += 50 - bottomHeight;

			final Image imgBackground = new Image(display, bestSize.x, bestSize.y);

			disposeList.add(imgBackground);
			GC gc = new GC(imgBackground);
			int dstY = imgPopupBounds.height - bestSize.y;
			if (dstY < 0)
				dstY = 0;
			gc.drawImage(imgPopup, 0, dstY, imgPopupBounds.width,
					imgPopupBounds.height - dstY, 0, 0, bestSize.x, bestSize.y);
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
				// Drawing of BG Image for pre SWT 3.2

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
							Rectangle bounds = ((Control) e.widget).getBounds();

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
							// Probably much slower

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
		}

		Rectangle bounds;
		try {
			bounds = MainWindow.getWindow().getShell().getMonitor().getClientArea();
		} catch (Exception e) {
			bounds = display.getClientArea();
		}

		final Rectangle endBounds = shell.computeTrim(bounds.width - bestSize.x,
				bounds.height - bestSize.y, bestSize.x, bestSize.y);
		// bottom and right trim will be off the edge, calulate this trim
		// and adjust it up and left (trim may not be the same size on all sides)
		int diff = (endBounds.x + endBounds.width) - (bounds.x + bounds.width);
		if (diff >= 0)
			endBounds.x -= diff + EDGE_GAP;
		diff = (endBounds.y + endBounds.height) - (bounds.y + bounds.height);
		if (diff >= 0)
			endBounds.y -= diff + EDGE_GAP;
		//System.out.println("best" + bestSize + ";mon" + bounds + ";end" + endBounds);

		FormData data = new FormData(bestSize.x, bestSize.y);
		cShell.setLayoutData(data);
		shell.layout();

		btnDetails.setEnabled(sDetails != null);
		btnHide.setFocus();
		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				try {
					popupList_mon.enter();

					popupList.remove(MessageSlideShell.this);

				} finally {
					popupList_mon.exit();
				}

				Utils.disposeSWTObjects(disposeList);
			}
		});

		if (mouseAdapter != null)
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

	/**
	 * Adds mousetracklistener to composite and all it's children
	 * 
	 * @param parent Composite to start at
	 * @param listener Listener to add
	 */
	private void addMouseTrackListener(Composite parent,
			MouseTrackListener listener) {
		if (parent == null || listener == null || parent.isDisposed())
			return;

		Control[] children = parent.getChildren();
		for (int i = 0; i < children.length; i++) {
			Control control = children[i];
			control.addMouseTrackListener(listener);
			if (control instanceof Composite)
				addMouseTrackListener((Composite) control, listener);
		}
	}

	/**
	 * Start the slid in, wait specified time while notifying user of impending
	 * auto-close, then slide out.  Run on separate thread, so this method
	 * returns immediately
	 * 
	 * @param endBounds end location and size wanted
	 */
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
				long lastNumPopups = 0;
				while ((bDelayPaused || delayLeft > 0) && !shell.isDisposed()) {
					int delayPausedOfs = (bDelayPaused ? 1 : 0);
					final long delaySecs = Math.round(delayLeft / 1000.0)
							+ delayPausedOfs;
					final long numPopups = popupList.size();
					if (lastDelaySecs != delaySecs || lastNumPopups != numPopups) {
						lastDelaySecs = delaySecs;
						lastNumPopups = numPopups;
						shell.getDisplay().asyncExec(new AERunnable() {
							public void runSupport() {
								String sText = "";
								
								if (lblCloseIn == null || lblCloseIn.isDisposed())
									return;

								if (!bDelayPaused)
									sText += MessageText.getString("popup.closing.in",
											new String[] { String.valueOf(delaySecs) });

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

					if (!bDelayPaused)
						delayLeft -= PAUSE;
					try {
						Thread.sleep(PAUSE);
					} catch (InterruptedException e) {
						delayLeft = 0;
					}
				}

				if (this.isInterrupted()) {
					if (shell != null && !shell.isDisposed())
						shell.dispose();
					return;
				}

				// start sliding in next popup
				if (slideInAfter != null)
					slideInAfter.startSliding(slideInAfterEndBounds);

				// slide out current popup
				new SlideShell(shell, SWT.RIGHT).run();
			}
		};
		thread.start();
	}

	/**
	 * Sets the slidey that will slide in when this one is closing/sliding out
	 * 
	 * @param slideInAfter The slideInAfter to set.
	 * @param slideInAfterEndBounds 
	 */
	public void setSlideInAfter(MessageSlideShell slideInAfter,
			Rectangle slideInAfterEndBounds) {
		this.slideInAfter = slideInAfter;
		this.slideInAfterEndBounds = slideInAfterEndBounds;
	}

	/**
	 * Waits until all slideys are closed before returning to caller.
	 */
	public void waitUntilClosed() {
		if (shell == null || shell.isDisposed())
			return;

		Display display = shell.getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}

	/**
	 * XXX This could/should be its own class 
	 */
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

			Display display = shell.getDisplay();
			display.syncExec(new Runnable() {
				public void run() {
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
				"ShortText", (String) null);
		for (int x = 0; x < 10; x++)
			text += "\n\n\n\n\n\n\n\nWow";
		slide = new MessageSlideShell(display, SWT.ICON_INFORMATION, title, text,
				"Details");

		slide = new MessageSlideShell(display, SWT.ICON_INFORMATION, title, text,
				(String) null);

		slide.waitUntilClosed();
	}
}
