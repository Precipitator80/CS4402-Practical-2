import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

// Number of solutions for each n: https://oeis.org/A000170 - Accessed 08.11.2023
public final class QueensGenerator {
  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("Usage: java QueensGenerator <n>");
      return;
    }
    int n = Integer.parseInt(args[0]);

    StringBuilder stringBuilder = new StringBuilder();

    stringBuilder.append("//" + n + "-Queens.\n");
    stringBuilder.append("\n// Number of variables:\n" + n + "\n");
    stringBuilder.append("\n// Domains of the variables: 0.. (inclusive)\n");
    for (int i = 0; i < n; i++)
      stringBuilder.append("0, " + (n - 1) + "\n");
    stringBuilder.append("\n// constraints (vars indexed from 0, allowed tuples):\n");

    for (int row1 = 0; row1 < n - 1; row1++)
      for (int row2 = row1 + 1; row2 < n; row2++) {
        stringBuilder.append("c(" + row1 + ", " + row2 + ")\n");
        for (int col1 = 0; col1 < n; col1++)
          for (int col2 = 0; col2 < n; col2++) {
            if ((col1 != col2) &&
                (Math.abs(col1 - col2) != (row2 - row1))) {
              stringBuilder.append(col1 + ", " + col2 + "\n");
            }
          }
        stringBuilder.append('\n');
      }

    String filePath = "instances/" + n + "Queens.csp";
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
      writer.write(stringBuilder.toString());
    } catch (IOException e) {
      System.out.println("Could not write file to " + filePath + "\n" + e.toString());
    }
  }
}
