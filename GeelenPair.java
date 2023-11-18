import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GeelenPair {
    private List<Set<Integer>> left; // All values that are consistent with the assignment.
    private List<Set<Integer>> lost; // All values that are inconsistent with the assignment.
    public int varAssigned;
    public int valAssigned;

    public int lostSize() {
        int size = 0;
        for (Set<Integer> domainPrunes : lost) {
            size += domainPrunes.size();
        }
        return size;
    }

    public GeelenPair(BinaryCSP instance, int varAssigned, int valAssigned) {
        this.varAssigned = varAssigned;
        this.valAssigned = valAssigned;
        left = new ArrayList<Set<Integer>>();
        lost = new ArrayList<Set<Integer>>();
        for (int var = 0; var < instance.getNoVariables(); var++) {
            left.add(new HashSet<Integer>());
            lost.add(new HashSet<Integer>());
        }

        for (BinaryConstraint constraint : instance.constraints) {
            if (constraint.containsVar(varAssigned)) {
                boolean reverse = constraint.getSecondVar() == varAssigned;
                int otherVar = !reverse ? constraint.getSecondVar() : constraint.getFirstVar();
                if (instance.varList.contains(otherVar)) {
                    for (int otherVal : instance.domains.get(otherVar)) {
                        // Check whether the constraint has a tuple matching the pairs of values.
                        boolean valSupported = constraint.supportsTuple(new Arc(varAssigned, otherVar),
                                new BinaryTuple(valAssigned, otherVal));
                        if (valSupported) {
                            left.get(otherVar).add(otherVal);
                        } else {
                            lost.get(otherVar).add(otherVal);
                        }
                    }
                }
            }
        }
    }
}
