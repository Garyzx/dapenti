package net.dasherz.dapenti.fragment;

import net.dasherz.dapenti.database.DBConstants;
import android.support.v4.app.Fragment;

/**
 * A simple {@link Fragment} subclass.
 * 
 */
public class PictureFragment extends PentiBaseFragment {

	public PictureFragment() {
	}

	@Override
	int getContentType() {
		return DBConstants.CONTENT_TYPE_PICTURE;
	}

}
