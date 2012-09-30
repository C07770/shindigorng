package edu.ucsf.profiles.shindig.spi;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletResponse;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.util.ImmediateFuture;
import org.apache.shindig.protocol.DataCollection;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.social.opensocial.spi.AppDataService;
import org.apache.shindig.social.opensocial.spi.GroupId;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

public class ProfilesAppDataService implements AppDataService {

	private Common common;
	
	@Inject
	public ProfilesAppDataService(Common common) {
		this.common = common;
	}
	
	
    public Future<DataCollection> getPersonData(Set<UserId> userIds, GroupId groupId,
    	      String appId, Set<String> fields, SecurityToken token) throws ProtocolException {    
        Connection conn = common.getConnection();
		appId = common.getAppId(appId);
        try {
            Map<String, Map<String, String>> idToData = Maps.newHashMap();
            Set<String> idSet = common.getIdSet(userIds, groupId, token);
            for (String id : idSet) {
                System.out.println("getPersonData " +id + " "
                         + groupId.getType() + " " + appId);
                Map<String, String> data = Maps.newHashMap();
                for (String key : fields) {
                    String value = getData(conn, id, appId, key );
                	System.out.println("  "+key+" "+ value);
                    data.put(key, value);
                }
                idToData.put(id, data);
            }
            return ImmediateFuture.newInstance(new DataCollection(idToData));
        } catch (SQLException je) {
            throw new ProtocolException(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, je
                            .getMessage(), je);
        }
		finally {
			try { conn.close(); } catch (SQLException se) {
				throw new ProtocolException(
						HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se
								.getMessage(), se);
			}
		}
    }

    public Future<Void> deletePersonData(UserId userId, GroupId groupId,
            String appId, Set<String> fields, SecurityToken token)
            throws ProtocolException {
    	
		appId = common.getAppId(appId);        
        Connection conn = common.getConnection();
        String id = userId.getUserId(token);

        try {
            for (String key : fields) {
                deleteData(conn, id, appId, key);
            }
            return ImmediateFuture.newInstance(null);
        } catch (SQLException se) {
            throw new ProtocolException(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se
                            .getMessage(), se);
        }
		finally {
			try { conn.close(); } catch (SQLException se) {
				throw new ProtocolException(
						HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se
								.getMessage(), se);
			}
		}
    }

    public Future<Void> updatePersonData(UserId userId, GroupId groupId,
            String appId, Set<String> fields, Map<String, String> values,
            SecurityToken token) throws ProtocolException {

		appId = common.getAppId(appId);
		Connection conn = common.getConnection();
        String id = userId.getUserId(token);
        try {
            for (String key : values.keySet()) {
            	String value = values.get(key);
                upsertData(conn, id, appId, key, value);
            }
            return ImmediateFuture.newInstance(null);
        } catch (SQLException se) {
            throw new ProtocolException(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se
                            .getMessage(), se);
        }
		finally {
			try { conn.close(); } catch (SQLException se) {
				throw new ProtocolException(
						HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se
								.getMessage(), se);
			}
		}
    }

    private String getData(Connection conn, String id, String appId, String key)
            throws SQLException {
        PreparedStatement ps = conn
                .prepareStatement("select value from shindig_appdata where appId=? AND userId = ? AND keyName = ?");
        ps.setString(1, appId);
        ps.setString(2, id);
        ps.setString(3, key);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getString("value");
        }
        return null;
    }

    private void deleteData(Connection conn, String id, String appId, String key)
            throws SQLException {
        CallableStatement cs = conn
		        .prepareCall("{ call shindig_deleteAppData(?, ?, ?)}");
		cs.setInt(1, Integer.parseInt(id));
		cs.setInt(2, Integer.parseInt(appId));
		cs.setString(3, key);
        cs.execute();
    }

    private void upsertData(Connection conn, String id, String appId,
            String key, String value) throws SQLException {
        CallableStatement cs = conn
                .prepareCall("{ call shindig_upsertAppData(?, ?, ?, ?)}");
        cs.setInt(1, Integer.parseInt(id));
        cs.setInt(2, Integer.parseInt(appId));
        cs.setString(3, key);
        cs.setString(4, value);
        cs.execute();
    }

}
