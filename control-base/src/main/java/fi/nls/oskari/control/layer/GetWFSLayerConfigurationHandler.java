package fi.nls.oskari.control.layer;

import fi.mml.map.mapwindow.service.db.MyPlacesService;
import fi.mml.map.mapwindow.service.db.MyPlacesServiceIbatisImpl;
import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.control.ActionParamsException;
import fi.nls.oskari.domain.map.UserDataLayer;
import fi.nls.oskari.domain.map.analysis.Analysis;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.map.userlayer.service.UserLayerDbService;
import fi.nls.oskari.map.userlayer.service.UserLayerDbServiceIbatisImpl;
import org.json.JSONObject;

import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionHandler;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.analysis.service.AnalysisDataService;
import fi.nls.oskari.util.ConversionHelper;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.PropertyUtil;
import fi.nls.oskari.util.ResponseHelper;
import fi.nls.oskari.domain.map.wfs.WFSLayerConfiguration;
import fi.nls.oskari.wfs.WFSLayerConfigurationService;
import fi.nls.oskari.wfs.WFSLayerConfigurationServiceIbatisImpl;

@OskariActionRoute("GetWFSLayerConfiguration")
public class GetWFSLayerConfigurationHandler extends ActionHandler {

    private static final Logger log = LogFactory
            .getLogger(GetWFSLayerConfigurationHandler.class);

    private final WFSLayerConfigurationService layerConfigurationService = new WFSLayerConfigurationServiceIbatisImpl();
    private AnalysisDataService analysisDataService = new AnalysisDataService();
    private UserLayerDbService userLayerDbService = new UserLayerDbServiceIbatisImpl();
    private MyPlacesService myPlacesService = new MyPlacesServiceIbatisImpl();

    private final static String ID = "id";

    private final static String RESULT = "result";
    private final static String RESULT_SUCCESS = "success";

    // Analysis
    public static final String ANALYSIS_BASELAYER_ID = "analysis.baselayer.id";
    public static final String ANALYSIS_PREFIX = "analysis_";

    // My places
    public static final String MYPLACES_BASELAYER_ID = "myplaces.baselayer.id";
    public static final String MYPLACES_PREFIX = "myplaces_";

    // User layer
    public static final String USERLAYER_BASELAYER_ID = "userlayer.baselayer.id";
    public static final String USERLAYER_PREFIX = "userlayer_";

    public void handleAction(ActionParameters params) throws ActionException {

        // Because of analysis layers
        final String sid = params.getHttpParam(ID, "n/a");
        final int id = ConversionHelper.getInt(getBaseLayerId(sid), -1);
        if(id == -1) {
            throw new ActionParamsException("Required parameter '" + ID + "' missing!");
        }

        final WFSLayerConfiguration lc = getLayerInfoForRedis(id, sid);
        if (lc == null) {
            throw new ActionParamsException("Couldn't find matching layer for id " + ID);
        }
        // lc.save() saves the layer info to redis as JSON
        lc.save();

        final JSONObject root = new JSONObject();
        JSONHelper.putValue(root, RESULT, RESULT_SUCCESS);
        ResponseHelper.writeResponse(params, root);
    }

    private WFSLayerConfiguration getLayerInfoForRedis(final int id, final String requestedLayerId) {

        WFSLayerConfiguration lc = layerConfigurationService.findConfiguration(id);

        log.warn("id:", id, "requested layer id:", requestedLayerId);
        log.warn(lc);
        final long userDataLayerId = extractId(requestedLayerId);
        UserDataLayer userLayer = null;

        // Extra manage for analysis
        if (requestedLayerId.startsWith(ANALYSIS_PREFIX)) {
            final Analysis analysis = analysisDataService.getAnalysisById(userDataLayerId);
            // Set analysis layer fields as id based
            lc.setSelectedFeatureParams(analysisDataService.getAnalysisNativeColumns(analysis));
            userLayer = analysis;
        }
        // Extra manage for myplaces
        else if (requestedLayerId.startsWith(MYPLACES_PREFIX)) {
            userLayer = myPlacesService.find((int)userDataLayerId);
        }
        // Extra manage for imported data
        else if (requestedLayerId.startsWith(USERLAYER_PREFIX)) {
            userLayer = userLayerDbService.getUserLayerById(userDataLayerId);
        }
        if(userLayer != null && userLayer.isPublished()) {
            // set id to user data layer id for redis
            lc.setLayerId(requestedLayerId);
            setupPublishedFlags(lc, userLayer.getUuid());
        }
        return lc;
    }

    /**
     * Return base wfs id
     * 
     * @param sid
     * @return id
     */
    private String getBaseLayerId(final String sid) {
        if (sid.startsWith(ANALYSIS_PREFIX)) {
            return PropertyUtil.get(ANALYSIS_BASELAYER_ID);
        }
        else if (sid.startsWith(MYPLACES_PREFIX)) {
            return PropertyUtil.get(MYPLACES_BASELAYER_ID);
        }
        else if (sid.startsWith(USERLAYER_PREFIX)) {
            return PropertyUtil.get(USERLAYER_BASELAYER_ID);
        }
        return sid;
    }

    private long extractId(final String layerId) {
        if (layerId == null) {
            return -1;
        }
        // takeout the last _-separated token
        // -> this is the actual id in analysis, myplaces, userlayer
        final String[] values = layerId.split("_");
        if(values.length < 2) {
            // wasn't valid id!
            return -1;
        }
        final String id = values[values.length - 1];
        return ConversionHelper.getLong(id, -1);
    }

    /**
     * Transport uses this uuid in WFS query instead of users id if published is true.
     * @param lc
     * @param uuid
     */
    private void setupPublishedFlags(final WFSLayerConfiguration lc, final String uuid) {
        lc.setPublished(true);
        lc.setUuid(uuid);
    }
}
