package net.TheDgtl.Stargate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Stargate - A portal plugin for Bukkit
 * Copyright (C) 2011, 2012 Steven "Drakia" Scott <Contact@TheDgtl.net>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

public class LangLoader {
	private String UTF8_BOM = "\uFEFF";
	// Variables
	private String datFolder;
	private String lang;
	private HashMap<String, String> strList;
	private HashMap<String, String> defList;
	
	public LangLoader(String datFolder, String lang) {
		this.lang = lang;
		this.datFolder = datFolder;
		
		File tmp = new File(datFolder, lang + ".txt");
		if (!tmp.exists()) {
			tmp.getParentFile().mkdirs();
		}
		updateLanguage(lang);
		
		strList = load(lang);
		// We have a default hashMap used for when new text is added.
		InputStream is = Stargate.class.getResourceAsStream("resources/" + lang + ".txt");
		if (is != null) {
			defList = load("en", is);
		} else {
			defList = null;
			Stargate.log.severe("[Stargate] Error loading backup language. There may be missing text ingame");
		}
	}
	
	public boolean reload() {
		// This extracts/updates the language as needed
		updateLanguage(lang);
		strList = load(lang);
		return true;
	}
	
	public String getString(String name) {
		String val = strList.get(name);
		if (val == null && defList != null) val = defList.get(name);
		if (val == null) return "";
		return val;
	}
	
	public void setLang(String lang) {
		this.lang = lang;
	}
	
	// This function updates on-disk language files
	// with missing lines from the in-JAR files
	private void updateLanguage(String lang) {
		// Load the current language file
		ArrayList<String> keyList = new ArrayList<String>();
		ArrayList<String> valList = new ArrayList<String>();
		
		HashMap<String, String> curLang = load(lang);
		
		InputStream is = Stargate.class.getResourceAsStream("resources/" + lang + ".txt");
		if (is == null) return;
		
		boolean updated = false;
		FileOutputStream fos = null;
		try {
			// Input stuff
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			
			String line = br.readLine();
			boolean firstLine = true;
			while (line != null) {
				// Strip UTF BOM
				if (firstLine) line = removeUTF8BOM(line);
				firstLine = false;
				// Split at first "="
				int eq = line.indexOf('=');
				if (eq == -1) {
					keyList.add("");
					valList.add("");
					line = br.readLine();
					continue;
				}
				String key = line.substring(0, eq);
				String val = line.substring(eq);
				
				if (curLang == null || curLang.get(key) == null) {
					keyList.add(key);
					valList.add(val);
					updated = true;
				} else {
					keyList.add(key);
					valList.add("=" + curLang.get(key));
					curLang.remove(key);
				}
				line = br.readLine();
			}
			br.close();
			
			// Save file
			fos = new FileOutputStream(datFolder + lang + ".txt");
			OutputStreamWriter out = new OutputStreamWriter(fos, "UTF8");
			BufferedWriter bw = new BufferedWriter(out);
			
			// Output normal Language data
			for (int i = 0; i < keyList.size(); i++) {
				bw.write(keyList.get(i) + valList.get(i));
				bw.newLine();
			}
			bw.newLine();
			// Output any custom language strings the user had
			for (String key : curLang.keySet()) {
				bw.write(key + "=" + curLang.get(key));
				bw.newLine();
			}
			
			bw.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (fos != null) {
				try {fos.close();} catch (Exception ex) {}
			}
		}
		if (updated)
			Stargate.log.info("[Stargate] Your language file (" + lang + ".txt) has been updated");
	}
	
	private HashMap<String, String> load(String lang) {
		return load(lang, null);
	}
	
	private HashMap<String, String> load(String lang, InputStream is) {
		HashMap<String, String> strings = new HashMap<String, String>();
		FileInputStream fis = null;
		InputStreamReader isr = null;
		try {
			if (is == null) {
				fis = new FileInputStream(datFolder + lang + ".txt");
				isr = new InputStreamReader(fis, "UTF8");
			} else {
				isr = new InputStreamReader(is, "UTF8");
			}
			BufferedReader br = new BufferedReader(isr);
			String line = br.readLine();
			boolean firstLine = true;
			while (line != null) {
				// Strip UTF BOM
				if (firstLine) line = removeUTF8BOM(line);
				firstLine = false;
				// Split at first "="
				int eq = line.indexOf('=');
				if (eq == -1) {
					line = br.readLine();
					continue;
				}
				String key = line.substring(0, eq);
				String val = line.substring(eq + 1);
				strings.put(key,  val);
				line = br.readLine();
			}
		} catch (Exception ex) {
			return null;
		} finally {
			if (fis != null) {
				try {fis.close();}
				catch (Exception ex) {}
			}
		}
		return strings;
	}
	
	public void debug() {
		Set<String> keys = strList.keySet();
		for (String key : keys) {
			Stargate.debug("LangLoader::Debug::strList", key + " => " + strList.get(key));
		}
		if (defList == null) return;
		keys = defList.keySet();
		for (String key : keys) {
			Stargate.debug("LangLoader::Debug::defList", key + " => " + defList.get(key));
		}
	}
	
    private String removeUTF8BOM(String s) {
        if (s.startsWith(UTF8_BOM)) {
            s = s.substring(1);
        }
        return s;
    }
}
