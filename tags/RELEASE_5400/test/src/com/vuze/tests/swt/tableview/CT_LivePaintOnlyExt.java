package com.vuze.tests.swt.tableview;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;

import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.util.MapUtils;

public class CT_LivePaintOnlyExt
	extends CoreTableColumn
	implements TableCellRefreshListener, TableCellSWTPaintListener,
	TableCellAddedListener, TableCellDisposeListener
{
	public static String name = new Object() {
	}.getClass().getEnclosingClass().getSimpleName();

	private static Timer timer;

	private static List<TableCell> cells = new ArrayList<TableCell>();

	private static String ID_TICS = name + ".num1";

	private static String ID_CELLPAINTS = name + ".numCP";

	private static String ID_CELLREFRESHES = name + ".numR";

	static {
		timer = new Timer("Simple Timer", 1);

		timer.setIndestructable();

		timer.setWarnWhenFull();

		timer.addEvent("updateLivePOExt", SystemTime.getOffsetTime(1000),
				new TimerEventPerformer() {
					public void perform(TimerEvent event) {
						TableCell[] array = cells.toArray(new TableCell[0]);
						for (TableCell cell : array) {
							if (cell.isDisposed()) {
								synchronized (cells) {
									cells.remove(cell);
								}
								continue;
							}
							TableViewTestDS ds = (TableViewTestDS) cell.getDataSource();
							int num = MapUtils.getMapInt(ds.map, ID_TICS, 0) + 1;
							ds.map.put(ID_TICS, num);
							cell.invalidate();
						}
						timer.addEvent("updateLiveExt", SystemTime.getOffsetTime(1000),
								this);
					}
				});
	}

	public CT_LivePaintOnlyExt() {
		super(name, 110, "test");
		addDataSourceType(TableViewTestDS.class);
		setRefreshInterval(TableColumn.INTERVAL_LIVE);
		setVisible(true);
	}

	public void refresh(TableCell cell) {
		TableViewTestDS ds = (TableViewTestDS) cell.getDataSource();

		int num = MapUtils.getMapInt(ds.map, ID_CELLREFRESHES, 0) + 1;
		ds.map.put(ID_CELLREFRESHES, num);

//		cell.setSortValue(num);
	}

	public void cellPaint(GC gc, TableCellSWT cell) {
		TableViewTestDS ds = (TableViewTestDS) cell.getDataSource();
		int numP = MapUtils.getMapInt(ds.map, ID_CELLPAINTS, 0) + 1;
		int numR = MapUtils.getMapInt(ds.map, ID_CELLREFRESHES, 0);
		int numT = MapUtils.getMapInt(ds.map, ID_TICS, 0);
		ds.map.put(ID_CELLPAINTS, numP);
		GCStringPrinter.printString(gc, "t=" + Integer.toString(numT) + ";r=" + Integer.toString(numR) + ";p="
				+ Integer.toString(numP), cell.getBounds(), true, true, SWT.RIGHT);
	}

	public void cellAdded(final TableCell cell) {
		synchronized (cells) {
			cells.add(cell);
		}
	}

	// @see org.gudy.azureus2.plugins.ui.tables.TableCellDisposeListener#dispose(org.gudy.azureus2.plugins.ui.tables.TableCell)
	public void dispose(TableCell cell) {
		synchronized (cells) {
			cells.remove(cell);
		}
	}
}
