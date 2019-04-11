package org.oskari.csw.request;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import fi.nls.oskari.search.channel.MetadataCatalogueQueryHelper;
import fi.nls.oskari.service.ServiceRuntimeException;
import fi.nls.oskari.util.IOHelper;
import fi.nls.oskari.util.JSONHelper;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.oskari.geojson.GeoJSONReader;
import org.xml.sax.SAXException;

import java.io.IOException;

import static org.junit.Assert.*;

public class GetRecordsTest {

    private FilterFactory2 filterFactory = CommonFactoryFinder.getFilterFactory2();

    @BeforeClass
    public static void setUp() {
        // use relaxed comparison settings
        XMLUnit.setIgnoreComments(true);
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreDiffBetweenTextAndCDATA(true);
        XMLUnit.setIgnoreAttributeOrder(true);
    }

    @Test(expected = ServiceRuntimeException.class)
    public void testWithNoFilter() {
        org.oskari.csw.request.GetRecords.createRequest(null);
        fail("Should have thrown exception");
    }

    @Test
    public void testSimpleFilter() throws IOException, SAXException {
        // build filter
        Filter filter = createEqualsFilter("my value", "myprop");
        String request = org.oskari.csw.request.GetRecords.createRequest(filter);

        // read expected result and compare
        String expected = IOHelper.readString(getClass().getResourceAsStream("GetRecords-simple.xml"));
        Diff xmlDiff = new Diff(request, expected);
        assertTrue("Should get expected simple request" + xmlDiff, xmlDiff.similar());
    }

    @Test
    public void testMultiFilter() throws IOException, SAXException {
        // build filter
        Filter equalfilter = createEqualsFilter("my value", "myprop");
        Filter likefilter = createEqualsFilter("input*", "query");

        String request = org.oskari.csw.request.GetRecords.createRequest(filterFactory.and(equalfilter, likefilter));

        // read expected result and compare
        String expected = IOHelper.readString(getClass().getResourceAsStream("GetRecords-multi.xml"));
        Diff xmlDiff = new Diff(request, expected);
        assertTrue("Should get expected and-filter request" + xmlDiff, xmlDiff.similar());
    }

    @Test
    public void testCoverageFilter() throws IOException, SAXException {

        // build filter
        String input = "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[292864,6845440],[292864,6781952],[393216,6781952],[393216,6845440],[292864,6845440]]]},\"properties\":{\"area\":\"Alue ei saa muodostaa silmukkaa. Piirrä risteämätön alue nähdäksesi mittaustuloksen.\"},\"id\":\"drawFeature3\"}],\"crs\":\"EPSG:3067\"}";
        Filter filter = createGeometryFilter(input);
        String request = org.oskari.csw.request.GetRecords.createRequest(filter);

        // read expected result and compare
        String expected = IOHelper.readString(getClass().getResourceAsStream("GetRecords-coverage.xml"));
        Diff xmlDiff = new Diff(request, expected);
        assertTrue("Should get expected coverage request" + xmlDiff, xmlDiff.similar());
    }

    private Filter createLikeFilter(final String searchCriterion,
                                    final String searchElementName) {
        if (searchCriterion == null || searchCriterion.isEmpty()) {
            return null;
        }
        Expression _property = filterFactory.property(searchElementName);
        return filterFactory.like(_property, searchCriterion,
                MetadataCatalogueQueryHelper.WILDCARD_CHARACTER,
                MetadataCatalogueQueryHelper.SINGLE_WILDCARD_CHARACTER,
                MetadataCatalogueQueryHelper.ESCAPE_CHARACTER,
                false);
    }

    private Filter createEqualsFilter(final String searchCriterion,
                                      final String searchElementName) {
        if (searchCriterion == null || searchCriterion.isEmpty()) {
            return null;
        }
        Expression _property = filterFactory.property(searchElementName);
        return filterFactory.equals(_property, filterFactory.literal(searchCriterion));
    }

    private Filter createGeometryFilter(final String searchCriterion) {
        try {
            JSONObject geojson = JSONHelper.createJSONObject(searchCriterion);

            JSONArray features = geojson.optJSONArray("features");
            if (features == null || features.length() != 1) {
                return null;
            }
            Geometry geom = GeoJSONReader.toGeometry(features.optJSONObject(0).optJSONObject("geometry"));
            CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:3067");
            CoordinateReferenceSystem targetCRS = CRS.decode(MetadataCatalogueQueryHelper.TARGET_SRS, true);

            MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS, true);
            Geometry transformed = JTS.transform(geom, transform);

            return filterFactory.intersects(
                    filterFactory.property("ows:BoundingBox"),
                    filterFactory.literal(transformed));
        } catch (Exception e) {
            throw new ServiceRuntimeException("Can't create GetRecords request with coverage filter", e);
        }
    }

}