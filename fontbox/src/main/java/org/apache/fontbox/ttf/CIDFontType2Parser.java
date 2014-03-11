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
package org.apache.fontbox.ttf;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;

public class CIDFontType2Parser extends AbstractTTFParser
{
    /** Number of fonts in ttc file: ttc file only */
    private int numFonts;

    /** Offest of each font in ttc file: ttc file only */
    private long[] offsetTable;

	public CIDFontType2Parser() {
		super(false);
	}

	public CIDFontType2Parser(boolean isEmbedded) {
		super(isEmbedded);
	}

    /**
     * Parse a TTC file and set the offset for the each font
     * 
     * @param raf The TTC file.
     * @throws IOException If there is an error parsing the true type font.
     */
    public void parseTTC(String ttcFile) throws IOException
    {
        RAFDataStream raf = new RAFDataStream(ttcFile, "r");
        parseTTC(raf);
    }

    /**
     * Parse a TTC file and set the offset for the each font
     * 
     * @param raf The TTC file.
     * @throws IOException If there is an error parsing the true type font.
     */
    public void parseTTC(File ttcFile) throws IOException
    {
        RAFDataStream raf = new RAFDataStream(ttcFile, "r");
        parseTTC(raf);
    }

    /**
     * Parse a TTC file and set the offset for the each font
     * 
     * @param raf The TTC file.
     * @throws IOException If there is an error parsing the true type font.
     */
    public void parseTTC(InputStream ttcData) throws IOException
    {
        parseTTC(new MemoryTTFDataStream(ttcData));
    }

    /**
     * Parse a TTC file and set the offset for the each font
     * 
     * @param raf The TTC file.
     * @throws IOException If there is an error parsing the true type font.
     */
    public void parseTTC(TTFDataStream raf) throws IOException
    {
        String ttcTag = raf.readString(4);
        if (!"ttcf".equals(ttcTag))
        {
            throw new IOException("This is not ttc file");
        }
        float version = raf.read32Fixed();
        numFonts = (int)raf.readUnsignedInt();
        offsetTable = new long[numFonts];
        for (int i=0; i<numFonts; i++)
        {
            offsetTable[i] = raf.readUnsignedInt();
        }

        if (version > 1.0)
        {
            raf.readUnsignedInt();  // ulDsigTag
            raf.readUnsignedInt();  // ulDsigLength
            raf.readUnsignedInt();  // ulDsigLength
        }
    }


    /**
     * Parse a file and get a true type font.
     * 
     * @param raf The TTC file.
     * @param index The Font index in the TTC file
     * @return A true type font.
     * @throws IOException If there is an error parsing the true type font.
     */
    public TrueTypeFont parseTTF(TTFDataStream raf, int index) throws IOException
    {
        if (!isValidIndex(index))
        {
            throw new IOException("Invalid font index "+index);
        }

        TrueTypeFont font = new TrueTypeFont(raf);
        raf.seek(offsetTable[index]);

        font.setVersion(raf.read32Fixed());
        int numberOfTables = raf.readUnsignedShort();
        int searchRange = raf.readUnsignedShort();
        int entrySelector = raf.readUnsignedShort();
        int rangeShift = raf.readUnsignedShort();
        for (int i = 0; i < numberOfTables; i++)
        {
            TTFTable table = super.readTableDirectory(raf);
            font.addTable(table);
        }

        // need to initialize a couple tables in a certain order
        parseTables(font, raf);

        return font;
    }

    private boolean isValidIndex(int index)
    {
        return (0 <= index && index <= numFonts);
    }

}
