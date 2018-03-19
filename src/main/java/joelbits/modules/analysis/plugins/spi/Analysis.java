package joelbits.modules.analysis.plugins.spi;

import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

public interface Analysis {
    Class<? extends Mapper> mapper(String mapper) throws IllegalArgumentException;
    Class<? extends Reducer> reducer(String reducer) throws IllegalArgumentException;
}
