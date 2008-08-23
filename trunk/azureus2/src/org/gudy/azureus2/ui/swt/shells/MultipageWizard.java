package org.gudy.azureus2.ui.swt.shells;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;

abstract public class MultipageWizard
{

	public static final String BUTTON_OK = "button.ok";

	public static final String BUTTON_CANCEL = "button.cancel";

	public static final String BUTTON_NEXT = "button.next";

	public static final String BUTTON_BACK = "button.back";

	private Shell shell;

	private Display display;

	private int shellStyle;

	private Shell parent;

	private Composite topPanel;

	private Composite contentPanel;

	private Composite toolbarPanel;

	private Label titleLabel;

	private Label descriptionLabel;

	/**
	 * A map of pageID(String)/<code>IWizardPage</code>; using LinkedHashMap since the order the pages are inserted is important
	 */
	private Map pages = new LinkedHashMap();

	private StackLayout contentStackLayout;

	private IWizardPage currentPage;

	private IWizardPage previousPage;

	/**
	 * A map of buttonID(String)/<code>Button</code>; using LinkedHashMap since the order the buttons are added is important
	 */
	private Map buttons = new LinkedHashMap();

	private SelectionListener defaultButtonListener;

	/**
	 * A little extra margin so the buttons are a little wider; typically the buttons
	 * are just slightly wider than the text but a even wider button looks nicer
	 */
	private int buttonExtraMargin = 50;

	public MultipageWizard(Display display, int shellStyle) {
		this.display = display;
		this.shellStyle = shellStyle;
		init();
	}

	public MultipageWizard(Shell parent, int shellStyle) {
		this.parent = parent;
		this.shellStyle = shellStyle;
		init();
	}

	public abstract void createPages();

	private void init() {

		if (null != parent) {
			shell = ShellFactory.createShell(parent, shellStyle);
		} else {
			shell = ShellFactory.createShell(display, shellStyle);
		}

		createControls();
		createPages();

		/*
		 * This invisible label is used to ensure the buttons are flushed-right
		 */
		Label dummy = new Label(toolbarPanel, SWT.NONE);
		dummy.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		createDefaultButtons(toolbarPanel);
	}

	private void createControls() {
		GridLayout gLayout = new GridLayout();
		gLayout.marginHeight = 0;
		gLayout.marginWidth = 0;
		gLayout.verticalSpacing = 0;
		shell.setLayout(gLayout);
		Utils.setShellIcon(shell);

		topPanel = new Composite(shell, SWT.NONE);
		topPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		GridLayout gLayout1 = new GridLayout();
		gLayout1.marginBottom = 10;
		topPanel.setLayout(gLayout1);
		topPanel.setBackground(shell.getDisplay().getSystemColor(
				SWT.COLOR_LIST_BACKGROUND));
		topPanel.setBackgroundMode(SWT.INHERIT_FORCE);

		Label separator1 = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		contentPanel = new Composite(shell, SWT.NONE);
		contentPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		contentStackLayout = new StackLayout();
		contentPanel.setLayout(contentStackLayout);

		Label separator2 = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		toolbarPanel = new Composite(shell, SWT.NONE);
		toolbarPanel.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));
		GridLayout gLayout2 = new GridLayout(3, false);
		gLayout2.marginHeight = 16;
		gLayout2.marginWidth = 16;
		toolbarPanel.setLayout(gLayout2);

		titleLabel = new Label(topPanel, SWT.NONE);
		titleLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		Utils.setFontHeight(titleLabel, 16, SWT.NORMAL);

		descriptionLabel = new Label(topPanel, SWT.WRAP);
		GridData gData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gData.horizontalIndent = 10;
		descriptionLabel.setLayoutData(gData);

		shell.layout(true, true);

	}

	public boolean addPage(IWizardPage page) {
		if (null == page) {
			return false;
		}

		if (true == pages.containsKey(page.getPageID())) {
			Debug.out("MultipageWizard:: a page with this ID already exists ID:"
					+ page.getPageID());
			return false;
		}

		pages.put(page.getPageID(), page);
		return true;
	}

	public boolean removePage(IWizardPage page) {
		if (null == page) {
			return false;
		}

		if (false == pages.containsKey(page.getPageID())) {
			Debug.out("MultipageWizard:: a page with this ID is not found ID:"
					+ page.getPageID());
			return false;
		}
		pages.remove(page.getPageID());
		page.performDispose();
		return true;
	}

	public void showPage(String pageID) {
		if (false == pages.containsKey(pageID)) {
			Debug.out("MultipageWizard:: a page with this ID is not found ID:"
					+ pageID);
			return;
		}

		IWizardPage page = (IWizardPage) pages.get(pageID);
		if (null != currentPage) {
			currentPage.performAboutToBeHidden();
		}
		page.performAboutToBeShown();

		previousPage = currentPage;
		currentPage = page;
		contentStackLayout.topControl = page.getControl();

		update();

		contentPanel.layout(true);
	}

	public void open() {
		/*
		 * Show the first page
		 */
		if (false == pages.isEmpty()) {
			IWizardPage page = (IWizardPage) pages.values().iterator().next();
			page.createControls(contentPanel);
			setCurrent(page);
		}

		shell.open();
	}

	private void setCurrent(IWizardPage page) {
		if (null != page) {
			currentPage = page;
			contentStackLayout.topControl = page.getControl();
			contentPanel.layout(true);
			update();
		}
	}

	private void update() {
		if (null != currentPage) {
			setText(currentPage.getWindowTitle());
			setTitle(currentPage.getTitle());
			setDescription(currentPage.getDesciption());
		}
	}

	public void setTitle(String title) {
		titleLabel.setText(title + "");
	}

	public void setDescription(String description) {
		descriptionLabel.setText(description + "");
	}

	private void createDefaultButtons(Composite buttonPanel) {
		createButton(BUTTON_CANCEL, null);
		if (pages.size() > 1) {
			createButton(BUTTON_BACK, null);
			createButton(BUTTON_NEXT, null);
		}
		createButton(BUTTON_OK, null);

		/*
		 * Invoke custom button creation
		 */
		for (Iterator iterator = pages.values().iterator(); iterator.hasNext();) {
			IWizardPage page = (IWizardPage) iterator.next();
			page.createCustomButtons();
		}

	}

	/**
	 * Show or hide the button with the given id
	 * @param buttonID
	 * @param value
	 */
	public void showButton(String buttonID, boolean value) {
		if (false == buttons.containsKey(buttonID)) {
			Debug.out("MultipageWizard:: a button with this ID is not found ID:"
					+ buttonID);
			return;
		}

		Button button = (Button) buttons.get(buttonID);

		if (true == value) {
			GridData gData = ((GridData) button.getLayoutData());
			gData.exclude = false;
			gData.widthHint = button.computeSize(SWT.DEFAULT, SWT.DEFAULT).y
					+ buttonExtraMargin;
		} else {
			GridData gData = ((GridData) button.getLayoutData());
			gData.exclude = true;
			gData.widthHint = 0;
		}

		toolbarPanel.layout(true);
	}

	public Button createButton(String buttonID, SelectionListener listener) {
		if (null == buttonID) {
			throw new IllegalArgumentException("A button requires a non-null ID");
		}

		if (true == buttons.containsKey(buttonID)) {
			Debug.out("MultipageWizard:: a button with this same ID already exists ID:"
					+ buttonID);
			return (Button) buttons.get(buttonID);
		}

		if (null == defaultButtonListener) {
			defaultButtonListener = new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					if (true == BUTTON_OK.equals(e.widget.getData("button.id"))) {
						performOK();
					} else if (true == BUTTON_CANCEL.equals(e.widget.getData("button.id"))) {
						performCancel();
					} else if (true == BUTTON_NEXT.equals(e.widget.getData("button.id"))) {
						performNext();
					} else if (true == BUTTON_BACK.equals(e.widget.getData("button.id"))) {
						performBack();
					}
				}

				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}
			};
		}

		Button button = new Button(toolbarPanel, SWT.PUSH);
		GridData gData = new GridData(SWT.END, SWT.BOTTOM, false, false);
		gData.widthHint = button.computeSize(SWT.DEFAULT, SWT.DEFAULT).y
				+ buttonExtraMargin;
		button.setLayoutData(gData);

		/*
		 * Add listener if given; for default buttons this is used in place of the default listener 
		 */
		if (null != listener) {
			button.addSelectionListener(listener);

		} else {

			/*
			 * Only add default listener to default buttons
			 */
			if (true == BUTTON_CANCEL.equals(buttonID)) {
				button.addSelectionListener(defaultButtonListener);
			} else if (true == BUTTON_OK.equals(buttonID)) {
				button.addSelectionListener(defaultButtonListener);
			} else if (true == BUTTON_NEXT.equals(buttonID)) {
				button.addSelectionListener(defaultButtonListener);
			} else if (true == BUTTON_BACK.equals(buttonID)) {
				button.addSelectionListener(defaultButtonListener);
			}
		}

		/*
		 * Set text to the button
		 */
		if (true == BUTTON_CANCEL.equals(buttonID)) {
			Messages.setLanguageText(button, "Button.cancel");
		} else if (true == BUTTON_OK.equals(buttonID)) {
			Messages.setLanguageText(button, "wizard.finish");
		} else if (true == BUTTON_NEXT.equals(buttonID)) {
			Messages.setLanguageText(button, "wizard.next");
		} else if (true == BUTTON_BACK.equals(buttonID)) {
			Messages.setLanguageText(button, "wizard.previous");
		} else {

			/*
			 * Custom button gets a default text; implementor can change as appropriate when
			 * this button is returned from this method
			 */
			button.setText("button" + (buttons.size() + 1));

		}

		button.setData("button.id", buttonID);

		buttons.put(buttonID, button);

		adjustToolbar();

		return button;
	}

	protected void performOK() {
		System.out.println("OK is pressed");//KN: sysout
	}

	protected void performCancel() {
		System.out.println("Cancel is pressed");//KN: sysout

	}

	protected void performNext() {
		if (true == pages.isEmpty()) {
			return;
		}

		if (null == currentPage) {
			IWizardPage page = (IWizardPage) pages.values().iterator().next();
			showPage(page.getPageID());
		} else {
			boolean foundCurrent = false;
			for (Iterator iterator = pages.values().iterator(); iterator.hasNext();) {
				IWizardPage page = (IWizardPage) iterator.next();
				if (true == foundCurrent) {
					showPage(page.getPageID());
					return;
				}

				if (page.getPageID().equals(currentPage.getPageID())) {
					foundCurrent = true;
				}
			}

			if (false == foundCurrent) {
				Debug.out("MultipageWizard:: there is no more page to go to");
			}

		}
		System.out.println("Next is pressed");//KN: sysout

	}

	protected void performBack() {

		if (null != previousPage) {
			showPage(previousPage.getPageID());
		}

		System.out.println("Back is pressed");//KN: sysout

	}

	/**
	 * Adjusting the number of columns to correspond with the number of buttons
	 */
	private void adjustToolbar() {
		/*
		 * NOTE: we're adding 1 to the number of columns because there is always an invisible
		 * label on the far left used for spacing so the buttons are right-aligned properly
		 */
		((GridLayout) toolbarPanel.getLayout()).numColumns = buttons.size() + 1;
		toolbarPanel.layout(true);
	}

	/* ===========================================
		 * Below are just some convenience delegations
		 * =========================================== */
	public Shell getShell() {
		return shell;
	}

	public void close() {
		shell.close();
	}

	public Object getData(String key) {
		return shell.getData(key);
	}

	public Point getLocation() {
		return shell.getLocation();
	}

	public Point getSize() {
		return shell.getSize();
	}

	public String getText() {
		return shell.getText();
	}

	public String getToolTipText() {
		return shell.getToolTipText();
	}

	public void setBounds(int x, int y, int width, int height) {
		shell.setBounds(x, y, width, height);
	}

	public void setData(String key, Object value) {
		shell.setData(key, value);
	}

	public void setLocation(int x, int y) {
		shell.setLocation(x, y);
	}

	public void setSize(int width, int height) {
		shell.setSize(width, height);
	}

	public void setText(String string) {
		shell.setText(string);
	}

	public void setToolTipText(String string) {
		shell.setToolTipText(string);
	}

	public void setVisible(boolean visible) {
		shell.setVisible(visible);
	}

	public Image getImage() {
		return shell.getImage();
	}

	public void setImage(Image image) {
		shell.setImage(image);
	}

}
