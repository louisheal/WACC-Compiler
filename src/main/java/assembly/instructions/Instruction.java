package assembly.instructions;

import assembly.Conditionals;
import assembly.Flags;
import assembly.Operand2;
import assembly.Register;

public abstract class Instruction {
  Register dest;
  Long immValue;
  Register operand1;
  Operand2 operand2;
  String extraInformation = "";
  String label;

  Register rdLo;
  Register rdHi;
  Register rn;
  Register rm;
}
