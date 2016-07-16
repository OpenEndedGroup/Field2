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
public class Import implements Runnable {
	IO2 io = new IO2();
	Queries q = new Queries(io);

	@Override
	public void run() {
		if (Options.remainingArgs.length != 2) {
			System.err.println("usage: Import name-of-document destination");
		}

		String from = Options.remainingArgs[0];
		String to = Options.remainingArgs[1];

		Raft raft = new Raft();

		Box root = new Box();
		try {

			System.out.println(" reading file ...");
			Serializable g = Raft.read(from);
			System.out.println(" loading document ...");
			raft.loadTopology((Raft.Group) g, root, x -> null);
			System.out.println(" saving document");
			Set<Box> topos = io.saveTopology(to, root, x -> true, x -> null);
			System.out.println(" complete, saved "+topos.size()+" boxes to "+to);

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

	}
}
