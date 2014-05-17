package fieldbox.boxes.plugins;

import com.badlogic.jglfw.Glfw;
import fieldbox.boxes.Box;
import fieldbox.boxes.Drawing;
import fieldbox.boxes.Keyboard;
import fieldbox.boxes.Manipulation;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Press command/meta delete to delete selected boxes
 */
public class Delete extends Box {

	public Delete(Box root)
	{
		this.properties.putToList(Keyboard.onKeyDown, (event, key) ->
		{
			if(event.after.isSuperDown() && key==Glfw.GLFW_KEY_DELETE)
			{
				List<Box> all = root.breadthFirst(root.downwards()).filter(x -> x.properties.isTrue(Manipulation.isSelected, false)).collect(Collectors.toList());
				for(Box bb : all)
					bb.disconnectFromAll();
				Drawing.dirty(Delete.this);
			}
			return null;
		});
	}

}
