package fieldbox.io;

import com.orientechnologies.orient.client.remote.OServerAdmin;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;

/**
 * Created by marc on 3/10/16.
 */
public class Server implements Runnable {

	static public void main(String[] a)
	{
		new Server().run();
	}

	@Override
	public void run() {

		System.setProperty("ORIENTDB_ROOT_PASSWORD", "admin");

		String root = "/Users/marc/Downloads/orientdb-community-2.1.12";

		OServer server = null;
		try {
			server = OServerMain.create();
			server.startup(
				"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
					+ "<orient-server>"
					+ "<network>"
					+ "<protocols>"
					+ "<protocol name=\"binary\" implementation=\"com.orientechnologies.orient.server.network.protocol.binary.ONetworkProtocolBinary\"/>"
					+ "<protocol name=\"http\" implementation=\"com.orientechnologies.orient.server.network.protocol.http.ONetworkProtocolHttpDb\"/>"
					+ "</protocols>"
					+ "<listeners>"
					+ "<listener ip-address=\"0.0.0.0\" port-range=\"2424-2430\" protocol=\"binary\"/>"
					+ "<listener ip-address=\"0.0.0.0\" port-range=\"2480-2490\" protocol=\"http\"/>"
					+ "</listeners>"
					+ "</network>"
					+ "<users>"
					+ "<user name=\"admin\" password=\"admin\" resources=\"*\"/>"
					+ "</users>"
					+ "<properties>"
					+ "<entry value=\"/var/tmp/orient_field\" name=\"server.database.path\" />"
					+ "<entry name=\"orientdb.www.path\" value=\"" + root + "/www/\"/>"
					+ "<entry name=\"orientdb.config.file\" value=\"" + root + "/config/orientdb-server-config.xml\"/>"
					+ "<entry name=\"server.cache.staticResources\" value=\"false\"/>"
					+ "<entry name=\"log.console.level\" value=\"info\"/>"
					+ "<entry name=\"log.file.level\" value=\"fine\"/>"
					//The following is required to eliminate an error or warning "Error on resolving property: ORIENTDB_HOME"
					+ "<entry name=\"plugin.dynamic\" value=\"false\"/>"
					+ "</properties>" + "</orient-server>");
			OServer m = server.activate();

			OServerAdmin admin = new OServerAdmin("remote:localhost").connect("admin", "admin");
			System.out.println(" databases are :"+admin.listDatabases());

			if (!admin.listDatabases().containsKey("FIELD"))
			{
				System.err.println(" creating database ");
				admin.createDatabase("FIELD", "graph", "plocal");
				System.err.println(" finished ");
			}

			while(true)
				Thread.sleep(1000);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}