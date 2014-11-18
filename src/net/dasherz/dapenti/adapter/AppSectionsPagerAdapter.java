package net.dasherz.dapenti.adapter;

import net.dasherz.dapenti.fragment.FavouriteFragment;
import net.dasherz.dapenti.fragment.PictureFragment;
import net.dasherz.dapenti.fragment.TuguaFragment;
import net.dasherz.dapenti.fragment.TwitteFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class AppSectionsPagerAdapter extends FragmentPagerAdapter {

	private final int tabCount;
	private final String[] tabNames;

	public AppSectionsPagerAdapter(FragmentManager fm, String[] tabNames, int tabCount) {
		super(fm);
		this.tabNames = tabNames;
		this.tabCount = tabCount;
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
		return tabCount;
	}

	@Override
	public CharSequence getPageTitle(int position) {
		return tabNames[position];
	}
}
