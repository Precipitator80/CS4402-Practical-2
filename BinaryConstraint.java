import java.util.*;

public final class BinaryConstraint {
  private int firstVar, secondVar;
  public ArrayList<BinaryTuple> tuples;

  public BinaryConstraint(int fv, int sv, ArrayList<BinaryTuple> t) {
    firstVar = fv;
    secondVar = sv;
    tuples = t;
  }

  public String toString() {
    StringBuffer result = new StringBuffer();
    result.append("c(" + firstVar + ", " + secondVar + ")\n");
    for (BinaryTuple bt : tuples)
      result.append(bt + "\n");
    return result.toString();
  }

  // SUGGESTION: You will want to add methods here to reason about the constraint
  public boolean matches(int firstVar, int secondVar) {
    return (this.firstVar == firstVar) && (this.secondVar == secondVar);
  }

  public boolean matches(Arc arc) {
    if (arc.reversed) {
      return this.firstVar == arc.getVal2() && this.secondVar == arc.getVal1();
    }
    return this.firstVar == arc.getVal1() && this.secondVar == arc.getVal2();
  }

  public int getFirstVar() {
    return firstVar;
  }

  public int getSecondVar() {
    return secondVar;
  }

  public boolean containsVar(int var) {
    return firstVar == var || secondVar == var;
  }

  // public ArrayList<BinaryTuple> getTuples() {
  //   return tuples;
  // }

  public boolean supportsTuple(Arc arc, BinaryTuple valueTuple) {
    // Get the values of the tuple accounting for reversion.
    // If the arc is reversed, then the order of the values must be switched.
    int val1 = !arc.reversed ? valueTuple.getVal1() : valueTuple.getVal2();
    int val2 = !arc.reversed ? valueTuple.getVal2() : valueTuple.getVal1();

    for (BinaryTuple tuple : tuples) {
      if (tuple.matches(val1, val2)) {
        return true;
      }
    }
    return false;
  }

  public boolean equals(BinaryConstraint constraint) {
    return this.firstVar == constraint.firstVar && this.secondVar == constraint.secondVar;
  }
}
