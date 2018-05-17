package joelbits.modules.analysis.plugins.mappers;

import joelbits.model.project.CodeRepository;
import joelbits.model.project.Project;
import joelbits.model.project.Revision;
import joelbits.modules.analysis.plugins.utils.AnalysisUtil;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;
import java.util.*;

/**
 * Loops through all revisions of a repository and collects individual words from each commit message in
 * order to investigate which words are most frequent.
 */
public final class LogWordsMapper extends Mapper<Text, BytesWritable, Text, Text> {
    private final AnalysisUtil analysisUtil = new AnalysisUtil();
    private final String[] categories = {"add", "update", "fix", "move", "change", "new", "improve",
            "create", "correct", "migrate", "simpl", "delete", "clean", "tweak", "refactor"};

    @Override
    public void map(Text key, BytesWritable value, Context context) throws IOException, InterruptedException {
        Project project = analysisUtil.getProject(Arrays.copyOf(value.getBytes(), value.getLength()));

        for (CodeRepository repository : project.getRepositories()) {
            List<Revision> revisions = repository.getRevisions();
            for (Revision revision : revisions) {
                String logMessage = revision.getLog().toLowerCase();
                if (revision.getFiles().isEmpty() || !logMessage.contains("benchmark")) {
                    continue;
                }
                List<String> words = Arrays.asList(categories);
                for (String word : words) {
                    if (logMessage.contains(word)) {
                        context.write(new Text(word), new Text());
                    }
                }
                context.write(new Text("benchmarkMessages"), new Text());
            }
        }
    }
}
