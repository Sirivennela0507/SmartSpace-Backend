package com.smartspace.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.smartspace.backend.model.Space;
import com.smartspace.backend.service.SpaceService;

@RestController
@RequestMapping("/api/spaces")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174"})
public class SpaceController {

    private final SpaceService spaceService;

    public SpaceController(SpaceService spaceService) {
        this.spaceService = spaceService;
    }

    @GetMapping
    public List<Space> getAllSpaces() {
        return spaceService.getAllSpaces();
    }

    @PostMapping
    public Space addSpace(@RequestBody Space space) {
        return spaceService.addSpace(space);
    }
}