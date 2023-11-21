/**
 * Assumes tuple values are integers
 */
public final class Arc extends BinaryTuple {
  public Arc(int var1, int var2) throws IllegalArgumentException {
    super(var1, var2);
    if (var1 == var2) {
      throw new IllegalArgumentException("An arc must be between two different variables!");
    }
    reversed = var2 < var1;
  }

  public boolean reversed;
}
