package com.aelitis.azureus.ui.swt.devices;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.ui.swt.views.IView;


import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarEnabler;

public class DevicesView
	implements UIUpdatable, IView, ToolBarEnabler
{	
	private Composite composite;
	
	public 
	DevicesView() 
	{
	}
	
	public void 
	initialize(
		Composite parent ) 
	{
		composite = new Composite(parent,SWT.NONE);
		
		composite.setLayout(new FormLayout());

		FormData data = new FormData();
		data.left = new FormAttachment(0,0);
		data.right = new FormAttachment(100,0);
		data.top = new FormAttachment(composite,0);
		data.bottom = new FormAttachment(100,0);

		Label label = new Label( composite, SWT.NULL );
		
		label.setText( "Nothing to show here" );
		
		label.setLayoutData( data );
	}
	
	public void 
	delete() 
	{
		if ( composite != null && !composite.isDisposed()){
			
			composite.dispose();
			
			composite = null;
		}
	}
	
	public boolean 
	isEnabled(
		String itemKey ) 
	{
		return false;
	}
	
	public String 
	getUpdateUIName() 
	{

		return null;
	}
	
	public boolean 
	isSelected(
		String itemKey ) 
	{
		return false;
	}
	
	public void 
	itemActivated(
		String itemKey )
	{
	}
	
	public void 
	updateUI() 
	{
	}
	
	public void 
	dataSourceChanged(
		Object newDataSource) 
	{
	}
	

	
	public void 
	generateDiagnostics(
		IndentWriter writer) 
	{
	}
	
	public Composite 
	getComposite() 
	{
		return composite;
	}
	
	public String 
	getData() 
	{
		return( "devices.view.title" );
	}
	
	public String 
	getFullTitle() 
	{
		return MessageText.getString("devices.view.title");
	}
	
	public String 
	getShortTitle() 
	{
		return( getFullTitle());
	}
	

	
	public void 
	refresh() 
	{
	}
	
	public void 
	updateLanguage() 
	{
	}
}
