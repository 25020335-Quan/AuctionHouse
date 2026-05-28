package auction.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageUploadUtil {
    private static final String API_KEY = "38584ae3c4ef27d2bec967562f29911f";
    private static final String UPLOAD_URL = "https://api.imgbb.com/1/upload";

    public static String uploadImage(File file) {
        try {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            String encodedString = Base64.getEncoder().encodeToString(fileContent);

            String data = URLEncoder.encode("key", "UTF-8") + "=" + URLEncoder.encode(API_KEY, "UTF-8") +
                    "&" + URLEncoder.encode("image", "UTF-8") + "=" + URLEncoder.encode(encodedString, "UTF-8");

            URL url = new URL(UPLOAD_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream())) {
                writer.write(data);
                writer.flush();
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String json = response.toString();
            Pattern pattern = Pattern.compile("\"display_url\":\"(.*?)\"");
            Matcher matcher = pattern.matcher(json);
            if (matcher.find()) {
                return matcher.group(1).replace("\\/", "/");
            }
        } catch (Exception e) {
            System.err.println("Lỗi up ảnh lên mây: " + e.getMessage());
        }
        return null;
    }
}
