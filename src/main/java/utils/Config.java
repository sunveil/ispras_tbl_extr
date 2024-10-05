package utils;

public class Config {

    public static boolean removeFrame = false;
    public static String tmpDir;
    public static String pathToGOSTJson;
    public static int left = 0;
    public static int top = 0;
    public static int width = 0;
    public static int height = 0;

    public Config(){
    }

    public void setRemoveFrame(boolean removeFrame) {
        this.removeFrame = removeFrame;
    }

    public void setLeft(int left){
        if (left > 0) {
            Config.left = left;
        }
    }

    public void setTop(int top){
        if (top > 0) {
            Config.top = top;
        }
    }

    public void setWidth(int width){
        if (width > 0) {
            Config.width = width;
        }
    }

    public void setHeight(int height){
        if (height > 0) {
            Config.height = height;
        }
    }


}
