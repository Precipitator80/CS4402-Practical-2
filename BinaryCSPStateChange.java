import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BinaryCSPStateChange {
    public List<Set<Integer>> domainPrunes;
    private List<BinaryConstraint> constraintChanges;
    public int assignedVar;

    public BinaryCSPStateChange(BinaryCSP instance, int assignedVar) {
        this.assignedVar = assignedVar;

        domainPrunes = new ArrayList<Set<Integer>>();
        for (int var = 0; var < instance.getNoVariables(); var++) {
            domainPrunes.add(new HashSet<Integer>());
        }

        constraintChanges = new ArrayList<BinaryConstraint>();
    }

    public void revert(BinaryCSP instance) {
        for (int var = 0; var < instance.getNoVariables(); var++) {
            for (int removedVal : domainPrunes.get(var)) {
                instance.domains.get(var).add(removedVal);
            }
        }

        for (BinaryConstraint changedConstraint : constraintChanges) {
            for (BinaryConstraint constraint : instance.constraints) {
                if (changedConstraint.equals(constraint)) {
                    for (BinaryTuple removedTuple : changedConstraint.tuples) {
                        constraint.tuples.add(removedTuple);
                    }
                }
            }
        }

        instance.varList.add(0, assignedVar);
    }

    public boolean addConstraintChange(int var1, int var2, BinaryTuple removedTuple) {
        // Try to find the relevant constraint if a tuple was already removed.
        for (BinaryConstraint binaryConstraint : constraintChanges) {
            if (binaryConstraint.matches(var1, var2)) {
                return binaryConstraint.tuples.add(removedTuple);
            }
        }

        // If the constraint has not had any tuples removed, then create a new entry for it.
        return constraintChanges.add(new BinaryConstraint(var1, var2, new ArrayList<BinaryTuple>() {
            {
                add(removedTuple);
            }
        }));
    }

    public List<BinaryConstraint> getConstraintChanges() {
        return constraintChanges;
    }
}
