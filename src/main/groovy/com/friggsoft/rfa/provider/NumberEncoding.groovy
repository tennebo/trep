package com.friggsoft.rfa.provider

import com.reuters.rfa.omm.OMMException
import com.reuters.rfa.omm.OMMNumeric

final class NumberEncoding {

    /**
     * Map an OMMNumeric exponent to a factor.
     */
    final static double[] EncodingFactor = [
        100000000000000D,   // 0 EXPONENT_NEG14
        10000000000000D,    // 1 EXPONENT_NEG13
        1000000000000D,     // 2 EXPONENT_NEG12
        100000000000D,      // 3 EXPONENT_NEG11
        10000000000D,       // 4 EXPONENT_NEG10
        1000000000,         // 5 EXPONENT_NEG9
        100000000,          // 6 EXPONENT_NEG8
        10000000,           // 7 EXPONENT_NEG7
        1000000,            // 8 EXPONENT_NEG6
        100000,             // 9 EXPONENT_NEG5
        10000,              // 10 EXPONENT_NEG4
        1000,               // 11 EXPONENT_NEG3
        100,                // 12 EXPONENT_NEG2
        10,                 // 13 EXPONENT_NEG1
        1,                  // 14 EXPONENT_0
        0.1,                // 15 EXPONENT_POS1
        0.01,               // 16 EXPONENT_POS2
        0.001,              // 17 EXPONENT_POS3
        0.0001,             // 18 EXPONENT_POS4
        0.00001,            // 19 EXPONENT_POS5
        0.000001,           // 20 EXPONENT_POS6
        0.0000001,          // 21 EXPONENT_POS7
        1,                  // 22 DIVISOR_1
        2,                  // 23 DIVISOR_2
        4,                  // 24 DIVISOR_4
        8,                  // 25 DIVISOR_8
        16,                 // 26 DIVISOR_16
        32,                 // 27 DIVISOR_32
        64,                 // 28 DIVISOR_64
        128,                // 29 DIVISOR_128
        256                 // 30 DIVISOR_256
    ]

    /**
     * Encode (and round) a floating point value into a 64-bit integer.
     */
    static long roundDouble2Long(double d, int exponentEncoding) {
        validateEncoding(exponentEncoding)

        double encodedDouble = d * EncodingFactor[exponentEncoding - OMMNumeric.EXPONENT_NEG14]
        long encodedLong = Math.round(encodedDouble)

        return encodedLong
    }

    /**
     * Throw if the given encoding parameter is out of range.
     */
    private static void validateEncoding(int encoding) {
        if (encoding < 0 || OMMNumeric.MAX_HINT <= encoding) {
            throw new OMMException("Encoding " + encoding + " is out of range")
        }
    }
}
