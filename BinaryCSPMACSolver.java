import java.time.Duration;
import java.time.Instant;
import java.util.Queue;

public class BinaryCSPMACSolver extends BinaryCSPSolver {
    public BinaryCSPMACSolver(String instanceFilePath, int solutionsToFind, int varSelectMode, int valSelectMode,
            boolean debugMode) {
        super(instanceFilePath, solutionsToFind, varSelectMode, valSelectMode, debugMode);
    }

    public BinaryCSPMACSolver(BinaryCSP instance, int solutionsToFind, int varSelectMode, int valSelectMode,
            boolean debugMode) {
        super(instance, solutionsToFind, varSelectMode, valSelectMode, debugMode);
    }

    public static void main(String[] args) {
        try {
            if (args.length > 0) {
                String instanceFilePath = args[0];
                int solutionsToFind = 0;
                int varSelectMode = 0;
                int valSelectMode = 0;
                boolean debugMode = false;
                if (args.length > 1) {
                    solutionsToFind = Integer.parseInt(args[1]);
                    if (args.length > 2) {
                        varSelectMode = Integer.parseInt(args[2]);
                        if (args.length > 3) {
                            valSelectMode = Integer.parseInt(args[3]);
                            if (args.length > 4) {
                                debugMode = Boolean.parseBoolean(args[4]);
                            }
                        }
                    }
                }

                new BinaryCSPMACSolver(instanceFilePath, solutionsToFind, varSelectMode, valSelectMode, debugMode)
                        .solve();
            }
        } catch (Exception e) {
            System.out.println(
                    "Usage: java BinaryCSPMACSolver <file.csp> [solutionsToFind] [varSelectMode] [valSelectMode]");
        }
    }

    @Override
    void solve() {
        // Create a starting state.
        enterNewState(-1);
        Instant start = Instant.now();

        // Ensure global arc consistency at the start.
        try {
            macAC3();
        } catch (EmptyDomainException e) {
            System.out.println("Initial problem is not arc consistent. Cannot find a solution.");
            return;
        }

        // Run the MAC3 search algorithm.
        MAC3();

        Instant finish = Instant.now();
        long timeElapsed = Duration.between(start, finish).toMillis();
        printInfo();
        System.out.println("Time taken: " + timeElapsed + "ms");
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

        // TODO Have geelen selectValAndAssign method.

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

        if (stopSearching()) {
            return;
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

        if (stopSearching()) {
            return;
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
