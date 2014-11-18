package net.dasherz.dapenti.fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.dasherz.dapenti.R;
import net.dasherz.dapenti.activity.PentiDetailActivity;
import net.dasherz.dapenti.adapter.PentiAdapter;
import net.dasherz.dapenti.constant.Constants;
import net.dasherz.dapenti.database.DBConstants;
import net.dasherz.dapenti.database.PentiDatabaseHelper;
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
public class FavouriteFragment extends Fragment {

	ListView mListView;
	PentiAdapter adapter;
	private SwipeRefreshLayout swipeLayout;
	private PentiDatabaseHelper dbhelper;
	int recordCount = 0;
	private boolean isRefreshing = false;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.list, container, false);
		mListView = (ListView) root.findViewById(R.id.pentiListView);
		mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		mListView.setMultiChoiceModeListener(new MultiChoiceModeListener() {
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
				inflater.inflate(R.menu.menu_fav, menu);
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
				} else if (item.getItemId() == R.id.remove_favourite) {
					StringBuffer buffer = new StringBuffer();

					for (Integer integer : adapter.getCurrentCheckedPosition()) {
						buffer.append(adapter.getItemId(integer)).append(",");
					}
					buffer.deleteCharAt(buffer.length() - 1);
					new RemoveFromFavTask().execute(buffer.toString());
				}
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
			mListView.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1,
					new String[] { "正在加载..." }));
		} else {
			mListView.setAdapter(adapter);
		}
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// if adapter is not initialized
				if (adapter == null || adapter.getItem(position) == null
						|| adapter.getItem(position).toString().equals(Constants.NO_MORE_NEW)) {
					return;
				}
				// if user click on load more item
				if (adapter.getItem(position).toString().equals(Constants.LOAD_MORE)) {
					new LoadItemTask().execute();
					return;
				}
				// if user click on twitte, then just return
				if (adapter.getData().get(position).get(DBConstants.ITEM_CONTENT_TYPE)
						.equals(String.valueOf(DBConstants.CONTENT_TYPE_TWITTE))) {
					return;
				}
				// if user clicked on an real item
				if (position < adapter.getCount() - 1 && adapter.getData().get(position) instanceof Map) {
					Map<String, String> item = adapter.getData().get(position);
					Log.d("TUGUA", "Opening new activity to show web page");
					Intent intent = new Intent(getActivity(), PentiDetailActivity.class);
					intent.putExtra(DBConstants.ITEM_ID, item.get(DBConstants.ITEM_ID));
					intent.putExtra(DBConstants.ITEM_TITLE, item.get(DBConstants.ITEM_TITLE));
					intent.putExtra(DBConstants.ITEM_DESCRIPTION, item.get(DBConstants.ITEM_DESCRIPTION));
					intent.putExtra(DBConstants.ITEM_LINK, item.get(DBConstants.ITEM_LINK));
					startActivity(intent);
				}

			}
		});
		dbhelper = new PentiDatabaseHelper(getActivity(), DBConstants.DATABASE_NAME, null, DBConstants.version);
		if (adapter == null) {
			getLatestData();
		}
		return root;
	}

	public void getLatestData() {
		if (!isRefreshing) {
			recordCount = 0;
			adapter = null;
			isRefreshing = true;
			new LoadItemTask().execute();
			swipeLayout.setRefreshing(false);
		}

	}

	private class LoadItemTask extends AsyncTask<Void, Void, List<Map<String, String>>> {

		/**
		 * return type: 0 no record in database, need to load data from web
		 */
		@Override
		protected List<Map<String, String>> doInBackground(Void... params) {
			// FIXME When browsering fav items, goto other tab, add one new item
			// to fav, then change back to fav. Click load more items, error
			// will happen, because the item's location in the query changes.
			// Currently no good solution, Refresh all list is bad, user will
			// lost item been reviewing. Not refresh will cause same item been
			// show twice.
			// Manually refresh will solve this.
			if (dbhelper.getCountForFav() == 0) {
				Log.d("DB", "No data for fav");
				return null;
			}
			List<Map<String, String>> data = dbhelper.readItems(-1, recordCount, recordCount
					+ DBConstants.ROW_COUNT_EVERY_READ);
			recordCount += data.size();

			if (adapter == null) {
				isRefreshing = false;
				return data;
			}
			// if the data is not in exist list, so add it to available set,
			// remove exist ones
			List<Map<String, String>> availableData = new ArrayList<>();
			List<String> existIds = new ArrayList<>();
			for (Map<String, String> existRow : adapter.getData()) {
				existIds.add(existRow.get(DBConstants.ITEM_ID));
			}
			for (Map<String, String> row : data) {
				if (!existIds.contains(row.get(DBConstants.ITEM_ID))) {
					availableData.add(row);
				}

			}
			isRefreshing = false;
			data = null;
			return availableData;
		}

		@Override
		protected void onPostExecute(List<Map<String, String>> data) {
			if (data == null) {
				mListView.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1,
						new String[] { "没有数据" }));
				isRefreshing = false;
				return;
			}
			if (data.size() == 0) {
				adapter.setFooter(Constants.NO_MORE_NEW);
				adapter.notifyDataSetChanged();
				Toast.makeText(getActivity(), "没有更多数据了，刷新试试", Toast.LENGTH_SHORT).show();
				return;
			}
			if (adapter == null) {
				adapter = new PentiAdapter(getActivity(), data, Constants.LOAD_MORE);
				mListView.setAdapter(adapter);
			} else {
				adapter.getData().addAll(data);
				adapter.notifyDataSetChanged();
			}

		}
	}

	public class RemoveFromFavTask extends AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... ids) {
			dbhelper.removeFromFav(ids[0]);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			Toast.makeText(getActivity(), "已经从收藏中移除。", Toast.LENGTH_SHORT).show();
			getLatestData();
		}
	}
}
