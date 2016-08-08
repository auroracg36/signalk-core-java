/*
 *
 * Copyright (C) 2012-2014 R T Huitema. All Rights Reserved.
 * Web: www.42.co.nz
 * Email: robert@42.co.nz
 * Author: R T Huitema
 *
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
 * WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package nz.co.fortytwo.signalk.model;

import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.SortedMap;

import com.google.common.eventbus.EventBus;

public interface SignalKModel{
		
	/**
	 * Get a val from the Model
	 */
	public abstract Object get(String key);

	/**
	 * Return a subtree from the Model - the tree is read-only, but is live
	 * and will be updated as the Model changes.
	 * @param key the key to retrieve - for example, "vessels.self" will return
	 * a subtree that may contain keys beginning "navigation", "environment" and so on.
	 */
	public abstract NavigableSet<String> getTree(String key);

	/**
	 * Get the common event bus instance.
	 */
	public abstract EventBus getEventBus();

	/**
	 * Return the full set of keys from this Model. The returned set
	 * is read-only and guaranteed to be the full set at the time this method is called,
	 * but as the model may be updated in another thread immediately after it's accuracy
	 * is no longer guaranteed after that. It can be iterated over without synchronization.
	 */
	public abstract NavigableSet<String> getKeys();

	/**
	 * Return the underlying data model. Note this is the live object,
	 * and should not be read without a {@link #readLock} being acquired, or
	 * modified at all.
	 */
	public abstract SortedMap<String, Object> getData();

	/**
	 * Add all values into the signalk model
	 * This method converts source to sourceRef, saving the actual source in sources.*
	 * 
	 * @param map
	 * @return
	 */
	public boolean putAll(SortedMap<String, Object> map);
	
	
	/**
	 * Generic put that accepts only null, boolean, Number, String and jsonArray
	 * @param key
	 * @param value
	 * @return
	 * @throws IllegalArgumentException
	 */
	//public boolean put(String key, Object val) throws IllegalArgumentException;
	
	public boolean put(String key, Object val, String source) throws IllegalArgumentException;
	public boolean put(String key, Object val, String source, String timestamp) throws IllegalArgumentException;

	/**
	 * Return a submap from the Model - the tree is read-only, but is live
	 * and will be updated as the Model changes.
	 * @param key the key to retrieve - for example, "vessels.self" will return
	 * a submap that may contain keys beginning "navigation", "environment" and so on.
	 */
	 
	public NavigableMap<String, Object> getSubMap(String key);

	/**
	 * Adds the '.value' suffic and returns the value. It will be boolean, number, string, jsonArray or null
	 * @param key
	 * @return
	 */
	public Object getValue(String key);

	/**
	 * Same as put, but it adds the suffix '.value' to the key
	 * @param string
	 * @param val
	 * @return
	 */
	public boolean putValue(String string, Object val);

	/**
	 * Gets the full data map. Use with care, it holds the config data too.
	 * @return
	 */
	SortedMap<String, Object> getFullData();

	/**Get the multiple values object 'values' or null if it doesnt exist
	 * @param string
	 * @return
	 */
	public abstract NavigableMap<String, Object> getValues(String string);

	/**
	 * Convenience method to set a position object
	 * @param string
	 * @param latitude
	 * @param longitude
	 * @param altitude
	 * @param srcRef
	 * @param ts
	 */
	public abstract void putPosition(String string, double latitude,
			double longitude, double d, String srcRef, String ts);

	/**
	 * Puts an entry in the sources structure.
	 * @param key
	 * @param val
	 * @param ts
	 * @return
	 * @throws IllegalArgumentException
	 */
	boolean putSource(String key, Object val, String ts) throws IllegalArgumentException;
	
	/**
	 * Saves an attr for a key entry.
	 * @param key
	 * @param val
	 * @param ts
	 * @return
	 * @throws IllegalArgumentException
	 */
	Attr putAttr(String key, Attr attr) throws IllegalArgumentException;

	/**
	 * Gets an attr for a key entry. Will recurse back to the first parent that has an attr.
	 * @param key
	 * @param val
	 * @param ts
	 * @return
	 * @throws IllegalArgumentException
	 */
	Attr  getAttr(String key) throws IllegalArgumentException;

	/**
	 * Removes the attr for this key. If recursive is true all subkeys are also removed
	 * @param key
	 * @param recursive
	 */
	void clearAttr(String key, boolean recursive);
	

}
