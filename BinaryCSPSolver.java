import java.util.*;

public abstract class BinaryCSPSolver {
  protected BinaryCSPSolver(BinaryCSP instance) {
    this.instance = instance;
    stateChanges = new Stack<BinaryCSPStateChange>();
  }

  /**
   * Method that will solve the CSP.
   */
  abstract void solve();

  // The instance to solve.
  BinaryCSP instance;

  // The number of solutions found.
  int solutionsFound = 0;

  final boolean DEBUG_MODE = false;

  // A states stack for each depth of search.
  // Each state has a list of variable domains.
  // Each domain is a set of integers.
  Stack<BinaryCSPStateChange> stateChanges;

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
  protected boolean enterNewState() {
    return stateChanges.add(new BinaryCSPStateChange(instance));
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
    boolean changed = false;
    Iterator<Integer> domainIterator = instance.domains.get(var).iterator();
    while (domainIterator.hasNext()) {
      int otherVal = domainIterator.next();
      if (val != otherVal) {
        domainIterator.remove();
        if (pruneDomain(var, otherVal)) {
          changed = true;
        }
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
   */
  protected boolean unassign(int var, int val) {
    //instance.domains.get(var).remove(val);
    boolean changed = pruneDomain(var, val);
    if (DEBUG_MODE) {
      System.out.println("Set var " + var + " != " + val);
    }
    return changed;
  }

  /**
   * Remove / prune a specific value from a variable's domain.
   * @param var The variable to remove the value from.
   * @param val The value to remove.
   * @return Whether the value was removed / pruned successfully.
   */
  private boolean pruneDomain(int var, int val) {
    //boolean pruned = instance.domains.get(var).remove(val);
    //if (pruned) {
    currentStateChanges().domainPrunes.get(var).add(val);
    return removeInvalidConstraints(var, val);
    //}
    //return pruned;
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
    //return selectVarAscending();
    return selectVarSmallestDomain();
  }

  protected int selectVar(Set<Integer> varSet) {
    return selectVarAscending(varSet);
    //return selectVarSmallestDomain();
  }

  /**
   * Selects a non-assigned variable based on ascending order.
   * @return The non-assigned variable with the smallest number.
   */
  private int selectVarAscending() {
    for (int var = 0; var < instance.getNoVariables(); var++) {
      if (instance.domains.get(var).size() > 1) {
        return var;
      }
    }
    System.out.println("Trying to select variable when all are assigned! Returning default 0.");
    return 0;
  }

  private int selectVarAscending(Set<Integer> varSet) {
    return varSet.iterator().next();
  }

  /**
   * Select a non-assigned variable based on which has the smallest domain.
   * @return The non-assigned variable with the smallest domain.
   */
  private int selectVarSmallestDomain() {
    int smallestDomainVar = -1;
    int smallestDomainSize = Integer.MAX_VALUE;
    for (int var = 0; var < instance.domains.size(); var++) {
      int domainSize = instance.domains.get(var).size();
      if (domainSize > 1 && domainSize < smallestDomainSize) {
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
   * Select the first value in the domain of a given variable.
   * Throws an exception if the domain is empty.
   * @param var The variable with the domain to get a value from.
   * @return The first value in the domain of the variable.
   */
  protected int selectVal(int var) {
    Set<Integer> domain = instance.domains.get(var);
    return domain.iterator().next();
  }

  /**
   * Checks whether assignments have been made for all variables.
   * @return Whether all variables have assignments.
   */
  protected boolean completeAssignments() {
    boolean completedAssignments = true;
    for (int var = 0; var < instance.domains.size() && completedAssignments; var++) {
      completedAssignments = instance.domains.get(var).size() == 1;
    }
    return completedAssignments;
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
  private void createArcs(BinaryConstraint constraint, Queue<Arc> queue) {
    queue.add(new Arc(constraint.getFirstVar(), constraint.getSecondVar()));
    queue.add(new Arc(constraint.getSecondVar(), constraint.getFirstVar()));
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

  /**
   * An arc revision that removes any domain values not supporting it.
   * @param arc The arc to revise.
   * @return whether the domain of the arc's primary / first variable was changed without any domain wipeout.
   */
  protected boolean revise(Arc arc) throws EmptyDomainException {
    // Boolean value to track whether the domain was changed.
    boolean changed = false;
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
              throw new EmptyDomainException("Domain wipeout!");
            }
          }
        }
        // Do not look for any further constraints as there should only be one matching one.
        break;
      }
    }
    if (changed && instance.domains.get(arc.getVal1()).size() == 1 && DEBUG_MODE) {
      System.out
          .println("Set var " + arc.getVal1() + " = " + instance.domains.get(arc.getVal1()).iterator().next()
              + " (Implicit)");
    }
    return changed;
  }

  /**
   * Print the solution and increment the solutions counter.
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
}
