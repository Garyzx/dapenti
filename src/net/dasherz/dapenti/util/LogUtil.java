package net.dasherz.dapenti.util;

import net.dasherz.dapenti.BuildConfig;
import android.util.Log;

public class LogUtil {
	private final static boolean debug = BuildConfig.DEBUG;

	public static void d(String tag, String string) {
		if (debug) {
			Log.d(tag, string);
		}

	}

}
