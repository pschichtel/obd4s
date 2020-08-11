package tel.schich.obd4s

import org.scalacheck.Prop.forAll
import org.scalacheck.Properties
import tel.schich.obd4s.obd.dtc.DiagnosticTroubleCode

class TroubleCodeSpec extends Properties("Trouble Codes") {
    property("should parse and toString with correctly") = forAll { (data: Short) =>
        val code = DiagnosticTroubleCode(data)
        val codeString = code.toString
        codeString.length == 5 && codeString(0) == code.component.letter && codeString.drop(1).forall(c => (('0' to '9') ++ ('a' to 'f') ++ ('A' to 'F')) contains c)
    }
}
