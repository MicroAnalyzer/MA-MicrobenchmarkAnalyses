package joelbits.modules.analysis.plugins.reducers;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public final class BenchmarkMeasurementReducer extends Reducer<Text, Text, Text, Text> {
    private static final Logger log = LoggerFactory.getLogger(BenchmarkMeasurementReducer.class);
    private final Map<String, Integer> configurations = new HashMap<>();

    @Override
    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        List<String> sortedValues = new ArrayList<>();
        for (Text value : values) {
            sortedValues.add(value.toString());
        }
        Collections.sort(sortedValues);
        String configuration = StringUtils.join(sortedValues.toArray(), " ");

        if (configurations.containsKey(configuration)) {
            int occurrences = configurations.get(configuration);
            configurations.put(configuration, ++occurrences);
        } else {
            configurations.put(configuration, 1);
        }
    }

    @Override
    public void cleanup(Context context) {
        try {
            for (Map.Entry<String, Integer> configuration : configurations.entrySet()) {
                context.write(new Text(configuration.getKey()), new Text(configuration.getValue() + System.lineSeparator()));
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
    }
}
