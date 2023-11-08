import java.util.Queue;

public class BinaryCSPMACSolver extends BinaryCSPSolver {
    public static void main(String[] args) {
        // Read in a BinaryCSP instance and initialise the solver.
        if (args.length != 1) {
            System.out.println("Usage: java BinaryCSPSolver <file.csp>");
            return;
        }
        BinaryCSPReader reader = new BinaryCSPReader();
        BinaryCSP instance = reader.readBinaryCSP(args[0]);
        BinaryCSPSolver macSolver = new BinaryCSPMACSolver(instance);

        // Solve the BinaryCSP instance.
        macSolver.solve();
    }

    public BinaryCSPMACSolver(BinaryCSP instance) {
        super(instance);
    }

    @Override
    void solve() {
        enterNewState();
        // Ensure global arc consistency at the start.
        try {
            macAC3();
        } catch (EmptyDomainException e) {
            System.out.println("Initial problem is not arc consistent. Cannot find a solution.");
            return;
        }

        // Run the MAC3 search algorithm.
        MAC3();

        if (solutionsFound == 0) {
            System.out.println("Failed to find a solution!");
        } else {
            System.out.println("Found " + solutionsFound + " solutions!");
        }
    }

    /**
     * Maintaining Arc Consistency Algorithm using AC3.
     */
    private void MAC3() {
        if (completeAssignments()) {
            showSolution(); // After finding a solution, continue searching for further solutions.
            return;
        }

        // Create a new state.
        enterNewState();

        // Select a variable and value to assign.
        int var = selectVar();
        int val = selectVal(var);

        // Assign the variable, removing all other values from its domain.
        boolean changed = assign(var, val);

        // Check whether all variables have been assigned.
        // If they have, show the solution and stop the algorithm.
        // Else, propagate the changes and run the algorithm again to choose further variables.
        // If no changes were made by AC3 (returns false) after checking for a solution, then a dead end was reached.
        try {
            if (macAC3(var) || changed) {
                MAC3();
            }
        } catch (EmptyDomainException e) {
            // Exception to let AC3 cancel early in the case of a domain wipeout.
            if (DEBUG_MODE) {
                System.out.println(e.toString() + " (1)");
            }
        }

        // If recursion finished, this code is reached.
        // Revert the state and remove the value that was checked from the domain.
        revertState();
        changed = unassign(var, val);

        // If the domain is not empty, propagate the domain pruning.
        // If this resulted in changes, run the algorithm again.
        if (!instance.domains.get(var).isEmpty()) {
            try {
                if (macAC3(var) || changed) {
                    MAC3();
                }
            } catch (EmptyDomainException e) {
                // Exception to let AC3 cancel early in the case of a domain wipeout.
                if (DEBUG_MODE) {
                    System.out.println(e.toString() + " (2)");
                }
            }
            //System.out.println("Finished exploring tree (1).");
        }
        //System.out.println("Finished exploring tree (2).");
        restoreDomain(var, val);
    }

    /**
     * AC3 with the entire graph.
     * @return whether any domains were changed.
     */
    private boolean macAC3() throws EmptyDomainException {
        return macAC3(getArcs());
    }

    /**
     * AC3 starting with one node.
     * @param var The variable of the start node.
     * @return whether any domains were changed.
     */
    private boolean macAC3(int var) throws EmptyDomainException {
        return macAC3(getArcs(var));
    }

    /**
     * Arc Consistency 3 in the MAC Algorithm.
     * @param queue The propagation queue of arcs to check.
     * @return whether any domains were changed.
     */
    private boolean macAC3(Queue<Arc> queue) throws EmptyDomainException {
        // Keep checking all arcs until the queue is empty.
        boolean changed = false;
        while (!queue.isEmpty()) {
            // Check the arc for support and prune the domain of the first value for any unsupported values.
            Arc arc = queue.poll();
            if (revise(arc)) {
                changed = true; // 
                queue.addAll(getTargetedArcs(arc));
            }
        }
        return changed;
    }
}
