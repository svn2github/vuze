/*
 * Created on 4 mai 2004
 * Created by Olivier Chalouhi
 * 
 * Copyright (C) 2004, 2005, 2006 Aelitis SAS, All rights Reserved
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
 * AELITIS, SAS au capital de 46,603.30 euros,
 * 8 Alle Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */
package org.gudy.azureus2.ui.swt.mainwindow;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.download.DownloadManager;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LogEvent;
import org.gudy.azureus2.core3.logging.LogIDs;
import org.gudy.azureus2.core3.logging.Logger;
import org.gudy.azureus2.core3.predicate.AllPredicate;
import org.gudy.azureus2.core3.predicate.NotPredicate;
import org.gudy.azureus2.core3.predicate.Predicable;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.pluginsimpl.local.download.DownloadManagerImpl;
import org.gudy.azureus2.ui.common.util.MenuItemManager;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.components.shell.ShellManager;
import org.gudy.azureus2.ui.swt.config.wizard.ConfigureWizard;
import org.gudy.azureus2.ui.swt.debug.UIDebugGenerator;
import org.gudy.azureus2.ui.swt.donations.OldDonationWindow;
import org.gudy.azureus2.ui.swt.exporttorrent.wizard.ExportTorrentWizard;
import org.gudy.azureus2.ui.swt.help.AboutWindow;
import org.gudy.azureus2.ui.swt.help.HealthHelpWindow;
import org.gudy.azureus2.ui.swt.importtorrent.wizard.ImportTorrentWizard;
import org.gudy.azureus2.ui.swt.maketorrent.NewTorrentWizard;
import org.gudy.azureus2.ui.swt.minibar.AllTransfersBar;
import org.gudy.azureus2.ui.swt.minibar.MiniBarManager;
import org.gudy.azureus2.ui.swt.nat.NatTestWindow;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.BasicPluginViewImpl;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewImpl;
import org.gudy.azureus2.ui.swt.pluginsinstaller.InstallPluginWizard;
import org.gudy.azureus2.ui.swt.pluginsuninstaller.UnInstallPluginWizard;
import org.gudy.azureus2.ui.swt.predicate.shell.*;
import org.gudy.azureus2.ui.swt.predicate.shellmanager.AllManagedShellsAreMinimizedPredicate;
import org.gudy.azureus2.ui.swt.predicate.shellmanager.ShellManagerIsEmptyPredicate;
import org.gudy.azureus2.ui.swt.sharing.ShareUtils;
import org.gudy.azureus2.ui.swt.speedtest.SpeedTestWizard;
import org.gudy.azureus2.ui.swt.update.UpdateMonitor;
import org.gudy.azureus2.ui.swt.views.AbstractIView;
import org.gudy.azureus2.ui.swt.views.table.TableViewSWT;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;
import org.gudy.azureus2.ui.swt.welcome.WelcomeWindow;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.core.AzureusCoreFactory;
import com.aelitis.azureus.ui.UIFunctions;
import com.aelitis.azureus.ui.UIFunctionsManager;
import com.aelitis.azureus.ui.swt.UIFunctionsManagerSWT;
import com.aelitis.azureus.ui.swt.UIFunctionsSWT;

import org.gudy.azureus2.plugins.update.UpdateCheckInstance;
import org.gudy.azureus2.plugins.update.UpdateCheckInstanceListener;

/**
 * @author Olivier Chalouhi
 * @author James Yeh Accessibility: Changes to allow better validation and unified menu bar state
 */
public class MainMenu {
	public static int MENU_BAR = 0;
	public static int MENU_TRANSFER = 1;
	public static int MENU_VIEW = 2;
	
	private static final LogIDs LOGID = LogIDs.GUI;

  private Display display;
  private MainWindow mainWindow;
  private Shell attachedShell;

  private Menu menuBar;
  
  private MenuItem menu_plugin;
  private Menu transferMenu;
  private Menu viewMenu;
  
  private AEMonitor plugin_view_mon = new AEMonitor("plugin_menu_view_mon");
  private Map plugin_view_info_map = new TreeMap();
  private Map plugin_logs_view_info_map = new TreeMap();
  private AzureusCore core;
  
  private MenuItem torrentItem;
  public void setTorrentMenuContext(Object[] context, final TableViewSWT tv,
			final boolean is_detailed_view) {
	  if (context == null) {context = new Object[0];}
	  final ArrayList result = new ArrayList(context.length);
	  for (int i=0; i<context.length; i++) {
		  if (context[i] instanceof DownloadManager) {result.add(context[i]);}
		  // We don't expect anything else for now.
	  }
	  Utils.execSWTThread(new AERunnable() {
		  public void runSupport() {
			  torrentItem.setData("downloads", (DownloadManager[])result.toArray(new DownloadManager[result.size()]));
			  torrentItem.setData("TableView", tv);
			  torrentItem.setData("is_detailed_view", Boolean.valueOf(is_detailed_view));
			  torrentItem.setEnabled(!result.isEmpty());
		  }
	  }, true); // async
  }

  /**
   * <p>Creates the main menu bar and attaches it to a shell that is not the main window</p>
   * <p>This constructor call is intended to be used with platforms that have a singular menu bar,
   * such as Mac OS X</p>
   * @param shell A shell
   */
  public MainMenu(final Shell shell) {
      mainWindow = MainWindow.getWindow();
      /*if(mainWindow == null)
          throw new IllegalStateException("MainWindow has not initialized yet; Shell attemped: " + shell.hashCode());
      if(shell == mainWindow.getShell())
          throw new IllegalStateException("Invalid MainMenu registration with MainWindow shell; use MainMenu._ctor(MainWindow) instead");*/

      if(mainWindow != null && !mainWindow.getShell().isDisposed() && mainWindow.getShell() != shell)
      {
          this.display = SWTThread.getInstance().getDisplay();
          attachedShell = shell;

          buildMenu(shell, true);
      }
  }

  public MainMenu(final MainWindow mainWindow) {
    this.mainWindow = mainWindow;
    this.display = SWTThread.getInstance().getDisplay();
    attachedShell = mainWindow.getShell();

    buildMenu(mainWindow.getShell(), true);
  }
  
  public void setMainWindow(MainWindow mainWindow) {
    this.mainWindow = mainWindow;
  }

	public void linkMenuBar(Shell parent) {
		parent.setMenuBar(menuBar);
	}

  public void createMenu(AzureusCore core, Shell shell) {
    this.display = SWTThread.getInstance().getDisplay();
    attachedShell = shell;

    this.core = core;
    buildMenu(shell, false);
  }

  /**
   * Populates Azureus' menu bar
   * @param locales
   * @param parent
   */
  private void buildMenu(final Shell parent, boolean linkToParent) {
    try {
    	if (core == null) {
    		core = AzureusCoreFactory.getSingleton();
    	}
      
      //The Main Menu
      menuBar = new Menu(parent, SWT.BAR);

      // one time disable conditions
      final boolean notMainWindow = mainWindow != null && attachedShell != mainWindow.getShell();
      boolean isModal = new ShellIsModalPredicate().evaluate(attachedShell);


      //The File Menu
      MenuItem fileItem = new MenuItem(menuBar, SWT.CASCADE);
      Messages.setLanguageText(fileItem, "MainWindow.menu.file"); //$NON-NLS-1$
      Menu fileMenu = new Menu(parent, SWT.DROP_DOWN);
      fileItem.setMenu(fileMenu);
      if(isModal) {performOneTimeDisable(fileItem, true);}

      MenuItem file_create = new MenuItem(fileMenu, SWT.NULL);
      Messages.setLanguageText(file_create, "MainWindow.menu.file.create"); //$NON-NLS-1$
      KeyBindings.setAccelerator(file_create, "MainWindow.menu.file.create");

      MenuItem file_new = new MenuItem(fileMenu, SWT.CASCADE);
      Messages.setLanguageText(file_new, "MainWindow.menu.file.open"); //$NON-NLS-1$
      
      MenuItem file_share= new MenuItem(fileMenu, SWT.CASCADE);
      Messages.setLanguageText(file_share, "MainWindow.menu.file.share"); //$NON-NLS-1$
  
      new MenuItem(fileMenu, SWT.SEPARATOR);

      MenuItem file_import = new MenuItem(fileMenu, SWT.NULL);
      KeyBindings.setAccelerator(file_import, "MainWindow.menu.file.import");
      Messages.setLanguageText(file_import, "MainWindow.menu.file.import"); //$NON-NLS-1$

      MenuItem file_export = new MenuItem(fileMenu, SWT.NULL);
      KeyBindings.setAccelerator(file_export, "MainWindow.menu.file.export");
      Messages.setLanguageText(file_export, "MainWindow.menu.file.export"); //$NON-NLS-1$

      new MenuItem(fileMenu, SWT.SEPARATOR);

      addCloseWindowMenuItem(fileMenu);
      MenuItem closeTabItem = addCloseTabMenuItem(fileMenu);
      if(notMainWindow) {performOneTimeDisable(closeTabItem, false);}
      addCloseDetailsMenuItem(fileMenu);
      addCloseDownloadBarsToMenu(fileMenu);


      //No need for restart and exit on OS X
      if(!Constants.isOSX) {
        new MenuItem(fileMenu, SWT.SEPARATOR);

        MenuItem file_restart = new MenuItem(fileMenu, SWT.NULL);
        Messages.setLanguageText(file_restart, "MainWindow.menu.file.restart"); //$NON-NLS-1$

        file_restart.addListener(SWT.Selection, new Listener() {

        public void handleEvent(Event event) {
						UIFunctionsManagerSWT.getUIFunctionsSWT().dispose(true, false);
					}
				});

        final MenuItem file_exit = new MenuItem(fileMenu, SWT.NULL);
        if(!COConfigurationManager.getBooleanParameter("Enable System Tray") || !COConfigurationManager.getBooleanParameter("Close To Tray")) {
            KeyBindings.setAccelerator(file_exit, "MainWindow.menu.file.exit");
        }
        Messages.setLanguageText(file_exit, "MainWindow.menu.file.exit"); //$NON-NLS-1$
        
        file_exit.addListener(SWT.Selection, new Listener() {
          public void handleEvent(Event e) {
          	UIFunctionsManagerSWT.getUIFunctionsSWT().dispose(false, false);
          }
        });

        // let platform decide
        ParameterListener paramListener = new ParameterListener() {
          public void parameterChanged(String parameterName) {
              if(COConfigurationManager.getBooleanParameter("Enable System Tray") && COConfigurationManager.getBooleanParameter("Close To Tray")) {
                  KeyBindings.removeAccelerator(file_exit, "MainWindow.menu.file.exit");
              }
              else {
                  KeyBindings.setAccelerator(file_exit, "MainWindow.menu.file.exit");
              }
          }
        };
        COConfigurationManager.addParameterListener("Enable System Tray", paramListener);
        COConfigurationManager.addParameterListener("Close To Tray", paramListener);
      }

      // file->open submenus
      final String PREFIX_FILEOPEN = "MainWindow.menu.file.open."; 
      
      Menu newMenu = new Menu(parent, SWT.DROP_DOWN);
      file_new.setMenu(newMenu);
      
      MenuItem fileOpenTorrent = new MenuItem(newMenu, SWT.NULL);
			Messages.setLanguageText(fileOpenTorrent, PREFIX_FILEOPEN + "torrent");
			KeyBindings.setAccelerator(fileOpenTorrent, PREFIX_FILEOPEN + "torrent");
			fileOpenTorrent.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					TorrentOpener.openTorrentWindow();
				}
			});

    MenuItem file_new_torrent_for_tracking = new MenuItem(newMenu, SWT.NULL);
      KeyBindings.setAccelerator(file_new_torrent_for_tracking, PREFIX_FILEOPEN + "torrentfortracking");
      Messages.setLanguageText(file_new_torrent_for_tracking, PREFIX_FILEOPEN + "torrentfortracking");
	  file_new_torrent_for_tracking.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
          TorrentOpener.openTorrentTrackingOnly();
        }      
      });
		  
      	// file->share submenus
      
      Menu shareMenu = new Menu(parent, SWT.DROP_DOWN);
      file_share.setMenu(shareMenu);
  
      MenuItem file_share_file = new MenuItem(shareMenu, SWT.NULL);
      KeyBindings.setAccelerator(file_share_file, "MainWindow.menu.file.share.file");
      Messages.setLanguageText(file_share_file, "MainWindow.menu.file.share.file");
      file_share_file.addListener(SWT.Selection, new Listener() {
      	public void handleEvent(Event e) {
      		ShareUtils.shareFile(core, parent);
      	}
      });
      
      MenuItem file_share_dir = new MenuItem(shareMenu, SWT.NULL);
      KeyBindings.setAccelerator(file_share_dir, "MainWindow.menu.file.share.dir");
      Messages.setLanguageText(file_share_dir, "MainWindow.menu.file.share.dir");
      file_share_dir.addListener(SWT.Selection, new Listener() {
      	public void handleEvent(Event e) {
      		ShareUtils.shareDir(core, parent);
      	}
      });
      
      MenuItem file_share_dircontents = new MenuItem(shareMenu, SWT.NULL);
      KeyBindings.setAccelerator(file_share_dircontents, "MainWindow.menu.file.share.dircontents");
      Messages.setLanguageText(file_share_dircontents, "MainWindow.menu.file.share.dircontents");
      file_share_dircontents.addListener(SWT.Selection, new Listener() {
      	public void handleEvent(Event e) {
      		ShareUtils.shareDirContents(core, parent, false);
      	}
      });
      MenuItem file_share_dircontents_rec = new MenuItem(shareMenu, SWT.NULL);
      KeyBindings.setAccelerator(file_share_dircontents_rec, "MainWindow.menu.file.share.dircontentsrecursive");
      Messages.setLanguageText(file_share_dircontents_rec, "MainWindow.menu.file.share.dircontentsrecursive");
      file_share_dircontents_rec.addListener(SWT.Selection, new Listener() {
      	public void handleEvent(Event e) {
      		ShareUtils.shareDirContents(core, parent, true);
      	}
      });
         	// file->create
      
      file_create.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
          new NewTorrentWizard(core, display);
        }
      });
      
      file_export.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
          new ExportTorrentWizard(core, display);
        }
      });
  
      file_import.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
          new ImportTorrentWizard(core,display);
        }
      });


        // hig compliance
        if(Constants.isOSX) {
            addViewMenu(parent, notMainWindow);
            addTransferMenu(parent, isModal, notMainWindow);
            createTorrentMenu(parent, isModal);
        }
        else { // previous ordering
            addTransferMenu(parent, isModal, notMainWindow);
            createTorrentMenu(parent, isModal);
            addViewMenu(parent, notMainWindow);
        }

        //the Tools menu
        if(!Constants.isOSX) {
            MenuItem menu_tools = new MenuItem(menuBar,SWT.CASCADE);
            Messages.setLanguageText(menu_tools, "MainWindow.menu.tools"); //$NON-NLS-1$
            Menu toolsMenu = new Menu(parent,SWT.DROP_DOWN);
            menu_tools.setMenu(toolsMenu);

            addBlockedIPsMenuItem(toolsMenu);
            addConsoleMenuItem(toolsMenu);
            addStatisticsMenuItem(toolsMenu);
            addNatTestMenuItem( toolsMenu );
            addSpeedTestMenuItem( toolsMenu );
            
            new MenuItem(toolsMenu, SWT.SEPARATOR);

            addConfigWizardMenuItem(toolsMenu);

            MenuItem view_config = new MenuItem(toolsMenu, SWT.NULL);
            KeyBindings.setAccelerator(view_config, "MainWindow.menu.view.configuration");
            Messages.setLanguageText(view_config, "MainWindow.menu.view.configuration"); //$NON-NLS-1$
            view_config.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event e) {
						UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
						if (uiFunctions != null) {
							uiFunctions.showConfig(null);
						}
					}
				});

            if(isModal) {performOneTimeDisable(menu_tools, true);}
      }
      
      //the Plugins menu
        menu_plugin = new MenuItem(menuBar, SWT.CASCADE);
        Messages.setLanguageText(menu_plugin, "MainWindow.menu.view.plugins"); //$NON-NLS-1$
        Menu pluginMenu = new Menu(parent,SWT.DROP_DOWN);
        menu_plugin.setMenu(pluginMenu);
        if(isModal) {performOneTimeDisable(menu_plugin, true);}
        MenuBuildUtils.addMaintenanceListenerForMenu(pluginMenu, new MenuBuildUtils.MenuBuilder() {
        	public void buildMenu(Menu menu) {
        		buildPluginMenu(parent, menu, notMainWindow);
        	}
        });
      
      // standard items
      if(Constants.isOSX) {
          // Window menu
          final MenuItem menu_window = new MenuItem(menuBar, SWT.CASCADE);
          Messages.setLanguageText(menu_window, "MainWindow.menu.window");
          final Menu windowMenu = new Menu(parent, SWT.DROP_DOWN);
          menu_window.setMenu(windowMenu);
          if(isModal) {performOneTimeDisable(menu_window, true);}

          // minimize, zoom
          addMinimizeWindowMenuItem(windowMenu);
          addZoomWindowMenuItem(windowMenu);
          new MenuItem(windowMenu, SWT.SEPARATOR);
          addBlockedIPsMenuItem(windowMenu);
          new MenuItem(windowMenu, SWT.SEPARATOR);
          addBringAllToFrontMenuItem(windowMenu);
          new MenuItem(windowMenu, SWT.SEPARATOR);

          appendWindowMenuItems(windowMenu);

      }
      
      if (Constants.isCVSVersion()) {
      	addDebugMenu(menuBar);
      }
      
      //The Help Menu
      MenuItem helpItem = new MenuItem(menuBar, SWT.CASCADE);
      Messages.setLanguageText(helpItem, "MainWindow.menu.help"); //$NON-NLS-1$
      final Menu helpMenu = new Menu(parent, SWT.DROP_DOWN);
      helpItem.setMenu(helpMenu);
      if(isModal) {performOneTimeDisable(helpItem, true);}

      if(!Constants.isOSX) {
          MenuItem help_about = new MenuItem(helpMenu, SWT.NULL);
          Messages.setLanguageText(help_about, "MainWindow.menu.help.about"); //$NON-NLS-1$
          help_about.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
              AboutWindow.show(display);
            }
          });
          new MenuItem(helpMenu,SWT.SEPARATOR);
      }

      MenuItem help_health = new MenuItem(helpMenu, SWT.NULL);
      Messages.setLanguageText(help_health, "MyTorrentsView.menu.health");
      help_health.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
          HealthHelpWindow.show( display );
        }
      });
      
      MenuItem help_whatsnew = new MenuItem(helpMenu, SWT.NULL);
      Messages.setLanguageText(help_whatsnew, "MainWindow.menu.help.releasenotes");
      help_whatsnew.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
          new WelcomeWindow(parent);
        }
      });

      MenuItem help_new = new MenuItem(helpMenu, SWT.NULL);
      Messages.setLanguageText(help_new, "MainWindow.menu.help.whatsnew"); //$NON-NLS-1$
      help_new.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
        	Utils.launch("http://azureus.sourceforge.net/changelog.php?version=" + Constants.AZUREUS_VERSION);
        }
      });

      MenuItem help_faq = new MenuItem(helpMenu, SWT.NULL);
      Messages.setLanguageText(help_faq, "MainWindow.menu.help.faq"); //$NON-NLS-1$
      help_faq.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
          String faqString = Constants.AZUREUS_WIKI;
          Utils.launch(faqString);
        }
      });


      MenuItem help_plugin= new MenuItem(helpMenu, SWT.NULL);
      Messages.setLanguageText(help_plugin, "MainWindow.menu.help.plugins"); //$NON-NLS-1$
      help_plugin.addListener(SWT.Selection, new Listener() {
          public void handleEvent(Event e) {
            String pluginString = "http://azureus.sourceforge.net/plugin_list.php";
            Utils.launch(pluginString);
          }
        });
      
      new MenuItem(helpMenu,SWT.SEPARATOR);
      
      if ( !SystemProperties.isJavaWebStartInstance()){
        MenuItem help_checkupdate = new MenuItem(helpMenu, SWT.NULL);
        KeyBindings.setAccelerator(help_checkupdate, "MainWindow.menu.help.checkupdate");
        Messages.setLanguageText(help_checkupdate, "MainWindow.menu.help.checkupdate"); //$NON-NLS-1$
        help_checkupdate.addListener(SWT.Selection, new Listener() {
        	public void handleEvent(Event e) {
        		UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
        		if (uiFunctions != null) {
        			uiFunctions.bringToFront();
        		}
        		UpdateMonitor.getSingleton(core).performCheck(true, false, false, new UpdateCheckInstanceListener() {
        			public void cancelled(UpdateCheckInstance instance) {
        			}
        			
        			public void complete(UpdateCheckInstance instance) {
        				if (instance.getUpdates().length == 0) {
        					Utils.execSWTThread(new AERunnable() {
        						public void runSupport() {
            					Utils.openMessageBox(parent,
													SWT.ICON_INFORMATION | SWT.OK,
													"window.update.noupdates", (String[]) null);
        						}
        					});
        				}
        			}
        		});
        	}
        });
      }

      /*
      MenuItem help_donate = new MenuItem(helpMenu, SWT.NULL);
      Messages.setLanguageText(help_donate, "MainWindow.menu.help.donate");
      help_donate.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
          new OldDonationWindow(display).show();
        }
      });
      */
      
      new MenuItem(helpMenu,SWT.SEPARATOR);
      MenuItem help_debug = new MenuItem(helpMenu, SWT.NULL);
      Messages.setLanguageText(help_debug, "MainWindow.menu.help.debug");
      help_debug.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
        	UIDebugGenerator.generate();
        }
      });
      
      /*
      new MenuItem(helpMenu,SWT.SEPARATOR);
      MenuItem testMenu = new MenuItem(helpMenu, SWT.NULL);
      testMenu.setText("Test");
      testMenu.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event arg0) {
          final Shell shell = helpMenu.getShell();
          AEThread runner = new AEThread("test list") {
            public void runSupport() {
              StringListChooser chooser = new StringListChooser(shell);
              chooser.setTitle("Test Dialog");
              chooser.setText("This is a test of a list choose dialog.\nPlease choose an item from the following list : ");
              chooser.addOption("Option 1 : SWT");
              chooser.addOption("Option 2 : Java");
              chooser.addOption("Option 3 : I am alive");          
              System.out.println("Result =" + chooser.open());
            }
          };
          runner.start();
          
        }
      });
      */
      
    } catch (Exception e) {
    	Logger.log(new LogEvent(LOGID, "Error while creating menu items", e));
    }
    
    if (linkToParent) {
    	parent.setMenuBar(menuBar);
    }
  }
  
  private void createIViewInfoMenuItem(Menu parent, final IViewInfo info) {
	  MenuItem item = new MenuItem(parent, SWT.NULL);
	  item.setText(info.name);
	  if (info.viewID != null) {item.setData("ViewID", info.viewID);}
      item.addListener(SWT.Selection,new Listener() {
    	  public void handleEvent(Event e) {
    		  UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
    		  if (uiFunctions != null) {info.openView(uiFunctions);}
    	  }
      });
  }
  
  private void createIViewInfoMenuItems(Menu parent, Map menu_data) {
	  Iterator itr = menu_data.values().iterator();
	  while (itr.hasNext()) {
		  createIViewInfoMenuItem(parent, (IViewInfo)itr.next());
	  }
  }
  
  private void createTorrentMenu(final Shell parent, boolean isModal) {
      // The Torrents menu.
      torrentItem = new MenuItem(menuBar, SWT.CASCADE);
      Messages.setLanguageText(torrentItem, "MainWindow.menu.torrent");
      final Menu torrentMenu = new Menu(parent, SWT.DROP_DOWN);
      torrentItem.setMenu(torrentMenu);
      if(isModal) {performOneTimeDisable(torrentItem, true);}
      MenuBuildUtils.addMaintenanceListenerForMenu(torrentMenu, new MenuBuildUtils.MenuBuilder() {
      	public void buildMenu(Menu menu) {
      		DownloadManager[] current_dls = (DownloadManager[])torrentItem.getData("downloads");
      		if (current_dls == null) {return;}
      		boolean is_detailed_view = ((Boolean)torrentItem.getData("is_detailed_view")).booleanValue();
      		TableViewSWT tv = (TableViewSWT) torrentItem.getData("TableView");
						TorrentUtil.fillTorrentMenu(menu, current_dls, core, attachedShell,
								!is_detailed_view, 0, tv);
    		org.gudy.azureus2.plugins.ui.menus.MenuItem[] menu_items;
    		menu_items = MenuItemManager.getInstance().getAllAsArray(
    				new String[] {"torrentmenu", "download_context"}
    		);
    		if (menu_items.length > 0) {
        		new MenuItem(menu, SWT.SEPARATOR);
    			Object[] plugin_dls = DownloadManagerImpl.getDownloadStatic(current_dls);
    			MenuBuildUtils.addPluginMenuItems(parent, menu_items, menu, true, true,
						new MenuBuildUtils.MenuItemPluginMenuControllerImpl(plugin_dls)
    			);
    		}
      	}
      });
  }
  
  private void buildPluginMenu(Shell parent, Menu pluginMenu, boolean notMainWindow) {
      //menu_plugin.setEnabled(false);
      //if(notMainWindow) {performOneTimeDisable(menu_plugin, true);}
      
      try {
    	  this.plugin_view_mon.enter();
    	  createIViewInfoMenuItems(pluginMenu, this.plugin_view_info_map);
    	    MenuItem menu_plugin_logViews = new MenuItem(pluginMenu, SWT.CASCADE);
			Messages.setLanguageText(menu_plugin_logViews, "MainWindow.menu.view.plugins.logViews");
			Menu pluginLogsMenu = new Menu(parent, SWT.DROP_DOWN);
			menu_plugin_logViews.setMenu(pluginLogsMenu);
			createIViewInfoMenuItems(pluginLogsMenu, this.plugin_logs_view_info_map);
      }
      finally {
    	  this.plugin_view_mon.exit();
      }
      
    new MenuItem(pluginMenu, SWT.SEPARATOR);
    
    org.gudy.azureus2.plugins.ui.menus.MenuItem[] plugin_items;
    plugin_items = MenuItemManager.getInstance().getAllAsArray("mainmenu");
    if (plugin_items.length > 0) {
    	MenuBuildUtils.addPluginMenuItems(parent, plugin_items, pluginMenu, true, true,
    		MenuBuildUtils.BASIC_MENU_ITEM_CONTROLLER
    	);
    	new MenuItem(pluginMenu, SWT.SEPARATOR);
    }
    
    MenuItem plugins_install_wizard = new MenuItem(pluginMenu, SWT.NULL);
    KeyBindings.setAccelerator(plugins_install_wizard, "MainWindow.menu.plugins.installPlugins");
    Messages.setLanguageText(plugins_install_wizard, "MainWindow.menu.plugins.installPlugins"); //$NON-NLS-1$
    plugins_install_wizard.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        new InstallPluginWizard(core, display);
      }
    });
    
    MenuItem plugins_uninstall_wizard = new MenuItem(pluginMenu, SWT.NULL);
    KeyBindings.setAccelerator(plugins_uninstall_wizard, "MainWindow.menu.plugins.uninstallPlugins");
    Messages.setLanguageText(plugins_uninstall_wizard, "MainWindow.menu.plugins.uninstallPlugins"); //$NON-NLS-1$
    plugins_uninstall_wizard.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        new UnInstallPluginWizard(core, display);
      }
    });
  
  }

	private void addDebugMenu(Menu menu) {
		MenuItem item;

		item = new MenuItem(menu, SWT.CASCADE);
		item.setText("Debug");
		Menu menuDebug = new Menu(menu.getParent(), SWT.DROP_DOWN);
		item.setMenu(menuDebug);

		item = new MenuItem(menuDebug, SWT.CASCADE);
		item.setText("ScreenSize");
		Menu menuSS = new Menu(menu.getParent(), SWT.DROP_DOWN);
		item.setMenu(menuSS);
		
		item = new MenuItem(menuSS, SWT.NONE);
		item.setText("640x400");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				mainWindow.getShell().setSize(640, 400);
			}
		});

	
		item = new MenuItem(menuSS, SWT.NONE);
		item.setText("800x560");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				mainWindow.getShell().setSize(850, 560);
			}
		});

	
		item = new MenuItem(menuSS, SWT.NONE);
		item.setText("1024x700");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				mainWindow.getShell().setSize(1024, 700);
			}
		});

	
		item = new MenuItem(menuSS, SWT.NONE);
		item.setText("1280x980");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				mainWindow.getShell().setSize(1280, 980);
			}
		});
		
		item = new MenuItem(menuDebug, SWT.NONE);
		item.setText("Reload messagebundle");
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				MessageText.loadBundle(true);
		        DisplayFormatters.setUnits();
		        DisplayFormatters.loadMessages();
		        UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
		        if (uiFunctions != null) {
		        	uiFunctions.refreshLanguage();    
		        }
			}
		});
		

	
	}

	private void addTransferMenu(final Shell parent, boolean modal, boolean notMainWindow)
  {
      // ******** The Download Menu

      MenuItem downloadItem = new MenuItem(menuBar, SWT.CASCADE);
      Messages.setLanguageText(downloadItem, "MainWindow.menu.transfers"); //$NON-NLS-1$
      transferMenu = new Menu(parent, SWT.DROP_DOWN);
      downloadItem.setMenu(transferMenu);
      if(modal) {performOneTimeDisable(downloadItem, true);}



        // new MenuItem(fileMenu,SWT.SEPARATOR);

      final MenuItem itemStartAll = new MenuItem(transferMenu,SWT.NULL);
      KeyBindings.setAccelerator(itemStartAll, "MainWindow.menu.transfers.startalltransfers");
      Messages.setLanguageText(itemStartAll,"MainWindow.menu.transfers.startalltransfers");

      final MenuItem itemStopAll = new MenuItem(transferMenu,SWT.NULL);
      KeyBindings.setAccelerator(itemStopAll, "MainWindow.menu.transfers.stopalltransfers");
      Messages.setLanguageText(itemStopAll,"MainWindow.menu.transfers.stopalltransfers");

      final MenuItem itemPause = new MenuItem(transferMenu,SWT.NULL);
      KeyBindings.setAccelerator(itemPause, "MainWindow.menu.transfers.pausetransfers");
      Messages.setLanguageText(itemPause,"MainWindow.menu.transfers.pausetransfers");
      if(notMainWindow) {performOneTimeDisable(itemPause, true);}

      final MenuItem itemResume = new MenuItem(transferMenu,SWT.NULL);
      KeyBindings.setAccelerator(itemResume, "MainWindow.menu.transfers.resumetransfers");
      Messages.setLanguageText(itemResume,"MainWindow.menu.transfers.resumetransfers");
      if(notMainWindow) {performOneTimeDisable(itemResume, true);}

      itemStartAll.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event arg0) {
        	core.getGlobalManager().startAllDownloads();
        }
      });

      itemStopAll.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event arg0) {
            ManagerUtils.asyncStopAll();
        }
      });

      itemPause.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event arg0)
        {
            ManagerUtils.asyncPause();
        }
      });

      itemResume.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event arg0)
        {
        	ManagerUtils.asyncResume();
        }
      });

      transferMenu.addMenuListener(
          new MenuListener() {
                public void
                menuShown(MenuEvent menu)
                {
                    itemPause.setEnabled( core.getGlobalManager().canPauseDownloads() );

                    itemResume.setEnabled( core.getGlobalManager().canResumeDownloads() );
                }

                public void
                menuHidden(MenuEvent	menu )
                {
                }
          });
  }

  private void addViewMenu(final Shell parent, boolean notMainWindow)
  {
  	try {
      // ******** The View Menu
      MenuItem viewItem = new MenuItem(menuBar, SWT.CASCADE);
      Messages.setLanguageText(viewItem, "MainWindow.menu.view"); //$NON-NLS-1$
      viewMenu = new Menu(parent, SWT.DROP_DOWN);
      viewItem.setMenu(viewMenu);
      if(notMainWindow) {performOneTimeDisable(viewItem, true);}

      addMenuItemLabel(viewMenu, "MainWindow.menu.view.show");
      indent(addMyTorrentsMenuItem(viewMenu));
      indent(addMyTrackerMenuItem(viewMenu));
      indent(addMySharesMenuItem(viewMenu));
      indent(addViewToolbarMenuItem(viewMenu));
      indent(addTransferBarToMenu(viewMenu));
      indent(addAllPeersMenuItem(viewMenu));
      
      if(Constants.isOSX) {
          indent(addConsoleMenuItem(viewMenu));
          indent(addStatisticsMenuItem(viewMenu));
      }
  	} catch (Exception e) {
  		Debug.out("Error creating View Menu", e);
  	}
  }
  
  protected void addPluginView(String sViewID, UISWTViewEventListener l) {
	  IViewInfo view_info = new IViewInfo();
	  view_info.viewID = sViewID;
	  view_info.event_listener = l;
	  
	  String sResourceID = UISWTViewImpl.CFG_PREFIX + sViewID + ".title";
	  boolean bResourceExists = MessageText.keyExists(sResourceID);
		
	  String name;
		
	  if (bResourceExists){
          name = MessageText.getString(sResourceID);
      } else {
			// try plain resource
			sResourceID	= sViewID;
			bResourceExists = MessageText.keyExists(sResourceID);
			
			if ( bResourceExists){
				name = MessageText.getString(sResourceID);
			}else{
				name = sViewID.replace('.', ' ' );	// support old plugins
			}
		}
	  
	  view_info.name = name;
	  
	  Map map_to_use = (l instanceof BasicPluginViewImpl) ? this.plugin_logs_view_info_map : this.plugin_view_info_map;
		
	  try {
		  plugin_view_mon.enter();
		  map_to_use.put(name, view_info);
	  }
	  finally {
		  plugin_view_mon.exit();  
	  }
  }
  
  
  private void removePluginViewsWithID(String sViewID, Map map) {
	  if (sViewID == null) {return;}
	  Iterator itr = map.values().iterator();
	  IViewInfo view_info = null;
	  while (itr.hasNext()) {
		  view_info = (IViewInfo)itr.next();
		  if (sViewID.equals(view_info.viewID)) {
			  itr.remove();
		  }
	  }
  }
  
  protected void removePluginViews(final String sViewID) {
	  try {
		  plugin_view_mon.enter();
		  removePluginViewsWithID(sViewID, plugin_view_info_map);
		  removePluginViewsWithID(sViewID, plugin_logs_view_info_map);
	  }
	  finally {
		  plugin_view_mon.exit();
	  }
	  Utils.execSWTThread(new AERunnable() {
		  public void runSupport() {
				UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
				if (uiFunctions != null) {
					uiFunctions.closePluginViews(sViewID);
				}
		  }
	  });
  }

 
  protected void addPluginView(final AbstractIView view, final String name) {
	  IViewInfo view_info = new IViewInfo();
	  view_info.name = name;
	  view_info.view = view;
	  try {
		  plugin_view_mon.enter();
		  plugin_view_info_map.put(name, view_info);
	  }
	  finally {
		  plugin_view_mon.exit();  
	  }
  }
  
  protected void
  removePluginView(
  	final AbstractIView 	view,
  	final String			name )
  {
	  IViewInfo view_info = null;
	  try {
		  plugin_view_mon.enter();
		  view_info = (IViewInfo)this.plugin_view_info_map.remove(name);
	  }
	  finally {
		  plugin_view_mon.exit();
	  }
	  
	  if (view_info != null) {
		  Utils.execSWTThread(new AERunnable() {
			  public void runSupport() {
				  UIFunctionsSWT uiFunctions = UIFunctionsManagerSWT.getUIFunctionsSWT();
  					if (uiFunctions != null) {
  						uiFunctions.closePluginView(view);
  					}
			  }
		  });
	  }
  }

  /**
   * Appends the list of opened interactive windows to the bottom of the specified parent menu
   * @param windowMenu The parent menu
   */
  private void appendWindowMenuItems(final Menu windowMenu)
  {
      final int numTopItems = windowMenu.getItemCount();
      Listener rebuild = new Listener() {
          public void handleEvent(Event event) {
          	try {
              if(windowMenu.isDisposed() || attachedShell.isDisposed())
                  return;

              final int size = ShellManager.sharedManager().getSize();
              if(size == windowMenu.getItemCount() - numTopItems)
              {
                  for(int i = numTopItems; i < windowMenu.getItemCount(); i++)
                  {
                      final MenuItem item = windowMenu.getItem(i);
                      item.setSelection(item.getData() == attachedShell);
                  }
                  return;
              }

              for(int i = numTopItems; i < windowMenu.getItemCount();)
                windowMenu.getItem(i).dispose();

              Iterator iter = ShellManager.sharedManager().getWindows();
              for(int i = 0; i < size; i++)
              {
                  final Shell sh = (Shell)iter.next();

                  if(sh.isDisposed() || sh.getText().length() == 0)
                     continue;

                  final MenuItem item = new MenuItem(windowMenu, SWT.CHECK);

                  item.setText(sh.getText());
                  item.setSelection(attachedShell == sh);
                  item.setData(sh);
                  
                  item.addSelectionListener(new SelectionAdapter()
                  {
                      public void widgetSelected(SelectionEvent event)
                      {
                          if(event.widget.isDisposed() || sh.isDisposed())
                              return;

                          if(sh.getMinimized())
                              sh.setMinimized(false);

                          sh.open();
                      }
                  });
              }
          	} catch (Exception e) {
          		Logger.log(new LogEvent(LogIDs.GUI, "rebuild menu error", e));
          	}
          }
      };

      ShellManager.sharedManager().addWindowAddedListener(rebuild);
      ShellManager.sharedManager().addWindowRemovedListener(rebuild);
      attachedShell.addListener(SWT.FocusIn, rebuild);
      windowMenu.addListener(SWT.Show, rebuild);
  }

    // individual menu items

  private static final MenuItem addMenuItem(Menu menu, String localizationKey, Listener selListener) {
  	return addMenuItem(menu, SWT.NONE, localizationKey, selListener);
  }

  private static final MenuItem addMenuItem(Menu menu, int style, String localizationKey, Listener selListener) {
      MenuItem item = new MenuItem(menu, style);
      Messages.setLanguageText(item, localizationKey);
      KeyBindings.setAccelerator(item, localizationKey);
      item.addListener(SWT.Selection, selListener);
      return item;
  }

  private static final void indent(MenuItem item) {
      item.setData("IndentItem", "YES");
      item.setText("  " + item.getText());
  }

  private static final MenuItem addMenuItemLabel(Menu menu, String localizationKey) {
      MenuItem item = new MenuItem(menu, SWT.NULL);
      Messages.setLanguageText(item, localizationKey);
      item.setEnabled(false);
      return item;
  }

  private MenuItem addMyTorrentsMenuItem(Menu menu) {
      return addMenuItem(menu, "MainWindow.menu.view.mytorrents", new Listener() {
        public void handleEvent(Event e) {
        	UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
  				if (uiFunctions != null) {
    				uiFunctions.showMyTorrents();
  				}
        }
      });
  }

  private MenuItem addAllPeersMenuItem(Menu menu) {
      return addMenuItem(menu, "MainWindow.menu.view.allpeers", new Listener() {
        public void handleEvent(Event e) {
        	UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
  				if (uiFunctions != null) {
    				uiFunctions.showAllPeersView();
  				}
        }
      });
  }
  
  private MenuItem addMyTrackerMenuItem(Menu menu)
    {
        return addMenuItem(menu, "MainWindow.menu.view.mytracker", new Listener() {
          public void handleEvent(Event e) {
          	UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
    				if (uiFunctions != null) {
      				uiFunctions.showMyTracker();
    				}
          }
        });
    }

  private MenuItem addMySharesMenuItem(Menu menu)
  {
      return addMenuItem(menu, "MainWindow.menu.view.myshares", new Listener() {
          public void handleEvent(Event e) {
          	UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
    				if (uiFunctions != null) {
      				uiFunctions.showMyShares();
    				}
          }
      });
  }
  
  private MenuItem addViewToolbarMenuItem(Menu menu)
  {
		final MenuItem item = addMenuItem(menu, SWT.CHECK,
				"MainWindow.menu.view.iconbar",
				new Listener() {
					public void handleEvent(Event e) {
						mainWindow.setIconBarEnabled(!mainWindow.getIconBarEnabled());
					}
				});

		final ParameterListener listener = new ParameterListener() {
			public void parameterChanged(String parameterName) {
				item.setSelection(COConfigurationManager.getBooleanParameter(parameterName));
			}
		};

		COConfigurationManager.addAndFireParameterListener("IconBar.enabled",
				listener);
		item.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				COConfigurationManager.removeParameterListener("IconBar.enabled",
						listener);
			}
		});
		return item;
	}
  

    private MenuItem addConsoleMenuItem(Menu menu) {
       return addMenuItem(menu, "MainWindow.menu.view.console", new Listener() {
          public void handleEvent(Event e) {
          	UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
						if (uiFunctions != null) {
							uiFunctions.showConsole();
						}
          }
        });
    }

    private MenuItem addStatisticsMenuItem(Menu menu) {
       return addMenuItem(menu, "MainWindow.menu.view.stats", new Listener() {
          public void handleEvent(Event e) {
          	UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
						if (uiFunctions != null) {
							uiFunctions.showStats();
						}
          }
        });
    }
    
    
    private MenuItem addNatTestMenuItem( Menu menu ) {
    	return addMenuItem(menu, "MainWindow.menu.tools.nattest", new Listener() {
        public void handleEvent(Event e) {
        	new NatTestWindow();
        }
      });    	
    }
    
    private MenuItem addSpeedTestMenuItem( Menu menu ) {
    	return addMenuItem(menu, "MainWindow.menu.tools.speedtest", new Listener() {
        public void handleEvent(Event e) {
        	new SpeedTestWizard(core, display);
        }
      });    	
    }

    private MenuItem addConfigWizardMenuItem(Menu menu) {
        return addMenuItem(menu, "MainWindow.menu.file.configure", new Listener() {
          public void handleEvent(Event e) {
            new ConfigureWizard(core, false);
          }
        });
    }

   private MenuItem addCloseDetailsMenuItem(Menu menu) {
       final MenuItem item = addMenuItem(menu, "MainWindow.menu.closealldetails", new Listener() {
         public void handleEvent(Event e) {
           Tab.closeAllDetails();
         }
      });

       Listener enableHandler = new Listener() {
        public void handleEvent(Event event) {
            if(!item.isDisposed() && !event.widget.isDisposed())
                item.setEnabled(mainWindow != null && mainWindow.getShell() == attachedShell && Tab.hasDetails());
        }
      };

      menu.addListener(SWT.Show,  enableHandler);
      attachedShell.addListener(SWT.FocusIn,  enableHandler);
      Tab.addTabAddedListener(enableHandler);
      Tab.addTabRemovedListener(enableHandler);

      return item;
  }

  private MenuItem addCloseWindowMenuItem(Menu menu) {
      final boolean isMainWindow = mainWindow != null && mainWindow.getShell() == attachedShell;
      MenuItem item = addMenuItem(menu, "MainWindow.menu.file.closewindow", new Listener() {
          public void handleEvent(Event event) {
              if(isMainWindow)
              {
                  if(MainWindow.isAlreadyDead) {return;}

                  mainWindow.close();
              }
              else if(attachedShell != null && !attachedShell.isDisposed())
              {
                  attachedShell.close();
              }
          }
      });

      if(!isMainWindow)
      {
          String oldText = item.getText();
          KeyBindings.setAccelerator(item, "MainWindow.menu.file.closetab");
          item.setText(oldText);
      }
      return item;
  }

  private MenuItem addCloseTabMenuItem(Menu menu) {
      return addMenuItem(menu, "MainWindow.menu.file.closetab", new Listener() {
          public void handleEvent(Event event) {
              if(MainWindow.isAlreadyDead) {return;}

              mainWindow.closeViewOrWindow();
          }
      });
  }

  private MenuItem addMinimizeWindowMenuItem(Menu menu) {
      final Predicable pred = new AllPredicate(new Predicable[]{new ShellCanMinimizePredicate(), new NotPredicate(new ShellIsMinimizedPredicate())});
      final MenuItem item = addMenuItem(menu, "MainWindow.menu.window.minimize", new Listener() {
          public void handleEvent(Event event) {
              if(attachedShell.isDisposed()) {event.doit = false; return;}

              attachedShell.setMinimized(true);
          }
      });

      Listener enableHandler = getEnableHandler(item, pred, attachedShell);

      menu.addListener(SWT.Show, enableHandler);
      attachedShell.addListener(SWT.FocusIn,  enableHandler);
      attachedShell.addListener(SWT.Iconify, enableHandler);
      attachedShell.addListener(SWT.Deiconify, enableHandler);

      return item;
  }

  private MenuItem addZoomWindowMenuItem(Menu menu) {
      final Predicable pred = new AllPredicate(new Predicable[]{new ShellCanMaximizePredicate(), new NotPredicate(new ShellIsMinimizedPredicate())});
      final MenuItem item = addMenuItem(menu, "MainWindow.menu.window.zoom", new Listener() {
          public void handleEvent(Event event) {
              if(attachedShell.isDisposed()) {event.doit = false; return;}

              attachedShell.setMaximized(!attachedShell.getMaximized());
          }
      });

      Listener enableHandler = getEnableHandler(item, pred, attachedShell);

      menu.addListener(SWT.Show, enableHandler);
      attachedShell.addListener(SWT.FocusIn,  enableHandler);
      attachedShell.addListener(SWT.Iconify, enableHandler);
      attachedShell.addListener(SWT.Deiconify, enableHandler);

      return item;
  }

  private MenuItem addBringAllToFrontMenuItem(Menu menu) {
      final Predicable pred = new NotPredicate(new AllManagedShellsAreMinimizedPredicate());
      final MenuItem item = addMenuItem(menu, "MainWindow.menu.window.alltofront", new Listener() {
          public void handleEvent(Event event) {
              Iterator iter = ShellManager.sharedManager().getWindows();
              while (iter.hasNext())
              {
                  Shell shell = (Shell) iter.next();
                  if(!shell.isDisposed() && !shell.getMinimized())
                      shell.open();
              }
          }
      });

      Listener enableHandler = getEnableHandler(item, pred, ShellManager.sharedManager());

      menu.addListener(SWT.Show, enableHandler);
      attachedShell.addListener(SWT.FocusIn,  enableHandler);
      setHandlerForShellManager(item, ShellManager.sharedManager(), enableHandler);

      return item;
  }

    private MenuItem addBlockedIPsMenuItem(Menu menu) {
      return addMenuItem(menu, "MainWindow.menu.view.ipFilter", new Listener() {
          public void handleEvent(Event event) {
              if(MainWindow.isAlreadyDead) {return;}

              BlockedIpsWindow.showBlockedIps(core, mainWindow.getShell());
          }
      });
  }

  public MenuItem addCloseDownloadBarsToMenu(Menu menu) {
    final MenuItem item = addMenuItem(menu, "MainWindow.menu.closealldownloadbars", new Listener() {
      public void handleEvent(Event e) {
    	  MiniBarManager.getManager().closeAll();
      }
    });

    final NotPredicate pred = new NotPredicate(new ShellManagerIsEmptyPredicate());
    final Listener enableHandler = getEnableHandler(item, pred, MiniBarManager.getManager().getShellManager());

    menu.addListener(SWT.Show, enableHandler);
    attachedShell.addListener(SWT.FocusIn,  enableHandler);
    setHandlerForShellManager(item, MiniBarManager.getManager().getShellManager(), enableHandler);

    return item;
  }

	private MenuItem addTransferBarToMenu(Menu menu) {
		final MenuItem item = addMenuItem(menu, SWT.CHECK,
			"MainWindow.menu.view.open_global_transfer_bar",
			new Listener() {
				public void handleEvent(Event e) {
					if (AllTransfersBar.getManager().isOpen(core.getGlobalManager())) {
						AllTransfersBar.close(core.getGlobalManager());
					}
					else {
						AllTransfersBar.open(core.getGlobalManager(), attachedShell);
					}
				}
			});

	    final NotPredicate pred = new NotPredicate(new ShellManagerIsEmptyPredicate());
	    final Listener selectHandler = getSelectionHandler(item, pred, MiniBarManager.getManager().getShellManager());
	    menu.addListener(SWT.Show, selectHandler);
	    return item;
	}
  
    // utility methods

  public static void updateMenuText(Object menu) {
    if (menu == null)
      return;
    if (menu instanceof Menu) {
      MenuItem[] menus = ((Menu) menu).getItems();
      for (int i = 0; i < menus.length; i++) {
        updateMenuText(menus[i]);
      }
    }
    else if (menu instanceof MenuItem) {
      MenuItem item = (MenuItem) menu;
      if (item.getData() != null) {
        if (item.getData() instanceof String) {
          item.setText(MessageText.getString((String) item.getData()));
          updateMenuText(item.getMenu());
        }
      }
    }
  }
  
  
  private static void setHandlerForShellManager(MenuItem item, final ShellManager mgr, final Listener evtHandler)
  {
      mgr.addWindowAddedListener(evtHandler);
      mgr.addWindowRemovedListener(evtHandler);
      item.addDisposeListener(new DisposeListener()
      {
          public void widgetDisposed(DisposeEvent event) {
              mgr.removeWindowAddedListener(evtHandler);
              mgr.removeWindowRemovedListener(evtHandler);
          }
      });
  }

  private static Listener getEnableHandler(final MenuItem item, final Predicable pred, final Object evalObj)
  {
      Listener enableHandler = new Listener() {
        public void handleEvent(Event event) {
            if(!item.isDisposed() && !event.widget.isDisposed())
              item.setEnabled(pred.evaluate(evalObj));
        }
      };
      return enableHandler;
  }
  
  private static Listener getSelectionHandler(final MenuItem item, final Predicable pred, final Object evalObj)
  {
      Listener enableHandler = new Listener() {
        public void handleEvent(Event event) {
            if(!item.isDisposed() && !event.widget.isDisposed())
              item.setSelection(pred.evaluate(evalObj));
        }
      };
      return enableHandler;
  }

  private void performOneTimeDisable(MenuItem item, boolean affectsChildMenuItems)
  {
      item.setEnabled(false);
      if(affectsChildMenuItems)
      {
          Menu childMenu = item.getMenu();
          if(childMenu == null)
            return;

          for(int i = 0; i < childMenu.getItemCount(); i++)
          {
              childMenu.getItem(i).setEnabled(false);
          }
      }
  }

	public Menu getMenu(int id) {
		if (id == MENU_BAR) {
			return menuBar;
		}
		if (id == MENU_TRANSFER) {
			return transferMenu;
		}
		if (id == MENU_VIEW) {
			return viewMenu;
		}

		return null;
	}
	
	private static class IViewInfo {
		public AbstractIView view;
		public String name;
		public String viewID;
		public UISWTViewEventListener event_listener;
		
		public void openView(UIFunctionsSWT uiFunctions) {
			if (event_listener != null) {
				uiFunctions.openPluginView(UISWTInstance.VIEW_MAIN, viewID, event_listener,	null, true);
			}
			else {
				uiFunctions.openPluginView(view, name);
			}
		}
		
	}
	
}


