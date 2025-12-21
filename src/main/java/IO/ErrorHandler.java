package IO;

import java.util.ArrayList;
import java.util.function.Consumer; // Listener için gerekli import

public class ErrorHandler {

    private static ErrorHandler instance;
    private ArrayList<String> errorMessages;
    private int errorCount;

    // Arayüzü güncellemek için kullanılacak fonksiyon (Callback/Listener)
    private Consumer<String> onErrorCallback;

    // Private constructor (Singleton için)
    private ErrorHandler() {
        this.errorMessages = new ArrayList<>();
        this.errorCount = 0;
    }

    // Global erişim noktası
    public static ErrorHandler getInstance() {
        if (instance == null) {
            instance = new ErrorHandler();
        }
        return instance;
    }

    // Controller'dan buraya abone olmak için kullanılacak metod
    // MainViewController burayı çağırarak "Hata olunca bana haber ver" der.
    public void setOnErrorListener(Consumer<String> callback) {
        this.onErrorCallback = callback;
    }

    /**
     * Hatayı kaydeder ve arayüze haber verir.
     * @param message Hata mesajı
     */
    public void logError(String message) {
        // 1. Hatayı listeye ekle
        this.errorMessages.add(message);
        this.errorCount++;

        // 2. Konsola yaz (Debugging için)
        System.err.println("[ErrorHandler] " + message);

        // 3. Eğer arayüz dinliyorsa (Listener varsa), ona haber ver
        if (onErrorCallback != null) {
            onErrorCallback.accept(message);
        }
    }

    public int getErrorCount() {
        return this.errorCount;
    }

    public ArrayList<String> getErrorMessages() {
        return this.errorMessages;
    }

    public void clear() {
        this.errorMessages.clear();
        this.errorCount = 0;
    }
}