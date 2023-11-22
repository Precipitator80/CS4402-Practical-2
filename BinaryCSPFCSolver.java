import java.util.ArrayList;
import java.util.List;

public class BinaryCSPFCSolver extends BinaryCSPSolver {
    public BinaryCSPFCSolver(String instanceFilePath, int solutionsToFind, int varSelectMode, int valSelectMode,
            boolean debugMode) {
        super(instanceFilePath, solutionsToFind, varSelectMode, valSelectMode, debugMode);
    }

    public BinaryCSPFCSolver(BinaryCSP instance, int solutionsToFind, int varSelectMode, int valSelectMode,
            boolean debugMode) {
        super(instance, solutionsToFind, varSelectMode, valSelectMode, debugMode);
    }

    public BinaryCSPFCSolver(BinaryCSP instance, int solutionsToFind, BinaryCSPSolver.VarSelectMode varSelectMode,
            BinaryCSPSolver.ValSelectMode valSelectMode, boolean debugMode) {
        super(instance, solutionsToFind, varSelectMode, valSelectMode, debugMode);
    }

    /**
     * Forward checking must always check consistency, even when a domain only had one value left.
     * This is because for each variable, only future arcs to that variable are checked.
     * Hence, a new variable choice must always check its future arcs.
     * In MAC, all related arcs are checked. Not just arcs to the current one.
     */
    @Override
    void enforceLocalConsistency(int var, boolean changed) throws EmptyDomainException {
        reviseFutureArcs(var);
    }

    @Override
    boolean prepareSolver() {
        return true; // Nothing to prepare at the start of FC.
    }

    /**
     * Revise all future arcs targeting this variable in order to enforce local arc consistency.
     * @param currentVar The variable that was just assigned.
     * @throws EmptyDomainException If an arc revision resulted in a domain wipeout.
     */
    private void reviseFutureArcs(int currentVar) throws EmptyDomainException {
        List<Arc> arcs = getFutureArcs(currentVar);
        for (Arc arc : arcs) {
            revise(arc);
        }
    }

    /**
     * Gets all the arcs of unassigned variables to the current variable.
     * @param currentVar The variable that was just assigned.
     * @return A list of arcs to revise.
     */
    private List<Arc> getFutureArcs(int currentVar) {
        List<Arc> arcs = new ArrayList<Arc>();
        for (int futureVar : instance.varList) {
            if (futureVar != currentVar) {
                arcs.add(new Arc(futureVar, currentVar));
            }
        }
        return arcs;
    }
}
