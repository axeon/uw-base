package uw.common.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 日期工具类
 */
public class DateUtils {

    /**
     * 时区格式：ZZ
     */
    public static final String ZONE_OFFSET = "ZZ";

    /**
     * 时间格式：yyyy-MM-dd
     */
    public static final String DATE = "yyyy-MM-dd";

    /**
     * 时间格式：yyyyMMdd
     */
    public static final String DATE_SIMPLE = "yyyyMMdd";

    /**
     * 时间格式：yyyy/MM/dd
     */
    public static final String DATE_SLASH = "yyyy/MM/dd";

    /**
     * 月份格式：yyyy-MM
     */
    public static final String MONTH = "yyyy-MM";

    /**
     * 月份格式：yyyy/MM
     */
    public static final String MONTH_SLASH = "yyyy/MM";

    /**
     * 月份格式：yyyyMM
     */
    public static final String MONTH_SIMPLE = "yyyyMM";

    /**
     * 月份格式：yyMM
     */
    public static final String MONTH_SHORT = "yyMM";

    /**
     * 时间格式：HH:mm:ss
     */
    public static final String TIME = "HH:mm:ss";

    /**
     * 时间格式：yyyy-MM-dd HH:mm:ss
     */
    public static final String TIME_SIMPLE = "HHmmss";

    /**
     * 时间格式： HH:mm
     */
    public static final String TIME_MINUTE = "HH:mm";

    /**
     * 时间格式： HHmm
     */
    public static final String TIME_MINUTE_SIMPLE = "HHmm";

    /**
     * 时间格式：HH:mm:ss.SSS
     */
    public static final String TIME_MILLIS = "HH:mm:ss.SSS";

    /**
     * 时间格式：HHmmssSSS
     */
    public static final String TIME_MILLIS_SIMPLE = "HHmmssSSS";

    /**
     * 时间格式：yyyy-MM-dd HH:mm:ss
     */
    public static final String DATE_TIME = "yyyy-MM-dd HH:mm:ss";

    /**
     * 时间格式：yyyy-MM-dd HH:mm
     */
    public static final String DATE_MINUTE = "yyyy-MM-dd HH:mm";

    /**
     * 时间格式：yyyy-MM-dd HH:mm:ss.SSS
     */
    public static final String DATE_MILLIS = "yyyy-MM-dd HH:mm:ss.SSS";

    /**
     * 时间格式：yyyy-MM-dd'T'HH:mm:ss
     */
    public static final String DATE_TIME_T = "yyyy-MM-dd'T'HH:mm:ss";

    /**
     * 时间格式：yyyy-MM-dd'T'HH:mm
     */
    public static final String DATE_MINUTE_T = "yyyy-MM-dd'T'HH:mm";

    /**
     * 时间格式：yyyy-MM-dd'T'HH:mm:ss.SSS
     */
    public static final String DATE_MILLIS_T = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    /**
     * 时间格式：yyyyMMddHHmmss
     */
    public static final String DATE_TIME_SIMPLE = "yyyyMMddHHmmss";

    /**
     * 时间格式：yyyy-MM-dd HH:mm
     */
    public static final String DATE_MINUTE_SIMPLE = "yyyyMMddHHmm";

    /**
     * 时间格式：yyyyMMddHHmmssSSS
     */
    public static final String DATE_MILLIS_SIMPLE = "yyyyMMddHHmmssSSS";

    /**
     * 时间格式：yyyy/MM/dd HH:mm
     */
    public static final String DATE_MINUTE_SLASH = "yyyy/MM/dd HH:mm";

    /**
     * 时间格式：yyyy/MM/dd HH:mm:ss
     */
    public static final String DATE_TIME_SLASH = "yyyy/MM/dd HH:mm:ss";

    /**
     * 时间格式：yyyy/MM/dd HH:mm:ss.SSS
     */
    public static final String DATE_MILLIS_SLASH = "yyyy/MM/dd HH:mm:ss.SSS";

    /**
     * 时间格式：yyyy-MM-dd'T'HH:mm:ssZZ
     */
    public static final String DATE_TIME_ISO = "yyyy-MM-dd'T'HH:mm:ssZZ";

    /**
     * 时间格式：yyyy-MM-dd'T'HH:mmZZ
     */
    public static final String DATE_MINUTE_ISO = "yyyy-MM-dd'T'HH:mmZZ";

    /**
     * 时间格式：yyyy-MM-dd'T'HH:mm:ss.SSSZZ
     */
    public static final String DATE_MILLIS_ISO = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ";

    /**
     * 时间格式：yyyy-MM-dd'T'HH:mm:ssZ
     */
    public static final String DATE_TIME_ISO2 = "yyyy-MM-dd'T'HH:mm:ssZ";

    /**
     * 时间格式：yyyy-MM-dd'T'HH:mmZ
     */
    public static final String DATE_MINUTE_ISO2 = "yyyy-MM-dd'T'HH:mmZ";

    /**
     * 时间格式：yyyy-MM-dd'T'HH:mm:ss.SSSZ
     */
    public static final String DATE_MILLIS_ISO2 = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    /**
     * 默认时区
     */
    public static final ZoneId DEFAULT_ZONE_ID = TimeZone.getDefault().toZoneId();

    public static final Map<String, String> DATE_FORMAT_MAP = new LinkedHashMap<>() {
        {
            put(DATE, "DATE");
            put(DATE_SIMPLE, "DATE_SIMPLE");
            put(DATE_SLASH, "DATE_SLASH");
            put(MONTH, "MONTH");
            put(MONTH_SLASH, "MONTH_SLASH");
            put(MONTH_SIMPLE, "MONTH_SIMPLE");
            put(MONTH_SHORT, "MONTH_SHORT");
            put(TIME, "TIME");
            put(TIME_SIMPLE, "TIME_SIMPLE");
            put(TIME_MINUTE, "TIME_MINUTE");
            put(TIME_MINUTE_SIMPLE, "TIME_MINUTE_SIMPLE");
            put(TIME_MILLIS, "TIME_MILLIS");
            put(TIME_MILLIS_SIMPLE, "TIME_MILLIS_SIMPLE");
            put(DATE_TIME, "DATE_TIME");
            put(DATE_MINUTE, "DATE_MINUTE");
            put(DATE_MILLIS, "DATE_MILLIS");
            put(DATE_TIME_T, "DATE_TIME_T");
            put(DATE_MINUTE_T, "DATE_MINUTE_T");
            put(DATE_MILLIS_T, "DATE_MILLIS_T");
            put(DATE_TIME_SIMPLE, "DATE_TIME_SIMPLE");
            put(DATE_MINUTE_SIMPLE, "DATE_MINUTE_SIMPLE");
            put(DATE_MILLIS_SIMPLE, "DATE_MILLIS_SIMPLE");
            put(DATE_MINUTE_SLASH, "DATE_MINUTE_SLASH");
            put(DATE_TIME_SLASH, "DATE_TIME_SLASH");
            put(DATE_MILLIS_SLASH, "DATE_MILLIS_SLASH");
            put(DATE_TIME_ISO, "DATE_TIME_ISO");
            put(DATE_MINUTE_ISO, "DATE_MINUTE_ISO");
            put(DATE_MILLIS_ISO, "DATE_MILLIS_ISO");
            put(DATE_TIME_ISO2, "DATE_TIME_ISO2");
            put(DATE_MINUTE_ISO2, "DATE_MINUTE_ISO2");
            put(DATE_MILLIS_ISO2, "DATE_MILLIS_ISO2");
        }
    };

    private static final Logger logger = LoggerFactory.getLogger(DateUtils.class);


    //-------------------------------------------------------------------日期格式转换---------------------------------------------------------------

    /**
     * 将时间格式转换字符串
     * 例子：dateToString(new Date(),DateUtil.DATE_TIME);
     *
     * @param date   Date实例
     * @param format 时间格式
     * @return 2017-06-06
     */
    public static String dateToString(Date date, String format) {
        if (date == null) {
            return null;
        }
        FastDateFormat fastDateFormat = FastDateFormat.getInstance(format);
        return fastDateFormat.format(date);
    }

    /**
     * 将字符串转换为Date实例
     *
     * @param dateString 2016-10-12
     * @param format     时间格式
     * @return Date实例
     */
    public static Date stringToDate(String dateString, String format) {
        Date date = null;
        FastDateFormat fastDateFormat = FastDateFormat.getInstance(format);
        try {
            date = fastDateFormat.parse(dateString);
        } catch (ParseException e) {
            logger.error(e.getMessage(), e);
        }
        return date;
    }


    /**
     * 简化字符串日期恢复横杠。
     *
     * @param simpleDate 20170606
     * @return 2017-06-06
     */
    public static String simpleDateStringFormat(String simpleDate) {
        if (StringUtils.isNotBlank(simpleDate))
            return simpleDate.substring(0, 4) + "-" + simpleDate.substring(4, 6) + "-" + simpleDate.substring(6, 8);
        else return null;
    }

    /**
     * 字符串日期简化去除横杠。
     *
     * @param simpleDate 20170606
     * @return 2017-06-06
     */
    public static String dateStringSimplify(String simpleDate) {
        if (StringUtils.isNotBlank(simpleDate) && simpleDate.length() >= 10)
            return simpleDate.substring(0, 4) + simpleDate.substring(5, 7) + "-" + simpleDate.substring(8, 10);
        else return null;
    }

    /**
     * Date类型转LocalDate类型
     *
     * @param date
     * @param zoneId 时区id
     * @return
     */
    public static LocalDate dateToLocalDate(Date date, ZoneId zoneId) {
        Instant instant = date.toInstant();
        if (zoneId == null) {
            return dateToLocalDate(date);
        }
        return instant.atZone(zoneId).toLocalDate();
    }

    /**
     * Date类型转LocalDate类型
     *
     * @param date
     * @return
     */
    public static LocalDate dateToLocalDate(Date date) {
        Instant instant = date.toInstant();
        return instant.atZone(DEFAULT_ZONE_ID).toLocalDate();
    }


    /**
     * LocalDate类型转Date类型
     *
     * @param localDate
     * @param zoneId    时区id
     * @return Date
     */
    public static Date localDateToDate(LocalDate localDate, ZoneId zoneId) {
        if (zoneId == null) {
            return localDateToDate(localDate);
        }
        ZonedDateTime zonedDateTime = localDate.atStartOfDay(zoneId);
        return Date.from(zonedDateTime.toInstant());
    }


    /**
     * LocalDate类型转Date类型
     *
     * @param localDate
     * @return Date
     */
    public static Date localDateToDate(LocalDate localDate) {
        ZonedDateTime zonedDateTime = localDate.atStartOfDay(DEFAULT_ZONE_ID);
        return Date.from(zonedDateTime.toInstant());
    }


    //-------------------------------------------------------------------日期增加或者减少---------------------------------------------------------------

    /**
     * 偏移年。
     *
     * @param date   日期
     * @param offset 偏移小时数，正数向未来偏移，负数向历史偏移
     * @return 偏移后的日期
     */
    public static Date offsetYear(Date date, int offset) {
        return offset(date, Calendar.YEAR, offset);
    }

    /**
     * 偏移月。
     *
     * @param date   日期
     * @param offset 偏移天数，正数向未来偏移，负数向历史偏移
     * @return 偏移后的日期
     */
    public static Date offsetMonth(Date date, int offset) {
        return offset(date, Calendar.MONTH, offset);
    }

    /**
     * 偏移周。
     *
     * @param date   日期
     * @param offset 偏移天数，正数向未来偏移，负数向历史偏移
     * @return 偏移后的日期
     */
    public static Date offsetWeek(Date date, int offset) {
        return offset(date, Calendar.WEEK_OF_YEAR, offset);
    }

    /**
     * 偏移天。
     *
     * @param date   日期
     * @param offset 偏移天数，正数向未来偏移，负数向历史偏移
     * @return 偏移后的日期
     */
    public static Date offsetDay(Date date, int offset) {
        return offset(date, Calendar.DAY_OF_YEAR, offset);
    }


    /**
     * 偏移小时。
     *
     * @param date   日期
     * @param offset 偏移小时数，正数向未来偏移，负数向历史偏移
     * @return 偏移后的日期
     */
    public static Date offsetHour(Date date, int offset) {
        return offset(date, Calendar.HOUR_OF_DAY, offset);
    }

    /**
     * 偏移分钟。
     *
     * @param date   日期
     * @param offset 偏移天数，正数向未来偏移，负数向历史偏移
     * @return 偏移后的日期
     */
    public static Date offsetMinute(Date date, int offset) {
        return offset(date, Calendar.MINUTE, offset);
    }


    /**
     * 偏移秒。
     *
     * @param date   日期
     * @param offset 偏移天数，正数向未来偏移，负数向历史偏移
     * @return 偏移后的日期
     */
    public static Date offsetSecond(Date date, int offset) {
        return offset(date, Calendar.SECOND, offset);
    }


    /**
     * 获取指定日期偏移指定时间后的时间
     *
     * @param date      基准日期
     * @param dateField 偏移的粒度大小（小时、天、月等)
     * @param offset    偏移量，正数为向后偏移，负数为向前偏移
     * @return 偏移后的日期
     */
    public static Date offset(Date date, int dateField, int offset) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(dateField, offset);
        return calendar.getTime();
    }


    //-------------------------------------------------------------------判断两个时间关系---------------------------------------------------------------

    /**
     * 判断两个时间是不是在同一天
     *
     * @param day1 第一个日期
     * @param day2 第二个日期
     * @return
     */
    public static boolean isSameDay(Date day1, Date day2) {
        Calendar firstCal = Calendar.getInstance();
        Calendar secondCal = Calendar.getInstance();
        firstCal.setTime(day1);
        secondCal.setTime(day2);
        return (firstCal.get(Calendar.YEAR) == secondCal.get(Calendar.YEAR)) && (firstCal.get(Calendar.MONTH) == secondCal.get(Calendar.MONTH)) && (firstCal.get(Calendar.DAY_OF_MONTH) == secondCal.get(Calendar.DAY_OF_MONTH));
    }


    /**
     * 获取日期相距天数，默认返回0，
     *
     * @param startDate 较大时结果为负数
     * @param endDate   较大时结果为正数
     * @return 4
     */
    public static int getDaysBetweenDate(Date startDate, Date endDate) {
        return (int) (startDate.getTime() - endDate.getTime() + 1000) / (24 * 60 * 60 * 1000);
    }

    /**
     * 判断两个时间相差多少分钟
     *
     * @param firstDate
     * @param secondDate
     * @return
     */
    public static long minutesDiff(Date firstDate, Date secondDate) {
        return Math.abs(secondDate.getTime() - firstDate.getTime()) / (1000 * 60);
    }


    /**
     * 获取两个日期之间的所有日期
     *
     * @param startDate
     * @param endDate
     * @return
     */
    public static List<LocalDate> getDatesBetween(Date startDate, Date endDate) {
        List<LocalDate> dates = new ArrayList<>();

        LocalDate startLocalDate = dateToLocalDate(startDate);
        LocalDate endLocalDate = dateToLocalDate(endDate);
        LocalDate currentDate = startLocalDate;

        while (!currentDate.isAfter(endLocalDate)) {
            dates.add(currentDate);
            currentDate = currentDate.plusDays(1);
        }
        return dates;
    }


    //-------------------------------------------------------------------获取时间对应的信息---------------------------------------------------------------


    /**
     * 获取指定日期是这个日期所在月份的第几天
     *
     * @param date 日期
     * @return 天
     */
    public static int dayOfMonth(Date date) {
        // 使用 Calendar 获取日期信息
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        // 获取该日期是所在月份的第几天
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * 判断某日期是星期几，星期天为第7天
     *
     * @param date
     * @return
     */
    public static int getDayOfWeek(Date date) {
        // 使用 Calendar 获取日期信息
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        int dayInt = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        if (dayInt == 0) {
            dayInt = 7;
        }
        return dayInt;
    }

    /**
     * 获取n天对应的毫秒数
     *
     * @param day n天
     * @return
     */
    public static long getTimestampOfDay(int day) {
        return day * (24L * 60 * 60 * 1000);
    }


    /**
     * 给出一个日期的当年开年第一天时间。
     *
     * @param date
     * @return
     */
    public static Date beginOfYear(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR));
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * 给出一个日期的去年开年第一天时间。
     *
     * @param date
     * @return
     */
    public static Date beginOfLastYear(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) - 1);
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }


    /**
     * 给出一个日期的去年开年第一天时间。
     *
     * @param date
     * @return
     */
    public static Date beginOfNextYear(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }


    /**
     * 给出一个日期的当月第一天时间。
     *
     * @param date
     * @return
     */
    public static Date beginOfMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR));
        cal.set(Calendar.MONTH, cal.get(Calendar.MONTH));
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * 给出一个日期的上月第一天时间。
     *
     * @param date
     * @return
     */
    public static Date beginOfLastMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR));
        cal.set(Calendar.MONTH, cal.get(Calendar.MONTH) - 1);
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }


    /**
     * 给出一个日期的下月第一天时间。
     *
     * @param date
     * @return
     */
    public static Date beginOfNextMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR));
        cal.set(Calendar.MONTH, cal.get(Calendar.MONTH) + 1);
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * 给出当天开始时间。
     *
     * @param date
     * @return
     */
    public static Date beginOfToday(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR));
        cal.set(Calendar.MONTH, cal.get(Calendar.MONTH));
        cal.set(Calendar.DATE, cal.get(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * 给出昨天开始时间。
     *
     * @param date
     * @return
     */
    public static Date beginOfYesterday(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR));
        cal.set(Calendar.MONTH, cal.get(Calendar.MONTH));
        cal.set(Calendar.DATE, cal.get(Calendar.DAY_OF_MONTH) - 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }


    /**
     * 给出明天开始时间。
     *
     * @param date
     * @return
     */
    public static Date beginOfTomorrow(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR));
        cal.set(Calendar.MONTH, cal.get(Calendar.MONTH));
        cal.set(Calendar.DATE, cal.get(Calendar.DAY_OF_MONTH) + 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }


    /**
     * 给出一个日期的当年开年第一天时间。
     *
     * @param date
     * @return
     */
    public static Date endOfYear(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return new Date(cal.getTimeInMillis() - 1);
    }

    /**
     * 给出一个日期的去年开年第一天时间。
     *
     * @param date
     * @return
     */
    public static Date endOfLastYear(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR));
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return new Date(cal.getTimeInMillis() - 1);
    }


    /**
     * 给出一个日期的去年开年第一天时间。
     *
     * @param date
     * @return
     */
    public static Date endOfNextYear(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 2);
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return new Date(cal.getTimeInMillis() - 1);
    }


    /**
     * 给出一个日期的当月第一天时间。
     *
     * @param date
     * @return
     */
    public static Date endOfMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR));
        cal.set(Calendar.MONTH, cal.get(Calendar.MONTH) + 1);
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return new Date(cal.getTimeInMillis() - 1);
    }

    /**
     * 给出一个日期的上月第一天时间。
     *
     * @param date
     * @return
     */
    public static Date endOfLastMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR));
        cal.set(Calendar.MONTH, cal.get(Calendar.MONTH));
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return new Date(cal.getTimeInMillis() - 1);
    }


    /**
     * 给出一个日期的下月第一天时间。
     *
     * @param date
     * @return
     */
    public static Date endOfNextMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR));
        cal.set(Calendar.MONTH, cal.get(Calendar.MONTH) + 2);
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return new Date(cal.getTimeInMillis() - 1);
    }

    /**
     * 给出当天结束时间。
     *
     * @param date
     * @return
     */
    public static Date endOfToday(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR));
        cal.set(Calendar.MONTH, cal.get(Calendar.MONTH));
        cal.set(Calendar.DATE, cal.get(Calendar.DAY_OF_MONTH) + 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return new Date(cal.getTimeInMillis() - 1);
    }

    /**
     * 给出昨天结束时间。
     *
     * @param date
     * @return
     */
    public static Date endOfYesterday(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR));
        cal.set(Calendar.MONTH, cal.get(Calendar.MONTH));
        cal.set(Calendar.DATE, cal.get(Calendar.DAY_OF_MONTH));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return new Date(cal.getTimeInMillis() - 1);
    }


    /**
     * 给出明天结束时间。
     *
     * @param date
     * @return
     */
    public static Date endOfTomorrow(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR));
        cal.set(Calendar.MONTH, cal.get(Calendar.MONTH));
        cal.set(Calendar.DATE, cal.get(Calendar.DAY_OF_MONTH) + 2);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return new Date(cal.getTimeInMillis() - 1);
    }

    /**
     * 生成自动匹配的日期格式的代码。
     * 内部使用。
     *
     * @return
     */
    private static String genFitPatternCode() {
        Map<Integer, List<String>> map = DATE_FORMAT_MAP.keySet().stream().collect(Collectors.groupingBy(p -> {
            String t = p.replace("'T'", "T");
            if (t.contains("Z")) {
                return t.length() + 4;
            } else {
                return t.length();
            }
        }, Collectors.toList()));
        StringBuilder sb = new StringBuilder();
        sb.append("switch(dateString.length()) {\n");
        for (Map.Entry<Integer, List<String>> entry : map.entrySet()) {
            sb.append("\tcase ").append(entry.getKey()).append(" ->\n");
            sb.append("\t\t// ");
            List<String> formatList = entry.getValue();
            for (String format : formatList) {
                sb.append(DATE_FORMAT_MAP.get(format)).append("(");
                sb.append(format).append(") ");
            }
            sb.append("\n");
            sb.append("\t\t").append(DATE_FORMAT_MAP.get(formatList.getFirst())).append(";");
            sb.append("\n");
        }
        sb.append("\tdefault: \n\t\treturn null;\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * 将字符串转换为Date实例。
     * 将会自动的根据日期格式进行匹配。
     *
     * @param dateString
     * @return
     */
    public static Date stringToDate(String dateString) {
        if (dateString == null) {
            return null;
        }
        //  处理UTC时区的特殊情况
        if (dateString.endsWith("Z")) {
            dateString = dateString.substring(0, dateString.length() - 1) + "+00:00";
        }
        return switch (dateString.length()) {
            case 4 ->
                // TIME_MINUTE_SIMPLE(HHmm)
                    stringToDate(dateString, TIME_MINUTE_SIMPLE);
            case 5 ->
                // TIME_MINUTE(HH:mm)
                    stringToDate(dateString, TIME_MINUTE);
            case 6 ->
                // TIME_SIMPLE(HHmmss)
                    stringToDate(dateString, MONTH_SIMPLE);
            case 7 ->
                // MONTH(yyyy-MM) MONTH_SLASH(yyyy/MM)
                    dateString.contains("-") ? stringToDate(dateString, MONTH) : stringToDate(dateString, MONTH_SLASH);
            case 8 ->
                // DATE_SIMPLE(yyyyMMdd) TIME(HH:mm:ss)
                    dateString.contains(":") ? stringToDate(dateString, TIME) : stringToDate(dateString, DATE_SIMPLE);
            case 9 ->
                // TIME_MILLIS_SIMPLE(HHmmssSSS)
                    stringToDate(dateString, TIME_MILLIS_SIMPLE);
            case 10 -> {
                // DATE(yyyy-MM-dd) DATE_SLASH(yyyy/MM/dd)
                if (dateString.contains("-")) {
                    yield stringToDate(dateString, DATE);
                } else if (dateString.contains("/")) {
                    yield stringToDate(dateString, DATE_SLASH);
                } else {
                    long timestamp = 0;
                    try {
                        timestamp = Long.parseLong(dateString);
                    } catch (Exception e) {
                        //ignore
                    }
                    if (timestamp > 0) {
                        yield new Date(timestamp * 1000L);
                    } else {
                        yield null;
                    }
                }
            }
            case 12 ->
                // TIME_MILLIS(HH:mm:ss.SSS) DATE_MINUTE_SIMPLE(yyyyMMddHHmm)
                    dateString.contains(":") ? stringToDate(dateString, TIME_MILLIS) : stringToDate(dateString, DATE_MINUTE_SIMPLE);
            case 13 -> {
                //timestamp millis
                long timestamp = 0;
                try {
                    timestamp = Long.parseLong(dateString);
                } catch (Exception e) {
                    //ignore
                }
                if (timestamp > 0) {
                    yield new Date(timestamp);
                } else {
                    yield null;
                }
            }
            case 14 ->
                // DATE_TIME_SIMPLE(yyyyMMddHHmmss)
                    stringToDate(dateString, DATE_TIME_SIMPLE);
            case 16 ->
                // DATE_MINUTE(yyyy-MM-dd HH:mm) DATE_MINUTE_SLASH(yyyy/MM/dd HH:mm)
                    dateString.contains("T") ? stringToDate(dateString, DATE_MINUTE_T) : dateString.contains("-") ? stringToDate(dateString, DATE_MINUTE) : stringToDate(dateString, DATE_MINUTE_SLASH);
            case 17 ->
                // DATE_MILLIS_SIMPLE(yyyyMMddHHmmssSSS)
                    stringToDate(dateString, DATE_MILLIS_SIMPLE);
            case 19 ->
                // DATE_TIME(yyyy-MM-dd HH:mm:ss) DATE_TIME_SLASH(yyyy/MM/dd HH:mm:ss)
                    dateString.contains("T") ? stringToDate(dateString, DATE_TIME_T) : dateString.contains("-") ? stringToDate(dateString, DATE_TIME) : stringToDate(dateString, DATE_TIME_SLASH);
            case 21 ->
                // DATE_MINUTE_ISO2(yyyy-MM-dd'T'HH:mmZ)
                    stringToDate(dateString, DATE_MINUTE_ISO2);
            case 22 ->
                // DATE_MINUTE_ISO(yyyy-MM-dd'T'HH:mmZZ)
                    stringToDate(dateString, DATE_MINUTE_ISO);
            case 23 ->
                // DATE_MILLIS(yyyy-MM-dd HH:mm:ss.SSS) DATE_MILLIS_SLASH(yyyy/MM/dd HH:mm:ss.SSS)
                    dateString.contains("T") ? stringToDate(dateString, DATE_MILLIS_T) : dateString.contains("-") ? stringToDate(dateString, DATE_MILLIS) : stringToDate(dateString, DATE_MILLIS_SLASH);
            case 24 ->
                // DATE_TIME_ISO2(yyyy-MM-dd'T'HH:mm:ssZ)
                    stringToDate(dateString, DATE_TIME_ISO2);
            case 25 ->
                // DATE_TIME_ISO(yyyy-MM-dd'T'HH:mm:ssZZ)
                    stringToDate(dateString, DATE_TIME_ISO);
            case 28 ->
                // DATE_MILLIS_ISO2(yyyy-MM-dd'T'HH:mm:ss.SSSZ)
                    stringToDate(dateString, DATE_MILLIS_ISO2);
            case 29 ->
                // DATE_MILLIS_ISO(yyyy-MM-dd'T'HH:mm:ss.SSSZZ)
                    stringToDate(dateString, DATE_MILLIS_ISO);
            default -> null;
        };
    }

//    public static void main(String[] args) {
//
//        System.out.println(System.currentTimeMillis());
//        System.out.println(System.currentTimeMillis()/1000);
//        String[] testDates = {
//                "2023-10-05",
//                "20231005",
//                "2023/10/05",
//                "2023-10",
//                "2023/10",
//                "202310",
//                "2310",
//                "12:34:56",
//                "123456",
//                "12:34",
//                "1234",
//                "2023-10-05 12:34:56",
//                "2023-10-05 12:34",
//                "2023-10-05 12:34:56.789",
//                "20231005123456",
//                "202310051234",
//                "1748406880150",
//                "1748406880",
//                "20231005123456789",
//                "2023/10/05 12:34",
//                "2023/10/05 12:34:56",
//                "2023/10/05 12:34:56.789",
//                "2023-10-05T12:34:56+0800",
//                "2023-10-05T12:34+0800",
//                "2023-10-05T12:34:56.789+0800",
//                "2023-10-05T12:34:56Z",
//                "2023-10-05T12:34Z",
//                "2023-10-05T12:34:56.789Z"
//        };
//
//        for (String dateStr : testDates) {
//            java.util.Date parsedDate = stringToDate(dateStr);
//            if (parsedDate != null) {
//                System.out.println(dateStr + " -> " + parsedDate);
//            } else {
//                System.out.println(dateStr + " -> 解析失败");
//            }
//        }
//    }
}
