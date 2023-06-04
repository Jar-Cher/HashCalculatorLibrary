package polytech.content.analyzer.hash;

public class ImageHashedType {
    private String filename;
    private int width;
    private int height;
    private float aspect;
    private String format;
    private long phash = 0; // 64-bits
    private String phashstring = "";

    public ImageHashedType() {
    }

    public ImageHashedType(String f, int w, int h, String fm) {
        filename = f;
        width = w;
        height = h;
        format = fm;
    }

    /**
     * calculate distance (number of mismatching bits) between s1 and s2 using String values
     *
     * @param s1
     * @param s2
     * @return
     */
    static public int distance(String s1, String s2) {
        int counter = 0;
        for (int k = 0; k < s1.length(); k++) {
            if (s1.charAt(k) != s2.charAt(k)) {
                counter++;
            }
        }
        return counter;
    }

    /**
     * calculate distance (number of mismatching bits) from this hash to s using String values
     *
     * @param s
     * @return
     */
    public int distance(String s) {
        return distance(getPhashstring(), s);
    }

    /**
     * calculate distance (number of mismatching bits) from this hash to s using long values
     *
     * @param l
     * @return
     */
    public int distance(long l) {
        return distance(getPhash(), l);
    }

    /**
     * calculate distance (number of mismatching bits) between l1 and l2 using long values
     *
     * @param l1
     * @param l2
     * @return
     */

    static public int distance(long l1, long l2) {
        return Long.bitCount(l1 ^ l2);
    }

    public float getAspect() {
        return aspect;
    }

    public void setAspect(float aspect) {
        this.aspect = aspect;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public long getPhash() {
        return phash;
    }

    public void setPhash(long phash) {
        this.phash = phash;
        setPhashstring(Long.toBinaryString(phash));
    }

    /**
     * @return the phashstring
     */
    public String getPhashstring() {
        return phashstring;
    }

    /**
     * @param phashstring the phashstring to set
     */
    public void setPhashstring(String phashstring) {
        this.phashstring = phashstring;
    }
}
