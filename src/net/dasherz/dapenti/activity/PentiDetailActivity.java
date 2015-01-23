package net.dasherz.dapenti.activity;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;

import net.dasherz.dapenti.R;
import net.dasherz.dapenti.database.DBConstants;
import net.dasherz.dapenti.database.DBHelper;
import net.dasherz.dapenti.util.LogUtil;
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
import android.view.ViewConfiguration;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

public class PentiDetailActivity extends Activity {
	private static final String TEXT_HTML_CHARSET_UTF_8 = "text/html; charset=UTF-8";

	/**
	 * This class can intercept all request sent by webview, currently it's no
	 * use, leave it as example
	 * 
	 * @author gary
	 * 
	 */
	public class MyWebViewClient extends WebViewClient {

		@Override
		public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
			if (!url.startsWith("http")) {
				return null;
			}
			try {
				URL requestUrl = new URL(url);
				HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
				connection.addRequestProperty("Referer", PentiDetailActivity.this.url);
				connection.setDoInput(true);
				connection.connect();
				InputStream stream = connection.getInputStream();
				return new WebResourceResponse(connection.getContentType(), connection.getContentEncoding(), stream);
			} catch (IOException e) {
				LogUtil.e(TAG, e.getMessage());
				e.printStackTrace();
			}
			return null;
		}

	}

	private static final String TAG = PentiDetailActivity.class.getSimpleName();
	TextView titleView;
	WebView tuguaWebView;
	String title, url, link;
	long id;
	ProgressBar progressBar;
	private DBHelper dbhelper;
	private ShareActionProvider mShareActionProvider;
	private Intent mShareIntent;
	private boolean optimizeHTML = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getOverflowMenu();
		setContentView(R.layout.activity_penti_detail);
		dbhelper = DBHelper.getInstance(getApplication());
		getActionBar().setDisplayHomeAsUpEnabled(true);
		titleView = (TextView) findViewById(R.id.tuguaTitle);
		tuguaWebView = (WebView) findViewById(R.id.tuguaDetailPage);
		tuguaWebView.loadData(getResources().getString(R.string.html_loading), TEXT_HTML_CHARSET_UTF_8, null);
		progressBar = (ProgressBar) findViewById(R.id.progressBar1);

		Intent intent = getIntent();
		id = intent.getLongExtra(DBConstants.ITEM_ID, -1);
		title = intent.getStringExtra(DBConstants.ITEM_TITLE);
		url = intent.getStringExtra(DBConstants.ITEM_DESCRIPTION);
		link = intent.getStringExtra(DBConstants.ITEM_LINK);
		titleView.setText(title);
		boolean whetherBlockImage = NetUtil.whetherBlockImage(this);
		tuguaWebView.getSettings().setBlockNetworkImage(whetherBlockImage);
		tuguaWebView
				.getSettings()
				.setUserAgentString(
						"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.95 Safari/537.36");
		new LoadPageTask().execute(url);
	}

	private void getOverflowMenu() {
		try {
			ViewConfiguration config = ViewConfiguration.get(this);
			Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
			if (menuKeyField != null) {
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	class LoadPageTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... params) {
			String lines = null;
			try {
				lines = NetUtil.getContentOfURL(params[0], optimizeHTML);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (lines == null) {
				lines = getResources().getString(R.string.get_data_failed);
			}
			String content = "<html xmlns=\"http://www.w3.org/1999/xhtml\" ><head><meta http-equiv='content-type' content='text/html; charset=utf-8' /><style type=\"text/css\">img {	width: 100%;}</style></head><body>"
					+ lines + "</body>";
			lines = null;
			return content;
		}

		@Override
		protected void onPostExecute(String result) {
			// LogUtil.d(TAG, result);
			tuguaWebView.loadDataWithBaseURL(url, result, null, TEXT_HTML_CHARSET_UTF_8, url);
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
		LogUtil.d(TAG, "onCreateOptionsMenu");
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
			Toast.makeText(this, getResources().getString(R.string.already_copied_to_clip), Toast.LENGTH_SHORT).show();
			break;
		case R.id.open_in_browser:
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
			startActivity(browserIntent);
			break;
		case R.id.open_original_content:
			if (optimizeHTML) {
				optimizeHTML = false;
				item.setTitle(R.string.optimized_content);
			} else {
				optimizeHTML = true;
				item.setTitle(R.string.original_content);
			}
			progressBar.setVisibility(View.VISIBLE);
			new LoadPageTask().execute(url);
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
			Toast.makeText(PentiDetailActivity.this, getResources().getString(R.string.already_added_to_fav),
					Toast.LENGTH_SHORT).show();
		}

		@Override
		protected Void doInBackground(String... params) {

			dbhelper.addToFav(params[0]);
			return null;

		}

	}

}
