package org.openml.apiconnector.algorithms;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.openml.apiconnector.io.OpenmlConnector;

public class QueryUtils {

	public static Integer[] getIdsFromDatabase(OpenmlConnector apiconnector, String sql) throws Exception {
		JSONArray runJson = (JSONArray) apiconnector.freeQuery(sql).get("data");

		Integer[] result = new Integer[runJson.length()];
		for (int i = 0; i < runJson.length(); ++i) {
			result[i] = (int) ((JSONArray) runJson.get(i)).getDouble(0);
		}

		return result;
	}

	public static double[] getNumbersFromDatabase(OpenmlConnector apiconnector, String sql) throws Exception {
		JSONArray runJson = (JSONArray) apiconnector.freeQuery(sql).get("data");

		double[] result = new double[runJson.length()];
		for (int i = 0; i < runJson.length(); ++i) {
			result[i] = ((JSONArray) runJson.get(i)).getDouble(0);
		}

		return result;
	}

	public static String[] getStringsFromDatabase(OpenmlConnector apiconnector, String sql) throws Exception {
		JSONArray runJson = (JSONArray) apiconnector.freeQuery(sql).get("data");

		String[] result = new String[runJson.length()];
		for (int i = 0; i < runJson.length(); ++i) {
			result[i] = ((JSONArray) runJson.get(i)).getString(0);
		}

		return result;
	}

	public static double getIntFromDatabase(OpenmlConnector apiconnector, String sql) throws Exception {
		Integer[] result = getIdsFromDatabase(apiconnector, sql);
		return result[0];
	}
	
	public static String getStringFromDatabase(OpenmlConnector apiconnector, String sql) throws Exception {
		return (String) getRecordFromDatabase(apiconnector, sql).getString(0);
	}
	
	public static JSONArray getRecordFromDatabase(OpenmlConnector apiconnector, String sql) throws Exception {
		JSONArray runJson = (JSONArray) apiconnector.freeQuery(sql).get("data");
		if (runJson.length() >= 1) {
			return (JSONArray) runJson.get(0);
		} else {
			throw new Exception("record not found");
		}
	}
	
	public static Map<Integer,String> getMapFromDatabase(OpenmlConnector apiconnector, String sql) throws Exception {
		Map<Integer,String> result = new HashMap<Integer, String>();
		JSONArray runJson = (JSONArray) apiconnector.freeQuery(sql).get("data");
		
		for (int i = 0; i < runJson.length(); ++i) {
			JSONArray row = ((JSONArray) runJson.get(i));
			result.put(row.getInt(0), row.getString(1));
		}
		
		return result;
	}
}
