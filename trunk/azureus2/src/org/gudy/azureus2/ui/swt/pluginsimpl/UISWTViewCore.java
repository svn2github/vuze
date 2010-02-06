package org.gudy.azureus2.ui.swt.pluginsimpl;

import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.ui.swt.plugins.PluginUISWTSkinObject;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;

/**
 * A holding area between the public UISWTView plugin interface,
 * and things that we may eventually move into UISWTView
 * 
 */
public interface UISWTViewCore
	extends UISWTView
{
	public static final int CONTROLTYPE_SKINOBJECT = 0x100 + 1;

	public void setSkinObject(PluginUISWTSkinObject so, Composite composite);
	
	public PluginUISWTSkinObject getSkinObject();
	
	public void setDataSource(Object ds);
}
