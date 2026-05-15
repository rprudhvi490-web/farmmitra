package com.weekendbasket.app.service;

import com.weekendbasket.app.dto.CommunityDto.*;
import com.weekendbasket.app.exception.ResourceNotFoundException;
import com.weekendbasket.app.exception.WeekendBasketException;
import com.weekendbasket.app.model.Community;
import com.weekendbasket.app.repository.CommunityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final CommunityRepository communityRepository;

    public List<CommunityResponse> getAll() {
        return communityRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public CommunityResponse getById(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public CommunityResponse create(CreateCommunityRequest request) {
        if (communityRepository.existsByName(request.name())) {
            throw new WeekendBasketException("Community already exists: " + request.name());
        }
        Community community = Community.builder()
                .name(request.name())
                .city(request.city())
                .address(request.address())
                .build();
        communityRepository.save(community);
        return toResponse(community);
    }

    @Transactional
    public CommunityResponse update(Long id, CreateCommunityRequest request) {
        Community community = find(id);
        community.setName(request.name());
        community.setCity(request.city());
        community.setAddress(request.address());
        communityRepository.save(community);
        return toResponse(community);
    }

    @Transactional
    public void deactivate(Long id) {
        Community community = find(id);
        community.setActive(false);
        communityRepository.save(community);
    }

    private Community find(Long id) {
        return communityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found with id: " + id));
    }

    private CommunityResponse toResponse(Community c) {
        return new CommunityResponse(c.getId(), c.getName(), c.getCity(), c.getAddress(), c.getActive());
    }
}
