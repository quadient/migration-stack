package com.quadient.wfdxml.utils;

import com.quadient.wfdxml.exceptions.WfdXmlException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class FileUtils {

    private FileUtils() {
    }

    public static void saveXmlToFile(String xmlData, File outputFile) {
        try (PrintWriter out = new PrintWriter(outputFile)) {
            out.println(xmlData);
        } catch (FileNotFoundException e) {
            throw new WfdXmlException("Unexpected error during save operation of xml into file", e);
        }
    }
}