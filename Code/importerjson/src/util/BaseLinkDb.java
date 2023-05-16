/**
 * Temporal link database access management.
 * 
 * Copyright 2017-2020 ICTU
 * Copyright 2017-2022 Leiden University
 * Copyright 2017-2023 Leon Helwerda
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
        public enum State { MISSING, DIFFERS, EXISTS }
        public final State state;
        public final LinkDates dates;
        
        public CheckResult() {
            this.state = State.MISSING;
            this.dates = null;
        }
        
        public CheckResult(State state, LinkDates dates) {
            this.state = state;
            this.dates = dates;
        }
    }
    
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
        CheckResult.State state;
        if (compareTimestamps(dates.start_date, current_dates.start_date, true) &&
                compareTimestamps(dates.end_date, current_dates.end_date, false)) {
            state = CheckResult.State.EXISTS;
        }
        else {
            state = CheckResult.State.DIFFERS;
        }
        return new CheckResult(state, current_dates);
    }
}
