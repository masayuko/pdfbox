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
package org.apache.pdfbox.util;

import java.io.UnsupportedEncodingException;

public class StringUtil
{
    /**
     * Converts a string to it ISO-8859-1 byte sequence
     *
     * It is an workaround for variable initialisations outside of functions.
     */ 
    public static byte[] getBytes(String s)
    {
        try
            {
                return s.getBytes("ISO-8859-1");
            }
        catch(UnsupportedEncodingException e)
            {
                throw new RuntimeException("Unsupported Encoding", e);
            }
    }

    public static String toHex4(int n)
    {
        String s = "0000" + Integer.toHexString(n);
        return s.substring(s.length() - 4);
    }

    public static String toHex(int n)
    {
        if (n < 0x10000)
        {
            return "<" + toHex4(n) + ">";
        }

        n -= 0x10000;
        int high = n / 0x400 + 0xd800;
        int low = n % 0x400 + 0xdc00;
        return "[<" + toHex4(high) + toHex4(low) + ">]";
    }

}
