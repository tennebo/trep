package com.friggsoft.rfa.consumer

import groovy.util.logging.Slf4j

import com.friggsoft.rfa.provider.DictionaryReader
import com.friggsoft.rfa.util.GenericOMMParser
import com.reuters.rfa.dictionary.DictionaryException
import com.reuters.rfa.dictionary.FidDef
import com.reuters.rfa.dictionary.FieldDictionary
import com.reuters.rfa.omm.OMMData
import com.reuters.rfa.omm.OMMEntry
import com.reuters.rfa.omm.OMMFieldEntry
import com.reuters.rfa.omm.OMMIterable
import com.reuters.rfa.omm.OMMNumeric
import com.reuters.rfa.omm.OMMTypes

/**
 * Parse an OMM message to extract the last traded price from the payload.
 */
@Slf4j
final class OmmPayloadParser {
    /**
     * Name of the last price field. From the file enumtype.def:
     *
     * "LST"   Price value is populated in the field LAST (TRDPRC_1)
     */
    private static final String TARGET_FIELD_NAME = "TRDPRC_1"

    /** A field dictionary (should be downloaded from the TREP server). */
    private final FieldDictionary dictionary = FieldDictionary.create()

    OmmPayloadParser() throws DictionaryException {
        DictionaryReader.load(dictionary)
        GenericOMMParser.setDictionary(dictionary)
    }

    /** For diagnostics. */
    private static String decodeString(OMMFieldEntry fieldEntry, FidDef fidDef) {
        OMMData value = fieldEntry.getData(fidDef.getOMMType())

        return value.toString()
    }

    /** Extract a value known to be a number. */
    private static double decodeDouble(OMMFieldEntry fieldEntry, FidDef fidDef) {
        OMMData value = fieldEntry.getData(fidDef.getOMMType())
        OMMNumeric nValue = (OMMNumeric)value

        return nValue.toDouble()
    }

    /**
     * Extract the traded price from the given payload.
     */
    double parsePayload(OMMData payload) {
        OMMIterable iterable = (OMMIterable) payload

        for (Iterator iter = iterable.iterator(); iter.hasNext(); ) {
            OMMEntry entry = (OMMEntry) iter.next()
            if (entry.getType() == OMMTypes.FIELD_ENTRY) {
                OMMFieldEntry fieldEntry = (OMMFieldEntry) entry
                FidDef fidDef = dictionary.getFidDef(fieldEntry.getFieldId())
                String fieldName = fidDef.getName()
                if (TARGET_FIELD_NAME.equals(fieldName)) {
                    // We have our traded price
                    return decodeDouble(fieldEntry, fidDef)
                } else {
                    log.trace("Dropping {}: {}", fieldName, decodeString(fieldEntry, fidDef))
                }
            }
        }
        // This will happen if we have subscribed to events other than 'traded price'
        log.trace("No {} value in payload {}", TARGET_FIELD_NAME, payload)
        return Double.NaN
    }

    /**
     * Extract all numerical values from the given payload. Dates, etc. are dropped.
     */
    void parsePayload(OMMData payload, Map<String, Double> valuesMap) {
        OMMIterable iterable = (OMMIterable) payload

        for (Iterator iter = iterable.iterator(); iter.hasNext(); ) {
            OMMEntry entry = (OMMEntry) iter.next()
            if (entry.getType() == OMMTypes.FIELD_ENTRY) {
                OMMFieldEntry fieldEntry = (OMMFieldEntry) entry
                FidDef fidDef = dictionary.getFidDef(fieldEntry.getFieldId())
                short type = fidDef.getOMMType()
                OMMData value = fieldEntry.getData(type)
                if (value instanceof OMMNumeric) {
                    String fieldName = fidDef.getName()
                    double dValue = ((OMMNumeric)value).toDouble()
                    valuesMap.put(fieldName, dValue)
                } else {
                    log.trace("Non-numeric value {}: {}", fidDef.getName(), value.toString())
                }
            } else {
                log.trace("Non-field entry: {}", entry.toString())
            }
        }
    }
}
