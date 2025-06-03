package uw.dao.util;

import java.util.Calendar;
import java.util.Date;

/**
 * 基于日期的分表规则工具类。
 * 在按年分表情况下，如果在回调中操作数据，可能刚好卡在分年门槛上。
 * 此时需要做一个简单回退来处理一下。
 */
public class ShardingDateUtils {

//    public static void main(String[] args) {
//        Date now = SystemClock.nowDate();
//        System.out.println(beginOfYear( now ));
//        System.out.println(endOfYear( now ));
//        System.out.println(beginOfLastYear( now ));
//        System.out.println(endOfLastYear( now ));
//        System.out.println(beginOfNextYear( now ));
//        System.out.println(endOfNextYear( now ));
//
//        System.out.println(beginOfMonth( now ));
//        System.out.println(endOfMonth( now ));
//        System.out.println(beginOfLastMonth( now ));
//        System.out.println(endOfLastMonth( now ));
//        System.out.println(beginOfNextMonth( now ));
//        System.out.println(endOfNextMonth( now ));
//
//        System.out.println(beginOfToday( now ));
//        System.out.println(endOfToday( now ));
//        System.out.println(beginOfYesterday( now ));
//        System.out.println(endOfYesterday( now ));
//        System.out.println(beginOfTomorrow( now ));
//        System.out.println(endOfTomorrow( now ));
//    }

    /**
     * 给出一个日期的当年开年第一天时间。
     *
     * @param date
     * @return
     */
    public static Date beginOfYear(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime( date );
        cal.set(Calendar.YEAR, cal.get( Calendar.YEAR ));
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set( Calendar.MILLISECOND,0 );
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
        cal.setTime( date );
        cal.set(Calendar.YEAR, cal.get( Calendar.YEAR )-1);
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set( Calendar.MILLISECOND,0 );
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
        cal.setTime( date );
        cal.set(Calendar.YEAR, cal.get( Calendar.YEAR )+1);
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set( Calendar.MILLISECOND,0 );
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
        cal.setTime( date );
        cal.set(Calendar.YEAR, cal.get( Calendar.YEAR ));
        cal.set(Calendar.MONTH, cal.get( Calendar.MONTH ));
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set( Calendar.MILLISECOND,0 );
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
        cal.setTime( date );
        cal.set(Calendar.YEAR, cal.get( Calendar.YEAR ));
        cal.set(Calendar.MONTH, cal.get( Calendar.MONTH )-1);
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set( Calendar.MILLISECOND,0 );
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
        cal.setTime( date );
        cal.set(Calendar.YEAR, cal.get( Calendar.YEAR ));
        cal.set(Calendar.MONTH, cal.get( Calendar.MONTH )+1);
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set( Calendar.MILLISECOND,0 );
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
        cal.setTime( date );
        cal.set(Calendar.YEAR, cal.get( Calendar.YEAR ));
        cal.set(Calendar.MONTH, cal.get( Calendar.MONTH ));
        cal.set(Calendar.DATE, cal.get( Calendar.DAY_OF_MONTH ));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set( Calendar.MILLISECOND,0 );
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
        cal.setTime( date );
        cal.set(Calendar.YEAR, cal.get( Calendar.YEAR ));
        cal.set(Calendar.MONTH, cal.get( Calendar.MONTH ));
        cal.set(Calendar.DATE, cal.get( Calendar.DAY_OF_MONTH )-1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set( Calendar.MILLISECOND,0 );
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
        cal.setTime( date );
        cal.set(Calendar.YEAR, cal.get( Calendar.YEAR ));
        cal.set(Calendar.MONTH, cal.get( Calendar.MONTH ));
        cal.set(Calendar.DATE, cal.get( Calendar.DAY_OF_MONTH )+1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set( Calendar.MILLISECOND,0 );
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
        cal.setTime( date );
        cal.set(Calendar.YEAR, cal.get( Calendar.YEAR )+1);
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set( Calendar.MILLISECOND,0 );
        return new Date(cal.getTimeInMillis()-1);
    }

    /**
     * 给出一个日期的去年开年第一天时间。
     *
     * @param date
     * @return
     */
    public static Date endOfLastYear(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime( date );
        cal.set(Calendar.YEAR, cal.get( Calendar.YEAR ));
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set( Calendar.MILLISECOND,0 );
        return new Date(cal.getTimeInMillis()-1);
    }


    /**
     * 给出一个日期的去年开年第一天时间。
     *
     * @param date
     * @return
     */
    public static Date endOfNextYear(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime( date );
        cal.set(Calendar.YEAR, cal.get( Calendar.YEAR )+2);
        cal.set(Calendar.MONTH, 0);
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set( Calendar.MILLISECOND,0 );
        return new Date(cal.getTimeInMillis()-1);
    }


    /**
     * 给出一个日期的当月第一天时间。
     *
     * @param date
     * @return
     */
    public static Date endOfMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime( date );
        cal.set(Calendar.YEAR, cal.get( Calendar.YEAR ));
        cal.set(Calendar.MONTH, cal.get( Calendar.MONTH )+1);
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set( Calendar.MILLISECOND,0 );
        return new Date(cal.getTimeInMillis()-1);
    }

    /**
     * 给出一个日期的上月第一天时间。
     *
     * @param date
     * @return
     */
    public static Date endOfLastMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime( date );
        cal.set(Calendar.YEAR, cal.get( Calendar.YEAR ));
        cal.set(Calendar.MONTH, cal.get( Calendar.MONTH ));
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set( Calendar.MILLISECOND,0 );
        return new Date(cal.getTimeInMillis()-1);
    }


    /**
     * 给出一个日期的下月第一天时间。
     *
     * @param date
     * @return
     */
    public static Date endOfNextMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime( date );
        cal.set(Calendar.YEAR, cal.get( Calendar.YEAR ));
        cal.set(Calendar.MONTH, cal.get( Calendar.MONTH )+2);
        cal.set(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set( Calendar.MILLISECOND,0 );
        return new Date(cal.getTimeInMillis()-1);
    }

    /**
     * 给出当天结束时间。
     *
     * @param date
     * @return
     */
    public static Date endOfToday(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime( date );
        cal.set(Calendar.YEAR, cal.get( Calendar.YEAR ));
        cal.set(Calendar.MONTH, cal.get( Calendar.MONTH ));
        cal.set(Calendar.DATE, cal.get( Calendar.DAY_OF_MONTH )+1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set( Calendar.MILLISECOND,0 );
        return new Date(cal.getTimeInMillis()-1);
    }

    /**
     * 给出昨天结束时间。
     *
     * @param date
     * @return
     */
    public static Date endOfYesterday(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime( date );
        cal.set(Calendar.YEAR, cal.get( Calendar.YEAR ));
        cal.set(Calendar.MONTH, cal.get( Calendar.MONTH ));
        cal.set(Calendar.DATE, cal.get( Calendar.DAY_OF_MONTH ));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set( Calendar.MILLISECOND,0 );
        return new Date(cal.getTimeInMillis()-1);
    }


    /**
     * 给出明天结束时间。
     *
     * @param date
     * @return
     */
    public static Date endOfTomorrow(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime( date );
        cal.set(Calendar.YEAR, cal.get( Calendar.YEAR ));
        cal.set(Calendar.MONTH, cal.get( Calendar.MONTH ));
        cal.set(Calendar.DATE, cal.get( Calendar.DAY_OF_MONTH )+2);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set( Calendar.MILLISECOND,0 );
        return new Date(cal.getTimeInMillis()-1);
    }



}
