package joelbits.modules.analysis.plugins.visitors;

import joelbits.model.ast.*;
import joelbits.model.ast.types.ModifierType;
import joelbits.modules.analysis.visitors.Visitor;

import java.util.*;

/**
 * Collects all annotations of each class, and also collects the @Benchmark for every method in each class
 * that has one. If a configuration exist inside the @Benchmark the configuration will be added to the list
 * of the corresponding @Benchmark, otherwise the list will be empty if no configuration of the @Benchmark annotation.
 *
 * The reason for collecting the class level annotations as well as the method level @Benchmark annotations is
 * because, for example, @Warmup could be configured on class level which then applies to all methods so in that
 * case there is no need to configure each @Benchmark and thus they are probably empty. In order to know how many
 * iterations, forks, etc, that are run for each @Benchmark we must look at the corresponding class level annotations.
 */
public final class BenchmarkConfigurationVisitor implements Visitor {
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
                benchmarkConfigurations.get(method.getName()).put(modifier.getName(), modifier.getMembersAndValues());
            }
        }
    }

    private boolean isBenchmark(Modifier modifier) {
        return modifier.getName().equals("Benchmark") && modifier.getType().equals(ModifierType.ANNOTATION);
    }

    private void extractClassConfiguration(Declaration declaration) {
        for (Modifier modifier : declaration.getModifiers()) {
            if (modifier.getType().equals(ModifierType.ANNOTATION)) {
                classConfigurations.put(modifier.getName(), modifier.getMembersAndValues());
            }
        }
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
