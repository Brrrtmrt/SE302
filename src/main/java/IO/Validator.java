package IO;
    import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
    import java.nio.file.Path;
    import java.util.regex.Pattern;
public class Validator {
    public static boolean validateFile(Path filePath) {
        //existence check
        if (Files.notExists(filePath)) {
            //ERROR LOG
            ErrorHandler.getInstance().logError("Dosya bulunamadı: " + filePath);
            return false;
        }
        //readable check
        if (!Files.isReadable(filePath)) {
            //ERROR LOG
            ErrorHandler.getInstance().logError("Dosya okunamadı: " + filePath);
            return false;
        }
        //regular file check
        if (!Files.isRegularFile(filePath)) {
            //ERROR LOG
            ErrorHandler.getInstance().logError("Normal olmayan dosya: " + filePath);
            return false;
        }
        //extension check
        if (!allowedextensions(filePath)) {
            //ERROR LOG
            ErrorHandler.getInstance().logError("Desteklenmeyen dosya uzantısı: " + filePath);
            return false;
        }
        //reachable check
        try {
            Files.newBufferedReader(filePath).close();
        } catch (Exception e) {
            //ERROR LOG
            ErrorHandler.getInstance().logError("Ulaşılamayan dosya: " + filePath + " - " + e.getMessage());
            return false;
        }
        //parseable check
        // detect separator by scanning until we find a line containing ',' or ';'
        char separator = ','; // default
        try (BufferedReader detectionReader = Files.newBufferedReader(filePath)) {
            String detectLine;
            while ((detectLine = detectionReader.readLine()) != null) {
                if (detectLine.trim().isEmpty()) continue;
                if (detectLine.indexOf(',') >= 0) { separator = ','; break; }
                if (detectLine.indexOf(';') >= 0) { separator = ';'; break; }
                // otherwise keep scanning until we find one of the delimiters
            }
        } catch (IOException e) {
            ErrorHandler.getInstance().logError("Ayırıcı dosya bulunamadı: " + filePath + " - " + e.getMessage());
            return false;
        }

        // now validate using the detected separator
        String sepRegex = Pattern.quote(String.valueOf(separator));
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) { lineNum++; continue; }
                String[] columns = line.split(sepRegex, -1); // -1 keeps trailing empty fields
                for (int coulmnNum = 0; coulmnNum < columns.length; coulmnNum++) {
                    String val = columns[coulmnNum];
                    if (val == null || val.trim().isEmpty() || "null".equalsIgnoreCase(val.trim()))  {
                        //ERROR LOG
                        ErrorHandler.getInstance().logError("Boş veya geçersiz(null) değer: " + filePath + ", " + lineNum + ". satır, " + (coulmnNum + 1) + ". sütun.");
                        return false;
                    }
                }
                lineNum++;
            }
        } catch (IOException e) {
            //ERROR LOG
            ErrorHandler.getInstance().logError("Dosya ayrıştırılamadı: " + filePath + " - " + e.getMessage());
            return false;
        }
        //all checks passed
        return true;

    }




//check for allowed extensions
//add more extensions if needed by   || filename.endsWith(".extension")
    public static boolean allowedextensions(Path filePath) {
        String fileName = filePath.getFileName().toString().toLowerCase();
        return fileName.endsWith(".csv");
    }
}
