package fielded;

import field.utility.*;
import fieldbox.boxes.Box;
import org.json.JSONStringer;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Helper class for the commands system
 */
public class Commands extends Box {


	static public final Dict.Prop<Supplier<Map<Pair<String, String>, Runnable>>> commands = new Dict.Prop<>("commands").type()
															   .doc("commands injected into the editor as ctrl-space menuSpecs. For a simpler, static, interface you might try `_.command`")
															   .toCannon();

	static public final Dict.Prop<IdempotencyMap<Function<Box, Void>>> command = new Dict.Prop<>("command").type()
													 .toCannon()
													 .doc("commands for this box (and all boxes below). For example `_.command.foo = function(_) bar(_)`")
													 .autoConstructs(() -> new IdempotencyMap<>(Function.class));
	static public final Dict.Prop<IdempotencyMap<Function<Box, Boolean>>> commandGuard = new Dict.Prop<>("commandGuard").type()
															    .toCannon()
															    .doc("a predicate that allows you to turn on and off a command. `_.commandGuard.foo = function(_) false` will turn off command `foo` for this box and all progengy")
															    .autoConstructs(() -> new IdempotencyMap<>(Function.class));

	static public final Dict.Prop<IdempotencyMap<String>> commandDoc = new Dict.Prop<>("commandDoc").type()
													.toCannon()
													.doc("documentation for commands for this box (and all boxes below). For example `_.commandDoc.foo = \"foos the bar\"`")
													.autoConstructs(() -> new IdempotencyMap<>(String.class));

	public LinkedHashMap<String, Runnable> callTable = new LinkedHashMap<>();
	public RemoteEditor.ExtendedCommand callTable_alternative = null;

	public void requestCommands(Optional<Box> box, String property, String text, Consumer<String> ret, int line, int ch) {

		// now we need to ask everybody if they have any commands to offer based on the above.

		//todo: handle no box case
		List<Triple<String, String, Runnable>> commands = getCommandsAndDocs(box.get());

		Log.log("remote.trace", ()->" commands are :" + commands);

		JSONStringer stringer = new JSONStringer();
		stringer.array();
		callTable.clear();
		for (Triple<String, String, Runnable> r : commands) {
			String u = UUID.randomUUID()
				       .toString();
			callTable.put(u, r.third);
			stringer.object();
			stringer.key("name")
				.value(r.first);
			stringer.key("info")
				.value(r.second);
			stringer.key("call")
				.value(u);
			stringer.endObject();
		}


		Log.log("remote.trace",()-> " call table looks like :" + callTable);

		stringer.endArray();

		ret.accept(stringer.toString());

	}

	static public void exportAsCommand(Box inside, Runnable r, String name, String doc) {
		inside.properties.getOrConstruct(command)
				 .put(name, (FunctionOfBox) (x) -> {
					 r.run();
					 return null;
				 });
		inside.properties.getOrConstruct(commandDoc)
				 .put(name, doc);
	}

	static public void exportAsCommand(Box inside, Runnable r, FunctionOfBox<Boolean> guard, String name, String doc) {
		inside.properties.getOrConstruct(command)
				 .put(name, (FunctionOfBox) (x) -> {
					 r.run();
					 return null;
				 });
		inside.properties.getOrConstruct(commandDoc)
				 .put(name, doc);
		inside.properties.getOrConstruct(commandGuard)
				 .put(name, guard);
	}

	public static List<Triple<String, String, Runnable>> getCommandsAndDocs(Box box) {
		List<Triple<String, String, Runnable>> commands = (List<Triple<String, String, Runnable>>) box.find(Commands.commands, box.both())
													      .flatMap(m -> m.get()
															     .entrySet()
															     .stream())
													      .map(x -> new Triple<>(x.getKey().first, x.getKey().second, x.getValue()))
													      .collect(Collectors.toList());


		IdempotencyMap<Function<Box, Void>> map = box.find(command, box.upwards())
						       .reduce(new IdempotencyMap<Function<Box, Void>>(Function.class), (a1, a2) -> {
							       IdempotencyMap<Function<Box, Void>
									   > q = new IdempotencyMap<>(Function.class);
							       q.putAll(a1);
							       q.putAll(a2);
							       return q;
						       });
		IdempotencyMap<String> mapDoc = box.find(commandDoc, box.upwards())
						   .reduce(new IdempotencyMap<String>(String.class), (a1, a2) -> {
							   IdempotencyMap<String> q = new IdempotencyMap<>(String.class);
							   q.putAll(a1);
							   q.putAll(a2);
							   return q;
						   });

		IdempotencyMap<Function<Box, Boolean>> guardDoc = box.find(commandGuard, box.upwards())
								     .reduce(new IdempotencyMap<Function<Box, Boolean>>(FunctionOfBox.class), (a1, a2) -> {
									     IdempotencyMap<Function<Box, Boolean>> q = new IdempotencyMap<>(FunctionOfBox.class);
									     q.putAll(a1);
									     q.putAll(a2);
									     return q;
								     });

		map.entrySet()
		   .forEach(x -> {
			   Function<Box, Boolean> g = guardDoc.get(x.getKey());
			   if (g == null || g.apply(box)) {
				   String name = rewriteCamelCase(x.getKey());
				   String doc = mapDoc.getOrDefault(x.getKey(), "");
				   commands.add(new Triple<String, String, Runnable>(name, doc, () -> {
					   x.getValue().apply(box);
				   }));
			   }
		   }); return commands;
	}

	static private String rewriteCamelCase(String key) {
		return key.replace("_", " ");
	}

	public RemoteEditor.SupportsPrompt supportsPrompt(Consumer<String> ret) {

		return (prompt, commands1, alternative) -> {
			JSONStringer stringer = new JSONStringer();
			stringer.object();
			stringer.key("prompt");
			stringer.value(prompt);
			stringer.key("commands");
			stringer.array();
			callTable.clear();
			for (Map.Entry<Pair<String, String>, Runnable> r : commands1.entrySet()) {
				String u = UUID.randomUUID()
					       .toString();
				callTable.put(u, r.getValue());
				stringer.object();
				stringer.key("name")
					.value(r.getKey().first);
				stringer.key("info")
					.value(r.getKey().second);
				stringer.key("call")
					.value(u);
				stringer.endObject();
			}
			stringer.endArray();

			if (alternative != null) {
				stringer.key("alternative");
				String u = UUID.randomUUID()
					       .toString();
				callTable_alternative = alternative;
				stringer.value(u);
			} else {
				callTable_alternative = null;
				stringer.key("alternative");
				callTable_alternative = alternative;
				stringer.value(null);
			}
			stringer.endObject();

			ret.accept(stringer.toString());
		};

	}

}
