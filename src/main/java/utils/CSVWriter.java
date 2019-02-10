package utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.function.Function;

public class CSVWriter<T> extends OutputStreamWriter {

    private String commaDelimiter = ",";

    public CSVWriter(File file) throws FileNotFoundException {
        super(new FileOutputStream(file));
    }

    public CSVWriter(String filePath) throws FileNotFoundException {
        super(new FileOutputStream(new File(filePath)));
    }

    public void setCommaDelimiter(String delimiter) {
        commaDelimiter = delimiter;
    }

    @SafeVarargs
    public final void writeObjects(String header, ArrayList<T> obs, Function<T, Object>... funcs) throws IOException {
        if (header.split(commaDelimiter).length != funcs.length)
            throw new IllegalArgumentException("num columns in header != num of functions to write with");
        write(header);
        append("\n");
        for (int i = 0; i < obs.size(); i++) {
            for (int j = 0; j < funcs.length; j++) {
                append(funcs[j].apply(obs.get(i)).toString());
                if (j != funcs.length - 1)
                    append(commaDelimiter);
            }
            if (i != obs.size() - 1)
                append("\n");
        }
    }
}
