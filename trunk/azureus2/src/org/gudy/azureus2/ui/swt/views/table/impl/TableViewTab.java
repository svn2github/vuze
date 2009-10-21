package org.gudy.azureus2.ui.swt.views.table.impl;

import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.ui.swt.views.AbstractIView;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;

public abstract class TableViewTab extends AbstractIView
{
	private TableViewSWT tv;
	private Object parentDataSource;
	private final String propertiesPrefix;

	
	public TableViewTab(String propertiesPrefix) {
		this.propertiesPrefix = propertiesPrefix;
	}
	
	public TableViewSWT getTableView() {
		return tv;
	}

	public final void initialize(Composite composite) {
		tv = initYourTableView();
		tv.initialize(composite);
		if (parentDataSource != null) {
			tv.setParentDataSource(parentDataSource);
		}
	}
	
	public abstract TableViewSWT initYourTableView();

	public final void dataSourceChanged(Object newDataSource) {
		this.parentDataSource = newDataSource;
		if (tv != null) {
			tv.setParentDataSource(newDataSource);
		}
	}

	public void updateLanguage() {
		super.updateLanguage();
		if (tv != null) {
			tv.updateLanguage();
		}
	}

	public final void refresh() {
		if (tv != null) {
			tv.refreshTable(false);
		}
	}

	// @see org.gudy.azureus2.ui.swt.views.AbstractIView#delete()
	public final void delete() {
		if (tv != null) {
			tv.delete();
		}
		super.delete();
	}

	// @see org.gudy.azureus2.ui.swt.views.AbstractIView#getData()
	public final String getData() {
		return getPropertiesPrefix() + ".title.short";
	}

	public final String getFullTitle() {
		return MessageText.getString(getPropertiesPrefix() + ".title.full");
	}

	// @see org.gudy.azureus2.ui.swt.views.AbstractIView#generateDiagnostics(org.gudy.azureus2.core3.util.IndentWriter)
	public final void generateDiagnostics(IndentWriter writer) {
		if (tv != null) {
			tv.generate(writer);
		}
	}
	
	// @see org.gudy.azureus2.ui.swt.views.AbstractIView#getComposite()
	public Composite getComposite() {
		return tv == null ? null : tv.getComposite();
	}
	
	public void itemActivated(String itemKey) {
		if (itemKey.equals("editcolumns")) {
			if (tv instanceof TableViewSWTImpl) {
				((TableViewSWTImpl)tv).showColumnEditor();
			}
		}
	}
	
	public boolean isEnabled(String itemKey) {
		if (itemKey.equals("editcolumns")) {return true;}
		return false;
	}

	public String getPropertiesPrefix() {
		return propertiesPrefix;
	}
}
