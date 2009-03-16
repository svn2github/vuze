package com.aelitis.azureus.ui.swt.subscriptions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.views.IView;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewSWTImpl;

import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionManagerFactory;
import com.aelitis.azureus.core.subs.SubscriptionManagerListener;
import com.aelitis.azureus.ui.common.table.*;
import com.aelitis.azureus.ui.common.updater.UIUpdatable;
import com.aelitis.azureus.ui.selectedcontent.ISelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.swt.columns.subscriptions.*;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.subscriptions.SubscriptionManagerUI.sideBarItem;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarEnabler;
import com.aelitis.azureus.ui.swt.toolbar.ToolBarEnablerSelectedContent;
import com.aelitis.azureus.ui.swt.utils.ColorCache;

public class SubscriptionsView
	implements UIUpdatable, IView, SubscriptionManagerListener, ToolBarEnabler
{
	private static final String TABLE_ID = "subscriptions";

	private TableViewSWT view;

	private Composite viewComposite;
	
	private Font textFont1;
	
	private Font textFont2;
	
	public SubscriptionsView() {
	}
	
	
	public void associationsChanged(byte[] association_hash) {
		// TODO Auto-generated method stub
		
	}
	
	public void 
	subscriptionSelected(
		Subscription subscription )
	{		
	}
	
	public void subscriptionAdded(Subscription subscription) {
		if ( subscription.isSubscribed()){
			view.addDataSource(subscription);
		}
	}
	
	public void subscriptionRemoved(Subscription subscription) {
		view.removeDataSource(subscription);
		
	}
	
	public void subscriptionChanged(Subscription subscription) {
		if ( view.getRow(subscription) == null ){
			subscriptionAdded( subscription );
		}else{
			view.refreshTable(true);
		}
	}
	
	public boolean isEnabled(String itemKey) {
		if("remove".equals(itemKey) ) {
			return view.getSelectedRows().length > 0;
		}
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
		if("remove".equals(itemKey) ) {
			removeSelected();
		}
	}


	private void removeSelected() {
		TableRowCore[] rows = view.getSelectedRows();
		for(int i = 0 ; i < rows.length ; i++) {
			Subscription subs = (Subscription) rows[i].getDataSource();
			MessageBoxShell mb = 
				new MessageBoxShell(
					Utils.findAnyShell(),
					MessageText.getString("message.confirm.delete.title"),
					MessageText.getString("message.confirm.delete.text",
							new String[] {
								subs.getName()
							}), 
					new String[] {
						MessageText.getString("Button.yes"),
						MessageText.getString("Button.no")
					},
					1 );
			
			int result = mb.open();
			if (result == 0) {
				subs.remove();
			}
		}
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
		if(textFont1 != null && ! textFont1.isDisposed()) {
			textFont1.dispose();
		}
		if(textFont2 != null && ! textFont2.isDisposed()) {
			textFont2.dispose();
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
		viewComposite.setLayout(new FormLayout());
		
		TableColumnCore[] columns = new TableColumnCore[] {
				new ColumnSubscriptionNew(TABLE_ID),
				new ColumnSubscriptionName(TABLE_ID),
				new ColumnSubscriptionNbNewResults(TABLE_ID),
				new ColumnSubscriptionNbResults(TABLE_ID),
				new ColumnSubscriptionLastChecked(TABLE_ID),
				new ColumnSubscriptionSubscribers(TABLE_ID),
				new ColumnSubscriptionAutoDownload(TABLE_ID),
				
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
			
			public void selected(TableRowCore[] rows) {
				ISelectedContent[] sels = new ISelectedContent[1];
				sels[0] = new ToolBarEnablerSelectedContent(SubscriptionsView.this);
				SelectedContentManager.changeCurrentlySelectedContent("IconBarEnabler",
						sels, view);
			}
			
		}, false) ;
		
		view.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent event) {
				
			}
			
			public void keyReleased(KeyEvent event) {
				if(event.keyCode == SWT.DEL) {
					removeSelected();
				}
			}
		});
		
		view.setRowDefaultHeight(20);
		
		view.initialize(viewComposite);
		
		final Composite composite = new Composite(viewComposite,SWT.BORDER);
		composite.setBackgroundMode(SWT.INHERIT_DEFAULT);
		composite.setBackground(ColorCache.getColor(composite.getDisplay(), "#F1F9F8"));
		
		Font font = composite.getFont();
		FontData fDatas[] = font.getFontData();
		for(int i = 0 ; i < fDatas.length ; i++) {
			fDatas[i].setHeight(150 * fDatas[i].getHeight() / 100);
			if(Constants.isWindows) {
				fDatas[i].setStyle(SWT.BOLD);
			}
		}
		
		textFont1 = new Font(composite.getDisplay(),fDatas);
		
		fDatas = font.getFontData();
		for(int i = 0 ; i < fDatas.length ; i++) {
			fDatas[i].setHeight(120 * fDatas[i].getHeight() / 100);
		}
		
		textFont2 = new Font(composite.getDisplay(),fDatas);
		
		Label preText = new Label(composite,SWT.NONE);
		preText.setForeground(ColorCache.getColor(composite.getDisplay(), "#6D6F6E"));
		preText.setFont(textFont1);
		preText.setText(MessageText.getString("subscriptions.view.help.1"));
		
		Label image = new Label(composite,SWT.NONE);
		ImageLoader.getInstance().setLabelImage(image, "btn_rss_add");
		
		Link postText = new Link(composite,SWT.NONE);
		postText.setForeground(ColorCache.getColor(composite.getDisplay(), "#6D6F6E"));
		postText.setFont(textFont2);
		postText.setText(MessageText.getString("subscriptions.view.help.2"));
		
		postText.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if(event.text != null &&  ( event.text.startsWith("http://") || event.text.startsWith("https://") ) ) {
					Utils.launch(event.text);
				}
			}
		});
		
		Label close = new Label(composite,SWT.NONE);		
		ImageLoader.getInstance().setLabelImage(close, "image.dismissX");
		close.setCursor(composite.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
		close.addListener(SWT.MouseUp, new Listener() {
			public void handleEvent(Event arg0) {
				COConfigurationManager.setParameter("subscriptions.view.showhelp", false);
				composite.setVisible(false);
				FormData data = (FormData) view.getComposite().getLayoutData();
				data.bottom = new FormAttachment(100,0);
				viewComposite.layout(true);
			}
		});
		
		FormLayout layout = new FormLayout();
		composite.setLayout(layout);
		
		FormData data;
		
		data = new FormData();
		data.left = new FormAttachment(0,15);
		data.top = new FormAttachment(0,20);
		data.bottom = new FormAttachment(postText,-5);
		preText.setLayoutData(data);
		
		data = new FormData();
		data.left = new FormAttachment(preText,5);
		data.top = new FormAttachment(preText,0,SWT.TOP);
		image.setLayoutData(data);
		
		data = new FormData();
		data.left = new FormAttachment(preText,0,SWT.LEFT);
		//data.top = new FormAttachment(preText,5);
		data.bottom = new FormAttachment(100,-20);
		postText.setLayoutData(data);
		
		data = new FormData();
		data.right = new FormAttachment(100,-10);
		data.top = new FormAttachment(0,10);		
		close.setLayoutData(data);
		
		data = new FormData();
		data.left = new FormAttachment(0,0);
		data.right = new FormAttachment(100,0);
		data.top = new FormAttachment(0,0);
		data.bottom = new FormAttachment(composite,0);		
		view.getComposite().setLayoutData(data);
		
		data = new FormData();
		data.left = new FormAttachment(0,0);
		data.right = new FormAttachment(100,0);
		data.bottom = new FormAttachment(100,0);		
		composite.setLayoutData(data);
		
		COConfigurationManager.setBooleanDefault("subscriptions.view.showhelp", true);
		if(!COConfigurationManager.getBooleanParameter("subscriptions.view.showhelp")) {
			composite.setVisible(false);
			data = (FormData) view.getComposite().getLayoutData();
			data.bottom = new FormAttachment(100,0);
			viewComposite.layout(true);
		}

	}
	
	public void refresh() {
		view.refreshTable(false);
	}
	
	public void updateLanguage() {
		view.updateLanguage();
	}
	
	
	
	
	
	
	
	
}
