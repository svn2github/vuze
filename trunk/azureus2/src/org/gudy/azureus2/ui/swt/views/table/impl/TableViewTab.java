package org.gudy.azureus2.ui.swt.views.table.impl;

import java.util.Map;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AEDiagnosticsEvidenceGenerator;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListener;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;

import com.aelitis.azureus.ui.common.ToolBarEnabler;

public abstract class TableViewTab<DATASOURCETYPE>
	implements UISWTViewCoreEventListener, ToolBarEnabler, AEDiagnosticsEvidenceGenerator
{
	private TableViewSWT<DATASOURCETYPE> tv;
	private Object parentDataSource;
	private final String propertiesPrefix;
	private Composite composite;
	private UISWTView swtView;

	
	public TableViewTab(String propertiesPrefix) {
		this.propertiesPrefix = propertiesPrefix;
	}
	
	public TableViewSWT<DATASOURCETYPE> getTableView() {
		return tv;
	}

	private final void initialize(Composite composite) {
		tv = initYourTableView();
		if (parentDataSource != null) {
			tv.setParentDataSource(parentDataSource);
		}
		Composite parent = initComposite(composite);
		tv.initialize(parent);
		if (parent != composite) {
			this.composite = composite;
		} else {
			this.composite = tv.getComposite();
		}
		
		tableViewTabInitComplete();
	}
	
	public void tableViewTabInitComplete() {
	}

	public Composite initComposite(Composite composite) {
		return composite;
	}

	public abstract TableViewSWT<DATASOURCETYPE> initYourTableView();

	private final void dataSourceChanged(Object newDataSource) {
		this.parentDataSource = newDataSource;
		if (tv != null) {
			tv.setParentDataSource(newDataSource);
		}
	}

	private final void refresh() {
		if (tv != null) {
			tv.refreshTable(false);
		}
	}

	private final void delete() {
		if (tv != null) {
			tv.delete();
		}
	}

	private final String getFullTitle() {
		return MessageText.getString(getPropertiesPrefix() + ".title.full");
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.core3.util.AEDiagnosticsEvidenceGenerator#generate(org.gudy.azureus2.core3.util.IndentWriter)
	 */
	public void generate(IndentWriter writer) {
		if (tv != null) {
			tv.generate(writer);
		}
	}
	
	public Composite getComposite() {
		return composite;
	}
	
	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.common.ToolBarEnabler#toolBarItemActivated(java.lang.String)
	 */
	public boolean toolBarItemActivated(String itemKey) {
		if (itemKey.equals("editcolumns")) {
			if (tv instanceof TableViewSWTImpl) {
				((TableViewSWTImpl<?>)tv).showColumnEditor();
				return true;
			}
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see com.aelitis.azureus.ui.common.ToolBarEnabler#refreshToolBar(java.util.Map)
	 */
	public void refreshToolBar(Map<String, Boolean> list) {
		list.put("editcolumns", true);
	}

	public String getPropertiesPrefix() {
		return propertiesPrefix;
	}
	
	public Menu getPrivateMenu() {
		return null;
	}
	
	private void viewActivated() {
		// cheap hack.. calling isVisible freshens table's visible status (and
		// updates subviews)
		if (tv instanceof TableViewSWTImpl) {
			((TableViewSWTImpl<?>)tv).isVisible();
		}
	}
	
	private void viewDeactivated() {
		if (tv instanceof TableViewSWTImpl) {
			((TableViewSWTImpl<?>)tv).isVisible();
		}
	}

	public boolean eventOccurred(UISWTViewEvent event) {
		switch (event.getType()) {
			case UISWTViewEvent.TYPE_CREATE:
				swtView = (UISWTView) event.getData();
				swtView.setTitle(getFullTitle());
				break;

			case UISWTViewEvent.TYPE_DESTROY:
				delete();
				break;

			case UISWTViewEvent.TYPE_INITIALIZE:
				initialize((Composite) event.getData());
				break;

			case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
				swtView.setTitle(getFullTitle());
				updateLanguage();
				Messages.updateLanguageForControl(composite);
				break;

			case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:
				dataSourceChanged(event.getData());
				break;

			case UISWTViewEvent.TYPE_FOCUSGAINED:
				viewActivated();
				break;

			case UISWTViewEvent.TYPE_FOCUSLOST:
				viewDeactivated();
				break;

			case UISWTViewEvent.TYPE_REFRESH:
				refresh();
				break;
		}

		return true;
	}

	public void updateLanguage() {
	}

	public UISWTView getSWTView() {
		return swtView;
	}
}
