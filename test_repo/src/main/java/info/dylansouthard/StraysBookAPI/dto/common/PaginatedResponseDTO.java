package info.dylansouthard.StraysBookAPI.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PaginatedResponseDTO<T> {
    @Schema(description = "List of items in the current page", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<T> content;

    @Schema(description = "Current page number (0-based)", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private int pageNumber;

    @Schema(description = "Number of items per page", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    private int pageSize;

    @Schema(description = "Total number of pages available", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private int totalPages;

    @Schema(description = "Total number of items available", example = "50", requiredMode = Schema.RequiredMode.REQUIRED)
    private long totalElements;

    @Schema(description = "Indicates if there is a next page", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean hasNext;

    @Schema(description = "Indicates if there is a previous page", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean hasPrevious;

}
