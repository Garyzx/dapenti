package net.dasherz.dapenti;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.dasherz.dapenti.database.DBConstants;
import net.dasherz.dapenti.database.PentiDatabaseHelper;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.database.Cursor;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
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
		root = inflater.inflate(R.layout.list, container, false);
		listView = (ListView) root.findViewById(R.id.tuguaListView);
		dbhelper = new PentiDatabaseHelper(getActivity(), DBConstants.DATABASE_NAME, null, DBConstants.version);
		loadList();
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Log.d("ITEM", "Item was clicked: " + position);
				Toast.makeText(getActivity(), "Item clicked,  positon: " + position + "  id: " + id, Toast.LENGTH_LONG)
						.show();
			}
		});
		listView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				Log.d("ITEM", "Item was long clicked: " + position);
				Toast.makeText(getActivity(), "Item long clicked,  positon: " + position + "  id: " + id,
						Toast.LENGTH_LONG).show();
				return true;
			}
		});
		return root;
	}

	private void loadList() {
		List<Map<String, String>> data = readDataFromDatabase();
		SimpleAdapter adapter = new SimpleAdapter(getActivity(), data, android.R.layout.simple_list_item_1,
				new String[] { DBConstants.ITEM_TITLE }, new int[] { android.R.id.text1 });

		listView.setAdapter(adapter);
	}

	public void reloadList() {
		loadList();
		Log.d("TEST", "reload invoked.");
	}

	private List<Map<String, String>> readDataFromDatabase() {
		Cursor cursor = dbhelper.getReadableDatabase().rawQuery("select * from tugua_item", null);
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

}
