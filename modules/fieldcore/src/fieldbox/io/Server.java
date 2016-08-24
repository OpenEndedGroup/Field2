package fieldbox.io;

import com.orientechnologies.orient.client.remote.OServerAdmin;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;
import fieldagent.Main;

import java.io.File;

/**
 * Created by marc on 3/10/16.
 */
public class Server implements Runnable {

	static public void main(String[] a) {
		new Server().run();
	}

	@Override
	public void run() {

		System.setProperty("ORIENTDB_ROOT_PASSWORD", "admin");

		String root = System.getProperty("appDir")+"/modules/fieldcore/resources/orientdb";

		System.out.println(" root is :"+root+" "+new File(root).exists());

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
					+ "<listener ip-address=\"0.0.0.0\" port-range=\"2480-2490\" protocol=\"http\">"+
				" <commands>\n" +
					"    <command implementation=\"com.orientechnologies.orient.server.network.protocol.http.command.get.OServerCommandGetStaticContent\" pattern=\"GET|www GET|studio/ GET| GET|*.htm GET|*.html GET|*.xml GET|*.jpeg GET|*.jpg GET|*.png GET|*.gif GET|*.js GET|*.css GET|*.swf GET|*.ico GET|*.txt GET|*.otf GET|*.pjs GET|*.svg\">\n" +
					"      <parameters>\n" +
					"        <entry value=\"Cache-Control: no-cache, no-store, max-age=0, must-revalidate\\r\\nPragma: no-cache\" name=\"http.cache:*.htm *.html\"/>\n" +
					"        <entry value=\"Cache-Control: max-age=120\" name=\"http.cache:default\"/>\n" +
					"      </parameters>\n" +
					"    </command>\n" +
					"  </commands>\n" +
					"</listener>\n"
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
			System.out.println(" databases are :" + admin.listDatabases());

			if (!admin.listDatabases().containsKey("FIELD")) {
				System.err.println(" creating database ");
				admin.createDatabase("FIELD", "graph", "plocal");
				System.err.println(" finished ");
			}

			while (true)
				Thread.sleep(1000);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}