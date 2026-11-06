package com.example.fittrackerpro.utils;

import android.content.Context;
import android.content.res.XmlResourceParser;

import com.example.fittrackerpro.R;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Day 8A: Demonstrates XML parsing with Android's XmlPullParser.
 * Reads bundled fitness tips from res/xml/fitness_tips.xml and displays them on the Home dashboard.
 */
public class TipsXmlParser {

    public static class Tip {
        public final String category;
        public final String title;
        public final String body;

        public Tip(String category, String title, String body) {
            this.category = category;
            this.title = title;
            this.body = body;
        }
    }

    private TipsXmlParser() {
    }

    public static List<Tip> loadTips(Context context) {
        List<Tip> list = new ArrayList<>();
        XmlResourceParser parser = null;
        try {
            parser = context.getResources().getXml(R.xml.fitness_tips);
            String category = null;
            String title = null;
            StringBuilder textBuilder = new StringBuilder();
            boolean insideTip = false;

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if ("tip".equals(parser.getName())) {
                        insideTip = true;
                        category = parser.getAttributeValue(null, "category");
                        title = parser.getAttributeValue(null, "title");
                        textBuilder.setLength(0);
                    }
                } else if (eventType == XmlPullParser.TEXT) {
                    if (insideTip && parser.getText() != null) {
                        textBuilder.append(parser.getText());
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if ("tip".equals(parser.getName()) && insideTip) {
                        String body = textBuilder.toString().trim().replaceAll("\\s+", " ");
                        list.add(new Tip(
                                category != null ? category : "Tip",
                                title != null ? title : "Fitness Tip",
                                body));
                        insideTip = false;
                    }
                }
                eventType = parser.next();
            }
        } catch (Exception ignored) {
            // Fallback tip if XML resource cannot be read
        } finally {
            if (parser != null) {
                parser.close();
            }
        }

        if (list.isEmpty()) {
            list.add(new Tip("Consistency", "Small habits add up",
                    "Log your workouts, meals, and water daily to track steady progress."));
        }
        return list;
    }
}
