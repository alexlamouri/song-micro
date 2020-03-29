package com.csc301.songmicroservice;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/")
public class SongController {

	@Autowired
	private final SongDal songDal;

	private OkHttpClient client = new OkHttpClient();

	
	public SongController(SongDal songDal) {
		this.songDal = songDal;
	}

	
	@RequestMapping(value = "/getSongById/{songId}", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getSongById(@PathVariable("songId") String songId,
			HttpServletRequest request) {
	
		Map<String, Object> response = new HashMap<String, Object>();
	
		DbQueryStatus dbQueryStatus = this.songDal.findSongById(songId);
		
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		response.put("message", dbQueryStatus.getMessage());
		response.put("path", String.format("GET %s", Utils.getUrl(request)));
		return response;
	}

	
	@RequestMapping(value = "/getSongTitleById/{songId}", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getSongTitleById(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		
		DbQueryStatus dbQueryStatus = this.songDal.getSongTitleById(songId);
		
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		response.put("message", dbQueryStatus.getMessage());
		response.put("path", String.format("GET %s", Utils.getUrl(request)));
		return response;
	}

	
	@RequestMapping(value = "/deleteSongById/{songId}", method = RequestMethod.DELETE)
	public @ResponseBody Map<String, Object> deleteSongById(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		DbQueryStatus dbQueryStatus = this.songDal.deleteSongById(songId);
		
		//Check if song was successfully deleted in Mongo, send to deleteSong in profile
		if (dbQueryStatus.getdbQueryExecResult() == DbQueryExecResult.QUERY_OK) {
			
			HttpUrl.Builder urlBuilder = HttpUrl.parse("http://localhost:3002" + "/deleteAllSongsFromDb").newBuilder();
			urlBuilder.addPathSegment(songId);
			String url = urlBuilder.build().toString();
			RequestBody body = RequestBody.create(null,new byte[0]);
			Request deleteSong = new Request.Builder() //making request to some other service 
					.url(url)
					.method("PUT", body)
					.build();
			Call call = client.newCall(deleteSong);
			Response responseFromProfileMs = null;
			
			try {
				//Send to profile
				responseFromProfileMs = call.execute();
				JSONObject result = new JSONObject(responseFromProfileMs.body().string());
				String resultStatus = result.getString("status");
				
				if (!resultStatus.equals("OK")) {
					dbQueryStatus = new DbQueryStatus("Song could not be deleted in Profile",DbQueryExecResult.QUERY_ERROR_GENERIC);
				} 
			} 
			
			catch (IOException e) {
				e.printStackTrace();
			}
		} 
		
		response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		response.put("message", dbQueryStatus.getMessage());
		response.put("path", String.format("GET %s", Utils.getUrl(request)));
		return response;
	}

	
	@RequestMapping(value = "/addSong", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addSong(@RequestParam Map<String, String> params,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		
		if (!(params.containsKey("songName") && params.containsKey("songArtistFullName") && params.containsKey("songAlbum"))){
			
			DbQueryStatus dbQueryStatus = new DbQueryStatus("Missing Parameters", DbQueryExecResult.QUERY_ERROR_GENERIC);
			
			response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
			response.put("message", dbQueryStatus.getMessage());
			response.put("path", String.format("GET %s", Utils.getUrl(request)));
			return response;
		}
		
		else {
			
			String newSongName = params.get("songName");
			String newSongArtist = params.get("songArtistFullName");
			String newSongAlbum = params.get("songAlbum");
			Song newSong = new Song(newSongName, newSongArtist, newSongAlbum);
			
			DbQueryStatus dbQueryStatus = this.songDal.addSong(newSong);
			dbQueryStatus.setData(newSong);
			
			response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
			response.put("message", dbQueryStatus.getMessage());
			response.put("path", String.format("GET %s", Utils.getUrl(request)));
			return response;
		}	
	}

	
	@RequestMapping(value = "/updateSongFavouritesCount/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> updateFavouritesCount(@PathVariable("songId") String songId,
			@RequestParam("shouldDecrement") String shouldDecrement, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		
		
		if (shouldDecrement.equals("true")) {
			
			DbQueryStatus dbQueryStatus = this.songDal.updateSongFavouritesCount(songId, true);
			
			response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
			response.put("message", dbQueryStatus.getMessage());
			response.put("path", String.format("GET %s", Utils.getUrl(request)));
			return response;
		} 
		
		else if (shouldDecrement.equals("false")) {
			
			DbQueryStatus dbQueryStatus = this.songDal.updateSongFavouritesCount(songId, false);
			
			response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
			response.put("message", dbQueryStatus.getMessage());
			response.put("path", String.format("GET %s", Utils.getUrl(request)));
			return response;
		} 
		
		else { //if shouldDecrement is something other than true or false
			
			DbQueryStatus dbQueryStatus = new DbQueryStatus("Wrong Parameters", DbQueryExecResult.QUERY_ERROR_GENERIC);
			
			response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
			response.put("message", dbQueryStatus.getMessage());
			response.put("path", String.format("GET %s", Utils.getUrl(request)));
			return response;
		}
	}
	
}