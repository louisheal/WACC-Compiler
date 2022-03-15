package assembly.instructions;

import assembly.Conditionals;
import assembly.Operand2;
import assembly.Register;

public class MOV extends Instruction {

  //MOV dest, immValue
  public MOV(Register dest, long immValue) {
    this.dest = dest;
    this.immValue = immValue;
  }
  //MOV{cond} dest, immValue
  public MOV(Register dest, long immValue, Conditionals conditionals) {
    this.dest = dest;
    this.immValue = immValue;
    this.extraInformation += conditionals.name();
  }
  //MOV dest, operand
  public MOV(Register dest, Operand2 operand2) {
    this.dest = dest;
    this.operand2 = operand2;
  }
  //MOV{cond} dest, operand
  public MOV(Register dest, Operand2 operand2, Conditionals conditionals) {
    this.dest = dest;
    this.operand2 = operand2;
    this.extraInformation += conditionals;
  }

  @Override
  public String toString() {
    //With immValue format
    if (operand2 == null) {
      return "MOV" + extraInformation + " " + dest + ", #" + immValue;
    }

    //With operand format
    return "MOV" + extraInformation + " " + dest + ", " + operand2;
  }
}