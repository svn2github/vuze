/*
 * Created on 6 sept. 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Text;
import org.gudy.azureus2.core.MessageText;
import org.gudy.azureus2.irc.IrcClient;
import org.gudy.azureus2.irc.MessageListener;

/**
 * @author Olivier
 * 
 */
public class IrcView extends AbstractIView implements MessageListener {

  Display display;
  StyledText consoleText;
  Text inputField;
  Color[] colors;

  IrcClient client;

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#initialize(org.eclipse.swt.widgets.Composite)
   */
  public void initialize(Composite composite) {
    display = composite.getDisplay();
    GridLayout layout = new GridLayout();
    composite.setLayout(layout);
    layout.numColumns = 1;
    GridData gridData = new GridData(GridData.FILL_BOTH);
    consoleText = new StyledText(composite, SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL);
    consoleText.setLayoutData(gridData);
    inputField = new Text(composite, SWT.BORDER);
    gridData = new GridData(GridData.FILL_HORIZONTAL);
    inputField.setLayoutData(gridData);
    inputField.addKeyListener(new KeyAdapter() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.events.KeyListener#keyReleased(org.eclipse.swt.events.KeyEvent)
       */
      public void keyReleased(KeyEvent event) {
        if (event.keyCode == 13) {
          String text = inputField.getText();
          inputField.setText("");
          client.sendMessage(text);
        }
      }
    });
    colors = new Color[4];
    colors[0] = MainWindow.blues[3];
    colors[1] = MainWindow.blues[2];
    colors[2] = MainWindow.blues[1];
    colors[3] = MainWindow.red_ConsoleView;
    client = new IrcClient(this);

  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getComposite()
   */
  public Composite getComposite() {
    return consoleText;
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#refresh()
   */
  public void refresh() {}

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#delete()
   */
  public void delete() {
    MainWindow.getWindow().setIrc(null);
    client.close();
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getShortTitle()
   */
  public String getShortTitle() {
    return MessageText.getString("IrcView.title.short"); //$NON-NLS-1$
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getFullTitle()
   */
  public String getFullTitle() {
    return MessageText.getString("IrcView.title.full"); //$NON-NLS-1$
  }

  public void messageReceived(String sender, String message) {
    doLog(2, sender + " > " + message);
  }

  public void systemMessage(String message) {
    doLog(0, message);
  }

  private void doLog(final int _color, final String _text) {
    display.asyncExec(new Runnable() {
      public void run() {
        if (consoleText == null || consoleText.isDisposed())
          return;
        ScrollBar sb = consoleText.getVerticalBar();

        //System.out.println(sb.getSelection()+ "/" + (sb.getMaximum() - sb.getThumb()));
        boolean autoScroll = sb.getSelection() == (sb.getMaximum() - sb.getThumb());
        int nbLines = consoleText.getLineCount();
        if (nbLines > 4096 + 256)
          consoleText.replaceTextRange(0, consoleText.getOffsetAtLine(256), ""); //$NON-NLS-1$
        Calendar now = GregorianCalendar.getInstance();
        String timeStamp = "[" + now.get(Calendar.HOUR_OF_DAY) + ":" + format(now.get(Calendar.MINUTE)) + ":" + format(now.get(Calendar.SECOND)) + "]  "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        nbLines = consoleText.getLineCount();
        consoleText.append(timeStamp + _text + "\n"); //$NON-NLS-1$
        consoleText.setLineBackground(nbLines - 1, 1, colors[_color]);
        if (autoScroll)
          consoleText.setTopIndex(nbLines);
      }
    });
  }

  private String format(int n) {
    if (n < 10)
      return "0" + n; //$NON-NLS-1$
    return "" + n; //$NON-NLS-1$
  }

}
