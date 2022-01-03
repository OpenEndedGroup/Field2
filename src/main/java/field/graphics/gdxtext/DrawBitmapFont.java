package field.graphics.gdxtext;

import field.graphics.MeshBuilder;
import field.graphics.Texture;
import field.linalg.Vec2;
import field.linalg.Vec3;
import field.utility.Pair;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by marc on 3/22/14.
 */
public class DrawBitmapFont {

	private final BitmapFontData data;
	private final MeshBuilder target;
	private final int cacheSize;

	public DrawBitmapFont(String fontName, MeshBuilder target, int unit, int cacheSize) {
		this.data = new BitmapFontData(fontName, fontName + ".jpg", unit);
		this.target = target;
		this.cacheSize = cacheSize;
	}

	public Texture getTexture() {
		return data.texture;
	}

	Map<List, Pair<MeshBuilder.Bookmark, MeshBuilder.Bookmark>> cache = new LinkedHashMap<List, Pair<MeshBuilder.Bookmark, MeshBuilder.Bookmark>>() {
		@Override
		protected boolean removeEldestEntry(Map.Entry<List, Pair<MeshBuilder.Bookmark, MeshBuilder.Bookmark>> eldest) {
			return size() > cacheSize;
		}
	};

	Map<List, Vec2> bounds = new LinkedHashMap<List, Vec2>() {
		@Override
		protected boolean removeEldestEntry(Map.Entry<List, Vec2> eldest) {
			return size() > cacheSize;
		}
	};

	public Vec2 dimensions(String text, float scale) {
		List<Object> hash = Arrays.asList(text, scale);
		return bounds.computeIfAbsent(hash, (k) -> _dimensions(text, scale));
	}

	private Vec2 _dimensions(String text, float scale) {

		if (text.contains("\n")) {
			String[] pieces = text.split("\n");
			Vec2 o = null;
			for (String s : pieces) {
				Vec2 d = _dimensions(s, scale);

				if (o == null)
				{
					o = d;
				}
				else {
					o = new Vec2(Math.max(o.x, d.x), o.y + data.getGlyph('I').height*scale*1.5f); // + leading?
				}
			}
			return o;
		}

		char[] ca = text.toCharArray();
		Vec2 v = new Vec2();
		for (int i = 0; i < ca.length; i++) {
			BitmapFontData.Glyph g = data.getGlyph(ca[i]);
			if (g == null) continue;

			if (i < ca.length - 1) {
				v.x += (g.xadvance - 32 + g.getKerning(ca[i + 1])) * scale;
			} else {
				v.x += g.xadvance * scale;
			}
//			v.y = Math.max(v.y, /*g.yoffset*scale +*/ g.height*scale);
			v.y = Math.max(v.y, /*g.yoffset*scale +*/g.height * scale);
		}
		return v;
	}

	public void draw(String text, Vec2 origin, float scale) {
		draw(text, origin, scale, null);
	}

	public void draw(String text, Vec2 origin, float scale, Object h) {
		draw(text, new Vec3(origin.x, origin.y, 0), scale, h);
	}

	public void draw(String text, Vec3 origin, float scale, Object h) {
		draw(text, origin, scale, h, origin);
	}

	public void draw(String text, Vec3 origin, float scale, Object h, Vec3 shaderOrigin) {

		if (text.contains("\n"))
		{
			String[] pieces = text.split("\n");
			Vec3 o = new Vec3(origin);
			for(String p : pieces)
			{
				if (p.trim().length()>0)
					draw(p, o, scale, h);
				o.y += data.getGlyph('I').height*scale*1.5f;
			}
			return ;
		}

		List<Object> hash = Arrays.asList(text, new Vec2(origin.toVec2()), scale, h);
		Pair<MeshBuilder.Bookmark, MeshBuilder.Bookmark> m = cache.computeIfAbsent(hash, (k) -> new Pair<>(target.bookmark().invalidate(), target.bookmark()));

		float smoothing = Math.min(4, Math.max(0.02f, scale));

		float Z = (float) origin.z;

		target.skipTo(m.first, m.second, hash, () -> {

			Vec2 at = new Vec2(origin.toVec2());
			char[] ca = text.toCharArray();

			float mx = data.getGlyph('M').yoffset;


			at.y += mx * scale;
			for (int i = 0; i < ca.length; i++) {
				BitmapFontData.Glyph g = data.getGlyph(ca[i]);
				if (g==null) continue;


				if (i == 0) {
					at.x -= g.xoffset * scale;
				}

				float shim = 5f;

				target.aux(3, g.srcX, g.srcY + g.height, smoothing);
				target.aux(4, (float)shaderOrigin.x, (float)shaderOrigin.y-shim);
				target.v(at.x + g.xoffset * scale, at.y - g.yoffset * scale, Z);
				target.aux(3, g.srcX + g.width, g.srcY + g.height, smoothing);
				target.aux(4, (float)shaderOrigin.x, (float)shaderOrigin.y-shim);
				target.v(at.x + g.width * scale + g.xoffset * scale, at.y - g.yoffset * scale, Z);
				target.aux(3, g.srcX + g.width, g.srcY, smoothing);
				target.aux(4, (float)shaderOrigin.x, (float)shaderOrigin.y-shim);
				target.v(at.x + g.width * scale + g.xoffset * scale, at.y - g.height * scale - g.yoffset * scale, Z);
				target.aux(3, g.srcX, g.srcY, smoothing);
				target.aux(4, (float)shaderOrigin.x, (float)shaderOrigin.y-shim);
				target.v(at.x + g.xoffset * scale, at.y - g.height * scale - g.yoffset * scale, Z);
				target.e_quad(0, 1, 2, 3);

				if (i < ca.length - 1) {
					at.x += (g.xadvance - 32 + g.getKerning(ca[i + 1])) * scale;
				}
			}
		});
	}


}
