package joelbits.modules.analysis.plugins.mappers;

import joelbits.modules.analysis.plugins.utils.AnalysisUtil;
import joelbits.modules.analysis.plugins.visitors.BenchmarkCountVisitor;
import joelbits.model.ast.ASTRoot;
import joelbits.model.project.CodeRepository;
import joelbits.model.project.Project;
import joelbits.model.project.Revision;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;
import java.util.*;

public final class EvolutionMapper extends Mapper<Text, BytesWritable, Text, Text> {
    private final AnalysisUtil analysisUtil = new AnalysisUtil();

    @Override
    public void map(Text key, BytesWritable value, Context context) throws IOException, InterruptedException {
        try {
            Project project = analysisUtil.getProject(Arrays.copyOf(value.getBytes(), value.getLength()));
            BenchmarkCountVisitor visitor = new BenchmarkCountVisitor();
            for (CodeRepository repository : project.getRepositories()) {
                for (Revision revision : repository.getRevisions()) {
                    try {
                        List<ASTRoot> benchmarkFiles = analysisUtil.allChangedFiles(revision, repository.getUrl());
                        for (ASTRoot changedFile : benchmarkFiles) {
                            changedFile.accept(visitor);
                        }
                    } catch (Exception e) {
                        System.err.println(e.toString());
                    }
                }
            }
            for (Map.Entry<String, Integer> benchmark : visitor.getChanges().entrySet()) {
                try {
                    context.write(new Text("benchmark"), new Text(project.getName() + ":" + benchmark.getKey() + ":" + benchmark.getValue()));
                } catch (Exception e) {
                    System.err.println(e.toString());
                }
            }
        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }
}
