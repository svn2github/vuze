/*
 * Created on 4 mai 2004
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.LGLogger;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.plugins.PluginView;
import org.gudy.azureus2.ui.swt.KeyBindings;
import org.gudy.azureus2.ui.swt.Messages;
import org.gudy.azureus2.ui.swt.Tab;
import org.gudy.azureus2.ui.swt.BlockedIpsWindow;
import org.gudy.azureus2.ui.swt.config.wizard.ConfigureWizard;
import org.gudy.azureus2.ui.swt.donations.DonationWindow2;
import org.gudy.azureus2.ui.swt.exporttorrent.wizard.ExportTorrentWizard;
import org.gudy.azureus2.ui.swt.help.AboutWindow;
import org.gudy.azureus2.ui.swt.help.HealthHelpWindow;
import org.gudy.azureus2.ui.swt.importtorrent.wizard.ImportTorrentWizard;
import org.gudy.azureus2.ui.swt.maketorrent.NewTorrentWizard;
import org.gudy.azureus2.ui.swt.pluginsinstaller.InstallPluginWizard;
import org.gudy.azureus2.ui.swt.pluginsuninstaller.UnInstallPluginWizard;
import org.gudy.azureus2.ui.swt.sharing.ShareUtils;
import org.gudy.azureus2.ui.swt.update.UpdateMonitor;

import java.util.Locale;

/**
 * @author Olivier Chalouhi
 *
 */
public class MainMenu {

  private Display display;
  private MainWindow mainWindow;
    
  private Menu menuBar;
  
  private MenuItem menu_plugin;
  private Menu pluginMenu;
  
  
  public MainMenu(MainWindow mainWindow) {
    this.mainWindow = mainWindow;
    this.display = SWTThread.getInstance().getDisplay();
  }

  /**
   * Populates Azureus' menu bar
   * @param locales
   */
  public void buildMenu(Locale[] locales) {
    try {
      
      //The Main Menu
      menuBar = new Menu(mainWindow.getShell(), SWT.BAR);
      mainWindow.getShell().setMenuBar(menuBar);
      
      
      //The File Menu
      MenuItem fileItem = new MenuItem(menuBar, SWT.CASCADE);
      Messages.setLanguageText(fileItem, "MainWindow.menu.file"); //$NON-NLS-1$
      Menu fileMenu = new Menu(mainWindow.getShell(), SWT.DROP_DOWN);
      fileItem.setMenu(fileMenu);

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
      addCloseTabMenuItem(fileMenu);
      addCloseDetailsMenuItem(fileMenu);
      addCloseDownloadBarsToMenu(fileMenu);



      //No need for restart and exit on OS X
      if(!Constants.isOSX) {
        new MenuItem(fileMenu, SWT.SEPARATOR);

        MenuItem file_restart = new MenuItem(fileMenu, SWT.NULL);
        Messages.setLanguageText(file_restart, "MainWindow.menu.file.restart"); //$NON-NLS-1$

        file_restart.addListener(SWT.Selection, new Listener() {

        public void handleEvent(Event event) {

            MainWindow.getWindow().dispose(true,false);
         }
        });

        MenuItem file_exit = new MenuItem(fileMenu, SWT.NULL);
        KeyBindings.setAccelerator(file_exit, "MainWindow.menu.file.exit");
        Messages.setLanguageText(file_exit, "MainWindow.menu.file.exit"); //$NON-NLS-1$
        
        file_exit.addListener(SWT.Selection, new Listener() {
          public void handleEvent(Event e) {
            mainWindow.dispose(false,false);
          }
        });
      }
      
      // file->open submenus
      
      Menu newMenu = new Menu(mainWindow.getShell(), SWT.DROP_DOWN);
      file_new.setMenu(newMenu);
  
      MenuItem file_new_torrent = new MenuItem(newMenu, SWT.NULL);
      KeyBindings.setAccelerator(file_new_torrent, "MainWindow.menu.file.open.torrent");
      Messages.setLanguageText(file_new_torrent, "MainWindow.menu.file.open.torrent"); //$NON-NLS-1$
      file_new_torrent.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
          TorrentOpener.openTorrent();
        }
      });
  
      MenuItem file_new_torrent_no_default = new MenuItem(newMenu, SWT.NULL);
      KeyBindings.setAccelerator(file_new_torrent_no_default, "MainWindow.menu.file.open.torrentnodefault");
      Messages.setLanguageText(file_new_torrent_no_default, "MainWindow.menu.file.open.torrentnodefault"); //$NON-NLS-1$
      file_new_torrent_no_default.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
          TorrentOpener.openTorrentNoDefaultSave(false);
        }      
      });
  
      MenuItem file_new_torrent_for_seeding = new MenuItem(newMenu, SWT.NULL);
      KeyBindings.setAccelerator(file_new_torrent_for_seeding, "MainWindow.menu.file.open.torrentforseeding");
      Messages.setLanguageText(file_new_torrent_for_seeding, "MainWindow.menu.file.open.torrentforseeding"); //$NON-NLS-1$
      file_new_torrent_for_seeding.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
          TorrentOpener.openTorrentNoDefaultSave(true);
        }      
      });
  
      MenuItem file_new_url = new MenuItem(newMenu,SWT.NULL);
      KeyBindings.setAccelerator(file_new_url, "MainWindow.menu.file.open.url");
      Messages.setLanguageText(file_new_url, "MainWindow.menu.file.open.url"); //$NON-NLS-1$
      file_new_url.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
          TorrentOpener.openUrl(mainWindow.getAzureusCore());
        }
      });
      MenuItem file_new_folder = new MenuItem(newMenu, SWT.NULL);
      KeyBindings.setAccelerator(file_new_folder, "MainWindow.menu.file.folder");
      Messages.setLanguageText(file_new_folder, "MainWindow.menu.file.folder"); //$NON-NLS-1$
      file_new_folder.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
          TorrentOpener.openDirectory();
        }
      });
  
      	// file->share submenus
      
      Menu shareMenu = new Menu(mainWindow.getShell(), SWT.DROP_DOWN);
      file_share.setMenu(shareMenu);
  
      MenuItem file_share_file = new MenuItem(shareMenu, SWT.NULL);
      KeyBindings.setAccelerator(file_share_file, "MainWindow.menu.file.share.file");
      Messages.setLanguageText(file_share_file, "MainWindow.menu.file.share.file");
      file_share_file.addListener(SWT.Selection, new Listener() {
      	public void handleEvent(Event e) {
      		ShareUtils.shareFile( mainWindow.getAzureusCore(),mainWindow.getShell() );
      	}
      });
      
      MenuItem file_share_dir = new MenuItem(shareMenu, SWT.NULL);
      KeyBindings.setAccelerator(file_share_dir, "MainWindow.menu.file.share.dir");
      Messages.setLanguageText(file_share_dir, "MainWindow.menu.file.share.dir");
      file_share_dir.addListener(SWT.Selection, new Listener() {
      	public void handleEvent(Event e) {
      		ShareUtils.shareDir( mainWindow.getAzureusCore(), mainWindow.getShell() );
      	}
      });
      
      MenuItem file_share_dircontents = new MenuItem(shareMenu, SWT.NULL);
      KeyBindings.setAccelerator(file_share_dircontents, "MainWindow.menu.file.share.dircontents");
      Messages.setLanguageText(file_share_dircontents, "MainWindow.menu.file.share.dircontents");
      file_share_dircontents.addListener(SWT.Selection, new Listener() {
      	public void handleEvent(Event e) {
      		ShareUtils.shareDirContents( mainWindow.getAzureusCore(),mainWindow.getShell(), false );
      	}
      });
      MenuItem file_share_dircontents_rec = new MenuItem(shareMenu, SWT.NULL);
      KeyBindings.setAccelerator(file_share_dircontents_rec, "MainWindow.menu.file.share.dircontentsrecursive");
      Messages.setLanguageText(file_share_dircontents_rec, "MainWindow.menu.file.share.dircontentsrecursive");
      file_share_dircontents_rec.addListener(SWT.Selection, new Listener() {
      	public void handleEvent(Event e) {
      		ShareUtils.shareDirContents( mainWindow.getAzureusCore(),mainWindow.getShell(), true );
      	}
      });
         	// file->create
      
      file_create.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
          new NewTorrentWizard(mainWindow.getAzureusCore(), display);
        }
      });
      
      file_export.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
          new ExportTorrentWizard(mainWindow.getAzureusCore(), display);
        }
      });
  
      file_import.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
          new ImportTorrentWizard(mainWindow.getAzureusCore(),display);
        }
      });
  
     
      	// ******** The Download Menu
      
      MenuItem downloadItem = new MenuItem(menuBar, SWT.CASCADE);
      Messages.setLanguageText(downloadItem, "MainWindow.menu.transfers"); //$NON-NLS-1$
      Menu downloadMenu = new Menu(mainWindow.getShell(), SWT.DROP_DOWN);
      downloadItem.setMenu(downloadMenu);

  
      
      // new MenuItem(fileMenu,SWT.SEPARATOR);
      
      final MenuItem itemStartAll = new MenuItem(downloadMenu,SWT.NULL);
      KeyBindings.setAccelerator(itemStartAll, "MainWindow.menu.transfers.startalltransfers");
      Messages.setLanguageText(itemStartAll,"MainWindow.menu.transfers.startalltransfers");
      
      final MenuItem itemStopAll = new MenuItem(downloadMenu,SWT.NULL);
      KeyBindings.setAccelerator(itemStopAll, "MainWindow.menu.transfers.stopalltransfers");
      Messages.setLanguageText(itemStopAll,"MainWindow.menu.transfers.stopalltransfers");

      final MenuItem itemPause = new MenuItem(downloadMenu,SWT.NULL);
      KeyBindings.setAccelerator(itemPause, "MainWindow.menu.transfers.pausetransfers");
      Messages.setLanguageText(itemPause,"MainWindow.menu.transfers.pausetransfers");
      
      final MenuItem itemResume = new MenuItem(downloadMenu,SWT.NULL);
      KeyBindings.setAccelerator(itemResume, "MainWindow.menu.transfers.resumetransfers");
      Messages.setLanguageText(itemResume,"MainWindow.menu.transfers.resumetransfers");

      itemStartAll.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event arg0) {
        	mainWindow.getGlobalManager().startAllDownloads();
        }
      });
      
      itemStopAll.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event arg0) {
        	mainWindow.getGlobalManager().stopAllDownloads();
        }
      });

      itemPause.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event arg0) 
        {
        	mainWindow.getGlobalManager().pauseDownloads();
        }
      });
      
      itemResume.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event arg0) 
        {
          mainWindow.getGlobalManager().resumeDownloads();
        }
      });
      
      downloadMenu.addMenuListener(
          	new MenuListener()
    		{
          		public void
    			menuShown(
    				MenuEvent	menu )
          		{
          		  itemPause.setEnabled( mainWindow.getGlobalManager().canPauseDownloads() );
         			          			
          		  itemResume.setEnabled( mainWindow.getGlobalManager().canResumeDownloads() );
          		}
          		
        		public void
    			menuHidden(
    				MenuEvent	menu )
          		{
          			
          		}
    		});
      
      	// ******** The View Menu
      MenuItem viewItem = new MenuItem(menuBar, SWT.CASCADE);
      Messages.setLanguageText(viewItem, "MainWindow.menu.view"); //$NON-NLS-1$
      Menu viewMenu = new Menu(mainWindow.getShell(), SWT.DROP_DOWN);
      viewItem.setMenu(viewMenu);

      addMenuItemLabel(viewMenu, "MainWindow.menu.view.show");
      indent(addMyTorrentsMenuItem(viewMenu));
      indent(addMyTrackerMenuItem(viewMenu));
      indent(addMySharesMenuItem(viewMenu));

        if(Constants.isOSX) {
          indent(addConsoleMenuItem(viewMenu));
          indent(addStatisticsMenuItem(viewMenu));
      }

      //the Tools menu
        if(!Constants.isOSX) {
            MenuItem menu_tools = new MenuItem(menuBar,SWT.CASCADE);
            Messages.setLanguageText(menu_tools, "MainWindow.menu.tools"); //$NON-NLS-1$
            Menu toolsMenu = new Menu(mainWindow.getShell(),SWT.DROP_DOWN);
            menu_tools.setMenu(toolsMenu);

            addBlockedIPsMenuItem(toolsMenu);
            addConsoleMenuItem(toolsMenu);
            addStatisticsMenuItem(toolsMenu);
            new MenuItem(toolsMenu, SWT.SEPARATOR);

            addConfigWizardMenuItem(toolsMenu);

            //No need for configuration on OS X
            MenuItem view_config = new MenuItem(toolsMenu, SWT.NULL);
            KeyBindings.setAccelerator(view_config, "MainWindow.menu.view.configuration");
            Messages.setLanguageText(view_config, "MainWindow.menu.view.configuration"); //$NON-NLS-1$
            view_config.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
            mainWindow.showConfig();
            }
            });
      }
      
      //the Plugins menu
      menu_plugin = new MenuItem(menuBar, SWT.CASCADE);
      Messages.setLanguageText(menu_plugin, "MainWindow.menu.view.plugins"); //$NON-NLS-1$
      pluginMenu = new Menu(mainWindow.getShell(),SWT.DROP_DOWN);
      menu_plugin.setEnabled(false);
      menu_plugin.setMenu(pluginMenu);
      
      new MenuItem(pluginMenu, SWT.SEPARATOR);
      
      MenuItem plugins_install_wizard = new MenuItem(pluginMenu, SWT.NULL);
      KeyBindings.setAccelerator(plugins_install_wizard, "MainWindow.menu.plugins.installPlugins");
      Messages.setLanguageText(plugins_install_wizard, "MainWindow.menu.plugins.installPlugins"); //$NON-NLS-1$
      plugins_install_wizard.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
          new InstallPluginWizard(mainWindow.getAzureusCore(), display);
        }
      });
      
      MenuItem plugins_uninstall_wizard = new MenuItem(pluginMenu, SWT.NULL);
      KeyBindings.setAccelerator(plugins_uninstall_wizard, "MainWindow.menu.plugins.uninstallPlugins");
      Messages.setLanguageText(plugins_uninstall_wizard, "MainWindow.menu.plugins.uninstallPlugins"); //$NON-NLS-1$
      plugins_uninstall_wizard.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
          new UnInstallPluginWizard(mainWindow.getAzureusCore(), display);
        }
      });

      // standard items
      if(Constants.isOSX) {
          // Window menu
          final MenuItem menu_window = new MenuItem(menuBar, SWT.CASCADE);
          Messages.setLanguageText(menu_window, "MainWindow.menu.window");
          Menu windowMenu = new Menu(mainWindow.getShell(), SWT.DROP_DOWN);
          menu_window.setMenu(windowMenu);

          // minimize, zoom
          addMinimizeWindowMenuItem(windowMenu);
          addZoomWindowMenuItem(windowMenu);
          new MenuItem(windowMenu, SWT.SEPARATOR);
          addBlockedIPsMenuItem(windowMenu);
          new MenuItem(windowMenu, SWT.SEPARATOR);
          addBringAllToFrontMenuItem(windowMenu);
      }

      //The Help Menu
      MenuItem helpItem = new MenuItem(menuBar, SWT.CASCADE);
      Messages.setLanguageText(helpItem, "MainWindow.menu.help"); //$NON-NLS-1$
      Menu helpMenu = new Menu(mainWindow.getShell(), SWT.DROP_DOWN);
      helpItem.setMenu(helpMenu);

      if(!Constants.isOSX) {
          MenuItem help_about = new MenuItem(helpMenu, SWT.NULL);
          Messages.setLanguageText(help_about, "MainWindow.menu.help.about"); //$NON-NLS-1$
          help_about.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
              AboutWindow.show(display);
            }
          });
      }

      MenuItem help_health = new MenuItem(helpMenu, SWT.NULL);
      Messages.setLanguageText(help_health, "MyTorrentsView.menu.health");
      help_health.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
          HealthHelpWindow.show( display );
        }
      });

      if ( !SystemProperties.isJavaWebStartInstance()){
          MenuItem help_checkupdate = new MenuItem(helpMenu, SWT.NULL);
          KeyBindings.setAccelerator(help_checkupdate, "MainWindow.menu.help.checkupdate");
          Messages.setLanguageText(help_checkupdate, "MainWindow.menu.help.checkupdate"); //$NON-NLS-1$
          help_checkupdate.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        UpdateMonitor.getSingleton( mainWindow.getAzureusCore()).performCheck();
      }
    });
      }

      new MenuItem(helpMenu,SWT.SEPARATOR);

      MenuItem help_faq = new MenuItem(helpMenu, SWT.NULL);
      Messages.setLanguageText(help_faq, "MainWindow.menu.help.faq"); //$NON-NLS-1$
      help_faq.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
          String faqString = Constants.AELITIS_WEB_SITE + "wiki/";
          Program.launch(faqString);
        }
      });

      MenuItem help_new = new MenuItem(helpMenu, SWT.NULL);
      Messages.setLanguageText(help_new, "MainWindow.menu.help.whatsnew"); //$NON-NLS-1$
      help_new.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
          Program.launch("http://azureus.sourceforge.net/changelog.php?version=" + Constants.AZUREUS_VERSION);
        }
      });

      new MenuItem(helpMenu,SWT.SEPARATOR);


      
      MenuItem help_plugin= new MenuItem(helpMenu, SWT.NULL);
      Messages.setLanguageText(help_plugin, "MainWindow.menu.help.plugins"); //$NON-NLS-1$
      help_plugin.addListener(SWT.Selection, new Listener() {
          public void handleEvent(Event e) {
            String pluginString = "http://azureus.sourceforge.net/plugin_list.php";
            Program.launch(pluginString);
          }
        });

      MenuItem help_donate = new MenuItem(helpMenu, SWT.NULL);
      Messages.setLanguageText(help_donate, "MainWindow.menu.help.donate"); //$NON-NLS-1$
      help_donate.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
          new DonationWindow2(display).show();
          //String donationString = "https://www.paypal.com/xclick/business=olivier%40gudy.org&item_name=Azureus&no_note=1&tax=0&currency_code=EUR";
          //Program.launch(donationString);
        }
      });
    } catch (Exception e) {
      LGLogger.log(LGLogger.ERROR, "Error while creating menu items");
      Debug.printStackTrace( e );
    }
  }

    private static final MenuItem addMenuItem(Menu menu, String localizationKey, Listener selListener) {
      MenuItem item = new MenuItem(menu, SWT.NULL);
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
          mainWindow.showMyTorrents();
        }
      });
  }

  private MenuItem addMyTrackerMenuItem(Menu menu)
    {
        return addMenuItem(menu, "MainWindow.menu.view.mytracker", new Listener() {
          public void handleEvent(Event e) {
            mainWindow.showMyTracker();
          }
        });
    }

    private MenuItem addMySharesMenuItem(Menu menu)
    {
        return addMenuItem(menu, "MainWindow.menu.view.myshares", new Listener() {
            public void handleEvent(Event e) {
            mainWindow.showMyShares();
            }
        });
    }

    private MenuItem addConsoleMenuItem(Menu menu) {
       return addMenuItem(menu, "MainWindow.menu.view.console", new Listener() {
          public void handleEvent(Event e) {
            mainWindow.showConsole();
          }
        });
    }

    private MenuItem addStatisticsMenuItem(Menu menu) {
       return addMenuItem(menu, "MainWindow.menu.view.stats", new Listener() {
          public void handleEvent(Event e) {
            mainWindow.showStats();
          }
        });
    }

    private void addConfigWizardMenuItem(Menu menu) {
        addMenuItem(menu, "MainWindow.menu.file.configure", new Listener() {
          public void handleEvent(Event e) {
            new ConfigureWizard(mainWindow.getAzureusCore(), display);
          }
        });
    }

   private void addCloseDetailsMenuItem(Menu menu) {
       addMenuItem(menu, "MainWindow.menu.closealldetails", new Listener() {
         public void handleEvent(Event e) {
           Tab.closeAllDetails();
         }
      });
  }

  private void addCloseWindowMenuItem(Menu menu) {
      addMenuItem(menu, "MainWindow.menu.file.closewindow", new Listener() {
          public void handleEvent(Event event) {
              if(MainWindow.isAlreadyDead) {return;}

              mainWindow.close();
          }
      });
  }

  private void addCloseTabMenuItem(Menu menu) {
      addMenuItem(menu, "MainWindow.menu.file.closetab", new Listener() {
          public void handleEvent(Event event) {
              if(MainWindow.isAlreadyDead) {return;}

              mainWindow.closeViewOrWindow();
          }
      });
  }

  private void addMinimizeWindowMenuItem(Menu menu) {
      addMenuItem(menu, "MainWindow.menu.window.minimize", new Listener() {
          public void handleEvent(Event event) {
              if(MainWindow.isAlreadyDead) {return;}

              mainWindow.getShell().setMinimized(true);
          }
      });
  }

  private void addZoomWindowMenuItem(Menu menu) {
      addMenuItem(menu, "MainWindow.menu.window.zoom", new Listener() {
          public void handleEvent(Event event) {
              if(MainWindow.isAlreadyDead) {return;}

              mainWindow.getShell().setMaximized(!MainWindow.getWindow().getShell().getMaximized());
          }
      });
  }

  private void addBringAllToFrontMenuItem(Menu menu) {
      addMenuItem(menu, "MainWindow.menu.window.alltofront", new Listener() {
          public void handleEvent(Event event) {
              if(MainWindow.isAlreadyDead) {return;}

              mainWindow.getShell().setMinimized(false);
          }
      });
  }

  private void addBlockedIPsMenuItem(Menu menu) {
      addMenuItem(menu, "ConfigView.section.ipfilter.list.title", new Listener() {
          public void handleEvent(Event event) {
              if(MainWindow.isAlreadyDead) {return;}

              BlockedIpsWindow.showBlockedIps(mainWindow.getAzureusCore(), mainWindow.getShell());
          }
      });
  }

  public void addCloseDownloadBarsToMenu(Menu menu) {
    addMenuItem(menu, "MainWindow.menu.closealldownloadbars", new Listener() {
      public void handleEvent(Event e) {
        mainWindow.closeDownloadBars();
      }
    });
  }


  public void updateMenuText(Object menu) {
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
  
  
  public void refreshLanguage() {
    if (display == null || display.isDisposed())
      return;

    display.asyncExec(new AERunnable() {
      public void runSupport() {
        updateMenuText(menuBar);
        mainWindow.setSelectedLanguageItem(); 
      }
    });
  }
  
  public void 
  addPluginView(
  	final PluginView view) 
  {
    display.asyncExec(new AERunnable() {
      public void runSupport() 
      {
      	String	name = view.getPluginViewName();
      	
      	MenuItem[]	items = pluginMenu.getItems();
      	
      	int	insert_at	= items.length;
      
      	for (int i=0;i<items.length;i++){
      	
      		if ( 	items[i].getStyle() == SWT.SEPARATOR ||
      				name.compareTo(items[i].getText()) < 0 ){
      			
      			insert_at  = i;
      			
      			break;
      		}
      	}
      	      	      	
        MenuItem item = new MenuItem(pluginMenu,SWT.NULL,insert_at);
        item.setText( name );
        item.addListener(SWT.Selection,new Listener() {
          public void handleEvent(Event e) {
            mainWindow.openPluginView(view);
          }
        });
        menu_plugin.setEnabled(true);
      }
    }); 
  }
}


