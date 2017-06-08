package com.dq.fileftpserver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Message
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.dq.swiftp.Defaults
import com.dq.swiftp.Globals
import com.dq.swiftp.UiUpdater
import java.io.File


/**
 * A placeholder fragment containing a simple view.
 */
class MainActivityFragment : Fragment() {
    val TAG: String = "FtpFragment"
    var contentView: View? = null;
    var text_wifi: TextView? = null;
    var text_ip: TextView? = null;
    var btn_ftp: Button? = null;
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        contentView = inflater.inflate(R.layout.fragment_main, container, false)
        text_wifi = contentView?.findViewById(R.id.text_wifi) as TextView?
        text_ip = contentView?.findViewById(R.id.text_ip) as TextView?
        btn_ftp = contentView?.findViewById(R.id.btn_ftp) as Button?
        init()
        return contentView
    }

    fun init() {
        var myContext: Context? = Globals.getContext()
        if (myContext == null) {
            myContext = activity.applicationContext
            if (myContext == null) {
                throw NullPointerException("Null context!?!?!?")
            }
            Globals.setContext(myContext)
        }
        btn_ftp?.setOnClickListener {
            Globals.setLastError(null)

            val rootDir = Defaults.chrootDir
            val chrootDir = File(rootDir)

            if (!chrootDir.isDirectory)
                return@setOnClickListener

            val context = activity.applicationContext
            val intent = Intent(context, FTPServerService::class.java)

            Globals.setChrootDir(chrootDir)
            LogUtil.d(TAG, "onClick" + Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
            if (!FTPServerService.isRunning()) {
                if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                    context.startService(intent)
                }
            } else {
                context.stopService(intent)
            }
        }
        text_wifi?.setOnClickListener {
            val intent = Intent(
                    android.provider.Settings.ACTION_WIFI_SETTINGS)
            startActivity(intent)
        }
        UiUpdater.registerClient(handler)
        updateUi()
        val filter = IntentFilter()
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        activity.registerReceiver(wifiReceiver, filter)
    }

    var wifiReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            LogUtil.d(TAG, "wifiReceiver, onReceive, action: " + intent.action)
            updateUi()
        }
    }

    fun updateUi() {
        LogUtil.d(TAG, "updateUi.")

        val wifiMgr = activity.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        var info = wifiMgr.connectionInfo
        val wifiId = info?.ssid
        val isWifiReady = FTPServerService.isWifiEnabled()
        LogUtil.d(TAG, "wifiId: $wifiId, isWifiReady: $isWifiReady")

        text_wifi?.text = if (isWifiReady) wifiId else getString(R.string.no_wifi_hint)
        if (isWifiReady) {
            text_wifi?.setTextColor(Color.argb(255, 68, 68, 68))
        } else {
            text_wifi?.setTextColor(Color.argb(178, 68, 68, 68))
        }

        val running = FTPServerService.isRunning()
        if (running) {
            LogUtil.d(TAG, "updateUi: server is running")
            // Put correct text in start/stop button
            // Fill in wifi status and address
            val address = FTPServerService.getWifiIp()
            if (address != null) {
                val port = ":" + FTPServerService.getPort()
                text_ip?.text = "ftp://" + address.hostAddress + if (FTPServerService.getPort() == 21) "" else port

            } else {
                // could not get IP address, stop the service
                val context = activity.applicationContext
                val intent = Intent(context, FTPServerService::class.java)
                context.stopService(intent)
                text_ip?.text = ""
            }
        }

        btn_ftp?.isEnabled = isWifiReady
        if (isWifiReady) {
            btn_ftp?.setText(if (running) R.string.stop_server else R.string.start_server)
        } else {
            if (FTPServerService.isRunning()) {
                val context = activity.applicationContext
                val intent = Intent(context, FTPServerService::class.java)
                context.stopService(intent)
            }

            btn_ftp?.text = "没有网络"
        }

        text_ip?.visibility = if (running) View.VISIBLE else View.INVISIBLE
    }


    var handler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                0 // We are being told to do a UI update
                -> {
                    // If more than one UI update is queued up, we only need to
                    // do one.
                    removeMessages(0)
                    updateUi()
                }
                1 // We are being told to display an error message
                -> removeMessages(1)
                else -> {
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        UiUpdater.unregisterClient(handler);
        activity.unregisterReceiver(wifiReceiver);
    }

}
