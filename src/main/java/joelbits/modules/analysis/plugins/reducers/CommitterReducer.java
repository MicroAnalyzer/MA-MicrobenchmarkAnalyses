package joelbits.modules.analysis.plugins.reducers;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.*;

public final class CommitterReducer extends Reducer<Text, Text, Text, Text> {
    private final Set<String> committers = new HashSet<>();
    private final Set<String> benchmarkers = new HashSet<>();

    @Override
    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        for (Text value : values) {
            if (!committers.contains(value.toString())) {
                committers.add(value.toString());
            }
            if (key.toString().equals("benchmarker") && !benchmarkers.contains(value.toString())) {
                benchmarkers.add(value.toString());
            }
        }
    }

    @Override
    public void cleanup(Context context) {
        try {
            context.write(new Text("committers: "), new Text(committers.size() + System.lineSeparator()));
            context.write(new Text("benchmarkers: "), new Text(benchmarkers.size() + ""));
        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }
}
