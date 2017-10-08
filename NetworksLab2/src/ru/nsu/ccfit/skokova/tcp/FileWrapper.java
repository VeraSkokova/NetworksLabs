package ru.nsu.ccfit.skokova.tcp;

import java.io.File;
import java.io.Serializable;

public class FileWrapper implements Serializable {
    private String fileName;
    private long fileSize;
    private File file;

    public FileWrapper(String fileName, long fileSize, File file) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.file = file;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public File getFile() {
        return file;
    }
}
