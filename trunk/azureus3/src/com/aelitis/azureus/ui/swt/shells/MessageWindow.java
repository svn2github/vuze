package com.aelitis.azureus.ui.swt.shells;

import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.ui.swt.ITwistieListener;
import org.gudy.azureus2.ui.swt.TwistieLabel;
import org.gudy.azureus2.ui.swt.TwistieSection;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.components.shell.ShellFactory;
import org.gudy.azureus2.ui.swt.components.shell.StyledShell;
import org.gudy.azureus2.ui.swt.components.widgets.BubbleButton;
import org.gudy.azureus2.ui.swt.mainwindow.Colors;
import org.gudy.azureus2.ui.swt.progress.ProgressReportMessage;

import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.utils.ColorCache;

public class MessageWindow
{
	private Label messageLabel = null;

	private String message = null;

	/**
	 * A List of <code>IMessage</code>
	 */
	private List detailMessages = null;

	private Button closeButton = null;

	private StyledText detailListWidget;

	private TwistieSection detailTwistie;

	private Label titleLabel = null;

	private String title = null;

	private Color errorColor;

	private Shell shell;
	private Composite content;
	
	public MessageWindow(Shell parentShell, int borderWidth) {
		shell = ShellFactory.createShell(parentShell, SWT.DIALOG_TRIM);
		FillLayout fillLayout = new FillLayout();
		fillLayout.marginHeight = 0;
		fillLayout.marginWidth = 0;
		shell.setLayout(fillLayout);

		if (true == Constants.isOSX) {
			UIFunctionsManagerSWT.getUIFunctionsSWT().createMainMenu(shell);
		}
		Utils.setShellIcon(shell);

		content = new Composite(shell, SWT.DOUBLE_BUFFERED);
		content.setBackgroundMode(SWT.INHERIT_DEFAULT);

		createControls(content);
	}


	private void createControls(Composite parent) {
		errorColor = Colors.colorError;

		//setBackground(ColorCache.getColor(parent.getDisplay(), 38, 38, 38));
		//parent.setBackground(ColorCache.getColor(parent.getDisplay(), 13, 13, 13));
		parent.setLayout(new FormLayout());
		parent.setBackgroundMode(SWT.INHERIT_FORCE);

		titleLabel = new Label(parent, SWT.NONE);
		Utils.setFontHeight(titleLabel, 12, SWT.NORMAL);
		//titleLabel.setForeground(ColorCache.getColor(parent.getDisplay(), 206, 206,
		//		206));

		FormData titleLabelData = new FormData();
		titleLabelData.top = new FormAttachment(0, 6);
		titleLabelData.left = new FormAttachment(0, 6);
		titleLabel.setLayoutData(titleLabelData);

		messageLabel = new Label(parent, SWT.WRAP);
		//messageLabel.setForeground(ColorCache.getColor(parent.getDisplay(), 206,
		//		206, 206));

		FormData messageLabelData = new FormData();
		messageLabelData.top = new FormAttachment(titleLabel, 20);
		messageLabelData.left = new FormAttachment(0, 20);
		messageLabelData.width = 300;
		messageLabel.setLayoutData(messageLabelData);

		closeButton = new Button(parent,SWT.PUSH);
		FormData closeButtonData = new FormData();
		closeButtonData.right = new FormAttachment(100, -20);
		closeButtonData.bottom = new FormAttachment(100, -20);
		closeButtonData.width = 80;
		closeButton.setLayoutData(closeButtonData);
		closeButton.setText("Close");
		closeButton.addMouseListener(new MouseAdapter() {
			public void mouseUp(MouseEvent e) {
				close();
			}
		});

		detailTwistie = new TwistieSection(parent, TwistieLabel.SHOW_SEPARATOR);

		detailTwistie.setVisible(false);

		FormData detailTwistieData = new FormData();
		detailTwistieData.left = new FormAttachment(0, 20);
		detailTwistieData.right = new FormAttachment(100, -20);
		detailTwistieData.top = new FormAttachment(messageLabel, 20);
		detailTwistieData.bottom = new FormAttachment(closeButton, -20);
		detailTwistie.setLayoutData(detailTwistieData);

		detailTwistie.setTitle("Detail:");

		Composite sectionContent = detailTwistie.getContent();
		//detailTwistie.setForeground(ColorCache.getColor(parent.getDisplay(), 206,
		//		206, 206));
		//		detailTwistie.setBackground(ColorCache.getColor(parent.getDisplay(),
		//				13, 13, 13));

		sectionContent.setLayout(new FormLayout());
		detailListWidget = new StyledText(sectionContent, SWT.V_SCROLL | SWT.WRAP);
		detailListWidget.setEditable(false);

		FormData detailListWidgetData = new FormData();
		detailListWidgetData.left = new FormAttachment(0, 0);
		detailListWidgetData.right = new FormAttachment(100, 0);
		detailListWidgetData.top = new FormAttachment(0, 0);
		detailListWidgetData.bottom = new FormAttachment(100, 0);
		detailListWidgetData.height = 100;
		detailListWidgetData.width = 300;
		detailListWidget.setLayoutData(detailListWidgetData);

		//detailListWidget.setBackground(ColorCache.getColor(parent.getDisplay(),
		//		255, 255, 230));

		detailTwistie.addTwistieListener(new ITwistieListener() {

			public void isCollapsed(boolean value) {
				if (null != shell && false == shell.isDisposed()) {
					shell.pack(true);
				}
			}
		});

	}

	public void open() {
		messageLabel.setText(message != null ? message : "");
		messageLabel.setSize(messageLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		if (null != title) {
			titleLabel.setText(title);
		}
		if (null != detailMessages && false == detailMessages.isEmpty()) {
			boolean hasError = false;
			for (Iterator iterator = detailMessages.iterator(); iterator.hasNext();) {
				Object obj = (Object) iterator.next();
				if (obj instanceof ProgressReportMessage) {
					ProgressReportMessage message = (ProgressReportMessage) obj;

					if (false == hasError && true == message.isError()) {
						hasError = true;
					}
					appendToDetail(message.getValue(), message.isError());
				}

			}

			detailTwistie.setVisible(hasError);
		} else {
			detailListWidget.setText("");
			detailTwistie.setVisible(false);
		}
		shell.pack(true);
		shell.open();
	}

	public boolean isAlive() {
		if (null == shell || true == shell.isDisposed()) {
			return false;
		}
		return true;
	}
	
	public void setSize(int width, int height) {
		shell.setSize(width,height);
	}
	
	public void close() {
		if (true == isAlive()) {
			shell.close();
		}
	}
	
	
	
	/**
	 * Appends the given message to the detail panel; render the message in error color if specified
	 * @param value
	 * @param isError if <code>true</code> then render the message in the system error color; otherwise render in default color
	 */
	private void appendToDetail(String value, boolean isError) {

		if (null == value || value.length() < 1) {
			return;
		}

		if (null == detailListWidget || detailListWidget.isDisposed()) {
			return;
		}

		int charCount = detailListWidget.getCharCount();
		detailListWidget.append(value + "\n");
		if (true == isError) {
			StyleRange style2 = new StyleRange();
			style2.start = charCount;
			style2.length = value.length();
			style2.foreground = errorColor;
			detailListWidget.setStyleRange(style2);
		}

	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String successMessage) {
		this.message = successMessage;
	}

	public List getDetailMessages() {
		return detailMessages;
	}

	public void setDetailMessages(List detailMessages) {
		this.detailMessages = detailMessages;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	public Shell getShell() {
		return shell;
	}

}
