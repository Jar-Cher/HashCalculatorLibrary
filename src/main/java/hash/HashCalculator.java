package hash;

import java.nio.file.Path;

public interface HashCalculator {

    Long calculateHash(Path path);

}
