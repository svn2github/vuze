/*
 * Copyright (C) 2003, 2004, 2005, 2006 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * AELITIS, SAS au capital de 46,603.30 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.views;

import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.IndentWriter;
import org.gudy.azureus2.ui.swt.Utils;
import org.gudy.azureus2.ui.swt.debug.ObfusticateImage;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnCreator;
import org.gudy.azureus2.ui.swt.views.table.utils.TableColumnManager;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreRunningListener;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.common.table.TableColumnCore;
import com.aelitis.azureus.ui.selectedcontent.SelectedContentManager;
import com.aelitis.azureus.ui.swt.utils.ColorCache;

import org.gudy.azureus2.plugins.ui.tables.TableManager;

/**
 * @author MjrTom
 *			2005/Dec/08: Avg Avail Item
 */

public class MyTorrentsSuperView extends AbstractIView implements
		ObfusticateImage, IViewExtension
{
	private static int SASH_WIDTH = 5;

	
  private MyTorrentsView torrentview;
  private MyTorrentsView seedingview;

	private Composite form;

	private MyTorrentsView lastSelectedView;


	private Composite child1;


	private Composite child2;


	private final Text txtFilter;


	private final Composite cCats;

  public MyTorrentsSuperView(Text txtFilter, Composite cCats) {
  	this.txtFilter = txtFilter;
		this.cCats = cCats;
		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(AzureusCore core) {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						TableColumnManager tcManager = TableColumnManager.getInstance();
						tcManager.addColumns(getCompleteColumns());
						tcManager.addColumns(getIncompleteColumns());
					}
				});
			}
		});
  }
  

  public Composite getComposite() {
    return form;
  }
  
  public void delete() {
    if (torrentview != null)
      torrentview.delete();
    if (seedingview != null)
      seedingview.delete();
    super.delete();
  }

  public void initialize(final Composite parent) {
    if (form != null) {
      return;
    }

  	form = new Composite(parent, SWT.NONE);
  	FormLayout flayout = new FormLayout();
  	flayout.marginHeight = 0;
  	flayout.marginWidth = 0;
  	form.setLayout(flayout);
  	GridData gridData;
  	gridData = new GridData(GridData.FILL_BOTH);
  	form.setLayoutData(gridData);
  	
  	GridLayout layout;
  	
  	
  	child1 = new Composite(form,SWT.NONE);
  	layout = new GridLayout();
  	layout.numColumns = 1;
  	layout.horizontalSpacing = 0;
  	layout.verticalSpacing = 0;
  	layout.marginHeight = 0;
  	layout.marginWidth = 0;
  	child1.setLayout(layout);

    final Sash sash = new Sash(form, SWT.HORIZONTAL);
    Image image = new Image(sash.getDisplay(), 9, SASH_WIDTH);
    ImageData imageData = image.getImageData();
    int[] row = new int[imageData.width];
    for (int i = 0; i < row.length; i++) {
   		row[i] = (i % 3) != 0 ? 0xE0E0E0 : 0x808080;
    	if (imageData.depth == 32) {
    		row[i] = (row[i] & 255) + (row[i] << 8);
    	}
		}
    for (int y = 1; y < imageData.height - 1; y++) {
    	imageData.setPixels(0, y, row.length, row, 0);
    }
    Arrays.fill(row, 0xE0E0E0E0);
  	imageData.setPixels(0, 0, row.length, row, 0);
  	imageData.setPixels(0, imageData.height - 1, row.length, row, 0);
    image.dispose();
    image = new Image(sash.getDisplay(), imageData);
    sash.setBackgroundImage(image);
    sash.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				sash.getBackgroundImage().dispose();
			}
		});

    child2 = new Composite(form,SWT.NULL);
    layout = new GridLayout();
    layout.numColumns = 1;
    layout.horizontalSpacing = 0;
    layout.verticalSpacing = 0;
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    child2.setLayout(layout);

    FormData formData;

		// FormData for table child1
		formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.top = new FormAttachment(0, 0);
		child1.setLayoutData(formData);
		final FormData child1Data = formData;
    
		// sash
		formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.top = new FormAttachment(child1);
		formData.height = SASH_WIDTH;
		sash.setLayoutData(formData);

    // child2
		formData = new FormData();
		formData.left = new FormAttachment(0, 0);
		formData.right = new FormAttachment(100, 0);
		formData.bottom = new FormAttachment(100, 0);
		formData.top = new FormAttachment(sash);
    // More precision, times by 100
    int weight = (int) (COConfigurationManager.getFloatParameter("MyTorrents.SplitAt"));
		if (weight > 10000) {
			weight = 10000;
		} else if (weight < 100) {
			weight *= 100;
		}
		// Min/max of 5%/95%
		if (weight < 500) {
			weight = 500;
		} else if (weight > 9000) {
			weight = 9000;
		}
		
		// height will be set on first resize call
		sash.setData("PCT", new Double((float)weight / 10000));
		child2.setLayoutData(formData);

		
		// Listeners to size the folder
		sash.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				final boolean FASTDRAG = true;

				if (FASTDRAG && e.detail == SWT.DRAG)
					return;

				child1Data.height = e.y + e.height - SASH_WIDTH;
				form.layout();

				Double l = new Double((double) child1.getBounds().height
						/ form.getBounds().height);
				sash.setData("PCT", l);
				if (e.detail != SWT.DRAG) {
					int i = (int) (l.doubleValue() * 10000);
					COConfigurationManager.setParameter("MyTorrents.SplitAt", i);
				}
			}
		});

		form.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event e) {
				Double l = (Double) sash.getData("PCT");
				if (l != null) {
					child1Data.height = (int) (form.getBounds().height * l
							.doubleValue());
					form.layout();
				}
			}
		});
		try {
  		Double l = (Double) sash.getData("PCT");
  		if (l != null) {
  			child1Data.height = (int) (form.getBounds().height * l
  					.doubleValue());
  		}
		} catch (Exception e) {
			
		}

		AzureusCoreFactory.addCoreRunningListener(new AzureusCoreRunningListener() {
			public void azureusCoreRunning(final AzureusCore core) {
				Utils.execSWTThread(new AERunnable() {
					public void runSupport() {
						initializeWithCore(core, parent);
					}

				});
			}
  	});

  }
  
  private void initializeWithCore(AzureusCore core, Composite parent) {
    torrentview = createTorrentView(core,
				TableManager.TABLE_MYTORRENTS_INCOMPLETE, false, getIncompleteColumns(),
				child1);

    seedingview = createTorrentView(core,
				TableManager.TABLE_MYTORRENTS_COMPLETE, true, getCompleteColumns(),
				child2);

  	initializeDone();
  }

  public void initializeDone() {
	}


	public void refresh() {
    if (getComposite() == null || getComposite().isDisposed())
      return;

    if (seedingview != null) {
    	seedingview.refresh();
    }
    if (torrentview != null) {
    	torrentview.refresh();
    }
  }

  public void updateLanguage() {
  	// no super call, the views will do their own
  	
    if (getComposite() == null || getComposite().isDisposed())
      return;

    if (seedingview != null) {
    	seedingview.updateLanguage();
    }
    if (torrentview != null) {
    	torrentview.updateLanguage();
    }
	}

	public String getFullTitle() {
    return MessageText.getString("MyTorrentsView.mytorrents");
  }
  
  // XXX: Is there an easier way to find out what has the focus?
  private MyTorrentsView getCurrentView() {
    // wrap in a try, since the controls may be disposed
    try {
      if (torrentview != null && torrentview.isTableFocus())
        lastSelectedView = torrentview;
      else if (seedingview != null && seedingview.isTableFocus())
      	lastSelectedView = seedingview;
    } catch (Exception ignore) {/*ignore*/}

    return lastSelectedView;
  }

  // IconBarEnabler
  public boolean isEnabled(String itemKey) {
    IView currentView = getCurrentView();
    if (currentView != null)
      return currentView.isEnabled(itemKey);
    else
      return false;
  }
  
  // IconBarEnabler
  // @see org.gudy.azureus2.ui.swt.views.AbstractIView#itemActivated(java.lang.String)
  public void itemActivated(String itemKey) {
    IView currentView = getCurrentView();
    if (currentView != null)
      currentView.itemActivated(itemKey);    
  }
  
  public DownloadManager[] getSelectedDownloads() {
	  MyTorrentsView currentView = getCurrentView();
	  if (currentView == null) {return null;}
	  return currentView.getSelectedDownloads();
  }
  
  public void
  generateDiagnostics(
	IndentWriter	writer )
  {
	  super.generateDiagnostics( writer );

	  try{
		  writer.indent();
	  
		  writer.println( "Downloading" );
		  
		  writer.indent();

		  torrentview.generateDiagnostics( writer );
	  
	  }finally{
		  
		  writer.exdent();
		  
		  writer.exdent();
	  }
	  
	  try{
		  writer.indent();
	  
		  writer.println( "Seeding" );
		  
		  writer.indent();

		  seedingview.generateDiagnostics( writer );
	  
	  }finally{
		  
		  writer.exdent();

		  writer.exdent();
	  }
  }

	public Image obfusticatedImage(Image image, Point shellOffset) {
		if (torrentview != null) {
			torrentview.obfusticatedImage(image, shellOffset);
		}
		if (seedingview != null) {
			seedingview.obfusticatedImage(image, shellOffset);
		}
		return image;
	}

	public Menu getPrivateMenu() {
		return null;
	}

	public void viewActivated() {
		SelectedContentManager.clearCurrentlySelectedContent();

		IView currentView = getCurrentView();
    if (currentView instanceof IViewExtension) {
    	((IViewExtension)currentView).viewActivated();
    }
    if (currentView instanceof MyTorrentsView) {
    	((MyTorrentsView)currentView).updateSelectedContent();
    }
	}

	public void viewDeactivated() {
    IView currentView = getCurrentView();
    if (currentView == null) {return;}
    if (currentView instanceof IViewExtension) {
    	((IViewExtension)currentView).viewDeactivated();
    }
    /*
    String ID = currentView.getShortTitle();
    if (currentView instanceof MyTorrentsView) {
    	ID = ((MyTorrentsView)currentView).getTableView().getTableID();
    }

    TableView tv = null;
    if (currentView instanceof MyTorrentsView) {
    	tv = ((MyTorrentsView) currentView).getTableView();    	
    }
    //SelectedContentManager.clearCurrentlySelectedContent();
    SelectedContentManager.changeCurrentlySelectedContent(ID, null, tv);
    */
	}
	
	/**
	 * Returns the set of columns for the incomplete torrents view
	 * Subclasses my override to return a different set of columns
	 * @return
	 */
	protected TableColumnCore[] getIncompleteColumns(){
		return TableColumnCreator.createIncompleteDM(TableManager.TABLE_MYTORRENTS_INCOMPLETE);
	}
	
	/**
	 * Returns the set of columns for the completed torrents view
	 * Subclasses my override to return a different set of columns
	 * @return
	 */
	protected TableColumnCore[] getCompleteColumns(){
		return TableColumnCreator.createCompleteDM(TableManager.TABLE_MYTORRENTS_COMPLETE);
	}
	
	
	/**
	 * Returns an instance of <code>MyTorrentsView</code>
	 * Subclasses my override to return a different instance of MyTorrentsView
	 * @param _azureus_core
	 * @param isSeedingView
	 * @param columns
	 * @param child1 
	 * @return
	 */
	protected MyTorrentsView createTorrentView(AzureusCore _azureus_core,
			String tableID, boolean isSeedingView, TableColumnCore[] columns, Composite c) {
		MyTorrentsView view = new MyTorrentsView(_azureus_core, tableID,
				isSeedingView, columns, txtFilter, cCats);
    view.initialize(c);
		c.addListener(SWT.Activate, new Listener() {
			public void handleEvent(Event event) {
				viewActivated();
			}
		});
		c.addListener(SWT.Deactivate, new Listener() {
			public void handleEvent(Event event) {
				viewDeactivated();
			}
		});
		return view;
	}


	public MyTorrentsView getTorrentview() {
		return torrentview;
	}


	public MyTorrentsView getSeedingview() {
		return seedingview;
	}
}