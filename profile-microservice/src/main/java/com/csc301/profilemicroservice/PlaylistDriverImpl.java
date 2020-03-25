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
		try (Session session = driver.session()){
			//check if user name exist
			String matchPlaylist = String.format("MATCH (nPlaylist:playlist {plName: \"%s-favourites\"}) RETURN nPlaylist", userName);
			StatementResult resultPlaylist = session.run(matchPlaylist);
			if (!(resultPlaylist.hasNext())) {
				result = new DbQueryStatus("Username does not exist",DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				return result;
			}
			
			//check if song is already in favorites
			String match = String.format("MATCH r=(nPlaylist:playlist {plName: \"%s-favourites\"})-[:includes]->(nSong:song {songId: \"%s\"}) RETURN r",userName,songId);
			StatementResult resultFav = session.run(match);
			if (resultFav.hasNext()) {
				result = new DbQueryStatus("Song already in favourites",DbQueryExecResult.QUERY_ERROR_GENERIC);
				return result;
			}
			 else {
				try (Transaction tx = session.beginTransaction()){
					String addSong = String.format("MATCH (nPlaylist:playlist) WHERE nPlaylist.plName = \"%s-favourites\" CREATE (nPlaylist)-[:includes]->(nSong:song {songId: \"%s\"})",userName,songId);
					tx.run(addSong);
					tx.success();
					result = new DbQueryStatus("Ok",DbQueryExecResult.QUERY_OK);
					return result;
					
				}
			}
		}
	}

	@Override
	public DbQueryStatus unlikeSong(String userName, String songId) {
		
		DbQueryStatus result;
		try (Session session = driver.session()){
			//check if user name exist
			String matchPlaylist = String.format("MATCH (nPlaylist:playlist {plName: \"%s-favourites\"}) RETURN nPlaylist", userName);
			StatementResult resultPlaylist = session.run(matchPlaylist);
			if (!(resultPlaylist.hasNext())) {
				result = new DbQueryStatus("Username does not exist",DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				return result;
			}
			
			//check if song is in favorites
			String match = String.format("MATCH r=(nPlaylist:playlist {plName: \"%s-favourites\"})-[:includes]->(nSong:song {songId: \"%s\"}) RETURN r",userName,songId);
			StatementResult resultFav = session.run(match);
			if (!(resultFav.hasNext())) {
				System.out.println("how?");
				result = new DbQueryStatus("Song not in favourites",DbQueryExecResult.QUERY_ERROR_GENERIC);
				return result;
			}
			 else {
				try (Transaction tx = session.beginTransaction()){
					String addSong = String.format("MATCH (nPlaylist:playlist {plName: \"%s-favourites\"})-[r:includes]->(nSong:song {songId: \"%s\"}) DELETE r",userName,songId);
					tx.run(addSong);
					tx.success();
					result = new DbQueryStatus("Ok",DbQueryExecResult.QUERY_OK);
					return result;
					
				}
			}
		}
	}
	

	@Override
	public DbQueryStatus deleteSongFromDb(String songId) {
		
		return null;
	}
}
