package fieldbox.io.io2.commands;

import field.utility.Options;
import fieldbox.boxes.Box;
import fieldbox.io.IO2;
import fieldbox.io.Raft;
import fieldbox.io.io2.Queries;

import java.io.IOException;
import java.io.Serializable;
import java.util.Set;

/**
 * Created by marc on 7/13/16.
 */
public class export implements Runnable {
	IO2 io = new IO2();
	Queries q = new Queries(io);

	@Override
	public void run() {
		if (Options.remainingArgs.length != 2) {
			System.err.println("usage: export name-of-document destination");
		}

		String from = Options.remainingArgs[0];
		String to = Options.remainingArgs[1];

		Raft raft = new Raft();

		Box root = new Box();
		try {
			System.out.println(" Loading document ... ");
			Set<Box> topos = io.loadTopology(from, root, x -> null, x -> true);
			System.out.println(" Building export ...");
			Serializable g = raft.saveTopology(root, x -> true, x -> null);
			System.out.println(" Saving file ...");
			raft.write(g, to);
			System.out.println(" Document exported to "+to);

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (InstantiationException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			System.exit(1);
		}

	}
}
