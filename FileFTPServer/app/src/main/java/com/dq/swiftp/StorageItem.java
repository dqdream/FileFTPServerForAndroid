package com.dq.swiftp;

import android.util.Log;

public class StorageItem {
    
    /**
     * internal is 1
     * sdCard is 2
     * usb is 3
     */
    private int id;
    
	/**
	 * internal or sdCard or usb 
	 */
	private String name;

	private String path;

	/**
	 * Environment.MEDIA_MOUNTED ...
	 */
	private int state;

	public StorageItem() {
	}

	public StorageItem(int id, String name, String path, int state) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.state = state;
    }
	
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public int getState() {
		return state;
	}

	public void setState(int state) {
		this.state = state;
	}

	@Override
	public String toString() {
		return "{" + "\"id\":\"" + id + "\", \"name\":\"" + name + "\", \"path\":\"" + path + "\", \"state\":\"" + state + "\"}";
	}
	
	public void update(int id, String name, String path, int state) {
	    setId(id);
		setName(name);
		setPath(path);
		setState(state);
	}

}
