/*
 * Created on 8 juil. 2003
 *
 */
package org.gudy.azureus2.ui.swt;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ScrollBar;
import org.gudy.azureus2.core.ILoggerListener;
import org.gudy.azureus2.core.Logger;

/**
 * @author Olivier
 * 
 */
public class ConsoleView implements IView, ILoggerListener {

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
    colors[0] = new Color(display, new RGB(64, 160, 255));
    colors[1] = new Color(display, new RGB(128, 192, 255));
    colors[2] = new Color(display, new RGB(192, 224, 255));
    colors[3] = new Color(display, new RGB(255, 192, 192));
    Logger.getLogger().setListener(this);
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
    MainWindow.getWindow().setConsole(null);
    Logger.getLogger().removeListener();
    if (colors != null) {
      for (int i = 0; i < colors.length; i++) {
        if (colors[i] != null && !colors[i].isDisposed())
          colors[i].dispose();
      }
    }
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getShortTitle()
   */
  public String getShortTitle() {
    return Messages.getString("ConsoleView.title.short"); //$NON-NLS-1$
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.ui.swt.IView#getFullTitle()
   */
  public String getFullTitle() {
    return Messages.getString("ConsoleView.title.full"); //$NON-NLS-1$
  }

  /* (non-Javadoc)
   * @see org.gudy.azureus2.core.ILoggerListener#log(int, int, int, java.lang.String)
   */
  public void log(int componentId, int event, int color, String text) {
    if (color < 0 || color > colors.length)
      return;
    doLog(color, text);
  }

  private void doLog(int color, String text) {
    final String _text = text;
    final int _color = color;
    if (display.isDisposed())
      return;
    display.asyncExec(new Runnable() {
      public void run() {
        if (consoleText.isDisposed())
          return;
        ScrollBar sb = consoleText.getVerticalBar();

        //System.out.println(sb.getSelection()+ "/" + (sb.getMaximum() - sb.getThumb()));
        boolean autoScroll = sb.getSelection() == (sb.getMaximum() - sb.getThumb());
        int nbLines = consoleText.getLineCount();
        if (nbLines > 4096 + 256)
          consoleText.replaceTextRange(0, consoleText.getOffsetAtLine(256), ""); //$NON-NLS-1$
        Calendar now = GregorianCalendar.getInstance();        
        String timeStamp =
          "[" + now.get(Calendar.HOUR_OF_DAY) + ":" + format(now.get(Calendar.MINUTE)) + ":" + format(now.get(Calendar.SECOND)) + "]  "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        nbLines = consoleText.getLineCount();
        consoleText.append(timeStamp + _text + "\n"); //$NON-NLS-1$
        consoleText.setLineBackground(nbLines - 1, 1, colors[_color]);
        if (autoScroll)
          consoleText.setTopIndex(nbLines);
      }
    });
  }
  
  
  private String format(int n) {
    if(n < 10) return "0" + n; //$NON-NLS-1$
    return "" + n; //$NON-NLS-1$
  }  

}
