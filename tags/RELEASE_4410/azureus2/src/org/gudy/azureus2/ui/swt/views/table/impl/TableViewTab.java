package org.gudy.azureus2.ui.swt.views.table.impl;

import java.util.Map;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.ui.swt.views.AbstractIView;
import org.gudy.azureus2.ui.swt.views.IViewExtension;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;

import com.aelitis.azureus.ui.common.ToolBarEnabler;

public abstract class TableViewTab<DATASOURCETYPE>
	extends AbstractIView
	implements IViewExtension, ToolBarEnabler
{
	private TableViewSWT<DATASOURCETYPE> tv;
	private Object parentDataSource;
	private final String propertiesPrefix;
	private Composite composite;

	
	public TableViewTab(String propertiesPrefix) {
		this.propertiesPrefix = propertiesPrefix;
	}
	
	public TableViewSWT<DATASOURCETYPE> getTableView() {
		return tv;
	}

	public final void initialize(Composite composite) {
		tv = initYourTableView();
		Composite parent = initComposite(composite);
		tv.initialize(parent);
		if (parent != composite) {
			this.composite = composite;
		} else {
			this.composite = tv.getComposite();
		}
		
		tableViewTabInitComplete();
		if (parentDataSource != null) {
			tv.setParentDataSource(parentDataSource);
		}
	}
	
	public void tableViewTabInitComplete() {
	}

	public Composite initComposite(Composite composite) {
		return composite;
	}

	public abstract TableViewSWT<DATASOURCETYPE> initYourTableView();

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
		return composite;
	}
	
	public boolean toolBarItemActivated(String itemKey) {
		if (itemKey.equals("editcolumns")) {
			if (tv instanceof TableViewSWTImpl) {
				((TableViewSWTImpl)tv).showColumnEditor();
				return true;
			}
		}
		return false;
	}
	
	public void refreshToolBar(Map<String, Boolean> list) {
		list.put("ediltColumns", true);
	}

	public String getPropertiesPrefix() {
		return propertiesPrefix;
	}
	
	public Menu getPrivateMenu() {
		return null;
	}
	
	public void viewActivated() {
		// cheap hack.. calling isVisible freshens table's visible status (and
		// updates subviews)
		if (tv instanceof TableViewSWTImpl) {
			((TableViewSWTImpl)tv).isVisible();
		}
	}
	
	public void viewDeactivated() {
		if (tv instanceof TableViewSWTImpl) {
			((TableViewSWTImpl)tv).isVisible();
		}
	}
}
