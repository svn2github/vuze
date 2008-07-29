package com.aelitis.azureus.ui.swt.subscriptions;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AEThread2;
import org.gudy.azureus2.ui.swt.ImageRepository;

import com.aelitis.azureus.core.subs.Subscription;
import com.aelitis.azureus.core.subs.SubscriptionAssociationLookup;
import com.aelitis.azureus.core.subs.SubscriptionException;
import com.aelitis.azureus.core.subs.SubscriptionLookupListener;
import com.aelitis.azureus.core.subs.SubscriptionManagerFactory;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.widgets.AnimatedImage;

public class SubscriptionListWindow implements SubscriptionLookupListener {
	
	private DownloadManager download;
	
	private Display display;
	private Shell shell;
	
	AnimatedImage animatedImage;
	
	Button action;
	Label loadingText;
	
	SubscriptionAssociationLookup lookup = null;
	
	Composite mainComposite;
	Composite loadingPanel;
	Composite listPanel;
	Table subscriptionsList;
	StackLayout mainLayout;
	
	public SubscriptionListWindow(DownloadManager download) {
		this.download = download;
		UIFunctionsSWT functionsSWT = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if(functionsSWT != null) {
			shell = new Shell(functionsSWT.getMainShell(),SWT.APPLICATION_MODAL);
		} else {
			shell = new Shell(SWT.TITLE | SWT.APPLICATION_MODAL);
		}
		
		display = shell.getDisplay();
		shell.setText(MessageText.getString("subscriptions.listwindow.title"));
		
		shell.setLayout(new FormLayout());
		
		mainComposite = new Composite(shell,SWT.NONE);
		Label separator = new Label(shell,SWT.SEPARATOR | SWT.HORIZONTAL);
		Button cancel = new Button(shell,SWT.PUSH);
		action = new Button(shell,SWT.PUSH);
		action.setText(MessageText.getString("Button.yes"));
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
		
		subscriptionsList = new Table(listPanel,SWT.V_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION);
		subscriptionsList.setHeaderVisible(true);
		
		TableColumn name = new TableColumn(subscriptionsList,SWT.NONE);
		name.setText(MessageText.getString("subscriptions.listwindow.name"));
		name.setWidth(310);
		name.setResizable(false);
		
		TableColumn popularity = new TableColumn(subscriptionsList,SWT.NONE);
		popularity.setText(MessageText.getString("subscriptions.listwindow.popularity"));
		popularity.setWidth(70);
		popularity.setResizable(false);
		
		subscriptionsList.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				action.setEnabled(subscriptionsList.getSelectionIndex() != -1);
			}
		});
		
		animatedImage = new AnimatedImage(loadingPanel);
		loadingText = new Label(loadingPanel,SWT.WRAP | SWT.CENTER);
		
		animatedImage.setImageFromName("spinner_big");
		String contentName = "Dummy";
		if(download != null) {
			contentName = download.getDisplayName();
		}
		loadingText.setText(MessageText.getString("subscriptions.listwindow.loadingtext", new String[] {contentName}));
		
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
		data.bottom = new FormAttachment(100,-5);
		loadingText.setLayoutData(data);
		
		boolean autoCheck = COConfigurationManager.getBooleanParameter("subscriptions.autocheck");
		
		if(autoCheck) {
			startChecking();
		} else {
			
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
					COConfigurationManager.setParameter("subscriptions.autocheck",true);
					startChecking();
					mainComposite.layout();
					action.setText(MessageText.getString("subscriptions.listwindow.subscribe"));
					action.setEnabled(false);
					action.removeListener(SWT.Selection,this);
				}
			});
			mainLayout.topControl = acceptPanel;
		}
		
		
		shell.setSize(400,300);
		shell.open();
			
	}

	private void startChecking() {
		try {
			if(download != null) {
				lookup = SubscriptionManagerFactory.getSingleton().lookupAssociations(download.getTorrent().getHash(), this);
			} else {
				//Dummy
				AEThread2 resultInjector = new AEThread2("test",true) {
					public void run() {
						try {
							Thread.sleep(1000);
							Subscription[] subscriptions = new Subscription[5];
							for(int i = 0 ; i < subscriptions.length ; i++) {
								final int index = i;
								subscriptions[i] = new Subscription() {
									public void addAssociation(byte[] hash) {
										// TODO Auto-generated method stub
										
									}
									public String getName() {
										return "Dummy subscription" + index;
									}
									public long getPopularity() {
										return 325 + index * 50;
									}
									public byte[] getPublicKey() {
										// TODO Auto-generated method stub
										return null;
									}
									public String getString() {
										// TODO Auto-generated method stub
										return null;
									}
									public int getVersion() {
										// TODO Auto-generated method stub
										return 0;
									}
									public boolean isMine() {
										// TODO Auto-generated method stub
										return false;
									}
									public boolean isPublic() {
										// TODO Auto-generated method stub
										return false;
									}
									public boolean isSubscribed() {
										// TODO Auto-generated method stub
										return false;
									}
									public void setSubscribed(boolean subscribed) {
										System.out.println(getName() + " is now subscribed : " + subscribed);
									}
								};
							}
							complete(null,subscriptions);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				};
				resultInjector.start();
			}
		} catch(Exception e) {
			failed(null,null);
		}
		animatedImage.start();
		mainLayout.topControl = loadingPanel;
	}
	
	private void populateSubscription(final Subscription subscription) {
		TableItem item = new TableItem(subscriptionsList,SWT.NONE);
		item.setData("subscription",subscription);
		item.setText(0,subscription.getName());
		try {
			item.setText(1,subscription.getPopularity() + "");
		} catch(SubscriptionException e) {
			item.setText(1,MessageText.getString("subscriptions.listwindow.popularity.unknown"));
		}
		subscriptionsList.setSelection(0);
		action.setEnabled(true);
	}
	
	public void found(byte[] hash, Subscription subscription) {
		// TODO Auto-generated method stub
		
	}
	
	public void complete(final byte[] hash,final Subscription[] subscriptions) {
		if( ! (subscriptions.length > 0) ) {
			failed(hash, null);
		}
		if(display != null && !display.isDisposed()) {
			display.asyncExec(new Runnable() {
				public void run() {
					animatedImage.stop();
					
					if(subscriptionsList != null && !subscriptionsList.isDisposed()) {
						for(int i = 0 ; i < subscriptions.length ; i++) {
							populateSubscription(subscriptions[i]);
						}
					}
					
					
					
					
					mainLayout.topControl = listPanel;
					mainComposite.layout();
					action.addListener(SWT.Selection, new Listener() {
						public void handleEvent(Event arg0) {
							if(subscriptionsList != null && !subscriptionsList.isDisposed()) {
								int selectedIndex = subscriptionsList.getSelectionIndex();
								if(selectedIndex != -1) {
									TableItem selectedItem = subscriptionsList.getItem(selectedIndex);
									Subscription subscription = (Subscription) selectedItem.getData("subscription");
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
	
	public void failed(byte[] hash,SubscriptionException error) {
		if(display != null && !display.isDisposed()) {
			display.asyncExec(new Runnable() {
				public void run() {
					animatedImage.stop();
					animatedImage.dispose();
					loadingText.setText(MessageText.getString("subscriptions.listwindow.failed"));
				}
			});
		}
		
	}
	
	public static void main(String[] args) {
		Display display = new Display();
		ImageRepository.loadImages(display);
		SubscriptionListWindow slw = new SubscriptionListWindow(null);
		while(!slw.shell.isDisposed()) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

}
