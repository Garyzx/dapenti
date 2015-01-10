package net.dasherz.dapenti.database;

import java.util.List;

import net.dasherz.dapenti.AppContext;
import net.dasherz.dapenti.database.PentiDao.Properties;
import android.content.Context;
import de.greenrobot.dao.query.QueryBuilder;

public class DBHelper {
	private static Context mContext;
	private static DBHelper instance;
	private PentiDao pentiDao;

	private DBHelper() {
	}

	public static DBHelper getInstance(Context context) {
		if (instance == null) {
			instance = new DBHelper();
			if (mContext == null) {
				mContext = context;
			}

			// 数据库对象
			DaoSession daoSession = AppContext.getDaoSession(mContext);
			instance.pentiDao = daoSession.getPentiDao();
		}
		return instance;
	}

	public int insertItemsIfNotExist(List<Penti> items, int contentType) {
		int newItemCount = 0;
		for (Penti penti : items) {
			if (isNew(penti)) {
				penti.setContentType(contentType);
				penti.setIsFavorite(false);
				pentiDao.insert(penti);
				newItemCount++;
			}
		}
		return newItemCount;
	}

	private String convertHtmlToText(String desc) {
		desc = desc.replaceAll("<[A-Z]{1,4}[^>]{0,}>", "").replaceAll("</[A-Z]{1,4}>", "").replaceAll("&nbsp;", " ")
				.replaceAll("&#8943;", "--");
		return desc.trim();
	}

	private boolean isNew(Penti penti) {
		List<Penti> result = pentiDao.queryBuilder().where(Properties.Title.eq(penti.getTitle())).list();
		return result.size() > 0 ? false : true;
	}

	public int getCountForType(int contentType) {
		List<Penti> result = pentiDao.queryBuilder().where(Properties.ContentType.eq(contentType)).list();
		return result.size();
	}

	public List<Penti> readItems(int contentType, int limit, int offset) {
		QueryBuilder<Penti> queryBuilder = pentiDao.queryBuilder().limit(limit).offset(offset)
				.orderDesc(Properties.PubDate);
		// FIXME magin number

		if (contentType != -1) {
			queryBuilder = queryBuilder.where(Properties.ContentType.eq(contentType));
		} else {
			// get favorite items
			queryBuilder = queryBuilder.where(Properties.IsFavorite.eq(true));
		}
		List<Penti> items = queryBuilder.list();
		for (Penti penti : items) {
			if (penti.getContentType().equals(DBConstants.CONTENT_TYPE_TWITTE)) {
				penti.setDescription(convertHtmlToText(penti.getDescription()));
			}
		}
		return items;
	}

	public int getCountForFav() {
		List<Penti> result = pentiDao.queryBuilder().where(Properties.IsFavorite.eq(true)).list();
		return result.size();
	}

	public void addToFav(String ids) {
		setFavValueForItem(ids, true);
	}

	/**
	 * 
	 * @param ids
	 *            ids of items
	 * @param value
	 */
	private void setFavValueForItem(String ids, boolean value) {
		// AppContext.getDaoSession(mContext).clear();
		String[] idArray = ids.split(",");
		for (String id : idArray) {
			Penti penti = pentiDao.load(Long.parseLong(id));
			penti.setIsFavorite(value);
			pentiDao.update(penti);
		}
	}

	public void removeFromFav(String ids) {
		setFavValueForItem(ids, false);
	}

}
