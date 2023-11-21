import java.util.LinkedList;
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

    @Override
    void enforceLocalConsistency(int var, boolean changed) throws EmptyDomainException {
        if (changed) {
            macAC3(var);
        }
    }

    @Override
    boolean prepareSolver() {
        // Ensure global arc consistency at the start.
        try {
            macAC3();
        } catch (EmptyDomainException e) {
            System.out.println("Initial problem is not arc consistent. Cannot find a solution.");
            return false;
        }
        return true;
    }

    /**
     * AC3 with the entire graph.
     * @return Whether any domains were changed.
     * @throws EmptyDomainException If an arc revision resulted in a domain wipeout.
     */
    private boolean macAC3() throws EmptyDomainException {
        return macAC3(getArcs());
    }

    /**
     * AC3 starting with one node.
     * @param var The variable of the start node.
     * @return Whether any domains were changed.
     * @throws EmptyDomainException If an arc revision resulted in a domain wipeout.
     */
    private boolean macAC3(int var) throws EmptyDomainException {
        return macAC3(getArcs(var));
    }

    /**
     * Arc Consistency 3 in the MAC Algorithm.
     * @param queue The propagation queue of arcs to check.
     * @return Whether any domains were changed.
     * @throws EmptyDomainException If an arc revision resulted in a domain wipeout.
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

    /**
    * Gets all the arcs of the instance's graph of variables (nodes) and constraints (edges).
    * @return A queue of all arcs in the instance's graph.
    */
    protected Queue<Arc> getArcs() {
        Queue<Arc> queue = new LinkedList<Arc>();
        for (BinaryConstraint constraint : instance.constraints) {
            createArcs(constraint, queue);
        }
        return queue;
    }

    /**
     * Gets all the arcs targeting a given variable / node.
     * @param arc An arc containing the target variable as well as another variable to ignore.
     * @return A queue of arcs targeting the given variable.
     */
    protected Queue<Arc> getTargetedArcs(Arc arc) {
        Queue<Arc> queue = new LinkedList<Arc>();
        for (BinaryConstraint constraint : instance.constraints) {
            if (constraint.containsVar(arc.getVal1())) {
                int otherVar = (constraint.getFirstVar() == arc.getVal1()) ? constraint.getSecondVar()
                        : constraint.getFirstVar();
                if (otherVar != arc.getVal2()) {
                    queue.add(new Arc(otherVar, arc.getVal1()));
                }
            }
        }
        return queue;
    }
}
