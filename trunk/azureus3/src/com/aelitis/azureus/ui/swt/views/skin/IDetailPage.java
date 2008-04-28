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
}
