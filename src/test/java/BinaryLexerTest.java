import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BinaryLexerTest {

  @Test
  public void addition_test1() {

    String treeResult =
        "(prog begin (stat (type (baseType int)) x = " + "(assignRHS (expr (expr (intLiter 3)) + "
            + "(expr (intLiter 5))))) end <EOF>)";
    String program = "begin\n" + "int x = 3 + 5\n" + "end";

    assertEquals(treeResult, Compiler.lexAnalyse(program));
  }

  @Test
  public void addition_test2() {

    String treeResult = "(prog begin (stat (type (baseType int)) x = "
        + "(assignRHS (expr (expr (expr (intLiter 5)) + (expr (intLiter 4))) "
        + "+ (expr (intLiter 2))))) end <EOF>)";
    String program = "begin\n" + "int x = 5 + 4 + 2\n" + "end";

    assertEquals(treeResult, Compiler.lexAnalyse(program));
  }

  @Test
  public void subtraction_test1() {

    String treeResult =
        "(prog begin (stat (type (baseType int)) x = " + "(assignRHS (expr (expr (intLiter 5)) - "
            + "(expr (intLiter 2))))) end <EOF>)";
    String program = "begin\n" + "int x = 5 - 2\n" + "end";

    assertEquals(treeResult, Compiler.lexAnalyse(program));
  }

  @Test
  public void subtraction_test2() {

    String treeResult = "(prog begin (stat (type (baseType int)) x = "
        + "(assignRHS (expr (expr (expr (intLiter 9)) " + "- (expr (intLiter 4))) - "
        + "(expr (intLiter 3))))) end <EOF>)";
    String program = "begin\n" + "int x = 9 - 4 - 3\n" + "end";

    assertEquals(treeResult, Compiler.lexAnalyse(program));
  }

  @Test
  public void multiplication_test1() {

    String treeResult =
        "(prog begin (stat (type (baseType int)) x = " + "(assignRHS (expr (expr (intLiter 2)) * "
            + "(expr (intLiter 6))))) end <EOF>)";
    String program = "begin\n" + "int x = 2 * 6\n" + "end";

    assertEquals(treeResult, Compiler.lexAnalyse(program));
  }

  @Test
  public void multiplication_test2() {

    String treeResult = "(prog begin (stat (type (baseType int)) x = "
        + "(assignRHS (expr (expr (expr (intLiter 5)) * "
        + "(expr (intLiter 3))) * (expr (intLiter 4))))) " + "end <EOF>)";
    String program = "begin\n" + "int x = 5 * 3 * 4\n" + "end";

    assertEquals(treeResult, Compiler.lexAnalyse(program));
  }


  @Test
  public void division_test1() {

    String treeResult =
        "(prog begin (stat (type (baseType int)) x = " + "(assignRHS (expr (expr (intLiter 8)) / "
            + "(expr (intLiter 4))))) end <EOF>)";
    String program = "begin\n" + "int x = 8 / 4\n" + "end";

    assertEquals(treeResult, Compiler.lexAnalyse(program));
  }

  @Test
  public void division_test2() {

    String treeResult = "(prog begin (stat (type (baseType int)) x = "
        + "(assignRHS (expr (expr (expr (intLiter 8)) / "
        + "(expr (intLiter 4))) / (expr (intLiter 2))))) " + "end <EOF>)";
    String program = "begin\n" + "int x = 8 / 4 / 2\n" + "end";

    assertEquals(treeResult, Compiler.lexAnalyse(program));
  }

  @Test
  public void modulo_test1() {

    String treeResult =
        "(prog begin (stat (type (baseType int)) x = " + "(assignRHS (expr (expr (intLiter 7)) % "
            + "(expr (intLiter 3))))) end <EOF>)";
    String program = "begin\n" + "int x = 7 % 3\n" + "end";

    assertEquals(treeResult, Compiler.lexAnalyse(program));
  }

  @Test
  public void modulo_test2() {

    String treeResult = "(prog begin (stat (type (baseType int)) x = "
        + "(assignRHS (expr (expr (expr (intLiter 200)) % "
        + "(expr (intLiter 3))) % (expr (intLiter 5))))) " + "end <EOF>)";
    String program = "begin\n" + "int x = 200 % 3 % 5\n" + "end";

    assertEquals(treeResult, Compiler.lexAnalyse(program));
  }

  @Test
  public void greater_than_test1() {

    String treeResult =
        "(prog begin (stat (type (baseType bool)) x = " + "(assignRHS (expr (expr (intLiter 9)) > "
            + "(expr (intLiter 8))))) end <EOF>)";
    String program = "begin\n" + "bool x = 9 > 8\n" + "end";

    assertEquals(treeResult, Compiler.lexAnalyse(program));
  }

  @Test
  public void greater_than_test2() {

    String treeResult =
        "(prog begin (stat (type (baseType bool)) x = " + "(assignRHS (expr (expr (intLiter 1)) > "
            + "(expr (intLiter 2))))) end <EOF>)";
    String program = "begin\n" + "bool x = 1 > 2\n" + "end";

    assertEquals(treeResult, Compiler.lexAnalyse(program));
  }

  @Test
  public void greater_than_or_equal_test1() {

    String treeResult =
        "(prog begin (stat (type (baseType bool)) x = " + "(assignRHS (expr (expr (intLiter 5)) >= "
            + "(expr (intLiter 5))))) end <EOF>)";
    String program = "begin\n" + "bool x = 5 >= 5\n" + "end";

    assertEquals(treeResult, Compiler.lexAnalyse(program));
  }

  @Test
  public void greater_than_or_equal_test2() {

    String treeResult =
        "(prog begin (stat (type (baseType bool)) x = " + "(assignRHS (expr (expr (intLiter 4)) >= "
            + "(expr (intLiter 5))))) end <EOF>)";
    String program = "begin\n" + "bool x = 4 >= 5\n" + "end";

    assertEquals(treeResult, Compiler.lexAnalyse(program));
  }

  @Test
  public void less_than_test1() {

    String treeResult =
        "(prog begin (stat (type (baseType bool)) x = " + "(assignRHS (expr (expr (intLiter 6)) < "
            + "(expr (intLiter 9))))) end <EOF>)";
    String program = "begin\n" + "bool x = 6 < 9\n" + "end";

    assertEquals(treeResult, Compiler.lexAnalyse(program));
  }

  @Test
  public void less_than_test2() {

    String treeResult =
        "(prog begin (stat (type (baseType bool)) x = " + "(assignRHS (expr (expr (intLiter 10)) < "
            + "(expr (intLiter 9))))) end <EOF>)";
    String program = "begin\n" + "bool x = 10 < 9\n" + "end";

    assertEquals(treeResult, Compiler.lexAnalyse(program));
  }

  @Test
  public void less_than_or_equal_test1() {

    String treeResult =
        "(prog begin (stat (type (baseType bool)) x = " + "(assignRHS (expr (expr (intLiter 3)) <= "
            + "(expr (intLiter 4))))) end <EOF>)";
    String program = "begin\n" + "bool x = 3 <= 4\n" + "end";

    assertEquals(treeResult, Compiler.lexAnalyse(program));
  }

  @Test
  public void less_than_or_equal_test2() {

    String treeResult =
        "(prog begin (stat (type (baseType bool)) x = " + "(assignRHS (expr (expr (intLiter 9)) <= "
            + "(expr (intLiter 4))))) end <EOF>)";
    String program = "begin\n" + "bool x = 9 <= 4\n" + "end";

    assertEquals(treeResult, Compiler.lexAnalyse(program));
  }

  @Test
  public void equal_test1() {

    String treeResult =
        "(prog begin (stat (type (baseType bool)) x = " + "(assignRHS (expr (expr (intLiter 4)) == "
            + "(expr (intLiter 4))))) end <EOF>)";
    String program = "begin\n" + "bool x = 4 == 4\n" + "end\n";

    assertEquals(treeResult, Compiler.lexAnalyse(program));
  }

  @Test
  public void equal_test2() {

    String treeResult =
        "(prog begin (stat (type (baseType bool)) x = " + "(assignRHS (expr (expr (intLiter 2)) == "
            + "(expr (intLiter 1))))) end <EOF>)";
    String program = "begin\n" + "bool x = 2 == 1\n" + "end";

    assertEquals(treeResult, Compiler.lexAnalyse(program));
  }

  @Test
  public void not_equal_test1() {

    String treeResult =
        "(prog begin (stat (type (baseType bool)) x = " + "(assignRHS (expr (expr (intLiter 1)) != "
            + "(expr (intLiter 2))))) end <EOF>)";
    String program = "begin\n" + "bool x = 1 != 2\n" + "end";

    assertEquals(treeResult, Compiler.lexAnalyse(program));
  }

  @Test
  public void not_equal_test2() {

    String treeResult =
        "(prog begin (stat (type (baseType bool)) x = " + "(assignRHS (expr (expr (intLiter 2)) != "
            + "(expr (intLiter 2))))) end <EOF>)";
    String program = "begin\n" + "bool x = 2 != 2\n" + "end";

    assertEquals(treeResult, Compiler.lexAnalyse(program));
  }

}
