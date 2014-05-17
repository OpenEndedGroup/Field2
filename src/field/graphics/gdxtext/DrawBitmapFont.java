package field.graphics.gdxtext;

import field.graphics.MeshBuilder;
import field.graphics.Texture;
import field.linalg.Vec2;
import field.utility.Pair;

import java.util.*;

/**
 * Created by marc on 3/22/14.
 */
public class DrawBitmapFont {

	private final BitmapFontData data;
	private final MeshBuilder target;
	private final int cacheSize;

	public DrawBitmapFont(String fontName, MeshBuilder target, int unit, int cacheSize) {
		this.data = new BitmapFontData(fontName, fontName+".jpg", unit);
		this.target = target;
		this.cacheSize = cacheSize;
	}

	public Texture getTexture()
	{
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
		char[] ca = text.toCharArray();
		Vec2 v = new Vec2();
		for (int i = 0; i < ca.length; i++) {
			BitmapFontData.Glyph g = data.getGlyph(ca[i]);

			if (i < ca.length - 1) {
				v.x += (g.xadvance + g.getKerning(ca[i + 1])) * scale;
			}
			else
			{
				v.x += g.xadvance*scale;
			}
			v.y = Math.max(v.y, /*g.yoffset*scale +*/ g.height*scale);
		}
		return v;
	}

	public void draw(String text, Vec2 origin, float scale) {
		draw(text, origin, scale, null);
	}

	public void draw(String text, Vec2 origin, float scale, Object h) {
		List<Object> hash = Arrays.asList(text, new Vec2(origin), scale,h);
		Pair<MeshBuilder.Bookmark, MeshBuilder.Bookmark> m = cache.computeIfAbsent(hash, (k) -> new Pair<>(target.bookmark().invalidate(), target.bookmark()));

//		System.out.println(" looked up hash for "+text+" "+origin+" "+scale+" and got "+m+" "+System.identityHashCode(m));

		target.skipTo(m.first, m.second, hash, () -> {

			Vec2 at = new Vec2(origin);
			char[] ca = text.toCharArray();
			int mx = 100;
			for( int i=0;i<ca.length;i++)
			{
				BitmapFontData.Glyph g = data.getGlyph(ca[i]);
				mx = Math.min(mx, g.yoffset);
			}

			at.y += mx*scale;
			for (int i = 0; i < ca.length; i++) {
				BitmapFontData.Glyph g = data.getGlyph(ca[i]);

				if (i==0)
				{
					at.x -= g.xoffset*scale;
				}


				target.aux(3, g.srcX, g.srcY + g.height);
				target.nextVertex(at.x + g.xoffset * scale, at.y - g.yoffset * scale, 0);
				target.aux(3, g.srcX + g.width, g.srcY + g.height);
				target.nextVertex(at.x + g.width * scale + g.xoffset * scale, at.y - g.yoffset * scale, 0);
				target.aux(3, g.srcX + g.width, g.srcY);
				target.nextVertex(at.x + g.width * scale + g.xoffset * scale, at.y - g.height * scale - g.yoffset * scale, 0);
				target.aux(3, g.srcX, g.srcY);
				target.nextVertex(at.x + g.xoffset * scale, at.y - g.height * scale - g.yoffset * scale, 0);
				target.nextElement_quad(0, 1, 2, 3);

				if (i < ca.length - 1) {
					at.x += (g.xadvance + g.getKerning(ca[i + 1])) * scale;
				}
			}
		});	}


}
