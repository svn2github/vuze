package org.gudy.azureus2.core;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.MainWindow;
import org.gudy.azureus2.ui.swt.Messages;

public class LocaleUtil {

  private static final String systemEncoding = System.getProperty("file.encoding"); //$NON-NLS-1$

	private static final String[] charset = {
    systemEncoding,
		"Big5","EUC-JP","EUC-KR","GB18030","GBK","ISO-2022-JP","ISO-2022-KR", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
		"Shift_JIS","KOI8-R","UTF-8","windows-1251","ISO-8859-1" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	};    

  private static final String[] generalCharsets = {
    "ISO-8859-1", "UTF-8", systemEncoding  //$NON-NLS-1$ //$NON-NLS-2$
  };

  private static final CharsetDecoder[] charsetDecoder = new CharsetDecoder[charset.length];

  static {
    for (int i = 0; i < charset.length; i++) {
      try {
        charsetDecoder[i] = Charset.forName(charset[i]).newDecoder();
      } catch (Exception ignore) {
      }
    }
  }

  private boolean rememberEncodingDecision = true;
  private boolean differentEncodingsChoosed = false;

  private String lastChoosedEncoding = null;
  private boolean waitForUserInput = true;

  public static String getCharsetString(byte[] array) throws UnsupportedEncodingException {
    return new String(array, getCharset(array));
  }

	public static String getCharset(byte[] array) {
    Candidate[] candidates = getCandidates(array);

    String defaultString = candidates[0].name;

		Arrays.sort(candidates);

		int minlength = candidates[0].name.length();
		
		/*
		int filterCount = 1;
		for (int i=1;i<candidates.length;i++) {
			if (((Candidate)candidates[i]).name.length() > minlength) break;
			filterCount++;
		}
		*/

		/*
		for (int i=0;i<filterCount;i++) {
			System.out.println(((Candidate)candidates[i]).charset);
		}
		*/
       
		// If the default string length == minlength assumes that
		// the array encoding is from default charset 
		if (defaultString != null && defaultString.length() == minlength) {
			return systemEncoding;
		}

		// Here it is assumed the shorter the string, the more likely is 
		// the correct charset
		return candidates[0].charset; 
	}    

  private static Candidate[] getCandidates(byte[] array) {
    Candidate[] candidates = new Candidate[charset.length];
    for (int i = 0; i < charset.length; i++) {
      candidates[i] = new Candidate();
      try {
        candidates[i].name = charsetDecoder[i].decode(ByteBuffer.wrap(array)).toString();
        candidates[i].charset = charset[i];
      } catch (Exception ignore) {
      }
    }
    return candidates;
  }

  public String getChoosableCharsetString(byte[] array) throws UnsupportedEncodingException {
    Candidate[] candidates = getCandidates(array);

    if(rememberEncodingDecision && lastChoosedEncoding != null) {
      for (int i = 0; i < candidates.length; i++) {
        if(candidates[i].name != null && lastChoosedEncoding.equals(candidates[i].charset)) {
          return candidates[i].name;
        }
      }
    }

    String defaultString = candidates[0].name;

    Arrays.sort(candidates);

    int minlength = candidates[0].name.length();
    
    // If the default string length == minlength assumes that
    // the array encoding is from default charset 
    if (defaultString != null && defaultString.length() == minlength) {
      return defaultString;
    }

    ArrayList choosableCandidates = new ArrayList(5);
    choosableCandidates.add(candidates[0]);
    
    // add all general candidates with names not already in the list
    for (int j = 0; j < generalCharsets.length; j++) {
      for (int i = 1; i < candidates.length; i++) {
        if(candidates[i].name != null && generalCharsets[j].equals(candidates[i].charset) && !choosableCandidates.contains(candidates[i])) {
          choosableCandidates.add(candidates[i]);
          break;
        }
      }
    }

    final Candidate[] candidatesToChoose = (Candidate[]) choosableCandidates.toArray(new Candidate[choosableCandidates.size()]);
    waitForUserInput = true;
    String lastChoosedEncodingSaved = lastChoosedEncoding;
    MainWindow.getWindow().getDisplay().asyncExec(new Runnable() {
      public void run() {
        showChoosableEncodingWindow(MainWindow.getWindow().getShell(), candidatesToChoose);
        waitForUserInput = false;
      }
    });
    
    while(waitForUserInput) {
      try {
        Thread.sleep(100);
      } catch (Exception ignore) {
      }
    }

    int choosedIndex = 0;
    for (int i = 1; i < candidatesToChoose.length; i++) {
      if(candidatesToChoose[i].name != null && lastChoosedEncoding.equals(candidatesToChoose[i].charset)) {
        choosedIndex = i;
        break;
      }
    }

    if(lastChoosedEncodingSaved != null && !lastChoosedEncodingSaved.equals(lastChoosedEncoding))
      differentEncodingsChoosed = true;

    return candidatesToChoose[choosedIndex].name; 
  }    

  private void showChoosableEncodingWindow(final Shell shell, final Candidate[] candidates) {
    final Display display = shell.getDisplay();
    final Shell s = new Shell(shell, SWT.TITLE | SWT.PRIMARY_MODAL);
    s.setImage(ImageRepository.getImage("azureus")); //$NON-NLS-1$
    s.setText(MessageText.getString("LocaleUtil.title")); //$NON-NLS-1$
    GridData gridData;
    s.setLayout(new GridLayout(1, true));
    s.setLayoutData(gridData = new GridData());

/*
    Label label = new Label(s, SWT.NONE);
    gridData = new GridData(GridData.HORIZONTAL_ALIGN_CENTER);
    label.setLayoutData(gridData);
    label.setText("Bitte wählen Sie das Encoding, welches am besten passt");
*/
    Group gChoose = new Group(s, SWT.NULL);
    gChoose.setLayout(new GridLayout(3, false));
    Messages.setLanguageText(gChoose, "LocaleUtil.section.chooseencoding"); //$NON-NLS-1$
    gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
    gridData.horizontalSpan = 3;
    gChoose.setLayoutData(gridData);

    Label label = new Label(gChoose, SWT.LEFT);
    gridData = new GridData();
    gridData.horizontalSpan = 3;
    label.setLayoutData(gridData);
    Messages.setLanguageText(label, "LocaleUtil.label.chooseencoding"); //$NON-NLS-1$

    final Table table = new Table(gChoose, SWT.SINGLE | SWT.FULL_SELECTION);
    table.setLinesVisible(true);
    table.setHeaderVisible(true);
    gridData = new GridData();
    gridData.horizontalSpan = 3;
    table.setLayoutData(gridData);

    String[] titlesPieces = { "filename", "encoding"}; //$NON-NLS-1$ //$NON-NLS-2$
    for (int i = 0; i < titlesPieces.length; i++) {
      TableColumn column = new TableColumn(table, SWT.LEFT);
      Messages.setLanguageText(column, "LocaleUtil.column." + titlesPieces[i]); //$NON-NLS-1$
    }

    // add candidates to table
    for (int i = 0; i < candidates.length; i++) {
      TableItem item = new TableItem(table, SWT.NULL);
      item.setText(0, candidates[i].name);
      item.setText(1, candidates[i].charset);
    }
    int lastSelectedIndex = 0;
    for (int i = 1; i < candidates.length; i++) {
      if(candidates[i].name != null && candidates[i].charset.equals(lastChoosedEncoding)) {
        lastSelectedIndex = i;
        break;
      }
    }
    table.select(lastSelectedIndex);

    // resize all columns to fit the widest entry 
    table.getColumn(0).pack();
    table.getColumn(1).pack();

    label = new Label(gChoose, SWT.LEFT);
    gridData = new GridData();
    gridData.horizontalSpan = 3;
    label.setLayoutData(gridData);
    Messages.setLanguageText(label, "LocaleUtil.label.hint.doubleclick"); //$NON-NLS-1$

    final Button ok = new Button(gChoose, SWT.PUSH);
    ok.setText(" "+MessageText.getString("Button.next")+" "); //$NON-NLS-1$ //$NON-NLS-3$ //$NON-NLS-2$
    ok.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
    label = new Label(gChoose, SWT.NULL);
    label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END));
    Messages.setLanguageText(label, "LocaleUtil.label.checkbox.rememberdecision"); //$NON-NLS-1$

    final Button checkBox = new Button(gChoose, SWT.CHECK);
    checkBox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
    checkBox.setSelection(rememberEncodingDecision);

    s.pack();
    Rectangle splashRect = s.getBounds();
    Rectangle displayRect = display.getBounds();
    int x = (displayRect.width - splashRect.width) / 2;
    int y = (displayRect.height - splashRect.height) / 2;
    s.setLocation(x, y);

    ok.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent event) {
        setChoosedIndex(s, table, checkBox, candidates);
      }
    });

    table.addMouseListener(new MouseAdapter() {
      public void mouseDoubleClick(MouseEvent mEvent) {
        setChoosedIndex(s, table, checkBox, candidates);
      }
    });

    s.open();
    while (!s.isDisposed()) {
      try {
        if (!display.readAndDispatch())
          display.sleep();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void setChoosedIndex(final Shell s, final Table table, final Button checkBox, Candidate[] candidates) {
    int selectedIndex = table.getSelectionIndex();
    if(-1 == selectedIndex) 
      return;
    rememberEncodingDecision = checkBox.getSelection();
    lastChoosedEncoding = candidates[selectedIndex].charset;
    s.dispose();
  }

  private static class Candidate implements Comparable {
    private String name;
    private String charset;

    public int compareTo(Object o) {
      Candidate candidate = (Candidate)o;
      if(null == name || null == candidate.name)
        return 0;
      if (candidate.name.hashCode()==name.hashCode() &&
        candidate.charset.hashCode()==charset.hashCode()) {
        return 0;
      }
      if (name.length() < candidate.name.length()) {
        return -1;
      } 
      return 1;
    }
    /**
     * only used for contains()
     * Warning: this implementation covers not all special cases
     */
    public boolean equals(Object obj) {
      Candidate other = (Candidate) obj;
      return name.equals(other.name);
    }

  }

  /**
   * @param lastEncoding the last (saved) encoding, which was used for the torrent
   */
  public LocaleUtil(Object lastEncoding) {
    super();
    if(lastEncoding != null) {
      String encoding = lastEncoding instanceof byte[] ? new String((byte[]) lastEncoding) : (String) lastEncoding; 
      for (int i = 0; i < charset.length; i++) {
        if(charset[i].equals(encoding)) {
          lastChoosedEncoding = encoding;
          return;
        }
      }
    }
  }

  public LocaleUtil() {
    this(null);
  }

  /**
   * @return true, if the lastChoosedEncoding should be remembered and only one encoding was always choosed
   */
  public boolean canEncodingBeSaved() {
    return !differentEncodingsChoosed && rememberEncodingDecision && lastChoosedEncoding != null;
  }

  /**
   * @return the last encoding choosed by the user; can be null
   */
  public String getLastChoosedEncoding() {
    return lastChoosedEncoding;
  }

}