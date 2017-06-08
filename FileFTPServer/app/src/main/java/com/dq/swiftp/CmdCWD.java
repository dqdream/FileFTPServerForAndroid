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
import java.io.IOException;

import android.os.Environment;
import android.util.Log;

import com.dq.fileftpserver.LogUtil;

public class CmdCWD extends FtpCmd implements Runnable {
	private static final String TAG = "FtpCmdCWD";
	protected String input;
	
	public CmdCWD(SessionThread sessionThread, String input) {
		super(sessionThread, CmdCWD.class.toString());
		this.input = input;
	}
	
	public void run() {
	    Log.d(TAG, "run.");
		myLog.l(Log.DEBUG, "CWD executing");
		String param = getParameter(input);
		File newDir;
		String errString = null;
		mainblock: {
//			newDir = inputPathToChrootedFile(sessionThread.getWorkingDir(), param);
			newDir = Environment.getExternalStorageDirectory();
			// Ensure the new path does not violate the chroot restriction
			if(violatesChroot(newDir)) {
				errString = "550 Invalid name or chroot violation\r\n";
				sessionThread.writeString(errString);
				myLog.l(Log.INFO, errString);
				break mainblock;
			}
			LogUtil.d("ftp_vv",newDir.getAbsolutePath()+","+newDir.canRead()+","+!newDir.isDirectory());
			try {
				newDir = newDir.getCanonicalFile();
				if(!newDir.isDirectory()) {
					sessionThread.writeString("550 Can't CWD to invalid directory\r\n");
				} else if(newDir.canRead()) {
					sessionThread.setWorkingDir(newDir);
					sessionThread.writeString("250 CWD successful\r\n");
				} else {
					sessionThread.writeString("550 That path is inaccessible\r\n");
				}
			} catch(IOException e) {
				sessionThread.writeString("550 Invalid path\r\n");
				break mainblock;
			}
		}
		myLog.l(Log.DEBUG, "CWD complete");
	}
}
