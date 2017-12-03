package com.friggsoft.rfa.diagnostics

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer
import org.springframework.boot.diagnostics.FailureAnalysis
import org.springframework.boot.diagnostics.FailureAnalyzer

import com.reuters.rfa.dictionary.DictionaryException

/**
 * An {@link FailureAnalyzer} that performs analysis of failures
 * to load the RDM field dictionary from file.
 *
 * This class must be registered in META-INF/spring.factories.
 */
final class DictionaryReadAnalyzer extends AbstractFailureAnalyzer<DictionaryException> {
    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, DictionaryException cause) {
        String description = cause.getMessage()
        String action =
                "Make sure the RDM Field Dictionary and its enum type definitions exist on the classpath"
        return new FailureAnalysis(description, action, cause)
    }
}
