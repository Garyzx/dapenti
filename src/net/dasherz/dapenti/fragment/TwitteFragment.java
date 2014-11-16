package net.dasherz.dapenti.fragment;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import net.dasherz.dapenti.R;
import net.dasherz.dapenti.constant.Constants;
import net.dasherz.dapenti.database.DBConstants;
import net.dasherz.dapenti.database.PentiDatabaseHelper;
import net.dasherz.dapenti.util.NetUtil;
import net.dasherz.dapenti.xml.PentiXmlParser;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

/**
 * A simple {@link Fragment} subclass.
 * 
 */
public class TwitteFragment extends Fragment {

	private ListView listView;
	private PentiAdapter adapter;
	private SwipeRefreshLayout swipeLayout;
	private PentiDatabaseHelper dbhelper;
	boolean isRefreshing = false;
	int recordCount = 0;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.list, container, false);
		recordCount = 0;
		listView = (ListView) root.findViewById(R.id.tuguaListView);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		listView.setMultiChoiceModeListener(new MultiChoiceModeListener() {
			int checkitemCount = 0;

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return true;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				checkitemCount = 0;
				adapter.clearSelection();
			}

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				MenuInflater inflater = getActivity().getMenuInflater();
				inflater.inflate(R.menu.list_select_menu, menu);
				return true;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				if (item.getItemId() == R.id.copy_title) {
					StringBuffer buffer = new StringBuffer();
					for (Integer integer : adapter.getCurrentCheckedPosition()) {
						buffer.append(adapter.getItem(integer));
					}
					Log.d("AD", buffer.toString());
					ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(
							android.content.Context.CLIPBOARD_SERVICE);
					ClipData clip = ClipData.newPlainText("titles", buffer.toString());
					clipboard.setPrimaryClip(clip);
					Toast.makeText(getActivity(), "已经复制标题到剪贴板。", Toast.LENGTH_SHORT).show();
				} else if (item.getItemId() == R.id.add_favourite) {
					// TODO
				}
				Toast.makeText(getActivity(), "add fav clicked.", Toast.LENGTH_SHORT).show();
				return true;
			}

			@Override
			public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
				if (checked) {
					checkitemCount++;
					adapter.setNewSelection(position, checked);
				} else {
					checkitemCount--;
					adapter.removeSelection(position);
				}
				mode.setTitle(checkitemCount + "个项目已选择");
			}
		});
		swipeLayout = (SwipeRefreshLayout) root.findViewById(R.id.swipe_container);
		swipeLayout.setColorSchemeColors(Color.BLACK, Color.BLUE, Color.GREEN, Color.YELLOW);
		swipeLayout.setOnRefreshListener(new OnRefreshListener() {

			@Override
			public void onRefresh() {
				getLatestData();

			}
		});
		if (adapter == null) {
			listView.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1,
					new String[] { "正在加载..." }));
		} else {
			listView.setAdapter(adapter);
		}
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (adapter == null || adapter.getItem(position) == null) {
					return;
				}
				if (adapter.getItem(position).toString().equals(Constants.LOAD_MORE)) {
					new LoadTwitteTask().execute();
					return;
				}
			}
		});
		dbhelper = new PentiDatabaseHelper(getActivity(), DBConstants.DATABASE_NAME, null, DBConstants.version);
		new LoadTwitteTask().execute();
		return root;
	}

	public void getLatestData() {
		if (!isRefreshing) {
			isRefreshing = true;
			new GetNewTwitteTask().execute(Constants.URL_TWITTE);
		}

	}

	public class GetNewTwitteTask extends AsyncTask<String, Void, Integer> {

		@Override
		protected void onPostExecute(Integer result) {

			if (result.intValue() > 0) {
				recordCount = 0;
				new LoadTwitteTask().execute();
			} else if (result.intValue() == -1) {
				Toast.makeText(getActivity(), "更新出错了", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(getActivity(), "已经是最新了", Toast.LENGTH_SHORT).show();
			}
			swipeLayout.setRefreshing(false);
			isRefreshing = false;
		}

		@Override
		protected Integer doInBackground(String... url) {
			InputStream stream = null;
			PentiXmlParser xmlParser = new PentiXmlParser();
			List<net.dasherz.dapenti.xml.PentiItem> items = null;

			try {
				stream = NetUtil.downloadUrl(url[0]);
				items = xmlParser.parse(stream);
				// Makes sure that the InputStream is closed after the app is
				// finished using it.
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (stream != null) {
					try {
						stream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			if (items == null) {
				return -1;
			}
			int itmeCount = dbhelper.insertItemsIfNotExist(items, DBConstants.CONTENT_TYPE_TWITTE);

			return itmeCount;
		}

	}

	public class LoadTwitteTask extends AsyncTask<Void, Void, List<Map<String, String>>> {

		@Override
		protected void onPostExecute(List<Map<String, String>> data) {
			if (data == null) {
				listView.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1,
						new String[] { "正在加载..." }));
				new GetNewTwitteTask().execute(Constants.URL_TWITTE);
				return;
			}
			if (data.size() == 0) {
				Toast.makeText(getActivity(), "没有更多数据了", Toast.LENGTH_SHORT).show();

			} else {
				if (adapter == null) {
					adapter = new PentiAdapter(getActivity(), data, DBConstants.ITEM_DESCRIPTION, Constants.LOAD_MORE);
					listView.setAdapter(adapter);
				} else {
					adapter.getData().addAll(data);
					adapter.notifyDataSetChanged();
				}

			}
		}

		/**
		 * return type: 0 no record in database, need to load data from web
		 */
		@Override
		protected List<Map<String, String>> doInBackground(Void... params) {
			if (dbhelper.getCountForType(DBConstants.CONTENT_TYPE_TWITTE) == 0) {
				return null;
			}
			List<Map<String, String>> data = dbhelper.readItems(DBConstants.CONTENT_TYPE_TWITTE, recordCount,
					recordCount + DBConstants.ROW_COUNT_EVERY_READ);
			recordCount += data.size();

			return data;
		}
	}

}
