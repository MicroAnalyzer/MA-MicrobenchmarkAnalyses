package joelbits.modules.analysis.plugins.utils;

import com.google.protobuf.InvalidProtocolBufferException;
import joelbits.modules.analysis.converters.ASTConverter;
import joelbits.modules.analysis.converters.ProjectConverter;
import joelbits.model.ast.ASTRoot;
import joelbits.model.project.ChangedFile;
import joelbits.model.project.CodeRepository;
import joelbits.model.project.Project;
import joelbits.model.project.Revision;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.util.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public final class AnalysisUtil {
    private static final Logger log = LoggerFactory.getLogger(AnalysisUtil.class);

    public ASTRoot getAST(byte[] benchmarkFile) throws InvalidProtocolBufferException {
        return new ASTConverter().convert(benchmarkFile);
    }

    public Project getProject(BytesWritable value) throws InvalidProtocolBufferException {
        byte[] project = Arrays.copyOf(value.getBytes(), value.getLength());
        return new ProjectConverter().convert(project);
    }

    /**
     * Retrieve all changed files in a revision that contains benchmarks.
     *
     * @param revision          the revision of interest
     * @param repositoryUrl     repository url of the project containing the revision
     * @return                  list of changed benchmark files in the revision
     */
    public List<ASTRoot> allChangedBenchmarkFiles(Revision revision, String repositoryUrl) {
        List<ASTRoot> changedBenchmarkFiles = new ArrayList<>();
        Set<String> mapFileKeys = new HashSet<>();
        ASTConverter astConverter = new ASTConverter();

        for (ChangedFile file : revision.getFiles()) {
            mapFileKeys.add(repositoryUrl + ":" + revision.getId() + ":" + file.getName());
        }

        Set<byte[]> benchmarkFiles = readMapFile(mapFileKeys);
        for (byte[] file : benchmarkFiles) {
            try {
                changedBenchmarkFiles.add(astConverter.convert(file));
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
    public Set<ASTRoot> latestFileSnapshots(CodeRepository repository) {
        Set<ASTRoot> latestVersionsChangedFiles = new HashSet<>();
        Set<String> uniqueBenchmarkFiles = new HashSet<>();
        Set<String> mapFileKeys = new HashSet<>();
        ASTConverter astConverter = new ASTConverter();

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
                latestVersionsChangedFiles.add(astConverter.convert(file));
            } catch (Exception e) {
                log.error(e.toString(), e);
            }
        }

        return latestVersionsChangedFiles;
    }

    private Set<byte[]> readMapFile(Set<String> mapFileKeys) {
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
