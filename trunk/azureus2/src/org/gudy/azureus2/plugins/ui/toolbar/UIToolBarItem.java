package org.gudy.azureus2.plugins.ui.toolbar;

public interface UIToolBarItem
{
	public String getID();
	public String getTextID();
	public void setTextID(String id);
	public String getImageID();
	public void setImageID(String id);
	public boolean isAlwaysAvailable();
	public boolean isEnabled();
	public void setEnabled(boolean b);
	public boolean triggerToolBarItem(long activationType, Object datasource);
	public void setDefaultActivationListener(UIToolBarActivationListener defaultActivation);
}
