package com.sgitmanagement.expresso.util;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExpiringConcurrentHashMap<K, V> extends ConcurrentHashMap<K, V> {

	private static final long serialVersionUID = 1L;

	private Map<K, Long> timeMap = new ConcurrentHashMap<>();
	private long expiryInSeconds = 0;

	public ExpiringConcurrentHashMap(long expiryInSeconds) {
		this.expiryInSeconds = expiryInSeconds;

		// Initialize the cleaner thread
		new CleanerThread().start();
	}

	@Override
	public V put(K key, V value) {
		Date date = new Date();
		timeMap.put(key, date.getTime());
		V returnVal = super.put(key, value);
		return returnVal;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		for (K key : m.keySet()) {
			put(key, m.get(key));
		}
	}

	@Override
	public V putIfAbsent(K key, V value) {
		if (!containsKey(key)) {
			return put(key, value);
		} else {
			return get(key);
		}
	}

	class CleanerThread extends Thread {
		@Override
		public void run() {
			while (true) {
				cleanMap();
				try {
					// verify every minute
					Thread.sleep(60 * 1000);
				} catch (InterruptedException e) {
					// cannot do much
				}
			}
		}

		private void cleanMap() {
			long currentTime = new Date().getTime();
			for (K key : timeMap.keySet()) {
				if (currentTime > (timeMap.get(key) + (expiryInSeconds * 1000))) {
					timeMap.remove(key);
				}
			}
		}
	}
}