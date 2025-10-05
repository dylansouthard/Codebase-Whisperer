package info.dylansouthard.StraysBookAPI.mapper;

import info.dylansouthard.StraysBookAPI.dto.vaccination.VaccinationSummaryDTO;
import info.dylansouthard.StraysBookAPI.model.Vaccination;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-08-11T16:29:08+0900",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 23.0.2 (Homebrew)"
)
@Component
public class VaccinationMapperImpl implements VaccinationMapper {

    @Override
    public VaccinationSummaryDTO toVaccinationSummaryDTO(Vaccination vaccination) {
        if ( vaccination == null ) {
            return null;
        }

        VaccinationSummaryDTO vaccinationSummaryDTO = new VaccinationSummaryDTO();

        vaccinationSummaryDTO.setId( vaccination.getId() );
        vaccinationSummaryDTO.setType( vaccination.getType() );
        vaccinationSummaryDTO.setVerificationStatus( vaccination.getVerificationStatus() );

        return vaccinationSummaryDTO;
    }
}
