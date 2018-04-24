package joelbits.modules.analysis.plugins.reducers;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class DeadCodeEliminationReducer extends Reducer<Text, Text, Text, Text> {
    private final List<String> benchmarks = new ArrayList<>();
    private final List<String> allowsDCE = new ArrayList<>();

    @Override
    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        benchmarks.add(key.toString());
        for (Text value : values) {
            allowsDCE.add(value.toString());
        }
    }

    @Override
    public void cleanup(Context context) {
        int nrOfBenchmarks = 0;
        int allowsDCE = 0;
        try {
            for (String bench : benchmarks) {
                nrOfBenchmarks += Integer.valueOf(bench);
            }
            for (String dce : this.allowsDCE) {
                allowsDCE += Integer.valueOf(dce);
            }
        } catch (Exception e) {
            System.err.println(e.toString());
        }

        try {
            context.write(new Text("Benchmarks: " + nrOfBenchmarks), new Text("allowsDCE: " + allowsDCE));
        } catch (IOException | InterruptedException e) {
            System.err.println(e.toString());
        }
    }
}
