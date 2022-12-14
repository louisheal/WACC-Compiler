package assembly.instructions;

import assembly.Operand2;
import assembly.Register;

public class STR extends Instruction {

  private boolean exclaim = false;

  //STR src, [dest]
  public STR(Register src, Operand2 dest) {
    this.operand1 = src;
    this.operand2 = dest;
  }

  //STR{B} dest, operand
  public STR(Register src, Operand2 dest, String suffix) {
    this.operand1 = src;
    this.operand2 = dest;
    this.extraInformation += suffix;
  }

  public STR setExclaim() {
    exclaim = true;
    return this;
  }

  @Override
  public String toString() {
    if (exclaim) {
      return String.format("STR%s %s, [%s]!", extraInformation, operand1, operand2);
    }
    return String.format("STR%s %s, [%s]", extraInformation, operand1, operand2);
  }
}
