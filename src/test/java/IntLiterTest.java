import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class IntLiterTest {

  @Test
  public void numberWithoutSignTokenisedAndParsedCorrectly() {

    String treeResult = "(prog begin (stat (type (baseType int)) x = "
        + "(assignRHS (expr (intLiter 5)))) end <EOF>)";
    String program = "begin\n" + "int x = 5\n" + "end";

    assertEquals(treeResult, Compiler.lexAnalyse(program));
  }

  @Test
  public void numberWithNegativeSignTokenisedAndParsedCorrectly() {

    String treeResult = "(prog begin (stat (type (baseType int)) x = "
        + "(assignRHS (expr (signedIntLiter - 5)))) end <EOF>)";
    String program = "begin\n" + "int x = -5\n" + "end";

    assertEquals(treeResult, Compiler.lexAnalyse(program));
  }

  @Test
  public void numberWithPositiveSignTokenisedAndParsedCorrectly() {

    String treeResult = "(prog begin (stat (type (baseType int)) x = "
        + "(assignRHS (expr (signedIntLiter + 5)))) end <EOF>)";
    String program = "begin\n" + "int x = +5\n" + "end";

    assertEquals(treeResult, Compiler.lexAnalyse(program));
  }

}
