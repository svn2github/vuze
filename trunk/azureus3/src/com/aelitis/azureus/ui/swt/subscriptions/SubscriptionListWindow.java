package com.aelitis.azureus.ui.swt.subscriptions;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;

import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionAssociationLookup;
import com.aelitis.azureus.core.subs.SubscriptionException;
import com.aelitis.azureus.core.subs.SubscriptionLookupListener;
import com.aelitis.azureus.core.subs.SubscriptionManager;
import com.aelitis.azureus.core.subs.SubscriptionManagerFactory;
import com.aelitis.azureus.core.subs.SubscriptionPopularityListener;
import com.aelitis.azureus.ui.swt.widgets.AnimatedImage;

public class SubscriptionListWindow implements SubscriptionLookupListener {
	
	private DownloadManager download;
	private boolean			useCachedSubs;
	
	private Display display;
	private Shell shell;
	
	AnimatedImage animatedImage;
	
	Button action;
	Label loadingText;
	ProgressBar loadingProgress;
	boolean loadingDone = false;
	
	SubscriptionAssociationLookup lookup = null;
	
	Composite mainComposite;
	Composite loadingPanel;
	Composite listPanel;
	Table subscriptionsList;
	StackLayout mainLayout;
	
	private class SubscriptionItemModel {
		String name;
		long popularity;
		String popularityDisplay;
		Subscription subscription;
		boolean selected;
	}
	
	SubscriptionItemModel subscriptionItems[];
	
	
	
	public SubscriptionListWindow(DownloadManager download, boolean useCachedSubs ) {
		this.download 		= download;
		this.useCachedSubs	= useCachedSubs;
		
		shell = ShellFactory.createMainShell(SWT.TITLE);
		shell.setSize(400,300);
		Utils.centreWindow(shell);
		
		display = shell.getDisplay();
		shell.setText(MessageText.getString("subscriptions.listwindow.title"));
		
		shell.setLayout(new FormLayout());
		
		mainComposite = new Composite(shell,SWT.NONE);
		Label separator = new Label(shell,SWT.SEPARATOR | SWT.HORIZONTAL);
		Button cancel = new Button(shell,SWT.PUSH);
		action = new Button(shell,SWT.PUSH);
		cancel.setText(MessageText.getString("Button.cancel"));
		
		FormData data;
		
		data = new FormData();
		data.left = new FormAttachment(0,0);
		data.right = new FormAttachment(100,0);
		data.top = new FormAttachment(0,0);
		data.bottom = new FormAttachment(separator,0);
		mainComposite.setLayoutData(data);
		
		data = new FormData();
		data.left = new FormAttachment(0,0);
		data.right = new FormAttachment(100,0);
		data.bottom = new FormAttachment(cancel,-2);
		separator.setLayoutData(data);
		
		data = new FormData();
		data.right = new FormAttachment(action);
		data.width = 100;
		data.bottom = new FormAttachment(100,-5);
		cancel.setLayoutData(data);
		
		data = new FormData();
		data.right = new FormAttachment(100,-5);
		data.width = 100;
		data.bottom = new FormAttachment(100,-5);
		action.setLayoutData(data);
		
		cancel.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				if(lookup != null) {
					lookup.cancel();
				}
				if(!shell.isDisposed()) {
					shell.dispose();
				}
			}
		});
		
		mainLayout = new StackLayout();
		mainComposite.setLayout(mainLayout);
		
		loadingPanel = new Composite(mainComposite,SWT.NONE);
		loadingPanel.setLayout(new FormLayout());
		
		listPanel = new Composite(mainComposite,SWT.NONE);
		listPanel.setLayout(new FillLayout());
		
		subscriptionsList = new Table(listPanel,SWT.V_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION | SWT.VIRTUAL);
		subscriptionsList.setHeaderVisible(true);
		
		TableColumn name = new TableColumn(subscriptionsList,SWT.NONE);
		name.setText(MessageText.getString("subscriptions.listwindow.name"));
		name.setWidth(310);
		name.setResizable(false);
		
		TableColumn popularity = new TableColumn(subscriptionsList,SWT.NONE);
		popularity.setText(MessageText.getString("subscriptions.listwindow.popularity"));
		popularity.setWidth(70);
		popularity.setResizable(false);
		
		subscriptionsList.addListener(SWT.SetData, new Listener() {
			public void handleEvent(Event e) {
				TableItem item = (TableItem) e.item;
				int index = subscriptionsList.indexOf(item);
				if(index >= 0 && index < subscriptionItems.length) {
					SubscriptionItemModel subscriptionItem = subscriptionItems[index];
					item.setText(0,subscriptionItem.name);
					item.setText(1,subscriptionItem.popularityDisplay);
				}
			}
		});
		
		subscriptionsList.setSortColumn(popularity);
		subscriptionsList.setSortDirection(SWT.DOWN);
		
		subscriptionsList.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				action.setEnabled(subscriptionsList.getSelectionIndex() != -1);
			}
		});
		
		Listener sortListener = new Listener() {
			public void handleEvent(Event e) {
				// determine new sort column and direction
				TableColumn sortColumn = subscriptionsList.getSortColumn();
				TableColumn currentColumn = (TableColumn) e.widget;
				int dir = subscriptionsList.getSortDirection();
				if (sortColumn == currentColumn) {
					dir = dir == SWT.UP ? SWT.DOWN : SWT.UP;
				} else {
					subscriptionsList.setSortColumn(currentColumn);
					dir = SWT.DOWN;
				}
				subscriptionsList.setSortDirection(dir);
				sortAndRefresh();
			}
		};
		name.addListener(SWT.Selection, sortListener);
		popularity.addListener(SWT.Selection, sortListener);
		
		animatedImage = new AnimatedImage(loadingPanel);
		loadingText = new Label(loadingPanel,SWT.WRAP | SWT.CENTER);
		loadingProgress = new ProgressBar(loadingPanel,SWT.HORIZONTAL);
		
		animatedImage.setImageFromName("spinner_big");
		String contentName = "Dummy";
		if(download != null) {
			contentName = download.getDisplayName();
		}
		loadingText.setText(MessageText.getString("subscriptions.listwindow.loadingtext", new String[] {contentName}));
		
		loadingProgress.setMinimum(0);
		loadingProgress.setMaximum(300);
		loadingProgress.setSelection(0);
		
		data = new FormData();
		data.left = new FormAttachment(1,2,-16);
		data.top = new FormAttachment(1,2,-32);
		data.width = 32;
		data.height = 32;
		animatedImage.setLayoutData(data);
		
		data = new FormData();
		data.left = new FormAttachment(0,5);
		data.right = new FormAttachment(100,-5);
		data.top = new FormAttachment(animatedImage.getControl(),10);
		data.height = 50;
		loadingText.setLayoutData(data);
		
		data = new FormData();
		data.left = new FormAttachment(0,5);
		data.right = new FormAttachment(100,-5);
		data.top = new FormAttachment(loadingText,5);
		loadingProgress.setLayoutData(data);
		
		boolean autoCheck = COConfigurationManager.getBooleanParameter("subscriptions.autocheck");
		
		if(autoCheck) {
			startChecking();
		} else {
			action.setText(MessageText.getString("Button.yes"));
			Composite acceptPanel = new Composite(mainComposite,SWT.NONE);
			acceptPanel.setLayout(new FormLayout());
			
			Label acceptLabel = new Label(acceptPanel,SWT.WRAP | SWT.CENTER);
			
			acceptLabel.setText(MessageText.getString("subscriptions.listwindow.autochecktext"));
			
			data = new FormData();
			data.left = new FormAttachment(0,5);
			data.right = new FormAttachment(100,-5);
			data.top = new FormAttachment(1,3,0);
			acceptLabel.setLayoutData(data);
			
			action.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					action.removeListener(SWT.Selection,this);
					COConfigurationManager.setParameter("subscriptions.autocheck",true);
					startChecking();
					mainComposite.layout();	
				}
			});
			mainLayout.topControl = acceptPanel;
		}
		
		
		//shell.setSize(400,300);
		shell.open();
			
	}

	private void startChecking() {
		action.setText(MessageText.getString("subscriptions.listwindow.subscribe"));
		action.setEnabled(false);
		try {
			if(download != null) {
				byte[] hash = download.getTorrent().getHash();
				
				SubscriptionManager subs_man = SubscriptionManagerFactory.getSingleton();
				if ( useCachedSubs ){
					Subscription[] subs = subs_man.getKnownSubscriptions( hash );
					complete(hash,subs);
				}else{
					lookup = subs_man.lookupAssociations(hash, this);
				}
				
			} else {

			}
			loadingDone = false;
			AEThread2 progressMover = new AEThread2("progressMover",true) {
				public void run() {
					final int[] waitTime = new int[1];
					waitTime[0]= 100;
					while(!loadingDone) {
						if(display != null && ! display.isDisposed()) {
							display.asyncExec(new Runnable() {
								public void run() {
									if(loadingProgress != null && !loadingProgress.isDisposed()) {
										int currentSelection = loadingProgress.getSelection() +1;
										loadingProgress.setSelection(currentSelection);
										if(currentSelection > (loadingProgress.getMaximum()) * 80 / 100) {
											waitTime[0] = 300;
										}
										if (currentSelection > (loadingProgress.getMaximum()) * 90 / 100) {
											waitTime[0] = 1000;
										}
									} else {
										loadingDone = true;
									}
								}
							});
						}
						try {
							Thread.sleep(waitTime[0]);
							//Thread.sleep(100);
						} catch (Exception e) {
							loadingDone = true;
						}
					}
				}
			};
			progressMover.start();
			
		} catch(Exception e) {
			failed(null,null);
		}
		animatedImage.start();
		mainLayout.topControl = loadingPanel;
	}
	
	/*private void populateSubscription(final Subscription subscription) {
		final TableItem item = new TableItem(subscriptionsList,SWT.NONE);
		item.setData("subscription",subscription);
		item.setText(0,subscription.getName());
		try {
			item.setText(1,MessageText.getString("subscriptions.listwindow.popularity.reading"));
			
			
		
		
		
		action.setEnabled(true);
	}*/
	
	public void found(byte[] hash, Subscription subscription) {
		// TODO Auto-generated method stub
		
	}
	
	public void complete(final byte[] hash,final Subscription[] subscriptions) {
		if( ! (subscriptions.length > 0) ) {
			failed(hash, null);
		} else {
			subscriptionItems = new SubscriptionItemModel[subscriptions.length];
			for(int i = 0 ; i < subscriptions.length ; i++) {
				final SubscriptionItemModel subscriptionItem = new SubscriptionItemModel();
				subscriptionItems[i] = subscriptionItem;
				subscriptionItem.name = subscriptions[i].getName();
				subscriptionItem.popularity = -1;
				subscriptionItem.popularityDisplay = MessageText.getString("subscriptions.listwindow.popularity.reading");
				subscriptionItem.subscription = subscriptions[i];
				
				try {
				subscriptions[i].getPopularity(
						new SubscriptionPopularityListener()
						{
							public void
							gotPopularity(
								long		popularity )
							{
								update(subscriptionItem,popularity, popularity + "" );
							}
							
							public void
							failed(
								SubscriptionException		error )
							{
								update(subscriptionItem,-2,MessageText.getString("subscriptions.listwindow.popularity.unknown"));
							}
							
							
						});
				} catch(SubscriptionException e) {
					
					update(subscriptionItem,-2,MessageText.getString("subscriptions.listwindow.popularity.unknown"));
				
				}
				
			}
			
			if(display != null && !display.isDisposed()) {
				display.asyncExec(new Runnable() {
					public void run() {
						animatedImage.stop();

						mainLayout.topControl = listPanel;
						mainComposite.layout();
						
						sortAndRefresh();
						subscriptionsList.setSelection(0);
						
						action.addListener(SWT.Selection, new Listener() {
							public void handleEvent(Event arg0) {
								if(subscriptionsList != null && !subscriptionsList.isDisposed()) {
									int selectedIndex = subscriptionsList.getSelectionIndex();
									if(selectedIndex >= 0 && selectedIndex < subscriptionItems.length) {
										Subscription subscription = (Subscription) subscriptionItems[selectedIndex].subscription;
										if(subscription != null) {
											subscription.setSubscribed(true);
										}
									}
								}
							}
						});
					}

				});
			}
		}
	}
	
	protected void
	update(
		final SubscriptionItemModel subscriptionItem,
		final long		popularity,
		final String	text )
	{
		subscriptionItem.popularity = popularity;
		subscriptionItem.popularityDisplay = text;
	
		display.asyncExec(
			new Runnable()
			{
				public void
				run()
				{
					sortAndRefresh();
				}
			});
	}
	
	private void sortAndRefresh() {
		
		if ( subscriptionsList.isDisposed()){
			
			return;
		}
		
		for(int i = 0 ; i < subscriptionItems.length ; i++) {
			subscriptionItems[i].selected = false;
		}
		
		int currentSelection = subscriptionsList.getSelectionIndex();
		if(currentSelection >= 0 && currentSelection < subscriptionItems.length) {
			subscriptionItems[currentSelection].selected = true;
		}
		
		final int dir = subscriptionsList.getSortDirection() == SWT.DOWN ? 1 : -1;
		final boolean nameSort = subscriptionsList.getColumn(0) == subscriptionsList.getSortColumn();
		Arrays.sort(subscriptionItems,new Comparator() {
			public int compare(Object arg0, Object arg1) {
				SubscriptionItemModel item0 = (SubscriptionItemModel) arg0;
				SubscriptionItemModel item1 = (SubscriptionItemModel) arg1;
				if(nameSort) {
					return dir * item0.name.compareTo(item1.name);
				} else {
					return dir * (int) (item1.popularity - item0.popularity);
				}	
			}
		});
		subscriptionsList.setItemCount(subscriptionItems.length);
		subscriptionsList.clearAll();
		if(currentSelection >= 0 && currentSelection < subscriptionItems.length) {
			for(int i = 0 ; i < subscriptionItems.length ; i++) {
				if(subscriptionItems[i].selected) {
					subscriptionsList.setSelection(i);
				}
			}
		}
	}
	
	public void failed(byte[] hash,SubscriptionException error) {
		if(display != null && !display.isDisposed()) {
			display.asyncExec(new Runnable() {
				public void run() {
					animatedImage.stop();
					animatedImage.dispose();
					loadingProgress.dispose();
					loadingText.setText(MessageText.getString("subscriptions.listwindow.failed"));
				}
			});
		}
		
	}
	
	public static void main(String[] args) {
		Display display = new Display();
		SubscriptionListWindow slw = new SubscriptionListWindow(null,false);
		while(!slw.shell.isDisposed()) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

}
