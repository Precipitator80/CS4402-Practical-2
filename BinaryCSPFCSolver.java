public class BinaryCSPFCSolver extends BinaryCSPSolver {
    public BinaryCSPFCSolver(BinaryCSP instance) {
        super(instance);
    }

    @Override
    void solve() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'solve'");
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

    // private boolean revise(int futureVarIndex, int currentVarIndex) {
    //   for (BinaryConstraint constraint : instance.getConstraints()) {
    //     if (constraint.getFirstVar() == currentVarIndex) {
    //       for (BinaryTuple tuple : constraint.getTuples()) {
    //         if (tuple.getVal1() == assignments[currentVarIndex]) {
    //           return true;
    //         } else {

    //         }
    //       }
    //     }
    //   }
    //   return false;
    // }

}
