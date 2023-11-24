import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public final class SudokuGenerator {

  /**
   * The constraint is always the same != on 1..9 Only the
   */
  private static void diseqTuples() {
    for (int val1 = 1; val1 <= 9; val1++)
      for (int val2 = 1; val2 <= 9; val2++)
        if (val1 != val2)
          stringBuilder.append(val1 + ", " + val2 + "\n");
  }

  static StringBuilder stringBuilder;

  public static void main(String[] args) {
    if (args.length != 0) {
      System.out.println("Usage: java SudokuGenerator");
      return;
    }

    stringBuilder = new StringBuilder();

    stringBuilder.append("//Sudoku.\n");
    stringBuilder.append("\n// Always 81 variables:\n" + 81 + "\n");
    stringBuilder.append("\n// Domains of the variables: 1..9 (inclusive)\n");
    stringBuilder.append("\n// Edit the following to provide clues\n");
    for (int row = 1; row <= 9; row++) {
      stringBuilder.append("//Row: " + row + "\n");
      for (int col = 1; col <= 9; col++) {
        stringBuilder.append("1, 9\n");
      }
    }
    stringBuilder.append("\n// constraints (vars indexed from 0, allowed tuples):\n");

    // Rows
    for (int row = 1; row <= 9; row++) {
      stringBuilder.append("//Row: " + row + "\n");
      for (int col1 = 1; col1 <= 8; col1++)
        for (int col2 = col1 + 1; col2 <= 9; col2++) {
          stringBuilder.append("c(" + ((row - 1) * 9 + col1 - 1) + ", " + ((row - 1) * 9 + col2 - 1) + ")\n");
          diseqTuples();
          stringBuilder.append('\n');
        }
    }

    // Cols
    for (int col = 1; col <= 9; col++) {
      stringBuilder.append("//Col: " + col + "\n");
      for (int row1 = 1; row1 <= 8; row1++)
        for (int row2 = row1 + 1; row2 <= 9; row2++) {
          stringBuilder.append("c(" + ((row1 - 1) * 9 + col - 1) + ", " + ((row2 - 1) * 9 + col - 1) + ")\n");
          diseqTuples();
          stringBuilder.append('\n');
        }
    }

    // 3 x 3 subsquares

    for (int subRow = 1; subRow <= 7; subRow += 3)
      for (int subCol = 1; subCol <= 7; subCol += 3) {
        stringBuilder.append("//Subsquare starting at row: " + subRow + ", col: " + subCol + "\n");
        for (int row1 = subRow; row1 <= subRow + 2; row1++)
          for (int col1 = subCol; col1 <= subCol + 2; col1++)
            for (int row2 = row1; row2 <= subRow + 2; row2++)
              for (int col2 = subCol; col2 <= subCol + 2; col2++) {
                // break symmetry: only allow cell1 != cell2 where cell1 is less than cell2 in
                //   the row-wise ordering of the subsquare
                if ((row2 > row1) || (col2 > col1)) {
                  stringBuilder.append("c(" + ((row1 - 1) * 9 + col1 - 1) + ", " + ((row2 - 1) * 9 + col2 - 1) + ")\n");
                  diseqTuples();
                  stringBuilder.append('\n');
                }
              }
      }

    String filePath = "instances/blank_sudoku.csp";
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
      writer.write(stringBuilder.toString());
    } catch (IOException e) {
      System.out.println("Could not write file to " + filePath + "\n" + e.toString());
    }
  }
}
