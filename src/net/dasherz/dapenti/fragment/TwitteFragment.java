package net.dasherz.dapenti.fragment;

import net.dasherz.dapenti.database.DBConstants;
import android.support.v4.app.Fragment;

/**
 * A simple {@link Fragment} subclass.
 * 
 */
public class TwitteFragment extends PentiBaseFragment {
	private static final String TAG = TwitteFragment.class.getSimpleName();

	@Override
	int getContentType() {
		return DBConstants.CONTENT_TYPE_TWITTE;
	}

}
