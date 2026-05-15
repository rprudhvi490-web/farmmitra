package com.weekendbasket.app.service;

import com.weekendbasket.app.dto.MasterTableDto.*;
import com.weekendbasket.app.exception.ResourceNotFoundException;
import com.weekendbasket.app.exception.WeekendBasketException;
import com.weekendbasket.app.model.MasterTable;
import com.weekendbasket.app.repository.MasterTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MasterTableService {

    private final MasterTableRepository masterTableRepository;

    public List<MasterResponse> getByType(String type) {
        return masterTableRepository.findByTypeOrderByLookupValueAsc(type).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<MasterResponse> getAll() {
        return masterTableRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MasterResponse create(CreateMasterRequest request) {
        if (masterTableRepository.existsByTypeAndLookupCode(request.type(), request.lookupCode())) {
            throw new WeekendBasketException("Master entry already exists: " + request.type() + "/" + request.lookupCode());
        }
        MasterTable entry = MasterTable.builder()
                .type(request.type())
                .lookupCode(request.lookupCode())
                .lookupItem(request.lookupItem())
                .lookupValue(request.lookupValue())
                .build();
        masterTableRepository.save(entry);
        return toResponse(entry);
    }

    @Transactional
    public MasterResponse update(Long id, CreateMasterRequest request) {
        MasterTable entry = masterTableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Master entry not found: " + id));
        entry.setLookupItem(request.lookupItem());
        entry.setLookupValue(request.lookupValue());
        masterTableRepository.save(entry);
        return toResponse(entry);
    }

    @Transactional
    public void delete(Long id) {
        if (!masterTableRepository.existsById(id)) {
            throw new ResourceNotFoundException("Master entry not found with id: " + id);
        }
        masterTableRepository.deleteById(id);
    }

    private MasterResponse toResponse(MasterTable m) {
        return new MasterResponse(m.getId(), m.getType(), m.getLookupCode(), m.getLookupItem(), m.getLookupValue());
    }
}
