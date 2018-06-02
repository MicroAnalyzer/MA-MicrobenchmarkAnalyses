package joelbits.modules.analysis.plugins.reducers;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.toList;

public final class CommitterReducer extends Reducer<Text, Text, Text, Text> {
    private final Set<String> committers = new HashSet<>();
    private final Set<String> benchmarkers = new HashSet<>();
    private final Map<String, Map<String, Integer>> allCommitDiagram = new HashMap<>();
    private final Map<String, Map<String, Integer>> benchCommitDiagram = new HashMap<>();

    @Override
    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        for (Text value : values) {
            try {
                String projectName = StringUtils.EMPTY;
                String committer = StringUtils.EMPTY;
                if (value.toString().contains(":")) {
                    String[] parts = value.toString().split(":");
                    projectName = parts[0];
                    committer = parts[1];
                } else {
                    continue;
                }

                if (key.toString().equals("committer") && !committers.contains(committer)) {
                    committers.add(committer);
                }
                if (key.toString().equals("benchmarker") && !benchmarkers.contains(committer)) {
                    benchmarkers.add(committer);
                }
                if (key.toString().equals("committer")) {
                    if (!allCommitDiagram.containsKey(projectName)) {
                        allCommitDiagram.put(projectName, new HashMap<>());
                    }
                    incrementCommit(allCommitDiagram.get(projectName), committer);
                }
                if (key.toString().equals("benchmarker")) {
                    if (!benchCommitDiagram.containsKey(projectName)) {
                        benchCommitDiagram.put(projectName, new HashMap<>());
                    }
                    incrementCommit(benchCommitDiagram.get(projectName), committer);
                }
            } catch (Exception e) {
                System.err.println(e.toString());
            }
        }
    }

    private synchronized void incrementCommit(Map<String, Integer> commits, String committer) {
        if (!commits.containsKey(committer)) {
            commits.put(committer, 1);
        } else {
            int occurrences = commits.get(committer) + 1;
            commits.put(committer, occurrences);
        }
    }

    @Override
    public void cleanup(Context context) {
        try {
            context.write(new Text("committers: "), new Text(committers.size() + System.lineSeparator()));
            context.write(new Text("benchmarkers: "), new Text(benchmarkers.size() + System.lineSeparator()));

            for (Map.Entry<String, Map<String, Integer>> project : allCommitDiagram.entrySet()) {
                try {
                    List<Map.Entry<String, Integer>> sortedDeveloperCommits = project.getValue().entrySet()
                            .stream().sorted(Map.Entry.<String, Integer>comparingByValue()
                                    .reversed()).collect(toList());

                    Map<String, Integer> benchmarkers = benchCommitDiagram.get(project.getKey());
                    context.write(new Text(System.lineSeparator() + project.getKey() + " has " + benchmarkers.size() + " benchmarkers"),
                            new Text(" out of " + sortedDeveloperCommits.size() + " committers" + System.lineSeparator()));
                    context.write(new Text("Top 5 committers of "), new Text(" " + project.getKey() + " are " + System.lineSeparator()));
                    for (int i = 0; i < 5; i++) {
                        if (i >= sortedDeveloperCommits.size()) {
                            break;
                        }
                        String topCommitter = sortedDeveloperCommits.get(i).getKey();
                        String commitRatio;
                        if (benchmarkers.containsKey(topCommitter)) {
                            commitRatio = benchmarkers.get(topCommitter) + "/" + sortedDeveloperCommits.get(i).getValue();
                        } else {
                            commitRatio = "0/" + sortedDeveloperCommits.get(i).getValue();
                        }
                        context.write(new Text(project.getKey()), new Text(sortedDeveloperCommits.get(i).getKey() + ": " + commitRatio + System.lineSeparator()));
                    }
                    context.write(new Text("Commits per developer affecting microbenchmark files in " + project.getKey()), new Text(" are " + StringUtils.join(benchmarkers.values(), ",") + System.lineSeparator()));
                } catch (Exception e) {
                    System.err.println(e.toString());
                }
            }
        } catch (Exception e) {
            System.err.println(e.toString());
        }
    }
}
