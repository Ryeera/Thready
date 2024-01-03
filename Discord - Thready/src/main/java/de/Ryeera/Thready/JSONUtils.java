package de.Ryeera.Thready;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A collection of utility-methods all around JSON.
 * 
 * @author Ryeera
 * @version 1.1
 * @see JSONArray
 * @see JSONObject
 */
public class JSONUtils {

	/**
	 * Read a JSON-String from a file
	 *
	 * @param  source      The file to read from
	 * @return a parsed JSONObject
	 * @throws IOException If the file could not be read
	 * @author Ryeera
	 * @since 1.0
	 */
	public static JSONObject readJSON(File source) throws IOException {
		String         json = "";
		BufferedReader in   = new BufferedReader(new FileReader(source));
		String         line;
		while ((line = in.readLine()) != null)
			if (!line.equals(""))
				json += line;
		in.close();
		return new JSONObject(json);
	}
	
	/**
	 * Writes a JSON-String to the specified file
	 *
	 * @param  json                  The String to write
	 * @param  file                  The file to write to
	 * @throws FileNotFoundException If the file could not be written
	 * @author Ryeera
	 * @since 1.0
	 */
	public static void writeJSON(JSONObject json, File file) throws FileNotFoundException {
		PrintWriter out = new PrintWriter(file);
		out.print(json.toString(2));
		out.flush();
		out.close();
	}
	
	/**
	 * Converts a JSONArray to an Array
	 *
	 * @param  source the JSONArray to be converted
	 * @return an Array containing all the Objects from the JSONArray
	 * @author Ryeera
	 * @since 1.1
	 */
	public static Object[] toArray(JSONArray source) {
		Object[] returns = new Object[source.length()];
		for (int i = 0; i < source.length(); i++)
			returns[i] = source.get(i);
		return returns;
	}
	
	/**
	 * Converts a JSONArray containing only Strings to a String-array
	 *
	 * @param  source the JSONArray to be converted
	 * @return an Array containing all Strings from the JSONArray
	 * @author Ryeera
	 * @since 1.1
	 */
	public static String[] toStringArray(JSONArray source) {
		String[] returns = new String[source.length()];
		for (int i = 0; i < source.length(); i++)
			returns[i] = source.getString(i);
		return returns;
	}
	
	/**
	 * Converts a JSONArray containing only Strings to a List of Strings
	 *
	 * @param  source the JSONArray to be converted
	 * @return a List containing all Strings from the JSONArray
	 * @author Ryeera
	 * @since 1.1
	 */
	public static List<String> toStringList(JSONArray source) {
		List<String> returns = new ArrayList<>();
		for (int i = 0; i < source.length(); i++)
			returns.add(source.getString(i));
		return returns;
	}
}
