package net.dasherz.dapenti.xml;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.dasherz.dapenti.database.DBConstants;
import net.dasherz.dapenti.database.Penti;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Xml;

public class PentiXmlParser {

	private static final String ns = null;

	public List<Penti> parse(InputStream in) throws XmlPullParserException, IOException, ParseException {
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

	private List<Penti> readRss(XmlPullParser parser) throws XmlPullParserException, IOException, ParseException {
		parser.require(XmlPullParser.START_TAG, ns, DBConstants.RSS);
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			if (name.equals(DBConstants.CHANNEL)) {
				return readChannel(parser);
			} else {
				skip(parser);
			}
		}

		return null;
	}

	private List<Penti> readChannel(XmlPullParser parser) throws XmlPullParserException, IOException, ParseException {
		List<Penti> items = new ArrayList<>();
		parser.require(XmlPullParser.START_TAG, ns, DBConstants.CHANNEL);
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			if (name.equals(DBConstants.ITEM)) {
				items.add(readItem(parser));
			} else {
				skip(parser);
			}
		}

		return items;
	}

	private Penti readItem(XmlPullParser parser) throws XmlPullParserException, IOException, ParseException {
		parser.require(XmlPullParser.START_TAG, ns, DBConstants.ITEM);
		Penti item = new Penti();
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();
			if (name.equals(DBConstants.ITEM_TITLE)) {
				item.setTitle(readTitle(parser));
			} else if (name.equals(DBConstants.ITEM_LINK)) {
				item.setLink(readLink(parser));
			} else if (name.equals(DBConstants.ITEM_AUTHOR)) {
				item.setAuthor(readAuthor(parser));
			} else if (name.equals(DBConstants.ITEM_PUB_DATE)) {
				item.setPubDate(readPubDate(parser));
			} else if (name.equals(DBConstants.ITEM_DESCRIPTION)) {
				item.setDescription(readDescription(parser));
			} else {
				skip(parser);
			}
		}
		return item;
	}

	private long readPubDate(XmlPullParser parser) throws XmlPullParserException, IOException, ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss Z", Locale.US);
		parser.require(XmlPullParser.START_TAG, ns, DBConstants.ITEM_PUB_DATE);
		String text = readText(parser);
		// delete day in a week, because it's not standard. "Wes"
		text = text.substring(5);
		parser.require(XmlPullParser.END_TAG, ns, DBConstants.ITEM_PUB_DATE);
		return sdf.parse(text).getTime();
	}

	private String readDescription(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, DBConstants.ITEM_DESCRIPTION);
		String text = readText(parser);
		parser.require(XmlPullParser.END_TAG, ns, DBConstants.ITEM_DESCRIPTION);
		return text;
	}

	private String readAuthor(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, DBConstants.ITEM_AUTHOR);
		String text = readText(parser);
		parser.require(XmlPullParser.END_TAG, ns, DBConstants.ITEM_AUTHOR);
		return text;
	}

	private String readLink(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, DBConstants.ITEM_LINK);
		String link = readText(parser);
		parser.require(XmlPullParser.END_TAG, ns, DBConstants.ITEM_LINK);
		return link;
	}

	private String readTitle(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, DBConstants.ITEM_TITLE);
		String title = readText(parser);
		parser.require(XmlPullParser.END_TAG, ns, DBConstants.ITEM_TITLE);
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
