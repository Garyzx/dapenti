package net.dasherz.dapenti.fragment;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import net.dasherz.dapenti.R;
import net.dasherz.dapenti.activity.PentiDetailActivity;
import net.dasherz.dapenti.adapter.PentiAdapter;
import net.dasherz.dapenti.constant.Constants;
import net.dasherz.dapenti.database.DBConstants;
import net.dasherz.dapenti.database.DBHelper;
import net.dasherz.dapenti.database.Penti;
import net.dasherz.dapenti.util.LogUtil;
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
public abstract class PentiBaseFragment extends Fragment {
	private static final String TAG = PentiBaseFragment.class.getSimpleName();
	private ListView listView;
	private PentiAdapter adapter;
	private SwipeRefreshLayout swipeLayout;
	// private PentiDatabaseHelper dbhelper;
	private DBHelper dbHelper;
	boolean isRefreshing = false;
	int recordCount = 0;

	abstract int getContentType();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.list, container, false);
		listView = (ListView) root.findViewById(R.id.pentiListView);
		handleMultiChoiceMode();
		handlePullingDownRefresh(root);
		if (adapter == null) {
			setLoadingForList();
		} else {
			listView.setAdapter(adapter);
		}
		handleItemClick();
		handlePullingUpLoading();
		dbHelper = DBHelper.getInstance(getActivity());
		if (adapter == null) {
			new LoadItemTask().execute();
		}
		return root;
	}

	private void setLoadingForList() {
		listView.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1,
				new String[] { getResources().getString(R.string.loading) }));
	}

	private void handleMultiChoiceMode() {
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
					Toast.makeText(getActivity(), getResources().getString(R.string.already_copied_to_clip),
							Toast.LENGTH_SHORT).show();
				} else if (item.getItemId() == R.id.add_favourite) {
					StringBuffer buffer = new StringBuffer();

					for (Integer integer : adapter.getCurrentCheckedPosition()) {
						buffer.append(adapter.getItemId(integer)).append(",");
					}
					buffer.deleteCharAt(buffer.length() - 1);
					new AddToFavTask().execute(buffer.toString());

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
	}

	private void handlePullingDownRefresh(View root) {
		swipeLayout = (SwipeRefreshLayout) root.findViewById(R.id.swipe_container);
		swipeLayout.setColorSchemeColors(Color.BLACK, Color.BLUE, Color.GREEN, Color.YELLOW);
		swipeLayout.setOnRefreshListener(new OnRefreshListener() {

			@Override
			public void onRefresh() {
				getLatestData();
			}
		});
	}

	private void handleItemClick() {
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// if adapter is not initialized
				if (adapter == null || adapter.getItem(position) == null) {
					return;
				}
				// if user click on twitte, then just return
				if (getContentType() == DBConstants.CONTENT_TYPE_TWITTE) {
					return;
				}
				// if user clicked on an real item
				if (position < adapter.getCount() - 1) {
					Penti item = adapter.getPentis().get(position);
					LogUtil.d(TAG, "Opening new activity to show web page");
					Intent intent = new Intent(getActivity(), PentiDetailActivity.class);
					intent.putExtra(DBConstants.ITEM_ID, item.getId());
					intent.putExtra(DBConstants.ITEM_TITLE, item.getTitle());
					intent.putExtra(DBConstants.ITEM_DESCRIPTION, item.getDescription());
					intent.putExtra(DBConstants.ITEM_LINK, item.getLink());
					startActivity(intent);
				}

			}
		});
	}

	private void handlePullingUpLoading() {
		listView.setOnScrollListener(new AbsListView.OnScrollListener() {
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
				LogUtil.d("ERROR", "Can't find proper url for this type.");
				break;
			}
			LogUtil.d(TAG, "Updating for content type: " + getContentType());
			// if url is not found, just reload the list
			if (url == null) {
				return 1;
			}
			InputStream stream = null;
			PentiXmlParser xmlParser = new PentiXmlParser();
			List<Penti> items = null;

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
			int itmeCount = dbHelper.insertItemsIfNotExist(items, getContentType());

			return itmeCount;
		}

		@Override
		protected void onPostExecute(Integer result) {

			if (result.intValue() > 0) {
				recordCount = 0;
				adapter = null;
				new LoadItemTask().execute();
			} else if (result.intValue() == -1) {
				Toast.makeText(getActivity(), getResources().getString(R.string.update_failed), Toast.LENGTH_SHORT)
						.show();
			} else {
				Toast.makeText(getActivity(), getResources().getString(R.string.already_updated), Toast.LENGTH_SHORT)
						.show();
			}
			swipeLayout.setRefreshing(false);
			isRefreshing = false;
		}

	}

	public class LoadItemTask extends AsyncTask<Void, Void, List<Penti>> {

		/**
		 * return type: 0 no record in database, need to load data from web
		 */
		@Override
		protected List<Penti> doInBackground(Void... params) {
			if (dbHelper.getCountForType(getContentType()) == 0) {
				return null;
			}
			List<Penti> data = dbHelper.readItems(getContentType(), DBConstants.ROW_COUNT_EVERY_READ, recordCount);
			recordCount += data.size();

			return data;
		}

		@Override
		protected void onPostExecute(List<Penti> data) {
			if (data == null) {
				setLoadingForList();
				new GetNewItemTask().execute(getContentType());
				return;
			}
			if (data.size() == 0) {
				adapter.notifyDataSetChanged();
				Toast.makeText(getActivity(), getResources().getString(R.string.no_more_data), Toast.LENGTH_SHORT)
						.show();
				return;
			}
			if (adapter == null) {
				adapter = new PentiAdapter(getActivity(), data);
				listView.setAdapter(adapter);
			} else {
				adapter.getPentis().addAll(data);
				adapter.notifyDataSetChanged();
			}

		}
	}

	public class AddToFavTask extends AsyncTask<String, Void, Void> {

		@Override
		protected void onPostExecute(Void result) {
			Toast.makeText(getActivity(), getResources().getString(R.string.already_added_to_fav), Toast.LENGTH_SHORT)
					.show();
		}

		@Override
		protected Void doInBackground(String... params) {

			dbHelper.addToFav(params[0]);
			return null;

		}

	}

}
