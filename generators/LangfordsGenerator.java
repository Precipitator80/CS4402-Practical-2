import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public final class LangfordsGenerator {
  // for k = 2, n must be 4m or 4m-1.
  // k = Number of times to have full range of numbers.
  // n = Domain range of numbers.
  // Langford's Sequences -  - http://datagenetics.com/blog/october32014/index.html - Accessed 23.11.2023
  public static void main(String[] args) {
    if (args.length != 2) {
      System.out.println("Usage: java LangfordsGenerator <k> <n>");
      System.out.println("for <k> sets of <n> integers");
      return;
    }

    StringBuilder stringBuilder = new StringBuilder();

    int k = Integer.parseInt(args[0]);
    int n = Integer.parseInt(args[1]);
    stringBuilder.append("//Langford, k = " + k + " n = " + n + "\n");

    int seqLength = k * n;
    stringBuilder.append("\n// Number of variables:\n" + seqLength + "\n");
    stringBuilder.append("\n// Domains of the variables: 1.. (inclusive)\n");
    // Variables are organised in n blocks of k, representing pos of each of the n ints
    for (int i = 0; i < seqLength; i++)
      stringBuilder.append("1, " + seqLength + "\n");

    stringBuilder.append("\n// constraints (vars indexed from 0, allowed tuples):\n");

    // iterate over the n blocks of k positions
    for (int block = 1; block <= n; block++)
      // iterate over the variables within a block
      for (int i = 0; i < k; i++) {
        if (i < k - 1) {
          // constrain relative to neighbour
          stringBuilder.append("c(" + ((block - 1) * k + i) + ", " + ((block - 1) * k + i + 1) + ")\n");
          // acceptable assignments position the occurrences block+1 apart
          for (int pos = 1; pos < seqLength; pos++)
            if (pos + block + 1 <= seqLength)
              stringBuilder.append(pos + ", " + (pos + block + 1) + "\n");
          stringBuilder.append('\n');
        }
        // constrain relative to future blocks to prevent same position being used twice
        // find start of next block
        for (int j = block * k; j < seqLength; j++) {
          stringBuilder.append("c(" + ((block - 1) * k + i) + ", " + j + ")\n");
          for (int val1 = 1; val1 <= seqLength; val1++)
            for (int val2 = 1; val2 <= seqLength; val2++)
              if (val1 != val2)
                stringBuilder.append(val1 + ", " + val2 + "\n");
          stringBuilder.append('\n');
        }
      }

    String filePath = "instances/langfords" + k + "_" + n + ".csp";
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
      writer.write(stringBuilder.toString());
    } catch (IOException e) {
      System.out.println("Could not write file to " + filePath + "\n" + e.toString());
    }
  }
}
