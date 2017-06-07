package com.dq.swiftp;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.text.TextUtils;
import android.util.Log;

import com.dq.fileftpserver.R;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultStorageManager {

    private static final String TAG = "DefaultStorageManager";

    private static final String NOT_PRESENT = "not_present";

    public static final int TYPE_OTHER = 0;
    public static final int TYPE_INTERNAL = 1;
    public static final int TYPE_SDCARD = 2;
    public static final int TYPE_USB = 3;

    public static final int STATE_UNMOUNTED = 0;
    public static final int STATE_CHECKING = 1;
    public static final int STATE_MOUNTED = 2;
    public static final int STATE_MOUNTED_READ_ONLY = 3;
    public static final int STATE_FORMATTING = 4;
    public static final int STATE_EJECTING = 5;
    public static final int STATE_UNMOUNTABLE = 6;
    public static final int STATE_REMOVED = 7;
    public static final int STATE_BAD_REMOVAL = 8;

    public static final String STR_OTHER = "otherStorage";
    public static final String STR_INTERNAL = "internalStorage";
    public static final String STR_SDCARD = "sdCard";
    public static final String STR_USB = "UsbStorage";

    public static final int VERSION_SDK = 23;

    private static DefaultStorageManager sInstance = new DefaultStorageManager();
    private static StorageManager mStorageManager = null;

    /**
     * storage info
     */
    private Map<Integer, StorageItem> mMountPointMap;

    /**
     * root path info
     * /storage/emulated/0 -> /storage/emulated
     * /storage/sdcard -> /storage
     */
    private List<String> mRootPathList;

    private Object mLock = new Object();

    private onUsbStateChangedListener mUsbStateChangedListener;
    //Gionee <wangpan><2016-12-22> add for 51825 begin
    private String ROOT_PATH;
    //Gionee <wangpan><2016-12-22> add for 51825 end

    public interface onUsbStateChangedListener {
        public void onUsbStateChanged(int state);
    }

    private DefaultStorageManager() {
        // Gionee <wangpan><2016-03-03> modify for CR01641568 begin
        mMountPointMap = new ConcurrentHashMap<Integer, StorageItem>();
        mRootPathList = new CopyOnWriteArrayList<String>();
        // Gionee <wangpan><2016-03-03> modify for CR01641568 end
    }

    public static DefaultStorageManager getInstance() {
        return sInstance;
    }

    public void updateMountPointList(Context context) {
        updateMountPointList(context, -1);
    }

    public void updateMountPointList(Context context, int state) {
        initStorageManager(context);
        new StorageAsyncTask().execute(context, state);
    }

    public void updateMountPointList(Context context, String state) {
        initStorageManager(context);
        new StorageAsyncTask().execute(context, switchState(state));
    }

    private void initStorageManager(Context context) {
        try {
            if (mStorageManager == null) {
                mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            }
        } catch (Exception e) {
            Log.e(TAG, "init storage manager exception.", e);
        }
    }

    private class StorageAsyncTask extends AsyncTask<Object, Integer, Map<Integer, StorageItem>> {

        private Context context;
        private int state = -1;

        @Override
        protected Map<Integer, StorageItem> doInBackground(Object... params) {
            Log.d(TAG, "doInBackground.");
            context = (Context) params[0];
            state = (Integer) params[1];

            Map<Integer, StorageItem> mountPointMap = new HashMap<Integer, StorageItem>();

            updateMountList(context, mountPointMap); // update state internal, sdcard
//            updateUsbOtg(mountPointMap); // update state usb for android 6.0

            return mountPointMap;
        }

        @Override
        protected void onPostExecute(Map<Integer, StorageItem> mountPointMap) {
            Log.d(TAG, "onPostExecute.");

            updateMountPointMap(mountPointMap);
            dumpStorage();

            updateRootPathList();
            dumpRootPath();

            // call back to update ui
            if (mUsbStateChangedListener != null) {
                mUsbStateChangedListener.onUsbStateChanged(state);
            }

        }
    }

    private void updateMountPointMap(Map<Integer, StorageItem> mountPointMap) {
        synchronized (mLock) {
            mMountPointMap.clear();
            mMountPointMap.putAll(mountPointMap);
        }
    }

    private void updateMountList(Context context, Map<Integer, StorageItem> mountPointMap) {
        try {
            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");

            StorageVolume[] storageVolumeArray = (StorageVolume[]) getVolumeList.invoke(mStorageManager);
            if (storageVolumeArray == null || storageVolumeArray.length <= 0) {
                Log.e(TAG, "initMountList, storage vol list == null or length <= 0.");
                return;
            }

            for (int i = 0; i < storageVolumeArray.length; i++) {
                StorageVolume volume = storageVolumeArray[i];
                Method getPath = volume.getClass().getMethod("getPath");
                String path = (String) getPath.invoke(volume);
                if (NOT_PRESENT.equals(getVolumeState(path))) {
                    Log.d(TAG, "path: " + path + " state is " + NOT_PRESENT);
                    continue;
                }

                String desc = volume.getDescription(context);
                Log.d(TAG, "storage desc: " + desc);

                if (isInternalStorage(context, volume)) {
                    initInternal(volume, mountPointMap);
                } else if (isExternalStorage(context, volume)) {
                    initExternal(volume, mountPointMap);
                } /*else if (isUsbStorage(context, volume)) { // android 5.0
                initUsb(volume, mountPointMap);
            }*/
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isInternalStorage(Context context, StorageVolume volume) {
        if (context.getString(R.string.description_internal).equals(volume.getDescription(context)) || context.getString(R.string.description_storage_internal).equals(volume.getDescription(context))) {  // modify for Android N
            return true;
        } else {
            return false;
        }
    }

    private void initInternal(StorageVolume volume, Map<Integer, StorageItem> mountPointMap) {
        try {
            Method getPath = volume.getClass().getMethod("getPath");
            String path = (String) getPath.invoke(volume);
            StorageItem internalItem = mountPointMap.get(TYPE_INTERNAL);
            if (internalItem == null) {
                internalItem = new StorageItem(TYPE_INTERNAL, STR_INTERNAL, path, switchState(getVolumeState(path)));
                mountPointMap.put(TYPE_INTERNAL, internalItem);
            } else {
                internalItem.update(TYPE_INTERNAL, STR_INTERNAL, path, switchState(getVolumeState(path)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public String getVolumeState(String mountPoint) {
        final StorageVolume vol = mStorageManager.getStorageVolume(new File(mountPoint));
        if (vol != null) {
            return vol.getState();
        } else {
            return Environment.MEDIA_UNKNOWN;
        }
    }
    private boolean isExternalStorage(Context context, StorageVolume volume) {
        String desc = volume.getDescription(context);
        if (TextUtils.isEmpty(desc)) {
            return false;
        }
        desc = desc.replaceAll(" ", "");
        if (desc.contains(context.getString(R.string.description_external))) {   // modify for CR01623155
            return true;
        } else {
            return false;
        }
    }

    private void initExternal(StorageVolume volume, Map<Integer, StorageItem> mountPointMap) {
        try{
            Method getPath = volume.getClass().getMethod("getPath");
            String path = (String) getPath.invoke(volume);
            StorageItem sdcardItem = mountPointMap.get(TYPE_SDCARD);
            if (sdcardItem == null) {
                sdcardItem = new StorageItem(TYPE_SDCARD, STR_SDCARD, path, switchState(getVolumeState(path)));
                mountPointMap.put(TYPE_SDCARD, sdcardItem);
            } else {
                sdcardItem.update(TYPE_SDCARD, STR_SDCARD, path, switchState(getVolumeState(path)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    private boolean isUsbStorage(Context context, StorageVolume volume) {
//        if (context.getString(R.string.description_usbotg).equals(volume.getDescription(context)) || context.getString(R.string.description_storage_usb).equals(volume.getDescription(context))) { // modify for Android N
//            return true;
//        } else {
//            return false;
//        }
//    }
//
//    private void initUsb(StorageVolume volume, Map<Integer, StorageItem> mountPointMap) {
//        StorageItem usbItem = mountPointMap.get(TYPE_USB);
//        if (usbItem == null) {
//            usbItem = new StorageItem(TYPE_USB, STR_USB, volume.getPath(), switchState(mStorageManager.getVolumeState(volume.getPath())));
//            mountPointMap.put(TYPE_USB, usbItem);
//        } else {
//            usbItem.update(TYPE_USB, STR_USB, volume.getPath(), switchState(mStorageManager.getVolumeState(volume.getPath())));
//        }
//    }

//    private void updateUsbOtg(Map<Integer, StorageItem> mountPointMap) {
//        int sdkVersion=android.os.Build.VERSION.SDK_INT;
//        Log.d(TAG, "updateUsbOtg, sdk version: " + sdkVersion);
//
//        if(sdkVersion >= VERSION_SDK) {
//            List<VolumeInfo> vols = mStorageManager.getVolumes();
//            if (vols == null) {
//                Log.e(TAG, "fols == null.");
//                return;
//            }
//            for (VolumeInfo vol : vols) {
//                boolean isUsb = isUsbOtg(vol);
//                if (isUsb) {
//                    initUsb(vol, mountPointMap);
//                }
//            }
//        }
//    }

//    private boolean isUsbOtg(VolumeInfo vol) {
//        boolean isUsbOtg = false;
//
//        String diskId = vol.getDiskId();
//        Log.d(TAG, "diskId: " + diskId);
//
//        if (diskId != null) {
//            // for usb otg, the disk id same as disk:8:x
//            String[] idSplit = diskId.split(":");
//            if (idSplit != null && idSplit.length == 2) {
//                if (idSplit[1].startsWith("8,")) {
//                    Log.d(TAG, "this is a usb otg");
//                    isUsbOtg = true;
//                }
//            }
//        }
//        return isUsbOtg;
//    }

//    private void initUsb(VolumeInfo volume, Map<Integer, StorageItem> mountPointMap) {
//
//        if(volume == null || volume.getPath() == null) {    // for CR01624689
//            Log.e(TAG, "volume == null || volume.getPath() == null.");
//            return;
//        }
//
//        StorageItem usbItem = mountPointMap.get(TYPE_USB);
//        if (usbItem == null) {
//            usbItem = new StorageItem(TYPE_USB, STR_USB, volume.getPath().getAbsolutePath(), volume.getState());
//            mountPointMap.put(TYPE_USB, usbItem);
//        } else {
//            usbItem.update(TYPE_USB, STR_USB, volume.getPath().getAbsolutePath(), volume.getState());
//        }
//    }

    private void updateRootPathList() {
        if (getStorageMountedCount() <= 0) {
            return;
        }

        synchronized (mLock) {
            // clear
            mRootPathList.clear();
            // update
            List<StorageItem> list = getMountedStorageList();
            List<String> rootPathList = new ArrayList<String>();
            for (StorageItem item : list) {
                String[] pathArray = item.getPath().split("/");
                String lastPath = pathArray[pathArray.length - 1];
                rootPathList.add(item.getPath().substring(0, item.getPath().indexOf(lastPath) - 1)); // /storage/emulated
            }
            mRootPathList.addAll(rootPathList);
        }
    }

    public int switchState(String stringState) {
        Log.d(TAG, "switch state: " + stringState);

        int state = -1;
        if (Environment.MEDIA_UNMOUNTED.equals(stringState) || Intent.ACTION_MEDIA_UNMOUNTED.equals(stringState)) {
            state = 0;
        } else if (Environment.MEDIA_CHECKING.equals(stringState)) {
            state = 1;
        } else if (Environment.MEDIA_MOUNTED.equals(stringState) || Intent.ACTION_MEDIA_MOUNTED.equals(stringState)) {
            state = 2;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(stringState)) {
            state = 3;
        } else if (Environment.MEDIA_UNMOUNTABLE.equals(stringState)) {
            state = 6;
        } else if (Environment.MEDIA_REMOVED.equals(stringState) || Intent.ACTION_MEDIA_EJECT.equals(stringState)) {
            state = 7;
        } else if (Environment.MEDIA_BAD_REMOVAL.equals(stringState)) {
            state = 8;
        } else {
            state = -1;
        }

        return state;
    }

    public String switchStateToString(int intState) {
        Log.d(TAG, "switchStateToString, state: " + intState);

        String state = null;
        if (intState == STATE_UNMOUNTED) {
            state = Environment.MEDIA_UNMOUNTED;
        } else if (intState == STATE_CHECKING) {
            state = Environment.MEDIA_CHECKING;
        } else if (intState == STATE_MOUNTED) {
            state = Environment.MEDIA_MOUNTED;
        } else if (intState == STATE_MOUNTED_READ_ONLY) {
            state = Environment.MEDIA_MOUNTED_READ_ONLY;
        } else if (intState == STATE_UNMOUNTABLE) {
            state = Environment.MEDIA_UNMOUNTABLE;
        } else if (intState == STATE_REMOVED) {
            state = Environment.MEDIA_REMOVED;
        } else if (intState == STATE_BAD_REMOVAL) {
            state = Environment.MEDIA_BAD_REMOVAL;
        } else {
            state = null;
        }
        return state;
    }

    private void dumpStorage() {
        Set<Entry<Integer, StorageItem>> entrySet = mMountPointMap.entrySet();
        for (Entry<Integer, StorageItem> entry : entrySet) {
            Log.d(TAG, "mount itm: " + entry.getValue().toString());
        }
    }

    private void dumpRootPath() {
        for (String item : mRootPathList) {
            Log.d(TAG, "root path item: " + item.toString());
        }
    }

    public String getStorageMountPath(int type) {
        String mountPath = null;
        try {
            switch (type) {
                case TYPE_INTERNAL:
                    if (mMountPointMap.get(TYPE_INTERNAL) != null) {
                        mountPath = mMountPointMap.get(TYPE_INTERNAL).getPath();
                    }
                    break;
                case TYPE_SDCARD:
                    if (mMountPointMap.get(TYPE_SDCARD) != null) {
                        mountPath = mMountPointMap.get(TYPE_SDCARD).getPath();
                    }
                    break;
                case TYPE_USB:
                    if (mMountPointMap.get(TYPE_USB) != null) {
                        mountPath = mMountPointMap.get(TYPE_USB).getPath();
                    }
                    break;
                default:
                    break;
            }
        } catch (Exception e) {     // modify for CR01736956
            Log.e(TAG, "getStorageMountPath exception for type: " + type, e);
        }
        return mountPath;
    }

    public boolean isStorageMounted(String path) {
        Log.d(TAG, "isStorageMounted, path: " + path);
        boolean isMounted = false;
        List<StorageItem> storageList = getStorageList();
        for (StorageItem item : storageList) {
            if (item.getPath().equals(path) && item.getState() == STATE_MOUNTED) {
                isMounted = true;
                break;
            }
        }
        return isMounted;
    }

    public boolean isStorageMounted(int type) { // TODO
        boolean isMounted = false;
        switch (type) {
            case TYPE_INTERNAL:
                if (mMountPointMap.get(TYPE_INTERNAL) != null && mMountPointMap.get(TYPE_INTERNAL).getState() == STATE_MOUNTED) {
                    isMounted = true;
                }
                break;
            case TYPE_SDCARD:
                if (mMountPointMap.get(TYPE_SDCARD) != null && mMountPointMap.get(TYPE_SDCARD).getState() == STATE_MOUNTED) {
                    isMounted = true;
                }
                break;
            case TYPE_USB:
                if (mMountPointMap.get(TYPE_USB) != null && mMountPointMap.get(TYPE_USB).getState() == STATE_MOUNTED) {
                    isMounted = true;
                }
                break;
            default:
                break;
        }
        return isMounted;
    }

    /*private boolean isStorageMounted(StorageVolume volume) {//Performance - Private method is never called
        return isStorageMounted(volume.getPath());
    }*/

    public boolean isStorage(String path) {
        Collection<StorageItem> collection = mMountPointMap.values();
        for (StorageItem item : collection) {
            if (item.getPath().equals(path)) {
                return true;
            }
        }
        return false;
    }

    public int getStorageMountedCount() {
        int count = 0;
        Collection<StorageItem> collection = mMountPointMap.values();
        for (StorageItem item : collection) {
            if (item.getState() == STATE_MOUNTED) {
                count++;
            }
        }
        return count;
    }

    /**
     * get storage item from cache map which state is mounted
     *
     * @return
     */
    public List<StorageItem> getMountedStorageList() {
        List<StorageItem> mountedStorageList = new ArrayList<StorageItem>();
        Collection<StorageItem> collection = mMountPointMap.values();
        for (StorageItem item : collection) {
            if (item.getState() == STATE_MOUNTED) {
                mountedStorageList.add(item);
            }
        }
        return mountedStorageList;
    }

    public List<StorageItem> getMountedStorageListOnSort() {
        List<StorageItem> mountedStorageList = new ArrayList<StorageItem>();
        StorageItem internalItem = mMountPointMap.get(TYPE_INTERNAL);
        if (internalItem != null && internalItem.getState() == STATE_MOUNTED) {
            mountedStorageList.add(internalItem);
        }
        StorageItem sdcardItem = mMountPointMap.get(TYPE_SDCARD);
        if (sdcardItem != null && sdcardItem.getState() == STATE_MOUNTED) {
            mountedStorageList.add(sdcardItem);
        }
        StorageItem usbItem = mMountPointMap.get(TYPE_USB);
        if (usbItem != null && usbItem.getState() == STATE_MOUNTED) {
            mountedStorageList.add(usbItem);
        }
        return mountedStorageList;
    }

    public File[] getMountedStorageArray() {
        List<StorageItem> mountedStorageList = getMountedStorageList();

        int size = mountedStorageList.size();
        File[] mountedStorageArray = new File[size];
        for (int i = 0; i < size; i++) {
            mountedStorageArray[i] = new File(mountedStorageList.get(i).getPath());
        }
        return mountedStorageArray;
    }

    public File[] getMountedStorageArrayOnSort() {
        //Gionee <wangpan><2016-03-17> modify for CR01653692 begin
        List<File> list = new ArrayList<File>();
        // Gionee <lilg><2016-09-26> modify for 2533 begin
        if (isStorageMounted(TYPE_INTERNAL)) {
            String internalPath = getStorageMountPath(TYPE_INTERNAL);
            if (TextUtils.isEmpty(internalPath)) {
                Log.e(TAG, "getMountedStorageArrayOnSort, internalPath is empty.");
            } else {
                list.add(new File(internalPath));
            }
        }
        if (isStorageMounted(TYPE_SDCARD)) {
            String sdCardPath = getStorageMountPath(TYPE_SDCARD);
            if (TextUtils.isEmpty(sdCardPath)) {
                Log.e(TAG, "getMountedStorageArrayOnSort, sdCardPath is empty.");
            } else {
                list.add(new File(sdCardPath));
            }
        }
        if (isStorageMounted(TYPE_USB)) {
            String usbPath = getStorageMountPath(TYPE_USB);
            if (TextUtils.isEmpty(usbPath)) {
                Log.e(TAG, "getMountedStorageArrayOnSort, usbPath is empty.");
            } else {
                list.add(new File(usbPath));
            }
        }
        return (File[]) list.toArray(new File[0]);
        // Gionee <lilg><2016-09-26> modify for 2533 end
        //Gionee <wangpan><2016-03-17> modify for CR01653692 end
    }

    /**
     * get storage item from cache map whatever its state is
     *
     * @return
     */
    public List<StorageItem> getStorageList() {
        List<StorageItem> storageList = new ArrayList<StorageItem>();
        Collection<StorageItem> collection = mMountPointMap.values();
        for (StorageItem item : collection) {
            if (item != null) {
                storageList.add(item);
            } else {
                Log.e(TAG, "getStorageList, StorageItem is null.");
            }
        }
        return storageList;
    }

    public List<String> getRootPathList() {
        return mRootPathList;
    }

    public boolean isRootPath(String path) {
        for (String item : mRootPathList) {
            if (item.equals(path)) {
                return true;
            }
        }
        return false;
    }

    //Gionee <wangpan><2016-12-22> add for 51825 begin
    public String getROOT_PATH() {
        try {
            for (String item : mRootPathList) {
                String[] tempPath = item.split("/");
                if (tempPath != null && tempPath.length > 0) {
                    ROOT_PATH = "/" + tempPath[1]; // /storage
                    Log.d(TAG, "ROOT_PATH: " + ROOT_PATH);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "isRootPath exception.", e);
            return "/storage";
        }
        if (null == ROOT_PATH) {
            ROOT_PATH = "/storage";
        }
        return ROOT_PATH;
    }

    //Gionee <wangpan><2016-12-22> add for 51825 end
    public int getRootPathLength(String path) {
        Log.d(TAG, "getRootLength, path: " + path);

        for (String item : mRootPathList) {
            if (path.startsWith(item)) {
                return item.length();
            }
        }
        return 1;
    }
    //Gionee liuwei 2013-10-23 add for CR00932833 end
    //Gionee <otg> <qudw> <2013-10-19> modify for CR00923978 end
}
