package fieldbox.io;

import field.utility.Dict;
import field.utility.Log;
import fieldbox.boxes.Box;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by marc on 08/03/16.
 */
public class Raft {

    static public class Group implements Serializable {
        public List<Node> nodes;
    }

    static public class Node implements Serializable {
        public List<Object> children = new ArrayList<>();
        public List<Object> parents = new ArrayList<>();
        public Map<String, Object> values = new LinkedHashMap<>();
    }

    Map<Box, Node> insideSave = new LinkedHashMap<>();
    Map<Node, Box> insideLoad = new LinkedHashMap<>();


    static public void write(Serializable s, String filename) throws IOException {
        try(ObjectOutputStream o = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(new File(filename))))) {
            o.writeObject(s);
        }
    }

    static public Serializable read(String filename) throws IOException, ClassNotFoundException {
        try(ObjectInputStream o = new ObjectInputStream(new BufferedInputStream(new FileInputStream(new File(filename))))) {
            return (Serializable) o.readObject();
        }
    }

    public Serializable saveTopology(Box root, Predicate<Box> save, Function<Box, String> alias) {

        List<Node> all = new ArrayList<>();
        Set<Box> complete = root.breadthFirstAll(root.allDownwardsFrom()).collect(Collectors.toSet());

        Function<Box, String> aalias = x -> {
            String q = alias.apply(x);
            if (q!=null) return q;
            if (x==root) return ">>root<<";
            return null;
        };

        insideSave.clear();

        root.breadthFirstAll(root.allDownwardsFrom()).forEach(x -> {
            String a = aalias.apply(x);
            if (a == null && save.test(x)) {
                try {
                    all.add(_saveBox(x, aalias, t -> save.test(t) && complete.contains(t)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });


        Group g = new Group();
        g.nodes = all;
        return g;

    }

    public List<Box> loadTopology(Group g, Box root, Function<String, Box> alias) {

        // policy for checking / merging based on UID

        insideLoad.clear();

        Function<String, Box> aalias = x -> {
            Box q = alias.apply(x);
            if (q!=null) return q;
            if (x.equals(">>root<<")) return root;
            return null;
        };


        for(Node n : g.nodes)
        {
            try {
                insideLoad.put(n, _loadBox(n, aalias, node -> true));
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        return new ArrayList<>(insideLoad.values());

    }




    private Box _loadBox(Node vertex, Function<String, Box> alias, Predicate<Node> load) throws InstantiationException, IllegalAccessException, IOException, ClassNotFoundException {
        if (insideLoad.containsKey(vertex)) {
            return insideLoad.get(vertex);
        }
        if (!load.test(vertex)) return null;

        Set<String> keys = vertex.values.keySet();
        Class<? extends Box> boxClass = Box.class;

        if (keys.contains("__class")) {
            try {
                boxClass = (Class<? extends Box>) this.getClass().getClassLoader().loadClass(""+fromValue(null, vertex.values.get("__class"), alias, load));
            } catch (ClassNotFoundException e) {
                Log.log("error", () -> "can't find class to instantiate box, continuing on anyway");
            }
            // keys.remove("__class");
        }

        Box b = boxClass.newInstance();


        if (keys.contains("__class")) {
            b.properties.put(IO.desiredBoxClass, (String)vertex.values.get("__class"));
            keys.remove("__class");
        }

        insideLoad.put(vertex, b);

        for (String s : keys) {
            if (s.startsWith("__")) continue;

            Dict.Prop p = new Dict.Prop(s);
            Dict.Prop pc = p.findCanon();
            if (pc != null) p = pc;

            Object val = fromValue(p, vertex.values.get(s), alias, load);
            if (val != null) {
                b.properties.put(p, val);
                p.getAttributes().put(IO.persistent, true);
            }
        }

        for (Object c : vertex.children) {
            if (c instanceof Node) {
                Box c2 = _loadBox((Node)c, alias, load);
                if (c2 != null) {
                    b.connect(c2);
                }
            } else if (c instanceof String) {
                Box c2 = alias.apply((String) c);
                if (c2 != null) {
                    b.connect(c2);
                }
            } else
                throw new IllegalArgumentException(" don't know what to make of child " + c + " / " + (c == null ? null : c.getClass()));
        }
        for (Object c : vertex.parents) {
            if (c instanceof Node) {
                Box c2 = _loadBox((Node)c, alias, load);
                if (c2 != null) {
                    c2.connect(b);
                }
            } else if (c instanceof String) {
                Box c2 = alias.apply((String) c);
                if (c2 != null)
                    c2.connect(b);
            } else
                throw new IllegalArgumentException(" don't know what to make of parent " + c + " / " + (c == null ? null : c.getClass()));
        }


        b.properties.put(new Dict.Prop<Node>("_dbvertex"), null);

        // here's where we need to be smart
        // do we mark this UID as external somehow?
        // or perhaps we mark it as null?
//        b.properties.put(IO.id, (String)vertex.values.get("uid"));

        b.properties.remove(IO.id);
        b.properties.remove(new Dict.Prop("uid"));

        System.out.println(" making raft with uid? "+b.properties);

        return b;
    }

    Pattern extractClass = Pattern.compile("%%(.*?)%%(.*)", Pattern.DOTALL);

    private <T> T fromValue(Dict.Prop<T> key, Object value, Function<String, Box> alias, Predicate<Node> load) throws InstantiationException, IllegalAccessException, IOException, ClassNotFoundException {
        if (value == null) return null;

        if (value instanceof String) {
            if (((String) value).startsWith("%%")) {
                Matcher m = extractClass.matcher(((String) value));
                m.find();
                String className = m.group(1);
                String content = m.group(2);

                return (T) fromValue(key, className, content, alias, load);
            }
        }

        if (value instanceof Node) {
            // consider this a little...
            Log.log("error.raft", () -> "cannot deserialize box reference");
            return null;
//            return (T) _loadBox((String) ((Node) value).getProperty("uid"), alias, load);
        }

        return (T) value;
    }



    private <T> T fromValue(Dict.Prop<T> key, String className, String content, Function<String, Box> alias, Predicate<Node> load) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {

        if (className.equals("java.lang.String")) return (T) content;
        if (className.equals("java.io.Serializable")) return (T) deserialize(content);
        if (className.equals("java.lang.Class"))
            return (T) Thread.currentThread().getContextClassLoader().loadClass(content);

        if (className.equals("vertex"))
        {
            Log.log("error.raft", () -> "cannot deserialize box reference");
            return null;//(T) _loadBox(content, alias, load);
        }

        return (T) content;
    }


    public Node saveBox(Box x, Function<Box, String> alias, Predicate<Box> save) throws IOException {
        insideSave.clear();
        try {
            Node v = _saveBox(x, alias, save);
            return v;
        } finally {
            insideSave.clear();
        }
    }

    protected Node _saveBox(Box x, Function<Box, String> alias, Predicate<Box> save) throws IOException {

        if (insideSave.containsKey(x)) return insideSave.get(x);
        if (!save.test(x)) return null;
        if (alias.apply(x) != null)
            throw new IllegalArgumentException(" can't save a box called " + alias.apply(x) + " / " + x);

        String uid = x.properties.getOrConstruct(IO.id);

        Node at = new Node();
        insideSave.put(x, at);

        Map<Dict.Prop, Object> q = x.properties.getMap();
        for (Map.Entry<Dict.Prop, Object> e : q.entrySet()) {
            if (IO.isPeristant(e.getKey()) || e.getKey().getName().equals("code")) { // code is aliased in IO1
                at.values.put(e.getKey().getName(), toValue(e.getKey(), e.getValue(), alias, save));
            }
        }

        if (x.properties.get(IO.desiredBoxClass)!=null)
            at.values.put("__class", x.properties.get(IO.desiredBoxClass));
        else
            at.values.put("__class", x.getClass().toString());

        // disconnected?

        at.children = x.children().stream().filter(z -> save.test(z)).map(z -> {
            try {
                String name = alias.apply(z);
                if (name != null) return name;
                return _saveBox(z, alias, save);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }).filter(z -> z != null).collect(Collectors.toList());

        at.parents = x.parents().stream().filter(z -> save.test(z)).map(z -> {
            try {
                String name = alias.apply(z);
                if (name != null) return name;
                return _saveBox(z, alias, save);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }).filter(z -> z != null).collect(Collectors.toList());

        return at;
    }

    private Object toValue(Dict.Prop key, Object value, Function<Box, String> alias, Predicate<Box> save) throws IOException {

        if (value == null) return null;
        if (value instanceof String) return "%%java.lang.String%%" + value;
        if (value instanceof Box)
            return _saveBox((Box) value, alias, save);

        if (value instanceof Serializable) return "%%java.io.Serializable%%" + serialize((Serializable) value);
        if (value instanceof Class) return "%%java.lang.Class%%" + ((Class) value).getName();

        // error
        return "" + value;
    }

    private String serialize(Serializable value) throws IOException {
        ByteArrayOutputStream a = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(a)) {
            oos.writeObject(value);
            return Base64.getEncoder().encodeToString(a.toByteArray());
        }
    }

    private Object deserialize(String value) throws IOException, ClassNotFoundException {
        byte[] a = Base64.getDecoder().decode(value);
        ByteArrayInputStream b = new ByteArrayInputStream(a);
        ObjectInputStream oos = new ObjectInputStream(b);
        try {
            return oos.readObject();
        } finally {
            oos.close();
        }
    }
}
