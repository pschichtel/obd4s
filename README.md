# obd4s

This library is a simple implementation of the OBD protocol over CAN and various ELM devices.

The CAN communication is handled by the [JavaCAN](https://github.com:pschichtel/javacan.git)
library, ELM communication uses other a serial connection or a bluetooth connection, depending
on the device being used.
