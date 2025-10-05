package info.dylansouthard.StraysBookAPI.mapper;

import info.dylansouthard.StraysBookAPI.dto.careEvent.CareEventDTO;
import info.dylansouthard.StraysBookAPI.dto.careEvent.CareEventSummaryDTO;
import info.dylansouthard.StraysBookAPI.dto.common.PaginatedResponseDTO;
import info.dylansouthard.StraysBookAPI.model.CareEvent;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring")
public interface CareEventMapper {

    CareEventDTO toCareEventDTO(CareEvent careEvent);

    CareEventSummaryDTO toCareEventSummaryDTO(CareEvent careEvent);

    default PaginatedResponseDTO<CareEventDTO> toPaginatedResponseDTO(Page<CareEvent> page) {
        return new PaginatedResponseDTO<>(
                page.getContent().stream().map(this::toCareEventDTO).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}

