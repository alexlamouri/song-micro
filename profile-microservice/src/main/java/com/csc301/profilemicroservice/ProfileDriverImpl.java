package com.csc301.profilemicroservice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
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
		
		try ( Session session = driver.session()){
			String match = String.format("MATCH (nProfile:profile {userName: \"%s\"}) RETURN nProfile", userName); //Matching for user name
			StatementResult resultProfile = session.run(match);
			
			String matchPl = String.format("MATCH (nPlaylist:playlist {plName: \"%s-favourites\"}) RETURN nPlaylist",userName); //Matching for playlist name
			StatementResult resultPlaylist = session.run(matchPl);
			
			String matchR = String.format("MATCH p=(nProfile:profile {userName: \"%s\"})-[r:created]->(nPlaylist:playlist {plName: \"%s-favourites\"}) RETURN p",userName,userName); //Matching for relationship
			StatementResult resultRelationship = session.run(matchR);
			
			if (resultProfile.hasNext()) { //user name already exists
				DbQueryStatus response = new DbQueryStatus("Username already exists",DbQueryExecResult.QUERY_ERROR_GENERIC);
				return response;
			} 
			else if (resultPlaylist.hasNext()) { //play list already exists
				DbQueryStatus response = new DbQueryStatus("Playlist already exists",DbQueryExecResult.QUERY_ERROR_GENERIC);
				return response;
			} else if (resultRelationship.hasNext()) { //relationship already exists
				DbQueryStatus response = new DbQueryStatus("Relationship already exists",DbQueryExecResult.QUERY_ERROR_GENERIC);
				return response;
			} else {
				try (Transaction tx = session.beginTransaction()){
			
					String createRelationship = String.format("CREATE (nProfile:profile {userName: \"%s\", fullName: \"%s\", password: \"%s\"})-[r:created]->(nPlaylist:playlist {plName: \"%s-favourites\"}) ",userName,fullName,password,userName); //add relationship
					tx.run(createRelationship);
					tx.success();
					
					DbQueryStatus response = new DbQueryStatus("Ok",DbQueryExecResult.QUERY_OK);
					return response;
				}
			}
		}
	}

	@Override
	public DbQueryStatus followFriend(String userName, String frndUserName) {
		
		try (Session session = driver.session()){
			//check if userName exists 
			String matchUserName = String.format("MATCH (nProfile:profile {userName: \"%s\"}) RETURN nProfile", userName); 
			StatementResult resultUserName = session.run(matchUserName);
			if (!(resultUserName.hasNext())) { 
				DbQueryStatus response = new DbQueryStatus("Username does not exist",DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				return response;
			} 
			String matchFrndUserName = String.format("MATCH (nFriendProfile:profile {userName: \"%s\"}) RETURN nFriendProfile", frndUserName); //Matching for user name
			StatementResult resultFrndUserName = session.run(matchUserName);
			if (!(resultFrndUserName.hasNext())) { //user name already exists
				DbQueryStatus response = new DbQueryStatus("Friend Username does not exist",DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				return response;
			} 
			else {
				try (Transaction tx = session.beginTransaction()){
					String follow = String.format("MATCH (nProfile:profile),(nFriendProfile:profile) WHERE nProfile.userName = \"%s\" AND nFriendProfile.userName = \"%s\" CREATE (nProfile)-[:follows]->(nFriendProfile)",userName,frndUserName);
					tx.run(follow);
					tx.success();
					
					DbQueryStatus response = new DbQueryStatus("Ok",DbQueryExecResult.QUERY_OK);
					return response;
				}
			}
		}
	}

	@Override
	public DbQueryStatus unfollowFriend(String userName, String frndUserName) {
		
		return null;
	}

	@Override
	public DbQueryStatus getAllSongFriendsLike(String userName) {
			
		return null;
	}
}
