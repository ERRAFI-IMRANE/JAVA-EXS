package application;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MainController {

    @FXML
    private ImageView imagePreview;

    @FXML
    private Label resultLabel;

    @FXML
    private void chooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                uploadAndDetect(file);
            } catch (Exception e) {
                e.printStackTrace();
                resultLabel.setText("Erreur lors de l'analyse");
            }
        }
    }

    private void uploadAndDetect(File file) throws Exception {
        String boundary = Long.toHexString(System.currentTimeMillis());
        URL url = new URL("http://127.0.0.1:8000/detect");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        OutputStream output = connection.getOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, "UTF-8"), true);

        // Send file
        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                .append(file.getName()).append("\"\r\n");
        writer.append("Content-Type: ").append(Files.probeContentType(file.toPath())).append("\r\n\r\n");
        writer.flush();
        Files.copy(file.toPath(), output);
        output.flush();
        writer.append("\r\n").flush();

        writer.append("--").append(boundary).append("--\r\n").flush();

        // Read JSON response
        InputStream responseStream = connection.getInputStream();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> responseMap = mapper.readValue(responseStream, Map.class);

        // Load output image from fixed path
        String outputImagePath = "C:\\Imrane_Programmation\\JAVA\\AI\\Local_API\\output.jpg";
        File outputFile = new File(outputImagePath);
        if (outputFile.exists()) {
            Image outputImage = new Image(outputFile.toURI().toString());
            imagePreview.setImage(outputImage);
        }

        // Show object info: class + confidence
        List<Map<String,Object>> objects = (List<Map<String,Object>>) responseMap.get("objects");
        StringBuilder sb = new StringBuilder();
        sb.append("Total objects detected: ").append(objects.size()).append("\n\n");
        for (Map<String,Object> obj : objects) {
            sb.append("Class: ").append(obj.get("type"))
                    .append("\nConfidence: ").append(String.format("%.2f", obj.get("confidence")))
                    .append("\n\n");
        }
        resultLabel.setText(sb.toString());
    }
}
