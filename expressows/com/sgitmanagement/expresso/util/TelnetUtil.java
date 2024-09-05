package com.sgitmanagement.expresso.util;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.net.telnet.TelnetClient;

public class TelnetUtil {
	static public void main(String[] args) throws Exception {
		// telnet every 30 secondes to the load balancer
		{
			final int DELAY_SECONDS = 30;
			Timer t = new Timer();
			t.schedule(new TimerTask() {

				@Override
				public void run() {
					TelnetClient tc = new TelnetClient();
					Date startDate = new Date();
					try {
						tc.connect("10.2.13.85", 443);
						Date endDate = new Date();
						System.out.println(DateUtil.formatDate(startDate, DateUtil.DATETIME_FORMAT_TL.get()) + ": " + (endDate.getTime() - startDate.getTime()));
					} catch (Exception ex) {
						System.err.println(DateUtil.formatDate(startDate, DateUtil.DATETIME_FORMAT_TL.get()) + ": " + ex);
					} finally {
						try {
							tc.disconnect();
						} catch (Exception ex) {
							// ignore
						}
					}
					tc = null;
				}
			}, 0, DELAY_SECONDS * 1000);
		}
	}
}
