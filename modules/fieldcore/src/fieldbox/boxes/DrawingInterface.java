package fieldbox.boxes;

import field.graphics.Bracketable;
import field.graphics.MeshBuilder;
import field.graphics.Scene;

import java.util.Optional;

/**
 * Created by marc on 5/11/2016.
 */
public interface DrawingInterface {
	MeshBuilder getLine(String layerName);

	MeshBuilder getMesh(String layerName);

	MeshBuilder getPoints(String layerName);

	default Optional<TextDrawing> getTextDrawing(Box from) {
		return from.first(TextDrawing.textDrawing, from.both());
	}

	Scene getShader();

	DrawingInterface addBracketable(Bracketable mesh);
}
