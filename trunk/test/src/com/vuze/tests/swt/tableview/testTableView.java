package com.vuze.tests.swt.tableview;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.ui.swt.UIConfigDefaultsSWT;
import org.gudy.azureus2.ui.swt.Utils;
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
		UIConfigDefaultsSWT.initialize();

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
		fd.height = 50;
		cBottom.setLayout(new FillLayout());
		cBottom.setLayoutData(fd);
		
		
		TableColumnCore[] columns = {
			new CT_ID(),
			new CT_InvalidOnly(),
			new CT_Live(),
		};
		
		TableColumnManager.getInstance().addColumns(columns);
		
		tv = new TableViewSWTImpl<TableViewTestDS>(TableViewTestDS.class, "test", "", columns, CT_ID.name);
		
		tv.initialize(cTV);
		
		for (int i = 0; i < 1000; i++) {
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
		
		Button btnRndInsert = new Button(cBottom, SWT.TOGGLE);
		btnRndInsert.setText("RndInsert");
		btnRndInsert.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				int size = tv.size(false);
				double pos = Math.random() * size;
				TableViewTestDS ds = new TableViewTestDS();
				ds.map.put("ID", new Double(pos));
				tv.addDataSource(ds);
			}
		});

		Button btnRndDel = new Button(cBottom, SWT.TOGGLE);
		btnRndDel.setText("RndDel");
		btnRndDel.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				int size = tv.size(false);
				int pos = (int) (Math.random() * size);
				tv.removeDataSource((TableViewTestDS) tv.getRow(pos).getDataSource(true));
			}
		});
		
		shell.open();
		
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}
}
