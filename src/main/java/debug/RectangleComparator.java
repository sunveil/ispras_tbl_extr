package debug;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Comparator;

class RectangleComparator implements Comparator<Rectangle2D> {

// override the compare() method
public int compare(Rectangle2D s1, Rectangle2D s2) {
    if (s1.getWidth() * s1.getHeight() < s2.getHeight() * s2.getWidth())
        return 0;
    else
        return -1;
    }
}

