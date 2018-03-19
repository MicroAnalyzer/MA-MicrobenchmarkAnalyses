package joelbits.modules.analysis.plugins.mappers;

import joelbits.modules.analysis.plugins.utils.AnalysisUtil;
import joelbits.modules.analysis.visitors.BenchmarkConfigurationVisitor;
import joelbits.model.ast.ASTRoot;
import joelbits.model.project.CodeRepository;
import joelbits.model.project.Project;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class BenchmarkConfigurationMapper extends Mapper<Text, BytesWritable, Text, Text> {
    private final List<String> processedBenchmarkFiles = new ArrayList<>();

    @Override
    public void map(Text key, BytesWritable value, Context context) throws IOException, InterruptedException {
        Project project = AnalysisUtil.getProject(value);
        for (CodeRepository repository : project.getRepositories()) {
            Set<ASTRoot> benchmarkFiles = AnalysisUtil.latestFileSnapshots(repository);

            for (ASTRoot changedFile : benchmarkFiles) {
                String declarationName = changedFile.getNamespaces().get(0).getDeclarations().get(0).getName();
                if (processedBenchmarkFiles.contains(declarationName)) {
                    continue;
                }
                processedBenchmarkFiles.add(declarationName);

                BenchmarkConfigurationVisitor visitor = new BenchmarkConfigurationVisitor();
                changedFile.accept(visitor);
                writeClassConfigurations(context, declarationName, visitor);

                writeBenchmarkConfigurations(context, declarationName, visitor);
            }
        }
    }

    private void writeBenchmarkConfigurations(Context context, String declarationName, BenchmarkConfigurationVisitor visitor) throws IOException, InterruptedException {
        for (Map.Entry<String, Map<String, List<String>>> configuration : visitor.getBenchmarkConfigurations().entrySet()) {
            for (Map.Entry<String, List<String>> benchmark : configuration.getValue().entrySet()) {
                if (!StringUtils.isEmpty(benchmark.getKey())) {
                    context.write(new Text(declarationName + ":" + configuration.getKey()), new Text("@" + benchmark.getKey() + "(" + StringUtils.join(benchmark.getValue(), ",").trim() + ")"));
                }
            }
        }
    }

    private void writeClassConfigurations(Context context, String declarationName, BenchmarkConfigurationVisitor visitor) throws IOException, InterruptedException {
        Map<String, List<String>> classConfigurations = visitor.getClassConfigurations();
        for (Map.Entry<String, List<String>> configuration : classConfigurations.entrySet()) {
            context.write(new Text(declarationName), new Text("@" + configuration.getKey() + "(" + StringUtils.join(configuration.getValue(), ",").trim() + ")"));
        }
    }
}
