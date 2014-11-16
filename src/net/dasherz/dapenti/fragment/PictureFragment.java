package net.dasherz.dapenti.fragment;

import net.dasherz.dapenti.database.DBConstants;
import android.support.v4.app.Fragment;

/**
 * A simple {@link Fragment} subclass.
 * 
 */
public class PictureFragment extends PentiBaseFragment {

	// @Override
	// public View onCreateView(LayoutInflater inflater, ViewGroup container,
	// Bundle savedInstanceState) {
	// View root = inflater.inflate(R.layout.list, container, false);
	// root.setBackgroundColor(Color.YELLOW);
	// return root;
	// }

	@Override
	int getContentType() {
		return DBConstants.CONTENT_TYPE_PICTURE;
	}

}
