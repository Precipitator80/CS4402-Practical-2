# CS4402-Practical-2
To compile, run the following:
```bash
Makefile all
```

After this, the Maintaining Arc Consistency and Forward Checking solvers can be run with the following commands:
```
java BinaryCSPMACSolver <file.csp> [solutionsToFind] [varSelectMode] [valSelectMode] [debugMode]
java BinaryCSPFCSolver <file.csp> [solutionsToFind] [varSelectMode] [valSelectMode] [debugMode]
```

file.csp: The path to a problem instance to solve.  
solutionsToFind (Optional): The number of solutions to find before stopping. 0 will attempt to find all solutions.  
varSelectMode (Optional): The mode to use when selecting a variable to assign (0 = Ascending, 1 = Min Domain).  
valSelectMode (Optional): The mode to use when selecting a value to assign to a variable (0 = Ascending, 1 = Min Conflicts).  
debugMode (Optional): Whether to log additional information to show each step taken by the solver (True / False). Useful for debugging and full understanding.  