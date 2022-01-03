package trace.graphics;

import field.graphics.GraphicsContext;
import field.utility.DocumentationProxyTo;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.NativeType;

import java.nio.*;
import java.util.concurrent.Callable;

/**
 * GL wrapped with a thread check
 */
@DocumentationProxyTo(GL11.class)
public class GL {

	@DocumentationProxyTo(GL11.class)
	public static void glEnable(int target) {
		check(() -> GL11.glEnable(target));
	}

	private static void check(Runnable o) {
		check(GraphicsContext.getContext() != null);
		o.run();
	}

	private static <T> T check2(Callable<T> o) {
		check(GraphicsContext.getContext() != null);
		try {
			return o.call();
		} catch (Exception e) {
			RuntimeException ex = new RuntimeException("exception thrown from OpenGL");
			ex.initCause(ex);
			throw ex;
		}
	}


	private static void check(boolean b) {
		if (!b)
			throw new IllegalArgumentException("can't run OpenGL here -- put it in a scene instead");
	}

	@DocumentationProxyTo(GL11.class)
	public static void glDisable(int target) {
		check(() -> GL11.glDisable(target));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glAccum(int op, float value) {
		check(() -> GL11.glAccum(op, value));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glAlphaFunc(int func, float ref) {
		check(() -> GL11.glAlphaFunc(func, ref));
	}

	@NativeType("GLboolean")
	@DocumentationProxyTo(GL11.class)
	public static boolean glAreTexturesResident(IntBuffer textures, ByteBuffer residences) {
		return check2(() -> GL11.glAreTexturesResident(textures, residences));
	}

	@NativeType("GLboolean")
	@DocumentationProxyTo(GL11.class)
	public static boolean glAreTexturesResident(int texture, ByteBuffer residences) {
		return check2(() -> GL11.glAreTexturesResident(texture, residences));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glArrayElement(int i) {
		check(() -> GL11.glArrayElement(i));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glBegin(int mode) {
		check(() -> GL11.glBegin(mode));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glBindTexture(int target, int texture) {
		check(() -> GL11.glBindTexture(target, texture));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glBitmap(int w, int h, float xOrig, float yOrig, float xInc, float yInc, ByteBuffer data) {
		check(() -> GL11.glBitmap(w, h, xOrig, yOrig, xInc, yInc, data));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glBitmap(int w, int h, float xOrig, float yOrig, float xInc, float yInc, long data) {
		check(() -> GL11.glBitmap(w, h, xOrig, yOrig, xInc, yInc, data));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glBlendFunc(int sfactor, int dfactor) {
		check(() -> GL11.glBlendFunc(sfactor, dfactor));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glCallList(int list) {
		check(() -> GL11.glCallList(list));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glCallLists(int type, ByteBuffer lists) {
		check(() -> GL11.glCallLists(type, lists));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glCallLists(ByteBuffer lists) {
		check(() -> GL11.glCallLists(lists));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glCallLists(ShortBuffer lists) {
		check(() -> GL11.glCallLists(lists));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glCallLists(IntBuffer lists) {
		check(() -> GL11.glCallLists(lists));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glClear(int mask) {
		check(() -> GL11.glClear(mask));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glClearAccum(float red, float green, float blue, float alpha) {
		check(() -> GL11.glClearAccum(red, green, blue, alpha));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glClearColor(float red, float green, float blue, float alpha) {
		check(() -> GL11.glClearColor(red, green, blue, alpha));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glClearDepth(double depth) {
		check(() -> GL11.glClearDepth(depth));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glClearIndex(float index) {
		check(() -> GL11.glClearIndex(index));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glClearStencil(int s) {
		check(() -> GL11.glClearStencil(s));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glClipPlane(int plane, DoubleBuffer equation) {
		check(() -> GL11.glClipPlane(plane, equation));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor3b(byte red, byte green, byte blue) {
		check(() -> GL11.glColor3b(red, green, blue));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor3s(short red, short green, short blue) {
		check(() -> GL11.glColor3s(red, green, blue));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor3i(int red, int green, int blue) {
		check(() -> GL11.glColor3i(red, green, blue));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor3f(float red, float green, float blue) {
		check(() -> GL11.glColor3f(red, green, blue));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor3d(double red, double green, double blue) {
		check(() -> GL11.glColor3d(red, green, blue));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor3ub(byte red, byte green, byte blue) {
		check(() -> GL11.glColor3ub(red, green, blue));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor3us(short red, short green, short blue) {
		check(() -> GL11.glColor3us(red, green, blue));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor3ui(int red, int green, int blue) {
		check(() -> GL11.glColor3ui(red, green, blue));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor3bv(ByteBuffer v) {
		check(() -> GL11.glColor3bv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor3sv(ShortBuffer v) {
		check(() -> GL11.glColor3sv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor3iv(IntBuffer v) {
		check(() -> GL11.glColor3iv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor3fv(FloatBuffer v) {
		check(() -> GL11.glColor3fv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor3dv(DoubleBuffer v) {
		check(() -> GL11.glColor3dv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor3ubv(ByteBuffer v) {
		check(() -> GL11.glColor3ubv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor3usv(ShortBuffer v) {
		check(() -> GL11.glColor3usv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor3uiv(IntBuffer v) {
		check(() -> GL11.glColor3uiv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor4b(byte red, byte green, byte blue, byte alpha) {
		check(() -> GL11.glColor4b(red, green, blue, alpha));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor4s(short red, short green, short blue, short alpha) {
		check(() -> GL11.glColor4s(red, green, blue, alpha));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor4i(int red, int green, int blue, int alpha) {
		check(() -> GL11.glColor4i(red, green, blue, alpha));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor4f(float red, float green, float blue, float alpha) {
		check(() -> GL11.glColor4f(red, green, blue, alpha));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor4d(double red, double green, double blue, double alpha) {
		check(() -> GL11.glColor4d(red, green, blue, alpha));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor4ub(byte red, byte green, byte blue, byte alpha) {
		check(() -> GL11.glColor4ub(red, green, blue, alpha));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor4us(short red, short green, short blue, short alpha) {
		check(() -> GL11.glColor4us(red, green, blue, alpha));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor4ui(int red, int green, int blue, int alpha) {
		check(() -> GL11.glColor4ui(red, green, blue, alpha));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor4bv(ByteBuffer v) {
		check(() -> GL11.glColor4bv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor4sv(ShortBuffer v) {
		check(() -> GL11.glColor4sv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor4iv(IntBuffer v) {
		check(() -> GL11.glColor4iv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor4fv(FloatBuffer v) {
		check(() -> GL11.glColor4fv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor4dv(DoubleBuffer v) {
		check(() -> GL11.glColor4dv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor4ubv(ByteBuffer v) {
		check(() -> GL11.glColor4ubv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor4usv(ShortBuffer v) {
		check(() -> GL11.glColor4usv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor4uiv(IntBuffer v) {
		check(() -> GL11.glColor4uiv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
		check(() -> GL11.glColorMask(red, green, blue, alpha));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColorMaterial(int face, int mode) {
		check(() -> GL11.glColorMaterial(face, mode));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColorPointer(int size, int type, int stride, ByteBuffer pointer) {
		check(() -> GL11.glColorPointer(size, type, stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColorPointer(int size, int type, int stride, long pointer) {
		check(() -> GL11.glColorPointer(size, type, stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColorPointer(int size, int type, int stride, ShortBuffer pointer) {
		check(() -> GL11.glColorPointer(size, type, stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColorPointer(int size, int type, int stride, IntBuffer pointer) {
		check(() -> GL11.glColorPointer(size, type, stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColorPointer(int size, int type, int stride, FloatBuffer pointer) {
		check(() -> GL11.glColorPointer(size, type, stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glCopyPixels(int x, int y, int width, int height, int type) {
		check(() -> GL11.glCopyPixels(x, y, width, height, type));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glCullFace(int mode) {
		check(() -> GL11.glCullFace(mode));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glDeleteLists(int list, int range) {
		check(() -> GL11.glDeleteLists(list, range));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glDepthFunc(int func) {
		check(() -> GL11.glDepthFunc(func));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glDepthMask(boolean flag) {
		check(() -> GL11.glDepthMask(flag));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glDepthRange(double zNear, double zFar) {
		check(() -> GL11.glDepthRange(zNear, zFar));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glDisableClientState(int cap) {
		check(() -> GL11.glDisableClientState(cap));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glDrawArrays(int mode, int first, int count) {
		check(() -> GL11.glDrawArrays(mode, first, count));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glDrawBuffer(int buf) {
		check(() -> GL11.glDrawBuffer(buf));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glDrawElements(int mode, int count, int type, long indices) {
		check(() -> GL11.glDrawElements(mode, count, type, indices));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glDrawElements(int mode, int type, ByteBuffer indices) {
		check(() -> GL11.glDrawElements(mode, type, indices));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glDrawElements(int mode, ByteBuffer indices) {
		check(() -> GL11.glDrawElements(mode, indices));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glDrawElements(int mode, ShortBuffer indices) {
		check(() -> GL11.glDrawElements(mode, indices));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glDrawElements(int mode, IntBuffer indices) {
		check(() -> GL11.glDrawElements(mode, indices));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glDrawPixels(int width, int height, int format, int type, ByteBuffer pixels) {
		check(() -> GL11.glDrawPixels(width, height, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glDrawPixels(int width, int height, int format, int type, long pixels) {
		check(() -> GL11.glDrawPixels(width, height, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glDrawPixels(int width, int height, int format, int type, ShortBuffer pixels) {
		check(() -> GL11.glDrawPixels(width, height, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glDrawPixels(int width, int height, int format, int type, IntBuffer pixels) {
		check(() -> GL11.glDrawPixels(width, height, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glDrawPixels(int width, int height, int format, int type, FloatBuffer pixels) {
		check(() -> GL11.glDrawPixels(width, height, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glEdgeFlag(boolean flag) {
		check(() -> GL11.glEdgeFlag(flag));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glEdgeFlagv(ByteBuffer flag) {
		check(() -> GL11.glEdgeFlagv(flag));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glEdgeFlagPointer(int stride, ByteBuffer pointer) {
		check(() -> GL11.glEdgeFlagPointer(stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glEdgeFlagPointer(int stride, long pointer) {
		check(() -> GL11.glEdgeFlagPointer(stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glEnableClientState(int cap) {
		check(() -> GL11.glEnableClientState(cap));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glEnd() {
		check(() -> GL11.glEnd());
	}

	@DocumentationProxyTo(GL11.class)
	public static void glEvalCoord1f(float u) {
		check(() -> GL11.glEvalCoord1f(u));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glEvalCoord1fv(FloatBuffer u) {
		check(() -> GL11.glEvalCoord1fv(u));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glEvalCoord1d(double u) {
		check(() -> GL11.glEvalCoord1d(u));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glEvalCoord1dv(DoubleBuffer u) {
		check(() -> GL11.glEvalCoord1dv(u));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glEvalCoord2f(float u, float v) {
		check(() -> GL11.glEvalCoord2f(u, v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glEvalCoord2fv(FloatBuffer u) {
		check(() -> GL11.glEvalCoord2fv(u));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glEvalCoord2d(double u, double v) {
		check(() -> GL11.glEvalCoord2d(u, v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glEvalCoord2dv(DoubleBuffer u) {
		check(() -> GL11.glEvalCoord2dv(u));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glEvalMesh1(int mode, int i1, int i2) {
		check(() -> GL11.glEvalMesh1(mode, i1, i2));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glEvalMesh2(int mode, int i1, int i2, int j1, int j2) {
		check(() -> GL11.glEvalMesh2(mode, i1, i2, j1, j2));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glEvalPoint1(int i) {
		check(() -> GL11.glEvalPoint1(i));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glEvalPoint2(int i, int j) {
		check(() -> GL11.glEvalPoint2(i, j));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glFeedbackBuffer(int type, FloatBuffer buffer) {
		check(() -> GL11.glFeedbackBuffer(type, buffer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glFinish() {
		check(() -> GL11.glFinish());
	}

	@DocumentationProxyTo(GL11.class)
	public static void glFlush() {
		check(() -> GL11.glFlush());
	}

	@DocumentationProxyTo(GL11.class)
	public static void glFogi(int pname, int param) {
		check(() -> GL11.glFogi(pname, param));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glFogiv(int pname, IntBuffer params) {
		check(() -> GL11.glFogiv(pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glFogf(int pname, float param) {
		check(() -> GL11.glFogf(pname, param));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glFogfv(int pname, FloatBuffer params) {
		check(() -> GL11.glFogfv(pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glFrontFace(int dir) {
		check(() -> GL11.glFrontFace(dir));
	}

	@NativeType("GLuint")
	@DocumentationProxyTo(GL11.class)
	public static int glGenLists(int s) {
		return check2(() -> GL11.glGenLists(s));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGenTextures(IntBuffer textures) {
		check(() -> GL11.glGenTextures(textures));
	}

	@NativeType("void")
	@DocumentationProxyTo(GL11.class)
	public static int glGenTextures() {
		return check2(() -> GL11.glGenTextures());
	}

	@DocumentationProxyTo(GL11.class)
	public static void glDeleteTextures(IntBuffer textures) {
		check(() -> GL11.glDeleteTextures(textures));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glDeleteTextures(int texture) {
		check(() -> GL11.glDeleteTextures(texture));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetClipPlane(int plane, DoubleBuffer equation) {
		check(() -> GL11.glGetClipPlane(plane, equation));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetBooleanv(int pname, ByteBuffer params) {
		check(() -> GL11.glGetBooleanv(pname, params));
	}

	@NativeType("void")
	@DocumentationProxyTo(GL11.class)
	public static boolean glGetBoolean(int pname) {
		return check2(() -> GL11.glGetBoolean(pname));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetFloatv(int pname, FloatBuffer params) {
		check(() -> GL11.glGetFloatv(pname, params));
	}

	@NativeType("void")
	@DocumentationProxyTo(GL11.class)
	public static float glGetFloat(int pname) {
		return check2(() -> GL11.glGetFloat(pname));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetIntegerv(int pname, IntBuffer params) {
		check(() -> GL11.glGetIntegerv(pname, params));
	}

	@NativeType("void")
	@DocumentationProxyTo(GL11.class)
	public static int glGetInteger(int pname) {
		return check2(() -> GL11.glGetInteger(pname));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetDoublev(int pname, DoubleBuffer params) {
		check(() -> GL11.glGetDoublev(pname, params));
	}

	@NativeType("void")
	@DocumentationProxyTo(GL11.class)
	public static double glGetDouble(int pname) {
		return check2(() -> GL11.glGetDouble(pname));
	}

	@NativeType("GLenum")
	@DocumentationProxyTo(GL11.class)
	public static int glGetError() {
		return check2(() -> GL11.glGetError());
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetLightiv(int light, int pname, IntBuffer data) {
		check(() -> GL11.glGetLightiv(light, pname, data));
	}

	@NativeType("void")
	@DocumentationProxyTo(GL11.class)
	public static int glGetLighti(int light, int pname) {
		return check2(() -> GL11.glGetLighti(light, pname));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetLightfv(int light, int pname, FloatBuffer data) {
		check(() -> GL11.glGetLightfv(light, pname, data));
	}

	@NativeType("void")
	@DocumentationProxyTo(GL11.class)
	public static float glGetLightf(int light, int pname) {
		return check2(() -> GL11.glGetLightf(light, pname));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetMapiv(int target, int query, IntBuffer data) {
		check(() -> GL11.glGetMapiv(target, query, data));
	}

	@NativeType("void")
	@DocumentationProxyTo(GL11.class)
	public static int glGetMapi(int target, int query) {
		return check2(() -> GL11.glGetMapi(target, query));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetMapfv(int target, int query, FloatBuffer data) {
		check(() -> GL11.glGetMapfv(target, query, data));
	}

	@NativeType("void")
	@DocumentationProxyTo(GL11.class)
	public static float glGetMapf(int target, int query) {
		return check2(() -> GL11.glGetMapf(target, query));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetMapdv(int target, int query, DoubleBuffer data) {
		check(() -> GL11.glGetMapdv(target, query, data));
	}

	@NativeType("void")
	@DocumentationProxyTo(GL11.class)
	public static double glGetMapd(int target, int query) {
		return check2(() -> GL11.glGetMapd(target, query));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetMaterialiv(int face, int pname, IntBuffer data) {
		check(() -> GL11.glGetMaterialiv(face, pname, data));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetMaterialfv(int face, int pname, FloatBuffer data) {
		check(() -> GL11.glGetMaterialfv(face, pname, data));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetPixelMapfv(int map, FloatBuffer data) {
		check(() -> GL11.glGetPixelMapfv(map, data));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetPixelMapfv(int map, long data) {
		check(() -> GL11.glGetPixelMapfv(map, data));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetPixelMapusv(int map, ShortBuffer data) {
		check(() -> GL11.glGetPixelMapusv(map, data));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetPixelMapusv(int map, long data) {
		check(() -> GL11.glGetPixelMapusv(map, data));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetPixelMapuiv(int map, IntBuffer data) {
		check(() -> GL11.glGetPixelMapuiv(map, data));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetPixelMapuiv(int map, long data) {
		check(() -> GL11.glGetPixelMapuiv(map, data));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetPointerv(int pname, PointerBuffer params) {
		check(() -> GL11.glGetPointerv(pname, params));
	}

	@NativeType("void")
	@DocumentationProxyTo(GL11.class)
	public static long glGetPointer(int pname) {
		return check2(() -> GL11.glGetPointer(pname));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetPolygonStipple(ByteBuffer pattern) {
		check(() -> GL11.glGetPolygonStipple(pattern));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetPolygonStipple(long pattern) {
		check(() -> GL11.glGetPolygonStipple(pattern));
	}

	@NativeType("const GLubyte *")
	@DocumentationProxyTo(GL11.class)
	public static String glGetString(int name) {
		return check2(() -> GL11.glGetString(name));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetTexEnviv(int env, int pname, IntBuffer data) {
		check(() -> GL11.glGetTexEnviv(env, pname, data));
	}

	@NativeType("void")
	@DocumentationProxyTo(GL11.class)
	public static int glGetTexEnvi(int env, int pname) {
		return check2(() -> GL11.glGetTexEnvi(env, pname));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetTexEnvfv(int env, int pname, FloatBuffer data) {
		check(() -> GL11.glGetTexEnvfv(env, pname, data));
	}

	@NativeType("void")
	@DocumentationProxyTo(GL11.class)
	public static float glGetTexEnvf(int env, int pname) {
		return check2(() -> GL11.glGetTexEnvf(env, pname));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetTexGeniv(int coord, int pname, IntBuffer data) {
		check(() -> GL11.glGetTexGeniv(coord, pname, data));
	}

	@NativeType("void")
	@DocumentationProxyTo(GL11.class)
	public static int glGetTexGeni(int coord, int pname) {
		return check2(() -> GL11.glGetTexGeni(coord, pname));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetTexGenfv(int coord, int pname, FloatBuffer data) {
		check(() -> GL11.glGetTexGenfv(coord, pname, data));
	}

	@NativeType("void")
	@DocumentationProxyTo(GL11.class)
	public static float glGetTexGenf(int coord, int pname) {
		return check2(() -> GL11.glGetTexGenf(coord, pname));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetTexGendv(int coord, int pname, DoubleBuffer data) {
		check(() -> GL11.glGetTexGendv(coord, pname, data));
	}

	@NativeType("void")
	@DocumentationProxyTo(GL11.class)
	public static double glGetTexGend(int coord, int pname) {
		return check2(() -> GL11.glGetTexGend(coord, pname));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetTexImage(int tex, int level, int format, int type, ByteBuffer pixels) {
		check(() -> GL11.glGetTexImage(tex, level, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetTexImage(int tex, int level, int format, int type, long pixels) {
		check(() -> GL11.glGetTexImage(tex, level, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetTexImage(int tex, int level, int format, int type, ShortBuffer pixels) {
		check(() -> GL11.glGetTexImage(tex, level, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetTexImage(int tex, int level, int format, int type, IntBuffer pixels) {
		check(() -> GL11.glGetTexImage(tex, level, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetTexImage(int tex, int level, int format, int type, FloatBuffer pixels) {
		check(() -> GL11.glGetTexImage(tex, level, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetTexImage(int tex, int level, int format, int type, DoubleBuffer pixels) {
		check(() -> GL11.glGetTexImage(tex, level, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetTexLevelParameteriv(int target, int level, int pname, IntBuffer params) {
		check(() -> GL11.glGetTexLevelParameteriv(target, level, pname, params));
	}

	@NativeType("void")
	@DocumentationProxyTo(GL11.class)
	public static int glGetTexLevelParameteri(int target, int level, int pname) {
		return check2(() -> GL11.glGetTexLevelParameteri(target, level, pname));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetTexLevelParameterfv(int target, int level, int pname, FloatBuffer params) {
		check(() -> GL11.glGetTexLevelParameterfv(target, level, pname, params));
	}

	@NativeType("void")
	@DocumentationProxyTo(GL11.class)
	public static float glGetTexLevelParameterf(int target, int level, int pname) {
		return check2(() -> GL11.glGetTexLevelParameterf(target, level, pname));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetTexParameteriv(int target, int pname, IntBuffer params) {
		check(() -> GL11.glGetTexParameteriv(target, pname, params));
	}

	@NativeType("void")
	@DocumentationProxyTo(GL11.class)
	public static int glGetTexParameteri(int target, int pname) {
		return check2(() -> GL11.glGetTexParameteri(target, pname));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetTexParameterfv(int target, int pname, FloatBuffer params) {
		check(() -> GL11.glGetTexParameterfv(target, pname, params));
	}

	@NativeType("void")
	@DocumentationProxyTo(GL11.class)
	public static float glGetTexParameterf(int target, int pname) {
		return check2(() -> GL11.glGetTexParameterf(target, pname));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glHint(int target, int hint) {
		check(() -> GL11.glHint(target, hint));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glIndexi(int index) {
		check(() -> GL11.glIndexi(index));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glIndexub(byte index) {
		check(() -> GL11.glIndexub(index));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glIndexs(short index) {
		check(() -> GL11.glIndexs(index));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glIndexf(float index) {
		check(() -> GL11.glIndexf(index));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glIndexd(double index) {
		check(() -> GL11.glIndexd(index));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glIndexiv(IntBuffer index) {
		check(() -> GL11.glIndexiv(index));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glIndexubv(ByteBuffer index) {
		check(() -> GL11.glIndexubv(index));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glIndexsv(ShortBuffer index) {
		check(() -> GL11.glIndexsv(index));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glIndexfv(FloatBuffer index) {
		check(() -> GL11.glIndexfv(index));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glIndexdv(DoubleBuffer index) {
		check(() -> GL11.glIndexdv(index));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glIndexMask(int mask) {
		check(() -> GL11.glIndexMask(mask));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glIndexPointer(int type, int stride, ByteBuffer pointer) {
		check(() -> GL11.glIndexPointer(type, stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glIndexPointer(int type, int stride, long pointer) {
		check(() -> GL11.glIndexPointer(type, stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glIndexPointer(int stride, ByteBuffer pointer) {
		check(() -> GL11.glIndexPointer(stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glIndexPointer(int stride, ShortBuffer pointer) {
		check(() -> GL11.glIndexPointer(stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glIndexPointer(int stride, IntBuffer pointer) {
		check(() -> GL11.glIndexPointer(stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glInitNames() {
		check(() -> GL11.glInitNames());
	}

	@DocumentationProxyTo(GL11.class)
	public static void glInterleavedArrays(int format, int stride, ByteBuffer pointer) {
		check(() -> GL11.glInterleavedArrays(format, stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glInterleavedArrays(int format, int stride, long pointer) {
		check(() -> GL11.glInterleavedArrays(format, stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glInterleavedArrays(int format, int stride, ShortBuffer pointer) {
		check(() -> GL11.glInterleavedArrays(format, stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glInterleavedArrays(int format, int stride, IntBuffer pointer) {
		check(() -> GL11.glInterleavedArrays(format, stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glInterleavedArrays(int format, int stride, FloatBuffer pointer) {
		check(() -> GL11.glInterleavedArrays(format, stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glInterleavedArrays(int format, int stride, DoubleBuffer pointer) {
		check(() -> GL11.glInterleavedArrays(format, stride, pointer));
	}

	@NativeType("GLboolean")
	@DocumentationProxyTo(GL11.class)
	public static boolean glIsEnabled(int cap) {
		return check2(() -> GL11.glIsEnabled(cap));
	}

	@NativeType("GLboolean")
	@DocumentationProxyTo(GL11.class)
	public static boolean glIsList(int list) {
		return check2(() -> GL11.glIsList(list));
	}

	@NativeType("GLboolean")
	@DocumentationProxyTo(GL11.class)
	public static boolean glIsTexture(int texture) {
		return check2(() -> GL11.glIsTexture(texture));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glLightModeli(int pname, int param) {
		check(() -> GL11.glLightModeli(pname, param));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glLightModelf(int pname, float param) {
		check(() -> GL11.glLightModelf(pname, param));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glLightModeliv(int pname, IntBuffer params) {
		check(() -> GL11.glLightModeliv(pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glLightModelfv(int pname, FloatBuffer params) {
		check(() -> GL11.glLightModelfv(pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glLighti(int light, int pname, int param) {
		check(() -> GL11.glLighti(light, pname, param));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glLightf(int light, int pname, float param) {
		check(() -> GL11.glLightf(light, pname, param));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glLightiv(int light, int pname, IntBuffer params) {
		check(() -> GL11.glLightiv(light, pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glLightfv(int light, int pname, FloatBuffer params) {
		check(() -> GL11.glLightfv(light, pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glLineStipple(int factor, short pattern) {
		check(() -> GL11.glLineStipple(factor, pattern));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glLineWidth(float width) {
		check(() -> GL11.glLineWidth(width));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glListBase(int base) {
		check(() -> GL11.glListBase(base));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glLoadMatrixf(FloatBuffer m) {
		check(() -> GL11.glLoadMatrixf(m));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glLoadMatrixd(DoubleBuffer m) {
		check(() -> GL11.glLoadMatrixd(m));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glLoadIdentity() {
		check(() -> GL11.glLoadIdentity());
	}

	@DocumentationProxyTo(GL11.class)
	public static void glLoadName(int name) {
		check(() -> GL11.glLoadName(name));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glLogicOp(int op) {
		check(() -> GL11.glLogicOp(op));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glMap1f(int target, float u1, float u2, int stride, int order, FloatBuffer points) {
		check(() -> GL11.glMap1f(target, u1, u2, stride, order, points));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glMap1d(int target, double u1, double u2, int stride, int order, DoubleBuffer points) {
		check(() -> GL11.glMap1d(target, u1, u2, stride, order, points));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glMap2f(int target, float u1, float u2, int ustride, int uorder, float v1, float v2, int vstride, int vorder, FloatBuffer points) {
		check(() -> GL11.glMap2f(target, u1, u2, ustride, uorder, v1, v2, vstride, vorder, points));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glMap2d(int target, double u1, double u2, int ustride, int uorder, double v1, double v2, int vstride, int vorder, DoubleBuffer points) {
		check(() -> GL11.glMap2d(target, u1, u2, ustride, uorder, v1, v2, vstride, vorder, points));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glMapGrid1f(int n, float u1, float u2) {
		check(() -> GL11.glMapGrid1f(n, u1, u2));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glMapGrid1d(int n, double u1, double u2) {
		check(() -> GL11.glMapGrid1d(n, u1, u2));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glMapGrid2f(int un, float u1, float u2, int vn, float v1, float v2) {
		check(() -> GL11.glMapGrid2f(un, u1, u2, vn, v1, v2));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glMapGrid2d(int un, double u1, double u2, int vn, double v1, double v2) {
		check(() -> GL11.glMapGrid2d(un, u1, u2, vn, v1, v2));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glMateriali(int face, int pname, int param) {
		check(() -> GL11.glMateriali(face, pname, param));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glMaterialf(int face, int pname, float param) {
		check(() -> GL11.glMaterialf(face, pname, param));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glMaterialiv(int face, int pname, IntBuffer params) {
		check(() -> GL11.glMaterialiv(face, pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glMaterialfv(int face, int pname, FloatBuffer params) {
		check(() -> GL11.glMaterialfv(face, pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glMatrixMode(int mode) {
		check(() -> GL11.glMatrixMode(mode));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glMultMatrixf(FloatBuffer m) {
		check(() -> GL11.glMultMatrixf(m));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glMultMatrixd(DoubleBuffer m) {
		check(() -> GL11.glMultMatrixd(m));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glFrustum(double l, double r, double b, double t, double n, double f) {
		check(() -> GL11.glFrustum(l, r, b, t, n, f));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glNewList(int n, int mode) {
		check(() -> GL11.glNewList(n, mode));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glEndList() {
		check(() -> GL11.glEndList());
	}

	@DocumentationProxyTo(GL11.class)
	public static void glNormal3f(float nx, float ny, float nz) {
		check(() -> GL11.glNormal3f(nx, ny, nz));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glNormal3b(byte nx, byte ny, byte nz) {
		check(() -> GL11.glNormal3b(nx, ny, nz));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glNormal3s(short nx, short ny, short nz) {
		check(() -> GL11.glNormal3s(nx, ny, nz));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glNormal3i(int nx, int ny, int nz) {
		check(() -> GL11.glNormal3i(nx, ny, nz));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glNormal3d(double nx, double ny, double nz) {
		check(() -> GL11.glNormal3d(nx, ny, nz));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glNormal3fv(FloatBuffer v) {
		check(() -> GL11.glNormal3fv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glNormal3bv(ByteBuffer v) {
		check(() -> GL11.glNormal3bv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glNormal3sv(ShortBuffer v) {
		check(() -> GL11.glNormal3sv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glNormal3iv(IntBuffer v) {
		check(() -> GL11.glNormal3iv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glNormal3dv(DoubleBuffer v) {
		check(() -> GL11.glNormal3dv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glNormalPointer(int type, int stride, ByteBuffer pointer) {
		check(() -> GL11.glNormalPointer(type, stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glNormalPointer(int type, int stride, long pointer) {
		check(() -> GL11.glNormalPointer(type, stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glNormalPointer(int type, int stride, ShortBuffer pointer) {
		check(() -> GL11.glNormalPointer(type, stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glNormalPointer(int type, int stride, IntBuffer pointer) {
		check(() -> GL11.glNormalPointer(type, stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glNormalPointer(int type, int stride, FloatBuffer pointer) {
		check(() -> GL11.glNormalPointer(type, stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glOrtho(double l, double r, double b, double t, double n, double f) {
		check(() -> GL11.glOrtho(l, r, b, t, n, f));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glPassThrough(float token) {
		check(() -> GL11.glPassThrough(token));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glPixelMapfv(int map, int size, long values) {
		check(() -> GL11.glPixelMapfv(map, size, values));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glPixelMapfv(int map, FloatBuffer values) {
		check(() -> GL11.glPixelMapfv(map, values));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glPixelMapusv(int map, int size, long values) {
		check(() -> GL11.glPixelMapusv(map, size, values));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glPixelMapusv(int map, ShortBuffer values) {
		check(() -> GL11.glPixelMapusv(map, values));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glPixelMapuiv(int map, int size, long values) {
		check(() -> GL11.glPixelMapuiv(map, size, values));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glPixelMapuiv(int map, IntBuffer values) {
		check(() -> GL11.glPixelMapuiv(map, values));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glPixelStorei(int pname, int param) {
		check(() -> GL11.glPixelStorei(pname, param));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glPixelStoref(int pname, float param) {
		check(() -> GL11.glPixelStoref(pname, param));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glPixelTransferi(int pname, int param) {
		check(() -> GL11.glPixelTransferi(pname, param));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glPixelTransferf(int pname, float param) {
		check(() -> GL11.glPixelTransferf(pname, param));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glPixelZoom(float xfactor, float yfactor) {
		check(() -> GL11.glPixelZoom(xfactor, yfactor));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glPointSize(float size) {
		check(() -> GL11.glPointSize(size));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glPolygonMode(int face, int mode) {
		check(() -> GL11.glPolygonMode(face, mode));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glPolygonOffset(float factor, float units) {
		check(() -> GL11.glPolygonOffset(factor, units));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glPolygonStipple(ByteBuffer pattern) {
		check(() -> GL11.glPolygonStipple(pattern));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glPolygonStipple(long pattern) {
		check(() -> GL11.glPolygonStipple(pattern));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glPushAttrib(int mask) {
		check(() -> GL11.glPushAttrib(mask));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glPushClientAttrib(int mask) {
		check(() -> GL11.glPushClientAttrib(mask));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glPopAttrib() {
		check(() -> GL11.glPopAttrib());
	}

	@DocumentationProxyTo(GL11.class)
	public static void glPopClientAttrib() {
		check(() -> GL11.glPopClientAttrib());
	}

	@DocumentationProxyTo(GL11.class)
	public static void glPopMatrix() {
		check(() -> GL11.glPopMatrix());
	}

	@DocumentationProxyTo(GL11.class)
	public static void glPopName() {
		check(() -> GL11.glPopName());
	}

	@DocumentationProxyTo(GL11.class)
	public static void glPrioritizeTextures(IntBuffer textures, FloatBuffer priorities) {
		check(() -> GL11.glPrioritizeTextures(textures, priorities));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glPushMatrix() {
		check(() -> GL11.glPushMatrix());
	}

	@DocumentationProxyTo(GL11.class)
	public static void glPushName(int name) {
		check(() -> GL11.glPushName(name));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos2i(int x, int y) {
		check(() -> GL11.glRasterPos2i(x, y));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos2s(short x, short y) {
		check(() -> GL11.glRasterPos2s(x, y));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos2f(float x, float y) {
		check(() -> GL11.glRasterPos2f(x, y));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos2d(double x, double y) {
		check(() -> GL11.glRasterPos2d(x, y));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos2iv(IntBuffer coords) {
		check(() -> GL11.glRasterPos2iv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos2sv(ShortBuffer coords) {
		check(() -> GL11.glRasterPos2sv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos2fv(FloatBuffer coords) {
		check(() -> GL11.glRasterPos2fv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos2dv(DoubleBuffer coords) {
		check(() -> GL11.glRasterPos2dv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos3i(int x, int y, int z) {
		check(() -> GL11.glRasterPos3i(x, y, z));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos3s(short x, short y, short z) {
		check(() -> GL11.glRasterPos3s(x, y, z));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos3f(float x, float y, float z) {
		check(() -> GL11.glRasterPos3f(x, y, z));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos3d(double x, double y, double z) {
		check(() -> GL11.glRasterPos3d(x, y, z));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos3iv(IntBuffer coords) {
		check(() -> GL11.glRasterPos3iv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos3sv(ShortBuffer coords) {
		check(() -> GL11.glRasterPos3sv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos3fv(FloatBuffer coords) {
		check(() -> GL11.glRasterPos3fv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos3dv(DoubleBuffer coords) {
		check(() -> GL11.glRasterPos3dv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos4i(int x, int y, int z, int w) {
		check(() -> GL11.glRasterPos4i(x, y, z, w));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos4s(short x, short y, short z, short w) {
		check(() -> GL11.glRasterPos4s(x, y, z, w));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos4f(float x, float y, float z, float w) {
		check(() -> GL11.glRasterPos4f(x, y, z, w));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos4d(double x, double y, double z, double w) {
		check(() -> GL11.glRasterPos4d(x, y, z, w));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos4iv(IntBuffer coords) {
		check(() -> GL11.glRasterPos4iv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos4sv(ShortBuffer coords) {
		check(() -> GL11.glRasterPos4sv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos4fv(FloatBuffer coords) {
		check(() -> GL11.glRasterPos4fv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos4dv(DoubleBuffer coords) {
		check(() -> GL11.glRasterPos4dv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glReadBuffer(int src) {
		check(() -> GL11.glReadBuffer(src));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glReadPixels(int x, int y, int width, int height, int format, int type, ByteBuffer pixels) {
		check(() -> GL11.glReadPixels(x, y, width, height, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glReadPixels(int x, int y, int width, int height, int format, int type, long pixels) {
		check(() -> GL11.glReadPixels(x, y, width, height, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glReadPixels(int x, int y, int width, int height, int format, int type, ShortBuffer pixels) {
		check(() -> GL11.glReadPixels(x, y, width, height, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glReadPixels(int x, int y, int width, int height, int format, int type, IntBuffer pixels) {
		check(() -> GL11.glReadPixels(x, y, width, height, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glReadPixels(int x, int y, int width, int height, int format, int type, FloatBuffer pixels) {
		check(() -> GL11.glReadPixels(x, y, width, height, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRecti(int x1, int y1, int x2, int y2) {
		check(() -> GL11.glRecti(x1, y1, x2, y2));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRects(short x1, short y1, short x2, short y2) {
		check(() -> GL11.glRects(x1, y1, x2, y2));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRectf(float x1, float y1, float x2, float y2) {
		check(() -> GL11.glRectf(x1, y1, x2, y2));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRectd(double x1, double y1, double x2, double y2) {
		check(() -> GL11.glRectd(x1, y1, x2, y2));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRectiv(IntBuffer v1, IntBuffer v2) {
		check(() -> GL11.glRectiv(v1, v2));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRectsv(ShortBuffer v1, ShortBuffer v2) {
		check(() -> GL11.glRectsv(v1, v2));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRectfv(FloatBuffer v1, FloatBuffer v2) {
		check(() -> GL11.glRectfv(v1, v2));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRectdv(DoubleBuffer v1, DoubleBuffer v2) {
		check(() -> GL11.glRectdv(v1, v2));
	}

	@NativeType("GLint")
	@DocumentationProxyTo(GL11.class)
	public static int glRenderMode(int mode) {
		return check2(() -> GL11.glRenderMode(mode));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRotatef(float angle, float x, float y, float z) {
		check(() -> GL11.glRotatef(angle, x, y, z));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRotated(double angle, double x, double y, double z) {
		check(() -> GL11.glRotated(angle, x, y, z));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glScalef(float x, float y, float z) {
		check(() -> GL11.glScalef(x, y, z));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glScaled(double x, double y, double z) {
		check(() -> GL11.glScaled(x, y, z));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glScissor(int x, int y, int width, int height) {
		check(() -> GL11.glScissor(x, y, width, height));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glSelectBuffer(IntBuffer buffer) {
		check(() -> GL11.glSelectBuffer(buffer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glShadeModel(int mode) {
		check(() -> GL11.glShadeModel(mode));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glStencilFunc(int func, int ref, int mask) {
		check(() -> GL11.glStencilFunc(func, ref, mask));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glStencilMask(int mask) {
		check(() -> GL11.glStencilMask(mask));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glStencilOp(int sfail, int dpfail, int dppass) {
		check(() -> GL11.glStencilOp(sfail, dpfail, dppass));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord1f(float s) {
		check(() -> GL11.glTexCoord1f(s));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord1s(short s) {
		check(() -> GL11.glTexCoord1s(s));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord1i(int s) {
		check(() -> GL11.glTexCoord1i(s));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord1d(double s) {
		check(() -> GL11.glTexCoord1d(s));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord1fv(FloatBuffer v) {
		check(() -> GL11.glTexCoord1fv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord1sv(ShortBuffer v) {
		check(() -> GL11.glTexCoord1sv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord1iv(IntBuffer v) {
		check(() -> GL11.glTexCoord1iv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord1dv(DoubleBuffer v) {
		check(() -> GL11.glTexCoord1dv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord2f(float s, float t) {
		check(() -> GL11.glTexCoord2f(s, t));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord2s(short s, short t) {
		check(() -> GL11.glTexCoord2s(s, t));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord2i(int s, int t) {
		check(() -> GL11.glTexCoord2i(s, t));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord2d(double s, double t) {
		check(() -> GL11.glTexCoord2d(s, t));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord2fv(FloatBuffer v) {
		check(() -> GL11.glTexCoord2fv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord2sv(ShortBuffer v) {
		check(() -> GL11.glTexCoord2sv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord2iv(IntBuffer v) {
		check(() -> GL11.glTexCoord2iv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord2dv(DoubleBuffer v) {
		check(() -> GL11.glTexCoord2dv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord3f(float s, float t, float r) {
		check(() -> GL11.glTexCoord3f(s, t, r));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord3s(short s, short t, short r) {
		check(() -> GL11.glTexCoord3s(s, t, r));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord3i(int s, int t, int r) {
		check(() -> GL11.glTexCoord3i(s, t, r));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord3d(double s, double t, double r) {
		check(() -> GL11.glTexCoord3d(s, t, r));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord3fv(FloatBuffer v) {
		check(() -> GL11.glTexCoord3fv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord3sv(ShortBuffer v) {
		check(() -> GL11.glTexCoord3sv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord3iv(IntBuffer v) {
		check(() -> GL11.glTexCoord3iv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord3dv(DoubleBuffer v) {
		check(() -> GL11.glTexCoord3dv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord4f(float s, float t, float r, float q) {
		check(() -> GL11.glTexCoord4f(s, t, r, q));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord4s(short s, short t, short r, short q) {
		check(() -> GL11.glTexCoord4s(s, t, r, q));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord4i(int s, int t, int r, int q) {
		check(() -> GL11.glTexCoord4i(s, t, r, q));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord4d(double s, double t, double r, double q) {
		check(() -> GL11.glTexCoord4d(s, t, r, q));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord4fv(FloatBuffer v) {
		check(() -> GL11.glTexCoord4fv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord4sv(ShortBuffer v) {
		check(() -> GL11.glTexCoord4sv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord4iv(IntBuffer v) {
		check(() -> GL11.glTexCoord4iv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord4dv(DoubleBuffer v) {
		check(() -> GL11.glTexCoord4dv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoordPointer(int size, int type, int stride, ByteBuffer pointer) {
		check(() -> GL11.glTexCoordPointer(size, type, stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoordPointer(int size, int type, int stride, long pointer) {
		check(() -> GL11.glTexCoordPointer(size, type, stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoordPointer(int size, int type, int stride, ShortBuffer pointer) {
		check(() -> GL11.glTexCoordPointer(size, type, stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoordPointer(int size, int type, int stride, IntBuffer pointer) {
		check(() -> GL11.glTexCoordPointer(size, type, stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoordPointer(int size, int type, int stride, FloatBuffer pointer) {
		check(() -> GL11.glTexCoordPointer(size, type, stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexEnvi(int target, int pname, int param) {
		check(() -> GL11.glTexEnvi(target, pname, param));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexEnviv(int target, int pname, IntBuffer params) {
		check(() -> GL11.glTexEnviv(target, pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexEnvf(int target, int pname, float param) {
		check(() -> GL11.glTexEnvf(target, pname, param));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexEnvfv(int target, int pname, FloatBuffer params) {
		check(() -> GL11.glTexEnvfv(target, pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexGeni(int coord, int pname, int param) {
		check(() -> GL11.glTexGeni(coord, pname, param));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexGeniv(int coord, int pname, IntBuffer params) {
		check(() -> GL11.glTexGeniv(coord, pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexGenf(int coord, int pname, float param) {
		check(() -> GL11.glTexGenf(coord, pname, param));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexGenfv(int coord, int pname, FloatBuffer params) {
		check(() -> GL11.glTexGenfv(coord, pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexGend(int coord, int pname, double param) {
		check(() -> GL11.glTexGend(coord, pname, param));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexGendv(int coord, int pname, DoubleBuffer params) {
		check(() -> GL11.glTexGendv(coord, pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, ByteBuffer pixels) {
		check(() -> GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, long pixels) {
		check(() -> GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, ShortBuffer pixels) {
		check(() -> GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, IntBuffer pixels) {
		check(() -> GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, FloatBuffer pixels) {
		check(() -> GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, DoubleBuffer pixels) {
		check(() -> GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format, int type, ByteBuffer pixels) {
		check(() -> GL11.glTexImage1D(target, level, internalformat, width, border, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format, int type, long pixels) {
		check(() -> GL11.glTexImage1D(target, level, internalformat, width, border, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format, int type, ShortBuffer pixels) {
		check(() -> GL11.glTexImage1D(target, level, internalformat, width, border, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format, int type, IntBuffer pixels) {
		check(() -> GL11.glTexImage1D(target, level, internalformat, width, border, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format, int type, FloatBuffer pixels) {
		check(() -> GL11.glTexImage1D(target, level, internalformat, width, border, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format, int type, DoubleBuffer pixels) {
		check(() -> GL11.glTexImage1D(target, level, internalformat, width, border, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glCopyTexImage2D(int target, int level, int internalFormat, int x, int y, int width, int height, int border) {
		check(() -> GL11.glCopyTexImage2D(target, level, internalFormat, x, y, width, height, border));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glCopyTexImage1D(int target, int level, int internalFormat, int x, int y, int width, int border) {
		check(() -> GL11.glCopyTexImage1D(target, level, internalFormat, x, y, width, border));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glCopyTexSubImage1D(int target, int level, int xoffset, int x, int y, int width) {
		check(() -> GL11.glCopyTexSubImage1D(target, level, xoffset, x, y, width));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glCopyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y, int width, int height) {
		check(() -> GL11.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexParameteri(int target, int pname, int param) {
		check(() -> GL11.glTexParameteri(target, pname, param));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexParameteriv(int target, int pname, IntBuffer params) {
		check(() -> GL11.glTexParameteriv(target, pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexParameterf(int target, int pname, float param) {
		check(() -> GL11.glTexParameterf(target, pname, param));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexParameterfv(int target, int pname, FloatBuffer params) {
		check(() -> GL11.glTexParameterfv(target, pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexSubImage1D(int target, int level, int xoffset, int width, int format, int type, ByteBuffer pixels) {
		check(() -> GL11.glTexSubImage1D(target, level, xoffset, width, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexSubImage1D(int target, int level, int xoffset, int width, int format, int type, long pixels) {
		check(() -> GL11.glTexSubImage1D(target, level, xoffset, width, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexSubImage1D(int target, int level, int xoffset, int width, int format, int type, ShortBuffer pixels) {
		check(() -> GL11.glTexSubImage1D(target, level, xoffset, width, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexSubImage1D(int target, int level, int xoffset, int width, int format, int type, IntBuffer pixels) {
		check(() -> GL11.glTexSubImage1D(target, level, xoffset, width, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexSubImage1D(int target, int level, int xoffset, int width, int format, int type, FloatBuffer pixels) {
		check(() -> GL11.glTexSubImage1D(target, level, xoffset, width, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexSubImage1D(int target, int level, int xoffset, int width, int format, int type, DoubleBuffer pixels) {
		check(() -> GL11.glTexSubImage1D(target, level, xoffset, width, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, ByteBuffer pixels) {
		check(() -> GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, long pixels) {
		check(() -> GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, ShortBuffer pixels) {
		check(() -> GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, IntBuffer pixels) {
		check(() -> GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, FloatBuffer pixels) {
		check(() -> GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, DoubleBuffer pixels) {
		check(() -> GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTranslatef(float x, float y, float z) {
		check(() -> GL11.glTranslatef(x, y, z));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTranslated(double x, double y, double z) {
		check(() -> GL11.glTranslated(x, y, z));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex2f(float x, float y) {
		check(() -> GL11.glVertex2f(x, y));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex2s(short x, short y) {
		check(() -> GL11.glVertex2s(x, y));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex2i(int x, int y) {
		check(() -> GL11.glVertex2i(x, y));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex2d(double x, double y) {
		check(() -> GL11.glVertex2d(x, y));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex2fv(FloatBuffer coords) {
		check(() -> GL11.glVertex2fv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex2sv(ShortBuffer coords) {
		check(() -> GL11.glVertex2sv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex2iv(IntBuffer coords) {
		check(() -> GL11.glVertex2iv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex2dv(DoubleBuffer coords) {
		check(() -> GL11.glVertex2dv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex3f(float x, float y, float z) {
		check(() -> GL11.glVertex3f(x, y, z));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex3s(short x, short y, short z) {
		check(() -> GL11.glVertex3s(x, y, z));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex3i(int x, int y, int z) {
		check(() -> GL11.glVertex3i(x, y, z));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex3d(double x, double y, double z) {
		check(() -> GL11.glVertex3d(x, y, z));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex3fv(FloatBuffer coords) {
		check(() -> GL11.glVertex3fv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex3sv(ShortBuffer coords) {
		check(() -> GL11.glVertex3sv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex3iv(IntBuffer coords) {
		check(() -> GL11.glVertex3iv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex3dv(DoubleBuffer coords) {
		check(() -> GL11.glVertex3dv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex4f(float x, float y, float z, float w) {
		check(() -> GL11.glVertex4f(x, y, z, w));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex4s(short x, short y, short z, short w) {
		check(() -> GL11.glVertex4s(x, y, z, w));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex4i(int x, int y, int z, int w) {
		check(() -> GL11.glVertex4i(x, y, z, w));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex4d(double x, double y, double z, double w) {
		check(() -> GL11.glVertex4d(x, y, z, w));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex4fv(FloatBuffer coords) {
		check(() -> GL11.glVertex4fv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex4sv(ShortBuffer coords) {
		check(() -> GL11.glVertex4sv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex4iv(IntBuffer coords) {
		check(() -> GL11.glVertex4iv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex4dv(DoubleBuffer coords) {
		check(() -> GL11.glVertex4dv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertexPointer(int size, int type, int stride, ByteBuffer pointer) {
		check(() -> GL11.glVertexPointer(size, type, stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertexPointer(int size, int type, int stride, long pointer) {
		check(() -> GL11.glVertexPointer(size, type, stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertexPointer(int size, int type, int stride, ShortBuffer pointer) {
		check(() -> GL11.glVertexPointer(size, type, stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertexPointer(int size, int type, int stride, IntBuffer pointer) {
		check(() -> GL11.glVertexPointer(size, type, stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertexPointer(int size, int type, int stride, FloatBuffer pointer) {
		check(() -> GL11.glVertexPointer(size, type, stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glViewport(int x, int y, int w, int h) {
		check(() -> GL11.glViewport(x, y, w, h));
	}

	@NativeType("GLboolean")
	@DocumentationProxyTo(GL11.class)
	public static boolean glAreTexturesResident(int[] textures, ByteBuffer residences) {
		return check2(() -> GL11.glAreTexturesResident(textures, residences));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glClipPlane(int plane, double[] equation) {
		check(() -> GL11.glClipPlane(plane, equation));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor3sv(short[] v) {
		check(() -> GL11.glColor3sv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor3iv(int[] v) {
		check(() -> GL11.glColor3iv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor3fv(float[] v) {
		check(() -> GL11.glColor3fv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor3dv(double[] v) {
		check(() -> GL11.glColor3dv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor3usv(short[] v) {
		check(() -> GL11.glColor3usv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor3uiv(int[] v) {
		check(() -> GL11.glColor3uiv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor4sv(short[] v) {
		check(() -> GL11.glColor4sv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor4iv(int[] v) {
		check(() -> GL11.glColor4iv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor4fv(float[] v) {
		check(() -> GL11.glColor4fv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor4dv(double[] v) {
		check(() -> GL11.glColor4dv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor4usv(short[] v) {
		check(() -> GL11.glColor4usv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glColor4uiv(int[] v) {
		check(() -> GL11.glColor4uiv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glDrawPixels(int width, int height, int format, int type, short[] pixels) {
		check(() -> GL11.glDrawPixels(width, height, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glDrawPixels(int width, int height, int format, int type, int[] pixels) {
		check(() -> GL11.glDrawPixels(width, height, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glDrawPixels(int width, int height, int format, int type, float[] pixels) {
		check(() -> GL11.glDrawPixels(width, height, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glEvalCoord1fv(float[] u) {
		check(() -> GL11.glEvalCoord1fv(u));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glEvalCoord1dv(double[] u) {
		check(() -> GL11.glEvalCoord1dv(u));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glEvalCoord2fv(float[] u) {
		check(() -> GL11.glEvalCoord2fv(u));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glEvalCoord2dv(double[] u) {
		check(() -> GL11.glEvalCoord2dv(u));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glFeedbackBuffer(int type, float[] buffer) {
		check(() -> GL11.glFeedbackBuffer(type, buffer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glFogiv(int pname, int[] params) {
		check(() -> GL11.glFogiv(pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glFogfv(int pname, float[] params) {
		check(() -> GL11.glFogfv(pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGenTextures(int[] textures) {
		check(() -> GL11.glGenTextures(textures));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glDeleteTextures(int[] textures) {
		check(() -> GL11.glDeleteTextures(textures));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetClipPlane(int plane, double[] equation) {
		check(() -> GL11.glGetClipPlane(plane, equation));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetFloatv(int pname, float[] params) {
		check(() -> GL11.glGetFloatv(pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetIntegerv(int pname, int[] params) {
		check(() -> GL11.glGetIntegerv(pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetDoublev(int pname, double[] params) {
		check(() -> GL11.glGetDoublev(pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetLightiv(int light, int pname, int[] data) {
		check(() -> GL11.glGetLightiv(light, pname, data));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetLightfv(int light, int pname, float[] data) {
		check(() -> GL11.glGetLightfv(light, pname, data));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetMapiv(int target, int query, int[] data) {
		check(() -> GL11.glGetMapiv(target, query, data));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetMapfv(int target, int query, float[] data) {
		check(() -> GL11.glGetMapfv(target, query, data));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetMapdv(int target, int query, double[] data) {
		check(() -> GL11.glGetMapdv(target, query, data));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetMaterialiv(int face, int pname, int[] data) {
		check(() -> GL11.glGetMaterialiv(face, pname, data));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetMaterialfv(int face, int pname, float[] data) {
		check(() -> GL11.glGetMaterialfv(face, pname, data));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetPixelMapfv(int map, float[] data) {
		check(() -> GL11.glGetPixelMapfv(map, data));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetPixelMapusv(int map, short[] data) {
		check(() -> GL11.glGetPixelMapusv(map, data));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetPixelMapuiv(int map, int[] data) {
		check(() -> GL11.glGetPixelMapuiv(map, data));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetTexEnviv(int env, int pname, int[] data) {
		check(() -> GL11.glGetTexEnviv(env, pname, data));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetTexEnvfv(int env, int pname, float[] data) {
		check(() -> GL11.glGetTexEnvfv(env, pname, data));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetTexGeniv(int coord, int pname, int[] data) {
		check(() -> GL11.glGetTexGeniv(coord, pname, data));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetTexGenfv(int coord, int pname, float[] data) {
		check(() -> GL11.glGetTexGenfv(coord, pname, data));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetTexGendv(int coord, int pname, double[] data) {
		check(() -> GL11.glGetTexGendv(coord, pname, data));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetTexImage(int tex, int level, int format, int type, short[] pixels) {
		check(() -> GL11.glGetTexImage(tex, level, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetTexImage(int tex, int level, int format, int type, int[] pixels) {
		check(() -> GL11.glGetTexImage(tex, level, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetTexImage(int tex, int level, int format, int type, float[] pixels) {
		check(() -> GL11.glGetTexImage(tex, level, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetTexImage(int tex, int level, int format, int type, double[] pixels) {
		check(() -> GL11.glGetTexImage(tex, level, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetTexLevelParameteriv(int target, int level, int pname, int[] params) {
		check(() -> GL11.glGetTexLevelParameteriv(target, level, pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetTexLevelParameterfv(int target, int level, int pname, float[] params) {
		check(() -> GL11.glGetTexLevelParameterfv(target, level, pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetTexParameteriv(int target, int pname, int[] params) {
		check(() -> GL11.glGetTexParameteriv(target, pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glGetTexParameterfv(int target, int pname, float[] params) {
		check(() -> GL11.glGetTexParameterfv(target, pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glIndexiv(int[] index) {
		check(() -> GL11.glIndexiv(index));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glIndexsv(short[] index) {
		check(() -> GL11.glIndexsv(index));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glIndexfv(float[] index) {
		check(() -> GL11.glIndexfv(index));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glIndexdv(double[] index) {
		check(() -> GL11.glIndexdv(index));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glInterleavedArrays(int format, int stride, short[] pointer) {
		check(() -> GL11.glInterleavedArrays(format, stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glInterleavedArrays(int format, int stride, int[] pointer) {
		check(() -> GL11.glInterleavedArrays(format, stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glInterleavedArrays(int format, int stride, float[] pointer) {
		check(() -> GL11.glInterleavedArrays(format, stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glInterleavedArrays(int format, int stride, double[] pointer) {
		check(() -> GL11.glInterleavedArrays(format, stride, pointer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glLightModeliv(int pname, int[] params) {
		check(() -> GL11.glLightModeliv(pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glLightModelfv(int pname, float[] params) {
		check(() -> GL11.glLightModelfv(pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glLightiv(int light, int pname, int[] params) {
		check(() -> GL11.glLightiv(light, pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glLightfv(int light, int pname, float[] params) {
		check(() -> GL11.glLightfv(light, pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glLoadMatrixf(float[] m) {
		check(() -> GL11.glLoadMatrixf(m));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glLoadMatrixd(double[] m) {
		check(() -> GL11.glLoadMatrixd(m));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glMap1f(int target, float u1, float u2, int stride, int order, float[] points) {
		check(() -> GL11.glMap1f(target, u1, u2, stride, order, points));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glMap1d(int target, double u1, double u2, int stride, int order, double[] points) {
		check(() -> GL11.glMap1d(target, u1, u2, stride, order, points));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glMap2f(int target, float u1, float u2, int ustride, int uorder, float v1, float v2, int vstride, int vorder, float[] points) {
		check(() -> GL11.glMap2f(target, u1, u2, ustride, uorder, v1, v2, vstride, vorder, points));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glMap2d(int target, double u1, double u2, int ustride, int uorder, double v1, double v2, int vstride, int vorder, double[] points) {
		check(() -> GL11.glMap2d(target, u1, u2, ustride, uorder, v1, v2, vstride, vorder, points));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glMaterialiv(int face, int pname, int[] params) {
		check(() -> GL11.glMaterialiv(face, pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glMaterialfv(int face, int pname, float[] params) {
		check(() -> GL11.glMaterialfv(face, pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glMultMatrixf(float[] m) {
		check(() -> GL11.glMultMatrixf(m));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glMultMatrixd(double[] m) {
		check(() -> GL11.glMultMatrixd(m));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glNormal3fv(float[] v) {
		check(() -> GL11.glNormal3fv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glNormal3sv(short[] v) {
		check(() -> GL11.glNormal3sv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glNormal3iv(int[] v) {
		check(() -> GL11.glNormal3iv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glNormal3dv(double[] v) {
		check(() -> GL11.glNormal3dv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glPixelMapfv(int map, float[] values) {
		check(() -> GL11.glPixelMapfv(map, values));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glPixelMapusv(int map, short[] values) {
		check(() -> GL11.glPixelMapusv(map, values));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glPixelMapuiv(int map, int[] values) {
		check(() -> GL11.glPixelMapuiv(map, values));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glPrioritizeTextures(int[] textures, float[] priorities) {
		check(() -> GL11.glPrioritizeTextures(textures, priorities));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos2iv(int[] coords) {
		check(() -> GL11.glRasterPos2iv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos2sv(short[] coords) {
		check(() -> GL11.glRasterPos2sv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos2fv(float[] coords) {
		check(() -> GL11.glRasterPos2fv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos2dv(double[] coords) {
		check(() -> GL11.glRasterPos2dv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos3iv(int[] coords) {
		check(() -> GL11.glRasterPos3iv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos3sv(short[] coords) {
		check(() -> GL11.glRasterPos3sv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos3fv(float[] coords) {
		check(() -> GL11.glRasterPos3fv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos3dv(double[] coords) {
		check(() -> GL11.glRasterPos3dv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos4iv(int[] coords) {
		check(() -> GL11.glRasterPos4iv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos4sv(short[] coords) {
		check(() -> GL11.glRasterPos4sv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos4fv(float[] coords) {
		check(() -> GL11.glRasterPos4fv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRasterPos4dv(double[] coords) {
		check(() -> GL11.glRasterPos4dv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glReadPixels(int x, int y, int width, int height, int format, int type, short[] pixels) {
		check(() -> GL11.glReadPixels(x, y, width, height, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glReadPixels(int x, int y, int width, int height, int format, int type, int[] pixels) {
		check(() -> GL11.glReadPixels(x, y, width, height, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glReadPixels(int x, int y, int width, int height, int format, int type, float[] pixels) {
		check(() -> GL11.glReadPixels(x, y, width, height, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRectiv(int[] v1, int[] v2) {
		check(() -> GL11.glRectiv(v1, v2));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRectsv(short[] v1, short[] v2) {
		check(() -> GL11.glRectsv(v1, v2));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRectfv(float[] v1, float[] v2) {
		check(() -> GL11.glRectfv(v1, v2));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glRectdv(double[] v1, double[] v2) {
		check(() -> GL11.glRectdv(v1, v2));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glSelectBuffer(int[] buffer) {
		check(() -> GL11.glSelectBuffer(buffer));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord1fv(float[] v) {
		check(() -> GL11.glTexCoord1fv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord1sv(short[] v) {
		check(() -> GL11.glTexCoord1sv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord1iv(int[] v) {
		check(() -> GL11.glTexCoord1iv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord1dv(double[] v) {
		check(() -> GL11.glTexCoord1dv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord2fv(float[] v) {
		check(() -> GL11.glTexCoord2fv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord2sv(short[] v) {
		check(() -> GL11.glTexCoord2sv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord2iv(int[] v) {
		check(() -> GL11.glTexCoord2iv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord2dv(double[] v) {
		check(() -> GL11.glTexCoord2dv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord3fv(float[] v) {
		check(() -> GL11.glTexCoord3fv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord3sv(short[] v) {
		check(() -> GL11.glTexCoord3sv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord3iv(int[] v) {
		check(() -> GL11.glTexCoord3iv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord3dv(double[] v) {
		check(() -> GL11.glTexCoord3dv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord4fv(float[] v) {
		check(() -> GL11.glTexCoord4fv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord4sv(short[] v) {
		check(() -> GL11.glTexCoord4sv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord4iv(int[] v) {
		check(() -> GL11.glTexCoord4iv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexCoord4dv(double[] v) {
		check(() -> GL11.glTexCoord4dv(v));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexEnviv(int target, int pname, int[] params) {
		check(() -> GL11.glTexEnviv(target, pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexEnvfv(int target, int pname, float[] params) {
		check(() -> GL11.glTexEnvfv(target, pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexGeniv(int coord, int pname, int[] params) {
		check(() -> GL11.glTexGeniv(coord, pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexGenfv(int coord, int pname, float[] params) {
		check(() -> GL11.glTexGenfv(coord, pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexGendv(int coord, int pname, double[] params) {
		check(() -> GL11.glTexGendv(coord, pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, short[] pixels) {
		check(() -> GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, int[] pixels) {
		check(() -> GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, float[] pixels) {
		check(() -> GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, double[] pixels) {
		check(() -> GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format, int type, short[] pixels) {
		check(() -> GL11.glTexImage1D(target, level, internalformat, width, border, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format, int type, int[] pixels) {
		check(() -> GL11.glTexImage1D(target, level, internalformat, width, border, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format, int type, float[] pixels) {
		check(() -> GL11.glTexImage1D(target, level, internalformat, width, border, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexImage1D(int target, int level, int internalformat, int width, int border, int format, int type, double[] pixels) {
		check(() -> GL11.glTexImage1D(target, level, internalformat, width, border, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexParameteriv(int target, int pname, int[] params) {
		check(() -> GL11.glTexParameteriv(target, pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexParameterfv(int target, int pname, float[] params) {
		check(() -> GL11.glTexParameterfv(target, pname, params));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexSubImage1D(int target, int level, int xoffset, int width, int format, int type, short[] pixels) {
		check(() -> GL11.glTexSubImage1D(target, level, xoffset, width, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexSubImage1D(int target, int level, int xoffset, int width, int format, int type, int[] pixels) {
		check(() -> GL11.glTexSubImage1D(target, level, xoffset, width, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexSubImage1D(int target, int level, int xoffset, int width, int format, int type, float[] pixels) {
		check(() -> GL11.glTexSubImage1D(target, level, xoffset, width, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexSubImage1D(int target, int level, int xoffset, int width, int format, int type, double[] pixels) {
		check(() -> GL11.glTexSubImage1D(target, level, xoffset, width, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, short[] pixels) {
		check(() -> GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, int[] pixels) {
		check(() -> GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, float[] pixels) {
		check(() -> GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, double[] pixels) {
		check(() -> GL11.glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex2fv(float[] coords) {
		check(() -> GL11.glVertex2fv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex2sv(short[] coords) {
		check(() -> GL11.glVertex2sv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex2iv(int[] coords) {
		check(() -> GL11.glVertex2iv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex2dv(double[] coords) {
		check(() -> GL11.glVertex2dv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex3fv(float[] coords) {
		check(() -> GL11.glVertex3fv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex3sv(short[] coords) {
		check(() -> GL11.glVertex3sv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex3iv(int[] coords) {
		check(() -> GL11.glVertex3iv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex3dv(double[] coords) {
		check(() -> GL11.glVertex3dv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex4fv(float[] coords) {
		check(() -> GL11.glVertex4fv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex4sv(short[] coords) {
		check(() -> GL11.glVertex4sv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex4iv(int[] coords) {
		check(() -> GL11.glVertex4iv(coords));
	}

	@DocumentationProxyTo(GL11.class)
	public static void glVertex4dv(double[] coords) {
		check(() -> GL11.glVertex4dv(coords));
	}


	/** AccumOp */
	public static final int
		GL_ACCUM  = 0x100,
		GL_LOAD   = 0x101,
		GL_RETURN = 0x102,
		GL_MULT   = 0x103,
		GL_ADD    = 0x104;

	/** AlphaFunction */
	public static final int
		GL_NEVER    = 0x200,
		GL_LESS     = 0x201,
		GL_EQUAL    = 0x202,
		GL_LEQUAL   = 0x203,
		GL_GREATER  = 0x204,
		GL_NOTEQUAL = 0x205,
		GL_GEQUAL   = 0x206,
		GL_ALWAYS   = 0x207;

	/** AttribMask */
	public static final int
		GL_CURRENT_BIT         = 0x1,
		GL_POINT_BIT           = 0x2,
		GL_LINE_BIT            = 0x4,
		GL_POLYGON_BIT         = 0x8,
		GL_POLYGON_STIPPLE_BIT = 0x10,
		GL_PIXEL_MODE_BIT      = 0x20,
		GL_LIGHTING_BIT        = 0x40,
		GL_FOG_BIT             = 0x80,
		GL_DEPTH_BUFFER_BIT    = 0x100,
		GL_ACCUM_BUFFER_BIT    = 0x200,
		GL_STENCIL_BUFFER_BIT  = 0x400,
		GL_VIEWPORT_BIT        = 0x800,
		GL_TRANSFORM_BIT       = 0x1000,
		GL_ENABLE_BIT          = 0x2000,
		GL_COLOR_BUFFER_BIT    = 0x4000,
		GL_HINT_BIT            = 0x8000,
		GL_EVAL_BIT            = 0x10000,
		GL_LIST_BIT            = 0x20000,
		GL_TEXTURE_BIT         = 0x40000,
		GL_SCISSOR_BIT         = 0x80000,
		GL_ALL_ATTRIB_BITS     = 0xFFFFF;

	/** BeginMode */
	public static final int
		GL_POINTS         = 0x0,
		GL_LINES          = 0x1,
		GL_LINE_LOOP      = 0x2,
		GL_LINE_STRIP     = 0x3,
		GL_TRIANGLES      = 0x4,
		GL_TRIANGLE_STRIP = 0x5,
		GL_TRIANGLE_FAN   = 0x6,
		GL_QUADS          = 0x7,
		GL_QUAD_STRIP     = 0x8,
		GL_POLYGON        = 0x9;

	/** BlendingFactorDest */
	public static final int
		GL_ZERO                = 0,
		GL_ONE                 = 1,
		GL_SRC_COLOR           = 0x300,
		GL_ONE_MINUS_SRC_COLOR = 0x301,
		GL_SRC_ALPHA           = 0x302,
		GL_ONE_MINUS_SRC_ALPHA = 0x303,
		GL_DST_ALPHA           = 0x304,
		GL_ONE_MINUS_DST_ALPHA = 0x305;

	/** BlendingFactorSrc */
	public static final int
		GL_DST_COLOR           = 0x306,
		GL_ONE_MINUS_DST_COLOR = 0x307,
		GL_SRC_ALPHA_SATURATE  = 0x308;

	/** Boolean */
	public static final int
		GL_TRUE  = 1,
		GL_FALSE = 0;

	/** ClipPlaneName */
	public static final int
		GL_CLIP_PLANE0 = 0x3000,
		GL_CLIP_PLANE1 = 0x3001,
		GL_CLIP_PLANE2 = 0x3002,
		GL_CLIP_PLANE3 = 0x3003,
		GL_CLIP_PLANE4 = 0x3004,
		GL_CLIP_PLANE5 = 0x3005;

	/** DataType */
	public static final int
		GL_BYTE           = 0x1400,
		GL_UNSIGNED_BYTE  = 0x1401,
		GL_SHORT          = 0x1402,
		GL_UNSIGNED_SHORT = 0x1403,
		GL_INT            = 0x1404,
		GL_UNSIGNED_INT   = 0x1405,
		GL_FLOAT          = 0x1406,
		GL_2_BYTES        = 0x1407,
		GL_3_BYTES        = 0x1408,
		GL_4_BYTES        = 0x1409,
		GL_DOUBLE         = 0x140A;

	/** DrawBufferMode */
	public static final int
		GL_NONE           = 0,
		GL_FRONT_LEFT     = 0x400,
		GL_FRONT_RIGHT    = 0x401,
		GL_BACK_LEFT      = 0x402,
		GL_BACK_RIGHT     = 0x403,
		GL_FRONT          = 0x404,
		GL_BACK           = 0x405,
		GL_LEFT           = 0x406,
		GL_RIGHT          = 0x407,
		GL_FRONT_AND_BACK = 0x408,
		GL_AUX0           = 0x409,
		GL_AUX1           = 0x40A,
		GL_AUX2           = 0x40B,
		GL_AUX3           = 0x40C;

	/** ErrorCode */
	public static final int
		GL_NO_ERROR          = 0,
		GL_INVALID_ENUM      = 0x500,
		GL_INVALID_VALUE     = 0x501,
		GL_INVALID_OPERATION = 0x502,
		GL_STACK_OVERFLOW    = 0x503,
		GL_STACK_UNDERFLOW   = 0x504,
		GL_OUT_OF_MEMORY     = 0x505;

	/** FeedBackMode */
	public static final int
		GL_2D               = 0x600,
		GL_3D               = 0x601,
		GL_3D_COLOR         = 0x602,
		GL_3D_COLOR_TEXTURE = 0x603,
		GL_4D_COLOR_TEXTURE = 0x604;

	/** FeedBackToken */
	public static final int
		GL_PASS_THROUGH_TOKEN = 0x700,
		GL_POINT_TOKEN        = 0x701,
		GL_LINE_TOKEN         = 0x702,
		GL_POLYGON_TOKEN      = 0x703,
		GL_BITMAP_TOKEN       = 0x704,
		GL_DRAW_PIXEL_TOKEN   = 0x705,
		GL_COPY_PIXEL_TOKEN   = 0x706,
		GL_LINE_RESET_TOKEN   = 0x707;

	/** FogMode */
	public static final int
		GL_EXP  = 0x800,
		GL_EXP2 = 0x801;

	/** FrontFaceDirection */
	public static final int
		GL_CW  = 0x900,
		GL_CCW = 0x901;

	/** GetMapTarget */
	public static final int
		GL_COEFF  = 0xA00,
		GL_ORDER  = 0xA01,
		GL_DOMAIN = 0xA02;

	/** GetTarget */
	public static final int
		GL_CURRENT_COLOR                 = 0xB00,
		GL_CURRENT_INDEX                 = 0xB01,
		GL_CURRENT_NORMAL                = 0xB02,
		GL_CURRENT_TEXTURE_COORDS        = 0xB03,
		GL_CURRENT_RASTER_COLOR          = 0xB04,
		GL_CURRENT_RASTER_INDEX          = 0xB05,
		GL_CURRENT_RASTER_TEXTURE_COORDS = 0xB06,
		GL_CURRENT_RASTER_POSITION       = 0xB07,
		GL_CURRENT_RASTER_POSITION_VALID = 0xB08,
		GL_CURRENT_RASTER_DISTANCE       = 0xB09,
		GL_POINT_SMOOTH                  = 0xB10,
		GL_POINT_SIZE                    = 0xB11,
		GL_POINT_SIZE_RANGE              = 0xB12,
		GL_POINT_SIZE_GRANULARITY        = 0xB13,
		GL_LINE_SMOOTH                   = 0xB20,
		GL_LINE_WIDTH                    = 0xB21,
		GL_LINE_WIDTH_RANGE              = 0xB22,
		GL_LINE_WIDTH_GRANULARITY        = 0xB23,
		GL_LINE_STIPPLE                  = 0xB24,
		GL_LINE_STIPPLE_PATTERN          = 0xB25,
		GL_LINE_STIPPLE_REPEAT           = 0xB26,
		GL_LIST_MODE                     = 0xB30,
		GL_MAX_LIST_NESTING              = 0xB31,
		GL_LIST_BASE                     = 0xB32,
		GL_LIST_INDEX                    = 0xB33,
		GL_POLYGON_MODE                  = 0xB40,
		GL_POLYGON_SMOOTH                = 0xB41,
		GL_POLYGON_STIPPLE               = 0xB42,
		GL_EDGE_FLAG                     = 0xB43,
		GL_CULL_FACE                     = 0xB44,
		GL_CULL_FACE_MODE                = 0xB45,
		GL_FRONT_FACE                    = 0xB46,
		GL_LIGHTING                      = 0xB50,
		GL_LIGHT_MODEL_LOCAL_VIEWER      = 0xB51,
		GL_LIGHT_MODEL_TWO_SIDE          = 0xB52,
		GL_LIGHT_MODEL_AMBIENT           = 0xB53,
		GL_SHADE_MODEL                   = 0xB54,
		GL_COLOR_MATERIAL_FACE           = 0xB55,
		GL_COLOR_MATERIAL_PARAMETER      = 0xB56,
		GL_COLOR_MATERIAL                = 0xB57,
		GL_FOG                           = 0xB60,
		GL_FOG_INDEX                     = 0xB61,
		GL_FOG_DENSITY                   = 0xB62,
		GL_FOG_START                     = 0xB63,
		GL_FOG_END                       = 0xB64,
		GL_FOG_MODE                      = 0xB65,
		GL_FOG_COLOR                     = 0xB66,
		GL_DEPTH_RANGE                   = 0xB70,
		GL_DEPTH_TEST                    = 0xB71,
		GL_DEPTH_WRITEMASK               = 0xB72,
		GL_DEPTH_CLEAR_VALUE             = 0xB73,
		GL_DEPTH_FUNC                    = 0xB74,
		GL_ACCUM_CLEAR_VALUE             = 0xB80,
		GL_STENCIL_TEST                  = 0xB90,
		GL_STENCIL_CLEAR_VALUE           = 0xB91,
		GL_STENCIL_FUNC                  = 0xB92,
		GL_STENCIL_VALUE_MASK            = 0xB93,
		GL_STENCIL_FAIL                  = 0xB94,
		GL_STENCIL_PASS_DEPTH_FAIL       = 0xB95,
		GL_STENCIL_PASS_DEPTH_PASS       = 0xB96,
		GL_STENCIL_REF                   = 0xB97,
		GL_STENCIL_WRITEMASK             = 0xB98,
		GL_MATRIX_MODE                   = 0xBA0,
		GL_NORMALIZE                     = 0xBA1,
		GL_VIEWPORT                      = 0xBA2,
		GL_MODELVIEW_STACK_DEPTH         = 0xBA3,
		GL_PROJECTION_STACK_DEPTH        = 0xBA4,
		GL_TEXTURE_STACK_DEPTH           = 0xBA5,
		GL_MODELVIEW_MATRIX              = 0xBA6,
		GL_PROJECTION_MATRIX             = 0xBA7,
		GL_TEXTURE_MATRIX                = 0xBA8,
		GL_ATTRIB_STACK_DEPTH            = 0xBB0,
		GL_CLIENT_ATTRIB_STACK_DEPTH     = 0xBB1,
		GL_ALPHA_TEST                    = 0xBC0,
		GL_ALPHA_TEST_FUNC               = 0xBC1,
		GL_ALPHA_TEST_REF                = 0xBC2,
		GL_DITHER                        = 0xBD0,
		GL_BLEND_DST                     = 0xBE0,
		GL_BLEND_SRC                     = 0xBE1,
		GL_BLEND                         = 0xBE2,
		GL_LOGIC_OP_MODE                 = 0xBF0,
		GL_INDEX_LOGIC_OP                = 0xBF1,
		GL_LOGIC_OP                      = 0xBF1,
		GL_COLOR_LOGIC_OP                = 0xBF2,
		GL_AUX_BUFFERS                   = 0xC00,
		GL_DRAW_BUFFER                   = 0xC01,
		GL_READ_BUFFER                   = 0xC02,
		GL_SCISSOR_BOX                   = 0xC10,
		GL_SCISSOR_TEST                  = 0xC11,
		GL_INDEX_CLEAR_VALUE             = 0xC20,
		GL_INDEX_WRITEMASK               = 0xC21,
		GL_COLOR_CLEAR_VALUE             = 0xC22,
		GL_COLOR_WRITEMASK               = 0xC23,
		GL_INDEX_MODE                    = 0xC30,
		GL_RGBA_MODE                     = 0xC31,
		GL_DOUBLEBUFFER                  = 0xC32,
		GL_STEREO                        = 0xC33,
		GL_RENDER_MODE                   = 0xC40,
		GL_PERSPECTIVE_CORRECTION_HINT   = 0xC50,
		GL_POINT_SMOOTH_HINT             = 0xC51,
		GL_LINE_SMOOTH_HINT              = 0xC52,
		GL_POLYGON_SMOOTH_HINT           = 0xC53,
		GL_FOG_HINT                      = 0xC54,
		GL_TEXTURE_GEN_S                 = 0xC60,
		GL_TEXTURE_GEN_T                 = 0xC61,
		GL_TEXTURE_GEN_R                 = 0xC62,
		GL_TEXTURE_GEN_Q                 = 0xC63,
		GL_PIXEL_MAP_I_TO_I              = 0xC70,
		GL_PIXEL_MAP_S_TO_S              = 0xC71,
		GL_PIXEL_MAP_I_TO_R              = 0xC72,
		GL_PIXEL_MAP_I_TO_G              = 0xC73,
		GL_PIXEL_MAP_I_TO_B              = 0xC74,
		GL_PIXEL_MAP_I_TO_A              = 0xC75,
		GL_PIXEL_MAP_R_TO_R              = 0xC76,
		GL_PIXEL_MAP_G_TO_G              = 0xC77,
		GL_PIXEL_MAP_B_TO_B              = 0xC78,
		GL_PIXEL_MAP_A_TO_A              = 0xC79,
		GL_PIXEL_MAP_I_TO_I_SIZE         = 0xCB0,
		GL_PIXEL_MAP_S_TO_S_SIZE         = 0xCB1,
		GL_PIXEL_MAP_I_TO_R_SIZE         = 0xCB2,
		GL_PIXEL_MAP_I_TO_G_SIZE         = 0xCB3,
		GL_PIXEL_MAP_I_TO_B_SIZE         = 0xCB4,
		GL_PIXEL_MAP_I_TO_A_SIZE         = 0xCB5,
		GL_PIXEL_MAP_R_TO_R_SIZE         = 0xCB6,
		GL_PIXEL_MAP_G_TO_G_SIZE         = 0xCB7,
		GL_PIXEL_MAP_B_TO_B_SIZE         = 0xCB8,
		GL_PIXEL_MAP_A_TO_A_SIZE         = 0xCB9,
		GL_UNPACK_SWAP_BYTES             = 0xCF0,
		GL_UNPACK_LSB_FIRST              = 0xCF1,
		GL_UNPACK_ROW_LENGTH             = 0xCF2,
		GL_UNPACK_SKIP_ROWS              = 0xCF3,
		GL_UNPACK_SKIP_PIXELS            = 0xCF4,
		GL_UNPACK_ALIGNMENT              = 0xCF5,
		GL_PACK_SWAP_BYTES               = 0xD00,
		GL_PACK_LSB_FIRST                = 0xD01,
		GL_PACK_ROW_LENGTH               = 0xD02,
		GL_PACK_SKIP_ROWS                = 0xD03,
		GL_PACK_SKIP_PIXELS              = 0xD04,
		GL_PACK_ALIGNMENT                = 0xD05,
		GL_MAP_COLOR                     = 0xD10,
		GL_MAP_STENCIL                   = 0xD11,
		GL_INDEX_SHIFT                   = 0xD12,
		GL_INDEX_OFFSET                  = 0xD13,
		GL_RED_SCALE                     = 0xD14,
		GL_RED_BIAS                      = 0xD15,
		GL_ZOOM_X                        = 0xD16,
		GL_ZOOM_Y                        = 0xD17,
		GL_GREEN_SCALE                   = 0xD18,
		GL_GREEN_BIAS                    = 0xD19,
		GL_BLUE_SCALE                    = 0xD1A,
		GL_BLUE_BIAS                     = 0xD1B,
		GL_ALPHA_SCALE                   = 0xD1C,
		GL_ALPHA_BIAS                    = 0xD1D,
		GL_DEPTH_SCALE                   = 0xD1E,
		GL_DEPTH_BIAS                    = 0xD1F,
		GL_MAX_EVAL_ORDER                = 0xD30,
		GL_MAX_LIGHTS                    = 0xD31,
		GL_MAX_CLIP_PLANES               = 0xD32,
		GL_MAX_TEXTURE_SIZE              = 0xD33,
		GL_MAX_PIXEL_MAP_TABLE           = 0xD34,
		GL_MAX_ATTRIB_STACK_DEPTH        = 0xD35,
		GL_MAX_MODELVIEW_STACK_DEPTH     = 0xD36,
		GL_MAX_NAME_STACK_DEPTH          = 0xD37,
		GL_MAX_PROJECTION_STACK_DEPTH    = 0xD38,
		GL_MAX_TEXTURE_STACK_DEPTH       = 0xD39,
		GL_MAX_VIEWPORT_DIMS             = 0xD3A,
		GL_MAX_CLIENT_ATTRIB_STACK_DEPTH = 0xD3B,
		GL_SUBPIXEL_BITS                 = 0xD50,
		GL_INDEX_BITS                    = 0xD51,
		GL_RED_BITS                      = 0xD52,
		GL_GREEN_BITS                    = 0xD53,
		GL_BLUE_BITS                     = 0xD54,
		GL_ALPHA_BITS                    = 0xD55,
		GL_DEPTH_BITS                    = 0xD56,
		GL_STENCIL_BITS                  = 0xD57,
		GL_ACCUM_RED_BITS                = 0xD58,
		GL_ACCUM_GREEN_BITS              = 0xD59,
		GL_ACCUM_BLUE_BITS               = 0xD5A,
		GL_ACCUM_ALPHA_BITS              = 0xD5B,
		GL_NAME_STACK_DEPTH              = 0xD70,
		GL_AUTO_NORMAL                   = 0xD80,
		GL_MAP1_COLOR_4                  = 0xD90,
		GL_MAP1_INDEX                    = 0xD91,
		GL_MAP1_NORMAL                   = 0xD92,
		GL_MAP1_TEXTURE_COORD_1          = 0xD93,
		GL_MAP1_TEXTURE_COORD_2          = 0xD94,
		GL_MAP1_TEXTURE_COORD_3          = 0xD95,
		GL_MAP1_TEXTURE_COORD_4          = 0xD96,
		GL_MAP1_VERTEX_3                 = 0xD97,
		GL_MAP1_VERTEX_4                 = 0xD98,
		GL_MAP2_COLOR_4                  = 0xDB0,
		GL_MAP2_INDEX                    = 0xDB1,
		GL_MAP2_NORMAL                   = 0xDB2,
		GL_MAP2_TEXTURE_COORD_1          = 0xDB3,
		GL_MAP2_TEXTURE_COORD_2          = 0xDB4,
		GL_MAP2_TEXTURE_COORD_3          = 0xDB5,
		GL_MAP2_TEXTURE_COORD_4          = 0xDB6,
		GL_MAP2_VERTEX_3                 = 0xDB7,
		GL_MAP2_VERTEX_4                 = 0xDB8,
		GL_MAP1_GRID_DOMAIN              = 0xDD0,
		GL_MAP1_GRID_SEGMENTS            = 0xDD1,
		GL_MAP2_GRID_DOMAIN              = 0xDD2,
		GL_MAP2_GRID_SEGMENTS            = 0xDD3,
		GL_TEXTURE_1D                    = 0xDE0,
		GL_TEXTURE_2D                    = 0xDE1,
		GL_FEEDBACK_BUFFER_POINTER       = 0xDF0,
		GL_FEEDBACK_BUFFER_SIZE          = 0xDF1,
		GL_FEEDBACK_BUFFER_TYPE          = 0xDF2,
		GL_SELECTION_BUFFER_POINTER      = 0xDF3,
		GL_SELECTION_BUFFER_SIZE         = 0xDF4;

	/** GetTextureParameter */
	public static final int
		GL_TEXTURE_WIDTH           = 0x1000,
		GL_TEXTURE_HEIGHT          = 0x1001,
		GL_TEXTURE_INTERNAL_FORMAT = 0x1003,
		GL_TEXTURE_COMPONENTS      = 0x1003,
		GL_TEXTURE_BORDER_COLOR    = 0x1004,
		GL_TEXTURE_BORDER          = 0x1005;

	/** HintMode */
	public static final int
		GL_DONT_CARE = 0x1100,
		GL_FASTEST   = 0x1101,
		GL_NICEST    = 0x1102;

	/** LightName */
	public static final int
		GL_LIGHT0 = 0x4000,
		GL_LIGHT1 = 0x4001,
		GL_LIGHT2 = 0x4002,
		GL_LIGHT3 = 0x4003,
		GL_LIGHT4 = 0x4004,
		GL_LIGHT5 = 0x4005,
		GL_LIGHT6 = 0x4006,
		GL_LIGHT7 = 0x4007;

	/** LightParameter */
	public static final int
		GL_AMBIENT               = 0x1200,
		GL_DIFFUSE               = 0x1201,
		GL_SPECULAR              = 0x1202,
		GL_POSITION              = 0x1203,
		GL_SPOT_DIRECTION        = 0x1204,
		GL_SPOT_EXPONENT         = 0x1205,
		GL_SPOT_CUTOFF           = 0x1206,
		GL_CONSTANT_ATTENUATION  = 0x1207,
		GL_LINEAR_ATTENUATION    = 0x1208,
		GL_QUADRATIC_ATTENUATION = 0x1209;

	/** ListMode */
	public static final int
		GL_COMPILE             = 0x1300,
		GL_COMPILE_AND_EXECUTE = 0x1301;

	/** LogicOp */
	public static final int
		GL_CLEAR         = 0x1500,
		GL_AND           = 0x1501,
		GL_AND_REVERSE   = 0x1502,
		GL_COPY          = 0x1503,
		GL_AND_INVERTED  = 0x1504,
		GL_NOOP          = 0x1505,
		GL_XOR           = 0x1506,
		GL_OR            = 0x1507,
		GL_NOR           = 0x1508,
		GL_EQUIV         = 0x1509,
		GL_INVERT        = 0x150A,
		GL_OR_REVERSE    = 0x150B,
		GL_COPY_INVERTED = 0x150C,
		GL_OR_INVERTED   = 0x150D,
		GL_NAND          = 0x150E,
		GL_SET           = 0x150F;

	/** MaterialParameter */
	public static final int
		GL_EMISSION            = 0x1600,
		GL_SHININESS           = 0x1601,
		GL_AMBIENT_AND_DIFFUSE = 0x1602,
		GL_COLOR_INDEXES       = 0x1603;

	/** MatrixMode */
	public static final int
		GL_MODELVIEW  = 0x1700,
		GL_PROJECTION = 0x1701,
		GL_TEXTURE    = 0x1702;

	/** PixelCopyType */
	public static final int
		GL_COLOR   = 0x1800,
		GL_DEPTH   = 0x1801,
		GL_STENCIL = 0x1802;

	/** PixelFormat */
	public static final int
		GL_COLOR_INDEX     = 0x1900,
		GL_STENCIL_INDEX   = 0x1901,
		GL_DEPTH_COMPONENT = 0x1902,
		GL_RED             = 0x1903,
		GL_GREEN           = 0x1904,
		GL_BLUE            = 0x1905,
		GL_ALPHA           = 0x1906,
		GL_RGB             = 0x1907,
		GL_RGBA            = 0x1908,
		GL_LUMINANCE       = 0x1909,
		GL_LUMINANCE_ALPHA = 0x190A;

	/** PixelType */
	public static final int GL_BITMAP = 0x1A00;

	/** PolygonMode */
	public static final int
		GL_POINT = 0x1B00,
		GL_LINE  = 0x1B01,
		GL_FILL  = 0x1B02;

	/** RenderingMode */
	public static final int
		GL_RENDER   = 0x1C00,
		GL_FEEDBACK = 0x1C01,
		GL_SELECT   = 0x1C02;

	/** ShadingModel */
	public static final int
		GL_FLAT   = 0x1D00,
		GL_SMOOTH = 0x1D01;

	/** StencilOp */
	public static final int
		GL_KEEP    = 0x1E00,
		GL_REPLACE = 0x1E01,
		GL_INCR    = 0x1E02,
		GL_DECR    = 0x1E03;

	/** StringName */
	public static final int
		GL_VENDOR     = 0x1F00,
		GL_RENDERER   = 0x1F01,
		GL_VERSION    = 0x1F02,
		GL_EXTENSIONS = 0x1F03;

	/** TextureCoordName */
	public static final int
		GL_S = 0x2000,
		GL_T = 0x2001,
		GL_R = 0x2002,
		GL_Q = 0x2003;

	/** TextureEnvMode */
	public static final int
		GL_MODULATE = 0x2100,
		GL_DECAL    = 0x2101;

	/** TextureEnvParameter */
	public static final int
		GL_TEXTURE_ENV_MODE  = 0x2200,
		GL_TEXTURE_ENV_COLOR = 0x2201;

	/** TextureEnvTarget */
	public static final int GL_TEXTURE_ENV = 0x2300;

	/** TextureGenMode */
	public static final int
		GL_EYE_LINEAR    = 0x2400,
		GL_OBJECT_LINEAR = 0x2401,
		GL_SPHERE_MAP    = 0x2402;

	/** TextureGenParameter */
	public static final int
		GL_TEXTURE_GEN_MODE = 0x2500,
		GL_OBJECT_PLANE     = 0x2501,
		GL_EYE_PLANE        = 0x2502;

	/** TextureMagFilter */
	public static final int
		GL_NEAREST = 0x2600,
		GL_LINEAR  = 0x2601;

	/** TextureMinFilter */
	public static final int
		GL_NEAREST_MIPMAP_NEAREST = 0x2700,
		GL_LINEAR_MIPMAP_NEAREST  = 0x2701,
		GL_NEAREST_MIPMAP_LINEAR  = 0x2702,
		GL_LINEAR_MIPMAP_LINEAR   = 0x2703;

	/** TextureParameterName */
	public static final int
		GL_TEXTURE_MAG_FILTER = 0x2800,
		GL_TEXTURE_MIN_FILTER = 0x2801,
		GL_TEXTURE_WRAP_S     = 0x2802,
		GL_TEXTURE_WRAP_T     = 0x2803;

	/** TextureWrapMode */
	public static final int
		GL_CLAMP  = 0x2900,
		GL_REPEAT = 0x2901;

	/** ClientAttribMask */
	public static final int
		GL_CLIENT_PIXEL_STORE_BIT  = 0x1,
		GL_CLIENT_VERTEX_ARRAY_BIT = 0x2,
		GL_CLIENT_ALL_ATTRIB_BITS  = 0xFFFFFFFF;

	/** polygon_offset */
	public static final int
		GL_POLYGON_OFFSET_FACTOR = 0x8038,
		GL_POLYGON_OFFSET_UNITS  = 0x2A00,
		GL_POLYGON_OFFSET_POINT  = 0x2A01,
		GL_POLYGON_OFFSET_LINE   = 0x2A02,
		GL_POLYGON_OFFSET_FILL   = 0x8037;

	/** texture */
	public static final int
		GL_ALPHA4                 = 0x803B,
		GL_ALPHA8                 = 0x803C,
		GL_ALPHA12                = 0x803D,
		GL_ALPHA16                = 0x803E,
		GL_LUMINANCE4             = 0x803F,
		GL_LUMINANCE8             = 0x8040,
		GL_LUMINANCE12            = 0x8041,
		GL_LUMINANCE16            = 0x8042,
		GL_LUMINANCE4_ALPHA4      = 0x8043,
		GL_LUMINANCE6_ALPHA2      = 0x8044,
		GL_LUMINANCE8_ALPHA8      = 0x8045,
		GL_LUMINANCE12_ALPHA4     = 0x8046,
		GL_LUMINANCE12_ALPHA12    = 0x8047,
		GL_LUMINANCE16_ALPHA16    = 0x8048,
		GL_INTENSITY              = 0x8049,
		GL_INTENSITY4             = 0x804A,
		GL_INTENSITY8             = 0x804B,
		GL_INTENSITY12            = 0x804C,
		GL_INTENSITY16            = 0x804D,
		GL_R3_G3_B2               = 0x2A10,
		GL_RGB4                   = 0x804F,
		GL_RGB5                   = 0x8050,
		GL_RGB8                   = 0x8051,
		GL_RGB10                  = 0x8052,
		GL_RGB12                  = 0x8053,
		GL_RGB16                  = 0x8054,
		GL_RGBA2                  = 0x8055,
		GL_RGBA4                  = 0x8056,
		GL_RGB5_A1                = 0x8057,
		GL_RGBA8                  = 0x8058,
		GL_RGB10_A2               = 0x8059,
		GL_RGBA12                 = 0x805A,
		GL_RGBA16                 = 0x805B,
		GL_TEXTURE_RED_SIZE       = 0x805C,
		GL_TEXTURE_GREEN_SIZE     = 0x805D,
		GL_TEXTURE_BLUE_SIZE      = 0x805E,
		GL_TEXTURE_ALPHA_SIZE     = 0x805F,
		GL_TEXTURE_LUMINANCE_SIZE = 0x8060,
		GL_TEXTURE_INTENSITY_SIZE = 0x8061,
		GL_PROXY_TEXTURE_1D       = 0x8063,
		GL_PROXY_TEXTURE_2D       = 0x8064;

	/** texture_object */
	public static final int
		GL_TEXTURE_PRIORITY   = 0x8066,
		GL_TEXTURE_RESIDENT   = 0x8067,
		GL_TEXTURE_BINDING_1D = 0x8068,
		GL_TEXTURE_BINDING_2D = 0x8069;

	/** vertex_array */
	public static final int
		GL_VERTEX_ARRAY                = 0x8074,
		GL_NORMAL_ARRAY                = 0x8075,
		GL_COLOR_ARRAY                 = 0x8076,
		GL_INDEX_ARRAY                 = 0x8077,
		GL_TEXTURE_COORD_ARRAY         = 0x8078,
		GL_EDGE_FLAG_ARRAY             = 0x8079,
		GL_VERTEX_ARRAY_SIZE           = 0x807A,
		GL_VERTEX_ARRAY_TYPE           = 0x807B,
		GL_VERTEX_ARRAY_STRIDE         = 0x807C,
		GL_NORMAL_ARRAY_TYPE           = 0x807E,
		GL_NORMAL_ARRAY_STRIDE         = 0x807F,
		GL_COLOR_ARRAY_SIZE            = 0x8081,
		GL_COLOR_ARRAY_TYPE            = 0x8082,
		GL_COLOR_ARRAY_STRIDE          = 0x8083,
		GL_INDEX_ARRAY_TYPE            = 0x8085,
		GL_INDEX_ARRAY_STRIDE          = 0x8086,
		GL_TEXTURE_COORD_ARRAY_SIZE    = 0x8088,
		GL_TEXTURE_COORD_ARRAY_TYPE    = 0x8089,
		GL_TEXTURE_COORD_ARRAY_STRIDE  = 0x808A,
		GL_EDGE_FLAG_ARRAY_STRIDE      = 0x808C,
		GL_VERTEX_ARRAY_POINTER        = 0x808E,
		GL_NORMAL_ARRAY_POINTER        = 0x808F,
		GL_COLOR_ARRAY_POINTER         = 0x8090,
		GL_INDEX_ARRAY_POINTER         = 0x8091,
		GL_TEXTURE_COORD_ARRAY_POINTER = 0x8092,
		GL_EDGE_FLAG_ARRAY_POINTER     = 0x8093,
		GL_V2F                         = 0x2A20,
		GL_V3F                         = 0x2A21,
		GL_C4UB_V2F                    = 0x2A22,
		GL_C4UB_V3F                    = 0x2A23,
		GL_C3F_V3F                     = 0x2A24,
		GL_N3F_V3F                     = 0x2A25,
		GL_C4F_N3F_V3F                 = 0x2A26,
		GL_T2F_V3F                     = 0x2A27,
		GL_T4F_V4F                     = 0x2A28,
		GL_T2F_C4UB_V3F                = 0x2A29,
		GL_T2F_C3F_V3F                 = 0x2A2A,
		GL_T2F_N3F_V3F                 = 0x2A2B,
		GL_T2F_C4F_N3F_V3F             = 0x2A2C,
		GL_T4F_C4F_N3F_V4F             = 0x2A2D;
}
