package org.apache.pdfbox.pdmodel.font;

import java.io.IOException;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;

public class PDCIDSystemInfo
{
    private COSDictionary cidsysteminfo;

    public static final PDCIDSystemInfo ADOBE_IDENTITY_0 = 
        new PDCIDSystemInfo("Adobe", "Identity", 0);

    public PDCIDSystemInfo(String info)
    {
        String[] parts = info.split("-");
        cidsysteminfo = new COSDictionary();
        cidsysteminfo.setString(COSName.REGISTRY, parts[0]);
        cidsysteminfo.setString(COSName.ORDERING, parts[1]);
        cidsysteminfo.setInt(COSName.SUPPLEMENT, Integer.valueOf(parts[2]));
    }

    public PDCIDSystemInfo(String registry, String ordering, int supplement)
    {
        cidsysteminfo = new COSDictionary();
        cidsysteminfo.setString(COSName.REGISTRY, registry);
        cidsysteminfo.setString(COSName.ORDERING, ordering);
        cidsysteminfo.setInt(COSName.SUPPLEMENT, supplement);
    }

    public COSDictionary getCIDSystemInfo()
    {
        return cidsysteminfo;
    }
}
