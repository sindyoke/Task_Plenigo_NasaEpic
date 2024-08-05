package com.example.plenigo;

import com.example.plenigo.service.NasaEpicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.IOException;

@ShellComponent
public class GetImages {

    @Autowired
    private NasaEpicService epicService;

    @ShellMethod(key = "dl", value = "Save all images in your target folder --t for the most recent date, or type date --d in format YYYY-MM-DD")
    public void getImages(@ShellOption(value = "--t") String target, @ShellOption(value = "--d", defaultValue = "") String date) {
        try {
            epicService.downloadEpicImages(date.isEmpty() ? null : date, target);
        } catch (IOException e) {
            System.err.println("Error downloading images: " + e.getMessage());
        }
    }
}
