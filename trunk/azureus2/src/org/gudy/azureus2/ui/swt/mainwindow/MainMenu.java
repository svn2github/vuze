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
import org.eclipse.swt.events.*;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.*;
import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.config.ParameterListener;
import org.gudy.azureus2.core3.internat.MessageText;
import org.gudy.azureus2.core3.logging.*;
import org.gudy.azureus2.core3.predicate.AllPredicate;
import org.gudy.azureus2.core3.predicate.NotPredicate;
import org.gudy.azureus2.core3.predicate.Predicable;
import org.gudy.azureus2.core3.util.AERunnable;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.core3.util.Constants;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.SystemProperties;
import org.gudy.azureus2.ui.swt.*;
import org.gudy.azureus2.ui.swt.components.StringListChooser;
import org.gudy.azureus2.ui.swt.components.shell.ShellManager;
import org.gudy.azureus2.ui.swt.config.wizard.ConfigureWizard;
import org.gudy.azureus2.ui.swt.donations.DonationWindow2;
import org.gudy.azureus2.ui.swt.exporttorrent.wizard.ExportTorrentWizard;
import org.gudy.azureus2.ui.swt.help.AboutWindow;
import org.gudy.azureus2.ui.swt.help.HealthHelpWindow;
import org.gudy.azureus2.ui.swt.importtorrent.wizard.ImportTorrentWizard;
import org.gudy.azureus2.ui.swt.maketorrent.NewTorrentWizard;
import org.gudy.azureus2.ui.swt.nat.NatTestWindow;
import org.gudy.azureus2.ui.swt.plugins.UISWTInstance;
import org.gudy.azureus2.ui.swt.plugins.UISWTPluginView;
import org.gudy.azureus2.ui.swt.plugins.UISWTView;
import org.gudy.azureus2.ui.swt.plugins.UISWTViewEventListener;
import org.gudy.azureus2.ui.swt.pluginsimpl.UISWTViewImpl;
import org.gudy.azureus2.ui.swt.pluginsinstaller.InstallPluginWizard;
import org.gudy.azureus2.ui.swt.pluginsuninstaller.UnInstallPluginWizard;
import org.gudy.azureus2.ui.swt.predicate.shell.ShellCanMaximizePredicate;
import org.gudy.azureus2.ui.swt.predicate.shell.ShellCanMinimizePredicate;
import org.gudy.azureus2.ui.swt.predicate.shell.ShellIsMinimizedPredicate;
import org.gudy.azureus2.ui.swt.predicate.shell.ShellIsModalPredicate;
import org.gudy.azureus2.ui.swt.predicate.shellmanager.AllManagedShellsAreMinimizedPredicate;
import org.gudy.azureus2.ui.swt.predicate.shellmanager.ShellManagerIsEmptyPredicate;
import org.gudy.azureus2.ui.swt.sharing.ShareUtils;
import org.gudy.azureus2.ui.swt.update.UpdateMonitor;
import org.gudy.azureus2.ui.swt.views.AbstractIView;
import org.gudy.azureus2.ui.swt.views.utils.ManagerUtils;
import org.gudy.azureus2.ui.swt.welcome.WelcomeWindow;
import org.gudy.azureus2.plugins.PluginView;

import java.util.Iterator;
import java.util.Locale;

/**
 * @author Olivier Chalouhi
 * @author James Yeh Accessibility: Changes to allow better validation and unified menu bar state
 */
public class MainMenu {

  private Display display;
  private MainWindow mainWindow;
  private Shell attachedShell;

  private Menu menuBar;
  
  private MenuItem menu_plugin;
  private Menu pluginMenu;

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

          buildMenu(MessageText.getLocales(), shell);
      }
  }

  public MainMenu(final MainWindow mainWindow) {
    this.mainWindow = mainWindow;
    this.display = SWTThread.getInstance().getDisplay();
    attachedShell = mainWindow.getShell();

    buildMenu(MessageText.getLocales(), mainWindow.getShell());
  }

  /**
   * Populates Azureus' menu bar
   * @param locales
   * @param parent
   */
  private void buildMenu(Locale[] locales, final Shell parent) {
    try {
      
      //The Main Menu
      menuBar = new Menu(parent, SWT.BAR);
      parent.setMenuBar(menuBar);

      // one time disable conditions
      boolean notMainWindow = (attachedShell != mainWindow.getShell());
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

            MainWindow.getWindow().dispose(true,false);
         }
        });

        final MenuItem file_exit = new MenuItem(fileMenu, SWT.NULL);
        if(!COConfigurationManager.getBooleanParameter("Enable System Tray") || !COConfigurationManager.getBooleanParameter("Close To Tray")) {
            KeyBindings.setAccelerator(file_exit, "MainWindow.menu.file.exit");
        }
        Messages.setLanguageText(file_exit, "MainWindow.menu.file.exit"); //$NON-NLS-1$
        
        file_exit.addListener(SWT.Selection, new Listener() {
          public void handleEvent(Event e) {
            mainWindow.dispose(false,false);
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
      
      Menu newMenu = new Menu(parent, SWT.DROP_DOWN);
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
  
    MenuItem file_new_torrent_for_tracking = new MenuItem(newMenu, SWT.NULL);
      KeyBindings.setAccelerator(file_new_torrent_for_tracking, "MainWindow.menu.file.open.torrentfortracking");
      Messages.setLanguageText(file_new_torrent_for_tracking, "MainWindow.menu.file.open.torrentfortracking");
	  file_new_torrent_for_tracking.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event e) {
          TorrentOpener.openTorrentTrackingOnly();
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
      
      Menu shareMenu = new Menu(parent, SWT.DROP_DOWN);
      file_share.setMenu(shareMenu);
  
      MenuItem file_share_file = new MenuItem(shareMenu, SWT.NULL);
      KeyBindings.setAccelerator(file_share_file, "MainWindow.menu.file.share.file");
      Messages.setLanguageText(file_share_file, "MainWindow.menu.file.share.file");
      file_share_file.addListener(SWT.Selection, new Listener() {
      	public void handleEvent(Event e) {
      		ShareUtils.shareFile( mainWindow.getAzureusCore(),parent );
      	}
      });
      
      MenuItem file_share_dir = new MenuItem(shareMenu, SWT.NULL);
      KeyBindings.setAccelerator(file_share_dir, "MainWindow.menu.file.share.dir");
      Messages.setLanguageText(file_share_dir, "MainWindow.menu.file.share.dir");
      file_share_dir.addListener(SWT.Selection, new Listener() {
      	public void handleEvent(Event e) {
      		ShareUtils.shareDir( mainWindow.getAzureusCore(), parent );
      	}
      });
      
      MenuItem file_share_dircontents = new MenuItem(shareMenu, SWT.NULL);
      KeyBindings.setAccelerator(file_share_dircontents, "MainWindow.menu.file.share.dircontents");
      Messages.setLanguageText(file_share_dircontents, "MainWindow.menu.file.share.dircontents");
      file_share_dircontents.addListener(SWT.Selection, new Listener() {
      	public void handleEvent(Event e) {
      		ShareUtils.shareDirContents( mainWindow.getAzureusCore(),parent, false );
      	}
      });
      MenuItem file_share_dircontents_rec = new MenuItem(shareMenu, SWT.NULL);
      KeyBindings.setAccelerator(file_share_dircontents_rec, "MainWindow.menu.file.share.dircontentsrecursive");
      Messages.setLanguageText(file_share_dircontents_rec, "MainWindow.menu.file.share.dircontentsrecursive");
      file_share_dircontents_rec.addListener(SWT.Selection, new Listener() {
      	public void handleEvent(Event e) {
      		ShareUtils.shareDirContents( mainWindow.getAzureusCore(),parent, true );
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


        // hig compliance
        if(Constants.isOSX) {
            addViewMenu(parent, notMainWindow);
            addTransferMenu(parent, isModal, notMainWindow);
        }
        else { // previous ordering
            addTransferMenu(parent, isModal, notMainWindow);
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
            
            MenuItem nat_test = new MenuItem(toolsMenu, SWT.NULL);
            //KeyBindings.setAccelerator(nat_test, "MainWindow.menu.tools.nattest");
            Messages.setLanguageText(nat_test, "MainWindow.menu.tools.nattest"); //$NON-NLS-1$
            nat_test.addListener(SWT.Selection, new Listener() {
              public void handleEvent(Event e) {
                new NatTestWindow();
              }
            });
            
            new MenuItem(toolsMenu, SWT.SEPARATOR);

            addConfigWizardMenuItem(toolsMenu);

            MenuItem view_config = new MenuItem(toolsMenu, SWT.NULL);
            KeyBindings.setAccelerator(view_config, "MainWindow.menu.view.configuration");
            Messages.setLanguageText(view_config, "MainWindow.menu.view.configuration"); //$NON-NLS-1$
            view_config.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event e) {
            mainWindow.showConfig();
            }
            });

            if(isModal) {performOneTimeDisable(menu_tools, true);}
      }
      
      //the Plugins menu
      menu_plugin = new MenuItem(menuBar, SWT.CASCADE);
      Messages.setLanguageText(menu_plugin, "MainWindow.menu.view.plugins"); //$NON-NLS-1$
      pluginMenu = new Menu(parent,SWT.DROP_DOWN);
      menu_plugin.setEnabled(false);
      menu_plugin.setMenu(pluginMenu);
      if(notMainWindow) {performOneTimeDisable(menu_plugin, true);}

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
          new WelcomeWindow();
        }
      });

      if ( !SystemProperties.isJavaWebStartInstance()){
          MenuItem help_checkupdate = new MenuItem(helpMenu, SWT.NULL);
          KeyBindings.setAccelerator(help_checkupdate, "MainWindow.menu.help.checkupdate");
          Messages.setLanguageText(help_checkupdate, "MainWindow.menu.help.checkupdate"); //$NON-NLS-1$
          help_checkupdate.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event e) {
        mainWindow.getShell().setFocus();
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
      LGLogger.log(LGLogger.ERROR, "Error while creating menu items");
      Debug.printStackTrace( e );
    }
  }

  private void addTransferMenu(final Shell parent, boolean modal, boolean notMainWindow)
  {
      // ******** The Download Menu

      MenuItem downloadItem = new MenuItem(menuBar, SWT.CASCADE);
      Messages.setLanguageText(downloadItem, "MainWindow.menu.transfers"); //$NON-NLS-1$
      Menu downloadMenu = new Menu(parent, SWT.DROP_DOWN);
      downloadItem.setMenu(downloadMenu);
      if(modal) {performOneTimeDisable(downloadItem, true);}



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
      if(notMainWindow) {performOneTimeDisable(itemPause, true);}

      final MenuItem itemResume = new MenuItem(downloadMenu,SWT.NULL);
      KeyBindings.setAccelerator(itemResume, "MainWindow.menu.transfers.resumetransfers");
      Messages.setLanguageText(itemResume,"MainWindow.menu.transfers.resumetransfers");
      if(notMainWindow) {performOneTimeDisable(itemResume, true);}

      itemStartAll.addListener(SWT.Selection, new Listener() {
        public void handleEvent(Event arg0) {
            mainWindow.getGlobalManager().startAllDownloads();
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
          mainWindow.getGlobalManager().resumeDownloads();
        }
      });

      downloadMenu.addMenuListener(
          new MenuListener() {
                public void
                menuShown(MenuEvent menu)
                {
                    itemPause.setEnabled( mainWindow.getGlobalManager().canPauseDownloads() );

                    itemResume.setEnabled( mainWindow.getGlobalManager().canResumeDownloads() );
                }

                public void
                menuHidden(MenuEvent	menu )
                {
                }
          });
  }

  private void addViewMenu(final Shell parent, boolean notMainWindow)
  {
      // ******** The View Menu
      MenuItem viewItem = new MenuItem(menuBar, SWT.CASCADE);
      Messages.setLanguageText(viewItem, "MainWindow.menu.view"); //$NON-NLS-1$
      Menu viewMenu = new Menu(parent, SWT.DROP_DOWN);
      viewItem.setMenu(viewMenu);
      if(notMainWindow) {performOneTimeDisable(viewItem, true);}

      addMenuItemLabel(viewMenu, "MainWindow.menu.view.show");
      indent(addMyTorrentsMenuItem(viewMenu));
      indent(addMyTrackerMenuItem(viewMenu));
      indent(addMySharesMenuItem(viewMenu));

      if(Constants.isOSX) {
          indent(addConsoleMenuItem(viewMenu));
          indent(addStatisticsMenuItem(viewMenu));
      }
  }

  public void
  addPluginView(
  	PluginView view)
  {
	  addPluginView( view, view.getPluginViewName());
  }
  
  /**
   * Add a UISWTPluginView to the main view
   * 
   * @param view view to add
   */
  public void
  addPluginView(
  	UISWTPluginView view)
  {
	  addPluginView( view, view.getPluginViewName());
  }

  public void addPluginView(final String sViewID, final UISWTViewEventListener l) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				String sResourceID = UISWTViewImpl.CFG_PREFIX + sViewID + ".title";
				boolean bResourceExists = MessageText.keyExists(sResourceID);
				
				String name;
				
				if (bResourceExists){
					name = MessageText.getString(sResourceID);
				}else{
					name = sViewID.replace('.', ' ' );	// support old plugins
				}
					
				MenuItem[] items = pluginMenu.getItems();

				int insert_at = items.length;

				for (int i = 0; i < items.length; i++) {
					if (items[i].getStyle() == SWT.SEPARATOR
							|| name.compareTo(items[i].getText()) < 0) {
						insert_at = i;
						break;
					}
				}

				MenuItem item = new MenuItem(pluginMenu, SWT.NULL, insert_at);
				item.setData("ViewID", sViewID);

				if (bResourceExists)
					Messages.setLanguageText(item, sResourceID);
				else
					item.setText(name);

				item.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event e) {
						mainWindow.openPluginView(UISWTInstance.VIEW_MAIN, sViewID, l,
								null, true);
					}
				});
				menu_plugin.setEnabled(true);
			}
		});
	}
  
  public void removePluginViews(final String sViewID) {
		Utils.execSWTThread(new AERunnable() {
			public void runSupport() {
				MenuItem[] items = pluginMenu.getItems();
				for (int i = 0; i < items.length; i++) {
					String sID = (String)items[i].getData("ViewID");
					if (sID != null && sID.equals(sViewID)) {
						items[i].dispose();
					}
				}
  			mainWindow.closePluginViews(sViewID);
			}
		});
  }


  protected void
  addPluginView(
  	final AbstractIView 	view,
  	final String			name )
  {

    display.asyncExec(new AERunnable() {
      public void runSupport()
      {
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
            mainWindow.openPluginView(view,name);
          }
        });
        menu_plugin.setEnabled(true);
      }
    });
  }
  
  public void
  removePluginView(
  	PluginView view)
  {
	  removePluginView( view, view.getPluginViewName());
  }
  
  public void
  removePluginView(
  	UISWTPluginView view)
  {
	  removePluginView( view, view.getPluginViewName());
  }
  
  protected void
  removePluginView(
  	final AbstractIView 	view,
  	final String			name )
  {
    display.asyncExec(new AERunnable() {
      public void runSupport()
      {
      	MenuItem[]	items = pluginMenu.getItems();

      	boolean	others = false;
      	
      	for (int i=0;i<items.length;i++){

      		MenuItem	item = items[i];
      		
      		if ( item.getStyle() == SWT.SEPARATOR ){
    	
      		}else if ( item.getText().equals( name )){
      			
      			item.dispose();
      			
      			mainWindow.closePluginView( view );
      			
      		}else{
      			others = true;
      		}
      	}
      	
      	menu_plugin.setEnabled(others);
      }
    });
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
              if(windowMenu.isDisposed())
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
          }
      };

      ShellManager.sharedManager().addWindowAddedListener(rebuild);
      ShellManager.sharedManager().addWindowRemovedListener(rebuild);
      attachedShell.addListener(SWT.FocusIn, rebuild);
      windowMenu.addListener(SWT.Show, rebuild);
  }

    // individual menu items

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

    private MenuItem addConfigWizardMenuItem(Menu menu) {
        return addMenuItem(menu, "MainWindow.menu.file.configure", new Listener() {
          public void handleEvent(Event e) {
            new ConfigureWizard(mainWindow.getAzureusCore(), display);
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
                item.setEnabled(mainWindow.getShell() == attachedShell && Tab.hasDetails());
        }
      };

      menu.addListener(SWT.Show,  enableHandler);
      attachedShell.addListener(SWT.FocusIn,  enableHandler);
      Tab.addTabAddedListener(enableHandler);
      Tab.addTabRemovedListener(enableHandler);

      return item;
  }

  private MenuItem addCloseWindowMenuItem(Menu menu) {
      final boolean isMainWindow = (mainWindow.getShell() == attachedShell);
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
      return addMenuItem(menu, "ConfigView.section.ipfilter.list.title", new Listener() {
          public void handleEvent(Event event) {
              if(MainWindow.isAlreadyDead) {return;}

              BlockedIpsWindow.showBlockedIps(mainWindow.getAzureusCore(), mainWindow.getShell());
          }
      });
  }

  public MenuItem addCloseDownloadBarsToMenu(Menu menu) {
    final MenuItem item = addMenuItem(menu, "MainWindow.menu.closealldownloadbars", new Listener() {
      public void handleEvent(Event e) {
        mainWindow.closeDownloadBars();
      }
    });

    final NotPredicate pred = new NotPredicate(new ShellManagerIsEmptyPredicate());
    final Listener enableHandler = getEnableHandler(item, pred, MinimizedWindow.getShellManager());

    menu.addListener(SWT.Show, enableHandler);
    attachedShell.addListener(SWT.FocusIn,  enableHandler);
    setHandlerForShellManager(item, MinimizedWindow.getShellManager(), enableHandler);

    return item;
  }


    // utility methods

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

}


