package utils;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

public class Config {

    public static boolean removeFrame = false;
    public static String tmpDir;
    public static String pathToGOSTJson;
    public static int left = 0;
    public static int top = 0;
    public static int width = 0;
    public static int height = 0;
    public static Map<Integer, Rectangle2D> bboxes = new HashMap<>();

    public Config(){
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
