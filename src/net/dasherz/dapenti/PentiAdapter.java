package net.dasherz.dapenti;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class PentiAdapter extends BaseAdapter {

	private final Context ctx;
	private final List<Map<String, String>> data;
	private final String itemName;
	private String footer;

	public PentiAdapter(Context ctx, List<Map<String, String>> data, String itemName, String footer) {
		super();
		this.ctx = ctx;
		this.data = data;
		this.itemName = itemName;
		this.footer = footer;
	}

	@Override
	public int getCount() {
		return getData().size() + 1;
	}

	@Override
	public Object getItem(int position) {
		if (position < getData().size()) {
			return getData().get(position).get(itemName);
		} else {
			return getFooter();

		}
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView textView = new TextView(ctx);
		textView.setTextSize(20);
		textView.setPadding(0, 5, 0, 5);
		textView.setMinHeight(200);
		if (position < getData().size()) {
			textView.setText(getData().get(position).get(itemName));
		} else {
			textView.setText(getFooter());
		}
		return textView;
	}

	public List<Map<String, String>> getData() {
		return data;
	}

	public String getFooter() {
		return footer;
	}

	public void setFooter(String footer) {
		this.footer = footer;
	}
}
