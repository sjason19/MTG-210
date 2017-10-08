package ca.ubc.cs.cpsc210.mindthegap.TfL;

/*
 * Copyright 2015-2016 Department of Computer Science UBC
 */

import ca.ubc.cs.cpsc210.mindthegap.model.Line;
import ca.ubc.cs.cpsc210.mindthegap.model.Station;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Wrapper for TfL Arrival Data Provider
 */
public class TfLHttpArrivalDataProvider extends AbstractHttpDataProvider {
    //private static final String ARRIVALS_API_BASE = "https://api.tfl.gov.uk";              // for TfL data
    private static final String ARRIVALS_API_BASE = "http://kunghit.ugrad.cs.ubc.ca:6060/";   // for simulated data (3pm to midnight)
    private Station stn;
    private String urlFormat = "https://api.tfl.gov.uk/Line/";

    public TfLHttpArrivalDataProvider(Station stn) {
        super();
        this.stn = stn;
    }

    @Override
    protected URL getURL() throws MalformedURLException {
        String stopPointId = stn.getID();
        String lineId = "";
        String url;
        for (Line l : stn.getLines()) {
            lineId = lineId.concat(l.getId()).concat(",");
        }
        lineId = lineId.substring(0, lineId.length() - 1);
        url = ARRIVALS_API_BASE + "/Line/" + lineId + "/Arrivals?stopPointId=" + stopPointId;
        return new URL(url);
    }
}
