import java.util.ArrayList;
import java.util.List;

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
        // Create a starting state.
        enterNewState(-1);

        forwardChecking();

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

    /**
     * Forward Checking:
     * Make a choice.
     * Propagate the choice.
     * If the choice was successful, make another choice.
     * If the choice was not successful, undo the changes and make the other choice.
     * Propagate the choice.
     * If neither worked, undo the single change and undo a previous choice.
     * @param varSet
     */
    private void forwardChecking() {
        // Cannot use the same completed assignments because consistency has to be checked.
        // After consistency of all variables is checked, the var set is fully emptied, completing the condition.
        if (completeAssignments()) {
            showSolution(); // After finding a solution, continue searching for further solutions.
            return;
        }

        if (instance.varSet.size() > 0) {
            // Select a variable and value to assign.
            int var = selectVar();
            int val = selectVal(var);

            // Make a guess.
            forwardCheckingLeftBranch(var, val);

            // If the guess failed, guess the opposite.
            forwardCheckingRightBranch(var, val);
        }
    }

    private void forwardCheckingLeftBranch(int var, int val) {
        // Assign the variable, removing all other values from its domain.
        assign(var, val);

        try {
            // Revise all future arcs to enforce local arc consistency.
            reviseFutureArcs(var);
            forwardChecking();
        } catch (EmptyDomainException e) {
            // Exception to let revision cancel early in the case of a domain wipeout.
            if (DEBUG_MODE) {
                System.out.println(e.toString() + " (FC Left Branch)");
            }
        }
    }

    private void forwardCheckingRightBranch(int var, int val) {
        unassign(var, val);
        if (!instance.domains.get(var).isEmpty()) {
            try {
                reviseFutureArcs(var);
                forwardChecking();
            } catch (EmptyDomainException e) {
                // Exception to let revision cancel early in the case of a domain wipeout.
                if (DEBUG_MODE) {
                    System.out.println(e.toString() + " (FC Right Branch)");
                }
            }
        }
        restoreDomain(var, val);
    }

    private void reviseFutureArcs(int currentVar) throws EmptyDomainException {
        List<Arc> arcs = getFutureArcs(currentVar);
        for (Arc arc : arcs) {
            revise(arc);
        }
    }

    private List<Arc> getFutureArcs(int currentVar) {
        List<Arc> arcs = new ArrayList<Arc>();
        for (int futureVar : instance.varSet) {
            if (futureVar != currentVar) {
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
