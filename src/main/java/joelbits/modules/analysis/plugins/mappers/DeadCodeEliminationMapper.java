package joelbits.modules.analysis.plugins.mappers;

import joelbits.model.ast.ASTRoot;
import joelbits.model.project.CodeRepository;
import joelbits.model.project.Project;
import joelbits.modules.analysis.plugins.utils.AnalysisUtil;
import joelbits.modules.analysis.plugins.visitors.DeadCodeEliminationVisitor;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class DeadCodeEliminationMapper extends Mapper<Text, BytesWritable, Text, Text> {
    private final List<String> processedBenchmarkFiles = new ArrayList<>();
    private final AnalysisUtil analysisUtil = new AnalysisUtil();

    @Override
    public void map(Text key, BytesWritable value, Context context) throws IOException, InterruptedException {
        int nrOfBenchmarks = 0;
        int allowsDCE = 0;

        Project project = analysisUtil.getProject(value);
        DeadCodeEliminationVisitor visitor = new DeadCodeEliminationVisitor();
        for (CodeRepository repository : project.getRepositories()) {
            Set<ASTRoot> benchmarkFiles = analysisUtil.latestFileSnapshots(repository);

            for (ASTRoot changedFile : benchmarkFiles) {
                String declarationName = "";
                if (changedFile.getNamespaces().isEmpty()) {
                    declarationName = "default";
                } else if (changedFile.getNamespaces().get(0).getDeclarations().isEmpty()) {
                    declarationName = "missing_declaration";
                } else {
                    declarationName = changedFile.getNamespaces().get(0).getDeclarations().get(0).getName();
                }

                if (processedBenchmarkFiles.contains(declarationName)) {
                    continue;
                }
                processedBenchmarkFiles.add(declarationName);

                visitor.resetAllowDCE();
                visitor.resetBenchmarks();
                changedFile.accept(visitor);
                nrOfBenchmarks += visitor.getNrBenchmarks();
                allowsDCE += visitor.getAllowDCE();
            }
        }
        context.write(new Text(Integer.toString(nrOfBenchmarks)), new Text(Integer.toString(allowsDCE)));
    }
}
