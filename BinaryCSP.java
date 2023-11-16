import java.util.*;

public final class BinaryCSP {
  private int[][] domainBounds;
  public ArrayList<BinaryConstraint> constraints;
  public ArrayList<Set<Integer>> domains;
  // Variables left to assign.
  Set<Integer> varSet;

  public BinaryCSP(int[][] db, ArrayList<BinaryConstraint> c) {
    domainBounds = db;
    constraints = c;

    // Create domains for the variables based on their bounds.
    domains = new ArrayList<Set<Integer>>();
    varSet = new LinkedHashSet<Integer>();
    for (int varIndex = 0; varIndex < getNoVariables(); varIndex++) {
      varSet.add(varIndex);
      Set<Integer> domain = new LinkedHashSet<Integer>();
      for (int val = getLB(varIndex); val <= getUB(varIndex); val++) {
        domain.add(val);
      }
      domains.add(domain);
    }
  }

  public String toString() {
    StringBuffer result = new StringBuffer();
    result.append("CSP:\n");
    for (int i = 0; i < domainBounds.length; i++)
      result.append("Var " + i + ": " + domainBounds[i][0] + " .. " + domainBounds[i][1] + "\n");
    for (BinaryConstraint bc : constraints)
      result.append(bc + "\n");
    return result.toString();
  }

  public int getNoVariables() {
    return domainBounds.length;
  }

  public int getLB(int varIndex) {
    return domainBounds[varIndex][0];
  }

  public int getUB(int varIndex) {
    return domainBounds[varIndex][1];
  }
}
