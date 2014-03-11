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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;

/**
 * This is implementation of the Type0 Font. See <a
 * href="https://issues.apache.org/jira/browse/PDFBOX-605">PDFBOX-605</a> for the related improvement issue.
 * 
 * @author <a href="mailto:ben@benlitchfield.com">Ben Litchfield</a>
 */
public class PDType0Font extends PDSimpleFont
{

    /**
     * Log instance.
     */
    private static final Log LOG = LogFactory.getLog(PDType0Font.class);

    private COSArray descendantFontArray;
    private PDFont descendantFont;
    private COSDictionary descendantFontDictionary;

    /**
     * Constructor.
     */
    public PDType0Font()
    {
        super();
        font.setItem(COSName.SUBTYPE, COSName.TYPE0);
    }

    /**
     * Constructor.
     * 
     * @param fontDictionary The font dictionary according to the PDF specification.
     */
    public PDType0Font(COSDictionary fontDictionary)
    {
        super(fontDictionary);
        descendantFontDictionary = (COSDictionary) getDescendantFonts().getObject(0);
        if (descendantFontDictionary != null)
        {
            try
            {
                descendantFont = PDFontFactory.createFont(descendantFontDictionary);
            }
            catch (IOException exception)
            {
                LOG.error("Error while creating the descendant font!");
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PDRectangle getFontBoundingBox() throws IOException
    {
        return descendantFont.getFontDescriptor().getFontBoundingBox();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getFontWidth(byte[] c, int offset, int length) throws IOException
    {
        if (descendantFont instanceof PDCIDFontType2Font)
        {
            // a suitable mapping is needed to address the correct width value
            PDCIDFontType2Font cidType2Font = (PDCIDFontType2Font) descendantFont;
            int code = getCodeFromArray(c, offset, length);
            if (cidType2Font.hasIdentityCIDToGIDMap() || cidType2Font.hasCIDToGIDMap())
            {
                return cidType2Font.getFontWidth(code);
            }
            else if (getCMap() != null)
            {
                String mappedString = getCMap().lookup(code, length);
                if (mappedString != null)
                {
                    return cidType2Font.getFontWidth(mappedString.codePointAt(0));
                }
            }
        }
        return descendantFont.getFontWidth(c, offset, length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getFontHeight(byte[] c, int offset, int length) throws IOException
    {
        return descendantFont.getFontHeight(c, offset, length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getAverageFontWidth() throws IOException
    {
        return descendantFont.getAverageFontWidth();
    }

    private COSArray getDescendantFonts()
    {
        if (descendantFontArray == null)
        {
            descendantFontArray = (COSArray) font.getDictionaryObject(COSName.DESCENDANT_FONTS);
        }
        return descendantFontArray;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getFontWidth(int charCode)
    {
        return descendantFont.getFontWidth(charCode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String encode(byte[] c, int offset, int length) throws IOException
    {
        String retval = null;
        if (hasToUnicode())
        {
            retval = super.encode(c, offset, length);
        }

        if (retval == null)
        {
            int result = cmap.lookupCID(c, offset, length);
            if (result != -1)
            {
                retval = descendantFont.cmapEncoding(result, 2, true, null);
            }
        }
        return retval;
    }

    /**
     * 
     * Provides the descendant font.
     * 
     * @return the descendant font.
     * 
     */
    public PDFont getDescendantFont()
    {
        return descendantFont;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getStringWidth(String string) throws IOException
    {
        byte[] bytes = string.getBytes("UTF-16BE");
        float totalWidth = 0f;
        for (int i=0; i<bytes.length; i=i+2)
        {
            totalWidth += getFontWidth(bytes, i, 2);
        }
        return totalWidth;
    }

    public void setDescendantFont(PDCIDFont cidfont)
    {
        descendantFont = cidfont;
        descendantFontArray = new COSArray();
        descendantFontArray.add(descendantFont);
        font.setItem(COSName.DESCENDANT_FONTS, descendantFontArray);
    }

    public void setEncoding(COSName encoding)
    {
        font.setItem(COSName.ENCODING, encoding);
    }

    public void setToUnicode(PDStream toUnicode)
    {
        font.setItem(COSName.TO_UNICODE, toUnicode);
    }

    public void resetToUnicode(PDStream toUnicode)
    {
        if (font.getItem(COSName.TO_UNICODE) != null)
        {
            font.removeItem(COSName.TO_UNICODE);
        }
        font.setItem(COSName.TO_UNICODE, toUnicode);
    }

}
