package net.dasherz.dapenti.fragment;

import java.util.ArrayList;
import java.util.List;

import net.dasherz.dapenti.R;
import net.dasherz.dapenti.activity.PentiDetailActivity;
import net.dasherz.dapenti.adapter.PentiAdapter;
import net.dasherz.dapenti.database.DBConstants;
import net.dasherz.dapenti.database.DBHelper;
import net.dasherz.dapenti.database.Penti;
import net.dasherz.dapenti.util.LogUtil;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
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
	private static final String TAG = FavouriteFragment.class.getSimpleName();
	ListView mListView;
	PentiAdapter adapter;
	private SwipeRefreshLayout swipeLayout;
	private DBHelper dbHelper;
	int recordCount = 0;
	private boolean isRefreshing = false;

	public FavouriteFragment() {
	}

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
					Toast.makeText(getActivity(), getResources().getString(R.string.already_copied_to_clip),
							Toast.LENGTH_SHORT).show();
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
				mode.setTitle(checkedItemCount + getResources().getString(R.string.item_chosen));
			}
		});
		handlePullingUpLoading();
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
					new String[] { getResources().getString(R.string.loading) }));
		} else {
			mListView.setAdapter(adapter);
		}
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// if adapter is not initialized
				if (adapter == null || adapter.getItem(position) == null) {
					return;
				}
				// if user click on twitte, then just return
				if (adapter.getPentis().get(position).getContentType().equals(DBConstants.CONTENT_TYPE_TWITTE)) {
					return;
				}
				// if user clicked on an real item
				Penti item = adapter.getPentis().get(position);
				LogUtil.d(TAG, "Opening new activity to show web page");
				Intent intent = new Intent(getActivity(), PentiDetailActivity.class);
				intent.putExtra(DBConstants.ITEM_ID, item.getId());
				intent.putExtra(DBConstants.ITEM_TITLE, item.getTitle());
				intent.putExtra(DBConstants.ITEM_DESCRIPTION, item.getDescription());
				intent.putExtra(DBConstants.ITEM_LINK, item.getLink());
				startActivity(intent);

			}
		});
		dbHelper = DBHelper.getInstance(getActivity());
		if (adapter == null) {
			getLatestData();
		}
		return root;
	}

	private void handlePullingUpLoading() {
		mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
			boolean isLastRow = false;

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				if (firstVisibleItem + visibleItemCount == totalItemCount && totalItemCount > 0) {
					isLastRow = true;
				}
			}

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				if (isLastRow && scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
					new LoadItemTask().execute();
					isLastRow = false;
				}
			}
		});
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

	private class LoadItemTask extends AsyncTask<Void, Void, List<Penti>> {

		/**
		 * return type: 0 no record in database, need to load data from web
		 */
		@Override
		protected List<Penti> doInBackground(Void... params) {
			// FIXME When browsering fav items, goto other tab, add one new item
			// to fav, then change back to fav. Click load more items, error
			// will happen, because the item's location in the query changes.
			// Currently no good solution, Refresh all list is bad, user will
			// lost item been reviewing. Not refresh will cause same item been
			// show twice.
			// Manually refresh will solve this.
			if (dbHelper.getCountForFav() == 0) {
				LogUtil.d(TAG, "No data for fav");
				return null;
			}
			List<Penti> data = dbHelper.readItems(-1, DBConstants.ROW_COUNT_EVERY_READ, recordCount);
			recordCount += data.size();

			if (adapter == null) {
				isRefreshing = false;
				return data;
			}
			// if the data is not in exist list, so add it to available set,
			// remove exist ones
			List<Penti> availableData = new ArrayList<>();
			List<String> existIds = new ArrayList<>();
			for (Penti existRow : adapter.getPentis()) {
				existIds.add(existRow.getId().toString());
			}
			for (Penti row : data) {
				if (!existIds.contains(row.getId())) {
					availableData.add(row);
				}

			}
			isRefreshing = false;
			data = null;
			return availableData;
		}

		@Override
		protected void onPostExecute(List<Penti> data) {
			if (data == null) {
				if (isAdded()) {
					mListView.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1,
							new String[] { getResources().getString(R.string.no_data) }));
				}
				isRefreshing = false;
				return;
			}
			if (data.size() == 0) {
				// adapter.notifyDataSetChanged();
				if (isAdded()) {
					Toast.makeText(getActivity(), getResources().getString(R.string.no_more_data_try_refresh),
							Toast.LENGTH_SHORT).show();
				}
				return;
			}
			if (adapter == null) {
				adapter = new PentiAdapter(getActivity(), data);
				mListView.setAdapter(adapter);
			} else {
				adapter.getPentis().addAll(data);
				adapter.notifyDataSetChanged();
			}

		}
	}

	public class RemoveFromFavTask extends AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... ids) {
			dbHelper.removeFromFav(ids[0]);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if (isAdded()) {
				Toast.makeText(getActivity(), getResources().getString(R.string.already_removed_from_fav),
						Toast.LENGTH_SHORT).show();
				getLatestData();
			}
		}
	}
}
