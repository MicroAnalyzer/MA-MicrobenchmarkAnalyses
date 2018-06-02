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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CommitterMapper extends Mapper<Text, BytesWritable, Text, Text> {
    private final AnalysisUtil analysisUtil = new AnalysisUtil();

    @Override
    public void map(Text key, BytesWritable value, Context context) throws IOException, InterruptedException {
        try {
            Project project = analysisUtil.getProject(Arrays.copyOf(value.getBytes(), value.getLength()));
            String projectName = project.getName();

            for (CodeRepository repository : project.getRepositories()) {
                List<Revision> revisions = repository.getRevisions();
                for (Revision revision : revisions) {
                    try {
                        String committer = revision.getCommitter().getUsername();
                        context.write(new Text("committer"), new Text(projectName + ":" + committer));
                        if (!revision.getFiles().isEmpty()) {
                            context.write(new Text("benchmarker"), new Text(projectName + ":" + committer));
                        }
                    } catch (Exception e) {
                        System.err.println(e.toString());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }
}
