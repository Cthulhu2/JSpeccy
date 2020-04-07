package snapshots;

import java.io.File;


/**
 * @author jsanchez
 */
public interface SnapshotFile {

    SpectrumState load(File filename)
            throws SnapshotException;

    boolean save(File filename, SpectrumState state)
            throws SnapshotException;
}
