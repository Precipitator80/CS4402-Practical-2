
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SolverDataExporter {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println(
                    "Error. Arguments: directoryPath outputFilename.\nYou must pass in the path to a folder containing instances. The output filename is optional.");
        } else {
            SolverDataExporter solverDataExporter = new SolverDataExporter();
            solverDataExporter.RunAndSaveResults(args[0]);
        }
    }

    private void RunAndSaveResults(String directoryPath) {
        // Read all info files at the given location.
        // Recursively list files in Java - Brett Ryan - https://stackoverflow.com/questions/2056221/recursively-list-files-in-java - Accessed 22.10.2023            
        try (Stream<Path> stream = Files.walk(Paths.get(directoryPath))) {
            // Search for all .info files recursively at the given path and sort the stream in alphabetical order.
            List<Path> files = stream.sorted().filter(Files::isRegularFile)
                    .filter(f -> f.toString().toLowerCase().endsWith(".csp")).toList();

            // Read each file's data and store it in a list.
            List<List<String>> csvRows = new ArrayList<List<String>>();
            List<String> headers = List.of("Instance", "Solver Type", "Solutions To Find", "Variable Ordering",
                    "Value Ordering", "Solutions Found", "Nodes Explored", "Revisions Done", "Time Taken");
            csvRows.add(headers);

            ExecutorService es = Executors.newCachedThreadPool();
            for (int solutionsToFind : new int[] { 0, 1 }) {
                for (String solverType : new String[] { "MAC", "FC" }) {
                    for (BinaryCSPFCSolver.VarSelectMode varSelectMode : BinaryCSPSolver.VarSelectMode.values()) {
                        for (BinaryCSPFCSolver.ValSelectMode valSelectMode : BinaryCSPSolver.ValSelectMode.values()) {
                            for (Path instanceFilePath : files) {
                                Runnable runnable = createConfigRunnable(solutionsToFind, solverType, varSelectMode,
                                        valSelectMode, instanceFilePath, csvRows);
                                es.execute(runnable);
                            }
                        }
                    }
                }
            }
            es.shutdown();
            try {
                boolean finished = es.awaitTermination(60, TimeUnit.MINUTES);
                if (finished) {
                    ExportToCSV(csvRows);
                } else {
                    ExportToCSV(csvRows, defaultFileName + "_TIMEOUT");
                }
            } catch (InterruptedException e) {
                System.err.println("Executor Service was interrupted!");
                ExportToCSV(csvRows, defaultFileName + "_INTERRUPTED");
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

    static final String defaultFileName = "BinaryCSPSolver_Output_Data.csv";

    public boolean ExportToCSV(List<List<String>> csvRows) throws IOException {
        return ExportToCSV(csvRows, defaultFileName);
    }

    public boolean ExportToCSV(List<List<String>> csvRows, String fileName) throws IOException {
        File csvOutputFile = new File(fileName);
        try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
            csvRows.stream()
                    .map(this::convertToCSV)
                    .forEach(pw::println);
        }
        return csvOutputFile.exists();
    }
}