package fielded.webserver;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import field.app.RunLoop;
import field.utility.Log;
import field.utility.Util;
import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Server {

	public static final String FIELD_FILESYSTEM = "/field/";
	Map<String, String> fixedResources = new HashMap<String, String>();
	Set<String> documentRoots = new LinkedHashSet<String>();

	public final NanoHTTPD server;
	public final WebSocketServer webSocketServer;

	Deque<Handler> handlers = new LinkedList<>();
	List<URIHandler> uriHandlers = new ArrayList<>();

	BiMap<String, WebSocket> knownSockets = HashBiMap.create();
	public final int port;
	public final int websocketPort;


	public interface Handler {
		// handle gets called in the main thread
		Object handle(Server server, WebSocket from, String address, Object payload);
	}

	public interface HandlerInMainThread extends Handler {
		default boolean will(Server server, WebSocket from, String address, Object payload) {
			return true;
		}
	}

	public interface URIHandler {
		NanoHTTPD.Response serve(String uri, NanoHTTPD.Method method, Map<String, String> headers, Map<String, String> parms, Map<String, String> files);
	}

	long uniq = 0;

	static public ThreadLocal<WebSocket> currentWebSocket = new ThreadLocal();


	public int getWebsocketPort() {
		return websocketPort;
	}

	public int getPort() {
		return port;
	}

	public Server(int port, int websocketPort) throws IOException {
		this.port = port;
		this.websocketPort = websocketPort;
		server = new NanoHTTPD(port) {
			@Override
			Response serve(String uri, Method method, Map<String, String> headers, Map<String, String> parms, Map<String, String> files) {

				Log.log("server", () -> "Serving " + uri);

				Log.log("server", () -> "will check:" + uriHandlers);

				Object id = parms.get("id");

				if (id == null) {
					id = "" + (uniq++);
				}

				if (fixedResources.containsKey(uri)) {
					String resource = fixedResources.get(uri);
					resource = resource.replace("///ID///", "" + id);
					resource = resource.replace("///PORT///", "" + port);
					resource = resource.replace("///WSPORT///", "" + websocketPort);
					return new Response(Response.Status.OK, null, resource);
				}

				if (uri.startsWith(FIELD_FILESYSTEM)) {

					String e = uri.substring("/field/filesystem/".length());

					for (String s : documentRoots) {
						File ff = new File(s + "/" + e);
						if (ff.exists()) {
							//return new Response(Response.Status.OK, null, new BufferedInputStream(new FileInputStream(ff)));

							return serveFile(uri, headers, ff, "application/octet-stream");
						}
					}
					return new Response(Response.Status.NOT_FOUND, null, "couldn't find " + e);
				}

				for (URIHandler u : uriHandlers) {
					Response r = u.serve(uri, method, headers, parms, files);
					if (r != null)
						return r;
				}

				return new Response(Response.Status.BAD_REQUEST, null, " couldn't understand request");
			}
		};

		server.start();

		webSocketServer = new WebSocketServer(new InetSocketAddress(websocketPort)) {
			@Override
			public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
				Log.log("remote.trace", () -> " websocket connected " + clientHandshake);
			}

			@Override
			public void onClose(WebSocket webSocket, int i, String s, boolean b) {
				Log.log("remote.trace", () -> " websocket closed " + i + " " + s + " " + b);
				synchronized (knownSockets) {
					System.err.println(" WEBSOCKET CLOSED ");
					knownSockets.values().remove(webSocket);
				}
			}

			@Override
			public void onMessage(WebSocket webSocket, String s) {

				Log.log("remote.trace", () -> " message:<" + s + ">");
				JSONObject o = new JSONObject(s);
				String address = o.getString("address");
				Object payload = o.get("payload");
				Object originalPayload = payload;

				for (Handler h : new ArrayList<>(handlers)) {
					if (h instanceof HandlerInMainThread) {
						if (((HandlerInMainThread) h).will(Server.this, webSocket, address, payload)) {
							final Object p = payload;
//							try {
							queue(() -> {
								currentWebSocket.set(webSocket);
								return h.handle(Server.this, webSocket, address, p);
							});

							// not threading these all through the main thread means that we don't get backlogged nearly as easily

//							} catch (InterruptedException | ExecutionException e) {
//								Log.log("remote.error"," exception thrown by asynchronous websocket handler <" + h + "> while servicing <" + s + " / " + address + "
// -> " + originalPayload + " " + p, e);
//							}
						}
					} else {
						currentWebSocket.set(webSocket);
						payload = h.handle(Server.this, webSocket, address, payload);
					}
				}
			}

			@Override
			public void onError(WebSocket webSocket, Exception e) {
				Log.log("remote.error", () -> " websocket error reported :" + e);
				e.printStackTrace();
			}
		};

		webSocketServer.start();

		addHandlerLast((server, socket, address, payload) -> {
			if (address.equals("field.name.connection")) {
				knownSockets.put(payload.toString(), socket);
				return null;
			}
			return payload;
		});

		RunLoop.main.mainLoop.attach(10, (p) -> update());

	}

	public Server addHandlerLast(Handler h) {
		handlers.add(h);
		return this;
	}

	public Server addHandlerLast(Predicate<String> addressPredicate, Handler h) {
		handlers.add(new HandlerInMainThread() {
			@Override
			public Object handle(Server server, WebSocket from, String address, Object payload) {
				return h.handle(server, from, address, payload);
			}

			@Override
			public boolean will(Server server, WebSocket from, String address, Object payload) {
				return addressPredicate.test(address);
			}
		});
		return this;
	}

	public Server addHandlerLast(Predicate<String> addressPredicate, Supplier<String> socketName, Handler h) {
		handlers.add(new HandlerInMainThread() {
			@Override
			public Object handle(Server server, WebSocket from, String address, Object payload) {
				return h.handle(server, from, address, payload);
			}

			@Override
			public boolean will(Server server, WebSocket from, String address, Object payload) {
				return addressPredicate.test(address) && Util.safeEq(knownSockets.inverse().get(from), socketName.get());
			}
		});
		return this;
	}

	public Server addURIHandler(URIHandler h) {
		uriHandlers.add(h);
		return this;
	}


	public void addHandlerFirst(Handler h) {
		handlers.add(h);
	}

	public void addDocumentRoot(String root) {
		documentRoots.add(root);
	}

	public void setFixedResource(String uri, String text) {
		fixedResources.put(uri, text);
	}

	BlockingQueue<Runnable> queue = new LinkedBlockingDeque<Runnable>();

	protected void update() {
		while (!queue.isEmpty()) {
			Runnable c = queue.poll();
			try {
				c.run();
			} catch (Exception e) {
				System.err.println(" exception thrown in main thread update ");
				e.printStackTrace();
			}
		}
	}


	public <T> Future<T> queue(Callable<T> c) {
		if (RunLoop.main.isMainThread()) {
			CompletableFuture<T> f = new CompletableFuture<>();
			try {
				T t = c.call();
				f.complete(t);
			} catch (Throwable tt) {
				f.completeExceptionally(tt);
			}
			return f;
		} else {
			CompletableFuture<T> f = new CompletableFuture<>();
			this.queue.add(() -> {
				try {
					T t = c.call();
					f.complete(t);
				} catch (Throwable tt) {
					tt.printStackTrace();
					f.completeExceptionally(tt);
				}
			});
			return f;
		}
	}

	public void queue(Runnable c) {
		this.queue.add(c);
	}


	public void broadcast(String message) {
		webSocketServer.connections().forEach(x -> x.send(message));
	}

	static public class ConnectionLost extends RuntimeException {
		public ConnectionLost(String s) {
			super(s);
		}
	}

	public void send(String name, String message) {
		synchronized (knownSockets) {
			if (knownSockets.get(name) != null) queue(() -> knownSockets.get(name).send(message));
			else throw new ConnectionLost(" cannot find connection called " + name);
		}
	}

	public void send(WebSocket name, String message) {

		queue(() -> name.send(message));
	}

	public void nameSocket(String name, WebSocket socket) {
		knownSockets.put(name, socket);
	}


}
