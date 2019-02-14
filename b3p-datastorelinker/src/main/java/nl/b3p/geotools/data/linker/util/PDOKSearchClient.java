/*
 * Copyright (C) 2017 B3Partners B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nl.b3p.geotools.data.linker.util;

import nl.b3p.geotools.data.linker.DataStoreLinker;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;


/**
 * @author Meine Toonen meinetoonen@b3partners.nl
 */
public class PDOKSearchClient {

    private SolrServer server;
    private static final Log log = LogFactory.getLog(DataStoreLinker.class);

    public PDOKSearchClient() {
        server = new HttpSolrServer("http://geodata.nationaalgeoregister.nl/locatieserver");
    }

    public JSONObject search(String term) {
        try {

            SolrQuery query = new SolrQuery();
            query.setQuery(term);
            query.setRequestHandler("/free");
            QueryResponse rsp = server.query(query);
            SolrDocumentList list = rsp.getResults();

            for (SolrDocument solrDocument : list) {
                System.out.println(solrDocument);
                JSONObject doc = solrDocumentToResult(solrDocument);

                // return on first
                if (doc != null) {
                    return doc;
                }
            }
        } catch (SolrServerException ex) {
            log.error("Cannot search:", ex);
        }

        return null;
    }


    private JSONObject solrDocumentToResult(SolrDocument doc) {
        JSONObject result = null;
        try {
            Map<String, Object> values = doc.getFieldValueMap();
            result = new JSONObject();
            for (String key : values.keySet()) {
                result.put(key, values.get(key));
            }

        } catch (JSONException ex) {
            log.error(ex);
        }
        return result;
    }
}