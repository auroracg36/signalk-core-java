package nz.co.fortytwo.signalk.util;
import static nz.co.fortytwo.signalk.util.SignalKConstants.source;
import static nz.co.fortytwo.signalk.util.SignalKConstants.sourceRef;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import mjson.Json;
import nz.co.fortytwo.signalk.model.SignalKModel;


public class JsonSerializer {

    private String indent=null;
    private StringBuilder curindent = new StringBuilder();
    private DecimalFormat[] df = new DecimalFormat[12]; // Up to 12dp for a number.
    
    private String [] arrayKeys = { 
    		//should derive this from schema really...
    		"config.server.security.config.ip",
    		"config.server.security.deny.ip",
    		"config.server.security.white.ip",
    		"config.server.client.mqtt.connect",
    		"config.server.client.tcp.connect",
    		"config.server.client.stomp.connect",
    		"config.server.serial.ports"
    	};
    /**
     * Set the ModelPrinter to pretty-print the output
     * The opposite of {@link #setCompact}
     * @param numspaces the number of spaces to indent each line, from 0..16
     */
    public void setPretty(int numspaces) {
        if (numspaces < 0 || numspaces > 16) {
            throw new IllegalArgumentException();
        }
        indent = "                ".substring(0, numspaces);
    }

    /**
     * Set the ModelPrinter to make the output as compact as possible.
     * The opposite of {@link #setPretty} and the default state.
     */
    public void setCompact() {
        indent = null;
    }

    /**
     * Return the number of decimal-places to use when printing the specified key (as a double)
     * The default is 8 for all keys.
     */
    public int getNumDecimalPlaces(String key) {
        return 8;
    }
    
    /**
     * Export the signalk model as a json string
     * @param signalk
     * @return
     * @throws IOException
     */
    public String write(SignalKModel signalk) throws IOException {
    	StringBuilder buffer = new StringBuilder();
    	if(signalk!=null && signalk.getFullData()!=null){
    		write(signalk.getFullData().entrySet().iterator(),'.',buffer);
    	}else{
    		buffer.append("{}");
    	}
		return buffer.toString();
	}

    /**
     * Write the values returned from the specified iterator to the specified output. If
     * no values are included then nothing is written, not even "{}"  and this method returns false.
     * A trailing newline is written only if the output is being pretty-printed.
     *
     * @param iterator the iterator containing the data - see class API docs for restrictions
     * @param separator how the keys are separated, eg '.' for keys like "a.b.c"
     * @param out the Appendable to write to
     * @return true if something was written to out
     */
    public boolean write(Iterator<Map.Entry<String,Object>> iterator, char separator, Appendable out) throws IOException {
        curindent.setLength(0);
        boolean begun = false;

        List<String> trail = new ArrayList<String>();       // The "breadcrumbs" of where we are in the tree
        boolean needcomma = false;
        String lastkey = null;

        while (iterator.hasNext()) {
            Map.Entry<String,Object> e = iterator.next();
            String key = e.getKey();
            Object value = e.getValue();
            if (!begun) {
                jsonBegin(out);
                begun = true;
            }
            String[] s = key.split("\\.");
            int l = s.length;
            if (lastkey != null && (key.compareTo(lastkey) <= 0 || (key.startsWith(lastkey) && key.charAt(lastkey.length()) == separator))) {
                throw new IllegalStateException("Key \""+key+"\" can't follow key \""+lastkey+"\"");
            }

            int j = 0;      // The number of tree entries in common with the previously output value
            while (l > j && trail.size() > j && trail.get(j).equals(s[j])) {
                j++;
            }
            while (trail.size() > j) {
                jsonClose(out);
                needcomma = true;
                trail.remove(trail.size() - 1);
            }
            if (needcomma) {
                needcomma = false;
                jsonComma(out);
            }
            while (l - 1 > j) {
                trail.add(jsonKey(s[j++], out));
                jsonBegin(out);
                needcomma = false;
            }

            if (needcomma) {
                needcomma = false;
                jsonComma(out);
            }
            jsonKey(s[j], out);
            if (value== null || "null".equals(value)) {
                jsonNull(key,out);
            } else if (value instanceof String) {
                jsonWrite((String)value, out);
            } else if (value instanceof Integer) {
                jsonWrite(((Integer)value).intValue(), out);
            } else if (value instanceof Number) {
                jsonWrite(s[j], ((Number)value).doubleValue(), out);
            } else if (value instanceof Boolean) {
                jsonWrite(((Boolean)value).booleanValue(), out);
            } else if (value instanceof Json && ((Json)value).isArray()) {
                out.append(((Json)value).toString());
            } else  {
                throw new IllegalStateException("Can't print value of type \""+value.getClass().getName()+"\" for key \""+key+"\"");
            }
            needcomma = true;
            lastkey = key;
        }
        while (!trail.isEmpty()) {
            jsonClose(out);
            trail.remove(trail.size() - 1);
        }
        if (begun) {
            jsonEnd(out);
        }
        return begun;
    }

    private void jsonWrite(String value, Appendable out) throws IOException {
    	if(value.startsWith("[") && value.endsWith("]")){
    		jsonWriteArray(value, out);
    		return;
    	}
        out.append('"');
        int len = value.length();
        char c = 0;
        for (int i=0; i<len; i++) {
            char b = c;
            c = value.charAt(i);
            switch (c) {
            case '\\':
            case '"':
                out.append('\\');
                out.append(c);
                break;
            case '/':
                if (b == '<') {
                    out.append('\\');
                }
                out.append(c);
                break;
            case '\b':
                out.append("\\b");
                break;
            case '\t':
                out.append("\\t");
                break;
            case '\n':
                out.append("\\n");
                break;
            case '\f':
                out.append("\\f");
                break;
            case '\r':
                out.append("\\r");
                break;
            default:
                if (c < 0x20 || (c >= 0x80 && c < 0xA0) || c == 0x2028 || c == 0x2029) {
                    String t = Integer.toHexString(c);
                    out.append("\\u");
                    switch(t.length()) {
                        case 1: out.append('0');
                        case 2: out.append('0');
                        case 3: out.append('0');
                    }
                    out.append(t);
                } else {
                    out.append(c);
                }
            }
        }
        out.append('"');
    }

    /**
     * Use the supplied schema to find if this is an array type, since we cant tell with null values
     * 
     * @param value
     * @return
     */
    private boolean isJsonArray(String key) {
		for(String k:arrayKeys){
			if(k.equals(key)) return true;
		}
		return false;
	}

	private void jsonWriteArray(String value, Appendable out) throws IOException {
    	if("[]".equals(value)){
    		out.append("[]");
    	}else{
    		out.append(value);
    	}
    }

	private void jsonWrite(int value, Appendable out) throws IOException {
        out.append(Integer.toString(value));
    }

    private void jsonNull(String key, Appendable out) throws IOException {
    	//if we have a null array key, we need to output []
    	if(isJsonArray(key)){
    		out.append("[]");
    	}else{
    		out.append("null");
    	}
    }

    private void jsonWrite(String key, double value, Appendable out) throws IOException {
        int dp = getNumDecimalPlaces(key);
        if (df[dp] == null) {
            //StringBuilder sb = new StringBuilder(dp + 2);
            //sb.append("#0.");
           // for (int i=0;i<dp;i++) {
           //     sb.append('0');
           // }
           // df[dp] = new DecimalFormat(sb.toString());
        	 //need to avoid locale decimal symbol
        	df[dp] = new DecimalFormat("0.0",DecimalFormatSymbols.getInstance(Locale.ENGLISH));
            df[dp].setMaximumFractionDigits(dp);
        }
        String v = df[dp].format(value);

        // Trim trailing '0' after decimal point for brevity.
        int j = v.length() - 1;
        while (j > 0 && v.charAt(j) == '0') {
            j--;
        }
        if (v.charAt(j) == '.') {
            j++;
        }
        out.append(v.substring(0, j + 1));
    }

    private void jsonWrite(boolean value, Appendable out) throws IOException {
        out.append(value ? "true" : "false");
    }

    private void jsonBegin(Appendable out) throws IOException {
        out.append('{');
        if (indent != null) {
            out.append('\n');
            curindent.append(indent);
            out.append(curindent);
        }
    }

    private void jsonEnd(Appendable out) throws IOException {
        if (indent != null) {
            out.append('\n');
        }
        out.append("}");
        if (indent != null) {
            out.append('\n');
        }
    }

    private String jsonKey(String key, Appendable out) throws IOException {
        jsonWrite(key, out);
        out.append(':');
        if (indent != null) {
            out.append(' ');
        }
        return key;
    }

    private void jsonClose(Appendable out) throws IOException {
        if (indent != null) {
            out.append('\n');
            curindent.setLength(curindent.length() - indent.length());
            out.append(curindent);
        }
        out.append("}");
    }

    private void jsonComma(Appendable out) throws IOException {
        out.append(',');
        if (indent != null) {
            out.append('\n');
            out.append(curindent);
        }
    }



    /**
     * Parse a JSON serialized String and return the Object it represents
     */
    public NavigableMap<String, Object> read(String s) throws IOException {
        return read(Json.read(s));
    }

    /**
     * Convert a json object to a flattened map of key/value pairs
     * 
     * @param json
     * @return NavigableMap useable in the signalk model
     */
    public NavigableMap<String, Object> read(Json json) {
		//get keys and recurse
    	ConcurrentSkipListMap<String, Object> map = new ConcurrentSkipListMap<String, Object>();
    	if(json.has(SignalKConstants.vessels) || json.has(SignalKConstants.CONFIG)){
    		recurseJsonFull(json,map,"");
    	}
    	if(json.has(SignalKConstants.UPDATES)){
    		parseJsonDelta(json,map,"");
    	}
		return map;
	}




	private void recurseJsonFull(Json json, ConcurrentSkipListMap<String, Object> map, String prefix) {
		for(Entry<String, Json> entry: json.asJsonMap().entrySet()){
			if(entry.getValue().isPrimitive()){
				map.put(prefix+entry.getKey(), entry.getValue().getValue());
			}else if(entry.getValue().isNull()){
				//we need to add null as string since null cant be used in a ConcurrentSkipList
				map.put(prefix+entry.getKey(), entry.getValue().toString());
				
			}else if(entry.getValue().isArray()){
				map.put(prefix+entry.getKey(), entry.getValue().toString());
			}else{
				recurseJsonFull(entry.getValue(), map, prefix+entry.getKey()+".");
			}
		}
		
	}
	
	private void parseJsonDelta(Json json, ConcurrentSkipListMap<String, Object> map, String prefix) {
		
		
	}




	/**
     * An object can implement this interface if it wants to control
     * how it's written to the JSON stream
     */
    public static interface JSONSerializable {
        public void write(Appendable a, boolean synchronize, StringBuilder eol) throws IOException;
    }




	public Json writeJson(SignalKModel model) throws IOException {
		if(model==null || model.getFullData().size()==0)return Json.object();
		return Json.read(write(model));
	}




	

    /*
    public static void main(String[] args) throws Exception {
        JSONSerializer json = new JSONSerializer();
        Object o = json.read(args[0]);
        String s = json.write(o);
        System.out.println(args[0]);
        System.out.println(o);
        System.out.println(s);
    }
    */
}

