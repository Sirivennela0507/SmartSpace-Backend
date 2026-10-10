package com.smartspace.backend.service;

import com.smartspace.backend.model.Space;
import com.smartspace.backend.repository.SpaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SpaceService {
    @Autowired
    private SpaceRepository spaceRepository;
    public Space addSpace(Space space) {
        return spaceRepository.save(space);
    }
    public List<Space> getAllSpaces() {
        return spaceRepository.findAll();
    }
}