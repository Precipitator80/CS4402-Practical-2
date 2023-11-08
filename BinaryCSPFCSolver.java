import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BinaryCSPFCSolver extends BinaryCSPSolver {
    public BinaryCSPFCSolver(BinaryCSP instance) {
        super(instance);
    }

    public static void main(String[] args) {
        // Read in a BinaryCSP instance and initialise the solver.
        if (args.length != 1) {
            System.out.println("Usage: java BinaryFCCSPSolver <file.csp>");
            return;
        }
        BinaryCSPReader reader = new BinaryCSPReader();
        BinaryCSP instance = reader.readBinaryCSP(args[0]);
        BinaryCSPSolver fcSolver = new BinaryCSPFCSolver(instance);

        // Solve the BinaryCSP instance.
        fcSolver.solve();
    }

    @Override
    void solve() {
        // Create a new state.
        enterNewState();

        Set<Integer> varSet = new LinkedHashSet<Integer>();
        for (int var = 0; var < instance.domains.size(); var++) {
            varSet.add(var);
        }

        forwardChecking(varSet);

        if (solutionsFound == 0) {
            System.out.println("Failed to find a solution!");
        } else {
            System.out.println("Found " + solutionsFound + " solutions!");
        }
    }

    /*
     * TODO From Lecture Notes:
     * An assigned variable has been assigned by a left branch.
     * A variable with one value remaining is not an assigned variable.
     * 
     * Why is this the case?
     * 
     * Do you also need to check node consistency somewhere?
     */

    private void forwardChecking(Set<Integer> varSet) {
        if (completeAssignments()) {
            showSolution(); // After finding a solution, continue searching for further solutions.
            return;
        }

        if (varSet.size() > 0) {
            // Create a new state.
            enterNewState();

            // Select a variable and value to assign.
            int var = selectVar(varSet);
            int val = selectVal(var);
            forwardCheckingLeftBranch(varSet, var, val);
            forwardCheckingRightBranch(varSet, var, val);
        }
    }

    private void forwardCheckingLeftBranch(Set<Integer> varSet, int var, int val) {
        // Assign the variable, removing all other values from its domain.
        boolean changed = assign(var, val);

        try {
            if (reviseFutureArcs(var, val) || changed) {
                Set<Integer> truncatedVarSet = new LinkedHashSet<Integer>(varSet);
                truncatedVarSet.remove(var);
                forwardChecking(truncatedVarSet);
            }
        } catch (EmptyDomainException e) {
            // Exception to let revision cancel early in the case of a domain wipeout.
            if (DEBUG_MODE) {
                System.out.println(e.toString() + " (FC Left Branch)");
            }
        }
    }

    private void forwardCheckingRightBranch(Set<Integer> varSet, int var, int val) {
        revertState();
        boolean changed = unassign(var, val);
        if (!instance.domains.get(var).isEmpty()) {
            try {
                if (reviseFutureArcs(var, val) || changed) {
                    Set<Integer> truncatedVarSet = new LinkedHashSet<Integer>(varSet);
                    truncatedVarSet.remove(var);
                    forwardChecking(truncatedVarSet);
                }
            } catch (EmptyDomainException e) {
                // Exception to let revision cancel early in the case of a domain wipeout.
                if (DEBUG_MODE) {
                    System.out.println(e.toString() + " (FC Right Branch)");
                }
            }
        }
        restoreDomain(var, val);
    }

    private boolean reviseFutureArcs(int var, int val) throws EmptyDomainException {
        boolean consistent = true;
        List<Arc> arcs = getFutureArcs(var);
        for (int i = 0; i < arcs.size() && consistent; i++) {
            consistent = revise(arcs.get(i));
        }
        return consistent;
    }

    private List<Arc> getFutureArcs(int currentVar) {
        List<Arc> arcs = new ArrayList<Arc>();
        for (int futureVar = 0; futureVar < instance.domains.size(); futureVar++) {
            if (futureVar != currentVar && instance.domains.get(futureVar).size() > 1) {
                arcs.add(new Arc(futureVar, currentVar));
            }
        }
        return arcs;
    }

    // private void forwardChecking() {
    //   forwardChecking(0);
    // }

    // private void forwardChecking(int varIndex) {
    //   List<Set<Integer>> currentState = stateChanges.peek();
    //   for (int possibleValue : currentState.get(varIndex)) {
    //     // Duplicate the previous state to allow easy backtracking in case an assignment is not consistent.
    //     stateChanges.add(new ArrayList<Set<Integer>>(currentState));
    //     currentState = stateChanges.peek();

    //     // Assign the possible value.
    //     assignments[varIndex] = possibleValue;
    //     // Check whether the other variables still have possible options by revising arcs.
    //     boolean consistent = true;
    //     for (int futureVarIndex = varIndex + 1; futureVarIndex < instance.getNoVariables()
    //         && consistent; futureVarIndex++) {
    //       consistent = revise(varIndex, futureVarIndex);
    //     }
    //     System.out.println("Consistent? " + consistent);
    //     System.out.println("States stack size: " + stateChanges.size());

    //     // If consistent (still choices left), then continue.
    //     // If inconsistent, revert to the previous state.
    //     if (consistent) {
    //       if (varIndex == instance.getNoVariables() - 1) {
    //         showSolution();
    //         System.exit(0);
    //       } else {
    //         forwardChecking(varIndex + 1);
    //       }
    //     } else if (stateChanges.size() > 1) {
    //       System.out.println("Popping last state");
    //       stateChanges.pop();
    //     } else {
    //       System.out.println("Unexpected solver state! Terminating solver.");
    //       System.exit(1);
    //     }
    //   }
    //   System.out.println("Could not find a solution!");
    // }
}
