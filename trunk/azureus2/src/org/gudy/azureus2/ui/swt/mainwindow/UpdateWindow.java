/*
 * Created on 3 mai 2004
 * Created by Olivier Chalouhi
 * 
 * Copyright (C) 2004 Aelitis SARL, All rights Reserved
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * AELITIS, SARL au capital de 30,000 euros,
 * 8 Alle Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.mainwindow;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.BDecoder;
import org.gudy.azureus2.ui.swt.ImageRepository;
import org.gudy.azureus2.ui.swt.Messages;

/**
 * @author Olivier Chalouhi
 *
 */
public class UpdateWindow implements Runnable{
  
  Display display;
  Shell shell;
  
  String latestVersion;  
  String latestVersionFileName;
  
  private static boolean jarDownloaded = false;
  private static boolean updateJar = false;
  
  public static void show(String latestVersion,String latestVersionFileName) {
    Display display = SWTThread.getInstance().getDisplay();
    if(display == null || display.isDisposed())
      return;
    display.asyncExec(new UpdateWindow(display,latestVersion,latestVersionFileName));
  }
  
  
  
  public UpdateWindow(Display display,String latestVersion,String latestVersionFileName) {
    this.display = display;
    this.latestVersion = latestVersion;
    this.latestVersionFileName = latestVersionFileName;
  }
  
  public void run() {
    
      final Shell s = new Shell(display, SWT.CLOSE | SWT.PRIMARY_MODAL);

      s.setImage(ImageRepository.getImage("azureus")); //$NON-NLS-1$
      s.setText(MessageText.getString("MainWindow.upgrade.assistant")); //$NON-NLS-1$
      s.setSize(250, 300);
      s.setLayout(new GridLayout(3, true));
      GridData gridData;
      s.setLayoutData(gridData = new GridData());
      //    gridData.horizontalIndent = 10;

      Group gInfo = new Group(s, SWT.NULL);
      gInfo.setLayout(new GridLayout());
      Messages.setLanguageText(gInfo, "MainWindow.upgrade.section.info"); //$NON-NLS-1$
      gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
      gInfo.setLayoutData(gridData);
      gridData.horizontalSpan = 3;

      Label label = new Label(gInfo, SWT.CENTER);
      int posMessage = latestVersion.indexOf(" (");
      String newVersion = posMessage >= 0 ? latestVersion.substring(0, posMessage) : latestVersion;
      label.setText(MessageText.getString("MainWindow.upgrade.newerversion") + ": " + newVersion + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      FontData[] fontData = label.getFont().getFontData();
      for (int i = 0; i < fontData.length; i++) {
        fontData[i].setStyle(SWT.BOLD);
      }
      Font fontLastestVer = new Font(display, fontData);
      label.setFont(fontLastestVer);
      label.setLayoutData(gridData = new GridData());
      gridData.horizontalSpan = 3;

      label = new Label(gInfo, SWT.LEFT);
      label.setText(MessageText.getString("MainWindow.upgrade.explanation") + ".\n"); //$NON-NLS-1$ //$NON-NLS-2$
      label.setLayoutData(gridData = new GridData());
      gridData.horizontalSpan = 3;

      label = new Label(s, SWT.LEFT);
      label.setText("\n"); //$NON-NLS-1$
      label.setLayoutData(gridData = new GridData());
      gridData.horizontalSpan = 3;

      Group gManual = new Group(s, SWT.NULL);
      gManual.setLayout(new GridLayout());
      Messages.setLanguageText(gManual, "MainWindow.upgrade.section.manual"); //$NON-NLS-1$
      gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
      gManual.setLayoutData(gridData);
      gridData.horizontalSpan = 3;

      label = new Label(gManual, SWT.NULL);
      label.setText(MessageText.getString("MainWindow.upgrade.explanation.manual") + ":\n"); //$NON-NLS-1$ //$NON-NLS-2$
      label.setLayoutData(gridData = new GridData());

      final String downloadLink;
      if (latestVersionFileName == null) {
        downloadLink = "http://azureus.sourceforge.net/Azureus2.jar"; //$NON-NLS-1$
      }
      else {
        downloadLink = "http://prdownloads.sourceforge.net/azureus/" + latestVersionFileName + "?download";
      }
      final Label linklabel = new Label(gManual, SWT.NULL);
      linklabel.setText(downloadLink);
      linklabel.setCursor(Cursors.handCursor);
      linklabel.setForeground(Colors.blue);
      linklabel.setLayoutData(gridData = new GridData());

      linklabel.addMouseListener(new MouseAdapter() {
        public void mouseDoubleClick(MouseEvent arg0) {
          Program.launch(downloadLink);
        }
        public void mouseDown(MouseEvent arg0) {
          Program.launch(downloadLink);
        }
      });

      label = new Label(s, SWT.LEFT);
      label.setText("\n"); //$NON-NLS-1$
      label.setLayoutData(gridData = new GridData());
      gridData.horizontalSpan = 3;

      Group gAutomatic = new Group(s, SWT.NULL);
      gAutomatic.setLayout(new GridLayout());
      Messages.setLanguageText(gAutomatic, "MainWindow.upgrade.section.automatic"); //$NON-NLS-1$
      gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL);
      gAutomatic.setLayoutData(gridData);
      gridData.horizontalSpan = 3;

      final Label step1 = new Label(gAutomatic, SWT.LEFT);
      step1.setText("- " + MessageText.getString("MainWindow.upgrade.step1")); //$NON-NLS-1$ //$NON-NLS-2$
      step1.setForeground(Colors.blue);
      step1.setLayoutData(gridData = new GridData());
      gridData.horizontalSpan = 3;

      final Label step2 = new Label(gAutomatic, SWT.LEFT);
      step2.setText("- " + MessageText.getString("MainWindow.upgrade.step2") + "\n\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      step2.setLayoutData(gridData = new GridData());
      gridData.horizontalSpan = 3;

      final Label hint = new Label(gAutomatic, SWT.LEFT);
      hint.setText(MessageText.getString("MainWindow.upgrade.hint1") + "."); //$NON-NLS-1$ //$NON-NLS-2$
      hint.setLayoutData(gridData = new GridData(GridData.FILL_HORIZONTAL));
      gridData.horizontalSpan = 3;

      label = new Label(gAutomatic, SWT.LEFT);
      label.setText("\n"); //$NON-NLS-1$
      label.setLayoutData(gridData = new GridData());
      gridData.horizontalSpan = 3;

      final ProgressBar progressBar = new ProgressBar(gAutomatic, SWT.SMOOTH);
      progressBar.setLayoutData(gridData = new GridData(GridData.FILL_HORIZONTAL));
      gridData.horizontalSpan = 3;
      progressBar.setToolTipText(MessageText.getString("MainWindow.upgrade.tooltip.progressbar")); //$NON-NLS-1$

      label = new Label(s, SWT.LEFT);
      label.setText("\n"); //$NON-NLS-1$
      label.setLayoutData(gridData = new GridData());
      gridData.horizontalSpan = 3;

      final Button next = new Button(s, SWT.PUSH);
      next.setText(" " + MessageText.getString("Button.next") + " > "); //$NON-NLS-1$ //$NON-NLS-3$ //$NON-NLS-2$

      gridData = new GridData();
      next.setLayoutData(gridData);

      final Button finish = new Button(s, SWT.PUSH);
      finish.setText(" " + MessageText.getString("Button.finish") + " "); //$NON-NLS-1$ //$NON-NLS-3$ //$NON-NLS-2$
      finish.setLayoutData(new GridData());

      final Button cancel = new Button(s, SWT.PUSH);
      cancel.setText(" " + MessageText.getString("Button.cancel") + " "); //$NON-NLS-1$ //$NON-NLS-3$ //$NON-NLS-2$
      cancel.setLayoutData(new GridData());

      SelectionAdapter update = new SelectionAdapter() {
        public void widgetSelected(SelectionEvent event) {
          downloadJar(progressBar, hint);
          if (jarDownloaded) {
            if (event.widget == finish) {
              updateJar = true;
        current_upgrade_window = null;
              s.dispose();
              dispose();
            }
            else {
              next.setEnabled(false);
              step1.setForeground(Colors.black);
              step2.setForeground(Colors.blue);
              s.setDefaultButton(finish);
              hint.setText(MessageText.getString("MainWindow.upgrade.hint2") + "."); //$NON-NLS-1$ //$NON-NLS-2$
              hint.setForeground(Colors.black);
              hint.pack();
              linklabel.setEnabled(false);
            }
          }
          else {
            if (event.widget == finish) {
        current_upgrade_window = null;
              s.dispose();
            }
            else {
              hint.setText(MessageText.getString("MainWindow.upgrade.error.downloading.hint") + "!"); //$NON-NLS-1$ //$NON-NLS-2$
              hint.setForeground(Colors.red);
              hint.pack();
              next.setEnabled(false);
              finish.setEnabled(false);
            }
          }
        }
      };

      next.addSelectionListener(update);
      finish.addSelectionListener(update);

      cancel.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent event) {
          s.dispose();
      current_upgrade_window = null;
        }
      });

      s.pack();

      Rectangle parent = mainWindow.getBounds();
      Rectangle child = s.getBounds();
      child.x = parent.x + (parent.width - child.width) / 2;
      child.y = parent.y + (parent.height - child.height) / 2;
      s.setBounds(child);

      s.open();
      s.setFocus();

      if (fontLastestVer != null && !fontLastestVer.isDisposed()) {
        fontLastestVer.dispose();
      }
  }
  
  private void downloadJar(final ProgressBar progressBar, final Label hint) {
    if (jarDownloaded) {
      progressBar.setSelection(progressBar.getMaximum());
      return;
    }

    FileOutputStream fos = null;
    InputStream in = null;
    FileWriter log = null;
    boolean foundMirror = false;
    
    try {
      String userPath = System.getProperty("user.dir");
      File logFile = new File( userPath, "update.log" );
      log = new FileWriter( logFile, true );
      
      //File originFile = FileUtil.getApplicationFile("Azureus2.jar"); //$NON-NLS-1$
      File originFile = new File(userPath, "Azureus2.jar");
      File newFile = new File(originFile.getParentFile(), "Azureus2-new.jar"); //$NON-NLS-1$
      
      log.write("downloadJar:: originFile=" + originFile.getAbsolutePath()
                           + " newFile=" + newFile.getAbsolutePath() + "\n");
      
      URL reqUrl = null;
      if (latestVersionFileName == null) {
        reqUrl = new URL("http://azureus.sourceforge.net/Azureus2.jar"); //$NON-NLS-1$
      }
      else {
        //New update method, using sourceforge mirrors.
        URL mirrorsUrl = new URL("http://prdownloads.sourceforge.net/azureus/" + latestVersionFileName + "?download");
        String mirrorsHtml = readUrl(mirrorsUrl);
        List mirrors = new ArrayList();
        String pattern = "/azureus/" + latestVersionFileName + "?use_mirror=";
        int position = mirrorsHtml.indexOf(pattern);
        while (position > 0) {
          int end = mirrorsHtml.indexOf(">", position);
          if (end < 0) {
            position = -1;
          }
          else {
            String mirror = mirrorsHtml.substring(position, end);
            //System.out.println(mirror);
            mirrors.add(mirror);
            position = mirrorsHtml.indexOf(pattern, position + 1);
          }
        }

        //Grab a random mirror
        if ( mirrors.size() > 0 ) {
          int random = (int) (Math.random() * mirrors.size());
          String mirror = (String) (mirrors.get(random));

          URL mirrorUrl = new URL("http://prdownloads.sourceforge.net" + mirror);
          String mirrorHtml = readUrl(mirrorUrl);
          pattern = "<META HTTP-EQUIV=\"refresh\" content=\"5; URL=";
          position = mirrorHtml.indexOf(pattern);
          if ( position >= 0 ) {
            int end = mirrorHtml.indexOf("\">", position);
            if ( end >= 0 ) {
              reqUrl = new URL(mirrorHtml.substring(position + pattern.length(), end));
              foundMirror = true;
            }
          }
        }
      }

      if (reqUrl == null || !foundMirror) {
        reqUrl = getMirrorFromBackupList( log );
      }
      
      HttpURLConnection con = null;
      try {
        con = (HttpURLConnection) reqUrl.openConnection();
        con.connect();
        in = con.getInputStream();
      } catch (IOException e) {
        //probably a 404 error, try one last time
        if (con != null)  con.disconnect();
        URL backup = getMirrorFromBackupList( log );
        con = (HttpURLConnection) backup.openConnection();
        con.connect();
        in = con.getInputStream();
      }
            
      hint.setText(MessageText.getString("MainWindow.upgrade.downloadingfrom") + con.getURL());
      
      log.write("downloadJar:: downloading new Azureus jar from " + con.getURL() + " .....");
      
      fos = new FileOutputStream(newFile);

      progressBar.setMinimum(0);
      progressBar.setMaximum(100);

      final InputStream input = in;
      final FileOutputStream output = fos;

      final long length = con.getContentLength();
      final byte[] buffer = new byte[8192];
      int c;
      long bytesDownloaded = 0L;
      while ((c = input.read(buffer)) != -1) {
        output.write(buffer, 0, c);
        bytesDownloaded += c;
        int progress = (int) Math.round(((double) bytesDownloaded / length) * 100);
        progressBar.setSelection(progress <= 100 ? progress : 100);
        progressBar.update();
        display.readAndDispatch();
      }
      log.write("done\n");
      
      jarDownloaded = true;
    }
    catch (MalformedURLException e) {
      e.printStackTrace();
    }
    catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      try {
        if (log != null) log.close();
        if (fos != null) fos.close();
        if (in != null) in.close();
      }
      catch (Exception e) {}
    }
  }
  
  private URL getMirrorFromBackupList( FileWriter log ) {
    try{ log.write("Retrieving backup SF mirror list..."); } catch (Exception e) {}
    ByteArrayOutputStream message = new ByteArrayOutputStream();
    int nbRead = 0;
    HttpURLConnection con = null;
    InputStream is = null;
    try {
      String url = "http://azureus.sourceforge.net/mirrors.php";
      URL mirrorUrl = new URL(url);
      con = (HttpURLConnection) mirrorUrl.openConnection();
      con.connect();
      is = con.getInputStream();
      byte[] data = new byte[1024];
      while (nbRead >= 0) {
        nbRead = is.read(data);
        if (nbRead >= 0) {
          message.write(data, 0, nbRead);
        }
      }
      Map decoded = BDecoder.decode(message.toByteArray());
      List mirrors = (List)decoded.get("mirrors");
      int random = (int) (Math.random() * mirrors.size());
      String mirror = new String( (byte[])mirrors.get(random) );
      return new URL( mirror + latestVersionFileName );
    }
    catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
  
  private void updateJar() {
    FileOutputStream out = null;
    InputStream in = null;
    try {
      String classPath = System.getProperty("java.class.path"); //$NON-NLS-1$
      String libraryPath = System.getProperty("java.library.path"); //$NON-NLS-1$
      String userPath = System.getProperty("user.dir"); //$NON-NLS-1$
      String javaPath = System.getProperty("java.home")
                      + System.getProperty("file.separator")
                      + "bin"
                      + System.getProperty("file.separator");
      
     
      //remove any trailing slashes
      if (libraryPath.endsWith("\\")) {
        libraryPath = libraryPath.substring(0, libraryPath.length() -1);
      }
      
      File logFile = new File( userPath, "update.log" );
      FileWriter log = new FileWriter( logFile, true );
      
      log.write(new Date(SystemTime.getCurrentTime()).toString() + "\n");
      log.write("updateJar:: classPath=" + classPath
                         + " libraryPath=" + libraryPath
                         + " userPath=" + userPath + "\n");
    
      //File updaterJar = FileUtil.getApplicationFile("Updater.jar"); //$NON-NLS-1$
      File updaterJar = new File(userPath, "Updater2.jar");
      
      log.write("updateJar:: looking for " + updaterJar.getAbsolutePath() + "\n");
      
      if (!updaterJar.isFile()) {
        log.write("updateJar:: downloading new Updater2.jar file .....");
        URL reqUrl = new URL("http://azureus.sourceforge.net/Updater2.jar"); //$NON-NLS-1$
        HttpURLConnection con = (HttpURLConnection) reqUrl.openConnection();
        con.connect();
        in = con.getInputStream();
        out = new FileOutputStream(updaterJar);
        byte[] buffer = new byte[2048];
        int c;
        while ((c = in.read(buffer)) != -1) {
          out.write(buffer, 0, c);
          display.readAndDispatch();
        }
        log.write("done\n");
      }
      else log.write("updateJar:: using existing Updater2.jar file\n");

      String exec = javaPath + "java -classpath \"" + updaterJar.getAbsolutePath()
                  + "\" org.gudy.azureus2.update.Updater \"" + classPath
                  + "\" \"" + libraryPath
                  + "\" \"" + userPath + "\"";

      log.write("updateJar:: executing command: " + exec + "\n");
      if (log != null) log.close();
      
     RestartUtil.exec(exec);
     
    }
    catch (Exception e1) {
      e1.printStackTrace();
      updateJar = false;
    }
    finally {
      try {
        if (out != null) out.close();
        if (in != null) in.close();
      }
      catch (Exception e) {}
    }
  }

  private String readUrl(URL url) {
    String result = "";
    InputStream in = null;
    try {
      HttpURLConnection con = (HttpURLConnection) url.openConnection();
      con.connect();
      in = con.getInputStream();
      final ByteArrayOutputStream message = new ByteArrayOutputStream();
      byte[] data = new byte[1024];
      int nbRead = 0;
      while (nbRead >= 0) {
        try {
          nbRead = in.read(data);
          if (nbRead >= 0)
            message.write(data, 0, nbRead);
          Thread.sleep(20);
        }
        catch (Exception e) {
          nbRead = -1;
        }
        display.readAndDispatch();
      }
      result = message.toString();
    }
    catch (NoClassDefFoundError ignoreSSL) { // javax/net/ssl/SSLSocket
    }
    catch (Exception ignore) {}
    finally {
      if (in != null) {
        try {
          in.close();
        }
        catch (Exception e) {}
        in = null;
      }
    }

    return result;
  }
}
