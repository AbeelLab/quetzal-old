package nl.defsoftware.mrgb.view.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import nl.defsoftware.mrgb.view.models.NodeLabel;

public class RootController extends VBox {
	private static final Logger log = LoggerFactory.getLogger(RootController.class);

	@FXML
	private GridPane graphPane;

	@FXML
	private ScrollPane scrollPane;

	@FXML
	Circle circle;

	@FXML
	private TextField textField;

	@FXML
	private ToolBarController toolbar;

	// private GraphHashMap graphMap;
	private HashMap<Short, short[]> graphMap;
	private Color nodeColor = new Color(0, 0, 0, 0.4);

	public RootController() {
		log.info("init Root Controller");
		toolbar = new ToolBarController();
		toolbar.setRootController(this);

		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("mrgb.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(this);
		try {
			fxmlLoader.load();
		} catch (IOException exception) {
			log.error(exception.getMessage());
			throw new RuntimeException(exception);
		}

		scrollPane.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
		scrollPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

		graphPane.setHgap(5);
		graphPane.setVgap(5);
	}

	public void setGraphMap(HashMap<Short, short[]> graphMap) {
		this.graphMap = graphMap;

		graphPane.add(createNode(Short.parseShort("1"), 1, 5), 10, 5);
		graphPane.add(new Label("1"), 2, 5);

		for (Short key : graphMap.keySet()) {
			List<Node> nodes = createFxNodes(graphMap.get(key));
			for (Node node : nodes) {
				Circle circle = (Circle) node;
				int x = Double.valueOf(circle.getCenterX()).intValue();
				int y = Double.valueOf(circle.getCenterY()).intValue();
				graphPane.add(circle, x, y);
				graphPane.add(new Label(circle.getId()), x, y + 1, 3, 1);
			}
		}
	}

	public void setGraphMapForNodeLabels(HashMap<Short, short[]> graphMap) {
		this.graphMap = graphMap;

		graphPane.add(createNode(Short.parseShort("1"), 1, 100), 10, 5);

		for (Short key : graphMap.keySet()) {
			for (NodeLabel nodeLabel : createNodeLabels(graphMap.get(key))) {
				graphPane.add(nodeLabel,
				    Double.valueOf(nodeLabel.getCenterX()).intValue(),
				    Double.valueOf(nodeLabel.getCenterY()).intValue());
			}
		}
	}

	private List<Node> createEdges(short from, short[] to) {
		return null;
	}

	private List<Node> createFxNodes(short[] ids) {
		List<Node> nodes = new ArrayList<Node>(ids.length);
		int heightPos = 1;
		int heightOffset = 0;
		for (int i = 0; i < ids.length; i++) {
			if (i > 0) {
				heightOffset += 5;
			}
			int widthOffset = ids[0];
			nodes.add(createNode(ids[i], widthOffset, heightPos + heightOffset));
		}
		return nodes;
	}

	private List<NodeLabel> createNodeLabels(short[] ids) {
		List<NodeLabel> nodes = new ArrayList<>(ids.length);
		int heightPos = 1;
		int heightOffset = 0;
		for (int i = 0; i < ids.length; i++) {
			if (i > 0) {
				heightOffset += 5;
			}
			int widthOffset = ids[0];
			nodes.add(createAndJoinLabelForNode(ids[i], widthOffset, heightOffset));
		}
		return nodes;
	}

	private NodeLabel createAndJoinLabelForNode(short id, int xPosition,
	    int yPosition) {
		return new NodeLabel(String.valueOf(id), xPosition * 10, yPosition, 10.0,
		    nodeColor);
	}

	private Circle createNode(short id, int xPosition, int yPosition) {
		Circle c = new Circle(xPosition * 10, yPosition, 10.0, nodeColor);
		c.setId(Short.toString(id));
		return c;
	}

}