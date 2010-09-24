package com.vuze.tests.swt.tableview;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.ui.UIInputReceiver;
import org.gudy.azureus2.plugins.ui.UIInputReceiverListener;
import org.gudy.azureus2.ui.swt.SimpleTextEntryWindow;
import org.gudy.azureus2.ui.swt.UIConfigDefaultsSWT;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewSWTImpl;

import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.common.table.TableSelectionListener;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.swt.uiupdater.UIUpdaterSWT;

public class testTableView
{
	private static TableViewSWTImpl<TableViewTestDS> tv;

	private static boolean pause = true;

	public static void main(String[] args) {
		Display display = new Display();
		FormData fd;

		COConfigurationManager.initialise();
		COConfigurationManager.setParameter("Table.useTree", true);
		COConfigurationManager.setParameter("Table.extendedErase", true);
		//COConfigurationManager.setParameter("GUI Refresh", 15000);
		UIConfigDefaultsSWT.initialize();
		
		Colors.getInstance();

		Shell shell = new Shell(display, SWT.SHELL_TRIM);
		FormLayout fl = new FormLayout();
		shell.setLayout(fl);

		Composite cTV = new Composite(shell, SWT.BORDER);

		Composite cBottom = new Composite(shell, SWT.BORDER);
		Composite cToggles = new Composite(shell, SWT.BORDER);

		fd = Utils.getFilledFormData();
		fd.bottom = new FormAttachment(cToggles, -5);
		cTV.setLayout(new FillLayout());
		cTV.setLayoutData(fd);

		fd = Utils.getFilledFormData();
		fd.top = null;
		fd.bottom = new FormAttachment(cBottom);
		cToggles.setLayout(new RowLayout());
		cToggles.setLayoutData(fd);

		fd = Utils.getFilledFormData();
		fd.top = null;
		cBottom.setLayout(new RowLayout());
		cBottom.setLayoutData(fd);

		TableColumnCore[] columns = {
			new CT_ID(),
			new CT_InvalidOnly(),
			new CT_Live(),
			new CT_LiveExt(),
			new CT_InvOnlyExt(),
			new CT_InvOnlyReord(),
		};

		com.aelitis.azureus.ui.common.table.impl.TableColumnManager.getInstance().addColumns(columns);

		tv = new TableViewSWTImpl<TableViewTestDS>(TableViewTestDS.class, "test",
				"", columns, CT_ID.name, SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL | SWT.CASCADE);

		tv.initialize(cTV);
		
		tv.setRowDefaultHeight(20);
		
		tv.addSelectionListener(new TableSelectionListener() {
			public void selected(TableRowCore[] row) {
			}
			
			public void mouseExit(TableRowCore row) {
			}
			
			public void mouseEnter(TableRowCore row) {
			}
			
			public void focusChanged(TableRowCore focus) {
			}
			
			public void deselected(TableRowCore[] rows) {
			}
			
			public void defaultSelected(TableRowCore[] rows, int stateMask) {
				System.out.println("Selected " + rows.length);
			}
		}, false);

		//addRows(500);

		tv.addKeyListener(new KeyListener() {

			public void keyReleased(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.DEL) {
					List<Object> sources = tv.getSelectedDataSources();
					int count = sources.size();
					if (count == 0) {
						return;
					}
					int i = tv.getRow((TableViewTestDS) sources.get(count - 1)).getIndex();
					if (i >= tv.getRowCount() - 1) {
						i -= count;
					} else {
						i++;
					}
					TableRowCore[] newSelRows = i < 0 || i >= tv.getRowCount() ? new TableRowCore[0] : new TableRowCore[] { tv.getRow(i) };
					tv.removeDataSources(sources.toArray(new TableViewTestDS[0]));
					tv.setSelectedRows(newSelRows);
				} else if (e.keyCode == SWT.INSERT) {
					TableViewTestDS ds = new TableViewTestDS();
					ds.map.put("ID", new Double(3.1));
					tv.addDataSource(ds);
				}
			}
		});

		UIUpdaterSWT.getInstance().addUpdater(new UIUpdatable() {

			public void updateUI() {
				if (pause) {
					return;
				}
				tv.refreshTable(false);
			}

			public String getUpdateUIName() {
				return "tableTest";
			}
		});

		Button btnPauseRefresh = new Button(cToggles, SWT.TOGGLE);
		btnPauseRefresh.setText("Pause Refresh");
		btnPauseRefresh.setSelection(pause);
		btnPauseRefresh.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				pause = !pause;
			}
		});

		Button btnRndChaos = new Button(cToggles, SWT.TOGGLE);
		btnRndChaos.setText("RndChaos");
		btnRndChaos.addListener(SWT.Selection, new Listener() {
			boolean enabled[] = { false };
			public void handleEvent(Event event) {
				enabled[0] = !enabled[0];
				if (enabled[0]) {
					final cChaos cChaos = new cChaos(enabled);
					startChaos(cChaos);
				}
			}
		});

		Button btnRndChaos1 = new Button(cToggles, SWT.TOGGLE);
		btnRndChaos1.setText("RndChaos");
		btnRndChaos1.addListener(SWT.Selection, new Listener() {
			boolean enabled[] = { false };
			public void handleEvent(Event event) {
				enabled[0] = !enabled[0];
				if (enabled[0]) {
					final cChaos cChaos = new cChaos(enabled);
					startChaos(cChaos);
				}
			}
		});

		/////////////

		Button btnAdd1 = new Button(cBottom, SWT.PUSH);
		btnAdd1.setText("Add 1");
		btnAdd1.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				addRows(1);
			}
		});

		Button btnAdd10 = new Button(cBottom, SWT.PUSH);
		btnAdd10.setText("Add 10");
		btnAdd10.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				addRows(10);
			}
		});

		Button btnAdd100 = new Button(cBottom, SWT.PUSH);
		btnAdd100.setText("Add 100");
		btnAdd100.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				addRows(100);
			}
		});

		Button btnAddX = new Button(cBottom, SWT.PUSH);
		btnAddX.setText("Add...");
		btnAddX.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				SimpleTextEntryWindow entryWindow = new SimpleTextEntryWindow("!Add!", "!How Many?");
				entryWindow.prompt(new UIInputReceiverListener() {
					public void UIInputReceiverClosed(UIInputReceiver receiver) {
						if (receiver.hasSubmittedInput()) {
							int parseInt = Integer.parseInt(receiver.getSubmittedInput());
							addRows(parseInt);
						}
					}
				});
			}
		});

		Button btnAddSame = new Button(cBottom, SWT.PUSH);
		btnAddSame.setText("Add 5 Same + 1");
		btnAddSame.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				ArrayList<TableViewTestDS> dataSources = tv.getDataSources();
				int i = 0;
				int count = 5;
				TableViewTestDS[] add = new TableViewTestDS[count + 1];
				for (TableViewTestDS ds : dataSources) {
					add[i++] = ds;
					if (i >= count) {
						break;
					}
				}
				add[i] = new TableViewTestDS();
				tv.addDataSources(add);
			}
		});

		Button btnAddSubs = new Button(cBottom, SWT.PUSH);
		btnAddSubs.setText("Add Subs");
		btnAddSubs.setSelection(pause);
		btnAddSubs.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				addSubs();
			}
		});

		Button btnClear = new Button(cBottom, SWT.PUSH);
		btnClear.setText("Clear");
		btnClear.setSelection(pause);
		btnClear.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				tv.removeAllTableRows();
			}
		});

		Button btnManualRefresh = new Button(cBottom, SWT.PUSH);
		btnManualRefresh.setText("Manual Refresh");
		btnManualRefresh.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				tv.refreshTable(false);
			}
		});

		Button btnRndInsert = new Button(cBottom, SWT.PUSH);
		btnRndInsert.setText("RndInsert");
		btnRndInsert.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				rndInsert(1);
			}
		});

		Button btnRndDel = new Button(cBottom, SWT.PUSH);
		btnRndDel.setText("RndDel");
		btnRndDel.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				rndDel(1);
			}
		});

		Button btnRndDel5 = new Button(cBottom, SWT.PUSH);
		btnRndDel5.setText("RndDelx5");
		btnRndDel5.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				rndDel(5);
			}
		});

		Button btnRndAddDell5 = new Button(cBottom, SWT.PUSH);
		btnRndAddDell5.setText("RndAddDelx5");
		btnRndAddDell5.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				rndInsert(5);
				rndDel(5);
			}
		});

		shell.open();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	/**
	 * 
	 *
	 * @since 4.4.0.5
	 */
	protected static void addSubs() {
		TableRowCore[] selectedRows = tv.getSelectedRows();
		for (TableRowCore row : selectedRows) {
			row.setSubItemCount((int) (Math.random() * 50));
		}
	}

	/**
	 * @param i
	 *
	 * @since 4.4.0.5
	 */
	private static void addRows(int num) {
		for (int i = 0; i < num; i++) {
			tv.addDataSource(new TableViewTestDS());
		}
	}

	protected static void rndDel(int num) {
		int size = tv.size(false);
		if (size <= 0) {
			return;
		}
		TableViewTestDS[] ds = new TableViewTestDS[num];
		for (int i = 0; i < ds.length; i++) {
			int pos = (int) (Math.random() * size);
			ds[i] = (TableViewTestDS) tv.getRow(pos).getDataSource(true);
		}
		tv.removeDataSources(ds);
	}

	protected static void rndInsert(int num) {
		for (int i = 0; i < num; i++) {
  		int size = tv.size(false);
  		double pos = Math.random() * size;
  		TableViewTestDS ds = new TableViewTestDS();
  		ds.map.put("ID", new Double(pos));
  		tv.addDataSource(ds);
		}
	}

	protected static void startChaos(final cChaos cChaos) {
		for (int i = 0; i < 10; i++) {
			SimpleTimer.addEvent("chaos" + i,
					SystemTime.getOffsetTime((long) (Math.random() * 3000)), cChaos);
		}
	}

	public static class cChaos
		implements TimerEventPerformer
	{
		private final boolean[] enabled;

		public cChaos(boolean[] enabled) {
			this.enabled = enabled;
		}

		public void perform(TimerEvent event) {
			if (!enabled[0]) {
				return;
			}
			if (Math.random() > 0.5) {
				rndDel(1);
			} else {
				rndInsert(1);
			}
			SimpleTimer.addEvent("chaos",
					SystemTime.getOffsetTime((long) (Math.random() * 3000)), this);
		}
	}
}
