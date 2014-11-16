package net.dasherz.dapenti.activity;

import java.io.IOException;

import net.dasherz.dapenti.R;
import net.dasherz.dapenti.database.DBConstants;
import net.dasherz.dapenti.database.PentiDatabaseHelper;
import net.dasherz.dapenti.util.NetUtil;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class PentiDetailActivity extends Activity {

	TextView titleView;
	WebView tuguaWebView;
	String id, title, url, link;
	ProgressBar progressBar;
	private PentiDatabaseHelper dbhelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_penti_detail);
		dbhelper = new PentiDatabaseHelper(this, DBConstants.DATABASE_NAME, null, DBConstants.version);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		titleView = (TextView) findViewById(R.id.tuguaTitle);
		tuguaWebView = (WebView) findViewById(R.id.tuguaDetailPage);
		progressBar = (ProgressBar) findViewById(R.id.progressBar1);

		Intent intent = getIntent();
		id = intent.getStringExtra(DBConstants.ITEM_ID);
		title = intent.getStringExtra(DBConstants.ITEM_TITLE);
		url = intent.getStringExtra(DBConstants.ITEM_DESCRIPTION);
		link = intent.getStringExtra(DBConstants.ITEM_LINK);
		titleView.setText(title);
		boolean whetherBlockImage = NetUtil.whetherBlockImage(this);
		tuguaWebView.getSettings().setBlockNetworkImage(whetherBlockImage);
		new LoadPageTask().execute(url);
	}

	class LoadPageTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			String lines = null;
			try {
				lines = NetUtil.getContentOfURL(params[0]);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (lines == null) {
				lines = "获取数据失败。";
			}
			String content = "<html xmlns=\"http://www.w3.org/1999/xhtml\" ><head><meta http-equiv='content-type' content='text/html; charset=utf-8' /></head><body>"
					+ lines + "</body>";
			lines = null;
			return content;
		}

		@Override
		protected void onPostExecute(String result) {
			tuguaWebView.loadData(result, "text/html; charset=UTF-8", null);
			progressBar.setVisibility(View.GONE);
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.tugua_detail, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int menuId = item.getItemId();
		if (menuId == android.R.id.home) {
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		if (menuId == R.id.add_favourite) {
			new AddToFavTask().execute(id);
			return true;
		}
		if (menuId == R.id.copy_title) {
			ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText("title", title);
			clipboard.setPrimaryClip(clip);
			Toast.makeText(this, "已经复制标题到剪贴板。", Toast.LENGTH_SHORT).show();
			return true;
		}
		if (menuId == R.id.open_in_browser) {
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
			startActivity(browserIntent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public class AddToFavTask extends AsyncTask<String, Void, Void> {

		@Override
		protected void onPostExecute(Void result) {
			Toast.makeText(PentiDetailActivity.this, "已经添加到收藏", Toast.LENGTH_SHORT).show();
		}

		@Override
		protected Void doInBackground(String... params) {

			dbhelper.addToFav(params[0]);
			return null;

		}

	}

}
