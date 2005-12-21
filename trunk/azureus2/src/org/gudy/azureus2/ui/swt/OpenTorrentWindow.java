/*
 * OpenTorrentWindow.java
 *
 * Created on February 23, 2004, 4:09 PM
 * 
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
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
 *
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

package org.gudy.azureus2.ui.swt;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.StringIterator;
import org.gudy.azureus2.core3.config.StringList;
import org.gudy.azureus2.core3.disk.DiskManagerFileInfo;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.global.GlobalManager;
import org.gudy.azureus2.core3.internat.LocaleUtil;
import org.gudy.azureus2.core3.internat.LocaleUtilDecoder;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloader;
import org.gudy.azureus2.core3.torrentdownloader.TorrentDownloaderCallBackInterface;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.mainwindow.TorrentOpener;
import org.gudy.azureus2.ui.swt.shells.MessagePopupShell;

/**
 * Torrent Opener Window.
 * 
 * @author TuxPaper
 */
public class OpenTorrentWindow implements TorrentDownloaderCallBackInterface {

	/** Don't allow disabling of downloading for files smaller than this */
	private final static int MIN_NODOWNLOAD_SIZE = 1024 * 1024;

	private final static String PARAM_DEFSAVEPATH = "Default save path";

	private final static int STARTMODE_QUEUED = 0;

	private final static int STARTMODE_STOPPED = 1;

	private final static int STARTMODE_FORCESTARTED = 2;

	private final static int STARTMODE_SEEDING = 3;

	private final static int QUEUELOCATION_TOP = 0;

	private final static int QUEUELOCATION_BOTTOM = 1;

	private final static String[] startModes = { "queued", "stopped",
			"forceStarted", "seeding" };

	private final static String[] queueLocations = { "first", "last" };

	/** Only one window, since it can handle multiple torrents */
	private static OpenTorrentWindow stOpenTorrentWindow = null;

	// SWT Stuff
	private Shell shell;

	private Table dataFileTable;

	private Table tableTorrents;

	private Button ok;

	private Combo cmbDataDir;

	private Combo cmbStartMode = null;

	private Combo cmbQueueLocation = null;

	// Link to the outside
	private GlobalManager gm;

	// Internal Stuff

	/** TorrentFileInfo list.  All dataFiles currently in table, same order */
	private ArrayList dataFiles = new ArrayList();

	/** TorrentInfo list.  All torrents to open, same order as table */
	private ArrayList torrentList = new ArrayList();

	/** List of torrents being downloaded.  Stored so we don't close window
	 * until they are done/aborted.
	 */
	private ArrayList downloaders = new ArrayList();

	private boolean bOverrideStartModeToStopped = false;

	private boolean bDefaultForSeeding;

	/** Things to be disposed of when window closes */
	private ArrayList disposeList = new ArrayList();

	private boolean bClosed = false;

	/** Shell to use to open children (FileDialog, etc) */
	private Shell shellForChildren;

	private String sDestDir;

	/**
	 * 
	 * @param parent
	 * @param gm
	 * @param sPathOfFilesToOpen
	 * @param sFilesToOpen
	 * @param bDefaultStopped
	 * @param bForSeeding 
	 */
	public synchronized static final void invoke(Shell parent, GlobalManager gm,
			String sPathOfFilesToOpen, String[] sFilesToOpen,
			boolean bDefaultStopped, boolean bForSeeding, boolean bPopupOpenURL) {

		String saveSilentlyDir = null;

		if (stOpenTorrentWindow == null) {
			boolean bMustOpen = (sPathOfFilesToOpen == null && sFilesToOpen == null)
					|| bForSeeding;
			if (!bMustOpen) {
				saveSilentlyDir = getSaveSilentlyDir();
				bMustOpen = saveSilentlyDir == null;
			}

			stOpenTorrentWindow = new OpenTorrentWindow(parent, gm, bMustOpen);
		} else {
			if (stOpenTorrentWindow.shell != null)
				stOpenTorrentWindow.shell.forceActive();
		}

		if (stOpenTorrentWindow != null) {
			// local var because static may get set o null
			OpenTorrentWindow openTorrentWindow = stOpenTorrentWindow;
			openTorrentWindow.bOverrideStartModeToStopped = bDefaultStopped;
			openTorrentWindow.bDefaultForSeeding = bForSeeding;
			if (sFilesToOpen != null) {
				// If none of the files sent to us were valid files, don't open the 
				// window
				if (!bPopupOpenURL
						&& openTorrentWindow.addTorrents(sPathOfFilesToOpen, sFilesToOpen) == 0
						&& openTorrentWindow.torrentList.size() == 0
						&& openTorrentWindow.downloaders.size() == 0) {
					openTorrentWindow.close(true, true);
					return;
				}
			}

			if (bPopupOpenURL)
				openTorrentWindow.browseURL();

			if (saveSilentlyDir != null) {
				openTorrentWindow.sDestDir = saveSilentlyDir;
				openTorrentWindow.openTorrents();
				openTorrentWindow.close(true, false);
			}
		}
	}

	/**
	 * 
	 * @param parent
	 * @param gm
	 */
	public synchronized static final void invoke(final Shell parent,
			GlobalManager gm) {
		invoke(parent, gm, null, null, false, false, false);
	}

	public synchronized static final void invokeURLPopup(final Shell parent,
			GlobalManager gm) {
		invoke(parent, gm, null, null, false, false, true);
	}

	/**
	 * 
	 * @param parent
	 * @param gm
	 * @param bOpenWindow 
	 */
	private OpenTorrentWindow(final Shell parent, GlobalManager gm,
			boolean bOpenWindow) {
		this.gm = gm;

		sDestDir = COConfigurationManager.getStringParameter(PARAM_DEFSAVEPATH, "");

		if (bOpenWindow)
			openWindow(parent);
		else
			shellForChildren = parent;
	}

	private void openWindow(Shell parent) {
		boolean bTorrentInClipboard = false;
		GridData gridData;
		Label label;
		Composite cArea;

		shell = org.gudy.azureus2.ui.swt.components.shell.ShellFactory.createShell(
				parent, SWT.RESIZE | SWT.DIALOG_TRIM);

		shellForChildren = shell;

		shell.setText(MessageText.getString("OpenTorrentWindow.title"));
		Utils.setShellIcon(shell);

		GridLayout layout = new GridLayout();
		shell.setLayout(layout);
		shell.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event e) {
				resizeTables(3);
			}
		});

		Clipboard clipboard = new Clipboard(shell.getDisplay());

		String sClipText = (String) clipboard.getContents(TextTransfer
				.getInstance());
		if (sClipText != null)
			bTorrentInClipboard = addTorrentsFromTextList(sClipText, true) > 0;

		//    label = new Label(shell, SWT.BORDER | SWT.WRAP);
		//    Messages.setLanguageText(label, "OpenTorrentWindow.message");
		//    gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		//    label.setLayoutData(gridData);

		// Torrents
		// ========

		Composite cButtons = new Composite(shell, SWT.NONE);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		cButtons.setLayoutData(gridData);
		RowLayout rLayout = new RowLayout(SWT.HORIZONTAL);
		rLayout.marginBottom = 0;
		rLayout.marginLeft = 0;
		rLayout.marginRight = 0;
		rLayout.marginTop = 0;
		rLayout.spacing = 5;
		cButtons.setLayout(rLayout);

		// Buttons for tableTorrents

		Button browseTorrent = new Button(cButtons, SWT.PUSH);
		Messages.setLanguageText(browseTorrent, "OpenTorrentWindow.addFiles");
		browseTorrent.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				FileDialog fDialog = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
				fDialog
						.setFilterExtensions(new String[] { "*.torrent", "*.tor", "*.*" });
				fDialog.setFilterNames(new String[] { "*.torrent", "*.tor", "*.*" });
				fDialog.setFilterPath(TorrentOpener.getFilterPathTorrent());
				fDialog.setText(MessageText.getString("MainWindow.dialog.choose.file"));
				String fileName = fDialog.open();
				if (fileName != null) {
					addTorrents(fDialog.getFilterPath(), fDialog.getFileNames());
				}
			}
		});

		Button browseURL = new Button(cButtons, SWT.PUSH);
		Messages.setLanguageText(browseURL, "OpenTorrentWindow.addFiles.URL");
		browseURL.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				browseURL();
			}
		});

		Button browseFolder = new Button(cButtons, SWT.PUSH);
		Messages.setLanguageText(browseFolder, "OpenTorrentWindow.addFiles.Folder");
		browseFolder.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				DirectoryDialog fDialog = new DirectoryDialog(shell, SWT.NULL);
				fDialog.setFilterPath(TorrentOpener.getFilterPathTorrent());
				fDialog.setMessage(MessageText
						.getString("MainWindow.dialog.choose.folder"));
				String path = TorrentOpener.setFilterPathTorrent(fDialog.open());
				if (path != null)
					addTorrents(path, null);
			}
		});

		if (bTorrentInClipboard) {
			Button pasteOpen = new Button(cButtons, SWT.PUSH);
			Messages.setLanguageText(pasteOpen,
					"OpenTorrentWindow.addFiles.Clipboard");
			pasteOpen.setToolTipText(sClipText);
			pasteOpen.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					Clipboard clipboard = new Clipboard(shell.getDisplay());

					String sClipText = (String) clipboard.getContents(TextTransfer
							.getInstance());
					if (sClipText != null) {
						addTorrentsFromTextList(sClipText, false);
					}
				}
			});
		}

		Group gTorrentsArea = new Group(shell, SWT.NONE);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gTorrentsArea.setLayoutData(gridData);
		layout = new GridLayout();
		gTorrentsArea.setLayout(layout);
		Messages
				.setLanguageText(gTorrentsArea, "OpenTorrentWindow.torrentLocation");

		Composite cTorrentList = new Composite(gTorrentsArea, SWT.NONE);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		cTorrentList.setLayoutData(gridData);

		createTorrentListArea(cTorrentList);

		Composite cTorrentOptions = new Composite(gTorrentsArea, SWT.NONE);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		cTorrentOptions.setLayoutData(gridData);
		layout = new GridLayout();
		layout.marginHeight = 0;
		cTorrentOptions.setLayout(layout);

		CLabel clabel = new CLabel(cTorrentOptions, SWT.SHADOW_OUT);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		clabel.setLayoutData(gridData);
		Messages.setLanguageText(clabel, "OpenTorrentWindow.torrent.options");

		int userMode = COConfigurationManager.getIntParameter("User Mode");
		if (userMode > 0) {
			Composite cTorrentModes = new Composite(cTorrentOptions, SWT.NONE);
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			cTorrentModes.setLayoutData(gridData);
			layout = new GridLayout();
			layout.numColumns = 4;
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			cTorrentModes.setLayout(layout);

			label = new Label(cTorrentModes, SWT.NONE);
			gridData = new GridData(GridData.VERTICAL_ALIGN_CENTER);
			label.setLayoutData(gridData);
			Messages.setLanguageText(label, "OpenTorrentWindow.startMode");

			cmbStartMode = new Combo(cTorrentModes, SWT.BORDER | SWT.READ_ONLY);
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			cmbStartMode.setLayoutData(gridData);
			updateStartModeCombo();
			cmbStartMode.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					setSelectedStartMode(cmbStartMode.getSelectionIndex());
				}
			});

			label = new Label(cTorrentModes, SWT.NONE);
			gridData = new GridData(GridData.VERTICAL_ALIGN_CENTER);
			label.setLayoutData(gridData);
			Messages.setLanguageText(label, "OpenTorrentWindow.addPosition");

			cmbQueueLocation = new Combo(cTorrentModes, SWT.BORDER | SWT.READ_ONLY);
			gridData = new GridData(GridData.FILL_HORIZONTAL);
			cmbQueueLocation.setLayoutData(gridData);
			updateQueueLocationCombo();
			cmbQueueLocation.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					setSelectedQueueLocation(cmbQueueLocation.getSelectionIndex());
				}
			});
		}

		// Save To..
		// =========

		Composite cSaveTo = new Composite(cTorrentOptions, SWT.NONE);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		cSaveTo.setLayoutData(gridData);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		cSaveTo.setLayout(layout);

		label = new Label(cSaveTo, SWT.NONE);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gridData.horizontalSpan = 2;
		label.setLayoutData(gridData);
		Messages.setLanguageText(label, "OpenTorrentWindow.dataLocation");

		cmbDataDir = new Combo(cSaveTo, SWT.BORDER);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		cmbDataDir.setLayoutData(gridData);

		cmbDataDir.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				sDestDir = cmbDataDir.getText();

				int[] indexes = tableTorrents.getSelectionIndices();
				for (int i = 0; i < indexes.length; i++) {
					TorrentInfo info = (TorrentInfo) torrentList.get(indexes[i]);
					info.sDestDir = sDestDir;
				}

				tableTorrents.clearAll();

				updateOKButton();
			}
		});

		cmbDataDir.setText(sDestDir);
		final StringList dirList = COConfigurationManager
				.getStringListParameter("saveTo_list");
		StringIterator iter = dirList.iterator();
		while (iter.hasNext()) {
			cmbDataDir.add(iter.next());
		}

		Button browseData = new Button(cSaveTo, SWT.PUSH);
		Messages.setLanguageText(browseData, "ConfigView.button.browse");

		browseData.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				String sSavePath;
				String sDefPath = cmbDataDir.getText();

				if (sDefPath.length() > 0) {
					File f = new File(sDefPath);

					if (!f.exists()) {
						f.mkdirs();
					}
				}

				DirectoryDialog dDialog = new DirectoryDialog(shell, SWT.SYSTEM_MODAL);
				dDialog.setFilterPath(sDefPath);
				dDialog.setMessage(MessageText
						.getString("MainWindow.dialog.choose.savepath_forallfiles"));
				sSavePath = dDialog.open();

				if (sSavePath != null) {
					cmbDataDir.setText(sSavePath);
				}
			}
		});

		// File List
		// =========

		cArea = new Composite(shell, SWT.NULL);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		cArea.setLayout(layout);
		gridData = new GridData(GridData.FILL_BOTH);
		cArea.setLayoutData(gridData);

		label = new Label(cArea, SWT.WRAP);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		label.setLayoutData(gridData);
		Messages.setLanguageText(label, "OpenTorrentWindow.fileList");

		createTableDataFiles(cArea);

		// Ok, cancel

		cArea = new Composite(shell, SWT.NULL);
		layout = new GridLayout();
		layout.numColumns = 2;
		cArea.setLayout(layout);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
		cArea.setLayoutData(gridData);

		ok = new Button(cArea, SWT.PUSH);
		ok.setEnabled(false);
		Messages.setLanguageText(ok, "Button.ok");
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_END);
		gridData.widthHint = 70;
		ok.setLayoutData(gridData);
		shell.setDefaultButton(ok);
		ok.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				String sDefaultPath = COConfigurationManager
						.getStringParameter(PARAM_DEFSAVEPATH);
				if (!sDestDir.equals(sDefaultPath)) {
					// Move sDestDir to top of list

					// First, check to see if sDestDir is already in the list
					File fDestDir = new File(sDestDir);
					int iDirPos = -1;
					for (int i = 0; i < dirList.size(); i++) {
						String sDirName = dirList.get(i);
						File dir = new File(sDirName);
						if (dir.equals(fDestDir)) {
							iDirPos = i;
							break;
						}
					}

					// If already in list, remove it
					if (iDirPos > 0 && iDirPos < dirList.size())
						dirList.remove(iDirPos);

					// and add it to the top
					dirList.add(0, sDestDir);

					// Limit to 10
					if (dirList.size() > 10)
						dirList.remove(dirList.size() - 1);

					// Temporary list cleanup
					try {
						for (int j = 0; j < dirList.size(); j++) {
							File dirJ = new File(dirList.get(j));
							for (int i = 0; i < dirList.size(); i++) {
								try {
									if (i == j)
										continue;

									File dirI = new File(dirList.get(i));

									if (dirI.equals(dirJ)) {
										dirList.remove(i);
										// dirList shifted up, fix indexes
										if (j > i)
											j--;
										i--;
									}
								} catch (Exception e) {
									// Ignore
								}
							}
						}
					} catch (Exception e) {
						// Ignore
					}

					COConfigurationManager.setParameter("saveTo_list", dirList);
					COConfigurationManager.save();
				}

				if (!COConfigurationManager.getBooleanParameter("Use default data dir"))
					COConfigurationManager.setParameter(PARAM_DEFSAVEPATH, sDestDir);

				openTorrents();
				close(true, false);
			}
		});

		Button cancel = new Button(cArea, SWT.PUSH);
		Messages.setLanguageText(cancel, "Button.cancel");
		gridData = new GridData();
		gridData.widthHint = 70;
		cancel.setLayoutData(gridData);
		cancel.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				close(true, true);
			}
		});

		shell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if (!bClosed)
					close(false, true);
			}
		});

		shell.addListener(SWT.Traverse, new Listener() {
			public void handleEvent(Event e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE) {
					close(true, true);
				}
			}
		});

		KeyListener pasteKeyListener = new org.eclipse.swt.events.KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				int key = e.character;
				if ((e.stateMask & SWT.MOD1) != 0 && e.character <= 26
						&& e.character > 0)
					key += 'a' - 1;

				if ((key == 'v' && (e.stateMask & SWT.MOD1) > 0)
						|| (e.keyCode == SWT.INSERT && (e.stateMask & SWT.SHIFT) > 0)) {
					e.doit = false;

					// Paste
					Clipboard clipboard = new Clipboard(shell.getDisplay());

					String sClipText = (String) clipboard.getContents(TextTransfer
							.getInstance());
					if (sClipText != null) {
						addTorrentsFromTextList(sClipText, false);
					}
				}
			}
		};

		setPasteKeyListener(shell, pasteKeyListener);

		DropTarget dropTarget = new DropTarget(shell, DND.DROP_DEFAULT
				| DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK | DND.DROP_TARGET_MOVE);
		// Order is important
		dropTarget.setTransfer(new Transfer[] { URLTransfer.getInstance(),
				FileTransfer.getInstance(), TextTransfer.getInstance() });
		dropTarget.addDropListener(new DropTargetAdapter() {
			public void dropAccept(DropTargetEvent event) {
				event.currentDataType = URLTransfer.pickBestType(event.dataTypes,
						event.currentDataType);
			}

			public void dragEnter(DropTargetEvent event) {
				if ((event.operations & DND.DROP_LINK) > 0)
					event.detail = DND.DROP_LINK;
				else if ((event.operations & DND.DROP_COPY) > 0)
					event.detail = DND.DROP_COPY;
			}

			public void drop(DropTargetEvent event) {
				if (event.data instanceof URLTransfer.URLType)
					addTorrents(null,
							new String[] { ((URLTransfer.URLType) event.data).linkURL });
				else if (event.data instanceof String[])
					addTorrents(null, (String[]) event.data);
				else if (event.data instanceof String)
					addTorrents(null, new String[] { (String) event.data });
			}
		});

		shell.pack();

		Utils.centreWindow(shell);
		shell.open();
	}

	private void updateStartModeCombo() {
		if (cmbStartMode == null)
			return;
		
		int[] indexes = tableTorrents.getSelectionIndices();
		String[] sItemsText = new String[startModes.length];
		int iMaxMatches = 0;
		int iIndexToSelect = getDefaultStartMode();
		for (int i = 0; i < startModes.length; i++) {
			int iMatches = 0;
			for (int j = 0; j < indexes.length; j++) {
				TorrentInfo info = (TorrentInfo) torrentList.get(indexes[j]);
				if (info.iStartID == i)
					iMatches++;
			}

			if (iMatches > iMaxMatches) {
				iMaxMatches = iMatches;
				iIndexToSelect = i;
			}

			String sText = MessageText.getString("OpenTorrentWindow.startMode."
					+ startModes[i]);
			if (iMatches > 0)
				sText += " "
						+ MessageText.getString("OpenTorrentWindow.xOfTotal", new String[] {
								Integer.toString(iMatches), Integer.toString(indexes.length) });
			sItemsText[i] = sText;
		}
		cmbStartMode.setItems(sItemsText);
		cmbStartMode.select(iIndexToSelect);
		cmbStartMode.layout(true);
	}

	private void updateQueueLocationCombo() {
		if (cmbQueueLocation == null)
			return;
		
		int[] indexes = tableTorrents.getSelectionIndices();
		String[] sItemsText = new String[queueLocations.length];
		int iMaxMatches = 0;
		int iIndexToSelect = QUEUELOCATION_BOTTOM;
		for (int i = 0; i < queueLocations.length; i++) {
			int iMatches = 0;
			for (int j = 0; j < indexes.length; j++) {
				TorrentInfo info = (TorrentInfo) torrentList.get(indexes[j]);
				if (info.iQueueLocation == i)
					iMatches++;
			}

			if (iMatches > iMaxMatches) {
				iMaxMatches = iMatches;
				iIndexToSelect = i;
			}

			String sText = MessageText.getString("OpenTorrentWindow.addPosition."
					+ queueLocations[i]);
			if (iMatches > 0)
				sText += " "
						+ MessageText.getString("OpenTorrentWindow.xOfTotal", new String[] {
								Integer.toString(iMatches), Integer.toString(indexes.length) });
			sItemsText[i] = sText;
		}
		cmbQueueLocation.setItems(sItemsText);
		cmbQueueLocation.select(iIndexToSelect);
	}

	/**
	 * @param c
	 * @param keyListener
	 */
	private void setPasteKeyListener(Control c, KeyListener keyListener) {
		if (!(c instanceof Text || c instanceof Combo))
			c.addKeyListener(keyListener);
		if (c instanceof Composite) {
			Control[] controls = ((Composite) c).getChildren();
			for (int i = 0; i < controls.length; i++) {
				setPasteKeyListener(controls[i], keyListener);
			}
		}
	}

	private void browseURL() {
		new OpenUrlWindow(MainWindow.getWindow().getAzureusCore(),
				shellForChildren, null, null, OpenTorrentWindow.this);
	}

	private void close(boolean dispose, boolean bCancel) {
		stOpenTorrentWindow = null;
		// Can't rely on (stOpenTorrentWindow == null) to check if we are closed
		// since another thread may create another OpenTorrentWindow while
		// we are closing this one.
		bClosed = true;

		if (dispose && shell != null && !shell.isDisposed()) {
			// We won't be recalled by disposal hook because we set bClosed
			shell.dispose();
		}

		Utils.disposeSWTObjects(disposeList);

		if (downloaders.size() > 0) {
			for (Iterator iter = downloaders.iterator(); iter.hasNext();) {
				TorrentDownloader element = (TorrentDownloader) iter.next();
				element.cancel();
			}
			downloaders.clear();
		}

		if (bCancel) {
			for (Iterator iter = torrentList.iterator(); iter.hasNext();) {
				TorrentInfo info = (TorrentInfo) iter.next();
				if (info.bDeleteFileOnCancel) {
					File file = new File(info.sFileName);
					if (file.exists())
						file.delete();
				}
			}
			torrentList.clear();
		}
	}

	private void createTorrentListArea(Composite cArea) {
		GridData gridData;
		TableColumn tc;

		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		cArea.setLayout(layout);

		tableTorrents = new Table(cArea, SWT.MULTI | SWT.BORDER
				| SWT.FULL_SELECTION | SWT.VIRTUAL);
		gridData = new GridData(GridData.FILL_HORIZONTAL
				| GridData.VERTICAL_ALIGN_FILL);
		gridData.widthHint = 450;
		tableTorrents.setLayoutData(gridData);

		tc = new TableColumn(tableTorrents, SWT.NULL);
		Messages.setLanguageText(tc, "OpenTorrentWindow.torrentTable.name");
		tc.setWidth(150);
		tc = new TableColumn(tableTorrents, SWT.NULL);
		Messages.setLanguageText(tc, "OpenTorrentWindow.torrentTable.saveLocation");
		tc.setWidth(150);
		tc = new TableColumn(tableTorrents, SWT.NULL);
		Messages.setLanguageText(tc, "OpenTorrentWindow.startMode");
		tc.setWidth(70);
		tc = new TableColumn(tableTorrents, SWT.NULL);
		Messages.setLanguageText(tc, "OpenTorrentWindow.addPosition");
		tc.setWidth(80);

		tableTorrents.addListener(SWT.SetData, new Listener() {
			public void handleEvent(Event event) {
				TableItem item = (TableItem) event.item;
				int index = tableTorrents.indexOf(item);
				TorrentInfo info = (TorrentInfo) torrentList.get(index);
				item.setText(new String[] {
						info.getTorrentName(),
						info.sDestDir,
						MessageText.getString("OpenTorrentWindow.startMode."
								+ startModes[info.iStartID]),
						MessageText.getString("OpenTorrentWindow.addPosition."
								+ queueLocations[info.iQueueLocation]) });
				if (!info.isValid) {
					item.setForeground(Colors.red);
					Font font = item.getFont();
					FontData[] fd = font.getFontData();
					for (int i = 0; i < fd.length; i++) {
						fd[i].setStyle(SWT.ITALIC);
					}
					font = new Font(item.getDisplay(), fd);
					disposeList.add(font);
					item.setFont(font);
				}
				Utils.alternateRowBackground(item);
			}
		});

		tableTorrents.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				dataFiles.clear();
				int[] indexes = tableTorrents.getSelectionIndices();
				for (int i = 0; i < indexes.length; i++) {
					TorrentInfo info = (TorrentInfo) torrentList.get(indexes[i]);
					TorrentFileInfo[] files = info.getFiles();
					dataFiles.addAll(Arrays.asList(files));
				}

				updateStartModeCombo();
				updateQueueLocationCombo();

				dataFileTable.setItemCount(dataFiles.size());
				dataFileTable.clearAll();
				resizeTables(2);
			}
		});

		tableTorrents.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.DEL) {
					deleteSelected(tableTorrents, torrentList);
					e.doit = false;
				}
			}
		});

		tableTorrents.setHeaderVisible(true);

		// Menu for tableTorrents

		String sTitle;
		Menu menu = new Menu(tableTorrents);
		MenuItem item;
		sTitle = MessageText.getString("OpenTorrentWindow.startMode");

		int userMode = COConfigurationManager.getIntParameter("User Mode");
		for (int i = 0; i < startModes.length; i++) {
			if (i == STARTMODE_FORCESTARTED && userMode == 0)
				continue;

			item = new MenuItem(menu, SWT.PUSH);
			item.setData("Value", new Long(i));
			item.setText(sTitle
					+ ": "
					+ MessageText.getString("OpenTorrentWindow.startMode."
							+ startModes[i]));

			item.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					Long l = (Long) e.widget.getData("Value");
					if (l != null) {
						setSelectedStartMode(l.intValue());
						updateOKButton();
					}
				}
			});
		}

		item = new MenuItem(menu, SWT.SEPARATOR);
		sTitle = MessageText.getString("OpenTorrentWindow.addPosition");

		for (int i = 0; i < queueLocations.length; i++) {
			item = new MenuItem(menu, SWT.PUSH);
			item.setData("Value", new Long(i));
			item.setText(sTitle
					+ ": "
					+ MessageText.getString("OpenTorrentWindow.addPosition."
							+ queueLocations[i]));

			item.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					Long l = (Long) e.widget.getData("Value");
					if (l != null) {
						setSelectedQueueLocation(l.intValue());
					}
				}
			});
		}

		item = new MenuItem(menu, SWT.SEPARATOR);

		item = new MenuItem(menu, SWT.PUSH);
		// steal text
		Messages.setLanguageText(item, "MyTorrentsView.menu.remove");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				deleteSelected(tableTorrents, torrentList);
			}
		});

		item = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(item,
				"OpenTorrentWindow.fileList.changeDestination");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int[] indexes = tableTorrents.getSelectionIndices();
				String sDefPath = sDestDir;

				for (int i = 0; i < indexes.length; i++) {
					TorrentInfo info = (TorrentInfo) torrentList.get(indexes[i]);

					if (info.files.length == 1) {
						FileDialog fDialog = new FileDialog(shellForChildren,
								SWT.SYSTEM_MODAL);
						fDialog.setFilterPath(sDefPath);
						fDialog.setText(info.getTorrentName() + ": "
								+ MessageText.getString("MainWindow.dialog.choose.savepath"));
						String sNewFile = fDialog.open();

						if (sNewFile != null) {
							info.files[0].sDestFileName = sNewFile;
							String sNewPath = new File(sNewFile).getParent();
							if (sNewPath != null)
								sDefPath = sNewPath;
						}
					} else {
						DirectoryDialog dDialog = new DirectoryDialog(shellForChildren,
								SWT.SYSTEM_MODAL);

						dDialog.setFilterPath(sDefPath);
						dDialog.setMessage(MessageText
								.getString("MainWindow.dialog.choose.savepath")
								+ " (" + info.getTorrentName() + ")");
						String sNewDir = dDialog.open();

						if (sNewDir == null)
							return;

						TorrentFileInfo[] files = info.getFiles();
						for (int j = 0; j < files.length; j++) {
							TorrentFileInfo fileInfo = files[j];
							File file = new File(sNewDir, fileInfo.sFileName);
							fileInfo.sDestFileName = file.getAbsolutePath();
							String sNewPath = file.getParent();
							if (sNewPath != null)
								sDefPath = sNewPath;
						}
					}

				} // for i

				updateOKButton();
			}
		});

		tableTorrents.setMenu(menu);

		Composite cTorrentListRight = new Composite(cArea, SWT.NONE);
		gridData = new GridData(GridData.FILL_VERTICAL);
		cTorrentListRight.setLayoutData(gridData);
		RowLayout rLayout = new RowLayout(SWT.VERTICAL);
		rLayout.marginBottom = 0;
		rLayout.marginLeft = 0;
		rLayout.marginRight = 0;
		rLayout.marginTop = 0;
		rLayout.spacing = 5;
		rLayout.fill = true;
		cTorrentListRight.setLayout(rLayout);

		Button torMoveUp = new Button(cTorrentListRight, SWT.PUSH);
		torMoveUp.setImage(ImageRepository.getImage("up"));
		torMoveUp.setToolTipText(MessageText.getString("Button.moveUp"));
		torMoveUp.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				int[] indices = tableTorrents.getSelectionIndices();
				if (indices.length == 0)
					return;

				Arrays.sort(indices);
				if (indices[0] == 0)
					return;

				for (int i = 0; i < indices.length; i++) {
					int pos = indices[i];
					Object save = torrentList.get(pos - 1);
					torrentList.set(pos - 1, torrentList.get(pos));
					torrentList.set(pos, save);

					indices[i]--;
				}
				tableTorrents.setSelection(indices);
				tableTorrents.clearAll();
			}
		});

		Button torMoveDown = new Button(cTorrentListRight, SWT.PUSH);
		torMoveDown.setImage(ImageRepository.getImage("down"));
		torMoveDown.setToolTipText(MessageText.getString("Button.moveDown"));
		torMoveDown.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				int[] indices = tableTorrents.getSelectionIndices();
				if (indices.length == 0)
					return;

				Arrays.sort(indices);
				int max = indices.length - 1;
				if (indices[max] == torrentList.size() - 1)
					return;

				for (int i = max; i >= 0; i--) {
					int pos = indices[i];
					Object save = torrentList.get(pos + 1);
					torrentList.set(pos + 1, torrentList.get(pos));
					torrentList.set(pos, save);

					indices[i]++;
				}
				tableTorrents.setSelection(indices);
				tableTorrents.clearAll();
			}
		});

		Button torMoveRemove = new Button(cTorrentListRight, SWT.PUSH);
		torMoveRemove.setToolTipText(MessageText
				.getString("OpenTorrentWindow.torrent.remove"));
		torMoveRemove.setImage(ImageRepository.getImage("delete"));
		torMoveRemove.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				deleteSelected(tableTorrents, torrentList);
			}
		});

	}

	/**
	 * @param iLocation
	 */
	protected void setSelectedQueueLocation(int iLocation) {
		int[] indices = tableTorrents.getSelectionIndices();
		for (int i = 0; i < indices.length; i++) {
			TorrentInfo info = (TorrentInfo) torrentList.get(indices[i]);
			info.iQueueLocation = iLocation;
		}
		updateQueueLocationCombo();
		tableTorrents.clear(indices);
	}

	/**
	 * @param iStartID
	 */
	protected void setSelectedStartMode(int iStartID) {
		int[] indices = tableTorrents.getSelectionIndices();
		for (int i = 0; i < indices.length; i++) {
			TorrentInfo info = (TorrentInfo) torrentList.get(indices[i]);
			info.iStartID = iStartID;
		}
		updateStartModeCombo();
		tableTorrents.clear(indices);
	}

	private void deleteSelected(Table table, ArrayList list) {
		int[] indexes = table.getSelectionIndices();
		Arrays.sort(indexes);
		for (int i = indexes.length - 1; i >= 0; i--) {
			if (list.get(indexes[i]) instanceof TorrentInfo) {
				TorrentInfo info = (TorrentInfo) list.get(indexes[i]);
				if (info.bDeleteFileOnCancel) {
					File file = new File(info.sFileName);
					if (file.exists())
						file.delete();
				}
			}
			list.remove(indexes[i]);
		}
		table.setItemCount(list.size());
		table.clearAll();
		table.notifyListeners(SWT.Selection, new Event());
	}

	private void createTableDataFiles(Composite cArea) {
		GridData gridData;
		TableColumn tc;

		dataFileTable = new Table(cArea, SWT.BORDER | SWT.CHECK
				| SWT.FULL_SELECTION | SWT.VIRTUAL | SWT.MULTI);
		gridData = new GridData(GridData.FILL_BOTH);
		gridData.heightHint = 100;
		gridData.widthHint = 100;
		dataFileTable.setLayoutData(gridData);

		tc = new TableColumn(dataFileTable, SWT.NULL);
		Messages.setLanguageText(tc, "OpenTorrentWindow.fileTable.fileName");
		tc.setWidth(150);
		tc = new TableColumn(dataFileTable, SWT.NULL);
		Messages.setLanguageText(tc, "OpenTorrentWindow.fileTable.size");
		tc.setAlignment(SWT.TRAIL);
		tc.setWidth(90);
		tc = new TableColumn(dataFileTable, SWT.NULL);
		Messages.setLanguageText(tc, "OpenTorrentWindow.fileTable.destinationName");
		tc.setWidth(140);

		dataFileTable.addListener(SWT.SetData, new Listener() {
			public void handleEvent(Event event) {
				final TableItem item = (TableItem) event.item;
				int index = dataFileTable.indexOf(item);
				final TorrentFileInfo file = (TorrentFileInfo) dataFiles.get(index);

				item.setText(new String[] { file.sFullFileName,
						DisplayFormatters.formatByteCountToKiBEtc(file.lSize),
						file.sDestFileName });
				if (!file.isValid) {
					item.setForeground(Colors.red);
					Font font = item.getFont();
					FontData[] fd = font.getFontData();
					for (int i = 0; i < fd.length; i++) {
						fd[i].setStyle(SWT.ITALIC);
					}
					font = new Font(item.getDisplay(), fd);
					disposeList.add(font);
					item.setFont(font);
				}
				Utils.alternateRowBackground(item);
		    Utils.setCheckedInSetData(item, file.bDownload);
			}
		});

		dataFileTable.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent event) {
				if (event.detail == SWT.CHECK) {
					TableItem item = (TableItem) event.item;
					int index = dataFileTable.indexOf(item);
					TorrentFileInfo file = (TorrentFileInfo) dataFiles.get(index);
					// don't allow disabling of small files
					// XXX Maybe warning prompt instead?
					if (!item.getChecked() && file.lSize <= MIN_NODOWNLOAD_SIZE
							&& file.parent.iStartID != STARTMODE_SEEDING)
						item.setChecked(true);
					else
						file.bDownload = item.getChecked();
				}
			}

		});

		dataFileTable.setHeaderVisible(true);

		Menu menu = new Menu(dataFileTable);
		dataFileTable.setMenu(menu);

		MenuItem item;

		item = new MenuItem(menu, SWT.PUSH);
		Messages.setLanguageText(item,
				"OpenTorrentWindow.fileList.changeDestination");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int[] indexes = dataFileTable.getSelectionIndices();
				for (int i = 0; i < indexes.length; i++) {
					TorrentFileInfo fileInfo = (TorrentFileInfo) dataFiles
							.get(indexes[i]);
					int style = (fileInfo.parent.iStartID == STARTMODE_SEEDING)
							? SWT.OPEN : SWT.SAVE;
					FileDialog fDialog = new FileDialog(shellForChildren,
							SWT.SYSTEM_MODAL | style);
					fDialog.setFilterPath(cmbDataDir.getText());
					fDialog.setFileName(fileInfo.sDestFileName == null
							? fileInfo.sFullFileName : fileInfo.sDestFileName);
					fDialog.setText(MessageText
							.getString("MainWindow.dialog.choose.savepath")
							+ " (" + fileInfo.sFullFileName + ")");
					String sNewName = fDialog.open();

					if (sNewName == null)
						return;

					if (fileInfo.parent.iStartID == STARTMODE_SEEDING) {
						File file = new File(sNewName);
						if (file.length() == fileInfo.lSize)
							fileInfo.sDestFileName = sNewName;
						else {
							Utils.openMessageBox(shellForChildren, SWT.OK,
									"OpenTorrentWindow.mb.badSize", new String[] {
											file.getName(), fileInfo.sFullFileName });
						}
					} else {
						fileInfo.sDestFileName = sNewName;
					}
				} // for i

				updateOKButton();
			}
		});
	}

	/**
	 * Add Torrent(s) to Window using a text list of files/urls/torrents
	 * 
	 * @param sClipText Text to parse
	 */
	private int addTorrentsFromTextList(String sClipText, boolean bVerifyOnly) {
		String[] lines = null;
		int iNumFound = 0;
		// # of consecutive non torrent lines
		int iNoTorrentLines = 0;
		// no use checking the whole clipboard (which may be megabytes)
		final int MAX_CONSECUTIVE_NONTORRENT_LINES = 100;

		final String[] splitters = { "\r\n", "\n", "\r", "\t" };

		for (int i = 0; i < splitters.length; i++)
			if (sClipText.indexOf(splitters[i]) >= 0) {
				lines = sClipText.split(splitters[i]);
				break;
			}

		if (lines == null)
			lines = new String[] { sClipText };

		// Check if URL, 20 byte hash, Dir, or file
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i].trim();
			if (line.startsWith("\"") && line.endsWith("\"")) {
				line = line.substring(1, line.length() - 2);
			}

			boolean ok;

			if (line == "") {
				ok = false;
			} else if (isURL(line)) {
				ok = true;
			} else {
				File file = new File(line);

				if (!file.exists()) {
					ok = false;
				} else if (file.isDirectory()) {
					addTorrents(lines[i], null);
					ok = false;
				} else
					ok = true;
			}

			if (!ok) {
				iNoTorrentLines++;
				lines[i] = null;
				if (iNoTorrentLines > MAX_CONSECUTIVE_NONTORRENT_LINES)
					break;
			} else {
				iNumFound++;
				iNoTorrentLines = 0;
			}
		}

		if (bVerifyOnly)
			return iNumFound;

		return addTorrents(null, lines);
	}

	/**
	 * Add Torrent(s) to window
	 * 
	 * @param sTorrentFilePath
	 * @param sTorrentFilenames
	 * @return # torrents actually added to list (or downloading)
	 */
	private int addTorrents(String sTorrentFilePath, String[] sTorrentFilenames) {
		sTorrentFilePath = ensureTrailingSeparator(sTorrentFilePath);

		// Process Directory
		if (sTorrentFilePath != null && sTorrentFilenames == null) {
			File dir = new File(sTorrentFilePath);
			if (!dir.isDirectory())
				return 0;

			final File[] files = dir.listFiles(new FileFilter() {
				public boolean accept(File arg0) {
					if (FileUtil.getCanonicalFileName(arg0.getName())
							.endsWith(".torrent"))
						return true;
					if (FileUtil.getCanonicalFileName(arg0.getName()).endsWith(".tor"))
						return true;
					return false;
				}
			});

			if (files.length == 0)
				return 0;

			sTorrentFilenames = new String[files.length];
			for (int i = 0; i < files.length; i++)
				sTorrentFilenames[i] = files[i].getName();
		}

		int numAdded = 0;
		for (int i = 0; i < sTorrentFilenames.length; i++) {
			if (sTorrentFilenames[i] == null || sTorrentFilenames[i] == "")
				continue;

			// Process URL
			if (isURL(sTorrentFilenames[i])) {
				if (COConfigurationManager.getBooleanParameter("Add URL Silently"))
					new FileDownloadWindow(MainWindow.getWindow().getAzureusCore(),
							shellForChildren, sTorrentFilenames[i], null, this);
				else
					new OpenUrlWindow(MainWindow.getWindow().getAzureusCore(),
							shellForChildren, sTorrentFilenames[i], null, this);
				numAdded++;
				continue;
			}

			// Process File
			String sFileName = ((sTorrentFilePath == null) ? "" : sTorrentFilePath)
					+ sTorrentFilenames[i];

			if (addTorrent(sFileName, sFileName) != null)
				numAdded++;
		}

		if (numAdded > 0 && shell != null && tableTorrents != null
				&& !shell.isDisposed()) {
			int iTotal = torrentList.size();
			tableTorrents.setItemCount(iTotal);
			tableTorrents.clearAll();
			// select the ones we just added
			tableTorrents.select(iTotal - numAdded, iTotal - 1);
			// select doesn't notify listeners? Do it manually.
			tableTorrents.notifyListeners(SWT.Selection, new Event());

			resizeTables(1);
			updateOKButton();
		}
		return numAdded;
	}

	private TorrentInfo addTorrent(String sFileName,
			final String sOriginatingLocation) {
		TorrentInfo info = null;
		TOTorrent torrent = null;
		File torrentFile;
		boolean bDeleteFileOnCancel = false;

		// Make a copy if user wants that.  We'll delete it when we cancel, if we 
		// actually made a copy.
		try {
			File fOriginal = new File(sFileName);
			torrentFile = TorrentUtils.copyTorrentFileToSaveDir(fOriginal, true);
			bDeleteFileOnCancel = !fOriginal.equals(torrentFile);
			// TODO if the files are still equal, and it isn't in the save
			//       dir, we should copy it to a temp file in case something
			//       re-writes it.  No need to copy a torrent coming from the
			//       downloader though..
		} catch (IOException e1) {
			// Use torrent in wherever it is and hope for the best
			// XXX Should error instead?
			torrentFile = new File(sFileName);
		}

		// Load up the torrent, see it it's real
		try {
			torrent = TorrentUtils.readFromFile(torrentFile, false);
		} catch (final TOTorrentException e) {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					if (shell == null)
						new MessagePopupShell(MessagePopupShell.ICON_ERROR,
								"OpenTorrentWindow.mb.openError", Debug.getStackTrace(e),
								new String[] { sOriginatingLocation, e.getMessage() },
								MainWindow.getWindow().getDisplay());
					else
						Utils.openMessageBox(shell, SWT.OK,
								"OpenTorrentWindow.mb.openError", new String[] {
										sOriginatingLocation, e.getMessage() });
				}
			});

			if (bDeleteFileOnCancel)
				torrentFile.delete();

			return null;
		}

		// Check if torrent already exists in gm, and add if not
		final DownloadManager existingDownload = gm.getDownloadManager(torrent);
		if (existingDownload == null) {
			info = new TorrentInfo(torrentFile.getAbsolutePath(), torrent,
					bDeleteFileOnCancel);
			info.sOriginatingLocation = sOriginatingLocation;
			torrentList.add(info);
		} else {
			Utils.execSWTThread(new AERunnable() {
				public void runSupport() {
					if (shell == null)
						new MessagePopupShell(MessagePopupShell.ICON_ERROR,
								"OpenTorrentWindow.mb.alreadyExists", null, new String[] {
										sOriginatingLocation, existingDownload.getDisplayName() },
								MainWindow.getWindow().getDisplay());
					else
						Utils.openMessageBox(shell, SWT.OK,
								"OpenTorrentWindow.mb.alreadyExists", new String[] {
										sOriginatingLocation, existingDownload.getDisplayName() });
				}
			});

			if (bDeleteFileOnCancel)
				torrentFile.delete();
		}

		return info;
	}

	private boolean isURL(String sURL) {
		String sLower = sURL.toLowerCase();
		return sLower.startsWith("http://") || sLower.startsWith("https://")
				|| sLower.startsWith("magnet:") || sLower.startsWith("ftp://")
				|| sLower.matches("^[a-fA-F0-9]{40}$");
	}

	private void updateOKButton() {
		if (ok == null || bClosed)
			return;

		File file = new File(cmbDataDir.getText());
		boolean bEnable = file.isDirectory() && torrentList != null
				&& torrentList.size() > 0 && downloaders.size() == 0;

		// Check for seeding
		for (int i = 0; i < torrentList.size(); i++) {
			boolean bTorrentValid = true;
			TorrentInfo info = (TorrentInfo) torrentList.get(i);
			if (info.iStartID == STARTMODE_SEEDING) {
				// check if all selected files exist
				TorrentFileInfo[] files = info.getFiles();
				for (int j = 0; j < files.length; j++) {
					TorrentFileInfo fileInfo = files[j];
					if (!fileInfo.bDownload)
						continue;

					String sFullPath;
					if (fileInfo.sDestFileName == null)
						sFullPath = ensureTrailingSeparator(cmbDataDir.getText())
								+ fileInfo.sFullFileName;
					else
						sFullPath = fileInfo.sDestFileName;

					file = new File(sFullPath);
					if (!file.exists()) {

						bEnable = false;
						fileInfo.isValid = false;
						bTorrentValid = false;
					} else if (!fileInfo.isValid) {
						fileInfo.isValid = true;
					}
				}
			}

			info.isValid = bTorrentValid;
		}

		ok.setEnabled(bEnable);
		tableTorrents.clearAll();
		dataFileTable.clearAll();
	}

	/**
	 * Resize the columns of the tables to fit without horizontal scrollbar
	 * 
	 * @param which bitwise field of which table to recalc
	 *         Bit 0: torrents table
	 *         Bit 1: Data Files Table
	 */
	private void resizeTables(int which) {
		if (Constants.isLinux)
			return;

		TableColumn[] tcs;
		if ((which & 1) > 0) {
			tcs = tableTorrents.getColumns();
			int newSize = tableTorrents.getClientArea().width - 20;
			for (int i = 1; i < tcs.length; i++)
				newSize -= tcs[i].getWidth();
			if (newSize > 10)
				tcs[0].setWidth(newSize);
		}

		// Adjust only first column
		if ((which & 2) > 0) {
			tcs = dataFileTable.getColumns();
			int newSize = dataFileTable.getClientArea().width - 20;
			for (int i = 1; i < tcs.length; i++)
				newSize -= tcs[i].getWidth();
			if (newSize > 10)
				tcs[0].setWidth(newSize);
		}
	}

	/**
	 * Open the torrents already added based on user choices
	 * 
	 * @param sDataDir 
	 */
	private void openTorrents() {
		ArrayList addedTorrentsTop = new ArrayList();

		for (int i = 0; i < torrentList.size(); i++) {
			TorrentInfo info = (TorrentInfo) torrentList.get(i);
			try {
				if (info.torrent == null)
					continue;

				// set "queued" to STATE_WAITING so that auto-open details will work  
				// (even if the torrent immediately goes to queued)
				int iStartMode = (info.iStartID == STARTMODE_STOPPED)
						? DownloadManager.STATE_STOPPED : DownloadManager.STATE_WAITING;

				DownloadManager dm = gm
						.addDownloadManager(info.sFileName, info.sDestDir, iStartMode,
								true, info.iStartID == STARTMODE_SEEDING);

				// If dm is null, most likely there was an error printed.. let's hope
				// the user was notified and skip the error quietly.
				// We don't have to worry about deleting the file (info.bDelete..)
				// since gm.addDown.. will handle it.
				if (dm == null)
					continue;

				if (info.iQueueLocation == QUEUELOCATION_TOP)
					addedTorrentsTop.add(dm);

				if (iStartMode == STARTMODE_FORCESTARTED)
					dm.setForceStart(true);

				DiskManagerFileInfo[] dmFileInfo = dm.getDiskManagerFileInfo();
				TorrentFileInfo[] files = info.getFiles();
				for (int j = 0; j < dmFileInfo.length; j++) {
					int iIndex = dmFileInfo[j].getIndex();
					if (iIndex >= 0 && iIndex < files.length
							&& files[iIndex].lSize == dmFileInfo[j].getLength()) {

						File fDest;
						if (files[iIndex].sDestFileName != null) {
							fDest = new File(files[iIndex].sDestFileName);
							dmFileInfo[j].setLink(fDest);
						} else {
							fDest = new File(info.sDestDir, files[iIndex].sFullFileName);
						}

						if (!files[iIndex].bDownload) {
							dmFileInfo[j].setSkipped(true);
							if (!fDest.exists()) {
								dmFileInfo[j].setStorageType(DiskManagerFileInfo.ST_COMPACT);
							}
						}
					}
				}
			} catch (Exception e) {
				if (shell == null)
					new MessagePopupShell(MessagePopupShell.ICON_ERROR,
							"OpenTorrentWindow.mb.openError", Debug.getStackTrace(e),
							new String[] { info.sOriginatingLocation, e.getMessage() },
							MainWindow.getWindow().getDisplay());
				else
					Utils.openMessageBox(shell, SWT.OK, "OpenTorrentWindow.mb.openError",
							new String[] { info.sOriginatingLocation, e.getMessage() });
			}
		}

		if (addedTorrentsTop.size() > 0) {
			DownloadManager[] dms = (DownloadManager[]) addedTorrentsTop
					.toArray(new DownloadManager[0]);
			gm.moveTop(dms);
		}

		torrentList.clear();
	}

	private int getDefaultStartMode() {
		if (bDefaultForSeeding)
			return STARTMODE_SEEDING;
		return (bOverrideStartModeToStopped || COConfigurationManager
				.getBooleanParameter("Default Start Torrents Stopped"))
				? STARTMODE_STOPPED : STARTMODE_QUEUED;
	}

	// TorrentDownloaderCallBackInterface
	public void TorrentDownloaderEvent(int state, final TorrentDownloader inf) {
		// This method is run even if the window is closed.

		if (state == TorrentDownloader.STATE_INIT) {
			downloaders.add(inf);

		} else if (state == TorrentDownloader.STATE_FINISHED) {
			// This can be called more than once for each inf..
			if (!downloaders.contains(inf))
				return;
			downloaders.remove(inf);

			File file = inf.getFile();
			if (addTorrent(file.getAbsolutePath(), inf.getURL()) == null) {
				// addTorrent may not delete it on error if the downloader saved it
				// to the place where user wants to store torrents (which is most 
				// likely) 
				if (file.exists())
					file.delete();
				return;
			}

			if (shell != null && !shell.isDisposed()) {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						tableTorrents.setItemCount(torrentList.size());
						tableTorrents.clearAll();

						// select the one we just added
						tableTorrents.select(torrentList.size() - 1);
						// select doesn't notify listeners? Do it manually.
						tableTorrents.notifyListeners(SWT.Selection, new Event());

						resizeTables(1);
					}
				});
			} else {
				String saveSilentlyDir = getSaveSilentlyDir();
				if (saveSilentlyDir != null) {
					sDestDir = saveSilentlyDir;
					openTorrents();
				}
			}

		} else if (state == TorrentDownloader.STATE_CANCELLED
				|| state == TorrentDownloader.STATE_ERROR
				|| state == TorrentDownloader.STATE_DUPLICATE) {
			downloaders.remove(inf);
		} else
			return;

		// definitely on a different thread..
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				updateOKButton();
			}
		});
	}

	/**
	 * Class to store one Torrent file's info.  Used to populate table and store
	 * user's choices.
	 */
	private class TorrentInfo {
		/** Where the torrent came from.  Could be a file, URL, or some other text */
		String sOriginatingLocation;

		/** Filename the .torrent is saved to */
		String sFileName;

		String sDestDir;

		TOTorrent torrent;

		int iStartID;

		int iQueueLocation;

		boolean isValid;

		boolean bDeleteFileOnCancel;

		private TorrentFileInfo[] files = null;

		/**
		 * Init
		 * 
		 * @param sFileName
		 * @param torrent
		 * @param bDeleteFileOnCancel 
		 */
		public TorrentInfo(String sFileName, TOTorrent torrent,
				boolean bDeleteFileOnCancel) {
			this.bDeleteFileOnCancel = bDeleteFileOnCancel;
			this.sFileName = sFileName;
			this.sOriginatingLocation = sFileName;
			this.torrent = torrent;
			this.sDestDir = OpenTorrentWindow.this.sDestDir;

			iStartID = getDefaultStartMode();
			iQueueLocation = QUEUELOCATION_BOTTOM;
			isValid = true;

			// Force a check on the encoding, will prompt user if we dunno
			try {
				LocaleUtil.getSingleton().getTorrentEncoding(TorrentInfo.this.torrent);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public TorrentFileInfo[] getFiles() {
			if (files == null && torrent != null) {
				TOTorrentFile[] tfiles = torrent.getFiles();
				files = new TorrentFileInfo[tfiles.length];
				for (int i = 0; i < files.length; i++) {
					files[i] = new TorrentFileInfo(this, tfiles[i], i);
				}
			}

			return files;
		}

		public String getTorrentName() {
			if (torrent == null)
				return "";
			try {
				LocaleUtilDecoder decoder = LocaleUtil.getSingleton()
						.getTorrentEncodingIfAvailable(torrent);
				if (decoder != null)
					return decoder.decodeString(torrent.getName());
			} catch (Exception e) {
			}

			try {
				return new String(torrent.getName());
			} catch (Exception e) {
				return "TextDecodingError";
			}
		}
	}

	/**
	 * Class to store the file list of a Torrent.  Used to populate table and
	 * store user's choices
	 */
	private class TorrentFileInfo {
		String sFileName;

		String sFullFileName;

		long lSize;

		boolean bDownload;

		String sDestFileName;

		long iIndex;

		boolean isValid;

		final TorrentInfo parent;

		/**
		 * Init
		 * 
		 * @param parent 
		 * @param torrentFile
		 * @param iIndex
		 */
		public TorrentFileInfo(TorrentInfo parent, TOTorrentFile torrentFile,
				int iIndex) {
			this.parent = parent;
			lSize = torrentFile.getLength();
			this.iIndex = iIndex;
			bDownload = true;
			sDestFileName = null;
			isValid = true;

			sFileName = torrentFile.getRelativePath(); // translated to locale
			if (parent.torrent.isSimpleTorrent()) {
				sFullFileName = sFileName;
			} else {
				sFullFileName = parent.getTorrentName() + File.separator
						+ torrentFile.getRelativePath();
			}
		}
	}

	private String ensureTrailingSeparator(String sPath) {
		if (sPath == null || sPath.length() == 0 || sPath.endsWith(File.separator))
			return sPath;
		return sPath + File.separator;
	}

	/**
	 * 
	 * @return Null if user doesn't want to save silently, or if no path set
	 */
	private static String getSaveSilentlyDir() {
		boolean bUseDefault = COConfigurationManager
				.getBooleanParameter("Use default data dir");
		if (!bUseDefault)
			return null;

		String sDefDir = "";
		try {
			sDefDir = COConfigurationManager.getDirectoryParameter(PARAM_DEFSAVEPATH);
		} catch (IOException e) {
			return null;
		}

		return (sDefDir == "") ? null : sDefDir;
	}
}
