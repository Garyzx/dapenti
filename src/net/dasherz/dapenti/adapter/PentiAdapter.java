package net.dasherz.dapenti.adapter;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import net.dasherz.dapenti.R;
import net.dasherz.dapenti.database.DBConstants;
import net.dasherz.dapenti.database.Penti;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class PentiAdapter extends BaseAdapter {

	@SuppressLint("UseSparseArrays")
	private final HashMap<Integer, Boolean> mSelection = new HashMap<Integer, Boolean>();

	private final Context ctx;
	private final List<Penti> pentis;
	private final LayoutInflater mInflater;

	public PentiAdapter(Context ctx, List<Penti> pentis) {
		super();
		this.ctx = ctx;
		this.pentis = pentis;
		mInflater = LayoutInflater.from(ctx);

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
		mSelection.clear();
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		return getPentis().size();
	}

	@Override
	public Object getItem(int position) {
		if (position < pentis.size()) {
			if (pentis.get(position).getContentType().equals(DBConstants.CONTENT_TYPE_TWITTE)) {
				return pentis.get(position).getDescription();
			} else {
				return pentis.get(position).getTitle();
			}
		}
		return null;
	}

	@Override
	public long getItemId(int position) {
		if (position < pentis.size()) {
			return pentis.get(position).getId();
		} else {
			return -1;

		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.tugua_entry, parent, false);
			holder = new ViewHolder();
			holder.text = (TextView) convertView.findViewById(R.id.tugua_entry);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		if (position < pentis.size()) {
			holder.text.setText(getItem(position).toString());
		}
		if (mSelection.get(position) != null) {
			convertView.setBackgroundColor(ctx.getResources().getColor(R.color.holo_blue_color));
		} else {
			convertView.setBackgroundColor(Color.TRANSPARENT);
		}
		return convertView;
	}

	public List<Penti> getPentis() {
		return pentis;
	}

	static class ViewHolder {
		TextView text;
	}
}
