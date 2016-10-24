/**
 * 
 */
package nl.defsoftware.mrgb.view.controllers;

import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Shape;
import nl.defsoftware.mrgb.services.GraphHandler;
import nl.defsoftware.mrgb.services.GraphService;
import nl.defsoftware.mrgb.view.GraphScrollPane;
import nl.defsoftware.mrgb.view.actions.ActionStateEnums;
import nl.defsoftware.mrgb.view.models.IGraphViewModel;
import nl.defsoftware.mrgb.view.models.RibbonGraphModel;

/**
 * This controller ensures that the layers are set for interacting with the
 * graph. A graphHandler class will ensure the loading of the graph model and is
 * responsible for the layout algorithm.
 *
 * @author D.L. Ettema
 * @date 7 Jun 2016
 */
public class GraphController implements Initializable, MapChangeListener<ActionStateEnums, Boolean> {

    private static final Logger log = LoggerFactory.getLogger(GraphController.class);

    private GraphService graphService;

    private Group groupedNodes;

    private MouseGestures mouseGestures;

    private GraphHandler graphHandler;

    private GraphScrollPane scrollPane;

    private IGraphViewModel<Shape> model;

    private static final double SCROLL_ZOOM_FACTOR = 0.0025;

    @FXML
    private AnchorPane mainPane;

    @FXML
    private Pane nodePane;

    @FXML
    private ScrollBar scrollbar;

    @FXML
    private Canvas edgeCanvas;

    @FXML
    private Label showingRange;

    private final IntegerProperty amountOfLevels = new SimpleIntegerProperty(0);
    private final DoubleProperty zoomFactor = new SimpleDoubleProperty(1.0);
    private boolean needsUpdate = false;

    public GraphController() {
        graphService = new GraphService();
        model = new RibbonGraphModel();
        
        nodePane = new Pane();
        nodePane.setPrefHeight(500.0);
        nodePane.setPrefWidth(300.0);
        nodePane.setStyle("-fx-back-ground-color: white;");
        
        groupedNodes = new Group();
        groupedNodes.getChildren().add(nodePane);
        
        mouseGestures = new MouseGestures(this);
        scrollPane = new GraphScrollPane(groupedNodes);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollBarPolicy.ALWAYS);
        scrollPane.setVbarPolicy(ScrollBarPolicy.ALWAYS);
        scrollPane.setController(this);
        scrollPane.setChangeListener(graphHandler);

        edgeCanvas = new Canvas();
        mainPane = new AnchorPane();
        scrollbar = new ScrollBar();
//        mainPane.getChildren().add(scrollPane);
//        mainPane.getChildren().add(groupedNodes);

        GraphAnimationTimer timer = new GraphAnimationTimer();
        timer.start();
        graphHandler = new GraphHandler(graphService.getParsedSequences(), graphService.getGenomeNames());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    public void updateView() {
        needsUpdate = true;
    }
    
    public void updateGraph() {
//        double viewRange = (mainPane.getHeight() / zoomFactor.get()) + 1;
//        double viewingStart = Math.max(amountOfLevels.multiply(scrollbar.getValue()).doubleValue(), 0.0);
        double dummyRange = 500;
        double dummyViewingStartCoordinate = 1;
        clear();
        zoomFactor.bind(scrollPane.getScaleYProperty());
        graphHandler.loadAlternateGraphViewModel(
                nodePane, 
                edgeCanvas, 
                model, 
                dummyViewingStartCoordinate, 
                dummyRange, 
                zoomFactor);
        endUpdate();
        log.info("VIEW GRAPH");
        
    }

    private void clear() {
        nodePane.getChildren().clear();
        edgeCanvas.getGraphicsContext2D().clearRect(0, 0, edgeCanvas.getWidth(), edgeCanvas.getWidth());
    }
    
    @Override
    public void onChanged(MapChangeListener.Change<? extends ActionStateEnums, ? extends Boolean> change) {
        printMemoryUsage();
        if (change.getKey() instanceof ActionStateEnums) {
            if (ActionStateEnums.LOAD_DATA_AND_PARSE == change.getKey()) {
                // @TODO do this work in another thread.
                graphService.loadDataAndParse();
                log.info("LOAD DATA AND PARSE");
            } else if (ActionStateEnums.VIEW_GRAPH == change.getKey()) {
                updateGraph();
            }
        }
        printMemoryUsage();
    }

    private void printMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long bytes = (runtime.totalMemory() - runtime.freeMemory());
        double megabytes = bytes / 1024.0 / 1024.0;
        log.info("memory = " + String.format(Locale.US, "%.3f", megabytes) + " MB");
    }

    public ScrollPane getScrollPane() {
        return this.scrollPane;
    }

    public Pane getMainPane() {
        return mainPane;
    }
    
    private void endUpdate() {

        // add components to graph pane

        nodePane.getChildren().addAll(model.getAllEdges());
        nodePane.getChildren().addAll(model.getAddedSequences());
        nodePane.getChildren().addAll(model.getAllLabels());

        // remove components from graph pane
         nodePane.getChildren().removeAll(model.getRemovedSequences());
         nodePane.getChildren().removeAll(model.getRemovedEdges());

        // enable dragging of cells
//        for (Node node : model.getAddedSequences()) {
//            mouseGestures.makeDraggable(node);
//        }

        // every cell must have a parent, if it doesn't, then the graphParent is
        // the parent
        // model.attachOrphansToGraphParent(model.getAddedSequences());
        //
        // // remove reference to graphParent
        // model.disconnectFromGraphParent(model.getRemovedSequences());

        // merge added & removed cells with all cells
        model.merge();
    }

    public double getScale() {
        return this.scrollPane.getScaleValue();
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
