/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014 Benoit 'BoD' Lubek (BoD@JRAF.org) //TODO <- replace with *your* name/email
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jraf.android.networkmonitor.app.export.kml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Map;

import android.text.TextUtils;

/**
 * Writes a KML document with styles and placemarks.
 */
class KMLWriter extends PrintWriter {
    private static final String STYLEMAP_RED = "#stylemap_red";
    private static final String STYLEMAP_GREEN = "#stylemap_green";
    private static final String STYLEMAP_YELLOW = "#stylemap_yellow";
    private final KMLStyle mKmlStyle;
    private final String mEmptyLabel;

    public KMLWriter(File file, KMLStyle kmlStyle, String emptyLabel) throws FileNotFoundException {
        super(file);
        mKmlStyle = kmlStyle;
        mEmptyLabel = emptyLabel;
    }

    public void writeHeader() {
        println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        println("<kml xmlns=\"http://earth.google.com/kml/2.1\">");
        println("  <Document>");
        writeStyles();
    }

    /**
     * Write a single Placemark
     * 
     * @values map of field name to value
     */
    public void writePlacemark(String name, Map<String, String> values, String latitude, String longitude, String timestamp) {
        println("    <Placemark>");
        writePlacemarkName(name);
        writePlacemarkCoordinates(longitude, latitude);
        writePlacemarkExtendedData(values);
        writePlacemarkTimestamp(timestamp);
        writePlacemarkStyleUrl(name, values);
        println("    </Placemark>");
        flush();
    }

    public void writeFooter() {
        println("  </Document>");
        println("</kml>");
        flush();
    }

    /**
     * To make the output file a bit smaller, we use KML styles: one style for pass (green), one for slow (yellow) and one for fail (red).
     */
    private void writeStyles() {
        // KML colors are of the format aabbggrr: https://developers.google.com/kml/documentation/kmlreference#color
        writeStyle("red", "ff0000ff");
        writeStyle("green", "ff00ff00");
        writeStyle("yellow", "ff00ffff");
    }

    /**
     * Write the style xml for the given color.
     * 
     * @param colorName the name of the color: used for the name of the kml style and stylemap
     * @param colorCode the aabbggrr color code for this color.
     */
    private void writeStyle(String colorName, String colorCode) {
        println("    <StyleMap id=\"stylemap_" + colorName + "\">");
        // Write the style map
        String[] keys = new String[] { "normal", "highlight" };
        for (String key : keys) {
            println("      <Pair>");
            println("        <key>" + key + "</key>");
            println("        <styleUrl>#style_" + colorName + "</styleUrl>");
            println("      </Pair>");
        }
        println("    </StyleMap>");

        // Write the style
        println("    <Style id=\"style_" + colorName + "\">");

        // The icon style
        println("      <IconStyle>");
        print("        <color>");
        print(colorCode);
        println("</color>");
        println("        <Icon>");
        print("          <href>");
        String iconUrl = "http://maps.google.com/mapfiles/kml/shapes/placemark_circle.png";
        print(iconUrl);
        println("</href>");
        println("        </Icon>");
        println("      </IconStyle>");

        // The label style
        println("      <LabelStyle>");
        println("        <scale>1.0</scale>");
        println("      </LabelStyle>");
        println("    </Style>");
    }

    /**
     * Write the label of this placemark. This is the value for the column the user chose to export.
     */
    private void writePlacemarkName(String label) {
        print("      <name>");
        if (TextUtils.isEmpty(label)) label = mEmptyLabel;
        print(label);
        println("</name>");
    }

    /**
     * Refer to the correct style, depending on the value of the data point for this record.
     */
    private void writePlacemarkStyleUrl(String placemarkName, Map<String, String> values) {
        final String styleUrl;
        if (TextUtils.isEmpty(placemarkName)) {
            styleUrl = STYLEMAP_YELLOW;
        } else {
            KMLStyle.IconColor iconColor = mKmlStyle.getColor(values);
            if (iconColor == org.jraf.android.networkmonitor.app.export.kml.KMLStyle.IconColor.GREEN) styleUrl = STYLEMAP_GREEN;
            else if (iconColor == org.jraf.android.networkmonitor.app.export.kml.KMLStyle.IconColor.RED) styleUrl = STYLEMAP_RED;
            else
                styleUrl = STYLEMAP_YELLOW;
        }
        print("      <styleUrl>");
        print(styleUrl);
        println("</styleUrl>");
    }

    /**
     * Write the device coordinates for this record.
     */
    private void writePlacemarkCoordinates(String longitude, String latitude) {
        // Write the device coordinates.
        println("      <Point>");
        print("        <coordinates>");
        print(longitude + "," + latitude);
        println("</coordinates>");
        println("      </Point>");
    }

    /**
     * Write the timestamp.
     */
    private void writePlacemarkTimestamp(String timestamp) {
        print("      <TimeStamp><when>");
        print(timestamp);
        println("</when></TimeStamp>");
    }

    /**
     * Write all the attributes we were able to retrieve for this record.
     */
    private void writePlacemarkExtendedData(Map<String, String> values) {
        println("      <ExtendedData>");
        for (String columnName : values.keySet()) {
            String value = values.get(columnName);
            if (!TextUtils.isEmpty(value)) {
                println("        <Data name=\"" + columnName + "\">");
                print("          <value>");
                print(value);
                println("</value>");
                println("        </Data>");
            }
        }
        println("      </ExtendedData>");
    }

}