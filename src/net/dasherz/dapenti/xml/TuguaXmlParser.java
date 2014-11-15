package net.dasherz.dapenti.xml;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Xml;

public class TuguaXmlParser {

	private static final String ns = null;

	public List<TuguaItem> parse(InputStream in) throws XmlPullParserException, IOException, ParseException {
		try {
			XmlPullParser parser = Xml.newPullParser();
			parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			parser.setInput(in, null);
			parser.nextTag();
			return readRss(parser);
		} finally {
			in.close();
		}
	}

	private List<TuguaItem> readRss(XmlPullParser parser) throws XmlPullParserException, IOException, ParseException {
		parser.require(XmlPullParser.START_TAG, ns, "rss");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			if (name.equals("channel")) {
				return readChannel(parser);
			} else {
				skip(parser);
			}
		}

		return null;
	}

	private List<TuguaItem> readChannel(XmlPullParser parser) throws XmlPullParserException, IOException,
			ParseException {
		List<TuguaItem> items = new ArrayList<>();
		parser.require(XmlPullParser.START_TAG, ns, "channel");
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			if (name.equals("item")) {
				items.add(readItem(parser));
			} else {
				skip(parser);
			}
		}

		return items;
	}

	private TuguaItem readItem(XmlPullParser parser) throws XmlPullParserException, IOException, ParseException {
		parser.require(XmlPullParser.START_TAG, ns, "item");
		TuguaItem item = new TuguaItem();
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			if (name.equals("title")) {
				item.setTitle(readTitle(parser));
			} else if (name.equals("link")) {
				item.setLink(readLink(parser));
			} else if (name.equals("author")) {
				item.setAuthor(readAuthor(parser));
			} else if (name.equals("pubDate")) {
				item.setPubDate(readPubDate(parser));
			} else if (name.equals("description")) {
				item.setDescription(readDescription(parser));
			} else {
				skip(parser);
			}
		}
		return item;
	}

	private long readPubDate(XmlPullParser parser) throws XmlPullParserException, IOException, ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss Z", Locale.US);
		parser.require(XmlPullParser.START_TAG, ns, "pubDate");
		String text = readText(parser);
		// delete day in a week, because it's not standard. "Wes"
		text = text.substring(5);
		parser.require(XmlPullParser.END_TAG, ns, "pubDate");
		return sdf.parse(text).getTime();
	}

	private String readDescription(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, "description");
		String text = readText(parser);
		parser.require(XmlPullParser.END_TAG, ns, "description");
		return text;
	}

	private String readAuthor(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, "author");
		String text = readText(parser);
		parser.require(XmlPullParser.END_TAG, ns, "author");
		return text;
	}

	private String readLink(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, "link");
		String link = readText(parser);
		parser.require(XmlPullParser.END_TAG, ns, "link");
		return link;
	}

	private String readTitle(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, "title");
		String title = readText(parser);
		parser.require(XmlPullParser.END_TAG, ns, "title");
		return title;
	}

	private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
		String result = "";
		if (parser.next() == XmlPullParser.TEXT) {
			result = parser.getText();
			parser.nextTag();
		}
		return result;
	}

	private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
		if (parser.getEventType() != XmlPullParser.START_TAG) {
			throw new IllegalStateException();
		}
		int depth = 1;
		while (depth != 0) {
			switch (parser.next()) {
			case XmlPullParser.END_TAG:
				depth--;
				break;
			case XmlPullParser.START_TAG:
				depth++;
				break;
			}
		}
	}

}
