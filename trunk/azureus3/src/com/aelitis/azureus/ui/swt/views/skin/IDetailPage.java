package com.aelitis.azureus.ui.swt.views.skin;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public interface IDetailPage
{
	public String getPageID();
	public void createControls(Composite parent);
	public Control getControl();
}
