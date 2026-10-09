package ${package};

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONObject;

import java.io.IOException;

/**
 * @author ${author}
 */
public interface ${className}APIClient {

	public String get(String path) throws IOException, PortalException;

	public JSONObject getJSONObject(String path)
		throws IOException, PortalException;

}