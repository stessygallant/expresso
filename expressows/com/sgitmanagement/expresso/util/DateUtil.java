package com.sgitmanagement.expresso.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.DateUtils;

public class DateUtil {
	private static final int MILLI_TO_HOUR = 1000 * 60 * 60;

	public static final ThreadLocal<DateFormat> DATE_FORMAT_TL = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd");
		}
	};
	public static final ThreadLocal<DateFormat> TIME_FORMAT_TL = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("HH:mm:ss");
		}
	};
	public static final ThreadLocal<DateFormat> TIME_NOSEC_FORMAT_TL = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("HH:mm");
		}
	};
	public static final ThreadLocal<DateFormat> US_DATE_FORMAT_TL = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("MM/dd/yy");
		}
	};
	public static final ThreadLocal<DateFormat> US_DATE_LONG_YEAR_FORMAT_TL = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("MM/dd/yyyy");
		}
	};
	public static final ThreadLocal<DateFormat> US_DATETIME_SHORT_FORMAT_TL = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("M/d/yyyy h:mm:ss a");
		}
	};
	public static final ThreadLocal<DateFormat> DATE_FORMAT_SHORT_YEAR_TL = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("MM-dd-yy");
		}
	};
	public static final ThreadLocal<DateFormat> DATETIME_FORMAT_TL = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		}
	};
	public static final ThreadLocal<DateFormat> DATETIME_NOSEC_FORMAT_TL = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm");
		}
	};
	public static final ThreadLocal<DateFormat> DATETIME_MMM_NOSEC_FORMAT_TL = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MMM-dd HH:mm");
		}
	};
	public static final ThreadLocal<DateFormat> DATETIME_NO_YEAR_MMM_NOSEC_FORMAT_TL = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("MMM-dd HH:mm");
		}
	};
	public static final ThreadLocal<DateFormat> DATE_MMM_FORMAT_TL = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MMM-dd");
		}
	};
	public static final ThreadLocal<DateFormat> DATETIMEZ_FORMAT_TL = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
		}
	};
	public static final ThreadLocal<DateFormat> DATETIMEZ2_FORMAT_TL = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		}
	};
	public static final ThreadLocal<DateFormat> DATETIMETZ_FORMAT_TL = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
		}
	};
	public static final ThreadLocal<DateFormat> DATETIME_JSON_FORMAT_TL = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		}
	};
	public static final ThreadLocal<DateFormat> DATETIMETz_FORMAT_TL = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		}
	};

	// Thu Dec 22 00:00:00 EST 2016
	public static final ThreadLocal<DateFormat> DATETIME_TEXT_FORMAT_TL = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("EEE MMM HH:mm:ss zzz yyyy");
		}
	};

	public static final ThreadLocal<DateFormat> DATETIME_FILE_FORMAT = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS");
		}
	};

	public static final ThreadLocal<DateFormat> DATETIME_LIMS_FORMAT = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyyMMdd HHmmss");
		}
	};

	public static final ThreadLocal<DateFormat> DATETIME_LIMS_FILE_FORMAT = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyyMMddHHmmss");
		}
	};

	public static Date parseDate(Object d) {
		if (d == null) {
			return null;
		} else if (d instanceof Date) {
			return (Date) d;
		} else if (d instanceof String && ((String) d).trim().length() == 0) {
			return null;
		} else {
			Date date = null;
			String dateString = ((String) d).trim();
			try {
				if (dateString.length() == 0) {
					// cannot convert
				} else if (dateString.contains("AM") || dateString.contains("PM")) {
					date = US_DATETIME_SHORT_FORMAT_TL.get().parse(dateString);
				} else if (dateString.length() == 8 && dateString.contains("/")) {
					date = US_DATE_FORMAT_TL.get().parse(dateString);
				} else if (dateString.length() == 5 && dateString.contains(":")) {
					date = TIME_NOSEC_FORMAT_TL.get().parse(dateString);
				} else if (dateString.length() == 8 && dateString.contains(":")) {
					date = TIME_FORMAT_TL.get().parse(dateString);
				} else if (dateString.length() == 8 && dateString.contains("-")) {
					date = DATE_FORMAT_SHORT_YEAR_TL.get().parse(dateString);
				} else if (dateString.length() == 10 && dateString.contains("-")) {
					date = DATE_FORMAT_TL.get().parse(dateString);
				} else if (dateString.length() == 10 && dateString.contains("/")) {
					date = US_DATE_LONG_YEAR_FORMAT_TL.get().parse(dateString);
				} else if (dateString.length() == 11) {
					date = DATE_MMM_FORMAT_TL.get().parse(dateString);
				} else if (dateString.length() == 19) {
					if (dateString.contains("T")) {
						date = DATETIMETz_FORMAT_TL.get().parse(dateString);
					} else {
						date = DATETIME_FORMAT_TL.get().parse(dateString);
					}
				} else if (dateString.length() == 20) {
					date = DATETIMETZ_FORMAT_TL.get().parse(dateString);
				} else if (dateString.length() == 16) {
					date = DATETIME_NOSEC_FORMAT_TL.get().parse(dateString);
				} else if (dateString.length() == 17) {
					date = DATETIME_MMM_NOSEC_FORMAT_TL.get().parse(dateString);
				} else if (dateString.length() == 23) {
					date = DATETIMEZ2_FORMAT_TL.get().parse(dateString);
				} else if (dateString.length() > 22) {
					if (dateString.contains(".")) {
						date = DATETIMEZ_FORMAT_TL.get().parse(dateString);
					} else if (dateString.substring(20).contains(":")) {
						date = DATETIMETz_FORMAT_TL.get().parse(dateString);
					} else if (Character.isLetter(dateString.charAt(0))) {
						date = DATETIME_TEXT_FORMAT_TL.get().parse(dateString);
					} else {
						date = DATETIMETZ_FORMAT_TL.get().parse(dateString);
					}
				} else {
					throw new Exception("Date format not supported");
				}
			} catch (Exception e) {
				System.err.println("Error parsing date [" + dateString + "]: " + e);
			}
			// System.out.println("[" + dateString + "] -> [" + date + "]");
			return date;
		}
	}

	static public String formatDate(Date date, DateFormat dateFormat) {
		try {
			if (date == null) {
				return null;
			} else {
				return dateFormat.format(date);
			}
		} catch (Exception e) {
			System.err.println("Error formatting :" + e);
			return null;
		}
	}

	static public String formatDate(Date date) {
		try {
			if (date == null) {
				return null;
			} else {
				return DATE_FORMAT_TL.get().format(date);
			}
		} catch (Exception e) {
			System.err.println("Error formatting :" + e);
			return null;
		}
	}

	static public String formatDateTime(Date date) {
		try {
			if (date == null) {
				return null;
			} else {
				return DATETIME_JSON_FORMAT_TL.get().format(date);
			}
		} catch (Exception e) {
			System.err.println("Error formatting :" + e);
			return null;
		}
	}

	static public Date addDays(Date date, int days) {
		return DateUtils.addDays(date, days);
	}

	static public Date addYears(Date date, int years) {
		return DateUtils.addYears(date, years);
	}

	static public Date addWorkingDays(Date date, int days) {
		Calendar calendar = addWorkingDays(DateUtils.toCalendar(date), days);
		return calendar.getTime();
	}

	static public Calendar addWorkingDays(Calendar calendar, int days) {
		int count = 0;
		while (count < days) {
			calendar.add(Calendar.DAY_OF_YEAR, 1);
			if (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY && calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
				count++;
			}
		}
		return calendar;
	}

	static public Date addHours(Date date, int hours) {
		return DateUtils.addHours(date, hours);
	}

	static public Date addMinutes(Date date, int minutes) {
		return DateUtils.addMinutes(date, minutes);
	}

	/**
	 * Verify if the date is between fromDate and toDate (inclusively). Use DateUtils.truncate(date, Calendar.DATE) to truncate the dates before calling this method if needed
	 *
	 * @param date     date
	 * @param fromDate date
	 * @param toDate   date
	 * @return
	 */
	static public boolean between(Date date, Date fromDate, Date toDate) {
		if (date != null && fromDate != null && toDate != null) {
			return date.getTime() >= fromDate.getTime() && date.getTime() <= toDate.getTime();
		} else {
			return false;
		}
	}

	/**
	 * Get a diff between two dates
	 *
	 * @param date1    the oldest date
	 * @param date2    the newest date
	 * @param timeUnit the unit in which you want the diff
	 * @return the diff value, in the provided unit
	 */
	static public long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
		if (date2 != null && date1 != null) {

			date1 = new Date(date1.getTime());
			date2 = new Date(date2.getTime());

			Duration duration = Duration.between(date1.toInstant(), date2.toInstant());
			switch (timeUnit) {
			case DAYS:
				// long diffInMillies = Math.abs(date2.getTime() - date1.getTime());
				// long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
				// return diff;
				// long between = ChronoUnit.DAYS.between(date1.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(), date2.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
				return LocalDate.ofInstant(date1.toInstant(), ZoneId.systemDefault()).until(LocalDate.ofInstant(date2.toInstant(), ZoneId.systemDefault()), ChronoUnit.DAYS);
			case HOURS:
				return duration.toHours();
			case MINUTES:
				return duration.toMinutes();
			case SECONDS:
				return duration.getSeconds();
			case MILLISECONDS:
				return duration.toMillis();
			default:
				throw new RuntimeException("Unsupported TimeUnit : " + timeUnit);
			}
		} else {
			return 0;
		}
	}

	/**
	 * Date only (time is 00:00:00.000)
	 *
	 * @return
	 */
	static public Date newDate() {
		Calendar calendar = Calendar.getInstance();

		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);

		return calendar.getTime();
	}

	static public Date addTime(Date date, Date time) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);

		Calendar calTime = Calendar.getInstance();
		calTime.setTime(time);

		calendar.set(Calendar.HOUR_OF_DAY, calTime.get(Calendar.HOUR_OF_DAY));
		calendar.set(Calendar.MINUTE, calTime.get(Calendar.MINUTE));
		calendar.set(Calendar.SECOND, calTime.get(Calendar.SECOND));
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}

	static public int getTime(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int min = calendar.get(Calendar.MINUTE);
		int sec = calendar.get(Calendar.SECOND);
		return hour * 60 * 60 + min * 60 + sec;
	}

	static public Date removeTime(Date date) {
		if (date != null) {
			return DateUtils.truncate(date, Calendar.DATE);
		} else {
			return null;
		}
	}

	/**
	 *
	 * @param startTime
	 * @param endTime
	 */
	static public float getHoursBetweenTime(String startTime, String endTime) {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(startTime.substring(0, 2)));
		cal.set(Calendar.MINUTE, Integer.parseInt(startTime.substring(3)));
		Date startDate = DateUtils.truncate(cal.getTime(), Calendar.MINUTE);

		cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(endTime.substring(0, 2)));
		cal.set(Calendar.MINUTE, Integer.parseInt(endTime.substring(3)));
		Date endDate = DateUtils.truncate(cal.getTime(), Calendar.MINUTE);

		if (endDate.before(startDate)) {
			endDate = DateUtils.addDays(endDate, 1);
		}

		return (float) (endDate.getTime() - startDate.getTime()) / MILLI_TO_HOUR;
	}

	/**
	 * Returns the maximum of two dates. A null date is treated as being less than any non-null date.
	 */
	public static Date max(Date d1, Date d2) {
		if (d1 == null && d2 == null) {
			return null;
		}
		if (d1 == null) {
			return d2;
		}
		if (d2 == null) {
			return d1;
		}
		return (d1.after(d2)) ? d1 : d2;
	}

	/**
	 * Returns the minimum of two dates. A null date is treated as being greater than any non-null date.
	 */
	public static Date min(Date d1, Date d2) {
		if (d1 == null && d2 == null) {
			return null;
		}
		if (d1 == null) {
			return d2;
		}
		if (d2 == null) {
			return d1;
		}
		return (d1.before(d2)) ? d1 : d2;
	}

	static public void main(String[] args) {
		// System.out.println(purgeInvalidCharacters("éàçïôèÉ"));

		// String s = "a,b,\"c,d\"";
		// String[] lines = Util.splitAvoidQuotes(s, ',', true);
		// for (int i = 0; i < lines.length; i++) {
		// System.out.println(lines[i]);
		// }

		// String s = "2018-10-22T14:02:27.000Z";
		// s = "2019-11-01T04:00:00.000Z";
		//
		// Date d = DateUtil.parseDate(s);
		// System.out.println(d);

		// System.out.println(getHoursBetweenTime("18:00", "06:00"));

		System.out.println(DateUtil.parseDate("2021-09-19T14:03:47Z"));
	}
}
