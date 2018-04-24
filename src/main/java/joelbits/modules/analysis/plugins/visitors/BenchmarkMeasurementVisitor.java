package joelbits.modules.analysis.plugins.visitors;

import joelbits.model.ast.*;
import joelbits.model.ast.types.ModifierType;
import joelbits.modules.analysis.visitors.Visitor;

import java.util.*;

/**
 * Collects the @Benchmark for every method in a class that is missing the @Warmup, @Measurement, and @Fork
 * annotations. This is done because a @Benchmark will run with default settings (warmup = 20 iterations,
 * forks = 10, measurement = 20 iterations) unless any of these are overriden with an explicit annotation of the
 * three mentioned above. Also, if @Warmup, @Measurement, and/or @Fork are given on a class level, these will be
 * added to each @Benchmark method, unless any of these annotations are explicit on the method level, since that
 * would override the class level configuration.
 *
 * The reason for collecting the class level annotations as well as the method level @Benchmark annotations is
 * because, for example, @Warmup could be configured on class level which then applies to all methods so in that
 * case there is no need to configure each @Benchmark and thus they are probably empty. In order to know how many
 * iterations, forks, etc, that are run for each @Benchmark we must look at the corresponding class level annotations.
 */
public final class BenchmarkMeasurementVisitor implements Visitor {
    private final Map<String, Map<String, List<String>>> benchmarkConfigurations = new HashMap<>();       // key is method name that have @Benchmark annotation, value is list of benchmark configuration parameters
    private final Map<String, List<String>> classConfigurations = new HashMap<>();                        // key is annotation name, value is list of the annotations configuration parameters

    @Override
    public boolean visit(ASTNode node) {
        return false;
    }

    @Override
    public boolean visitEnter(ASTNode node) {
        if (node instanceof Declaration) {
            Declaration declaration = (Declaration) node;
            classConfigurations.clear();
            extractClassConfiguration(declaration);

            benchmarkConfigurations.clear();
            for (Method method : declaration.getMethods()) {
                extractBenchmarkConfiguration(method);
            }

            return false;
        }

        return !(node instanceof Variable) && !(node instanceof Method);
    }

    private void extractBenchmarkConfiguration(Method method) {
        if (method.getModifiers().stream().anyMatch(this::isBenchmark)) {
            benchmarkConfigurations.put(method.getName(), new HashMap<>());
            for (Modifier modifier : method.getModifiers()) {
                if (isMeasurementConfiguration(modifier.getName()) || isBenchmark(modifier)) {
                    benchmarkConfigurations.get(method.getName()).put(modifier.getName(), modifier.getMembersAndValues());
                }
            }
        }
    }

    private boolean isBenchmark(Modifier modifier) {
        return modifier.getName().equals("Benchmark") && modifier.getType().equals(ModifierType.ANNOTATION);
    }

    private void extractClassConfiguration(Declaration declaration) {
        for (Modifier modifier : declaration.getModifiers()) {
            if (modifier.getType().equals(ModifierType.ANNOTATION) && isMeasurementConfiguration(modifier.getName())) {
                classConfigurations.put(modifier.getName(), modifier.getMembersAndValues());
            }
        }
    }

    private boolean isMeasurementConfiguration(String configuration) {
        return configuration.equals("Warmup") || configuration.equals("Measurement") || configuration.equals("Fork");
    }

    @Override
    public boolean visitLeave(ASTNode node) {
        return !(node instanceof Variable);
    }

    public Map<String, Map<String, List<String>>> getBenchmarkConfigurations() {
        return benchmarkConfigurations;
    }

    public Map<String, List<String>> getClassConfigurations() {
        return classConfigurations;
    }
}
