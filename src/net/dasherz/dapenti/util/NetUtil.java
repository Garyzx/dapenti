package net.dasherz.dapenti.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import net.dasherz.dapenti.constant.Constants;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class NetUtil {
	// Given a string representation of a URL, sets up a connection and gets
	// an input stream.
	public static InputStream downloadUrl(String urlString) throws IOException {
		URL url = new URL(urlString);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setReadTimeout(10000 /* milliseconds */);
		conn.setConnectTimeout(15000 /* milliseconds */);
		conn.setRequestMethod("GET");
		conn.setDoInput(true);
		// Starts the query
		conn.connect();
		InputStream stream = conn.getInputStream();
		return stream;
	}

	// convert InputStream to String
	// for web show only
	public static String getContentOfURL(String url, boolean optimizeHTML) throws IOException {
		InputStream is = downloadUrl(url);
		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();
		String line = null;
		try {
			br = new BufferedReader(new InputStreamReader(is));
			while ((line = br.readLine()) != null) {
				if (optimizeHTML) {
					line = optimizeHTMLCode(line);
				}
				sb.append(line);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return sb.toString();
	}

	private static String optimizeHTMLCode(String line) {
		if (isNormalGif(line)) {
			line = line.replace("<IMG", "<IMG width=\"100%\"");
		}
		line = line.replace("<p>&nbsp;</p>", "").replace("<p><strong><font size=\"3\"></font></strong>&nbsp;</p>", "");
		line = line.replaceAll("<IFRAME[^>]+?></IFRAME>", "");
		line = line.replaceAll("(<A[^>]+?>)(http.+?)(</A>)", "$1¡¥Ω”µÿ÷∑$3");
		return line;
	}

	private static boolean isNormalGif(String line) {
		if (line.contains("<IMG") || line.contains("<img")) {
			if (line.contains("type=\"face\"")) {
				return false;
			} else {
				return true;
			}
		}
		return false;
	}

	public static boolean getNetworkMode(Context ctx) {
		ConnectivityManager connectivityManager = (ConnectivityManager) ctx
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
		if (activeNetInfo != null && activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {
			Log.d("NET", "in WIFI");
			return true;
		}
		Log.d("NET", "no WIFI");
		return false;
	}

	public static boolean whetherBlockImage(Context ctx) {
		SharedPreferences settings = ctx.getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE);
		boolean loadPicture = settings.getBoolean("loadPicture", true);
		boolean loadPictureUnderWIFI = settings.getBoolean("loadPictureUnderWIFI", true);
		boolean isUnderWIFI = getNetworkMode(ctx);

		if (!loadPicture) {
			return true;
		} else {
			if (loadPictureUnderWIFI == false) {
				return false;
			} else if (loadPictureUnderWIFI && isUnderWIFI) {
				return false;
			} else {
				return true;
			}
		}
	}
}
