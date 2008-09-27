package com.aelitis.azureus.ui.swt.subscriptions;

import java.awt.SystemColor;

import javax.swing.plaf.FontUIResource;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.ui.swt.Utils;

import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;
import com.aelitis.azureus.ui.swt.columns.torrent.ColumnMediaThumb.disposableUISWTGraphic;

public class SubscriptionWizard {
	
	private static final int MODE_OPT_IN = 1;
	private static final int MODE_SUBSCRIBE = 2;
	private static final int MODE_CREATE_SEARCH = 3;
	private static final int MODE_CREATE_RSS = 4;
	
	private final String TITLE_OPT_IN = MessageText.getString("Wizard.Subscription.optin.title");
	private final String TITLE_SUBSCRIBE = MessageText.getString("Wizard.Subscription.subscribe.title");
	private final String TITLE_CREATE = MessageText.getString("Wizard.Subscription.create.title");
	
	Display display;
	Shell shell;
	
	Label title;
	
	Button cancelButton;
	Button searchButton;
	Button saveButton;
	Button yesButton;
	Button addButton;
	Button availableButton;
	Button createButton;
	
	Font titleFont;
	Font subTitleFont;
	Font textInputFont;
	
	
	Composite main;
	StackLayout mainLayout;
	Composite optinComposite;
	Composite subscribeComposite;
	Composite createComposite;
	TabFolder createTabFolder;
	TabItem   createRSSTabItem;
	TabItem   createSearchTabItem;
	Composite availableSubscriptionComposite;
	
	
	public SubscriptionWizard() {
		UIFunctionsSWT functionsSWT = UIFunctionsManagerSWT.getUIFunctionsSWT();
		if(functionsSWT != null) {
			Shell mainShell = functionsSWT.getMainShell();
			shell = new Shell(mainShell,SWT.TITLE);
			shell.setSize(500,400);
			Utils.centerWindowRelativeTo(shell, mainShell);
		} else {
			shell = new Shell(SWT.TITLE | SWT.CLOSE | SWT.RESIZE);
			shell.setSize(500,400);
			Utils.centreWindow(shell);
		}
		
		display = shell.getDisplay();
		
		createFonts();
		
		shell.setText(MessageText.getString("Wizard.Subscription.title"));
		
		shell.addListener(SWT.Dispose, new Listener() {
			public void handleEvent(Event event) {
				
				if(titleFont != null && !titleFont.isDisposed()) {
					titleFont.dispose();
				}
				
				if(textInputFont != null && !textInputFont.isDisposed()) {
					textInputFont.dispose();
				}
				
				if(subTitleFont != null && !subTitleFont.isDisposed()) {
					subTitleFont.dispose();
				}
				
			}
		});
		
		Composite header = new Composite(shell, SWT.NONE);
		header.setBackgroundMode(SWT.INHERIT_DEFAULT);
		Label topSeparator = new Label(shell,SWT.SEPARATOR |SWT.HORIZONTAL);
		main = new Composite(shell, SWT.NONE);
		Label bottomSeparator = new Label(shell,SWT.SEPARATOR |SWT.HORIZONTAL);
		Composite footer = new Composite(shell, SWT.NONE);
		
		FormLayout layout = new FormLayout();
		shell.setLayout(layout);
		
		FormData data;
		
		data = new FormData();
		data.top = new FormAttachment(0,0);
		data.left = new FormAttachment(0,0);
		data.right = new FormAttachment(100,0);
		//data.height = 50;
		header.setLayoutData(data);
		
		data = new FormData();
		data.top = new FormAttachment(header,0);
		data.left = new FormAttachment(0,0);
		data.right = new FormAttachment(100,0);
		topSeparator.setLayoutData(data);
		
		data = new FormData();
		data.top = new FormAttachment(topSeparator,0);
		data.left = new FormAttachment(0,0);
		data.right = new FormAttachment(100,0);
		data.bottom = new FormAttachment(bottomSeparator,0);
		main.setLayoutData(data);
		
		data = new FormData();
		data.left = new FormAttachment(0,0);
		data.right = new FormAttachment(100,0);
		data.bottom = new FormAttachment(footer,0);
		bottomSeparator.setLayoutData(data);
		
		data = new FormData();
		data.bottom = new FormAttachment(100,0);
		data.left = new FormAttachment(0,0);
		data.right = new FormAttachment(100,0);
		//data.height = 100;
		footer.setLayoutData(data);
		
		populateHeader(header);
		populateFooter(footer);
		
		mainLayout = new StackLayout();
		main.setLayout(mainLayout);
		
		optinComposite = createOptInComposite(main);
		createComposite = createCreateComposite(main);
		availableSubscriptionComposite = createAvailableSubscriptionComposite(main);
		
		setMode(MODE_OPT_IN);
		
		shell.layout();
		shell.open();
	}
	
	private void populateHeader(Composite header) {
		header.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
		title = new Label(header, SWT.NONE);
		title.setText("This is a title");
		
		title.setFont(titleFont);
		
		FillLayout layout = new FillLayout();
		layout.marginHeight = 10;
		layout.marginWidth = 10;
		header.setLayout(layout);
		
	}
	
	private Composite createOptInComposite(Composite parent) {
		Composite composite = new Composite(parent,SWT.NONE);
		
		Label subTitle = new Label(composite,SWT.NONE);
		subTitle.setFont(subTitleFont);
		subTitle.setText(MessageText.getString("Wizard.Subscription.optin.subtitle"));
		
		Label description = new Label(composite,SWT.NONE);
		//subTitle.setFont(subTitleFont);
		description.setText(MessageText.getString("Wizard.Subscription.optin.description"));
		
		FormLayout layout = new FormLayout();
		layout.marginLeft = 50;
		layout.marginTop = 50;
		layout.spacing = 5;
		composite.setLayout(layout);
		
		FormData data;
		
		data = new FormData();
		data.top = new FormAttachment(0);
		data.left = new FormAttachment(0);
		subTitle.setLayoutData(data);
		
		data = new FormData();
		data.top = new FormAttachment(subTitle);
		data.left = new FormAttachment(subTitle,0,SWT.LEFT);
		description.setLayoutData(data);
		
		return composite;
	}
	
	private Composite createCreateComposite(Composite parent) {
		Composite composite = new Composite(parent,SWT.NONE);
		
		FillLayout layout = new FillLayout();
		layout.marginHeight = 8;
		layout.marginWidth  = 8;
		
		composite.setLayout(layout);
		
		createTabFolder = new TabFolder(composite,SWT.NONE);
		createTabFolder.setFont(subTitleFont);
		
		createSearchTabItem = new TabItem(createTabFolder,SWT.NONE);
		createSearchTabItem.setText(MessageText.getString("Wizard.Subscription.create.search"));
		createSearchTabItem.setControl(createCreateSearchComposite(createTabFolder));
		
		createRSSTabItem = new TabItem(createTabFolder,SWT.NONE);
		createRSSTabItem.setText(MessageText.getString("Wizard.Subscription.create.rss"));
		createRSSTabItem.setControl(createCreateRSSComposite(createTabFolder));
		
		createTabFolder.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				TabItem[] selectedItems = createTabFolder.getSelection();
				if(selectedItems.length != 1) {
					return;
				}
				TabItem selectedItem = selectedItems[0];
				if(selectedItem == createRSSTabItem) {
					setMode(MODE_CREATE_RSS);
				} else {
					setMode(MODE_CREATE_SEARCH);
				}
			}
		});
		
		return composite;
	}
	
	private Composite createCreateRSSComposite(Composite parent) {
		Composite composite = new Composite(parent,SWT.NONE);

		Text feedUrl = new Text(composite, SWT.SINGLE);
		feedUrl.setFont(textInputFont);
		feedUrl.setText(MessageText.getString("Wizard.Subscription.rss.inputPrompt"));

		Label subTitle = new Label(composite,SWT.NONE);
		subTitle.setFont(subTitleFont);
		subTitle.setText(MessageText.getString("Wizard.Subscription.rss.subtitle"));
		
		Label description = new Label(composite, SWT.WRAP);
		//subTitle.setFont(subTitleFont);
		description.setText(MessageText.getString("Wizard.Subscription.rss.description"));
		
		FormLayout layout = new FormLayout();
		layout.marginLeft = 50;
		layout.marginRight = 50;
		layout.marginTop = 50;
		layout.spacing = 5;
		composite.setLayout(layout);
		
		FormData data;

		data = new FormData();
		data.top = new FormAttachment(0);
		data.left = new FormAttachment(0, 40);
		data.right = new FormAttachment(100, -40);
		data.height = 30;
		feedUrl.setLayoutData(data);

		data = new FormData();
		data.top = new FormAttachment(feedUrl, 20);
		data.left = new FormAttachment(0);
		subTitle.setLayoutData(data);
		
		data = new FormData();
		data.top = new FormAttachment(subTitle);
		data.left = new FormAttachment(subTitle,0,SWT.LEFT);
		data.right = new FormAttachment(100, 0);
		description.setLayoutData(data);

		return composite;
	}
	
	private Composite createCreateSearchComposite(Composite parent) {
		Composite composite = new Composite(parent,SWT.NONE);
		
		Text searchInput = new Text(composite, SWT.SINGLE);
		searchInput.setFont(textInputFont);
		searchInput.setText(MessageText.getString("Wizard.Subscription.search.inputPrompt"));
		
		Label subTitle = new Label(composite,SWT.NONE);
		subTitle.setFont(subTitleFont);
		subTitle.setText(MessageText.getString("Wizard.Subscription.search.subtitle"));
		
		Label description = new Label(composite,SWT.NONE);
		//subTitle.setFont(subTitleFont);
		description.setText(MessageText.getString("Wizard.Subscription.search.description"));
		
		FormLayout layout = new FormLayout();
		layout.marginLeft = 50;
		layout.marginRight = 50;
		layout.marginTop = 50;
		layout.spacing = 5;
		composite.setLayout(layout);
		
		FormData data;

		data = new FormData();
		data.top = new FormAttachment(0);
		data.left = new FormAttachment(0, 40);
		data.right = new FormAttachment(100, -40);
		data.height = 30;
		searchInput.setLayoutData(data);

		data = new FormData();
		data.top = new FormAttachment(searchInput, 20);
		data.left = new FormAttachment(0);
		subTitle.setLayoutData(data);
		
		data = new FormData();
		data.top = new FormAttachment(subTitle);
		data.left = new FormAttachment(subTitle,0,SWT.LEFT);
		description.setLayoutData(data);
		return composite;
	}
	
	private Composite createAvailableSubscriptionComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		
		return composite;
	}
	
	private void createFonts() {
		
		FontData[] fDatas = shell.getFont().getFontData();
		
		for(int i = 0 ; i < fDatas.length ; i++) {
			fDatas[i].setStyle(SWT.BOLD);
		}
		subTitleFont = new Font(display,fDatas);
		
		
		for(int i = 0 ; i < fDatas.length ; i++) {
			fDatas[i].setHeight(14);
		}
		titleFont = new Font(display,fDatas);
		
		
		for(int i = 0 ; i < fDatas.length ; i++) {
			fDatas[i].setHeight(12);
			fDatas[i].setStyle(SWT.NONE);
		}
		textInputFont = new Font(display,fDatas);
		
		
	}
	
	private void populateFooter(Composite footer) {
		yesButton = new Button(footer, SWT.PUSH);
		yesButton.setText(MessageText.getString("Button.yes"));

		addButton = new Button(footer, SWT.PUSH);
		addButton.setText(MessageText.getString("Button.add"));
		
		saveButton = new Button(footer, SWT.PUSH);
		saveButton.setText(MessageText.getString("Button.save"));
		
		searchButton = new Button(footer, SWT.PUSH);
		searchButton.setText(MessageText.getString("Button.search"));
		
		cancelButton = new Button(footer,SWT.PUSH);
		//cancelButton.setText(MessageText.getString("Button.cancel"));
			
		createButton = new Button(footer,SWT.PUSH);
		createButton.setText(MessageText.getString("Button.createNewSubscription"));
		
		availableButton = new Button(footer,SWT.PUSH);
		availableButton.setText(MessageText.getString("Button.availableSubscriptions"));
		
		FormLayout layout = new FormLayout();
		layout.marginHeight = 5;
		layout.marginWidth = 5;
		layout.spacing = 5;
		
		footer.setLayout(layout);
		FormData data;
		
		data = new FormData();
		data.right = new FormAttachment(100);
		data.width = 100;
		
		yesButton.setLayoutData(data);
		addButton.setLayoutData(data);
		searchButton.setLayoutData(data);
		
		data = new FormData();
		data.right = new FormAttachment(100);
		data.width = 100;
		saveButton.setLayoutData(data);
		
		data = new FormData();
		data.right = new FormAttachment(saveButton);
		data.width = 100;
		cancelButton.setLayoutData(data);
		
		data = new FormData();
		data.left = new FormAttachment(0);
		data.width = 150;
		createButton.setLayoutData(data);
		availableButton.setLayoutData(data);
		
		
		yesButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				COConfigurationManager.setParameter("subscriptions.opted_in",true);
				setMode(MODE_SUBSCRIBE);
			}
		});
		
		createButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				setMode(MODE_CREATE_SEARCH);
			}
		});
		
		availableButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				boolean opted_in = COConfigurationManager.getBooleanParameter("subscriptions.opted_in");
				if(!opted_in) {
					setMode(MODE_OPT_IN);
				} else {
					setMode(MODE_SUBSCRIBE);
				}
			}
		});
		
		cancelButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event arg0) {
				shell.close();
			}
		});
	}
	
	private void setMode(int mode) {
		addButton.setVisible(false);
		searchButton.setVisible(false);
		saveButton.setVisible(false);
		yesButton.setVisible(false);
		createButton.setVisible(false);
		availableButton.setVisible(false);
		cancelButton.setText(MessageText.getString("Button.cancel"));
		
		String titleText = TITLE_OPT_IN;
		
		switch (mode) {
		case MODE_SUBSCRIBE :
			titleText = TITLE_SUBSCRIBE;
			createButton.setVisible(true);
			addButton.setVisible(true);
			break;
			
		case MODE_CREATE_RSS :
			mainLayout.topControl = createComposite;
			createTabFolder.setSelection(createRSSTabItem);
			titleText = TITLE_CREATE;
			availableButton.setVisible(true);
			saveButton.setVisible(true);
			break;
			
		case MODE_CREATE_SEARCH :
			mainLayout.topControl = createComposite;
			createTabFolder.setSelection(createSearchTabItem);
			titleText = TITLE_CREATE;
			availableButton.setVisible(true);
			searchButton.setVisible(true);
			break;
			
		case MODE_OPT_IN:
		default:
			mainLayout.topControl = optinComposite;
			cancelButton.setText(MessageText.getString("Button.no"));
			createButton.setVisible(true);
			yesButton.setVisible(true);
			break;
		}
			
		main.layout(true,true);
		
		title.setText(titleText);
	}
	
	public static void main(String args[]) {
		final SubscriptionWizard sw = new SubscriptionWizard();
		
//		Thread t = new Thread() {
//			public void run() {
//				final int[] i = new int[1];
//				i[0] = 0;
//				while(true) {
//					try {
//						i[0]++;
//						sw.display.asyncExec(new Runnable() {
//							
//							public void run() {
//								if(! sw.title.isDisposed()) {
//									sw.title.setText("test " + i[0]);
//								}
//							}
//						});
//						
//					} catch (Exception e) {
//						e.printStackTrace();
//					} finally {
//						try {
//							Thread.sleep(500);
//						} catch (Exception e) {
//							//e.printStackTrace();
//						}
//					}
//				}
//			}
//		};
//		
//		t.setDaemon(true);
//		t.start();
		
		while( ! sw.shell.isDisposed()) {
			if(! sw.display.readAndDispatch()) {
				sw.display.sleep();
			}
		}
		
		sw.display.dispose();
		
	}

}
