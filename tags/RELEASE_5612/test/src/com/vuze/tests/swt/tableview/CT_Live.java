package com.vuze.tests.swt.tableview;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;

import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.ui.tables.TableCell;
import org.gudy.azureus2.plugins.ui.tables.TableCellRefreshListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.ui.swt.shells.GCStringPrinter;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWT;
import org.gudy.azureus2.ui.swt.views.table.TableCellSWTPaintListener;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.util.MapUtils;

public class CT_Live
	extends CoreTableColumn
	implements TableCellRefreshListener,
		TableCellSWTPaintListener
{
	public static String name = new Object() { }.getClass().getEnclosingClass().getSimpleName();
	private static String ID_CELLPAINTS = name + ".numCP";

	public CT_Live() {
		super(name, 110, "test");
		addDataSourceType(TableViewTestDS.class);
		setRefreshInterval(TableColumn.INTERVAL_LIVE);
		setVisible(true);
	}

	public void refresh(TableCell cell) {
		TableViewTestDS ds = (TableViewTestDS) cell.getDataSource();

		int num = MapUtils.getMapInt(ds.map, name + ".num1", 0) + 1;
		ds.map.put(name + ".num1", num);
		
		cell.setSortValue(num);
		cell.setText(Integer.toString(num));
	}

	public void cellPaint(GC gc, TableCellSWT cell) {
		TableViewTestDS ds = (TableViewTestDS) cell.getDataSource();
		int num = MapUtils.getMapInt(ds.map, ID_CELLPAINTS, 0) + 1;
		ds.map.put(ID_CELLPAINTS, num);
		GCStringPrinter.printString(gc, "p=" + Integer.toString(num), cell.getBounds(), true, true, SWT.RIGHT);
	}
}
