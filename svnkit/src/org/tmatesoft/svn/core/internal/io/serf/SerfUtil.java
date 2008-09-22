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
package org.tmatesoft.svn.core.internal.io.serf;

import java.util.Map;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.internal.io.dav.DAVConnection;
import org.tmatesoft.svn.core.internal.io.dav.DAVElement;
import org.tmatesoft.svn.core.internal.io.dav.DAVProperties;
import org.tmatesoft.svn.core.internal.io.dav.DAVUtil;
import org.tmatesoft.svn.core.internal.io.dav.http.HTTPStatus;
import org.tmatesoft.svn.core.internal.util.SVNEncodingUtil;
import org.tmatesoft.svn.core.internal.util.SVNHashMap;
import org.tmatesoft.svn.core.internal.util.SVNPathUtil;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNLogType;


/**
 * @version 1.2.0
 * @author  TMate Software Ltd.
 */
public class SerfUtil {

    public static DAVProperties deliverProperties(SerfRepository repository, DAVConnection connection, 
            String path, String label, DAVElement[] properties, int depth, boolean cacheProps) throws SVNException {
        if (cacheProps) {
            DAVProperties result = new DAVProperties();
            DAVProperties propsCache = repository.getResourceProperties();
            if (hasPropsInCache(propsCache, properties, result)) {
                return result;
            }
        }
        
        Map resultMap = new SVNHashMap();
        HTTPStatus status = DAVUtil.getProperties(connection, path, depth, label, properties, 
                resultMap);
        
        if (status.getError() != null) {
            SVNErrorManager.error(status.getError(), SVNLogType.NETWORK);
        }
        
        if (!resultMap.isEmpty()) {
            DAVProperties result = (DAVProperties) resultMap.values().iterator().next();
            if (cacheProps) {
                repository.setResourceProperties(result);
            }
            return result;
        }
        
        label = label == null ? "NULL" : label;
        SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_REQUEST_FAILED, 
                "Failed to find label ''{0}'' for URL ''{1}''", new Object[] {label, path});
        SVNErrorManager.error(err, SVNLogType.NETWORK);
        return null;
    }
    
    public static DAVProperties getStartingProperties(SerfRepository repository, DAVConnection connection, 
            String path, String label) throws SVNException {
        return deliverProperties(repository, connection, path, label, DAVElement.STARTING_PROPERTIES, 
                DAVUtil.DEPTH_ZERO, true);
    }

    public static DAVProperties findStartingProperties(SerfRepository repos, DAVConnection connection, 
            String path) throws SVNException {
        DAVProperties props = null;
        String loppedPath = "";
        
        do {
            SVNErrorMessage err = null;
            SVNException nested=null;
            try {
                props = getStartingProperties(repos, connection, path, null);
            } catch (SVNException e) {
                if (e.getErrorMessage() == null) {
                    throw e;
                }
                err = e.getErrorMessage();
            }            
            if (err == null) {
                break;
            }
            if (err.getErrorCode() != SVNErrorCode.FS_NOT_FOUND) {
                SVNErrorManager.error(err, SVNLogType.NETWORK);
            }
            loppedPath = SVNPathUtil.append(SVNPathUtil.tail(path), loppedPath);
            int length = path.length();
            path = "/".equals(path) ? "" : SVNPathUtil.removeTail(path);
            if (length == path.length()) {
                SVNErrorMessage err2 = SVNErrorMessage.create(err.getErrorCode(), 
                        "The path was not part of repository");
                SVNErrorManager.error(err2, err, nested, SVNLogType.NETWORK);
            }
            
        } while (!"".equals(path));

        if (props != null) {
            if (props.getPropertyValue(DAVElement.REPOSITORY_UUID) != null && repos != null) {
                repos.setRepositoryUUID(props.getPropertyValue(DAVElement.REPOSITORY_UUID).getString());
            }
            if (props.getPropertyValue(DAVElement.BASELINE_RELATIVE_PATH) != null && repos != null) {
                String relativePath = props.getPropertyValue(DAVElement.BASELINE_RELATIVE_PATH).getString();
                relativePath = SVNEncodingUtil.uriEncode(relativePath);
                String rootPath = path.substring(0, path.length() - relativePath.length());
                repos.setRepositoryRoot(repos.getLocation().setPath(rootPath, true));
            }
            props.setLoppedPath(loppedPath);
        }
        
        return props;
    }
    
    public static String getVCCPath(DAVConnection connection, SerfRepository repository, String path) throws SVNException {
        DAVProperties properties = DAVUtil.findStartingProperties(connection, repository, path);
        //DAVProperties properties = findStartingProperties(repository, connection, path);
        SVNPropertyValue vcc = properties.getPropertyValue(DAVElement.VERSION_CONTROLLED_CONFIGURATION);
        if (vcc == null) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.RA_DAV_OPTIONS_REQ_FAILED, 
                    "The OPTIONS response did not include the requested version-controlled-configuration value");
            SVNErrorManager.error(err, SVNLogType.NETWORK);
        }
        return vcc.getString();
    }

    private static boolean hasPropsInCache(DAVProperties propsCache, DAVElement[] properties, DAVProperties result) {
        boolean hitCache = true;
        for (int i = 0; i < properties.length; i++) {
            DAVElement property = properties[i];
            SVNPropertyValue propValue = propsCache.getPropertyValue(property);
            if (propValue != null) {
                result.setProperty(property, propValue);
            } else {
                hitCache = false;
            }
        }
        return hitCache;
    }
}
