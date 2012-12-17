package com.aelitis.azureus.ui.common.table.impl;

import java.util.Comparator;

import com.aelitis.azureus.ui.common.table.TableRowCore;

public class TableRowCoreSorter
	implements Comparator<TableRowCore>
{
	@SuppressWarnings("null")
	public int compare(TableRowCore o1, TableRowCore o2) {
		TableRowCore parent1 = o1.getParentRowCore();
		TableRowCore parent2 = o2.getParentRowCore();
		boolean hasParent1 = parent1 != null;
		boolean hasParent2 = parent2 != null;

		if (parent1 == parent2 || (!hasParent1 && !hasParent2)) {
			return o1.getIndex() - o2.getIndex();
		}
		if (hasParent1 && hasParent2) {
			return parent1.getIndex() - parent2.getIndex();
		}
		if (hasParent1) {
			if (parent1 == o2) {
				return 1;
			}
			return parent1.getIndex() - o2.getIndex();
		}
		if (o1 == parent2) {
			return 0;
		}
		return o1.getIndex() - parent2.getIndex();
	}
}
