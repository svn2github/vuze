package com.vuze.tests.swt.tableview;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;

import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.util.MapUtils;

public class CT_LivePaintOnly
	extends CoreTableColumn
	implements TableCellRefreshListener, TableCellSWTPaintListener
{
	public static String name = new Object() {
	}.getClass().getEnclosingClass().getSimpleName();

	private static String ID_CELLPAINTS = name + ".numCP";

	private static String ID_CELLREFRESHES = name + ".numR";

	public CT_LivePaintOnly() {
		super(name, 110, "test");
		addDataSourceType(TableViewTestDS.class);
		setRefreshInterval(TableColumn.INTERVAL_LIVE);
		setVisible(true);
	}

	public void refresh(TableCell cell) {
		TableViewTestDS ds = (TableViewTestDS) cell.getDataSource();

		int num = MapUtils.getMapInt(ds.map, ID_CELLREFRESHES, 0) + 1;
		ds.map.put(ID_CELLREFRESHES, num);

		cell.setSortValue(num);
	}

	public void cellPaint(GC gc, TableCellSWT cell) {
		TableViewTestDS ds = (TableViewTestDS) cell.getDataSource();
		int numP = MapUtils.getMapInt(ds.map, ID_CELLPAINTS, 0) + 1;
		int numR = MapUtils.getMapInt(ds.map, ID_CELLREFRESHES, 0);
		ds.map.put(ID_CELLPAINTS, numP);
		GCStringPrinter.printString(gc, "r=" + Integer.toString(numR) + ";p="
				+ Integer.toString(numP), cell.getBounds(), true, true, SWT.RIGHT);
	}
}
