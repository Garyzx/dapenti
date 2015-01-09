package net.dasherz.dapenti.activity;

import java.io.IOException;

import net.dasherz.dapenti.R;
import net.dasherz.dapenti.database.DBConstants;
import net.dasherz.dapenti.database.DBHelper;
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
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

public class PentiDetailActivity extends Activity {

	TextView titleView;
	WebView tuguaWebView;
	String title, url, link;
	long id;
	ProgressBar progressBar;
	private DBHelper dbhelper;
	private ShareActionProvider mShareActionProvider;
	private Intent mShareIntent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_penti_detail);
		dbhelper = DBHelper.getInstance(getApplication());
		getActionBar().setDisplayHomeAsUpEnabled(true);
		titleView = (TextView) findViewById(R.id.tuguaTitle);
		tuguaWebView = (WebView) findViewById(R.id.tuguaDetailPage);
		progressBar = (ProgressBar) findViewById(R.id.progressBar1);

		Intent intent = getIntent();
		id = intent.getLongExtra(DBConstants.ITEM_ID, -1);
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
		MenuItem item = menu.findItem(R.id.share);
		mShareActionProvider = (ShareActionProvider) item.getActionProvider();
		mShareIntent = new Intent();
		mShareIntent.setAction(Intent.ACTION_SEND);
		mShareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
		mShareIntent.putExtra(Intent.EXTRA_TITLE, title);
		mShareIntent.putExtra(Intent.EXTRA_TEXT, url);
		mShareIntent.setType("text/plain");
		setShareIntent(mShareIntent);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int menuId = item.getItemId();
		switch (menuId) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			break;
		case R.id.add_favourite:
			new AddToFavTask().execute(String.valueOf(id));
			break;
		case R.id.copy_title:
			ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText("title", title);
			clipboard.setPrimaryClip(clip);
			Toast.makeText(this, "已经复制标题到剪贴板。", Toast.LENGTH_SHORT).show();
			break;
		case R.id.open_in_browser:
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
			startActivity(browserIntent);
			break;
		default:
			break;
		}
		return true;
	}

	private void setShareIntent(Intent shareIntent) {
		if (mShareActionProvider != null) {
			mShareActionProvider.setShareIntent(shareIntent);
		}
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
