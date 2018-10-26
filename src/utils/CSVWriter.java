package utils;

import bezier.Point;

import java.io.*;
import java.util.ArrayList;
import java.util.function.Function;

public class CSVWriter extends OutputStreamWriter {

    private char COMMA_DELIMITER = ',';
    private char NEWLINE = '\n';

    public CSVWriter(File file) throws FileNotFoundException {
        super(new FileOutputStream(file));
    }

    public CSVWriter(String filePath) throws FileNotFoundException {
        super(new FileOutputStream(new File(filePath)));

    }

    public void write(String header, ArrayList<Point> path, Function<Point, String>... values) throws IOException {
        write(header);
        append(NEWLINE);
        for (Point p : path) {
            for (Function<Point, String> f : values) {
                append(f.apply(p)).append(COMMA_DELIMITER);
            }
            append(NEWLINE);
        }
    }

}
