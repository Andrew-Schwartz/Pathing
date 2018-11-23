package utils;

import bezier.Point;

import java.io.*;
import java.util.ArrayList;
import java.util.function.Function;

public class CSVWriter extends OutputStreamWriter {

    private char COMMA_DELIMITER = ',';
    private char NEWLINE = '\n';
    private File csv;

    public CSVWriter(File file) throws FileNotFoundException {
        super(new FileOutputStream(file));
        csv = file;
    }

    public CSVWriter(String filePath) throws FileNotFoundException {
        super(new FileOutputStream(new File(filePath)));
        csv = new File(filePath);
    }

    @SafeVarargs
    public final void writePoints(String header, ArrayList<Point> points, Function<Point, Object>... values) throws IOException {
        assert header.split(String.valueOf(COMMA_DELIMITER)).length == values.length;
        write(header);
        append(NEWLINE);
        for (Point p : points) {
            for (Function<Point, Object> f : values) {
                append(f.apply(p).toString()).append(COMMA_DELIMITER);
            }
            append(NEWLINE);
        }
    }
}
