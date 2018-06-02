package joelbits.modules.analysis.plugins.reducers;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.*;

public final class EvolutionReducer extends Reducer<Text, Text, Text, Text> {
    private final Map<String, Map<String, String>> benchmarkEvolution = new HashMap<>();

    @Override
    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        for (Text value : values) {
            try {
                String projectName = StringUtils.EMPTY;
                String benchmark = StringUtils.EMPTY;
                String changes = StringUtils.EMPTY;
                if (value.toString().contains(":")) {
                    String[] parts = value.toString().split(":");
                    projectName = parts[0];
                    benchmark = parts[1];
                    changes = parts[2];
                } else {
                    continue;
                }

                if (key.toString().equals("benchmark")) {
                    if (!benchmarkEvolution.containsKey(projectName)) {
                        benchmarkEvolution.put(projectName, new HashMap<>());
                    }
                    if (!benchmarkEvolution.get(projectName).containsKey(benchmark)) {
                        benchmarkEvolution.get(projectName).put(benchmark, changes);
                    }
                }
            } catch (Exception e) {
                System.err.println(e.toString());
            }
        }
    }

    @Override
    public void cleanup(Context context) {
        for (Map.Entry<String, Map<String, String>> project : benchmarkEvolution.entrySet()) {
            try {
                int benchmarks = project.getValue().size();
                context.write(new Text(System.lineSeparator() + "In " + project.getKey()), new Text(" there are " + benchmarks + " microbenchmarks" + System.lineSeparator()));
                for (Map.Entry<String, String> benchmark : project.getValue().entrySet()) {
                    context.write(new Text(benchmark.getKey() + " has been modified "), new Text(benchmark.getValue() + " times" + System.lineSeparator()));
                }
            } catch (Exception e) {
                System.err.println(e.toString());
            }
        }
    }
}
