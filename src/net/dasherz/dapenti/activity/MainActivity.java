package net.dasherz.dapenti.activity;

import net.dasherz.dapenti.R;
import net.dasherz.dapenti.constant.Constants;
import net.dasherz.dapenti.fragment.FavouriteFragment;
import net.dasherz.dapenti.fragment.PentiBaseFragment;
import net.dasherz.dapenti.fragment.PictureFragment;
import net.dasherz.dapenti.fragment.TuguaFragment;
import net.dasherz.dapenti.fragment.TwitteFragment;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
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
	private boolean doubleBackToExitPressedOnce;

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

		SharedPreferences settings = getSharedPreferences(Constants.PREFERENCE_NAME, MODE_PRIVATE);

		mViewPager.setCurrentItem(Integer.parseInt(settings.getString("defaultChannel", "0")));
		Log.d("SP", "" + settings.getString("defaultChannel", ""));
		Log.d("SP", "" + settings.getBoolean("loadPicture", false));
		Log.d("SP", "" + settings.getBoolean("loadPictureUnderWIFI", false));
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
			Intent intent = new Intent(this, FragmentPreferences.class);
			startActivity(intent);
			return true;
		}
		if (id == R.id.refresh) {
			PentiBaseFragment pentiFragment = (PentiBaseFragment) getActiveFragment(mViewPager, getActionBar()
					.getSelectedTab().getPosition());
			pentiFragment.getLatestData();
			return true;
		}
		return super.onOptionsItemSelected(item);
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

	@Override
	public void onBackPressed() {
		if (doubleBackToExitPressedOnce) {
			super.onBackPressed();
			return;
		}

		this.doubleBackToExitPressedOnce = true;
		Toast.makeText(this, "再按一次退出", Toast.LENGTH_SHORT).show();

		new Handler().postDelayed(new Runnable() {

			@Override
			public void run() {
				doubleBackToExitPressedOnce = false;
			}
		}, 2000);
	}
}
