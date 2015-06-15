package fielded;

import field.app.ThreadSync;
import field.utility.Dict;
import field.utility.Log;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Commands to help you manipulate text and call commands that would otherwise originate inside the editor
 */
public class EditorUtils {

	static private final String prefix = "editor.utils.";
	Map<String, Consumer<JSONObject>> messageTable = new LinkedHashMap<String, Consumer<JSONObject>>() {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, Consumer<JSONObject>> eldest) {
			return this.size() > 5;
		}
	};
	int suffix = 0;
	private RemoteEditor editor;

	public EditorUtils(RemoteEditor editor) {
		this.editor = editor;
		editor.getServer()
		      .addHandlerLast((s, ws, a, p) -> {

			      if (!a.startsWith(prefix)) return p;

			      System.out.println(" message table is :" + messageTable);
			      Consumer<JSONObject> c = messageTable.remove(a);
			      if (c == null) Log.log("editorUtils.error", "no handler for message :" + a);
			      else {
				      c.accept((JSONObject) p);
			      }
			      return p;
		      });
	}

	public JSONObject sendAndRespond(String message) {

		suffix++;
		message = message.replace("####", prefix + suffix);

		CompletableFuture<JSONObject> c = new CompletableFuture<>();

		messageTable.put(prefix + suffix, s -> c.complete(s));
		System.out.println(" message table is :" + messageTable);
		editor.sendJavaScriptNow(message);

		if (ThreadSync.get().mainThread==Thread.currentThread())
		{
			try {
				return c.get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}

		try {
			while (!c.isDone()) {
				ThreadSync.yield(1);
			}
			return c.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return null;
	}


	public int getCursorPosition() {
		JSONObject o = sendAndRespond("_messageBus.publish(\"toField.####\",cm.getCursor())");

		String t = getCurrentText();
		int ch = o.getInt("ch");
		int ln = o.getInt("line");
		int q = 0;
		int ql = 0;
		for (int i = 0; i < t.length(); i++) {
			char c = t.charAt(i);

			if (ql == ln) return q + ch;

			if (c == '\n') {
				ql += 1;
				q += 1;
			} else {
				q += 1;
			}
		}
		if (ql == ln) return q + ch;
		return -1;
	}

	public void setCursorPosition(int n) {
		editor.sendJavaScriptNow("cm.setCursor(cm.posFromIndex(" + n + "))");
		editor.sendJavaScriptNow("setTimeout(function () {" +
						     "                cm.focus()" +
						     "            }, 25);" +
						     "            cm.focus();");
	}

	public String getCurrentText() {
		if (editor.currentSelection == null) return null;
		return editor.currentSelection.properties.get(editor.currentlyEditing);
	}

	public void setCurrentText(String to) {
		try {
			Dict.Prop<String> p = getCurrentProperty();

			//TODO: we can do this in remote editor better.

			editor.setCurrentlyEditingProperty(null);
			editor.currentSelection.properties.put(p, to);
			for (int i = 0; i < 1; i++)
				ThreadSync.yield(1);
			editor.setCurrentlyEditingProperty(p);
			for (int i = 0; i < 1; i++)
				ThreadSync.yield(1);
			for (int i = 0; i < 1; i++)
				ThreadSync.yield(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void replaceRange(int start, int end, String to)
	{
		editor.sendJavaScriptNow("cm.getDoc().replaceRange(\""+to+"\", cm.posFromIndex("+start+"), cm.posFromIndex("+end+"))");
	}

	public Dict.Prop<String> getCurrentProperty() {
		return editor.currentlyEditing;
	}

	public void executeCurrentBracket() {
		editor.sendJavaScriptNow("Current_Bracket()");
	}


}
