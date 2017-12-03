/*
 * This code is copied from the QuickStart examples from the Reuters Developer Community.
 */
package com.friggsoft.rfa.util

import com.reuters.rfa.omm.OMMFilterEntry
import com.reuters.rfa.omm.OMMFilterList
import com.reuters.rfa.omm.OMMMap
import com.reuters.rfa.omm.OMMMapEntry
import com.reuters.rfa.omm.OMMVector
import com.reuters.rfa.omm.OMMVectorEntry

final class ExampleUtil {

    static String mapFlagsString(OMMMap data) {
        StringBuilder buf = new StringBuilder(60)

        if (data.has(OMMMap.HAS_DATA_DEFINITIONS)) {
            buf.append("HAS_DATA_DEFINITIONS")
        }

        if (data.has(OMMMap.HAS_SUMMARY_DATA)) {
            if (buf.length() != 0)
                buf.append(" | ")

            buf.append("HAS_SUMMARY_DATA")
        }

        if (data.has(OMMMap.HAS_PERMISSION_DATA_PER_ENTRY)) {
            if (buf.length() != 0)
                buf.append(" | ")

            buf.append("HAS_PERMISSION_DATA_PER_ENTRY")
        }

        if (data.has(OMMMap.HAS_TOTAL_COUNT_HINT)) {
            if (buf.length() != 0)
                buf.append(" | ")

            buf.append("HAS_TOTAL_COUNT_HINT")
        }

        if (data.has(OMMMap.HAS_KEY_FIELD_ID)) {
            if (buf.length() != 0)
                buf.append(" | ")

            buf.append("HAS_KEY_FIELD_ID")
        }
        return buf.toString()
    }

    static String mapEntryFlagsString(OMMMapEntry data) {
        StringBuilder buf = new StringBuilder(60)

        if (data.has(OMMMapEntry.HAS_PERMISSION_DATA)) {
            buf.append("HAS_PERMISSION_DATA")
        }
        return buf.toString()
    }

    static String vectorFlagsString(OMMVector data) {
        StringBuilder buf = new StringBuilder(60)

        if (data.has(OMMVector.HAS_DATA_DEFINITIONS)) {
            buf.append("HAS_DATA_DEFINITIONS")
        }

        if (data.has(OMMVector.HAS_SUMMARY_DATA)) {
            if (buf.length() != 0)
                buf.append(" | ")

            buf.append("HAS_SUMMARY_DATA")
        }

        if (data.has(OMMVector.HAS_PERMISSION_DATA_PER_ENTRY)) {
            if (buf.length() != 0)
                buf.append(" | ")

            buf.append("HAS_PERMISSION_DATA_PER_ENTRY")
        }

        if (data.has(OMMVector.HAS_TOTAL_COUNT_HINT)) {
            if (buf.length() != 0)
                buf.append(" | ")

            buf.append("HAS_TOTAL_COUNT_HINT")
        }

        if (data.has(OMMVector.HAS_SORT_ACTIONS)) {
            if (buf.length() != 0)
                buf.append(" | ")

            buf.append("HAS_SORT_ACTIONS")
        }
        return buf.toString()
    }

    static String filterListFlagsString(OMMFilterList data) {
        StringBuilder buf = new StringBuilder(60)

        if (data.has(OMMFilterList.HAS_PERMISSION_DATA_PER_ENTRY)) {
            buf.append("HAS_PERMISSION_DATA_PER_ENTRY")
        }

        if (data.has(OMMFilterList.HAS_TOTAL_COUNT_HINT)) {
            if (buf.length() != 0)
                buf.append(" | ")

            buf.append("HAS_TOTAL_COUNT_HINT")
        }
        return buf.toString()
    }

    static String vectorEntryFlagsString(OMMVectorEntry data) {
        StringBuilder buf = new StringBuilder(60)

        if (data.has(OMMVectorEntry.HAS_PERMISSION_DATA)) {
            buf.append("HAS_PERMISSION_DATA")
        }
        return buf.toString()
    }

    static String filterEntryFlagsString(OMMFilterEntry data) {
        StringBuilder buf = new StringBuilder(60)

        if (data.has(OMMFilterEntry.HAS_PERMISSION_DATA)) {
            buf.append("HAS_PERMISSION_DATA")
        }
        if (data.has(OMMFilterEntry.HAS_DATA_FORMAT)) {
            if (buf.length() != 0)
                buf.append(" | ")

            buf.append("HAS_DATA_FORMAT")
        }
        return buf.toString()
    }
}
