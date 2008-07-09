package com.aelitis.azureus.ui.swt.views.skin;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractDetailPage
	implements IDetailPage
{

	protected String pageID = null;

	private DetailPanel detailPanel = null;

	protected ActivationListener activationListener = null;

	private List refreshListeners = new ArrayList();

	public AbstractDetailPage(DetailPanel detailPanel, String pageID) {
		this.pageID = pageID;
		this.detailPanel = detailPanel;
	}

	public String getPageID() {
		return pageID;
	}

	public DetailPanel getDetailPanel() {
		return detailPanel;
	}

	public void setActivationListener(ActivationListener activationListener) {
		this.activationListener = activationListener;
	}

	public void addRefreshListener(RefreshListener listener) {
		if (false == refreshListeners.contains(listener)) {
			refreshListeners.add(listener);
		}
	}

	public void removeRefreshListener(RefreshListener listener) {
		if (true == refreshListeners.contains(listener)) {
			refreshListeners.remove(listener);
		}
	}

	protected void notifyRefreshListeners() {
		RefreshListener[] listeners = (RefreshListener[]) refreshListeners.toArray(new RefreshListener[refreshListeners.size()]);
		for (int i = 0; i < listeners.length; i++) {
			listeners[i].refreshCompleted();
			if (true == listeners[i].runOnlyOnce()) {
				removeRefreshListener(listeners[i]);
			}
		}
	}

}
