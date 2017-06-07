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

import android.util.Log;

public class Settings {
	/**
	 * soanr
	 * Malicious code vulnerability - Field should be package protected
	 */
	private static int inputBufferSize = 256;
	private static boolean allowOverwrite = false;
	private static int dataChunkSize = 8192;  // do file I/O in 8k chunks 
	private static int sessionMonitorScrollBack = 10;
	private static int serverLogScrollBack = 10;
	private static int uiLogLevel = Log.INFO;
	
	public static int getUiLogLevel() {
		return uiLogLevel;
	}

	public static void setUiLogLevel(int uiLogLevel) {
		Settings.uiLogLevel = uiLogLevel;
	}

	public static int getInputBufferSize() {
		return inputBufferSize;
	}

	public static void setInputBufferSize(int inputBufferSize) {
		Settings.inputBufferSize = inputBufferSize;
	}

	public static boolean isAllowOverwrite() {
		return allowOverwrite;
	}

	public static void setAllowOverwrite(boolean allowOverwrite) {
		Settings.allowOverwrite = allowOverwrite;
	}

	public static int getDataChunkSize() {
		return dataChunkSize;
	}

	public static void setDataChunkSize(int dataChunkSize) {
		Settings.dataChunkSize = dataChunkSize;
	}

	public static int getSessionMonitorScrollBack() {
		return sessionMonitorScrollBack;
	}

	public static void setSessionMonitorScrollBack(
			int sessionMonitorScrollBack) 
	{
		Settings.sessionMonitorScrollBack = sessionMonitorScrollBack;
	}

	public static int getServerLogScrollBack() {
		return serverLogScrollBack;
	}

	public static void setLogScrollBack(int serverLogScrollBack) {
		Settings.serverLogScrollBack = serverLogScrollBack;
	}
	
	
}
