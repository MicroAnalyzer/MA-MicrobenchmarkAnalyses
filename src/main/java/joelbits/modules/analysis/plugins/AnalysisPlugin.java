package joelbits.modules.analysis.plugins;

import com.google.auto.service.AutoService;
import joelbits.modules.analysis.plugins.mappers.*;
import joelbits.modules.analysis.plugins.reducers.*;
import joelbits.modules.analysis.plugins.spi.Analysis;
import joelbits.modules.analysis.plugins.visitors.DeadCodeEliminationSourceCodeVisitor;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

@AutoService(Analysis.class)
public class AnalysisPlugin implements Analysis {
    @Override
    public Class<? extends Mapper> mapper(String mapper) {
        switch (mapper) {
            case "dce":
                return DeadCodeEliminationMapper.class;
            case "cf":
                return ConstantFoldingMapper.class;
            case "sourcedce":
                return DeadCodeEliminationSourceCodeMapper.class;
            case "sourcecf":
                return ConstantFoldingSourceCodeMapper.class;
            case "configurations":
                return BenchmarkConfigurationMapper.class;
            case "count":
                return BenchmarkCountMapper.class;
            case "changes":
                return BenchmarkEvolutionMapper.class;
            case "evolution":
                return EvolutionMapper.class;
            case "measurement":
                return BenchmarkMeasurementMapper.class;
            case "benchmarkers":
                return CommitterMapper.class;
            case "words":
                return LogWordsMapper.class;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public Class<? extends Reducer> reducer(String reducer) {
        switch (reducer) {
            case "dce":
            case "cf":
            case "sourcedce":
            case "sourcecf":
                return BenchmarkOptimizationReducer.class;
            case "configurations":
            case "count":
            case "changes":
                return BenchmarkReducer.class;
            case "evolution":
                return EvolutionReducer.class;
            case "words":
                return RatioReducer.class;
            case "benchmarkers":
                return CommitterReducer.class;
            case "measurement":
                return BenchmarkMeasurementReducer.class;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public String toString() {
        return "thesis";
    }
}
