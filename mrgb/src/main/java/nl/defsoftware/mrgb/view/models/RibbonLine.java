package nl.defsoftware.mrgb.view.models;

import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;

/**
 * 
 * @author D.L. Ettema
 * @date 27 September 2016
 *
 */
public class RibbonLine extends Line {

    private Integer id;
    
    public RibbonLine(Integer id) {
        this.id = id;
        
        setStrokeWidth(3.0);
        setFill(Paint.valueOf("BLACK"));
        setStartX(0);
        setStartY(0);
        setEndX(0);
        setEndY(23);
        setOpacity(0.5);
        setRotate(0.0);
    }
    
    
}