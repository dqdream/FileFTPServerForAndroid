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

package com.dq.swiftp;

import java.io.File;

import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.dq.fileftpserver.FTPServerApp;

public class CmdRNTO extends FtpCmd implements Runnable {
	private static final String TAG = "FtpCmdRNTO";
	protected String input;

	public CmdRNTO(SessionThread sessionThread, String input) {
		super(sessionThread, CmdRNTO.class.toString());
		this.input = input;
	}
	
	public void run() {
	    Log.d(TAG, "run.");
		String param = getParameter(input);
		String errString = null;
		File toFile = null;
		myLog.l(Log.DEBUG, "RNTO executing\r\n");
		mainblock: {
			myLog.l(Log.INFO, "param: " + param); 
			toFile = inputPathToChrootedFile(sessionThread.getWorkingDir(), param);
			myLog.l(Log.INFO, "RNTO parsed: " + toFile.getPath());
			if(violatesChroot(toFile)) {
				errString = "550 Invalid name or chroot violation\r\n";
				break mainblock;
			}
			File fromFile = sessionThread.getRenameFrom();
			if(fromFile == null) {
				errString = "550 Rename error, maybe RNFR not sent\r\n";
				break mainblock;
			}
			if(!fromFile.renameTo(toFile)) {
				errString = "550 Error during rename operation\r\n";
				break mainblock;
			}
		}
		if(errString != null) {
			sessionThread.writeString(errString);
			myLog.l(Log.INFO, "RNFR failed: " + errString.trim());
		} else {
			sessionThread.writeString("250 rename successful\r\n");
			
			// renamed success, so need to scan media store.
			updateMediaStore(toFile);
		}
		sessionThread.setRenameFrom(null);
		myLog.l(Log.DEBUG, "RNTO finished");
	}
	
	// Gionee <lilg><2015-08-12> modify for CR01532322 begin
	private void updateMediaStore(File newFile) {

		File oldFile = sessionThread.getRenameFrom();
		Log.d(TAG, "oldFile: " + oldFile.getAbsolutePath());
		Log.d(TAG, "newFile: " + newFile.getAbsolutePath());

		// delete old item
		int deleteNum = FTPServerApp.getInstance().getContentResolver().delete(MediaStore.Files.getContentUri("external"), MediaStore.Files.FileColumns.DATA + " = " + "'" + oldFile.getAbsolutePath() + "'", null);
		Log.d(TAG, "deleteNum: " + deleteNum);
		
		// insert new item
		Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		scanIntent.setData(Uri.fromFile(newFile));
		FTPServerApp.getInstance().sendBroadcast(scanIntent);

	}
	// Gionee <lilg><2015-08-12> modify for CR01532322 end
}
