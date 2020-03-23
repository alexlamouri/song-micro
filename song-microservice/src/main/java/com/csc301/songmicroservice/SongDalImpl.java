package com.csc301.songmicroservice;

import java.io.IOException;

import org.bson.types.ObjectId;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import com.mongodb.client.MongoDatabase;



@Repository
public class SongDalImpl implements SongDal {

	private final MongoTemplate db;

	@Autowired
	public SongDalImpl(MongoTemplate mongoTemplate) {
		this.db = mongoTemplate;
	}

	@Override
	public DbQueryStatus addSong(Song songToAdd) throws JSONException {
		// TODO Auto-generated method stub
		DbQueryStatus result;
		try {
			this.db.insert(songToAdd);
			result = new DbQueryStatus("Ok", DbQueryExecResult.QUERY_OK);
		} catch (JSONException e) {
			result = new DbQueryStatus("JSONError", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
		
		return result;
	}

	@Override
	public DbQueryStatus findSongById(String songId) {
		// TODO Auto-generated method stub
		DbQueryStatus result=null;
		try {
			Query findId = new Query();
			findId.addCriteria(Criteria.where("_id").is(songId));
			Song sid = this.db.findOne(findId, Song.class);	
			if (sid == null) { //if id does not exist
				result = new DbQueryStatus("Ok",DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				return result;
			}
			result = new DbQueryStatus("Ok",DbQueryExecResult.QUERY_OK);
			result.setData(sid);
			return result;
		} catch (JSONException e) {
			result = new DbQueryStatus("JSONError", DbQueryExecResult.QUERY_ERROR_GENERIC);
			return result;
		}
	}

	@Override
	public DbQueryStatus getSongTitleById(String songId) {
		// TODO Auto-generated method stub
		DbQueryStatus result=null;
		try {
			Query findId = new Query();
			findId.addCriteria(Criteria.where("_id").is(songId));
			Song song = this.db.findOne(findId, Song.class);
			if (song == null) {
				result = new DbQueryStatus("Ok",DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				return result;
			}
			result = new DbQueryStatus("Ok",DbQueryExecResult.QUERY_OK);
			result.setData(song.getSongName());
			return result;
		} catch (JSONException e) {
			result = new DbQueryStatus("JSONError", DbQueryExecResult.QUERY_ERROR_GENERIC);
			return result;
		}
	}

	@Override
	public DbQueryStatus deleteSongById(String songId) {
		// TODO Auto-generated method stub
		DbQueryStatus result = null;
		try {
			Query findId = new Query();
			findId.addCriteria(Criteria.where("_id").is(songId));
			Song song = this.db.findOne(findId, Song.class);
			if (song == null) {
				result = new DbQueryStatus("Ok",DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				return result;
			}
			this.db.remove(findId,Song.class);
			result = new DbQueryStatus("Ok",DbQueryExecResult.QUERY_OK);
			return result;
		}catch (JSONException e) {
			result = new DbQueryStatus("JSONError", DbQueryExecResult.QUERY_ERROR_GENERIC);
			return result;
		}
	}

	@Override
	public DbQueryStatus updateSongFavouritesCount(String songId, boolean shouldDecrement) {
		// TODO Auto-generated method stub
		DbQueryStatus result = null;
		try {
			Query findId = new Query();
			findId.addCriteria(Criteria.where("_id").is(songId));
			Song song = this.db.findOne(findId, Song.class);
			if (song == null) { 
				result = new DbQueryStatus("Ok",DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				return result;
			}
			if (shouldDecrement) {
				long fav = song.getSongAmountFavourites();
				if (fav > 0) {
					song.setSongAmountFavourites(fav-1);
				}
				this.db.save(song);
				result = new DbQueryStatus("Ok",DbQueryExecResult.QUERY_OK);
				return result;
			} else {
				long fav = song.getSongAmountFavourites();
				song.setSongAmountFavourites(fav+1);
				this.db.save(song);
				result = new DbQueryStatus("Ok",DbQueryExecResult.QUERY_OK);
				return result;
			}
		}catch (JSONException e) {
			result = new DbQueryStatus("JSONError", DbQueryExecResult.QUERY_ERROR_GENERIC);
			return result;
		}
	}
}