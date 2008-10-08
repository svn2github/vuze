package com.aelitis.azureus.ui.swt.subscriptions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.gudy.azureus2.ui.swt.IconBarEnabler;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewSWTImpl;

import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionManagerFactory;
import com.aelitis.azureus.core.subs.SubscriptionManagerListener;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.swt.columns.utils.TableColumnCreatorV3;

public class SubscriptionsView
	implements UIUpdatable, IconBarEnabler, SubscriptionManagerListener
{
	private static final String TABLE_ID = "subscriptions";

	private TableViewSWT view;

	private Composite viewComposite;
	
	public SubscriptionsView(Composite parent) {
		viewComposite = new Composite(parent,SWT.NONE);
		viewComposite.setLayout(new FillLayout());
		
		TableColumnCore[] columns = TableColumnCreatorV3.createSubscriptions(TABLE_ID);
		
		view = new TableViewSWTImpl(TABLE_ID, TABLE_ID, columns, "name", SWT.MULTI
				| SWT.FULL_SELECTION | SWT.VIRTUAL);
		
		view.addDataSources(SubscriptionManagerFactory.getSingleton().getSubscriptions());
	}
	
	
	public void associationsChanged(byte[] association_hash) {
		// TODO Auto-generated method stub
		
	}
	
	public void subscriptionAdded(Subscription subscription) {
		view.addDataSource(subscription);
		
	}
	
	public void subscriptionRemoved(Subscription subscription) {
		view.removeDataSource(subscription);
		
	}
	
	public void subscriptionChanged(Subscription subscription) {
		
	}
	
	public boolean isEnabled(String itemKey) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public String getUpdateUIName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public boolean isSelected(String itemKey) {
		return false;
	}
	
	public void itemActivated(String itemKey) {
		// TODO Auto-generated method stub
		
	}
	
	public void updateUI() {
		// TODO Auto-generated method stub
		
	}
	
	
	
	
}
