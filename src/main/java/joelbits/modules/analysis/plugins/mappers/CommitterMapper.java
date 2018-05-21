package joelbits.modules.analysis.plugins.mappers;

import joelbits.model.project.CodeRepository;
import joelbits.model.project.Project;
import joelbits.model.project.Revision;
import joelbits.modules.analysis.plugins.utils.AnalysisUtil;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Loops through all revisions of a repository and writes the ratio: benchmarkers/committers to find
 * out how many of the committers are working on microbenchmarks.
 */
public final class CommitterMapper extends Mapper<Text, BytesWritable, Text, Text> {
    private final AnalysisUtil analysisUtil = new AnalysisUtil();

    @Override
    public void map(Text key, BytesWritable value, Context context) throws IOException, InterruptedException {
        Project project = analysisUtil.getProject(Arrays.copyOf(value.getBytes(), value.getLength()));

        for (CodeRepository repository : project.getRepositories()) {
            List<Revision> revisions = repository.getRevisions();

            for (Revision revision : revisions) {
                String committer = revision.getCommitter().getUsername();
                context.write(new Text("committer"), new Text(committer));
                if (!revision.getFiles().isEmpty()) {
                    context.write(new Text("benchmarker"), new Text(committer));
                }
            }
        }
    }
}
