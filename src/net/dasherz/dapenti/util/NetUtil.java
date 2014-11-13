package net.dasherz.dapenti.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

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

	public static String getContentOfURL(String url) throws IOException {
		return getStringFromInputStream(downloadUrl(url));
	}

	// convert InputStream to String
	// for web show only
	private static String getStringFromInputStream(InputStream is) {

		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();
		// List<String> contents = new ArrayList<String>();
		String line;
		try {

			br = new BufferedReader(new InputStreamReader(is));
			while ((line = br.readLine()) != null) {
				// to improve the load speed, maybe
				// FIXME
				if (line.contains("<IMG") && !line.contains("gif")) {
					line = line.replace("<IMG", "<IMG width=\"100%\"");
				}
				// contents.add(line);
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
}
