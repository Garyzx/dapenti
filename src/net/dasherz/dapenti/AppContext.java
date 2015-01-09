package net.dasherz.dapenti;

import net.dasherz.dapenti.database.DBConstants;
import net.dasherz.dapenti.database.DaoMaster;
import net.dasherz.dapenti.database.DaoMaster.OpenHelper;
import net.dasherz.dapenti.database.DaoSession;
import android.app.Application;
import android.content.Context;

public class AppContext extends Application {
	private static DaoMaster daoMaster;
	private static DaoSession daoSession;

	/**
	 * 取得DaoMaster
	 * 
	 * @param context
	 * @return
	 */
	public static DaoMaster getDaoMaster(Context context) {
		if (daoMaster == null) {
			OpenHelper helper = new DaoMaster.DevOpenHelper(context, DBConstants.DATABASE_NAME, null);
			daoMaster = new DaoMaster(helper.getWritableDatabase());
		}
		return daoMaster;
	}

	/**
	 * 取得DaoSession
	 * 
	 * @param context
	 * @return
	 */
	public static DaoSession getDaoSession(Context context) {
		if (daoSession == null) {
			if (daoMaster == null) {
				daoMaster = getDaoMaster(context);
			}
			daoSession = daoMaster.newSession();
		}
		return daoSession;
	}
}
