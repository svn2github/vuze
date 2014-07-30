package com.aelitis.azureus.ui.swt.subscriptions;

import java.util.Map;

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
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener;
import org.gudy.azureus2.plugins.ui.toolbar.UIToolBarItem;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEvent;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewCoreEventListener;
import org.gudy.azureus2.ui.swt.shells.MessageBoxShell;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.table.impl.TableViewFactory;

import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionManagerFactory;
import com.aelitis.azureus.core.subs.SubscriptionManagerListener;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.UserPrompterResultListener;
import com.aelitis.azureus.ui.common.ToolBarItem;
import com.aelitis.azureus.ui.common.table.*;
import com.aelitis.azureus.ui.common.table.impl.TableColumnManager;
import com.aelitis.azureus.ui.mdi.MultipleDocumentInterface;
import com.aelitis.azureus.ui.selectedcontent.ISelectedContent;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.swt.columns.subscriptions.*;
import com.aelitis.azureus.ui.swt.imageloader.ImageLoader;
import com.aelitis.azureus.ui.swt.utils.ColorCache;

public class SubscriptionsView
	implements SubscriptionManagerListener, UIPluginViewToolBarListener,
	UISWTViewCoreEventListener
{
	private static final String TABLE_ID = "subscriptions";

	private TableViewSWT view;

	private Composite viewComposite;
	
	private Font textFont1;
	
	private Font textFont2;

	private UISWTView swtView;
	
	public SubscriptionsView() {
	}
	
	
	/* (non-Javadoc)
	 * @see com.aelitis.azureus.core.subs.SubscriptionManagerListener#associationsChanged(byte[])
	 */
	public void associationsChanged(byte[] association_hash) {
		// TODO Auto-generated method stub
		
	}
	
	/* (non-Javadoc)
	 * @see com.aelitis.azureus.core.subs.SubscriptionManagerListener#subscriptionSelected(com.aelitis.azureus.core.subs.Subscription)
	 */
	public void 
	subscriptionSelected(
		Subscription subscription )
	{		
	}
	
	/* (non-Javadoc)
	 * @see com.aelitis.azureus.core.subs.SubscriptionManagerListener#subscriptionAdded(com.aelitis.azureus.core.subs.Subscription)
	 */
	public void subscriptionAdded(Subscription subscription) {
		if ( subscription.isSubscribed()){
			view.addDataSource(subscription);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.aelitis.azureus.core.subs.SubscriptionManagerListener#subscriptionRemoved(com.aelitis.azureus.core.subs.Subscription)
	 */
	public void subscriptionRemoved(Subscription subscription) {
		view.removeDataSource(subscription);
		
	}
	
	/* (non-Javadoc)
	 * @see com.aelitis.azureus.core.subs.SubscriptionManagerListener#subscriptionChanged(com.aelitis.azureus.core.subs.Subscription)
	 */
	public void subscriptionChanged(Subscription subscription) {
		if ( !subscription.isSubscribed()){
			subscriptionRemoved(subscription);
		}else if ( view.getRow(subscription) == null ){
			subscriptionAdded( subscription );
		}else{
			view.refreshTable(true);
		}
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.UIPluginViewToolBarListener#refreshToolBarItems(java.util.Map)
	 */
	public void refreshToolBarItems(Map<String, Long> list) {
		if ( view == null ){
			return;	// can happen on first selection it seems
		}
		int numRows = view.getSelectedRowsSize();
		list.put("remove", numRows > 0 ? UIToolBarItem.STATE_ENABLED : 0);
		list.put("share", numRows == 1 ? UIToolBarItem.STATE_ENABLED : 0);
	}

	/* (non-Javadoc)
	 * @see org.gudy.azureus2.plugins.ui.toolbar.UIToolBarActivationListener#toolBarItemActivated(com.aelitis.azureus.ui.common.ToolBarItem, long, java.lang.Object)
	 */
	public boolean toolBarItemActivated(ToolBarItem item, long activationType,
			Object datasource) {
		if("remove".equals(item.getID()) ) {
			removeSelected();
			return true;
		}
		return false;
	}


	private void removeSelected() {
		TableRowCore[] rows = view.getSelectedRows();
		Subscription[] subs = new Subscription[rows.length];
		int i = 0;
		for (Subscription subscription : subs) {
			subs[i] = (Subscription) rows[i++].getDataSource();
		}
		removeSubs(subs, 0);
	}
	
	private void removeSubs(final Subscription[] toRemove, final int startIndex) {
		if (toRemove == null || startIndex >= toRemove.length) {
			return;
		}

		if (toRemove[startIndex] == null) {
			int nextIndex = startIndex + 1;
			if (nextIndex < toRemove.length) {
				removeSubs(toRemove, nextIndex);
			}
			return;
		}

		MessageBoxShell mb = new MessageBoxShell(
				MessageText.getString("message.confirm.delete.title"),
				MessageText.getString("message.confirm.delete.text", new String[] {
					toRemove[startIndex].getName()
				}));

		if (startIndex == toRemove.length - 1) {
			mb.setButtons(0, new String[] {
				MessageText.getString("Button.yes"),
				MessageText.getString("Button.no"),
			}, new Integer[] { 0, 1 });
		} else {
			mb.setButtons(1, new String[] {
				MessageText.getString("Button.removeAll"),
				MessageText.getString("Button.yes"),
				MessageText.getString("Button.no"),
			}, new Integer[] { 2, 0, 1 });
		}

		mb.open(new UserPrompterResultListener() {
			public void prompterClosed(int result) {
				if (result == 0) {
					toRemove[startIndex].setSubscribed( false );
				} else if (result == 2) {
					for (int i = startIndex; i < toRemove.length; i++) {
						if (toRemove[i] != null) {
							toRemove[i].setSubscribed( false );
						}
					}
					return;
				}

				int nextIndex = startIndex + 1;
				if (nextIndex < toRemove.length) {
					removeSubs(toRemove, nextIndex);
				}
			}
		});
	}
	
	
	private void delete() {
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
	
	private Composite getComposite() {
		return viewComposite;
	}
	
	private String getFullTitle() {
		return MessageText.getString("subscriptions.overview");
	}
	
	private void initialize(Composite parent) {
		
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
				new ColumnSubscriptionCategory(TABLE_ID),
				new ColumnSubscriptionTag(TABLE_ID),
				
		};
		
		TableColumnManager tcm = TableColumnManager.getInstance();
		tcm.setDefaultColumnNames(TABLE_ID, new String[] {
			ColumnSubscriptionNew.COLUMN_ID,
			ColumnSubscriptionName.COLUMN_ID,
			ColumnSubscriptionNbNewResults.COLUMN_ID,
			ColumnSubscriptionNbResults.COLUMN_ID,
			ColumnSubscriptionAutoDownload.COLUMN_ID,
		});
		
		view = TableViewFactory.createTableViewSWT(Subscription.class, TABLE_ID, TABLE_ID,
				columns, "name", SWT.SINGLE | SWT.FULL_SELECTION | SWT.VIRTUAL);
		
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
						String key = "Subscription_" + ByteFormatter.encodeString(sub.getPublicKey());
						MultipleDocumentInterface mdi = UIFunctionsManager.getUIFunctions().getMDI();
						if (mdi != null) {
							mdi.showEntryByID(key);
						}
					}
				}
				
			}
			
			public void selected(TableRowCore[] rows) {
				ISelectedContent[] sels = new ISelectedContent[rows.length];
				
				for (int i=0;i<rows.length;i++){
					
					sels[i] = new SubscriptionSelectedContent((Subscription)rows[i].getDataSource());
				}
				
				SelectedContentManager.changeCurrentlySelectedContent(view.getTableID(),
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
		data.top = new FormAttachment(preText,0,SWT.CENTER);
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
		viewComposite.setLayoutData(data);
		
		data = new FormData();
		data.left = new FormAttachment(0,0);
		data.right = new FormAttachment(100,0);
		data.bottom = new FormAttachment(100,0);		
		composite.setLayoutData(data);

		COConfigurationManager.setBooleanDefault("subscriptions.view.showhelp", true);
		if(!COConfigurationManager.getBooleanParameter("subscriptions.view.showhelp")) {
			composite.setVisible(false);
			data = (FormData) viewComposite.getLayoutData();
			data.bottom = new FormAttachment(100,0);
			viewComposite.layout(true);
		}
	}
	
	private void refresh() {
		if ( view != null ){
			view.refreshTable(false);
		}
	}

	public boolean eventOccurred(UISWTViewEvent event) {
    switch (event.getType()) {
      case UISWTViewEvent.TYPE_CREATE:
      	swtView = (UISWTView)event.getData();
      	swtView.setTitle(getFullTitle());
        break;

      case UISWTViewEvent.TYPE_DESTROY:
        delete();
        break;

      case UISWTViewEvent.TYPE_INITIALIZE:
        initialize((Composite)event.getData());
        break;

      case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
      	Messages.updateLanguageForControl(getComposite());
      	swtView.setTitle(getFullTitle());
        break;

      case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:
      	//dataSourceChanged(event.getData());
        break;
        
      case UISWTViewEvent.TYPE_FOCUSGAINED:
      	break;
        
      case UISWTViewEvent.TYPE_REFRESH:
        refresh();
        break;
    }

    return true;
  }

}
