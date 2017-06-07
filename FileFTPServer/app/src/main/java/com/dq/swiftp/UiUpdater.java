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

import java.util.ArrayList;
import java.util.List;

import android.os.Handler;
import android.util.Log;

public class UiUpdater {
    private static final String TAG = "FileManager_UiUpdater";
	protected static MyLog myLog = new MyLog("UiUpdater");
	protected static final List<Handler> clients = new ArrayList<Handler>();
	
	public static void registerClient(Handler client) {
		if(!clients.contains(client)) {
			clients.add(client);
		}
	}
	
	public static void unregisterClient(Handler client) {
		while(clients.contains(client)) {
			clients.remove(client);
		}
	}
	
	public static void updateClients() {
	    Log.d(TAG, "updateClients.");
		//myLog.l(Log.DEBUG, "UI update");
		//Log.d("UiUpdate", "Update now");
		for (Handler client : clients) {
			client.sendEmptyMessage(0);
		}
	}
}
