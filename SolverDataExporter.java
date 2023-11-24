
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SolverDataExporter {
    static final String defaultFileName = "BinaryCSPSolver_Output_Data.csv";
    static final String outputFolder = "Results/";

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println(
                    "Usage: args <instancesDirectoryPath> [outputFilename] [numberOfSolutions].\nYou must pass in the path to a folder containing instances. The output filename and number of solutions are optional.");
        } else {
            SolverDataExporter solverDataExporter = new SolverDataExporter();
            String instancesDirectoryPath = args[0];
            String outputFilename = defaultFileName;
            int numberOfSolutions = 1;
            if (args.length > 1) {
                outputFilename = args[1];
                if (args.length > 2) {
                    numberOfSolutions = Integer.parseInt(args[2]);
                }
            }
            solverDataExporter.RunAndSaveResults(instancesDirectoryPath, outputFilename, numberOfSolutions);
        }
    }

    private void RunAndSaveResults(String directoryPath, String outputFilename, int solutionsToFind) {
        // Read all info files at the given location.
        // Recursively list files in Java - Brett Ryan - https://stackoverflow.com/questions/2056221/recursively-list-files-in-java - Accessed 22.10.2023            
        try (Stream<Path> stream = Files.walk(Paths.get(directoryPath))) {
            // Search for all .info files recursively at the given path and sort the stream in alphabetical order.
            List<Path> files = stream.sorted().filter(Files::isRegularFile)
                    .filter(f -> f.toString().toLowerCase().endsWith(".csp")).toList();

            // Read each file's data and store it in a list.
            // Choosing the best concurrency list in Java - Travis Webb - https://stackoverflow.com/questions/8203864/choosing-the-best-concurrency-list-in-java - Accessed 22.11.2023
            List<List<String>> csvRows = Collections.synchronizedList(new ArrayList<List<String>>());
            List<String> headers = List.of("Instance", "Solver Type", "Solutions To Find", "Variable Ordering",
                    "Value Ordering", "Solutions Found", "Nodes Explored", "Revisions Done", "Time Taken");
            csvRows.add(headers);

            // Run the problems across multiple threads.
            // wait until all threads finish their work in java - Peter Lawrey - https://stackoverflow.com/questions/7939257/wait-until-all-threads-finish-their-work-in-java - Accessed 22.11.2023
            ExecutorService es = Executors.newCachedThreadPool();
            for (String solverType : new String[] { "MAC", "FC" }) {
                for (BinaryCSPFCSolver.VarSelectMode varSelectMode : BinaryCSPSolver.VarSelectMode.values()) {
                    // Switch with this when running medium Sudoku.
                    // BinaryCSPFCSolver.VarSelectMode varSelectMode = BinaryCSPFCSolver.VarSelectMode.SMALLEST_DOMAIN;
                    for (BinaryCSPFCSolver.ValSelectMode valSelectMode : BinaryCSPSolver.ValSelectMode.values()) {
                        for (Path instanceFilePath : files) {
                            Runnable runnable = createConfigRunnable(solutionsToFind, solverType, varSelectMode,
                                    valSelectMode, instanceFilePath, csvRows);
                            es.execute(runnable);
                        }
                    }
                }
            }
            es.shutdown();
            try {
                boolean finished = es.awaitTermination(1, TimeUnit.HOURS);
                if (finished) {
                    ExportToCSV(csvRows, outputFilename + ".csv");
                } else {
                    ExportToCSV(csvRows, outputFilename + "_TIMEOUT.csv");
                }
            } catch (InterruptedException e) {
                System.err.println("Executor Service was interrupted!");
                ExportToCSV(csvRows, outputFilename + "_INTERRUPTED.csv");
            }

        } catch (IOException e) {
            System.out.println("Error when trying to get path:\n" + e.toString());
        }
    }

    public Runnable createConfigRunnable(int solutionsToFind, String solverType,
            BinaryCSPFCSolver.VarSelectMode varSelectMode,
            BinaryCSPFCSolver.ValSelectMode valSelectMode, Path instanceFilePath, List<List<String>> csvRows) {
        return new Runnable() {
            @Override
            public void run() {
                BinaryCSPSolver solver;
                BinaryCSP instance = new BinaryCSPReader().readBinaryCSP(instanceFilePath.toString());
                if (solverType.equals("MAC")) {
                    solver = new BinaryCSPMACSolver(
                            instance, solutionsToFind, varSelectMode, valSelectMode, false);
                } else {
                    solver = new BinaryCSPFCSolver(
                            instance, solutionsToFind, varSelectMode, valSelectMode, false);
                }

                solver.solve();

                List<String> row = new ArrayList<String>();
                String[] splitPath = instanceFilePath.toString().split("/");
                String instanceName = splitPath[splitPath.length - 1];
                row.add(instanceName);
                row.add(solverType);
                row.add(String.valueOf(solutionsToFind));
                row.add(String.valueOf(varSelectMode));
                row.add(String.valueOf(valSelectMode));
                row.addAll(ReadSolverResults(solver));
                csvRows.add(row);
            }
        };
    }

    // Reading a CSV File into an Array - Baeldung - https://www.baeldung.com/java-csv-file-array - Accessed 22.10.2023
    private List<String> ReadSolverResults(BinaryCSPSolver solver) {
        List<String> csvList = new ArrayList<String>();
        csvList.add(String.valueOf(solver.solutionsFound));
        csvList.add(String.valueOf(solver.nodesExplored));
        csvList.add(String.valueOf(solver.revisionsDone));
        csvList.add(String.valueOf(solver.timeTaken));
        return csvList;
    }

    // How to Write to a CSV File in Java - Baeldung - https://www.baeldung.com/java-csv - Accessed 22.10.2023
    // Converting 'ArrayList<String> to 'String[]' in Java - Bozho - https://stackoverflow.com/questions/4042434/converting-arrayliststring-to-string-in-java - Accessed 22.10.2023
    public String convertToCSV(List<String> data) {
        return Stream.of(data.toArray(new String[0])).collect(Collectors.joining(","));
    }

    public boolean ExportToCSV(List<List<String>> csvRows) throws IOException {
        return ExportToCSV(csvRows, defaultFileName);
    }

    public boolean ExportToCSV(List<List<String>> csvRows, String fileName) throws IOException {
        if (csvRows.size() > 1) {
            File csvOutputFile = new File(outputFolder + fileName);
            try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
                csvRows.stream()
                        .map(this::convertToCSV)
                        .forEach(pw::println);
            }
            return csvOutputFile.exists();
        }
        System.out.println("No instances were run!");
        return false;
    }
}