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

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fontbox.util.BoundingBox;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDCIDFontType0Font;
import org.apache.pdfbox.pdmodel.font.PDCIDSystemInfo;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptorDictionary;
import org.apache.pdfbox.util.ResourceLoader;

/**
 * This is implementation for the CJK Fonts.
 *
 * @author Keiji Suzuki</a>
 * 
 */
public class PDCJKCIDFontType0Font extends PDType0Font
{
    /**
     * Log instance.
     */
    private static final Log LOG = LogFactory.getLog(PDCJKCIDFontType0Font.class);

    private static final Map<String, String> SUPPORTED_FONTS = new HashMap<String, String>();

    static
    {
        loadSupportedFonts("org/apache/pdfbox/resources/cjk/supported_fonts.properties");
    }

    private static final Map<String, PDFont> CACHED_FONTS = new HashMap<String, PDFont>();

    public static PDFont getFont(String fontName) throws IOException
    {
        if (!SUPPORTED_FONTS.containsKey(fontName))
        {
            throw new IOException(fontName+" is not supported");
        }

        PDFont font = null;
        if (CACHED_FONTS.containsKey(fontName))
        {
            font = CACHED_FONTS.get(fontName);
        }
        else
        {
            font = makeFont(fontName);
            CACHED_FONTS.put(fontName, font);
        }

        return font;
    }

    private static PDFont makeFont(String fontName) throws IOException
    {
        String path = String.format("org/apache/pdfbox/resources/cjk/%s.properties", fontName);
        Properties fontProperties = ResourceLoader.loadProperties(path, false);
        if (fontProperties == null)
        {
            throw new MissingResourceException("Font properties not found: " + path, PDCJKCIDFontType0Font.class.getName(), path);
        }

        PDFontDescriptorDictionary fd = new PDFontDescriptorDictionary();
        fd.setFontName(fontName);
        fd.setFlags(Integer.valueOf(fontProperties.getProperty("Flags")).intValue());

        String fontBBox = fontProperties.getProperty("FontBBox");
        String[] bb = fontBBox.substring(1, fontBBox.length() - 2).split(" ");
        BoundingBox bbox = new BoundingBox();
        bbox.setLowerLeftX(Integer.valueOf(bb[0]).intValue());
        bbox.setLowerLeftY(Integer.valueOf(bb[1]).intValue());
        bbox.setUpperRightX(Integer.valueOf(bb[2]).intValue());
        bbox.setUpperRightY(Integer.valueOf(bb[3]).intValue());
        fd.setFontBoundingBox(new PDRectangle(bbox));

        fd.setItalicAngle(Integer.valueOf(fontProperties.getProperty("ItalicAngle")).intValue());
        fd.setAscent(Integer.valueOf(fontProperties.getProperty("Ascent")).intValue());
        fd.setDescent(Integer.valueOf(fontProperties.getProperty("Descent")).intValue());
        fd.setCapHeight(Integer.valueOf(fontProperties.getProperty("CapHeight")).intValue());
        fd.setStemV(Integer.valueOf(fontProperties.getProperty("StemV")).intValue());

        PDCIDFontType0Font cid = new PDCIDFontType0Font();
        cid.setBaseFont(fontName);
        cid.setCIDSystemInfo(new PDCIDSystemInfo(fontProperties.getProperty("CIDSystemInfo")));
        cid.setFontDescriptor(fd);
        cid.setDefaultWidth(Integer.valueOf(fontProperties.getProperty("DW")).intValue());
        cid.setFontWidths(getWidths(fontProperties.getProperty("W")));

        PDType0Font font = new PDType0Font();
        font.setBaseFont(fontName);
        font.setEncoding(COSName.getPDFName(fontProperties.getProperty("Encoding")));
        font.setDescendantFont(cid);

        return font;
    }

    private static void loadSupportedFonts(String location)
    {
        try
        {
            Properties supportedProperties = ResourceLoader.loadProperties(location, false);
            if (supportedProperties == null)
            {
                throw new MissingResourceException("Supported fonts properties not found: " + location, PDCJKCIDFontType0Font.class.getName(), location);
            }
            Enumeration<?> names = supportedProperties.propertyNames();
            for (Object name : Collections.list(names))
            {
                String fontName = name.toString();
                String fontType = supportedProperties.getProperty(fontName);
                SUPPORTED_FONTS.put(fontName, fontType.toLowerCase());
            }
        }
        catch (IOException io)
        {
            LOG.error("error while reading the supported fonts property file.", io);
        }
    }

    private static COSArray getWidths(String wString) throws IOException
    {
        COSArray outer = new COSArray();
        COSArray inner = null;

        StringTokenizer st = new StringTokenizer(wString);
        if (st.countTokens() % 2 != 0)
        {
            throw new IOException("wString is invalid");
        }
        else if (st.countTokens() == 2)
        {
            outer.add(COSInteger.get(Long.parseLong(st.nextToken())));
            inner = new COSArray();
            inner.add(COSInteger.get(Long.parseLong(st.nextToken())));
            outer.add(inner);
            return outer;
        }

        final int FIRST = 0;
        final int BRACKET = 1;
        final int SERIAL = 2;

        long lastCid   = Long.parseLong(st.nextToken());
        long lastValue = Long.parseLong(st.nextToken());
        outer.add(COSInteger.get(lastCid));
        int state = FIRST;

        while (st.hasMoreTokens())
        {
            long cid   = Long.parseLong(st.nextToken());
            long value = Long.parseLong(st.nextToken());

            switch (state)
            {
                case FIRST:
                {
                    if (cid == lastCid + 1 && value == lastValue) 
                    {
                        state = SERIAL;
                    }
                    else if (cid == lastCid + 1) 
                    {
                        state = BRACKET;
                        inner = new COSArray();
                        inner.add(COSInteger.get(lastValue));
                    }
                    else 
                    {
                        inner = new COSArray();
                        inner.add(COSInteger.get(lastValue));
                        outer.add(inner);
                        outer.add(COSInteger.get(cid));
                    }
                    break;
                }
                case BRACKET:
                {
                    if (cid == lastCid + 1 && value == lastValue)
                    {
                        state = SERIAL;
                        outer.add(inner);
                        outer.add(COSInteger.get(lastCid));
                    }
                    else if (cid == lastCid + 1)
                    {
                        inner.add(COSInteger.get(lastValue));
                    }
                    else 
                    {
                        state = FIRST;
                        inner.add(COSInteger.get(lastCid));
                        outer.add(inner);
                        outer.add(COSInteger.get(cid));
                    }
                    break;
                }
                case SERIAL:
                {
                    if (cid != lastCid + 1 || value != lastValue)
                    {
                        outer.add(COSInteger.get(lastCid));
                        outer.add(COSInteger.get(lastValue));
                        outer.add(COSInteger.get(cid));
                        state = FIRST;
                    }
                    break;
                }
            }
            lastValue = value;
            lastCid = cid;
        }
        switch (state) {
            case FIRST: {
                inner = new COSArray();
                inner.add(COSInteger.get(lastValue));
                outer.add(inner);
                break;
            }
            case BRACKET: {
                inner.add(COSInteger.get(lastValue));
                outer.add(inner);
                break;
            }
            case SERIAL: {
                outer.add(COSInteger.get(lastCid));
                outer.add(COSInteger.get(lastValue));
                break;
            }
        }

        return outer;
    }

}
