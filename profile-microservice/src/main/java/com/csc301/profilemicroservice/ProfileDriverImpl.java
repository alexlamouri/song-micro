package com.csc301.profilemicroservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import org.springframework.stereotype.Repository;
import org.neo4j.driver.v1.Transaction;

@Repository
public class ProfileDriverImpl implements ProfileDriver {

	Driver driver = ProfileMicroserviceApplication.driver;

	public static void InitProfileDb() {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.userName)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.password)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT nProfile.userName IS UNIQUE";
				trans.run(queryStr);

				trans.success();
			}
			session.close();
		}
	}
	
	@Override
	public DbQueryStatus createUserProfile(String userName, String fullName, String password) {
		
		DbQueryStatus result;
		
		try (Transaction tx = driver.session().beginTransaction()) {
			
			String findProfile = String.format("MATCH (nProfile:profile {userName: \"%s\"}) RETURN nProfile", userName);
			StatementResult profile = tx.run(findProfile);
			
			if (profile.hasNext()) { // if Profile exists
				
				tx.failure();
				result = new DbQueryStatus("Username already exists", DbQueryExecResult.QUERY_ERROR_GENERIC);
			}
			
			else { // if Profile does not exist
				
				String findPlaylist = String.format("MATCH (nPlaylist:playlist {plName: \"%s-favourites\"}) RETURN nPlaylist", userName);
				StatementResult playlist = tx.run(findPlaylist);
				
				if (playlist.hasNext()) { // if Playlist exists
					
					tx.failure();
					result = new DbQueryStatus("Playlist already exists", DbQueryExecResult.QUERY_ERROR_GENERIC);
				}
				
				else { // if Playlist does not exist
						
					String createProfileAndPlaylist = String.format("CREATE (nProfile:profile {userName: \"%s\", fullName: \"%s\", password: \"%s\"})-[r:created]->(nPlaylist:playlist {plName: \"%s-favourites\"}) ",userName,fullName,password,userName);
					tx.run(createProfileAndPlaylist);
					
					tx.success();
					result = new DbQueryStatus("OK",DbQueryExecResult.QUERY_OK);
				}
			}
			
			return result;
		}
	}

	@Override
	public DbQueryStatus followFriend(String userName, String friendUserName) {
		
		DbQueryStatus result;
		
		try (Transaction tx = driver.session().beginTransaction()) {
			
			if (userName.equals(friendUserName)) { // if User and Friend are same
				
				tx.failure();
				result = new DbQueryStatus("User cannot follow self", DbQueryExecResult.QUERY_ERROR_GENERIC);
			}
			
			else { // if User and Friend are different
			
				String findUser = String.format("MATCH (nProfile:profile {userName: \"%s\"}) RETURN nProfile", userName); 
				StatementResult user = tx.run(findUser);
			
				if (!user.hasNext()) { // if User does not exist
					
					tx.failure();
					result = new DbQueryStatus("User does not exist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				} 
			
				else { // if User exists
					
					String findFriend = String.format("MATCH (nFriendProfile:profile {userName: \"%s\"}) RETURN nFriendProfile", friendUserName); //Matching for user name
					StatementResult friend = tx.run(findFriend);
				
					if (!friend.hasNext()) { // if Friend does not exist
						
						tx.failure();
						result = new DbQueryStatus("Friend does not exist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
					} 
				
					else {
						
						String findFollowing = String.format("MATCH r=(nProfile:profile)-[:follows]->(nFriendProfile:profile) WHERE nProfile.userName = \"%s\" AND nFriendProfile.userName = \"%s\" RETURN r", userName, friendUserName);
						StatementResult following = tx.run(findFollowing);
						
						if (following.hasNext()) { // if User already follows Friend
							
							tx.failure();
							result = new DbQueryStatus("User already follows Friend", DbQueryExecResult.QUERY_ERROR_GENERIC);
						}
						
						else { // if User does not follow Friend
							
							String followFriend = String.format("MATCH (nProfile:profile),(nFriendProfile:profile) WHERE nProfile.userName = \"%s\" AND nFriendProfile.userName = \"%s\" CREATE (nProfile)-[:follows]->(nFriendProfile)", userName, friendUserName);
							tx.run(followFriend);
							
							tx.success();
							result = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
						}
					}
				}
			}	
			
			return result;
		}
	}

	@Override
	public DbQueryStatus unfollowFriend(String userName, String friendUserName) {
		
		DbQueryStatus result;
		
		try (Transaction tx = driver.session().beginTransaction()) {
			
			String findUser = String.format("MATCH (nProfile:profile {userName: \"%s\"}) RETURN nProfile", userName); 
			StatementResult user = tx.run(findUser);
			
			if (!user.hasNext()) { // if User does not exist
				
				tx.failure();
				result = new DbQueryStatus("User does not exist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			} 
			
			else {
				
				String findFriend = String.format("MATCH (nFriendProfile:profile {userName: \"%s\"}) RETURN nFriendProfile", friendUserName); //Matching for user name
				StatementResult friend = tx.run(findFriend);
				
				if (!friend.hasNext()) { // if Friend does not exist
					
					tx.failure();
					result = new DbQueryStatus("Friend does not exist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				} 
				
				else {
					
					String findFollowing = String.format("MATCH r=(nProfile:profile)-[:follows]->(nFriendProfile:profile) WHERE nProfile.userName = \"%s\" AND nFriendProfile.userName = \"%s\" RETURN r", userName, friendUserName);
					StatementResult following = tx.run(findFollowing);
					
					if (!following.hasNext()) { // if User does not follow Friend
						
						tx.failure();
						result = new DbQueryStatus("User does not follow Friend", DbQueryExecResult.QUERY_ERROR_GENERIC);
						
					} 
					
					else { // if User follows Friend
						
						String unfollowFriend = String.format("MATCH (nProfile:profile {userName: \"%s\"})-[f:follows]->(nFriendProfile:profile {userName: \"%s\"}) DELETE f", userName, friendUserName);
						tx.run(unfollowFriend);
						tx.success();
			
						result = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
					}
				}
			}
		}
			
		return result;
	}
	

	@Override
	public DbQueryStatus getAllSongFriendsLike(String userName) {
		
		DbQueryStatus result;
		
		try (Transaction tx = driver.session().beginTransaction()) {
			
			String findUser = String.format("MATCH (nProfile:profile {userName: \"%s\"}) RETURN nProfile", userName); 
			StatementResult user = tx.run(findUser);
			
			if (!user.hasNext()) { // if User does not exist
				result = new DbQueryStatus("User does not exist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			} 
			
			else { // if User exists
				
				String findFriends = String.format("MATCH (u:profile)-[r:follows]->(f:profile) WHERE u.userName = \"%s\" RETURN f.userName;", userName);
				StatementResult friends = tx.run(findFriends);
				
				JSONArray friendsFavouritesArray = new JSONArray();
				
				while (friends.hasNext()) { // get all Friends
					
					Record friend = friends.next();
					String friendUserName = friend.get("f.userName").asString();
					System.out.println(friendUserName);
					
					String findFavourites = String.format("MATCH (p:playlist)-[i:includes]->(s:song) WHERE p.plName = \"%s-favourites\" RETURN s.songId;", friendUserName);
					StatementResult favourites = tx.run(findFavourites);
					
					JSONArray favouritesArray = new JSONArray();
					
					while (favourites.hasNext()) { // get all Friends Favourites
						
						Record favourite = favourites.next();
						String favouriteSongId = favourite.get("s.songId").asString();
						System.out.println(favouriteSongId);
						
						favouritesArray.put(favouriteSongId);
					}
					
					JSONObject friendsFavourites = new JSONObject().put(friendUserName, favouritesArray);
					System.out.println(friendsFavourites);
					friendsFavouritesArray.put(friendsFavourites);
					System.out.println(friendsFavouritesArray);
				}
				
				result = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
				result.setData(friendsFavouritesArray.toList());
			}

			return result;
		}
	}
}
