/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jiaomo.framework.commons;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.zip.*;

/**
 * @author tangyanyun
 * {@code @email} tangyanyun@sina.com
 */


public class ZipUtils {

    public static void zip(List<String> fileNames, String zipFileName) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFileName))) {
            for (String fileName : fileNames) {
                File file = new File(fileName);
                zip(file, file.getName(), zipOutputStream);
            }
        }
    }

    public static void zip(String fileName,String zipFileName) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFileName))) {
            File file = new File(fileName);
            zip(file, file.getName(), zipOutputStream);
        }
    }
    public static void zip(String fileName,File zipFile) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile))) {
            File file = new File(fileName);
            zip(file, file.getName(), zipOutputStream);
        }
    }
    public static void zip(String fileName,String fileNameInZip,File zipFile) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zip(new File(fileName), fileNameInZip, zipOutputStream);
        }
    }
    public static void zip(File file,String fileNameInZip,ZipOutputStream zipOutputStream) throws IOException {
        if (file.isDirectory()) {
            for (File subFile : Objects.requireNonNull(file.listFiles())) {
                zip(subFile,fileNameInZip + "/" + subFile.getName(),zipOutputStream);
            }
        } else {
            ZipEntry entry = new ZipEntry(fileNameInZip);
            zipOutputStream.putNextEntry(entry);

            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int iLen;
                while ((iLen = fileInputStream.read(buffer)) != -1) {
                    zipOutputStream.write(buffer, 0, iLen);
                }
            }

            zipOutputStream.closeEntry();
        }
    }

    public static void unzip(String zipFileName) throws IOException {
        unzip(new File(zipFileName),".");
    }
    public static void unzip(String zipFileName, String outputFolder) throws IOException {
        unzip(new File(zipFileName),outputFolder);
    }
    public static void unzip(File zipFile, String outputFolder) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile))) {
            byte[] buffer = new byte[4096];
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                File outputFile = new File(outputFolder + "/" + entry.getName());
                if (entry.isDirectory()) {
                    outputFile.mkdirs();
                } else {
                    new File(outputFile.getParent()).mkdirs();
                    try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
                        int iLen;
                        while ((iLen = zipInputStream.read(buffer)) != -1) {
                            fileOutputStream.write(buffer, 0, iLen);
                        }
                    }
                }
                zipInputStream.closeEntry();
            }
        }
    }

    public static void main(String[] args) throws ZipException, IOException {
//        zip("/Users/tangyanyun/bin","/tmp/bb.zip");
//        unzip("/tmp/bb.zip","/tmp");
        zip("/Users/tangyanyun/bin",".",new File("/tmp/bb.zip"));
        unzip("/tmp/bb.zip","/tmp/bb11");
    }
}

