package net.dasherz.dapenti.fragment;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.dasherz.dapenti.R;
import net.dasherz.dapenti.activity.TuguaDetailActivity;
import net.dasherz.dapenti.constant.Constants;
import net.dasherz.dapenti.database.DBConstants;
import net.dasherz.dapenti.database.PentiDatabaseHelper;
import net.dasherz.dapenti.util.NetUtil;
import net.dasherz.dapenti.xml.PentiXmlParser;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
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
public abstract class PentiBaseFragment extends Fragment {

	private ListView listView;
	private PentiAdapter adapter;
	private SwipeRefreshLayout swipeLayout;
	private PentiDatabaseHelper dbhelper;
	boolean isRefreshing = false;
	int recordCount = 0;

	abstract int getContentType();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.list, container, false);
		listView = (ListView) root.findViewById(R.id.pentiListView);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		listView.setMultiChoiceModeListener(new MultiChoiceModeListener() {
			int checkedItemCount = 0;

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return true;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				checkedItemCount = 0;
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
					ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(
							android.content.Context.CLIPBOARD_SERVICE);
					ClipData clip = ClipData.newPlainText("titles", buffer.toString());
					clipboard.setPrimaryClip(clip);
					Toast.makeText(getActivity(), "已经复制标题到剪贴板。", Toast.LENGTH_SHORT).show();
				} else if (item.getItemId() == R.id.add_favourite) {
					// TODO
					final List<Long> favItemId = new ArrayList<>();
					for (Integer integer : adapter.getCurrentCheckedPosition()) {
						favItemId.add(adapter.getItemId(integer));
					}
					new Thread() {

						@Override
						public void run() {
							dbhelper.addToFav(favItemId);
						}
					}.start();

					Toast.makeText(getActivity(), "已经添加到收藏。", Toast.LENGTH_SHORT).show();
				}
				// Toast.makeText(getActivity(), "add fav clicked.",
				// Toast.LENGTH_SHORT).show();
				mode.finish();
				return true;
			}

			@Override
			public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
				if (checked) {
					checkedItemCount++;
					adapter.setNewSelection(position, checked);
				} else {
					checkedItemCount--;
					adapter.removeSelection(position);
				}
				mode.setTitle(checkedItemCount + "个项目已选择");
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
				// if adapter is not initialized
				if (adapter == null || adapter.getItem(position) == null) {
					return;
				}
				// if user click on load more item
				if (adapter.getItem(position).toString().equals(Constants.LOAD_MORE)) {
					new LoadItemTask().execute();
					return;
				}
				// if user click on twitte, then just return
				if (getContentType() == DBConstants.CONTENT_TYPE_TWITTE) {
					return;
				}
				// if user clicked on an real item
				if (position < adapter.getCount() - 1 && adapter.getData().get(position) instanceof Map) {
					Map<String, String> item = adapter.getData().get(position);
					Log.d("TUGUA", "Opening new activity to show web page");
					Intent intent = new Intent(getActivity(), TuguaDetailActivity.class);
					intent.putExtra(DBConstants.ITEM_TITLE, item.get(DBConstants.ITEM_TITLE));
					intent.putExtra(DBConstants.ITEM_DESCRIPTION, item.get(DBConstants.ITEM_DESCRIPTION));
					intent.putExtra(DBConstants.ITEM_LINK, item.get(DBConstants.ITEM_LINK));
					startActivity(intent);
				}

			}
		});
		dbhelper = new PentiDatabaseHelper(getActivity(), DBConstants.DATABASE_NAME, null, DBConstants.version);
		if (adapter == null) {
			new LoadItemTask().execute();
		}
		return root;
	}

	public void getLatestData() {
		if (!isRefreshing) {
			isRefreshing = true;
			new GetNewItemTask().execute(getContentType());
		}

	}

	public class GetNewItemTask extends AsyncTask<Integer, Void, Integer> {

		@Override
		protected Integer doInBackground(Integer... type) {
			String url = null;
			switch (type[0].intValue()) {
			case DBConstants.CONTENT_TYPE_TUGUA:
				url = Constants.URL_TUGUA;
				break;
			case DBConstants.CONTENT_TYPE_TWITTE:
				url = Constants.URL_TWITTE;
				break;
			case DBConstants.CONTENT_TYPE_PICTURE:
				url = Constants.URL_PICTURE;
				break;
			default:
				Log.d("ERROR", "Can't find proper url for this type.");
				break;
			}
			InputStream stream = null;
			PentiXmlParser xmlParser = new PentiXmlParser();
			List<net.dasherz.dapenti.xml.PentiItem> items = null;

			try {
				stream = NetUtil.downloadUrl(url);
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
			int itmeCount = dbhelper.insertItemsIfNotExist(items, getContentType());

			return itmeCount;
		}

		@Override
		protected void onPostExecute(Integer result) {

			if (result.intValue() > 0) {
				recordCount = 0;
				adapter = null;
				new LoadItemTask().execute();
			} else if (result.intValue() == -1) {
				Toast.makeText(getActivity(), "更新出错了", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(getActivity(), "已经是最新了", Toast.LENGTH_SHORT).show();
			}
			swipeLayout.setRefreshing(false);
			isRefreshing = false;
		}

	}

	public class LoadItemTask extends AsyncTask<Void, Void, List<Map<String, String>>> {

		/**
		 * return type: 0 no record in database, need to load data from web
		 */
		@Override
		protected List<Map<String, String>> doInBackground(Void... params) {
			if (dbhelper.getCountForType(getContentType()) == 0) {
				return null;
			}
			List<Map<String, String>> data = dbhelper.readItems(getContentType(), recordCount, recordCount
					+ DBConstants.ROW_COUNT_EVERY_READ);
			recordCount += data.size();

			return data;
		}

		@Override
		protected void onPostExecute(List<Map<String, String>> data) {
			if (data == null) {
				listView.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1,
						new String[] { "正在加载..." }));
				new GetNewItemTask().execute(getContentType());
				return;
			}
			if (data.size() == 0) {
				Toast.makeText(getActivity(), "没有更多数据了", Toast.LENGTH_SHORT).show();
				return;
			}
			if (adapter == null) {
				String itemName = DBConstants.ITEM_TITLE;
				if (getContentType() == DBConstants.CONTENT_TYPE_TWITTE) {
					itemName = DBConstants.ITEM_DESCRIPTION;
				}
				adapter = new PentiAdapter(getActivity(), data, itemName, Constants.LOAD_MORE);
				listView.setAdapter(adapter);
			} else {
				adapter.getData().addAll(data);
				adapter.notifyDataSetChanged();
			}

		}
	}

}
