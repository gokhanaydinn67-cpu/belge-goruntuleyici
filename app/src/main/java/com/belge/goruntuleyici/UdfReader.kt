package com.belge.goruntuleyici

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.util.zip.ZipFile

object UdfReader {

    fun read(path: String): String {
        val zip = ZipFile(path)
        try {
            val e = zip.getEntry("content.xml")
                ?: return "UDF içinde content.xml bulunamadı"
            val p = Xml.newPullParser()
            p.setInput(zip.getInputStream(e), null)
            var ev = p.eventType
            while (ev != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG && p.name == "content") {
                    val text = p.nextText()
                    return text.replace("\t", "    ")
                }
                ev = p.next()
            }
            return "UDF içinde metin bulunamadı"
        } finally {
            zip.close()
        }
    }
}
