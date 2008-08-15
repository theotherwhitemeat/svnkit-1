/*
 * ====================================================================
 * Copyright (c) 2004-2008 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.wc;

import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNException;


/**
 * <code>SVNEventAdapter</code> is an adapter class for {@link ISVNEventHandler}.
 * Users's event handler implementations should extend this adapter class rather than implementing 
 * {@link ISVNEventHandler} directly. This way, if the {@link ISVNEventHandler} interface is changed  
 * in future, users' event handler implementations won't get broken since the changes will be reflected in 
 * this adapter class. 
 * 
 * @version 1.2.0
 * @author  TMate Software Ltd.
 * @since   1.2.0
 */
public class SVNEventAdapter implements ISVNEventHandler {

    /**
     * Does nothing. To be overriden by a user's implementation.
     * @throws SVNCancelException 
     */
    public void checkCancelled() throws SVNCancelException {
    }

    /**
     * Does nothing. To be overriden by a user's implementation.
     * 
     * @param event 
     * @param progress 
     * @throws SVNException 
     */
    public void handleEvent(SVNEvent event, double progress) throws SVNException {
    }

}
