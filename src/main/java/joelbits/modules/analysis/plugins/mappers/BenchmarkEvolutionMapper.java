package joelbits.modules.analysis.plugins.mappers;

import joelbits.modules.analysis.plugins.utils.AnalysisUtil;
import joelbits.modules.analysis.plugins.visitors.BenchmarkEvolutionVisitor;
import joelbits.model.ast.ASTRoot;
import joelbits.model.project.CodeRepository;
import joelbits.model.project.Project;
import joelbits.model.project.Revision;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;
import java.util.List;

public final class BenchmarkEvolutionMapper extends Mapper<Text, BytesWritable, Text, Text> {
    private final AnalysisUtil analysisUtil = new AnalysisUtil();

    @Override
    public void map(Text key, BytesWritable value, Context context) throws IOException, InterruptedException {
        Project project = analysisUtil.getProject(value);
        for (CodeRepository repository : project.getRepositories()) {
            for (Revision revision : repository.getRevisions()) {
                List<ASTRoot> benchmarkFiles = analysisUtil.allChangedBenchmarkFiles(revision, repository.getUrl());
                for (ASTRoot changedFile : benchmarkFiles) {
                    String declarationName = changedFile.getNamespaces().get(0).getDeclarations().get(0).getName();
                    BenchmarkEvolutionVisitor visitor = new BenchmarkEvolutionVisitor();
                    changedFile.accept(visitor);
                    for (String change : visitor.getBenchmarkChanges()) {
                        context.write(new Text(project.getName() + ":" + declarationName + ":" + change), new Text(revision.getCommitDate().toString() + " log: " + revision.getLog()));
                    }
                }
            }
        }
        context.write(new Text(project.getName()), new Text(project.getCreatedDate().toString()));
    }
}
