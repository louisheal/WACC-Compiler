import assembly.Conditionals;
import assembly.Flags;
import assembly.instructions.*;
import assembly.Operand2;
import assembly.Register;
import ast.*;

import ast.Type.EType;
import java.util.*;

import static assembly.PredefinedFunctions.*;
import static assembly.PredefinedFunctions.Functions.*;
import static ast.Type.EType.*;

public class Converter extends ASTVisitor<List<Instruction>> {

  /* A list of general purpose registers: r4, r5, r6, r7, r8, r9, r10 and r11. */
  List<Register> unusedRegisters = initialiseGeneralRegisters();

  /* Function return registers. */
  private final Register r0 = new Register(0);
  private final Register r1 = new Register(1);

  /* Special registers. */
  private final Register sp = new Register(13);
  private final Register pc = new Register(15);

  private int spLocation = 0;
  private int labelNum = 0;
  private int functionByte = 0;
  SymbolTable currentST;

  /* Print flags */
  private boolean hasPrintInt = false;
  private boolean hasPrintBool = false;
  private boolean hasPrintString = false;
  private boolean hasPrintReference = false;
  private boolean hasPrintLn = false;

  /* Read flags */
  private boolean hasReadInt = false;
  private boolean hasReadChar = false;

  /* Free flags */
  private boolean hasFreePair = false;

  /* Error flags */
  private boolean checkNullPointer = false;
  private boolean runtimeErr = false;
  private boolean isDiv = false;
  private boolean isCalc = false;
  private boolean isArrayLookup = false;

  /* Data flag */
  private boolean hasData = false;

  private String getLabel() {
    int result = labelNum;
    labelNum++;
    return "L" + result;
  }

  private Type getExpressionType(Expression expr) {

    if (expr == null) {
      return null;
    }

    switch (expr.getExprType()) {

      case INTLITER:
      case NEG:
      case ORD:
      case LEN:
      case DIVIDE:
      case MULTIPLY:
      case MODULO:
      case PLUS:
      case MINUS:
        return new Type(INT);

      case BOOLLITER:
      case NOT:
      case GT:
      case GTE:
      case LT:
      case LTE:
      case EQ:
      case NEQ:
      case AND:
      case OR:
        return new Type(BOOL);

      case CHARLITER:
      case CHR:
        return new Type(CHAR);

      case STRINGLITER:
        return new Type(STRING);

      case IDENT:
        return currentST.getType(expr.getIdent());

      case ARRAYELEM:
        ArrayElem arrayElem = expr.getArrayElem();
        Type result = currentST.getType(arrayElem.getIdent());
        for (int i = 0; i < arrayElem.getExpression().size(); i++) {
          result = result.getArrayType();
        }
        return result;

      case BRACKETS:
        return getExpressionType(expr.getExpression1());

    }
    return null;
  }

  private List<Register> initialiseGeneralRegisters() {

    List<Register> registers = new ArrayList<>();

    registers.add(new Register(4));
    registers.add(new Register(5));
    registers.add(new Register(6));
    registers.add(new Register(7));
    registers.add(new Register(8));
    registers.add(new Register(9));
    registers.add(new Register(10));
    registers.add(new Register(11));

    return registers;
  }

  private Register popUnusedRegister() {
    Register register = unusedRegisters.get(0);
    unusedRegisters.remove(register);
    return register;
  }

  private void pushUnusedRegister(Register register) {
    unusedRegisters.add(0, register);
  }

  private int sizeOfTypeOnStack(Type type) {

    if (type == null) {
      return 4;
    }

    switch (type.getType()) {
      case INT:
      case PAIR:
      case ARRAY:
      case STRING:
        return 4;
      case CHAR:
      case BOOL:
        return 1;
    }

    return 0;
  }

  private int totalBytesInScope(Statement statement) {
    return totalBytesInStatement(statement) + maxBeginStatement(statement);
  }

  private int totalBytesInStatement(Statement statement) {

    switch(statement.getStatType()) {

      case DECLARATION:
        return sizeOfTypeOnStack(statement.getLhsType());

      case CONCAT:
        return totalBytesInStatement(statement.getStatement1()) + totalBytesInStatement(statement.getStatement2());

      case WHILE:
        return totalBytesInStatement(statement.getStatement1()) + maxBeginStatement(statement);

      case IF:
        int stat1Size = totalBytesInStatement(statement.getStatement1());
        int stat2Size = totalBytesInStatement(statement.getStatement2());
        return Math.max(stat1Size, stat2Size);
    }

    return 0;
  }

  private int maxBeginStatement(Statement statement) {

    List<Statement> beginStatements = getBeginStatements(statement);

    int size = 0;
    for (Statement s : beginStatements) {
      int sSize = totalBytesInScope(s.getStatement1());
      if (sSize > size) {
        size = sSize;
      }
    }

    return size;
  }

  private List<Statement> getBeginStatements(Statement statement) {

    List<Statement> beginStatements = new ArrayList<>();

    if (statement.getStatType() == Statement.StatType.BEGIN) {
      beginStatements.add(statement);
    }

    if (statement.getStatType() == Statement.StatType.CONCAT) {
      beginStatements.addAll(getBeginStatements(statement.getStatement1()));
      beginStatements.addAll(getBeginStatements(statement.getStatement2()));
    }

    return beginStatements;
  }

  @Override
  public List<Instruction> visitProgram(Program program) {

    List<Instruction> instructions = new ArrayList<>();

    instructions.add(new TEXT(""));
    instructions.add(new LABEL("")); // Leave gap in lines

    instructions.add(new GLOBALMAIN(""));

    /* Generate the assembly instructions for each function. */
    for (Function function : program.getFunctions()) {
      instructions.addAll(visitFunction(function));
    }

    instructions.add(new LABEL("main:"));

    //TODO: ADD LR
    instructions.add(new LABEL("PUSH {lr}"));

    int totalBytes = totalBytesInScope(program.getStatement());
    spLocation = totalBytes;

    while (totalBytes > 1024) {
      instructions.add(new LABEL("SUB sp, sp, #1024"));
      totalBytes -= 1024;
    }
    if (totalBytes > 0) {
      instructions.add(new LABEL("SUB sp, sp, #" + totalBytes));
    }

    totalBytes = spLocation;

    currentST = new SymbolTable(null);

    /* Generate the assembly instructions for the program body. */
    instructions.addAll(visitStatement(program.getStatement()));

    while (totalBytes > 1024) {
      instructions.add(new LABEL("ADD sp, sp, #1024"));
      totalBytes -= 1024;
    }
    if (totalBytes > 0) {
      instructions.add(new LABEL("ADD sp, sp, #" + totalBytes));
    }

    instructions.add(new LDR(r0, 0));
    instructions.add(new LABEL("POP {pc}"));
    instructions.add(new LTORG(""));

    //TODO: Add instructions as args

    if (isArrayLookup) {
      instructions.addAll(getFunctionInstructions(P_CHECK_ARRAY_BOUNDS));
      runtimeErr = true;
      hasData = true;
    }

    if (checkNullPointer) {
      instructions.addAll(getFunctionInstructions(P_CHECK_NULL_POINTER));
      runtimeErr = true;
      hasData = true;
    }

    if (isDiv) {
      instructions.addAll(getFunctionInstructions(P_CHECK_DIVIDE_BY_ZERO));
      runtimeErr = true;
      hasData = true;
    }

    if (isCalc) {
      instructions.addAll(getFunctionInstructions(P_THROW_OVERFLOW_ERROR));
      runtimeErr = true;
      hasData = true;
    }

    if (hasFreePair) {
      instructions.addAll(getFunctionInstructions(P_FREE_PAIR));
      runtimeErr = true;
      hasData = true;
    }

    if (hasPrintInt) {
      instructions.addAll(getFunctionInstructions(P_PRINT_INT));
      hasData = true;
    }

    if (runtimeErr) {
      instructions.addAll(getFunctionInstructions(P_THROW_RUNTIME_ERROR));
      hasPrintString = true;
      hasData = true;
    }

    if (hasPrintBool) {
      instructions.addAll(getFunctionInstructions(P_PRINT_BOOL));
      hasData = true;
    }

    if (hasPrintString) {
      instructions.addAll(getFunctionInstructions(P_PRINT_STRING));
      hasData = true;
    }

    if (hasPrintLn) {
      instructions.addAll(getFunctionInstructions(P_PRINT_LN));
      hasData = true;
    }

    if (hasPrintReference) {
      instructions.addAll(getFunctionInstructions(P_PRINT_REFERENCE));
      hasData = true;
    }

    if (hasReadInt) {
      instructions.addAll(getFunctionInstructions(P_READ_INT));
      hasData = true;
    }

    if (hasReadChar) {
      instructions.addAll(getFunctionInstructions(P_READ_CHAR));
      hasData = true;
    }

    if (hasData) {
      instructions.add(0, new DATA(""));
      instructions.add(1, new LABEL(""));
      instructions.add(2, new LABEL("")); // Leave gap in lines
      instructions.addAll(2, getMessages());
    }

    return instructions;
  }

  @Override
  public List<Instruction> visitFunction(Function function) {

    List<Instruction> instructions = new ArrayList<>();

    /* Everytime entering a new function, functionByte is updated */
    functionByte = totalBytesInScope(function.getStatement());

    /* Initialise symbol table. */
    currentST = new SymbolTable(null);

    /* Function wrapper instructions. */
    instructions.add(new LABEL (function.getIdent() + ":"));
    instructions.add(new LABEL ("PUSH {lr}"));

    /* Move stack pointer to allocate space on the stack for the function to use. */
    int totalBytes = totalBytesInScope(function.getStatement());
    spLocation = totalBytes;

    while (totalBytes > 1024) {
      instructions.add(new LABEL ("SUB sp, sp, #1024"));
      totalBytes -= 1024;
    }
    if (totalBytes > 0) {
      instructions.add(new LABEL (String.format("SUB sp, sp, #%d", totalBytes)));
    }

    totalBytes = spLocation;

    /* Add parameters to symbol table. */
    int stackOffset = totalBytes + 4;
    for (Param param : function.getParams()) {
      currentST.newVariable(param.getIdent(), param.getType());
      currentST.setSPMapping(param.getIdent(), stackOffset);
      stackOffset += sizeOfTypeOnStack(param.getType());
    }

    /* Evaluate function body. */
    instructions.addAll(visitStatement(function.getStatement()));

    instructions.add(new LTORG( ""));

    return instructions;
  }

  @Override
  public List<Instruction> visitSkipStatement(Statement statement) {
    /* Generate instructions when a skip statement is found, that is, no instructions. */
    return Collections.emptyList();
  }

  @Override
  public List<Instruction> visitDeclarationStatement(Statement statement) {

    /* Generate instructions to evaluate the RHS and put the result into the first unused register. */
    List<Instruction> instructions = new ArrayList<>(visitRHS(statement.getRHS()));

    /* Retrieve the first register which is where the value of the RHS is stored. */
    Register rn = popUnusedRegister();

    int stackOffset = spLocation - sizeOfTypeOnStack(statement.getLhsType());

    String instruction = "STR";

    if (sizeOfTypeOnStack(statement.getLhsType()) == 1) {
      instruction += "B";
    }

    if (stackOffset > 0) {
      // STR rn, [sp]
      instruction += String.format(" %s, [sp, #%d]", rn, stackOffset);
    } else {
      // STR rn, [sp, #i]
      instruction += String.format(" %s, [sp]", rn);
    }
    instructions.add(new LABEL(instruction));

    /* Add the variable to the symbol table. */
    currentST.setSPMapping(statement.getLhsIdent(), stackOffset);
    currentST.newVariable(statement.getLhsIdent(), statement.getLhsType());

    /* Update the spLocation variable to point to the next available space on the stack. */
    spLocation = stackOffset;

    /* Mark the register used in the evaluation of this function as no longer in use. */
    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitReassignmentStatement(Statement statement) {

    /* Evaluate RHS expression and store result in rn. */
    List<Instruction> instructions = new ArrayList<>(visitRHS(statement.getRHS()));

    /* Retrieve first unused register. */
    Register rn = popUnusedRegister();

    /* Code generation when the LHS is an identity. */
    if (statement.getLHS().getAssignType() == AssignLHS.LHSType.IDENT) {
      int stackOffset = currentST.getSPMapping(statement.getLHS().getIdent());

      Expression expression = new ExpressionBuilder().buildIdentExpr(statement.getLHS().getIdent());

      String instruction = "STR";

      if (sizeOfTypeOnStack(getExpressionType(expression)) == 1) {
        instruction += "B";
      }

      if (stackOffset > 0) {
        instruction += String.format(" %s, [sp, #%d]", rn, stackOffset);
      } else {
        instruction += String.format(" %s, [sp]", rn);
      }

      instructions.add(new LABEL(instruction));

      pushUnusedRegister(rn);

      return instructions;
    }

    /* Code generation when the LHS is a pair element or array element. */

    /* Evaluate the LHS and store its address in rm. */
    instructions.addAll(visitLHS(statement.getLHS()));

    /* Retrieve the next unused register. */
    Register rm = popUnusedRegister();

    Type type = currentST.getType(getIdentFromLHS(statement.getLHS()));

    if (statement.getLHS().getAssignType() == AssignLHS.LHSType.PAIRELEM) {
      if (statement.getLHS().getPairElem().getType() == PairElem.PairElemType.FST) {
        type = type.getFstType();
      } else {
        type = type.getSndType();
      }
    }

    if (statement.getLHS().getAssignType() == AssignLHS.LHSType.ARRAYELEM) {
      type = getExpressionType(new ExpressionBuilder().buildArrayExpr(statement.getLHS().getArrayElem()));
    }

    if (sizeOfTypeOnStack(type) == 1) {
      instructions.add(new STR(rm, new Operand2(rn), "B"));
    } else {
      instructions.add(new STR (rm, new Operand2(rn)));
    }

    /* Mark registers as no longer in use. */
    pushUnusedRegister(rm);
    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitIdentLHS (AssignLHS lhs){

    List<Instruction> instructions = new ArrayList<>();

    int stackOffset = currentST.getSPMapping(lhs.getIdent());

    Register rn = popUnusedRegister();

    instructions.add(new ADD(rn, sp, new Operand2(stackOffset)));

    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitArrayElemLHS(AssignLHS lhs) {

    /* Set ArrayLookup flag to true. */
    isArrayLookup = true;

    ArrayElem arrayElem = lhs.getArrayElem();
    List<Instruction> instructions = new ArrayList<>();
    String instruction;

    /* Find where the array address is stored on the stack. */
    int stackOffset = currentST.getSPMapping(arrayElem.getIdent());

    /* Allocate one register: rn for this function to use. */
    Register rn = popUnusedRegister();

    // ADD rn, sp, #offset
    instructions.add(new LABEL(String.format("ADD %s, sp, #%d", rn, stackOffset)));

    for (Expression expression : arrayElem.getExpression()) {

      /* Evaluate the index of the ArrayElem and store it in rm. */
      instructions.addAll(visitExpression(expression));

      /* Retrieve the register rm which contains the value of the index. */
      Register rm = popUnusedRegister();

      // LDR rn, [rn]
      instructions.add(new LDR(rn, new Operand2(rn)));

      // MOV r0, rm
      instructions.add(new MOV(r0, new Operand2(rm)));

      // MOV r1, rn
      instructions.add(new MOV(r1, new Operand2(rn)));

      // BL p_check_array_bounds
      instructions.add(new BL("p_check_array_bounds"));

      // ADD rn, rn, #4
      instructions.add(new ADD(rn, rn, new Operand2(4)));

      if (sizeOfTypeOnStack(getExpressionType(new ExpressionBuilder().buildArrayExpr(arrayElem))) == 1) {
        // ADD rn, rn, rm
        instruction = String.format("ADD %s, %s, %s", rn, rn, rm);
      } else {
        // ADD rn, rn, rm, LSL #2
        instruction = String.format("ADD %s, %s, %s, LSL #2", rn, rn, rm);
      }
      instructions.add(new LABEL(instruction));

      pushUnusedRegister(rm);
    }

    /* Mark the register used in the evaluation of this function as no longer in use. */
    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitIfStatement(Statement statement) {

    /* Evaluate the condition expression. */
    List<Instruction> instructions = new ArrayList<>(visitExpression(statement.getExpression()));

    /* Generate Labels. */
    String label1 = getLabel();
    String label2 = getLabel();

    /* Retrieve the register containing the result from evaluating the condition. */
    Register rn = popUnusedRegister();

    // CMP rn, #0
    instructions.add(new CMP(rn, new Operand2(0)));

    // BEQ Lx
    instructions.add(new LABEL("BEQ " + label1));

    /* Mark the register rn as no longer in use. */
    pushUnusedRegister(rn);

    /* Generate instructions for the 'if' clause of the statement and change scope. */
    currentST = new SymbolTable(currentST);
    int temp = spLocation;
    instructions.addAll(visitStatement(statement.getStatement1()));
    spLocation = temp;
    currentST = currentST.getParent();

    // BL Lx+1
    instructions.add(new BL(label2));

    //Lx:
    instructions.add(new LABEL(label1 + ":"));

    /* Generate instructions for the 'else' clause of the statement and change scope. */
    currentST = new SymbolTable(currentST);
    temp = spLocation;
    instructions.addAll(visitStatement(statement.getStatement2()));
    spLocation = temp;
    currentST = currentST.getParent();

    //Lx+1:
    instructions.add(new LABEL(label2 + ":"));

    return instructions;
  }

  @Override
  public List<Instruction> visitWhileStatement(Statement statement) {

    List<Instruction> instructions = new ArrayList<>();

    /* Generate Labels. */
    String label1 = getLabel();
    String label2 = getLabel();

    // B Lx
    instructions.add(new LABEL("B " + label1));

    // Lx+1:
    instructions.add(new LABEL(label2 + ":"));

    /* Generate code for the while body and change scope. */
    currentST = new SymbolTable(currentST);
    int temp = spLocation;
    instructions.addAll(visitStatement(statement.getStatement1()));
    spLocation = temp;
    currentST = currentST.getParent();

    // Lx:
    instructions.add(new LABEL(label1 + ":"));

    /* Evaluate the condition expression. */
    instructions.addAll(visitExpression(statement.getExpression()));

    /* Retrieve register containing the evaluation of the conditional. */
    Register rn = popUnusedRegister();

    // CMP rn, #1
    instructions.add(new CMP(rn, new Operand2(1)));

    // BEQ Lx+1
    instructions.add(new LABEL("BEQ " + label2));

    /* Mark the register rn as no longer in use. */
    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitBeginStatement(Statement statement) {

    /* Generate code for begin body and change scope. */
    currentST = new SymbolTable(currentST);
    int temp = spLocation;
    List<Instruction> instructions = new ArrayList<>(visitStatement(statement.getStatement1()));
    spLocation = temp;
    currentST = currentST.getParent();

    return instructions;
  }

  @Override
  public List<Instruction> visitConcatStatement(Statement statement) {
    List<Instruction> instructions = new ArrayList<>();

    /* Generate the assembly code for each statement in the concatenated statement. */
    instructions.addAll(visitStatement(statement.getStatement1()));
    instructions.addAll(visitStatement(statement.getStatement2()));

    return instructions;
  }

  @Override
  public List<Instruction> visitReturnStatement(Statement statement) {

    /* Evaluate the expression on the rhs of the return statement. */
    List<Instruction> instructions = new ArrayList<>(visitExpression(statement.getExpression()));

    /* Allocate a register to use for the duration of this function. */
    Register rn = popUnusedRegister();

    // MOV r0, rn
    instructions.add(new MOV(r0, new Operand2(rn)));

    instructions.add(new ADD(sp, sp, new Operand2(functionByte)));

    instructions.add(new POP(pc));

    /* Mark the allocated register as no longer in use. */
    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitIntLiterExp(Expression expression) {

    /* Allocate a register: rn for this function to use. */
    Register rn = popUnusedRegister();

    List<Instruction> instructions = new ArrayList<>();

    // LDR rn, =i
    instructions.add(new LDR(rn, expression.getIntLiter()));

    /* Mark the register used in the evaluation of this function as no longer in use. */
    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitBoolLiterExp(Expression expression) {

    /* Allocate a register: rn for this function to use. */
    Register rn = popUnusedRegister();

    long boolVal = expression.getBoolLiter() ? 1 : 0;

    List<Instruction> instructions = new ArrayList<>();

    // MOV rn, #(1 | 0)
    instructions.add(new MOV(rn, boolVal));

    /* Mark the register used in the evaluation of this function as no longer in use. */
    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitCharLiterExp(Expression expression) {

    /* Allocate a register: rn for this function to use. */
    Register rn = popUnusedRegister();

    List<Instruction> instructions = new ArrayList<>();

    // MOV rn, #charVal
    String instruction = String.format("MOV %s, #'%s'", rn, expression.getCharLiter());

    switch (expression.getCharLiter()) {
      case '\0':
        instruction = String.format("MOV %s, #0", rn);
        break;
      case '\b':
        instruction = String.format("MOV %s, #8", rn);
        break;
      case '\t':
        instruction = String.format("MOV %s, #9", rn);
        break;
      case '\n':
        instruction = String.format("MOV %s, #10", rn);
        break;
      case '\f':
        instruction = String.format("MOV %s, #12", rn);
        break;
      case '\r':
        instruction = String.format("MOV %s, #13", rn);
        break;
    }

    instructions.add(new LABEL(instruction));

    /* Mark the register used in the evaluation of this function as no longer in use. */
    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitStringLiterExp(Expression expression) {

    List<Instruction> instructions = new ArrayList<>();

    String string = expression.getStringLiter();

    String message = string.substring(string.indexOf('"') + 1, string.lastIndexOf('"'));
    int messageLength = getMessageLength(message);

    /* Generate a message number for the string. */
    String msgLabel = getMessageLabel();
    addMessage(new LABEL(msgLabel + ":"));
    // length - 2 to remove to ensure quotation marks aren't include in the length
    addMessage(new WORD(messageLength));
    addMessage(new ASCII(string));

    /* Allocate a register: rn for this function to use. */
    Register rn = popUnusedRegister();

    // LDR rn, =msg_0
    instructions.add(new LDR(rn, msgLabel));

    /* Mark the register used in the evaluation of this function as no longer in use. */
    pushUnusedRegister(rn);

    // To ensure the messages are added to the top of assembly code
    hasData = true;

    return instructions;
  }

  private int getMessageLength(String message) {

    int messageLength = 0;

    for (int i = 0; i < message.length(); i++) {
      if (message.charAt(i) == '\\') {
        i++;
      }
      messageLength++;
    }

    return messageLength;
  }

  @Override
  public List<Instruction> visitIdentExp(Expression expression) {

    List<Instruction> instructions = new ArrayList<>();

    /* Retrieve the position of the variable on the stack from the symbol table. */
    int stackOffset = currentST.getSPMapping(expression.getIdent());

    /* Allocate a register: rn for this function to use. */
    Register rn = popUnusedRegister();

    String instruction = "LDR";

    if (sizeOfTypeOnStack(getExpressionType(expression)) == 1){
      instruction += "SB";
    }

    if (stackOffset != 0) {
      instruction += String.format(" %s, [sp, #%d]", rn, stackOffset);
    } else {
      instruction += String.format(" %s, [sp]", rn);
    }
    instructions.add(new LABEL(instruction));

    /* Mark the register used in the evaluation of this function as no longer in use. */
    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitArrayElemExp(Expression expression) {

    AssignLHS assignLHS = new AssignLHSBuilder().buildArrayLHS(expression.getArrayElem());

    Type type = currentST.getType(expression.getArrayElem().getIdent());

    /* Calculate the address of the Array Elem and store it in register rn. */
    List<Instruction> instructions = new ArrayList<>(visitLHS(assignLHS));

    /* Retrieve the register rn, which contains the address of the array elem. */
    Register rn = popUnusedRegister();

    String instruction = "LDR";

    if (sizeOfTypeOnStack(type.getArrayType()) == 1) {
      instruction += "SB";
    }

    instruction += String.format(" %s, [%s]", rn, rn);

    // LDR rn, [rn]
    instructions.add(new LABEL(instruction));

    /* Mark the register used in the evaluation of this function as no longer in use. */
    pushUnusedRegister(rn);

    return instructions;
  }

  private String getIdentFromLHS(AssignLHS lhs){
    switch (lhs.getAssignType()) {
      case ARRAYELEM:
        return lhs.getArrayElem().getIdent();
      //TODO Check PAIRELEM ident is correct
      case PAIRELEM:
        return lhs.getPairElem().getExpression().getIdent();
      default:
        return lhs.getIdent();
    }
  }

  private List<Instruction> translateUnaryExpression(Expression expression) {
    /* Generate assembly instructions for the expression. */
    return new ArrayList<>(visitExpression(expression.getExpression1()));
  }

  @Override
  public List<Instruction> visitNotExp(Expression expression) {
    List<Instruction> instructions = translateUnaryExpression(expression);

    /* Allocate a register: rn for this function to use. */
    Register rn = popUnusedRegister();

    // EOR rn, rn, #1
    instructions.add(new EOR(rn, rn, new Operand2(1)));

    /* Mark the register used in the evaluation of this function as no longer in use. */
    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitNegExp(Expression expression) {

    isCalc = true;

    List<Instruction> instructions = translateUnaryExpression(expression);

    /* Allocate a register: rn for this function to use. */
    Register rn = popUnusedRegister();

    // RSBS rn, rn, #0
    instructions.add(new RSB(rn, rn, new Operand2(0), Flags.S));

    // BLVS p_throw_overflow_error
    instructions.add(new BL("p_throw_overflow_error", Conditionals.VS));

    /* Mark the register used in the evaluation of this function as no longer in use. */
    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitLenExp(Expression expression) {
    List<Instruction> instructions = translateUnaryExpression(expression);

    Register rn = popUnusedRegister();

    instructions.add(new LDR(rn, new Operand2(rn)));

    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitOrdExp(Expression expression) {
    // MOV r4, expr
    return translateUnaryExpression(expression);
  }

  @Override
  public List<Instruction> visitChrExp(Expression expression) {
    // MOV r4, expr
    return translateUnaryExpression(expression);
  }

  @Override
  public List<Instruction> visitNull(Expression expression) {
    ArrayList<Instruction> instructions = new ArrayList<>();
    Register rn = popUnusedRegister();
    instructions.add(new LDR(rn, 0));
    pushUnusedRegister(rn);
    return instructions;
  }

  private List<Instruction> translateBinaryExpression(Expression expression) {

    /* Generate assembly instructions for the first expression. */
    List<Instruction> instructions = new ArrayList<>(visitExpression(expression.getExpression1()));

    /* Declare that rn is in use. */
    Register rn = popUnusedRegister();

    /* Decides if the compiler should switch to accumulator mode*/
    if (Objects.equals(rn.toString(), "r10")){

      /* Push result of expression1 in R10 into stack */
      instructions.add(new PUSH(rn));

      /* Declare rn is free to use */
      pushUnusedRegister(rn);

      /* Generate code for expression2, this is put into register 10 */
      instructions.addAll(visitExpression(expression.getExpression2()));

      /* Declare that rn is in use. */
      rn = popUnusedRegister();

      /* Register rs is the final unused register R11 */
      Register rs = popUnusedRegister();

      /* The result of expression 1 on the stack is popped into register rs */
      instructions.add(new POP(rs));

      /* Declare that rs is no longer in use. */
      pushUnusedRegister(rs);

      /* Declare that rn is no longer in use. */
      pushUnusedRegister(rn);

      return instructions;
    }

    /* Generate assembly instructions for the second expression. */
    instructions.addAll(visitExpression(expression.getExpression2()));

    /* Declare that rn is no longer in use. */
    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitPlusExp(Expression expression) {

    isCalc = true;

    /* Generate assembly code to evaluate both expressions and store them in Rn, Rn+1. */
    List<Instruction> instructions = translateBinaryExpression(expression);

    /* Allocate two registers: rn and rm (rn+1) for this function to use. */
    Register rn = popUnusedRegister();
    Register rm = popUnusedRegister();

    // ADDS Rn, Rn, Rn+1
    instructions.add(new ADD(rn, rn, new Operand2(rm), Flags.S));

    // BLVS p_throw_overflow_error
    instructions.add(new BL("p_throw_overflow_error", Conditionals.VS));

    /* Mark the two registers used in the evaluation of this function as no longer in use. */
    pushUnusedRegister(rm);
    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitMinusExp(Expression expression) {

    isCalc = true;

    /* Generate assembly code to evaluate both expressions and store them in Rn, Rn+1. */
    List<Instruction> instructions = translateBinaryExpression(expression);

    /* Allocate two registers: rn and rm (rn+1) for this function to use. */
    Register rn = popUnusedRegister();
    Register rm = popUnusedRegister();

    // TODO: Is this a strong enough check?
    /* Swap registers for accumulator register allocation. */
    if (Objects.equals(rn.toString(), "r10")) {
      // SUBS Rn+1, Rn+1, Rn
      instructions.add(new SUB(rm, rm, new Operand2(rn), Flags.S));
    } else {
      // SUBS Rn, Rn, Rn+1
      instructions.add(new SUB(rn, rn, new Operand2(rm), Flags.S));
    }

    // BLVS p_throw_overflow_error
    instructions.add(new BL("p_throw_overflow_error", Conditionals.VS));

    /* Mark the two registers used in the evaluation of this function as no longer in use. */
    pushUnusedRegister(rm);
    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitMulExp(Expression expression) {

    isCalc = true;

    /* Generate assembly code to evaluate both expressions and store them in Rn, Rn+1. */
    List<Instruction> instructions = translateBinaryExpression(expression);

    /* Allocate two registers: rn and rm (rn+1) for this function to use. */
    Register rn = popUnusedRegister();
    Register rm = popUnusedRegister();

    // SMULL rn, rm, rn, rm
    instructions.add(new SMULL(rn, rm, rn, rm));

    // CMP Rn+1, Rn, ASR #31
    instructions.add(new LABEL("CMP " + rm + ", " + rn + ", ASR #31"));

    // BLNE p_throw_overflow_error
    instructions.add(new BL("p_throw_overflow_error", Conditionals.NE));

    /* Mark the two registers used in the evaluation of this function as no longer in use. */
    pushUnusedRegister(rm);
    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitDivExp(Expression expression) {

    //Set isDiv to True for visitProgram
    isDiv = true;

    /* Generate assembly code to evaluate both expressions and store them in Rn, Rn+1. */
    List<Instruction> instructions = translateBinaryExpression(expression);

    /* Allocate two registers: rn and rm (rn+1) for this function to use. */
    Register rn = popUnusedRegister();
    Register rm = popUnusedRegister();

    // TODO: Is this a strong enough check?
    /* Swap registers for accumulator register allocation. */
    if (Objects.equals(rn.toString(), "r10")) {
      // MOV R0, Rn+1
      instructions.add(new MOV(r0, new Operand2(rm)));

      // MOV R1, Rn
      instructions.add(new MOV(r1, new Operand2(rn)));
    }

    // MOV R0, Rn
    instructions.add(new MOV(r0, new Operand2(rn)));

    // MOV R1, Rn+1
    instructions.add(new MOV(r1, new Operand2(rm)));

    // BL p_check_divide_by_zero
    instructions.add(new BL("p_check_divide_by_zero"));

    // BL __aeabi_idiv
    instructions.add(new BL("__aeabi_idiv"));

    // MOV Rn, R0
    instructions.add(new MOV(rn, new Operand2(r0)));

    /* Mark the two registers used in the evaluation of this function as no longer in use. */
    pushUnusedRegister(rm);
    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitModExp(Expression expression) {

    //Set isDiv to True for visitProgram
    isDiv = true;

    /* Generate assembly code to evaluate both expressions and store them in Rn, Rn+1. */
    List<Instruction> instructions = translateBinaryExpression(expression);

    /* Allocate two registers: rn and rm (rn+1) for this function to use. */
    Register rn = popUnusedRegister();
    Register rm = popUnusedRegister();

    // TODO: Is this a strong enough check?
    /* Swap registers for accumulator register allocation. */
    if (Objects.equals(rn.toString(), "r10")) {
      // MOV R0, Rn+1
      instructions.add(new MOV(r0, new Operand2(rm)));

      // MOV R1, Rn
      instructions.add(new MOV(r1, new Operand2(rn)));
    } else {
      // MOV R0, Rn
      instructions.add(new MOV(r0, new Operand2(rn)));

      // MOV R1, Rn+1
      instructions.add(new MOV( r1, new Operand2(rm)));
    }

    // BL p_check_divide_by_zero
    instructions.add(new BL("p_check_divide_by_zero"));

    // BL __aeabi_idiv
    instructions.add(new BL("__aeabi_idivmod"));

    // MOV Rn, R1
    instructions.add(new MOV(rn, new Operand2(r1)));

    /* Mark the two registers used in the evaluation of this function as no longer in use. */
    pushUnusedRegister(rm);
    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitGreaterExp(Expression expression) {

    /* Generate assembly code to evaluate both expressions and store them in Rn, Rn+1. */
    List<Instruction> instructions = translateBinaryExpression(expression);

    /* Allocate two registers: rn and rm (rn+1) for this function to use. */
    Register rn = popUnusedRegister();
    Register rm = popUnusedRegister();

    // TODO: Is this a strong enough check?
    /* Swap registers for accumulator register allocation. */
    if (Objects.equals(rn.toString(), "r10")) {
      // CMP Rn+1, Rn
      instructions.add(new CMP(rm, new Operand2(rn)));
    } else {
      // CMP Rn, Rn+1
      instructions.add(new CMP(rn, new Operand2(rm)));
    }

    // MOVLE Rn, #0
    instructions.add(new MOV(rn, 0, Conditionals.LE));

    // MOVGT Rn, #1
    instructions.add(new MOV(rn, 1, Conditionals.GT));

    /* Mark the two registers used in the evaluation of this function as no longer in use. */
    pushUnusedRegister(rm);
    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitGreaterEqExp(Expression expression) {

    /* Generate assembly code to evaluate both expressions and store them in Rn, Rn+1. */
    List<Instruction> instructions = translateBinaryExpression(expression);

    /* Allocate two registers: rn and rm (rn+1) for this function to use. */
    Register rn = popUnusedRegister();
    Register rm = popUnusedRegister();

    // TODO: Is this a strong enough check?
    /* Swap registers for accumulator register allocation. */
    if (Objects.equals(rn.toString(), "r10")) {
      // CMP Rn+1, Rn
      instructions.add(new CMP(rm, new Operand2(rn)));
    } else {
      // CMP Rn, Rn+1
      instructions.add(new CMP(rn, new Operand2(rm)));
    }

    // MOVLT Rn, #0
    instructions.add(new MOV(rn, 0, Conditionals.LT));

    // MOVGE Rn, #1
    instructions.add(new MOV(rn, 1, Conditionals.GE));

    /* Mark the two registers used in the evaluation of this function as no longer in use. */
    pushUnusedRegister(rm);
    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitLessExp(Expression expression) {

    /* Generate assembly code to evaluate both expressions and store them in Rn, Rn+1. */
    List<Instruction> instructions = translateBinaryExpression(expression);

    /* Allocate two registers: rn and rm (rn+1) for this function to use. */
    Register rn = popUnusedRegister();
    Register rm = popUnusedRegister();

    // TODO: Is this a strong enough check?
    /* Swap registers for accumulator register allocation. */
    if (Objects.equals(rn.toString(), "r10")) {
      // CMP Rn+1, Rn
      instructions.add(new CMP(rm, new Operand2(rn)));
    } else {
      // CMP Rn, Rn+1
      instructions.add(new CMP(rn, new Operand2(rm)));
    }

    // MOVGE Rn, #0
    instructions.add(new MOV(rn, 0, Conditionals.GE));

    // MOVLT Rn, #1
    instructions.add(new MOV(rn, 1, Conditionals.LT));

    /* Mark the two registers used in the evaluation of this function as no longer in use. */
    pushUnusedRegister(rm);
    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitLessEqExp(Expression expression) {

    /* Generate assembly code to evaluate both expressions and store them in Rn, Rn+1. */
    List<Instruction> instructions = translateBinaryExpression(expression);

    /* Allocate two registers: rn and rm (rn+1) for this function to use. */
    Register rn = popUnusedRegister();
    Register rm = popUnusedRegister();

    // TODO: Is this a strong enough check?
    /* Swap registers for accumulator register allocation. */
    if (Objects.equals(rn.toString(), "r10")) {
      // CMP Rn+1, Rn
      instructions.add(new CMP(rm, new Operand2(rn)));
    } else {
      // CMP Rn, Rn+1
      instructions.add(new CMP(rn, new Operand2(rm)));
    }

    // MOVGT Rn, #0
    instructions.add(new MOV(rn, 0, Conditionals.GT));

    // MOVLE Rn, #1
    instructions.add(new MOV(rn, 1, Conditionals.LE));

    /* Mark the two registers used in the evaluation of this function as no longer in use. */
    pushUnusedRegister(rm);
    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitEqExp(Expression expression) {

    /* Generate assembly code to evaluate both expressions and store them in Rn, Rn+1. */
    List<Instruction> instructions = translateBinaryExpression(expression);

    /* Allocate two registers: rn and rm (rn+1) for this function to use. */
    Register rn = popUnusedRegister();
    Register rm = popUnusedRegister();

    // CMP Rn, Rn+1
    instructions.add(new CMP(rn, new Operand2(rm)));

    // MOVEQ Rn, #1
    instructions.add(new MOV(rn, 1, Conditionals.EQ));

    // MOVNE Rn, #0
    instructions.add(new MOV(rn, 0, Conditionals.NE));

    /* Mark the two registers used in the evaluation of this function as no longer in use. */
    pushUnusedRegister(rm);
    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitNeqExp(Expression expression) {

    /* Generate assembly code to evaluate both expressions and store them in Rn, Rn+1. */
    List<Instruction> instructions = translateBinaryExpression(expression);

    /* Allocate two registers: rn and rm (rn+1) for this function to use. */
    Register rn = popUnusedRegister();
    Register rm = popUnusedRegister();

    // CMP Rn, Rn+1
    instructions.add(new CMP(rn, new Operand2(rm)));

    // MOVNE Rn, #1
    instructions.add(new MOV(rn, 1, Conditionals.NE));

    // MOVEQ Rn, #0
    instructions.add(new MOV(rn, 0, Conditionals.EQ));

    /* Mark the two registers used in the evaluation of this function as no longer in use. */
    pushUnusedRegister(rm);
    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitAndExp(Expression expression) {

    /* Generate assembly code to evaluate both expressions and store them in Rn, Rn+1. */
    List<Instruction> instructions = translateBinaryExpression(expression);

    /* Allocate two registers: rn and rm (rn+1) for this function to use. */
    Register rn = popUnusedRegister();
    Register rm = popUnusedRegister();

    // AND r4, r4, r5
    instructions.add(new LABEL(String.format("AND %s, %s, %s", rn, rn, rm)));

    /* Mark the two registers used in the evaluation of this function as no longer in use. */
    pushUnusedRegister(rm);
    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitOrExp(Expression expression) {

    /* Generate assembly code to evaluate both expressions and store them in Rn, Rn+1. */
    List<Instruction> instructions = translateBinaryExpression(expression);

    /* Allocate two registers: rn and rm (rn+1) for this function to use. */
    Register rn = popUnusedRegister();
    Register rm = popUnusedRegister();

    // AND r4, r4, r5
    instructions.add(new LABEL(String.format("ORR %s, %s, %s", rn, rn, rm)));

    /* Mark the two registers used in the evaluation of this function as no longer in use. */
    pushUnusedRegister(rm);
    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitExprRHS(AssignRHS rhs) {
    return visitExpression(rhs.getExpression1());
  }

  @Override
  public List<Instruction> visitArrayRHS(AssignRHS rhs) {

    List<Expression> array = rhs.getArray();
    List<Instruction> instructions = new ArrayList<>();

    int mallocSize = 4;
    int typeSize = 0;

    /* If array is not empty get stack size of type. */
    if(!array.isEmpty()) {
      typeSize = sizeOfTypeOnStack(getExpressionType(array.get(0)));
    }

    for (int i = 0; i < array.size(); i++) {
      mallocSize += typeSize;
    }

    // LDR r0, =mallocSize
    instructions.add(new LDR(r0, mallocSize));

    // BL malloc
    instructions.add(new BL("malloc"));

    /* Allocate one register: rn for this function to use. */
    Register rn = popUnusedRegister();

    // MOV rn, r0
    instructions.add(new MOV(rn, new Operand2(r0)));

    int offset = 4;
    for (Expression expression : array) {

      /* Generate instructions to evaluate each expression. */
      instructions.addAll(visitExpression(expression));

      /* Retrieve the register containing the evaluated expression. */
      Register rm = popUnusedRegister();

      /* Store the evaluated expression into the malloc location at an offset. */
      if (typeSize == 1) {
        instructions.add(new LABEL(String.format("STRB %s, [%s, #%d]", rm, rn, offset)));
      } else {
        instructions.add(new LABEL(String.format("STR %s, [%s, #%d]", rm, rn, offset)));
      }

      /* Mark register rm as no longer in use. */
      pushUnusedRegister(rm);

      offset += typeSize;
    }

    /* Allocate a register: rm for this function to use. */
    Register rm = popUnusedRegister();

    // LDR rm, =array.size()
    instructions.add(new LDR(rm, array.size()));

    // STR r5, [r4]
    instructions.add(new STR(rn, new Operand2(rm)));

    /* Mark the two registers used in the evaluation of this function as no longer in use. */
    pushUnusedRegister(rm);
    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitNewPairRHS(AssignRHS rhs) {

    checkNullPointer = true;

    List<Instruction> instructions = new ArrayList<>();
    Register rn, rm;

    // LDR r0, =8
    /* Loads the value 8 into register 0, as every pair is 8 bytes on the heap. */
    instructions.add(new LDR(r0, 8));

    // BL malloc
    instructions.add(new BL("malloc"));

    /* Allocate one register: rn for this function to use. */
    rn = popUnusedRegister();

    // MOV rn, r0
    instructions.add(new MOV(rn, new Operand2(r0)));

    /* Evaluate the first expression in the pair. */
    instructions.addAll(visitExpression(rhs.getExpression1()));


    /* Retrieve the register containing the value of the first expression. */
    rm = popUnusedRegister();

    int sizeOfExp1 = sizeOfTypeOnStack(getExpressionType(rhs.getExpression1()));
    int sizeOfExp2 = sizeOfTypeOnStack(getExpressionType(rhs.getExpression2()));

    // LDR r0, =sizeOfType
    instructions.add(new LDR(r0, sizeOfExp1));

    // BL malloc
    instructions.add(new BL("malloc"));

    if (sizeOfExp1 == 1) {
      // STRB rm, [r0]
      instructions.add(new STR(r0, new Operand2(rm), "B"));
    } else {
      // STR rm, [r0]
      instructions.add(new STR(r0, new Operand2(rm)));
    }

    // STR r0, [rn]
    instructions.add(new STR(rn, new Operand2(r0)));

    /* Mark the register rm as no longer in use. */
    pushUnusedRegister(rm);

    /* Evaluate the second expression in the pair. */
    instructions.addAll(visitExpression(rhs.getExpression2()));

    /* Retrieve the register containing the value of the first expression. */
    rm = popUnusedRegister();

    // LDR r0, =sizeOfType
    instructions.add(new LDR(r0, sizeOfTypeOnStack(getExpressionType(rhs.getExpression2()))));

    // BL malloc
    instructions.add(new BL("malloc"));

    if (sizeOfExp2 == 1) {
      // STRB rm, [r0]
      instructions.add(new STR(r0, new Operand2(rm), "B"));
    } else {
      // STR rm, [r0]
      instructions.add(new STR(r0, new Operand2(rm)));
    }

    // STR r0, [rn, #4]
    instructions.add(new LABEL(String.format("STR r0, [%s, #4]", rn)));

    /* Mark the two registers used in the evaluation of this function as no longer in use. */
    pushUnusedRegister(rm);
    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitPairElemRHS(AssignRHS rhs) {

    PairElem pairElem = rhs.getPairElem();

    /* Place pairElem inside a new AssignLHS class. */
    AssignLHS assignLHS = new AssignLHSBuilder().buildPairLHS(pairElem);

    /* Calculate the address of the pair elem and store it in the first unused register.
       Uses our already defined visitPairElemLHS function to reduce duplication. */
    List<Instruction> instructions = new ArrayList<>(visitLHS(assignLHS));

    /* Retrieve first unused register. */
    Register rn = popUnusedRegister();

    Type type = getExpressionType(pairElem.getExpression());

    if (pairElem.getType() == PairElem.PairElemType.FST) {
      type = type.getFstType();
    } else {
      type = type.getSndType();
    }

    if (sizeOfTypeOnStack(type) == 1) {
      // LDRSB rn, [rn]
      instructions.add(new LABEL(String.format("LDRSB %s, [%s]", rn, rn)));
    } else {
      // LDR rn, [rn]
      instructions.add(new LDR(rn, new Operand2(rn)));
    }

    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitCallRHS(AssignRHS rhs) {

    List<Instruction> instructions = new ArrayList<>();
    int totalSize = 0;

    for (int i = rhs.getArgList().size() - 1; i >= 0; i--) {

      Expression expression = rhs.getArgList().get(i);

      /* Evaluate argument and store in first unused register. */
      instructions.addAll(visitExpression(expression));

      /* Retrieve the first unused register. */
      Register rn = popUnusedRegister();

      int expSize = sizeOfTypeOnStack(getExpressionType(expression));
      if (expSize > 1) {
        instructions.add(new LABEL(String.format("STR %s, [sp, #-%d]!", rn, expSize)));
      } else {
        instructions.add(new LABEL(String.format("STRB %s, [sp, #-%d]!", rn, expSize)));
      }

      totalSize += expSize;
      currentST.incrementOffset(expSize);

      /* Mark register rn as no longer in use. */
      pushUnusedRegister(rn);
    }

    currentST.resetOffset();

    // BL f_functionIdentity
    instructions.add(new BL(rhs.getFunctionIdent()));

    // ADD sp, sp, #totalSize
    instructions.add(new LABEL("ADD sp, sp, #" + totalSize));

    /* Retrieve the first unused register. */
    Register rn = popUnusedRegister();

    // MOV rn, r0
    instructions.add(new MOV(rn, new Operand2(r0)));

    /* Mark register rn as no longer in use. */
    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitReadStatement(Statement statement) {

    /* Generate code to retrieve the address of the LHS. */
    List<Instruction> instructions = new ArrayList<>(visitLHS(statement.getLHS()));

    /* Retrieve the first unused register. */
    Register rn = popUnusedRegister();

    // MOV r0, rn
    instructions.add(new MOV(r0, new Operand2(rn)));

    Type type = currentST.getType(getIdentFromLHS(statement.getLHS()));
    EType eType;

    /* Find the EType of the LHS by using the AssignType. */
    if (statement.getLHS().getAssignType() == AssignLHS.LHSType.IDENT) {
      eType = type.getType();
    } else if (statement.getLHS().getAssignType() == AssignLHS.LHSType.ARRAYELEM) {
      while (type.getType() == ARRAY) {
        type = type.getArrayType();
      }
      eType = type.getType();
    } else {
      if (statement.getLHS().getPairElem().getType() == PairElem.PairElemType.FST) {
        eType = type.getFstType().getType();
      } else {
        eType = type.getSndType().getType();
      }
    }

    /* Use int/char instruction depending on the type of the LHS. */
    if (eType == INT) {
      // BL p_read_int
      instructions.add(new BL("p_read_int"));
      hasReadInt = true;
    } else if (eType == CHAR) {
      // BL p_read_char
      instructions.add(new BL("p_read_char"));
      hasReadChar = true;
    }

    /* Mark register rn as no longer in use. */
    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitFreeStatement(Statement statement) {

    /* Evaluate the expression to be printed and store the result in the first unused register. */
    List<Instruction> instructions = new ArrayList<>(visitExpression(statement.getExpression()));

    /* Retrieve the first unused register. */
    Register rn = popUnusedRegister();

    // MOV r0, rn
    instructions.add(new MOV(r0, new Operand2(rn)));
    // BL p_free_pair
    instructions.add(new BL("p_free_pair"));

    /* Mark register rn as no longer in use. */
    pushUnusedRegister(rn);

    hasFreePair = true;

    return instructions;
  }

  @Override
  public List<Instruction> visitExitStatement(Statement statement) {
    //TODO: check if need exit code?
    List<Instruction> instructions = new ArrayList<>();
    long exitCode = 0;

    /* Retrieve the first unused register. */
    Register rn = popUnusedRegister();

    if (statement.getExpression().getIdent() == null) {
      exitCode = statement.getExpression().getIntLiter();

      // LDR rn, =exitCode
      instructions.add(new LDR(rn, exitCode));

      // MOV r0, rn
      instructions.add(new MOV(r0, new Operand2(rn)));

    } else {
      exitCode = currentST.getSPMapping(statement.getExpression().getIdent());

      // LDR rn, [sp]
      instructions.add(new LDR(rn, new Operand2(sp)));

      // MOV r0, rn
      instructions.add(new MOV(r0, new Operand2(rn)));
    }
    // BL exit
    instructions.add(new BL("exit"));

    /* Mark register rn as no longer in use. */
    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitPrintStatement(Statement statement) {

    /* Evaluate the expression to be printed and store the result in the first unused register. */
    List<Instruction> instructions = new ArrayList<>(visitExpression(statement.getExpression()));

    /* Retrieve the first unused register. */
    Register rn = popUnusedRegister();

    // MOV r0, rn
    instructions.add(new MOV(r0, new Operand2(rn)));

    // Type of expression is stored
    Type type = getExpressionType(statement.getExpression());

    if (Objects.equals(type, new Type(INT))) {
      // BL p_print_int
      instructions.add(new BL("p_print_int"));
      hasPrintInt = true;
    } else if (Objects.equals(type, new Type(STRING)) || Objects.equals(type, new Type(ARRAY, new Type(CHAR)))) {
      // BL p_print_string
      instructions.add(new BL("p_print_string"));
      hasPrintString = true;
    } else if (Objects.equals(type, new Type(BOOL))) {
      // BL p_print_bool
      instructions.add(new BL("p_print_bool"));
      hasPrintBool = true;
    } else if (Objects.equals(type, new Type(CHAR))) {
      // BL putchar
      instructions.add(new BL("putchar"));
    } else {
      // For printing arrays and pairs
      // BL p_print_reference
      instructions.add(new BL("p_print_reference"));
      hasPrintReference = true;
    }

    //TODO: check how much to add to rn after each branch link (happens when multiple print statements)

    /* Mark register rn as no longer in use. */
    pushUnusedRegister(rn);

    return instructions;
  }

  @Override
  public List<Instruction> visitPrintlnStatement(Statement statement) {

    List<Instruction> instructions = new ArrayList<>(visitPrintStatement(statement));

    // BL p_print_ln
    instructions.add(new BL("p_print_ln"));
    hasPrintLn = true;

    return instructions;
  }

  @Override
  public List<Instruction> visitPairElemLHS(AssignLHS lhs) {

    checkNullPointer = true;

    PairElem pairElem = lhs.getPairElem();

    /* Calculate address of pair and store it in rn. */
    List<Instruction> instructions = new ArrayList<>(visitExpression(pairElem.getExpression()));

    /* Allocate one register: rn for this function to use. */
    Register rn = popUnusedRegister();

    // MOV r0, rn
    instructions.add(new MOV(r0, new Operand2(rn)));

    // BL p_check_null_pointer
    instructions.add(new BL("p_check_null_pointer"));

    if (pairElem.getType() == PairElem.PairElemType.FST) {
      // LDR rn, [rn]
      instructions.add(new LDR(rn, new Operand2(rn)));
    } else {
      // LDR rn, [rn, #4]
      instructions.add(new LABEL(String.format("LDR %s, [%s, #4]", rn, rn)));
    }

    pushUnusedRegister(rn);

    return instructions;
  }
}
