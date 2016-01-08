package fieldipython.zmq;

import field.utility.Triple;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IPythonInterface {

	private ZContext context;
	private Socket listen;
	private Socket speak;
	String session = UUID.randomUUID().toString();

	public IPythonInterface() {
		context = new ZContext();
	}

	public void initInput(String iopub_url) {
		listen = context.createSocket(ZMQ.SUB);
		listen.connect(iopub_url);
		listen.subscribe(new byte[0]);

		new Thread() {
			public void run() {
				while (true) {
					String a = listen.recvStr();
					List<String> data = new ArrayList<>();
					data.add(a);
					while (listen.hasReceiveMore()) {
						String next = listen.recvStr();
						data.add(next);
					}

					System.out.println(" proc :"+data);

					List<String> datad = new ArrayList<>();
					String aa = speak.recvStr(ZMQ.NOBLOCK);
					do {
						datad.add(aa);
						while (speak.hasReceiveMore()) {
							String next = speak.recvStr();
							datad.add(next);
						}
						aa = speak.recvStr(ZMQ.NOBLOCK);
					} while (aa != null);

					System.out.println(" side channel :"+datad);

					process(data);
				}
			}
		}.start();
	}

	public void send(String python) {
		Map<String, String> h = new HashMap<>();
		h.put("msg_id", UUID.randomUUID().toString());
		h.put("session", session);
		h.put("username", "field");
		h.put("msg_type", "execute_request");

		Map<String, String> c = new HashMap<>();
		c.put("code", python);
		c.put("silent", "false");

		List<String> a = new ArrayList<>();
		a.add("field");
		a.add("<IDS|MSG>");
		a.add("");
		a.add(new JSONObject(h).toString());
		a.add("{}");
		a.add("{}");
		a.add(new JSONObject(c).toString());

		for (int i = 0; i < a.size() - 1; i++)
			speak.sendMore(a.get(i));
		speak.send(a.get(a.size() - 1));
	}

	public List<Triple<Integer, Integer, String>> complete(String left) {
		
		String aa = speak.recvStr(ZMQ.NOBLOCK);
		do {
			List<String> data = new ArrayList<>();
			data.add(aa);
			while (speak.hasReceiveMore()) {
				String next = speak.recvStr();
				data.add(next);
			}
			aa = speak.recvStr(ZMQ.NOBLOCK);
		} while (aa != null);

		
		Map<String, String> h = new HashMap<>();
		h.put("msg_id", UUID.randomUUID().toString());
		h.put("session", session);
		h.put("username", "field");
		h.put("msg_type", "complete_request");
		h.put("version", "5");

		Map<String, Object> c = new HashMap<>();
		c.put("code", left);
		c.put("cursor_pos", left.length());

		List<String> a = new ArrayList<>();
		a.add("field");
		a.add("<IDS|MSG>");
		a.add("");
		a.add(new JSONObject(h).toString());
		a.add("{}");
		a.add("{}");
		a.add(new JSONObject(c).toString());

		System.out.println(" sending :" + a);

		for (int i = 0; i < a.size() - 1; i++)
			speak.sendMore(a.get(i));
		speak.send(a.get(a.size() - 1));

		aa = speak.recvStr();
		List<String> data = new ArrayList<>();
		data.add(aa);
		while (speak.hasReceiveMore()) {
			String next = speak.recvStr();
			data.add(next);
		}
		try {
			System.out.println(" ? reply :" + data);
			System.out.println(" ? reply :" + data.get(6));

			JSONObject jo = new JSONObject(data.get(6));
			if (!jo.isNull("matches")) {
				JSONArray ar = ((JSONArray) jo.get("matches"));
				List<Triple<Integer, Integer, String>>  r = new ArrayList<>();
				int cs = jo.getInt("cursor_start");
				int ce = jo.getInt("cursor_end");

				for (int i = 0; i < ar.length(); i++) {
					r.add(new Triple<Integer, Integer, String>(cs, ce, (String) (ar.get(i))));
				}
				return r;
			}
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public List<String> inpsectCallable(String left, int level) {
		String aa = speak.recvStr(ZMQ.NOBLOCK);
		do {
			List<String> data = new ArrayList<>();
			data.add(aa);
			while (speak.hasReceiveMore()) {
				String next = speak.recvStr();
				data.add(next);
			}
			aa = speak.recvStr(ZMQ.NOBLOCK);
		} while (aa != null);
		
		Map<String, String> h = new HashMap<>();
		h.put("msg_id", UUID.randomUUID().toString());
		h.put("session", session);
		h.put("username", "field");
		h.put("msg_type", "inspect_request");
		h.put("version", "5");

		Map<String, Object> c = new HashMap<>();
		c.put("code", left);
		c.put("cursor_pos", left.length());
		c.put("detail_level", level);

		List<String> a = new ArrayList<>();
		a.add("field");
		a.add("<IDS|MSG>");
		a.add("");
		a.add(new JSONObject(h).toString());
		a.add("{}");
		a.add("{}");
		a.add(new JSONObject(c).toString());

		for (int i = 0; i < a.size() - 1; i++)
			speak.sendMore(a.get(i));
		speak.send(a.get(a.size() - 1));

		aa = speak.recvStr();
		List<String> data = new ArrayList<>();
		data.add(aa);
		while (speak.hasReceiveMore()) {
			String next = speak.recvStr();
			data.add(next);
		}
		try {
			System.out.println(" ? replyc :" + data.get(6));

			JSONObject info = new JSONObject(data.get(6));

			System.out.println(" info "+info);
			JSONObject d = info.getJSONObject("data");
			System.out.println(" data "+d);
			if (d==null) return null;
			String o = d.getString("text/plain");

			o = o.replaceAll("\033\\[(\\d|;)+?m", "");


			System.out.println(" text/plain " + o);
			return Collections.singletonList(o);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public void initOutput(String shellURL) {
		speak = context.createSocket(ZMQ.DEALER);
		speak.connect(shellURL);

	}

	protected void process(List<String> data) {

		System.out.println("everything :" + data);

		boolean found = false;
		for (int i = 0; i < data.size(); i++) {
			if (data.get(i).equals("<IDS|MSG>")) {
				data = data.subList(i + 2, data.size());
				found = true;
				break;
			}
		}
		if (!found || data.size() < 4) {
			System.out.println(" unparseable ? " + data);
			return;
		}

		try {
			JSONObject o = new JSONObject(data.get(3));
			process(o);
		} catch (JSONException e) {
			e.printStackTrace();
			System.out.println(" unparseable ? " + data.get(3));
		}



	}

	private void process(JSONObject o) {
		try {
			System.out.println("DATA :" + o.toString(4));

			String text = "";
			String texte = "";

			if (o.has("name")) {
				Object n = o.get("name");
				if (n != null && n.equals("stdout"))
					text = o.get("text") + "";
				if (n != null && n.equals("stderr"))
					texte = o.get("text") + "";
			}
			if (o.has("ename")) {
				String ename = (String) o.get("ename");
				texte = ename + "\n";
				String evalue = (String) o.get("evalue");
				texte += evalue + "\n";

				JSONArray a = o.getJSONArray("traceback");
				for (int i = 0; i < a.length(); i++) {
					texte += "    " + a.get(i) + "\n";
				}
			}

			if (o.has("data"))
			{
				JSONObject d = o.getJSONObject("data");
				if (d.has("image/png"))
				{
					String q = d.getString("image/png");
					text += "<img src='data:image/png;base64,"+q.replace("\n", "").replace("\\n", "")+"'>";
				}
				else if (d.has("text/html"))
				{
					String q = d.getString("text/html");
					text += q;
				}
			}

			text = text.trim();
			if (text.length() > 0)
				synchronized (output) {
					output.add(text);
				}
			texte = texte.trim();
			if (texte.length() > 0)
				synchronized (errors) {

					texte = clean(texte);

					errors.add(texte);
				}
		} catch (JSONException e) {
			e.printStackTrace();
		}


	}

	static public String clean(String texte) {
		Pattern c = Pattern.compile("\\u001b[^m]*m");
		Matcher m = c.matcher(texte);
		texte = m.replaceAll("");
		return texte;
	}

	List<String> output = new ArrayList<String>();

	public List<String> getOutput() {

		List<String> s = new ArrayList<String>();
		synchronized (output) {
			s.addAll(output);
			output.clear();
		}
		return s;

	}

	List<String> errors = new ArrayList<String>();

	public List<String> getErrors() {

		List<String> s = new ArrayList<String>();
		synchronized (errors) {
			s.addAll(errors);
			errors.clear();
		}
		return s;

	}

}
