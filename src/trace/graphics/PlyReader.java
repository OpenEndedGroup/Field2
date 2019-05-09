package trace.graphics;

import field.graphics.MeshBuilder;
import field.linalg.Vec3;
import field.linalg.Vec4;
import field.utility.Pair;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * Class for reading meshes from files in PLY format.
 * @author Kazó Csaba
 */
public final class PlyReader {

    static public class InvalidPlyFormatException extends IOException {
        /**
         * Constructs an InvalidPlyFormatException with the specified
         * detail message and cause.
         *
         * @param   message   the detail message. The detail message is saved for
         *          later retrieval by the {@link Throwable#getMessage()} method.
         * @param  cause the cause (which is saved for later retrieval by the
         *         {@link Throwable#getCause()} method).
         */
        public InvalidPlyFormatException(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * Constructs an InvalidPlyFormatException with the specified
         * cause.
         *
         * @param  cause the cause (which is saved for later retrieval by the
         *         {@link Throwable#getCause()} method).
         */
        public InvalidPlyFormatException(Throwable cause) {
            super(cause == null ? null : cause.toString());
            this.initCause(cause);
        }

        /**
         * Constructs an InvalidPlyFormatException with the specified
         * detail message.
         *
         * @param   message   the detail message. The detail message is saved for
         *          later retrieval by the {@link Throwable#getMessage()} method.
         */
        public InvalidPlyFormatException(String message) {
            super(message);
        }
    }


    enum Type {
        CHAR {
            @Override
            public Number parse(Scanner scanner) throws IOException {
                try {
                    int value=scanner.nextInt();
                    if (value<-Byte.MIN_VALUE || value>Byte.MAX_VALUE) throw new InputMismatchException("Char out of range");
                    return value;
                } catch (InputMismatchException e) {
                    throw new InvalidPlyFormatException("Cannot parse '"+scanner.next()+"' as char", e);
                } catch (NoSuchElementException e) {
                    throw new InvalidPlyFormatException("Unexpected end of file", e);
                }
            }
            @Override
            public Number read(ByteBuffer buffer) throws IOException {
                return buffer.get();
            }
        },
        UCHAR {
            @Override
            public Number parse(Scanner scanner) throws IOException {
                try {
                    int value=scanner.nextInt();
                    if (value<0 || value>255) throw new InputMismatchException("Uchar out of range: "+value);
                    return value;
                } catch (InputMismatchException e) {
                    throw new InvalidPlyFormatException("Cannot parse '"+scanner.next()+"' as uchar", e);
                } catch (NoSuchElementException e) {
                    throw new InvalidPlyFormatException("Unexpected end of file", e);
                }
            }
            @Override
            public Number read(ByteBuffer buffer) throws IOException {
                return buffer.get() & 0xFF;
            }
        },
        SHORT {
            @Override
            public Number parse(Scanner scanner) throws IOException {
                try {
                    int value=scanner.nextInt();
                    if (value<-Short.MIN_VALUE || value>Short.MAX_VALUE) throw new InputMismatchException("Short out of range");
                    return value;
                } catch (InputMismatchException e) {
                    throw new InvalidPlyFormatException("Cannot parse '"+scanner.next()+"' as short", e);
                } catch (NoSuchElementException e) {
                    throw new InvalidPlyFormatException("Unexpected end of file", e);
                }
            }
            @Override
            public Number read(ByteBuffer buffer) throws IOException {
                return buffer.getShort();
            }
        },
        USHORT {
            @Override
            public Number parse(Scanner scanner) throws IOException {
                try {
                    int value=scanner.nextInt();
                    if (value<0 || value>65535) throw new InputMismatchException("Ushort out of range");
                    return value;
                } catch (InputMismatchException e) {
                    throw new InvalidPlyFormatException("Cannot parse '"+scanner.next()+"' as ushort", e);
                } catch (NoSuchElementException e) {
                    throw new InvalidPlyFormatException("Unexpected end of file", e);
                }
            }

            @Override
            public Number read(ByteBuffer buffer) throws IOException {
                return buffer.getShort() & 0xFFFF;
            }
        },
        INT {
            @Override
            public Number parse(Scanner scanner) throws IOException {
                try {
                    return scanner.nextInt();
                } catch (InputMismatchException e) {
                    throw new InvalidPlyFormatException("Cannot parse '"+scanner.next()+"' as int", e);
                } catch (NoSuchElementException e) {
                    throw new InvalidPlyFormatException("Unexpected end of file", e);
                }
            }

            @Override
            public Number read(ByteBuffer buffer) throws IOException {
                return buffer.getInt();
            }
        },
        UINT {
            @Override
            public Number parse(Scanner scanner) throws IOException {
                try {
                    long value=scanner.nextLong();
                    if (value<0 || value>4294967295L) throw new InputMismatchException("Uint out of range");
                    return value;
                } catch (InputMismatchException e) {
                    throw new InvalidPlyFormatException("Cannot parse '"+scanner.next()+"' as uint", e);
                } catch (NoSuchElementException e) {
                    throw new InvalidPlyFormatException("Unexpected end of file", e);
                }
            }

            @Override
            public Number read(ByteBuffer buffer) throws IOException {
                return buffer.getInt() & 0xFFFFFFFFL;
            }
        },
        FLOAT {
            @Override
            public Number parse(Scanner scanner) throws IOException {
                try {
                    return scanner.nextFloat();
                } catch (InputMismatchException e) {
                    throw new InvalidPlyFormatException("Cannot parse '"+scanner.next()+"' as float", e);
                } catch (NoSuchElementException e) {
                    throw new InvalidPlyFormatException("Unexpected end of file", e);
                }
            }

            @Override
            public Number read(ByteBuffer buffer) throws IOException {
                return buffer.getFloat();
            }
        },
        DOUBLE {
            @Override
            public Number parse(Scanner scanner) throws IOException {
                try {
                    return scanner.nextDouble();
                } catch (InputMismatchException e) {
                    throw new InvalidPlyFormatException("Cannot parse '"+scanner.next()+"' as double", e);
                } catch (NoSuchElementException e) {
                    throw new InvalidPlyFormatException("Unexpected end of file", e);
                }
            }

            @Override
            public Number read(ByteBuffer buffer) throws IOException {
                return buffer.getDouble();
            }

        };
        public abstract Number parse(Scanner scanner) throws IOException;
        public abstract Number read(ByteBuffer buffer) throws IOException;
    }

    static abstract class Property {
        public String name;

        public Property(String name) {
            this.name = name;
        }

    }

    static class ListProperty extends Property {
        public Type countType;
        public Type elemType;

        public ListProperty(String name, Type countType, Type elemType) {
            super(name);
            this.countType = countType;
            this.elemType = elemType;
        }

    }
    
    static class ScalarProperty extends Property {
        public Type type;

        public ScalarProperty(String name, Type type) {
            super(name);
            this.type = type;
        }

    }

    static public class Element {
        public String name;
        public int count;
        public List<Property> properties=new ArrayList<>();

        public Element(String name, int count) {
            this.name = name;
            this.count = count;
        }

    }
    private final List<Element> elements;

    private final Path file;

    // null means ascii
    private final ByteOrder fileFormat;

    private Element vertexElement=null;
    private int vertexXPropIndex=-1, vertexYPropIndex=-1, vertexZPropIndex=-1;
    private int vertexRedPropIndex=-1, vertexGreenPropIndex=-1, vertexBluePropIndex=-1;
    private int normalXPropIndex=-1, normalYPropIndex=-1, normalZPropIndex=-1;
    private Element faceElement=null;
    private int vertexIndicesPropIndex=-1;

    private final boolean hasVertices, hasVertexColors, hasFaces, hasNormals;

    /**
     * Creates a new instance that reads data from the specified file. The constructor parses the header of the PLY file,
     * and the user should query its contents with the {@link #hasVertices()}, {@link #hasFaces()} etc. functions before
     * calling the appropriate reader method.
     * @param file the file to read from
     * @throws InvalidPlyFormatException if the file format is incorrect
     * @throws IOException if an I/O error occurs
     */
    public PlyReader(Path file) throws IOException, InvalidPlyFormatException {
        this.file=file;
        try (Scanner scanner=new Scanner(file, "US-ASCII")) {
            scanner.useLocale(Locale.ROOT);
            String line=scanner.nextLine();
            if (line==null || !line.equals("ply"))
                throw new InvalidPlyFormatException("File is not in PLY format");

            String format=null;
            String version=null;
            elements=new ArrayList<>();
            { // parse header
                Element currentElement=null;
                while (true) {
                    if (!scanner.hasNextLine()) {
                        throw new InvalidPlyFormatException("Unexpected end of file");
                    }
                    line=scanner.nextLine();
                    Scanner wordScanner=new Scanner(line);
                    String keyword=wordScanner.next();
                    if ("format".equals(keyword)) {
                        format=wordScanner.next();
                        version=wordScanner.next();
                        if (wordScanner.hasNext()) throw new InvalidPlyFormatException("Invalid file format");
                    } else if ("comment".equals(keyword))
                        continue;
                    else if ("element".equals(keyword)) {
                        String name=wordScanner.next();
                        int count=wordScanner.nextInt();
                        if (count<0) throw new InvalidPlyFormatException("Element "+name+" has negative instances");
                        if (wordScanner.hasNext()) throw new InvalidPlyFormatException("Invalid file format");
                        currentElement=new Element(name, count);
                        elements.add(currentElement);
                    } else if ("property".equals(keyword)) {
                        if (currentElement==null) throw new InvalidPlyFormatException("Property without element");
                        Property property;
                        String type=wordScanner.next();
                        if ("list".equals(type)) {
                            Type countType=parse(wordScanner.next());
                            if (countType==Type.FLOAT || countType==Type.DOUBLE) throw new InvalidPlyFormatException("List element count type must be integral");
                            Type elemType=parse(wordScanner.next());
                            String name=wordScanner.next();
                            if (wordScanner.hasNext()) throw new InvalidPlyFormatException("Invalid file format");
                            property=new ListProperty(name, countType, elemType);
                        } else {
                            String name=wordScanner.next();
                            Type scalarType=parse(type);
                            property=new ScalarProperty(name, scalarType);
                        }
                        currentElement.properties.add(property);
                    } else if ("obj_info".equals(keyword)) {
                        // ignore
                    } else if ("end_header".equals(keyword))
                        break;
                    else
                        throw new InvalidPlyFormatException("Unrecognized keyword in header: "+keyword);
                }
            }
            if (format==null) throw new InvalidPlyFormatException("No format specification found in header");
            if (!"1.0".equals(version)) throw new InvalidPlyFormatException("Unknown format version: "+version);
            if ("ascii".equals(format)) {
                fileFormat=null;
            } else {
                if ("binary_big_endian".equals(format))
                    fileFormat=ByteOrder.BIG_ENDIAN;
                else if ("binary_little_endian".equals(format))
                    fileFormat=ByteOrder.LITTLE_ENDIAN;
                else
                    throw new InvalidPlyFormatException("Invalid format: "+format);
            }
        }

        for (Element e: elements) {
            if ("vertex".equals(e.name)) {
                if (vertexElement!=null) throw new InvalidPlyFormatException("Multiple vertex elements");
                vertexElement=e;
                for (int pi=0; pi<e.properties.size(); pi++) {
                    Property p=e.properties.get(pi);
                    switch (p.name) {
                        case "x":
                            if (p instanceof ListProperty) throw new InvalidPlyFormatException("Invalid vertex.x property");
                            if (vertexXPropIndex!=-1) throw new InvalidPlyFormatException("Multiple vertex.x properties");
                            vertexXPropIndex=pi;
                            break;
                        case "y":
                            if (p instanceof ListProperty) throw new InvalidPlyFormatException("Invalid vertex.y property");
                            if (vertexYPropIndex!=-1) throw new InvalidPlyFormatException("Multiple vertex.y properties");
                            vertexYPropIndex=pi;
                            break;
                        case "z":
                            if (p instanceof ListProperty) throw new InvalidPlyFormatException("Invalid vertex.z property");
                            if (vertexZPropIndex!=-1) throw new InvalidPlyFormatException("Multiple vertex.z properties");
                            vertexZPropIndex=pi;
                            break;
                        case "red":
                            if (p instanceof ListProperty) throw new InvalidPlyFormatException("Invalid vertex.red property");
                            if (vertexRedPropIndex!=-1) throw new InvalidPlyFormatException("Multiple vertex.red properties");
                            vertexRedPropIndex=pi;
                            break;
                        case "green":
                            if (p instanceof ListProperty) throw new InvalidPlyFormatException("Invalid vertex.green property");
                            if (vertexGreenPropIndex!=-1) throw new InvalidPlyFormatException("Multiple vertex.green properties");
                            vertexGreenPropIndex=pi;
                            break;
                        case "blue":
                            if (p instanceof ListProperty) throw new InvalidPlyFormatException("Invalid vertex.blue property");
                            if (vertexBluePropIndex!=-1) throw new InvalidPlyFormatException("Multiple vertex.blue properties");
                            vertexBluePropIndex=pi;
                            break;

                        case "nx":
                            if (p instanceof ListProperty) throw new InvalidPlyFormatException("Invalid normal.x property");
                            if (normalXPropIndex!=-1) throw new InvalidPlyFormatException("Multiple normal.x properties");
                            normalXPropIndex=pi;
                            break;
                        case "ny":
                            if (p instanceof ListProperty) throw new InvalidPlyFormatException("Invalid normal.y property");
                            if (normalYPropIndex!=-1) throw new InvalidPlyFormatException("Multiple normal.y properties");
                            normalYPropIndex=pi;
                            break;
                        case "nz":
                            if (p instanceof ListProperty) throw new InvalidPlyFormatException("Invalid normal.z property");
                            if (normalZPropIndex!=-1) throw new InvalidPlyFormatException("Multiple normal.z properties");
                            normalZPropIndex=pi;
                            break;



                        case "diffuse_red":
                            if (p instanceof ListProperty) throw new InvalidPlyFormatException("Invalid vertex.red property");
                            if (vertexRedPropIndex!=-1) throw new InvalidPlyFormatException("Multiple vertex.red properties");
                            vertexRedPropIndex=pi;
                            break;
                        case "diffuse_green":
                            if (p instanceof ListProperty) throw new InvalidPlyFormatException("Invalid vertex.green property");
                            if (vertexGreenPropIndex!=-1) throw new InvalidPlyFormatException("Multiple vertex.green properties");
                            vertexGreenPropIndex=pi;
                            break;
                        case "diffuse_blue":
                            if (p instanceof ListProperty) throw new InvalidPlyFormatException("Invalid vertex.blue property");
                            if (vertexBluePropIndex!=-1) throw new InvalidPlyFormatException("Multiple vertex.blue properties");
                            vertexBluePropIndex=pi;
                            break;
                    }
                }
            } else if ("face".equals(e.name)) {
                if (faceElement!=null) throw new InvalidPlyFormatException("Multiple face elements");
                faceElement=e;
                for (int pi=0; pi<e.properties.size(); pi++) {
                    Property p=e.properties.get(pi);
                    if (p.name.startsWith("vertex_ind")) {

                        if (p instanceof ScalarProperty) throw new InvalidPlyFormatException("Face.vertex_indices property is not a list");
                        if (((ListProperty)p).elemType==Type.FLOAT || ((ListProperty)p).elemType==Type.DOUBLE) throw new InvalidPlyFormatException("Face vertex indices must be integral");
                        if (vertexIndicesPropIndex!=-1) throw new InvalidPlyFormatException("Multiple face.vertex_indices properties");
                        vertexIndicesPropIndex=pi;
                    }
                }
            }
        }
        hasVertices=vertexElement!=null;
        if (hasVertices) {
            if (vertexXPropIndex==-1) throw new InvalidPlyFormatException("No vertex.x property found");
            if (vertexYPropIndex==-1) throw new InvalidPlyFormatException("No vertex.y property found");
            if (vertexZPropIndex==-1) throw new InvalidPlyFormatException("No vertex.z property found");

        }

        hasVertexColors=vertexRedPropIndex!=-1 || vertexGreenPropIndex!=-1 || vertexBluePropIndex!=-1;
        if (hasVertexColors) {
            if (vertexRedPropIndex==-1 || vertexGreenPropIndex==-1 || vertexBluePropIndex==-1) throw new InvalidPlyFormatException("Incomplete vertex color");
        }

        hasNormals = normalXPropIndex!=-1 || normalYPropIndex!=-1 || normalZPropIndex!=-1;
        if (hasNormals) {
            if (normalXPropIndex==-1 || normalYPropIndex==-1 || normalZPropIndex==-1) throw new InvalidPlyFormatException("Incomplete normals");
        }


        hasFaces=faceElement!=null && faceElement.count>0;
        if (hasFaces) {
            if (!hasVertices) throw new InvalidPlyFormatException("Faces without vertices");
            if (vertexIndicesPropIndex==-1) throw new InvalidPlyFormatException("No face.vertex_indices property found");
        }
    }

    /**
     * Returns whether the PLY file contains vertex data.
     * @return {@code true} if the file contains vertices
     */
    public boolean hasVertices() {
        return hasVertices;
    }
    /**
     * Returns whether the PLY file contains colored vertex data.
     * @return {@code true} if the file contains vertices along with vertex colors
     */
    public boolean hasVertexColors() {
        return hasVertexColors;
    }
    /**
     * Returns whether the PLY file contains a mesh.
     * @return {@code true} if the file contains mesh data (vertices and faces)
     */
    public boolean hasFaces() {
        return hasFaces;
    }
    private Input getInput() throws IOException {
        if (fileFormat==null) {
            return new AsciiInput(Files.newInputStream(file));
        } else {
            return new BinaryInput(Files.newByteChannel(file, StandardOpenOption.READ), fileFormat);
        }
    }
    /**
     * Reads vertices from the file.
     * @return the vertices defined by this file as a point list
     * @throws IOException if an I/O error occurs
     * @throws InvalidPlyFormatException if the file format is incorrect
     * @throws IllegalStateException if the file does not contain vertex data
     */
    public List<Vec3> readVertices() throws IOException, InvalidPlyFormatException {
        if (!hasVertices) throw new IllegalStateException("No vertices");
        if (hasVertexColors) return readColoredVertices().first;

        Input input=getInput();

        List<Vec3> vertices=new ArrayList<>(vertexElement.count);

        for (Element currentElement: elements) {
            if (currentElement==vertexElement) {
				/* Parse vertices */
                for (int elemIndex=0; elemIndex<currentElement.count; elemIndex++) {
                    Vec3 v= new Vec3();
                    vertices.add(v);
                    for (int propIndex=0; propIndex<currentElement.properties.size(); propIndex++) {
                        Property prop=currentElement.properties.get(propIndex);
                        if (propIndex==vertexXPropIndex) {
                            v.x(input.read(((ScalarProperty)prop).type).doubleValue());
                        } else if (propIndex==vertexYPropIndex) {
                            v.y(input.read(((ScalarProperty)prop).type).doubleValue());
                        } else if (propIndex==vertexZPropIndex) {
                            v.z(input.read(((ScalarProperty)prop).type).doubleValue());
                        } else {
                            // ignore any other property
                            if (prop instanceof ListProperty) {
                                int count=input.read(((ListProperty)prop).countType).intValue();
                                if (count<0) throw new InvalidPlyFormatException("List with negative number of elements");
                                for (int i=0; i<count; i++) {
                                    input.read(((ListProperty)prop).elemType);
                                }
                            } else {
                                input.read(((ScalarProperty)prop).type);
                            }
                        }
                    }
                }
            } else {
				/* Parse anything else */
                for (int elemIndex=0; elemIndex<currentElement.count; elemIndex++) {
                    for (int propIndex=0; propIndex<currentElement.properties.size(); propIndex++) {
                        Property prop=currentElement.properties.get(propIndex);
                        if (prop instanceof ListProperty) {
                            int count=input.read(((ListProperty)prop).countType).intValue();
                            if (count<0) throw new InvalidPlyFormatException("List with negative number of elements");
                            for (int i=0; i<count; i++) {
                                input.read(((ListProperty)prop).elemType);
                            }
                        } else {
                            input.read(((ScalarProperty)prop).type);
                        }
                    }
                }
            }
        }
        input.needEnd();

        return new ArrayList<Vec3>(vertices);
    }
    /**
     * Reads colored vertices from the file.
     * @return the vertices defined by this file as a colored point list
     * @throws IOException if an I/O error occurs
     * @throws InvalidPlyFormatException if the file format is incorrect
     * @throws IllegalStateException if the file does not contain colored vertex data
     */
    public Pair<List<Vec3>, List<Vec4>> readColoredVertices() throws IOException, InvalidPlyFormatException {
        if (!hasVertices) throw new IllegalStateException("No vertices");
        if (!hasVertexColors) throw new IllegalStateException("No vertex colors");

        List<Vec3> vertices=new ArrayList<>(vertexElement.count);
        List<Vec4> colors=new ArrayList<>(vertexElement.count);

        try (Input input=getInput()) {

            for (Element currentElement: elements) {
                if (currentElement==vertexElement) {
                    /* Parse vertices */
                    int red=-1, green=-1, blue=-1;
                    for (int elemIndex=0; elemIndex<currentElement.count; elemIndex++) {
                        Vec3 v= new Vec3();
                        vertices.add(v);
                        for (int propIndex=0; propIndex<currentElement.properties.size(); propIndex++) {
                            Property prop=currentElement.properties.get(propIndex);
                            if (propIndex==vertexXPropIndex) {
                                v.x(input.read(((ScalarProperty)prop).type).doubleValue());
                            } else if (propIndex==vertexYPropIndex) {
                                v.y(input.read(((ScalarProperty)prop).type).doubleValue());
                            } else if (propIndex==vertexZPropIndex) {
                                v.z(input.read(((ScalarProperty)prop).type).doubleValue());
                            } else if (propIndex==vertexRedPropIndex) {
                                red=input.read(((ScalarProperty)prop).type).intValue();
                            } else if (propIndex==vertexGreenPropIndex) {
                                green=input.read(((ScalarProperty)prop).type).intValue();
                            } else if (propIndex==vertexBluePropIndex) {
                                blue=input.read(((ScalarProperty)prop).type).intValue();
                            } else {
                                // ignore any other property
                                if (prop instanceof ListProperty) {
                                    int count=input.read(((ListProperty)prop).countType).intValue();
                                    if (count<0) throw new InvalidPlyFormatException("List with negative number of elements");
                                    for (int i=0; i<count; i++) {
                                        input.read(((ListProperty)prop).elemType);
                                    }
                                } else {
                                    input.read(((ScalarProperty)prop).type);
                                }
                            }
                        }
                        colors.add(new Vec4(red, green, blue, 1));
                    }
                } else {
                    /* Parse anything else */
                    for (int elemIndex=0; elemIndex<currentElement.count; elemIndex++) {
                        for (int propIndex=0; propIndex<currentElement.properties.size(); propIndex++) {
                            Property prop=currentElement.properties.get(propIndex);
                            if (prop instanceof ListProperty) {
                                int count=input.read(((ListProperty)prop).countType).intValue();
                                if (count<0) throw new InvalidPlyFormatException("List with negative number of elements");
                                for (int i=0; i<count; i++) {
                                    input.read(((ListProperty)prop).elemType);
                                }
                            } else {
                                input.read(((ScalarProperty)prop).type);
                            }
                        }
                    }
                }
            }
            input.needEnd();
        }

        return new Pair<List<Vec3>, List<Vec4>>(vertices, colors);
    }


    public class Vertex
    {
        public Vec3 at = new Vec3();
        public Map<String, List<Float>> attributes = new LinkedHashMap<>();

        public void x(double v) {
            at.x = v;
        }

        public void y(double v) {
            at.y = v;
        }

        public void z(double v) {
            at.z = v;
        }
    }

    /**
     * Reads a mesh from this file.
     * @return the mesh contained in the file
     * @throws IOException if an I/O error occurs
     * @throws InvalidPlyFormatException if the format of the file is incorrect
     * @throws IllegalStateException if the file doesn't contain any faces ({@link #hasFaces()} returns {@code false})
     */
    public Pair<List<Vertex>, List<int[]>> readMesh() throws IOException, InvalidPlyFormatException {
        if (!hasFaces) throw new IllegalStateException("No faces");

        List<Vertex> vertices=new ArrayList<>(vertexElement.count);
        List<int[]> triangles=new ArrayList<>(faceElement.count);

        try (Input input=getInput()) {

            for (Element currentElement: elements) {
                if (currentElement==vertexElement) {
					/* Parse vertices */
                    for (int elemIndex=0; elemIndex<currentElement.count; elemIndex++) {
                        Vertex v=new Vertex();
                        vertices.add(v);
                        for (int propIndex=0; propIndex<currentElement.properties.size(); propIndex++) {
                            Property prop=currentElement.properties.get(propIndex);
                            if (propIndex==vertexXPropIndex) {
                                v.x(input.read(((ScalarProperty)prop).type).doubleValue());
                            } else if (propIndex==vertexYPropIndex) {
                                v.y(input.read(((ScalarProperty)prop).type).doubleValue());
                            } else if (propIndex==vertexZPropIndex) {
                                v.z(input.read(((ScalarProperty)prop).type).doubleValue());
                            } else {
                                if (prop instanceof ListProperty) {
                                    List<Float> d = new ArrayList<>();
                                    int count=input.read(((ListProperty)prop).countType).intValue();
                                    if (count<0) throw new InvalidPlyFormatException("List with negative number of elements");
                                    for (int i=0; i<count; i++) {
                                        d.add(input.read(((ListProperty)prop).elemType).floatValue());
                                    }
                                    v.attributes.put(prop.name, d);
                                } else {
                                    float d = input.read(((ScalarProperty)prop).type).floatValue();
                                    v.attributes.put(prop.name, Collections.singletonList(d));
                                }
                            }
                        }
                    }
                } else if (currentElement==faceElement) {
					/* Parse faces */
                    for (int elemIndex=0; elemIndex<currentElement.count; elemIndex++) {
                        for (int propIndex=0; propIndex<currentElement.properties.size(); propIndex++) {
                            Property prop=currentElement.properties.get(propIndex);
                            if (propIndex==vertexIndicesPropIndex) {
                                ListProperty lp=(ListProperty)prop;
                                int count=input.read(lp.countType).intValue();
                                if (count<3) throw new InvalidPlyFormatException("Face with "+count+" vertices");
                                switch (count) {
                                    case 3:
                                        Number v1,v2,v3,v4;
                                        v1=input.read(lp.elemType);
                                        if (v1.longValue()<0 || v1.longValue()>=vertexElement.count) throw new InvalidPlyFormatException("Invalid vertex index: "+v1.longValue());
                                        v2=input.read(lp.elemType);
                                        if (v2.longValue()<0 || v2.longValue()>=vertexElement.count) throw new InvalidPlyFormatException("Invalid vertex index: "+v2.longValue());
                                        v3=input.read(lp.elemType);
                                        if (v3.longValue()<0 || v3.longValue()>=vertexElement.count) throw new InvalidPlyFormatException("Invalid vertex index: "+v3.longValue());
                                        triangles.add(new int[]{v1.intValue(), v2.intValue(), v3.intValue()});
                                        break;
                                    case 4:
                                        v1=input.read(lp.elemType);
                                        if (v1.longValue()<0 || v1.longValue()>=vertexElement.count) throw new InvalidPlyFormatException("Invalid vertex index: "+v1.longValue());
                                        v2=input.read(lp.elemType);
                                        if (v2.longValue()<0 || v2.longValue()>=vertexElement.count) throw new InvalidPlyFormatException("Invalid vertex index: "+v2.longValue());
                                        v3=input.read(lp.elemType);
                                        if (v3.longValue()<0 || v3.longValue()>=vertexElement.count) throw new InvalidPlyFormatException("Invalid vertex index: "+v3.longValue());
                                        v4=input.read(lp.elemType);
                                        if (v4.longValue()<0 || v4.longValue()>=vertexElement.count) throw new InvalidPlyFormatException("Invalid vertex index: "+v4.longValue());
                                        triangles.add(new int[]{v1.intValue(), v2.intValue(), v3.intValue()});
                                        triangles.add(new int[]{v1.intValue(), v3.intValue(), v4.intValue()});
                                        break;
                                    default:
                                        throw new InvalidPlyFormatException("Cannot handle faces with more than 4 vertices");
                                }
                            } else if (prop instanceof ListProperty) {
                                int count=input.read(((ListProperty)prop).countType).intValue();
                                if (count<0) throw new InvalidPlyFormatException("List with negative number of elements");
                                for (int i=0; i<count; i++) {
                                    input.read(((ListProperty)prop).elemType);
                                }
                            } else {
                                input.read(((ScalarProperty)prop).type);
                            }
                        }
                    }
                } else {
					/* Parse anything else */
                    for (int elemIndex=0; elemIndex<currentElement.count; elemIndex++) {
                        for (int propIndex=0; propIndex<currentElement.properties.size(); propIndex++) {
                            Property prop=currentElement.properties.get(propIndex);
                            if (prop instanceof ListProperty) {
                                int count=input.read(((ListProperty)prop).countType).intValue();
                                if (count<0) throw new InvalidPlyFormatException("List with negative number of elements");
                                for (int i=0; i<count; i++) {
                                    input.read(((ListProperty)prop).elemType);
                                }
                            } else {
                                input.read(((ScalarProperty)prop).type);
                            }
                        }
                    }
                }
            }
            input.needEnd();
        }
        return new Pair(vertices, triangles);
    }

    interface Input extends Closeable {
        public Number read(Type type) throws IOException;
        public void needEnd() throws IOException;
    }
    private static class AsciiInput implements Input {
        private final Scanner scanner;

        public AsciiInput(InputStream in) throws IOException {
            scanner = new Scanner(new BufferedInputStream(in), "US-ASCII");

            // skip the header
            String line;
            do {
                line=scanner.nextLine();
            } while (!"end_header".equals(line));
        }

        @Override
        public Number read(Type type) throws IOException {
            return type.parse(scanner);
        }

        @Override
        public void needEnd() throws IOException {
            if (scanner.hasNext())
                throw new InvalidPlyFormatException("Invalid file format: expected end of file, found "+scanner.next());
        }

        @Override
        public void close() throws IOException {
            scanner.close();
        }

    }
    private static class BinaryInput implements Input {
        private final ReadableByteChannel channel;
        private final ByteBuffer buffer;
        private int bufferLength;

        public BinaryInput(ReadableByteChannel channel, ByteOrder byteOrder) throws IOException {
            final byte[] END="end_header".getBytes("US-ASCII");
            byte[] endTest=new byte[END.length];

            this.channel=channel;
            buffer=ByteBuffer.allocate(8192).order(byteOrder);
            bufferLength=0;
            // skip header
            int lineStart=0;
            int read;
            while (true) {
                read=channel.read(buffer);
                if (read==-1) throw new InvalidPlyFormatException("Cannot find the end of the header on the second pass: file has been modified");
                bufferLength+=read;
                for (int i=bufferLength-read; i<bufferLength; i++) {
                    if (buffer.get(i)==(byte)'\n') {
                        int length=i-lineStart;
                        if (length==END.length) {
                            buffer.position(lineStart);
                            buffer.get(endTest);
                            buffer.get(); // skip the '\n'
                            if (Arrays.equals(END, endTest)) {
                                // done skipping header
                                buffer.limit(bufferLength);
                                buffer.compact();
                                buffer.flip();
                                return;
                            }
                        }
                        lineStart=i+1;
                    }
                }
                if (buffer.remaining()==0) {
                    if (lineStart==0) throw new InvalidPlyFormatException("Line too long");
                    buffer.position(lineStart);
                    buffer.limit(bufferLength);
                    buffer.compact();
                    bufferLength-=lineStart;
                    lineStart=0;
                    buffer.limit(buffer.capacity());
                }
            }
        }

        @Override
        public Number read(Type type) throws IOException {
            while (true) {
                try {
                    return type.read(buffer);
                } catch (BufferUnderflowException e) {}
                int position=buffer.position();
                int limit=buffer.limit();

                if (position>buffer.capacity()-20) {
                    buffer.compact();
                    limit=limit-position;
                    position=0;
                }

                buffer.limit(buffer.capacity());
                buffer.position(limit);
                int read=channel.read(buffer);
                if (read==-1) throw new InvalidPlyFormatException("Unexpected end of file");
                if (read==0) throw new AssertionError();
                buffer.limit(limit+read);
                buffer.position(position);
            }
        }

        @Override
        public void needEnd() throws IOException {
            if (buffer.remaining()!=0) throw new InvalidPlyFormatException("Expected end of file");
            buffer.position(0);
            buffer.limit(1);
            if (channel.read(buffer)!=-1) throw new InvalidPlyFormatException("Expected end of file");
        }

        @Override
        public void close() throws IOException {
            channel.close();
        }

    }
    /**
     * Reads a mesh from a PLY file.
     * @param file the file to read from
     * @return the mesh contained in the file
     * @throws IOException if an I/O error occurs
     * @throws InvalidPlyFormatException if the format of the file is incorrect or no mesh is found
     */
    public static Pair<List<Vertex>, List<int[]>> readMesh(File file) throws IOException, InvalidPlyFormatException {
        PlyReader reader=new PlyReader(file.toPath());
        if (!reader.hasFaces()) throw new InvalidPlyFormatException("No faces found");

        return reader.readMesh();
    }


    private static Type parse(String type) throws InvalidPlyFormatException {
        if (type.equals("char")) return Type.CHAR;
        if (type.equals("uchar")) return Type.UCHAR;
        if (type.equals("short")) return Type.SHORT;
        if (type.equals("ushort")) return Type.USHORT;
        if (type.equals("int")) return Type.INT;
        if (type.equals("uint")) return Type.UINT;
        if (type.equals("float")) return Type.FLOAT;
        if (type.equals("double")) return Type.DOUBLE;
        throw new InvalidPlyFormatException("Unrecognized type: "+type);
    }
}
