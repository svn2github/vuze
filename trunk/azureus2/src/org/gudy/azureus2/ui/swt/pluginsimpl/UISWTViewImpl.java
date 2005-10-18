/*
 * File    : UISWTViewImpl.java
 * Created : Oct 14, 2005
 * By      : TuxPaper
 *
 * Copyright (C) 2005 Aelitis SARL, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

package org.gudy.azureus2.ui.swt.pluginsimpl;

import java.awt.Frame;
import java.awt.Panel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.mainwindow.MainWindow;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.views.AbstractIView;

/**
 * @author TuxPaper
 *
 */
public class UISWTViewImpl extends AbstractIView implements UISWTView {
	public static final String CFG_PREFIX = "Views.plugins.";

	private Object dataSource = null;

	private final UISWTViewEventListener eventListener;

	private Composite composite;

	private final String sViewID;

	private int iControlType = UISWTView.CONTROLTYPE_SWT;
	
	private boolean bFirstGetCompositeCall = true;

	private final String sParentID;
	
	private String sTitle = null;

	/**
	 * 
	 * @param sViewID
	 * @param eventListener
	 */
	public UISWTViewImpl(String sParentID, String sViewID, UISWTViewEventListener eventListener)
			throws Exception {
		this.sParentID = sParentID;
		this.sViewID = sViewID;
		this.eventListener = eventListener;

		if (!eventListener.eventOccurred(new UISWTViewEventImpl(this,
				UISWTViewEvent.TYPE_CREATE, this)))
			throw new Exception();
	}

	// UISWTPluginView implementation
	// ==============================

	public Object getDataSource() {
		return dataSource;
	}

	public String getViewID() {
		return sViewID;
	}

	public void closeView() {
		try {
			Display display = MainWindow.getWindow().getDisplay();
			
			if ( display.getThread() == Thread.currentThread()){
				
				MainWindow.getWindow().closePluginView(this);
				
			}else{
				
				display.syncExec(
					new Runnable()
					{
						public void
						run()
						{
							MainWindow.getWindow().closePluginView(UISWTViewImpl.this);
						}
					});
			}
		} catch (Exception e) {
			Debug.out(e);
		}
	}

	public void setControlType(int iControlType) {
		if (iControlType == UISWTView.CONTROLTYPE_AWT
				|| iControlType == UISWTView.CONTROLTYPE_SWT)
			this.iControlType = iControlType;
	}

	public void triggerEvent(int eventType, Object data) {
		eventListener.eventOccurred(new UISWTViewEventImpl(this, eventType, data));
	}

	
	public void setTitle(String title) {
		sTitle = title;
		
	}

	// AbstractIView Implementation
	// ============================

	public void dataSourceChanged(Object newDataSource) {
		dataSource = newDataSource;

		eventListener.eventOccurred(new UISWTViewEventImpl(this,
				UISWTViewEvent.TYPE_DATASOURCE_CHANGED, newDataSource));
	}

	public void delete() {
		eventListener.eventOccurred(new UISWTViewEventImpl(this,
				UISWTViewEvent.TYPE_DESTROY, null));
		super.delete();
	}

	public Composite getComposite() {
		if (bFirstGetCompositeCall) {
			bFirstGetCompositeCall = false;
		}
		return composite;
	}

	public String getData() {
		final String key = CFG_PREFIX + sViewID + ".title";
		if (MessageText.keyExists(key))
			return key;
		// For now, to get plugin developers to update their plugins
		// return key;
		// For release, change it to this, to make it at least shorter:
		return sViewID;
	}

	public String getFullTitle() {
		if (sTitle != null)
			return sTitle;

		return super.getFullTitle();
	}

	public void initialize(Composite parent) {
		if (iControlType == UISWTView.CONTROLTYPE_SWT) {
			composite = new Composite(parent, SWT.NULL);
			GridLayout layout = new GridLayout(1, false);
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			composite.setLayout(layout);
			GridData gridData = new GridData(GridData.FILL_BOTH);
			composite.setLayoutData(gridData);

			eventListener.eventOccurred(new UISWTViewEventImpl(this,
					UISWTViewEvent.TYPE_INITIALIZE, composite));
		} else {
			composite = new Composite(parent, SWT.EMBEDDED);
			GridLayout layout = new GridLayout(1, false);
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			composite.setLayout(layout);
			GridData gridData = new GridData(GridData.FILL_BOTH);
			composite.setLayoutData(gridData);

			Frame f = SWT_AWT.new_Frame(composite);

			Panel pan = new Panel();

			f.add(pan);

			eventListener.eventOccurred(new UISWTViewEventImpl(this,
					UISWTViewEvent.TYPE_INITIALIZE, pan));
		}
		
		if (composite != null) {
			composite.addListener(SWT.Activate, new Listener() {
				public void handleEvent(Event event) {
					eventListener.eventOccurred(new UISWTViewEventImpl(UISWTViewImpl.this,
							UISWTViewEvent.TYPE_FOCUSGAINED, null));
				}
			});
	
			composite.addListener(SWT.Deactivate, new Listener() {
				public void handleEvent(Event event) {
					eventListener.eventOccurred(new UISWTViewEventImpl(UISWTViewImpl.this,
							UISWTViewEvent.TYPE_FOCUSLOST, null));
				}
			});
		}
	}

	public void refresh() {
		eventListener.eventOccurred(new UISWTViewEventImpl(this,
				UISWTViewEvent.TYPE_REFRESH, null));
	}

	public void updateLanguage() {
		super.updateLanguage();

		eventListener.eventOccurred(new UISWTViewEventImpl(this,
				UISWTViewEvent.TYPE_LANGUAGEUPDATE, null));
	}
	
	// Core Functions
	public String getParentID() {
		return sParentID;
	}
}
