package com.csc301.profilemicroservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
			
			if (profile.hasNext()) { // if existing profile
				
				tx.failure();
				result = new DbQueryStatus("Username already exists", DbQueryExecResult.QUERY_ERROR_GENERIC);
			}
			
			else {
				
				String findPlaylist = String.format("MATCH (nPlaylist:playlist {plName: \"%s-favourites\"}) RETURN nPlaylist", userName);
				StatementResult playlist = tx.run(findPlaylist);
				
				if (playlist.hasNext()) { // if existing playlist
					
					tx.failure();
					result = new DbQueryStatus("Playlist already exists", DbQueryExecResult.QUERY_ERROR_GENERIC);
				}
				
				else {
					
					String findRelationship = String.format("MATCH p=(nProfile:profile {userName: \"%s\"})-[r:created]->(nPlaylist:playlist {plName: \"%s-favourites\"}) RETURN p", userName, userName);
					StatementResult relationship = tx.run(findRelationship);
					
					if (relationship.hasNext()) { // if existing relationship
						
						tx.failure();
						result = new DbQueryStatus("Relationship already exists", DbQueryExecResult.QUERY_ERROR_GENERIC);
					}
					
					else {
						
						String createRelationship = String.format("CREATE (nProfile:profile {userName: \"%s\", fullName: \"%s\", password: \"%s\"})-[r:created]->(nPlaylist:playlist {plName: \"%s-favourites\"}) ",userName,fullName,password,userName);
						tx.run(createRelationship);
						tx.success();
						
						result = new DbQueryStatus("Ok",DbQueryExecResult.QUERY_OK);
					}
				}
				
			}
			
			return result;
		}
	}

	@Override
	public DbQueryStatus followFriend(String userName, String frndUserName) {
		
		DbQueryStatus result;
		
		try (Transaction tx = driver.session().beginTransaction()) {
			
			String findSelfProfile = String.format("MATCH (nProfile:profile {userName: \"%s\"}) RETURN nProfile", userName); 
			StatementResult selfProfile = tx.run(findSelfProfile);
			
			if (!selfProfile.hasNext()) { // if profile does not exist
				
				tx.failure();
				result = new DbQueryStatus("Username does not exist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			} 
			
			else {
				
				String findFriendProfile = String.format("MATCH (nFriendProfile:profile {userName: \"%s\"}) RETURN nFriendProfile", frndUserName); //Matching for user name
				StatementResult friendProfile = tx.run(findFriendProfile);
				
				if (!friendProfile.hasNext()) { // if friend profile does not exist
					
					tx.failure();
					result = new DbQueryStatus("Friend Username does not exist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				} 
				
				else {
					String findFollowFriend = String.format("MATCH r=(nProfile:profile)-[:follows]->(nFriendProfile:profile) WHERE nProfile.userName = \"%s\" AND nFriendProfile.userName = \"%s\" RETURN r",userName,frndUserName);
					StatementResult followFriendR = tx.run(findFollowFriend);
					
					if (followFriendR.hasNext()) { // check if friend already follows username
						
						tx.failure();
						result = new DbQueryStatus("Friend username already follows username", DbQueryExecResult.QUERY_ERROR_GENERIC);
					}
						else {
							String followFriend = String.format("MATCH (nProfile:profile),(nFriendProfile:profile) WHERE nProfile.userName = \"%s\" AND nFriendProfile.userName = \"%s\" CREATE (nProfile)-[:follows]->(nFriendProfile)", userName, frndUserName);
							tx.run(followFriend);
							tx.success();
					
							result = new DbQueryStatus("Ok", DbQueryExecResult.QUERY_OK);
						}
					
				}
			}
			
			return result;
		}
	}

	@Override
	public DbQueryStatus unfollowFriend(String userName, String frndUserName) {
		
		DbQueryStatus result;
		
		try (Transaction tx = driver.session().beginTransaction()) {
			
			String findSelfProfile = String.format("MATCH (nProfile:profile {userName: \"%s\"}) RETURN nProfile", userName); 
			StatementResult selfProfile = tx.run(findSelfProfile);
			
			if (!selfProfile.hasNext()) { // if profile does not exist
				
				tx.failure();
				result = new DbQueryStatus("Username does not exist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			} 
			
			else {
				
				String findFriendProfile = String.format("MATCH (nFriendProfile:profile {userName: \"%s\"}) RETURN nFriendProfile", frndUserName); //Matching for user name
				StatementResult friendProfile = tx.run(findFriendProfile);
				
				if (!friendProfile.hasNext()) { // if friend profile does not exist
					
					tx.failure();
					result = new DbQueryStatus("Friend Username does not exist", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				} 
				
				else {
					
					String follow = String.format("MATCH r=(nProfile:profile)-[:follows]->(nFriendProfile:profile) WHERE nProfile.userName = \"%s\" AND nFriendProfile.userName = \"%s\" RETURN r",userName,frndUserName);
					StatementResult checkFollow = tx.run(follow);
					
					if (!checkFollow.hasNext()) { //check if friend follows user name
						
						tx.failure();
						result = new DbQueryStatus("Friend Username does not exist", DbQueryExecResult.QUERY_ERROR_GENERIC);
						
					} 
					else {
						String unfollowFriend = String.format("MATCH (nProfile:profile {userName: \"%s\"})-[f:follows]->(nFriendProfile:profile {userName: \"%s\"}) DELETE f",userName,frndUserName);
						tx.run(unfollowFriend);
						tx.success();
			
						result = new DbQueryStatus("Ok", DbQueryExecResult.QUERY_OK);
						}
					}
				}
			}
			
			return result;
		}
	

	@Override
	public DbQueryStatus getAllSongFriendsLike(String userName) {
		DbQueryStatus result;
		try (Session session = driver.session()){
			//check if userName exists 
			String matchUserName = String.format("MATCH (nProfile:profile {userName: \"%s\"}) RETURN nProfile", userName); 
			StatementResult resultUserName = session.run(matchUserName);
			if (!(resultUserName.hasNext())) { 
				DbQueryStatus response = new DbQueryStatus("Username does not exist",DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				return response;
			} 
			
			//TODO: Complete rest of it 
			
		}
		return null;
	}
}
