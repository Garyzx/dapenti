package net.dasherz.dapenti;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A simple {@link Fragment} subclass.
 * 
 */
public class FavouriteFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.list, container, false);
		root.setBackgroundColor(Color.GRAY);
		return root;
	}

}
