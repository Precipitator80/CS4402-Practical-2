import java.time.Duration;
import java.time.Instant;
import java.util.*;

public abstract class BinaryCSPSolver {
  public BinaryCSPSolver(String instanceFilePath, int solutionsToFind, int varSelectMode, int valSelectMode,
      boolean debugMode) {
    this(new BinaryCSPReader().readBinaryCSP(instanceFilePath), solutionsToFind, varSelectMode, valSelectMode,
        debugMode);
  }

  public BinaryCSPSolver(BinaryCSP instance, int solutionsToFind, int varSelectMode, int valSelectMode,
      boolean debugMode) {
    this.instance = instance;
    this.solutionsToFind = solutionsToFind;
    this.varSelectMode = VarSelectMode.values()[varSelectMode];
    this.valSelectMode = ValSelectMode.values()[valSelectMode];
    this.DEBUG_MODE = debugMode;
    this.stateChanges = new Stack<BinaryCSPStateChange>();
  }

  /**
   * Main method to run one of the specialised solvers.
   * @param args file.csp [solverType] [solutionsToFind] [varSelectMode] [valSelectMode] [debugMode]
   */
  public static void main(String[] args) {
    try {
      if (args.length > 0) {
        String instanceFilePath = args[0];
        String solverType = "";
        int solutionsToFind = 0;
        int varSelectMode = 0;
        int valSelectMode = 0;
        boolean debugMode = false;
        if (args.length > 1) {
          solverType = args[1];
          if (args.length > 2) {
            solutionsToFind = Integer.parseInt(args[1]);
            if (args.length > 3) {
              varSelectMode = Integer.parseInt(args[2]);
              if (args.length > 4) {
                valSelectMode = Integer.parseInt(args[3]);
                if (args.length > 5) {
                  debugMode = Boolean.parseBoolean(args[4]);
                }
              }
            }
          }
        }

        switch (solverType) {
          case "FC":
            new BinaryCSPFCSolver(instanceFilePath, solutionsToFind, varSelectMode, valSelectMode, debugMode).solve();
            break;
          default:
            System.out.println("Did not pass in valid solver type (FC / MAC). Defaulting to MAC.");
          case "MAC":
          case "":
            new BinaryCSPMACSolver(instanceFilePath, solutionsToFind, varSelectMode, valSelectMode, debugMode).solve();
            break;
        }
      }
    } catch (Exception e) {
      System.out.println(
          "Usage: java BinaryCSPSolver <file.csp> [solverType] [solutionsToFind] [varSelectMode] [valSelectMode] [debugMode]");
    }
  }

  enum VarSelectMode {
    ASCENDING,
    SMALLEST_DOMAIN
  }

  enum ValSelectMode {
    ASCENDING,
    MIN_CONFLICTS
  }

  // The instance to solve.
  BinaryCSP instance;

  // The number of solutions the solver should find. 0 = All solutions.
  int solutionsToFind;

  // Settings for how the solver should select variables and values.
  VarSelectMode varSelectMode;
  ValSelectMode valSelectMode;

  // Variables to log solver data.
  int solutionsFound = 0; // The number of solutions found.
  int nodesExplored = 0; // The number of nodes explored.
  int revisionsDone = 0; // The number of arc revisions done.

  // Flag to print out solver logic.
  final boolean DEBUG_MODE;

  // A states stack for each depth of search.
  // Each state has a list of variable domains.
  // Each domain is a set of integers.
  Stack<BinaryCSPStateChange> stateChanges;

  /**
   * Algorithm to enforce local arc consistency.
   * @param var The var to enforce local arc consistency around.
   * @param changed Whether the preceding assignment / unassignment changed var's domain.
   * @throws EmptyDomainException If the domain of any variables were wiped out during revision.
   */
  abstract void enforceLocalConsistency(int var, boolean changed) throws EmptyDomainException;

  /**
   * Method for solver types that require special setup at the start.
   * For example, MAC must enforce global arc consistency before solving.
   * FC does not require any special setup.
   * @return Whether the solver prepared successfully.
   */
  abstract boolean prepareSolver();

  /**
   * Method that sets up and starts the solver.
   * Also records solver information and prints it at the end.
   */
  protected void solve() {
    // Create a starting state.
    enterNewState(-1);
    Instant start = Instant.now();

    boolean ready = prepareSolver();

    if (ready) {
      recursiveStep(); // Start the first recursive step of solving.

      // Print solver information after finishing.
      Instant finish = Instant.now();
      long timeElapsed = Duration.between(start, finish).toMillis();
      printInfo();
      System.out.println("Time taken: " + timeElapsed + "ms");
    } else {
      System.err.println("Failed to prepare solver!");
    }
  }

  /**
   * Solves the CSP through recursive steps.
   * @return Whether to stop searching. Used to propagate exit condition in case of finding limited solutions.
   */
  private boolean recursiveStep() {
    if (completeAssignments()) {
      showSolution(); // After finding a solution, continue searching for further solutions.
      return solutionsToFind > 0 && solutionsFound >= solutionsToFind;
    }

    // LEFT BRANCH: Make a guess.
    // Select a variable and value to assign.
    int var = selectVar();
    int val = selectVal(var);

    // Assign the variable, removing all other values from its domain.
    boolean changed = assign(var, val);

    // TODO Have geelen selectValAndAssign method.

    try {
      // If any values were removed, propagate the changes.
      enforceLocalConsistency(var, changed);

      // If no domains were wiped out by the changes, run the algorithm again to choose further variables.
      boolean stopSearch = recursiveStep();
      if (stopSearch) {
        return true;
      }
    } catch (EmptyDomainException e) {
      // Exception to let AC3 cancel early in the case of a domain wipeout.
      if (DEBUG_MODE) {
        System.out.println(e.toString() + " (1)");
      }
    }

    // RIGHT BRANCH: If the guess failed, guess the opposite.
    try {
      unassign(var, val);
      enforceLocalConsistency(var, true); // Unassign will always change the variable's domain if not wiping it out.
      boolean stopSearch = recursiveStep();
      if (stopSearch) {
        return true;
      }
    } catch (EmptyDomainException e) {
      // Exception to let AC3 cancel early in the case of a domain wipeout.
      if (DEBUG_MODE) {
        System.out.println(e.toString() + " (2)");
      }
    }

    //System.out.println("Finished exploring tree (1).");
    //System.out.println("Finished exploring tree (2).");
    restoreDomain(var, val);

    return false;
  }

  /**
   * Get the current state change.
   * @return The current state change.
   */
  protected BinaryCSPStateChange currentStateChanges() {
    return stateChanges.peek();
  }

  /**
   * Enter a new state by adding an empty state change to the stack.
   * @return Whether the state was entered successfully.
   */
  protected boolean enterNewState(int assignedVar) {
    return stateChanges.add(new BinaryCSPStateChange(instance, assignedVar));
  }

  /**
   * Pops the current state and reverts any changes made by it.
   * Only done when the current state is not the starting state.
   */
  protected void revertState() {
    if (stateChanges.size() > 1) {
      BinaryCSPStateChange stateChange = stateChanges.pop();
      stateChange.revert(instance);
    } else {
      if (DEBUG_MODE) {
        System.out.println("States stack is at starting size.");
      }
    }
  }

  /**
   * Assign a specific value to a variable by removing all other values from the domain.
   * This means that the variable is set to equal this specific value (left branch).
   * @param var The variable to assign the value to.
   * @param val The value to assign.
   */
  protected boolean assign(int var, int val) {
    // Create a new state.
    enterNewState(var);
    nodesExplored++;
    instance.varList.remove((Object) var);

    boolean changed = false;
    Iterator<Integer> domainIterator = instance.domains.get(var).iterator();
    while (domainIterator.hasNext()) {
      int otherVal = domainIterator.next();
      if (val != otherVal) {
        domainIterator.remove();
        try {
          pruneDomain(var, otherVal);
        } catch (EmptyDomainException e) {
          System.err.println("Domain wipeout while assigning a variable! There is likely an error in the code.");
        }
        changed = true;
      }
    }

    if (DEBUG_MODE) {
      System.out.println("Set var " + var + " = " + val);
    }
    return changed;
  }

  /**
   * "Unassign" a specific value to a variable by removing it from the domain.
   * This means that the variable is set to not equal this specific value (right branch).
   * @param var The variable to remove the value from.
   * @param val The value to remove.
   * @throws EmptyDomainException If domain pruning resulted in a wipeout.
   */
  protected void unassign(int var, int val) throws EmptyDomainException {
    revertState();
    nodesExplored++;
    pruneDomain(var, val);
    if (DEBUG_MODE) {
      System.out.println("Set var " + var + " != " + val);
    }
  }

  /**
   * Remove / prune a specific value from a variable's domain.
   * @param var The variable to remove the value from.
   * @param val The value to remove.
   * @return Whether the value was removed / pruned successfully.
   * @throws EmptyDomainException If domain pruning resulted in a wipeout.
   */
  private void pruneDomain(int var, int val) throws EmptyDomainException {
    instance.domains.get(var).remove(val);
    currentStateChanges().domainPrunes.get(var).add(val);
    if (instance.domains.get(var).isEmpty()) {
      throw new EmptyDomainException("Domain wipeout when pruning domain!");
    }
    removeInvalidConstraints(var, val);
  }

  /**
   * Remove all the constraints that use a value that is no longer in the variable's domain.
   * @param var The variable with the changed domain.
   * @param val The value that was removed from the domain.
   */
  private boolean removeInvalidConstraints(int var, int val) {
    boolean changed = false;
    for (BinaryConstraint constraint : instance.constraints) {
      if (constraint.containsVar(var)) {
        boolean reverse = constraint.getSecondVar() == var;
        Iterator<BinaryTuple> tupleIterator = constraint.tuples.iterator();
        while (tupleIterator.hasNext()) {
          BinaryTuple tuple = tupleIterator.next();
          if ((!reverse && tuple.getVal1() == val) || (reverse && tuple.getVal2() == val)) {
            currentStateChanges().addConstraintChange(constraint.getFirstVar(), constraint.getSecondVar(), tuple);
            tupleIterator.remove();
            changed = true;
          }
        }
      }
    }
    return changed;
  }

  /**
   * Readds a value to the domain of a variable and readds any removed constraints.
   * @param var The variable with the domain to restore.
   * @param val The value to put back into the domain.
   */
  protected void restoreDomain(int var, int val) {
    instance.domains.get(var).add(val);
    currentStateChanges().domainPrunes.get(var).remove(val);

    for (BinaryConstraint changedConstraint : currentStateChanges().getConstraintChanges()) {
      if (changedConstraint.containsVar(var)) {
        boolean reverse = changedConstraint.getSecondVar() == var;
        Iterator<BinaryTuple> tupleIterator = changedConstraint.tuples.iterator();
        while (tupleIterator.hasNext()) {
          BinaryTuple removedTuple = tupleIterator.next();
          if ((!reverse && removedTuple.getVal1() == val) || (reverse && removedTuple.getVal2() == val)) {
            for (BinaryConstraint constraint : instance.constraints) {
              if (constraint.equals(changedConstraint)) {
                constraint.tuples.add(removedTuple);
                break;
              }
            }
            tupleIterator.remove();
          }
        }
      }
    }
  }

  /**
   * Method to select a variable to make a choice for.
   * @return The variable to make a choice for.
   */
  protected int selectVar() {
    switch (varSelectMode) {
      case SMALLEST_DOMAIN:
        return selectVarSmallestDomain();
      default:
        return selectVarAscending();
    }
  }

  /**
   * Selects a non-assigned variable based on ascending order.
   * @return The non-assigned variable with the smallest number.
   */
  private int selectVarAscending() {
    if (!instance.varList.isEmpty()) {
      return instance.varList.get(0);
    }
    System.out.println("Trying to select variable when all are assigned! Returning default 0.");
    return 0;
  }

  /**
   * Select a non-assigned variable based on which has the smallest domain.
   * @return The non-assigned variable with the smallest domain.
   */
  private int selectVarSmallestDomain() {
    int smallestDomainVar = -1;
    int smallestDomainSize = Integer.MAX_VALUE;
    for (int var : instance.varList) {
      int domainSize = instance.domains.get(var).size();
      if (domainSize < smallestDomainSize) {
        smallestDomainVar = var;
        smallestDomainSize = instance.domains.get(smallestDomainVar).size();
      }
    }
    if (smallestDomainVar == -1) {
      System.out.println("Trying to select variable when all are assigned! Returning default 0.");
      return 0;
    }
    return smallestDomainVar;
  }

  /**
   * Selects a value in the domain of a given variable.
   * @param var The variable with the domain to get a value from.
   * @return The first value in the domain of the variable.
   */
  protected int selectVal(int var) {
    switch (valSelectMode) {
      case MIN_CONFLICTS:
        return selectValMinConflicts(var).valAssigned;
      default:
        return selectValAscending(var);
    }
  }

  /**
   * Select the first value in the domain of a given variable.
   * Throws an undeclared exception if the domain is empty. Should not happen in practice.
   * @param var The variable with the domain to get a value from.
   * @return The first value in the domain of the variable.
   */
  private int selectValAscending(int var) {
    return instance.domains.get(var).iterator().next();
  }

  /** TODO If using a Geelen promise / heuristic / etc, do value choosing and assigning in one step to avoid searching for lost constraints twice. */
  /**
   * Selects the value with the minimum conflicts in the domain of a given variable.
   * This is the value that removes the fewest values from the domains of variables left to assign.
   * @param var The variable with the domain to get a value from.
   * @return The value with the minimum conflicts.
   */
  private GeelenPair selectValMinConflicts(int var) {
    GeelenPair minGeelenPair = null;
    int minLost = Integer.MAX_VALUE;
    for (GeelenPair potentialGeelenPair : getGeelenPairs(var)) {
      if (potentialGeelenPair.lostSize() < minLost) {
        minGeelenPair = potentialGeelenPair;
        minLost = potentialGeelenPair.lostSize();
      }
    }
    return minGeelenPair;
  }

  /**
   * Gets the geelen pairs for a specific variable.
   * @param var The variable to get the pairs for.
   * @return A set of Geelen pairs for each value of var's domain.
   */
  private Set<GeelenPair> getGeelenPairs(int var) {
    Set<GeelenPair> geelenPairs = new LinkedHashSet<GeelenPair>();
    Iterator<Integer> domainIterator = instance.domains.get(var).iterator();
    while (domainIterator.hasNext()) {
      int val = domainIterator.next();
      geelenPairs.add(new GeelenPair(instance, var, val));
    }
    return geelenPairs;
  }

  /**
   * Checks whether assignments have been made for all variables.
   * @return Whether all variables have assignments.
   */
  protected boolean completeAssignments() {
    return instance.varList.isEmpty();
  }

  /**
   * Gets all the arcs around a single variable (node) in the instance's graph.
   * @param var The variable to get connected arcs of.
   * @return All arcs around the variable.
   */
  protected Queue<Arc> getArcs(int var) {
    Queue<Arc> queue = new LinkedList<Arc>();
    for (BinaryConstraint constraint : instance.constraints) {
      if (constraint.getFirstVar() == var || constraint.getSecondVar() == var) {
        createArcs(constraint, queue);
      }
    }
    return queue;
  }

  /**
   * Creates arcs for a constraint and adds them to a queue.
   * @param constraint The constraint to create arcs for.
   * @param queue The queue to add the arcs to.
   */
  protected void createArcs(BinaryConstraint constraint, Queue<Arc> queue) {
    queue.add(new Arc(constraint.getFirstVar(), constraint.getSecondVar()));
    queue.add(new Arc(constraint.getSecondVar(), constraint.getFirstVar()));
  }

  /**
   * An arc revision that removes any domain values not supporting it.
   * @param arc The arc to revise.
   * @return Whether the domain of the arc's primary / first variable was changed without any domain wipeout.
   * @throws EmptyDomainException If an arc revision resulted in a domain wipeout.
   */
  protected boolean revise(Arc arc) throws EmptyDomainException {
    // Boolean value to track whether the domain was changed.
    boolean changed = false;
    revisionsDone++;
    // Try to find the constraint that matches the arc.
    for (BinaryConstraint binaryConstraint : instance.constraints) {
      if (binaryConstraint.matches(arc)) {
        // Check each value in the arc's primary / first variable's domain for support.

        Iterator<Integer> domainIterator = instance.domains.get(arc.getVal1()).iterator();
        while (domainIterator.hasNext()) {
          int val1 = domainIterator.next();

          // To be supported, the value must have a matching value in the second variable's domain to satisfy the constraint.
          boolean valSupported = false;
          for (int val2 : instance.domains.get(arc.getVal2())) {
            // Check whether the constraint has a tuple matching the pairs of values.
            valSupported = binaryConstraint.supportsTuple(arc, new BinaryTuple(val1, val2));
            if (valSupported) {
              break;
            }
          }
          // If the value is not supported, remove it from the domain of the variable.
          // Remove the value from the iterator first to avoid an error.
          // Then prune the domain and updated the changed bool variable.
          if (!valSupported) {
            domainIterator.remove();
            pruneDomain(arc.getVal1(), val1);
            changed = true;

            // Check whether the domain is empty.
            if (instance.domains.get(arc.getVal1()).isEmpty()) {
              throw new EmptyDomainException("Domain wipeout when revising arcs!");
            }
          }
        }
        // Do not look for any further constraints as there should only be one matching one.
        break;
      }
    }
    return changed;
  }

  /**
   * Prints the solution and increment the solutions counter.
   */
  protected void showSolution() {
    StringBuilder stringBuilder = new StringBuilder("Found solution!\n");
    for (Set<Integer> domain : instance.domains) {
      stringBuilder.append(domain.iterator().next());
      stringBuilder.append('\n');
    }
    System.out.println(stringBuilder.toString());
    solutionsFound++;
  }

  /**
   * Prints solver information.
   */
  protected void printInfo() {
    if (solutionsFound == 0) {
      System.out.println("Failed to find a solution!");
    } else {
      System.out.println("Found " + solutionsFound + " solutions!");
    }
    System.out.println("Explored " + nodesExplored + " nodes!");
    System.out.println("Performed " + revisionsDone + " arc revisions!");
  }
}
