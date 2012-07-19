package com.aelitis.azureus.ui.swt.plugininstall;

public interface SimplePluginInstallerListener {
	
	public void finished();
	
	public void progress(int percent);
	
	public void failed( Throwable e );

}
