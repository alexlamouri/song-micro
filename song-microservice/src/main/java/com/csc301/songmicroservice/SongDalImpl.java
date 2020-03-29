package com.csc301.songmicroservice;

import org.json.JSONObject;
import org.json.JSONException;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public class SongDalImpl implements SongDal {

	private final MongoTemplate db;

	@Autowired
	public SongDalImpl(MongoTemplate mongoTemplate) {
		this.db = mongoTemplate;
	}

	@Override
	public DbQueryStatus addSong(Song songToAdd) {

		DbQueryStatus result;
		
		try {
			
			Query findSong = new Query();
			findSong.addCriteria(Criteria.where("songName").is(songToAdd.getSongName()));
			findSong.addCriteria(Criteria.where("songArtistFullName").is(songToAdd.getSongArtistFullName()));	
			Song foundSong = this.db.findOne(findSong, Song.class);
			
			if (foundSong == null) { // if no duplicate song
				
				this.db.insert(songToAdd);
				
				result = new DbQueryStatus("Added song", DbQueryExecResult.QUERY_OK);
				result.setData(songToAdd.getJsonRepresentation());
			}
			
			else { // if duplicate song
				result = new DbQueryStatus("Song already added", DbQueryExecResult.QUERY_ERROR_GENERIC);
			}
		}
		
		catch (JSONException e) {
			result = new DbQueryStatus("JSON Error", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
		
		return result;
	}

	@Override
	public DbQueryStatus findSongById(String songId) {
		
		DbQueryStatus result;
		
		try {
			
			if (!ObjectId.isValid(songId)) {
				
				result = new DbQueryStatus("songId is not a valid id", DbQueryExecResult.QUERY_ERROR_GENERIC);
				return result;
			}
			
			Query findSong = new Query();
			findSong.addCriteria(Criteria.where("_id").is(songId));
			Song foundSong = this.db.findOne(findSong, Song.class);
			
			if (foundSong != null) { // if song found by id
				
				result = new DbQueryStatus("Song found", DbQueryExecResult.QUERY_OK);
				result.setData(foundSong.getJsonRepresentation());
				return result;
			}
			
			else { // if song not found by id
				
				result = new DbQueryStatus("Song not found", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				return result;
			}
		}
		
		catch (JSONException e) {
			
			result = new DbQueryStatus("JSON Error", DbQueryExecResult.QUERY_ERROR_GENERIC);
			return result;
		}
	}

	@Override
	public DbQueryStatus getSongTitleById(String songId) {

		DbQueryStatus result;
		
		try {
			
			if (!ObjectId.isValid(songId)) {
				
				result = new DbQueryStatus("songId is not a valid id", DbQueryExecResult.QUERY_ERROR_GENERIC);
				return result;
			}
			
			Query findSong = new Query();
			findSong.addCriteria(Criteria.where("_id").is(songId));
			Song foundSong = this.db.findOne(findSong, Song.class);
			
			if (foundSong != null) { // if song found by id
				
				result = new DbQueryStatus("Song title found", DbQueryExecResult.QUERY_OK);
				result.setData(foundSong.getSongName()); // respond with song title
				return result;
			}
			
			else { // if song not found by id
				
				result = new DbQueryStatus("Song title not found", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				return result;
			}
		}
		
		catch (JSONException e) {
			
			result = new DbQueryStatus("JSON Error", DbQueryExecResult.QUERY_ERROR_GENERIC);
			return result;
		}
	}

	@Override
	public DbQueryStatus deleteSongById(String songId) {
		
		DbQueryStatus result;
		
		try {
			
			if (!ObjectId.isValid(songId)) {
				
				result = new DbQueryStatus("songId is not a valid id", DbQueryExecResult.QUERY_ERROR_GENERIC);
				return result;
			}
			
			Query findSong = new Query();
			findSong.addCriteria(Criteria.where("_id").is(songId));
			Song foundSong = this.db.findOne(findSong, Song.class);
			
			if (foundSong != null) { // if song found by id
				
				this.db.remove(foundSong);
				result = new DbQueryStatus("Song deleted", DbQueryExecResult.QUERY_OK);
				return result;
			}
			
			else { // if song not found by id
				
				result = new DbQueryStatus("Song not found", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				return result;
			}
		}
		
		catch (JSONException e) {
			
			result = new DbQueryStatus("JSON Error", DbQueryExecResult.QUERY_ERROR_GENERIC);
			return result;
		}
	}

	@Override
	public DbQueryStatus updateSongFavouritesCount(String songId, boolean shouldDecrement) {

		DbQueryStatus result;
		
		try {
			
			if (!ObjectId.isValid(songId)) {
				
				result = new DbQueryStatus("songId is not a valid id", DbQueryExecResult.QUERY_ERROR_GENERIC);
				return result;
			}
			
			Query findSong = new Query();
			findSong.addCriteria(Criteria.where("_id").is(songId));
			Song foundSong = this.db.findOne(findSong, Song.class);
			
			if (foundSong != null) { // if song found by id
				
				long numFav = foundSong.getSongAmountFavourites();
				
				if (shouldDecrement && numFav > 0) { // if decrementing likes
					
					foundSong.setSongAmountFavourites(numFav - 1);
					this.db.save(foundSong);
					
					result = new DbQueryStatus("Amount favourites decremented", DbQueryExecResult.QUERY_OK);
					return result;
				}
				
				else if (!shouldDecrement) { // if incrementing likes
					
					foundSong.setSongAmountFavourites(numFav + 1);
					this.db.save(foundSong);
					
					result = new DbQueryStatus("Amount favourites incremented", DbQueryExecResult.QUERY_OK);
					return result;
				}
				
				else {
					
					result = new DbQueryStatus("Amount favourites did not change", DbQueryExecResult.QUERY_ERROR_GENERIC);
					return result;
				}
			}
			
			else { // if song not found by id
				
				result = new DbQueryStatus("Song not found", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				return result;
			}
		}
		
		catch (JSONException e) {
			
			result = new DbQueryStatus("JSON Error", DbQueryExecResult.QUERY_ERROR_GENERIC);
			return result;
		}
	}
	
}