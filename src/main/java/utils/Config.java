package utils;

public class Config {

    public static boolean removeFrame = false;
    public static String tmpDir;
    public Config(boolean removeFrame){
        this.removeFrame = removeFrame;
    }

    public void setRemoveFrame(boolean removeFrame) {
        this.removeFrame = removeFrame;
    }
}
