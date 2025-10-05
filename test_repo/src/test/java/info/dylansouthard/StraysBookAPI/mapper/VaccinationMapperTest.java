package info.dylansouthard.StraysBookAPI.mapper;


import info.dylansouthard.StraysBookAPI.dto.vaccination.VaccinationSummaryDTO;
import info.dylansouthard.StraysBookAPI.model.Vaccination;
import info.dylansouthard.StraysBookAPI.model.enums.VaccinationType;
import info.dylansouthard.StraysBookAPI.model.enums.VerificationStatusType;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.*;

public class VaccinationMapperTest {
    VaccinationMapper vaccinationMapper = Mappers.getMapper(VaccinationMapper.class);

    @Test
    public void When_MappingToVaccinationSummaryDTO_Then_ReturnVaccinationSummaryDTO() {
        Vaccination vaccination = new Vaccination();
        vaccination.setId(1L);
        vaccination.setType(VaccinationType.FELV);
        VaccinationSummaryDTO dto = vaccinationMapper.toVaccinationSummaryDTO(vaccination);

        assertAll("Vaccination Summary Assertions",
                ()->assertNotNull(dto, "Vaccination Summary DTO should not be null"),
                ()->assertEquals(VaccinationType.FELV, dto.getType()),
                ()->assertEquals(VerificationStatusType.UNVERIFIED, dto.getVerificationStatus())
                );
    }
}
