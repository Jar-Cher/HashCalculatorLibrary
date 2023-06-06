
import hash.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.io.File;

public class PHashTest {

    @Test
    public void TestBasicHashCalculation() {
        hash.PHashCalculator PHashCalculator = new hash.PHashCalculator();
        Long pHash = PHashCalculator.calculateHash(new File("src/main/resources/HeadOverHeels.gif").toPath());
        Assertions.assertEquals("-3414753420769533133", String.valueOf(pHash));
    }

    @Test
    public void TestHashCalculationFailure() {
        hash.PHashCalculator PHashCalculator = new hash.PHashCalculator();
        Long pHash = PHashCalculator.calculateHash(new File("src/main/resources/ololo.gif").toPath());
        Assertions.assertNull(pHash);
    }

    @Test
    public void TestHashComparisonOfCopies() {
        hash.PHashCalculator PHashCalculator = new hash.PHashCalculator();
        Long pHash1 = PHashCalculator.calculateHash(new File("src/main/resources/HeadOverHeels.gif").toPath());
        Long pHash2 = PHashCalculator.calculateHash(new File("src/main/resources/HeadOverHeelsCopy.gif").toPath());
        Assertions.assertEquals(pHash1, pHash2);
    }

    @Test
    public void TestHashComparisonOfGifPng() {
        hash.PHashCalculator PHashCalculator = new hash.PHashCalculator();
        Long pHash1 = PHashCalculator.calculateHash(new File("src/main/resources/HeadOverHeels.gif").toPath());
        Long pHash2 = PHashCalculator.calculateHash(new File("src/main/resources/HeadOverHeelsPng.png").toPath());
        Assertions.assertEquals(pHash1, pHash2);
    }

    @Test
    public void TestHashComparisonOfSimilarPics() {
        hash.PHashCalculator PHashCalculator = new hash.PHashCalculator();
        Long pHash1 = PHashCalculator.calculateHash(new File("src/main/resources/HeadOverHeels.gif").toPath());
        Long pHash2 = PHashCalculator.calculateHash(new File("src/main/resources/HeadOverHeelsMutilated.gif").toPath());
        Assertions.assertTrue(ImageHashedType.distance(pHash1, pHash2) < 5);
    }

    @Test
    public void TestHashComparisonOfUnsimilarPics() {
        hash.PHashCalculator PHashCalculator = new hash.PHashCalculator();
        Long pHash1 = PHashCalculator.calculateHash(new File("src/main/resources/HeadOverHeels.gif").toPath());
        Long pHash2 = PHashCalculator.calculateHash(new File("src/main/resources/HeadOverHeelsCoverArt.jpg").toPath());
        Assertions.assertTrue(ImageHashedType.distance(pHash1, pHash2) > 5);
    }

    @Test
    public void TestHashComparisonOfDifferentPics() {
        hash.PHashCalculator PHashCalculator = new hash.PHashCalculator();
        Long pHash1 = PHashCalculator.calculateHash(new File("src/main/resources/HeadOverHeels.gif").toPath());
        Long pHash2 = PHashCalculator.calculateHash(new File("src/main/resources/BadPussies.jpg").toPath());
        Assertions.assertTrue(ImageHashedType.distance(pHash1, pHash2) > 5);
    }

    @Test
    public void TestHashComparisonOfAnimatedGifAndJpg() {
        hash.PHashCalculator PHashCalculator = new hash.PHashCalculator();
        Long pHash1 = PHashCalculator.calculateHash(new File("src/main/resources/NyanCat.gif").toPath());
        Long pHash2 = PHashCalculator.calculateHash(new File("src/main/resources/NyanCat.jpg").toPath());
        Assertions.assertTrue(ImageHashedType.distance(pHash1, pHash2) < 5);
    }
}
