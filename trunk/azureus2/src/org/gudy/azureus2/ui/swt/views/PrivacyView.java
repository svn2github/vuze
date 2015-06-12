/**
 * Copyright (C) Azureus Software, Inc, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.ui.swt.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.MenuBuildUtils;
import org.gudy.azureus2.ui.swt.MenuBuildUtils.MenuBuilder;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListener;
import org.gudy.azureus2.ui.swt.views.utils.TagUIUtils;



public class PrivacyView
	implements UISWTViewCoreEventListener
{
	public static final String MSGID_PREFIX = "PrivacyView";

	private UISWTView swtView;

	private Composite cMainComposite;

	private ScrolledComposite sc;

	private Composite parent;

	public 
	PrivacyView() 
	{
	}

	public boolean eventOccurred(UISWTViewEvent event) {
		switch (event.getType()) {
			case UISWTViewEvent.TYPE_CREATE:
				swtView = (UISWTView) event.getData();
				swtView.setTitle(getFullTitle());
				break;

			case UISWTViewEvent.TYPE_DESTROY:
				delete();
				break;

			case UISWTViewEvent.TYPE_INITIALIZE:
				parent = (Composite) event.getData();
				break;

			case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
				Messages.updateLanguageForControl(cMainComposite);
				swtView.setTitle(getFullTitle());
				break;

			case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:
				Object ds = event.getData();
				dataSourceChanged(ds);
				break;

			case UISWTViewEvent.TYPE_FOCUSGAINED:
				initialize();
				//if (taggables == null) {
					dataSourceChanged(swtView.getDataSource());
				//}
				break;
				
			case UISWTViewEvent.TYPE_FOCUSLOST:
				delete();
				break;

			case UISWTViewEvent.TYPE_REFRESH:
				refresh();
				break;
		}

		return true;
	}

	private void delete() {
		Utils.disposeComposite(sc);
		dataSourceChanged(null);
	}

	private void refresh() {
	}

	private void 
	dataSourceChanged(Object ds) {


		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				swt_updateFields();
			}
		});
	}

	private void 
	initialize() 
	{
		if (cMainComposite == null || cMainComposite.isDisposed()){
			
			if ( parent == null || parent.isDisposed()){
				return;
			}
			
			sc = new ScrolledComposite(parent, SWT.V_SCROLL);
			sc.setExpandHorizontal(true);
			sc.setExpandVertical(true);
			sc.getVerticalBar().setIncrement(16);
			
			Layout parentLayout = parent.getLayout();
			
			if ( parentLayout instanceof GridLayout ){
				
				GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
				
				sc.setLayoutData(gd);
				
			}else if ( parentLayout instanceof FormLayout ){
				
				sc.setLayoutData(Utils.getFilledFormData());
			}

			cMainComposite = new Composite(sc, SWT.NONE);

			sc.setContent(cMainComposite);
			
		}else{
			
			Utils.disposeComposite(cMainComposite, false);
		}
		
		cMainComposite.setLayout(new GridLayout(1, false));


		
		sc.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				Rectangle r = sc.getClientArea();
				Point size = cMainComposite.computeSize(r.width, SWT.DEFAULT);
				sc.setMinSize(size);
			}
		});

		swt_updateFields();

		Rectangle r = sc.getClientArea();
		Point size = cMainComposite.computeSize(r.width, SWT.DEFAULT);
		sc.setMinSize(size);
	}

	private String 
	getFullTitle() 
	{
		return( MessageText.getString("label.privacy"));
	}

	private void 
	swt_updateFields() 
	{
		if ( cMainComposite == null || cMainComposite.isDisposed()){
			return;
		}
		
	}
}
