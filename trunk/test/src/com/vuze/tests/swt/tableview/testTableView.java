package com.vuze.tests.swt.tableview;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.ui.swt.UIConfigDefaultsSWT;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewSWTImpl;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnManager;

import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.swt.uiupdater.UIUpdaterSWT;

public class testTableView
{
	private static TableViewSWTImpl<TableViewTestDS> tv;

	private static boolean pause = false;

	public static void main(String[] args) {
		Display display = new Display();
		FormData fd;

		COConfigurationManager.initialise();
		COConfigurationManager.setParameter("Table.useTree", true);
		UIConfigDefaultsSWT.initialize();
		
		Colors.getInstance();

		Shell shell = new Shell(display, SWT.SHELL_TRIM);
		FormLayout fl = new FormLayout();
		shell.setLayout(fl);

		Composite cTV = new Composite(shell, SWT.BORDER);

		Composite cBottom = new Composite(shell, SWT.BORDER);

		fd = Utils.getFilledFormData();
		fd.bottom = new FormAttachment(cBottom, -5);
		cTV.setLayout(new FillLayout());
		cTV.setLayoutData(fd);

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
		};

		TableColumnManager.getInstance().addColumns(columns);

		tv = new TableViewSWTImpl<TableViewTestDS>(TableViewTestDS.class, "test",
				"", columns, CT_ID.name, SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL | SWT.CASCADE);

		tv.initialize(cTV);
		
		tv.setRowDefaultHeight(20);

		for (int i = 0; i < 50; i++) {
			tv.addDataSource(new TableViewTestDS() {
			});
		}

		tv.addKeyListener(new KeyListener() {

			public void keyReleased(KeyEvent e) {
			}

			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.DEL) {
					List<TableViewTestDS> sources = tv.getSelectedDataSources();
					tv.removeDataSources((TableViewTestDS[]) sources.toArray(new TableViewTestDS[0]));
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

		Button btnPauseRefresh = new Button(cBottom, SWT.TOGGLE);
		btnPauseRefresh.setText("Pause");
		btnPauseRefresh.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				pause = !pause;
			}
		});

		Button btnRndInsert = new Button(cBottom, SWT.PUSH);
		btnRndInsert.setText("RndInsert");
		btnRndInsert.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				rndInsert();
			}
		});

		Button btnRndDel = new Button(cBottom, SWT.PUSH);
		btnRndDel.setText("RndDel");
		btnRndDel.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				rndDel();
			}
		});

		Button btnRndChaos = new Button(cBottom, SWT.TOGGLE);
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

		Button btnRndChaos1 = new Button(cBottom, SWT.TOGGLE);
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

		shell.open();

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	protected static void rndDel() {
		int size = tv.size(false);
		if (size <= 0) {
			return;
		}
		int pos = (int) (Math.random() * size);
		tv.removeDataSource((TableViewTestDS) tv.getRow(pos).getDataSource(true));
	}

	protected static void rndInsert() {
		int size = tv.size(false);
		double pos = Math.random() * size;
		TableViewTestDS ds = new TableViewTestDS();
		ds.map.put("ID", new Double(pos));
		tv.addDataSource(ds);
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
				rndDel();
			} else {
				rndInsert();
			}
			SimpleTimer.addEvent("chaos",
					SystemTime.getOffsetTime((long) (Math.random() * 3000)), this);
		}
	}
}
