package net.TheDgtl.Stargate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Set;

public class LangLoader {
	private String UTF8_BOM = "\uFEFF";
	// Variables
	private String datFolder;
	private String lang;
	private HashMap<String, String> strList;
	
	public LangLoader(String datFolder, String lang) {
		this.lang = lang;
		this.datFolder = datFolder;
		strList = new HashMap<String, String>();
		
		File tmp = new File(datFolder, lang + ".txt");
		if (!tmp.exists()) {
			tmp.getParentFile().mkdirs();
			loadDefaults();
		}
		
		load();
	}
	
	public boolean reload() {
		strList = new HashMap<String, String>();
		load();
		return true;
	}
	
	public String getString(String name) {
		String val = strList.get(name);
		if (val == null) return "";
		return val;
	}
	
	public void setLang(String lang) {
		this.lang = lang;
	}
	
	private void loadDefaults() {
		InputStream is = Stargate.class.getResourceAsStream("resources/" + lang + ".txt");
		if (is == null) return;
		Stargate.log.info("[Stargate] Extracting initial language file -- " + lang + ".txt");
		
		FileOutputStream fos = null;
		try {
			// Input stuff
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			
			// Save file
			fos = new FileOutputStream(datFolder + lang + ".txt");
			OutputStreamWriter out = new OutputStreamWriter(fos);
			BufferedWriter bw = new BufferedWriter(out);
			
			String line = br.readLine();
			while (line != null) {
				bw.write(line);
				bw.newLine();
				line = br.readLine();
			}
			bw.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (fos != null) {
				try {fos.close();} catch (Exception ex) {}
			}
		}
	}
	
	private boolean load() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(datFolder + lang + ".txt");
			InputStreamReader isr = new InputStreamReader(fis, "UTF8");
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
				strList.put(key,  val);
				line = br.readLine();
			}
		} catch (Exception ex) {
			return false;
		} finally {
			if (fis != null) {
				try {fis.close();}
				catch (Exception ex) {}
			}
		}
		return true;
	}
	
	public void debug() {
		Set<String> keys = strList.keySet();
		for (String key : keys) {
			Stargate.debug("LangLoader::Debug", key + " => " + strList.get(key));
		}
	}
	
    private String removeUTF8BOM(String s) {
        if (s.startsWith(UTF8_BOM)) {
            s = s.substring(1);
        }
        return s;
    }
}
