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

	public static final String CREATE_TABLE_TUGUA_SQL = "create table tugua_item(_id integer primary key autoincrement , title text, link text ,author text,pubDate integer,description text);";
	public static final String SELECT_TUGUA = "select * from tugua_item order by pubDate DESC Limit 10 Offset ?";
	public static final String SELECT_TUGUA_ALL = "select * from tugua_item order by pubDate DESC";

}
