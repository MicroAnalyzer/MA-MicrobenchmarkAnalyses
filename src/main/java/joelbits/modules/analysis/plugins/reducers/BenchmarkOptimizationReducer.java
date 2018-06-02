package joelbits.modules.analysis.plugins.reducers;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class BenchmarkOptimizationReducer extends Reducer<Text, Text, Text, Text> {
    private final List<String> benchmarks = new ArrayList<>();
    private final List<String> optimizations = new ArrayList<>();
    private final Map<String, String> optimizationsPerProject = new HashMap<>();

    @Override
    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        if (key.toString().equals("DCE") || key.toString().equals("CF")) {
            for (Text value : values) {
                String[] parts = value.toString().split(":");
                String project = parts[0];
                String benchmarkOptimizations = parts[1];
                if (!optimizationsPerProject.containsKey(project)) {
                    optimizationsPerProject.put(project, benchmarkOptimizations);
                }
            }
        } else {
            benchmarks.add(key.toString());
            for (Text value : values) {
                optimizations.add(value.toString());
            }
        }
    }

    @Override
    public void cleanup(Context context) {
        int nrOfBenchmarks = 0;
        int optimizations = 0;
        try {
            for (String benchmark : benchmarks) {
                nrOfBenchmarks += Integer.valueOf(benchmark);
            }
            for (String optimization : this.optimizations) {
                optimizations += Integer.valueOf(optimization);
            }
        } catch (Exception e) {
            System.err.println(e.toString());
        }

        try {
            context.write(new Text("Benchmarks: " + nrOfBenchmarks), new Text("optimizations: " + optimizations + System.lineSeparator()));
            for (Map.Entry<String, String> project : optimizationsPerProject.entrySet()) {
                context.write(new Text(project.getKey() + "  "), new Text(project.getValue() + System.lineSeparator()));
            }
        } catch (IOException | InterruptedException e) {
            System.err.println(e.toString());
        }
    }
}
