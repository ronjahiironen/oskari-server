package org.oskari.capabilities.ogc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import fi.nls.oskari.domain.map.OskariLayer;
import org.oskari.capabilities.ogc.wfs.FeaturePropertyType;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class LayerCapabilitiesWFS extends LayerCapabilitiesOGC {

    public static final String OGC_API_CRS_URI = "crs-uri";
    public static final String MAX_FEATURES = CapabilitiesConstants.KEY_MAX_FEATURES;
    public static final String NAMESPACE_URI = "nsUri"; // previously KEY_NAMESPACE_URL = "namespaceURL";
    public static final String FEATURE_PROPERTIES = "featureProperties";
    public static final String GEOMETRY_FIELD = "geomName";

    public LayerCapabilitiesWFS(@JsonProperty("name") String name, @JsonProperty("title") String title) {
        super(name, title);
        setType(OskariLayer.TYPE_WFS);
    }


    public void setSupportedCrsURIs(Set<String> uris) {
        addCapabilityData(OGC_API_CRS_URI, uris);
    }

    @JsonIgnore
    public Set<String> getSupportedCrsURIs() {
        return (Set<String>) getTypeSpecific().getOrDefault(OGC_API_CRS_URI, Collections.emptySet());
    }

    public void setFeatureProperties(Collection<FeaturePropertyType> props) {
        if (props != null) {
            addCapabilityData(FEATURE_PROPERTIES, props);
            setGeometryField(props.stream()
                    .filter(p -> p.isGeometry())
                    .map(p -> p.name)
                    .findFirst()
                    .orElse(null));
        }
    }

    @JsonIgnore
    public Collection<FeaturePropertyType> getFeatureProperties() {
        return (Collection<FeaturePropertyType>) getTypeSpecific().getOrDefault(FEATURE_PROPERTIES, Collections.emptyList());
    }

    public void setGeometryField(String geomName) {
        if (geomName != null) {
            addCapabilityData(GEOMETRY_FIELD, geomName);
        }
    }

    @JsonIgnore
    public String getGeometryField() {
        return (String) getTypeSpecific().get(GEOMETRY_FIELD);
    }
    @JsonIgnore
    public String getNamespaceUri() {
        return (String) getTypeSpecific().getOrDefault(NAMESPACE_URI, null);
    }
    public void setNamespaceUri(String uri) {
        if (uri != null) {
            addCapabilityData(NAMESPACE_URI, uri);
        }
    }
    public void setMaxFeatures(int count) {
        if (count < 0) {
            return;
        }
        try {
            addCapabilityData(MAX_FEATURES, count);
        } catch (Exception ignored) {}
    }
    @JsonIgnore
    public Integer getMaxScale() {
        return (Integer) getTypeSpecific().get(MAX_FEATURES);
    }
}
