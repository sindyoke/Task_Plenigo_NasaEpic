package com.example.plenigo.service;

import com.example.plenigo.util.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import org.springframework.http.HttpHeaders;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Service
public class NasaEpicService {
    @Value("${nasa.epic.api.url}")
    private String apiUrl;

    @Value("${nasa.epic.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public void downloadEpicImages(String date, String targetFolder) throws IOException {
        if (date == null) {
            date = getLastAvailableDate().split(" ")[0];
            System.out.println("date was null so lastAvailable is " + date);
        }

        if(date != null && !validDate(date)){
            System.out.println("Date format is not valid. Please enter date in format YYYY-MM-DD");
            return;
        }

        String url = String.format("%s/api/natural/date/%s?api_key=%s", apiUrl, date, apiKey);
        Map<String, Object>[] images = fetchImagesWithRetry(url, 3);

        if (images == null || images.length == 0) {
            System.out.println("No images available for the given date.");
            return;
        }

        Path dateFolder = Paths.get(targetFolder, date);
        FileUtils.createDirectoryIfNotExists(dateFolder);

        System.out.print("Downloading images");

        for (Map<String, Object> image : images) {
            System.out.print(".");
            String imageName = (String) image.get("image");
            String imageUrl = String.format("%s/archive/natural/%s/png/%s.png?api_key=%s", apiUrl, date.replace("-", "/"), imageName, apiKey);
            Path imagePath = dateFolder.resolve(imageName + ".png");
            downloadFileWithHeaders(imageUrl, imagePath);
        }

        System.out.println("\nDownloaded " + images.length + " images for date " + date);

    }

    private String getLastAvailableDate() {
        String url = String.format("%s/api/natural?api_key=%s", apiUrl, apiKey);
        Map<String, Object>[] dates = restTemplate.getForObject(url, Map[].class);
        if (dates == null || dates.length == 0) {
            throw new IllegalStateException("No available dates found");
        }
        return (String) dates[0].get("date");
    }

    private Map<String, Object>[] fetchImagesWithRetry(String url, int maxRetries) {
        int attempts = 0;
        while (attempts < maxRetries) {
            try {
                ResponseEntity<List<Map<String, Object>>> responseEntity = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {
                        }
                );
                List<Map<String, Object>> imagesList = responseEntity.getBody();
                if (imagesList != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object>[] imagesArray = imagesList.toArray(new Map[0]);
                    return imagesArray;
                }
            } catch (Exception e) {
                attempts++;
                if (attempts >= maxRetries) {
                    throw new RuntimeException("Failed to fetch images after " + maxRetries + " attempts", e);
                }
                try {
                    Thread.sleep((long) Math.pow(2, attempts) * 1000); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread was interrupted during backoff", ie);
                }
            }
        }
        return null;
    }

    private void downloadFileWithHeaders(String imageUrl, Path imagePath) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<byte[]> response = restTemplate.exchange(imageUrl, HttpMethod.GET, entity, byte[].class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Files.write(imagePath, response.getBody());
        } else {
            throw new IOException("Failed to download image: " + response.getStatusCode());
        }
    }

    boolean validDate (String stringToTest) {
        try {
            LocalDate.parse(stringToTest, formatter);
            return true;
        } catch (DateTimeParseException dtpe) {
            return false;
        }
    }

}
