package net.dasherz.dapenti;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.List;

import net.dasherz.dapenti.database.DBConstants;
import net.dasherz.dapenti.database.PentiDatabaseHelper;
import net.dasherz.dapenti.xml.TuguaXmlParser;
import net.dasherz.dapenti.xml.TuguaXmlParser.TuguaItem;

import org.xmlpull.v1.XmlPullParserException;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements ActionBar.TabListener {
	public class AppSectionsPagerAdapter extends FragmentPagerAdapter {

		private static final int TAB_COUNT = 4;
		private final String[] tabNames = getResources().getStringArray(R.array.tab_name_list);

		public AppSectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int i) {
			Fragment fragment = null;
			switch (i) {
			case 0:
				fragment = new TuguaFragment();
				break;
			case 1:
				fragment = new TwitteFragment();
				break;
			case 2:
				fragment = new PictureFragment();
				break;
			case 3:
				fragment = new FavouriteFragment();
				break;

			}
			return fragment;
		}

		@Override
		public int getCount() {
			return TAB_COUNT;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return tabNames[position];
		}

	}

	AppSectionsPagerAdapter mAppSectionsPagerAdapter;
	ViewPager mViewPager;
	FragmentManager mFragmentManager = getSupportFragmentManager();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mAppSectionsPagerAdapter = new AppSectionsPagerAdapter(getSupportFragmentManager());
		final ActionBar actionBar = getActionBar();
		actionBar.setHomeButtonEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		mViewPager = (ViewPager) findViewById(R.id.viewPager);
		mViewPager.setAdapter(mAppSectionsPagerAdapter);
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				actionBar.setSelectedNavigationItem(position);
			}
		});
		for (int i = 0; i < mAppSectionsPagerAdapter.getCount(); i++) {
			actionBar.addTab(actionBar.newTab().setText(mAppSectionsPagerAdapter.getPageTitle(i)).setTabListener(this));
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		if (id == R.id.refresh) {
			new RefreshTuguaTask().execute(Constants.URL_TUGUA);
			return true;
		}
		return super.onOptionsItemSelected(item);
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(List<TuguaItem> items) {
			int itemUpdated = 0;
			PentiDatabaseHelper dbhelper = new PentiDatabaseHelper(MainActivity.this, DBConstants.DATABASE_NAME, null,
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
				TuguaFragment tuguaFragment = (TuguaFragment) getActiveFragment(mViewPager, 0);
				tuguaFragment.reloadList();
			} else {
				Toast.makeText(MainActivity.this, "已经是最新了", Toast.LENGTH_SHORT).show();
			}
		}
	}

	private List<TuguaItem> loadXmlFromNetwork(String urlString) throws XmlPullParserException, IOException,
			ParseException {
		InputStream stream = null;
		TuguaXmlParser stackOverflowXmlParser = new TuguaXmlParser();
		List<TuguaItem> items = null;

		try {
			stream = downloadUrl(urlString);
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

	// Given a string representation of a URL, sets up a connection and gets
	// an input stream.
	private InputStream downloadUrl(String urlString) throws IOException {
		URL url = new URL(urlString);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setReadTimeout(10000 /* milliseconds */);
		conn.setConnectTimeout(15000 /* milliseconds */);
		conn.setRequestMethod("GET");
		conn.setDoInput(true);
		// Starts the query
		conn.connect();
		InputStream stream = conn.getInputStream();
		return stream;
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {

	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {

	}

	// http://stackoverflow.com/questions/8785221/retrieve-a-fragment-from-a-viewpager
	public Fragment getActiveFragment(ViewPager container, int position) {
		String name = makeFragmentName(container.getId(), position);
		return mFragmentManager.findFragmentByTag(name);
	}

	private static String makeFragmentName(int viewId, int index) {
		return "android:switcher:" + viewId + ":" + index;
	}
}
