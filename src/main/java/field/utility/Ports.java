package field.utility;

import java.io.IOException;
import java.net.*;

public class Ports {

	static public int nextAvailable(int from)
	{
		for(int i=from;i<Integer.MAX_VALUE;i++)
		{
			if (available(i)) return i;
		}
		throw new RuntimeException();
	}
	static  public boolean available(int port) {
		try {
			var s = new ServerSocket(port);
			s.close();
			return true;
		} catch (IOException e) {
			System.out.println(" -- port "+port+" cannot be opened");
			return false;
		}
	}

//	static public boolean available(int port) {
//		Socket s = null;
//		try {
//			s = new Socket("localhost", port);
//			return false;
//		} catch (IOException e) {
//			return true;
//		} finally {
//			if (s != null) {
//				try {
//					s.close();
//				} catch (IOException e) {`
//					throw new RuntimeException();
//				}
//			}
//		}
//	}
}
