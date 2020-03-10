package com.tv.ads;

import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ApplicationMain {
	public static void main(String[] args) {
		TreeMap<LocalDateTime, Integer> map = new TreeMap<>();
		
		JSONObject jsonObject = parseJsonFile("src/main/java/com/tv/ads/new_users.json");
		
		buildTVSpotsMap(jsonObject, map);
		
		long averageUsersPerMinute = calculateUsers(jsonObject, map);
		
		calculateAndPrintNewUsers(map, averageUsersPerMinute);
	}

	private static JSONObject parseJsonFile(String path) {
		JSONParser parser = new JSONParser();
		JSONObject jsonObject = new JSONObject();
		
		try (FileReader reader = new FileReader(path)){
			Object obj = parser.parse(reader);
			jsonObject = (JSONObject) obj;
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		} 
		return jsonObject;
	}
	
	@SuppressWarnings("unchecked")
	private static void buildTVSpotsMap(JSONObject jsonObject, SortedMap<LocalDateTime, Integer> map){
		JSONArray tvSpots = (JSONArray) jsonObject.get("tvSpots");
		Iterator<JSONObject> iterator = tvSpots.iterator();
		
		// Create a new entry in the map with the timestamp of the spot as the key, 
		// the value will be the counter for the new users, right now it will be put as 0
		while (iterator.hasNext()) {
			JSONObject tvSpot = iterator.next();
			LocalDateTime datetime = convertToLocalDateTime(tvSpot.get("time").toString());
			map.put(datetime, 0);
		}
	}
	
	@SuppressWarnings("unchecked")
	private static long calculateUsers(JSONObject jsonObject, TreeMap<LocalDateTime, Integer> map) {
		JSONArray newUsers = (JSONArray) jsonObject.get("newUsers");
		Object[] arr =  map.keySet().toArray();
		
		Iterator<JSONObject> iterator = newUsers.iterator();
		LocalDateTime firstUserTimestamp = convertToLocalDateTime(iterator.next().get("time").toString());
		int usersBeforeFirstTvSpot = 1;
		
		while (iterator.hasNext()) {
			JSONObject newUser = iterator.next();
			LocalDateTime userTime = convertToLocalDateTime(newUser.get("time").toString());
			
			// For every new user, we will acommodate its timestamp on the corresponding entry of the map
			for(int i = 0; i < arr.length; i ++) {
				LocalDateTime currentTvSpotTime = (LocalDateTime) arr[i];
				
				// For all cases except on the last index
				if(i < arr.length - 1) {
					LocalDateTime nextTvSpotTime = (LocalDateTime) arr[i+1];
					
					// Add one more user to the total count of usersBeforeFirstTvSpot, 
					// this will be used later to get the average number of users added in a minute before the first TV Spot
					if (userTime.isBefore(currentTvSpotTime) && i == 0) {
						usersBeforeFirstTvSpot++;
					}
					// Increase the counter of the key timestamp in the map when the user was added between current and next tv spot
					else if(userTime.isAfter(currentTvSpotTime) && userTime.isBefore(nextTvSpotTime)) {
						Integer currentUsers = map.get(arr[i]);
						map.put(currentTvSpotTime, currentUsers + 1);
						break;
					}
				} 
				// On the last index
				else if(userTime.isAfter(currentTvSpotTime)) { 
					Integer currentUsers = map.get(arr[i]);
					map.put(currentTvSpotTime, currentUsers + 1);
				}
			}
		}
		long minutes = calculateTimeDifferenceInMinutes(firstUserTimestamp, (LocalDateTime) arr[0]);
		return usersBeforeFirstTvSpot / minutes;
	}

	@SuppressWarnings("unchecked")
	private static void calculateAndPrintNewUsers(TreeMap<LocalDateTime, Integer> map, long averageUsersPerMinute) {
		Object[] arr = map.entrySet().toArray();
		
		for(int i = 0; i < arr.length; i++) {
			Entry<LocalDateTime,Integer> currentMapEntry = (Entry<LocalDateTime,Integer>) arr[i];
			
			if(i < arr.length - 1) {
				Entry<LocalDateTime,Integer> nextMapEntry = (Entry<LocalDateTime,Integer>) arr[i+1];
				long timeDelta = calculateTimeDifferenceInMinutes(currentMapEntry.getKey(), nextMapEntry.getKey());
				
				// Get current users added in the map key timestamp but subtract the average users should have been expected at every minute
				long newUsersAdded = Math.abs(currentMapEntry.getValue().intValue() - (timeDelta * averageUsersPerMinute));
				System.out.println("Spot " + (i+1) + ": " + newUsersAdded + " new users");
			} else {
				System.out.println("Spot " + (i+1) + ": " + currentMapEntry.getValue().intValue() + " new users");
			}
		}
	}
	
	private static LocalDateTime convertToLocalDateTime(String datetime) {
		DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
		return LocalDateTime.parse(datetime,format);
	}
	
	private static long calculateTimeDifferenceInMinutes(LocalDateTime first, LocalDateTime second) {
		return ChronoUnit.MINUTES.between(first, second);
	}
	
	/*
	 * Currently the time complexity will be O(n*m) in the worst scenario (when the user was added to the system after the last TV spot timestamp)
	 * n = size of tv spots
	 * m = size of new users
	 * 
	 * One way to improve the performance can be to group the new users timestamp by minute (it will contain the minute and total number of users in that minute), 
	 * that way the m size will decrease and we just to have to compare if the current user minute is between the current and next TV spot timestamp
	 * and once we find the corresponding timeframe, we will add the total number of users in that minute to the map counter
	 */
}

