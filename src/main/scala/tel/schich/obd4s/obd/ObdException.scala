package tel.schich.obd4s.obd

import tel.schich.obd4s.Cause

class ObdException(val cause: Cause) extends RuntimeException
