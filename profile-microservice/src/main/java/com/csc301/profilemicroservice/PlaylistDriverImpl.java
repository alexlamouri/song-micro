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
			
			String findPlaylist = String.format(
					"MATCH r = (nProfile:profile)-[:created]->(nPlaylist:playlist)"
					+ "WHERE nProfile.userName = \"%s\" "
					+ "AND nPlaylist.plName = \"%s-favourites\" "
					+ "RETURN r", 
					userName, userName);
			StatementResult playlist = tx.run(findPlaylist);
			
			if (!playlist.hasNext()) { // if User's Playlist not found
				
				tx.failure();
				result = new DbQueryStatus("User does not exist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}
			
			else { // if User's Playlist found
				
				String findSongInPlaylist = String.format(
						"MATCH r = (nPlaylist:playlist)-[:includes]->(nSong:song) "
						+ "WHERE nPlaylist.plName = \"%s-favourites\" "
						+ "AND nSong.songId = \"%s\" "
						+ "RETURN r", 
						userName, songId);
				StatementResult songInPlaylist = tx.run(findSongInPlaylist);
				
				if (songInPlaylist.hasNext()) { // if Song in User's Playlist
					
					tx.failure();
					result = new DbQueryStatus("Song already liked by user", DbQueryExecResult.QUERY_ERROR_GENERIC);
				}
				
				else { // if song not in User's Playlist
					
					String findSongInDb = String.format(
							"MATCH (nSong:song) "
							+ "WHERE nSong.songId = \"%s\" "
							+ "RETURN nSong", 
							songId);
					StatementResult songInDb = tx.run(findSongInDb);
					
					if (songInDb.hasNext()) { // if Song in Neo4j
						
						String likeExistingSong = String.format(
								"MATCH (nPlaylist:playlist), (nSong:song) "
								+ "WHERE nPlaylist.plName = \"%s-favourites\" "
								+ "AND nSong.songId = \"%s\" "
								+ "CREATE (nPlaylist)-[:includes]->(nSong)", 
								userName, songId);
						tx.run(likeExistingSong);
						tx.success();
					}
					
					else { // if Song not in Neo4j
						
						String likeNewSong = String.format(
								"MATCH (nPlaylist:playlist) "
								+ "WHERE nPlaylist.plName = \"%s-favourites\" "
								+ "CREATE (nPlaylist)-[:includes]->(nSong:song {songId: \"%s\"})", 
								userName, songId);
						tx.run(likeNewSong);
						tx.success();
					}
				
					result = new DbQueryStatus("User liked song", DbQueryExecResult.QUERY_OK);	
				}	
			}	
		}
		
		return result;
	}

	@Override
	public DbQueryStatus unlikeSong(String userName, String songId) {
		
		DbQueryStatus result;
		
		try (Transaction tx = driver.session().beginTransaction()) {
			
			String findPlaylist = String.format(
					"MATCH r = (nProfile:profile)-[:created]->(nPlaylist:playlist)"
					+ "WHERE nProfile.userName = \"%s\" "
					+ "AND nPlaylist.plName = \"%s-favourites\" "
					+ "RETURN r", 
					userName, userName);
			StatementResult playlist = tx.run(findPlaylist);
			
			if (!playlist.hasNext()) { // if user playlist not found
				
				tx.failure();
				result = new DbQueryStatus("User does not exist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}
			
			else { // if user playlist found
				
				String findSongInPlaylist = String.format(
						"MATCH r = (nPlaylist:playlist)-[:includes]->(nSong:song) "
						+ "WHERE nPlaylist.plName = \"%s-favourites\" "
						+ "AND nSong.songId = \"%s\" "
						+ "RETURN r", 
						userName, songId);
				StatementResult songInPlaylist = tx.run(findSongInPlaylist);
				
				if (!songInPlaylist.hasNext()) { // if song not in user playlist
					
					tx.failure();
					result = new DbQueryStatus("Song not liked by user", DbQueryExecResult.QUERY_ERROR_GENERIC);
				}
				
				else { // if song in user playlist
					
					String unlikeSong = String.format(
							"MATCH (nPlaylist:playlist)-[i:includes]->(nSong:song) "
							+ "WHERE nPlaylist.plName = \"%s-favourites\" "
							+ "AND nSong.songId = \"%s\" "
							+ "DELETE i", userName, songId);
					tx.run(unlikeSong);
					tx.success();
					
					result = new DbQueryStatus("User unliked song", DbQueryExecResult.QUERY_OK);	
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
			
			result = new DbQueryStatus("Deleted song from Db",DbQueryExecResult.QUERY_OK);
			return result;
		}
	}
	
}
