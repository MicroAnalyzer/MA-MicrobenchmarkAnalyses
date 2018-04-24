package joelbits.modules.analysis.plugins.utils;

import java.io.File;

final class PathUtil {
    private static final String PATH = System.getProperty("user.dir") + File.separator;
    private static final String CHANGED_FILES = PATH + "files" + File.separator;

    static String benchmarksMapFile() {
        return CHANGED_FILES;
    }
}
