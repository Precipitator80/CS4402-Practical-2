# CS4402-Practical-2: Binary CSP Solvers

The specification detailed the creation of two binary constraint satisfaction problem solvers, one using Forward Checking (FC) and the other Maintaining Arc Consistency (MAC). Both solvers employ 2-way branching and were augmented with two variable and value ordering strategies to choose from. The performance of the solvers and selection strategies were evaluated against instances of Langford's problem, the n-queens problem and Sudoku. Finally, it is observed how solver performances varies among different instances of the three problems.

[See the project report here.](190018469-CS4402-Practical-2.pdf)

To compile, run the following:
```bash
Makefile all
```

After this, the solvers can be run with the following command:
```
java BinaryCSPSolver <file.csp> [solverType] [solutionsToFind] [varSelectMode] [valSelectMode] [debugMode]
```

file.csp: The path to a problem instance to solve.  
solverType: The solver type to use (MAC / FC).  
solutionsToFind (Optional): The number of solutions to find before stopping. 0 will attempt to find all solutions.  
varSelectMode (Optional): The mode to use when selecting a variable to assign (0 = Ascending, 1 = Min Domain).  
valSelectMode (Optional): The mode to use when selecting a value to assign to a variable (0 = Ascending, 1 = Min Conflicts).  
debugMode (Optional): Whether to log additional information to show each step taken by the solver (True / False). Useful for debugging and full understanding.  
