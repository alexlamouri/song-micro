package com.csc301.profilemicroservice;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.springframework.stereotype.Repository;
import org.neo4j.driver.v1.Transaction;

@Repository
public class PlaylistDriverImpl implements PlaylistDriver {

	Driver driver = ProfileMicroserviceApplication.driver;

	public static void InitPlaylistDb() {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nPlaylist:playlist) ASSERT exists(nPlaylist.plName)";
				trans.run(queryStr);
				trans.success();
			}
			session.close();
		}
	}

	@Override
	public DbQueryStatus likeSong(String userName, String songId) {

		DbQueryStatus result;
		
		try (Transaction tx = driver.session().beginTransaction()) {
			
			String findPlaylist = String.format("MATCH (nPlaylist:playlist {plName: \"%s-favourites\"}) RETURN nPlaylist", userName);
			StatementResult playlist = tx.run(findPlaylist);
			
			if (!playlist.hasNext()) { // if user playlist not found
				
				tx.failure();
				result = new DbQueryStatus("Username does not exist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}
			
			else { // if user playlist found
				
				String findSong = String.format("MATCH r=(nPlaylist:playlist {plName: \"%s-favourites\"})-[:includes]->(nSong:song {songId: \"%s\"}) RETURN r", userName, songId);
				StatementResult song = tx.run(findSong);
				
				if (song.hasNext()) { // if song in user playlist
					
					tx.failure();
					result = new DbQueryStatus("Song already in favourites", DbQueryExecResult.QUERY_ERROR_GENERIC);
				}
				
				else { // if song not in user playlist
					
					String likeSong = String.format("MATCH (nPlaylist:playlist) WHERE nPlaylist.plName = \"%s-favourites\" CREATE (nPlaylist)-[:includes]->(nSong:song {songId: \"%s\"})", userName, songId);
					tx.run(likeSong);
					tx.success();
					
					result = new DbQueryStatus("OK",DbQueryExecResult.QUERY_OK);	
				}	
			}	
		}
		
		return result;
	}

	@Override
	public DbQueryStatus unlikeSong(String userName, String songId) {
		
		DbQueryStatus result;
		
		try (Transaction tx = driver.session().beginTransaction()) {
			
			String findPlaylist = String.format("MATCH (nPlaylist:playlist {plName: \"%s-favourites\"}) RETURN nPlaylist", userName);
			StatementResult playlist = tx.run(findPlaylist);
			
			if (!playlist.hasNext()) { // if user playlist not found
				
				tx.failure();
				result = new DbQueryStatus("Username does not exist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}
			
			else { // if user playlist found
				
				String findSong = String.format("MATCH r=(nPlaylist:playlist {plName: \"%s-favourites\"})-[:includes]->(nSong:song {songId: \"%s\"}) RETURN r", userName, songId);
				StatementResult song = tx.run(findSong);
				
				if (!song.hasNext()) { // if song not in user playlist
					
					tx.failure();
					result = new DbQueryStatus("Song not in favourites", DbQueryExecResult.QUERY_ERROR_GENERIC);
				}
				
				else { // if song in user playlist
					
					String unlikeSong = String.format("MATCH (nPlaylist:playlist {plName: \"%s-favourites\"})-[r:includes]->(nSong:song {songId: \"%s\"}) DELETE r", userName, songId);
					tx.run(unlikeSong);
					tx.success();
					
					result = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);	
				}	
			}	
		}
		
		return result;
	}

	@Override
	public DbQueryStatus deleteSongFromDb(String songId) {
		
		DbQueryStatus result;
		
		try (Transaction tx = driver.session().beginTransaction()) {
			
			String deleteSong = String.format("MATCH (nPlaylist:playlist)-[r:includes]->(nSong:song {songId: \"%s\"}) WHERE nPlaylist.plName =~ '.*-favourites' DELETE r,nSong",songId);
			tx.run(deleteSong);
			tx.success();
			
			result = new DbQueryStatus("Ok",DbQueryExecResult.QUERY_OK);
			return result;
		}
	}
}
