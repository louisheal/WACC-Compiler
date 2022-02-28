import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import org.junit.Test;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Arrays;

public class EmulatorValidTests {
  //TODO: Add assert fail in correct spaces

  MySecurityManager securityManager = new MySecurityManager();

  int failedTests = 0;
  int totalTests = 0;

  // Will ensure that the files listed are not folders
  FileFilter folderFilter = pathname -> !pathname.isDirectory();

  public void runTests(File[] files) throws FileNotFoundException {
    for (File file : files) {
      totalTests++;
      String expectedOutput = scan(file);
      //TODO: extract actual output from executing assembly file
      String actualOutput = "";

      System.out.print("RUNNING " + file.getName() + ": ");
      if (actualOutput.equals(expectedOutput)) {
        System.out.println("PASS");
      } else {
        failedTests++;
        System.out.println("FAIL");
      }
      System.out.println("EXPECTED OUTPUT:");
      System.out.print(expectedOutput);
      System.out.println("ACTUAL OUTPUT:\n");
      System.out.print(actualOutput);
    }
  }

  public String scan(File file) throws FileNotFoundException {
    final Scanner scanner = new Scanner(file);
    StringBuilder output = new StringBuilder();
    while (scanner.hasNextLine()) {
      final String lineFromFile = scanner.nextLine();
      if (lineFromFile.contains("Output")) {
        String nextLine = scanner.nextLine();
        // Keeps appending the outputs until there is a gap which is where output stops
        while (!nextLine.equals("")) {
          output.append(nextLine.substring(1).strip()).append("\n");
          nextLine = scanner.nextLine();
        }
      }
    }
    return output.toString();
  }

  public String extractFromAssembly(File file) throws IOException {

    //Executes the commands neccessary to receieve the full output of assembly emulator of a .s file
    String[] commands = {"sh", "-c", "echo ' ' | ./wacc_examples/refEmulate " + file.getName()};
    String s;
    StringBuilder sb = new StringBuilder();
    Process p = Runtime.getRuntime().exec(commands);
    BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
    while (((s = br.readLine()) != null)) {
      sb.append(s).append("\n");
    }

    //Extracts all lines after 'Emulation Output'
    BufferedReader bufferedReader = new BufferedReader(new StringReader(sb.toString()));
    String line = null;
    StringBuilder outputExtracted = new StringBuilder();

    while ((line = bufferedReader.readLine()) != null) {
      if (line.contains("Emulation Output")) {
        while ((line = bufferedReader.readLine()) != null) {
          outputExtracted.append(line).append("\n");
      }
      break;
    }
  }

  //Removes the last 2 lines which are "------" and exit code
  String[] lines = outputExtracted.toString().split("\n");
  String[] linesWithoutExit = Arrays.copyOf(lines, lines.length - 2);

  StringBuilder arrayToString = new StringBuilder();
  for (int i = 0; i < linesWithoutExit.length; i++) {
    arrayToString.append(linesWithoutExit[i]);
    if (i != linesWithoutExit.length - 1) {
      arrayToString.append("\n");
    }
  }

  return arrayToString.toString();
  }

  @Test
  public void advancedTests() throws IOException {
    System.setSecurityManager(securityManager);

    File directory = new File("wacc_examples/valid/advanced/");
    File[] examples = directory.listFiles();

  }

  @Test
  public void arrayTests() throws IOException {
    System.setSecurityManager(securityManager);

    File directory = new File("wacc_examples/valid/array/");
    File[] examples = directory.listFiles();

    assert examples != null;
    runTests(examples);
  }

  @Test
  public void basicExitTests() throws IOException {
    System.setSecurityManager(securityManager);

    File directory = new File("wacc_examples/valid/basic/exit/");
    File[] examples = directory.listFiles();

    assert examples != null;
    runTests(examples);
  }

  @Test
  public void basicSkipTests() throws IOException {
    System.setSecurityManager(securityManager);

    File directory = new File("wacc_examples/valid/basic/skip/");
    File[] examples = directory.listFiles();

    assert examples != null;
    runTests(examples);
  }

  @Test
  public void expressionsTests() throws IOException {
    System.setSecurityManager(securityManager);

    File directory = new File("wacc_examples/valid/expressions/");
    File[] examples = directory.listFiles();

    System.out.println("Hello");


  }

  @Test
  public void nestedFunctionTests() throws IOException {
    System.setSecurityManager(securityManager);

    File directory = new File("wacc_examples/valid/function/nested_functions/");
    File[] examples = directory.listFiles();

    assert examples != null;
    runTests(examples);
  }

  @Test
  public void simpleFunctionTests() throws IOException {
    System.setSecurityManager(securityManager);

    File directory = new File("wacc_examples/valid/function/simple_functions/");
    File[] examples = directory.listFiles();

    assert examples != null;
    runTests(examples);
  }

  @Test
  public void ifTests() throws IOException {
    System.setSecurityManager(securityManager);

    File directory = new File("wacc_examples/valid/if/");
    File[] examples = directory.listFiles();

    assert examples != null;
    runTests(examples);
  }

  @Test
  public void IOTests() throws IOException {
    System.setSecurityManager(securityManager);

    File directory = new File("wacc_examples/valid/IO/");
    File[] examples = directory.listFiles(folderFilter);

    assert examples != null;
    runTests(examples);
  }

  @Test
  public void IOPrintTests() throws IOException {
    System.setSecurityManager(securityManager);


    File directory = new File("wacc_examples/valid/IO/print");
    File[] examples = directory.listFiles();

    assert examples != null;
    runTests(examples);
  }

  @Test
  public void IOReadTests() throws IOException {
    System.setSecurityManager(securityManager);


    File directory = new File("wacc_examples/valid/IO/read");
    File[] examples = directory.listFiles();

    assert examples != null;
    runTests(examples);
  }

  @Test
  public void pairsTests() throws IOException {
    System.setSecurityManager(securityManager);

    File directory = new File("wacc_examples/valid/pairs/");
    File[] examples = directory.listFiles();

    assert examples != null;
    runTests(examples);
  }



  @Test
  public void runtimeErrorOutOfBoundsTests() throws IOException {
    System.setSecurityManager(securityManager);

    File directory = new File("wacc_examples/valid/runtimeErr/arrayOutOfBounds/");
    File[] examples = directory.listFiles();

    assert examples != null;
    runTests(examples);
  }


  @Test
  public void runtimeErrorDivideByZeroTests() throws IOException {
    System.setSecurityManager(securityManager);

    File directory = new File("wacc_examples/valid/runtimeErr/divideByZero/");
    File[] examples = directory.listFiles();

    assert examples != null;
    runTests(examples);
  }


  @Test
  public void runtimeErrorIntegerOverflowTests() throws IOException {
    System.setSecurityManager(securityManager);

    File directory = new File("wacc_examples/valid/runtimeErr/integerOverflow/");
    File[] examples = directory.listFiles();

    assert examples != null;
    runTests(examples);
  }


  @Test
  public void runtimeErrorNullDereferenceTests() throws IOException {
    System.setSecurityManager(securityManager);

    File directory = new File("wacc_examples/valid/runtimeErr/nullDereference/");
    File[] examples = directory.listFiles();

    assert examples != null;
    runTests(examples);
  }

  @Test
  public void scopeTests() throws IOException {
    System.setSecurityManager(securityManager);

    File directory = new File("wacc_examples/valid/scope/");
    File[] examples = directory.listFiles();

    assert examples != null;
    runTests(examples);
  }

  @Test
  public void sequenceTests() throws IOException {
    System.setSecurityManager(securityManager);

    File directory = new File("wacc_examples/valid/sequence/");
    File[] examples = directory.listFiles();

    assert examples != null;
    runTests(examples);
  }

  @Test
  public void variablesTests() throws IOException {
    System.setSecurityManager(securityManager);

    File directory = new File("wacc_examples/valid/variables/");
    File[] examples = directory.listFiles();

    assert examples != null;
    runTests(examples);
  }

  @Test
  public void whileTests() throws IOException {
    System.setSecurityManager(securityManager);

    File directory = new File("wacc_examples/valid/while/");
    File[] examples = directory.listFiles();

    assert examples != null;
    runTests(examples);
  }
}