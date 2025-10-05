package info.dylansouthard.StraysBookAPI.model.shared;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import info.dylansouthard.StraysBookAPI.serializers.GeoSchemaSerializer;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "latitude", "longitude" })
@JsonSerialize(using = GeoSchemaSerializer.class)
@Getter
@Setter
@NoArgsConstructor
@Embeddable
public class GeoSchema {


@Column(columnDefinition = "GEOMETRY(Point, 4326)")
@JsonIgnore
    private Point location;

    @JsonIgnore
    public Point getLocation() {
        return location;
    }

    public GeoSchema(double latitude, double longitude) {
        this.location = createPoint(latitude, longitude);
    }


    private Point createPoint(double latitude, double longitude) {
        GeometryFactory geometryFactory = new GeometryFactory();
        return geometryFactory.createPoint(new Coordinate(longitude, latitude));
    }

    @Override
    public String toString() {
        return String.format("{latitude=%.4f, longitude=%.4f}",
                this.getLatitude(), this.getLongitude());
    }

    @JsonProperty("latitude")
    public double getLatitude() {
        return location != null ? location.getY() : 0;
    }
    @JsonProperty("longitude")
    public double getLongitude() {
        return location != null ? location.getX() : 0;
    }

    @JsonProperty("latitude")
    public void setLatitude(double latitude) {
        if (this.location == null) {
            this.location = createPoint(latitude, 0);
        } else {
            this.location = createPoint(latitude, getLongitude());
        }
    }

    @JsonProperty("longitude")
    public void setLongitude(double longitude) {
        if (this.location == null) {
            this.location = createPoint(0, longitude);
        } else {
            this.location = createPoint(getLatitude(), longitude);
        }
    }
}
