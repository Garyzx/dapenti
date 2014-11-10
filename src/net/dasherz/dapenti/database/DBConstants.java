package net.dasherz.dapenti.database;

public class DBConstants {
	public static final int version = 1;
	public static final String DATABASE_NAME = "penti.db3";
	public static final String TABLE_TUGUA = "tugua_item";
	public static final String ITEM_DESCRIPTION = "description";
	public static final String ITEM_PUB_DATE = "pubDate";
	public static final String ITEM_AUTHOR = "author";
	public static final String ITEM_LINK = "link";
	public static final String ITEM_TITLE = "title";

	public static final String CREATE_TABLE_TUGUA_SQL = "create table tugua_item(_id integer primary key autoincrement , title , link,author,pubDate,description);";

}
