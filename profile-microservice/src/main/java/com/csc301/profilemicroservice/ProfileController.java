package com.csc301.profilemicroservice;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.csc301.profilemicroservice.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/")
public class ProfileController {
	public static final String KEY_USER_NAME = "userName";
	public static final String KEY_USER_FULLNAME = "fullName";
	public static final String KEY_USER_PASSWORD = "password";

	@Autowired
	private final ProfileDriverImpl profileDriver;

	@Autowired
	private final PlaylistDriverImpl playlistDriver;

	OkHttpClient client = new OkHttpClient();

	public ProfileController(ProfileDriverImpl profileDriver, PlaylistDriverImpl playlistDriver) {
		this.profileDriver = profileDriver;
		this.playlistDriver = playlistDriver;
	}

	@RequestMapping(value = "/profile", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addProfile(@RequestParam Map<String, String> params,
			HttpServletRequest request) {
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("POST %s", Utils.getUrl(request)));
		if (!(params.containsKey("userName") && params.containsKey("fullName") && params.containsKey("password"))){
			DbQueryStatus dbQueryStatus = new DbQueryStatus("Missing Parameters", DbQueryExecResult.QUERY_ERROR_GENERIC);
			response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
			return response;
		}
		String newUserName = params.get("userName");
		String newFullName = params.get("fullName");
		String newPasswd = params.get("password");
		DbQueryStatus dbQueryStatus = this.profileDriver.createUserProfile(newUserName, newFullName, newPasswd);
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		return response;
	}

	@RequestMapping(value = "/followFriend/{userName}/{friendUserName}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> followFriend(@PathVariable("userName") String userName,
			@PathVariable("friendUserName") String friendUserName, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		DbQueryStatus dbQueryStatus = this.profileDriver.followFriend(userName, friendUserName);
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		return response;
	}

	@RequestMapping(value = "/getAllFriendFavouriteSongTitles/{userName}", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getAllFriendFavouriteSongTitles(@PathVariable("userName") String userName,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));

		return null;
	}


	@RequestMapping(value = "/unfollowFriend/{userName}/{friendUserName}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> unfollowFriend(@PathVariable("userName") String userName,
			@PathVariable("friendUserName") String friendUserName, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		DbQueryStatus dbQueryStatus = this.profileDriver.unfollowFriend(userName, friendUserName);
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		return response;
	}

	@RequestMapping(value = "/likeSong/{userName}/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> likeSong(@PathVariable("userName") String userName,
			@PathVariable("songId") String songId, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		//String path = String.format("PUT http://localhost:3001/updateSongFavouritesCount/%s?shouldDecrement=false", songId);
		DbQueryStatus dbQueryStatus = null; 
		
		//Check if song exists in MongoDb
		if (songId != null) {
			HttpUrl.Builder urlGetSong = HttpUrl.parse("http://localhost:3001" + "/getSongById").newBuilder();
			urlGetSong.addPathSegment(songId);
			String urlSong = urlGetSong.build().toString();
			
			//RequestBody bodySong = RequestBody.create(null,new byte[0]);
			Request requestGetSong = new Request.Builder() //making request to some Song service 
					.url(urlSong)
					.method("GET", null)
					.build();
			Call callGetSong = client.newCall(requestGetSong);
			Response responseFromGetSong = null;
			
			try {
				responseFromGetSong = callGetSong.execute();
				JSONObject data = new JSONObject(responseFromGetSong.body().string());
				String status = data.getString("status");
				
				if (!status.equals("OK")){
					dbQueryStatus = new DbQueryStatus("Song does not exist in MongoDb",DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				} else { 
					//Song exists
					//Send it to likeSong (neo4j)
					dbQueryStatus = this.playlistDriver.likeSong(userName, songId);
					
					//likeSong is success, send it to updateSongFavouritesCount (Mongo)
					if (dbQueryStatus.getdbQueryExecResult() == DbQueryExecResult.QUERY_OK) {
					
						HttpUrl.Builder urlBuilder = HttpUrl.parse("http://localhost:3001" + "/updateSongFavouritesCount").newBuilder();
						urlBuilder.addPathSegment(songId);
						urlBuilder.addQueryParameter("shouldDecrement", "false");
						String url = urlBuilder.build().toString();
						
						RequestBody body = RequestBody.create(null,new byte[0]);
						Request requestSong = new Request.Builder() //making request to some other service 
								.url(url)
								.method("PUT", body)
								.build();
						Call call = client.newCall(requestSong);
						Response responseFromSongMs = null;
						String responseFromSong = "";

						try {
							responseFromSongMs = call.execute();
							JSONObject result = new JSONObject(responseFromSongMs.body().string());
							String resultStatus = result.getString("status");
							if (!resultStatus.equals("OK")) {
								dbQueryStatus = new DbQueryStatus("Song could not be incremented",DbQueryExecResult.QUERY_ERROR_GENERIC);
							} 
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
			}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		return response;
	}

	@RequestMapping(value = "/unlikeSong/{userName}/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> unlikeSong(@PathVariable("userName") String userName,
			@PathVariable("songId") String songId, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));

		DbQueryStatus dbQueryStatus = null; 
		
		//Check if song exists in MongoDb
		if (songId != null) {
			HttpUrl.Builder urlGetSong = HttpUrl.parse("http://localhost:3001" + "/getSongById").newBuilder();
			urlGetSong.addPathSegment(songId);
			String urlSong = urlGetSong.build().toString();
			Request requestGetSong = new Request.Builder() //making request to some Song service 
					.url(urlSong)
					.method("GET", null)
					.build();
			Call callGetSong = client.newCall(requestGetSong);
			Response responseFromGetSong = null;
			
			try {
				responseFromGetSong = callGetSong.execute();
				JSONObject data = new JSONObject(responseFromGetSong.body().string());
				String status = data.getString("status");
				
				if (!status.equals("OK")){
					dbQueryStatus = new DbQueryStatus("Song does not exist in MongoDb",DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
				} else { 
					//Song exists
					//Send it to unlikeSong (neo4j)
					dbQueryStatus = this.playlistDriver.unlikeSong(userName, songId);
					
					//likeSong is success, send it to updateSongFavouritesCount (Mongo)
					if (dbQueryStatus.getdbQueryExecResult() == DbQueryExecResult.QUERY_OK) {
					
						HttpUrl.Builder urlBuilder = HttpUrl.parse("http://localhost:3001" + "/updateSongFavouritesCount").newBuilder();
						urlBuilder.addPathSegment(songId);
						urlBuilder.addQueryParameter("shouldDecrement", "true");
						String url = urlBuilder.build().toString();
						
						RequestBody body = RequestBody.create(null,new byte[0]);
						Request requestSong = new Request.Builder() //making request to some other service 
								.url(url)
								.method("PUT", body)
								.build();
						Call call = client.newCall(requestSong);
						Response responseFromSongMs = null;
						String responseFromSong = "";

						try {
							responseFromSongMs = call.execute();
							JSONObject result = new JSONObject(responseFromSongMs.body().string());
							String resultStatus = result.getString("status");
							if (!resultStatus.equals("OK")) {
								dbQueryStatus = new DbQueryStatus("Song could not be incremented",DbQueryExecResult.QUERY_ERROR_GENERIC);
							} 
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
			}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		return response;
	}

	@RequestMapping(value = "/deleteAllSongsFromDb/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> deleteAllSongsFromDb(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("path", String.format("PUT %s", Utils.getUrl(request)));
		
		DbQueryStatus dbQueryStatus = this.playlistDriver.deleteSongFromDb(songId);
		
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		return response;
	}
}