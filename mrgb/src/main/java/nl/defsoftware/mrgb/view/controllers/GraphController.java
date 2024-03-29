/**
 * 
 */
package nl.defsoftware.mrgb.view.controllers;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import javafx.animation.AnimationTimer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.MapChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import nl.defsoftware.mrgb.Util;
import nl.defsoftware.mrgb.graphs.GraphHandler;
import nl.defsoftware.mrgb.graphs.models.Node;
import nl.defsoftware.mrgb.services.GraphService;
import nl.defsoftware.mrgb.view.GraphScrollPane;
import nl.defsoftware.mrgb.view.GraphToFxScenePainter;
import nl.defsoftware.mrgb.view.ScrollAndZoomHandler;
import nl.defsoftware.mrgb.view.actions.ActionStateEnums;
import nl.defsoftware.mrgb.view.models.IGraphViewModel;
import nl.defsoftware.mrgb.view.models.RibbonGraphModel;

/**
 * This controller ensures that the layers are set for interacting with the graph. A graphHandler class will ensure the
 * loading of the graph model and is responsible for the layout algorithm.
 *
 * @author D.L. Ettema
 * @date 7 Jun 2016
 */
public class GraphController implements Initializable, MapChangeListener<ActionStateEnums, Boolean> {

    private static final Logger log = LoggerFactory.getLogger(GraphController.class);

    private static final double SCROLL_ZOOM_FACTOR = 0.0025;

    private GraphScrollPane scrollPane;

    private Pane nodePane;

    private Canvas edgeCanvas;

    private GraphService graphService;

    private GraphToFxScenePainter graphToFxScenePainter;

    private MouseGestures mouseGestures;

    private GraphHandler<Shape> graphHandler;

    private ScrollAndZoomHandler scrollAndZoomHandler;

    private IGraphViewModel<Shape> model;

    private final IntegerProperty amountOfLevels = new SimpleIntegerProperty(0);
    private final DoubleProperty zoomFactor = new SimpleDoubleProperty(1.0);
    private boolean needsUpdate = false;

    public GraphController() {
//        this.setBackground(new Background(new BackgroundFill(Paint.valueOf("yellow"), null, null)));
        graphService = new GraphService();
        model = new RibbonGraphModel();

        nodePane = new Pane();
        nodePane.setPickOnBounds(false);//needed to capture scrollevent on the underlying scrollpane.
        nodePane.setBackground(new Background(new BackgroundFill(Paint.valueOf("white"), null, null)));
        nodePane.setBorder(new Border(new BorderStroke(Paint.valueOf("green"), BorderStrokeStyle.SOLID, null, new BorderWidths(5.0))));
        
        edgeCanvas = new Canvas();
        edgeCanvas.setPickOnBounds(false);//needed to capture scrollevent on the underlying scrollpane.

        mouseGestures = new MouseGestures(this);

        scrollPane = new GraphScrollPane(new Group(nodePane/*, edgeCanvas*/));
        scrollPane.setBorder(new Border(new BorderStroke(Paint.valueOf("red"), BorderStrokeStyle.SOLID, null, new BorderWidths(5.0))));
        
//        nodePane.prefHeightProperty().bind(scrollPane.heightProperty());
        nodePane.prefWidthProperty().bind(scrollPane.widthProperty());
//        edgeCanvas.heightProperty().bind(scrollPane.heightProperty());
        edgeCanvas.widthProperty().bind(scrollPane.widthProperty());
        
        scrollAndZoomHandler = new ScrollAndZoomHandler(scrollPane, this);
        scrollPane.setOnScrollEventHandler(scrollAndZoomHandler);

        GraphAnimationTimer timer = new GraphAnimationTimer();
        timer.start();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Int2ObjectLinkedOpenHashMap<Node> sequencesDataMap = graphService.getParsedSequences();
        graphHandler = new GraphHandler<Shape>(sequencesDataMap, graphService.getGenomeNames(), graphService.getDetectedBubbles(sequencesDataMap));
        graphToFxScenePainter = new GraphToFxScenePainter(nodePane, edgeCanvas, scrollPane, graphHandler, graphService.getGridHandler());
    }

    public void updateGraph() {
        if (graphHandler == null) {
            initialize(null, null);
        }
        // double viewRange = (mainPane.getHeight() / zoomFactor.get()) + 1;
        // double viewingStart = Math.max(amountOfLevels.multiply(scrollbar.getValue()).doubleValue(), 0.0);
        // int dummyRange = graphService.getParsedSequences().size();
        int dummyRange = 500;
        Node targetNode = graphService.getParsedSequences().get(dummyRange - 1);
        int dummyViewingStartCoordinate = 1;
        clear();
        zoomFactor.bind(scrollAndZoomHandler.getScaleYProperty());

        List<Integer> longestPath = graphService.calculateLongestPath(graphService.getParsedSequences(), 0,
                targetNode.getNodeId());

        graphHandler.loadGraphViewModel(model, 
                dummyViewingStartCoordinate, 
                dummyRange, 
                zoomFactor, 
                longestPath);
        endUpdate();
        log.info("VIEW GRAPH");
    }

    public void notifyUpdateView() {
        needsUpdate = true;
    }
    
    private void clear() {
        nodePane.getChildren().clear();
        edgeCanvas.getGraphicsContext2D().clearRect(0, 0, edgeCanvas.getWidth(), edgeCanvas.getHeight());
    }

    @Override
    public void onChanged(MapChangeListener.Change<? extends ActionStateEnums, ? extends Boolean> change) {
        Util.printMemoryUsage();
        if (change.getKey() instanceof ActionStateEnums) {
            if (ActionStateEnums.LOAD_DATA_AND_PARSE == change.getKey()) {
                // @TODO do this work in another thread.
                graphService.loadDataAndParse();
                initialize(null, null);
                log.info("Ready to view");
            } else if (ActionStateEnums.VIEW_GRAPH == change.getKey()) {
                updateGraph();
            } else if (ActionStateEnums.DUMMY_ACTION == change.getKey()) {
                Rectangle r = new Rectangle(30, 70, 10, 10);
                Rectangle r2 = new Rectangle(30, 45, 20, 20);
                Rectangle r3 = new Rectangle(30, 90, 10, 10);
                log.info("layout: " + r.getLayoutX() + " y: " + r.getLayoutY());
                log.info("bounds: " + r.getBoundsInParent().toString());
                r.setLayoutX(100.0);
                r.setLayoutY(100.0);
                log.info("layout: " + r.getLayoutX() + " y: " + r.getLayoutY());
                log.info("bounds: " + r.getBoundsInParent().toString());
                r3.setScaleX(2);
                r3.setScaleY(2);
                nodePane.getChildren().add(r);
                nodePane.getChildren().add(r2);
                nodePane.getChildren().add(r3);
            }
        }
        Util.printMemoryUsage();
    }

    public Region getContent() {
        return this.scrollPane;
    }

    private void endUpdate() {

        // add components to graph pane
        // drawLinesOnCanvas(model.getAddedEdges());
        nodePane.getChildren().addAll(model.getAddedEdges());
        nodePane.getChildren().addAll(model.getAddedSequences());
        nodePane.getChildren().addAll(model.getAllLabels());

        // remove components from graph pane
        nodePane.getChildren().removeAll(model.getRemovedSequences());
        nodePane.getChildren().removeAll(model.getRemovedEdges());

        // enable dragging of cells
        // for (Node node : model.getAddedSequences()) {
        // mouseGestures.makeDraggable(node);
        // }

        // every cell must have a parent, if it doesn't, then the graphParent is
        // the parent
        // model.attachOrphansToGraphParent(model.getAddedSequences());
        //
        // // remove reference to graphParent
        // model.disconnectFromGraphParent(model.getRemovedSequences());

        // merge added & removed cells with all cells
        model.merge();
    }

    private void drawLinesOnCanvas(Set<Shape> addedEdges) {
        edgeCanvas.getGraphicsContext2D().clearRect(0, 0, edgeCanvas.getWidth(), edgeCanvas.getHeight());
        for (Shape shape : addedEdges) {
            edgeCanvas.getGraphicsContext2D().strokeLine(shape.getLayoutBounds().getMinX(),
                    shape.getLayoutBounds().getMinY(), shape.getLayoutBounds().getMaxX(),
                    shape.getLayoutBounds().getMaxY());
        }
    }

    public double getScale() {
        return this.scrollAndZoomHandler.getScaleValue();
    }

    private class GraphAnimationTimer extends AnimationTimer {

        @Override
        public void handle(long now) {
            if (needsUpdate) {
                log.info("Updating graph");
                needsUpdate = false;
                updateGraph();
            }
        }
    }
}
