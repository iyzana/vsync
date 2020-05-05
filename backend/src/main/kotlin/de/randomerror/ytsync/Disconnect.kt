package de.randomerror.ytsync

import java.lang.RuntimeException

class Disconnect(message: String = "invalid command"): RuntimeException(message)