package com.friggsoft.rfa.provider

import groovy.util.logging.Slf4j

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

import com.reuters.rfa.dictionary.DictionaryException
import com.reuters.rfa.dictionary.FieldDictionary

@Slf4j
final class DictionaryReader {

    /** Field dictionary to return the consumers. */
    private static final String fieldDictionaryFilename = "RDM/RDMFieldDictionary"

    /** Enum types for the field dictionary. */
    private static final String enumDictionaryFilename = "RDM/enumtype.def"

    /**
     * Initialize the field dictionary from file.
     *
     * @param rwfDictionary dictionary to read into
     */
    static void load(FieldDictionary rwfDictionary) throws DictionaryException {
        try {
            String fieldDictPath = copyFromClasspathIntoTempFile(fieldDictionaryFilename)
            FieldDictionary.readRDMFieldDictionary(rwfDictionary, fieldDictPath)

            String enumDictPath = copyFromClasspathIntoTempFile(enumDictionaryFilename)
            FieldDictionary.readEnumTypeDef(rwfDictionary, enumDictPath)
        }
        catch (DictionaryException ex) {
            log.error("Dictionary read error: " + ex.getMessage())
            throw ex
        }
        catch (IOException ex) {
            String msg = String.format("Cannot read dictionary: %s", ex.getMessage())
            log.error(msg)
            throw new DictionaryException(msg, ex)
        }
    }

    /**
     * Copy the file from resources to a temporary file. This is a kludge for
     * readers that can't handle the file residing inside of an uber-jar.
     *
     * The copied file fill be deleted on JVM exit.
     *
     * @param filename name of file to find (may be relative path)
     * @return the full path to the copied file
     */
    private static String copyFromClasspathIntoTempFile(String filename) throws IOException {
        Path source = Paths.get(filename).getFileName()
        Path target = Files.createTempFile(source.toString(), ".txt")
        InputStream inputStream = readFromClasspathIntoStream(filename)
        try {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            inputStream.close()
        }
        target.toFile().deleteOnExit()
        return target.toAbsolutePath().normalize().toString()
    }

    /**
     * Caller must close the returned stream.
     */
    private static InputStream readFromClasspathIntoStream(String filename) throws IOException {
        assert filename != null, 'filename cannot be null'

        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename)
        if (inputStream == null) {
            throw new IOException("Could not find " + filename)
        }
        return inputStream
    }
}
