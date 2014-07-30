package com.vuze.tests.swt.tableview;

import java.util.List;
import java.util.ResourceBundle;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.ui.UIInputReceiver;
import org.gudy.azureus2.plugins.ui.UIInputReceiverListener;
import org.gudy.azureus2.ui.swt.SimpleTextEntryWindow;
import org.gudy.azureus2.ui.swt.UIConfigDefaultsSWT;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWTMenuFillListener;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewFactory;
import org.gudy.azureus2.ui.swt.views.table.painted.TableViewPainted;

import com.aelitis.azureus.ui.common.table.*;
import com.aelitis.azureus.ui.common.table.impl.TableColumnManager;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.swt.columns.utils.TableColumnCreatorV3;
import com.aelitis.azureus.ui.swt.uiupdater.UIUpdaterSWT;

public class testTableView
{
	private static TableViewSWT tv;

	private static boolean pause = true;

	private static boolean printDiff;
	
	private static int numChaos = 6;
	private static Thread[] chaosThreads;

	@SuppressWarnings({
		"unchecked",
		"rawtypes"
	})
	public static void main(String[] args) {
		Display display = new Display();
		FormData fd;
		

		COConfigurationManager.initialise();
		COConfigurationManager.setParameter("Table.useTree", true);
		COConfigurationManager.setParameter("Table.extendedErase", true);
		//COConfigurationManager.setParameter("GUI Refresh", 15000);
		UIConfigDefaultsSWT.initialize();

		Colors.getInstance();
		
		ResourceBundle bundle = ResourceBundle.getBundle("com.vuze.tests.swt.tableview.text");
		MessageText.integratePluginMessages(bundle);

		TableColumnCreatorV3.initCoreColumns();

		Shell shell = new Shell(display, SWT.SHELL_TRIM);
		shell.setText("testTableView");
		shell.setSize(800, 400);
		FormLayout fl = new FormLayout();
		shell.setLayout(fl);

		Composite cTop = new Composite(shell, SWT.BORDER);

		Composite cTV = new Composite(shell, SWT.BORDER);

		Composite cBottom = new Composite(shell, SWT.BORDER);
		Composite cToggles = new Composite(shell, SWT.BORDER);

		fd = Utils.getFilledFormData();
		fd.bottom = null;
		cTop.setLayoutData(fd);

		fd = Utils.getFilledFormData();
		fd.top = new FormAttachment(cTop, 5);
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
			new CT_Text(),
			new CT_InvalidOnly(),
			new CT_Live(),
			new CT_LivePaintOnly(),
			new CT_LivePaintOnlyExt(),
			new CT_LiveExt(),
			new CT_InvOnlyExt(),
			new CT_InvOnlyReord(),
		};

		TableColumnManager tcm = com.aelitis.azureus.ui.common.table.impl.TableColumnManager.getInstance();
		tcm.addColumns(columns);
		String names[] = new String[columns.length];
		for (int i = 0; i < columns.length; i++) {
			TableColumnCore c = columns[i];
			names[i] = c.getName();
		}
		tcm.setDefaultColumnNames("test", names);

		tv = TableViewFactory.createTableViewSWT(true, TableViewTestDS.class,
				"test", "", columns, CT_ID.name, SWT.MULTI | SWT.FULL_SELECTION
						| SWT.VIRTUAL | SWT.CASCADE);

		tv.initialize(cTV);

		tv.setRowDefaultHeight(40);
		//	tv.setRowDefaultHeight(16);
		
		tv.addCountChangeListener(new TableCountChangeListener() {
			public void rowRemoved(TableRowCore row) {
			}
			
			public void rowAdded(TableRowCore row) {
				//row.setHeight((int) (16 + (Math.random() * 100)));
			}
		});

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
				if (e.keyCode == SWT.DEL) {
					System.out.println("SMURF");
					List<Object> sources = tv.getSelectedDataSources();
					int count = sources.size();
					if (count == 0) {
						return;
					}
					tv.removeDataSources(sources.toArray());
//					int i = tv.getRow((TableViewTestDS) sources.get(count - 1)).getIndex();
//					if (i >= tv.getRowCount() - 1) {
//						i -= count;
//					} else {
//						i++;
//					}
//					TableRowCore[] newSelRows = i < 0 || i >= tv.getRowCount()
//							? new TableRowCore[0] : new TableRowCore[] {
//								tv.getRow(i)
//							};
//					tv.removeDataSources(sources.toArray(new TableViewTestDS[0]));
//					tv.setSelectedRows(newSelRows);
				} else if (e.keyCode == SWT.INSERT) {
					TableViewTestDS ds = new TableViewTestDS();
					ds.map.put("ID", new Double(3.1));
					tv.addDataSource(ds);
				}
			}
		});
		
		tv.addMenuFillListener(new TableViewSWTMenuFillListener() {
			
			public void fillMenu(String sColumnName, Menu menu) {
				MenuItem menuItem = new MenuItem(menu, SWT.PUSH);
				menuItem.setText("MenuFrom fillMenu");
			}
			
			public void addThisColumnSubMenu(String sColumnName, Menu menuThisColumn) {
				MenuItem menuItem = new MenuItem(menuThisColumn, SWT.PUSH);
				menuItem.setText("MenuFrom addThisColumnSubMenu");
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
/*
				SimpleTimer.addEvent("YourKitS", SystemTime.getOffsetTime(1000l * 9),
						new TimerEventPerformer() {
							public void perform(TimerEvent event) {
								try {
									Controller controller;
									controller = new Controller();
									System.out.println("STARTING");
									controller.startCPUProfiling(ProfilingModes.CPU_TRACING,
											Controller.DEFAULT_FILTERS,
											Controller.DEFAULT_WALLTIME_SPEC);
									System.out.println("STARTED");

									SimpleTimer.addEvent("YourKitE",
											SystemTime.getOffsetTime(1000l * 60),
											new TimerEventPerformer() {
												public void perform(TimerEvent event) {
													try {
														Controller controller;
														controller = new Controller();
														controller.stopCPUProfiling();
														System.out.println("STOPPED");
													} catch (Exception e) {
														e.printStackTrace();
													}
												}
											});
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						});
*/
			}
		});

		chaosThreads = new Thread[numChaos];
		for (int i = 0; i < numChaos; i++) {
			final boolean[] enabled = { false };
			final Thread thread = chaosThreads[i] = new Thread("Chaos " + i) {
				public void run() {
					while (!enabled[0]) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					while (enabled[0]) {
  					if (Math.random() > 0.5) {
  						rndDel((int) (Math.random() * 100));
  					} else {
  						rndInsert((int) (Math.random() * 100));
  					}
  					
  					if (!enabled[0]) {
  						break;
  					}
  					
  					long sleepFor = (long) (Math.random() * 200);

  					try {
							Thread.sleep(sleepFor);
						} catch (InterruptedException e) {
						}
					}
				}
			};
			thread.setDaemon(true);
			thread.start();
  		final Button btnRndChaos = new Button(cToggles, SWT.TOGGLE);
  		btnRndChaos.setText("RndChaos " + i);
  		btnRndChaos.addListener(SWT.Selection, new Listener() {
  			public void handleEvent(Event event) {
  				enabled[0] = btnRndChaos.getSelection();
  			}
  		});
		}


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
				SimpleTextEntryWindow entryWindow = new SimpleTextEntryWindow("!Add!",
						"!How Many?!");
				entryWindow.prompt(new UIInputReceiverListener() {
					public void UIInputReceiverClosed(UIInputReceiver receiver) {
						if (receiver.hasSubmittedInput()) {
							final int parseInt = Integer.parseInt(receiver.getSubmittedInput());
							Utils.getOffOfSWTThread(new AERunnable() {
								public void runSupport() {
									addRows(parseInt);
								}
							});
						}
					}
				});
			}
		});

		Button btnAddSame = new Button(cBottom, SWT.PUSH);
		btnAddSame.setText("Add 5 Same + 1");
		btnAddSame.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				List<TableViewTestDS> dataSources = tv.getDataSources();
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
		btnAddSubs.setText("Add Subs to Selected");
		btnAddSubs.setSelection(pause);
		btnAddSubs.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				addSubs(-1);
			}
		});

		Button btnAddXSubs = new Button(cBottom, SWT.PUSH);
		btnAddXSubs.setText("Add Subs...");
		btnAddXSubs.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				SimpleTextEntryWindow entryWindow = new SimpleTextEntryWindow("!Add Subs!",
						"!How Many?!");
				entryWindow.prompt(new UIInputReceiverListener() {
					public void UIInputReceiverClosed(UIInputReceiver receiver) {
						if (receiver.hasSubmittedInput()) {
							int parseInt = Integer.parseInt(receiver.getSubmittedInput());
							addSubs(parseInt);
						}
					}
				});
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

		Button btnRedraw = new Button(cBottom, SWT.PUSH);
		btnRedraw.setText("redraw");
		btnRedraw.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				Composite composite = tv.getComposite();
				Rectangle ca = composite.getClientArea();
				composite.redraw(ca.x, ca.y, ca.width, ca.height, true);
				composite.update();
			}
		});
		Button btnRedraw1 = new Button(cBottom, SWT.PUSH);
		btnRedraw1.setText("redraw1");
		btnRedraw1.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				((TableViewPainted) tv).swt_updateCanvasImage(false);
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
		
		Button chkPrintDiff = new Button(cBottom, SWT.CHECK);
		chkPrintDiff.setText("PrintDiff");
		chkPrintDiff.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				printDiff = !printDiff;
			}
		});
		
		
		
		
		//////////////////
		
		
		
		cTop.setLayout(new FillLayout());
		Text txtFilter = new Text(cTop, SWT.BORDER);
		tv.enableFilterCheck(txtFilter, new TableViewFilterCheck() {
			public void filterSet(String filter) {
				System.out.println("Filter set to " + filter.length() + "; " + filter);
			}

			public boolean filterCheck(Object ds, String filter, boolean regex) {
				if (filter == null || filter.length() == 0) {
					return true;
				}
				if (ds instanceof TableViewTestDS) {
					TableViewTestDS tds = (TableViewTestDS) ds;
					String s = (String) tds.map.get("text");
					if (s == null || s.length() == 0) {
						System.out.println("BURP");
						return true;
					}
					return s.contains(filter);
				}
				return true;
			}
		});
		
		
		//////////////////

		shell.open();

		while (!shell.isDisposed()) {
			try {
				long last = System.currentTimeMillis();

				while (!display.isDisposed() && display.readAndDispatch())
					;
				if (display.isDisposed()) {
					return;
				}

				long now = System.currentTimeMillis();
				long diff = now - last;
				last = now;

				if (diff > 1 && printDiff) {
					System.out.println(diff);
				}

				display.sleep();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	protected static void addSubs(int num) {
		TableRowCore[] selectedRows = tv.getSelectedRows();
		for (TableRowCore row : selectedRows) {
			if (num < 0) {
				num = (int) (Math.random() * (-num));
			}
			//row.setSubItemCount(num);
			TableViewTestDS[] subitems = new TableViewTestDS[num];
			for (int i = 0; i < num; i++) {
				TableViewTestDS ds = new TableViewTestDS();
				ds.map.put("ID", new Double(i));
				subitems[i] = ds;
			}
			row.setSubItems(subitems);
			row.setExpanded(true);
		}
	}

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
			TableRowCore row = tv.getRowQuick(pos);
			if (row != null) {
				ds[i] = (TableViewTestDS) row.getDataSource(true);
			}
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
}
