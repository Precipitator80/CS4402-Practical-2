import java.util.Queue;

public class BinaryCSPMACSolver extends BinaryCSPSolver {
    public BinaryCSPMACSolver(BinaryCSP instance) {
        super(instance);
    }

    public BinaryCSPMACSolver(String instanceFilePath) {
        super(instanceFilePath);
    }

    public static void main(String[] args) {
        // Read in a BinaryCSP instance to initialise and start the solver.
        if (args.length != 1) {
            System.out.println("Usage: java BinaryCSPMACSolver <file.csp>");
            return;
        }
        new BinaryCSPMACSolver(args[0]).solve();
    }

    @Override
    void solve() {
        // Create a starting state.
        enterNewState(-1);

        // Ensure global arc consistency at the start.
        try {
            macAC3();
        } catch (EmptyDomainException e) {
            System.out.println("Initial problem is not arc consistent. Cannot find a solution.");
            return;
        }

        // Run the MAC3 search algorithm.
        MAC3();

        printResults();
    }

    /**
     * Maintaining Arc Consistency Algorithm using AC3.
     */
    private void MAC3() {
        if (completeAssignments()) {
            showSolution(); // After finding a solution, continue searching for further solutions.
            return;
        }

        // Select a variable and value to assign.
        int var = selectVar();
        int val = selectVal(var);

        // Assign the variable, removing all other values from its domain.
        boolean changed = assign(var, val);

        try {
            // If any values were removed, propagate the changes.
            if (changed) {
                macAC3(var);
            }

            // If no domains were wiped out by the changes, run the algorithm again to choose further variables.
            MAC3();
        } catch (EmptyDomainException e) {
            // Exception to let AC3 cancel early in the case of a domain wipeout.
            if (DEBUG_MODE) {
                System.out.println(e.toString() + " (1)");
            }
        }

        // If recursion finished, this code is reached.
        // Revert the state and remove the value that was checked from the domain.

        // If the domain is not empty, propagate the domain pruning.
        // If this resulted in changes, run the algorithm again.
        try {
            unassign(var, val);
            macAC3(var);
            MAC3();
        } catch (EmptyDomainException e) {
            // Exception to let AC3 cancel early in the case of a domain wipeout.
            if (DEBUG_MODE) {
                System.out.println(e.toString() + " (2)");
            }
        }
        //System.out.println("Finished exploring tree (1).");
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
