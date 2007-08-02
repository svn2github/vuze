/*
 * Created on Jun 29, 2006 10:16:26 PM
 * Copyright (C) 2006 Aelitis, All Rights Reserved.
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
package com.aelitis.azureus.ui.swt.browser.listener.publish;

import org.eclipse.swt.widgets.Shell;

import com.aelitis.azureus.ui.swt.browser.msg.BrowserMessage;
import com.aelitis.azureus.ui.swt.browser.txn.AbstractTransactionalListener;
import com.aelitis.azureus.ui.swt.browser.txn.Transaction;

/**
 * Handles messages from the PublishNewContent page.
 * 
 * <ul>
 *   <li> choose-file </li>
 *   <li> choose-folder </li>
 *   <li> torrent-ready </li>
 * </ul>
 * 
 * @author dharkness
 * @created Jul 18, 2006
 */
public class PublishListener extends AbstractTransactionalListener
{
    public static final String DEFAULT_LISTENER_ID = "publish";

    public static final String OP_CHOOSE_FILE = "choose-file";
    public static final String OP_CHOOSE_FOLDER = "choose-folder";
    public static final String OP_CHOOSE_THUMBNAIL = "choose-thumbnail";
    public static final String OP_EDIT_THUMBNAIL = "edit-thumbnail";
    public static final String OP_TORRENT_READY = "torrent-ready";
    public static final String OP_CANCEL = "cancel";
    
    
    private Shell shell;
    
    private LocalHoster hoster;

	public PublishListener(Shell s, LocalHoster hoster) {
		this(s, DEFAULT_LISTENER_ID, hoster);
	}

	public PublishListener(Shell s, String id, LocalHoster hoster) {
        super(id);
        
        this.shell = s;
        this.hoster = hoster;
        
        registerOperationTxnRequiresNew(OP_CHOOSE_FILE);
        registerOperationTxnRequiresNew(OP_CHOOSE_FOLDER);
        registerOperationTxnMandatory(OP_CHOOSE_THUMBNAIL);
        registerOperationTxnMandatory(OP_TORRENT_READY);        
        registerOperationTxnRequired(OP_CANCEL);
        registerOperationTxnRequiresNew(OP_EDIT_THUMBNAIL);
    }

    /**
     * Handles the given message, usually by parsing the parameters 
     * and calling the appropriate operation.
     * 
     * @param message holds all message information
     */
    public void handleTxnlMessage ( BrowserMessage message , Transaction txn ) {
        PublishTransaction realTxn = (PublishTransaction) txn;
        
        realTxn.setShell(shell);
        realTxn.setLocalHoster(hoster);
        
        if ( OP_CHOOSE_FILE.equals(message.getOperationId()) ) {
            realTxn.chooseFile(message);
        }
        else if ( OP_CHOOSE_FOLDER.equals(message.getOperationId()) ) {
            realTxn.chooseFolder(message);
        }
        else if ( OP_CHOOSE_THUMBNAIL.equals(message.getOperationId()) || OP_EDIT_THUMBNAIL.equals(message.getOperationId())) {
            realTxn.chooseThumbnail(message);
        }
        else if ( OP_TORRENT_READY.equals(message.getOperationId()) ) {
            realTxn.torrentIsReady(message);
        }
        else if ( OP_CANCEL.equals(message.getOperationId()) ) {
            realTxn.cancel();
        }
        else {
            throw new IllegalArgumentException("Unknown operation: " + message.getOperationId());
        }
    }
}
