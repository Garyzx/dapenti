package net.dasherz.dapenti;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xmlpull.v1.XmlPullParserException;

import net.dasherz.dapenti.database.DBConstants;
import net.dasherz.dapenti.database.PentiDatabaseHelper;
import net.dasherz.dapenti.util.NetUtil;
import net.dasherz.dapenti.xml.TuguaXmlParser;
import net.dasherz.dapenti.xml.TuguaXmlParser.TuguaItem;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A simple {@link Fragment} subclass.
 * 
 */
public class TuguaFragment extends Fragment {

	PentiDatabaseHelper dbhelper;
	View root;
	ListView listView;
	int recordCount;
	private SwipeRefreshLayout swipeLayout;
	PentiAdapter adapter;

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (dbhelper != null) {
			dbhelper.close();
			Log.d("DB", "closing dbhelper.");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		recordCount = 0;
		root = inflater.inflate(R.layout.list, container, false);
		listView = (ListView) root.findViewById(R.id.tuguaListView);
		swipeLayout = (SwipeRefreshLayout) root.findViewById(R.id.swipe_container);
		swipeLayout.setOnRefreshListener(new OnRefreshListener() {

			@Override
			public void onRefresh() {
				getLatestData();

			}
		});
		dbhelper = new PentiDatabaseHelper(getActivity(), DBConstants.DATABASE_NAME, null, DBConstants.version);
		loadList();
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Log.d("ITEM", "Item was clicked: " + position);
				Toast.makeText(getActivity(), "Item clicked,  positon: " + position + "  id: " + id, Toast.LENGTH_SHORT)
						.show();
				Toast.makeText(getActivity(), listView.getAdapter().getItem(position).toString(), Toast.LENGTH_SHORT)
						.show();
				if (adapter.getItem(position).toString().equals(Constants.LOAD_MORE)) {
					List<Map<String, String>> extraData = readDataFromDatabase();
					adapter.getData().addAll(extraData);
					if (extraData.size() == 0) {
						adapter.setFooter(Constants.NO_MORE_NEW);
					}
					adapter.notifyDataSetChanged();
				} else if (position < adapter.getCount() - 1 && adapter.getData().get(position) instanceof Map) {
					Map<String, String> item = adapter.getData().get(position);
					Log.d("TEST", item.toString());
					Intent intent = new Intent(getActivity(), TuguaDetailActivity.class);
					intent.putExtra(DBConstants.ITEM_TITLE, item.get(DBConstants.ITEM_TITLE));
					intent.putExtra(DBConstants.ITEM_DESCRIPTION, item.get(DBConstants.ITEM_DESCRIPTION));
					startActivity(intent);
				}
			}
		});
		listView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				Log.d("ITEM", "Item was long clicked: " + position);
				Toast.makeText(getActivity(), "Item long clicked,  positon: " + position + "  id: " + id,
						Toast.LENGTH_SHORT).show();
				listView.setFocusable(false);
				listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
				return true;
			}
		});
		return root;
	}

	private void loadList() {

		List<Map<String, String>> data = readDataFromDatabase();
		if (data.size() == 0) {
			if (isTableEmpty()) {
				listView.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1,
						new String[] { "正在加载..." }));
				getLatestData();
			} else {
				Toast.makeText(getActivity(), "没有更多数据了", Toast.LENGTH_SHORT).show();
			}

		} else {
			// SimpleAdapter adapter = new SimpleAdapter(getActivity(), data,
			// android.R.layout.simple_list_item_1,
			// new String[] { DBConstants.ITEM_TITLE }, new int[] {
			// android.R.id.text1 });
			adapter = new PentiAdapter(getActivity(), data, DBConstants.ITEM_TITLE, Constants.LOAD_MORE);
			listView.setAdapter(adapter);

		}
	}

	private boolean isTableEmpty() {
		Cursor cursor = dbhelper.getReadableDatabase().rawQuery(DBConstants.SELECT_TUGUA_ALL, null);
		return cursor.getCount() == 0;
	}

	public void reloadList() {
		loadList();
		Log.d("TEST", "reload invoked.");
	}

	public void getLatestData() {
		new RefreshTuguaTask().execute(Constants.URL_TUGUA);
		Log.d("TEST", "get latest data.");
	}

	private List<Map<String, String>> readDataFromDatabase() {
		String limit = String.valueOf(recordCount);
		Cursor cursor = dbhelper.getReadableDatabase().rawQuery(DBConstants.SELECT_TUGUA, new String[] { limit });
		recordCount += cursor.getCount();
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
		Log.d("DB", "total size: " + data.size());
		cursor.close();
		return data;
	}

	private class RefreshTuguaTask extends AsyncTask<String, Void, List<TuguaItem>> {

		@Override
		protected List<TuguaItem> doInBackground(String... urls) {
			try {
				return loadXmlFromNetwork(urls[0]);
			} catch (IOException e) {
				e.printStackTrace();
				// return getResources().getString(R.string.connection_error);
			} catch (XmlPullParserException e) {
				e.printStackTrace();
				// return getResources().getString(R.string.xml_error);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(List<TuguaItem> items) {
			if (items == null) {
				Toast.makeText(getActivity(), "更新出错了", Toast.LENGTH_SHORT).show();
				swipeLayout.setRefreshing(false);
				return;
			}
			int itemUpdated = 0;
			PentiDatabaseHelper dbhelper = new PentiDatabaseHelper(getActivity(), DBConstants.DATABASE_NAME, null,
					DBConstants.version);
			// dbhelper.getWritableDatabase().execSQL("delete from tugua_item");
			for (TuguaItem item : items) {
				Cursor cursor = dbhelper.getReadableDatabase().query(false, DBConstants.TABLE_TUGUA,
						new String[] { DBConstants.ITEM_TITLE }, "title=?", new String[] { item.getTitle() }, null,
						null, null, null);
				if (cursor.getCount() == 0) {
					ContentValues valus = new ContentValues();
					valus.put(DBConstants.ITEM_TITLE, item.getTitle());
					valus.put(DBConstants.ITEM_LINK, item.getLink());
					valus.put(DBConstants.ITEM_AUTHOR, item.getAuthor());
					valus.put(DBConstants.ITEM_PUB_DATE, item.getPubDate());
					valus.put(DBConstants.ITEM_DESCRIPTION, item.getDescription());
					dbhelper.getWritableDatabase().insert(DBConstants.TABLE_TUGUA, null, valus);
					itemUpdated++;
					Log.d("DB", "insert new record: " + item.getTitle());
				}
			}
			dbhelper.close();
			if (itemUpdated > 0) {
				// load from beginning
				recordCount = 0;
				reloadList();
			} else {
				Toast.makeText(getActivity(), "已经是最新了", Toast.LENGTH_SHORT).show();
			}
			swipeLayout.setRefreshing(false);
		}
	}

	private List<TuguaItem> loadXmlFromNetwork(String urlString) throws XmlPullParserException, IOException,
			ParseException {
		InputStream stream = null;
		TuguaXmlParser stackOverflowXmlParser = new TuguaXmlParser();
		List<TuguaItem> items = null;

		try {
			stream = NetUtil.downloadUrl(urlString);
			items = stackOverflowXmlParser.parse(stream);
			// Makes sure that the InputStream is closed after the app is
			// finished using it.
		} finally {
			if (stream != null) {
				stream.close();
			}
		}

		return items;
	}

}
