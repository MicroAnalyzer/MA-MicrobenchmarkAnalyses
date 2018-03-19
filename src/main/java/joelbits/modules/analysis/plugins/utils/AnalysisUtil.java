package joelbits.modules.analysis.plugins.utils;

import com.google.protobuf.InvalidProtocolBufferException;
import joelbits.modules.analysis.converters.ASTConverter;
import joelbits.modules.analysis.converters.ProjectConverter;
import joelbits.model.ast.ASTRoot;
import joelbits.model.project.ChangedFile;
import joelbits.model.project.CodeRepository;
import joelbits.model.project.Project;
import joelbits.model.project.Revision;
import joelbits.modules.analysis.plugins.utils.PathUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class AnalysisUtil {
    private static final Logger log = LoggerFactory.getLogger(AnalysisUtil.class);

    public static ASTRoot getAST(byte[] benchmarkFile) throws InvalidProtocolBufferException {
        return ASTConverter.convert(benchmarkFile);
    }

    public static Project getProject(BytesWritable value) throws InvalidProtocolBufferException {
        byte[] project = Arrays.copyOf(value.getBytes(), value.getLength());
        return ProjectConverter.convert(project);
    }

    /**
     * Retrieves the difference of each file that has been changed between two revisions in a repository.
     * A map is returned containing the difference between the revisions of each file. The key is the file path
     * since it is unique for each file in a repository, and the value is the ASTRoot representing the changes
     * of the file.
     *
     * @param leastRecent       the revision of interest oldest in time
     * @param mostRecent        the revision of interest most recent in time
     * @param repositoryUrl     the url of the repository containing the revisions
     * @return                  a list containing the difference of each file between the revisions
     */
    public static Map<String, ASTRoot> differenceRevisions(Revision leastRecent, Revision mostRecent, String repositoryUrl) {


        return new HashMap<>();
    }

    /**
     * Retrieve all changed files in a revision that contains benchmarks.
     *
     * @param revision          the revision of interest
     * @param repositoryUrl     repository url of the project containing the revision
     * @return                  list of changed benchmark files in the revision
     */
    public static List<ASTRoot> allChangedBenchmarkFiles(Revision revision, String repositoryUrl) {
        List<ASTRoot> changedBenchmarkFiles = new ArrayList<>();
        Set<String> mapFileKeys = new HashSet<>();

        for (ChangedFile file : revision.getFiles()) {
            mapFileKeys.add(repositoryUrl + ":" + revision.getId() + ":" + file.getName());
        }

        Set<byte[]> benchmarkFiles = readMapFile(mapFileKeys);
        for (byte[] file : benchmarkFiles) {
            try {
                changedBenchmarkFiles.add(ASTConverter.convert(file));
            } catch (Exception e) {
                log.error(e.toString(), e);
            }
        }

        return changedBenchmarkFiles;
    }

    /**
     * Return latest version of all unique benchmark files (in their ASTRoot representation) from repository.
     *
     * @param repository        the repository to retrieve latest benchmark file snapshots from
     * @return                  list of latest version of all benchmark files in repository
     */
    public static Set<ASTRoot> latestFileSnapshots(CodeRepository repository) {
        Set<ASTRoot> latestVersionsChangedFiles = new HashSet<>();
        Set<String> uniqueBenchmarkFiles = new HashSet<>();
        Set<String> mapFileKeys = new HashSet<>();

        for (Revision revision : repository.getRevisions()) {
            for (ChangedFile file : revision.getFiles()) {
                if (!uniqueBenchmarkFiles.contains(file.getName())) {
                    uniqueBenchmarkFiles.add(file.getName());
                    mapFileKeys.add(repository.getUrl() + ":" + revision.getId() + ":" + file.getName());
                }
            }
        }

        Set<byte[]> benchmarkFiles = readMapFile(mapFileKeys);
        for (byte[] file : benchmarkFiles) {
            try {
                latestVersionsChangedFiles.add(ASTConverter.convert(file));
            } catch (Exception e) {
                log.error(e.toString(), e);
            }
        }

        return latestVersionsChangedFiles;
    }

    private static Set<byte[]> readMapFile(Set<String> mapFileKeys) {
        Set<byte[]> benchmarkFiles = new HashSet<>();

        Configuration conf = new Configuration();
        Path path = new Path(PathUtil.benchmarksMapFile());
        try (MapFile.Reader mapReader = new MapFile.Reader(path, conf)) {
            for (String fileKey : mapFileKeys) {
                BytesWritable value = (BytesWritable) ReflectionUtils.newInstance(mapReader.getValueClass(), conf);
                mapReader.get(new Text(fileKey), value);
                benchmarkFiles.add(Arrays.copyOf(value.getBytes(), value.getLength()));
            }
        } catch (Exception e) {
            log.error(e.toString(), e);
        }

        return benchmarkFiles;
    }
}
