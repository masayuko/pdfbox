/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.pdfbox.pdmodel.font;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.apache.fontbox.ttf.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fontbox.util.BoundingBox;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.util.StringUtil;

/**
 * This is implementation for the CJK Fonts.
 *
 * @author Keiji Suzuki</a>
 * 
 */
public class PDUnicodeCIDFontType2Font extends PDType0Font
{
    /**
     * Log instance.
     */
    private static final Log LOG = LogFactory.getLog(PDUnicodeCIDFontType2Font.class);

    /** Original font path */
    private static String fontPath;

    /** Does the original font have serif */
    private static boolean isSerif;

    /** Prefix of the embedded font */
    private static String prefix;

    /** Font name of the embedded font*/
    private static String bfname;

    /** Codes used in wrriten text */
    private TreeSet<Integer> usedCodes = new TreeSet<Integer>();

    /** Default font width of the embedded font */
    private long defaultW;

    /** Array of font widths of the embedded font */
    private COSArray wArray;

    /** Embedded font */
    private TTFSubFont subFont;

    /** Map from unicode to cid */
    private CMAPEncodingEntry unicode2cidMap;

    public PDUnicodeCIDFontType2Font(String fontPath, boolean isSerif) throws IOException
    {
        super();
        this.fontPath = fontPath;
        this.isSerif = isSerif;
        this.prefix = getPrefix();

        boolean isTTC = false;
        int fontIndex = -1;
        int ttcPos = fontPath.toLowerCase().indexOf("ttc");
        if (ttcPos > -1)
        {
            isTTC = true;
            fontIndex = Integer.valueOf(fontPath.substring(ttcPos+4));
            fontPath = fontPath.substring(0, ttcPos+3);
        }

        RAFDataStream raf = new RAFDataStream(new File(fontPath), "r");
        CIDFontType2Parser parser = new CIDFontType2Parser(false);
        TrueTypeFont ttf = null;
        if (isTTC)
        {
            parser.parseTTC(raf);
            ttf = parser.parseTTF(raf, fontIndex);
        }
        else
        {
            ttf = parser.parseTTF(raf);
        }

        subFont = new TTFSubFont(ttf, prefix);

        PDFontDescriptorDictionary fd = new PDFontDescriptorDictionary();
        loadDescriptorDictionary(ttf, fd);

        PDCIDFontType2Font desendant = new PDCIDFontType2Font();
        desendant.setBaseFont(bfname);
        desendant.setCIDSystemInfo(PDCIDSystemInfo.ADOBE_IDENTITY_0);
        desendant.setFontDescriptor(fd);
        desendant.setDefaultWidth(defaultW);

        setBaseFont(bfname);
        setEncoding(COSName.IDENTITY_H);
        setDescendantFont(desendant);

    }

    public void reloadFont(PDDocument document)  throws IOException
    {
        int[] used = getUsedCodes();
        for (int cp : used)
        {
            subFont.addCharCode(cp);
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        subFont.writeToStream(bos);
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        PDStream fontStream = new PDStream(document, bis);
        fontStream.addCompression();
        fontStream.getStream().setInt(COSName.LENGTH1, fontStream.getByteArray().length);

        PDCIDFontType2Font descendantFont = (PDCIDFontType2Font) getDescendantFont();
        PDFontDescriptorDictionary fd = (PDFontDescriptorDictionary) descendantFont.getFontDescriptor();
        fd.resetFontFile2(fontStream);


        InputStream stream = fontStream.createInputStream();
        TrueTypeFont ttf = null;
        try
        {
            TTFParser parser = new TTFParser();
            ttf = parser.parseTTF(stream);

            CMAPTable cmapTable = ttf.getCMAP();
            CMAPEncodingEntry[] cmaps = cmapTable.getCmaps();
            CMAPEncodingEntry gidMap = null;

            for (int i = 0; i < cmaps.length; i++)
            {
                if (cmaps[i].getPlatformId() == CMAPTable.PLATFORM_WINDOWS)
                {
                    int platformEncoding = cmaps[i].getPlatformEncodingId();
                    if (CMAPTable.ENCODING_UNICODE == platformEncoding)
                    {
                        gidMap = cmaps[i];
                        break;
                    }
                }
            }

            Object[] unicode2cid = new Object[used.length];
            TreeSet<Integer> gidset = new TreeSet<Integer>();
            Map<Integer, Integer> cid2gid = new HashMap<Integer, Integer>();
            Map<Integer, Integer> gid2cid = new HashMap<Integer, Integer>();
            int maxcid = Integer.MIN_VALUE;
            for (int i=0, len=used.length; i<len; i++)
            {
                int unicode = used[i];
                int cid = unicode2cidMap.getGlyphId(unicode);
                int gid = gidMap.getGlyphId(unicode);
                int[] pair = {unicode, cid};
                unicode2cid[i] = pair;
                gidset.add(gid);
                cid2gid.put(cid, gid);
                gid2cid.put(gid, cid);
                if (cid > maxcid)
                {
                    maxcid = cid;
                }
            }

            int[] gids = new int[used.length];
            int j = 0;
            for (int gid : gidset)
            {
                gids[j++] = gid;
            }

            HeaderTable header = ttf.getHeader();
            float scaling = 1000f / header.getUnitsPerEm();

            HorizontalMetricsTable hmtx = ttf.getHorizontalMetrics();
            int[] widths = hmtx.getAdvanceWidth();
            wArray = new COSArray();
            COSArray inner;
            for (int i=0, len=gids.length; i<len; i++)
            {
                wArray.add(COSInteger.get(gid2cid.get(gids[i])));
                inner = new COSArray();
                inner.add(COSInteger.get(Math.round(widths[gids[i]] * scaling)));
                wArray.add(inner); 
            }
            descendantFont.resetFontWidths(wArray);
            descendantFont.resetCID2GID(getCIDToGID(document, maxcid, cid2gid));

            this.resetToUnicode(getToUnicode(document, unicode2cid));
        }
        finally
        {
            if (ttf != null)
            {
                ttf.close();
            }
        }

    }

    private void loadDescriptorDictionary(TrueTypeFont ttf, PDFontDescriptorDictionary fd) throws IOException
    {
        NamingTable naming = ttf.getNaming();
        List<NameRecord> records = naming.getNameRecords();
        for (int i = 0; i < records.size(); i++)
        {
            NameRecord nr = records.get(i);
            if (nr.getNameId() == NameRecord.NAME_POSTSCRIPT_NAME)
            {
                bfname = nr.getString();
                fd.setFontName(bfname);
            }
        }

        OS2WindowsMetricsTable os2 = ttf.getOS2Windows();
        boolean isSymbolic = false;
        switch (os2.getFamilyClass())
        {
        case OS2WindowsMetricsTable.FAMILY_CLASS_SYMBOLIC:
            isSymbolic = true;
            break;
        case OS2WindowsMetricsTable.FAMILY_CLASS_SCRIPTS:
            fd.setScript(true);
            break;
        case OS2WindowsMetricsTable.FAMILY_CLASS_CLAREDON_SERIFS:
        case OS2WindowsMetricsTable.FAMILY_CLASS_FREEFORM_SERIFS:
        case OS2WindowsMetricsTable.FAMILY_CLASS_MODERN_SERIFS:
        case OS2WindowsMetricsTable.FAMILY_CLASS_OLDSTYLE_SERIFS:
        case OS2WindowsMetricsTable.FAMILY_CLASS_SLAB_SERIFS:
            isSerif = true;
            break;
        default:
            // do nothing
        }
        switch (os2.getWidthClass())
        {
            case OS2WindowsMetricsTable.WIDTH_CLASS_ULTRA_CONDENSED:
                fd.setFontStretch("UltraCondensed");
                break;
            case OS2WindowsMetricsTable.WIDTH_CLASS_EXTRA_CONDENSED:
                fd.setFontStretch("ExtraCondensed");
                break;
            case OS2WindowsMetricsTable.WIDTH_CLASS_CONDENSED:
                fd.setFontStretch("Condensed");
                break;
            case OS2WindowsMetricsTable.WIDTH_CLASS_SEMI_CONDENSED:
                fd.setFontStretch("SemiCondensed");
                break;
            case OS2WindowsMetricsTable.WIDTH_CLASS_MEDIUM:
                fd.setFontStretch("Normal");
                break;
            case OS2WindowsMetricsTable.WIDTH_CLASS_SEMI_EXPANDED:
                fd.setFontStretch("SemiExpanded");
                break;
            case OS2WindowsMetricsTable.WIDTH_CLASS_EXPANDED:
                fd.setFontStretch("Expanded");
                break;
            case OS2WindowsMetricsTable.WIDTH_CLASS_EXTRA_EXPANDED:
                fd.setFontStretch("ExtraExpanded");
                break;
            case OS2WindowsMetricsTable.WIDTH_CLASS_ULTRA_EXPANDED:
                fd.setFontStretch("UltraExpanded");
                break;
            default:
                // do nothing
        }
        fd.setFontWeight(os2.getWeightClass());
        fd.setSymbolic(isSymbolic);
        fd.setNonSymbolic(!isSymbolic);
        fd.setSerif(isSerif);

        HeaderTable header = ttf.getHeader();
        PDRectangle rect = new PDRectangle();
        float scaling = 1000f / header.getUnitsPerEm();
        rect.setLowerLeftX(Math.round(header.getXMin() * scaling));
        rect.setLowerLeftY(Math.round(header.getYMin() * scaling));
        rect.setUpperRightX(Math.round(header.getXMax() * scaling));
        rect.setUpperRightY(Math.round(header.getYMax() * scaling));
        fd.setFontBoundingBox(rect);

        HorizontalHeaderTable hHeader = ttf.getHorizontalHeader();
        fd.setAscent(Math.round(hHeader.getAscender() * scaling));
        fd.setDescent(Math.round(hHeader.getDescender() * scaling));

        GlyphTable glyphTable = ttf.getGlyph();
        GlyphData[] glyphs = glyphTable.getGlyphs();

        PostScriptTable ps = ttf.getPostScript();
        fd.setFixedPitch(ps.getIsFixedPitch() > 0);
        fd.setItalicAngle(ps.getItalicAngle());

        String[] names = ps.getGlyphNames();

        if (names != null)
        {
            for (int i = 0; i < names.length; i++)
            {
                // if we have a capital H then use that, otherwise use the
                // tallest letter
                if (names[i].equals("H"))
                {
                    fd.setCapHeight(Math.round(glyphs[i].getBoundingBox().getUpperRightY() / scaling));
                }
                if (names[i].equals("x"))
                {
                    fd.setXHeight(Math.round(glyphs[i].getBoundingBox().getUpperRightY() / scaling));
                }
            }
        }

        // hmm there does not seem to be a clear definition for StemV,
        // this is close enough and I am told it doesn't usually get used.
        fd.setStemV(Math.round(fd.getFontBoundingBox().getWidth() * .13f));

        fd.setFlags(fd.getFlags());

        CMAPTable cmapTable = ttf.getCMAP();
        CMAPEncodingEntry[] cmaps = cmapTable.getCmaps();

        for (int i = 0; i < cmaps.length; i++)
        {
            if (cmaps[i].getPlatformId() == CMAPTable.PLATFORM_WINDOWS)
            {
                int platformEncoding = cmaps[i].getPlatformEncodingId();
                if (CMAPTable.ENCODING_UNICODE == platformEncoding)
                {
                    unicode2cidMap = cmaps[i];
                    break;
                }
            }
        }

        HorizontalMetricsTable hmtx = ttf.getHorizontalMetrics();
        int[] widths = hmtx.getAdvanceWidth();
        defaultW = Math.round(widths[0] * scaling);
    }

    public int getCID(int unicode)
    {
        return unicode2cidMap.getGlyphId(unicode);
    }

    private String getPrefix()
    {
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<6; i++)
        {
            sb.append((char)('A'+rnd.nextInt(26)));
        }
        return sb.toString() + "+";
    }

    public void setUsedCodes(String text)
    {
        for (int i = 0, len=text.length(), cp; i < len; i += Character.charCount(cp)) 
        {
            cp = text.codePointAt(i);
            usedCodes.add(cp);
        }
    }

    private int[] getUsedCodes()
    {
        int[] used = new int[usedCodes.size()];
        int j = 0;
        for (Integer code : usedCodes)
        {
            used[j++] = code.intValue();
        }

        return used;
    }

    private PDStream getToUnicode(PDDocument document, Object[] unicode2cid) throws IOException
    {
        StringBuilder sb = new StringBuilder(
            "/CIDInit /ProcSet findresource begin\n" +
            "12 dict begin\n" +
            "begincmap\n" +
            "/CIDSystemInfo\n" +
            "<< /Registry (TT1+0)\n" +
            "/Ordering (T42UV)\n" +
            "/Supplement 0\n" +
            ">> def\n" +
            "/CMapName /TT1+0 def\n" +
            "/CMapType 2 def\n" +
            "1 begincodespacerange\n" +
            "<0000> <FFFF>\n" +
            "endcodespacerange\n"
        );
        int size = 0;
        for (int i = 0; i < unicode2cid.length; ++i)
        {
            if (size == 0) 
            {
                if (i != 0) {
                    sb.append("endbfchar\n");
                }
                size = Math.min(100, unicode2cid.length - i);
                sb.append(size).append(" beginbfchar\n");
            }
            --size;
            int[] pair = (int[])unicode2cid[i];
            sb.append(StringUtil.toHex(pair[1])).append(" ").append(StringUtil.toHex(pair[0])).append('\n');
        }
        sb.append(
            "endbfchar\n" +
            "endcmap\n" +
            "CMapName currentdict /CMap defineresource pop\n" +
            "end end\n"
        );

        ByteArrayInputStream bis = new ByteArrayInputStream(sb.toString().getBytes(Charset.forName("US-ASCII")));
        PDStream toUnicode = new PDStream(document, bis);
        toUnicode.addCompression();
        return toUnicode;
    }

    private COSBase getCIDToGID(PDDocument document, int maxcid, Map<Integer, Integer> cid2gid) throws IOException
    {
        byte[] bs = new byte[2*(maxcid+1)];
        for (int i=0; i<=maxcid; i++)
        {
            if (cid2gid.containsKey(i))
            {
                int gid = cid2gid.get(i);
                bs[i*2]   = (byte)((gid >> 8) & 0xFF);
                bs[i*2+1] = (byte)(gid & 0xFF);
            }
            else
            {
                bs[i*2]   = (byte)0;
                bs[i*2+1] = (byte)0;
            }
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(bs);
        PDStream cidToGID = new PDStream(document, bis);
        cidToGID.addCompression();
        return cidToGID.getCOSObject();
    }


}
