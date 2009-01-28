package com.aelitis.azureus.ui.swt.devices;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.ui.swt.views.IView;


import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarEnabler;

public class DevicesView
	implements UIUpdatable, IView, ToolBarEnabler
{	
	private Composite viewComposite;
	
	public DevicesView() {
	}
	
	
	public boolean isEnabled(String itemKey) {

		return false;
	}
	
	public String getUpdateUIName() {

		return null;
	}
	
	public boolean isSelected(String itemKey) {
		return false;
	}
	
	public void itemActivated(String itemKey) {
	}


	
	public void updateUI() {
	}
	
	public void dataSourceChanged(Object newDataSource) {
	}
	
	public void delete() {

	}
	
	public void generateDiagnostics(IndentWriter writer) {

	}
	
	public Composite getComposite() {
		return viewComposite;
	}
	
	public String getData() {
		return "devices.view.title";
	}
	
	public String getFullTitle() {
		return MessageText.getString("devices.view.title");
	}
	
	public String getShortTitle() {
		return MessageText.getString("devices.view.title");
	}
	
	public void initialize(Composite parent) {
		
		viewComposite = new Composite(parent,SWT.NONE);
	}
	
	public void refresh() {
	}
	
	public void updateLanguage() {
	}
}
