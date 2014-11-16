package net.dasherz.dapenti.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.dasherz.dapenti.xml.PentiItem;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class PentiDatabaseHelper extends SQLiteOpenHelper {

	public PentiDatabaseHelper(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DBConstants.CREATE_TABLE_TUGUA_SQL);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d("DB", "Currently no update.");
	}

	public int insertItemsIfNotExist(List<PentiItem> items, int contentType) {
		int itemUpdated = 0;
		for (PentiItem item : items) {
			Cursor cursor = this.getReadableDatabase().query(false, DBConstants.TABLE_TUGUA,
					new String[] { DBConstants.ITEM_TITLE }, "title=?", new String[] { item.getTitle() }, null, null,
					null, null);
			if (cursor.getCount() == 0) {
				ContentValues valus = new ContentValues();
				valus.put(DBConstants.ITEM_TITLE, item.getTitle());
				valus.put(DBConstants.ITEM_LINK, item.getLink());
				valus.put(DBConstants.ITEM_AUTHOR, item.getAuthor());
				valus.put(DBConstants.ITEM_PUB_DATE, item.getPubDate());
				valus.put(DBConstants.ITEM_DESCRIPTION, item.getDescription());
				valus.put(DBConstants.ITEM_CONTENT_TYPE, String.valueOf(contentType));

				this.getWritableDatabase().insert(DBConstants.TABLE_TUGUA, null, valus);
				itemUpdated++;
				Log.d("DB", "insert new record: " + item.getTitle());
			}
			cursor.close();
		}
		return itemUpdated;
	}

	public List<Map<String, String>> readItems(int contentType, int from, int to) {
		String offset = String.valueOf(from);
		String limit = String.valueOf(to - from);
		String type = String.valueOf(contentType);
		Cursor cursor = this.getReadableDatabase().rawQuery(DBConstants.SELECT_TUGUA,
				new String[] { type, limit, offset });
		List<Map<String, String>> data = new ArrayList<Map<String, String>>();
		while (cursor.moveToNext()) {
			Map<String, String> map = new HashMap<String, String>();
			map.put(DBConstants.ITEM_TITLE, cursor.getString(1));
			map.put(DBConstants.ITEM_LINK, cursor.getString(2));
			map.put(DBConstants.ITEM_AUTHOR, cursor.getString(3));
			map.put(DBConstants.ITEM_PUB_DATE, cursor.getString(4));
			map.put(DBConstants.ITEM_DESCRIPTION, cursor.getString(5));
			data.add(map);
		}
		Log.d("DB", "current data size: " + data.size());
		cursor.close();
		return data;
	}

	public int getCountForType(int contentType) {
		Cursor cursor = this.getReadableDatabase().rawQuery(DBConstants.SELECT_TUGUA_ALL,
				new String[] { String.valueOf(contentType) });
		return cursor.getCount();
	}

}
