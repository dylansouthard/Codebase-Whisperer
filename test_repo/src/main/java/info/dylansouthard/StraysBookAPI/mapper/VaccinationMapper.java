package info.dylansouthard.StraysBookAPI.mapper;

import info.dylansouthard.StraysBookAPI.dto.vaccination.VaccinationSummaryDTO;
import info.dylansouthard.StraysBookAPI.model.Vaccination;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VaccinationMapper {
    VaccinationSummaryDTO toVaccinationSummaryDTO(Vaccination vaccination);
}
