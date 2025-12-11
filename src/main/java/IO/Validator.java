package IO;
    import java.io.BufferedReader;
    import java.nio.file.Files;
    import java.nio.file.Path;
public class Validator {
    public static boolean validateFile(Path filePath) {
        //existence check
        if (Files.notExists(filePath)) {
            throw new RuntimeException("File does not exist: " + filePath);
        }
        //readable check
        if (!Files.isReadable(filePath)) {
            throw new RuntimeException("File is not readable: " + filePath);
        }
        //regular file check
        if (!Files.isRegularFile(filePath)) {
            throw new RuntimeException("Not a regular file: " + filePath);
        }
        //extansion check
        if (!allowedextensions(filePath)) {
            throw new RuntimeException("File has an unsupported extension: " + filePath);
        }
        //reachable check
        try {
            Files.newBufferedReader(filePath).close();
        } catch (Exception e) {
            throw new RuntimeException("File is not reachable: " + filePath);
        }
        //parseable check
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            // read each line and verify no empty/null cells
            String line;
            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",", -1); // -1 keeps trailing empty fields
                for (int coulmnNum = 0; coulmnNum < columns.length; coulmnNum++) {
                    String val = columns[coulmnNum];
                    if (val == null || val.trim().isEmpty() || "null".equalsIgnoreCase(val.trim()))  {
         throw new RuntimeException(
                "Empty or null value in file " + filePath + " at line " + lineNum + ", column " + (coulmnNum + 1));
                    }
                }
                lineNum++;
            }

        } catch (RuntimeException re) {
            throw re; // rethrow validation errors
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse file: " + filePath, e);
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
