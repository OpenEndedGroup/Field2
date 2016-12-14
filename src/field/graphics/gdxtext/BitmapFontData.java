package field.graphics.gdxtext;

/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

import field.graphics.Texture;

import java.io.*;
import java.util.StringTokenizer;

public class BitmapFontData {

	public final Texture texture;

	static private final int LOG2_PAGE_SIZE = 9;
	static private final int PAGE_SIZE = 1 << LOG2_PAGE_SIZE;
	static private final int PAGES = 0x10000 / PAGE_SIZE;

	public static final char[] xChars = {'x', 'e', 'a', 'o', 'n', 's', 'r', 'c', 'u', 'm', 'v', 'w', 'z'};
	public static final char[] capChars = {'M', 'N', 'B', 'D', 'C', 'E', 'F', 'K', 'A', 'G', 'H', 'I', 'J', 'L', 'O', 'P', 'Q',
		    'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};

	public static class Glyph {
		public int id;
		public int srcX;
		public int srcY;
		public int width, height;
		public float u, v, u2, v2;
		public int xoffset, yoffset;
		public int xadvance;
		public byte[][] kerning;

		/** The index to the texture page that holds this glyph. */
		public int page = 0;

		public int getKerning (char ch) {
			if (kerning != null) {
				byte[] page = kerning[ch >>> LOG2_PAGE_SIZE];
				if (page != null) return page[ch & PAGE_SIZE - 1];
			}
			return 0;
		}

		public void setKerning (int ch, int value) {
			if (kerning == null) kerning = new byte[PAGES][];
			byte[] page = kerning[ch >>> LOG2_PAGE_SIZE];
			if (page == null) kerning[ch >>> LOG2_PAGE_SIZE] = page = new byte[PAGE_SIZE];
			page[ch & PAGE_SIZE - 1] = (byte)value;
		}
	}

	/**
	 * The first discovered image path; included for backwards-compatibility This is the same as imagePaths[0].
	 *
	 * @deprecated use imagePaths[0] instead
	 */
	@Deprecated
	public String imagePath;

	/**
	 * An array of the image paths, i.e. for multiple texture pages
	 */
	public String[] imagePaths;
	public String fontFile;
	public float lineHeight;
	public float capHeight = 1;
	public float ascent;
	public float descent;
	public float down;
	public float scaleX = 1, scaleY = 1;

	public final Glyph[][] glyphs = new Glyph[PAGES][];
	public float spaceWidth;
	public float xHeight = 1;

	@SuppressWarnings("deprecation")
	public BitmapFontData(String fontFile, String textureJpeg, int textureUnit) {
		this.fontFile = fontFile;
		this.texture = new Texture(Texture.TextureSpecification.fromJpeg(textureUnit, textureJpeg, true));

		try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fontFile)), 512)) {
			reader.readLine(); // info

			String line = reader.readLine();
			if (line == null) throw new IllegalArgumentException("Invalid font file: " + fontFile);
			String[] common = line.split(" ", 7); // we want the 6th element to be in tact; i.e. "page=N"

			// we only really NEED lineHeight and base
			if (common.length < 3) throw new IllegalArgumentException("Invalid font file: " + fontFile);

			if (!common[1].startsWith("lineHeight="))
				throw new IllegalArgumentException("Invalid font file: " + fontFile);
			lineHeight = Integer.parseInt(common[1].substring(11));

			if (!common[2].startsWith("base="))
				throw new IllegalArgumentException("Invalid font file: " + fontFile);
			float baseLine = Integer.parseInt(common[2].substring(5));

			// parse the pages count
			int imgPageCount = 1;
			if (common.length >= 6 && common[5] != null && common[5].startsWith("pages=")) {
				try {
					imgPageCount = Math.max(1, Integer.parseInt(common[5].substring(6)));
				} catch (NumberFormatException e) {
					// just ignore and only use one page...
					// somebody must have tampered with the page count >:(
				}
			}

			imagePaths = new String[imgPageCount];

			// read each page definition
			for (int p = 0; p < imgPageCount; p++) {
				// read each "page" info line
				line = reader.readLine();
				if (line == null)
					throw new IllegalArgumentException("Expected more 'page' definitions in font file " + fontFile);
				String[] pageLine = line.split(" ", 4);
				if (!pageLine[2].startsWith("file="))
					throw new IllegalArgumentException("Invalid font file: " + fontFile);

				// we will expect ID to mean "index" -- if for some reason this is not the case, it will fuck everything up
				// so we need to warn the user that their BMFont output is bogus
				if (pageLine[1].startsWith("id=")) {
					try {
						int pageID = Integer.parseInt(pageLine[1].substring(3));
						if (pageID != p)
							throw new IllegalArgumentException("Invalid font file: " + fontFile + " -- page ids must be indices starting at 0");
					} catch (NumberFormatException e) {
						throw new IllegalArgumentException("NumberFormatException on 'page id' element of " + fontFile);
					}
				}

				String imgFilename = null;
				if (pageLine[2].endsWith("\"")) {
					imgFilename = pageLine[2].substring(6, pageLine[2].length() - 1);
				} else {
					imgFilename = pageLine[2].substring(5, pageLine[2].length());
				}

				String path = new File(new File(fontFile).getParentFile(), imgFilename).getAbsolutePath().replaceAll("\\\\", "/");
				if (this.imagePath == null) this.imagePath = path;
				imagePaths[p] = path;
			}
			descent = 0;

			while (true) {
				line = reader.readLine();
				if (line == null) break; // EOF
				if (line.startsWith("kernings ")) break; // Starting kernings block
				if (!line.startsWith("char ")) continue;

				Glyph glyph = new Glyph();

				StringTokenizer tokens = new StringTokenizer(line, " =");
				tokens.nextToken();
				tokens.nextToken();
				int ch = Integer.parseInt(tokens.nextToken());
				if (ch <= Character.MAX_VALUE) setGlyph(ch, glyph);
				else continue;
				glyph.id = ch;
				tokens.nextToken();
				glyph.srcX = Integer.parseInt(tokens.nextToken());
				tokens.nextToken();
				glyph.srcY = Integer.parseInt(tokens.nextToken());
				tokens.nextToken();
				glyph.width = Integer.parseInt(tokens.nextToken());
				tokens.nextToken();
				glyph.height = Integer.parseInt(tokens.nextToken());
				tokens.nextToken();
				glyph.xoffset = Integer.parseInt(tokens.nextToken());
				tokens.nextToken();
				glyph.yoffset = -(glyph.height + Integer.parseInt(tokens.nextToken()));
				tokens.nextToken();
				glyph.xadvance = Integer.parseInt(tokens.nextToken());

				// also check for page.. a little safer here since we don't want to break any old functionality
				// and since maybe some shitty BMFont tools won't bother writing page id??
				if (tokens.hasMoreTokens()) tokens.nextToken();
				if (tokens.hasMoreTokens()) {
					try {
						glyph.page = Integer.parseInt(tokens.nextToken());
					} catch (NumberFormatException e) {
					}
				}

				if (glyph.width > 0 && glyph.height > 0)
					descent = Math.min(baseLine + glyph.yoffset, descent);
			}

			while (true) {
				line = reader.readLine();
				if (line == null) break;
				if (!line.startsWith("kerning ")) break;

				StringTokenizer tokens = new StringTokenizer(line, " =");
				tokens.nextToken();
				tokens.nextToken();
				int first = Integer.parseInt(tokens.nextToken());
				tokens.nextToken();
				int second = Integer.parseInt(tokens.nextToken());
				if (first < 0 || first > Character.MAX_VALUE || second < 0 || second > Character.MAX_VALUE)
					continue;
				Glyph glyph = getGlyph((char) first);
				tokens.nextToken();
				int amount = Integer.parseInt(tokens.nextToken());
				if (glyph != null) { // it appears BMFont outputs kerning for glyph pairs not contained in the font, hence the null
// check
					glyph.setKerning(second, amount);
				}
			}

			Glyph spaceGlyph = getGlyph(' ');
			if (spaceGlyph == null) {
				spaceGlyph = new Glyph();
				spaceGlyph.id = (int) ' ';
				Glyph xadvanceGlyph = getGlyph('l');
				if (xadvanceGlyph == null) xadvanceGlyph = getFirstGlyph();
				spaceGlyph.xadvance = xadvanceGlyph.xadvance;
				setGlyph(' ', spaceGlyph);
			}
			spaceWidth = spaceGlyph != null ? spaceGlyph.xadvance + spaceGlyph.width : 1;

			Glyph xGlyph = null;
			for (int i = 0; i < xChars.length; i++) {
				xGlyph = getGlyph(xChars[i]);
				if (xGlyph != null) break;
			}
			if (xGlyph == null) xGlyph = getFirstGlyph();
			xHeight = xGlyph.height;

			Glyph capGlyph = null;
			for (int i = 0; i < capChars.length; i++) {
				capGlyph = getGlyph(capChars[i]);
				if (capGlyph != null) break;
			}
			if (capGlyph == null) {
				for (Glyph[] page : this.glyphs) {
					if (page == null) continue;
					for (Glyph glyph : page) {
						if (glyph == null || glyph.height == 0 || glyph.width == 0) continue;
						capHeight = Math.max(capHeight, glyph.height);
					}
				}
			} else capHeight = capGlyph.height;

			ascent = baseLine - capHeight;
			down = -lineHeight;
		} catch (Exception ex) {
			throw new IllegalArgumentException("Error loading font file: " + fontFile, ex);
		}
	}

	public void setGlyph(int ch, Glyph glyph) {
		Glyph[] page = glyphs[ch / PAGE_SIZE];
		if (page == null) glyphs[ch / PAGE_SIZE] = page = new Glyph[PAGE_SIZE];
		page[ch & PAGE_SIZE - 1] = glyph;
	}

	public Glyph getFirstGlyph() {
		for (Glyph[] page : this.glyphs) {
			if (page == null) continue;
			for (Glyph glyph : page) {
				if (glyph == null || glyph.height == 0 || glyph.width == 0) continue;
				return glyph;
			}
		}
		throw new IllegalArgumentException("No glyphs found!");
	}

	/**
	 * Returns the glyph for the specified character, or null if no such glyph exists.
	 */
	public Glyph getGlyph(char ch) {
		Glyph[] page = glyphs[ch / PAGE_SIZE];
		if (page != null) return page[ch & PAGE_SIZE - 1];
		return null;
	}

	/**
	 * Returns the first image path; included for backwards-compatibility. Use getImagePath(int) instead.
	 *
	 * @return the first image path in the array
	 * @deprecated use getImagePath(int index) instead
	 */
	@Deprecated
	public String getImagePath() {
		return imagePath;
	}

	/**
	 * Returns the image path for the texture page at the given index.
	 *
	 * @param index the index of the page, AKA the "id" in the BMFont file
	 * @return the texture page
	 */
	public String getImagePath(int index) {
		return imagePaths[index];
	}

	public String[] getImagePaths() {
		return imagePaths;
	}

	public String getFontFile() {
		return fontFile;
	}
}