package org.gudy.azureus2.ui.swt.views.peersstats;

import java.util.*;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.download.DownloadManagerPeerListener;
import org.gudy.azureus2.core3.global.GlobalManagerListener;
import org.gudy.azureus2.core3.peer.PEPeer;
import org.gudy.azureus2.core3.peer.PEPeerListener;
import org.gudy.azureus2.core3.peer.PEPeerManager;
import org.gudy.azureus2.core3.util.FileUtil;

import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableColumnCreationListener;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.pluginsimpl.local.PluginInitializer;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.ClipboardCopy;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewSWTImpl;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewTab;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.peermanager.piecepicker.util.BitFlags;
import com.aelitis.azureus.core.util.bloom.BloomFilter;
import com.aelitis.azureus.core.util.bloom.BloomFilterFactory;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableLifeCycleListener;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.util.MapUtils;

public class PeersStatsView
	extends TableViewTab
	implements TableLifeCycleListener, GlobalManagerListener,
	DownloadManagerPeerListener
{
	private AzureusCore core;

	private TableViewSWTImpl<PeersStatsDataSource> tv;

	private boolean columnsAdded;

	private Map<String, PeersStatsDataSource> mapData;

	private Composite parent;

	private BloomFilter bloomFilter;

	private PeersStatsOverall overall;

	public PeersStatsView() {
		super("PeersStats");

		initAndLoad();

		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(AzureusCore core) {
				initColumns(core);
			}
		});
	}

	public Composite initComposite(Composite composite) {
		parent = new Composite(composite, SWT.BORDER);
		parent.setLayout(new FormLayout());
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		return parent;
	}

	public void tableViewInitComplete() {
		Composite cTV = (Composite) parent.getChildren()[0];
		Composite cBottom = new Composite(parent, SWT.None);
		FormData fd;
		fd = Utils.getFilledFormData();
		fd.bottom = new FormAttachment(cBottom);
		cTV.setLayoutData(fd);
		fd = Utils.getFilledFormData();
		fd.top = null;
		cBottom.setLayoutData(fd);
		cBottom.setLayout(new FormLayout());

		Button btnCopy = new Button(cBottom, SWT.PUSH);
		btnCopy.setLayoutData(new FormData());
		btnCopy.setText("Copy");
		btnCopy.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				TableRowCore[] rows = tv.getRows();
				StringBuilder sb = new StringBuilder();
				sb.append("Hits,Client,Bytes Sent,Bytes Received,Bad Bytes\n");
				for (TableRowCore row : rows) {
					PeersStatsDataSource stat = (PeersStatsDataSource) row.getDataSource();
					if (stat == null) {
						continue;
					}
					sb.append(stat.count);
					sb.append(",");
					sb.append(stat.client.replaceAll(",", ""));
					sb.append(",");
					sb.append(stat.bytesSent);
					sb.append(",");
					sb.append(stat.bytesReceived);
					sb.append(",");
					sb.append(stat.bytesDiscarded);
					sb.append("\n");
				}
				ClipboardCopy.copyToClipBoard(sb.toString());
			}
		});
	}

	public TableViewSWT initYourTableView() {
		tv = new TableViewSWTImpl<PeersStatsDataSource>(PeersStatsDataSource.class,
				"PeersStats", getPropertiesPrefix(), new TableColumnCore[0],
				ColumnPS_Count.COLUMN_ID, SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL);
		/*
				tv.addTableDataSourceChangedListener(this, true);
				tv.addRefreshListener(this, true);
				tv.addSelectionListener(this, false);
				tv.addMenuFillListener(this);
				*/
		tv.addLifeCycleListener(this);

		return tv;
	}

	private void initColumns(AzureusCore core) {
		synchronized (PeersStatsView.class) {

			if (columnsAdded) {

				return;
			}

			columnsAdded = true;
		}

		UIManager uiManager = PluginInitializer.getDefaultInterface().getUIManager();

		TableManager tableManager = uiManager.getTableManager();

		tableManager.registerColumn(PeersStatsDataSource.class,
				ColumnPS_Name.COLUMN_ID, new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnPS_Name(column);
					}
				});
		tableManager.registerColumn(PeersStatsDataSource.class,
				ColumnPS_Count.COLUMN_ID, new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnPS_Count(column);
					}
				});
		tableManager.registerColumn(PeersStatsDataSource.class,
				ColumnPS_Discarded.COLUMN_ID, new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnPS_Discarded(column);
					}
				});
		tableManager.registerColumn(PeersStatsDataSource.class,
				ColumnPS_Received.COLUMN_ID, new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnPS_Received(column);
					}
				});
		tableManager.registerColumn(PeersStatsDataSource.class,
				ColumnPS_Sent.COLUMN_ID, new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnPS_Sent(column);
					}
				});
		tableManager.registerColumn(PeersStatsDataSource.class,
				ColumnPS_Pct.COLUMN_ID, new TableColumnCreationListener() {
					public void tableColumnCreated(TableColumn column) {
						new ColumnPS_Pct(column);
					}
				});
	}

	public void tableViewDestroyed() {
		core.getGlobalManager().removeListener(this);
		List downloadManagers = core.getGlobalManager().getDownloadManagers();
		for (Object object : downloadManagers) {
			((DownloadManager) object).removePeerListener(this);
		}
		save();
	}

	private void initAndLoad() {
		mapData = new HashMap<String, PeersStatsDataSource>();

		synchronized (mapData) {
			Map map = FileUtil.readResilientConfigFile("peersstats.dat");

			Map mapBloom = MapUtils.getMapMap(map, "bloomfilter", null);
			if (mapBloom != null) {
				bloomFilter = BloomFilterFactory.deserialiseFromMap(mapBloom);
			}
			if (bloomFilter == null) {
				bloomFilter = BloomFilterFactory.createRotating(
						BloomFilterFactory.createAddOnly(100000), 2);
			}

			overall = new PeersStatsOverall();

			List listSavedData = MapUtils.getMapList(map, "data", null);
			if (listSavedData != null) {
				for (Object val : listSavedData) {
					try {
						Map mapVal = (Map) val;
						if (mapVal != null) {
							PeersStatsDataSource ds = new PeersStatsDataSource(mapVal);
							ds.overall = overall;

							if (!mapData.containsKey(ds.client)) {
								mapData.put(ds.client, ds);
								overall.count += ds.count; 
							}
						}
							
					} catch (Exception e) {
						// ignore
					}
				}
			}
		}
	}

	private void save() {
		Map<String, Object> map = new HashMap<String, Object>();
		synchronized (mapData) {
			map.put("data", new ArrayList(mapData.values()));
			map.put("bloomfilter", bloomFilter.serialiseToMap());
		}
		FileUtil.writeResilientConfigFile("peersstats.dat", map);
	}

	public void tableViewInitialized() {
		synchronized (mapData) {
			if (mapData.values().size() > 0) {
				tv.addDataSources(mapData.values().toArray(new PeersStatsDataSource[0]));
			}
		}
		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {

			public void azureusCoreRunning(AzureusCore core) {
				register(core);
			}
		});
	}

	protected void register(AzureusCore core) {
		this.core = core;
		core.getGlobalManager().addListener(this);
	}

	public void destroyInitiated() {
	}

	public void destroyed() {
	}

	public void downloadManagerAdded(DownloadManager dm) {
		dm.addPeerListener(this, true);
	}

	public void downloadManagerRemoved(DownloadManager dm) {
		dm.removePeerListener(this);
	}

	public void seedingStatusChanged(boolean seedingOnlyMode,
			boolean potentiallySeedingOnlyMode) {
	}

	public void peerAdded(PEPeer peer) {
		peer.addListener(new PEPeerListener() {

			public void stateChanged(PEPeer peer, int newState) {
				if (newState == PEPeer.TRANSFERING) {
					addPeer(peer);
				} else if (newState == PEPeer.CLOSING
						|| newState == PEPeer.DISCONNECTED) {
					peer.removeListener(this);
				}
			}

			public void sentBadChunk(PEPeer peer, int pieceNum, int totalBadChunks) {
			}

			public void removeAvailability(PEPeer peer, BitFlags peerHavePieces) {
			}

			public void addAvailability(PEPeer peer, BitFlags peerHavePieces) {
			}
		});
	}

	protected void addPeer(PEPeer peer) {
		synchronized (mapData) {
			byte[] peerId = peer.getId();
			if (bloomFilter.contains(peerId)) {
				return;
			}

			bloomFilter.add(peerId);

			String id = getID(peer);
			PeersStatsDataSource stat = mapData.get(id);
			boolean needNew = stat == null;
			if (needNew) {
				stat = new PeersStatsDataSource();
				stat.overall = overall;
				mapData.put(id, stat);
			}

			overall.count++;

			stat.client = getID(peer);
			stat.count++;
			stat.current++;
			if (needNew) {
				tv.addDataSource(stat);
			} else {
				TableRowCore row = tv.getRow(stat);
				if (row != null) {
					row.invalidate();
				}
			}
		}
	}

	public void peerManagerAdded(PEPeerManager manager) {
	}

	public void peerManagerRemoved(PEPeerManager manager) {
	}

	public void peerManagerWillBeAdded(PEPeerManager manager) {
	}

	public void peerRemoved(PEPeer peer) {
		synchronized (mapData) {
			PeersStatsDataSource stat = mapData.get(getID(peer));
			if (stat != null) {
				stat.current--;
				stat.bytesReceived += peer.getStats().getTotalDataBytesReceived();
				stat.bytesSent += peer.getStats().getTotalDataBytesSent();
				stat.bytesDiscarded += peer.getStats().getTotalBytesDiscarded();

				TableRowCore row = tv.getRow(stat);
				if (row != null) {
					row.invalidate();
				}
			}
		}
	}

	private String getID(PEPeer peer) {
		String s = peer.getClient();
		return s.replaceAll(" v?[0-9.]+", "");
	}
}
