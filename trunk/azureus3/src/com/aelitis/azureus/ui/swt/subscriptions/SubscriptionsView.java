package com.aelitis.azureus.ui.swt.subscriptions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.ui.swt.IconBarEnabler;
import org.gudy.azureus2.ui.swt.views.IView;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewSWTImpl;

import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionManagerFactory;
import com.aelitis.azureus.core.subs.SubscriptionManagerListener;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.common.table.TableLifeCycleListener;
import com.aelitis.azureus.ui.common.table.TableRowCore;
import com.aelitis.azureus.ui.common.table.TableSelectionAdapter;
import com.aelitis.azureus.ui.common.table.TableSelectionListener;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.swt.columns.subscriptions.ColumnSubscriptionName;
import com.aelitis.azureus.ui.swt.columns.subscriptions.ColumnSubscriptionNbNewResults;
import com.aelitis.azureus.ui.swt.columns.subscriptions.ColumnSubscriptionNbResults;
import com.aelitis.azureus.ui.swt.columns.subscriptions.ColumnSubscriptionNew;
import com.aelitis.azureus.ui.swt.columns.subscriptions.ColumnSubscriptionLastChecked;
import com.aelitis.azureus.ui.swt.columns.utils.TableColumnCreatorV3;
import com.aelitis.azureus.ui.swt.subscriptions.SubscriptionManagerUI.sideBarItem;

public class SubscriptionsView
	implements UIUpdatable, IconBarEnabler, IView, SubscriptionManagerListener
{
	private static final String TABLE_ID = "subscriptions";

	private TableViewSWT view;

	private Composite viewComposite;
	
	public SubscriptionsView() {
		
		
		
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
		view.refreshTable(true);
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
	
	public void dataSourceChanged(Object newDataSource) {
		// TODO Auto-generated method stub
		
	}
	
	public void delete() {
		view.delete();
		if (viewComposite != null && !viewComposite.isDisposed()) {
			viewComposite.dispose();
		}
	}
	
	public void generateDiagnostics(IndentWriter writer) {
		view.generate(writer);
	}
	
	public Composite getComposite() {
		return viewComposite;
	}
	
	public String getData() {
		return "subscriptions.view.title";
	}
	
	public String getFullTitle() {
		return MessageText.getString("subscriptions.view.title");
	}
	
	public String getShortTitle() {
		return MessageText.getString("subscriptions.view.title");
	}
	
	public void initialize(Composite parent) {
		
		viewComposite = new Composite(parent,SWT.NONE);
		viewComposite.setLayout(new FillLayout());
		
		TableColumnCore[] columns = new TableColumnCore[] {
				new ColumnSubscriptionNew(TABLE_ID),
				new ColumnSubscriptionName(TABLE_ID),
				new ColumnSubscriptionNbNewResults(TABLE_ID),
				new ColumnSubscriptionNbResults(TABLE_ID),
				new ColumnSubscriptionLastChecked(TABLE_ID),
		};
		
		view = new TableViewSWTImpl(TABLE_ID, TABLE_ID, columns, "name", SWT.SINGLE
				| SWT.FULL_SELECTION | SWT.VIRTUAL);
		
		view.addLifeCycleListener(new TableLifeCycleListener() {
			public void tableViewInitialized() {
				SubscriptionManagerFactory.getSingleton().addListener( SubscriptionsView.this );
				
				view.addDataSources(SubscriptionManagerFactory.getSingleton().getSubscriptions( true ));
			}
		
			public void tableViewDestroyed() {
				SubscriptionManagerFactory.getSingleton().removeListener( SubscriptionsView.this );

			}
		});
		
		view.addSelectionListener(new TableSelectionAdapter() {
			public void defaultSelected(TableRowCore[] rows, int stateMask) {
				if(rows.length == 1) {
					TableRowCore row = rows[0];
					
					Subscription sub = (Subscription) row.getDataSource();
					if(sub != null) {
						sideBarItem item = (sideBarItem) sub.getUserData(SubscriptionManagerUI.SUB_IVIEW_KEY);
						item.activate();
					}
				}
				
			}
		}, false) ;
		
		view.setRowDefaultHeight(20);
		
		view.initialize(viewComposite);
		view.getComposite().setLayoutData(null);
	}
	
	public void refresh() {
		view.refreshTable(false);
	}
	
	public void updateLanguage() {
		view.updateLanguage();
	}
	
	
	
	
	
	
	
	
}
