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
public class PDType0CJKFont extends PDType0Font
{
    /**
     * Log instance.
     */
    private static final Log LOG = LogFactory.getLog(PDType0CJKFont.class);

    private static final Map<String, String> SUPPORTED_FONTS = new HashMap<String, String>();

    static
    {
        loadSupportedFonts("org/apache/pdfbox/resources/cjk/supported_fonts.properties");
    }

    public static final PDType0CJKFont JAPANESE_GOTHIC = new PDType0CJKFont("KozGoPr6N-Medium");

    public static final PDType0CJKFont JAPANESE_MINCHO = new PDType0CJKFont("KozMinPr6N-Regular");

    public static final PDType0CJKFont SIMPLIFIED_CHINESE_GOTHIC = new PDType0CJKFont("AdobeHeitiStd-Regular");

    public static final PDType0CJKFont SIMPLIFIED_CHINESE_MINCHO = new PDType0CJKFont("AdobeSongStd-Light");

    public static final PDType0CJKFont TRADITIONAL_CHINESE_GOTHIC = new PDType0CJKFont("AdobeFanHeitiStd-Bold");

    public static final PDType0CJKFont TRADITIONAL_CHINESE_MINCHO = new PDType0CJKFont("AdobeMingStd-Light");

    public static final PDType0CJKFont KOREAN_GOTHIC = new PDType0CJKFont("AdobeGothicStd-Bold");

    public static final PDType0CJKFont KOREAN_MINCHO = new PDType0CJKFont("AdobeMyungjoStd-Medium");


    public PDType0CJKFont(String fontName)
    {
        super();

        try
        {
            if (!SUPPORTED_FONTS.containsKey(fontName))
            {
                throw new IOException(fontName+" is not supported");
            }
    
            String path = String.format("org/apache/pdfbox/resources/cjk/%s.properties", fontName);
            Properties fontProperties = ResourceLoader.loadProperties(path, false);
            if (fontProperties == null)
            {
                throw new MissingResourceException("Font properties not found: " + path, PDType0CJKFont.class.getName(), path);
            }
    
            PDFontDescriptorDictionary fd = new PDFontDescriptorDictionary();
            fd.setFontName(fontName);
            fd.setFlags(Integer.valueOf(fontProperties.getProperty("Flags")).intValue());
    
            String fontBBox = fontProperties.getProperty("FontBBox");
            String[] bb = fontBBox.substring(1, fontBBox.length() - 1).split(" ");
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
    
            PDCIDFont cid = new PDCIDFontType0Font();
            cid.setBaseFont(fontName);
            cid.setCIDSystemInfo(new PDCIDSystemInfo(fontProperties.getProperty("CIDSystemInfo")));
            cid.setFontDescriptor(fd);
            cid.setDefaultWidth(Integer.valueOf(fontProperties.getProperty("DW")).intValue());
            cid.setFontWidths(fontProperties.getProperty("W"));
    
            setBaseFont(fontName);
            setEncoding(COSName.getPDFName(fontProperties.getProperty("Encoding")));
            setDescendantFont(cid);
        }
        catch (IOException e)
        {
            LOG.error("Something went wrong when constructing PDType0CJKFont", e);
        }
    }

    private static void loadSupportedFonts(String location)
    {
        try
        {
            Properties supportedProperties = ResourceLoader.loadProperties(location, false);
            if (supportedProperties == null)
            {
                throw new MissingResourceException("Supported fonts properties not found: " + location, PDType0CJKFont.class.getName(), location);
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

}
