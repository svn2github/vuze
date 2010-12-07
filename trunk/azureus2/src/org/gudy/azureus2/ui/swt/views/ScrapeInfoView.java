package org.gudy.azureus2.ui.swt.views;

import java.net.URL;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.tracker.client.TRTrackerAnnouncer;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraper;
import org.gudy.azureus2.core3.tracker.client.TRTrackerScraperResponse;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.BufferedLabel;
import org.gudy.azureus2.ui.swt.components.BufferedTruncatedLabel;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.maketorrent.MultiTrackerEditor;
import org.gudy.azureus2.ui.swt.maketorrent.TrackerEditorListener;

public class ScrapeInfoView
	extends AbstractIView
	implements IViewExtension
{
	private DownloadManager manager;

	private Composite cScrapeInfoView;

	private BufferedTruncatedLabel tracker_status;

	private Button updateButton;

	private BufferedLabel trackerUpdateIn;

	private Menu menuTracker;

	private MenuItem itemSelect;

	private BufferedTruncatedLabel trackerUrlValue;

	private long lastRefreshSecs;

	public ScrapeInfoView(DownloadManager dm) {
		this.manager = dm;
	}

	public String getData() {
		return "ScrapeInfoView.title";
	}

	public void initialize(Composite parent) {
		Label label;
		GridData gridData;
		final Display display = parent.getDisplay();

		if (cScrapeInfoView == null || cScrapeInfoView.isDisposed()) {
			cScrapeInfoView = new Composite(parent, SWT.NONE);
		}

		gridData = new GridData(GridData.FILL_BOTH);
		cScrapeInfoView.setLayoutData(gridData);

		GridLayout layoutInfo = new GridLayout();
		layoutInfo.numColumns = 4;
		cScrapeInfoView.setLayout(layoutInfo);

		label = new Label(cScrapeInfoView, SWT.LEFT);
		Messages.setLanguageText(label, "GeneralView.label.trackerurl"); //$NON-NLS-1$
		label.setCursor(display.getSystemCursor(SWT.CURSOR_HAND));
		label.setForeground(Colors.blue);
		label.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent arg0) {
				String announce = trackerUrlValue.getText();
				if (announce != null && announce.length() != 0) {
					new Clipboard(display).setContents(new Object[] {
						announce
					}, new Transfer[] {
						TextTransfer.getInstance()
					});
				}
			}

			public void mouseDown(MouseEvent arg0) {
				String announce = trackerUrlValue.getText();
				if (announce != null && announce.length() != 0) {
					new Clipboard(display).setContents(new Object[] {
						announce
					}, new Transfer[] {
						TextTransfer.getInstance()
					});
				}
			}
		});

		menuTracker = new Menu(parent.getShell(), SWT.POP_UP);
		itemSelect = new MenuItem(menuTracker, SWT.CASCADE);
		Messages.setLanguageText(itemSelect, "GeneralView.menu.selectTracker");
		MenuItem itemEdit = new MenuItem(menuTracker, SWT.NULL);
		Messages.setLanguageText(itemEdit, "MyTorrentsView.menu.editTracker");

		cScrapeInfoView.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				menuTracker.dispose();
			}
		});

		itemEdit.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				final TOTorrent torrent = manager.getTorrent();

				if (torrent == null) {
					return;
				}

				List group = TorrentUtils.announceGroupsToList(torrent);

				new MultiTrackerEditor(null, group, new TrackerEditorListener() {
					public void trackersChanged(String str, String str2, List _group) {
						TorrentUtils.listToAnnounceGroups(_group, torrent);

						try {
							TorrentUtils.writeToFile(torrent);
						} catch (Throwable e2) {

							Debug.printStackTrace(e2);
						}

						TRTrackerAnnouncer tc = manager.getTrackerClient();

						if (tc != null) {

							tc.resetTrackerUrl(true);
						}
					}
				}, true);
			}
		});

		final Listener menuListener = new Listener() {
			public void handleEvent(Event e) {
				if (e.widget instanceof MenuItem) {

					String text = ((MenuItem) e.widget).getText();

					TOTorrent torrent = manager.getTorrent();

					TorrentUtils.announceGroupsSetFirst(torrent, text);

					try {
						TorrentUtils.writeToFile(torrent);

					} catch (TOTorrentException f) {

						Debug.printStackTrace(f);
					}

					TRTrackerAnnouncer tc = manager.getTrackerClient();

					if (tc != null) {

						tc.resetTrackerUrl(false);
					}
				}
			}
		};

		menuTracker.addListener(SWT.Show, new Listener() {
			public void handleEvent(Event e) {
				Menu menuSelect = itemSelect.getMenu();
				if (menuSelect != null && !menuSelect.isDisposed()) {
					menuSelect.dispose();
				}
				if (manager == null || cScrapeInfoView == null
						|| cScrapeInfoView.isDisposed()) {
					return;
				}
				List groups = TorrentUtils.announceGroupsToList(manager.getTorrent());
				menuSelect = new Menu(cScrapeInfoView.getShell(), SWT.DROP_DOWN);
				itemSelect.setMenu(menuSelect);
				Iterator iterGroups = groups.iterator();
				while (iterGroups.hasNext()) {
					List trackers = (List) iterGroups.next();
					MenuItem menuItem = new MenuItem(menuSelect, SWT.CASCADE);
					Messages.setLanguageText(menuItem, "wizard.multitracker.group");
					Menu menu = new Menu(cScrapeInfoView.getShell(), SWT.DROP_DOWN);
					menuItem.setMenu(menu);
					Iterator iterTrackers = trackers.iterator();
					while (iterTrackers.hasNext()) {
						String url = (String) iterTrackers.next();
						MenuItem menuItemTracker = new MenuItem(menu, SWT.CASCADE);
						menuItemTracker.setText(url);
						menuItemTracker.addListener(SWT.Selection, menuListener);
					}
				}
			}
		});

		trackerUrlValue = new BufferedTruncatedLabel(cScrapeInfoView, SWT.LEFT, 70);

		trackerUrlValue.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent event) {
				if (event.button == 3
						|| (event.button == 1 && event.stateMask == SWT.CONTROL)) {
					menuTracker.setVisible(true);
				} else if (event.button == 1) {
					String url = trackerUrlValue.getText();
					if (url.startsWith("http://") || url.startsWith("https://")) {
						int pos = -1;
						if ((pos = url.indexOf("/announce")) != -1) {
							url = url.substring(0, pos + 1);
						}
						Utils.launch(url);
					}
				}
			}
		});

		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 3;
		trackerUrlValue.setLayoutData(gridData);

		////////////////////////

		label = new Label(cScrapeInfoView, SWT.LEFT);
		Messages.setLanguageText(label, "GeneralView.label.tracker");
		tracker_status = new BufferedTruncatedLabel(cScrapeInfoView, SWT.LEFT, 150);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 3;
		tracker_status.setLayoutData(gridData);

		label = new Label(cScrapeInfoView, SWT.LEFT);
		Messages.setLanguageText(label, "GeneralView.label.updatein");
		trackerUpdateIn = new BufferedLabel(cScrapeInfoView, SWT.LEFT);
		gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_CENTER);
		trackerUpdateIn.setLayoutData(gridData);

		updateButton = new Button(cScrapeInfoView, SWT.PUSH);
		Messages.setLanguageText(updateButton, "GeneralView.label.trackerurlupdate");
		updateButton.setLayoutData(new GridData());
		updateButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				if ( manager.getTrackerClient() != null ){
				
					manager.requestTrackerAnnounce( false );
					
				}else{
					
					manager.requestTrackerScrape( true );
				}
			}
		});

		cScrapeInfoView.layout(true);

	}

	public void refresh() {
		super.refresh();

		long thisRefreshSecs = SystemTime.getCurrentTime() / 1000;
		if (lastRefreshSecs != thisRefreshSecs) {
			lastRefreshSecs = thisRefreshSecs;
			setTracker();
		}
	}

	public Composite getComposite() {
		return cScrapeInfoView;
	}

	public Menu getPrivateMenu() {
		return null;
	}

	public void viewActivated() {
	}

	public void viewDeactivated() {
	}

	private void setTracker() {
		if (cScrapeInfoView == null || cScrapeInfoView.isDisposed()) {
			return;
		}

		Display display = cScrapeInfoView.getDisplay();

		String status 	= manager.getTrackerStatus();
		int time 		= manager.getTrackerTime();

		TRTrackerAnnouncer trackerClient = manager.getTrackerClient();

		if ( trackerClient != null ){
			
			tracker_status.setText( trackerClient.getStatusString());
			
			time = trackerClient.getTimeUntilNextUpdate();
			
		}else{
			
			tracker_status.setText( status );
		}

		if (time < 0) {

			trackerUpdateIn.setText(MessageText.getString("GeneralView.label.updatein.querying"));

		} else {

			trackerUpdateIn.setText(TimeFormatter.formatColon(time));
		}

		boolean update_state;

		String trackerURL = null;

		if (trackerClient != null) {

			URL temp = trackerClient.getTrackerURL();

			if (temp != null) {

				trackerURL = temp.toString();
			}
		}

		if (trackerURL == null) {

			TOTorrent torrent = manager.getTorrent();

			if (torrent != null) {

				trackerURL = torrent.getAnnounceURL().toString();
			}
		}

		if (trackerURL != null) {

			trackerUrlValue.setText(trackerURL);

			if ((trackerURL.startsWith("http://") || trackerURL.startsWith("https://"))) {
				trackerUrlValue.setForeground(Colors.blue);
				trackerUrlValue.setCursor(display.getSystemCursor(SWT.CURSOR_HAND));
				Messages.setLanguageText(trackerUrlValue.getWidget(),
						"GeneralView.label.trackerurlopen.tooltip", true);
			} else {
				trackerUrlValue.setForeground(null);
				trackerUrlValue.setCursor(null);
				Messages.setLanguageText(trackerUrlValue.getWidget(), null);
				trackerUrlValue.setToolTipText(null);
			}
		}

		if (trackerClient != null) {

			update_state = ((SystemTime.getCurrentTime() / 1000
					- trackerClient.getLastUpdateTime() >= TRTrackerAnnouncer.REFRESH_MINIMUM_SECS));

		} else {
			TRTrackerScraperResponse sr = manager.getTrackerScrapeResponse();
			
			if ( sr == null ){
				
				update_state = true;
				
			}else{
				
				update_state = ((SystemTime.getCurrentTime()
						- sr.getScrapeStartTime() >= TRTrackerScraper.REFRESH_MINIMUM_SECS * 1000));
			}
		}

		if (updateButton.getEnabled() != update_state) {

			updateButton.setEnabled(update_state);
		}
		cScrapeInfoView.layout();
	}

	public void setDownlaodManager(DownloadManager dm) {
		manager = dm;
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				Utils.disposeComposite(cScrapeInfoView, false);
				initialize(cScrapeInfoView.getParent());
			}
		});
	}

}
