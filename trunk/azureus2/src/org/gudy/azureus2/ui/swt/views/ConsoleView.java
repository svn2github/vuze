/*
 * Created on 8 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt.views;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ScrollBar;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.ILoggerListener;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.ui.swt.MainWindow;

/**
 * @author Olivier
 * 
 */
public class ConsoleView extends AbstractIView implements ILoggerListener {

  Display display;
  StyledText consoleText;
  Color[] colors;

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#initialize(org.eclipse.swt.widgets.Composite)
   */
  public void initialize(Composite composite) {
    display = composite.getDisplay();
    consoleText = new StyledText(composite, SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL);    
    colors = new Color[4];
    colors[0] = MainWindow.blues[3];
    colors[1] = MainWindow.blues[2];
    colors[2] = MainWindow.blues[1];
    colors[3] = MainWindow.red_ConsoleView;
    LGLogger.setListener(this);
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
    LGLogger.removeListener();
  	MainWindow.getWindow().getShell().setFocus();
    MainWindow.getWindow().setConsole(null);
    consoleText.dispose();    
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getFullTitle()
   */
  public String getFullTitle() {
    return MessageText.getString("ConsoleView.title.full"); //$NON-NLS-1$
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.core.ILoggerListener#log(int, int, int, java.lang.String)
   */
  public void log(int componentId, int event, int color, String text) {
    if (color < 0 || color > colors.length || display == null || display.isDisposed())
      return;
    doLog(color, text);
  }

  private void doLog(final int _color, final String _text) {
    if(display == null || display.isDisposed())
      return;
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
        String timeStamp =
          "[".concat(String.valueOf(now.get(Calendar.HOUR_OF_DAY)).concat(":").concat(format(now.get(Calendar.MINUTE)) + ":" + format(now.get(Calendar.SECOND))).concat("]  "); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$        
        consoleText.append(timeStamp + _text + "\n"); //$NON-NLS-1$
        nbLines = consoleText.getLineCount();
        consoleText.setLineBackground(nbLines - 2, 1, colors[_color]);
        if (autoScroll)
          consoleText.setTopIndex(nbLines-1);
      }
    });
  }
  
  
  private String format(int n) {
    if(n < 10) return "0".concat(String.valueOf(n)); //$NON-NLS-1$
    return String.valueOf(n); //$NON-NLS-1$
  }  

}
