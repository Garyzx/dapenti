package net.dasherz.dapenti.adapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.dasherz.dapenti.R;
import net.dasherz.dapenti.database.DBConstants;
import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class PentiAdapter extends BaseAdapter {

	@SuppressLint("UseSparseArrays")
	private final HashMap<Integer, Boolean> mSelection = new HashMap<Integer, Boolean>();

	private final Context ctx;
	private final List<Map<String, String>> data;

	public PentiAdapter(Context ctx, List<Map<String, String>> data) {
		super();
		this.ctx = ctx;
		this.data = data;
	}

	public void setNewSelection(int position, boolean value) {
		mSelection.put(position, value);
		notifyDataSetChanged();
	}

	public boolean isPositionChecked(int position) {
		Boolean result = mSelection.get(position);
		return result == null ? false : result;
	}

	public Set<Integer> getCurrentCheckedPosition() {
		return mSelection.keySet();
	}

	public void removeSelection(int position) {
		mSelection.remove(position);
		notifyDataSetChanged();
	}

	public void clearSelection() {
		mSelection.clear();// = new HashMap<Integer, Boolean>();
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return getData().size();
	}

	@Override
	public Object getItem(int position) {
		if (position < getData().size()) {
			if (getData().get(position).get(DBConstants.ITEM_CONTENT_TYPE)
					.equals(String.valueOf(DBConstants.CONTENT_TYPE_TWITTE))) {
				return getData().get(position).get(DBConstants.ITEM_DESCRIPTION);
			} else {
				return getData().get(position).get(DBConstants.ITEM_TITLE);
			}
		}
		return null;
	}

	@Override
	public long getItemId(int position) {
		if (position < getData().size()) {
			return Integer.parseInt(getData().get(position).get(DBConstants.ITEM_ID));
		} else {
			return -1;

		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View entry = inflater.inflate(R.layout.tugua_entry, parent, false);

		TextView textView = (TextView) entry.findViewById(R.id.tugua_entry);
		if (position < getData().size()) {
			textView.setText(getItem(position).toString());
		}
		if (mSelection.get(position) != null) {
			entry.setBackgroundColor(ctx.getResources().getColor(R.color.holo_blue_color));
		}
		return entry;
	}

	public List<Map<String, String>> getData() {
		return data;
	}
}
