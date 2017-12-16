package com.friggsoft.rfa.config

/**
 * Symbolic constants for TREP configuration.
 */
interface Constants {
    /** Application name. No idea what this is for. */
    String application = "application"

    /** Username to log in to the service as. */
    String user = "user"

    /** For DACS. */
    String position = "position"

    /** Name of the service to connect to. */
    String serviceName = "serviceName"

    /** Path to the session configuration: "namespace::sessionName". */
    String session = "session"

    /** Message Model Type. */
    String MessageModelType = "mmt"

    /**
     * Filename used for low-level data and error tracing by the RSSL Transport.
     * Messages are logged using the J2SE Logging API. If the filename is “console”
     * then a ConsoleHandler is used instead of a FileHandler.
     *
     * To disable RSSL log, set to “none”.
     */
    String logFileName = "logFileName"

    /** Enables or disables detailed tracing of the connection process. */
    String mountTrace = "mountTrace"
}
