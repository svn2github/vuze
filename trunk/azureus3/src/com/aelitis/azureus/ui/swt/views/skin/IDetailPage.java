package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.aelitis.azureus.core.messenger.ClientMessageContext;

public interface IDetailPage
{
	public String getPageID();

	public void createControls(Composite parent);

	public Control getControl();

	public DetailPanel getDetailPanel();

	public ClientMessageContext getMessageContext();

	public void refresh();

	public void setActivationListener(ActivationListener listener);

	public void addRefreshListener(RefreshListener listener);

	public void removeRefreshListener(RefreshListener listener);

	public abstract class RefreshListener
	{
		public boolean runOnlyOnce() {
			return false;
		}

		public abstract void refreshCompleted();
	}

	public interface ActivationListener
	{
		public void pageActivated();
	}
}
