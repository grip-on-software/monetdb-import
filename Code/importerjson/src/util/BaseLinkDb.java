/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import java.sql.Timestamp;
import java.util.Objects;

/**
 * Base class for database access management using a table that contains temporal
 * data via a date range.
 * @author Leon Helwerda
 */
public class BaseLinkDb extends BaseDb {
    /**
     * Object containing temporal information related to a link relationship.
     */
    public static class LinkDates {
        /**
         * Timestamp since which the link exists, or null if the start date is unknown.
         */
        public final Timestamp start_date;
        /**
         * Timestamp at which the link ceases to exist due to a change in the issues,
         * or null if the link still exists.
         */
        public final Timestamp end_date;
        
        public LinkDates() {
            this.start_date = null;
            this.end_date = null;
        }
        
        public LinkDates(Timestamp start_date, Timestamp end_date) {
            this.start_date = start_date;
            this.end_date = end_date;
        }
        
        @Override
        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            if (other instanceof LinkDates) {
                LinkDates otherDates = (LinkDates) other;
                return ((start_date == null ? otherDates.start_date == null : start_date.equals(otherDates.start_date)) &&
                        (end_date == null ? otherDates.end_date == null : end_date.equals(otherDates.end_date)));
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 53 * hash + Objects.hashCode(this.start_date);
            hash = 53 * hash + Objects.hashCode(this.end_date);
            return hash;
        }
    }
    
    public static class CheckResult {
        public enum State { MISSING, DIFFERS, EXISTS };
        public State state = State.MISSING;
        public LinkDates dates = null;
    };
    
    protected boolean compareTimestamps(Timestamp date, Timestamp current_date, boolean allowEarlier) {
        if (date == null) {
            return (current_date == null);
        }
        if (allowEarlier && current_date != null && date.after(current_date)) {
            return true;
        }
        return date.equals(current_date);
    }

    protected CheckResult compareLinkDates(LinkDates dates, LinkDates current_dates) {
        CheckResult result = new CheckResult();
        result.dates = current_dates;
        if (compareTimestamps(dates.start_date, current_dates.start_date, true) &&
                compareTimestamps(dates.end_date, current_dates.end_date, false)) {
            result.state = CheckResult.State.EXISTS;
        }
        else {
            result.state = CheckResult.State.DIFFERS;
        }
        return result;
    }
}
