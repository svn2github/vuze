package org.gudy.azureus2.ui.swt.test;

import org.eclipse.swt.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.internal.ole.win32.*;
import org.eclipse.swt.internal.win32.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.ui.swt.URLTransfer;

/**
 * Application to identify supported URL drag and drop IDs on different OSes.
 * Uncomment target.setTransfer to see all dropped IDs.
 * 
 * @see org.gudy.azureus2.ui.swt.URLTransfer
 * @author Rene Leonhardt
 */
public class Win32TransferTypes extends ByteArrayTransfer {

  private static Win32TransferTypes _instance = new Win32TransferTypes();
  private int[] ids;
  private String[] names;

  public static void main(String[] args) {
    Display display = new Display();
    Shell shell = new Shell(display);
    shell.setLayout(new FillLayout());
    Canvas canvas = new Canvas(shell, SWT.NONE);
    DropTarget target = new DropTarget(canvas, DND.DROP_DEFAULT | DND.DROP_COPY | DND.DROP_LINK | DND.DROP_MOVE | DND.DROP_TARGET_MOVE | DND.DROP_NONE);
    target.setTransfer(new Transfer[] { URLTransfer.getInstance()});
    // uncomment to find out the dropped type names and IDs on other OSes than Windows
//    target.setTransfer(new Transfer[] { Win32TransferTypes.getInstance(), TextTransfer.getInstance(), FileTransfer.getInstance()});
    target.addDropListener(new DropTargetAdapter() {
      public void dragEnter(DropTargetEvent event) {
//        if(event.detail == DND.DROP_NONE)
          event.detail = DND.DROP_LINK;
        String ops = "";
        if ((event.operations & DND.DROP_COPY) != 0)
          ops += "Copy;";
        if ((event.operations & DND.DROP_MOVE) != 0)
          ops += "Move;";
        if ((event.operations & DND.DROP_LINK) != 0)
          ops += "Link;";
        System.out.println("Allowed Operations are " + ops);

        TransferData[] data = event.dataTypes;
        for (int i = 0; i < data.length; i++) {
          int id = data[i].type;
          String name = getNameFromId(id);
          System.out.println("Data type is " + id + " " + name);
        }
      }
      public void dragOver(DropTargetEvent event) {
        event.detail = DND.DROP_LINK;
      }
      public void drop(DropTargetEvent event) {
        System.out.println("URL dropped: " + event.data);
      }
    });

    shell.setSize(400, 400);
    shell.open();
    while (!shell.isDisposed()) {
      if (!display.readAndDispatch())
        display.sleep();
    }
    display.dispose();
  }

  public static Win32TransferTypes getInstance() {
    return _instance;
  }
  Win32TransferTypes() {
    ids = new int[50000];
    names = new String[50000];
    for (int i = 0; i < ids.length; i++) {
      ids[i] = i;
      names[i] = getNameFromId(i);
    }
  }
  public void javaToNative(Object object, TransferData transferData) {}
  public Object nativeToJava(TransferData transferData) {
    return "Win32TransferType test";
  }
  protected String[] getTypeNames() {
    return names;
  }
  protected int[] getTypeIds() {
    return ids;
  }
  static String getNameFromId(int id) {
    String name = null;
    int maxSize = 128;
    TCHAR buffer = new TCHAR(0, maxSize);
    int size = COM.GetClipboardFormatName(id, buffer, maxSize);
    String type = null;
    if (size != 0) {
      name = buffer.toString(0, size);
    } else {
      switch (id) {
        case COM.CF_HDROP :
          name = "CF_HDROP";
          break;
        case COM.CF_TEXT :
          name = "CF_TEXT";
          break;
        case COM.CF_BITMAP :
          name = "CF_BITMAP";
          break;
        case COM.CF_METAFILEPICT :
          name = "CF_METAFILEPICT";
          break;
        case COM.CF_SYLK :
          name = "CF_SYLK";
          break;
        case COM.CF_DIF :
          name = "CF_DIF";
          break;
        case COM.CF_TIFF :
          name = "CF_TIFF";
          break;
        case COM.CF_OEMTEXT :
          name = "CF_OEMTEXT";
          break;
        case COM.CF_DIB :
          name = "CF_DIB";
          break;
        case COM.CF_PALETTE :
          name = "CF_PALETTE";
          break;
        case COM.CF_PENDATA :
          name = "CF_PENDATA";
          break;
        case COM.CF_RIFF :
          name = "CF_RIFF";
          break;
        case COM.CF_WAVE :
          name = "CF_WAVE";
          break;
        case COM.CF_UNICODETEXT :
          name = "CF_UNICODETEXT";
          break;
        case COM.CF_ENHMETAFILE :
          name = "CF_ENHMETAFILE";
          break;
        case COM.CF_LOCALE :
          name = "CF_LOCALE";
          break;
        case COM.CF_MAX :
          name = "CF_MAX";
          break;
      }

    }
    return name;
  }
}