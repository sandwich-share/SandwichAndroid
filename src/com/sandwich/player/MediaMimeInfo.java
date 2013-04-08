package com.sandwich.player;

import java.util.HashMap;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.os.Build;

public class MediaMimeInfo {
	static HashMap<String, String> mimeMapping = new HashMap<String, String>();
	
	static {
		// All lower case mime types and extensions
		mimeMapping.put("m4a", "audio/mp4");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1)
			mimeMapping.put("aac", "audio/x-aac");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1)
			mimeMapping.put("flac", "audio/x-flac");
		mimeMapping.put("mp3", "audio/mpeg");
		mimeMapping.put("ogg", "audio/ogg");
		mimeMapping.put("wav", "audio/vnd.wave");
		
		mimeMapping.put("3gp", "video/3gpp");
		mimeMapping.put("mp4", "video/mp4");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			mimeMapping.put("mkv", "video/x-matroska");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			mimeMapping.put("webm", "video/webm");
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			mimeMapping.put("ts", "video/mp2t");
	}
	
	@SuppressLint("DefaultLocale")
	public static String getMimeTypeForPath(String path)
	{
		String extension = path;
		
		if (extension.lastIndexOf('.') >= 0)
			extension = extension.substring(extension.lastIndexOf('.') + 1);
		
		extension = extension.toLowerCase(Locale.US);
		
		return mimeMapping.get(extension);
	}
}
