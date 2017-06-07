/*
Copyright 2009 David Revell

This file is part of SwiFTP.

SwiFTP is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

SwiFTP is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with SwiFTP.  If not, see <http://www.gnu.org/licenses/>.
*/

/*
 * Since the FTP verbs LIST and NLST do very similar things related to listing
 * directory contents, the common tasks that they share have been factored
 * out into this abstract class. Both CmdLIST and CmdNLST inherit from this
 * class. 
 */
package com.dq.swiftp;

import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.List;



public abstract class CmdAbstractListing extends FtpCmd {
    private static final String TAG = "CmdAbstractListing";
    
    protected static final MyLog staticLog = new MyLog(CmdLIST.class.toString());
    
    public CmdAbstractListing(SessionThread sessionThread, String input) {
        super(sessionThread, CmdAbstractListing.class.toString());
    }
    
    abstract String makeLsString(File file);
    
    // Gionee <lilg><2014-09-22> modify for CR01375662 begin
    // Creates a directory listing by finding the contents of the directory,
    // calling makeLsString on each file, and concatenating the results.
    // Returns an error string if failure, returns null on success. May be
    // called by CmdLIST or CmdNLST, since they each override makeLsString
    // in a different way.
    public String listDirectory(StringBuilder response, File dir) {
        Log.d(TAG, "listDirectory.");
        
        if(!dir.isDirectory()) {
            return "500 Internal error, listDirectory on non-directory\r\n";
        }
        myLog.l(Log.DEBUG, "Listing directory: " + dir.toString());
        Log.d(TAG, "Listing directory: " + dir.toString());
        Log.d(TAG, "ROOT_PATH: " + Defaults.chrootDir);
        
        // Gionee <lilg><2015-02-11> modify for CR01447221 begin
        if(TextUtils.isEmpty(Defaults.chrootDir)){
            return "500 Internal error, please try again.\r\n";
        }
        // Gionee <lilg><2015-02-11> modify for CR01447221 end
        
        if (Defaults.chrootDir.trim().equals(dir.toString().trim())) {
            // is /storage/
            // Gionee <lilg><2015-01-20> modify for CR01620980 begin
            if(DefaultStorageManager.getInstance().getMountedStorageList().size() == 1 && DefaultStorageManager.getInstance().isStorageMounted(DefaultStorageManager.TYPE_INTERNAL)) {
                String interStoragePath = DefaultStorageManager.getInstance().getStorageMountPath(DefaultStorageManager.TYPE_INTERNAL);
                Log.d(TAG, "interStoragePath: " + interStoragePath);
                File[] entries = new File(interStoragePath).listFiles();
                if(entries == null) {
                    return "500 Couldn't list directory. Check config and mount status.\r\n";
                }
                appendMountList(response, entries);
            } else {
                appendMountList(response);
            }
            // Gionee <lilg><2015-01-20> modify for CR01620980 end
            
        } else {
            // not /storage/
            // Get a listing of all files and directories in the path
            File[] entries = dir.listFiles();
            if(entries == null) {
                return "500 Couldn't list directory. Check config and mount status.\r\n";
            }
            myLog.l(Log.DEBUG, "Dir len " + entries.length);
            appendMountList(response, entries);
        }
        
        return null;
    }

    private void appendMountList(StringBuilder response) {
        List<StorageItem> mountedStorageList = DefaultStorageManager.getInstance().getMountedStorageList();
            for(StorageItem item : mountedStorageList){
                Log.d(TAG, "mount point: " + item.toString());
                String curLine = makeLsString(new File(item.getPath()));
                if(curLine != null) {
                    response.append(curLine);
                }
            }
    }
	
	private void appendMountList(StringBuilder response, File[] entries) {
		for(File entry : entries) {
		    // Gionee liuwei 2013-11-25 add for CR00956992 begin
		    if (DefaultStorageManager.getInstance().isStorage(entry.getAbsolutePath())) {
		        if (!DefaultStorageManager.getInstance().isStorageMounted(entry.getAbsolutePath()))
		            continue;
		    }else{
		        if(isRootDir(entry.getAbsolutePath())){
		            continue;
		        }
		    }
		    String curLine = makeLsString(entry);
		    // Gionee liuwei 2013-11-25 add for CR00956992 end
		    if(curLine != null) {
		        response.append(curLine);
		    }
		}
	}
	// Gionee <lilg><2014-09-22> modify for CR01375662 end

    private boolean isRootDir(String path) {
        if(path.contains("/")){
         String [] array = path.split("/"); 
         if(array.length == 3){
          return true;   
         }
        }
        return false;
    }

    // Send the directory listing over the data socket. Used by CmdLIST and
    // CmdNLST.
    // Returns an error string on failure, or returns null if successful.
    protected String sendListing(String listing) {
        Log.d(TAG, "sendListing.");
        
        if(sessionThread.startUsingDataSocket()) {
            myLog.l(Log.DEBUG, "LIST/NLST done making socket");
        } else {
            sessionThread.closeDataSocket();
            return "425 Error opening data socket\r\n";
        }
        String mode = sessionThread.isBinaryMode() ? "BINARY" : "ASCII";
        sessionThread.writeString(
                "150 Opening "+mode+" mode data connection for file list\r\n");
        myLog.l(Log.DEBUG, "Sent code 150, sending listing string now");
        if(!sessionThread.sendViaDataSocket(listing)) {
            myLog.l(Log.DEBUG, "sendViaDataSocket failure");
            sessionThread.closeDataSocket();
            return "426 Data socket or network error\r\n";
        }
        sessionThread.closeDataSocket();
        myLog.l(Log.DEBUG, "Listing sendViaDataSocket success");
        sessionThread.writeString("226 Data transmission OK\r\n");
        return null;
    }
}
