package joelbits.modules.analysis.plugins.reducers;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class RatioReducer extends Reducer<Text, Text, Text, Text> {
    private final Map<String, Integer> ratios = new HashMap<>();

    @Override
    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        for (Text value : values) {
            if (ratios.containsKey(key.toString())) {
                int occurrences = ratios.get(key.toString());
                ratios.put(key.toString(), ++occurrences);
            } else {
                ratios.put(key.toString(), 1);
            }
        }
    }

    @Override
    public void cleanup(Context context) {
        try {
            for (Map.Entry<String, Integer> ratio : ratios.entrySet()) {
                context.write(new Text(ratio.getKey()), new Text(ratio.getValue() + System.lineSeparator()));
            }
        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }
}
