package com.vuze.tests.swt.tableview;

import org.gudy.azureus2.plugins.ui.tables.*;
import org.gudy.azureus2.ui.swt.views.table.utils.CoreTableColumn;

import com.aelitis.azureus.util.MapUtils;

public class CT_Text
	extends CoreTableColumn
	implements TableCellRefreshListener, TableCellAddedListener
{
	public static String name = new Object() { }.getClass().getEnclosingClass().getSimpleName();
	private static String ID_TICS = name + ".num1";

	public CT_Text() {
		super(name, 110, "test");
		addDataSourceType(TableViewTestDS.class);
		setRefreshInterval(TableColumn.INTERVAL_INVALID_ONLY);
		setVisible(true);
	}
	
	public void refresh(TableCell cell) {
		TableViewTestDS ds = (TableViewTestDS) cell.getDataSource();

		int num = MapUtils.getMapInt(ds.map, ID_TICS, 0) + 1;
		ds.map.put(ID_TICS, num);

		cell.setText((String) ds.map.get("text"));
	}

	public void cellAdded(TableCell cell) {
		TableViewTestDS ds = (TableViewTestDS) cell.getDataSource();
		if (!ds.map.containsKey("text")) {
			ds.map.put("text", getRandomWord());
		}
	}

	private String getRandomWord() {
		int len = (int) (Math.random() * 10) + 2;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < len; i ++) {
			double f = Math.random();
			char c;
			if (f < 0.1) {
				c = (char) ((Math.random() * 12) + '0');
			} else if (f < 0.2) {
				c = (char) ((Math.random() * 26) + 'A');
			} else if (f < 0.98) {
				c = (char) ((Math.random() * 26) + 'a');
			} else {
				c = ' ';
			}
			sb.append(c);
		}
		return sb.toString();
	}
}
